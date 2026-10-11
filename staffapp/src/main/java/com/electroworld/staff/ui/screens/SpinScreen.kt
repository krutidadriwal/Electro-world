package com.electroworld.staff.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.CreateSpinGrantRequest
import com.electroworld.staff.data.network.CreateSpinPrizeRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.SaveSpinPurchaseItemRequest
import com.electroworld.staff.data.network.SpinGrant
import com.electroworld.staff.data.network.SpinOverviewResponse
import com.electroworld.staff.data.network.SpinPrize
import com.electroworld.staff.data.network.SpinPurchaseItem
import com.electroworld.staff.data.network.UndoSpinGrantRequest
import com.electroworld.staff.data.network.UpdateSpinPrizeRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

private enum class SpinTab(val label: String) { SPINS("Spins"), PRIZES("Prizes"), ITEMS("Purchase items") }

// A destructive action waiting on the admin's "are you sure".
private class PendingConfirm(val message: String, val failure: String, val action: suspend () -> Unit)

// The server's own { error } text where it sent one -- several of these
// (e.g. "still mapped to ...") say exactly what to fix, which a generic
// "failed" wouldn't.
private fun serverErrorMessage(e: Exception): String? = try {
  (e as? HttpException)?.response()?.errorBody()?.string()?.let { JSONObject(it).optString("error") }?.ifBlank { null }
} catch (parseError: Exception) {
  null
}

// Admin-only: everything behind the customer app's Spin to Win -- unlocking
// a spin when a customer buys something, the prize stock the wheel draws
// from, and which purchases can win which prizes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val scope = rememberCoroutineScope()

  var overview by remember { mutableStateOf<SpinOverviewResponse?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isBusy by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableStateOf(SpinTab.SPINS) }

  var showUnlockDialog by remember { mutableStateOf(false) }
  var showAddPrizeDialog by remember { mutableStateOf(false) }
  var showAddItemDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<SpinPurchaseItem?>(null) }
  var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      overview = NetworkModule.spinApi.overview()
    } catch (e: Exception) {
      // Keep whatever was already loaded; an action's own error (set just
      // before the refresh it triggers) takes priority over this one.
      if (errorMessage == null) errorMessage = "Failed to load Spin to Win."
    } finally {
      isLoading = false
    }
  }

  // Every change goes through here: run it, then reload so the three tabs
  // (which all read the same overview) can't drift apart.
  fun runAction(failure: String, action: suspend () -> Unit) {
    if (isBusy) return
    isBusy = true
    errorMessage = null
    scope.launch {
      try {
        action()
      } catch (e: Exception) {
        errorMessage = serverErrorMessage(e) ?: failure
      } finally {
        isBusy = false
        refreshTrigger++
      }
    }
  }

  val prizes = overview?.prizes ?: emptyList()
  val purchaseItems = overview?.purchaseItems ?: emptyList()

  if (showUnlockDialog) {
    UnlockSpinDialog(
      purchaseItems = purchaseItems,
      onDismiss = { showUnlockDialog = false },
      onUnlock = { phone, item ->
        showUnlockDialog = false
        runAction("Failed to unlock the spin.") {
          NetworkModule.spinApi.unlock(CreateSpinGrantRequest(phone = phone, purchaseItemId = item.id))
        }
      }
    )
  }

  if (showAddPrizeDialog) {
    AddPrizeDialog(
      onDismiss = { showAddPrizeDialog = false },
      onAdd = { name, quantity ->
        showAddPrizeDialog = false
        runAction("Failed to add the prize.") {
          NetworkModule.spinApi.createPrize(CreateSpinPrizeRequest(name = name, quantity = quantity))
        }
      }
    )
  }

  if (showAddItemDialog || editingItem != null) {
    val existing = editingItem
    PurchaseItemDialog(
      existing = existing,
      prizes = prizes,
      onDismiss = {
        showAddItemDialog = false
        editingItem = null
      },
      onSave = { name, mappedPrizeIds ->
        showAddItemDialog = false
        editingItem = null
        runAction("Failed to save the purchase item.") {
          val request = SaveSpinPurchaseItemRequest(id = existing?.id, name = name, mappedPrizeIds = mappedPrizeIds)
          if (existing == null) {
            NetworkModule.spinApi.createPurchaseItem(request)
          } else {
            NetworkModule.spinApi.updatePurchaseItem(request)
          }
        }
      }
    )
  }

  pendingConfirm?.let { confirm ->
    AlertDialog(
      onDismissRequest = { pendingConfirm = null },
      text = { Text(confirm.message) },
      confirmButton = {
        Button(onClick = {
          pendingConfirm = null
          runAction(confirm.failure, confirm.action)
        }) { Text("Confirm") }
      },
      dismissButton = { TextButton(onClick = { pendingConfirm = null }) { Text("Cancel") } }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Spin to Win") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      if (overview != null) {
        FloatingActionButton(
          onClick = {
            when (selectedTab) {
              SpinTab.SPINS -> showUnlockDialog = true
              SpinTab.PRIZES -> showAddPrizeDialog = true
              SpinTab.ITEMS -> showAddItemDialog = true
            }
          }
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = when (selectedTab) {
              SpinTab.SPINS -> "Unlock a spin"
              SpinTab.PRIZES -> "Add prize"
              SpinTab.ITEMS -> "Add purchase item"
            }
          )
        }
      }
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      TabRow(selectedTabIndex = selectedTab.ordinal) {
        SpinTab.values().forEach { tab ->
          Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab.label) })
        }
      }
      errorMessage?.let {
        // secondary, not error -- this sits directly on `background`, and
        // the palette's error role is Charcoal, invisible there in dark mode.
        Text(
          it,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
      }

      val loaded = overview
      if (loaded == null) {
        if (isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        return@Column
      }

      // Bottom padding leaves room for the FAB over the last row's buttons.
      val listPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp)
      when (selectedTab) {
        SpinTab.SPINS -> LazyColumn(contentPadding = listPadding) {
          item {
            Text(
              "${loaded.pendingCount} pending  ·  ${loaded.playedCount} played",
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(bottom = 12.dp)
            )
          }
          if (loaded.grants.isEmpty()) {
            item { EmptyHint("No spins unlocked yet. Tap + when a customer makes a purchase.") }
          }
          items(loaded.grants, key = { it.id }) { grant ->
            GrantRow(
              grant = grant,
              enabled = !isBusy,
              onRevoke = {
                pendingConfirm = PendingConfirm(
                  message = "Revoke the unplayed spin for ${grant.phone}?",
                  failure = "Failed to revoke the spin."
                ) { NetworkModule.spinApi.revoke(grant.id) }
              },
              onUndo = {
                pendingConfirm = PendingConfirm(
                  message = "Undo this win for ${grant.phone}? \"${grant.prizeName ?: "The prize"}\" goes back into stock " +
                    "and the customer can spin again.",
                  failure = "Failed to undo the win."
                ) { NetworkModule.spinApi.undoWin(UndoSpinGrantRequest(grant.id)) }
              }
            )
          }
        }

        SpinTab.PRIZES -> LazyColumn(contentPadding = listPadding) {
          if (prizes.isEmpty()) {
            item { EmptyHint("No prizes yet. Customers can't spin until at least one is in stock.") }
          }
          items(prizes, key = { it.id }) { prize ->
            PrizeRow(
              prize = prize,
              enabled = !isBusy,
              onSetQuantity = { quantity ->
                runAction("Failed to update stock.") {
                  NetworkModule.spinApi.updatePrize(UpdateSpinPrizeRequest(id = prize.id, quantity = quantity))
                }
              },
              onDelete = {
                pendingConfirm = PendingConfirm(
                  message = "Delete \"${prize.name}\" from the prize list?",
                  failure = "Failed to delete the prize."
                ) { NetworkModule.spinApi.deletePrize(prize.id) }
              }
            )
          }
        }

        SpinTab.ITEMS -> LazyColumn(contentPadding = listPadding) {
          if (purchaseItems.isEmpty()) {
            item { EmptyHint("No purchase items yet. Add what customers buy so a spin can be unlocked against it.") }
          }
          items(purchaseItems, key = { it.id }) { item ->
            PurchaseItemRow(
              item = item,
              prizes = prizes,
              enabled = !isBusy,
              onEdit = { editingItem = item },
              onDelete = {
                pendingConfirm = PendingConfirm(
                  message = "Delete \"${item.name}\"? Spins already unlocked with it are not affected.",
                  failure = "Failed to delete the purchase item."
                ) { NetworkModule.spinApi.deletePurchaseItem(item.id) }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyHint(text: String) {
  Text(text, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 24.dp))
}

// Server timestamps are full ISO-8601; rows only need the date, as DD-MM-YYYY.
private fun formatSpinDate(isoTimestamp: String): String {
  val parts = isoTimestamp.substringBefore('T').split("-")
  return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else isoTimestamp
}

@Composable
private fun GrantRow(grant: SpinGrant, enabled: Boolean, onRevoke: () -> Unit, onUndo: () -> Unit) {
  val isPlayed = grant.usedAt != null
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = listOfNotNull(grant.customerName, grant.phone).joinToString("  ·  "),
          style = MaterialTheme.typography.bodyLarge
        )
        Text("Bought: ${grant.boughtItem}", style = MaterialTheme.typography.bodyMedium)
        Text(
          text = if (isPlayed) {
            "Won ${grant.prizeName ?: "a prize"} on ${formatSpinDate(grant.usedAt ?: "")}"
          } else {
            "Pending since ${formatSpinDate(grant.createdAt)}"
          },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (isPlayed) FontWeight.Bold else FontWeight.Normal
        )
      }
      TextButton(onClick = if (isPlayed) onUndo else onRevoke, enabled = enabled) {
        Text(if (isPlayed) "Undo win" else "Revoke")
      }
    }
  }
}

@Composable
private fun PrizeRow(prize: SpinPrize, enabled: Boolean, onSetQuantity: (Int) -> Unit, onDelete: () -> Unit) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(prize.name, style = MaterialTheme.typography.bodyLarge)
        if (prize.quantity == 0) {
          Text("Out of stock -- not on the wheel", style = MaterialTheme.typography.labelMedium)
        }
      }
      IconButton(onClick = { onSetQuantity(prize.quantity - 1) }, enabled = enabled && prize.quantity > 0) {
        Icon(Icons.Default.Remove, contentDescription = "Decrease stock")
      }
      Text(prize.quantity.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      IconButton(onClick = { onSetQuantity(prize.quantity + 1) }, enabled = enabled) {
        Icon(Icons.Default.Add, contentDescription = "Increase stock")
      }
      IconButton(onClick = onDelete, enabled = enabled) {
        Icon(Icons.Default.Delete, contentDescription = "Delete prize")
      }
    }
  }
}

@Composable
private fun PurchaseItemRow(
  item: SpinPurchaseItem,
  prizes: List<SpinPrize>,
  enabled: Boolean,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val mappedNames = prizes.filter { it.id in item.mappedPrizeIds }.map { it.name }
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.name, style = MaterialTheme.typography.bodyLarge)
        Text(
          text = if (item.mappedPrizeIds.isEmpty()) "Wins: any prize in stock" else "Wins: ${mappedNames.joinToString(", ")}",
          style = MaterialTheme.typography.labelMedium
        )
      }
      IconButton(onClick = onEdit, enabled = enabled) {
        Icon(Icons.Default.Edit, contentDescription = "Edit purchase item")
      }
      IconButton(onClick = onDelete, enabled = enabled) {
        Icon(Icons.Default.Delete, contentDescription = "Delete purchase item")
      }
    }
  }
}

@Composable
private fun UnlockSpinDialog(
  purchaseItems: List<SpinPurchaseItem>,
  onDismiss: () -> Unit,
  onUnlock: (phone: String, item: SpinPurchaseItem) -> Unit
) {
  var phone by remember { mutableStateOf("") }
  var selectedItem by remember { mutableStateOf<SpinPurchaseItem?>(null) }
  var itemMenuExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Unlock a spin") },
    text = {
      Column {
        if (purchaseItems.isEmpty()) {
          Text("Add a purchase item first (Purchase items tab).")
        } else {
          OutlinedTextField(
            value = phone,
            onValueChange = { input -> phone = input.filter { it.isDigit() }.take(10) },
            label = { Text("Customer phone (10 digits)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
          )
          Box(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
              value = selectedItem?.name ?: "",
              onValueChange = {},
              readOnly = true,
              label = { Text("What did they buy?") },
              modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.matchParentSize().clickable { itemMenuExpanded = true })
            DropdownMenu(expanded = itemMenuExpanded, onDismissRequest = { itemMenuExpanded = false }) {
              purchaseItems.forEach { item ->
                DropdownMenuItem(
                  text = { Text(item.name) },
                  onClick = {
                    selectedItem = item
                    itemMenuExpanded = false
                  }
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = phone.length == 10 && selectedItem != null,
        onClick = { selectedItem?.let { onUnlock(phone, it) } }
      ) { Text("Unlock") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
private fun AddPrizeDialog(onDismiss: () -> Unit, onAdd: (name: String, quantity: Int) -> Unit) {
  var name by remember { mutableStateOf("") }
  var quantity by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add prize") },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Prize name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = quantity,
          onValueChange = { input -> quantity = input.filter { it.isDigit() }.take(5) },
          label = { Text("Quantity in stock") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
      }
    },
    confirmButton = {
      Button(
        enabled = name.isNotBlank(),
        onClick = { onAdd(name.trim(), quantity.toIntOrNull() ?: 0) }
      ) { Text("Add") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
private fun PurchaseItemDialog(
  existing: SpinPurchaseItem?,
  prizes: List<SpinPrize>,
  onDismiss: () -> Unit,
  onSave: (name: String, mappedPrizeIds: List<String>) -> Unit
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  // Only ids of prizes that still exist -- saving drops any stale ones.
  var mappedPrizeIds by remember {
    mutableStateOf(existing?.mappedPrizeIds.orEmpty().filter { id -> prizes.any { it.id == id } }.toSet())
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Add purchase item" else "Edit purchase item") },
    text = {
      Column {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Item name (e.g. Washing Machine)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Text(
          text = if (mappedPrizeIds.isEmpty()) {
            "Prizes it can win: any prize in stock. Tick prizes to limit it to those."
          } else {
            "Prizes it can win: only the ticked ones."
          },
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
          prizes.forEach { prize ->
            val isMapped = prize.id in mappedPrizeIds
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { mappedPrizeIds = if (isMapped) mappedPrizeIds - prize.id else mappedPrizeIds + prize.id },
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Start
            ) {
              Checkbox(checked = isMapped, onCheckedChange = null, modifier = Modifier.padding(8.dp).size(24.dp))
              Text("${prize.name} (${prize.quantity} in stock)")
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = name.isNotBlank(),
        onClick = { onSave(name.trim(), mappedPrizeIds.toList()) }
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

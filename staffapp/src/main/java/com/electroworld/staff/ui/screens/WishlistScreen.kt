package com.electroworld.staff.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.Category
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.Subcategory
import com.electroworld.staff.data.network.WishlistItem
import com.electroworld.staff.data.network.WishlistAddRequest
import com.electroworld.staff.data.network.WishlistStatusRequest
import com.electroworld.staff.data.network.WishlistCallRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val WISHLIST_STATUSES = listOf(
  "not_contacted",
  "interested",
  "not_interested",
  "call_back_later",
  "purchased",
  "unreachable"
)

private fun statusLabel(status: String): String = when (status) {
  "not_contacted" -> "Not Contacted"
  "interested" -> "Interested"
  "not_interested" -> "Not Interested"
  "call_back_later" -> "Call Back Later"
  "purchased" -> "Purchased"
  "unreachable" -> "Unreachable"
  else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(modifier: Modifier = Modifier) {
  var items by remember { mutableStateOf<List<WishlistItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf<String?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  if (showAddDialog) {
    AddToWishlistDialog(
      onDismiss = { showAddDialog = false },
      onAdded = {
        showAddDialog = false
        refreshTrigger++
      }
    )
  }

  val filteredItems = items
    .filter { statusFilter == null || it.status == statusFilter }
    .filter { item ->
      searchQuery.isBlank() ||
        item.customerName.contains(searchQuery, ignoreCase = true) ||
        item.customerPhone.contains(searchQuery, ignoreCase = true) ||
        item.categoryName.contains(searchQuery, ignoreCase = true) ||
        (item.subcategoryName?.contains(searchQuery, ignoreCase = true) ?: false)
    }

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      items = NetworkModule.wishlistApi.list().items
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load wishlist."
    } finally {
      isLoading = false
    }
  }

  Scaffold(
    topBar = { TopAppBar(title = { Text("Wishlist") }) },
    floatingActionButton = {
      FloatingActionButton(onClick = { showAddDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = "Add customer to wishlist")
      }
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Search customer or item") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      )

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        item {
          FilterChip(
            selected = statusFilter == null,
            onClick = { statusFilter = null },
            label = { Text("All") }
          )
        }
        items(WISHLIST_STATUSES) { status ->
          FilterChip(
            selected = statusFilter == status,
            onClick = { statusFilter = if (statusFilter == status) null else status },
            label = { Text(statusLabel(status)) }
          )
        }
      }

      errorMessage?.let {
        Text(
          it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
      }

      if (isLoading && items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else if (filteredItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            if (items.isEmpty()) "No wishlist entries yet." else "No matches found.",
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
          items(filteredItems, key = { it.id }) { wishlistItem ->
            WishlistRow(
              item = wishlistItem,
              onStatusSelected = { newStatus ->
                scope.launch {
                  try {
                    NetworkModule.wishlistApi.updateStatus(
                      WishlistStatusRequest(wishlistItemId = wishlistItem.id, status = newStatus)
                    )
                    refreshTrigger++
                  } catch (e: Exception) {
                    errorMessage = "Failed to update status."
                  }
                }
              },
              onCall = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${wishlistItem.customerPhone}")))
                scope.launch {
                  try {
                    NetworkModule.wishlistApi.logCall(WishlistCallRequest(wishlistItemId = wishlistItem.id))
                    refreshTrigger++
                  } catch (e: Exception) {
                    // The call itself already went through via the dialer -- a failed log
                    // shouldn't look like a failed call to the salesperson.
                  }
                }
              }
            )
          }
        }
      }
    }
  }
}

// Lets a salesperson log a walk-in customer's interest directly -- the
// customer doesn't need the EW app or even an account yet (the server
// creates a bare placeholder account for a new phone number); once they
// later verify that number in the app, this wishlist entry is already there.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToWishlistDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
  var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
  var isLoadingCategories by remember { mutableStateOf(true) }
  var phone by remember { mutableStateOf("") }
  var customerName by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<Category?>(null) }
  var selectedSubcategory by remember { mutableStateOf<Subcategory?>(null) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }
  var subcategoryMenuExpanded by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    try {
      categories = NetworkModule.catalogApi.categories().categories
    } catch (e: Exception) {
      errorMessage = "Failed to load categories."
    } finally {
      isLoadingCategories = false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add to wishlist") },
    text = {
      Column {
        OutlinedTextField(
          value = phone,
          onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) phone = it },
          label = { Text("Customer phone (10 digits)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = customerName,
          onValueChange = { customerName = it },
          label = { Text("Customer name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        if (isLoadingCategories) {
          CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else {
          Box(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
              value = selectedCategory?.name ?: "",
              onValueChange = {},
              readOnly = true,
              label = { Text("Category") },
              modifier = Modifier.fillMaxWidth().clickable { categoryMenuExpanded = true }
            )
            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
              categories.forEach { category ->
                DropdownMenuItem(
                  text = { Text(category.name) },
                  onClick = {
                    selectedCategory = category
                    selectedSubcategory = null
                    categoryMenuExpanded = false
                  }
                )
              }
            }
          }

          val subcategories = selectedCategory?.subcategories.orEmpty()
          if (subcategories.isNotEmpty()) {
            Box(modifier = Modifier.padding(top = 8.dp)) {
              OutlinedTextField(
                value = selectedSubcategory?.name ?: "Any",
                onValueChange = {},
                readOnly = true,
                label = { Text("Subcategory (optional)") },
                modifier = Modifier.fillMaxWidth().clickable { subcategoryMenuExpanded = true }
              )
              DropdownMenu(expanded = subcategoryMenuExpanded, onDismissRequest = { subcategoryMenuExpanded = false }) {
                DropdownMenuItem(
                  text = { Text("Any") },
                  onClick = { selectedSubcategory = null; subcategoryMenuExpanded = false }
                )
                subcategories.forEach { subcategory ->
                  DropdownMenuItem(
                    text = { Text(subcategory.name) },
                    onClick = { selectedSubcategory = subcategory; subcategoryMenuExpanded = false }
                  )
                }
              }
            }
          }
        }

        errorMessage?.let {
          Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isSaving && phone.length == 10 && customerName.isNotBlank() && selectedCategory != null,
        onClick = {
          val category = selectedCategory ?: return@Button
          errorMessage = null
          isSaving = true
          scope.launch {
            try {
              NetworkModule.wishlistApi.add(
                WishlistAddRequest(
                  phone = phone,
                  customerName = customerName.trim(),
                  categoryIconKey = category.iconKey,
                  subcategoryId = selectedSubcategory?.id
                )
              )
              onAdded()
            } catch (e: Exception) {
              errorMessage = "Failed to add to wishlist. Please try again."
            } finally {
              isSaving = false
            }
          }
        }
      ) {
        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Add")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
private fun WishlistRow(
  item: WishlistItem,
  onStatusSelected: (String) -> Unit,
  onCall: () -> Unit
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }
  var confirmingPurchase by remember { mutableStateOf(false) }

  if (confirmingPurchase) {
    AlertDialog(
      onDismissRequest = { confirmingPurchase = false },
      title = { Text("Mark as purchased?") },
      text = { Text("This will remove ${item.customerName} from the wishlist.") },
      confirmButton = {
        androidx.compose.material3.TextButton(
          onClick = {
            confirmingPurchase = false
            onStatusSelected("purchased")
          }
        ) { Text("Remove") }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = { confirmingPurchase = false }) { Text("Cancel") }
      }
    )
  }

  Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(item.customerName, style = MaterialTheme.typography.bodyLarge)
          Text(
            item.customerPhone,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
        IconButton(onClick = onCall) {
          Icon(Icons.Default.Call, contentDescription = "Call ${item.customerName}")
        }
      }

      Text(
        if (item.subcategoryName != null) "${item.categoryName} · ${item.subcategoryName}" else item.categoryName,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )

      Text(
        "Wishlisted: ${formatIst(item.createdAt) ?: item.createdAt}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 4.dp)
      )

      item.lastCalledAt?.let {
        Text(
          "Last called: ${formatIst(it) ?: it}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }

      Box(modifier = Modifier.padding(top = 8.dp)) {
        AssistChip(
          onClick = { statusMenuExpanded = true },
          label = { Text(statusLabel(item.status)) },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = statusMenuExpanded, onDismissRequest = { statusMenuExpanded = false }) {
          WISHLIST_STATUSES.forEach { status ->
            DropdownMenuItem(
              text = { Text(statusLabel(status)) },
              onClick = {
                statusMenuExpanded = false
                if (status != item.status) {
                  if (status == "purchased") {
                    confirmingPurchase = true
                  } else {
                    onStatusSelected(status)
                  }
                }
              }
            )
          }
        }
      }
    }
  }
}

// Postgres timestamptz values arrive as UTC ISO-8601 strings (e.g.
// "2026-09-30T12:34:56.789Z") -- reformatted here into IST since that's the
// timezone the sales staff using this app work in.
private fun formatIst(isoTimestamp: String): String? {
  return try {
    val truncated = isoTimestamp.substringBefore('.').substringBefore('Z')
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = parser.parse(truncated) ?: return null
    val formatter = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    "${formatter.format(date)} IST"
  } catch (e: Exception) {
    null
  }
}

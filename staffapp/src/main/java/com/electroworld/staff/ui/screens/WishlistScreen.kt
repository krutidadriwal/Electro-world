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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.electroworld.staff.data.network.lookupCustomerName
import com.electroworld.staff.data.network.WishlistItem
import com.electroworld.staff.data.network.WishlistAddRequest
import com.electroworld.staff.data.network.WishlistStatusRequest
import com.electroworld.staff.data.network.WishlistCallRequest
import androidx.compose.ui.graphics.Color
import com.electroworld.staff.ui.theme.StatusGreen
import com.electroworld.staff.ui.theme.StatusRed
import com.electroworld.staff.ui.theme.StatusYellow
import com.electroworld.staff.ui.theme.staffCardColors
import com.electroworld.staff.ui.theme.statusCardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val WISHLIST_STATUSES = listOf(
  "not_contacted",
  "unreachable",
  "invalid_number",
  "call_back_later",
  "sent_details_whatsapp",
  "store_visit_scheduled",
  "interested_comparing_prices",
  "interested_waiting_emi",
  "interested_waiting_stock",
  "interested_waiting_offer",
  "purchased",
  "pending_delivery",
  "bought_elsewhere",
  "dropped_plan",
  "not_interested"
)

// Card tint per status (see statusCardColors): red = still to call, or a
// lost lead; yellow = being followed up; green = won (purchased rows are
// removed from the list, so pending delivery is the win that's still shown).
private fun wishlistStatusColor(status: String): Color? = when (status) {
  "not_contacted", "invalid_number", "not_interested", "bought_elsewhere", "dropped_plan" -> StatusRed
  "unreachable", "call_back_later", "sent_details_whatsapp", "store_visit_scheduled",
  "interested", "interested_comparing_prices", "interested_waiting_emi",
  "interested_waiting_stock", "interested_waiting_offer" -> StatusYellow
  "purchased", "pending_delivery" -> StatusGreen
  else -> null
}

private fun statusLabel(status: String): String = when (status) {
  "not_contacted" -> "Not Contacted"
  "unreachable" -> "Unreachable (No answer/Switched off)"
  "invalid_number" -> "Invalid/Wrong Number"
  "call_back_later" -> "Call Back Later"
  "sent_details_whatsapp" -> "Sent Details (WhatsApp)"
  "store_visit_scheduled" -> "Store Visit Scheduled"
  "interested_comparing_prices" -> "Interested - Comparing Prices"
  "interested_waiting_emi" -> "Interested - Waiting for EMI/Finance"
  "interested_waiting_stock" -> "Interested - Waiting for Stock"
  "interested_waiting_offer" -> "Interested - Waiting for Sale/Offer"
  "purchased" -> "Purchased (Deal Closed)"
  "pending_delivery" -> "Pending Delivery/Installation"
  "bought_elsewhere" -> "Bought Elsewhere (Competitor/Online)"
  "dropped_plan" -> "Dropped Plan (Postponed/Cancelled)"
  "not_interested" -> "Not Interested"
  else -> status
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(initialStatusFilter: String? = null, onBack: () -> Unit, modifier: Modifier = Modifier) {
  var items by remember { mutableStateOf<List<WishlistItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf(initialStatusFilter) }
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
    topBar = {
      TopAppBar(
        title = { Text("Wishlist") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
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

      var statusFilterMenuExpanded by remember { mutableStateOf(false) }
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
          value = statusFilter?.let { statusLabel(it) } ?: "All",
          onValueChange = {},
          readOnly = true,
          label = { Text("Filter by status") },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
          modifier = Modifier.fillMaxWidth()
        )
        // A readOnly OutlinedTextField's own touch handling (cursor/focus)
        // swallows taps before a clickable attached directly to it ever
        // fires -- an invisible overlay on top intercepts the tap first.
        Box(
          modifier = Modifier
            .matchParentSize()
            .clickable { statusFilterMenuExpanded = true }
        )
        DropdownMenu(expanded = statusFilterMenuExpanded, onDismissRequest = { statusFilterMenuExpanded = false }) {
          DropdownMenuItem(
            text = { Text("All") },
            onClick = { statusFilter = null; statusFilterMenuExpanded = false }
          )
          WISHLIST_STATUSES.forEach { status ->
            DropdownMenuItem(
              text = { Text(statusLabel(status)) },
              onClick = { statusFilter = status; statusFilterMenuExpanded = false }
            )
          }
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
  var customerExists by remember { mutableStateOf(false) }
  var isLookingUpCustomer by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf<Category?>(null) }
  var selectedSubcategory by remember { mutableStateOf<Subcategory?>(null) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }
  var subcategoryMenuExpanded by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var categoriesLoadTrigger by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(categoriesLoadTrigger) {
    isLoadingCategories = true
    try {
      categories = NetworkModule.catalogApi.categories().categories
    } catch (e: Exception) {
      categories = emptyList()
    } finally {
      isLoadingCategories = false
    }
  }

  // Auto-fills the name for a phone that already has an account, instead of
  // staff re-typing it -- and locks the field so it can't be accidentally
  // overwritten, since that name belongs to the real account, not this form.
  LaunchedEffect(phone) {
    if (phone.length == 10) {
      isLookingUpCustomer = true
      val existingName = lookupCustomerName(phone)
      if (existingName != null) {
        customerName = existingName
        customerExists = true
      } else {
        customerExists = false
      }
      isLookingUpCustomer = false
    } else {
      customerExists = false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add to wishlist") },
    text = {
      // AlertDialog's text slot doesn't scroll on its own and this form has
      // enough fields to overflow its capped height on smaller screens --
      // without this, fields past the fold (the category picker included)
      // render but are simply unreachable, which looks like the picker never
      // loaded at all.
      Column(
        modifier = Modifier
          .heightIn(max = 560.dp)
          .verticalScroll(rememberScrollState())
      ) {
        OutlinedTextField(
          value = phone,
          onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) phone = it },
          label = { Text("Customer phone (10 digits)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        CustomerLookupStatus(phone, isLookingUpCustomer, customerExists)
        OutlinedTextField(
          value = customerName,
          onValueChange = { customerName = it },
          readOnly = customerExists,
          label = { Text(if (customerExists) "Customer name (existing account)" else "Customer name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        if (isLoadingCategories) {
          CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else if (categories.isEmpty()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
          ) {
            Text("Couldn't load categories.", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { categoriesLoadTrigger++ }) { Text("Retry") }
          }
        } else {
          Box(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
              value = selectedCategory?.name ?: "",
              onValueChange = {},
              readOnly = true,
              label = { Text("Category") },
              modifier = Modifier.fillMaxWidth()
            )
            Box(
              modifier = Modifier
                .matchParentSize()
                .clickable { categoryMenuExpanded = true }
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
                modifier = Modifier.fillMaxWidth()
              )
              Box(
                modifier = Modifier
                  .matchParentSize()
                  .clickable { subcategoryMenuExpanded = true }
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
        enabled = !isSaving && phone.length == 10 && (customerExists || customerName.isNotBlank()) && selectedCategory != null,
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
      title = { Text("Mark deal as closed?") },
      text = { Text("${item.customerName} will stay in this list as Purchased for a week, then drop off.") },
      confirmButton = {
        androidx.compose.material3.TextButton(
          onClick = {
            confirmingPurchase = false
            onStatusSelected("purchased")
          }
        ) { Text("Mark closed") }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = { confirmingPurchase = false }) { Text("Cancel") }
      }
    )
  }

  Card(colors = statusCardColors(wishlistStatusColor(item.status)), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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

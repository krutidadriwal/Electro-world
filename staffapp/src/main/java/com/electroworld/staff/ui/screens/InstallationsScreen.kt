package com.electroworld.staff.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Search
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
import com.electroworld.staff.data.network.InstallationAddRequest
import com.electroworld.staff.data.network.InstallationItem
import com.electroworld.staff.data.network.InstallationStatusRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.lookupCustomerName
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun requestTypeLabel(item: InstallationItem): String = when {
  item.wantsDemo && item.wantsInstallation -> "Demo + Installation"
  item.wantsDemo -> "Demo"
  item.wantsInstallation -> "Installation"
  else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationsScreen(initialStatusFilter: String? = null, onBack: () -> Unit, modifier: Modifier = Modifier) {
  var items by remember { mutableStateOf<List<InstallationItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf(initialStatusFilter) }
  var selectedItem by remember { mutableStateOf<InstallationItem?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  val filteredItems = items
    .filter { statusFilter == null || it.status == statusFilter }
    .filter { item ->
      searchQuery.isBlank() ||
        item.customerName.contains(searchQuery, ignoreCase = true) ||
        item.customerPhone.contains(searchQuery, ignoreCase = true) ||
        item.categoryName.contains(searchQuery, ignoreCase = true) ||
        item.itemName.contains(searchQuery, ignoreCase = true)
    }

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      items = NetworkModule.installationsApi.list().items
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load installation requests."
    } finally {
      isLoading = false
    }
  }

  fun updateStatus(item: InstallationItem, newStatus: String, fields: StatusChangeFields) {
    scope.launch {
      try {
        NetworkModule.installationsApi.updateStatus(
          InstallationStatusRequest(
            installationId = item.id,
            status = newStatus,
            referenceNumber = fields.referenceNumber,
            technicianName = fields.technicianName,
            technicianMobile = fields.technicianMobile,
            reason = fields.reason,
            closedDate = fields.closedDate
          )
        )
        refreshTrigger++
      } catch (e: Exception) {
        errorMessage = "Failed to update status."
      }
    }
  }

  selectedItem?.let { item ->
    BackHandler { selectedItem = null }
    InstallationDetailScreen(
      item = item,
      onBack = { selectedItem = null },
      onStatusSelected = { newStatus, fields -> updateStatus(item, newStatus, fields) },
      modifier = modifier
    )
    return
  }

  if (showAddDialog) {
    AddInstallationDialog(
      onDismiss = { showAddDialog = false },
      onAdded = {
        showAddDialog = false
        refreshTrigger++
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Installations") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { showAddDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = "Log an installation request")
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
          value = statusFilter?.let { serviceStatusLabel(it) } ?: "All",
          onValueChange = {},
          readOnly = true,
          label = { Text("Filter by status") },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
          modifier = Modifier.fillMaxWidth()
        )
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
          ALL_STATUSES_FOR_FILTER.forEach { status ->
            DropdownMenuItem(
              text = { Text(serviceStatusLabel(status)) },
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
            if (items.isEmpty()) "No installation requests yet." else "No matches found.",
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
          items(filteredItems, key = { it.id }) { installation ->
            InstallationRow(
              item = installation,
              onClick = { selectedItem = installation },
              onStatusSelected = { newStatus, fields -> updateStatus(installation, newStatus, fields) },
              onCall = {
                val number = installation.contactPhone?.takeIf { it.isNotBlank() } ?: installation.customerPhone
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InstallationRow(
  item: InstallationItem,
  onClick: () -> Unit,
  onStatusSelected: (String, StatusChangeFields) -> Unit,
  onCall: () -> Unit
) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick)) {
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
        "${item.categoryName} · ${item.itemName}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )
      Text(
        requestTypeLabel(item),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 4.dp)
      )

      Box(modifier = Modifier.padding(top = 8.dp)) {
        StatusAssistChip(currentStatus = item.status, onConfirmed = onStatusSelected)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallationDetailScreen(
  item: InstallationItem,
  onBack: () -> Unit,
  onStatusSelected: (String, StatusChangeFields) -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Installation request details") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
      Text(item.customerName, style = MaterialTheme.typography.headlineSmall)
      Text(
        item.customerPhone,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline
      )

      Spacer(modifier = Modifier.height(16.dp))
      DetailField("Category", item.categoryName)
      DetailField("Item", item.itemName)
      DetailField("Requested", requestTypeLabel(item))
      DetailField("Address", item.address)
      item.contactPhone?.takeIf { it.isNotBlank() }?.let { DetailField("Contact phone", it) }
      item.invoiceFileName?.takeIf { it.isNotBlank() }?.let { DetailField("Invoice", it) }
      DetailField("Requested on", formatIst(item.createdAt) ?: item.createdAt)
      item.resolvedAt?.let { DetailField("Resolved", formatIst(it) ?: it) }
      item.installationNumber?.takeIf { it.isNotBlank() }?.let { DetailField("Service center no.", it) }
      if (!item.technicianName.isNullOrBlank() || !item.technicianMobile.isNullOrBlank()) {
        DetailField("Technician", listOfNotNull(item.technicianName, item.technicianMobile).joinToString(" - "))
      }
      item.blockedReason?.takeIf { it.isNotBlank() }?.let { DetailField("Blocked reason", it) }
      item.cancellationReason?.takeIf { it.isNotBlank() }?.let { DetailField("Cancellation reason", it) }
      item.closedDate?.let { DetailField("Closed date", it) }

      Spacer(modifier = Modifier.height(16.dp))
      StatusAssistChip(currentStatus = item.status, onConfirmed = onStatusSelected)
    }
  }
}

@Composable
private fun DetailField(label: String, value: String) {
  Column(modifier = Modifier.padding(bottom = 12.dp)) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
    Text(value, style = MaterialTheme.typography.bodyLarge)
  }
}

// Lets a salesperson log a walk-in/phone-in demo/installation request
// directly -- same no-account-needed pattern as the Wishlist tab's
// AddToWishlistDialog (the server creates a bare placeholder account for a
// new phone number).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInstallationDialog(onDismiss: () -> Unit, onAdded: () -> Unit) {
  var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
  var isLoadingCategories by remember { mutableStateOf(true) }
  var categoriesLoadTrigger by remember { mutableStateOf(0) }
  var phone by remember { mutableStateOf("") }
  var customerName by remember { mutableStateOf("") }
  var customerExists by remember { mutableStateOf(false) }
  var isLookingUpCustomer by remember { mutableStateOf(false) }
  var itemName by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf<Category?>(null) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }
  var wantsDemo by remember { mutableStateOf(false) }
  var wantsInstallation by remember { mutableStateOf(false) }
  var address by remember { mutableStateOf("") }
  var contactPhone by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  // Only categories where at least one of demo/installation is actually
  // offered -- matches the customer app's own Installation Request form.
  val installableCategories = categories.filter { it.canInstall || it.canDemo }

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

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log an installation request") },
    text = {
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
        OutlinedTextField(
          value = itemName,
          onValueChange = { itemName = it },
          label = { Text("Item (e.g. LG 1.5 Ton Split AC)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        if (isLoadingCategories) {
          CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        } else if (installableCategories.isEmpty()) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
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
            Box(modifier = Modifier.matchParentSize().clickable { categoryMenuExpanded = true })
            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
              installableCategories.forEach { category ->
                DropdownMenuItem(
                  text = { Text(category.name) },
                  onClick = {
                    selectedCategory = category
                    // Pre-tick whichever option the category actually
                    // offers, same default as the customer app's form.
                    wantsDemo = category.canDemo
                    wantsInstallation = category.canInstall
                    categoryMenuExpanded = false
                  }
                )
              }
            }
          }

          selectedCategory?.let { category ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 8.dp).clickable(enabled = category.canDemo) { wantsDemo = !wantsDemo }
            ) {
              Checkbox(checked = wantsDemo, onCheckedChange = { wantsDemo = it }, enabled = category.canDemo)
              Text(
                "Demo" + if (!category.canDemo) " (not available for this category)" else "",
                color = if (category.canDemo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable(enabled = category.canInstall) { wantsInstallation = !wantsInstallation }
            ) {
              Checkbox(checked = wantsInstallation, onCheckedChange = { wantsInstallation = it }, enabled = category.canInstall)
              Text(
                "Installation" + if (!category.canInstall) " (not available for this category)" else "",
                color = if (category.canInstall) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
              )
            }
          }
        }

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Address") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
          value = contactPhone,
          onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) contactPhone = it },
          label = { Text("Alternate contact phone (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        errorMessage?.let {
          Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isSaving && phone.length == 10 && (customerExists || customerName.isNotBlank()) && itemName.isNotBlank() &&
          selectedCategory != null && address.isNotBlank() && (wantsDemo || wantsInstallation),
        onClick = {
          val category = selectedCategory ?: return@Button
          errorMessage = null
          isSaving = true
          scope.launch {
            try {
              NetworkModule.installationsApi.add(
                InstallationAddRequest(
                  phone = phone,
                  customerName = customerName.trim(),
                  categoryIconKey = category.iconKey,
                  itemName = itemName.trim(),
                  wantsDemo = wantsDemo,
                  wantsInstallation = wantsInstallation,
                  address = address.trim(),
                  contactPhone = contactPhone.takeIf { it.isNotBlank() }
                )
              )
              onAdded()
            } catch (e: Exception) {
              errorMessage = "Failed to log request. Please try again."
            } finally {
              isSaving = false
            }
          }
        }
      ) {
        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Log")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

// Postgres timestamptz values arrive as UTC ISO-8601 strings -- reformatted
// here into IST since that's the timezone the sales staff using this app
// work in.
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

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.electroworld.staff.data.network.InstallationItem
import com.electroworld.staff.data.network.InstallationStatusRequest
import com.electroworld.staff.data.network.NetworkModule
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val SERVICE_REQUEST_STATUSES = listOf("open", "in_progress", "resolved", "closed")

private fun serviceStatusLabel(status: String): String = when (status) {
  "open" -> "Open"
  "in_progress" -> "In Progress"
  "resolved" -> "Resolved"
  "closed" -> "Closed"
  else -> status
}

private fun requestTypeLabel(item: InstallationItem): String = when {
  item.wantsDemo && item.wantsInstallation -> "Demo + Installation"
  item.wantsDemo -> "Demo"
  item.wantsInstallation -> "Installation"
  else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationsScreen(modifier: Modifier = Modifier) {
  var items by remember { mutableStateOf<List<InstallationItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf<String?>(null) }
  var selectedItem by remember { mutableStateOf<InstallationItem?>(null) }
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

  fun updateStatus(item: InstallationItem, newStatus: String) {
    scope.launch {
      try {
        NetworkModule.installationsApi.updateStatus(InstallationStatusRequest(item.id, newStatus))
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
      onStatusSelected = { newStatus -> updateStatus(item, newStatus) },
      modifier = modifier
    )
    return
  }

  Scaffold(
    topBar = { TopAppBar(title = { Text("Installations") }) },
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
          SERVICE_REQUEST_STATUSES.forEach { status ->
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
              onStatusSelected = { newStatus -> updateStatus(installation, newStatus) },
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
  onStatusSelected: (String) -> Unit,
  onCall: () -> Unit
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }

  Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick)) {
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
        AssistChip(
          onClick = { statusMenuExpanded = true },
          label = { Text(serviceStatusLabel(item.status)) },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = statusMenuExpanded, onDismissRequest = { statusMenuExpanded = false }) {
          SERVICE_REQUEST_STATUSES.forEach { status ->
            DropdownMenuItem(
              text = { Text(serviceStatusLabel(status)) },
              onClick = {
                statusMenuExpanded = false
                if (status != item.status) onStatusSelected(status)
              }
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallationDetailScreen(
  item: InstallationItem,
  onBack: () -> Unit,
  onStatusSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }

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

      Spacer(modifier = Modifier.height(16.dp))
      Box {
        AssistChip(
          onClick = { statusMenuExpanded = true },
          label = { Text(serviceStatusLabel(item.status)) },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = statusMenuExpanded, onDismissRequest = { statusMenuExpanded = false }) {
          SERVICE_REQUEST_STATUSES.forEach { status ->
            DropdownMenuItem(
              text = { Text(serviceStatusLabel(status)) },
              onClick = {
                statusMenuExpanded = false
                if (status != item.status) onStatusSelected(status)
              }
            )
          }
        }
      }
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

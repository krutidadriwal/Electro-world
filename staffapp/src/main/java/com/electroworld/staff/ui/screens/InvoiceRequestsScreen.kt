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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import com.electroworld.staff.data.network.InvoiceRequestItem
import com.electroworld.staff.data.network.InvoiceRequestStatusRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun invoiceStatusLabel(status: String): String = when (status) {
  "open" -> "Open"
  "closed" -> "Closed"
  else -> status
}

// Admin-only tab (gated in HomeScreen). Unlike Complaints/Installations,
// "Closed" isn't a status that lingers to filter on -- picking it deletes
// the request server-side (see invoiceRequestsStatus in
// server/api/staff/[...path].js) and pushes the customer a heads-up that
// their invoice is ready, so there's no status filter here: every row shown
// is, by construction, still open.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceRequestsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  var items by remember { mutableStateOf<List<InvoiceRequestItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var isRefreshing by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  val filteredItems = items.filter { item ->
    searchQuery.isBlank() ||
      item.customerName.contains(searchQuery, ignoreCase = true) ||
      item.customerPhone.contains(searchQuery, ignoreCase = true) ||
      item.description.contains(searchQuery, ignoreCase = true)
  }

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      items = NetworkModule.invoiceRequestsApi.list().items
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load invoice requests."
    } finally {
      isLoading = false
      isRefreshing = false
    }
  }

  fun updateStatus(item: InvoiceRequestItem, newStatus: String) {
    scope.launch {
      try {
        NetworkModule.invoiceRequestsApi.updateStatus(InvoiceRequestStatusRequest(item.id, newStatus))
        refreshTrigger++
      } catch (e: Exception) {
        errorMessage = "Failed to update status."
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Invoice Requests") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        label = { Text("Search customer or description") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      )

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
      } else StaffPullToRefresh(isRefreshing = isRefreshing, onRefresh = { isRefreshing = true; refreshTrigger++ }) {
        if (filteredItems.isEmpty()) {
          PullableMessage {
            Text(
              if (items.isEmpty()) "No invoice requests yet." else "No matches found.",
              color = MaterialTheme.colorScheme.outline
            )
          }
        } else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
          items(filteredItems, key = { it.id }) { item ->
            InvoiceRequestRow(
              item = item,
              onStatusSelected = { newStatus -> updateStatus(item, newStatus) },
              onCall = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.customerPhone}")))
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun InvoiceRequestRow(
  item: InvoiceRequestItem,
  onStatusSelected: (String) -> Unit,
  onCall: () -> Unit
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }
  var confirmingClose by remember { mutableStateOf(false) }

  if (confirmingClose) {
    AlertDialog(
      onDismissRequest = { confirmingClose = false },
      title = { Text("Close this request?") },
      text = {
        Text(
          "This will remove the request and notify ${item.customerName} that their invoice has been uploaded."
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmingClose = false
            onStatusSelected("closed")
          }
        ) { Text("Close & notify") }
      },
      dismissButton = {
        TextButton(onClick = { confirmingClose = false }) { Text("Cancel") }
      }
    )
  }

  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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
        item.description,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )
      Text(
        "Requested: ${formatIst(item.createdAt) ?: item.createdAt}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 4.dp)
      )

      Box(modifier = Modifier.padding(top = 8.dp)) {
        AssistChip(
          onClick = { statusMenuExpanded = true },
          label = { Text(invoiceStatusLabel(item.status)) },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = statusMenuExpanded, onDismissRequest = { statusMenuExpanded = false }) {
          DropdownMenuItem(
            text = { Text("Open") },
            onClick = {
              statusMenuExpanded = false
              if (item.status != "open") onStatusSelected("open")
            }
          )
          DropdownMenuItem(
            text = { Text("Closed") },
            onClick = {
              statusMenuExpanded = false
              confirmingClose = true
            }
          )
        }
      }
    }
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

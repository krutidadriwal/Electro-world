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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.InvoiceRequestItem
import com.electroworld.staff.data.network.NetworkModule

// Not a persisted notification log -- these counts are always the current
// pending backlog (e.g. "4 new wishlist requests" means 4 not-yet-contacted
// wishlist items exist right now), recomputed from the same list endpoints
// the Wishlist/Complaints/Installations tabs use. The push notification that
// pops up on a new request carries the same live count, computed server-side
// at send time (see server/lib/staffNotify.js and its callers).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(isAdmin: Boolean, onNavigate: (target: String) -> Unit, modifier: Modifier = Modifier) {
  var wishlistCount by remember { mutableStateOf(0) }
  var complaintsCount by remember { mutableStateOf(0) }
  var installationsCount by remember { mutableStateOf(0) }
  var invoiceItems by remember { mutableStateOf<List<InvoiceRequestItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showInvoiceRequests by remember { mutableStateOf(false) }

  val invoiceCount = invoiceItems.count { it.status == "open" }

  LaunchedEffect(Unit) {
    isLoading = true
    try {
      wishlistCount = NetworkModule.wishlistApi.list().items.count { it.status == "not_contacted" }
      complaintsCount = NetworkModule.complaintsApi.list().items.count { it.status == "open" }
      installationsCount = NetworkModule.installationsApi.list().items.count { it.status == "open" }
      if (isAdmin) {
        invoiceItems = NetworkModule.invoiceRequestsApi.list().items
      }
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load notifications."
    } finally {
      isLoading = false
    }
  }

  if (showInvoiceRequests) {
    BackHandler { showInvoiceRequests = false }
    InvoiceRequestsScreen(items = invoiceItems, onBack = { showInvoiceRequests = false }, modifier = modifier)
    return
  }

  Scaffold(
    topBar = { TopAppBar(title = { Text("Notifications") }) },
    modifier = modifier
  ) { padding ->
    if (isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      val hasAnyAlert = wishlistCount > 0 || complaintsCount > 0 || installationsCount > 0 || (isAdmin && invoiceCount > 0)
      Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        errorMessage?.let {
          Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (!hasAnyAlert) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You're all caught up.", color = MaterialTheme.colorScheme.outline)
          }
        } else {
          if (wishlistCount > 0) {
            AlertRow(
              title = "Wishlist",
              message = "$wishlistCount new wishlist request${if (wishlistCount == 1) "" else "s"}",
              onClick = { onNavigate("wishlist:not_contacted") }
            )
          }
          if (complaintsCount > 0) {
            AlertRow(
              title = "Complaints",
              message = "$complaintsCount open complaint${if (complaintsCount == 1) "" else "s"}",
              onClick = { onNavigate("complaints:open") }
            )
          }
          if (installationsCount > 0) {
            AlertRow(
              title = "Installations",
              message = "$installationsCount open installation request${if (installationsCount == 1) "" else "s"}",
              onClick = { onNavigate("installations:open") }
            )
          }
          if (isAdmin && invoiceCount > 0) {
            AlertRow(
              title = "Invoice requests",
              message = "$invoiceCount pending invoice request${if (invoiceCount == 1) "" else "s"}",
              onClick = { showInvoiceRequests = true }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AlertRow(title: String, message: String, onClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.NotificationsActive,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(end = 12.dp)
        )
        Column {
          Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
          Text(message, style = MaterialTheme.typography.bodyLarge)
        }
      }
      Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
  }
}

// Admin-only, read-only review list -- no status-change action exists for
// invoice requests yet (out of scope: only a "new request" admin alert was
// asked for), so this just lets the admin see and call about them.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceRequestsScreen(items: List<InvoiceRequestItem>, onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Invoice requests") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    if (items.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("No invoice requests yet.", color = MaterialTheme.colorScheme.outline)
      }
    } else {
      LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp)) {
        items(items, key = { it.id }) { item ->
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
                IconButton(onClick = {
                  context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.customerPhone}")))
                }) {
                  Icon(Icons.Default.Call, contentDescription = "Call ${item.customerName}")
                }
              }
              Text(item.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
              Text(
                if (item.status == "open") "Open" else item.status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

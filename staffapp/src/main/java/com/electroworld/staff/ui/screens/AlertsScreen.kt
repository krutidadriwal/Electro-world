package com.electroworld.staff.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.UnreadableInvoiceItem
import com.electroworld.staff.data.network.UnreadableInvoiceRetryRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch

// Not a persisted notification log -- these counts are always the current
// pending backlog (e.g. "4 new wishlist requests" means 4 not-yet-contacted
// wishlist items exist right now), recomputed from the same list endpoints
// the Wishlist/Complaints/Installations tabs use. The push notification that
// pops up on a new request carries the same live count, computed server-side
// at send time (see server/lib/staffNotify.js and its callers).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(isAdmin: Boolean, onNavigate: (target: String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
  var wishlistCount by remember { mutableStateOf(0) }
  var complaintsCount by remember { mutableStateOf(0) }
  var installationsCount by remember { mutableStateOf(0) }
  var invoiceCount by remember { mutableStateOf(0) }
  var unreadableInvoices by remember { mutableStateOf<List<UnreadableInvoiceItem>>(emptyList()) }
  var unreadableMessage by remember { mutableStateOf<String?>(null) }
  var retryingFileId by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    isLoading = true
    try {
      wishlistCount = NetworkModule.wishlistApi.list().items.count { it.status == "not_contacted" }
      complaintsCount = NetworkModule.complaintsApi.list().items.count { it.status == "open" }
      installationsCount = NetworkModule.installationsApi.list().items.count { it.status == "open" }
      if (isAdmin) {
        invoiceCount = NetworkModule.invoiceRequestsApi.list().items.count { it.status == "open" }
      }
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load notifications."
    } finally {
      isLoading = false
    }
  }

  // Loaded on its own, after the counts above: this section is the one part
  // of the tab that's an actual list rather than a live count, and a failure
  // here shouldn't take the request alerts down with it.
  LaunchedEffect(Unit) {
    if (!isAdmin) return@LaunchedEffect
    try {
      unreadableInvoices = NetworkModule.unreadableInvoicesApi.list().items
    } catch (e: Exception) {
      unreadableMessage = "Failed to load invoices without points."
    }
  }

  fun retryUnreadable(item: UnreadableInvoiceItem) {
    scope.launch {
      retryingFileId = item.fileId
      try {
        val result = NetworkModule.unreadableInvoicesApi.retry(UnreadableInvoiceRetryRequest(item.fileId))
        val invoiceNumber = item.invoiceName.removeSuffix(".pdf")
        unreadableMessage = when (result.outcome) {
          "credited" -> "$invoiceNumber read successfully. ${result.points} points added for ${item.customerName}."
          "unreadable" -> "$invoiceNumber still can't be read. Replace the PDF in the customer's folder."
          else -> "$invoiceNumber cleared. It will be checked again if it's still in the customer's folder."
        }
        unreadableInvoices = NetworkModule.unreadableInvoicesApi.list().items
      } catch (e: Exception) {
        unreadableMessage = "Failed to check the invoice again."
      } finally {
        retryingFileId = null
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notifications") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    if (isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      val hasRequestAlert = wishlistCount > 0 || complaintsCount > 0 || installationsCount > 0 || (isAdmin && invoiceCount > 0)
      val showUnreadableSection = isAdmin && (unreadableInvoices.isNotEmpty() || unreadableMessage != null)
      Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        errorMessage?.let {
          Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        if (!hasRequestAlert && !showUnreadableSection) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You're all caught up.", color = MaterialTheme.colorScheme.outline)
          }
        } else Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
              onClick = { onNavigate("invoice_requests:open") }
            )
          }

          if (showUnreadableSection) {
            Text(
              text = "INVOICES WITHOUT POINTS",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.sp,
              modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            if (unreadableInvoices.isNotEmpty()) {
              Text(
                "The total on these invoices couldn't be read, so no points were given.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 8.dp)
              )
            }
            unreadableMessage?.let {
              Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
            }
            unreadableInvoices.forEach { item ->
              UnreadableInvoiceRow(
                item = item,
                isRetrying = retryingFileId == item.fileId,
                retryEnabled = retryingFileId == null,
                onRetry = { retryUnreadable(item) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AlertRow(title: String, message: String, onClick: () -> Unit) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick)) {
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

@Composable
private fun UnreadableInvoiceRow(item: UnreadableInvoiceItem, isRetrying: Boolean, retryEnabled: Boolean, onRetry: () -> Unit) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(item.invoiceName.removeSuffix(".pdf"), style = MaterialTheme.typography.bodyLarge)
        Text(
          "${item.customerName} · ${item.customerPhone}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline
        )
        Text(
          "Found ${item.processedAt.substringBefore('T')}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }
      if (isRetrying) {
        CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
      } else {
        TextButton(onClick = onRetry, enabled = retryEnabled) { Text("Check again") }
      }
    }
  }
}

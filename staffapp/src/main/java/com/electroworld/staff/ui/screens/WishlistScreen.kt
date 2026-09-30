package com.electroworld.staff.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.WishlistItem
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
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

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
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
      } else if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No wishlist entries yet.", color = MaterialTheme.colorScheme.outline)
        }
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
          items(items, key = { it.id }) { wishlistItem ->
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

@Composable
private fun WishlistRow(
  item: WishlistItem,
  onStatusSelected: (String) -> Unit,
  onCall: () -> Unit
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }

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
                if (status != item.status) onStatusSelected(status)
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

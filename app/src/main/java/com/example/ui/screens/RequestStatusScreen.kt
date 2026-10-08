package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.Complaint
import com.example.data.network.Installation
import com.example.data.network.NetworkModule
import com.example.ui.theme.appCardColors

/**
 * One row in a request-status list, already flattened from whichever
 * request type (complaint or installation) is being shown.
 */
data class RequestStatusItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val address: String,
  val status: String,
  val createdAt: String,
  // Only ever populated for assigned_to_service_center ("Service center
  // no. X") and assigned_to_technician ("Technician: Name - Mobile") --
  // blocked/cancelled reasons are staff-internal and deliberately never
  // surfaced here.
  val assignedInfo: String? = null
)

// primary/secondary/outline rather than green/amber/gray -- the 8-swatch
// palette has no green, so "resolved" reads as the most saturated/positive
// swatch available (primary) instead.
@Composable
private fun statusColor(status: String): Color = when (status) {
  "resolved", "closed" -> MaterialTheme.colorScheme.primary
  "in_progress" -> MaterialTheme.colorScheme.secondary
  else -> MaterialTheme.colorScheme.outline
}

private fun statusLabel(status: String) = status.replace('_', ' ').replaceFirstChar { it.uppercase() }

// Server timestamps are full ISO-8601 (e.g. "2024-05-01T12:34:56.789Z");
// this list only needs the date, as DD-MM-YYYY -- same convention as
// DashboardScreen's formatInvoiceDate (duplicated here since that one is
// file-private).
private fun formatRequestDate(isoTimestamp: String): String {
  val datePart = isoTimestamp.substringBefore('T')
  val parts = datePart.split("-")
  return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else isoTimestamp
}

/**
 * Full-screen list of a user's past requests of one kind (complaints or
 * installations), showing their current status. Shared by both
 * RegisterComplaintScreen and InstallationRequestScreen's history icon.
 */
@Composable
fun RequestStatusScreen(
  title: String,
  emptyMessage: String,
  onBack: () -> Unit,
  loadItems: suspend () -> List<RequestStatusItem>,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  var statusItems by remember { mutableStateOf<List<RequestStatusItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    isLoading = true
    errorMessage = null
    try {
      statusItems = loadItems()
    } catch (e: Exception) {
      errorMessage = "Unable to load your requests. Please check your connection and try again."
    } finally {
      isLoading = false
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(top = 40.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    }
  ) { paddingValues ->
    when {
      isLoading -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }
      errorMessage != null -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))
          Text(errorMessage ?: "", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
      }
      statusItems.isEmpty() -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(Icons.Default.Info, "None yet", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))
          Text(emptyMessage, color = MaterialTheme.colorScheme.outline, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
      }
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(statusItems) { item ->
            Card(
              colors = appCardColors(),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor(item.status).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp)
                  ) {
                    Text(
                      text = statusLabel(item.status),
                      color = statusColor(item.status),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(item.subtitle, color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(3.dp))
                Text(item.address, color = MaterialTheme.colorScheme.outline, fontSize = 10.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(formatRequestDate(item.createdAt), color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                  // Staff-assigned info -- absent until staff sets the
                  // status to assigned_to_service_center/assigned_to_technician,
                  // so this section is simply skipped until then.
                  item.assignedInfo?.let { info ->
                    Text(
                      text = info,
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ComplaintStatusScreen(userPhone: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
  RequestStatusScreen(
    title = "My Complaints",
    emptyMessage = "You haven't registered any complaints yet.",
    onBack = onBack,
    loadItems = {
      NetworkModule.userApi.getComplaints(userPhone).complaints.map { it.toStatusItem() }
    },
    modifier = modifier
  )
}

@Composable
fun InstallationStatusScreen(userPhone: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
  RequestStatusScreen(
    title = "My Installation Requests",
    emptyMessage = "You haven't requested any installations or demos yet.",
    onBack = onBack,
    loadItems = {
      NetworkModule.userApi.getInstallations(userPhone).installations.map { it.toStatusItem() }
    },
    modifier = modifier
  )
}

// Mirrors the staff app's status values (see ServiceRequestStatus.kt) --
// only these two ever have customer-facing info to show; blocked/cancelled
// reasons are staff-internal.
private fun assignedInfoFor(status: String, serviceCenterNo: String?, technicianName: String?, technicianMobile: String?): String? =
  when (status) {
    "assigned_to_service_center" -> serviceCenterNo?.takeIf { it.isNotBlank() }?.let { "Service center no. $it" }
    "assigned_to_technician" -> listOfNotNull(
      technicianName?.takeIf { it.isNotBlank() },
      technicianMobile?.takeIf { it.isNotBlank() }
    ).takeIf { it.isNotEmpty() }?.joinToString(" - ")?.let { "Technician: $it" }
    else -> null
  }

private fun Complaint.toStatusItem() = RequestStatusItem(
  id = id,
  title = categoryName + (subcategoryName?.let { " - $it" } ?: ""),
  subtitle = description,
  address = address,
  status = status,
  createdAt = createdAt,
  assignedInfo = assignedInfoFor(status, complaintNumber, technicianName, technicianMobile)
)

private fun Installation.toStatusItem() = RequestStatusItem(
  id = id,
  title = "$itemName ($categoryName)",
  subtitle = listOfNotNull(
    if (wantsDemo) "Demo" else null,
    if (wantsInstallation) "Installation" else null
  ).joinToString(" + "),
  address = address,
  status = status,
  createdAt = createdAt,
  assignedInfo = assignedInfoFor(status, installationNumber, technicianName, technicianMobile)
)

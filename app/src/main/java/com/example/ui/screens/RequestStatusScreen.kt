package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.Complaint
import com.example.data.network.Installation
import com.example.data.network.NetworkModule
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SuccessGreen

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
  val createdAt: String
)

private fun statusColor(status: String) = when (status) {
  "resolved", "closed" -> SuccessGreen
  "in_progress" -> GoldPrimary
  else -> GoldSecondary
}

private fun statusLabel(status: String) = status.replace('_', ' ').replaceFirstChar { it.uppercase() }

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
    containerColor = SlateBackground,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SlateSurface)
          .padding(top = 40.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, color = OnSlateText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
          CircularProgressIndicator(color = GoldPrimary)
        }
      }
      errorMessage != null -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(Icons.Default.Warning, "Error", tint = GoldSecondary, modifier = Modifier.padding(bottom = 8.dp))
          Text(errorMessage ?: "", color = OnSlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
      }
      statusItems.isEmpty() -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(Icons.Default.Info, "None yet", tint = GoldSecondary, modifier = Modifier.padding(bottom = 8.dp))
          Text(emptyMessage, color = OnSlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
      }
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(statusItems) { item ->
            Card(
              colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(item.title, color = OnSlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                  Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor(item.status).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp)
                  ) {
                    Text(
                      text = statusLabel(item.status),
                      color = statusColor(item.status),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.subtitle, color = OnSlateTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.address, color = OnSlateTextSecondary, fontSize = 11.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.createdAt, color = OnSlateTextSecondary, fontSize = 10.sp)
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

private fun Complaint.toStatusItem() = RequestStatusItem(
  id = id,
  title = categoryName + (subcategoryName?.let { " - $it" } ?: ""),
  subtitle = description,
  address = address,
  status = status,
  createdAt = createdAt
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
  createdAt = createdAt
)

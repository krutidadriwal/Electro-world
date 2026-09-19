package com.electroworld.staff.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.CreateNotificationRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.NotificationItem
import com.electroworld.staff.data.network.UploadNotificationImageRequest
import kotlinx.coroutines.launch

private val DURATION_PRESETS = listOf(
  "1 day" to 24 * 60,
  "3 days" to 3 * 24 * 60,
  "7 days" to 7 * 24 * 60,
  "30 days" to 30 * 24 * 60
)

@Composable
fun NotificationsScreen(isAdmin: Boolean, modifier: Modifier = Modifier) {
  var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var showCompose by remember { mutableStateOf(false) }
  var refreshTrigger by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      notifications = NetworkModule.notificationsApi.list().notifications
    } catch (e: Exception) {
      // Leave the previous list showing; a manual pull-to-refresh isn't
      // implemented yet for this first pass.
    } finally {
      isLoading = false
    }
  }

  if (showCompose) {
    ComposeNotificationScreen(
      onSent = {
        showCompose = false
        refreshTrigger++
      },
      onCancel = { showCompose = false }
    )
    return
  }

  Scaffold(
    floatingActionButton = {
      if (isAdmin) {
        FloatingActionButton(onClick = { showCompose = true }) {
          Icon(Icons.Default.Add, contentDescription = "New notification")
        }
      }
    },
    modifier = modifier
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      if (isLoading && notifications.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
      } else if (notifications.isEmpty()) {
        Text("No notifications sent yet.", modifier = Modifier.padding(24.dp))
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
          items(notifications) { notification ->
            NotificationCard(notification)
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationCard(notification: NotificationItem) {
  Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(notification.message, style = MaterialTheme.typography.bodyLarge)
      Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (notification.imageUrl != null) {
          Icon(Icons.Default.Image, contentDescription = "Has image")
        }
        Text(
          text = if (notification.expired) "Expired" else "Active",
          style = MaterialTheme.typography.labelMedium,
          color = if (notification.expired) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
        )
      }
      Text(notification.createdAt, style = MaterialTheme.typography.labelSmall)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeNotificationScreen(onSent: () -> Unit, onCancel: () -> Unit) {
  val context = LocalContext.current
  var message by remember { mutableStateOf("") }
  var selectedDurationMinutes by remember { mutableStateOf(DURATION_PRESETS.first().second) }
  var imageUri by remember { mutableStateOf<Uri?>(null) }
  var isSending by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  val pickImageLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri -> imageUri = uri }

  Scaffold(
    topBar = { TopAppBar(title = { Text("New notification") }) }
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
      OutlinedTextField(
        value = message,
        onValueChange = { message = it; errorMessage = null },
        label = { Text("Message") },
        modifier = Modifier.fillMaxWidth()
      )

      Text(
        "Show for",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DURATION_PRESETS.forEach { (label, minutes) ->
          FilterChip(
            selected = selectedDurationMinutes == minutes,
            onClick = { selectedDurationMinutes = minutes },
            label = { Text(label) }
          )
        }
      }

      OutlinedButton(
        onClick = { pickImageLauncher.launch("image/*") },
        modifier = Modifier.padding(top = 16.dp)
      ) {
        Icon(Icons.Default.Image, contentDescription = null)
        Text(if (imageUri == null) " Add image (optional)" else " Change image")
      }

      errorMessage?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
      }

      Row(modifier = Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, enabled = !isSending) { Text("Cancel") }
        Button(
          enabled = !isSending && message.isNotBlank(),
          onClick = {
            errorMessage = null
            isSending = true
            scope.launch {
              try {
                var imageDriveFileId: String? = null
                var uploadedImageUrl: String? = null
                val uri = imageUri
                if (uri != null) {
                  val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                  if (bytes != null) {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val uploadResponse = NetworkModule.notificationsApi.uploadImage(
                      UploadNotificationImageRequest(
                        fileName = "notification-${System.currentTimeMillis()}",
                        mimeType = mimeType,
                        base64Data = base64
                      )
                    )
                    imageDriveFileId = uploadResponse.driveFileId
                    uploadedImageUrl = uploadResponse.url
                  }
                }
                NetworkModule.notificationsApi.create(
                  CreateNotificationRequest(
                    message = message.trim(),
                    durationMinutes = selectedDurationMinutes,
                    imageDriveFileId = imageDriveFileId,
                    imageUrl = uploadedImageUrl
                  )
                )
                onSent()
              } catch (e: Exception) {
                errorMessage = "Failed to send notification. Please try again."
              } finally {
                isSending = false
              }
            }
          }
        ) {
          if (isSending) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Send")
        }
      }
    }
  }
}

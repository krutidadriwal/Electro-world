package com.electroworld.staff.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.electroworld.staff.data.network.CreateNotificationRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.NotificationItem
import com.electroworld.staff.data.network.UploadNotificationImageRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val DURATION_PRESETS = listOf(
  "1 day" to 24 * 60,
  "3 days" to 3 * 24 * 60,
  "7 days" to 7 * 24 * 60,
  "30 days" to 30 * 24 * 60
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(isAdmin: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
  var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var showCompose by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf<NotificationItem?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var isRefreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      notifications = NetworkModule.notificationsApi.list().notifications
    } catch (e: Exception) {
      // Leave the previous list showing; pulling down retries.
    } finally {
      isLoading = false
      isRefreshing = false
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

  selected?.let { notification ->
    BackHandler { selected = null }
    NotificationDetailScreen(notification, onBack = { selected = null }, modifier = modifier)
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Broadcast") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
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
      if (isLoading && notifications.isEmpty() && !isRefreshing) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
      } else StaffPullToRefresh(isRefreshing = isRefreshing, onRefresh = { isRefreshing = true; refreshTrigger++ }) {
        if (notifications.isEmpty()) {
          PullableMessage {
            Text("No notifications sent yet.")
          }
        } else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
          items(notifications) { notification ->
            NotificationCard(notification, onClick = { selected = notification })
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationCard(notification: NotificationItem, onClick: () -> Unit) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick)) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(notification.message, style = MaterialTheme.typography.bodyLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
      Text(formatIst(notification.createdAt) ?: notification.createdAt, style = MaterialTheme.typography.labelSmall)
    }
  }
}

// One sent notification in full: the whole image (never cropped) and the
// complete message, as customers see it in the app.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDetailScreen(notification: NotificationItem, onBack: () -> Unit, modifier: Modifier = Modifier) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Notification") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      notification.imageUrl?.let { url ->
        FullyVisibleImage(url = url, modifier = Modifier.clip(RoundedCornerShape(12.dp)))
        Spacer(modifier = Modifier.height(16.dp))
      }
      Text(notification.message, style = MaterialTheme.typography.bodyLarge)
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = if (notification.expired) "Expired" else "Active",
        style = MaterialTheme.typography.labelLarge,
        color = if (notification.expired) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
      )
      Text(
        "Sent ${formatIst(notification.createdAt) ?: notification.createdAt} · shown for ${durationText(notification.durationMinutes)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

// The image whole, whatever its shape. AsyncImage needs an explicit aspect
// ratio before the image loads (it can collapse to nothing otherwise), so it
// starts at 16:9 and switches to the image's own proportions once loaded.
@Composable
private fun FullyVisibleImage(url: String, modifier: Modifier = Modifier) {
  var aspectRatio by remember(url) { mutableFloatStateOf(16f / 9f) }
  AsyncImage(
    model = url,
    contentDescription = null,
    onSuccess = { state ->
      val size = state.painter.intrinsicSize
      if (size.isSpecified && size.width > 0f && size.height > 0f) aspectRatio = size.width / size.height
    },
    contentScale = ContentScale.Fit,
    modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio)
  )
}

private fun durationText(minutes: Int): String = when {
  minutes % (24 * 60) == 0 -> (minutes / (24 * 60)).let { "$it day${if (it == 1) "" else "s"}" }
  minutes % 60 == 0 -> (minutes / 60).let { "$it hour${if (it == 1) "" else "s"}" }
  else -> "$minutes minutes"
}

// Postgres timestamptz values arrive as UTC ISO-8601 strings -- shown in IST,
// like the rest of the app.
private fun formatIst(isoTimestamp: String): String? = try {
  val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
  parser.parse(isoTimestamp.substringBefore('.').substringBefore('Z'))?.let { date ->
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }.format(date) + " IST"
  }
} catch (e: Exception) {
  null
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

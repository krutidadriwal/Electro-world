package com.electroworld.staff.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.electroworld.staff.R
import com.electroworld.staff.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// What a complaint / installation request's detail screen shows -- the
// screen and the share image are both built from the same fields list, so
// the image never carries anything the screen doesn't.
data class RequestShareContent(
  val badge: String,
  val title: String,
  val customerName: String,
  val customerPhone: String,
  val fields: List<Pair<String, String>>
)

// Shares a request's details as an image (drawn on the phone, like the
// price-list shares) straight to the Android share sheet. Unlike price
// shares there's no customer dialog, history or server log -- it's a
// screenshot of what's on file, sent wherever staff choose (often a
// technician). Render RequestShareHost once per screen for the progress
// dialog.
@Stable
class RequestSharer internal constructor(private val context: Context, private val scope: CoroutineScope) {
  internal var isWorking by mutableStateOf(false)

  fun share(content: RequestShareContent) {
    if (isWorking) return
    isWorking = true
    scope.launch {
      try {
        val file = createImage(context, content)
        launchShareSheet(context, file)
      } catch (e: Exception) {
        Toast.makeText(context, "Couldn't create the image. Please try again.", Toast.LENGTH_LONG).show()
      } finally {
        isWorking = false
      }
    }
  }
}

@Composable
fun rememberRequestSharer(): RequestSharer {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  return remember { RequestSharer(context, scope) }
}

@Composable
fun RequestShareHost(sharer: RequestSharer) {
  if (sharer.isWorking) PreparingImageDialog()
}

private suspend fun createImage(context: Context, content: RequestShareContent): File {
  val session = SessionManager(context)
  val sharedAt = System.currentTimeMillis()
  val logo = withContext(Dispatchers.IO) { BitmapFactory.decodeResource(context.resources, R.drawable.ew_logo) }
  val bitmap = withContext(Dispatchers.Default) {
    ShareImageRenderer.renderRequestDetails(content, session.name.ifBlank { session.email }, sharedAt, logo)
  }
  // Under shares/ (the FileProvider's folder) but apart from the price-list
  // history's images. Only the latest is kept -- nothing re-shares these.
  return withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "shares/requests").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    File(dir, "request_$sharedAt.png").also { file ->
      FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
  }
}

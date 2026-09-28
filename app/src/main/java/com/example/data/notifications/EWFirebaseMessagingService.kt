package com.example.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.SessionManager
import com.example.data.network.NetworkModule
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "electro_world_announcements"

class EWFirebaseMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val sessionManager = SessionManager(applicationContext)
    if (!sessionManager.isLoggedIn) return
    CoroutineScope(Dispatchers.IO).launch {
      registerFcmToken(NetworkModule.deviceTokenApi, sessionManager.userPhone)
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val title = message.data["title"] ?: getString(R.string.app_name)
    val body = message.data["body"] ?: return
    val trayId = message.data["notificationId"]?.let { notificationTrayId(it) } ?: Random.nextInt()
    val imageUrl = message.data["imageUrl"]

    // Data-only messages (see lib/fcm.js on the server) mean we're always
    // the ones building the notification -- including downloading its image,
    // which the OS used to do for us when this was a `notification` message.
    // FCM already delivers onMessageReceived off the main thread, so a
    // blocking download here is fine (Firebase allows ~20s of background
    // work before the OS may reclaim it).
    CoroutineScope(Dispatchers.IO).launch {
      val image = imageUrl?.let { downloadBitmap(it) }
      showNotification(applicationContext, trayId, title, body, image)
    }
  }
}

private fun downloadBitmap(url: String): Bitmap? = try {
  URL(url).openStream().use { BitmapFactory.decodeStream(it) }
} catch (e: Exception) {
  null
}

// Derived from the server-side notification's id so the tray entry can be
// looked up and cancelled later (see DashboardScreen's openNotification) --
// reading it in-app clears this specific tray notification, which is how
// OEM launchers that show a numeric home-screen badge (count of active,
// undismissed notifications -- not a stock-Android feature, but common on
// e.g. Samsung/OnePlus/Xiaomi) keep that count in sync with what's unread.
fun notificationTrayId(serverNotificationId: String): Int = serverNotificationId.hashCode()

private fun ensureChannel(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
  val manager = context.getSystemService(NotificationManager::class.java) ?: return
  if (manager.getNotificationChannel(CHANNEL_ID) != null) return
  manager.createNotificationChannel(
    NotificationChannel(CHANNEL_ID, "Announcements", NotificationManager.IMPORTANCE_DEFAULT)
  )
}

private fun showNotification(context: Context, trayId: Int, title: String, body: String, image: Bitmap?) {
  ensureChannel(context)
  val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.mipmap.ic_launcher)
    .setContentTitle(title)
    .setContentText(body)
    .setAutoCancel(true)
    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

  builder.setStyle(
    if (image != null) {
      NotificationCompat.BigPictureStyle().bigPicture(image).setSummaryText(body)
    } else {
      NotificationCompat.BigTextStyle().bigText(body)
    }
  )

  NotificationManagerCompat.from(context).notify(trayId, builder.build())
}

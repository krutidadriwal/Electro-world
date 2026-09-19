package com.example.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.SessionManager
import com.example.data.network.NetworkModule
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
    val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
    val body = message.notification?.body ?: message.data["body"] ?: return
    showNotification(applicationContext, title, body)
  }
}

private fun ensureChannel(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
  val manager = context.getSystemService(NotificationManager::class.java) ?: return
  if (manager.getNotificationChannel(CHANNEL_ID) != null) return
  manager.createNotificationChannel(
    NotificationChannel(CHANNEL_ID, "Announcements", NotificationManager.IMPORTANCE_DEFAULT)
  )
}

private fun showNotification(context: Context, title: String, body: String) {
  ensureChannel(context)
  val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.mipmap.ic_launcher)
    .setContentTitle(title)
    .setContentText(body)
    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
    .setAutoCancel(true)
    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    .build()
  NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
}

package com.electroworld.staff.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.electroworld.staff.MainActivity
import com.electroworld.staff.data.SessionManager
import com.electroworld.staff.data.network.NetworkModule
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Bumped from "staff_alerts" now that a custom sound is set -- a channel's
// sound is locked in at creation, so switching sounds requires a new id or
// Android keeps serving the old channel (with its old sound) forever.
private const val CHANNEL_ID = "staff_alerts_v2"

class StaffFirebaseMessagingService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val sessionManager = SessionManager(applicationContext)
    if (!sessionManager.isLoggedIn) return
    CoroutineScope(Dispatchers.IO).launch {
      registerStaffFcmToken(NetworkModule.staffApi)
    }
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val title = message.data["title"] ?: "EW Ops Hub"
    val body = message.data["body"] ?: return
    val target = message.data["target"]
    val trayId = target?.hashCode() ?: Random.nextInt()
    showNotification(applicationContext, trayId, title, body, target)
  }
}

private fun ensureChannel(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
  val manager = context.getSystemService(NotificationManager::class.java) ?: return
  if (manager.getNotificationChannel(CHANNEL_ID) != null) return

  // staffapp/src/main/res/raw/cha_ching.mp3
  val soundUri: Uri = Uri.parse("android.resource://${context.packageName}/raw/cha_ching")
  val audioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

  val channel = NotificationChannel(CHANNEL_ID, "Staff alerts", NotificationManager.IMPORTANCE_HIGH).apply {
    setSound(soundUri, audioAttributes)
    enableVibration(true)
  }
  manager.createNotificationChannel(channel)
}

private fun showNotification(context: Context, trayId: Int, title: String, body: String, target: String?) {
  ensureChannel(context)

  val intent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    if (target != null) putExtra(MainActivity.EXTRA_NOTIFICATION_TARGET, target)
  }
  val pendingIntent = PendingIntent.getActivity(
    context,
    trayId,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  )

  val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(android.R.drawable.ic_popup_reminder)
    .setContentTitle(title)
    .setContentText(body)
    .setAutoCancel(true)
    .setContentIntent(pendingIntent)
    .setPriority(NotificationCompat.PRIORITY_HIGH)

  NotificationManagerCompat.from(context).notify(trayId, builder.build())
}

package com.example.data.notifications

import android.util.Log
import com.example.data.network.DeviceTokenApi
import com.example.data.network.RegisterDeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "FcmTokenRegistrar"

private suspend fun currentFcmToken(): String = suspendCancellableCoroutine { continuation ->
  FirebaseMessaging.getInstance().token
    .addOnSuccessListener { token -> continuation.resume(token) }
    .addOnFailureListener { error -> continuation.resumeWithException(error) }
}

// Called after login (once a phone number is known) and from onNewToken
// (in case the token rotates while already logged in). Best-effort: a
// failure here shouldn't block login or crash the app -- the token just
// won't be registered until the next successful call.
suspend fun registerFcmToken(deviceTokenApi: DeviceTokenApi, userPhone: String) {
  if (userPhone.isBlank()) return
  try {
    val token = currentFcmToken()
    deviceTokenApi.registerToken(RegisterDeviceTokenRequest(phone = userPhone, fcmToken = token))
  } catch (e: Exception) {
    Log.w(TAG, "Failed to register FCM token", e)
  }
}

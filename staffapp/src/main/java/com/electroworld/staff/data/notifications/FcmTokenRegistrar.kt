package com.electroworld.staff.data.notifications

import android.util.Log
import com.electroworld.staff.data.network.RegisterStaffDeviceTokenRequest
import com.electroworld.staff.data.network.StaffApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "StaffFcmTokenRegistrar"

private suspend fun currentFcmToken(): String = suspendCancellableCoroutine { continuation ->
  FirebaseMessaging.getInstance().token
    .addOnSuccessListener { token -> continuation.resume(token) }
    .addOnFailureListener { error -> continuation.resumeWithException(error) }
}

// Called after login and from onNewToken (in case the token rotates while
// already logged in). Best-effort: a failure here shouldn't block login or
// crash the app -- the token just won't be registered until the next
// successful call.
suspend fun registerStaffFcmToken(staffApi: StaffApi) {
  try {
    val token = currentFcmToken()
    staffApi.registerDeviceToken(RegisterStaffDeviceTokenRequest(fcmToken = token))
  } catch (e: Exception) {
    Log.w(TAG, "Failed to register FCM token", e)
  }
}

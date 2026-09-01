package com.example.data.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Thin wrapper around Firebase Phone Auth for sending/verifying OTPs. */
class PhoneAuthManager {
  private val auth = FirebaseAuth.getInstance()
  private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

  sealed class OtpEvent {
    data class CodeSent(val verificationId: String) : OtpEvent()
    data class AutoVerified(val idToken: String) : OtpEvent()
    data class Failed(val message: String) : OtpEvent()
  }

  // Call again with the same phone number to resend -- the previously captured
  // resend token is applied automatically once onCodeSent has fired once.
  fun sendOtp(phoneE164: String, activity: Activity, onEvent: (OtpEvent) -> Unit) {
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        signInWithCredentialAsync(credential, onEvent) { OtpEvent.AutoVerified(it) }
      }

      override fun onVerificationFailed(e: FirebaseException) {
        onEvent(OtpEvent.Failed(e.message ?: "Unable to send verification code"))
      }

      override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
        resendToken = token
        onEvent(OtpEvent.CodeSent(id))
      }
    }

    val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
      .setPhoneNumber(phoneE164)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setActivity(activity)
      .setCallbacks(callbacks)
    resendToken?.let { optionsBuilder.setForceResendingToken(it) }

    PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
  }

  suspend fun verifyOtp(verificationId: String, code: String): String =
    signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code))

  private fun signInWithCredentialAsync(
    credential: PhoneAuthCredential,
    onEvent: (OtpEvent) -> Unit,
    onSuccess: (String) -> OtpEvent
  ) {
    auth.signInWithCredential(credential).addOnCompleteListener { task ->
      val user = task.result?.user
      if (!task.isSuccessful || user == null) {
        onEvent(OtpEvent.Failed(task.exception?.message ?: "Verification failed"))
        return@addOnCompleteListener
      }
      user.getIdToken(true).addOnCompleteListener { tokenTask ->
        val token = tokenTask.result?.token
        if (tokenTask.isSuccessful && token != null) {
          onEvent(onSuccess(token))
        } else {
          onEvent(OtpEvent.Failed(tokenTask.exception?.message ?: "Failed to obtain ID token"))
        }
      }
    }
  }

  private suspend fun signInWithCredential(credential: PhoneAuthCredential): String =
    suspendCancellableCoroutine { continuation ->
      auth.signInWithCredential(credential).addOnCompleteListener { task ->
        val user = task.result?.user
        if (!task.isSuccessful || user == null) {
          continuation.resumeWithException(task.exception ?: Exception("Verification failed"))
          return@addOnCompleteListener
        }
        user.getIdToken(true).addOnCompleteListener { tokenTask ->
          val token = tokenTask.result?.token
          if (tokenTask.isSuccessful && token != null) {
            continuation.resume(token)
          } else {
            continuation.resumeWithException(tokenTask.exception ?: Exception("Failed to obtain ID token"))
          }
        }
      }
    }
}

package com.electroworld.staff

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.electroworld.staff.data.SessionManager
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.notifications.registerStaffFcmToken
import com.electroworld.staff.ui.screens.HomeScreen
import com.electroworld.staff.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {
  // Held outside Compose state-hoisting (set in onCreate/onNewIntent, read
  // inside setContent) so a tapped push notification's target survives both
  // a cold start and -- via singleTop + onNewIntent, see the manifest --
  // being re-delivered to an already-running instance.
  private val notificationTarget = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    NetworkModule.init(applicationContext)
    notificationTarget.value = intent?.getStringExtra(EXTRA_NOTIFICATION_TARGET)
    enableEdgeToEdge()
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val context = LocalContext.current
          val sessionManager = remember { SessionManager(context) }
          var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn) }
          var role by remember { mutableStateOf(sessionManager.role) }
          var email by remember { mutableStateOf(sessionManager.email) }
          var isResolvingProfile by remember { mutableStateOf(false) }

          // Android 13+ (API 33) requires this permission to actually be
          // granted at runtime -- declaring it in the manifest alone does
          // nothing; without this, every push the server sends is silently
          // enqueued-then-blocked by the OS (confirmed via `adb shell dumpsys
          // notification`: numBlocked rising while numPostedByApp stayed 0),
          // with no error anywhere, including inside onMessageReceived.
          val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
          ) { /* no-op either way -- the in-app Notifications tab still works without tray pushes */ }

          LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
              ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }

          LaunchedEffect(isLoggedIn) {
            if (isLoggedIn && role.isBlank()) {
              isResolvingProfile = true
              try {
                val me = NetworkModule.staffApi.me()
                sessionManager.saveProfile(me.id, me.email, me.role)
                role = me.role
                email = me.email
              } catch (e: Exception) {
                sessionManager.clearSession()
                isLoggedIn = false
              } finally {
                isResolvingProfile = false
              }
            }
            if (isLoggedIn) {
              registerStaffFcmToken(NetworkModule.staffApi)
            }
          }

          if (isLoggedIn && !isResolvingProfile) {
            HomeScreen(
              isAdmin = role == "admin",
              email = email,
              role = role,
              notificationTarget = notificationTarget.value,
              onNotificationTargetConsumed = { notificationTarget.value = null },
              onLogout = {
                sessionManager.clearSession()
                isLoggedIn = false
                role = ""
                email = ""
              }
            )
          } else if (!isLoggedIn) {
            LoginScreen(
              onLoginSuccess = { accessToken, refreshToken ->
                sessionManager.saveTokens(accessToken, refreshToken)
                isLoggedIn = true
              }
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    notificationTarget.value = intent.getStringExtra(EXTRA_NOTIFICATION_TARGET)
  }

  companion object {
    const val EXTRA_NOTIFICATION_TARGET = "notification_target"
  }
}

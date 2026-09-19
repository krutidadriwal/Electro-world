package com.electroworld.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.electroworld.staff.data.SessionManager
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.ui.screens.HomeScreen
import com.electroworld.staff.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    NetworkModule.init(applicationContext)
    enableEdgeToEdge()
    setContent {
      MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val context = LocalContext.current
          val sessionManager = remember { SessionManager(context) }
          var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn) }
          var role by remember { mutableStateOf(sessionManager.role) }
          var isResolvingProfile by remember { mutableStateOf(false) }

          LaunchedEffect(isLoggedIn) {
            if (isLoggedIn && role.isBlank()) {
              isResolvingProfile = true
              try {
                val me = NetworkModule.staffApi.me()
                sessionManager.saveProfile(me.id, me.email, me.role)
                role = me.role
              } catch (e: Exception) {
                sessionManager.clearSession()
                isLoggedIn = false
              } finally {
                isResolvingProfile = false
              }
            }
          }

          if (isLoggedIn && !isResolvingProfile) {
            HomeScreen(isAdmin = role == "admin")
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
}

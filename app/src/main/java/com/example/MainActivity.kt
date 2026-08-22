package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.SessionManager
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
  LOGIN,
  DASHBOARD,
  WISHLIST
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }
        var currentScreen by remember {
          mutableStateOf(if (sessionManager.isLoggedIn) Screen.DASHBOARD else Screen.LOGIN)
        }
        var userName by remember { mutableStateOf(sessionManager.userName) }
        var userPhone by remember { mutableStateOf(sessionManager.userPhone) }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
          when (currentScreen) {
            Screen.LOGIN -> {
              LoginScreen(
                onLoginSuccess = { name, phone ->
                  sessionManager.saveSession(name, phone)
                  userName = name
                  userPhone = phone
                  currentScreen = Screen.DASHBOARD
                },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.DASHBOARD -> {
              DashboardScreen(
                userName = userName,
                userPhone = userPhone,
                onSignOut = {
                  sessionManager.clearSession()
                  currentScreen = Screen.LOGIN
                },
                onOpenWishlist = { currentScreen = Screen.WISHLIST },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.WISHLIST -> {
              WishlistScreen(
                onBack = { currentScreen = Screen.DASHBOARD },
                modifier = Modifier.padding(innerPadding)
              )
            }
          }
        }
      }
    }
  }
}


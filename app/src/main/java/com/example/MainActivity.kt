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
import com.example.ui.screens.ComplaintStatusScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InstallationRequestScreen
import com.example.ui.screens.InstallationStatusScreen
import com.example.ui.screens.MyInvoicesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.RegisterComplaintScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
  LOGIN,
  DASHBOARD,
  WISHLIST,
  REGISTER_COMPLAINT,
  COMPLAINT_STATUS,
  INSTALLATION_REQUEST,
  INSTALLATION_STATUS,
  MY_INVOICES
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
              OnboardingScreen(
                onLoginSuccess = { name, phone, token ->
                  sessionManager.saveSession(name, phone, token)
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
                onOpenRegisterComplaint = { currentScreen = Screen.REGISTER_COMPLAINT },
                onOpenInstallation = { currentScreen = Screen.INSTALLATION_REQUEST },
                onOpenInvoices = { currentScreen = Screen.MY_INVOICES },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.MY_INVOICES -> {
              MyInvoicesScreen(
                userPhone = userPhone,
                onBack = { currentScreen = Screen.DASHBOARD },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.WISHLIST -> {
              WishlistScreen(
                userPhone = userPhone,
                onBack = { currentScreen = Screen.DASHBOARD },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.REGISTER_COMPLAINT -> {
              RegisterComplaintScreen(
                userPhone = userPhone,
                onBack = { currentScreen = Screen.DASHBOARD },
                onOpenHistory = { currentScreen = Screen.COMPLAINT_STATUS },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.COMPLAINT_STATUS -> {
              ComplaintStatusScreen(
                userPhone = userPhone,
                onBack = { currentScreen = Screen.REGISTER_COMPLAINT },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.INSTALLATION_REQUEST -> {
              InstallationRequestScreen(
                userPhone = userPhone,
                onBack = { currentScreen = Screen.DASHBOARD },
                onOpenHistory = { currentScreen = Screen.INSTALLATION_STATUS },
                modifier = Modifier.padding(innerPadding)
              )
            }
            Screen.INSTALLATION_STATUS -> {
              InstallationStatusScreen(
                userPhone = userPhone,
                onBack = { currentScreen = Screen.INSTALLATION_REQUEST },
                modifier = Modifier.padding(innerPadding)
              )
            }
          }
        }
      }
    }
  }
}


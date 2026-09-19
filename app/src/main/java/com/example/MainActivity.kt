package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.data.SessionManager
import com.example.data.network.NetworkModule
import com.example.data.notifications.registerFcmToken
import com.example.ui.screens.ComplaintStatusScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InstallationRequestScreen
import com.example.ui.screens.InstallationStatusScreen
import com.example.ui.screens.MyInvoicesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
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
  MY_INVOICES,
  PROFILE
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

        val notificationPermissionLauncher = rememberLauncherForActivityResult(
          ActivityResultContracts.RequestPermission()
        ) { /* no-op: pushes still arrive, just without a tray notification if denied */ }

        LaunchedEffect(userPhone) {
          if (userPhone.isBlank()) return@LaunchedEffect
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
          ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
          registerFcmToken(NetworkModule.deviceTokenApi, userPhone)
        }

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
                onOpenProfile = { currentScreen = Screen.PROFILE },
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
            Screen.PROFILE -> {
              ProfileScreen(
                userName = userName,
                userPhone = userPhone,
                onBack = { currentScreen = Screen.DASHBOARD },
                onNameUpdated = { updatedName ->
                  sessionManager.updateName(updatedName)
                  userName = updatedName
                },
                onAccountDeleted = {
                  sessionManager.clearSession()
                  currentScreen = Screen.LOGIN
                },
                onSignOut = {
                  sessionManager.clearSession()
                  currentScreen = Screen.LOGIN
                },
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


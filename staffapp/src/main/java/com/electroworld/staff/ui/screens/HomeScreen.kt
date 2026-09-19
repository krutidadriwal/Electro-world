package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class HomeTab { NOTIFICATIONS, STAFF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(isAdmin: Boolean, modifier: Modifier = Modifier) {
  var selectedTab by remember { mutableStateOf(HomeTab.NOTIFICATIONS) }

  Scaffold(
    topBar = { TopAppBar(title = { Text("Electro World Staff") }) },
    bottomBar = {
      if (isAdmin) {
        NavigationBar {
          NavigationBarItem(
            selected = selectedTab == HomeTab.NOTIFICATIONS,
            onClick = { selectedTab = HomeTab.NOTIFICATIONS },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
            label = { Text("Notifications") }
          )
          NavigationBarItem(
            selected = selectedTab == HomeTab.STAFF,
            onClick = { selectedTab = HomeTab.STAFF },
            icon = { Icon(Icons.Default.People, contentDescription = "Staff") },
            label = { Text("Staff") }
          )
        }
      }
    },
    modifier = modifier
  ) { padding ->
    when (selectedTab) {
      HomeTab.NOTIFICATIONS -> NotificationsScreen(isAdmin = isAdmin, modifier = Modifier.padding(padding))
      HomeTab.STAFF -> StaffScreen(modifier = Modifier.padding(padding))
    }
  }
}

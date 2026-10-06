package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class HomeTab { ALERTS, PRICE_LIST, WISHLIST, COMPLAINTS, INSTALLATIONS, INVOICE_REQUESTS, BROADCAST, STAFF }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  isAdmin: Boolean,
  email: String,
  role: String,
  onLogout: () -> Unit,
  notificationTarget: String? = null,
  onNotificationTargetConsumed: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(HomeTab.ALERTS) }
  var showProfile by remember { mutableStateOf(false) }

  // Preset filters applied the next time the relevant tab is entered --
  // consumed by each screen's own `initialStatusFilter` param at the moment
  // it's (re)composed, see navigateToTarget below. Intentionally not reset
  // afterwards: each screen only reads it once, as its own remembered
  // state's initial value, so a later manual tab switch starts fresh.
  var wishlistInitialFilter by remember { mutableStateOf<String?>(null) }
  var complaintsInitialFilter by remember { mutableStateOf<String?>(null) }
  var installationsInitialFilter by remember { mutableStateOf<String?>(null) }

  // Shared by both a tapped push notification (see MainActivity) and tapping
  // a row in-app on the Notifications tab -- same "tab:status" target
  // strings the server sends (see server/lib/staffNotify.js's callers).
  fun navigateToTarget(target: String) {
    val parts = target.split(":", limit = 2)
    val tab = parts.getOrNull(0)
    val filter = parts.getOrNull(1)
    when (tab) {
      "wishlist" -> {
        wishlistInitialFilter = filter
        selectedTab = HomeTab.WISHLIST
      }
      "complaints" -> {
        complaintsInitialFilter = filter
        selectedTab = HomeTab.COMPLAINTS
      }
      "installations" -> {
        installationsInitialFilter = filter
        selectedTab = HomeTab.INSTALLATIONS
      }
      "invoice_requests" -> selectedTab = HomeTab.INVOICE_REQUESTS
      else -> selectedTab = HomeTab.ALERTS
    }
  }

  LaunchedEffect(notificationTarget) {
    notificationTarget?.let {
      navigateToTarget(it)
      onNotificationTargetConsumed()
    }
  }

  if (showProfile) {
    ProfileScreen(
      email = email,
      role = role,
      onBack = { showProfile = false },
      onLogout = onLogout,
      modifier = modifier
    )
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Electro World Staff") },
        navigationIcon = {
          IconButton(onClick = { showProfile = true }) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
          }
        }
      )
    },
    bottomBar = {
      // Every signed-in staff member (not just admins) needs a way to reach
      // Price List and Wishlist -- only Staff management and the customer
      // Broadcast composer stay admin-only.
      NavigationBar {
        NavigationBarItem(
          selected = selectedTab == HomeTab.ALERTS,
          onClick = { selectedTab = HomeTab.ALERTS },
          icon = { Icon(Icons.Default.Notifications, contentDescription = "Notifications") },
          label = { Text("Notifications") }
        )
        NavigationBarItem(
          selected = selectedTab == HomeTab.PRICE_LIST,
          onClick = { selectedTab = HomeTab.PRICE_LIST },
          icon = { Icon(Icons.Default.Sell, contentDescription = "Price List") },
          label = { Text("Price List") }
        )
        NavigationBarItem(
          selected = selectedTab == HomeTab.WISHLIST,
          onClick = { selectedTab = HomeTab.WISHLIST },
          icon = { Icon(Icons.Default.Favorite, contentDescription = "Wishlist") },
          label = { Text("Wishlist") }
        )
        NavigationBarItem(
          selected = selectedTab == HomeTab.COMPLAINTS,
          onClick = { selectedTab = HomeTab.COMPLAINTS },
          icon = { Icon(Icons.Default.Warning, contentDescription = "Complaints") },
          label = { Text("Complaints") }
        )
        NavigationBarItem(
          selected = selectedTab == HomeTab.INSTALLATIONS,
          onClick = { selectedTab = HomeTab.INSTALLATIONS },
          icon = { Icon(Icons.Default.Build, contentDescription = "Installations") },
          label = { Text("Installs") }
        )
        if (isAdmin) {
          NavigationBarItem(
            selected = selectedTab == HomeTab.INVOICE_REQUESTS,
            onClick = { selectedTab = HomeTab.INVOICE_REQUESTS },
            icon = { Icon(Icons.Default.Receipt, contentDescription = "Invoice Requests") },
            label = { Text("Invoices") }
          )
          NavigationBarItem(
            selected = selectedTab == HomeTab.BROADCAST,
            onClick = { selectedTab = HomeTab.BROADCAST },
            icon = { Icon(Icons.Default.Campaign, contentDescription = "Broadcast") },
            label = { Text("Broadcast") }
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
      HomeTab.ALERTS -> AlertsScreen(
        isAdmin = isAdmin,
        onNavigate = { target -> navigateToTarget(target) },
        modifier = Modifier.padding(padding)
      )
      HomeTab.PRICE_LIST -> PriceListScreen(isAdmin = isAdmin, modifier = Modifier.padding(padding))
      HomeTab.WISHLIST -> WishlistScreen(initialStatusFilter = wishlistInitialFilter, modifier = Modifier.padding(padding))
      HomeTab.COMPLAINTS -> ComplaintsScreen(initialStatusFilter = complaintsInitialFilter, modifier = Modifier.padding(padding))
      HomeTab.INSTALLATIONS -> InstallationsScreen(initialStatusFilter = installationsInitialFilter, modifier = Modifier.padding(padding))
      HomeTab.INVOICE_REQUESTS -> InvoiceRequestsScreen(modifier = Modifier.padding(padding))
      HomeTab.BROADCAST -> NotificationsScreen(isAdmin = isAdmin, modifier = Modifier.padding(padding))
      HomeTab.STAFF -> StaffScreen(modifier = Modifier.padding(padding))
    }
  }
}

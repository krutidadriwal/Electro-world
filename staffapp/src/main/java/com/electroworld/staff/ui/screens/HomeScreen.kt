package com.electroworld.staff.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroworld.staff.data.network.NetworkModule

private enum class HomeTab { ALERTS, PRICE_LIST, WISHLIST, COMPLAINTS, INSTALLATIONS, INVOICE_REQUESTS, BROADCAST, STAFF, CATEGORY_IMAGES, SPIN }

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
  // null = the department grid (dashboard home); non-null = that tab shown
  // full-screen with its own back arrow, matching the customer app's
  // dashboard-of-cards -> full-screen-module pattern rather than a
  // persistent bottom NavigationBar.
  var selectedTab by remember { mutableStateOf<HomeTab?>(null) }
  var showProfile by remember { mutableStateOf(false) }
  var wishlistCount by remember { mutableStateOf(0) }
  var complaintsCount by remember { mutableStateOf(0) }
  var installationsCount by remember { mutableStateOf(0) }
  var invoiceCount by remember { mutableStateOf(0) }
  val unreadAlertCount = wishlistCount + complaintsCount + installationsCount + invoiceCount

  // Preset filters applied the next time the relevant tab is entered --
  // consumed by each screen's own `initialStatusFilter` param at the moment
  // it's (re)composed, see navigateToTarget below. Intentionally not reset
  // afterwards: each screen only reads it once, as its own remembered
  // state's initial value, so a later manual visit starts fresh.
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

  // Refreshed whenever we return to the dashboard (selectedTab becomes
  // null), so acting on an alert and coming back updates the bell badge.
  LaunchedEffect(selectedTab) {
    if (selectedTab != null) return@LaunchedEffect
    try {
      wishlistCount = NetworkModule.wishlistApi.list().items.count { it.status == "not_contacted" }
      complaintsCount = NetworkModule.complaintsApi.list().items.count { it.status == "open" }
      installationsCount = NetworkModule.installationsApi.list().items.count { it.status == "open" }
      if (isAdmin) {
        invoiceCount = NetworkModule.invoiceRequestsApi.list().items.count { it.status == "open" }
      }
    } catch (e: Exception) {
      // Leave the previous badge counts showing rather than clearing them.
    }
  }

  BackHandler(enabled = selectedTab != null) { selectedTab = null }

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

  selectedTab?.let { tab ->
    val onBack = { selectedTab = null }
    when (tab) {
      HomeTab.ALERTS -> AlertsScreen(isAdmin = isAdmin, onNavigate = { navigateToTarget(it) }, onBack = onBack, modifier = modifier)
      HomeTab.PRICE_LIST -> PriceListScreen(isAdmin = isAdmin, onBack = onBack, modifier = modifier)
      HomeTab.WISHLIST -> WishlistScreen(initialStatusFilter = wishlistInitialFilter, onBack = onBack, modifier = modifier)
      HomeTab.COMPLAINTS -> ComplaintsScreen(initialStatusFilter = complaintsInitialFilter, onBack = onBack, modifier = modifier)
      HomeTab.INSTALLATIONS -> InstallationsScreen(initialStatusFilter = installationsInitialFilter, onBack = onBack, modifier = modifier)
      HomeTab.INVOICE_REQUESTS -> InvoiceRequestsScreen(onBack = onBack, modifier = modifier)
      HomeTab.BROADCAST -> NotificationsScreen(isAdmin = isAdmin, onBack = onBack, modifier = modifier)
      HomeTab.STAFF -> StaffScreen(onBack = onBack, modifier = modifier)
      HomeTab.CATEGORY_IMAGES -> CategoryImagesScreen(onBack = onBack, modifier = modifier)
      HomeTab.SPIN -> SpinScreen(onBack = onBack, modifier = modifier)
    }
    return
  }

  val priceListModule = DashboardModule("PRICE LIST", Icons.Default.Sell, "Browse the retail price list") { selectedTab = HomeTab.PRICE_LIST }

  val adminModules = listOf(
    DashboardModule("INVOICE REQUESTS", Icons.Default.Receipt, "Re-send invoice requests", badgeCount = invoiceCount) { selectedTab = HomeTab.INVOICE_REQUESTS },
    DashboardModule("BROADCAST", Icons.Default.Campaign, "notify customers") { selectedTab = HomeTab.BROADCAST },
    DashboardModule("STAFF", Icons.Default.People, "Manage staff accounts") { selectedTab = HomeTab.STAFF },
    DashboardModule("CATEGORY IMAGES", Icons.Default.Image, "Set price list hero images") { selectedTab = HomeTab.CATEGORY_IMAGES },
    DashboardModule("SPIN TO WIN", Icons.Default.Casino, "Unlock spins & manage prizes") { selectedTab = HomeTab.SPIN }
  )

  val serviceModules = listOf(
    DashboardModule("NOTIFICATIONS", Icons.Default.Notifications, "New alerts & requests", badgeCount = unreadAlertCount) { selectedTab = HomeTab.ALERTS },
    DashboardModule("WISHLIST", Icons.Default.Favorite, "Interest Check", badgeCount = wishlistCount) { selectedTab = HomeTab.WISHLIST },
    DashboardModule("COMPLAINTS", Icons.Default.Warning, "Resolve complaints", badgeCount = complaintsCount) { selectedTab = HomeTab.COMPLAINTS },
    DashboardModule("INSTALLATIONS", Icons.Default.Build, "Demo & Installation", badgeCount = installationsCount) { selectedTab = HomeTab.INSTALLATIONS }
  )

  Scaffold(modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) { padding ->
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        DashboardHeader(
          email = email,
          role = role,
          onOpenProfile = { showProfile = true }
        )
      }
      // Price List leads, full-width (spans both columns) -- it's the single
      // highest-traffic tool (every sale starts there), so it gets top
      // billing and more tap area rather than sharing a row.
      item(span = { GridItemSpan(maxLineSpan) }) {
        ModuleCard(priceListModule, modifier = Modifier.fillMaxWidth())
      }

      if (isAdmin) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          SectionLabel("ADMIN")
        }
        items(adminModules) { module -> ModuleCard(module) }
      }

      item(span = { GridItemSpan(maxLineSpan) }) {
        SectionLabel("SERVICES")
      }
      items(serviceModules) { module -> ModuleCard(module) }
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Text(
    text = text,
    color = MaterialTheme.colorScheme.primary,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 2.sp,
    modifier = Modifier.padding(vertical = 6.dp)
  )
}

@Composable
private fun ModuleCard(module: DashboardModule, modifier: Modifier = Modifier) {
  Box(modifier = modifier) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
        .clickable(onClick = module.onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Box(
          modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(module.icon, contentDescription = module.title, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = module.title,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = module.subtitle,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    if (module.badgeCount > 0) {
      CountBadge(count = module.badgeCount, modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp))
    }
  }
}

private data class DashboardModule(
  val title: String,
  val icon: ImageVector,
  val subtitle: String,
  val badgeCount: Int = 0,
  val onClick: () -> Unit
)

// Count pill -- shared across each dashboard card's badge, so it always
// matches what the Notifications tab would show for that same category
// (both are driven by the same not_contacted/open counts fetched in
// HomeScreen). Uses `secondary` (swatch6, deep amber) rather than a
// red/error color -- the palette has no red.
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.size(22.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = if (count > 99) "99+" else count.toString(),
      color = MaterialTheme.colorScheme.onSecondary,
      fontSize = 11.sp,
      lineHeight = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun DashboardHeader(
  email: String,
  role: String,
  onOpenProfile: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 40.dp, bottom = 16.dp)
      .clickable(onClick = onOpenProfile),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = email.take(2).ifBlank { "EW" }.uppercase(),
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
    }
    Column(modifier = Modifier.padding(start = 12.dp)) {
      // outline, not onSurfaceVariant -- this text sits directly on
      // `background`, not on a `surface`-colored card, and in dark mode
      // onSurfaceVariant is set to the same color as background (see
      // Theme.kt), which would make it invisible there.
      Text(text = "Welcome Back,", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
      Text(
        text = email.ifBlank { "Staff" },
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(text = if (role == "admin") "Admin" else "Employee", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
    }
  }
}

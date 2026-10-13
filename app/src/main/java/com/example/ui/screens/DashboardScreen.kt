package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import com.example.data.InvoiceFileCache
import com.example.data.InvoiceListCache
import com.example.data.NotificationReadStore
import com.example.data.network.ActiveNotification
import com.example.data.network.CreateInvoiceRequestRequest
import com.example.data.network.InvoiceFile
import com.example.data.network.NetworkModule
import com.example.data.notifications.notificationTrayId
import com.example.ui.theme.appCardColors
import kotlinx.coroutines.launch

// Screen Enumeration for dialog overlays
enum class ActiveModule {
  NONE,
  VISIT_STORE,
  NOTIFICATIONS
}

@Composable
fun DashboardScreen(
  userName: String,
  userPhone: String,
  onOpenProfile: () -> Unit,
  onOpenWishlist: () -> Unit,
  onOpenRegisterComplaint: () -> Unit,
  onOpenInstallation: () -> Unit,
  onOpenInvoices: () -> Unit,
  onOpenPoints: () -> Unit,
  onOpenSpin: () -> Unit,
  modifier: Modifier = Modifier,
  // Lets the caller land here with an overlay already open (Spin to Win's
  // "Get spin access" opens Visit Store, a tapped tray notification opens
  // Notifications) -- also while the dashboard is already showing.
  initialOverlay: ActiveModule = ActiveModule.NONE,
  onInitialOverlayShown: () -> Unit = {}
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val invoiceListCache = remember { InvoiceListCache(context) }
  var activeOverlay by remember { mutableStateOf(initialOverlay) }
  LaunchedEffect(initialOverlay) {
    if (initialOverlay == ActiveModule.NONE) return@LaunchedEffect
    activeOverlay = initialOverlay
    onInitialOverlayShown()
  }
  var availableSpins by remember { mutableStateOf(0) }
  // null until the first load (or while offline) -- the header chip shows a
  // dash rather than a misleading 0.
  var pointsBalance by remember { mutableStateOf<Int?>(null) }
  val displayName = remember(userName) { headerDisplayName(userName) }

  LaunchedEffect(userPhone) {
    try {
      pointsBalance = NetworkModule.userApi.getPoints(userPhone).balance
    } catch (e: Exception) {
      // Offline -- leave the previous balance (or the dash) showing.
    }
  }

  LaunchedEffect(userPhone) {
    try {
      availableSpins = NetworkModule.userApi.getSpinState(userPhone).availableSpins
    } catch (e: Exception) {
      // Offline -- the banner just keeps its default "locked" wording.
    }
  }
  var recentInvoices by remember { mutableStateOf(invoiceListCache.get(userPhone)?.take(3) ?: emptyList()) }
  var openingInvoiceId by remember { mutableStateOf<String?>(null) }
  val notificationReadStore = remember { NotificationReadStore(context) }
  var unreadNotificationCount by remember { mutableStateOf(0) }

  // Re-fetches whenever the notifications overlay closes, so opening one
  // (which marks it read) is reflected in the bell badge right away.
  LaunchedEffect(activeOverlay) {
    if (activeOverlay != ActiveModule.NONE) return@LaunchedEffect
    try {
      val active = NetworkModule.notificationsApi.getActiveNotifications().notifications
      unreadNotificationCount = active.count { !notificationReadStore.isRead(it.id) }
    } catch (e: Exception) {
      // Offline -- leave the previous count showing rather than clearing it.
    }
  }

  fun openInvoice(invoice: InvoiceFile) {
    if (openingInvoiceId != null) return
    openingInvoiceId = invoice.id
    coroutineScope.launch {
      try {
        val file = InvoiceFileCache.getOrDownload(context, userPhone, invoice)
        InvoiceFileCache.openFile(context, file)
      } catch (e: Exception) {
        Toast.makeText(context, "Unable to open invoice. Please try again.", Toast.LENGTH_SHORT).show()
      } finally {
        openingInvoiceId = null
      }
    }
  }

  LaunchedEffect(userPhone) {
    try {
      val fresh = NetworkModule.userApi.getInvoices(userPhone).invoices
      invoiceListCache.save(userPhone, fresh)
      recentInvoices = fresh.take(3)
    } catch (e: Exception) {
      // Offline -- leave whatever was loaded from cache (or the empty list)
      // showing rather than clearing it out.
    }
  }

  // Back dismisses whichever overlay module is open instead of falling
  // through to the system default (closing the app).
  BackHandler(enabled = activeOverlay != ActiveModule.NONE) { activeOverlay = ActiveModule.NONE }

  // Dashboard Grid definition: left/right pairs per row
  val modules = listOf(
    DashboardModuleItem("INSTALLATION REQUEST", Icons.Default.Settings, "Schedule a new installation", onClick = onOpenInstallation),
    DashboardModuleItem("REGISTER COMPLAINT", Icons.Default.Warning, "Report a service issue", onClick = onOpenRegisterComplaint),
    DashboardModuleItem("WISHLIST", Icons.Default.Favorite, "Browse categories you've saved", onClick = onOpenWishlist),
    DashboardModuleItem("VISIT STORE", Icons.Default.LocationOn, "Address, map & contact", ActiveModule.VISIT_STORE)
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Main Scrollable Area
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(bottom = 24.dp)
      ) {
        // Flat header: avatar + greeting (tap to open Profile) and a
        // notification bell, matching the reference layout.
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier
              .weight(1f)
              .clickable { onOpenProfile() },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = displayName.take(2).ifBlank { "EW" }.uppercase(),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
              // outline: the palette's muted-text tone (see Theme.kt).
              Text(text = "Welcome Back,", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
              Text(
                text = displayName.ifBlank { "Guest" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          PointsChip(
            balance = pointsBalance,
            onClick = onOpenPoints,
            modifier = Modifier.padding(start = 8.dp, end = 10.dp)
          )

          Box {
            Box(
              modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable { activeOverlay = ActiveModule.NOTIFICATIONS },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
              )
            }
            if (unreadNotificationCount > 0) {
              // secondary, not a red/error color -- the palette has no red,
              // same convention as the staff app's CountBadge.
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .offset(x = 2.dp, y = (-2).dp)
                  .size(18.dp)
                  .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                  color = MaterialTheme.colorScheme.onSecondary,
                  fontSize = 10.sp,
                  lineHeight = 10.sp,
                  fontWeight = FontWeight.Bold,
                  style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                  )
                )
              }
            }
          }
        }

        // Section header
        Text(
          text = "STORE DEPARTMENTS & SERVICES",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
        )

        // 2-column module grid
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .padding(horizontal = 12.dp),
          userScrollEnabled = false,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(modules) { item ->
            Card(
              modifier = Modifier
                .height(140.dp)
                .clickable { item.onClick?.invoke() ?: run { activeOverlay = item.actionType } }
                .testTag("module_${item.title.lowercase().replace(" ", "_")}"),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
              Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = item.title,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        SpinToWinBanner(
          availableSpins = availableSpins,
          onClick = onOpenSpin,
          modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "MY INVOICES",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
          Row(
            modifier = Modifier.clickable { onOpenInvoices() },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "See more", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          colors = appCardColors(),
          shape = RoundedCornerShape(16.dp)
        ) {
          if (recentInvoices.isEmpty()) {
            Text(
              text = "No invoices yet.",
              color = MaterialTheme.colorScheme.outline,
              fontSize = 12.sp,
              modifier = Modifier.padding(16.dp)
            )
          } else {
            Row(
              modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(14.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              recentInvoices.forEach { invoice ->
                InvoiceChip(
                  invoice = invoice,
                  isOpening = openingInvoiceId == invoice.id,
                  onClick = { openInvoice(invoice) }
                )
              }
            }
          }
        }
      }

      // Overlay portal for the active module's detail screen. Visit Store
      // reads as a page of its own -- fully opaque backdrop, a darker header
      // band -- where Notifications is a card floating over the dimmed
      // dashboard.
      val isVisitStore = activeOverlay == ActiveModule.VISIT_STORE
      AnimatedVisibility(
        visible = activeOverlay != ActiveModule.NONE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
      ) {
        Surface(
          color = MaterialTheme.colorScheme.scrim.copy(alpha = if (isVisitStore) 1f else 0.85f),
          modifier = Modifier.fillMaxSize()
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(20.dp),
            contentAlignment = Alignment.Center
          ) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                // No shadow for Visit Store: it sits on a fully opaque
                // backdrop, where there is nothing for it to lift off.
                .let { if (isVisitStore) it else it.shadow(16.dp, RoundedCornerShape(20.dp)) },
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(20.dp),
              border = if (isVisitStore) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
              Column(modifier = Modifier.fillMaxSize()) {
                // Modal Header Bar -- a darker inset band for Visit Store
                // (matching the card's own top corner radius), vs. the plain
                // inline header the other overlay uses.
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .let {
                      if (isVisitStore) {
                        it.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                      } else {
                        it
                      }
                    }
                    .padding(20.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isVisitStore) {
                      Icon(
                        imageVector = getModuleIcon(activeOverlay),
                        contentDescription = "Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                      ) {
                        // onSurface: this badge sits on
                        // primary.copy(alpha=0.1f) over the overlay card's
                        // own `surface` background.
                        Icon(
                          imageVector = getModuleIcon(activeOverlay),
                          contentDescription = "Icon",
                          tint = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = getModuleTitle(activeOverlay),
                      color = MaterialTheme.colorScheme.onSurface,
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  IconButton(
                    onClick = { activeOverlay = ActiveModule.NONE },
                    modifier = Modifier.background(
                      if (isVisitStore) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                      CircleShape
                    )
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Dismiss",
                      tint = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }

                if (!isVisitStore) {
                  Divider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                  )
                }

                // Render respective interactive placeholder layouts dynamically
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 20.dp, top = if (isVisitStore) 20.dp else 0.dp, bottom = 20.dp)
                ) {
                  when (activeOverlay) {
                    ActiveModule.VISIT_STORE -> VisitStoreSubScreen()
                    ActiveModule.NOTIFICATIONS -> NotificationsSubScreen()
                    else -> Unit
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

// Module Descriptor
data class DashboardModuleItem(
  val title: String,
  val icon: ImageVector,
  val subtitle: String,
  val actionType: ActiveModule = ActiveModule.NONE,
  val onClick: (() -> Unit)? = null
)

// First name only for the header greeting, so it fits beside the points chip
// and bell: leading titles are skipped and everything after the first real
// name (middle names, surname) is dropped -- "Mr. Ajayshriraj Dada" shows as
// "Ajayshriraj". A name that is nothing but titles is shown as typed.
private val NameTitles = setOf(
  "mr", "mrs", "ms", "miss", "mx", "dr", "prof", "er", "adv", "ca",
  "shri", "shree", "sri", "smt", "shrimati", "kumari", "km"
)

private fun headerDisplayName(fullName: String): String {
  // Split on dots as well as spaces, so "Mr.Ajay" (no space) still parts.
  val words = fullName.trim().split(Regex("[\\s.]+")).filter { it.isNotEmpty() }
  return words.firstOrNull { it.lowercase() !in NameTitles } ?: fullName.trim()
}

// The points balance, shown in the header to the left of the bell. Tapping
// it opens the Points screen.
@Composable
private fun PointsChip(balance: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .height(44.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp)
      .testTag("header_points_chip"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = "Points",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(16.dp)
    )
    Text(
      text = balance?.toString() ?: "–",
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      modifier = Modifier.padding(start = 6.dp)
    )
  }
}

// Entry point to SpinScreen, badged with the EW logo (the same amber one
// that sits on the wheel's hub there).
@Composable
private fun SpinToWinBanner(availableSpins: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("spin_to_win_banner"),
    colors = appCardColors(),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ew_logo_amber),
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier.size(64.dp)
      )
      Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
        Text(
          text = "SPIN TO WIN!",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Black
        )
        Text(
          text = when {
            availableSpins > 1 -> "You have $availableSpins spins ready"
            availableSpins == 1 -> "You have a spin ready"
            else -> "Tap to get limited offers"
          },
          color = MaterialTheme.colorScheme.outline,
          fontSize = 12.sp
        )
      }
      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun InvoiceChip(invoice: InvoiceFile, isOpening: Boolean, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .width(130.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
      .clickable(enabled = !isOpening) { onClick() }
      .padding(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      if (isOpening) {
        androidx.compose.material3.CircularProgressIndicator(
          modifier = Modifier.size(14.dp),
          color = MaterialTheme.colorScheme.secondary,
          strokeWidth = 1.5.dp
        )
      } else {
        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = invoice.name,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    invoice.createdAt?.let {
      Text(
        text = formatInvoiceDate(it),
        color = MaterialTheme.colorScheme.outline,
        fontSize = 10.sp,
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}

// Map screen icons
private fun getModuleIcon(module: ActiveModule): ImageVector {
  return when (module) {
    ActiveModule.VISIT_STORE -> Icons.Default.LocationOn
    ActiveModule.NOTIFICATIONS -> Icons.Default.Notifications
    else -> Icons.Default.Info
  }
}

private fun getModuleTitle(module: ActiveModule): String {
  return when (module) {
    ActiveModule.VISIT_STORE -> "Visit Our Store"
    ActiveModule.NOTIFICATIONS -> "Notifications"
    else -> ""
  }
}

// Google Drive returns full ISO-8601 timestamps (createdTime, e.g.
// "2024-05-01T12:34:56.789Z"); invoices only need the date, as DD-MM-YYYY.
// minSdk 24 predates java.time (API 26) with no desugaring configured, so
// this parses the "yyyy-MM-dd" prefix directly rather than via java.time.
private fun formatInvoiceDate(isoTimestamp: String): String {
  val datePart = isoTimestamp.substringBefore('T')
  val parts = datePart.split("-")
  return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else isoTimestamp
}

// ==========================================
// INDIVIDUAL MODULES INTERACTIVE UI LAYOUTS
// ==========================================

// 1. MY INVOICES SUB-SCREEN
@Composable
fun MyInvoicesSubScreen(userPhone: String) {
  val context = LocalContext.current
  val invoiceListCache = remember { InvoiceListCache(context) }
  var invoices by remember { mutableStateOf(invoiceListCache.get(userPhone) ?: emptyList()) }
  var isLoading by remember { mutableStateOf(invoices.isEmpty()) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var openingInvoiceId by remember { mutableStateOf<String?>(null) }
  var showRequestDialog by remember { mutableStateOf(false) }
  var requestDescription by remember { mutableStateOf("") }
  var isSubmittingRequest by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  fun openInvoice(invoice: InvoiceFile) {
    if (openingInvoiceId != null) return
    openingInvoiceId = invoice.id
    coroutineScope.launch {
      try {
        val file = InvoiceFileCache.getOrDownload(context, userPhone, invoice)
        InvoiceFileCache.openFile(context, file)
      } catch (e: Exception) {
        Toast.makeText(context, "Unable to open invoice. Please try again.", Toast.LENGTH_SHORT).show()
      } finally {
        openingInvoiceId = null
      }
    }
  }

  fun submitInvoiceRequest() {
    val trimmed = requestDescription.trim()
    if (trimmed.isEmpty()) return
    isSubmittingRequest = true
    coroutineScope.launch {
      try {
        NetworkModule.userApi.createInvoiceRequest(CreateInvoiceRequestRequest(phone = userPhone, description = trimmed))
        isSubmittingRequest = false
        showRequestDialog = false
        requestDescription = ""
        Toast.makeText(context, "Request submitted. Our team will follow up soon.", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        isSubmittingRequest = false
        Toast.makeText(context, "Unable to submit request. Please try again.", Toast.LENGTH_SHORT).show()
      }
    }
  }

  LaunchedEffect(userPhone) {
    if (invoices.isEmpty()) isLoading = true
    try {
      val fresh = com.example.data.network.NetworkModule.userApi.getInvoices(userPhone).invoices
      invoices = fresh
      invoiceListCache.save(userPhone, fresh)
      errorMessage = null
    } catch (e: Exception) {
      // Offline (or the request just failed) with something already cached
      // to show -- stay on the stale list silently rather than erroring out.
      if (invoices.isEmpty()) {
        errorMessage = "Unable to load invoices. Please check your connection and try again."
      }
    } finally {
      isLoading = false
    }
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    when {
      isLoading -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
      }
      errorMessage != null -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp, textAlign = TextAlign.Center)
          }
        }
      }
      invoices.isEmpty() -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Info, "No invoices", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No invoices found for your account yet.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp, textAlign = TextAlign.Center)
          }
        }
      }
      else -> {
        items(invoices) { inv ->
          Card(
            colors = appCardColors(),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = openingInvoiceId == null) { openInvoice(inv) },
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = inv.name,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                inv.createdAt?.let {
                  Text(text = formatInvoiceDate(it), color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
              }
              if (openingInvoiceId == inv.id) {
                androidx.compose.material3.CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.primary,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.Default.Description, "Open PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              }
            }
          }
        }
      }
    }

    // Always the last item, regardless of loading/error/empty/list state,
    // so it naturally sits at the bottom of the scroll -- for a user who
    // can't find what they're looking for above, or has nothing listed yet.
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp)
          .clickable { showRequestDialog = true },
        colors = appCardColors(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          }
          Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = "Can't find an invoice?", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "Request one from our team", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
        }
      }
    }
  }

  if (showRequestDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { if (!isSubmittingRequest) showRequestDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      title = { Text("Request an Invoice", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "Describe the purchase you need an invoice for (item, approximate date, etc.) and our team will follow up.",
            color = MaterialTheme.colorScheme.outline,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
          )
          androidx.compose.material3.OutlinedTextField(
            value = requestDescription,
            onValueChange = { requestDescription = it },
            placeholder = { Text("e.g. Refrigerator purchased around March 2026", color = MaterialTheme.colorScheme.outline) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = { submitInvoiceRequest() },
          enabled = !isSubmittingRequest && requestDescription.trim().isNotEmpty()
        ) {
          if (isSubmittingRequest) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
          } else {
            Text("Submit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showRequestDialog = false }, enabled = !isSubmittingRequest) {
          Text("Cancel", color = MaterialTheme.colorScheme.outline)
        }
      }
    )
  }
}

// 2. NOTIFICATIONS SUB-SCREEN
// Lists still-active (not-yet-expired) notifications, first line only,
// greyed out once opened. Read state is a local "seen" marker -- kept in
// SharedPreferences via NotificationReadStore, never sent to the server.
@Composable
fun NotificationsSubScreen() {
  val context = LocalContext.current
  val readStore = remember { NotificationReadStore(context) }
  var notifications by remember { mutableStateOf<List<ActiveNotification>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var readIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var selected by remember { mutableStateOf<ActiveNotification?>(null) }

  LaunchedEffect(Unit) {
    try {
      val fresh = NetworkModule.notificationsApi.getActiveNotifications().notifications
      notifications = fresh
      readIds = fresh.filter { readStore.isRead(it.id) }.map { it.id }.toSet()
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Unable to load notifications. Please check your connection and try again."
    } finally {
      isLoading = false
    }
  }

  fun openNotification(notification: ActiveNotification) {
    readStore.markRead(notification.id)
    readIds = readIds + notification.id
    selected = notification
    // Clears this specific tray notification so OEM launchers that show a
    // numeric home-screen badge (a count of active, undismissed
    // notifications) decrement it immediately, same as reading a chat does
    // in WhatsApp -- see notificationTrayId's doc for why this id matches.
    NotificationManagerCompat.from(context).cancel(notificationTrayId(notification.id))
  }

  // Unread first, read (greyed out) pushed to the bottom -- stable within
  // each group, so it doesn't reshuffle the server's created_at desc order.
  val sortedNotifications = remember(notifications, readIds) {
    notifications.sortedBy { it.id in readIds }
  }

  when {
    isLoading -> {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    }
    errorMessage != null -> {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp, textAlign = TextAlign.Center)
      }
    }
    notifications.isEmpty() -> {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(Icons.Default.Notifications, "No notifications", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("You're all caught up -- no active notifications.", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp, textAlign = TextAlign.Center)
      }
    }
    else -> {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(sortedNotifications, key = { it.id }) { notification ->
          val isRead = notification.id in readIds
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isRead) 0.4f else 1f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { openNotification(notification) },
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .background(
                    (if (isRead) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary).copy(alpha = 0.35f),
                    CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Notifications,
                  contentDescription = null,
                  tint = if (isRead) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                  text = notification.message.substringBefore('\n'),
                  color = if (isRead) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                  fontSize = 13.sp,
                  fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = formatInvoiceDate(notification.createdAt),
                  color = MaterialTheme.colorScheme.outline,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
              Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }

  selected?.let { notification ->
    NotificationDetailDialog(notification = notification, onDismiss = { selected = null })
  }
}

@Composable
private fun NotificationDetailDialog(notification: ActiveNotification, onDismiss: () -> Unit) {
  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "Announcement", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        notification.imageUrl?.let { url ->
          // AsyncImage needs an explicit height before the image has loaded
          // (its intrinsic size is unknown until then) -- fillMaxWidth()
          // alone, with no height/aspectRatio, was collapsing to zero height
          // inside this Dialog + scrollable Column and never showing
          // anything. 16:9 matches the images actually uploaded so far;
          // Crop keeps it filling the box even if that ever varies slightly.
          AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(16f / 9f)
              .clip(RoundedCornerShape(12.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
          )
          Spacer(modifier = Modifier.height(14.dp))
        }
        Text(text = notification.message, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = formatInvoiceDate(notification.createdAt), color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
      }
    }
  }
}

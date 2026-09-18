package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.InvoiceFileCache
import com.example.data.InvoiceListCache
import com.example.data.network.CreateInvoiceRequestRequest
import com.example.data.network.InvoiceFile
import com.example.data.network.NetworkModule
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import kotlinx.coroutines.launch

// Screen Enumeration for dialog overlays
enum class ActiveModule {
  NONE,
  VISIT_STORE
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
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val invoiceListCache = remember { InvoiceListCache(context) }
  var activeOverlay by remember { mutableStateOf(ActiveModule.NONE) }
  var showNotificationsToast by remember { mutableStateOf(false) }
  var recentInvoices by remember { mutableStateOf(invoiceListCache.get(userPhone)?.take(3) ?: emptyList()) }
  var openingInvoiceId by remember { mutableStateOf<String?>(null) }

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
    containerColor = SlateBackground
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
                .background(SlateSurfaceVariant, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = userName.take(2).ifBlank { "EW" }.uppercase(),
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
              Text(text = "Welcome Back,", color = OnSlateTextSecondary, fontSize = 12.sp)
              Text(
                text = userName.ifBlank { "Guest" },
                color = OnSlateText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Box(
            modifier = Modifier
              .size(44.dp)
              .background(SlateSurface, CircleShape)
              .clickable { showNotificationsToast = true },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Notifications",
              tint = GoldSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        if (showNotificationsToast) {
          Text(
            text = "You're all caught up -- no new notifications.",
            color = OnSlateTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
          )
        }

        // Section header
        Text(
          text = "STORE DEPARTMENTS & SERVICES",
          color = GoldPrimary,
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
            .height(300.dp)
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
              colors = CardDefaults.cardColors(containerColor = SlateSurface),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, SlateSurfaceVariant)
            ) {
              Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .background(GoldPrimary.copy(alpha = 0.12f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = GoldSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = item.title,
                  color = OnSlateText,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

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
            color = GoldPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
          Row(
            modifier = Modifier.clickable { onOpenInvoices() },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "See more", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = GoldSecondary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          colors = CardDefaults.cardColors(containerColor = SlateSurface),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, SlateSurfaceVariant)
        ) {
          if (recentInvoices.isEmpty()) {
            Text(
              text = "No invoices yet.",
              color = OnSlateTextSecondary,
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

      // Overlay portal for the active module's detail screen
      AnimatedVisibility(
        visible = activeOverlay != ActiveModule.NONE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
      ) {
        Surface(
          color = Color(0xF2121212), // High contrast transparent scrim background
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
                .shadow(16.dp, RoundedCornerShape(20.dp)),
              colors = CardDefaults.cardColors(containerColor = SlateSurface),
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(20.dp)
              ) {
                // Overlay Sheet Header
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .background(GoldPrimary.copy(alpha = 0.1f), CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = getModuleIcon(activeOverlay),
                        contentDescription = "Icon",
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = getModuleTitle(activeOverlay),
                      color = OnSlateText,
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  IconButton(
                    onClick = { activeOverlay = ActiveModule.NONE },
                    modifier = Modifier.background(SlateSurfaceVariant, CircleShape)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Dismiss",
                      tint = OnSlateTextSecondary
                    )
                  }
                }

                Divider(
                  color = SlateSurfaceVariant,
                  modifier = Modifier.padding(vertical = 12.dp)
                )

                // Render respective interactive placeholder layouts dynamically
                Box(modifier = Modifier.weight(1f)) {
                  when (activeOverlay) {
                    ActiveModule.VISIT_STORE -> VisitStoreSubScreen()
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

@Composable
private fun InvoiceChip(invoice: InvoiceFile, isOpening: Boolean, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .width(130.dp)
      .background(SlateSurfaceVariant, RoundedCornerShape(12.dp))
      .clickable(enabled = !isOpening) { onClick() }
      .padding(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      if (isOpening) {
        androidx.compose.material3.CircularProgressIndicator(
          modifier = Modifier.size(14.dp),
          color = GoldSecondary,
          strokeWidth = 1.5.dp
        )
      } else {
        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(14.dp))
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = invoice.name,
      color = OnSlateText,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    invoice.createdAt?.let {
      Text(
        text = formatInvoiceDate(it),
        color = OnSlateTextSecondary,
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
    else -> Icons.Default.Info
  }
}

private fun getModuleTitle(module: ActiveModule): String {
  return when (module) {
    ActiveModule.VISIT_STORE -> "Visit Our Store"
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
            androidx.compose.material3.CircularProgressIndicator(color = GoldPrimary)
          }
        }
      }
      errorMessage != null -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Warning, "Error", tint = GoldSecondary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage ?: "", color = OnSlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
          }
        }
      }
      invoices.isEmpty() -> {
        item {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Info, "No invoices", tint = GoldSecondary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No invoices found for your account yet.", color = OnSlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
          }
        }
      }
      else -> {
        items(invoices) { inv ->
          Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
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
                  color = OnSlateText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                inv.createdAt?.let {
                  Text(text = formatInvoiceDate(it), color = OnSlateTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
              }
              if (openingInvoiceId == inv.id) {
                androidx.compose.material3.CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = GoldPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(Icons.Default.Description, "Open PDF", tint = GoldPrimary, modifier = Modifier.size(20.dp))
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
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f))
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(GoldPrimary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
          }
          Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = "Can't find an invoice?", color = OnSlateText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "Request one from our team", color = OnSlateTextSecondary, fontSize = 11.sp)
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(18.dp))
        }
      }
    }
  }

  if (showRequestDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { if (!isSubmittingRequest) showRequestDialog = false },
      containerColor = SlateSurface,
      title = { Text("Request an Invoice", color = OnSlateText, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            text = "Describe the purchase you need an invoice for (item, approximate date, etc.) and our team will follow up.",
            color = OnSlateTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
          )
          androidx.compose.material3.OutlinedTextField(
            value = requestDescription,
            onValueChange = { requestDescription = it },
            placeholder = { Text("e.g. Refrigerator purchased around March 2026", color = OnSlateTextSecondary) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GoldPrimary,
              unfocusedBorderColor = SlateSurfaceVariant,
              focusedContainerColor = SlateSurface,
              unfocusedContainerColor = SlateSurface,
              focusedTextColor = OnSlateText,
              unfocusedTextColor = OnSlateText
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
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GoldPrimary, strokeWidth = 2.dp)
          } else {
            Text("Submit", color = GoldPrimary, fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showRequestDialog = false }, enabled = !isSubmittingRequest) {
          Text("Cancel", color = OnSlateTextSecondary)
        }
      }
    )
  }
}

// 2. VISIT STORE SUB-SCREEN
@Composable
fun VisitStoreSubScreen() {
  val context = LocalContext.current
  val storeName = com.example.BuildConfig.STORE_NAME
  val storeAddress = com.example.BuildConfig.STORE_ADDRESS
  val storePhone = com.example.BuildConfig.STORE_PHONE
  val storeWorkingHours = com.example.BuildConfig.STORE_WORKING_HOURS
  val storeLatitude = com.example.BuildConfig.STORE_LATITUDE
  val storeLongitude = com.example.BuildConfig.STORE_LONGITUDE

  Column(modifier = Modifier.fillMaxSize()) {
    Card(
      colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(text = storeName, color = GoldSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = storeAddress, color = OnSlateText, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
        Text(text = "Hours: $storeWorkingHours", color = OnSlateTextSecondary, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
          Icon(Icons.Default.Phone, "Phone", tint = GoldPrimary, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = storePhone, color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
      onClick = {
        val uri = Uri.parse("geo:$storeLatitude,$storeLongitude?q=$storeLatitude,$storeLongitude($storeName)")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
      },
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.LocationOn, "Map", tint = SlateBackground, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("VIEW ON MAP", color = SlateBackground, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
      onClick = {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$storePhone")))
      },
      colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceVariant, contentColor = OnSlateText),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.Phone, "Call", tint = GoldPrimary, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("CALL STORE", fontWeight = FontWeight.Bold)
    }
  }
}

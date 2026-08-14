package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ElectroWorldLogo
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen Enumeration for dialog overlays
enum class ActiveModule {
  NONE,
  MY_INVOICES,
  TRACK_COMPLAINT,
  STORES,
  BUY_NOW,
  CALL_CUSTOMER_CARE,
  MAIL_YOUR_FEEDBACK
}

// Data Classes for State Management
data class StoreInvoice(
  val id: String,
  val itemNames: List<String>,
  val purchaseDate: String,
  val totalAmount: Double,
  val serialNumber: String
)

data class ServiceComplaint(
  val id: String,
  val applianceName: String,
  val issueDescription: String,
  val registeredDate: String,
  var status: String, // "Registered", "Technician Assigned", "In Progress", "Resolved"
  var currentStep: Int // 1, 2, 3, 4
)

data class StoreLocation(
  val name: String,
  val address: String,
  val workingHours: String,
  val telephone: String
)

data class ProductItem(
  val name: String,
  val originalPrice: Double,
  val salePrice: Double,
  val category: String,
  val iconCode: ImageVector
)

@Composable
fun DashboardScreen(
  onSignOut: () -> Unit,
  onOpenWishlist: () -> Unit,
  modifier: Modifier = Modifier
) {
  var activeOverlay by remember { mutableStateOf(ActiveModule.NONE) }
  var searchQuery by remember { mutableStateOf("") }

  // Shared application states (Simulated Room DB tables)
  val invoicesList = remember {
    mutableStateListOf(
      StoreInvoice("INV-2026-004", listOf("OLED Smart Cinema 65\"", "Wall Mount Bracket"), "June 12, 2026", 949.98, "SN-59A83KD7"),
      StoreInvoice("INV-2026-001", listOf("Inverter AC Dual-Cool"), "April 24, 2026", 499.99, "SN-AC29471X")
    )
  }

  val complaintsList = remember {
    mutableStateListOf(
      ServiceComplaint("CMP-8491", "Triple-Door Aero Fridge", "Icemaker not dispensing ice properly.", "June 20, 2026", "Technician Assigned", 2)
    )
  }

  // Dashboard Grid definition matching image
  val modules = listOf(
    DashboardModuleItem("WISHLIST", Icons.Default.Favorite, "Browse categories you've saved", onClick = onOpenWishlist),
    DashboardModuleItem("MY INVOICES", Icons.Default.Info, "View store receipts & purchases", ActiveModule.MY_INVOICES),
    DashboardModuleItem("TRACK COMPLAINT", Icons.Default.Warning, "Service tickets status", ActiveModule.TRACK_COMPLAINT),
    DashboardModuleItem("STORES", Icons.Default.LocationOn, "Find nearest Electro World branch", ActiveModule.STORES),
    DashboardModuleItem("BUY NOW", Icons.Default.ShoppingCart, "Browse current catalog and shop", ActiveModule.BUY_NOW),
    DashboardModuleItem("CALL CUSTOMER CARE", Icons.Default.Phone, "Ringing support line hotlines", ActiveModule.CALL_CUSTOMER_CARE),
    DashboardModuleItem("MAIL YOUR FEEDBACK", Icons.Default.Email, "Send store review feedback", ActiveModule.MAIL_YOUR_FEEDBACK)
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = SlateBackground,
    topBar = {
      // Custom Dashboard Brand Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(SlateSurface)
          .padding(top = 40.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          ElectroWorldLogo(iconSize = 42f)

          // Header Right controls (Avatar with Status indicator + Logout)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .background(SlateSurfaceVariant, CircleShape)
                .clickable { onSignOut() },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "EW",
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              // Online Indicator dot
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .background(SuccessGreen, CircleShape)
                  .align(Alignment.TopEnd)
                  .border(2.dp, SlateSurface, CircleShape)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Store locator & general catalog search bar mockup
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search services, devices, catalogs...", color = OnSlateTextSecondary, fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, "Search Icon", tint = GoldPrimary, modifier = Modifier.size(20.dp)) },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = SlateSurfaceVariant,
            focusedContainerColor = SlateBackground,
            unfocusedContainerColor = SlateBackground,
            focusedTextColor = OnSlateText,
            unfocusedTextColor = OnSlateText
          ),
          shape = RoundedCornerShape(24.dp),
          singleLine = true
        )
      }
    }
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
        // Welcome User Card Banner
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          colors = CardDefaults.cardColors(containerColor = SlateSurface),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f))
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Welcome Back,",
                color = OnSlateTextSecondary,
                fontSize = 13.sp
              )
              Text(
                text = "+1 (555) 019-2834",
                color = OnSlateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Premium Club Elite Member",
                color = GoldSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
              )
            }

            // Points indicator badge
            Column(
              horizontalAlignment = Alignment.End,
              modifier = Modifier
                .background(SlateSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(10.dp)
            ) {
              Text("PTS BALANCE", color = OnSlateTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
              Text("12,450", color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
          }
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

        // 2x4 Custom Module Grid matching "Vijay Sales" dashboard layout exactly!
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier
            .fillMaxWidth()
            .height(440.dp)
            .padding(horizontal = 12.dp),
          userScrollEnabled = false,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(modules) { item ->
            Card(
              modifier = Modifier
                .fillMaxHeight()
                .clickable { item.onClick?.invoke() ?: run { activeOverlay = item.actionType } }
                .testTag("module_${item.title.lowercase().replace(" ", "_")}"),
              colors = CardDefaults.cardColors(containerColor = SlateSurface),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, SlateSurfaceVariant)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                // Glowy backing for icon
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .background(GoldPrimary.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.2f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = GoldSecondary,
                    modifier = Modifier.size(24.dp)
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = item.title,
                  color = OnSlateText,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = item.subtitle,
                  color = OnSlateTextSecondary,
                  fontSize = 9.sp,
                  textAlign = TextAlign.Center,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Flash Deals Section
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "FLASH DEALS",
            color = GoldPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
          Box(
            modifier = Modifier
              .background(AlertRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "ENDS IN 04:32:15",
              color = AlertRed,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          }
        }

        // Horizontal deals items
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            FlashDealItemCard(
              name = "Samsung 65\" Ultra UHD OLED TV",
              originalPrice = 1399.99,
              salePrice = 899.99,
              discount = "35% OFF",
              tag = "TELEVISION"
            )
          }
          item {
            FlashDealItemCard(
              name = "MacBook Air M3 13\" Space Gray",
              originalPrice = 1099.99,
              salePrice = 849.99,
              discount = "22% OFF",
              tag = "LAPTOP"
            )
          }
          item {
            FlashDealItemCard(
              name = "Sony WF-1000XM5 Active Buds",
              originalPrice = 299.99,
              salePrice = 199.99,
              discount = "33% OFF",
              tag = "AUDIO"
            )
          }
          item {
            FlashDealItemCard(
              name = "Dyson PureCool Air Tower Link",
              originalPrice = 449.99,
              salePrice = 299.99,
              discount = "33% OFF",
              tag = "APPLIANCE"
            )
          }
        }
      }

      // 3. OVERLAY PORTALS FOR THE 8 MODULE PLACEHOLDERS (Fully Custom & Interactive)
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
                    ActiveModule.MY_INVOICES -> MyInvoicesSubScreen(invoices = invoicesList)
                    ActiveModule.TRACK_COMPLAINT -> TrackComplaintSubScreen(complaints = complaintsList)
                    ActiveModule.STORES -> StoresSubScreen()
                    ActiveModule.BUY_NOW -> BuyNowSubScreen()
                    ActiveModule.CALL_CUSTOMER_CARE -> CallCustomerCareSubScreen()
                    ActiveModule.MAIL_YOUR_FEEDBACK -> MailFeedbackSubScreen()
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
fun FlashDealItemCard(
  name: String,
  originalPrice: Double,
  salePrice: Double,
  discount: String,
  tag: String
) {
  Card(
    modifier = Modifier
      .width(170.dp)
      .height(185.dp),
    colors = CardDefaults.cardColors(containerColor = SlateSurface),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, SlateSurfaceVariant)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Small decorative abstract line vector mimicking a product shape
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(72.dp)
          .align(Alignment.TopCenter)
      ) {
        val w = size.width
        val h = size.height
        // Draw diagonal artistic mesh lines representing premium device
        drawCircle(
          color = GoldPrimary.copy(alpha = 0.05f),
          radius = 50f,
          center = Offset(w * 0.8f, h * 0.5f)
        )
        drawLine(
          color = GoldSecondary.copy(alpha = 0.15f),
          start = Offset(0f, h * 0.9f),
          end = Offset(w, h * 0.3f),
          strokeWidth = 2f
        )
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(10.dp)
      ) {
        // Tag & Discount Banner
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = tag,
            color = GoldSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Box(
            modifier = Modifier
              .background(AlertRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = discount,
              color = AlertRed,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Product Name
        Text(
          text = name,
          color = OnSlateText,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Prices Row
        Row(
          verticalAlignment = Alignment.Bottom,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "$${String.format("%.0f", salePrice)}",
            color = GoldPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "$${String.format("%.0f", originalPrice)}",
            color = OnSlateTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            style = TextStyle(
              textDecoration = TextDecoration.LineThrough
            )
          )
        }
      }
    }
  }
}

// Map screen icons
private fun getModuleIcon(module: ActiveModule): ImageVector {
  return when (module) {
    ActiveModule.MY_INVOICES -> Icons.Default.Info
    ActiveModule.TRACK_COMPLAINT -> Icons.Default.Warning
    ActiveModule.STORES -> Icons.Default.LocationOn
    ActiveModule.BUY_NOW -> Icons.Default.ShoppingCart
    ActiveModule.CALL_CUSTOMER_CARE -> Icons.Default.Phone
    ActiveModule.MAIL_YOUR_FEEDBACK -> Icons.Default.Email
    else -> Icons.Default.Info
  }
}

private fun getModuleTitle(module: ActiveModule): String {
  return when (module) {
    ActiveModule.MY_INVOICES -> "Store Invoices & Bills"
    ActiveModule.TRACK_COMPLAINT -> "Service Complaint Center"
    ActiveModule.STORES -> "Electro World Stores"
    ActiveModule.BUY_NOW -> "Catalog Buy Center"
    ActiveModule.CALL_CUSTOMER_CARE -> "Premium Customer Care"
    ActiveModule.MAIL_YOUR_FEEDBACK -> "Review & Feedback Station"
    else -> ""
  }
}

// ==========================================
// INDIVIDUAL MODULES INTERACTIVE UI LAYOUTS
// ==========================================

// 1. MY INVOICES SUB-SCREEN
@Composable
fun MyInvoicesSubScreen(invoices: List<StoreInvoice>) {
  var selectedInvoice by remember { mutableStateOf<StoreInvoice?>(null) }
  var downloadProgress by remember { mutableStateOf<Float?>(null) }
  val scope = rememberCoroutineScope()

  if (selectedInvoice == null) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(invoices) { inv ->
        Card(
          colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { selectedInvoice = inv },
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = inv.id, color = GoldSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(
                text = inv.itemNames.joinToString(", "),
                color = OnSlateText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(180.dp)
              )
              Text(text = inv.purchaseDate, color = OnSlateTextSecondary, fontSize = 11.sp)
            }
            Text(
              text = "$${String.format("%.2f", inv.totalAmount)}",
              color = GoldLight,
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }
    }
  } else {
    // Detailed Invoice Breakdown Sheet View
    val inv = selectedInvoice!!
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      IconButton(
        onClick = { selectedInvoice = null },
        modifier = Modifier.background(SlateSurfaceVariant, CircleShape)
      ) {
        Icon(Icons.Default.ArrowBack, "Back", tint = GoldSecondary)
      }

      Spacer(modifier = Modifier.height(12.dp))

      Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = "OFFICIAL RETAIL RECEIPT", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
          Text(text = "Electro World Store HQ #12", color = OnSlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
          Text(text = "Customer Mobile: +1 (555) 019-2834", color = OnSlateTextSecondary, fontSize = 11.sp)
          Text(text = "Invoice ID: ${inv.id}", color = OnSlateTextSecondary, fontSize = 11.sp)
          Text(text = "Warranty Serial: ${inv.serialNumber}", color = OnSlateTextSecondary, fontSize = 11.sp)

          Divider(color = SlateSurface, modifier = Modifier.padding(vertical = 12.dp))

          // Purchase items break
          inv.itemNames.forEach { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = "1x $item", color = OnSlateText, fontSize = 13.sp)
              Text(text = "$${String.format("%.2f", inv.totalAmount / inv.itemNames.size)}", color = OnSlateTextSecondary, fontSize = 13.sp)
            }
          }

          Divider(color = SlateSurface, modifier = Modifier.padding(vertical = 12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "Sales Tax (6.5%)", color = OnSlateTextSecondary, fontSize = 11.sp)
            Text(text = "$${String.format("%.2f", inv.totalAmount * 0.065)}", color = OnSlateTextSecondary, fontSize = 11.sp)
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "NET TOTAL", color = OnSlateText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = "$${String.format("%.2f", inv.totalAmount)}", color = GoldPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (downloadProgress != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
          LinearProgressIndicator(
            progress = downloadProgress!!,
            color = GoldPrimary,
            trackColor = SlateSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Downloading invoice PDF... ${(downloadProgress!! * 100).toInt()}%",
            color = GoldSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      } else {
        Button(
          onClick = {
            scope.launch {
              downloadProgress = 0.0f
              while (downloadProgress!! < 1.0f) {
                delay(150)
                downloadProgress = downloadProgress!! + 0.15f
              }
              downloadProgress = 1.0f
              delay(300)
              downloadProgress = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Refresh, "Download Icon", tint = SlateBackground, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("DOWNLOAD RECEIPT PDF", color = SlateBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

// 2. TRACK COMPLAINT SUB-SCREEN
@Composable
fun TrackComplaintSubScreen(complaints: List<ServiceComplaint>) {
  var showNewTicketForm by remember { mutableStateOf(false) }
  var applianceName by remember { mutableStateOf("") }
  var problemDesc by remember { mutableStateOf("") }
  var formError by remember { mutableStateOf<String?>(null) }

  if (!showNewTicketForm) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("ACTIVE SERVICE TICKETS", color = OnSlateTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Button(
          onClick = { showNewTicketForm = true },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.15f), contentColor = GoldPrimary),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("+ NEW TICKET", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(complaints) { ticket ->
          Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(text = ticket.id, color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                  text = ticket.status.uppercase(),
                  color = if (ticket.currentStep == 4) SuccessGreen else GoldPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Text(text = ticket.applianceName, color = OnSlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
              Text(text = ticket.issueDescription, color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

              // Stepper visual
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val steps = listOf("Reg", "Assigned", "EnRoute", "Done")
                steps.forEachIndexed { index, name ->
                  val isActive = index + 1 <= ticket.currentStep
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                      modifier = Modifier
                        .size(18.dp)
                        .background(if (isActive) GoldPrimary else SlateSurface, CircleShape)
                        .border(1.dp, if (isActive) GoldPrimary else OnSlateTextSecondary.copy(alpha = 0.3f), CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      if (isActive) {
                        Icon(Icons.Default.Check, "Checked", tint = SlateBackground, modifier = Modifier.size(10.dp))
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = name, color = if (isActive) GoldLight else OnSlateTextSecondary, fontSize = 9.sp)
                  }
                  if (index < steps.size - 1) {
                    Divider(
                      color = if (index + 2 <= ticket.currentStep) GoldPrimary else OnSlateTextSecondary.copy(alpha = 0.2f),
                      modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 12.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  } else {
    // New Complaint register Form
    Column(modifier = Modifier.fillMaxSize()) {
      IconButton(onClick = { showNewTicketForm = false }) {
        Icon(Icons.Default.ArrowBack, "Back", tint = GoldSecondary)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text("File a Support Request", color = OnSlateText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Text("Tell our smart repair hub what is wrong. A service representative will coordinate service details.", color = OnSlateTextSecondary, fontSize = 12.sp)

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = applianceName,
        onValueChange = { applianceName = it },
        placeholder = { Text("Appliance name (e.g. Smart TV)", color = OnSlateTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant,
          focusedContainerColor = SlateSurface,
          unfocusedContainerColor = SlateSurface
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = problemDesc,
        onValueChange = { problemDesc = it },
        placeholder = { Text("What seems to be the problem?", color = OnSlateTextSecondary) },
        modifier = Modifier
          .fillMaxWidth()
          .height(100.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant,
          focusedContainerColor = SlateSurface,
          unfocusedContainerColor = SlateSurface
        ),
        maxLines = 4
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          if (applianceName.isBlank() || problemDesc.isBlank()) {
            formError = "Please fill in all complaint details."
          } else {
            val randomNum = (1000..9999).random()
            (complaints as MutableList).add(
              ServiceComplaint("CMP-$randomNum", applianceName, problemDesc, "June 26, 2026", "Registered", 1)
            )
            showNewTicketForm = false
            applianceName = ""
            problemDesc = ""
            formError = null
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("SUBMIT TICKET TO ELECTRO REPAIRS", color = SlateBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }

      if (formError != null) {
        Text(text = formError!!, color = AlertRed, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
      }
    }
  }
}

// 3. STORES SUB-SCREEN
@Composable
fun StoresSubScreen() {
  val branches = listOf(
    StoreLocation("Electro World Downtown Plaza", "104 Main Boulevard, Suite A, Metropolis", "10:00 AM - 09:30 PM", "+1 (800) 555-0144"),
    StoreLocation("EW Express - Galleria Mall", "Floor 2, Galleria Shopping Hub, Heights District", "11:00 AM - 09:00 PM", "+1 (800) 555-0199"),
    StoreLocation("Electro World Smart Megastore", "Suite 400, Industrial Ring Road, Southside", "09:00 AM - 10:00 PM", "+1 (800) 555-0211")
  )

  Column(modifier = Modifier.fillMaxSize()) {
    // Fake Slate styled Map Grid Canvas
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(110.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(SlateSurfaceVariant)
        .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw slate grid grids to mock mapping lines
        drawLine(color = OnSlateTextSecondary.copy(alpha = 0.1f), start = Offset(w * 0.2f, 0f), end = Offset(w * 0.2f, h), strokeWidth = 2f)
        drawLine(color = OnSlateTextSecondary.copy(alpha = 0.1f), start = Offset(w * 0.5f, 0f), end = Offset(w * 0.5f, h), strokeWidth = 2f)
        drawLine(color = OnSlateTextSecondary.copy(alpha = 0.1f), start = Offset(w * 0.8f, 0f), end = Offset(w * 0.8f, h), strokeWidth = 2f)
        drawLine(color = OnSlateTextSecondary.copy(alpha = 0.1f), start = Offset(0f, h * 0.3f), end = Offset(w, h * 0.3f), strokeWidth = 2f)
        drawLine(color = OnSlateTextSecondary.copy(alpha = 0.1f), start = Offset(0f, h * 0.7f), end = Offset(w, h * 0.7f), strokeWidth = 2f)

        // Draw custom gold pin indicators
        drawCircle(color = GoldPrimary, radius = 8f, center = Offset(w * 0.3f, h * 0.4f))
        drawCircle(color = GoldSecondary, radius = 6f, center = Offset(w * 0.55f, h * 0.65f))
        drawCircle(color = GoldLight, radius = 8f, center = Offset(w * 0.78f, h * 0.25f))
      }
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .background(Color(0xCC1E2022), RoundedCornerShape(6.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Icon(Icons.Default.LocationOn, "GPS", tint = GoldPrimary, modifier = Modifier.size(10.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("3 PLACES LOCATED NEARBY", color = OnSlateText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      items(branches) { store ->
        Card(
          colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
          shape = RoundedCornerShape(10.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = store.name, color = GoldSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = store.address, color = OnSlateText, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "Hours: ${store.workingHours}", color = OnSlateTextSecondary, fontSize = 10.sp)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, "Call store", tint = GoldPrimary, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = store.telephone, color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

// 4. BUY NOW SUB-SCREEN
@Composable
fun BuyNowSubScreen() {
  val products = listOf(
    ProductItem("Apex Console X - Gold Edition", 599.99, 499.99, "Gaming", Icons.Default.ShoppingCart),
    ProductItem("EW Soundcore Bar 5.1 Dolby", 249.99, 179.99, "Audio", Icons.Default.Refresh),
    ProductItem("Smart Hub Touch Controller", 129.99, 89.99, "Smarthome", Icons.Default.Settings),
    ProductItem("EW Pro Air Purifier V2", 349.99, 269.99, "Appliances", Icons.Default.Info)
  )

  var activeCheckoutItem by remember { mutableStateOf<ProductItem?>(null) }
  var checkoutSuccess by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  if (activeCheckoutItem == null) {
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(products) { item ->
        Card(
          colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .background(SlateSurface, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(item.iconCode, "Product", tint = GoldPrimary, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(text = item.name, color = OnSlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text(text = item.category, color = OnSlateTextSecondary, fontSize = 10.sp)
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$${item.salePrice}", color = GoldSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "$${item.originalPrice}",
                  color = OnSlateTextSecondary,
                  fontSize = 11.sp,
                  style = TextStyle(
                    textDecoration = TextDecoration.LineThrough
                  )
                )
              }
            }

            Button(
              onClick = { activeCheckoutItem = item },
              colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("BUY", fontSize = 11.sp, color = SlateBackground, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  } else {
    // Simulated Checkout Dialog
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (!checkoutSuccess) {
        Icon(Icons.Default.ShoppingCart, "Checkout", tint = GoldSecondary, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Confirm Your Order", color = OnSlateText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Are you sure you want to purchase the '${activeCheckoutItem!!.name}' for a special promo pricing of $${activeCheckoutItem!!.salePrice}?",
          color = OnSlateTextSecondary,
          fontSize = 13.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(16.dp),
          lineHeight = 18.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = { activeCheckoutItem = null },
            colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceVariant, contentColor = OnSlateText)
          ) {
            Text("CANCEL")
          }

          Button(
            onClick = {
              scope.launch {
                checkoutSuccess = true
                delay(2000)
                checkoutSuccess = false
                activeCheckoutItem = null
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
          ) {
            Text("AUTHORIZE CHECKOUT", color = SlateBackground, fontWeight = FontWeight.Bold)
          }
        }
      } else {
        Icon(Icons.Default.Check, "Order Placed", tint = SuccessGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text("Transaction Authorized!", color = SuccessGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Your receipt has been compiled in 'MY INVOICES'.", color = OnSlateTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
      }
    }
  }
}

// 5. CALL CUSTOMER CARE SUB-SCREEN
@Composable
fun CallCustomerCareSubScreen() {
  var isCalling by remember { mutableStateOf(false) }
  var callDuration by remember { mutableStateOf(0) }
  var isMuted by remember { mutableStateOf(false) }
  var isSpeaker by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(isCalling) {
    if (isCalling) {
      callDuration = 0
      while (isCalling) {
        delay(1000)
        callDuration += 1
      }
    }
  }

  if (!isCalling) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(Icons.Default.Phone, "Phone care", tint = GoldSecondary, modifier = Modifier.size(56.dp))
      Spacer(modifier = Modifier.height(14.dp))
      Text("Support Line Direct Call", color = OnSlateText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Text(
        text = "Speak immediately with our priority care desk to handle appliance setup, delivery coordinates, or repair tickets.",
        color = OnSlateTextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = { isCalling = true },
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(0.8f)
      ) {
        Text("DIAL PRIORITY HOTLINE", color = SlateBackground, fontWeight = FontWeight.Bold)
      }
    }
  } else {
    // Beautiful calling overlay panel
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(SlateSurface),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Spacer(modifier = Modifier.height(20.dp))

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .background(GoldPrimary.copy(alpha = 0.1f), CircleShape)
            .border(2.dp, GoldPrimary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text("EW", color = GoldSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Sarah, Senior Support Supervisor", color = OnSlateText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
          text = if (callDuration == 0) "Dialing & Securing Gateway..." else "Active Support Call: ${String.format("%02d:%02d", callDuration / 60, callDuration % 60)}",
          color = GoldSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // Live controls row
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(20.dp)
      ) {
        // Mute
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier.background(if (isMuted) GoldPrimary else SlateSurfaceVariant, CircleShape)
          ) {
            Icon(Icons.Default.Refresh, "Mute", tint = if (isMuted) SlateBackground else OnSlateText)
          }
          Text("MUTE", color = OnSlateTextSecondary, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
        }

        // Speaker
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          IconButton(
            onClick = { isSpeaker = !isSpeaker },
            modifier = Modifier.background(if (isSpeaker) GoldPrimary else SlateSurfaceVariant, CircleShape)
          ) {
            Icon(Icons.Default.LocationOn, "Speaker", tint = if (isSpeaker) SlateBackground else OnSlateText)
          }
          Text("SPEAKER", color = OnSlateTextSecondary, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
        }
      }

      Button(
        onClick = { isCalling = false },
        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
        shape = CircleShape,
        modifier = Modifier
          .size(56.dp)
          .padding(bottom = 12.dp),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(Icons.Default.Close, "Hang up", tint = OnSlateText)
      }
    }
  }
}

// 6. MAIL YOUR FEEDBACK SUB-SCREEN
@Composable
fun MailFeedbackSubScreen() {
  var activeRating by remember { mutableStateOf(5) }
  var feedbackText by remember { mutableStateOf("") }
  var feedbackCategory by remember { mutableStateOf("App Experience") }
  var feedbackSuccess by remember { mutableStateOf(false) }

  val feedbackCats = listOf("App Experience", "Store Service", "Product Range", "Warranty Care")

  if (!feedbackSuccess) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      Text("Rate Your Electro Experience", color = OnSlateText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
      Text("Your reviews are read daily by our regional executive team to optimize client services.", color = OnSlateTextSecondary, fontSize = 12.sp)

      Spacer(modifier = Modifier.height(14.dp))

      // Star system
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
      ) {
        for (i in 1..5) {
          val isActive = i <= activeRating
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Star $i",
            tint = if (isActive) GoldPrimary else SlateSurfaceVariant,
            modifier = Modifier
              .size(38.dp)
              .clickable { activeRating = i }
              .padding(horizontal = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text("Focus Area", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        feedbackCats.forEach { cat ->
          val selected = feedbackCategory == cat
          Box(
            modifier = Modifier
              .background(if (selected) GoldPrimary else SlateSurfaceVariant, RoundedCornerShape(16.dp))
              .clickable { feedbackCategory = cat }
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(text = cat, color = if (selected) SlateBackground else OnSlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = feedbackText,
        onValueChange = { feedbackText = it },
        placeholder = { Text("What can we do to improve or what did you love?", color = OnSlateTextSecondary) },
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant,
          focusedContainerColor = SlateSurface,
          unfocusedContainerColor = SlateSurface
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          feedbackSuccess = true
        },
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("SEND SECURE FEEDBACK", color = SlateBackground, fontWeight = FontWeight.Bold)
      }
    }
  } else {
    // Success feedback view
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(Icons.Default.Check, "Thank you", tint = SuccessGreen, modifier = Modifier.size(56.dp))
      Spacer(modifier = Modifier.height(12.dp))
      Text("Thank You For Your Feedback!", color = OnSlateText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      Text(
        text = "Your submission has been dispatched directly to the support optimization hub.",
        color = OnSlateTextSecondary,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      TextButton(onClick = {
        feedbackSuccess = false
        feedbackText = ""
        activeRating = 5
      }) {
        Text("WRITE ANOTHER SUBMISSION", color = GoldSecondary, fontWeight = FontWeight.Bold)
      }
    }
  }
}

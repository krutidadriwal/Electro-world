package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.ui.components.ElectroWorldLogo
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

// Screen Enumeration for dialog overlays
enum class ActiveModule {
  NONE,
  MY_INVOICES,
  VISIT_STORE
}

@Composable
fun DashboardScreen(
  userName: String,
  userPhone: String,
  onSignOut: () -> Unit,
  onOpenWishlist: () -> Unit,
  onOpenRegisterComplaint: () -> Unit,
  onOpenInstallation: () -> Unit,
  modifier: Modifier = Modifier
) {
  var activeOverlay by remember { mutableStateOf(ActiveModule.NONE) }

  // Dashboard Grid definition: left/right pairs per row
  val modules = listOf(
    DashboardModuleItem("MY INVOICES", Icons.Default.Info, "View store receipts & purchases", ActiveModule.MY_INVOICES),
    DashboardModuleItem("VISIT STORE", Icons.Default.LocationOn, "Address, map & contact", ActiveModule.VISIT_STORE),
    DashboardModuleItem("REGISTER COMPLAINT", Icons.Default.Warning, "Report a service issue", onClick = onOpenRegisterComplaint),
    DashboardModuleItem("WISHLIST", Icons.Default.Favorite, "Browse categories you've saved", onClick = onOpenWishlist),
    DashboardModuleItem("INSTALLATION REQUEST", Icons.Default.Settings, "Schedule a new installation", onClick = onOpenInstallation)
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
                text = userName.ifBlank { "Guest" },
                color = OnSlateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "+91 $userPhone",
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

        // 2-column module grid
        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier
            .fillMaxWidth()
            .height(370.dp)
            .padding(horizontal = 12.dp),
          userScrollEnabled = false,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(modules) { item ->
            Card(
              modifier = Modifier
                .height(118.dp)
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
                    ActiveModule.MY_INVOICES -> MyInvoicesSubScreen(userPhone = userPhone)
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

// Map screen icons
private fun getModuleIcon(module: ActiveModule): ImageVector {
  return when (module) {
    ActiveModule.MY_INVOICES -> Icons.Default.Info
    ActiveModule.VISIT_STORE -> Icons.Default.LocationOn
    else -> Icons.Default.Info
  }
}

private fun getModuleTitle(module: ActiveModule): String {
  return when (module) {
    ActiveModule.MY_INVOICES -> "Store Invoices & Bills"
    ActiveModule.VISIT_STORE -> "Visit Our Store"
    else -> ""
  }
}

// ==========================================
// INDIVIDUAL MODULES INTERACTIVE UI LAYOUTS
// ==========================================

// 1. MY INVOICES SUB-SCREEN
@Composable
fun MyInvoicesSubScreen(userPhone: String) {
  var invoices by remember { mutableStateOf<List<com.example.data.network.InvoiceFile>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val context = LocalContext.current

  LaunchedEffect(userPhone) {
    isLoading = true
    errorMessage = null
    try {
      invoices = com.example.data.network.NetworkModule.userApi.getInvoices(userPhone).invoices
    } catch (e: Exception) {
      errorMessage = "Unable to load invoices. Please check your connection and try again."
    } finally {
      isLoading = false
    }
  }

  when {
    isLoading -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        androidx.compose.material3.CircularProgressIndicator(color = GoldPrimary)
      }
    }
    errorMessage != null -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Default.Warning, "Error", tint = GoldSecondary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(errorMessage ?: "", color = OnSlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
      }
    }
    invoices.isEmpty() -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Default.Info, "No invoices", tint = GoldSecondary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("No invoices found for your account yet.", color = OnSlateTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
      }
    }
    else -> {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(invoices) { inv ->
          Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val url = "${com.example.BuildConfig.SERVER_BASE_URL}/api/invoice-file?id=${inv.id}&phone=$userPhone"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
              },
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
                  Text(text = it, color = OnSlateTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
              }
              Icon(Icons.Default.Description, "Open PDF", tint = GoldPrimary, modifier = Modifier.size(20.dp))
            }
          }
        }
      }
    }
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

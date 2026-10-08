package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.NetworkModule
import com.example.data.network.PointsHistoryEntry
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant

/**
 * Full-screen rewards home: gold-gradient balance card + point history list,
 * matching the header/back pattern used by MyInvoicesScreen/ProfileScreen.
 */
@Composable
fun PointsScreen(
  userPhone: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  var balance by remember { mutableStateOf<Int?>(null) }
  var history by remember { mutableStateOf<List<PointsHistoryEntry>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(userPhone) {
    try {
      val response = NetworkModule.userApi.getPoints(userPhone)
      balance = response.balance
      history = response.history
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Unable to load your points. Please check your connection and try again."
    } finally {
      isLoading = false
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = SlateBackground,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SlateSurface)
          .padding(top = 40.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack, modifier = Modifier.background(SlateSurfaceVariant, CircleShape)) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldSecondary)
        }
        Text(
          text = "My Points",
          color = OnSlateText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 12.dp)
        )
      }
    }
  ) { paddingValues ->
    when {
      isLoading -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(paddingValues),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = GoldPrimary)
        }
      }
      errorMessage != null -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(vertical = 60.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.Warning, "Error", tint = GoldSecondary, modifier = Modifier.height(40.dp))
          Text(
            text = errorMessage ?: "",
            color = OnSlateTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
          )
        }
      }
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(paddingValues),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item { PointsBalanceCard(balance = balance ?: 0) }

          item {
            Text(
              text = "POINT HISTORY",
              color = GoldPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.sp,
              modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
            )
          }

          if (history.isEmpty()) {
            item {
              Text(
                text = "No points earned yet.",
                color = OnSlateTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 20.dp)
              )
            }
          } else {
            items(history, key = { it.id }) { entry ->
              PointsHistoryRow(entry = entry)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PointsBalanceCard(balance: Int) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        brush = Brush.linearGradient(listOf(GoldGradientStart, GoldGradientEnd)),
        shape = RoundedCornerShape(20.dp)
      )
      .padding(24.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "Available points",
        color = Color(0xCC1E2022),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = balance.toString(),
        color = SlateBackground,
        fontSize = 40.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 6.dp)
      )
    }
  }
}

// Server timestamps are full ISO-8601 (e.g. "2024-05-01T12:34:56.789Z");
// history rows only need the date, as DD-MM-YYYY -- matches
// DashboardScreen's formatInvoiceDate (duplicated here since that one is
// file-private).
private fun formatPointsDate(isoTimestamp: String): String {
  val datePart = isoTimestamp.substringBefore('T')
  val parts = datePart.split("-")
  return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else isoTimestamp
}

@Composable
private fun PointsHistoryRow(entry: PointsHistoryEntry) {
  Card(
    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .background(GoldPrimary.copy(alpha = 0.15f), CircleShape)
            .padding(8.dp)
        ) {
          Icon(Icons.Default.Star, contentDescription = null, tint = GoldSecondary, modifier = Modifier.height(16.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
          Text(text = entry.reason, color = OnSlateText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Text(text = formatPointsDate(entry.createdAt), color = OnSlateTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
      }
      Text(
        text = if (entry.points >= 0) "+${entry.points}" else entry.points.toString(),
        color = if (entry.points >= 0) GoldSecondary else OnSlateTextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

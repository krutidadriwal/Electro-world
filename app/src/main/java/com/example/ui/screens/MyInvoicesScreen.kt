package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant

/**
 * Full-screen home for Store Invoices & Bills, matching the header/back
 * pattern used by Wishlist, Register Complaint, and Installation Request
 * instead of being shown as a modal overlay over the dashboard.
 */
@Composable
fun MyInvoicesScreen(
  userPhone: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

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
          text = "Store Invoices & Bills",
          color = OnSlateText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 12.dp)
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp)
    ) {
      MyInvoicesSubScreen(userPhone = userPhone)
    }
  }
}

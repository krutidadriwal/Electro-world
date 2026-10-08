package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Swatch1Charcoal
import com.example.ui.theme.Swatch2Paper
import com.example.ui.theme.Swatch3Amber
import com.example.ui.theme.Swatch6DeepAmber

// VISIT STORE sub-screen, rendered as an overlay inside DashboardScreen.
// Explicit (theme-independent) palette per an exact design spec -- every
// color below is a literal swatch, not a MaterialTheme role, so this reads
// identically in light and dark mode.
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
    // Store Info Box: same Swatch2Paper as the modal and header bar.
    Card(
      colors = CardDefaults.cardColors(containerColor = Swatch2Paper),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(text = storeName, color = Swatch3Amber, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = storeAddress, color = Swatch1Charcoal, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
        Text(text = "Hours: $storeWorkingHours", color = Swatch1Charcoal, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
          Icon(Icons.Default.Phone, "Phone", tint = Swatch3Amber, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = storePhone, color = Swatch1Charcoal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Primary CTA
    Button(
      onClick = {
        val uri = Uri.parse("geo:$storeLatitude,$storeLongitude?q=$storeLatitude,$storeLongitude($storeName)")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
      },
      colors = ButtonDefaults.buttonColors(containerColor = Swatch6DeepAmber),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.LocationOn, "Map", tint = Swatch1Charcoal, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("VIEW ON MAP", color = Swatch1Charcoal, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Secondary CTA: outlined, not filled
    Button(
      onClick = {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$storePhone")))
      },
      colors = ButtonDefaults.buttonColors(containerColor = Swatch2Paper, contentColor = Swatch1Charcoal),
      border = BorderStroke(1.dp, Swatch1Charcoal),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.Phone, "Call", tint = Swatch1Charcoal, modifier = Modifier.width(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("CALL STORE", color = Swatch1Charcoal, fontWeight = FontWeight.Bold)
    }
  }
}

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
import androidx.compose.material.icons.automirrored.filled.Chat
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
import com.example.ui.theme.CrispPaper
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.PaleGold
import com.example.ui.theme.VividAmber

// VISIT STORE sub-screen, rendered as an overlay inside DashboardScreen.
// Every color below is a literal palette color (see Color.kt), not a
// MaterialTheme role.
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
    // Store Info Box: Obsidian, inset into the Deep Slate modal card.
    Card(
      colors = CardDefaults.cardColors(containerColor = ObsidianCharcoal),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(text = storeName, color = VividAmber, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = storeAddress, color = CrispPaper, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
        Text(text = "Hours: $storeWorkingHours", color = CrispPaper, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
          Icon(Icons.Default.Phone, "Phone", tint = VividAmber, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = storePhone, color = CrispPaper, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
      colors = ButtonDefaults.buttonColors(containerColor = VividAmber),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.LocationOn, "Map", tint = ObsidianCharcoal, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("VIEW ON MAP", color = ObsidianCharcoal, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Secondary CTA: outlined, not filled
    Button(
      onClick = {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$storePhone")))
      },
      colors = ButtonDefaults.buttonColors(containerColor = ObsidianCharcoal, contentColor = CrispPaper),
      border = BorderStroke(1.dp, PaleGold),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.Default.Phone, "Call", tint = PaleGold, modifier = Modifier.width(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("CALL STORE", color = CrispPaper, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(10.dp))

    // WhatsApp CTA: same outlined style and number as Call Store.
    Button(
      onClick = {
        val digits = storePhone.filter { it.isDigit() }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")))
      },
      colors = ButtonDefaults.buttonColors(containerColor = ObsidianCharcoal, contentColor = CrispPaper),
      border = BorderStroke(1.dp, PaleGold),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp)
    ) {
      Icon(Icons.AutoMirrored.Filled.Chat, "WhatsApp", tint = PaleGold, modifier = Modifier.width(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("WHATSAPP STORE", color = CrispPaper, fontWeight = FontWeight.Bold)
    }
  }
}

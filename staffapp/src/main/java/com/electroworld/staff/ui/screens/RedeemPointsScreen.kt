package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Admin-only tab (listed under ADMIN in HomeScreen). Placeholder until
// redeeming customers' points is built.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemPointsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Redeem Points") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        Icons.Default.Redeem,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
      )
      Text(
        "Coming soon",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 16.dp)
      )
      Text(
        "Redeeming customer points will be available here.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }
}

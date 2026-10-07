package com.electroworld.staff.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  email: String,
  role: String,
  onBack: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Profile") },
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
      modifier = Modifier
        .padding(padding)
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top
    ) {
      Spacer(modifier = Modifier.height(24.dp))
      // Same avatar treatment as the dashboard header (HomeScreen.kt) --
      // initials on a surface-colored circle -- so the profile screen you
      // land on from tapping that avatar doesn't show a different, generic
      // placeholder icon instead.
      Box(
        modifier = Modifier.size(96.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = email.take(2).ifBlank { "EW" }.uppercase(),
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold,
          fontSize = 28.sp
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      // Explicit onBackground (not left to default/ambient resolution,
      // which wasn't rendering visibly in either theme) -- Paper in dark
      // mode, Charcoal in light, matching Theme.kt's onBackground mapping.
      Text(
        email,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        role.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary
      )
      Spacer(modifier = Modifier.height(32.dp))
      Button(
        onClick = onLogout,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.onBackground,
          contentColor = MaterialTheme.colorScheme.background
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Log out")
      }
    }
  }
}

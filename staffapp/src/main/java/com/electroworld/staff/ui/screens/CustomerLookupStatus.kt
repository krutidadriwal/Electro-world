package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Shared by the Wishlist/Complaints/Installations "add" dialogs: shows
// where the phone-number lookup landed, right under the phone field, so
// it's obvious *before* staff reaches the name field whether they need to
// type one or it's already been filled in and locked.
@Composable
fun CustomerLookupStatus(phone: String, isLookingUp: Boolean, customerExists: Boolean) {
  if (phone.length != 10) return
  when {
    isLookingUp -> Text(
      "Checking...",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(top = 4.dp)
    )
    customerExists -> Text(
      "Customer exists",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(top = 4.dp)
    )
    else -> Text(
      "New customer -- name required",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.error,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

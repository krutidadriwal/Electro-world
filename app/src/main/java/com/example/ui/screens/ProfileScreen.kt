package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.DeleteUserRequest
import com.example.data.network.NetworkModule
import com.example.data.network.UpdateUserRequest
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import kotlinx.coroutines.launch

/**
 * Account settings: view identity, edit the display name, or permanently
 * delete the account. Phone/PIN aren't editable here -- phone is the login
 * identifier and PIN already has its own OTP-backed forgot-PIN flow.
 */
@Composable
fun ProfileScreen(
  userName: String,
  userPhone: String,
  onBack: () -> Unit,
  onNameUpdated: (String) -> Unit,
  onAccountDeleted: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isEditing by remember { mutableStateOf(false) }
  var editedName by remember { mutableStateOf(userName) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var isDeleting by remember { mutableStateOf(false) }
  val coroutineScope = rememberCoroutineScope()

  fun navigateBack() {
    when {
      isEditing -> { isEditing = false; errorMessage = null }
      else -> onBack()
    }
  }

  BackHandler(onBack = ::navigateBack)

  fun saveName() {
    if (editedName.trim().isEmpty()) {
      errorMessage = "Name cannot be empty."
      return
    }
    isSaving = true
    errorMessage = null
    coroutineScope.launch {
      try {
        val response = NetworkModule.userApi.updateUser(UpdateUserRequest(phone = userPhone, name = editedName.trim()))
        isSaving = false
        isEditing = false
        onNameUpdated(response.name)
      } catch (e: Exception) {
        isSaving = false
        errorMessage = "Unable to save changes. Please try again."
      }
    }
  }

  fun deleteAccount() {
    isDeleting = true
    coroutineScope.launch {
      try {
        NetworkModule.userApi.deleteUser(DeleteUserRequest(phone = userPhone))
        isDeleting = false
        showDeleteConfirm = false
        onAccountDeleted()
      } catch (e: Exception) {
        isDeleting = false
        errorMessage = "Unable to delete account. Please try again."
      }
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
        IconButton(onClick = ::navigateBack, modifier = Modifier.background(SlateSurfaceVariant, CircleShape)) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldSecondary)
        }
        Text(
          text = "Profile",
          color = OnSlateText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 12.dp)
        )
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(20.dp)
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .align(Alignment.CenterHorizontally)
          .background(SlateSurfaceVariant, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = userName.take(2).ifBlank { "EW" }.uppercase(),
          color = GoldPrimary,
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      if (isEditing) {
        Text(
          text = "FULL NAME",
          color = OnSlateTextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
          value = editedName,
          onValueChange = { editedName = it; errorMessage = null },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = SlateSurfaceVariant,
            focusedContainerColor = SlateSurface,
            unfocusedContainerColor = SlateSurface,
            focusedTextColor = OnSlateText,
            unfocusedTextColor = OnSlateText
          ),
          shape = RoundedCornerShape(12.dp)
        )

        errorMessage?.let {
          Text(text = it, color = AlertRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          TextButton(onClick = { isEditing = false; editedName = userName; errorMessage = null }) {
            Text("Cancel", color = OnSlateTextSecondary)
          }
          Button(
            onClick = { saveName() },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldDark),
            shape = RoundedCornerShape(10.dp)
          ) {
            if (isSaving) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SlateBackground, strokeWidth = 2.dp)
            } else {
              Text("Save", color = SlateBackground, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      } else {
        Text(
          text = userName.ifBlank { "Guest" },
          color = OnSlateText,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
          text = "+91 $userPhone",
          color = OnSlateTextSecondary,
          fontSize = 13.sp,
          modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ProfileActionRow(
          icon = Icons.Default.Edit,
          label = "Edit Profile",
          onClick = { isEditing = true; editedName = userName }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(12.dp))

        ProfileActionRow(
          icon = Icons.Default.Logout,
          label = "Sign Out",
          onClick = onSignOut
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileActionRow(
          icon = Icons.Default.DeleteForever,
          label = "Delete Account",
          destructive = true,
          onClick = { showDeleteConfirm = true }
        )

        errorMessage?.let {
          Text(text = it, color = AlertRed, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
      containerColor = SlateSurface,
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed) },
      title = { Text("Delete Account?", color = OnSlateText, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "This permanently deletes your account, complaints, wishlist, and installation requests. This cannot be undone.",
          color = OnSlateTextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        TextButton(onClick = { deleteAccount() }, enabled = !isDeleting) {
          if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AlertRed, strokeWidth = 2.dp)
          } else {
            Text("Delete", color = AlertRed, fontWeight = FontWeight.Bold)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }, enabled = !isDeleting) {
          Text("Cancel", color = OnSlateTextSecondary)
        }
      }
    )
  }
}

@Composable
private fun ProfileActionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  destructive: Boolean = false,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = SlateSurface),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (destructive) AlertRed else GoldPrimary,
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = label,
        color = if (destructive) AlertRed else OnSlateText,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 14.dp).weight(1f)
      )
      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = OnSlateTextSecondary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

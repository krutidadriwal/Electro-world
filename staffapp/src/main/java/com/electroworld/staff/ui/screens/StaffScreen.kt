package com.electroworld.staff.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroworld.staff.data.network.CreateStaffRequest
import com.electroworld.staff.data.network.DeleteStaffRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.StaffMember
import com.electroworld.staff.data.network.UpdateStaffRoleRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  var staff by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var showAddDialog by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<StaffMember?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var isRefreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      staff = NetworkModule.staffApi.list().staff
    } catch (e: Exception) {
      // Keep the previous list on failure.
    } finally {
      isLoading = false
      isRefreshing = false
    }
  }

  if (showAddDialog) {
    AddStaffDialog(
      onDismiss = { showAddDialog = false },
      onCreated = {
        showAddDialog = false
        refreshTrigger++
      }
    )
  }

  pendingDelete?.let { member ->
    DeleteStaffDialog(
      member = member,
      onDismiss = { pendingDelete = null },
      onDeleted = {
        pendingDelete = null
        refreshTrigger++
      }
    )
  }

  androidx.compose.material3.Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Staff") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = { showAddDialog = true }) {
        Icon(Icons.Default.Add, contentDescription = "Add staff")
      }
    },
    modifier = modifier
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      if (isLoading && staff.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
      } else StaffPullToRefresh(isRefreshing = isRefreshing, onRefresh = { isRefreshing = true; refreshTrigger++ }) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
          items(staff) { member ->
            StaffRow(
              member,
              onRoleChange = { newRole ->
                scope.launch {
                  try {
                    NetworkModule.staffApi.updateRole(UpdateStaffRoleRequest(member.id, newRole))
                    refreshTrigger++
                  } catch (e: Exception) {
                    // Silently ignore -- the row will just not update; the
                    // admin can retry.
                  }
                }
              },
              onDelete = { pendingDelete = member }
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffRow(member: StaffMember, onRoleChange: (String) -> Unit, onDelete: () -> Unit) {
  var menuExpanded by remember { mutableStateOf(false) }
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
        if (member.displayName != member.email) {
          SingleLineShrinkingText(
            member.email,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
        Text(member.role, style = MaterialTheme.typography.labelMedium)
      }
      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(Icons.Default.ManageAccounts, contentDescription = "Change role or delete")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
          listOf("admin", "employee").forEach { role ->
            DropdownMenuItem(
              text = { Text(role) },
              onClick = {
                menuExpanded = false
                if (role != member.role) onRoleChange(role)
              }
            )
          }
          HorizontalDivider()
          DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
              Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            onClick = {
              menuExpanded = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

// Keeps text on one line by stepping the font size down until it fits,
// instead of wrapping or ellipsizing -- used for emails, which are only
// useful when shown in full. Drawing is skipped until the final size is
// found so the oversized intermediate layouts never flash on screen.
@Composable
private fun SingleLineShrinkingText(
  text: String,
  style: TextStyle,
  color: Color,
  minFontSize: TextUnit = 8.sp
) {
  var fontSize by remember(text) { mutableStateOf(style.fontSize) }
  var readyToDraw by remember(text) { mutableStateOf(false) }
  Text(
    text,
    style = style,
    color = color,
    fontSize = fontSize,
    maxLines = 1,
    softWrap = false,
    onTextLayout = { result ->
      if (result.hasVisualOverflow && fontSize > minFontSize) {
        val smaller = fontSize * 0.9f
        fontSize = if (smaller < minFontSize) minFontSize else smaller
      } else {
        readyToDraw = true
      }
    },
    modifier = Modifier.drawWithContent { if (readyToDraw) drawContent() }
  )
}

@Composable
private fun DeleteStaffDialog(member: StaffMember, onDismiss: () -> Unit, onDeleted: () -> Unit) {
  var isDeleting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = { if (!isDeleting) onDismiss() },
    title = { Text("Delete staff member?") },
    text = {
      Column {
        Text(
          "${member.displayName} will be removed from the system and their login will stop working. " +
            "This can't be undone."
        )
        errorMessage?.let {
          Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isDeleting,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError
        ),
        onClick = {
          errorMessage = null
          isDeleting = true
          scope.launch {
            try {
              val response = NetworkModule.staffApi.delete(DeleteStaffRequest(member.id))
              if (response.isSuccessful) onDeleted() else errorMessage = "Failed to delete staff member."
            } catch (e: Exception) {
              errorMessage = "Failed to delete staff member."
            } finally {
              isDeleting = false
            }
          }
        }
      ) {
        if (isDeleting) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Delete")
      }
    },
    dismissButton = { TextButton(enabled = !isDeleting, onClick = onDismiss) { Text("Cancel") } }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("employee") }
  var roleMenuExpanded by remember { mutableStateOf(false) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add staff member") },
    text = {
      Column {
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Temporary password") },
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Box(modifier = Modifier.padding(top = 8.dp)) {
          OutlinedTextField(
            value = role,
            onValueChange = {},
            readOnly = true,
            label = { Text("Role") },
            modifier = Modifier.fillMaxWidth()
          )
          Box(
            modifier = Modifier
              .matchParentSize()
              .clickable { roleMenuExpanded = true }
          )
          DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
            listOf("admin", "employee").forEach { option ->
              DropdownMenuItem(text = { Text(option) }, onClick = { role = option; roleMenuExpanded = false })
            }
          }
        }
        errorMessage?.let {
          Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isSaving && email.isNotBlank() && password.length >= 8,
        onClick = {
          errorMessage = null
          isSaving = true
          scope.launch {
            try {
              NetworkModule.staffApi.create(CreateStaffRequest(email.trim(), password, role))
              onCreated()
            } catch (e: Exception) {
              errorMessage = "Failed to create staff member."
            } finally {
              isSaving = false
            }
          }
        }
      ) {
        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Add")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.CreateStaffRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.StaffMember
import com.electroworld.staff.data.network.UpdateStaffRoleRequest
import kotlinx.coroutines.launch

@Composable
fun StaffScreen(modifier: Modifier = Modifier) {
  var staff by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var showAddDialog by remember { mutableStateOf(false) }
  var refreshTrigger by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      staff = NetworkModule.staffApi.list().staff
    } catch (e: Exception) {
      // Keep the previous list on failure.
    } finally {
      isLoading = false
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

  androidx.compose.material3.Scaffold(
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
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
          items(staff) { member ->
            StaffRow(member) { newRole ->
              scope.launch {
                try {
                  NetworkModule.staffApi.updateRole(UpdateStaffRoleRequest(member.id, newRole))
                  refreshTrigger++
                } catch (e: Exception) {
                  // Silently ignore -- the row will just not update; the
                  // admin can retry.
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffRow(member: StaffMember, onRoleChange: (String) -> Unit) {
  var menuExpanded by remember { mutableStateOf(false) }
  Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text(member.email, style = MaterialTheme.typography.bodyLarge)
        Text(member.role, style = MaterialTheme.typography.labelMedium)
      }
      Box {
        TextButton(onClick = { menuExpanded = true }) { Text("Change role") }
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
        }
      }
    }
  }
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
            modifier = Modifier.fillMaxWidth().clickable { roleMenuExpanded = true }
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

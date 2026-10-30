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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroworld.staff.data.network.CreateStaffRequest
import com.electroworld.staff.data.titleCaseAsTyped
import com.electroworld.staff.data.toTitleCase
import com.electroworld.staff.data.network.DeleteStaffRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.StaffMember
import com.electroworld.staff.data.network.UpdateStaffRoleRequest
import com.electroworld.staff.data.network.CreateCustomerRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

private enum class StaffTab(val label: String) {
  STAFF("Staff"),
  ADD_CUSTOMER("Add customer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  var staff by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var showAddDialog by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<StaffMember?>(null) }
  var selectedTab by remember { mutableStateOf(StaffTab.STAFF) }
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
        title = { Text("Accounts") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    floatingActionButton = {
      if (selectedTab == StaffTab.STAFF) {
        FloatingActionButton(onClick = { showAddDialog = true }) {
          Icon(Icons.Default.Add, contentDescription = "Add staff")
        }
      }
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      TabRow(selectedTabIndex = selectedTab.ordinal) {
        StaffTab.entries.forEach { tab ->
          Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab.label) })
        }
      }
      if (selectedTab == StaffTab.ADD_CUSTOMER) {
        AddCustomerTab()
      } else Box(modifier = Modifier.fillMaxSize()) {
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

// The server's own { error } text where it sent one ("An account already
// exists for this number", ...), which says more than a generic failure.
private fun serverErrorMessage(e: Exception): String? = try {
  (e as? HttpException)?.response()?.errorBody()?.string()?.let { JSONObject(it).optString("error") }?.ifBlank { null }
} catch (parseError: Exception) {
  null
}

// Signs a customer up directly, for when they can't get past the customer
// app's OTP step. The admin sets a 4-digit PIN and tells the customer, who
// then logs in with phone + PIN (and can change it through Forgot PIN).
@Composable
private fun AddCustomerTab() {
  var phone by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var pin by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  fun clearMessages() {
    errorMessage = null
    successMessage = null
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      "Create an account for a customer who can't log in through the OTP in the customer app. " +
        "Share the PIN with them -- they log in with their phone number and this PIN.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.outline
    )
    OutlinedTextField(
      value = phone,
      onValueChange = { input -> phone = input.filter { it.isDigit() }.take(10); clearMessages() },
      label = { Text("Customer phone (10 digits)") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = name,
      onValueChange = { name = it.titleCaseAsTyped(); clearMessages() },
      label = { Text("Customer name") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
      modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
      value = pin,
      onValueChange = { input -> pin = input.filter { it.isDigit() }.take(4); clearMessages() },
      label = { Text("4-digit PIN") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
      modifier = Modifier.fillMaxWidth()
    )
    errorMessage?.let {
      Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    successMessage?.let {
      Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
    }
    Button(
      enabled = !isSaving && phone.length == 10 && name.isNotBlank() && pin.length == 4,
      onClick = {
        clearMessages()
        isSaving = true
        scope.launch {
          try {
            val created = NetworkModule.staffApi.createCustomer(
              CreateCustomerRequest(phone = phone, name = name.toTitleCase(), pin = pin)
            )
            successMessage = "Account created for ${created.name.toTitleCase()} (${created.phone}). " +
              "They can now log in with PIN $pin."
            phone = ""
            name = ""
            pin = ""
          } catch (e: Exception) {
            errorMessage = serverErrorMessage(e) ?: "Failed to create customer account."
          } finally {
            isSaving = false
          }
        }
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text("Create customer account")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
  var name by remember { mutableStateOf("") }
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
          value = name,
          onValueChange = { name = it.titleCaseAsTyped() },
          label = { Text("Name") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
        enabled = !isSaving && name.isNotBlank() && email.isNotBlank() && password.length >= 8,
        onClick = {
          errorMessage = null
          isSaving = true
          scope.launch {
            try {
              NetworkModule.staffApi.create(CreateStaffRequest(email = email.trim(), password = password, role = role, name = name.toTitleCase()))
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

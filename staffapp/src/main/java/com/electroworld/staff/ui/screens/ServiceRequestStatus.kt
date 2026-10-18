package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Shared between ComplaintsScreen and InstallationsScreen -- the status
// workflow and its per-status required fields are identical for both
// request types (see schema.sql: complaint_status/installation_status are
// extended with the same 5 new values).

// 'open' is only ever the pre-action default -- staff can never set it
// manually (see VALID_SERVICE_REQUEST_STATUSES in api/staff/[...path].js),
// so it's excluded from the "change to" list but kept for the filter list.
val ACTIONABLE_STATUSES = listOf(
  "assigned_to_service_center",
  "assigned_to_technician",
  "blocked",
  "cancelled_by_staff",
  "cancelled_by_customer",
  "closed"
)
val ALL_STATUSES_FOR_FILTER = listOf("open") + ACTIONABLE_STATUSES

// in_progress/resolved are deprecated (dropped from ACTIONABLE_STATUSES and
// the filter list) but still labeled here so any existing row still
// carrying one of them displays readably rather than as a raw enum value.
fun serviceStatusLabel(status: String): String = when (status) {
  "open" -> "Open"
  "in_progress" -> "In Progress"
  "resolved" -> "Resolved"
  "assigned_to_service_center" -> "Assigned to Service Center"
  "assigned_to_technician" -> "Assigned to Technician"
  "blocked" -> "Blocked"
  "cancelled_by_staff" -> "Cancelled by Staff"
  "cancelled_by_customer" -> "Cancelled by Customer"
  "closed" -> "Closed"
  else -> status
}

// Which of these is populated depends entirely on the `status` passed to
// ServiceRequestStatusDialog -- see statusChangeFieldsError in
// api/staff/[...path].js for the server-side mirror of this requirement.
data class StatusChangeFields(
  val referenceNumber: String? = null,
  val technicianName: String? = null,
  val technicianMobile: String? = null,
  val reason: String? = null,
  val closedDate: String? = null
)

/**
 * Status chip + dropdown that, on picking a different status, opens
 * [ServiceRequestStatusDialog] to collect that status's required field(s)
 * before calling [onConfirmed] -- replaces the old immediate-PATCH-on-click
 * AssistChip/DropdownMenu pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// referenceLabel names the reference number asked for on "Assigned to
// Service Center" -- "Installation request ID" or "Complaint request ID".
fun StatusAssistChip(currentStatus: String, referenceLabel: String, onConfirmed: (String, StatusChangeFields) -> Unit) {
  var menuExpanded by remember { mutableStateOf(false) }
  var pendingStatus by remember { mutableStateOf<String?>(null) }

  Box {
    AssistChip(
      onClick = { menuExpanded = true },
      label = { Text(serviceStatusLabel(currentStatus)) },
      trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
    )
    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
      ACTIONABLE_STATUSES.forEach { status ->
        DropdownMenuItem(
          text = { Text(serviceStatusLabel(status)) },
          onClick = {
            menuExpanded = false
            if (status != currentStatus) pendingStatus = status
          }
        )
      }
    }
  }

  pendingStatus?.let { status ->
    ServiceRequestStatusDialog(
      status = status,
      referenceLabel = referenceLabel,
      onDismiss = { pendingStatus = null },
      onConfirm = { fields ->
        onConfirmed(status, fields)
        pendingStatus = null
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceRequestStatusDialog(status: String, referenceLabel: String, onDismiss: () -> Unit, onConfirm: (StatusChangeFields) -> Unit) {
  var referenceNumber by remember { mutableStateOf("") }
  var technicianName by remember { mutableStateOf("") }
  var technicianMobile by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("") }
  var showDatePicker by remember { mutableStateOf(false) }
  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
  val selectedDate = datePickerState.selectedDateMillis?.let { formatDate(it) }

  val isValid = when (status) {
    "assigned_to_service_center" -> referenceNumber.isNotBlank()
    "assigned_to_technician" -> technicianName.isNotBlank() && technicianMobile.length == 10
    "blocked", "cancelled_by_staff", "cancelled_by_customer" -> reason.isNotBlank()
    "closed" -> selectedDate != null
    else -> true
  }

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
      dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(serviceStatusLabel(status)) },
    text = {
      Column {
        when (status) {
          "assigned_to_service_center" -> {
            OutlinedTextField(
              value = referenceNumber,
              onValueChange = { referenceNumber = it },
              label = { Text(referenceLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
          "assigned_to_technician" -> {
            OutlinedTextField(
              value = technicianName,
              onValueChange = { technicianName = it },
              label = { Text("Technician name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = technicianMobile,
              onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) technicianMobile = it },
              label = { Text("Mobile no. (10 digits)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
          }
          "blocked", "cancelled_by_staff", "cancelled_by_customer" -> {
            OutlinedTextField(
              value = reason,
              onValueChange = { reason = it },
              label = { Text("Reason") },
              minLines = 2,
              modifier = Modifier.fillMaxWidth()
            )
          }
          "closed" -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(selectedDate ?: "No date selected", modifier = Modifier.padding(end = 12.dp))
              TextButton(onClick = { showDatePicker = true }) { Text("Pick date") }
            }
            Text(
              "Are you sure you want to mark this as closed?",
              color = MaterialTheme.colorScheme.outline,
              modifier = Modifier.padding(top = 12.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        enabled = isValid,
        onClick = {
          onConfirm(
            StatusChangeFields(
              referenceNumber = referenceNumber.takeIf { status == "assigned_to_service_center" },
              technicianName = technicianName.takeIf { status == "assigned_to_technician" },
              technicianMobile = technicianMobile.takeIf { status == "assigned_to_technician" },
              reason = reason.takeIf { status == "blocked" || status == "cancelled_by_staff" || status == "cancelled_by_customer" },
              closedDate = selectedDate.takeIf { status == "closed" }
            )
          )
        }
      ) { Text("Confirm") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

// DatePickerState.selectedDateMillis is UTC midnight of the picked calendar
// day, so formatting must stay in UTC -- converting to any local timezone
// (IST included) could land on the day before/after.
private fun formatDate(millis: Long): String {
  val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
  }
  return formatter.format(java.util.Date(millis))
}

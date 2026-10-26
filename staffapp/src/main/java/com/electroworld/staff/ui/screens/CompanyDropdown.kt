package com.electroworld.staff.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.Company
import com.electroworld.staff.data.network.NetworkModule

// The required "Company *" picker on the Log a complaint / installation
// dialogs. Loads the list itself (managed on Catalog Setup -> Companies).
@Composable
fun CompanyDropdown(selected: Company?, onSelect: (Company) -> Unit, modifier: Modifier = Modifier) {
  var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var loadFailed by remember { mutableStateOf(false) }
  var loadTrigger by remember { mutableStateOf(0) }
  var expanded by remember { mutableStateOf(false) }

  LaunchedEffect(loadTrigger) {
    isLoading = true
    loadFailed = false
    try {
      companies = NetworkModule.companiesApi.list().companies
    } catch (e: Exception) {
      loadFailed = true
    } finally {
      isLoading = false
    }
  }

  when {
    isLoading -> CircularProgressIndicator(modifier = modifier.padding(top = 16.dp))
    loadFailed -> Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 16.dp)) {
      Text("Couldn't load companies.", color = MaterialTheme.colorScheme.error)
      TextButton(onClick = { loadTrigger++ }) { Text("Retry") }
    }
    companies.isEmpty() -> Text(
      "No companies added yet. An admin can add them under Catalog Setup.",
      color = MaterialTheme.colorScheme.error,
      modifier = modifier.padding(top = 16.dp)
    )
    else -> Box(modifier = modifier.padding(top = 8.dp)) {
      OutlinedTextField(
        value = selected?.name ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Company *") },
        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
      )
      Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        companies.forEach { company ->
          DropdownMenuItem(text = { Text(company.name) }, onClick = { expanded = false; onSelect(company) })
        }
      }
    }
  }
}

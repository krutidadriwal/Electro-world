package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.Company
import com.example.ui.theme.DeepSlate

// The required "Company *" picker on Register Complaint and Installation
// Request -- same look as those forms' other dropdowns. companies is null
// while the list is still loading.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDropdownField(companies: List<Company>?, selected: Company?, onSelect: (Company) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Text("Company *", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it && !companies.isNullOrEmpty() }) {
    OutlinedTextField(
      value = selected?.name ?: if (companies == null) "Loading companies..." else "Select a company",
      onValueChange = {},
      readOnly = true,
      trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.outline) },
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
      ),
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = DeepSlate) {
      companies.orEmpty().forEach { company ->
        DropdownMenuItem(text = { Text(company.name) }, onClick = {
          onSelect(company)
          expanded = false
        })
      }
    }
  }
}

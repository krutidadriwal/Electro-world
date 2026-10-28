package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.AddCompanyRequest
import com.electroworld.staff.data.network.Company
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization

private enum class CatalogSetupTab(val label: String) {
  CATEGORY_IMAGES("Category Images"),
  COMPANIES("Companies")
}

// Admin-only: the store's reference data that the rest of the app picks
// from -- hero images per price-list category, and the companies (brands)
// offered in the complaint / installation Company dropdown.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSetupScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  var selectedTab by remember { mutableStateOf(CatalogSetupTab.CATEGORY_IMAGES) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Catalog Setup") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      TabRow(selectedTabIndex = selectedTab.ordinal) {
        CatalogSetupTab.entries.forEach { tab ->
          Tab(selected = selectedTab == tab, onClick = { selectedTab = tab }, text = { Text(tab.label) })
        }
      }
      when (selectedTab) {
        CatalogSetupTab.CATEGORY_IMAGES -> CategoryImagesTab()
        CatalogSetupTab.COMPANIES -> CompaniesTab()
      }
    }
  }
}

// Add a company by name at the top; delete one from its row. Deleting only
// removes it from the dropdown -- requests already made for it keep its name.
@Composable
private fun CompaniesTab() {
  var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var isRefreshing by remember { mutableStateOf(false) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var newName by remember { mutableStateOf("") }
  var isAdding by remember { mutableStateOf(false) }
  var pendingDelete by remember { mutableStateOf<Company?>(null) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      companies = NetworkModule.companiesApi.list().companies
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load companies."
    } finally {
      isLoading = false
      isRefreshing = false
    }
  }

  fun add() {
    val name = newName.trim()
    if (name.isEmpty() || isAdding) return
    isAdding = true
    errorMessage = null
    scope.launch {
      try {
        NetworkModule.companiesApi.add(AddCompanyRequest(name))
        newName = ""
        refreshTrigger++
      } catch (e: HttpException) {
        errorMessage = if (e.code() == 409) "$name is already in the list." else "Failed to add $name."
      } catch (e: Exception) {
        errorMessage = "Failed to add $name."
      } finally {
        isAdding = false
      }
    }
  }

  pendingDelete?.let { company ->
    var isDeleting by remember(company) { mutableStateOf(false) }
    AlertDialog(
      onDismissRequest = { if (!isDeleting) pendingDelete = null },
      title = { Text("Delete ${company.name}?") },
      text = {
        Text("It will no longer be offered when logging a complaint or installation. Requests already made for it keep showing its name.")
      },
      confirmButton = {
        Button(
          enabled = !isDeleting,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          onClick = {
            isDeleting = true
            scope.launch {
              try {
                val response = NetworkModule.companiesApi.delete(company.id)
                if (!response.isSuccessful) errorMessage = "Failed to delete ${company.name}."
              } catch (e: Exception) {
                errorMessage = "Failed to delete ${company.name}."
              } finally {
                pendingDelete = null
                refreshTrigger++
              }
            }
          }
        ) { Text("Delete") }
      },
      dismissButton = { TextButton(enabled = !isDeleting, onClick = { pendingDelete = null }) { Text("Cancel") } }
    )
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = newName,
        onValueChange = { newName = it },
        label = { Text("New company (e.g. LG)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.weight(1f)
      )
      Button(enabled = newName.isNotBlank() && !isAdding, onClick = { add() }) {
        if (isAdding) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Add")
      }
    }

    errorMessage?.let {
      Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }

    if (isLoading && companies.isEmpty() && !isRefreshing) {
      Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
      }
    } else StaffPullToRefresh(isRefreshing = isRefreshing, onRefresh = { isRefreshing = true; refreshTrigger++ }) {
      if (companies.isEmpty()) {
        PullableMessage {
          Text(
            "No companies yet. Complaints and installation requests can't be logged until at least one is added.",
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(24.dp)
          )
        }
      } else LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(companies, key = { it.id }) { company ->
          Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(company.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
              IconButton(onClick = { pendingDelete = company }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${company.name}", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }
    }
  }
}

package com.electroworld.staff.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.PriceListItem
import kotlinx.coroutines.launch

private sealed class PriceListNav {
  data object Categories : PriceListNav()
  data class Groups(val category: String) : PriceListNav()
  data class Items(val category: String, val groupName: String) : PriceListNav()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(isAdmin: Boolean, modifier: Modifier = Modifier) {
  var allItems by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var isSyncing by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var nav by remember { mutableStateOf<PriceListNav>(PriceListNav.Categories) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedItem by remember { mutableStateOf<PriceListItem?>(null) }
  val scope = rememberCoroutineScope()

  selectedItem?.let { item ->
    CustomerPriceScreen(item = item, onBack = { selectedItem = null }, modifier = modifier)
    return
  }

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      allItems = NetworkModule.priceListApi.list().items
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load price list."
    } finally {
      isLoading = false
    }
  }

  // Each drill-down level searches its own scope (items vs. group names), so
  // a query typed at one level shouldn't linger and silently filter the next.
  LaunchedEffect(nav) { searchQuery = "" }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(navTitle(nav)) },
        navigationIcon = {
          val parent = navParent(nav)
          if (parent != null) {
            IconButton(onClick = { nav = parent }) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
          }
        },
        actions = {
          if (isAdmin) {
            IconButton(
              enabled = !isSyncing,
              onClick = {
                scope.launch {
                  isSyncing = true
                  errorMessage = null
                  try {
                    NetworkModule.priceListApi.sync()
                    refreshTrigger++
                  } catch (e: Exception) {
                    errorMessage = "Sync failed. Please try again."
                  } finally {
                    isSyncing = false
                  }
                }
              }
            ) {
              if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Sync, contentDescription = "Sync price list")
              }
            }
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      // Only the root (categories) and groups levels get a search bar --
      // root searches items/categories directly, groups searches group
      // names within the selected category. The items level is reached by
      // picking a specific group, which is already a small, browsable list.
      if (nav !is PriceListNav.Items) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text(if (nav is PriceListNav.Groups) "Search groups" else "Search items or categories") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
      }

      errorMessage?.let {
        Text(
          it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
      }

      if (isLoading && allItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
        when (val current = nav) {
          is PriceListNav.Categories -> CategoriesLevel(
            allItems = allItems,
            searchQuery = searchQuery,
            onCategorySelected = { nav = PriceListNav.Groups(it) },
            onItemSelected = { selectedItem = it }
          )
          is PriceListNav.Groups -> GroupsLevel(
            allItems = allItems,
            category = current.category,
            searchQuery = searchQuery,
            onGroupSelected = { nav = PriceListNav.Items(current.category, it) }
          )
          is PriceListNav.Items -> ItemsLevel(
            allItems = allItems,
            category = current.category,
            groupName = current.groupName,
            onItemSelected = { selectedItem = it }
          )
        }
      }
    }
  }
}

private fun navTitle(nav: PriceListNav): String = when (nav) {
  is PriceListNav.Categories -> "Price List"
  is PriceListNav.Groups -> nav.category
  is PriceListNav.Items -> nav.groupName
}

private fun navParent(nav: PriceListNav): PriceListNav? = when (nav) {
  is PriceListNav.Categories -> null
  is PriceListNav.Groups -> PriceListNav.Categories
  is PriceListNav.Items -> PriceListNav.Groups(nav.category)
}

@Composable
private fun CategoriesLevel(
  allItems: List<PriceListItem>,
  searchQuery: String,
  onCategorySelected: (String) -> Unit,
  onItemSelected: (PriceListItem) -> Unit
) {
  if (searchQuery.isNotBlank()) {
    val matches = allItems.filter {
      it.itemName.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }
    ItemRows(matches, onItemSelected)
    return
  }

  val categories = allItems.groupBy { it.category }.toSortedMap()
  if (categories.isEmpty()) {
    EmptyState("No price list loaded yet.")
    return
  }
  LazyColumn(contentPadding = PaddingValues(16.dp)) {
    items(categories.entries.toList()) { (category, itemsInCategory) ->
      NavRow(title = category, subtitle = "${itemsInCategory.size} items") { onCategorySelected(category) }
    }
  }
}

@Composable
private fun GroupsLevel(
  allItems: List<PriceListItem>,
  category: String,
  searchQuery: String,
  onGroupSelected: (String) -> Unit
) {
  val itemsInCategory = allItems.filter { it.category == category }
  val groups = itemsInCategory
    .groupBy { it.groupName }
    .filterKeys { searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) }
    .toSortedMap()

  if (groups.isEmpty()) {
    EmptyState("No groups match your search.")
    return
  }
  LazyColumn(contentPadding = PaddingValues(16.dp)) {
    items(groups.entries.toList()) { (group, itemsInGroup) ->
      NavRow(title = group, subtitle = "${itemsInGroup.size} items") { onGroupSelected(group) }
    }
  }
}

@Composable
private fun ItemsLevel(
  allItems: List<PriceListItem>,
  category: String,
  groupName: String,
  onItemSelected: (PriceListItem) -> Unit
) {
  val itemsInGroup = allItems.filter { it.category == category && it.groupName == groupName }
  ItemRows(itemsInGroup, onItemSelected)
}

@Composable
private fun ItemRows(items: List<PriceListItem>, onItemSelected: (PriceListItem) -> Unit) {
  if (items.isEmpty()) {
    EmptyState("No items match your search.")
    return
  }
  LazyColumn(contentPadding = PaddingValues(16.dp)) {
    items(items) { item -> ItemRow(item, onClick = { onItemSelected(item) }) }
  }
}

@Composable
private fun ItemRow(item: PriceListItem, onClick: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp)
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(item.itemName, style = MaterialTheme.typography.bodyLarge)
      Text(
        "${item.category} · ${item.groupName}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline
      )
      Text(
        "₹${formatPrice(item.finalPrice)}  ·  ${item.stockLabel}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

// Full-screen, customer-facing view: the salesperson hands their phone to the
// customer, so this favors large, unambiguous type over information density.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerPriceScreen(item: PriceListItem, onBack: () -> Unit, modifier: Modifier = Modifier) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
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
        .fillMaxSize()
        .padding(padding)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        item.itemName,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center
      )
      Text(
        "${item.category} · ${item.groupName}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
      )

      Spacer(modifier = Modifier.height(40.dp))

      if (item.mrp > item.finalPrice) {
        Text(
          "MRP ₹${formatPrice(item.mrp)}",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.outline,
          textDecoration = TextDecoration.LineThrough
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      Text(
        "₹${formatPrice(item.finalPrice)}",
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      if (item.mrp > item.finalPrice) {
        val savings = item.mrp - item.finalPrice
        Text(
          "You save ₹${formatPrice(savings)}",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 12.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        item.stockLabel,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
      }
      Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
  }
}

@Composable
private fun EmptyState(message: String) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(message, color = MaterialTheme.colorScheme.outline)
  }
}

private fun formatPrice(value: Double): String {
  val rounded = Math.round(value)
  return "%,d".format(rounded)
}

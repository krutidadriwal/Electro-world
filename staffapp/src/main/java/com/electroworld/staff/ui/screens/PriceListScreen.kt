package com.electroworld.staff.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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

// What the radio group is scoped to: which field the root list browses/groups
// by, and which field a typed search query matches against.
private enum class SearchScope { CATEGORY, GROUP, ITEM }

private sealed class PriceListNav {
  data object Root : PriceListNav()
  data class Items(val title: String, val category: String?, val groupName: String?) : PriceListNav()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceListScreen(isAdmin: Boolean, modifier: Modifier = Modifier) {
  var allItems by remember { mutableStateOf<List<PriceListItem>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var isSyncing by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var nav by remember { mutableStateOf<PriceListNav>(PriceListNav.Root) }
  var scope by remember { mutableStateOf(SearchScope.CATEGORY) }
  var searchQuery by remember { mutableStateOf("") }
  var hideOutOfStock by remember { mutableStateOf(false) }
  var selectedItem by remember { mutableStateOf<PriceListItem?>(null) }
  val coroutineScope = rememberCoroutineScope()

  val visibleItems = if (hideOutOfStock) allItems.filterNot { isOutOfStock(it) } else allItems

  selectedItem?.let { item ->
    BackHandler { selectedItem = null }
    CustomerPriceScreen(item = item, onBack = { selectedItem = null }, modifier = modifier)
    return
  }

  val parentNav = navParent(nav)
  BackHandler(enabled = parentNav != null) {
    if (parentNav != null) nav = parentNav
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

  // Drilling into a category/group's items is a dead end with nothing left to
  // search, so the query only makes sense back at the root -- clear it on the
  // way in so it isn't silently stale when the user backs out.
  LaunchedEffect(nav) { if (nav is PriceListNav.Items) searchQuery = "" }

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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hide 0 pcs", style = MaterialTheme.typography.labelSmall)
            Checkbox(checked = hideOutOfStock, onCheckedChange = { hideOutOfStock = it })

            if (isAdmin) {
              IconButton(
                enabled = !isSyncing,
                onClick = {
                  coroutineScope.launch {
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
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      // The radio group and search bar only make sense at the root -- once
      // you've drilled into a specific category/group's items there's
      // nothing left to scope or search within.
      if (nav is PriceListNav.Root) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp)
        ) {
          ScopeRadioOption("Categories", scope == SearchScope.CATEGORY) { scope = SearchScope.CATEGORY }
          ScopeRadioOption("Groups", scope == SearchScope.GROUP) { scope = SearchScope.GROUP }
          ScopeRadioOption("Items", scope == SearchScope.ITEM) { scope = SearchScope.ITEM }
        }

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = {
            Text(
              when (scope) {
                SearchScope.CATEGORY -> "Search categories"
                SearchScope.GROUP -> "Search groups"
                SearchScope.ITEM -> "Search items"
              }
            )
          },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

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
          is PriceListNav.Root -> RootLevel(
            allItems = visibleItems,
            scope = scope,
            searchQuery = searchQuery,
            isAdmin = isAdmin,
            onScopeChange = { scope = it },
            onCategorySelected = { nav = PriceListNav.Items(title = it, category = it, groupName = null) },
            onGroupSelected = { nav = PriceListNav.Items(title = it, category = null, groupName = it) },
            onItemSelected = { selectedItem = it }
          )
          is PriceListNav.Items -> {
            val itemsInScope = visibleItems.filter {
              (current.category == null || it.category == current.category) &&
                (current.groupName == null || it.groupName == current.groupName)
            }
            ItemRows(itemsInScope, isAdmin, onItemSelected = { selectedItem = it })
          }
        }
      }
    }
  }
}

private fun navTitle(nav: PriceListNav): String = when (nav) {
  is PriceListNav.Root -> "Price List"
  is PriceListNav.Items -> nav.title
}

private fun navParent(nav: PriceListNav): PriceListNav? = when (nav) {
  is PriceListNav.Root -> null
  is PriceListNav.Items -> PriceListNav.Root
}

@Composable
private fun RootLevel(
  allItems: List<PriceListItem>,
  scope: SearchScope,
  searchQuery: String,
  isAdmin: Boolean,
  onScopeChange: (SearchScope) -> Unit,
  onCategorySelected: (String) -> Unit,
  onGroupSelected: (String) -> Unit,
  onItemSelected: (PriceListItem) -> Unit
) {
  when (scope) {
    SearchScope.CATEGORY -> {
      val categories = allItems.map { it.category }.distinct()
        .filter { searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) }
        .sorted()
      if (categories.isEmpty()) {
        NoMatchesState(query = searchQuery, currentScope = scope, onScopeChange = onScopeChange)
        return
      }
      LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(categories) { category ->
          val count = allItems.count { it.category == category }
          NavRow(title = category, subtitle = "$count items") { onCategorySelected(category) }
        }
      }
    }
    SearchScope.GROUP -> {
      val groups = allItems.groupBy { it.groupName }
        .filterKeys { searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) }
        .toSortedMap()
      if (groups.isEmpty()) {
        NoMatchesState(query = searchQuery, currentScope = scope, onScopeChange = onScopeChange)
        return
      }
      LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(groups.entries.toList()) { (group, itemsInGroup) ->
          NavRow(
            title = group,
            subtitle = "${itemsInGroup.first().category} · ${itemsInGroup.size} items"
          ) { onGroupSelected(group) }
        }
      }
    }
    SearchScope.ITEM -> {
      val matches = allItems.filter { searchQuery.isBlank() || it.itemName.contains(searchQuery, ignoreCase = true) }
      if (matches.isEmpty()) {
        NoMatchesState(query = searchQuery, currentScope = scope, onScopeChange = onScopeChange)
        return
      }
      ItemRows(matches, isAdmin, onItemSelected)
    }
  }
}

// Shown when a search finds nothing under the currently selected radio
// scope -- the match might still exist under a different scope (e.g. typing
// an item name while "Categories" is selected), so this offers a one-tap way
// to broaden the search instead of leaving the user stuck on an empty list.
@Composable
private fun NoMatchesState(query: String, currentScope: SearchScope, onScopeChange: (SearchScope) -> Unit) {
  if (query.isBlank()) {
    EmptyState("No price list loaded yet.")
    return
  }
  val otherScopes = SearchScope.values().filter { it != currentScope }
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
      Text(
        "No ${currentScope.label().lowercase()} match \"$query\".",
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Try switching to ${otherScopes.joinToString(" or ") { it.label() }} above.",
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable { onScopeChange(otherScopes.first()) }
      )
    }
  }
}

private fun SearchScope.label(): String = when (this) {
  SearchScope.CATEGORY -> "Categories"
  SearchScope.GROUP -> "Groups"
  SearchScope.ITEM -> "Items"
}

@Composable
private fun ItemRows(items: List<PriceListItem>, isAdmin: Boolean, onItemSelected: (PriceListItem) -> Unit) {
  if (items.isEmpty()) {
    EmptyState("No items match your search.")
    return
  }
  LazyColumn(contentPadding = PaddingValues(16.dp)) {
    items(items) { item -> ItemRow(item, isAdmin = isAdmin, onClick = { onItemSelected(item) }) }
  }
}

@Composable
private fun ItemRow(item: PriceListItem, isAdmin: Boolean, onClick: () -> Unit) {
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
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
      ) {
        if (item.mrp > item.finalPrice) {
          Text(
            "₹${formatPrice(item.mrp)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textDecoration = TextDecoration.LineThrough,
            modifier = Modifier.padding(end = 8.dp)
          )
        }
        Text("₹${formatPrice(item.finalPrice)}", style = MaterialTheme.typography.bodyMedium)
        Text(
          "  ·  ${item.stockLabel}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }
      if (isAdmin && item.costPrice != null) {
        Text(
          "Cost ₹${formatPrice(item.costPrice)}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
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
private fun ScopeRadioOption(label: String, selected: Boolean, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    RadioButton(selected = selected, onClick = onClick)
    Text(label, style = MaterialTheme.typography.bodyMedium)
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

// stockLabel is a free-text field from the Tally export (e.g. "12 pcs", "0 pcs")
// with no separate numeric quantity -- out of stock is read off its leading number.
private fun isOutOfStock(item: PriceListItem): Boolean {
  val leadingNumber = Regex("""^-?\d+""").find(item.stockLabel.trim())?.value
  return leadingNumber == "0"
}

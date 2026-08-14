package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.WishlistCategory
import com.example.data.mock.WishlistItem
import com.example.data.mock.WishlistMockData
import com.example.data.mock.WishlistSubcategory
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant

/**
 * Wishlist page: Categories -> Subcategories -> Features.
 * Data currently comes from [WishlistMockData]; swap for a real
 * repository/database read once that layer exists.
 */
@Composable
fun WishlistScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCategory by remember { mutableStateOf<WishlistCategory?>(null) }
  var selectedSubcategory by remember { mutableStateOf<WishlistSubcategory?>(null) }

  fun navigateBack() {
    when {
      selectedSubcategory != null -> selectedSubcategory = null
      selectedCategory != null -> selectedCategory = null
      else -> onBack()
    }
  }

  BackHandler(onBack = ::navigateBack)

  val screenTitle = selectedSubcategory?.name ?: selectedCategory?.name ?: "Wishlist"

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
        IconButton(onClick = ::navigateBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = screenTitle,
          color = OnSlateText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when {
        selectedCategory == null -> WishlistCategoryList(
          categories = WishlistMockData.categories,
          onSelect = { selectedCategory = it }
        )
        selectedSubcategory == null -> WishlistSubcategoryList(
          subcategories = selectedCategory!!.subcategories,
          onSelect = { selectedSubcategory = it }
        )
        else -> WishlistFeatureList(items = selectedSubcategory!!.items)
      }
    }
  }
}

@Composable
private fun WishlistCategoryList(
  categories: List<WishlistCategory>,
  onSelect: (WishlistCategory) -> Unit
) {
  if (categories.isEmpty()) {
    WishlistEmptyState(message = "Your wishlist is empty.")
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(categories) { category ->
      Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelect(category) }
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(GoldPrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(category.icon, contentDescription = category.name, tint = GoldSecondary, modifier = Modifier.size(22.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(text = category.name, color = OnSlateText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "${category.subcategories.size} subcategories",
              color = OnSlateTextSecondary,
              fontSize = 11.sp
            )
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSlateTextSecondary)
        }
      }
    }
  }
}

@Composable
private fun WishlistSubcategoryList(
  subcategories: List<WishlistSubcategory>,
  onSelect: (WishlistSubcategory) -> Unit
) {
  if (subcategories.isEmpty()) {
    WishlistEmptyState(message = "No subcategories yet.")
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(subcategories) { subcategory ->
      Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelect(subcategory) }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = subcategory.name, color = OnSlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "${subcategory.items.size} items",
              color = OnSlateTextSecondary,
              fontSize = 11.sp
            )
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSlateTextSecondary)
        }
      }
    }
  }
}

@Composable
private fun WishlistFeatureList(items: List<WishlistItem>) {
  if (items.isEmpty()) {
    WishlistEmptyState(message = "No items in this subcategory yet.")
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(items) { item ->
      Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = item.name, color = OnSlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "$${String.format("%.2f", item.price)}",
              color = GoldPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "FEATURES",
            color = GoldSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          item.features.forEach { feature ->
            Text(
              text = "• $feature",
              color = OnSlateTextSecondary,
              fontSize = 12.sp,
              modifier = Modifier.padding(vertical = 1.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WishlistEmptyState(message: String) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = message, color = OnSlateTextSecondary, fontSize = 14.sp)
  }
}

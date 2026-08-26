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
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.mock.WishlistItem
import com.example.data.mock.WishlistMockData
import com.example.data.network.Category
import com.example.data.network.ConfirmWishlistRequest
import com.example.data.network.NetworkModule
import com.example.data.network.Subcategory
import com.example.data.network.WishlistItemDto
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import kotlinx.coroutines.launch

/**
 * Wishlist page: Categories -> Subcategories -> Features.
 * Category/subcategory browse structure comes from Supabase (shared with
 * Register Complaint, see GET /api/categories). Item-level detail (price,
 * features) still comes from [WishlistMockData] until a real product
 * catalog backend exists. Which categories/subcategories the user has
 * actually wishlisted is stored in Supabase (see GET/POST /api/wishlist).
 */
private fun iconForKey(iconKey: String): ImageVector = when (iconKey.uppercase()) {
  "TV" -> Icons.Default.Tv
  "AC", "CLH" -> Icons.Default.DeviceThermostat
  "REF", "MWO", "OKA", "KHK", "LCK", "ROUV", "WCD", "DFV", "AFK", "WHH" -> Icons.Default.Kitchen
  "SSH" -> Icons.Default.Headphones
  "WMC" -> Icons.Default.Checkroom
  else -> Icons.Default.Info
}

@Composable
fun WishlistScreen(
  userPhone: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()

  var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  var wishlistItems by remember { mutableStateOf<List<WishlistItemDto>>(emptyList()) }

  var selectedCategory by remember { mutableStateOf<Category?>(null) }
  var selectedSubcategory by remember { mutableStateOf<Subcategory?>(null) }
  var viewingMyWishlist by remember { mutableStateOf(false) }

  LaunchedEffect(userPhone) {
    isLoading = true
    errorMessage = null
    try {
      categories = NetworkModule.userApi.getCategories().categories
      wishlistItems = NetworkModule.userApi.getWishlist(userPhone).items
    } catch (e: Exception) {
      errorMessage = "Unable to load categories. Please check your connection and try again."
    } finally {
      isLoading = false
    }
  }

  fun navigateBack() {
    when {
      viewingMyWishlist -> viewingMyWishlist = false
      selectedSubcategory != null -> selectedSubcategory = null
      selectedCategory != null -> selectedCategory = null
      else -> onBack()
    }
  }

  BackHandler(onBack = ::navigateBack)

  val screenTitle = when {
    viewingMyWishlist -> "My Wishlist"
    else -> selectedSubcategory?.name ?: selectedCategory?.name ?: "Wishlist"
  }

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
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        if (!viewingMyWishlist && selectedCategory == null) {
          IconButton(onClick = { viewingMyWishlist = true }) {
            Icon(Icons.Default.Bookmarks, contentDescription = "View my wishlist", tint = GoldPrimary)
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when {
        isLoading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(color = GoldPrimary)
          }
        }
        errorMessage != null -> WishlistEmptyState(message = errorMessage ?: "")
        viewingMyWishlist -> WishlistSavedList(items = wishlistItems)
        selectedCategory == null -> WishlistCategoryList(
          categories = categories,
          onSelect = { selectedCategory = it }
        )
        selectedCategory!!.subcategories.isEmpty() ->
          WishlistFeatureList(items = WishlistMockData.itemsForCategory(selectedCategory!!.iconKey))
        selectedSubcategory == null -> {
          val category = selectedCategory!!
          WishlistSubcategoryPicker(
            category = category,
            wishlistItems = wishlistItems,
            onOpenSubcategory = { selectedSubcategory = it },
            onConfirm = { categoryStarred, starredSubcategoryIds ->
              coroutineScope.launch {
                try {
                  val updated = NetworkModule.userApi.confirmWishlist(
                    ConfirmWishlistRequest(
                      phone = userPhone,
                      categoryIconKey = category.iconKey,
                      categoryStarred = categoryStarred,
                      subcategoryIds = starredSubcategoryIds.toList()
                    )
                  ).items
                  wishlistItems = wishlistItems.filter { it.categoryIconKey != category.iconKey } + updated
                  selectedCategory = null
                } catch (e: Exception) {
                  errorMessage = "Unable to save your wishlist. Please check your connection and try again."
                }
              }
            }
          )
        }
        else -> WishlistFeatureList(items = WishlistMockData.itemsForSubcategory(selectedSubcategory!!.name))
      }
    }
  }
}

@Composable
private fun WishlistCategoryList(
  categories: List<Category>,
  onSelect: (Category) -> Unit
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
            Icon(iconForKey(category.iconKey), contentDescription = category.name, tint = GoldSecondary, modifier = Modifier.size(22.dp))
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

// Category name + star (header), each subcategory with its own star, and a
// Confirm Wishlist button at the bottom that saves the current selection.
@Composable
private fun WishlistSubcategoryPicker(
  category: Category,
  wishlistItems: List<WishlistItemDto>,
  onOpenSubcategory: (Subcategory) -> Unit,
  onConfirm: (categoryStarred: Boolean, starredSubcategoryIds: Set<String>) -> Unit
) {
  val existingForCategory = wishlistItems.filter { it.categoryIconKey == category.iconKey }

  var categoryStarred by remember(category.iconKey) {
    mutableStateOf(existingForCategory.any { it.subcategoryId == null })
  }
  var starredSubcategoryIds by remember(category.iconKey) {
    mutableStateOf(existingForCategory.mapNotNull { it.subcategoryId }.toSet())
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = category.name, color = OnSlateText, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      IconButton(onClick = { categoryStarred = !categoryStarred }) {
        Icon(
          imageVector = if (categoryStarred) Icons.Default.Star else Icons.Default.StarBorder,
          contentDescription = if (categoryStarred) "Unstar category" else "Star whole category",
          tint = GoldSecondary
        )
      }
    }

    LazyColumn(
      contentPadding = PaddingValues(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.weight(1f)
    ) {
      items(category.subcategories) { subcategory ->
        val starred = starredSubcategoryIds.contains(subcategory.id)
        Card(
          colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSubcategory(subcategory) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 14.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = subcategory.name, color = OnSlateText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = {
                starredSubcategoryIds = if (starred) {
                  starredSubcategoryIds - subcategory.id
                } else {
                  starredSubcategoryIds + subcategory.id
                }
              }) {
                Icon(
                  imageVector = if (starred) Icons.Default.Star else Icons.Default.StarBorder,
                  contentDescription = if (starred) "Unstar subcategory" else "Star subcategory",
                  tint = GoldSecondary
                )
              }
              Icon(Icons.Default.ChevronRight, contentDescription = null, tint = OnSlateTextSecondary)
            }
          }
        }
      }
    }

    Button(
      onClick = { onConfirm(categoryStarred, starredSubcategoryIds) },
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(10.dp)
    ) {
      Text("CONFIRM WISHLIST", color = SlateBackground, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun WishlistSavedList(items: List<WishlistItemDto>) {
  if (items.isEmpty()) {
    WishlistEmptyState(message = "You haven't wishlisted anything yet.")
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
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Star, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = if (item.subcategoryName != null) "${item.categoryName} • ${item.subcategoryName}" else item.categoryName,
            color = OnSlateText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
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

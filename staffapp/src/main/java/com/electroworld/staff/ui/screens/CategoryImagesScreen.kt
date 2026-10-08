package com.electroworld.staff.ui.screens

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.electroworld.staff.data.network.CategoryImage
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.UploadCategoryImageRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch

// Admin-only: assigns the hero image shown at the end of CustomerPriceScreen
// (PriceListScreen.kt) for every item in a category. One category at a time --
// tapping a row's button opens the system picker scoped to that row.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryImagesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var categories by remember { mutableStateOf<List<CategoryImage>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var uploadingCategory by remember { mutableStateOf<String?>(null) }
  // Set right before launching the picker, read in its callback -- the
  // picker's result arrives asynchronously with no way to pass this through
  // the launch() call itself.
  var pendingCategory by remember { mutableStateOf<String?>(null) }

  val pickImageLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    val category = pendingCategory
    pendingCategory = null
    if (uri != null && category != null) {
      scope.launch {
        uploadingCategory = category
        errorMessage = null
        try {
          val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          if (bytes != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            NetworkModule.priceListApi.uploadCategoryImage(
              UploadCategoryImageRequest(
                category = category,
                fileName = category.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
                mimeType = mimeType,
                base64Data = base64
              )
            )
            refreshTrigger++
          }
        } catch (e: Exception) {
          errorMessage = "Failed to upload image for $category. Please try again."
        } finally {
          uploadingCategory = null
        }
      }
    }
  }

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      categories = NetworkModule.priceListApi.categoryImages().categories
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load categories."
    } finally {
      isLoading = false
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Category Images") },
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
      errorMessage?.let {
        Text(
          it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
      }
      if (isLoading && categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
          items(categories) { item ->
            CategoryImageRow(
              item = item,
              isUploading = uploadingCategory == item.category,
              onPick = {
                pendingCategory = item.category
                pickImageLauncher.launch("image/*")
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryImageRow(item: CategoryImage, isUploading: Boolean, onPick: () -> Unit) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (item.imageUrl != null) {
        AsyncImage(
          model = item.imageUrl,
          contentDescription = item.category,
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
        )
      } else {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(item.category, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
      if (isUploading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        OutlinedButton(onClick = onPick) {
          Text(if (item.imageUrl == null) "Add" else "Change")
        }
      }
    }
  }
}

package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryCache
import com.example.data.InvoiceFileCache
import com.example.data.InvoiceListCache
import com.example.data.network.Category
import com.example.data.network.CreateComplaintRequest
import com.example.data.network.InvoiceFile
import com.example.data.network.NetworkModule
import com.example.data.network.Subcategory
import com.example.ui.theme.Swatch2Paper
import kotlinx.coroutines.launch

// Fixed Paper in light mode (matching the rest of the app's dropdowns);
// dark mode keeps the Material default tonal surface.
@Composable
private fun dropdownMenuContainerColor() = if (isSystemInDarkTheme()) MenuDefaults.containerColor else Swatch2Paper

/**
 * Full-screen Register Complaint flow (its own screen, not a dashboard
 * overlay) so the form has room to breathe on small devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterComplaintScreen(
  userPhone: String,
  onBack: () -> Unit,
  onOpenHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val invoiceListCache = remember { InvoiceListCache(context) }
  val categoryCache = remember { CategoryCache(context) }

  var invoices by remember { mutableStateOf(invoiceListCache.get(userPhone) ?: emptyList()) }
  var selectedInvoice by remember { mutableStateOf<InvoiceFile?>(null) }
  var invoiceMenuExpanded by remember { mutableStateOf(false) }
  var openingInvoiceId by remember { mutableStateOf<String?>(null) }

  // Lets the user preview an invoice straight from the picker, before (or
  // instead of) selecting it -- same cache-then-open flow as My Invoices/
  // Dashboard (InvoiceFileCache), so a PDF already opened from there is
  // served from disk here too.
  fun openInvoice(invoice: InvoiceFile) {
    if (openingInvoiceId != null) return
    openingInvoiceId = invoice.id
    coroutineScope.launch {
      try {
        val file = InvoiceFileCache.getOrDownload(context, userPhone, invoice)
        InvoiceFileCache.openFile(context, file)
      } catch (e: Exception) {
        Toast.makeText(context, "Unable to open invoice. Please try again.", Toast.LENGTH_SHORT).show()
      } finally {
        openingInvoiceId = null
      }
    }
  }

  var categories by remember { mutableStateOf(categoryCache.get() ?: emptyList()) }
  var selectedCategory by remember { mutableStateOf<Category?>(null) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }

  var selectedSubcategory by remember { mutableStateOf<Subcategory?>(null) }
  var subcategoryMenuExpanded by remember { mutableStateOf(false) }

  var description by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var contactPhone by remember { mutableStateOf("") }

  var isSubmitting by remember { mutableStateOf(false) }
  var submitError by remember { mutableStateOf<String?>(null) }
  var submitSuccess by remember { mutableStateOf(false) }

  // Both lists render instantly from cache above; this just refreshes them
  // from Supabase in the background and updates the cache (and the visible
  // list) only once the fresh copy actually arrives, instead of blocking the
  // form behind a network round-trip every time it's opened.
  LaunchedEffect(userPhone) {
    try {
      val fresh = NetworkModule.userApi.getInvoices(userPhone).invoices
      invoiceListCache.save(userPhone, fresh)
      invoices = fresh
    } catch (e: Exception) {
      // Invoice list is optional context for a complaint; silently keep
      // whatever was cached (or empty) rather than blocking the form.
    }
    try {
      val fresh = NetworkModule.userApi.getCategories().categories
      categoryCache.save(fresh)
      categories = fresh
    } catch (e: Exception) {
      if (categories.isEmpty()) {
        submitError = "Unable to load product categories. Please check your connection and try again."
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(top = 40.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Register Complaint",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenHistory) {
          Icon(Icons.Default.History, contentDescription = "My complaints", tint = MaterialTheme.colorScheme.primary)
        }
      }
    }
  ) { paddingValues ->
    if (submitSuccess) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(Icons.Default.CheckCircle, "Submitted", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Complaint Registered", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Our team will get in touch with you shortly.",
          color = MaterialTheme.colorScheme.outline,
          fontSize = 12.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = {
            selectedInvoice = null
            selectedCategory = null
            selectedSubcategory = null
            description = ""
            address = ""
            contactPhone = ""
            submitSuccess = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("FILE ANOTHER COMPLAINT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
      }
      return@Scaffold
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(12.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Text("Related Invoice (optional)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      ExposedDropdownMenuBox(
        expanded = invoiceMenuExpanded,
        onExpandedChange = { invoiceMenuExpanded = it }
      ) {
        OutlinedTextField(
          value = selectedInvoice?.name ?: "No specific invoice",
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
        ExposedDropdownMenu(expanded = invoiceMenuExpanded, onDismissRequest = { invoiceMenuExpanded = false }, containerColor = dropdownMenuContainerColor()) {
          DropdownMenuItem(text = { Text("No specific invoice") }, onClick = {
            selectedInvoice = null
            invoiceMenuExpanded = false
          })
          invoices.forEach { inv ->
            DropdownMenuItem(
              text = { Text(inv.name) },
              onClick = {
                selectedInvoice = inv
                invoiceMenuExpanded = false
              },
              trailingIcon = {
                if (openingInvoiceId == inv.id) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                  )
                } else {
                  IconButton(onClick = { openInvoice(inv) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                      Icons.Default.Visibility,
                      contentDescription = "View invoice",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text("Product Category", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      ExposedDropdownMenuBox(
        expanded = categoryMenuExpanded,
        onExpandedChange = { categoryMenuExpanded = it }
      ) {
        OutlinedTextField(
          value = selectedCategory?.name ?: "Select a category",
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
        ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }, containerColor = dropdownMenuContainerColor()) {
          categories.forEach { option ->
            DropdownMenuItem(text = { Text(option.name) }, onClick = {
              selectedCategory = option
              selectedSubcategory = null
              categoryMenuExpanded = false
            })
          }
        }
      }

      if (selectedCategory != null && selectedCategory!!.subcategories.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))

        Text("Product Type (optional)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        ExposedDropdownMenuBox(
          expanded = subcategoryMenuExpanded,
          onExpandedChange = { subcategoryMenuExpanded = it }
        ) {
          OutlinedTextField(
            value = selectedSubcategory?.name ?: "Select a product type",
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
          ExposedDropdownMenu(expanded = subcategoryMenuExpanded, onDismissRequest = { subcategoryMenuExpanded = false }, containerColor = dropdownMenuContainerColor()) {
            selectedCategory!!.subcategories.forEach { option ->
              DropdownMenuItem(text = { Text(option.name) }, onClick = {
                selectedSubcategory = option
                subcategoryMenuExpanded = false
              })
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text("Description *", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        placeholder = { Text("Describe the issue in detail...") },
        minLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text("Address *", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        placeholder = { Text("Where should our technician visit?") },
        minLines = 2,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text("Alternate Contact Number (optional)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = contactPhone,
        onValueChange = { if (it.length <= 10) contactPhone = it.filter { c -> c.isDigit() } },
        placeholder = { Text("10-digit mobile number") },
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
      )

      submitError?.let {
        Spacer(modifier = Modifier.height(10.dp))
        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          val category = selectedCategory
          if (category == null || description.trim().isEmpty() || address.trim().isEmpty()) {
            submitError = "Please fill in category, description and address."
            return@Button
          }
          submitError = null
          isSubmitting = true
          coroutineScope.launch {
            try {
              NetworkModule.userApi.createComplaint(
                CreateComplaintRequest(
                  phone = userPhone,
                  invoiceFileId = selectedInvoice?.id,
                  invoiceFileName = selectedInvoice?.name,
                  categoryIconKey = category.iconKey,
                  subcategoryId = selectedSubcategory?.id,
                  description = description.trim(),
                  address = address.trim(),
                  contactPhone = contactPhone.ifEmpty { null }
                )
              )
              submitSuccess = true
            } catch (e: Exception) {
              submitError = "Unable to submit complaint. Please check your connection and try again."
            } finally {
              isSubmitting = false
            }
          }
        },
        enabled = !isSubmitting,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text(if (isSubmitting) "SUBMITTING..." else "SUBMIT COMPLAINT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.Category
import com.example.data.network.CreateComplaintRequest
import com.example.data.network.InvoiceFile
import com.example.data.network.NetworkModule
import com.example.data.network.Subcategory
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import kotlinx.coroutines.launch

/**
 * Full-screen Register Complaint flow (its own screen, not a dashboard
 * overlay) so the form has room to breathe on small devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterComplaintScreen(
  userPhone: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  val coroutineScope = rememberCoroutineScope()

  var invoices by remember { mutableStateOf<List<InvoiceFile>>(emptyList()) }
  var selectedInvoice by remember { mutableStateOf<InvoiceFile?>(null) }
  var invoiceMenuExpanded by remember { mutableStateOf(false) }

  var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
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

  LaunchedEffect(userPhone) {
    try {
      invoices = NetworkModule.userApi.getInvoices(userPhone).invoices
    } catch (e: Exception) {
      // Invoice list is optional context for a complaint; silently allow filing without it.
    }
    try {
      categories = NetworkModule.userApi.getCategories().categories
    } catch (e: Exception) {
      submitError = "Unable to load product categories. Please check your connection and try again."
    }
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
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldPrimary)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Register Complaint",
          color = OnSlateText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
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
        Icon(Icons.Default.CheckCircle, "Submitted", tint = GoldSecondary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Complaint Registered", color = OnSlateText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Our team will get in touch with you shortly.",
          color = OnSlateTextSecondary,
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
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("FILE ANOTHER COMPLAINT", color = SlateBackground, fontWeight = FontWeight.Bold)
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
      Text("Related Invoice (optional)", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      ExposedDropdownMenuBox(
        expanded = invoiceMenuExpanded,
        onExpandedChange = { invoiceMenuExpanded = it }
      ) {
        OutlinedTextField(
          value = selectedInvoice?.name ?: "No specific invoice",
          onValueChange = {},
          readOnly = true,
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = OnSlateTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSlateText,
            unfocusedTextColor = OnSlateText,
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = SlateSurfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = invoiceMenuExpanded, onDismissRequest = { invoiceMenuExpanded = false }) {
          DropdownMenuItem(text = { Text("No specific invoice") }, onClick = {
            selectedInvoice = null
            invoiceMenuExpanded = false
          })
          invoices.forEach { inv ->
            DropdownMenuItem(text = { Text(inv.name) }, onClick = {
              selectedInvoice = inv
              invoiceMenuExpanded = false
            })
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text("Product Category", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      ExposedDropdownMenuBox(
        expanded = categoryMenuExpanded,
        onExpandedChange = { categoryMenuExpanded = it }
      ) {
        OutlinedTextField(
          value = selectedCategory?.name ?: "Select a category",
          onValueChange = {},
          readOnly = true,
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = OnSlateTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSlateText,
            unfocusedTextColor = OnSlateText,
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = SlateSurfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
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

        Text("Product Type (optional)", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        ExposedDropdownMenuBox(
          expanded = subcategoryMenuExpanded,
          onExpandedChange = { subcategoryMenuExpanded = it }
        ) {
          OutlinedTextField(
            value = selectedSubcategory?.name ?: "Select a product type",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = OnSlateTextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = OnSlateText,
              unfocusedTextColor = OnSlateText,
              focusedBorderColor = GoldPrimary,
              unfocusedBorderColor = SlateSurfaceVariant
            ),
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
          )
          ExposedDropdownMenu(expanded = subcategoryMenuExpanded, onDismissRequest = { subcategoryMenuExpanded = false }) {
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

      Text("Description *", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        placeholder = { Text("Describe the issue in detail...") },
        minLines = 3,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          unfocusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text("Address *", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        placeholder = { Text("Where should our technician visit?") },
        minLines = 2,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          unfocusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text("Alternate Contact Number (optional)", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = contactPhone,
        onValueChange = { if (it.length <= 10) contactPhone = it.filter { c -> c.isDigit() } },
        placeholder = { Text("10-digit mobile number") },
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          unfocusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      )

      submitError?.let {
        Spacer(modifier = Modifier.height(10.dp))
        Text(it, color = GoldSecondary, fontSize = 12.sp)
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
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text(if (isSubmitting) "SUBMITTING..." else "SUBMIT COMPLAINT", color = SlateBackground, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

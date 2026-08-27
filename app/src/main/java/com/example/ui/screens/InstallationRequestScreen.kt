package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.Category
import com.example.data.network.CreateInstallationRequest
import com.example.data.network.InvoiceFile
import com.example.data.network.NetworkModule
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import kotlinx.coroutines.launch

/**
 * Full-screen Installation Request flow (its own screen, not a dashboard
 * overlay), mirroring RegisterComplaintScreen's layout so the form has room
 * to breathe on small devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationRequestScreen(
  userPhone: String,
  onBack: () -> Unit,
  onOpenHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var invoices by remember { mutableStateOf<List<InvoiceFile>>(emptyList()) }
  var selectedInvoice by remember { mutableStateOf<InvoiceFile?>(null) }
  var invoiceMenuExpanded by remember { mutableStateOf(false) }

  var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
  var selectedCategory by remember { mutableStateOf<Category?>(null) }
  var categoryMenuExpanded by remember { mutableStateOf(false) }

  var itemName by remember { mutableStateOf("") }
  var wantsDemo by remember { mutableStateOf(false) }
  var wantsInstallation by remember { mutableStateOf(false) }
  var address by remember { mutableStateOf("") }
  var contactPhone by remember { mutableStateOf("") }

  var isSubmitting by remember { mutableStateOf(false) }
  var submitError by remember { mutableStateOf<String?>(null) }
  var submitSuccess by remember { mutableStateOf(false) }

  // Only categories that support at least one of demo/installation are
  // offered on this form.
  val visibleCategories = categories.filter { it.canInstall || it.canDemo }

  fun selectCategory(category: Category) {
    selectedCategory = category
    categoryMenuExpanded = false
    // By default, both options are ticked when both are available; when only
    // one is available, only that one is ticked (the other stays disabled).
    wantsDemo = category.canDemo
    wantsInstallation = category.canInstall
  }

  fun promptUnavailable(feature: String) {
    Toast.makeText(context, "$feature is not available for this product.", Toast.LENGTH_SHORT).show()
  }

  LaunchedEffect(userPhone) {
    try {
      invoices = NetworkModule.userApi.getInvoices(userPhone).invoices
    } catch (e: Exception) {
      // Invoice list is optional context for an installation request; silently allow filing without it.
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
          text = "Installation Request",
          color = OnSlateText,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenHistory) {
          Icon(Icons.Default.History, contentDescription = "My installation requests", tint = GoldPrimary)
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
        Icon(Icons.Default.CheckCircle, "Submitted", tint = GoldSecondary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Request Submitted", color = OnSlateText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            itemName = ""
            wantsDemo = false
            wantsInstallation = false
            address = ""
            contactPhone = ""
            submitSuccess = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("FILE ANOTHER REQUEST", color = SlateBackground, fontWeight = FontWeight.Bold)
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

      Text("Item Name *", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
      OutlinedTextField(
        value = itemName,
        onValueChange = { itemName = it },
        placeholder = { Text("e.g. LG 1.5 Ton Split AC") },
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = OnSlateText,
          unfocusedTextColor = OnSlateText,
          focusedBorderColor = GoldPrimary,
          unfocusedBorderColor = SlateSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text("Product Category *", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
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
          visibleCategories.forEach { option ->
            DropdownMenuItem(text = { Text(option.name) }, onClick = { selectCategory(option) })
          }
        }
      }

      if (selectedCategory != null) {
        Spacer(modifier = Modifier.height(14.dp))

        Text("What do you need? *", color = OnSlateTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))

        val category = selectedCategory!!
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Checkbox(
            checked = wantsDemo,
            onCheckedChange = { checked ->
              if (category.canDemo) wantsDemo = checked else promptUnavailable("Demo")
            },
            enabled = category.canDemo,
            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
          )
          Text(
            text = "Demo",
            color = if (category.canDemo) OnSlateText else OnSlateTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier
              .weight(1f)
              .clickableIfDisabled(enabled = category.canDemo) { promptUnavailable("Demo") }
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Checkbox(
            checked = wantsInstallation,
            onCheckedChange = { checked ->
              if (category.canInstall) wantsInstallation = checked else promptUnavailable("Installation")
            },
            enabled = category.canInstall,
            colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
          )
          Text(
            text = "Installation",
            color = if (category.canInstall) OnSlateText else OnSlateTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier
              .weight(1f)
              .clickableIfDisabled(enabled = category.canInstall) { promptUnavailable("Installation") }
          )
        }
      }

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
          if (category == null || itemName.trim().isEmpty() || address.trim().isEmpty()) {
            submitError = "Please fill in item name, category and address."
            return@Button
          }
          if (!wantsDemo && !wantsInstallation) {
            submitError = "Please select demo and/or installation."
            return@Button
          }
          submitError = null
          isSubmitting = true
          coroutineScope.launch {
            try {
              NetworkModule.userApi.createInstallation(
                CreateInstallationRequest(
                  phone = userPhone,
                  invoiceFileId = selectedInvoice?.id,
                  invoiceFileName = selectedInvoice?.name,
                  categoryIconKey = category.iconKey,
                  itemName = itemName.trim(),
                  wantsDemo = wantsDemo,
                  wantsInstallation = wantsInstallation,
                  address = address.trim(),
                  contactPhone = contactPhone.ifEmpty { null }
                )
              )
              submitSuccess = true
            } catch (e: Exception) {
              submitError = "Unable to submit request. Please check your connection and try again."
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
        Text(if (isSubmitting) "SUBMITTING..." else "SUBMIT REQUEST", color = SlateBackground, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

private fun Modifier.clickableIfDisabled(enabled: Boolean, onClick: () -> Unit): Modifier =
  if (enabled) this else this.then(Modifier.clickable(onClick = onClick))

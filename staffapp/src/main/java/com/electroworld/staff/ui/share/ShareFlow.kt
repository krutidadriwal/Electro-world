package com.electroworld.staff.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.electroworld.staff.R
import com.electroworld.staff.data.SessionManager
import com.electroworld.staff.data.network.PriceListItem
import com.electroworld.staff.data.network.lookupCustomerName
import com.electroworld.staff.data.share.QuoteLine
import com.electroworld.staff.data.share.ShareHistory
import com.electroworld.staff.data.share.ShareKind
import com.electroworld.staff.data.share.ShareRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ShareRequest {
  data class Item(val item: PriceListItem, val imageUrl: String?) : ShareRequest()
  // categoryImageUrls: category -> image URL, for each item card's picture.
  data class Quotation(val lines: List<QuoteLine>, val categoryImageUrls: Map<String, String>) : ShareRequest()
}

// Drives one share: customer details dialog -> image drawn on the phone ->
// saved to on-device history -> Android share sheet -> who-to-whom log sent
// to the server (retried later if offline). Call start(); render
// ShareFlowHost once wherever the flow should be able to appear.
@Stable
class ShareFlowState {
  internal var pending by mutableStateOf<ShareRequest?>(null)
  internal var isWorking by mutableStateOf(false)

  // Pre-fills the next dialog, e.g. sharing an item then a quotation to the
  // same customer. Kept for this screen visit only.
  internal var lastCustomer: ShareCustomer? = null

  fun start(request: ShareRequest) {
    pending = request
  }
}

@Composable
fun rememberShareFlowState(): ShareFlowState = remember { ShareFlowState() }

@Composable
fun ShareFlowHost(state: ShareFlowState) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  state.pending?.let { request ->
    CustomerDetailsDialog(
      title = if (request is ShareRequest.Quotation) "Share quotation" else "Share price",
      initial = state.lastCustomer,
      onDismiss = { state.pending = null },
      onConfirm = { customer ->
        state.pending = null
        state.lastCustomer = customer
        state.isWorking = true
        scope.launch {
          try {
            createAndShare(context, request, customer)
          } catch (e: Exception) {
            Toast.makeText(context, "Couldn't create the image. Please try again.", Toast.LENGTH_LONG).show()
          } finally {
            state.isWorking = false
          }
          // After the share sheet is up, so a slow network never delays it.
          ShareHistory.syncPendingLogs()
        }
      }
    )
  }

  if (state.isWorking) PreparingImageDialog()
}

// Shown while a share image is drawn and saved -- also used by RequestShare.kt.
@Composable
internal fun PreparingImageDialog() {
  Dialog(onDismissRequest = {}) {
    Card {
      Row(
        modifier = Modifier.padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text("Preparing image…")
      }
    }
  }
}

// Phone is required; the name auto-fills (and locks) for an existing app
// customer, otherwise staff must type it -- both go into the share log.
@Composable
private fun CustomerDetailsDialog(
  title: String,
  initial: ShareCustomer?,
  onDismiss: () -> Unit,
  onConfirm: (ShareCustomer) -> Unit
) {
  var phone by remember { mutableStateOf(initial?.phone ?: "") }
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var isLookingUp by remember { mutableStateOf(false) }
  var isExistingCustomer by remember { mutableStateOf(false) }

  LaunchedEffect(phone) {
    if (phone.length == 10) {
      isLookingUp = true
      val existingName = lookupCustomerName(phone)
      isLookingUp = false
      if (existingName != null) {
        name = existingName
        isExistingCustomer = true
      } else {
        // Don't leave a previous lookup's name attached to a different number.
        if (isExistingCustomer) name = ""
        isExistingCustomer = false
      }
    } else if (isExistingCustomer) {
      name = ""
      isExistingCustomer = false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column {
        OutlinedTextField(
          value = phone,
          onValueChange = { value -> phone = value.filter { it.isDigit() }.take(10) },
          label = { Text("Customer phone") },
          prefix = { Text("+91 ") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it.take(100) },
          label = { Text("Customer name") },
          singleLine = true,
          enabled = !isExistingCustomer,
          supportingText = {
            Text(
              when {
                isLookingUp -> "Looking up customer…"
                isExistingCustomer -> "Existing customer"
                phone.length == 10 -> "New customer — name required"
                else -> "Enter the 10-digit number first"
              }
            )
          },
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
      }
    },
    confirmButton = {
      TextButton(
        enabled = phone.length == 10 && name.isNotBlank() && !isLookingUp,
        onClick = { onConfirm(ShareCustomer(name.trim(), phone)) }
      ) { Text("Share") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

private suspend fun createAndShare(context: Context, request: ShareRequest, customer: ShareCustomer) {
  val session = SessionManager(context)
  val sharedAt = System.currentTimeMillis()
  val logo = withContext(Dispatchers.IO) { BitmapFactory.decodeResource(context.resources, R.drawable.ew_logo) }
  val details = ShareDetails(
    customer = customer,
    sharedBy = session.name.ifBlank { session.email },
    sharedAt = sharedAt,
    validTill = ShareHistory.validTillFrom(sharedAt),
    logo = logo
  )

  val bitmap = when (request) {
    is ShareRequest.Item -> {
      val productImage = request.imageUrl?.let { loadBitmap(context, it) }
      withContext(Dispatchers.Default) { ShareImageRenderer.renderItem(details, request.item, productImage) }
    }
    is ShareRequest.Quotation -> {
      val images = request.lines.map { it.category }.distinct()
        .mapNotNull { category -> request.categoryImageUrls[category]?.let { url -> loadBitmap(context, url)?.let { category to it } } }
        .toMap()
      withContext(Dispatchers.Default) { ShareImageRenderer.renderQuotation(details, request.lines, images) }
    }
  }

  val file = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "shares").apply { mkdirs() }
    File(dir, "share_$sharedAt.png").also { file ->
      FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
  }

  ShareHistory.add(
    ShareRecord(
      id = UUID.randomUUID().toString(),
      kind = if (request is ShareRequest.Quotation) ShareKind.QUOTATION else ShareKind.ITEM,
      summary = when (request) {
        is ShareRequest.Item -> request.item.itemName
        is ShareRequest.Quotation ->
          if (request.lines.size == 1) "1 item" else "${request.lines.size} items"
      },
      customerName = customer.name,
      customerPhone = customer.phone,
      sharedAt = sharedAt,
      validTill = details.validTill,
      imagePath = file.absolutePath
    )
  )
  launchShareSheet(context, file)
}

// Re-sends a still-valid image from the history sheet (not logged again --
// the original share already was).
fun reshare(context: Context, record: ShareRecord) {
  val file = File(record.imagePath)
  if (!file.exists()) {
    Toast.makeText(context, "That image is no longer on this phone.", Toast.LENGTH_SHORT).show()
    return
  }
  launchShareSheet(context, file)
}

internal fun launchShareSheet(context: Context, file: File) {
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  val send = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newRawUri(null, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  context.startActivity(Intent.createChooser(send, "Share via"))
}

// The category image, via Coil's cache when the screen already showed it.
// Software bitmap (allowHardware(false)) so it can be drawn onto a Canvas.
private suspend fun loadBitmap(context: Context, url: String): Bitmap? = try {
  val result = context.imageLoader.execute(
    ImageRequest.Builder(context).data(url).allowHardware(false).size(1000).build()
  )
  (result as? SuccessResult)?.drawable?.toBitmap()
} catch (e: Exception) {
  null
}

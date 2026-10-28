package com.electroworld.staff.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.SecureFlagPolicy
import coil.compose.AsyncImage
import com.electroworld.staff.BuildConfig
import com.electroworld.staff.R
import com.electroworld.staff.data.share.QuoteCart
import com.electroworld.staff.data.share.QuoteLine
import com.electroworld.staff.data.share.ShareHistory
import com.electroworld.staff.data.share.ShareKind
import com.electroworld.staff.data.share.ShareRecord
import com.electroworld.staff.ui.theme.DeepSlate
import com.electroworld.staff.ui.theme.ObsidianCharcoal
import com.electroworld.staff.ui.theme.PaleGold
import com.electroworld.staff.ui.theme.PlatinumMuted
import com.electroworld.staff.ui.theme.StatusYellow
import com.electroworld.staff.ui.theme.VividAmber
import com.electroworld.staff.ui.theme.staffCardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// The history sheet shows prices and opens in its own window, so it blocks
// screenshots itself rather than relying on the price list's window flag.
@OptIn(ExperimentalMaterial3Api::class)
private val SecureSheet = ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.SecureOn)

// Item names on the quote cards -- the share image's CREAM.
private val Cream = Color(0xFFEDE6DC)

// Bottom bar on the price list while the quotation has anything in it.
@Composable
fun QuoteBar(onClick: () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clickable(onClick = onClick)
  ) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
      val count = QuoteCart.lines.size
      Text(
        if (count == 1) "1 item in quote" else "$count items in quote",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp).weight(1f)
      )
      Text("VIEW QUOTE", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
  }
}

// Full-screen quotation, laid out like the shared image (charcoal page, brand
// header, one dark card per item with its category picture) so staff can
// show it to the customer on the phone itself, not only send it. Drawn in
// the price list's own window, so its FLAG_SECURE covers this screen too.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteScreen(
  categoryImageUrls: Map<String, String>,
  onBack: () -> Unit,
  onShare: (List<QuoteLine>) -> Unit,
  modifier: Modifier = Modifier
) {
  var confirmingClear by remember { mutableStateOf(false) }
  val lines = QuoteCart.lines

  if (confirmingClear) {
    AlertDialog(
      onDismissRequest = { confirmingClear = false },
      title = { Text("Clear quotation?") },
      text = { Text("All ${lines.size} items will be removed.") },
      confirmButton = {
        TextButton(onClick = {
          confirmingClear = false
          QuoteCart.clear()
          onBack()
        }) { Text("Clear") }
      },
      dismissButton = { TextButton(onClick = { confirmingClear = false }) { Text("Cancel") } }
    )
  }

  Scaffold(
    containerColor = ObsidianCharcoal,
    topBar = {
      TopAppBar(
        title = { Text("Quotation") },
        navigationIcon = {
          IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = {
          if (lines.isNotEmpty()) TextButton(onClick = { confirmingClear = true }) { Text("Clear") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianCharcoal)
      )
    },
    bottomBar = {
      if (lines.isNotEmpty()) {
        Button(
          onClick = { onShare(lines.toList()) },
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Text("Share quotation", modifier = Modifier.padding(start = 8.dp))
        }
      }
    },
    modifier = modifier
  ) { padding ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
      item { QuoteHeader() }
      item {
        val now = remember { System.currentTimeMillis() }
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            if (lines.size == 1) "1 item" else "${lines.size} items",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Cream
          )
          Text(
            "${ShareImageRenderer.formatDate(now)}  ·  Valid till ${ShareImageRenderer.formatDate(ShareHistory.validTillFrom(now))}",
            style = MaterialTheme.typography.labelMedium,
            color = PlatinumMuted
          )
        }
      }
      if (lines.isEmpty()) {
        item {
          Text(
            "No items yet. Tap the cart icon on any item to add it.",
            color = PlatinumMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp)
          )
        }
      }
      items(lines, key = { it.itemName }) { line ->
        QuoteItemCard(line, categoryImageUrls[line.category])
      }
      if (lines.isNotEmpty()) item { QuoteFooter() }
    }
  }
}

// The shared image's header band: logo, store name and phone, QUOTATION
// badge, gold rule.
@Composable
private fun QuoteHeader() {
  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Image(
        painter = painterResource(R.drawable.ew_logo),
        contentDescription = null,
        modifier = Modifier.size(width = 64.dp, height = 54.dp)
      )
      Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
        Text(
          BuildConfig.STORE_NAME.ifBlank { "Electro World" },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = PaleGold,
          maxLines = 2
        )
        if (BuildConfig.STORE_PHONE.isNotBlank()) {
          Text(BuildConfig.STORE_PHONE, style = MaterialTheme.typography.labelMedium, color = PlatinumMuted, maxLines = 1)
        }
      }
      Text(
        "QUOTATION",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = VividAmber
      )
    }
    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(PaleGold))
  }
}

// [image]  Item name        MRP 46000.00  [x]
//                           Price 32000.00
@Composable
private fun QuoteItemCard(line: QuoteLine, imageUrl: String?) {
  Card(
    colors = staffCardColors(),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp)
    ) {
      Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        if (imageUrl != null) {
          AsyncImage(
            model = imageUrl,
            contentDescription = line.category,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(64.dp)
          )
        } else {
          Icon(Icons.Default.Image, contentDescription = null, tint = PlatinumMuted, modifier = Modifier.size(32.dp))
        }
      }
      Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
        Text(line.itemName, style = MaterialTheme.typography.bodyLarge, color = Cream, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(line.category, style = MaterialTheme.typography.labelSmall, color = PlatinumMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Column(horizontalAlignment = Alignment.End) {
        if (line.mrp > line.finalPrice) {
          Text(
            buildAnnotatedString {
              append("MRP ")
              withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(ShareImageRenderer.plainAmount(line.mrp))
              }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Cream
          )
        }
        Text(
          "Price ${ShareImageRenderer.plainAmount(line.finalPrice)}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = StatusYellow
        )
      }
      IconButton(onClick = { QuoteCart.remove(line.itemName) }) {
        Icon(Icons.Default.Close, contentDescription = "Remove from quote", tint = PlatinumMuted, modifier = Modifier.size(18.dp))
      }
    }
  }
}

@Composable
private fun QuoteFooter() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 12.dp)
      .background(DeepSlate)
      .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    if (BuildConfig.STORE_ADDRESS.isNotBlank()) {
      Text(BuildConfig.STORE_ADDRESS, style = MaterialTheme.typography.labelMedium, color = PlatinumMuted, textAlign = TextAlign.Center)
    }
    Text(
      "Prices inclusive of GST · Subject to stock availability",
      style = MaterialTheme.typography.labelSmall,
      color = PlatinumMuted,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}

// Still-valid shares made from this phone; tap one to send the same image again.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareHistorySheet(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val records = ShareHistory.active

  ModalBottomSheet(onDismissRequest = onDismiss, properties = SecureSheet) {
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
      Text("Shared in the last 3 days", style = MaterialTheme.typography.titleLarge)
      Text(
        "Tap one to send it again.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
          Text("Nothing shared yet.", color = MaterialTheme.colorScheme.outline)
        }
        return@Column
      }
      LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
        items(records, key = { it.id }) { record ->
          ShareRecordRow(record, onClick = { reshare(context, record) })
        }
      }
    }
  }
}

@Composable
private fun ShareRecordRow(record: ShareRecord, onClick: () -> Unit) {
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick)) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        if (record.kind == ShareKind.QUOTATION) Icons.AutoMirrored.Filled.ReceiptLong else Icons.Default.Sell,
        contentDescription = if (record.kind == ShareKind.QUOTATION) "Quotation" else "Item price",
        tint = MaterialTheme.colorScheme.secondary
      )
      Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
        Text(
          if (record.customerPhone != null) "${record.customerName} · +91 ${record.customerPhone}" else "No customer details",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold
        )
        Text(record.summary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          "Shared ${formatShareTime(record.sharedAt)}" + if (!record.logged) " · log pending" else "",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}

private fun formatShareTime(millis: Long): String =
  SimpleDateFormat("d MMM, h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    .format(Date(millis))

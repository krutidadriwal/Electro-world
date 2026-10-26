package com.electroworld.staff.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.StrikethroughSpan
import com.electroworld.staff.BuildConfig
import com.electroworld.staff.data.network.PriceListItem
import com.electroworld.staff.data.share.QuoteLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

data class ShareCustomer(val name: String, val phone: String)

// Everything printed on a share image besides the item(s) themselves.
data class ShareDetails(
  val customer: ShareCustomer,
  val sharedBy: String,
  val sharedAt: Long,
  val validTill: Long,
  val logo: Bitmap?
)

// Draws the price-list share images ("GPay receipt" style cards) with plain
// android.graphics -- entirely on the phone, no server involved. Plain Canvas
// rather than capturing Compose UI because these are drawn off-screen, can
// be taller than the screen (long quotations), and must look identical on
// every phone regardless of its font scale or theme.
object ShareImageRenderer {
  private const val WIDTH = 1080
  private const val MARGIN = 64f
  private const val CONTENT_WIDTH = (WIDTH - 2 * MARGIN).toInt()

  // Light, print-like card (reads better in WhatsApp than the app's dark
  // theme), with the brand's charcoal + gold for the header.
  private const val CHARCOAL = 0xFF1A1715.toInt()
  private const val GOLD = 0xFFF0CF8C.toInt()
  private const val AMBER = 0xFFFFB800.toInt()
  private const val TEXT = 0xFF1A1715.toInt()
  private const val MUTED = 0xFF7A736C.toInt()
  private const val PANEL = 0xFFF4F1EC.toInt()
  private const val DIVIDER = 0xFFE3DED7.toInt()
  private const val GREEN = 0xFF2E7D32.toInt()
  private const val GREEN_BG = 0xFFE6F2E8.toInt()
  // Dark quotation -- the app's own ObsidianCharcoal / DeepSlate / StatusYellow.
  private const val DEEP_SLATE = 0xFF2D2825.toInt()
  private const val CREAM = 0xFFEDE6DC.toInt()
  private const val DARK_MUTED = 0xFFA8A39D.toInt()
  private const val PRICE_AMBER = 0xFFE2A13B.toInt()

  fun renderItem(details: ShareDetails, item: PriceListItem, productImage: Bitmap?): Bitmap {
    val card = Card()
    card.header(details.logo, badge = "PRICE")
    card.space(48f)

    if (productImage != null) {
      card.block(560f) { c, top ->
        val box = RectF(MARGIN, top, WIDTH - MARGIN, top + 560f)
        c.drawRoundRect(box, 32f, 32f, fill(PANEL))
        box.inset(40f, 32f)
        drawFit(c, productImage, box)
      }
      card.space(40f)
    }

    card.text(item.itemName, paint(54f, TEXT, bold = true), Layout.Alignment.ALIGN_CENTER, maxLines = 3)
    card.space(8f)
    card.text("${item.category} · ${item.groupName}", paint(30f, MUTED), Layout.Alignment.ALIGN_CENTER, maxLines = 2)
    card.space(36f)

    val hasDiscount = item.mrp > item.finalPrice
    if (hasDiscount) {
      card.text("MRP ${rupees(item.mrp)}", paint(38f, MUTED, strike = true), Layout.Alignment.ALIGN_CENTER)
      card.space(4f)
    }
    card.text(rupees(item.finalPrice), paint(120f, TEXT, bold = true), Layout.Alignment.ALIGN_CENTER)
    if (hasDiscount) {
      val savings = item.mrp - item.finalPrice
      val percent = (savings / item.mrp * 100).roundToInt()
      card.space(20f)
      card.pill("You save ${rupees(savings)} ($percent% off)", paint(36f, GREEN, bold = true), GREEN_BG)
    }

    card.space(48f)
    card.divider()
    card.space(32f)
    card.detailRows(details, includeSharedBy = true)
    card.space(40f)
    card.footer(details)
    return card.render()
  }

  // Dark, one card per item -- same look as the app's own list rows: category
  // image, item name, MRP (struck through) and price. No quantities or totals.
  fun renderQuotation(details: ShareDetails, lines: List<QuoteLine>, categoryImages: Map<String, Bitmap>): Bitmap {
    val card = Card()
    card.header(details.logo, badge = "QUOTATION")
    card.space(44f)
    card.text("Quotation for ${details.customer.name}", paint(40f, CREAM, bold = true), maxLines = 2)
    card.space(8f)
    card.text(
      "+91 ${details.customer.phone}  ·  ${formatDate(details.sharedAt)}  ·  Valid till ${formatDate(details.validTill)}",
      paint(28f, DARK_MUTED),
      maxLines = 2
    )
    card.space(4f)
    card.text("Shared by ${details.sharedBy}", paint(28f, DARK_MUTED), maxLines = 1)
    card.space(40f)
    lines.forEach { line -> card.quoteItemCard(line, categoryImages[line.category]) }
    card.space(16f)
    card.footer(details, dark = true)
    return card.render(background = CHARCOAL)
  }

  // A complaint / installation request's detail screen as an image: the same
  // fields, in the same order, label above value. No prices, so no
  // GST/validity footer -- just who shared it and when.
  fun renderRequestDetails(content: RequestShareContent, sharedBy: String, sharedAt: Long, logo: Bitmap?): Bitmap {
    val card = Card()
    card.header(logo, badge = content.badge)
    card.space(44f)
    card.text(content.title.uppercase(), paint(26f, MUTED, bold = true).apply { letterSpacing = 0.1f })
    card.space(12f)
    card.text(content.customerName, paint(52f, TEXT, bold = true), maxLines = 2)
    card.space(4f)
    card.text(content.customerPhone, paint(32f, MUTED), maxLines = 1)
    card.space(32f)
    card.divider()
    card.space(28f)
    content.fields.forEach { (label, value) ->
      card.text(label, paint(26f, MUTED))
      card.space(4f)
      card.text(value, paint(34f, TEXT))
      card.space(24f)
    }
    card.divider()
    card.space(24f)
    card.text(
      "Shared by $sharedBy · ${formatDateTime(sharedAt)}",
      paint(26f, MUTED),
      Layout.Alignment.ALIGN_CENTER,
      maxLines = 2
    )
    card.space(32f)
    card.addressFooter()
    return card.render()
  }

  // --- building blocks -----------------------------------------------------

  // A card is built top to bottom as a list of fixed-height blocks; the
  // bitmap is only allocated once the total height is known.
  private class Card {
    private val ops = mutableListOf<(Canvas) -> Unit>()
    var height = 0f
      private set

    fun block(blockHeight: Float, draw: (Canvas, Float) -> Unit) {
      val top = height
      ops += { canvas -> draw(canvas, top) }
      height += blockHeight
    }

    fun space(amount: Float) {
      height += amount
    }

    fun render(background: Int = android.graphics.Color.WHITE): Bitmap {
      val bitmap = Bitmap.createBitmap(WIDTH, ceil(height).toInt(), Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      canvas.drawColor(background)
      ops.forEach { it(canvas) }
      return bitmap
    }
  }

  private fun Card.text(
    value: String,
    paint: TextPaint,
    align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    maxLines: Int = Int.MAX_VALUE,
    left: Float = MARGIN,
    width: Int = CONTENT_WIDTH
  ) {
    val layout = layout(value, paint, width, align, maxLines)
    block(layout.height.toFloat()) { c, top -> drawLayout(c, layout, left, top) }
  }

  private fun Card.header(logo: Bitmap?, badge: String) {
    val bandHeight = 220f
    val storeName = BuildConfig.STORE_NAME.ifBlank { "Electro World" }
    val logoBox = RectF(MARGIN, 40f, MARGIN + 166f, 180f)
    val textLeft = if (logo != null) logoBox.right + 32f else MARGIN
    val badgePaint = paint(30f, AMBER, bold = true).apply { letterSpacing = 0.15f }
    val badgeWidth = badgePaint.measureText(badge)
    val textWidth = (WIDTH - MARGIN - badgeWidth - 32f - textLeft).toInt()
    val nameLayout = layout(storeName, paint(46f, GOLD, bold = true), textWidth, maxLines = 2)
    val phoneLayout = BuildConfig.STORE_PHONE.takeIf { it.isNotBlank() }
      ?.let { layout(it, paint(28f, 0xFFCFC9C2.toInt()), textWidth, maxLines = 1) }

    block(bandHeight) { c, top ->
      c.drawRect(0f, top, WIDTH.toFloat(), top + bandHeight, fill(CHARCOAL))
      logo?.let { drawFit(c, it, RectF(logoBox.left, top + logoBox.top, logoBox.right, top + logoBox.bottom)) }
      val textHeight = nameLayout.height + (phoneLayout?.height?.plus(6) ?: 0)
      var y = top + (bandHeight - textHeight) / 2f
      drawLayout(c, nameLayout, textLeft, y)
      y += nameLayout.height + 6
      phoneLayout?.let { drawLayout(c, it, textLeft, y) }
      c.drawText(badge, WIDTH - MARGIN - badgeWidth, top + bandHeight / 2f + 10f, badgePaint)
    }
    block(8f) { c, top -> c.drawRect(0f, top, WIDTH.toFloat(), top + 8f, fill(GOLD)) }
  }

  private fun Card.pill(value: String, paint: TextPaint, background: Int) {
    val textWidth = paint.measureText(value)
    val pillHeight = paint.textSize + 36f
    block(pillHeight) { c, top ->
      val left = (WIDTH - textWidth) / 2f - 32f
      c.drawRoundRect(RectF(left, top, WIDTH - left, top + pillHeight), pillHeight / 2, pillHeight / 2, fill(background))
      c.drawText(value, (WIDTH - textWidth) / 2f, top + pillHeight / 2f - (paint.ascent() + paint.descent()) / 2f, paint)
    }
  }

  private fun Card.divider() {
    block(2f) { c, top -> c.drawRect(MARGIN, top, WIDTH - MARGIN, top + 2f, fill(DIVIDER)) }
  }

  private fun Card.detailRows(details: ShareDetails, includeSharedBy: Boolean) {
    labelValue("Prepared for", details.customer.name)
    labelValue("Phone", "+91 ${details.customer.phone}")
    labelValue("Date", formatDate(details.sharedAt))
    labelValue("Valid till", formatDate(details.validTill))
    if (includeSharedBy) labelValue("Shared by", details.sharedBy)
  }

  private fun Card.labelValue(label: String, value: String) {
    val labelWidth = 260
    val labelLayout = layout(label, paint(28f, MUTED), labelWidth, maxLines = 1)
    val valueLayout = layout(value, paint(30f, TEXT, bold = true), CONTENT_WIDTH - labelWidth, maxLines = 2)
    val rowHeight = maxOf(labelLayout.height, valueLayout.height) + 18f
    block(rowHeight) { c, top ->
      drawLayout(c, labelLayout, MARGIN, top + 2f)
      drawLayout(c, valueLayout, MARGIN + labelWidth, top)
    }
  }

  // [image]  Item name            MRP 46000.00
  //                               Price 32000.00
  private fun Card.quoteItemCard(line: QuoteLine, image: Bitmap?) {
    val cardLeft = 40f
    val cardRight = WIDTH - 40f
    val padding = 40f
    val imageSize = 170f
    val priceWidth = 380
    val nameLeft = cardLeft + padding + imageSize + 32f
    val nameWidth = (cardRight - padding - priceWidth - 24f - nameLeft).toInt()

    val nameLayout = layout(line.itemName, paint(46f, CREAM), nameWidth, maxLines = 3)
    val mrpLayout = if (line.mrp > line.finalPrice) {
      val amount = plainAmount(line.mrp)
      val text = SpannableStringBuilder("MRP ").append(amount)
      text.setSpan(StrikethroughSpan(), text.length - amount.length, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      layout(text, paint(38f, CREAM), priceWidth, Layout.Alignment.ALIGN_OPPOSITE, maxLines = 1)
    } else {
      null
    }
    val priceLayout = layout("Price ${plainAmount(line.finalPrice)}", paint(40f, PRICE_AMBER), priceWidth, Layout.Alignment.ALIGN_OPPOSITE, maxLines = 1)
    val priceBlockHeight = (mrpLayout?.height?.plus(10) ?: 0) + priceLayout.height
    val contentHeight = maxOf(imageSize, nameLayout.height.toFloat(), priceBlockHeight.toFloat())
    val cardHeight = contentHeight + 2 * padding

    block(cardHeight + 24f) { c, top ->
      c.drawRoundRect(RectF(cardLeft, top, cardRight, top + cardHeight), 36f, 36f, fill(DEEP_SLATE))
      val centerY = top + cardHeight / 2f
      image?.let {
        val left = cardLeft + padding
        drawFit(c, it, RectF(left, centerY - imageSize / 2f, left + imageSize, centerY + imageSize / 2f))
      }
      drawLayout(c, nameLayout, nameLeft, centerY - nameLayout.height / 2f)
      var y = centerY - priceBlockHeight / 2f
      val priceLeft = cardRight - padding - priceWidth
      mrpLayout?.let {
        drawLayout(c, it, priceLeft, y)
        y += it.height + 10
      }
      drawLayout(c, priceLayout, priceLeft, y)
    }
  }

  private fun Card.footer(details: ShareDetails, dark: Boolean = false) {
    val lines = listOfNotNull(
      BuildConfig.STORE_ADDRESS.takeIf { it.isNotBlank() },
      "Prices inclusive of GST · Valid till ${formatDate(details.validTill)} · Subject to stock availability"
    )
    val layouts = lines.mapIndexed { index, line ->
      layout(line, paint(if (index == 0) 28f else 24f, if (dark) DARK_MUTED else MUTED), CONTENT_WIDTH, Layout.Alignment.ALIGN_CENTER)
    }
    val footerHeight = layouts.sumOf { it.height } + 12f * (layouts.size - 1) + 64f
    block(footerHeight) { c, top ->
      c.drawRect(0f, top, WIDTH.toFloat(), top + footerHeight, fill(if (dark) DEEP_SLATE else PANEL))
      var y = top + 32f
      layouts.forEach {
        drawLayout(c, it, MARGIN, y)
        y += it.height + 12f
      }
    }
    block(8f) { c, top -> c.drawRect(0f, top, WIDTH.toFloat(), top + 8f, fill(GOLD)) }
  }

  // Store address band + gold rule, for images without prices.
  private fun Card.addressFooter() {
    val address = BuildConfig.STORE_ADDRESS.takeIf { it.isNotBlank() }
    if (address != null) {
      val layout = layout(address, paint(28f, MUTED), CONTENT_WIDTH, Layout.Alignment.ALIGN_CENTER)
      val footerHeight = layout.height + 64f
      block(footerHeight) { c, top ->
        c.drawRect(0f, top, WIDTH.toFloat(), top + footerHeight, fill(PANEL))
        drawLayout(c, layout, MARGIN, top + 32f)
      }
    }
    block(8f) { c, top -> c.drawRect(0f, top, WIDTH.toFloat(), top + 8f, fill(GOLD)) }
  }

  // --- low-level helpers ---------------------------------------------------

  private fun paint(size: Float, color: Int, bold: Boolean = false, strike: Boolean = false) =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = size
      this.color = color
      typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
      isStrikeThruText = strike
    }

  private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

  private fun layout(
    value: CharSequence,
    paint: TextPaint,
    width: Int,
    align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    maxLines: Int = Int.MAX_VALUE
  ): StaticLayout =
    StaticLayout.Builder.obtain(value, 0, value.length, paint, width.coerceAtLeast(1))
      .setAlignment(align)
      .setIncludePad(false)
      .setMaxLines(maxLines)
      .setEllipsize(TextUtils.TruncateAt.END)
      .build()

  private fun drawLayout(canvas: Canvas, layout: StaticLayout, left: Float, top: Float) {
    canvas.save()
    canvas.translate(left, top)
    layout.draw(canvas)
    canvas.restore()
  }

  private fun drawFit(canvas: Canvas, bitmap: Bitmap, box: RectF) {
    val scale = min(box.width() / bitmap.width, box.height() / bitmap.height)
    val w = bitmap.width * scale
    val h = bitmap.height * scale
    val dst = RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2)
    canvas.drawBitmap(bitmap, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
  }

  // Same grouping as the price list screen, so the image matches what the
  // customer was just shown.
  private fun amountText(value: Double): String = String.format(Locale.US, "%,d", Math.round(value))

  fun plainAmount(value: Double): String = String.format(Locale.US, "%.2f", value)

  fun rupees(value: Double): String = "₹${amountText(value)}"

  fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
      .format(Date(millis))

  private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
      .format(Date(millis))
}

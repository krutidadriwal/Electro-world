package com.electroworld.staff

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.electroworld.staff.data.network.PriceListItem
import com.electroworld.staff.data.share.QuoteLine
import com.electroworld.staff.ui.share.ShareCustomer
import com.electroworld.staff.ui.share.ShareDetails
import com.electroworld.staff.ui.share.ShareImageRenderer
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// TEMPORARY: renders sample share images to the app's files dir for a visual check.
@RunWith(AndroidJUnit4::class)
class SharePreviewTest {
  @Test
  fun renderSamples() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ew_logo)
    val now = System.currentTimeMillis()
    val details = ShareDetails(ShareCustomer("Asha Verma", "9876543210"), "Kruti", now, now + 3 * 86_400_000L, logo)
    val item = PriceListItem("Air Conditioner", "LG Split AC 1.5 Ton", "LG 1.5 Ton 5 Star AI DUAL Inverter Split AC (US-Q19YNZE)", 41490.0, 52990.0, null, "12 pcs")
    val lines = listOf(
      QuoteLine("AC 2.0 TR Fix Speed", "AC", "Haier", 46000.0, 32000.0),
      QuoteLine("Air Conditioner Haier 1.5 Ton", "AC", "Haier", 46000.0, 32000.0),
      QuoteLine("Samsung 253L 3 Star Frost Free Double Door Refrigerator", "REF", "Samsung", 31000.0, 26490.0),
      QuoteLine("Havells Ceiling Fan", "FAN", "Havells", 3200.0, 3200.0)
    )
    val dir = File(context.filesDir, "preview").apply { mkdirs() }
    save(ShareImageRenderer.renderItem(details, item, logo), File(dir, "item.png"))
    save(ShareImageRenderer.renderQuotation(details, lines, mapOf("AC" to logo, "REF" to logo)), File(dir, "quote.png"))
  }

  private fun save(bitmap: Bitmap, file: File) {
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }
}

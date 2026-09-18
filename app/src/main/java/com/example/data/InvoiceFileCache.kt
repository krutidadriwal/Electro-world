package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.network.InvoiceFile
import com.example.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Caches downloaded invoice PDFs on disk, keyed by phone + invoice id, so
 * opening the same invoice a second time never re-hits the server/Drive --
 * it's served straight from local storage instead.
 */
object InvoiceFileCache {

  private fun cacheFile(context: Context, phone: String, invoiceId: String): File {
    val dir = File(context.filesDir, "invoices/$phone")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "$invoiceId.pdf")
  }

  /**
   * Returns the local file for this invoice, downloading and caching it
   * first if it isn't already on disk. A non-empty existing file is trusted
   * as-is -- invoice PDFs are immutable once issued, so there's no need to
   * ever re-check the server once a copy is cached.
   */
  suspend fun getOrDownload(context: Context, phone: String, invoice: InvoiceFile): File {
    val file = cacheFile(context, phone, invoice.id)
    if (file.exists() && file.length() > 0) {
      return file
    }

    return withContext(Dispatchers.IO) {
      val body = NetworkModule.userApi.downloadInvoiceFile(id = invoice.id, phone = phone)
      // Download to a temp file first and rename on success, so a failed/
      // interrupted download never leaves a corrupt file mistaken for a
      // valid cache hit on the next open.
      val tempFile = File(file.parentFile, "${invoice.id}.pdf.tmp")
      body.byteStream().use { input ->
        tempFile.outputStream().use { output -> input.copyTo(output) }
      }
      tempFile.renameTo(file)
      file
    }
  }

  fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
  }
}

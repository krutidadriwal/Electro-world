package com.electroworld.staff.data.share

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.ShareLogRequest
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// How long a shared price/quotation is valid -- printed on the image, and
// how long it stays in the on-device history.
const val SHARE_VALIDITY_DAYS = 3

enum class ShareKind { ITEM, QUOTATION }

@JsonClass(generateAdapter = true)
data class ShareRecord(
  val id: String,
  val kind: ShareKind,
  // e.g. the item name, or "4 items · ₹1,24,500"
  val summary: String,
  // Both null when an admin shared it (no customer dialog for admins).
  val customerName: String? = null,
  val customerPhone: String? = null,
  val sharedAt: Long,
  val validTill: Long,
  val imagePath: String,
  // Whether the who-shared-to-whom log reached the server yet.
  val logged: Boolean = false
) {
  val isExpired: Boolean get() = System.currentTimeMillis() > validTill
}

// On-device history of shared images, so staff can see (and re-send) what
// they shared that is still valid. Expired records are pruned along with
// their image files -- but only once their server log has gone through, so
// an offline share is never silently dropped from the logs.
object ShareHistory {
  private const val PREFS_NAME = "electro_world_share_history"
  private const val KEY_RECORDS = "records"

  private var prefs: SharedPreferences? = null
  private val adapter = Moshi.Builder().build()
    .adapter<List<ShareRecord>>(Types.newParameterizedType(List::class.java, ShareRecord::class.java))
  private val syncMutex = Mutex()

  val records = mutableStateListOf<ShareRecord>()

  fun init(context: Context) {
    if (prefs != null) return
    prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val saved = prefs?.getString(KEY_RECORDS, null)
    records.addAll(saved?.let { runCatching { adapter.fromJson(it) }.getOrNull() }.orEmpty())
    prune()
  }

  // Newest first, still-valid only -- what the history sheet shows.
  val active: List<ShareRecord> get() = records.filterNot { it.isExpired }.sortedByDescending { it.sharedAt }

  fun validTillFrom(sharedAt: Long): Long = sharedAt + TimeUnit.DAYS.toMillis(SHARE_VALIDITY_DAYS.toLong())

  fun add(record: ShareRecord) {
    records.add(record)
    save()
  }

  // Sends every not-yet-logged share to the server. Safe to call often (after
  // each share, and whenever the price list opens); failures just stay
  // pending for next time.
  suspend fun syncPendingLogs() = syncMutex.withLock {
    records.filterNot { it.logged }.forEach { record ->
      try {
        NetworkModule.shareLogApi.log(
          ShareLogRequest(record.customerPhone, record.customerName, isoUtc(record.sharedAt))
        )
        val index = records.indexOfFirst { it.id == record.id }
        if (index >= 0) records[index] = records[index].copy(logged = true)
      } catch (e: Exception) {
        return@withLock // offline or server down -- try again later
      } finally {
        save()
      }
    }
    prune()
  }

  private fun prune() {
    val (expired, kept) = records.partition { it.isExpired && it.logged }
    if (expired.isEmpty()) return
    expired.forEach { File(it.imagePath).delete() }
    records.clear()
    records.addAll(kept)
    save()
  }

  private fun save() {
    prefs?.edit()?.putString(KEY_RECORDS, adapter.toJson(records.toList()))?.apply()
  }

  private fun isoUtc(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
      .format(Date(millis))
}

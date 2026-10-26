package com.electroworld.staff.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electroworld.staff.data.network.ActivityLogEntry
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.StaffMember
import com.electroworld.staff.ui.theme.staffCardColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Admin-only: everything staff have done (status changes, calls, shares,
// spins, broadcasts...), newest first, as plain sentences for an owner to
// skim -- see server/lib/activityLog.js for what gets logged.

private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

// The "Work" filter. Keys match WORK_TYPES in server/lib/activityLog.js;
// icons match the dashboard tiles for the same area.
private data class WorkType(val key: String, val label: String, val icon: ImageVector)

private val WORK_TYPES = listOf(
  WorkType("wishlist", "Wishlist", Icons.Default.Favorite),
  WorkType("complaint", "Complaints", Icons.Default.Warning),
  WorkType("installation", "Installations", Icons.Default.Build),
  WorkType("invoice", "Invoices", Icons.Default.Receipt),
  WorkType("price_share", "Price sharing", Icons.Default.Share),
  WorkType("spin", "Spin to Win", Icons.Default.Casino),
  WorkType("broadcast", "Broadcasts", Icons.Default.Campaign),
  WorkType("staff", "Staff accounts", Icons.Default.People),
  WorkType("price_list", "Price list", Icons.Default.Sell)
)

private enum class DatePreset(val label: String) {
  TODAY("Today"),
  YESTERDAY("Yesterday"),
  LAST_7_DAYS("Last 7 days"),
  THIS_MONTH("This month"),
  ALL_TIME("All time"),
  CUSTOM("Custom range...")
}

// The chosen date filter. For CUSTOM, customStart/customEnd are the picker's
// own values: midnight UTC of the first and last day picked.
private data class DateFilter(
  val preset: DatePreset,
  val customStart: Long? = null,
  val customEnd: Long? = null
)

private fun startOfDayIst(millis: Long): Calendar = Calendar.getInstance(IST).apply {
  timeInMillis = millis
  set(Calendar.HOUR_OF_DAY, 0)
  set(Calendar.MINUTE, 0)
  set(Calendar.SECOND, 0)
  set(Calendar.MILLISECOND, 0)
}

// A picker date (midnight UTC of that calendar day) as the start of that
// same calendar day in IST.
private fun pickerDayStartIst(pickerMillis: Long): Calendar {
  val utc = Calendar.getInstance(UTC).apply { timeInMillis = pickerMillis }
  return Calendar.getInstance(IST).apply {
    clear()
    set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
  }
}

private fun Calendar.plusDays(days: Int): Calendar =
  (clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, days) }

// [from, to) in millis for the server, in IST day boundaries since that's
// where the shop is. null = unbounded on that side.
private fun DateFilter.range(now: Long = System.currentTimeMillis()): Pair<Long?, Long?> {
  val today = startOfDayIst(now)
  return when (preset) {
    DatePreset.TODAY -> today.timeInMillis to null
    DatePreset.YESTERDAY -> today.plusDays(-1).timeInMillis to today.timeInMillis
    DatePreset.LAST_7_DAYS -> today.plusDays(-6).timeInMillis to null
    DatePreset.THIS_MONTH -> (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis to null
    DatePreset.ALL_TIME -> null to null
    DatePreset.CUSTOM -> {
      val start = customStart?.let { pickerDayStartIst(it) }
      val end = (customEnd ?: customStart)?.let { pickerDayStartIst(it).plusDays(1) }
      start?.timeInMillis to end?.timeInMillis
    }
  }
}

private fun DateFilter.label(): String {
  if (preset != DatePreset.CUSTOM || customStart == null) return preset.label
  val formatter = SimpleDateFormat("d MMM", Locale.US).apply { timeZone = UTC }
  val start = formatter.format(Date(customStart))
  return if (customEnd == null || customEnd == customStart) start else "$start - ${formatter.format(Date(customEnd))}"
}

private fun isoUtc(millis: Long): String =
  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = UTC }.format(Date(millis))

private fun parseServerTimestamp(isoTimestamp: String): Date? = try {
  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = UTC }
    .parse(isoTimestamp.substringBefore('.').substringBefore('Z'))
} catch (e: Exception) {
  null
}

// "Today" / "Yesterday" / "Mon, 20 Oct 2026" -- the heading entries are
// grouped under.
private fun dayHeading(date: Date, now: Long = System.currentTimeMillis()): String {
  val day = startOfDayIst(date.time).timeInMillis
  val today = startOfDayIst(now)
  return when (day) {
    today.timeInMillis -> "Today"
    today.plusDays(-1).timeInMillis -> "Yesterday"
    else -> SimpleDateFormat("EEE, d MMM yyyy", Locale.US).apply { timeZone = IST }.format(date)
  }
}

private fun timeOfDay(date: Date): String =
  SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = IST }.format(date)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  var staff by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
  var staffFilter by remember { mutableStateOf<StaffMember?>(null) }
  var dateFilter by remember { mutableStateOf(DateFilter(DatePreset.LAST_7_DAYS)) }
  var workFilter by remember { mutableStateOf<WorkType?>(null) }
  var showRangePicker by remember { mutableStateOf(false) }
  var refreshTrigger by remember { mutableIntStateOf(0) }

  var entries by remember { mutableStateOf<List<ActivityLogEntry>>(emptyList()) }
  var nextCursor by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Worked out once per filter change, so "Today" and the pages after the
  // first all use the same boundaries even if midnight passes meanwhile.
  val range = remember(dateFilter, refreshTrigger) { dateFilter.range() }

  suspend fun fetchPage(before: String?) = NetworkModule.activityLogApi.list(
    staffId = staffFilter?.id,
    workType = workFilter?.key,
    from = range.first?.let(::isoUtc),
    to = range.second?.let(::isoUtc),
    before = before
  )

  LaunchedEffect(Unit) {
    try {
      staff = NetworkModule.staffApi.list().staff.sortedBy { it.displayName.lowercase() }
    } catch (e: Exception) {
      // The employee filter just stays at "Everyone".
    }
  }

  LaunchedEffect(staffFilter, workFilter, range, refreshTrigger) {
    isLoading = true
    errorMessage = null
    entries = emptyList()
    nextCursor = null
    try {
      val page = fetchPage(before = null)
      entries = page.items
      nextCursor = page.nextCursor
    } catch (e: Exception) {
      errorMessage = "Failed to load activity."
    } finally {
      isLoading = false
    }
  }

  if (showRangePicker) {
    val pickerState = rememberDateRangePickerState(
      initialSelectedStartDateMillis = dateFilter.customStart,
      initialSelectedEndDateMillis = dateFilter.customEnd
    )
    DatePickerDialog(
      onDismissRequest = { showRangePicker = false },
      confirmButton = {
        TextButton(
          enabled = pickerState.selectedStartDateMillis != null,
          onClick = {
            dateFilter = DateFilter(DatePreset.CUSTOM, pickerState.selectedStartDateMillis, pickerState.selectedEndDateMillis)
            showRangePicker = false
          }
        ) { Text("OK") }
      },
      dismissButton = { TextButton(onClick = { showRangePicker = false }) { Text("Cancel") } }
    ) {
      DateRangePicker(
        state = pickerState,
        title = { Text("Pick dates", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
        showModeToggle = false,
        modifier = Modifier.weight(1f)
      )
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Activity Log") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { refreshTrigger++ }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      FilterDropdown(
        label = "Employee",
        value = staffFilter?.displayName ?: "Everyone",
        options = listOf("Everyone" to null) + staff.map { it.displayName to it },
        onSelect = { staffFilter = it },
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp)
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterDropdown(
          label = "Date",
          value = dateFilter.label(),
          options = DatePreset.entries.map { it.label to it },
          onSelect = { preset ->
            if (preset == DatePreset.CUSTOM) showRangePicker = true else dateFilter = DateFilter(preset)
          },
          modifier = Modifier.weight(1f)
        )
        FilterDropdown(
          label = "Work",
          value = workFilter?.label ?: "All work",
          options = listOf("All work" to null) + WORK_TYPES.map { it.label to it },
          onSelect = { workFilter = it },
          modifier = Modifier.weight(1f)
        )
      }

      errorMessage?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
      }

      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else if (entries.isEmpty() && errorMessage == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No activity for these filters.", color = MaterialTheme.colorScheme.outline)
        }
      } else {
        val grouped = entries
          .mapNotNull { entry -> parseServerTimestamp(entry.createdAt)?.let { entry to it } }
          .groupBy { (_, date) -> dayHeading(date) }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
          grouped.forEach { (heading, dayEntries) ->
            item(key = "heading-$heading") {
              Text(
                heading,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
              )
            }
            items(dayEntries, key = { (entry, _) -> entry.id }) { (entry, date) ->
              ActivityRow(entry, date)
            }
          }
          // Reaching the end loads the next (older) page. Keyed on the
          // cursor so each page is only requested once; leaves composition
          // (cancelling the request) if the filters change meanwhile.
          nextCursor?.let { cursor ->
            item(key = "load-more-$cursor") {
              LaunchedEffect(cursor) {
                try {
                  val page = fetchPage(before = cursor)
                  entries = entries + page.items
                  nextCursor = page.nextCursor
                } catch (e: Exception) {
                  errorMessage = "Failed to load older activity."
                  nextCursor = null
                }
              }
              Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActivityRow(entry: ActivityLogEntry, date: Date) {
  val workType = WORK_TYPES.firstOrNull { it.key == entry.workType }
  Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
      Box(
        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        workType?.let {
          Icon(it.icon, contentDescription = it.label, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        }
      }
      Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
        Text(
          buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(entry.staffName) }
            append(" ")
            append(entry.description)
          },
          style = MaterialTheme.typography.bodyMedium
        )
        entry.customerPhone?.let { phone ->
          val customer = entry.customerName?.takeIf { it.isNotBlank() }?.let { "$it · $phone" } ?: phone
          Text(
            "Customer: $customer",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
      Text(
        timeOfDay(date),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 8.dp)
      )
    }
  }
}

// Read-only field that opens a menu of options -- same tap-overlay pattern
// as the status filter on WishlistScreen (a readOnly OutlinedTextField
// swallows taps itself).
@Composable
private fun <T> FilterDropdown(
  label: String,
  value: String,
  options: List<Pair<String, T>>,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text(label) },
      trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
      modifier = Modifier.fillMaxWidth()
    )
    Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { (text, option) ->
        DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(option) })
      }
    }
  }
}

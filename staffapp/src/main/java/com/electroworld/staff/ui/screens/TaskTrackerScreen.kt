package com.electroworld.staff.ui.screens

// EXPERIMENTAL -- Task Tracker. Admin-only board where the owner posts the
// development work that's needed and the developer moves it through
// statuses. May grow into a task tracker for all employees, or be removed.
//
// Shown only when the build flag TASK_TRACKER_MODE (root .env, read by the
// secrets plugin into BuildConfig) is "true" -- the server has its own flag
// of the same name. To remove the feature from the app:
//   1. delete this file and data/network/TaskTrackerApi.kt
//   2. delete the `taskTrackerApi` line in NetworkModule.kt
//   3. delete every line tagged "task tracker" in HomeScreen.kt
//   4. drop TASK_TRACKER_MODE from .env / .env.example
// (server-side steps are listed in server/api/tasks/[...path].js)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.electroworld.staff.BuildConfig
import com.electroworld.staff.data.network.CreateTaskRequest
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.StaffMember
import com.electroworld.staff.data.network.TrackerTask
import com.electroworld.staff.data.network.UpdateTaskRequest
import com.electroworld.staff.ui.theme.StatusGreen
import com.electroworld.staff.ui.theme.StatusRed
import com.electroworld.staff.ui.theme.StatusYellow
import com.electroworld.staff.ui.theme.statusCardColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TaskTrackerFeature {
  val isEnabled: Boolean = BuildConfig.TASK_TRACKER_MODE == "true"
}

// Must match STATUSES / PRIORITIES in server/api/tasks/[...path].js.
private val TASK_STATUSES = listOf("todo", "in_progress", "testing", "done", "wont_do")
private val ACTIVE_STATUSES = setOf("todo", "in_progress", "testing")
private val TASK_PRIORITIES = listOf("urgent", "high", "normal", "low")

private fun taskStatusLabel(status: String): String = when (status) {
  "todo" -> "To do"
  "in_progress" -> "In progress"
  "testing" -> "Ready to test"
  "done" -> "Done"
  "wont_do" -> "Won't do"
  else -> status
}

private fun taskStatusColor(status: String): Color? = when (status) {
  "in_progress", "testing" -> StatusYellow
  "done" -> StatusGreen
  else -> null
}

private fun priorityLabel(priority: String): String = priority.replaceFirstChar { it.uppercase() }

// Filter chips: a virtual "Active" bucket (default), each real status, and All.
private const val FILTER_ACTIVE = "active"
private const val FILTER_ALL = "all"
private val FILTERS = listOf(FILTER_ACTIVE) + TASK_STATUSES + FILTER_ALL

private fun filterLabel(filter: String): String = when (filter) {
  FILTER_ACTIVE -> "Active"
  FILTER_ALL -> "All"
  else -> taskStatusLabel(filter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTrackerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  var tasks by remember { mutableStateOf<List<TrackerTask>>(emptyList()) }
  var staff by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var staffError by remember { mutableStateOf<String?>(null) }
  var refreshTrigger by remember { mutableStateOf(0) }
  var filter by remember { mutableStateOf(FILTER_ACTIVE) }
  // null = no editor open; editingNew = the "new task" editor.
  var editingTask by remember { mutableStateOf<TrackerTask?>(null) }
  var editingNew by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(refreshTrigger) {
    isLoading = true
    try {
      tasks = NetworkModule.taskTrackerApi.list().items
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Failed to load tasks (${describeError(e)})."
    } finally {
      isLoading = false
    }
    // Loaded separately so a task-list failure can't leave the assignee
    // picker silently empty.
    try {
      staff = NetworkModule.taskTrackerApi.assignees().staff
      staffError = null
    } catch (e: Exception) {
      staffError = "Couldn't load the staff list (${describeError(e)})."
    }
  }

  // Runs a write, then reloads -- every mutation on this screen goes through here.
  fun mutate(failureMessage: String, block: suspend () -> Unit) {
    scope.launch {
      try {
        block()
        refreshTrigger++
      } catch (e: Exception) {
        errorMessage = failureMessage
      }
    }
  }

  // Most urgent first, then newest first (server already returns newest first,
  // and sortedBy is stable).
  val visibleTasks = tasks
    .filter {
      when (filter) {
        FILTER_ACTIVE -> it.status in ACTIVE_STATUSES
        FILTER_ALL -> true
        else -> it.status == filter
      }
    }
    .sortedBy { TASK_PRIORITIES.indexOf(it.priority).let { i -> if (i < 0) TASK_PRIORITIES.size else i } }

  if (editingNew || editingTask != null) {
    TaskEditorDialog(
      task = editingTask,
      staff = staff,
      staffError = staffError,
      onDismiss = {
        editingNew = false
        editingTask = null
      },
      onSave = { title, description, priority, assigneeId ->
        val existing = editingTask
        editingNew = false
        editingTask = null
        if (existing == null) {
          mutate("Failed to create task.") {
            NetworkModule.taskTrackerApi.create(CreateTaskRequest(title, description, priority, assigneeId))
          }
        } else {
          mutate("Failed to save task.") {
            NetworkModule.taskTrackerApi.update(
              UpdateTaskRequest(
                id = existing.id,
                title = title,
                description = description,
                priority = priority,
                assignedTo = assigneeId ?: ""
              )
            )
          }
        }
      },
      onDelete = { task ->
        editingTask = null
        mutate("Failed to delete task.") { NetworkModule.taskTrackerApi.delete(task.id) }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Task Tracker") },
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
    floatingActionButton = {
      FloatingActionButton(onClick = { editingNew = true }) {
        Icon(Icons.Default.Add, contentDescription = "New task")
      }
    },
    modifier = modifier
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FILTERS.forEach { option ->
          val count = tasks.count {
            when (option) {
              FILTER_ACTIVE -> it.status in ACTIVE_STATUSES
              FILTER_ALL -> true
              else -> it.status == option
            }
          }
          FilterChip(
            selected = filter == option,
            onClick = { filter = option },
            label = { Text("${filterLabel(option)} ($count)") }
          )
        }
      }

      errorMessage?.let {
        Text(
          it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
      }

      if (isLoading && tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else if (visibleTasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            if (tasks.isEmpty()) "No tasks yet. Tap + to add one." else "Nothing here.",
            color = MaterialTheme.colorScheme.outline
          )
        }
      } else {
        // Bottom padding keeps the last card clear of the FAB.
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)) {
          items(visibleTasks, key = { it.id }) { task ->
            TaskRow(
              task = task,
              onClick = { editingTask = task },
              onStatusSelected = { newStatus ->
                mutate("Failed to update status.") {
                  NetworkModule.taskTrackerApi.update(UpdateTaskRequest(id = task.id, status = newStatus))
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TaskRow(task: TrackerTask, onClick: () -> Unit, onStatusSelected: (String) -> Unit) {
  var statusMenuExpanded by remember { mutableStateOf(false) }

  Card(
    colors = statusCardColors(taskStatusColor(task.status)),
    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          task.title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )
        Text(
          priorityLabel(task.priority),
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (task.priority == "urgent" || task.priority == "high") StatusRed else MaterialTheme.colorScheme.outline,
          modifier = Modifier.padding(start = 8.dp)
        )
      }

      if (task.description.isNotBlank()) {
        Text(
          task.description,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 6.dp)
        )
      }

      Text(
        "Assigned: ${task.assignedToEmail ?: "Unassigned"}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 8.dp)
      )
      Text(
        "Posted by ${task.createdByEmail ?: "unknown"} · ${formatTaskDate(task.createdAt)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline
      )
      task.completedAt?.let {
        Text(
          "Completed ${formatTaskDate(it)}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }

      Box(modifier = Modifier.padding(top = 8.dp)) {
        AssistChip(
          onClick = { statusMenuExpanded = true },
          label = { Text(taskStatusLabel(task.status)) },
          trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = statusMenuExpanded, onDismissRequest = { statusMenuExpanded = false }) {
          TASK_STATUSES.forEach { status ->
            DropdownMenuItem(
              text = { Text(taskStatusLabel(status)) },
              onClick = {
                statusMenuExpanded = false
                if (status != task.status) onStatusSelected(status)
              }
            )
          }
        }
      }
    }
  }
}

// Create (task == null) or edit an existing task. Status isn't edited here --
// that's the quick dropdown on each row.
@Composable
private fun TaskEditorDialog(
  task: TrackerTask?,
  staff: List<StaffMember>,
  staffError: String?,
  onDismiss: () -> Unit,
  onSave: (title: String, description: String, priority: String, assigneeId: String?) -> Unit,
  onDelete: (TrackerTask) -> Unit
) {
  var title by remember { mutableStateOf(task?.title ?: "") }
  var description by remember { mutableStateOf(task?.description ?: "") }
  var priority by remember { mutableStateOf(task?.priority ?: "normal") }
  var assigneeId by remember { mutableStateOf(task?.assignedTo) }
  var assigneeMenuExpanded by remember { mutableStateOf(false) }
  var confirmingDelete by remember { mutableStateOf(false) }

  if (confirmingDelete && task != null) {
    AlertDialog(
      onDismissRequest = { confirmingDelete = false },
      title = { Text("Delete this task?") },
      text = { Text("\"${task.title}\" will be permanently removed.") },
      confirmButton = { TextButton(onClick = { onDelete(task) }) { Text("Delete") } },
      dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } }
    )
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (task == null) "New task" else "Edit task") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it.take(200) },
          label = { Text("Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it.take(5000) },
          label = { Text("Details") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Text("Priority", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          TASK_PRIORITIES.forEach { option ->
            FilterChip(
              selected = priority == option,
              onClick = { priority = option },
              label = { Text(priorityLabel(option)) }
            )
          }
        }

        Text("Assigned to", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        Box {
          AssistChip(
            onClick = { assigneeMenuExpanded = true },
            label = {
              Text(
                staff.firstOrNull { it.id == assigneeId }?.email ?: "Unassigned",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
          )
          DropdownMenu(expanded = assigneeMenuExpanded, onDismissRequest = { assigneeMenuExpanded = false }) {
            DropdownMenuItem(
              text = { Text("Unassigned") },
              onClick = {
                assigneeId = null
                assigneeMenuExpanded = false
              }
            )
            // Server already limits this to admins (see listAssignees in
            // server/api/tasks/[...path].js).
            staff.forEach { member ->
              DropdownMenuItem(
                text = { Text(member.email) },
                onClick = {
                  assigneeId = member.id
                  assigneeMenuExpanded = false
                }
              )
            }
          }
        }

        staffError?.let {
          Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }

        if (task != null) {
          TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Delete task", color = StatusRed)
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = title.isNotBlank(),
        onClick = { onSave(title.trim(), description.trim(), priority, assigneeId) }
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

// HTTP status + the server's {"error": ...} message when there is one, so a
// failure says *why* (e.g. a missing table shows up as a 500).
private fun describeError(e: Exception): String {
  if (e is retrofit2.HttpException) {
    val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
    val message = body?.let { Regex(""""error"\s*:\s*"([^"]*)"""").find(it)?.groupValues?.get(1) }
    return listOfNotNull("HTTP ${e.code()}", message).joinToString(": ")
  }
  return e.message ?: e.javaClass.simpleName
}

// timestamptz arrives as UTC ISO-8601; shown as an IST date, matching the
// other tabs.
private fun formatTaskDate(isoTimestamp: String): String {
  return try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val date = parser.parse(isoTimestamp.substringBefore('.').substringBefore('Z')) ?: return isoTimestamp
    SimpleDateFormat("d MMM yyyy", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }.format(date)
  } catch (e: Exception) {
    isoTimestamp
  }
}

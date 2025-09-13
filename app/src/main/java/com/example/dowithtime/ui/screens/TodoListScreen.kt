package com.example.dowithtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.dowithtime.data.Subtask
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import android.content.ClipboardManager
import android.content.Context
import com.example.dowithtime.data.Task
import com.example.dowithtime.data.TaskList
import com.example.dowithtime.viewmodel.TaskViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.dowithtime.ui.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.DialogProperties


// Move formatDuration function outside the main composable
internal fun formatDuration(durationSeconds: Int): String {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60

    return when {
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0s"
    }
}

@Composable
fun TaskItem(
    task: Task,
    index: Int,
    totalTasks: Int,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onToggleDaily: () -> Unit,
    modifier: Modifier = Modifier,
    showDuration: Boolean = true,
    displayedDurationSeconds: Int? = null,
    isDropTarget: Boolean = false
) {
    // Determine if this is a completed daily task
    val isCompletedDaily = task.isDaily && task.completedToday

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isCompletedDaily) 0.6f else 1f
            }
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDropTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                isCompletedDaily -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task title and duration stacked vertically
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompletedDaily) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    if (isCompletedDaily) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (showDuration) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱",
                            fontSize = 12.sp,
                            color = if (isCompletedDaily) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatDuration(displayedDurationSeconds ?: task.durationSeconds),
                            fontSize = 13.sp,
                            color = if (isCompletedDaily) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.7f
                            ) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            // Complete button
            Button(
                onClick = onComplete,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Complete",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Daily toggle button
            Card(
                modifier = Modifier
                    .size(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (task.isDaily) MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.12f
                    ) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onToggleDaily() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔄",
                        fontSize = 14.sp,
                        color = if (task.isDaily) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Trash can icon centered in a small card
            Card(
                modifier = Modifier
                    .size(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ErrorRed.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, Int, Boolean, List<Pair<String, Int>>) -> Unit,
    isDaily: Boolean = false,
    addToTop: Boolean,
    onAddToTopChange: (Boolean) -> Unit,
    timersDisabled: Boolean,
    viewModel: TaskViewModel
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }

    // Focus management
    val titleFocusRequester = remember { FocusRequester() }
    val minutesFocusRequester = remember { FocusRequester() }
    val secondsFocusRequester = remember { FocusRequester() }

    val focusManager = LocalFocusManager.current

    // Auto-focus title field when dialog opens
    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

    fun clearForm() {
        title = ""
        minutes = ""
        seconds = ""
        titleError = false
        durationError = false
        // Re-focus title field after clearing
        titleFocusRequester.requestFocus()
    }

    // Subtasks editor state for Add dialog (not yet persisted until confirm)
    val newSubtasks = remember { mutableStateListOf<Pair<String, Int>>() }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newSubtaskMinutes by remember { mutableStateOf("") }
    var newSubtaskSeconds by remember { mutableStateOf("") }
    // (Edit-subtask dialog state lives in EditTaskDialog)
    var advancedExpanded by remember { mutableStateOf(false) }
    var showSubtaskEditor by remember { mutableStateOf(false) }
    // We don't have direct access to viewModel here; rely on duration validation logic below being bypassed when totalSeconds==0 allowed.
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .then(if (showSubtaskEditor) Modifier.fillMaxWidth().fillMaxHeight(0.9f) else Modifier),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (isDaily) "Add New Daily Task" else "Add New Task",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                // Task title field
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = false
                        },
                        label = { Text("Task Title") },
                        isError = titleError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester)
                             .focusProperties { next = minutesFocusRequester },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                val durationVisible = !timersDisabled && newSubtasks.isEmpty()
                                if (durationVisible) {
                                    minutesFocusRequester.requestFocus()
                                } else if (timersDisabled) {
                                    if (title.isBlank()) {
                                        titleError = true
                                    } else {
                                        onAddTask(title, 0, isDaily, newSubtasks.toList())
                                        clearForm()
                                        newSubtaskTitle = ""
                                        newSubtaskMinutes = ""
                                        newSubtaskSeconds = ""
                                        newSubtasks.clear()
                                    }
                                }
                            },
                            onDone = {
                                val durationVisible = !timersDisabled && newSubtasks.isEmpty()
                                if (durationVisible) {
                                    minutesFocusRequester.requestFocus()
                                } else if (timersDisabled) {
                                    if (title.isBlank()) {
                                        titleError = true
                                    } else {
                                        onAddTask(title, 0, isDaily, newSubtasks.toList())
                                        clearForm()
                                        newSubtaskTitle = ""
                                        newSubtaskMinutes = ""
                                        newSubtaskSeconds = ""
                                        newSubtasks.clear()
                                    }
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addToTop,
                        onCheckedChange = onAddToTopChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add new tasks to top of list",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onAddToTopChange(!addToTop) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Duration section (hidden when timerless or when building with subtasks)
                if (!timersDisabled && newSubtasks.isEmpty()) {
                    Text(
                    text = "Duration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Minutes field
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            OutlinedTextField(
                                value = minutes,
                                onValueChange = {
                                    minutes = it.filter { char -> char.isDigit() }
                                    durationError = false
                                },
                                label = { Text("Minutes") },
                                isError = durationError,
                                modifier = Modifier
                                    .fillMaxWidth()
                                     .focusRequester(minutesFocusRequester)
                                     .focusProperties {
                                         previous = titleFocusRequester
                                         next = secondsFocusRequester
                                     }
                                      ,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { secondsFocusRequester.requestFocus() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Seconds field
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            OutlinedTextField(
                                value = seconds,
                                onValueChange = {
                                    seconds = it.filter { char -> char.isDigit() }
                                    durationError = false
                                },
                                label = { Text("Seconds") },
                                isError = durationError,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(secondsFocusRequester)
                                    .focusProperties { previous = minutesFocusRequester }
                                     ,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        // Validate and add task
                                        if (title.isBlank()) {
                                            titleError = true
                                            titleFocusRequester.requestFocus()
                                            return@KeyboardActions
                                        }

                                        val minutesInt = minutes.toIntOrNull() ?: 0
                                        val secondsInt = seconds.toIntOrNull() ?: 0
                                        val totalSeconds = minutesInt * 60 + secondsInt

                                         // Require at least one non-zero when timers are enabled and no subtasks
                                         if (!timersDisabled && newSubtasks.isEmpty() && totalSeconds <= 0) {
                                            durationError = true
                                            minutesFocusRequester.requestFocus()
                                            return@KeyboardActions
                                        }

                                        onAddTask(title, if (timersDisabled || newSubtasks.isNotEmpty()) 0 else totalSeconds, isDaily, newSubtasks.toList())
                                        clearForm()
                                        newSubtaskTitle = ""
                                        newSubtaskMinutes = ""
                                        newSubtaskSeconds = ""
                                        newSubtasks.clear()
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                // Advanced options expander
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (showSubtaskEditor) 420.dp else 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedExpanded = !advancedExpanded }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced options",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (advancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                        if (advancedExpanded) {
                            Divider()
                            if (!showSubtaskEditor) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = { showSubtaskEditor = true }) {
                                        Text("Add Subtasks")
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    // Premade Tasks dropdown
                                    var presetsExpanded by remember { mutableStateOf(false) }
                                    var selectedPresetName by remember { mutableStateOf("") }
                                    val allLists by viewModel.taskLists.collectAsState()
                                    val premadePrefix = "Premade: "
                                    val premadeLists = allLists.filter { it.name.startsWith(premadePrefix) }
                                    val localScope = rememberCoroutineScope()
                                    Box {
                                        OutlinedButton(onClick = { presetsExpanded = true }) {
                                            Text(if (selectedPresetName.isBlank()) "Premade Tasks" else selectedPresetName)
                                        }
                                        DropdownMenu(expanded = presetsExpanded, onDismissRequest = { presetsExpanded = false }) {
                                            if (premadeLists.isEmpty()) {
                                                DropdownMenuItem(text = { Text("No premade lists yet") }, onClick = { presetsExpanded = false })
                                            } else {
                                                premadeLists.forEach { list ->
                                                    DropdownMenuItem(text = { Text(list.name.removePrefix(premadePrefix)) }, onClick = {
                                                        selectedPresetName = list.name.removePrefix(premadePrefix)
                                                        presetsExpanded = false
                                                        // Populate title and subtasks from this premade list
                                                        title = selectedPresetName
                                                        // Load tasks and their subtasks for this list using a coroutine so we can collect once
                                                        localScope.launch {
                                                            val listTasks = viewModel.fetchTasksForListOnce(list.id)
                                                            newSubtasks.clear()
                                                            listTasks.sortedBy { it.order }.forEach { t ->
                                                                val subs = viewModel.fetchSubtasksForTaskOnce(t.id)
                                                                val seconds = if (subs.isNotEmpty()) subs.sumOf { it.durationSeconds } else t.durationSeconds
                                                                newSubtasks.add(t.title to seconds)
                                                            }
                                                            showSubtaskEditor = true
                                                        }
                                                    })
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Subtasks editor takes majority of dialog space
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        "Subtasks",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    // Existing subtasks list (taller when editing)
                                    LazyColumn(
                                        modifier = Modifier.heightIn(max = 360.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(newSubtasks) { index, pair ->
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Text("${index + 1}.", modifier = Modifier.width(24.dp))
                                                Column(modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        // Load into editor fields for quick edit
                                                        newSubtaskTitle = pair.first
                                                        val m = pair.second / 60
                                                        val s = pair.second % 60
                                                        newSubtaskMinutes = if (m == 0) "" else m.toString()
                                                        newSubtaskSeconds = if (s == 0) "" else s.toString()
                                                    }) {
                                                    Text(pair.first, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        formatDuration(pair.second),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    if (index > 0) {
                                                        val item = newSubtasks.removeAt(index)
                                                        newSubtasks.add(index - 1, item)
                                                    }
                                                }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
                                                IconButton(onClick = {
                                                    if (index < newSubtasks.size - 1) {
                                                        val item = newSubtasks.removeAt(index)
                                                        newSubtasks.add(index + 1, item)
                                                    }
                                                }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                                                IconButton(onClick = { newSubtasks.removeAt(index) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete subtask")
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    // Subtask entry fields: title on its own line, then duration fields
                                    OutlinedTextField(
                                        value = newSubtaskTitle,
                                        onValueChange = { newSubtaskTitle = it },
                                        label = { Text("Subtask title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = newSubtaskMinutes,
                                            onValueChange = { newSubtaskMinutes = it.filter { c -> c.isDigit() } },
                                            label = { Text("Minutes") },
                                            singleLine = true,
                                            modifier = Modifier.width(100.dp)
                                        )
                                        OutlinedTextField(
                                            value = newSubtaskSeconds,
                                            onValueChange = { newSubtaskSeconds = it.filter { c -> c.isDigit() } },
                                            label = { Text("Seconds") },
                                            singleLine = true,
                                            modifier = Modifier.width(100.dp)
                                        )
                                        Button(onClick = {
                                            val m = newSubtaskMinutes.toIntOrNull() ?: 0
                                            val s = newSubtaskSeconds.toIntOrNull() ?: 0
                                            val total = if (timersDisabled) 0 else m * 60 + s
                                            if (newSubtaskTitle.isNotBlank() && (timersDisabled || total > 0)) {
                                                // If this title exists, update it; otherwise add new
                                                val existingIndex = newSubtasks.indexOfFirst { it.first == newSubtaskTitle }
                                                if (existingIndex >= 0) newSubtasks[existingIndex] = newSubtaskTitle to total
                                                else newSubtasks.add(newSubtaskTitle to total)
                                                newSubtaskTitle = ""
                                                newSubtaskMinutes = ""
                                                newSubtaskSeconds = ""
                                            }
                                        }) { Text("Add") }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showSubtaskEditor = false }) { Text("Done") }
                                    }
                                }
                            }
                        }
                    }
                }

                if (titleError) {
                    Text(
                        text = "Please enter a task title",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (durationError) {
                    Text(
                        text = "Please enter a valid duration (at least 1 second)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = run {
                    val minutesInt = minutes.toIntOrNull() ?: 0
                    val secondsInt = seconds.toIntOrNull() ?: 0
                    // If timers are enabled and no subtasks, require at least one non-zero value
                    !( !timersDisabled && newSubtasks.isEmpty() && (minutesInt <= 0 && secondsInt <= 0) )
                },
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                                 titleFocusRequester.requestFocus()
                        return@Button
                    }

                    val minutesInt = minutes.toIntOrNull() ?: 0
                    val secondsInt = seconds.toIntOrNull() ?: 0
                    val totalSeconds = minutesInt * 60 + secondsInt
                    // Enforce at least one non-zero value when timers are enabled and no subtasks
                    if (!timersDisabled && newSubtasks.isEmpty() && (minutesInt <= 0 && secondsInt <= 0)) {
                        durationError = true
                                 minutesFocusRequester.requestFocus()
                        return@Button
                    }
                    onAddTask(title, if (timersDisabled || newSubtasks.isNotEmpty()) 0 else totalSeconds, isDaily, newSubtasks.toList())
                    clearForm()
                    newSubtaskTitle = ""
                    newSubtaskMinutes = ""
                    newSubtaskSeconds = ""
                    newSubtasks.clear()
                    // Keep dialog open for faster entry, as before
                },
                modifier = Modifier
                    .background(
                        brush = PrimaryGradient,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Add Task",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )

    // (Subtask edit dialog is only available in EditTaskDialog)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    currentPosition: Int,
    onDismiss: () -> Unit,
    onEditTask: (String, Int, Boolean, Int) -> Unit,
    viewModel: TaskViewModel
) {
    var title by remember { mutableStateOf(task.title) }
    var minutes by remember { mutableStateOf((task.durationSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((task.durationSeconds % 60).toString()) }
    var order by remember { mutableStateOf((currentPosition + 1).toString()) }
    var titleError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }
    var orderError by remember { mutableStateOf(false) }

    // Focus management
    val titleFocusRequester = remember { FocusRequester() }
    val minutesFocusRequester = remember { FocusRequester() }
    val secondsFocusRequester = remember { FocusRequester() }
    val orderFocusRequester = remember { FocusRequester() }

    // Auto-focus title field when dialog opens
    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

    val timersDisabled by viewModel.disableTimers.collectAsState()
    val subtasks by viewModel.getSubtasksFlow(task.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newSubtaskMinutes by remember { mutableStateOf("") }
    var newSubtaskSeconds by remember { mutableStateOf("") }
    // Inline subtask edit dialog state (scoped to EditTaskDialog)
    var showEditSubDialog by remember { mutableStateOf(false) }
    var editingSubtask by remember { mutableStateOf<Subtask?>(null) }
    var editSubTitle by remember { mutableStateOf("") }
    var editSubMinutes by remember { mutableStateOf("") }
    var editSubSeconds by remember { mutableStateOf("") }

    // Advanced options state
    var advancedExpanded by remember { mutableStateOf(false) }
    var showSubtaskEditor by remember { mutableStateOf(false) }
    // If this task already has subtasks, open the mini list view editor by default
    LaunchedEffect(subtasks.size) {
        if (subtasks.isNotEmpty()) {
            showSubtaskEditor = true
            advancedExpanded = true
        }
    }

    fun clearForm() {
        title = ""
        minutes = ""
        seconds = ""
        order = "1"
        titleError = false
        durationError = false
        orderError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .then(if (showSubtaskEditor) Modifier.fillMaxWidth().fillMaxHeight(0.9f) else Modifier),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Text(
                "Edit Task",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // Compact position field aligned to the right (always shown)
                run {
                    OutlinedTextField(
                        value = order,
                        onValueChange = {
                            order = it.filter { ch -> ch.isDigit() }
                            orderError = false
                        },
                        label = { Text("Position") },
                        singleLine = true,
                        isError = orderError,
                        modifier = Modifier.width(96.dp)
                    )
                    if (subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            // Open add subtask dialog (blank)
                            editingSubtask = null
                            editSubTitle = ""
                            editSubMinutes = ""
                            editSubSeconds = ""
                            showEditSubDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add subtask")
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (subtasks.isNotEmpty()) Modifier.fillMaxHeight() else Modifier)
                    .padding(vertical = 8.dp)
            ) {
                // When subtasks exist, present a mini list-view layout: title as list title,
                // small inline position field, then the subtask list taking most space.
                if (subtasks.isNotEmpty()) {
                    // Title as list title (compact)
                    Text(
                        text = "${task.title}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                // Task title field. When subtasks exist, keep editable title but the top-of-dialog
                // "list title" line already shows the current title; keep this editable for changes.
                if (subtasks.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = false
                        },
                        label = { Text("Task Title") },
                        isError = titleError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocusRequester),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { minutesFocusRequester.requestFocus() }
                        )
                    )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Duration section (hide when timerless or subtasks exist)
                if (!timersDisabled && subtasks.isEmpty()) {
                    Text(
                        text = "Duration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // Minutes field
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = {
                                minutes = it.filter { char -> char.isDigit() }
                                durationError = false
                            },
                            label = { Text("Minutes") },
                            isError = durationError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(minutesFocusRequester),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { secondsFocusRequester.requestFocus() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Seconds field
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        OutlinedTextField(
                            value = seconds,
                            onValueChange = {
                                seconds = it.filter { char -> char.isDigit() }
                                durationError = false
                            },
                            label = { Text("Seconds") },
                            isError = durationError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(secondsFocusRequester),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { orderFocusRequester.requestFocus() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Position section is now always in the header; remove the lower block
                if (false) {
                Text(
                    text = "Position",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    OutlinedTextField(
                        value = order,
                        onValueChange = {
                            order = it.filter { char -> char.isDigit() }
                            orderError = false
                        },
                        label = { Text("Position (1, 2, 3, etc.)") },
                        isError = orderError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(orderFocusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // Validate and save
                                if (title.isBlank()) {
                                    titleError = true
                                    titleFocusRequester.requestFocus()
                                    return@KeyboardActions
                                }

                                val minutesInt = minutes.toIntOrNull() ?: 0
                                val secondsInt = seconds.toIntOrNull() ?: 0
                                val rawTotalSeconds = minutesInt * 60 + secondsInt
                                val usingSubtasks = subtasks.isNotEmpty()
                                val effectiveTotalSeconds = if (timersDisabled || usingSubtasks) 0 else rawTotalSeconds

                                if (!timersDisabled && !usingSubtasks && effectiveTotalSeconds <= 0) {
                                    durationError = true
                                    minutesFocusRequester.requestFocus()
                                    return@KeyboardActions
                                }

                                val orderInt = order.toIntOrNull() ?: 0
                                if (orderInt <= 0) {
                                    orderError = true
                                    orderFocusRequester.requestFocus()
                                    return@KeyboardActions
                                }

                                onEditTask(title, effectiveTotalSeconds, task.isDaily, orderInt - 1)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Advanced options expander (Subtasks)
                val integrated = subtasks.isNotEmpty() && showSubtaskEditor
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (integrated) Modifier.weight(1f) else Modifier)
                        .heightIn(min = if (showSubtaskEditor && !integrated) 420.dp else 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (integrated) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = if (integrated) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!integrated) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { advancedExpanded = !advancedExpanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Subtasks",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (advancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                        }
                        if (advancedExpanded) {
                            if (!integrated) Divider()
                            if (subtasks.isEmpty() && !showSubtaskEditor) {
                                // No subtasks yet: present Add Subtasks and Premade Tasks actions (like Add Task dialog)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Add Subtasks button opens the Add/Edit Subtask popup in add mode
                                    Button(onClick = {
                                        editingSubtask = null
                                        editSubTitle = ""
                                        editSubMinutes = ""
                                        editSubSeconds = ""
                                        showEditSubDialog = true
                                    }) { Text("Add Subtasks") }

                                    // Premade Tasks dropdown reused from Add Task dialog
                                    var presetsExpanded by remember { mutableStateOf(false) }
                                    var selectedPresetName by remember { mutableStateOf("") }
                                    val allLists by viewModel.taskLists.collectAsState()
                                    val premadePrefix = "Premade: "
                                    val premadeLists = allLists.filter { it.name.startsWith(premadePrefix) }
                                    val localScope = rememberCoroutineScope()
                                    Box {
                                        Button(onClick = { presetsExpanded = true }) { Text(if (selectedPresetName.isBlank()) "Premade Tasks" else selectedPresetName) }
                                        DropdownMenu(expanded = presetsExpanded, onDismissRequest = { presetsExpanded = false }) {
                                            premadeLists.forEach { list ->
                                                DropdownMenuItem(text = { Text(list.name.removePrefix(premadePrefix)) }, onClick = {
                                                    selectedPresetName = list.name.removePrefix(premadePrefix)
                                                    presetsExpanded = false
                                                    // Import subtasks from selected premade list into this task
                                                    localScope.launch {
                                                        val listTasks = viewModel.fetchTasksForListOnce(list.id)
                                                        listTasks.sortedBy { it.order }.forEach { t ->
                                                            val subs = viewModel.fetchSubtasksForTaskOnce(t.id)
                                                            val seconds = if (subs.isNotEmpty()) subs.sumOf { it.durationSeconds } else t.durationSeconds
                                                            viewModel.addSubtask(task.id, t.title, seconds)
                                                        }
                                                        showSubtaskEditor = true
                                                    }
                                                })
                                            }
                                        }
                                    }
                                }
                            } else if (showSubtaskEditor) {
                                // Subtasks editor takes majority of dialog space (mini list view)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (integrated) 0.dp else 12.dp)
                                ) {
                                    Spacer(Modifier.height(4.dp))
                                    // Mini list view with drag & drop reorder
                                    var subListTopY by remember { mutableStateOf(0f) }
                                    val subListState = rememberLazyListState()
                                    var isDraggingSub by remember { mutableStateOf(false) }
                                    var subDragOffset by remember { mutableStateOf(0f) }
                                    var subItemHeightPx by remember { mutableStateOf(0f) }
                                    var subItemTopY by remember { mutableStateOf(0f) }
                                    var subStartCenterY by remember { mutableStateOf(0f) }
                                    var subDragHoverIndex by remember { mutableStateOf<Int?>(null) }
                                    var subDraggingId by remember { mutableStateOf<Int?>(null) }
                                    var subGestureStartIndex by remember { mutableStateOf(0) }

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .onGloballyPositioned { subListTopY = it.positionInRoot().y },
                                        state = subListState,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(subtasks) { index, st ->
                                            val hovered = subDragHoverIndex == index
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onGloballyPositioned { coords ->
                                                        subItemHeightPx = coords.size.height.toFloat()
                                                        subItemTopY = coords.positionInRoot().y
                                                    }
                                                    .graphicsLayer {
                                                        if (isDraggingSub && subDraggingId == st.id) {
                                                            translationY = subDragOffset
                                                            scaleX = 1.02f
                                                            scaleY = 1.02f
                                                            shadowElevation = 12f
                                                            alpha = 0.98f
                                                        }
                                                    }
                                                    .zIndex(if (isDraggingSub && subDraggingId == st.id) 1f else 0f)
                                                    .pointerInput(st.id) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                isDraggingSub = true
                                                                subDragOffset = 0f
                                                                subGestureStartIndex = index
                                                                subStartCenterY = subItemTopY + (subItemHeightPx / 2f)
                                                                subDragHoverIndex = index
                                                                subDraggingId = st.id
                                                            },
                                                            onDrag = { _, dragAmount ->
                                                                subDragOffset += dragAmount.y
                                                                val centerY = subStartCenterY + subDragOffset
                                                                val visible = subListState.layoutInfo.visibleItemsInfo
                                                                val viewportTop = subListTopY
                                                                val viewportBottom = viewportTop + subListState.layoutInfo.viewportEndOffset
                                                                val edge = 64f
                                                                val firstIndex = subListState.firstVisibleItemIndex
                                                                val lastIndex = visible.lastOrNull()?.index ?: firstIndex
                                                                if (centerY < viewportTop + edge) {
                                                                    scope.launch { subListState.animateScrollToItem((firstIndex - 1).coerceAtLeast(0)) }
                                                                } else if (centerY > viewportBottom - edge) {
                                                                    scope.launch { subListState.animateScrollToItem((lastIndex + 1).coerceAtMost(subtasks.lastIndex)) }
                                                                }
                                                                if (visible.isNotEmpty()) {
                                                                    val first = visible.first()
                                                                    val last = visible.last()
                                                                    val topFirst = subListTopY + first.offset
                                                                    val bottomLast = subListTopY + last.offset + last.size
                                                                    val candidateIndex = when {
                                                                        centerY <= topFirst -> 0
                                                                        centerY >= bottomLast -> subtasks.lastIndex
                                                                        else -> {
                                                                            val over = visible.minByOrNull { info ->
                                                                                val center = subListTopY + info.offset + (info.size / 2f)
                                                                                kotlin.math.abs(centerY - center)
                                                                            }
                                                                            over?.index ?: subGestureStartIndex
                                                                        }
                                                                    }
                                                                    subDragHoverIndex = candidateIndex
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val centerY = subStartCenterY + subDragOffset
                                                                val visible = subListState.layoutInfo.visibleItemsInfo
                                                                val target = if (visible.isNotEmpty()) {
                                                                    val first = visible.first()
                                                                    val last = visible.last()
                                                                    val topFirst = subListTopY + first.offset
                                                                    val bottomLast = subListTopY + last.offset + last.size
                                                                    when {
                                                                        centerY <= topFirst -> 0
                                                                        centerY >= bottomLast -> subtasks.lastIndex
                                                                        else -> {
                                                                            val over = visible.minByOrNull { info ->
                                                                                val center = subListTopY + info.offset + (info.size / 2f)
                                                                                kotlin.math.abs(centerY - center)
                                                                            }
                                                                            over?.index ?: subGestureStartIndex
                                                                        }
                                                                    }
                                                                } else subGestureStartIndex
                                                                if (target != subGestureStartIndex) {
                                                                    val reordered = subtasks.toMutableList().apply {
                                                                        val moved = removeAt(subGestureStartIndex)
                                                                        add(target, moved)
                                                                    }
                                                                    viewModel.reorderSubtasks(task.id, reordered)
                                                                }
                                                                isDraggingSub = false
                                                                subDragOffset = 0f
                                                                subDragHoverIndex = null
                                                                subDraggingId = null
                                                            },
                                                            onDragCancel = {
                                                                isDraggingSub = false
                                                                subDragOffset = 0f
                                                                subDragHoverIndex = null
                                                                subDraggingId = null
                                                            }
                                                        )
                                                    },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Menu,
                                                        contentDescription = "Drag",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    )
                                                    Text("${index + 1}.", modifier = Modifier.width(24.dp))
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                // Open inline edit dialog prefilled like in main list
                                                                editingSubtask = st
                                                                editSubTitle = st.title
                                                                val m = st.durationSeconds / 60
                                                                val s = st.durationSeconds % 60
                                                                editSubMinutes = if (m == 0) "" else m.toString()
                                                                editSubSeconds = if (s == 0) "" else s.toString()
                                                                showEditSubDialog = true
                                                            }
                                                    ) {
                                                        Text(st.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                                        Text(
                                                            formatDuration(st.durationSeconds),
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    IconButton(onClick = { viewModel.deleteSubtask(st.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete subtask")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Spacer(Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showSubtaskEditor = false }) { Text("Done") }
                                    }
                                }
                            }
                        }
                    }
                }

                if (titleError) {
                    Text(
                        text = "Please enter a task title",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (durationError) {
                    Text(
                        text = "Please enter a valid duration (at least 1 second)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (orderError) {
                    Text(
                        text = "Please enter a valid position (1 or higher)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        titleFocusRequester.requestFocus()
                        return@Button
                    }

                    val minutesInt = minutes.toIntOrNull() ?: 0
                    val secondsInt = seconds.toIntOrNull() ?: 0
                    val rawTotalSeconds = minutesInt * 60 + secondsInt
                    val usingSubtasks = subtasks.isNotEmpty()
                    val totalSeconds = if (timersDisabled || usingSubtasks) 0 else rawTotalSeconds

                    if (!timersDisabled && !usingSubtasks && totalSeconds <= 0) {
                        durationError = true
                        minutesFocusRequester.requestFocus()
                        return@Button
                    }

                    val orderInt = order.toIntOrNull() ?: 0
                    if (orderInt <= 0) {
                        orderError = true
                        orderFocusRequester.requestFocus()
                        return@Button
                    }

                    onEditTask(title, totalSeconds, task.isDaily, orderInt - 1)
                },
                modifier = Modifier
                    .background(
                        brush = PrimaryGradient,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )

    // Add/Edit Subtask pop-up dialog
    if (showEditSubDialog) {
        AlertDialog(
            onDismissRequest = { showEditSubDialog = false },
            confirmButton = {
                Button(onClick = {
                    val minutesInt = editSubMinutes.toIntOrNull() ?: 0
                    val secondsInt = editSubSeconds.toIntOrNull() ?: 0
                    val total = if (timersDisabled) 0 else minutesInt * 60 + secondsInt
                    val sub = editingSubtask
                    if (sub == null) {
                        if (editSubTitle.isNotBlank()) {
                            viewModel.addSubtask(task.id, editSubTitle, total)
                        }
                    } else {
                        viewModel.updateSubtask(sub.copy(title = editSubTitle, durationSeconds = total))
                    }
                    showEditSubDialog = false
                }) { Text(if (editingSubtask == null) "Add" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditSubDialog = false }) { Text("Cancel") }
            },
            title = { Text(if (editingSubtask == null) "Add Subtask" else "Edit Subtask") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editSubTitle,
                        onValueChange = { editSubTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editSubMinutes,
                            onValueChange = { editSubMinutes = it.filter { c -> c.isDigit() } },
                            label = { Text("Minutes") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                        OutlinedTextField(
                            value = editSubSeconds,
                            onValueChange = { editSubSeconds = it.filter { c -> c.isDigit() } },
                            label = { Text("Seconds") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun MoveTasksDialog(
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onMoveTasks: (List<Task>, Int) -> Unit
) {
    var selectedTasks by remember { mutableStateOf(setOf<Int>()) }
    var targetPosition by remember { mutableStateOf(TextFieldValue("1")) }
    var positionError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Move Tasks",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Select tasks to move:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Task selection list
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tasks) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTasks = if (selectedTasks.contains(task.id)) {
                                        selectedTasks - task.id
                                    } else {
                                        selectedTasks + task.id
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedTasks.contains(task.id),
                                onCheckedChange = { checked ->
                                    selectedTasks = if (checked) {
                                        selectedTasks + task.id
                                    } else {
                                        selectedTasks - task.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatDuration(task.durationSeconds),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Move to position:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = targetPosition,
                    onValueChange = {
                        targetPosition =
                            it.copy(text = it.text.filter { char -> char.isDigit() })
                        positionError = false
                    },
                    label = { Text("Position (1, 2, 3, etc.)") },
                    isError = positionError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (positionError) {
                    Text(
                        text = "Please enter a valid position (1 or higher)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTasks.isEmpty()) {
                        return@Button
                    }

                    val positionInt = targetPosition.text.toIntOrNull() ?: 0
                    if (positionInt <= 0) {
                        positionError = true
                        return@Button
                    }

                    val selectedTaskList = tasks.filter { it.id in selectedTasks }
                    onMoveTasks(selectedTaskList, positionInt - 1)
                },
                enabled = selectedTasks.isNotEmpty(),
                modifier = Modifier
                    .background(
                        brush = PrimaryGradient,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Move ${selectedTasks.size} Task${if (selectedTasks.size != 1) "s" else ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun PasteToListDialog(
    sourceListId: Int?,
    sourceIsDaily: Boolean,
    taskLists: List<TaskList>,
    onDismiss: () -> Unit,
    onPasteTasks: (Int, Int) -> Unit
) {
    var selectedTargetListId by remember { mutableStateOf(0) } // 0 for unselected, positive for regular lists
    var targetPosition by remember { mutableStateOf(TextFieldValue("1")) }
    var positionError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Paste Tasks to Other List",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Select target list:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Dropdown for target list selection
                Box {
                    OutlinedTextField(
                        value = taskLists.find { it.id == selectedTargetListId }?.name
                            ?: "Select a list",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle dropdown"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Regular lists (exclude premade lists from destinations)
                        val destinationLists = taskLists.filter { !it.name.startsWith("Premade: ") }
                        destinationLists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = {
                                    selectedTargetListId = list.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Paste at position:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = targetPosition,
                    onValueChange = {
                        targetPosition =
                            it.copy(text = it.text.filter { char -> char.isDigit() })
                        positionError = false
                    },
                    label = { Text("Position (1, 2, 3, etc.)") },
                    isError = positionError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (positionError) {
                    Text(
                        text = "Please enter a valid position (1 or higher)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val positionInt = targetPosition.text.toIntOrNull() ?: 0
                    if (positionInt <= 0) {
                        positionError = true
                        return@Button
                    }
                    onPasteTasks(selectedTargetListId, positionInt - 1)
                },
                enabled = selectedTargetListId != 0, // 0 is the default "Select a list" state
                modifier = Modifier
                    .background(
                        brush = PrimaryGradient,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Paste Tasks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TaskViewModel,
    onNavigateToDo: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    val todayTotalSeconds by viewModel.todayTotalSeconds.collectAsState()
    val summaries by viewModel.dailySummaries.collectAsState()
    val showCtdadBar by viewModel.showCtdadBar.collectAsState()
    val taskLists by viewModel.taskLists.collectAsState()
    val currentListId by viewModel.currentListId.collectAsState()
    val wasInDailyList by viewModel.wasInDailyList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingTaskPosition by remember { mutableStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameListId by remember { mutableStateOf<Int?>(null) }
    var renameListValue by remember { mutableStateOf(TextFieldValue("")) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf(TextFieldValue("")) }
    var showAddPremadeDialog by remember { mutableStateOf(false) }
    var newPremadeName by remember { mutableStateOf(TextFieldValue("")) }
    var showMoveTasksDialog by remember { mutableStateOf(false) }
    var showPasteToListDialog by remember { mutableStateOf(false) }
    var pasteSourceListId by remember { mutableStateOf<Int?>(null) }
    var pasteSourceIsDaily by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncData by remember { mutableStateOf("") }
    var showDayLogsDialog by remember { mutableStateOf(false) }
    var dayLogsToShow by remember { mutableStateOf("") }

    // Add to top checkbox state
    var addToTop by rememberSaveable { mutableStateOf(false) }

    // History selection state (replaces Dailies page)
    var historySelected by remember { mutableStateOf(false) }
    // Dailies flag retained but disabled in UI
    var dailiesSelected by remember { mutableStateOf(false) }

    // Update dailiesSelected when wasInDailyList changes
    LaunchedEffect(Unit) {
        dailiesSelected = false
        viewModel.setWasInDailyList(false)
    }

    // Drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Filter tasks for Dailies or selected list (Dailies disabled)
    val filteredTasks = tasks


    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedAudioFile by remember { mutableStateOf("") }

    // File picker launcher for MP3
    val mp3PickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedAudioFile = uri.toString()
                // TODO: Save to persistent settings if needed
            }
        }

    // Add a reference to the LazyColumn's state
    val listState = rememberLazyListState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxHeight()) {
                // History item (no heading)
                NavigationDrawerItem(
                    label = { Text("History") },
                    selected = historySelected,
                    onClick = {
                        historySelected = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                // User lists
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = "Your Lists",
                    fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = { showAddListDialog = true },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Add", fontSize = 12.sp, color = Color.White)
                    }
                }
                // Premade lists are those with the special prefix
                val premadePrefix = "Premade: "
                val premadeLists = taskLists.filter { it.name.startsWith(premadePrefix) }
                // Remove old preset quick-add UI
                taskLists.forEach { list ->
                    // Skip showing premade lists under "Your Lists"
                    if (list.name.startsWith(premadePrefix)) return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationDrawerItem(
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        list.name,
                                        fontWeight = if (!dailiesSelected && list.id == currentListId) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        renameListId = list.id
                                        renameListValue = TextFieldValue(list.name)
                                        showRenameDialog = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Rename List",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (taskLists.size > 1) {
                                        IconButton(onClick = { viewModel.deleteTaskList(list.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete List",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            selected = !dailiesSelected && list.id == currentListId,
                            onClick = {
                                historySelected = false
                                dailiesSelected = false
                                viewModel.setWasInDailyList(false)
                                viewModel.selectList(list.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                pasteSourceIsDaily = false
                                pasteSourceListId = list.id
                                showPasteToListDialog = true
                                scope.launch { drawerState.close() }
                            }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Paste List to other list",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Premade Tasks section: always show the heading and add button
                                // Always show the Premade Tasks section
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Premade Tasks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = { showAddPremadeDialog = true },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Add", fontSize = 12.sp, color = Color.White)
                    }
                }

                // Render premade lists under this section
                premadeLists.forEach { premade ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationDrawerItem(
                            label = { Text(premade.name.removePrefix(premadePrefix)) },
                            selected = !historySelected && currentListId == premade.id,
                            onClick = {
                                historySelected = false
                                dailiesSelected = false
                                viewModel.setWasInDailyList(false)
                                viewModel.selectList(premade.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            renameListId = premade.id
                            renameListValue = TextFieldValue(premade.name.removePrefix(premadePrefix))
                            showRenameDialog = true
                        }) {
                    Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename List",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { viewModel.deleteTaskList(premade.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete List",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                // Move Tasks button pinned to bottom
                Button(
                    onClick = { showMoveTasksDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = Color.White)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Move Tasks", color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Settings button (stays accessible via drawer)
                Button(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings", color = Color.White)
                }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (historySelected) "CTDAD History" else taskLists.find { it.id == currentListId }?.name
                                    ?: "Tasks",
                                fontWeight = FontWeight.Bold
                            )
                            val timersDisabled by viewModel.disableTimers.collectAsState()
                            if (showCtdadBar && !historySelected && !timersDisabled) {
                                val hours = todayTotalSeconds / 3600
                                val minutes = (todayTotalSeconds % 3600) / 60
                                Text(String.format("%02d:%02d", hours, minutes), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    navigationIcon = {},
                    actions = {}
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Removed in favor of top bar CTDAD display
                // Show loading screen while data is being initialized
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading tasks...",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (!historySelected && filteredTasks.isEmpty()) {
                            // Empty state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (dailiesSelected) "No daily tasks yet" else "No tasks yet",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (dailiesSelected) "Tap the + button to add your first daily task" else "Tap the + button to add your first task",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            if (historySelected) {
                                // Show CTDAD history (previous days)
                                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                val previous = summaries.filter { it.date != todayStr }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 56.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(previous) { summary ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp)
                                                .clickable {
                                                    dayLogsToShow = summary.date
                                                    showDayLogsDialog = true
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(summary.date, fontWeight = FontWeight.Medium)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val h = summary.totalSeconds / 3600
                                                    val m = (summary.totalSeconds % 3600) / 60
                                                    Text(String.format("%02d:%02d", h, m), fontWeight = FontWeight.Bold)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text("tap for details", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Task list with working drag and drop
                                // Hover index for visual target highlight while dragging
                                var dragHoverIndex by remember { mutableStateOf<Int?>(null) }
                                // Track which task is currently being dragged to avoid highlighting itself
                                var draggingTaskId by remember { mutableStateOf<Int?>(null) }
                                // Drag-and-drop reorderable task list
                                var listTopY by remember { mutableStateOf(0f) }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 56.dp)
                                        .onGloballyPositioned { coords ->
                                            listTopY = coords.positionInRoot().y
                                        },
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    state = listState
                                ) {
                                    itemsIndexed(
                                        items = filteredTasks,
                                        key = { _, task -> task.id }
                                    ) { index, task ->
                                        // Reorder by long-press drag with a picked-up card that follows the finger
                                        var isDragging by remember { mutableStateOf(false) }
                                        var itemHeightPx by remember { mutableStateOf(0f) }
                                        var dragOffset by remember { mutableStateOf(0f) }
                                        val rowSpacingPx = with(LocalDensity.current) { 6.dp.toPx() }
                                        // Gesture-scoped starting index to avoid staleness after reordering
                                        var gestureStartIndex by remember { mutableStateOf(index) }
                                        var itemTopY by remember { mutableStateOf(0f) }
                                        var startCenterY by remember { mutableStateOf(0f) }

                                        val timersDisabled by viewModel.disableTimers.collectAsState()
                                        val subtasks by viewModel.getSubtasksFlow(task.id).collectAsState(initial = emptyList())
                                        val displayDuration = when {
                                            timersDisabled -> 0
                                            subtasks.isNotEmpty() -> subtasks.sumOf { it.durationSeconds }
                                            else -> task.durationSeconds
                                        }
                                        TaskItem(
                                            task = task,
                                            index = index,
                                            totalTasks = filteredTasks.size,
                                            onDelete = { viewModel.deleteTask(task) },
                                            onComplete = {
                                                if (task.isDaily) {
                                                    scope.launch {
                                                        viewModel.markDailyTaskCompleted(task)
                                                    }
                                                } else {
                                                    viewModel.markTaskCompleted(task)
                                                }
                                            },
                                            onEdit = {
                                                editingTask = task
                                                editingTaskPosition = index
                                                showEditDialog = true
                                            },
                                            onToggleDaily = { viewModel.toggleDailyTask(task) },
                                            modifier = Modifier
                                                .onGloballyPositioned { coords ->
                                                    itemHeightPx = coords.size.height.toFloat()
                                                    itemTopY = coords.positionInRoot().y
                                                }
                                                .graphicsLayer {
                                                    if (isDragging) {
                                                        translationY = dragOffset
                                                        scaleX = 1.02f
                                                        scaleY = 1.02f
                                                        shadowElevation = 16f
                                                        alpha = 0.98f
                                                    }
                                                }
                                                .zIndex(if (isDragging) 1f else 0f)
                                                .pointerInput(task.id) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            isDragging = true
                                                            dragOffset = 0f
                                                            // Compute fresh start index from current list
                                                            gestureStartIndex = filteredTasks.indexOfFirst { it.id == task.id }.coerceAtLeast(0)
                                                            startCenterY = itemTopY + (itemHeightPx / 2f)
                                                            dragHoverIndex = gestureStartIndex
                                                            draggingTaskId = task.id
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            dragOffset += dragAmount.y
                                                            val centerY = startCenterY + dragOffset
                                                            val visible = listState.layoutInfo.visibleItemsInfo
                                                            // Auto-scroll near edges (index-based for broad version compatibility)
                                                            val viewportTop = listTopY
                                                            val viewportBottom = viewportTop + listState.layoutInfo.viewportEndOffset
                                                            val edge = 64f
                                                            val firstIndex = listState.firstVisibleItemIndex
                                                            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstIndex
                                                            if (centerY < viewportTop + edge) {
                                                                scope.launch { listState.animateScrollToItem((firstIndex - 1).coerceAtLeast(0)) }
                                                            } else if (centerY > viewportBottom - edge) {
                                                                scope.launch { listState.animateScrollToItem((lastIndex + 1).coerceAtMost(filteredTasks.lastIndex)) }
                                                            }
                                                            if (visible.isNotEmpty()) {
                                                                val first = visible.first()
                                                                val last = visible.last()
                                                                val topFirst = listTopY + first.offset
                                                                val bottomLast = listTopY + last.offset + last.size
                                                                val candidateIndex = when {
                                                                    centerY <= topFirst -> 0
                                                                    centerY >= bottomLast -> filteredTasks.lastIndex
                                                                    else -> {
                                                                        val over = visible.minByOrNull { info ->
                                                                            val center = listTopY + info.offset + (info.size / 2f)
                                                                            kotlin.math.abs(centerY - center)
                                                                        }
                                                                        over?.index ?: gestureStartIndex
                                                                    }
                                                                }
                                                                dragHoverIndex = candidateIndex
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            val centerY = startCenterY + dragOffset
                                                            val visible = listState.layoutInfo.visibleItemsInfo
                                                            val target = if (visible.isNotEmpty()) {
                                                                val first = visible.first()
                                                                val last = visible.last()
                                                                val topFirst = listTopY + first.offset
                                                                val bottomLast = listTopY + last.offset + last.size
                                                                when {
                                                                    centerY <= topFirst -> 0
                                                                    centerY >= bottomLast -> filteredTasks.lastIndex
                                                                    else -> {
                                                                        val over = visible.minByOrNull { info ->
                                                                            val center = listTopY + info.offset + (info.size / 2f)
                                                                            kotlin.math.abs(centerY - center)
                                                                        }
                                                                        over?.index ?: gestureStartIndex
                                                                    }
                                                                }
                                                            } else gestureStartIndex
                                                            if (target != gestureStartIndex) {
                                                                viewModel.updateTaskWithOrder(task, target)
                                                            }
                                                            isDragging = false
                                                            dragOffset = 0f
                                                            dragHoverIndex = null
                                                            draggingTaskId = null
                                                        },
                                                        onDragCancel = {
                                                            isDragging = false
                                                            dragOffset = 0f
                                                            dragHoverIndex = null
                                                            draggingTaskId = null
                                                        }
                                                    )
                                                },
                                            showDuration = !timersDisabled,
                                            displayedDurationSeconds = displayDuration,
                                            isDropTarget = (dragHoverIndex == index) && (draggingTaskId != task.id)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Start Tasks button at bottom
                        if (!historySelected && filteredTasks.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.setWasInDailyList(dailiesSelected)
                                        viewModel.startCurrentTask()
                                        onNavigateToDo()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = TimerGradient,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Start Tasks",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add task dialog
            if (showAddDialog) {
                val timersDisabled by viewModel.disableTimers.collectAsState()
                AddTaskDialog(
                    onDismiss = { showAddDialog = false },
                    onAddTask = { title: String, durationSeconds: Int, isDaily: Boolean, newSubtasks: List<Pair<String, Int>> ->
                        if (newSubtasks.isEmpty()) {
                            viewModel.addTask(title, durationSeconds, isDaily, addToTop)
                        } else {
                            viewModel.addTaskWithSubtasks(title, durationSeconds, isDaily, addToTop, newSubtasks)
                        }
                        // Don't close the dialog - let it stay open for the next task
                    },
                    isDaily = dailiesSelected,
                    addToTop = addToTop,
                    onAddToTopChange = { addToTop = it },
                    timersDisabled = timersDisabled,
                    viewModel = viewModel
                )
            }

            // Edit task dialog
            if (showEditDialog && editingTask != null) {
                EditTaskDialog(
                    task = editingTask!!,
                    currentPosition = editingTaskPosition,
                    onDismiss = {
                        showEditDialog = false
                        editingTask = null
                    },
                    onEditTask = { title: String, durationSeconds: Int, isDaily: Boolean, order: Int ->
                        viewModel.updateTaskWithOrder(
                            editingTask!!.copy(
                                title = title,
                                durationSeconds = durationSeconds
                            ), order
                        )
                        showEditDialog = false
                        editingTask = null
                    },
                    viewModel = viewModel
                )
            }

            // Rename list dialog
            if (showRenameDialog && renameListId != null) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text("Rename List") },
                    text = {
                        OutlinedTextField(
                            value = renameListValue,
                            onValueChange = { renameListValue = it },
                            label = { Text("List Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (renameListValue.text.isNotBlank()) {
                                val list = taskLists.find { it.id == renameListId }
                                list?.let { 
                                    val newName = if (list.name.startsWith("Premade: ")) {
                                        "Premade: " + renameListValue.text
                                    } else {
                                        renameListValue.text
                                    }
                                    viewModel.updateTaskList(it.copy(name = newName))
                                }
                                showRenameDialog = false
                            }
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Add list dialog
            if (showAddListDialog) {
                AlertDialog(
                    onDismissRequest = { showAddListDialog = false },
                    title = { Text("Add New List") },
                    text = {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            label = { Text("List Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newListName.text.isNotBlank()) {
                                viewModel.addTaskList(newListName.text)
                                newListName = TextFieldValue("")
                                showAddListDialog = false
                            }
                        }) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddListDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Add premade task dialog
            if (showAddPremadeDialog) {
                AlertDialog(
                    onDismissRequest = { showAddPremadeDialog = false },
                    title = { Text("Add Premade Task List") },
                    text = {
                        OutlinedTextField(
                            value = newPremadeName,
                            onValueChange = { newPremadeName = it },
                            label = { Text("Premade Task List Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newPremadeName.text.isNotBlank()) {
                                viewModel.addTaskList("Premade: " + newPremadeName.text)
                                newPremadeName = TextFieldValue("")
                                showAddPremadeDialog = false
                            }
                        }) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPremadeDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // First-run notification prompt
            val showNotifPrompt by viewModel.showNotificationPrompt.collectAsState()
            if (showNotifPrompt) {
                AlertDialog(
                    onDismissRequest = { viewModel.completeFirstRunPrompt() },
                    title = { Text("Enable Notifications?") },
                    text = { Text("Enable notifications so timers and alarms can alert you even when the app is in the background.") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.completeFirstRunPrompt()
                            viewModel.openNotificationSettings()
                        }) { Text("Open Settings") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.completeFirstRunPrompt() }) { Text("Not now") }
                    }
                )
            }

            // Settings dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Settings") },
                    text = {
                        Column {
                            // CTDAD visibility toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showCtdadBar, onCheckedChange = { viewModel.setShowCtdadBar(it) })
                                Spacer(Modifier.width(8.dp))
                                Text("Show CTDAD in top bar")
                            }
                            Spacer(Modifier.height(8.dp))
                            // Actual time vs planned duration
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val useActual by viewModel.useActualTimeForCtdad.collectAsState()
                                Checkbox(checked = useActual, onCheckedChange = { viewModel.setUseActualTimeForCtdad(it) })
                                Spacer(Modifier.width(8.dp))
                                Text("Use actual time spent for CTDAD")
                            }
                            Spacer(Modifier.height(8.dp))
                            // Disable timers (timerless mode)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val timersDisabled by viewModel.disableTimers.collectAsState()
                                Checkbox(checked = timersDisabled, onCheckedChange = { viewModel.setDisableTimers(it) })
                                Spacer(Modifier.width(8.dp))
                                Text("Disable timers (timerless mode)")
                            }
                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))
                            Text("Notifications:")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.openNotificationSettings() }) { Text("Open Notification Settings") }
                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))
                            Text("Alarm Sound:")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { mp3PickerLauncher.launch("audio/mpeg") }) {
                                Text("Choose MP3 File")
                            }
                            if (selectedAudioFile.isNotEmpty()) {
                                Text(
                                    "Selected: $selectedAudioFile",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showSettingsDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Day logs dialog
            if (showDayLogsDialog && dayLogsToShow.isNotEmpty()) {
                val logs by viewModel.getCompletedLogsByDay(dayLogsToShow).collectAsState(initial = emptyList())
                AlertDialog(
                    onDismissRequest = { showDayLogsDialog = false },
                    title = { Text("Completed on $dayLogsToShow") },
                    text = {
                        Column(modifier = Modifier.heightIn(max = 320.dp)) {
                            if (logs.isEmpty()) {
                                Text("No logs for this day")
                            } else {
                                logs.forEach { log ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(log.title)
                                        val h = log.durationSeconds / 3600
                                        val m = (log.durationSeconds % 3600) / 60
                                        Text(String.format("%02d:%02d", h, m))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showDayLogsDialog = false }) { Text("Close") }
                    }
                )
            }

            // Sync dialog
            if (showSyncDialog) {
                val context = LocalContext.current
                AlertDialog(
                    onDismissRequest = { showSyncDialog = false },
                    title = { Text("Sync Data") },
                    text = {
                        Column {
                            Text("Export your data to copy to the web app, or import data from the web app.")
                            Spacer(Modifier.height(16.dp))

                            Text("Export Data", fontWeight = FontWeight.Bold)
                            Text("Click the button below to copy your current data to clipboard.")
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val data = viewModel.exportData()
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText(
                                            "DoWithTime Data",
                                            data
                                        )
                                        clipboard.setPrimaryClip(clip)
                                        showSyncDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export to Clipboard")
                            }

                            Spacer(Modifier.height(16.dp))
                            Divider()
                            Spacer(Modifier.height(16.dp))

                            Text("Import Data", fontWeight = FontWeight.Bold)
                            Text("Paste data from the web app below to import it here.")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = syncData,
                                onValueChange = { syncData = it },
                                label = { Text("Paste web app data here") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (syncData.isNotBlank()) {
                                        val success = viewModel.importData(syncData)
                                        if (success) {
                                            showSyncDialog = false
                                            syncData = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Import Data")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showSyncDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Move Tasks dialog
            if (showMoveTasksDialog) {
                MoveTasksDialog(
                    tasks = if (dailiesSelected) dailyTasks else tasks,
                    onDismiss = { showMoveTasksDialog = false },
                    onMoveTasks = { selectedTasks: List<Task>, targetPosition: Int ->
                        if (dailiesSelected) {
                            viewModel.insertDailyTasksAtPosition(selectedTasks, targetPosition)
                        } else {
                            viewModel.insertTasksAtPosition(selectedTasks, targetPosition)
                        }
                        showMoveTasksDialog = false
                    }
                )
            }

            // Paste to List dialog
            if (showPasteToListDialog) {
                PasteToListDialog(
                    sourceListId = pasteSourceListId,
                    sourceIsDaily = pasteSourceIsDaily,
                    taskLists = taskLists,
                    onDismiss = { showPasteToListDialog = false },
                    onPasteTasks = { targetListId: Int, targetPosition: Int ->
                        viewModel.pasteTasksFromListToPosition(
                            pasteSourceListId,
                            pasteSourceIsDaily,
                            targetListId,
                            targetPosition
                        )
                        showPasteToListDialog = false
                    }
                )
            }
            // Floating add button bottom-right
            if (!historySelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { showAddDialog = true },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
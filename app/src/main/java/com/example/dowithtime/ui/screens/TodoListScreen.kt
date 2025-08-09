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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.dowithtime.ui.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


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
    modifier: Modifier = Modifier
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
            containerColor = if (isCompletedDaily) MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            ) else MaterialTheme.colorScheme.surface
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
                        text = formatDuration(task.durationSeconds),
                        fontSize = 13.sp,
                        color = if (isCompletedDaily) MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
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
    onAddTask: (String, Int, Boolean) -> Unit,
    isDaily: Boolean = false,
    addToTop: Boolean,
    onAddToTopChange: (Boolean) -> Unit
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.shadow(16.dp, RoundedCornerShape(20.dp)),
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
                modifier = Modifier.padding(vertical = 8.dp)
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

                // Duration section
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

                                    if (totalSeconds <= 0) {
                                        durationError = true
                                        minutesFocusRequester.requestFocus()
                                        return@KeyboardActions
                                    }

                                    onAddTask(title, totalSeconds, isDaily)
                                    clearForm()
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
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        titleFocusRequester.requestFocus()
                        return@Button
                    }

                    val minutesInt = minutes.toIntOrNull() ?: 0
                    val secondsInt = seconds.toIntOrNull() ?: 0
                    val totalSeconds = minutesInt * 60 + secondsInt

                    if (totalSeconds <= 0) {
                        durationError = true
                        minutesFocusRequester.requestFocus()
                        return@Button
                    }

                    onAddTask(title, totalSeconds, isDaily)
                    clearForm()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    currentPosition: Int,
    onDismiss: () -> Unit,
    onEditTask: (String, Int, Boolean, Int) -> Unit
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.shadow(16.dp, RoundedCornerShape(20.dp)),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Edit Task",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Duration section
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

                Spacer(modifier = Modifier.height(20.dp))

                // Order section
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
                                val totalSeconds = minutesInt * 60 + secondsInt

                                if (totalSeconds <= 0) {
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

                                onEditTask(title, totalSeconds, task.isDaily, orderInt - 1)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
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
                    val totalSeconds = minutesInt * 60 + secondsInt

                    if (totalSeconds <= 0) {
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
    var selectedTargetListId by remember { mutableStateOf(0) } // 0 for unselected, -1 for daily, positive for regular lists
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
                        value = when (selectedTargetListId) {
                            -1 -> "Dailies"
                            else -> taskLists.find { it.id == selectedTargetListId }?.name
                                ?: "Select a list"
                        },
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
                        // Daily option
                        DropdownMenuItem(
                            text = { Text("Dailies") },
                            onClick = {
                                selectedTargetListId = -1
                                expanded = false
                            }
                        )

                        // Regular lists
                        taskLists.forEach { list ->
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
    var showMoveTasksDialog by remember { mutableStateOf(false) }
    var showPasteToListDialog by remember { mutableStateOf(false) }
    var pasteSourceListId by remember { mutableStateOf<Int?>(null) }
    var pasteSourceIsDaily by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncData by remember { mutableStateOf("") }

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
                // CTDAD History section
                Text(
                    text = "History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("CTDAD History") },
                    selected = historySelected,
                    onClick = {
                        historySelected = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                // User lists
                Text(
                    text = "Your Lists",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                taskLists.forEach { list ->
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
                // Add List button
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showAddListDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New List")
                }
                Spacer(Modifier.height(16.dp))
                // Move Tasks button
                Button(
                    onClick = { showMoveTasksDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Move Tasks")
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Settings button
                Button(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (historySelected) "CTDAD History" else taskLists.find { it.id == currentListId }?.name
                                ?: "Tasks",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSyncDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Sync")
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header: Today's Completed Time (CTDAD)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Completed Today", fontWeight = FontWeight.Bold)
                        val hours = todayTotalSeconds / 3600
                        val minutes = (todayTotalSeconds % 3600) / 60
                        Text(String.format("%02d:%02d", hours, minutes), fontWeight = FontWeight.Bold)
                    }
                }
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
                                                .padding(horizontal = 12.dp),
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
                                                val h = summary.totalSeconds / 3600
                                                val m = (summary.totalSeconds % 3600) / 60
                                                Text(String.format("%02d:%02d", h, m), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Task list with working drag and drop
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 56.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    state = listState
                                ) {
                                    itemsIndexed(
                                        items = filteredTasks,
                                        key = { _, task -> task.id }
                                    ) { index, task ->
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
                                            onToggleDaily = { viewModel.toggleDailyTask(task) }
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
                AddTaskDialog(
                    onDismiss = { showAddDialog = false },
                    onAddTask = { title: String, durationSeconds: Int, isDaily: Boolean ->
                        viewModel.addTask(title, durationSeconds, isDaily, addToTop)
                        // Don't close the dialog - let it stay open for the next task
                    },
                    isDaily = dailiesSelected,
                    addToTop = addToTop,
                    onAddToTopChange = { addToTop = it }
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
                    }
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
                                list?.let { viewModel.updateTaskList(it.copy(name = renameListValue.text)) }
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

            // Settings dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Settings") },
                    text = {
                        Column {
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
        }
    }
}
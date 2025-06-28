package com.example.dowithtime.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dowithtime.data.Task
import com.example.dowithtime.viewmodel.TaskViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TaskViewModel,
    onNavigateToDo: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingTaskPosition by remember { mutableStateOf(0) }
    
    // Debug: Log when tasks change
    LaunchedEffect(tasks) {
        println("UI: Tasks updated - ${tasks.mapIndexed { index, task -> "${index}:${task.title}" }}")
    }
    
    // Drag state
    var draggedItemId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task list - takes up remaining space and scrolls
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks yet.\nAdd your first task to get started!",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    itemsIndexed(
                        items = tasks,
                        key = { _, task -> task.id }
                    ) { index, task ->
                        TaskItem(
                            task = task,
                            index = index,
                            totalTasks = tasks.size,
                            isDragging = draggedItemId == task.id,
                            dragOffset = if (draggedItemId == task.id) dragOffset else 0f,
                            isHovered = hoveredIndex == index,
                            onDelete = { viewModel.deleteTask(task) },
                            onComplete = { viewModel.markTaskCompleted(task) },
                            onEdit = { 
                                editingTask = task
                                editingTaskPosition = index
                                showEditDialog = true
                            },
                            onDragStart = { offset ->
                                val actualIndex = tasks.indexOfFirst { it.id == task.id }
                                println("=== DRAG START ===")
                                println("Task: ${task.title} (ID: ${task.id})")
                                println("Current index: $index")
                                println("Actual index: $actualIndex")
                                println("Start offset: $offset")
                                draggedItemId = task.id
                                dragOffset = 0f
                                hoveredIndex = null
                            },
                            onDragEnd = { 
                                val actualIndex = tasks.indexOfFirst { it.id == task.id }
                                println("=== DRAG END ===")
                                println("Final drag offset: $dragOffset")
                                println("Actual index: $actualIndex")
                                draggedItemId?.let { fromId ->
                                    // Calculate target position based on final drag offset
                                    // Use actual task height as threshold (based on measurements)
                                    val threshold = 220f // Pixels needed to move one position
                                    val targetIndex = when {
                                        dragOffset > threshold -> {
                                            // Calculate how many positions to move down
                                            val positionsDown = (dragOffset / threshold).toInt()
                                            (actualIndex + positionsDown).coerceAtMost(tasks.size - 1)
                                        }
                                        dragOffset < -threshold -> {
                                            // Calculate how many positions to move up
                                            val positionsUp = (-dragOffset / threshold).toInt()
                                            (actualIndex - positionsUp).coerceAtLeast(0)
                                        }
                                        else -> {
                                            // Not dragged far enough - stay in same position
                                            actualIndex
                                        }
                                    }
                                    
                                    // Debug output
                                    println("Drag: from index $actualIndex to target $targetIndex, offset: $dragOffset")
                                    println("Threshold: $threshold")
                                    println("Will reorder: ${targetIndex != actualIndex}")
                                    
                                    if (targetIndex != actualIndex) {
                                        println("Calling reorderTask($actualIndex, $targetIndex)")
                                        viewModel.reorderTask(actualIndex, targetIndex)
                                    }
                                }
                                draggedItemId = null
                                dragOffset = 0f
                                hoveredIndex = null
                                println("=== DRAG END COMPLETE ===")
                            },
                            onDrag = { change, dragAmount ->
                                if (draggedItemId == task.id) {
                                    dragOffset += dragAmount.y
                                    println("Drag update: offset = $dragOffset, amount = $dragAmount")
                                    
                                    // Calculate which task we're hovering over
                                    // Use a smaller threshold for hover detection to make it more sensitive
                                    val hoverThreshold = 110f // Half the task height for easier hover detection
                                    val positionThreshold = 220f // Full task height for accurate position calculation
                                    val actualIndex = tasks.indexOfFirst { it.id == task.id }
                                    val targetIndex = when {
                                        dragOffset > hoverThreshold -> {
                                            // Calculate how many positions to move down using the correct threshold
                                            val positionsDown = (dragOffset / positionThreshold).toInt()
                                            (actualIndex + positionsDown).coerceAtMost(tasks.size - 1)
                                        }
                                        dragOffset < -hoverThreshold -> {
                                            // Calculate how many positions to move up using the correct threshold
                                            val positionsUp = (-dragOffset / positionThreshold).toInt()
                                            (actualIndex - positionsUp).coerceAtLeast(0)
                                        }
                                        else -> actualIndex
                                    }
                                    hoveredIndex = if (targetIndex != actualIndex) targetIndex else null
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Fixed Start Tasks button at bottom
        if (tasks.isNotEmpty()) {
            Button(
                onClick = {
                    // Start the timer for the first task automatically
                    viewModel.startCurrentTask()
                    // Then navigate to the Do screen
                    onNavigateToDo()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Text(
                    text = "Start Tasks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Add task dialog
    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAddTask = { title, durationSeconds, isDaily ->
                viewModel.addTask(title, durationSeconds, isDaily)
            }
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
            onEditTask = { title, durationSeconds, isDaily, order ->
                viewModel.updateTaskWithOrder(editingTask!!.copy(
                    title = title,
                    durationSeconds = durationSeconds,
                    isDaily = isDaily
                ), order)
                showEditDialog = false
                editingTask = null
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    index: Int,
    totalTasks: Int,
    isDragging: Boolean,
    dragOffset: Float,
    isHovered: Boolean,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = when {
                    isDragging -> dragOffset
                    isHovered -> 20f // Move down when hovered
                    else -> 0f
                }
                alpha = if (isDragging) 0.8f else 1f
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = onDragStart,
                    onDragEnd = onDragEnd,
                    onDrag = onDrag
                )
            }
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when {
                isDragging -> 8.dp
                isHovered -> 4.dp
                else -> 2.dp
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDuration(task.durationSeconds),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isDragging) {
                    Text(
                        text = "Drag offset: ${dragOffset.toInt()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Complete")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
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
    onAddTask: (String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }
    var isDaily by remember { mutableStateOf(false) }
    
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
        isDaily = false
        // Re-focus title field after clearing
        titleFocusRequester.requestFocus()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Task") },
        text = {
            Column {
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
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { minutesFocusRequester.requestFocus() }
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Duration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            .weight(1f)
                            .focusRequester(minutesFocusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { secondsFocusRequester.requestFocus() }
                        )
                    )
                    
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { 
                            seconds = it.filter { char -> char.isDigit() }
                            durationError = false
                        },
                        label = { Text("Seconds") },
                        isError = durationError,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(secondsFocusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // Validate and add task when Enter is pressed in seconds field
                                if (title.isNotBlank()) {
                                    val minutesInt = minutes.toIntOrNull() ?: 0
                                    val secondsInt = seconds.toIntOrNull() ?: 0
                                    val totalSeconds = minutesInt * 60 + secondsInt
                                    
                                    if (totalSeconds > 0) {
                                        onAddTask(title, totalSeconds, isDaily)
                                        clearForm()
                                    } else {
                                        durationError = true
                                    }
                                } else {
                                    titleError = true
                                    titleFocusRequester.requestFocus()
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
                        checked = isDaily,
                        onCheckedChange = { isDaily = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Make this a daily task",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (durationError) {
                    Text(
                        text = "Please enter a valid duration (at least 1 second)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
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
                        return@Button
                    }
                    
                    val minutesInt = minutes.toIntOrNull() ?: 0
                    val secondsInt = seconds.toIntOrNull() ?: 0
                    val totalSeconds = minutesInt * 60 + secondsInt
                    
                    if (totalSeconds <= 0) {
                        durationError = true
                        return@Button
                    }
                    
                    onAddTask(title, totalSeconds, isDaily)
                    clearForm() // Clear the form and re-focus title field
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
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
    var isDaily by remember { mutableStateOf(task.isDaily) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleError = false
                    },
                    label = { Text("Task Title") },
                    isError = titleError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Duration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { 
                            minutes = it.filter { char -> char.isDigit() }
                            durationError = false
                        },
                        label = { Text("Minutes") },
                        isError = durationError,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { 
                            seconds = it.filter { char -> char.isDigit() }
                            durationError = false
                        },
                        label = { Text("Seconds") },
                        isError = durationError,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = order,
                    onValueChange = { 
                        order = it.filter { char -> char.isDigit() }
                        orderError = false
                    },
                    label = { Text("Position (1, 2, 3, etc.)") },
                    isError = orderError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDaily,
                        onCheckedChange = { isDaily = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Make this a daily task",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (durationError) {
                    Text(
                        text = "Please enter a valid duration (at least 1 second)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                if (orderError) {
                    Text(
                        text = "Please enter a valid position (1 or higher)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
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
                        return@Button
                    }
                    
                    val minutesInt = minutes.toIntOrNull() ?: 0
                    val secondsInt = seconds.toIntOrNull() ?: 0
                    val totalSeconds = minutesInt * 60 + secondsInt
                    
                    if (totalSeconds <= 0) {
                        durationError = true
                        return@Button
                    }
                    
                    val orderInt = order.toIntOrNull() ?: 0
                    if (orderInt <= 0) {
                        orderError = true
                        return@Button
                    }
                    
                    onEditTask(title, totalSeconds, isDaily, orderInt - 1) // Convert to 0-based index
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(durationSeconds: Int): String {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0s"
    }
} 
package com.example.dowithtime.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dowithtime.data.Task
import com.example.dowithtime.viewmodel.TaskViewModel

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
    var draggedItem by remember { mutableStateOf<Task?>(null) }
    var draggedItemIndex by remember { mutableStateOf(-1) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp) // Add padding for the floating button
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
            
            // Task list
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    itemsIndexed(
                        items = tasks,
                        key = { _, task -> task.id }
                    ) { index, task ->
                        DraggableTaskItem(
                            task = task,
                            index = index,
                            tasks = tasks,
                            isDragging = draggedItem?.id == task.id,
                            onDelete = { viewModel.deleteTask(task) },
                            onComplete = { viewModel.markTaskCompleted(task) },
                            onEdit = { 
                                editingTask = task
                                showEditDialog = true
                            },
                            onDragStart = { 
                                draggedItem = task
                                draggedItemIndex = index
                            },
                            onDragEnd = { 
                                draggedItem = null
                                draggedItemIndex = -1
                            },
                            onMove = { fromIndex, toIndex ->
                                viewModel.reorderTasks(fromIndex, toIndex)
                            }
                        )
                    }
                }
            }
        }
        
        // Fixed Start Tasks button at bottom
        if (tasks.isNotEmpty()) {
            Button(
                onClick = onNavigateToDo,
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
                showAddDialog = false
            }
        )
    }
    
    // Edit task dialog
    if (showEditDialog && editingTask != null) {
        EditTaskDialog(
            task = editingTask!!,
            onDismiss = { 
                showEditDialog = false
                editingTask = null
            },
            onEditTask = { title, durationSeconds, isDaily ->
                viewModel.updateTask(editingTask!!.copy(
                    title = title,
                    durationSeconds = durationSeconds,
                    isDaily = isDaily
                ))
                showEditDialog = false
                editingTask = null
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
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

@Composable
fun DraggableTaskItem(
    task: Task,
    index: Int,
    tasks: List<Task>,
    isDragging: Boolean,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val density = LocalDensity.current
    var dragOffset by remember { mutableStateOf(0f) }
    var isBeingDragged by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .graphicsLayer {
                translationY = dragOffset
                alpha = if (isDragging || isBeingDragged) 0.8f else 1f
                scaleX = if (isDragging || isBeingDragged) 1.05f else 1f
                scaleY = if (isDragging || isBeingDragged) 1.05f else 1f
                shadowElevation = if (isDragging || isBeingDragged) 8f else 2f
            }
            .zIndex(if (isDragging || isBeingDragged) 1f else 0f)
            .pointerInput(task.id) {
                detectDragGestures(
                    onDragStart = { 
                        isBeingDragged = true
                        onDragStart()
                    },
                    onDragEnd = {
                        isBeingDragged = false
                        dragOffset = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        
                        // Simple threshold-based reordering
                        val threshold = 40.dp
                        val thresholdPx = with(density) { threshold.toPx() }
                        
                        if (dragOffset > thresholdPx && index < tasks.size - 1) {
                            // Move down
                            onMove(index, index + 1)
                            dragOffset = 0f
                        } else if (dragOffset < -thresholdPx && index > 0) {
                            // Move up
                            onMove(index, index - 1)
                            dragOffset = 0f
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order number
            Text(
                text = "${index + 1}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
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
                if (task.isDaily) {
                    Text(
                        text = "Daily Task",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
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
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onEditTask: (String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var minutes by remember { mutableStateOf((task.durationSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((task.durationSeconds % 60).toString()) }
    var titleError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }
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
                    
                    onEditTask(title, totalSeconds, isDaily)
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
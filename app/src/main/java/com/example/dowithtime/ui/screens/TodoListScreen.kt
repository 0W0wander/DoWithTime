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
                        .weight(1f), // Take up remaining space
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
                            onDelete = { viewModel.deleteTask(task) },
                            onComplete = { viewModel.markTaskCompleted(task) },
                            onEdit = { 
                                editingTask = task
                                showEditDialog = true
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
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
    
    fun clearForm() {
        title = ""
        minutes = ""
        seconds = ""
        titleError = false
        durationError = false
        isDaily = false
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
                    clearForm() // Clear the form for the next task
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
    onDismiss: () -> Unit,
    onEditTask: (String, Int, Boolean, Int) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var minutes by remember { mutableStateOf((task.durationSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((task.durationSeconds % 60).toString()) }
    var order by remember { mutableStateOf((task.order + 1).toString()) }
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
package com.example.dowithtime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.dowithtime.data.Task
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TaskViewModel,
    onNavigateToDo: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    val taskLists by viewModel.taskLists.collectAsState()
    val currentListId by viewModel.currentListId.collectAsState()
    val wasInDailyList by viewModel.wasInDailyList.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingTaskPosition by remember { mutableStateOf(0) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameListId by remember { mutableStateOf<Int?>(null) }
    var renameListValue by remember { mutableStateOf(TextFieldValue("")) }
    var showAddListDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf(TextFieldValue("")) }

    // Dailies selection state
    var dailiesSelected by remember { mutableStateOf(wasInDailyList) }
    
    // Update dailiesSelected when wasInDailyList changes
    LaunchedEffect(wasInDailyList) {
        dailiesSelected = wasInDailyList
    }

    // Drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Filter tasks for Dailies or selected list
    val filteredTasks = if (dailiesSelected) dailyTasks else tasks

    // Drag state
    var draggedItemId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var originalDragIndex by remember { mutableStateOf<Int?>(null) }
    // Track item positions (top..bottom in window)
    val itemPositions = remember { mutableStateMapOf<Int, IntRange>() }

    // Reset drag state whenever the list changes (e.g., after reorder)
    LaunchedEffect(filteredTasks) {
        draggedItemId = null
        dragOffset = 0f
        hoveredIndex = null
        originalDragIndex = null
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedAudioFile by remember { mutableStateOf("") }

    // File picker launcher for MP3
    val mp3PickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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
                // Dailies section
                Text(
                    text = "Dailies",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Dailies", fontWeight = if (dailiesSelected) FontWeight.Bold else FontWeight.Normal) },
                    selected = dailiesSelected,
                    onClick = {
                        dailiesSelected = true
                        viewModel.setWasInDailyList(true)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
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
                    NavigationDrawerItem(
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(list.name, fontWeight = if (!dailiesSelected && list.id == currentListId) FontWeight.Bold else FontWeight.Normal)
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    renameListId = list.id
                                    renameListValue = TextFieldValue(list.name)
                                    showRenameDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename List", modifier = Modifier.size(16.dp))
                                }
                                if (taskLists.size > 1) {
                                    IconButton(onClick = { viewModel.deleteTaskList(list.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete List", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        },
                        selected = !dailiesSelected && list.id == currentListId,
                        onClick = {
                            dailiesSelected = false
                            viewModel.setWasInDailyList(false)
                            viewModel.selectList(list.id)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
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
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New List")
                }
                Spacer(Modifier.height(16.dp))
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
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            text = if (dailiesSelected) "Dailies" else taskLists.find { it.id == currentListId }?.name ?: "Tasks",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (filteredTasks.isEmpty()) {
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
                                isDragging = draggedItemId == task.id,
                                dragOffset = if (draggedItemId == task.id) dragOffset else 0f,
                                isHovered = hoveredIndex == index,
                                onDelete = { viewModel.deleteTask(task) },
                                onComplete = { 
                                    if (task.isDaily) {
                                        viewModel.markDailyTaskCompleted(task)
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
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInWindow().y.toInt()
                                    val height = coordinates.size.height
                                    itemPositions[task.id] = position until (position + height)
                                },
                                onDragStart = { offset ->
                                    val actualIndex = filteredTasks.indexOfFirst { it.id == task.id }
                                    draggedItemId = task.id
                                    dragOffset = 0f
                                    hoveredIndex = null
                                    originalDragIndex = actualIndex
                                },
                                onDrag = { change, dragAmount ->
                                    if (draggedItemId == task.id && originalDragIndex != null) {
                                        dragOffset += dragAmount.y
                                        val pointerY = change.position.y.toInt() + itemPositions[task.id]?.first.orZero()
                                        val hoveredTaskId = itemPositions.entries.firstOrNull { (_, range) ->
                                            pointerY in range
                                        }?.key
                                        val newHoveredIndex = hoveredTaskId?.let { filteredTasks.indexOfFirst { it2 -> it2.id == it } }?.takeIf { it != -1 && it != originalDragIndex }
                                        if (newHoveredIndex != null) {
                                            // Perform reorder immediately
                                            viewModel.reorderTask(originalDragIndex!!, newHoveredIndex)
                                            // Update drag state for continued dragging
                                            originalDragIndex = newHoveredIndex
                                            dragOffset = 0f
                                        }
                                        hoveredIndex = newHoveredIndex
                                    }
                                },
                                onDragEnd = {
                                    draggedItemId = null
                                    dragOffset = 0f
                                    hoveredIndex = null
                                    originalDragIndex = null
                                }
                            )
                        }
                    }
                }
                
                // Compact Start Tasks button at bottom
                if (filteredTasks.isNotEmpty()) {
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
            onAddTask = { title, durationSeconds, isDaily ->
                viewModel.addTask(title, durationSeconds, isDaily)
                // Don't close the dialog - let it stay open for the next task
            },
            isDaily = dailiesSelected
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
            onEditTask = { title, durationSeconds, _, order ->
                viewModel.updateTaskWithOrder(editingTask!!.copy(
                    title = title,
                    durationSeconds = durationSeconds
                ), order)
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
                        Text("Selected: $selectedAudioFile", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onToggleDaily: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    // Determine if this is a completed daily task
    val isCompletedDaily = task.isDaily && task.completedToday
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = when {
                    isDragging -> dragOffset
                    isHovered -> 8f
                    else -> 0f
                }
                alpha = if (isDragging) 0.5f else if (isCompletedDaily) 0.6f else 1f
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
            containerColor = when {
                isHovered -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
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
                        color = if (isCompletedDaily) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (isDragging) {
                    Text(
                        text = "offset: ${dragOffset.toInt()}px",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
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
                    containerColor = if (task.isDaily) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
    isDaily: Boolean = false
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Number
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { secondsFocusRequester.requestFocus() }
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
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
                }
                
                // Error messages
                if (durationError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter a valid duration (at least 1 second)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
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
    var title by remember { mutableStateOf(TextFieldValue(task.title)) }
    var minutes by remember { mutableStateOf(TextFieldValue((task.durationSeconds / 60).toString())) }
    var seconds by remember { mutableStateOf(TextFieldValue((task.durationSeconds % 60).toString())) }
    var order by remember { mutableStateOf(TextFieldValue((currentPosition + 1).toString())) }
    var titleError by remember { mutableStateOf(false) }
    var durationError by remember { mutableStateOf(false) }
    var orderError by remember { mutableStateOf(false) }
    // Daily tasks cannot be edited to change their daily status
    
    // Focus management
    val titleFocusRequester = remember { FocusRequester() }
    val minutesFocusRequester = remember { FocusRequester() }
    val secondsFocusRequester = remember { FocusRequester() }
    val orderFocusRequester = remember { FocusRequester() }
    
    // Focus state tracking
    var titleFocused by remember { mutableStateOf(false) }
    var minutesFocused by remember { mutableStateOf(false) }
    var secondsFocused by remember { mutableStateOf(false) }
    var orderFocused by remember { mutableStateOf(false) }
    
    // Auto-select text when focused
    LaunchedEffect(titleFocused) {
        if (titleFocused) {
            title = title.copy(selection = TextRange(0, title.text.length))
        }
    }
    
    LaunchedEffect(minutesFocused) {
        if (minutesFocused) {
            minutes = minutes.copy(selection = TextRange(0, minutes.text.length))
        }
    }
    
    LaunchedEffect(secondsFocused) {
        if (secondsFocused) {
            seconds = seconds.copy(selection = TextRange(0, seconds.text.length))
        }
    }
    
    LaunchedEffect(orderFocused) {
        if (orderFocused) {
            order = order.copy(selection = TextRange(0, order.text.length))
        }
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
                            .onFocusChanged { titleFocused = it.isFocused }
                            .focusRequester(titleFocusRequester),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { minutesFocusRequester.requestFocus() })
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                                minutes = it.copy(text = it.text.filter { char -> char.isDigit() })
                                durationError = false
                            },
                            label = { Text("Minutes") },
                            isError = durationError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { minutesFocused = it.isFocused }
                                .focusRequester(minutesFocusRequester),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(onNext = { secondsFocusRequester.requestFocus() })
                        )
                    }
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
                                seconds = it.copy(text = it.text.filter { char -> char.isDigit() })
                                durationError = false
                            },
                            label = { Text("Seconds") },
                            isError = durationError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { secondsFocused = it.isFocused }
                                .focusRequester(secondsFocusRequester),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(onNext = { orderFocusRequester.requestFocus() })
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                            order = it.copy(text = it.text.filter { char -> char.isDigit() })
                            orderError = false
                        },
                        label = { Text("Position (1, 2, 3, etc.)") },
                        isError = orderError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { orderFocused = it.isFocused }
                            .focusRequester(orderFocusRequester),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            // Validate and save when Enter is pressed in order field
                            if (title.text.isBlank()) {
                                titleError = true
                                titleFocusRequester.requestFocus()
                                return@KeyboardActions
                            }
                            val minutesInt = minutes.text.toIntOrNull() ?: 0
                            val secondsInt = seconds.text.toIntOrNull() ?: 0
                            val totalSeconds = minutesInt * 60 + secondsInt
                            if (totalSeconds <= 0) {
                                durationError = true
                                minutesFocusRequester.requestFocus()
                                return@KeyboardActions
                            }
                            val orderInt = order.text.toIntOrNull() ?: 0
                            if (orderInt <= 0) {
                                orderError = true
                                orderFocusRequester.requestFocus()
                                return@KeyboardActions
                            }
                            onEditTask(title.text, totalSeconds, task.isDaily, orderInt - 1)
                        })
                    )
                }

                if (durationError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter a valid duration (at least 1 second)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (orderError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter a valid position (1 or higher)",
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.text.isBlank()) {
                        titleError = true
                        titleFocusRequester.requestFocus()
                        return@Button
                    }
                    val minutesInt = minutes.text.toIntOrNull() ?: 0
                    val secondsInt = seconds.text.toIntOrNull() ?: 0
                    val totalSeconds = minutesInt * 60 + secondsInt
                    if (totalSeconds <= 0) {
                        durationError = true
                        minutesFocusRequester.requestFocus()
                        return@Button
                    }
                    val orderInt = order.text.toIntOrNull() ?: 0
                    if (orderInt <= 0) {
                        orderError = true
                        orderFocusRequester.requestFocus()
                        return@Button
                    }
                    onEditTask(title.text, totalSeconds, task.isDaily, orderInt - 1)
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

private fun Int?.orZero() = this ?: 0 
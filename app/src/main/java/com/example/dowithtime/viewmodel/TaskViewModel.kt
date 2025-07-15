package com.example.dowithtime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dowithtime.data.AppDatabase
import com.example.dowithtime.data.Task
import com.example.dowithtime.data.TaskRepository
import com.example.dowithtime.data.TaskList
import com.example.dowithtime.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.dowithtime.data.CloudSync
import com.example.dowithtime.data.AppData
import com.example.dowithtime.data.SyncStatus

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val cloudSync: CloudSync
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    private val _dailyTasks = MutableStateFlow<List<Task>>(emptyList())
    val dailyTasks: StateFlow<List<Task>> = _dailyTasks.asStateFlow()
    
    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()
    
    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    private val _showAlarm = MutableStateFlow(false)
    val showAlarm: StateFlow<Boolean> = _showAlarm.asStateFlow()
    
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()
    
    private val _transitionTime = MutableStateFlow(10)
    val transitionTime: StateFlow<Int> = _transitionTime.asStateFlow()
    
    private var timerService: TimerService? = null
    
    // Multi-list support
    private val _taskLists = MutableStateFlow<List<TaskList>>(emptyList())
    val taskLists: StateFlow<List<TaskList>> = _taskLists.asStateFlow()
    private val _currentListId = MutableStateFlow<Int?>(null)
    val currentListId: StateFlow<Int?> = _currentListId.asStateFlow()
    
    private val _wasInDailyList = MutableStateFlow(false)
    val wasInDailyList: StateFlow<Boolean> = _wasInDailyList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _nextTask = MutableStateFlow<Task?>(null)
    val nextTask: StateFlow<Task?> = _nextTask.asStateFlow()
    
    private val _syncStatus = MutableStateFlow<SyncStatus?>(null)
    val syncStatus: StateFlow<SyncStatus?> = _syncStatus.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        cloudSync = CloudSync(application)
        
        // Set loading to true initially to prevent flash of completed tasks
        _isLoading.value = true
        
        refreshTaskLists()
        // Don't call refreshTasksForCurrentList here as it will be called after loadData() sets the current list ID
        
        // Reset daily task completion at app start
        resetDailyTaskCompletion()
        
        viewModelScope.launch {
            repository.getAllDailyTasks().collect { dailyTaskList ->
                _dailyTasks.value = dailyTaskList
            }
        }
        
        loadData()
        performAutoSync()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Don't load all tasks initially - only load incomplete tasks for the current list
            _taskLists.value = repository.getAllTaskLists().first()
            
            // Set current list to first available list if none is set
            if (_taskLists.value.isNotEmpty() && _currentListId.value == null) {
                _currentListId.value = _taskLists.value.first().id
                // Refresh tasks for the newly set current list
                refreshTasksForCurrentList(_currentListId.value)
            }
            
            // Set loading to false after data is loaded
            _isLoading.value = false
        }
    }
    
    private fun performAutoSync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Convert local data to web-compatible AppData format
                val localData = cloudSync.convertToWebFormat(
                    dailyTasks = repository.getAllTasks(),
                    taskLists = _taskLists.value,
                    currentListId = _currentListId.value
                )
                
                // Perform auto-sync
                val syncResult = cloudSync.autoSync(localData)
                syncResult.onSuccess { syncedData ->
                    // Update local data with synced data
                    updateLocalDataFromCloud(syncedData)
                }.onFailure { error ->
                    println("Auto-sync failed: ${error.message}")
                }
                
                // Get sync status
                val statusResult = cloudSync.getSyncStatus()
                statusResult.onSuccess { status ->
                    _syncStatus.value = status
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun updateLocalDataFromCloud(cloudData: AppData) {
        // Convert web data to Android format
        val (androidTasks, androidTaskLists, currentListId) = cloudSync.convertFromWebFormat(cloudData)
        
        // Clear existing data
        repository.clearAllTasks()
        repository.clearAllTaskLists()
        
        // Insert synced data
        if (androidTasks.isNotEmpty()) {
            repository.insertAllTasks(androidTasks)
        }
        if (androidTaskLists.isNotEmpty()) {
            repository.insertAllTaskLists(androidTaskLists)
        }
        
        // Update current list ID
        _currentListId.value = currentListId
        
        // Reload data - but don't populate _tasks with all tasks
        _taskLists.value = repository.getAllTaskLists().first()
        _dailyTasks.value = repository.getAllDailyTasks().first()
        
        // Set current list to first available list if none is set
        if (_taskLists.value.isNotEmpty() && _currentListId.value == null) {
            _currentListId.value = _taskLists.value.first().id
        }
        
        // Refresh tasks for the current list to show only incomplete tasks
        refreshTasksForCurrentList(_currentListId.value)
    }
    
    fun addTask(title: String, durationSeconds: Int, isDaily: Boolean = false, addToTop: Boolean = false) {
        viewModelScope.launch {
            // Ensure we have a valid current list ID for non-daily tasks
            val currentListId = if (isDaily) -1 else {
                _currentListId.value ?: _taskLists.value.firstOrNull()?.id ?: 1
            }
            
            val task = Task(
                title = title,
                durationSeconds = durationSeconds,
                isCompleted = false,
                order = if (isDaily) _dailyTasks.value.size else _tasks.value.size,
                listId = currentListId,
                isDaily = isDaily
            )
            repository.insertTask(task)
            
            if (isDaily) {
                refreshDailyTasks()
            } else {
                // Use the same list ID that was used to create the task
                refreshTasksForCurrentList(currentListId)
                
                // If addToTop is true, move the task to the top of the list
                if (addToTop) {
                    val currentTasks = repository.getIncompleteTasksByList(currentListId).first()
                    val newTask = currentTasks.last() // The task we just added will be at the end
                    insertTasksAtPosition(listOf(newTask), 0) // Move it to position 0 (top)
                }
            }
            uploadToCloud()
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            // Refresh tasks for the current list after updating
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            // Refresh tasks for the current list after deleting
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updatedTask)
            // Refresh tasks for the current list after toggling completion
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    
    fun addTaskList(name: String) {
        viewModelScope.launch {
            val taskList = TaskList(name = name)
            repository.insertTaskList(taskList)
            _taskLists.value = repository.getAllTaskLists().first()
            
            // If this is the first list, set it as current
            if (_taskLists.value.size == 1 && _currentListId.value == null) {
                _currentListId.value = taskList.id
                refreshTasksForCurrentList(taskList.id)
            }
            
            uploadToCloud()
        }
    }
    
    fun updateTaskList(taskList: TaskList) {
        viewModelScope.launch {
            repository.updateTaskList(taskList)
            _taskLists.value = repository.getAllTaskLists().first()
            uploadToCloud()
        }
    }
    
    fun deleteTaskList(taskList: TaskList) {
        viewModelScope.launch {
            repository.deleteTaskList(taskList)
            _taskLists.value = repository.getAllTaskLists().first()
            uploadToCloud()
        }
    }
    
    fun deleteTaskList(listId: Int) {
        viewModelScope.launch {
            val taskList = _taskLists.value.find { it.id == listId }
            taskList?.let { repository.deleteTaskList(it) }
            _taskLists.value = repository.getAllTaskLists().first()
            
            // If we deleted the current list, switch to the first available list
            if (_currentListId.value == listId) {
                _currentListId.value = _taskLists.value.firstOrNull()?.id
                if (_currentListId.value != null) {
                    refreshTasksForCurrentList(_currentListId.value)
                }
            }
            
            uploadToCloud()
        }
    }
    
    fun setCurrentList(listId: Int) {
        _currentListId.value = listId
        viewModelScope.launch {
            uploadToCloud()
        }
    }
    
    fun getTasksForList(listId: Int): List<Task> {
        return _tasks.value.filter { it.listId == listId }
    }
    
    fun getCurrentTasks(): List<Task> {
        return _currentListId.value?.let { listId ->
            _tasks.value.filter { it.listId == listId }
        } ?: _tasks.value
    }
    
    fun getCurrentList(): TaskList? {
        return _currentListId.value?.let { listId ->
            _taskLists.value.find { it.id == listId }
        }
    }
    
    private suspend fun uploadToCloud() {
        val appData = cloudSync.convertToWebFormat(
            dailyTasks = repository.getAllTasks(),
            taskLists = _taskLists.value,
            currentListId = _currentListId.value
        )
        cloudSync.uploadToCloud(appData)
    }
    
    fun manualSync() {
        performAutoSync()
    }
    
    // Export/Import functions for manual sync (keeping existing functionality)
    suspend fun exportData(): String {
        val appData = cloudSync.convertToWebFormat(
            dailyTasks = repository.getAllTasks(),
            taskLists = _taskLists.value,
            currentListId = _currentListId.value
        )
        return com.google.gson.Gson().toJson(appData)
    }
    
    fun importData(jsonData: String): Boolean {
        return try {
            val appData = com.google.gson.Gson().fromJson(jsonData, AppData::class.java)
            viewModelScope.launch {
                updateLocalDataFromCloud(appData)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun markTaskCompleted(task: Task) {
        viewModelScope.launch {
            repository.markTaskCompleted(task.id)
            // Refresh tasks for the current list after marking as completed
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    
    suspend fun markDailyTaskCompleted(task: Task) {
        repository.markDailyTaskCompleted(task.id)
        // Refresh daily tasks to get updated completion status
        refreshDailyTasks()
        uploadToCloud()
    }
    
    fun resetDailyTaskCompletion() {
        viewModelScope.launch {
            repository.resetDailyTaskCompletion()
            // Refresh daily tasks after resetting completion status
            refreshDailyTasks()
            uploadToCloud()
        }
    }
    
    private suspend fun refreshDailyTasks() {
        val dailyTaskList = repository.getAllDailyTasks().first()
        _dailyTasks.value = dailyTaskList
    }
    
    fun toggleDailyTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isDaily = !task.isDaily))
            // Refresh daily tasks after toggling daily status
            refreshDailyTasks()
            uploadToCloud()
        }
    }
    
    fun insertTasksAtPosition(tasksToInsert: List<Task>, targetPosition: Int) {
        viewModelScope.launch {
            val currentTasks = _tasks.value.toMutableList()
            val insertIndex = targetPosition.coerceIn(0, currentTasks.size)
            
            // Remove the tasks to insert from their current positions
            val taskIdsToInsert = tasksToInsert.map { it.id }
            currentTasks.removeAll { it.id in taskIdsToInsert }
            
            // Insert the tasks at the target position
            currentTasks.addAll(insertIndex, tasksToInsert)
            
            // Update all task orders to be sequential
            currentTasks.forEachIndexed { index, task ->
                repository.updateTaskOrder(task.id, index)
            }
            
            // Force refresh the tasks for the current list
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    
    fun insertDailyTasksAtPosition(tasksToInsert: List<Task>, targetPosition: Int) {
        viewModelScope.launch {
            val currentDailyTasks = _dailyTasks.value.toMutableList()
            val insertIndex = targetPosition.coerceIn(0, currentDailyTasks.size)
            
            // Remove the tasks to insert from their current positions
            val taskIdsToInsert = tasksToInsert.map { it.id }
            currentDailyTasks.removeAll { it.id in taskIdsToInsert }
            
            // Insert the tasks at the target position
            currentDailyTasks.addAll(insertIndex, tasksToInsert)
            
            // Update all task orders to be sequential
            currentDailyTasks.forEachIndexed { index, task ->
                repository.updateTaskOrder(task.id, index)
            }
            
            // Force refresh the daily tasks
            refreshDailyTasks()
            uploadToCloud()
        }
    }
    
    fun insertTasksToTargetList(tasksToInsert: List<Task>, targetListId: Int, targetPosition: Int) {
        viewModelScope.launch {
            // Get the current tasks from the target list
            val targetListTasks = repository.getIncompleteTasksByList(targetListId).first().toMutableList()
            val insertIndex = targetPosition.coerceIn(0, targetListTasks.size)
            
            // Create new tasks with new IDs and the target list ID
            val newTasks = tasksToInsert.map { originalTask ->
                originalTask.copy(
                    id = 0, // Let Room auto-generate new IDs
                    listId = targetListId,
                    order = 0 // Will be set properly below
                )
            }
            
            // Insert the new tasks into the database
            newTasks.forEach { task ->
                repository.insertTask(task)
            }
            
            // Get the updated list of tasks from the target list
            val updatedTargetListTasks = repository.getIncompleteTasksByList(targetListId).first().toMutableList()
            
            // Reorder all tasks to put the new tasks at the target position
            val tasksToReorder = mutableListOf<Task>()
            
            // Add tasks before the insert position
            tasksToReorder.addAll(updatedTargetListTasks.take(insertIndex))
            
            // Add the new tasks at the target position
            tasksToReorder.addAll(newTasks)
            
            // Add tasks after the insert position (excluding the new tasks we just added)
            val existingTasksAfterPosition = updatedTargetListTasks.drop(insertIndex).filter { existingTask ->
                !newTasks.any { newTask -> newTask.title == existingTask.title && newTask.durationSeconds == existingTask.durationSeconds }
            }
            tasksToReorder.addAll(existingTasksAfterPosition)
            
            // Update all task orders to be sequential
            tasksToReorder.forEachIndexed { index, task ->
                repository.updateTaskOrder(task.id, index)
            }
            
            // If the target list is the current list, refresh the current list's tasks
            if (targetListId == _currentListId.value) {
                refreshTasksForCurrentList(targetListId)
            }
            
            uploadToCloud()
        }
    }
    
    private var nextTaskReceiver: android.content.BroadcastReceiver? = null
    
    fun setTimerService(service: TimerService) {
        timerService = service
        
        // Stop any existing timer when the service is first connected
        // This prevents auto-start when the app launches
        stopTimer()
        
        viewModelScope.launch {
            service.currentTask.collect { task ->
                _currentTask.value = task
            }
        }
        viewModelScope.launch {
            service.timeRemaining.collect { time ->
                _timeRemaining.value = time
            }
        }
        viewModelScope.launch {
            service.isRunning.collect { running ->
                _isRunning.value = running
            }
        }
        viewModelScope.launch {
            service.isPaused.collect { paused ->
                _isPaused.value = paused
            }
        }
        viewModelScope.launch {
            service.showAlarm.collect { show ->
                _showAlarm.value = show
            }
        }
        viewModelScope.launch {
            service.isTransitioning.collect { transitioning ->
                val wasTransitioning = _isTransitioning.value
                _isTransitioning.value = transitioning
                // Only move to next task if we were actually transitioning and now we're not
                if (wasTransitioning && !transitioning) {
                    // When transition ends, move to next task
                    moveToNextTask()
                }
            }
        }
        viewModelScope.launch {
            service.transitionTime.collect { time ->
                _transitionTime.value = time
            }
        }
        
        // Register broadcast receiver for next task notifications from TimerService
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.dowithtime.NEXT_TASK")
            addAction("com.example.dowithtime.TRANSITION_FINISHED")
        }
        nextTaskReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                when (intent?.action) {
                    "com.example.dowithtime.NEXT_TASK" -> {
                        viewModelScope.launch {
                            // When timer expires, mark current task as completed and trigger transition
                            _currentTask.value?.let { currentTask ->
                                if (currentTask.isDaily) {
                                    // For daily tasks, mark as completed for today but don't remove from list
                                    markDailyTaskCompleted(currentTask)
                                } else {
                                    // For regular tasks, mark as completed and remove from list
                                    markTaskCompleted(currentTask)
                                    // Small delay to ensure the task list is updated
                                    kotlinx.coroutines.delay(100)
                                }
                                // Start the transition period
                                timerService?.let { service ->
                                    val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                                        action = TimerService.ACTION_NEXT_TASK
                                    }
                                    getApplication<Application>().startService(intent)
                                }
                                uploadToCloud()
                            }
                        }
                    }
                    "com.example.dowithtime.TRANSITION_FINISHED" -> {
                        // This is now handled by the isTransitioning flow collection
                        // No need to duplicate the handling here
                    }
                }
            }
        }
        getApplication<Application>().registerReceiver(nextTaskReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
    }
    
    fun startCurrentTask() {
        viewModelScope.launch {
            // First refresh the current task to ensure we have the first task
            refreshCurrentTask()
            
            _currentTask.value?.let { task ->
                // Immediately set the time remaining to the current task's duration
                _timeRemaining.value = task.durationSeconds * 1000L
                // Always reset the timer to the current task's duration
                timerService?.startTask(task)
            }
        }
    }
    
    fun pauseTimer() {
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_PAUSE
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun stopTimer() {
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun resetTimer() {
        _currentTask.value?.let { task ->
            // Immediately set the time remaining to the current task's duration
            _timeRemaining.value = task.durationSeconds * 1000L
            // Update the TimerService with the current task and its duration
            timerService?.updateTask(task)
        }
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_RESET
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun nextTask() {
        _currentTask.value?.let { currentTask ->
            viewModelScope.launch {
                if (currentTask.isDaily) {
                    // For daily tasks, mark as completed for today but don't remove from list
                    markDailyTaskCompleted(currentTask)
                } else {
                    // For regular tasks, mark as completed and remove from list
                    markTaskCompleted(currentTask)
                    // Small delay to ensure the task list is updated
                    kotlinx.coroutines.delay(100)
                }
                // Trigger the transition instead of directly moving to next task
                timerService?.let { service ->
                    val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                        action = TimerService.ACTION_NEXT_TASK
                    }
                    getApplication<Application>().startService(intent)
                }
                uploadToCloud()
            }
        }
    }
    
    fun skipTransition() {
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_SKIP_TRANSITION
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun stopAlarm() {
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_STOP_ALARM
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun completeCurrentTaskEarly() {
        _currentTask.value?.let { task ->
            viewModelScope.launch {
                if (task.isDaily) {
                    // For daily tasks, mark as completed for today but don't remove from list
                    markDailyTaskCompleted(task)
                } else {
                    // For regular tasks, mark as completed and remove from list
                    markTaskCompleted(task)
                    // Small delay to ensure the task list is updated
                    kotlinx.coroutines.delay(100)
                }
                moveToNextTask()
                uploadToCloud()
            }
        }
    }
    
    private suspend fun moveToNextTask() {
        val nextTask = getNextTask()
        if (nextTask != null) {
            _currentTask.value = nextTask
            _timeRemaining.value = nextTask.durationSeconds * 1000L
            // Always use startTask to ensure proper timer initialization
            timerService?.startTask(nextTask)
        } else {
            stopTimer()
        }
    }
    
    // Method to get the next task (for transition screen)
    suspend fun getNextTask(): Task? {
        // Check if we should be working with daily tasks based on the remembered state
        val shouldWorkWithDailyTasks = _wasInDailyList.value
        
        val nextTask = if (shouldWorkWithDailyTasks) {
            // For daily tasks, get the latest data directly from the database
            // and find the first uncompleted daily task
            val latestDailyTasks = repository.getAllDailyTasks().first()
            latestDailyTasks.firstOrNull { !it.completedToday }
        } else {
            // For regular tasks, find the first incomplete task from the current list
            // Force refresh the task list to ensure we have the latest data
            val currentListId = _currentListId.value ?: _taskLists.value.firstOrNull()?.id ?: 1
            val currentTasks = repository.getIncompleteTasksByList(currentListId).first()
            // Filter out daily tasks that are completed for today
            currentTasks.firstOrNull { task -> 
                !task.isDaily || !task.completedToday 
            }
        }
        
        // Update the StateFlow for UI consumption
        _nextTask.value = nextTask
        return nextTask
    }
    
    // Method to refresh the next task for UI
    fun refreshNextTaskForUI() {
        viewModelScope.launch {
            getNextTask()
        }
    }
    
    // Method to refresh current task from database
    suspend fun refreshCurrentTask() {
        val firstTask = getNextTask()
        
        _currentTask.value = firstTask
        // Set the time remaining to the current task's duration
        _currentTask.value?.let { task ->
            _timeRemaining.value = task.durationSeconds * 1000L
            // Update the TimerService with the current task and its duration
            timerService?.updateTask(task)
        }
    }
    
    fun selectList(listId: Int) {
        _currentListId.value = listId
        refreshTasksForCurrentList(listId)
    }
    
    fun setWasInDailyList(wasInDaily: Boolean) {
        _wasInDailyList.value = wasInDaily
    }
    
    fun pasteTasksFromListToPosition(sourceListId: Int?, isSourceDaily: Boolean, targetListId: Int, targetPosition: Int) {
        viewModelScope.launch {
            val tasksToPaste = if (isSourceDaily) {
                _dailyTasks.value
            } else {
                val sourceId = sourceListId ?: return@launch
                repository.getIncompleteTasksByList(sourceId).first()
            }
            
            if (tasksToPaste.isNotEmpty()) {
                val isTargetDaily = targetListId == -1 // -1 represents daily list
                if (isTargetDaily) {
                    insertDailyTasksAtPosition(tasksToPaste, targetPosition)
                } else {
                    // For regular lists, we need to handle the list assignment properly
                    val tasksWithNewList = tasksToPaste.map { it.copy(listId = targetListId) }
                    insertTasksToTargetList(tasksWithNewList, targetListId, targetPosition)
                }
                uploadToCloud()
            }
        }
    }
    fun renameList(listId: Int, newName: String) {
        viewModelScope.launch {
            val list = _taskLists.value.find { it.id == listId }
            if (list != null) {
                repository.updateTaskList(list.copy(name = newName))
                _taskLists.value = repository.getAllTaskLists().first()
                uploadToCloud()
            }
        }
    }
    fun refreshTasksForCurrentList(listId: Int? = null) {
        viewModelScope.launch {
            val currentListId = listId ?: _currentListId.value ?: _taskLists.value.firstOrNull()?.id ?: 1
            repository.getIncompleteTasksByList(currentListId).collect { taskList ->
                _tasks.value = taskList
                // Update the incomplete tasks state with the current list's tasks
                repository.updateIncompleteTasksState(taskList)
                if (_currentTask.value == null || !taskList.contains(_currentTask.value)) {
                    _currentTask.value = taskList.firstOrNull()
                }
            }
        }
    }
    fun refreshTaskLists() {
        viewModelScope.launch {
            repository.getAllTaskLists().collect { lists ->
                _taskLists.value = lists
            }
        }
    }
    
    fun updateTaskWithOrder(task: Task, newOrder: Int) {
        viewModelScope.launch {
            // First update the task with its new properties
            repository.updateTask(task)
            
            // If this is the current task, update it immediately
            if (_currentTask.value?.id == task.id) {
                _currentTask.value = task
                _timeRemaining.value = task.durationSeconds * 1000L
                // Update the TimerService with the refreshed task
                timerService?.updateTask(task)
            }
            
            // Then handle the order change if needed
            val currentTasks = if (task.isDaily) _dailyTasks.value.toMutableList() else _tasks.value.toMutableList()
            val currentIndex = currentTasks.indexOfFirst { it.id == task.id }
            
            if (currentIndex != -1) {
                // Remove the task from its current position
                currentTasks.removeAt(currentIndex)
                
                // Insert it at the new position
                val insertIndex = newOrder.coerceIn(0, currentTasks.size)
                currentTasks.add(insertIndex, task.copy(order = newOrder))
                
                // Update all task orders to be sequential
                currentTasks.forEachIndexed { index, t ->
                    repository.updateTaskOrder(t.id, index)
                }
            }
            
            // Refresh the appropriate task list
            if (task.isDaily) {
                refreshDailyTasks()
            } else {
                refreshTasksForCurrentList(_currentListId.value)
            }
            uploadToCloud()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Unregister broadcast receiver
        nextTaskReceiver?.let { receiver ->
            getApplication<Application>().unregisterReceiver(receiver)
            nextTaskReceiver = null
        }
    }
}
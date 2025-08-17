package com.example.dowithtime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dowithtime.data.AppDatabase
import com.example.dowithtime.data.Task
import com.example.dowithtime.data.TaskRepository
import com.example.dowithtime.data.TaskList
import com.example.dowithtime.data.DailySummary
import com.example.dowithtime.data.Subtask
import com.example.dowithtime.data.CompletedLog
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
import com.example.dowithtime.data.Preset
import com.example.dowithtime.data.PresetSubtask
import com.example.dowithtime.data.AppData
import com.example.dowithtime.data.SyncStatus

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val cloudSync: CloudSync
    private val prefs = application.getSharedPreferences("dowithtime_app_state", android.content.Context.MODE_PRIVATE)
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
    
    // Add callback for when all tasks are completed
    private var _onAllTasksCompleted: (() -> Unit)? = null
    private var _isInActiveSession = MutableStateFlow(false)
    
    fun setOnAllTasksCompletedCallback(callback: () -> Unit) {
        _onAllTasksCompleted = callback
    }
    
    val isInActiveSession: StateFlow<Boolean> = _isInActiveSession.asStateFlow()

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

    // CTDAD: today's total and history
    private val _todayTotalSeconds = MutableStateFlow(0)
    val todayTotalSeconds: StateFlow<Int> = _todayTotalSeconds.asStateFlow()

    private val _dailySummaries = MutableStateFlow<List<DailySummary>>(emptyList())
    val dailySummaries: StateFlow<List<DailySummary>> = _dailySummaries.asStateFlow()
    private val _completedCountToday = MutableStateFlow(0)
    val completedCountToday: StateFlow<Int> = _completedCountToday.asStateFlow()
    private val _completedLogsForDay = MutableStateFlow<List<CompletedLog>>(emptyList())
    val completedLogsForDay: StateFlow<List<CompletedLog>> = _completedLogsForDay.asStateFlow()

    // Presets
    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()
    private val _presetSubtasks = MutableStateFlow<Map<Int, List<PresetSubtask>>>(emptyMap())
    val presetSubtasks: StateFlow<Map<Int, List<PresetSubtask>>> = _presetSubtasks.asStateFlow()

    // Premade tasks are represented as regular lists with a special name prefix
    private val premadePrefix = "Premade: "

    // Subtasks cache for quick access
    private val _subtasksByTaskId = MutableStateFlow<Map<Int, List<Subtask>>>(emptyMap())
    val subtasksByTaskId: StateFlow<Map<Int, List<Subtask>>> = _subtasksByTaskId.asStateFlow()

    // Current active subtask when iterating tasks with subtasks
    private val _currentSubtask = MutableStateFlow<Subtask?>(null)
    val currentSubtask: StateFlow<Subtask?> = _currentSubtask.asStateFlow()
    private var currentStartedAtMs: Long? = null
    private var lastLoggedKey: String? = null
    private var lastLoggedAtMs: Long = 0L

    // Settings: show CTDAD bar and use actual time tracking
    private val _showCtdadBar = MutableStateFlow(true)
    val showCtdadBar: StateFlow<Boolean> = _showCtdadBar.asStateFlow()
    private val _useActualTimeForCtdad = MutableStateFlow(false)
    val useActualTimeForCtdad: StateFlow<Boolean> = _useActualTimeForCtdad.asStateFlow()
    private val _disableTimers = MutableStateFlow(false)
    val disableTimers: StateFlow<Boolean> = _disableTimers.asStateFlow()
    
    // Flag to prevent multiple creations of the default "Inbox" list
    private val _defaultInboxCreated = MutableStateFlow(false)
    // First-run notification prompt
    private val _showNotificationPrompt = MutableStateFlow(false)
    val showNotificationPrompt: StateFlow<Boolean> = _showNotificationPrompt.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        cloudSync = CloudSync(application)
        
        // Ensure default "Inbox" list exists and first-run setup
        viewModelScope.launch {
            val firstRunCompleted = prefs.getBoolean("first_run_completed", false)
            ensureDefaultInboxList()
            if (!firstRunCompleted) {
                _showNotificationPrompt.value = true
            }
        }
        // No dedicated list anymore; premade template lists will be named with a prefix
        viewModelScope.launch { _taskLists.value = repository.getAllTaskLists().first() }
        
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

        // Load presets
        viewModelScope.launch {
            repository.getAllPresets().collect { list ->
                _presets.value = list
            }
        }
        // Keep preset subtasks cache updated lazily per preset
        viewModelScope.launch {
            repository.getAllPresets().collect { list ->
                list.forEach { preset ->
                    launch {
                        repository.getPresetSubtasks(preset.id).collect { subs ->
                            _presetSubtasks.value = _presetSubtasks.value.toMutableMap().apply { put(preset.id, subs) }
                        }
                    }
                }
            }
        }

        // Initialize CTDAD tracking
        viewModelScope.launch {
            val today = getTodayDateString()
            repository.ensureDailySummary(today)
            repository.getDailySummaries().collect { summaries ->
                _dailySummaries.value = summaries
                val todayDate = getTodayDateString()
                val todaySummary = summaries.firstOrNull { it.date == todayDate }
                _todayTotalSeconds.value = todaySummary?.totalSeconds ?: 0
                _completedCountToday.value = repository.countCompletedOnDate(todayDate)
                repository.getCompletedLogsByDate(todayDate).collect { logs ->
                    _completedLogsForDay.value = logs
                }
            }
        }

        // Load settings
        _showCtdadBar.value = prefs.getBoolean("show_ctdad_bar", true)
        _useActualTimeForCtdad.value = prefs.getBoolean("use_actual_time_ctdad", false)
        _disableTimers.value = prefs.getBoolean("disable_timers", false)

        // Removed sample seeding for CTDAD history
    }
    
    private suspend fun ensureDefaultInboxList() {
        // Use a flag to prevent multiple creations during the same app session
        if (_defaultInboxCreated.value) return
        
        try {
            // First, check if there are multiple "Inbox" lists and clean them up
            val allLists = repository.getAllTaskLists().first()
            val inboxLists = allLists.filter { it.name == "Inbox" }
            
            if (inboxLists.size > 1) {
                // Keep only the first one, delete the rest
                for (i in 1 until inboxLists.size) {
                    repository.deleteTaskList(inboxLists[i])
                }
                // Refresh the lists
                _taskLists.value = repository.getAllTaskLists().first()
            }
            
            // Check if "Inbox" list exists (after cleanup)
            val existingInbox = repository.getTaskListByName("Inbox")
            if (existingInbox == null) {
                // Create the default "Inbox" list
                repository.insertTaskList(TaskList(name = "Inbox"))
                // Allow DB to assign ID, then fetch it back
                kotlinx.coroutines.delay(100)
                val created = repository.getTaskListByName("Inbox")
                _taskLists.value = repository.getAllTaskLists().first()
                _defaultInboxCreated.value = true
                if (_currentListId.value == null && created != null) {
                    _currentListId.value = created.id
                    // Persist selection for next launch
                    prefs.edit().putInt("last_selected_list_id", created.id).apply()
                }
            } else {
                // Inbox already exists, mark as created
                _defaultInboxCreated.value = true
                if (_currentListId.value == null) {
                    _currentListId.value = existingInbox.id
                    prefs.edit().putInt("last_selected_list_id", existingInbox.id).apply()
                }
            }
        } catch (e: Exception) {
            // If there's an error, still mark as created to prevent retries
            _defaultInboxCreated.value = true
            println("Error ensuring default inbox list: ${e.message}")
        }
    }

    fun completeFirstRunPrompt() {
        _showNotificationPrompt.value = false
        prefs.edit().putBoolean("first_run_completed", true).apply()
    }

    fun openNotificationSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getApplication<Application>().packageName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            // Fallback to app settings if notification settings action not supported
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:" + getApplication<Application>().packageName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Don't load all tasks initially - only load incomplete tasks for the current list
            _taskLists.value = repository.getAllTaskLists().first()
            
            // Restore the last selected list and daily list state from SharedPreferences
            val savedListId = prefs.getInt("last_selected_list_id", -1)
            val savedWasInDailyList = prefs.getBoolean("was_in_daily_list", false)
            
            // Only restore if we weren't in an active session (to avoid resuming tasks)
            val wasInActiveSession = prefs.getBoolean("was_in_active_session", false)
            if (!wasInActiveSession) {
                // Restore daily list state
                _wasInDailyList.value = savedWasInDailyList
                
                // Restore list selection if it's valid
                if (savedListId != -1 && _taskLists.value.any { it.id == savedListId }) {
                    _currentListId.value = savedListId
                } else if (_taskLists.value.isNotEmpty()) {
                    // Fallback to first available list
                    _currentListId.value = _taskLists.value.first().id
                }
            } else {
                // If we were in an active session, clear the session flag and use default list
                prefs.edit().putBoolean("was_in_active_session", false).apply()
                if (_taskLists.value.isNotEmpty() && _currentListId.value == null) {
                    _currentListId.value = _taskLists.value.first().id
                }
            }
            
            // Refresh tasks for the current list
            if (_currentListId.value != null) {
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
        
        // Ensure default "Inbox" list exists after cloud sync
        ensureDefaultInboxList()
        
        // Set current list to first available list if none is set
        if (_taskLists.value.isNotEmpty() && _currentListId.value == null) {
            _currentListId.value = _taskLists.value.first().id
        }
        
        // Refresh tasks for the current list to show only incomplete tasks
        refreshTasksForCurrentList(_currentListId.value)
    }

    // Helpers for CTDAD
    private fun getTodayDateString(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    private suspend fun addToTodayCtdad(seconds: Int) {
        if (seconds <= 0) return
        val today = getTodayDateString()
        repository.ensureDailySummary(today)
        repository.addToDailyTotal(today, seconds)
        // Update the exposed StateFlow so UI reflects changes immediately
        val todaySummary = repository.getSummaryByDate(today).first()
        _todayTotalSeconds.value = (todaySummary?.totalSeconds ?: 0)
    }
    private fun addCtdadForCurrentSegment(task: Task) {
        viewModelScope.launch {
            val usingActual = _useActualTimeForCtdad.value
            val now = System.currentTimeMillis()
            val started = currentStartedAtMs
            val segmentSeconds = when {
                usingActual && started != null -> ((now - started) / 1000L).toInt().coerceAtLeast(0)
                _currentSubtask.value != null -> _currentSubtask.value?.durationSeconds ?: 0
                else -> task.durationSeconds
            }
            val segmentId = "${task.id}:${_currentSubtask.value?.id ?: -1}"
            // De-dup guard: skip if we just logged this segment very recently
            if (segmentId == lastLoggedKey && (now - lastLoggedAtMs) < 1500) {
                currentStartedAtMs = null
                return@launch
            }
            lastLoggedKey = segmentId
            lastLoggedAtMs = now
            addToTodayCtdad(if (_disableTimers.value) 0 else segmentSeconds)
            // Log with appropriate title (subtask or task)
            val titleToLog = _currentSubtask.value?.title ?: task.title
            logCompletion(titleToLog, if (_disableTimers.value) 0 else segmentSeconds)
            currentStartedAtMs = null
        }
    }

    private fun computeCtdadSecondsFor(task: Task): Int {
        if (_disableTimers.value) return 0
        // When a task has subtasks, we account per subtask during iteration.
        // Avoid adding the full task duration again at task completion time.
        val hasSubtasks = runCatching { repository.getSubtasksForTask(task.id) }.getOrNull()?.let { flow ->
            // This is a Flow; we cannot collect here synchronously. Default to true only when currentSubtask exists.
            _currentSubtask.value != null
        } ?: false
        if (hasSubtasks) return 0
        return if (_useActualTimeForCtdad.value && _isInActiveSession.value) {
            val remainingSeconds = (_timeRemaining.value / 1000L).toInt()
            val spent = task.durationSeconds - remainingSeconds
            spent.coerceIn(0, task.durationSeconds)
        } else {
            task.durationSeconds
        }
    }

    fun setShowCtdadBar(show: Boolean) {
        _showCtdadBar.value = show
        prefs.edit().putBoolean("show_ctdad_bar", show).apply()
    }

    fun setUseActualTimeForCtdad(useActual: Boolean) {
        _useActualTimeForCtdad.value = useActual
        prefs.edit().putBoolean("use_actual_time_ctdad", useActual).apply()
    }

    fun setDisableTimers(disable: Boolean) {
        _disableTimers.value = disable
        prefs.edit().putBoolean("disable_timers", disable).apply()
    }

    fun getCompletedLogsByDay(date: String) = repository.getCompletedLogsByDate(date)
    suspend fun getCompletedCountForDate(date: String): Int = repository.countCompletedOnDate(date)
    
    fun addTask(title: String, durationSeconds: Int, isDaily: Boolean = false, addToTop: Boolean = false) {
        viewModelScope.launch {
            // Guard: when timers are enabled and no subtasks path, reject zero duration
            if (!_disableTimers.value && durationSeconds <= 0) {
                return@launch
            }
            // Ensure we have a valid current list ID for non-daily tasks
            val currentListId = if (isDaily) -1 else {
                _currentListId.value ?: _taskLists.value.firstOrNull()?.id ?: 1
            }
            
            val task = Task(
                title = title,
                durationSeconds = if (_disableTimers.value) 0 else durationSeconds,
                isCompleted = false,
                order = if (isDaily) _dailyTasks.value.size else _tasks.value.size,
                listId = currentListId,
                isDaily = isDaily
            )
            val newId = repository.insertTask(task)
            
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

    // Add task with optional subtasks (used by Add dialog)
    fun addTaskWithSubtasks(
        title: String,
        durationSeconds: Int,
        isDaily: Boolean,
        addToTop: Boolean,
        newSubtasks: List<Pair<String, Int>>
    ) {
        viewModelScope.launch {
            val currentListId = if (isDaily) -1 else {
                _currentListId.value ?: _taskLists.value.firstOrNull()?.id ?: 1
            }
            val useSubtasks = newSubtasks.isNotEmpty()
            if (!useSubtasks && !_disableTimers.value && durationSeconds <= 0) {
                return@launch
            }
            val effectiveDuration = if (_disableTimers.value || useSubtasks) 0 else durationSeconds
            val task = Task(
                title = title,
                durationSeconds = effectiveDuration,
                isCompleted = false,
                order = if (isDaily) _dailyTasks.value.size else _tasks.value.size,
                listId = currentListId,
                isDaily = isDaily
            )
            val newTaskId = repository.insertTask(task)
            if (useSubtasks) {
                newSubtasks.forEachIndexed { index, (stTitle, stSeconds) ->
                    val subtask = Subtask(
                        parentTaskId = newTaskId,
                        title = stTitle,
                        durationSeconds = if (_disableTimers.value) 0 else stSeconds,
                        isCompleted = false,
                        order = index
                    )
                    repository.insertSubtask(subtask)
                }
            }
            if (isDaily) {
                refreshDailyTasks()
            } else {
                refreshTasksForCurrentList(currentListId)
                if (addToTop) {
                    val currentTasks = repository.getIncompleteTasksByList(currentListId).first()
                    val newTask = currentTasks.last()
                    insertTasksAtPosition(listOf(newTask), 0)
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
        // Save the selected list to SharedPreferences
        prefs.edit().putInt("last_selected_list_id", listId).apply()
        refreshTasksForCurrentList(listId)
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
            val secondsToAdd = computeCtdadSecondsFor(task)
            addToTodayCtdad(secondsToAdd)
            logCompletion(task.title, secondsToAdd)
            // Refresh tasks for the current list after marking as completed
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    
    suspend fun markDailyTaskCompleted(task: Task) {
        repository.markDailyTaskCompleted(task.id)
        val secondsToAdd = computeCtdadSecondsFor(task)
        addToTodayCtdad(secondsToAdd)
        logCompletion(task.title, secondsToAdd)
        // Refresh daily tasks to get updated completion status
        refreshDailyTasks()
        uploadToCloud()
    }

    // Skip-CTDAD variants for internal flow when we already logged the segment
    private fun markTaskCompletedSkipCtdad(task: Task) {
        viewModelScope.launch {
            repository.markTaskCompleted(task.id)
            refreshTasksForCurrentList(_currentListId.value)
            uploadToCloud()
        }
    }
    private fun markDailyTaskCompletedSkipCtdad(task: Task) {
        viewModelScope.launch {
            repository.markDailyTaskCompleted(task.id)
            refreshDailyTasks()
            uploadToCloud()
        }
    }

    private suspend fun logCompletion(title: String, seconds: Int) {
        val today = getTodayDateString()
        repository.insertCompletedLog(CompletedLog(date = today, title = title, durationSeconds = seconds))
        _completedCountToday.value = repository.countCompletedOnDate(today)
    }

    // Subtasks API
    fun getSubtasksFlow(taskId: Int) = repository.getSubtasksForTask(taskId)
    suspend fun fetchTasksForListOnce(listId: Int): List<Task> {
        return repository.getIncompleteTasksByList(listId).first()
    }
    suspend fun fetchSubtasksForTaskOnce(taskId: Int): List<Subtask> {
        return repository.getSubtasksForTask(taskId).first()
    }
    fun addSubtask(parentTaskId: Int, title: String, durationSeconds: Int) {
        viewModelScope.launch {
            val current = repository.getSubtasksForTask(parentTaskId).first()
            val subtask = Subtask(parentTaskId = parentTaskId, title = title, durationSeconds = durationSeconds, order = current.size)
            repository.insertSubtask(subtask)
        }
    }
    fun updateSubtask(subtask: Subtask) {
        viewModelScope.launch { repository.updateSubtask(subtask) }
    }
    fun deleteSubtask(subtaskId: Int) {
        viewModelScope.launch { repository.deleteSubtask(subtaskId) }
    }
    fun reorderSubtasks(parentTaskId: Int, newOrder: List<Subtask>) {
        viewModelScope.launch {
            newOrder.forEachIndexed { index, st -> repository.updateSubtaskOrder(st.id, index) }
        }
    }

    // Presets API
    fun createPreset(name: String, subtasks: List<Pair<String, Int>>) {
        viewModelScope.launch {
            val presetId = repository.insertPreset(Preset(name = name))
            subtasks.forEachIndexed { index, (t, s) ->
                repository.insertPresetSubtask(PresetSubtask(presetId = presetId, title = t, durationSeconds = s, order = index))
            }
        }
    }
    fun deletePreset(presetId: Int) {
        viewModelScope.launch { repository.deletePreset(presetId) }
    }

    fun addPresetSubtask(presetId: Int, title: String, durationSeconds: Int) {
        viewModelScope.launch {
            val current = repository.getPresetSubtasks(presetId).first()
            repository.insertPresetSubtask(PresetSubtask(presetId = presetId, title = title, durationSeconds = durationSeconds, order = current.size))
        }
    }
    fun updatePresetSubtask(subtask: PresetSubtask) { viewModelScope.launch { repository.updatePresetSubtask(subtask) } }
    fun deletePresetSubtask(subtaskId: Int) { viewModelScope.launch { repository.deletePresetSubtask(subtaskId) } }
    fun reorderPresetSubtasks(presetId: Int, newOrder: List<PresetSubtask>) {
        viewModelScope.launch { newOrder.forEachIndexed { index, st -> repository.updatePresetSubtaskOrder(st.id, index) } }
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
            
            // Deep-copy with new IDs immediately and track them in order
            val insertedNewTasks = mutableListOf<Task>()
            tasksToInsert.forEach { originalTask ->
                val newTask = originalTask.copy(
                    id = 0,
                    listId = targetListId,
                    isDaily = false,
                    isCompleted = false,
                    completedToday = false,
                    order = 0
                )
                val newId = repository.insertTask(newTask)
                insertedNewTasks.add(newTask.copy(id = newId))

                // Clone subtasks for this task
                val subtasks = repository.getSubtasksForTask(originalTask.id).first()
                subtasks.forEachIndexed { index, st ->
                    repository.insertSubtask(
                        Subtask(
                            parentTaskId = newId,
                            title = st.title,
                            durationSeconds = if (_disableTimers.value) 0 else st.durationSeconds,
                            isCompleted = false,
                            order = index
                        )
                    )
                }
            }

            // Reorder: place the new block at requested index
            val updatedTargetListTasks = repository.getIncompleteTasksByList(targetListId).first().toMutableList()
            val byId = insertedNewTasks.map { it.id }.toSet()
            val existingWithoutInserted = updatedTargetListTasks.filter { it.id !in byId }.toMutableList()
            val safeIndex = insertIndex.coerceIn(0, existingWithoutInserted.size)
            existingWithoutInserted.addAll(safeIndex, insertedNewTasks)
            existingWithoutInserted.forEachIndexed { index, task -> repository.updateTaskOrder(task.id, index) }

            if (targetListId == _currentListId.value) {
                refreshTasksForCurrentList(targetListId)
            }

            uploadToCloud()
        }
    }
    
    private var nextTaskReceiver: android.content.BroadcastReceiver? = null
    // Lock iteration to the list that was active when the session started
    private var sessionListId: Int? = null
    
    fun setTimerService(service: TimerService) {
        timerService = service
        
        // Set up the callback for direct communication
        service.setViewModelCallback { 
            println("DEBUG: ViewModel callback invoked, calling nextTask()")
            nextTask() 
        }
        
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
            addAction("com.example.dowithtime.NEXT_TASK_DIRECT")
            addAction("com.example.dowithtime.TRANSITION_FINISHED")
        }
        println("DEBUG: Registering broadcast receiver with actions: ${filter.actionsIterator().asSequence().toList()}")
        nextTaskReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                println("DEBUG: Broadcast received: ${intent?.action}")
                when (intent?.action) {
                    "com.example.dowithtime.NEXT_TASK" -> {
                        // Delegate to unified nextTask() which handles subtasks and completion
                        nextTask()
                    }
                    "com.example.dowithtime.NEXT_TASK_DIRECT" -> {
                        println("DEBUG: NEXT_TASK_DIRECT broadcast received, calling nextTask() directly")
                        // Call the same method that the in-app button uses
                        nextTask()
                    }
                    "com.example.dowithtime.TRANSITION_FINISHED" -> {
                        // This is now handled by the isTransitioning flow collection
                        // No need to duplicate the handling here
                    }
                }
            }
        }
        getApplication<Application>().registerReceiver(nextTaskReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        println("DEBUG: Broadcast receiver registered successfully")
    }
    
    fun startCurrentTask() {
        viewModelScope.launch {
            // First refresh the current task to ensure we have the first task
            refreshCurrentTask()
            
            _currentTask.value?.let { task ->
                // Lock to the current list for the whole session to avoid unintended list switches
                sessionListId = _currentListId.value
                // Mark that we're in an active session
                _isInActiveSession.value = true
                // Save active session state to SharedPreferences
                prefs.edit().putBoolean("was_in_active_session", true).apply()
                // If task has subtasks, start with the first subtask's duration
                val subtasks = repository.getSubtasksForTask(task.id).first()
                val firstSubtask = subtasks.firstOrNull()
                if (firstSubtask != null) {
                    _currentSubtask.value = firstSubtask
                    _timeRemaining.value = firstSubtask.durationSeconds * 1000L
                    currentStartedAtMs = System.currentTimeMillis()
                    if (!_disableTimers.value) {
                        // Start timer using subtask duration and title context
                        timerService?.startTask(
                            task.copy(
                                title = firstSubtask.title,
                                durationSeconds = firstSubtask.durationSeconds
                            )
                        )
                    }
                } else {
                    // No subtasks, use the task's duration
                    _timeRemaining.value = task.durationSeconds * 1000L
                    currentStartedAtMs = System.currentTimeMillis()
                    if (!_disableTimers.value) {
                        timerService?.startTask(task)
                    }
                }
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
        // Reset session flag when manually stopping
        _isInActiveSession.value = false
        sessionListId = null
        // Clear active session state from SharedPreferences
        prefs.edit().putBoolean("was_in_active_session", false).apply()
    }
    
    fun resetTimer() {
        val task = _currentTask.value ?: return
        val subtask = _currentSubtask.value
        val durationSeconds = subtask?.durationSeconds ?: task.durationSeconds
        // Immediately set the time remaining to the current (sub)task's duration
        _timeRemaining.value = durationSeconds * 1000L
        if (!_disableTimers.value) {
            // Update the TimerService with the current (sub)task's duration
            val taskForTimer = if (subtask != null) {
                task.copy(title = subtask.title, durationSeconds = subtask.durationSeconds)
            } else task
            timerService?.updateTask(taskForTimer)
        }
        if (!_disableTimers.value) {
            timerService?.let { service ->
                val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                    action = TimerService.ACTION_RESET
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun nextTask() {
        _currentTask.value?.let { currentTask ->
            viewModelScope.launch {
                // Handle subtask progression first
                val subtasks = repository.getSubtasksForTask(currentTask.id).first()
                val currentSub = _currentSubtask.value
                if (subtasks.isNotEmpty()) {
                    val currentIndex = subtasks.indexOfFirst { it.id == currentSub?.id }
                    if (currentIndex != -1 && currentIndex < subtasks.size - 1) {
                        // Move to next subtask within the same task
                        val nextSubtask = subtasks[currentIndex + 1]
                        // Log time for the subtask we just finished
                        addCtdadForCurrentSegment(currentTask)
                        // Remove the completed subtask from the task
                        currentSub?.let { repository.deleteSubtask(it.id) }
                        _currentSubtask.value = nextSubtask
                        _timeRemaining.value = nextSubtask.durationSeconds * 1000L
                        currentStartedAtMs = System.currentTimeMillis()
                        if (_disableTimers.value) {
                            // In timerless mode, wait for user to press Next again
                        } else {
                            timerService?.startTask(
                                currentTask.copy(
                                    title = nextSubtask.title,
                                    durationSeconds = nextSubtask.durationSeconds
                                )
                            )
                        }
                        return@launch
                    } else if (currentIndex == subtasks.size - 1) {
                        // Finished last subtask, log (only if we actually had a current subtask) and clear
                        if (currentSub != null) {
                            addCtdadForCurrentSegment(currentTask)
                            // Remove the completed last subtask from the task
                            repository.deleteSubtask(currentSub.id)
                        }
                        _currentSubtask.value = null
                    } else if (currentIndex == -1) {
                        // No current subtask set (edge case), start from first
                        val firstSubtask = subtasks.first()
                        _currentSubtask.value = firstSubtask
                        _timeRemaining.value = firstSubtask.durationSeconds * 1000L
                        currentStartedAtMs = System.currentTimeMillis()
                        if (_disableTimers.value) {
                            // Wait for Next press
                        } else {
                            timerService?.startTask(
                                currentTask.copy(
                                    title = firstSubtask.title,
                                    durationSeconds = firstSubtask.durationSeconds
                                )
                            )
                        }
                        return@launch
                    }
                }
                val hadSubtasks = subtasks.isNotEmpty()
                if (currentTask.isDaily) {
                    // If there were no subtasks, log once here; otherwise, subtasks already logged
                    if (!hadSubtasks) addCtdadForCurrentSegment(currentTask)
                    markDailyTaskCompletedSkipCtdad(currentTask)
                } else {
                    // If there were no subtasks, log once here; otherwise, subtasks already logged
                    if (!hadSubtasks) addCtdadForCurrentSegment(currentTask)
                    markTaskCompletedSkipCtdad(currentTask)
                    // Small delay to ensure the task list is updated
                    kotlinx.coroutines.delay(100)
                }
                
                // Check if there's a next task before starting transition
                val nextTask = getNextTask()
                if (nextTask != null) {
                    // There's a next task, start the transition
                    if (!_disableTimers.value) {
                        timerService?.let { service ->
                            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                                action = TimerService.ACTION_NEXT_TASK
                            }
                            getApplication<Application>().startService(intent)
                        }
                    } else {
                        moveToNextTask()
                    }
                } else {
                    // No next task, immediately finish the session
                    stopTimer()
                    if (_isInActiveSession.value) {
                        _isInActiveSession.value = false
                        // Clear active session state from SharedPreferences
                        prefs.edit().putBoolean("was_in_active_session", false).apply()
                        _onAllTasksCompleted?.invoke()
                    }
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
        println("DEBUG: moveToNextTask called")
        val nextTask = getNextTask()
        println("DEBUG: Next task found: ${nextTask?.title}")
        
        if (nextTask != null) {
            println("DEBUG: Setting current task to: ${nextTask.title}")
            _currentTask.value = nextTask
            // If next task has subtasks, start with its first subtask
            val subtasks = repository.getSubtasksForTask(nextTask.id).first()
            val firstSubtask = subtasks.firstOrNull()
            if (firstSubtask != null) {
                _currentSubtask.value = firstSubtask
                _timeRemaining.value = firstSubtask.durationSeconds * 1000L
                currentStartedAtMs = System.currentTimeMillis()
                if (!_disableTimers.value) {
                    timerService?.startTask(
                        nextTask.copy(
                            title = firstSubtask.title,
                            durationSeconds = firstSubtask.durationSeconds
                        )
                    )
                }
            } else {
                _currentSubtask.value = null
                _timeRemaining.value = nextTask.durationSeconds * 1000L
                currentStartedAtMs = System.currentTimeMillis()
                if (!_disableTimers.value) {
                    // Use service only when timers are enabled
                    timerService?.startTask(nextTask)
                }
            }
        } else {
            println("DEBUG: No next task found, stopping timer")
            stopTimer()
            // Only notify if we're in an active session
            if (_isInActiveSession.value) {
                _isInActiveSession.value = false
                // Clear active session state from SharedPreferences
                prefs.edit().putBoolean("was_in_active_session", false).apply()
                _onAllTasksCompleted?.invoke()
            }
        }
    }
    
    // Method to get the next task (for transition screen)
    suspend fun getNextTask(): Task? {
        // Check if we should be working with daily tasks based on the remembered state
        val shouldWorkWithDailyTasks = _wasInDailyList.value

        val nextTask = if (shouldWorkWithDailyTasks) {
            // For daily tasks, get the latest data directly from the database
            val latestDailyTasks = repository.getAllDailyTasks().first()
            latestDailyTasks.firstOrNull { !it.completedToday }
        } else {
            // For regular tasks, always pull the latest tasks for the CURRENT LIST from DB
            val listId = sessionListId
                ?: _currentListId.value
                ?: _taskLists.value.firstOrNull()?.id
                ?: 1
            val latestListTasks = repository.getIncompleteTasksByList(listId).first()

            // Debug: Print the current tasks to see what's available for this list
            println("DEBUG: Current incomplete tasks for list $listId: ${latestListTasks.map { it.title }}")

            latestListTasks.firstOrNull()
        }
        
        // Debug: Print the selected next task
        println("DEBUG: Selected next task: ${nextTask?.title}")
        
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
        // Save the selected list to SharedPreferences
        prefs.edit().putInt("last_selected_list_id", listId).apply()
    }
    
    fun setWasInDailyList(wasInDaily: Boolean) {
        _wasInDailyList.value = wasInDaily
        // Save the daily list state to SharedPreferences
        prefs.edit().putBoolean("was_in_daily_list", wasInDaily).apply()
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
                // Deep-copy into a regular list (new IDs, clone subtasks)
                duplicateTasksIntoList(tasksToPaste, targetListId, targetPosition)
                uploadToCloud()
            }
        }
    }

    private suspend fun duplicateTasksIntoList(tasksToCopy: List<Task>, targetListId: Int, targetPosition: Int) {
        // Insert copies and capture new IDs
        val insertedNewTasks = mutableListOf<Task>()
        tasksToCopy.forEach { original ->
            val newTask = original.copy(
                id = 0,
                listId = targetListId,
                isDaily = false,
                isCompleted = false,
                completedToday = false,
                order = 0
            )
            val newId = repository.insertTask(newTask)
            insertedNewTasks.add(newTask.copy(id = newId))

            // Clone subtasks (if any)
            val subtasks = repository.getSubtasksForTask(original.id).first()
            subtasks.forEachIndexed { index, st ->
                repository.insertSubtask(
                    Subtask(
                        parentTaskId = newId,
                        title = st.title,
                        durationSeconds = if (_disableTimers.value) 0 else st.durationSeconds,
                        isCompleted = false,
                        order = index
                    )
                )
            }
        }

        // Reorder to place the block at the requested position
        val targetTasks = repository.getIncompleteTasksByList(targetListId).first().toMutableList()
        val safeIndex = targetPosition.coerceIn(0, targetTasks.size - insertedNewTasks.size).coerceAtLeast(0)
        // Pull out the inserted tasks by their new IDs, then reinsert at desired index
        val byId = insertedNewTasks.map { it.id }.toSet()
        val existingWithoutInserted = targetTasks.filter { it.id !in byId }.toMutableList()
        existingWithoutInserted.addAll(safeIndex, insertedNewTasks)
        existingWithoutInserted.forEachIndexed { idx, task -> repository.updateTaskOrder(task.id, idx) }

        // Always refresh the target list's tasks, but don't change the current list selection
        if (targetListId == _currentListId.value) {
            refreshTasksForCurrentList(targetListId)
        } else {
            // If pasting to a different list, just refresh that list's tasks without changing selection
            repository.getIncompleteTasksByList(targetListId).collect { taskList ->
                // Update the incomplete tasks state for the target list
                repository.updateIncompleteTasksState(taskList)
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
                println("DEBUG: refreshTasksForCurrentList - tasks: ${taskList.map { it.title }}")
                _tasks.value = taskList
                // Update the incomplete tasks state with the current list's tasks
                repository.updateIncompleteTasksState(taskList)
                // Only auto-update current task if it's null (initial load)
                // Don't auto-update when refreshing after task completion
                if (_currentTask.value == null) {
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
        // Reset the default inbox flag for next app session
        _defaultInboxCreated.value = false
        // Unregister broadcast receiver
        nextTaskReceiver?.let { receiver ->
            getApplication<Application>().unregisterReceiver(receiver)
            nextTaskReceiver = null
        }
    }
}
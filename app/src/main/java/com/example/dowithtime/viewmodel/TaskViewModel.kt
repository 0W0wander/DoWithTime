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

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
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
    private val _currentListId = MutableStateFlow(1)
    val currentListId: StateFlow<Int> = _currentListId.asStateFlow()
    
    private val _wasInDailyList = MutableStateFlow(false)
    val wasInDailyList: StateFlow<Boolean> = _wasInDailyList.asStateFlow()
    

    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        refreshTaskLists()
        refreshTasksForCurrentList()
        
        // Reset daily task completion at app start
        resetDailyTaskCompletion()
        
        viewModelScope.launch {
            repository.incompleteTasks.collect { taskList ->
                _tasks.value = taskList
                repository.updateIncompleteTasksState(taskList)
                // Update current task if it's null or if the current task is no longer in the list
                if (_currentTask.value == null || !taskList.contains(_currentTask.value)) {
                    _currentTask.value = taskList.firstOrNull()
                }
            }
        }
        
        viewModelScope.launch {
            repository.getAllDailyTasks().collect { dailyTaskList ->
                _dailyTasks.value = dailyTaskList
            }
        }
    }
    
    fun addTask(title: String, durationSeconds: Int, isDaily: Boolean = false) {
        viewModelScope.launch {
            val newOrder = _tasks.value.size
            val task = Task(
                title = title,
                durationSeconds = durationSeconds,
                order = newOrder,
                isDaily = isDaily
            )
            repository.insertTask(task)
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
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
            val currentTasks = _tasks.value.toMutableList()
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
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
    
    fun markTaskCompleted(task: Task) {
        viewModelScope.launch {
            repository.markTaskCompleted(task.id)
        }
    }
    
    fun markDailyTaskCompleted(task: Task) {
        viewModelScope.launch {
            repository.markDailyTaskCompleted(task.id)
            // Refresh daily tasks to get updated completion status
            refreshDailyTasks()
        }
    }
    
    fun resetDailyTaskCompletion() {
        viewModelScope.launch {
            repository.resetDailyTaskCompletion()
            // Refresh daily tasks after resetting completion status
            refreshDailyTasks()
        }
    }
    
    private fun refreshDailyTasks() {
        viewModelScope.launch {
            repository.getAllDailyTasks().collect { dailyTaskList ->
                _dailyTasks.value = dailyTaskList
            }
        }
    }
    
    fun toggleDailyTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isDaily = !task.isDaily))
            // Refresh daily tasks after toggling daily status
            refreshDailyTasks()
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
            refreshTasksForCurrentList()
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
        }
    }
    

    
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
                _isTransitioning.value = transitioning
                if (!transitioning) {
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
    }
    
    fun startCurrentTask() {
        // First refresh the current task to ensure we have the first task
        refreshCurrentTask()
        
        _currentTask.value?.let { task ->
            // Immediately set the time remaining to the current task's duration
            _timeRemaining.value = task.durationSeconds * 1000L
            // Always reset the timer to the current task's duration
            timerService?.startTask(task)
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
                }
                // Small delay to ensure the task list is updated
                kotlinx.coroutines.delay(100)
                // Then trigger the service to handle the transition
                timerService?.let { service ->
                    val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                        action = TimerService.ACTION_NEXT_TASK
                    }
                    getApplication<Application>().startService(intent)
                }
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
                }
                // Small delay to ensure the task list is updated
                kotlinx.coroutines.delay(100)
                moveToNextTask()
            }
        }
    }
    
    private fun moveToNextTask() {
        // Check if we're working with daily tasks
        val currentTask = _currentTask.value
        val isWorkingWithDailyTasks = currentTask?.isDaily == true
        
        val nextTask = if (isWorkingWithDailyTasks) {
            // For daily tasks, find the first uncompleted daily task
            _dailyTasks.value.firstOrNull { !it.completedToday }
        } else {
            // For regular tasks, find the first incomplete task
            _tasks.value.firstOrNull()
        }
        
        if (nextTask != null) {
            _currentTask.value = nextTask
            // Set the time remaining to the next task's duration
            _timeRemaining.value = nextTask.durationSeconds * 1000L
            // Update the TimerService with the new task and its duration
            timerService?.updateTask(nextTask)
            // Start the timer for the NEW task automatically
            timerService?.startTask(nextTask)
        } else {
            // No more tasks, stop the timer
            stopTimer()
        }
    }
    
    // Method to refresh current task from database
    fun refreshCurrentTask() {
        // Check if we should be working with daily tasks based on the remembered state
        val shouldWorkWithDailyTasks = _wasInDailyList.value
        
        val firstTask = if (shouldWorkWithDailyTasks) {
            // For daily tasks, find the first uncompleted daily task
            _dailyTasks.value.firstOrNull { !it.completedToday }
        } else {
            // For regular tasks, find the first incomplete task
            repository.incompleteTasksState.value.firstOrNull()
        }
        
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
        refreshTasksForCurrentList()
    }
    
    fun setWasInDailyList(wasInDaily: Boolean) {
        _wasInDailyList.value = wasInDaily
    }
    
    fun pasteTasksFromListToPosition(sourceListId: Int?, isSourceDaily: Boolean, targetListId: Int, targetPosition: Int) {
        viewModelScope.launch {
            val tasksToPaste = if (isSourceDaily) {
                _dailyTasks.value
            } else {
                repository.getIncompleteTasksByList(sourceListId!!).first()
            }
            
            if (tasksToPaste.isNotEmpty()) {
                val isTargetDaily = targetListId == -1 // -1 represents daily list
                if (isTargetDaily) {
                    insertDailyTasksAtPosition(tasksToPaste, targetPosition)
                } else {
                    // For regular lists, we need to handle the list assignment
                    val tasksWithNewList = tasksToPaste.map { it.copy(listId = targetListId) }
                    insertTasksAtPosition(tasksWithNewList, targetPosition)
                }
            }
        }
    }
    fun renameList(listId: Int, newName: String) {
        viewModelScope.launch {
            val list = _taskLists.value.find { it.id == listId }
            if (list != null) {
                repository.updateTaskList(list.copy(name = newName))
            }
        }
    }
    fun refreshTasksForCurrentList() {
        viewModelScope.launch {
            repository.getIncompleteTasksByList(_currentListId.value).collect { taskList ->
                _tasks.value = taskList
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
    fun addTaskList(name: String) {
        viewModelScope.launch {
            repository.insertTaskList(TaskList(name = name))
        }
    }
    fun updateTaskList(taskList: TaskList) {
        viewModelScope.launch {
            repository.updateTaskList(taskList)
        }
    }
    fun deleteTaskList(listId: Int) {
        viewModelScope.launch {
            taskLists.value.find { it.id == listId }?.let { repository.deleteTaskList(it) }
        }
    }
} 
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
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
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        refreshTaskLists()
        refreshTasksForCurrentList()
        
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
    
    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val taskList = _tasks.value.toMutableList()
            // Ensure indices are valid
            if (fromIndex < 0 || fromIndex >= taskList.size || 
                toIndex < 0 || toIndex >= taskList.size || fromIndex == toIndex) {
                return@launch
            }
            val movedTask = taskList.removeAt(fromIndex)
            taskList.add(toIndex, movedTask)
            // Update order for all tasks
            taskList.forEachIndexed { index, task ->
                repository.updateTaskOrder(task.id, index)
            }
            // Force refresh the tasks for the current list to get a new list reference
            refreshTasksForCurrentList()
        }
    }
    
    fun reorderTask(fromIndex: Int, toIndex: Int) {
        // Only reorder if indices are valid and different
        if (fromIndex != toIndex && fromIndex >= 0 && toIndex >= 0 && 
            fromIndex < _tasks.value.size && toIndex < _tasks.value.size) {
            reorderTasks(fromIndex, toIndex)
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
                // Mark the current task as completed
                markTaskCompleted(currentTask)
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
                markTaskCompleted(task)
                // Small delay to ensure the task list is updated
                kotlinx.coroutines.delay(100)
                moveToNextTask()
            }
        }
    }
    
    private fun moveToNextTask() {
        val incompleteTasks = _tasks.value
        
        // Always get the first incomplete task (topmost task) with latest data
        val nextTask = incompleteTasks.firstOrNull()
        
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
        val incompleteTasks = repository.incompleteTasksState.value
        // Always take the first task (topmost task) when starting tasks
        _currentTask.value = incompleteTasks.firstOrNull()
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
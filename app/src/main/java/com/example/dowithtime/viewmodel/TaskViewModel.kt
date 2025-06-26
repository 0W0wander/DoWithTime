package com.example.dowithtime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dowithtime.data.AppDatabase
import com.example.dowithtime.data.Task
import com.example.dowithtime.data.TaskRepository
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
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        
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
    
    fun addTask(title: String, durationSeconds: Int) {
        viewModelScope.launch {
            val newOrder = _tasks.value.size
            val task = Task(
                title = title,
                durationSeconds = durationSeconds,
                order = newOrder
            )
            repository.insertTask(task)
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
            val movedTask = taskList.removeAt(fromIndex)
            taskList.add(toIndex, movedTask)
            
            // Update order for all tasks
            taskList.forEachIndexed { index, task ->
                repository.updateTaskOrder(task.id, index)
            }
        }
    }
    
    fun setTimerService(service: TimerService) {
        timerService = service
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
        _currentTask.value?.let { task ->
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
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_RESET
            }
            getApplication<Application>().startService(intent)
        }
    }
    
    fun nextTask() {
        timerService?.let { service ->
            val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_NEXT_TASK
            }
            getApplication<Application>().startService(intent)
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
        
        // Get the first incomplete task (which will be the next one in order)
        val nextTask = incompleteTasks.firstOrNull()
        
        if (nextTask != null) {
            _currentTask.value = nextTask
            // Start the next task automatically with its correct duration
            startCurrentTask()
        } else {
            // No more tasks, stop the timer
            stopTimer()
        }
    }
    
    // Method to refresh current task from database
    fun refreshCurrentTask() {
        val incompleteTasks = repository.incompleteTasksState.value
        if (_currentTask.value == null || !incompleteTasks.contains(_currentTask.value)) {
            _currentTask.value = incompleteTasks.firstOrNull()
        }
    }
} 
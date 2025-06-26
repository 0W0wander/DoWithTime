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
    
    private var timerService: TimerService? = null
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        
        viewModelScope.launch {
            repository.incompleteTasks.collect { taskList ->
                _tasks.value = taskList
                if (_currentTask.value == null && taskList.isNotEmpty()) {
                    _currentTask.value = taskList.first()
                }
            }
        }
    }
    
    fun addTask(title: String, durationMinutes: Int) {
        viewModelScope.launch {
            val newOrder = _tasks.value.size
            val task = Task(
                title = title,
                durationMinutes = durationMinutes,
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
            // If this was the current task, move to next
            if (_currentTask.value?.id == task.id) {
                val nextTask = _tasks.value.find { it.id != task.id && !it.isCompleted }
                _currentTask.value = nextTask
            }
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
    }
    
    fun startCurrentTask() {
        _currentTask.value?.let { task ->
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
} 
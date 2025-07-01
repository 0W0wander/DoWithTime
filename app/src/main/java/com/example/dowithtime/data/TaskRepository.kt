package com.example.dowithtime.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val incompleteTasks: Flow<List<Task>> = taskDao.getIncompleteTasks()
    
    // StateFlow for synchronous access
    private val _incompleteTasksState = MutableStateFlow<List<Task>>(emptyList())
    val incompleteTasksState: StateFlow<List<Task>> = _incompleteTasksState
    
    fun getIncompleteTasksByList(listId: Int): Flow<List<Task>> = taskDao.getIncompleteTasksByList(listId)
    fun getAllDailyTasks(): Flow<List<Task>> = taskDao.getAllDailyTasks()

    // TaskList methods
    fun getAllTaskLists(): Flow<List<TaskList>> = taskDao.getAllTaskLists()
    suspend fun insertTaskList(taskList: TaskList) = taskDao.insertTaskList(taskList)
    suspend fun updateTaskList(taskList: TaskList) = taskDao.updateTaskList(taskList)
    suspend fun deleteTaskList(taskList: TaskList) = taskDao.deleteTaskList(taskList)
    
    init {
        // Update the state flow when incomplete tasks change
        // This will be handled by the ViewModel
    }
    
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }
    
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }
    
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    suspend fun updateTaskOrder(taskId: Int, newOrder: Int) {
        taskDao.updateTaskOrder(taskId, newOrder)
    }
    
    suspend fun markTaskCompleted(taskId: Int) {
        taskDao.markTaskCompleted(taskId)
    }
    
    suspend fun markDailyTaskCompleted(taskId: Int) {
        taskDao.markDailyTaskCompleted(taskId)
    }
    
    suspend fun resetDailyTaskCompletion() {
        taskDao.resetDailyTaskCompletion()
    }
    
    // Method to update the state flow
    fun updateIncompleteTasksState(tasks: List<Task>) {
        _incompleteTasksState.value = tasks
    }
    
    // Sync methods
    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }
    
    suspend fun deleteAllTaskLists() {
        taskDao.deleteAllTaskLists()
    }
} 
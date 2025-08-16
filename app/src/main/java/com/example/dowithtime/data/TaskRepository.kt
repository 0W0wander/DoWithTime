package com.example.dowithtime.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

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
    suspend fun getTaskListByName(name: String): TaskList? = taskDao.getTaskListByName(name)
    suspend fun insertTaskList(taskList: TaskList) = taskDao.insertTaskList(taskList)
    suspend fun updateTaskList(taskList: TaskList) = taskDao.updateTaskList(taskList)
    suspend fun deleteTaskList(taskList: TaskList) = taskDao.deleteTaskList(taskList)
    
    init {
        // Update the state flow when incomplete tasks change
        // This will be handled by the ViewModel
    }
    
    suspend fun insertTask(task: Task): Int {
        return taskDao.insertTask(task).toInt()
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
    
    // Additional methods needed by ViewModel
    suspend fun getAllTasks(): List<Task> {
        return taskDao.getAllTasks().first()
    }
    
    suspend fun clearAllTasks() {
        taskDao.deleteAllTasks()
    }
    
    suspend fun clearAllTaskLists() {
        taskDao.deleteAllTaskLists()
    }
    
    suspend fun insertAllTasks(tasks: List<Task>) {
        taskDao.insertAllTasks(tasks)
    }
    
    suspend fun insertAllTaskLists(taskLists: List<TaskList>) {
        taskDao.insertAllTaskLists(taskLists)
    }

    // Daily summaries (CTDAD)
    fun getDailySummaries(): Flow<List<DailySummary>> = taskDao.getDailySummaries()
    fun getSummaryByDate(date: String): Flow<DailySummary?> = taskDao.getSummaryByDate(date)
    suspend fun ensureDailySummary(date: String) = taskDao.ensureDailySummary(date)
    suspend fun addToDailyTotal(date: String, seconds: Int) = taskDao.addToDailyTotal(date, seconds)

    // Subtasks
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>> = taskDao.getSubtasksForTask(taskId)
    suspend fun insertSubtask(subtask: Subtask) = taskDao.insertSubtask(subtask)
    suspend fun updateSubtask(subtask: Subtask) = taskDao.updateSubtask(subtask)
    suspend fun updateSubtaskOrder(subtaskId: Int, newOrder: Int) = taskDao.updateSubtaskOrder(subtaskId, newOrder)
    suspend fun deleteSubtask(subtaskId: Int) = taskDao.deleteSubtask(subtaskId)
    suspend fun deleteSubtasksForTask(taskId: Int) = taskDao.deleteSubtasksForTask(taskId)

    // Completed logs
    suspend fun insertCompletedLog(log: CompletedLog) = taskDao.insertCompletedLog(log)
    suspend fun countCompletedOnDate(date: String): Int = taskDao.countCompletedOnDate(date)
    fun getCompletedLogsByDate(date: String): Flow<List<CompletedLog>> = taskDao.getCompletedLogsByDate(date)
} 
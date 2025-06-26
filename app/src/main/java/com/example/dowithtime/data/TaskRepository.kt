package com.example.dowithtime.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val incompleteTasks: Flow<List<Task>> = taskDao.getIncompleteTasks()
    
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
} 
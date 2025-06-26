package com.example.dowithtime.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY `order` ASC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY `order` ASC")
    fun getIncompleteTasks(): Flow<List<Task>>
    
    @Insert
    suspend fun insertTask(task: Task)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("UPDATE tasks SET `order` = :newOrder WHERE id = :taskId")
    suspend fun updateTaskOrder(taskId: Int, newOrder: Int)
    
    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    suspend fun markTaskCompleted(taskId: Int)
} 
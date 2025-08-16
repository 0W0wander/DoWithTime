package com.example.dowithtime.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY `order` ASC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY `order` ASC")
    fun getIncompleteTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE list_id = :listId AND is_completed = 0 ORDER BY `order` ASC")
    fun getIncompleteTasksByList(listId: Int): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE is_daily = 1 ORDER BY `order` ASC")
    fun getAllDailyTasks(): Flow<List<Task>>
    
    @Insert
    suspend fun insertTask(task: Task): Long
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("UPDATE tasks SET `order` = :newOrder WHERE id = :taskId")
    suspend fun updateTaskOrder(taskId: Int, newOrder: Int)
    
    @Query("UPDATE tasks SET is_completed = 1 WHERE id = :taskId")
    suspend fun markTaskCompleted(taskId: Int)
    
    @Query("UPDATE tasks SET completed_today = 1 WHERE id = :taskId")
    suspend fun markDailyTaskCompleted(taskId: Int)
    
    @Query("UPDATE tasks SET completed_today = 0 WHERE is_daily = 1")
    suspend fun resetDailyTaskCompletion()

    // Daily summaries (CTDAD)
    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getDailySummaries(): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries WHERE date = :date LIMIT 1")
    fun getSummaryByDate(date: String): Flow<DailySummary?>

    @Query("INSERT OR IGNORE INTO daily_summaries(date, total_seconds) VALUES(:date, 0)")
    suspend fun ensureDailySummary(date: String)

    @Query("UPDATE daily_summaries SET total_seconds = total_seconds + :seconds WHERE date = :date")
    suspend fun addToDailyTotal(date: String, seconds: Int)

    // Subtasks
    @Query("SELECT * FROM subtasks WHERE parent_task_id = :taskId ORDER BY `order` ASC")
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>>

    @Insert
    suspend fun insertSubtask(subtask: Subtask)

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Query("UPDATE subtasks SET `order` = :newOrder WHERE id = :subtaskId")
    suspend fun updateSubtaskOrder(subtaskId: Int, newOrder: Int)

    @Query("DELETE FROM subtasks WHERE id = :subtaskId")
    suspend fun deleteSubtask(subtaskId: Int)

    @Query("DELETE FROM subtasks WHERE parent_task_id = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Int)

    // Completed logs
    @Insert
    suspend fun insertCompletedLog(log: CompletedLog)

    @Query("SELECT COUNT(*) FROM completed_logs WHERE date = :date")
    suspend fun countCompletedOnDate(date: String): Int

    @Query("SELECT * FROM completed_logs WHERE date = :date ORDER BY id DESC")
    fun getCompletedLogsByDate(date: String): Flow<List<CompletedLog>>
    
    // TaskList methods
    @Query("SELECT * FROM task_lists")
    fun getAllTaskLists(): Flow<List<TaskList>>
    
    @Query("SELECT * FROM task_lists WHERE name = :name LIMIT 1")
    suspend fun getTaskListByName(name: String): TaskList?
    
    @Insert
    suspend fun insertTaskList(taskList: TaskList)
    
    @Update
    suspend fun updateTaskList(taskList: TaskList)
    
    @Delete
    suspend fun deleteTaskList(taskList: TaskList)
    
    // Sync methods
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    @Query("DELETE FROM task_lists")
    suspend fun deleteAllTaskLists()
    
    // Bulk insert methods for sync
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<Task>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTaskLists(taskLists: List<TaskList>)
} 
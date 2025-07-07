package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "order")
    val order: Int = 0,
    
    @ColumnInfo(name = "is_daily")
    val isDaily: Boolean = false,
    
    @ColumnInfo(name = "list_id")
    val listId: Int = 1,
    
    @ColumnInfo(name = "completed_today")
    val completedToday: Boolean = false
) 
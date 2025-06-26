package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val order: Int = 0
) 
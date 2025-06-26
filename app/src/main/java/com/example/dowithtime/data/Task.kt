package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val durationSeconds: Int,
    val isCompleted: Boolean = false,
    val order: Int = 0,
    val isDaily: Boolean = false
) 
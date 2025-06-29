package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
) 
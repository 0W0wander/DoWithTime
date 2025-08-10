package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "completed_logs")
data class CompletedLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "date")
    val date: String, // yyyy-MM-dd

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int
)



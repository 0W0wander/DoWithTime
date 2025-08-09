package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // yyyy-MM-dd

    @ColumnInfo(name = "total_seconds")
    val totalSeconds: Int = 0
)



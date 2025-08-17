package com.example.dowithtime.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "preset_subtasks")
data class PresetSubtask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val presetId: Int,
    val title: String,
    val durationSeconds: Int,
    val order: Int
)


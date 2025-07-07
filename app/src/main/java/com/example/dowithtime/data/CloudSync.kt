package com.example.dowithtime.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import java.util.*

class CloudSync(private val context: Context) {
    private val db: FirebaseFirestore = Firebase.firestore
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("dowithtime_sync", Context.MODE_PRIVATE)
    
    // Generate or get device ID
    private fun getDeviceId(): String {
        var deviceId = prefs.getString("dowithtime_device_id", null)
        if (deviceId == null) {
            deviceId = "android_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
            prefs.edit().putString("dowithtime_device_id", deviceId).apply()
        }
        return deviceId
    }
    
    // Upload data to cloud
    suspend fun uploadToCloud(appData: AppData): Result<Unit> {
        return try {
            val sharedDoc = db.collection("shared").document("tasks")
            val cloudData = mapOf(
                "data" to gson.toJson(appData),
                "lastUpdated" to com.google.firebase.Timestamp.now(),
                "version" to "1.0"
            )
            sharedDoc.set(cloudData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Download data from cloud
    suspend fun downloadFromCloud(): Result<AppData?> {
        return try {
            val sharedDoc = db.collection("shared").document("tasks")
            val docSnap = sharedDoc.get().await()
            
            if (docSnap.exists()) {
                val cloudData = docSnap.data
                val jsonData = cloudData?.get("data") as? String
                if (jsonData != null) {
                    val appData = gson.fromJson(jsonData, AppData::class.java)
                    Result.success(appData)
                } else {
                    Result.success(null)
                }
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get last update timestamp from cloud
    suspend fun getCloudLastUpdated(): Result<Date?> {
        return try {
            val sharedDoc = db.collection("shared").document("tasks")
            val docSnap = sharedDoc.get().await()
            
            if (docSnap.exists()) {
                val cloudData = docSnap.data
                val timestamp = cloudData?.get("lastUpdated") as? com.google.firebase.Timestamp
                Result.success(timestamp?.toDate())
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Auto-sync: compare timestamps and sync the newer data
    suspend fun autoSync(localData: AppData): Result<AppData> {
        return try {
            val cloudLastUpdated = getCloudLastUpdated().getOrNull()
            val localLastUpdated = prefs.getString("last_updated", "0")?.let { 
                Date(it.toLongOrNull() ?: 0) 
            } ?: Date(0)
            
            when {
                cloudLastUpdated == null -> {
                    // No cloud data, upload local data
                    uploadToCloud(localData)
                    prefs.edit().putString("last_updated", System.currentTimeMillis().toString()).apply()
                    Result.success(localData)
                }
                localLastUpdated > cloudLastUpdated -> {
                    // Local data is newer, upload to cloud
                    uploadToCloud(localData)
                    prefs.edit().putString("last_updated", System.currentTimeMillis().toString()).apply()
                    Result.success(localData)
                }
                else -> {
                    // Cloud data is newer, download from cloud
                    val cloudData = downloadFromCloud().getOrNull()
                    if (cloudData != null) {
                        prefs.edit().putString("last_updated", System.currentTimeMillis().toString()).apply()
                        Result.success(cloudData)
                    } else {
                        Result.success(localData)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get sync status
    suspend fun getSyncStatus(): Result<SyncStatus> {
        return try {
            val cloudLastUpdated = getCloudLastUpdated().getOrNull()
            val localLastUpdated = prefs.getString("last_updated", null)?.let { 
                Date(it.toLongOrNull() ?: 0) 
            }
            
            Result.success(
                SyncStatus(
                    isOnline = true,
                    lastSync = cloudLastUpdated ?: localLastUpdated,
                    deviceId = "shared"
                )
            )
        } catch (e: Exception) {
            Result.success(
                SyncStatus(
                    isOnline = false,
                    lastSync = null,
                    deviceId = "shared"
                )
            )
        }
    }
    
    // Manual sync
    suspend fun manualSync(localData: AppData): Result<AppData> {
        return autoSync(localData)
    }
    
    // Convert Android Task to WebTask
    private fun Task.toWebTask(): WebTask {
        return WebTask(
            id = this.id.toString(),
            title = this.title,
            durationSeconds = this.durationSeconds,
            completed = this.isCompleted,
            order = this.order
        )
    }
    
    // Convert WebTask to Android Task
    private fun WebTask.toAndroidTask(isDaily: Boolean = false, listId: Int = 1): Task {
        return Task(
            id = this.id.toIntOrNull() ?: 0,
            title = this.title,
            durationSeconds = this.durationSeconds,
            isCompleted = this.completed,
            order = this.order,
            isDaily = isDaily,
            listId = listId,
            completedToday = this.completed
        )
    }
    
    // Convert Android data to web-compatible format
    fun convertToWebFormat(
        dailyTasks: List<Task>,
        taskLists: List<TaskList>,
        currentListId: Int?
    ): AppData {
        // Separate daily tasks from list tasks
        val actualDailyTasks = dailyTasks.filter { it.isDaily }
        val listTasks = dailyTasks.filter { !it.isDaily }
        
        val webDailyTasks = actualDailyTasks.map { it.toWebTask() }
        
        val webTaskLists = taskLists.map { taskList ->
            // Get tasks for this specific list
            val tasksForThisList = listTasks.filter { it.listId == taskList.id }
            WebTaskList(
                id = taskList.id.toString(),
                name = taskList.name,
                tasks = tasksForThisList.map { it.toWebTask() }
            )
        }
        
        return AppData(
            dailyTasks = webDailyTasks,
            taskLists = webTaskLists,
            currentListId = currentListId?.toString(),
            isDarkMode = false
        )
    }
    
    // Convert web data to Android format
    fun convertFromWebFormat(appData: AppData): Triple<List<Task>, List<TaskList>, Int?> {
        // Convert daily tasks (these are stored with isDaily = true)
        val androidDailyTasks = appData.dailyTasks.map { it.toAndroidTask(isDaily = true) }
        
        // Convert task lists
        val androidTaskLists = appData.taskLists.map { webTaskList ->
            TaskList(
                id = webTaskList.id.toIntOrNull() ?: 0,
                name = webTaskList.name
            )
        }
        
        // Convert list tasks (these are stored with isDaily = false and listId)
        val listTasks = appData.taskLists.flatMap { webTaskList ->
            val listId = webTaskList.id.toIntOrNull() ?: 0
            webTaskList.tasks.map { webTask ->
                webTask.toAndroidTask(isDaily = false, listId = listId)
            }
        }
        
        // Combine all tasks
        val allTasks = androidDailyTasks + listTasks
        val currentListId = appData.currentListId?.toIntOrNull()
        
        return Triple(allTasks, androidTaskLists, currentListId)
    }
}

data class SyncStatus(
    val isOnline: Boolean,
    val lastSync: Date?,
    val deviceId: String
)

// Data class to match web app structure
data class AppData(
    val dailyTasks: List<WebTask> = emptyList(),
    val taskLists: List<WebTaskList> = emptyList(),
    val currentListId: String? = null,
    val isDarkMode: Boolean = false
)

// Web-compatible data classes
data class WebTask(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val completed: Boolean,
    val order: Int
)

data class WebTaskList(
    val id: String,
    val name: String,
    val tasks: List<WebTask>
) 
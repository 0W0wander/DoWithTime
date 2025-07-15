package com.example.dowithtime.service

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dowithtime.MainActivity
import com.example.dowithtime.R
import com.example.dowithtime.data.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {
    private val binder = TimerBinder()
    private var countDownTimer: CountDownTimer? = null
    private var transitionTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var viewModelCallback: (() -> Unit)? = null
    
    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask
    
    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused
    
    private val _showAlarm = MutableStateFlow(false)
    val showAlarm: StateFlow<Boolean> = _showAlarm
    
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning
    
    private val _transitionTime = MutableStateFlow(10)
    val transitionTime: StateFlow<Int> = _transitionTime
    
    companion object {
        const val CHANNEL_ID = "TimerChannel"
        const val ALARM_CHANNEL_ID = "AlarmChannel"
        const val NOTIFICATION_ID = 1
        const val ALARM_NOTIFICATION_ID = 2
        const val ACTION_START = "START"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
        const val ACTION_RESET = "RESET"
        const val ACTION_NEXT_TASK = "NEXT_TASK"
        const val ACTION_SKIP_TRANSITION = "SKIP_TRANSITION"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val ACTION_TRANSITION_FINISHED = "TRANSITION_FINISHED"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    fun setViewModelCallback(callback: () -> Unit) {
        viewModelCallback = callback
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
            ACTION_RESET -> resetTimer()
            ACTION_NEXT_TASK -> nextTask()
            ACTION_SKIP_TRANSITION -> skipTransition()
            ACTION_STOP_ALARM -> stopAlarm()
        }
        return START_NOT_STICKY
    }
    
    fun startTask(task: Task) {
        println("DEBUG: TimerService.startTask called with task: ${task.title}")
        // Stop any existing timer first
        countDownTimer?.cancel()
        transitionTimer?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        
        _currentTask.value = task
        // Always ensure we use the task's actual duration
        _timeRemaining.value = task.durationSeconds * 1000L
        _showAlarm.value = false
        _isTransitioning.value = false
        startTimer()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    fun updateTask(task: Task) {
        // Update the current task and time remaining without starting the timer
        _currentTask.value = task
        _timeRemaining.value = task.durationSeconds * 1000L
        updateNotification()
    }
    
    private fun startTimer() {
        // Cancel any existing timer first
        countDownTimer?.cancel()
        
        _isRunning.value = true
        _isPaused.value = false
        
        // Always use the current task's duration, never fall back to timeRemaining
        val durationToUse = _currentTask.value?.durationSeconds?.let { it * 1000L } ?: 0L
        
        if (durationToUse > 0) {
            countDownTimer = object : CountDownTimer(durationToUse, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    _timeRemaining.value = millisUntilFinished
                    updateNotification()
                }
                
                override fun onFinish() {
                    _timeRemaining.value = 0
                    _isRunning.value = false
                    showAlarmScreen()
                    updateNotification()
                }
            }.start()
        }
    }
    
    private fun pauseTimer() {
        if (!_isRunning.value) return
        
        countDownTimer?.cancel()
        _isRunning.value = false
        _isPaused.value = true
        updateNotification()
    }
    
    private fun stopTimer() {
        countDownTimer?.cancel()
        transitionTimer?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _timeRemaining.value = 0
        _currentTask.value = null
        _showAlarm.value = false
        _isTransitioning.value = false
        stopAlarm()
        
        // Cancel both notifications
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun resetTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _currentTask.value?.let { task ->
            _timeRemaining.value = task.durationSeconds * 1000L
        }
        updateNotification()
    }
    
    private fun showAlarmScreen() {
        _showAlarm.value = true
        playAlarm()
        showAlarmNotification()
    }
    
    private fun stopAlarm() {
        _showAlarm.value = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Cancel the alarm notification
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }
    
    private fun nextTask() {
        println("DEBUG: TimerService.nextTask called, showAlarm: ${_showAlarm.value}")
        val wasShowingAlarm = _showAlarm.value
        stopAlarm()
        
        // Reset the timer state to prepare for the next task
        _isRunning.value = false
        _isPaused.value = false
        
        // If we were showing an alarm, call the ViewModel's nextTask method directly
        // This bypasses the broadcast system and uses the same logic as the in-app button
        if (wasShowingAlarm) {
            // Call the ViewModel's nextTask method directly
            println("DEBUG: TimerService calling ViewModel nextTask directly")
            viewModelCallback?.invoke()
        } else {
            // For non-alarm cases, use the original broadcast system
            val intent = Intent("com.example.dowithtime.NEXT_TASK")
            println("DEBUG: TimerService sending NEXT_TASK broadcast")
            sendBroadcast(intent)
            
            if (_isTransitioning.value) {
                // If already transitioning, skip the transition entirely
                skipTransition()
            } else {
                startTransition()
            }
        }
    }
    
    private fun skipTransition() {
        transitionTimer?.cancel()
        _isTransitioning.value = false
        // Send broadcast to notify ViewModel to handle next task
        val intent = Intent("com.example.dowithtime.TRANSITION_FINISHED")
        sendBroadcast(intent)
        updateNotification()
    }
    
    private fun startTransition() {
        _isTransitioning.value = true
        _transitionTime.value = 10
        
        transitionTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _transitionTime.value = (millisUntilFinished / 1000).toInt()
                updateNotification()
            }
            
            override fun onFinish() {
                _isTransitioning.value = false
                // Send broadcast to notify ViewModel to handle next task
                val intent = Intent("com.example.dowithtime.TRANSITION_FINISHED")
                sendBroadcast(intent)
                updateNotification()
            }
        }.start()
    }
    
    private fun playAlarm() {
        // Play a short alarm sound instead of continuous loop
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer?.setOnCompletionListener {
            // Don't loop, just play once
        }
        mediaPlayer?.start()
    }
    
    private fun createNotificationChannel() {
        // Timer channel (low priority for ongoing timer)
        val timerChannel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Timer notifications"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        
        // Alarm channel (high priority for timer completion)
        val alarmChannel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Timer Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer completion notifications"
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
            setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
            setBypassDnd(true) // Show even in Do Not Disturb mode
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(alarmChannel)
    }
    
    private fun createNotification(): Notification {
        val task = _currentTask.value
        val timeRemaining = _timeRemaining.value
        
        val minutes = (timeRemaining / 1000) / 60
        val seconds = (timeRemaining / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        val title = when {
            _showAlarm.value -> "Time's Up!"
            _isTransitioning.value -> "Next task in ${_transitionTime.value}s"
            else -> task?.title ?: "No task"
        }
        
        val text = when {
            _showAlarm.value -> "Task time has expired"
            _isTransitioning.value -> "Preparing next task..."
            _isRunning.value -> "Time remaining: $timeText"
            else -> "Timer paused"
        }
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val startPauseIntent = Intent(this, TimerService::class.java).apply {
            action = if (_isRunning.value) ACTION_PAUSE else ACTION_START
        }
        val startPausePendingIntent = PendingIntent.getService(
            this, 1, startPauseIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val resetIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_RESET
        }
        val resetPendingIntent = PendingIntent.getService(
            this, 3, resetIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextTaskIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_NEXT_TASK
        }
        val nextTaskPendingIntent = PendingIntent.getService(
            this, 4, nextTaskIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val skipTransitionIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_SKIP_TRANSITION
        }
        val skipTransitionPendingIntent = PendingIntent.getService(
            this, 6, skipTransitionIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopAlarmIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopAlarmPendingIntent = PendingIntent.getService(
            this, 5, stopAlarmIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
        
        // Add progress bar for running tasks
        if (_isRunning.value && task != null && !_showAlarm.value && !_isTransitioning.value) {
            val totalTime = task.durationSeconds * 1000L
            val elapsed = totalTime - timeRemaining
            val progress = ((elapsed * 100) / totalTime).toInt()
            
            builder.setProgress(100, progress, false)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nProgress: $progress%"))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        
        when {
            _showAlarm.value -> {
                builder.addAction(R.drawable.ic_alarm_off, "Stop Alarm", stopAlarmPendingIntent)
                    .addAction(R.drawable.ic_next, "Next Task", nextTaskPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
            }
            _isTransitioning.value -> {
                builder.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
                    .addAction(R.drawable.ic_skip, "Skip", skipTransitionPendingIntent)
            }
            else -> {
                // Normal state - prioritize the most important buttons
                builder.addAction(
                    if (_isRunning.value) R.drawable.ic_pause else R.drawable.ic_play,
                    if (_isRunning.value) "Pause" else "Start",
                    startPausePendingIntent
                )
                    .addAction(R.drawable.ic_next, "Next Task", nextTaskPendingIntent)
                    .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            }
        }
        
        return builder.build()
    }
    
    private fun showAlarmNotification() {
        val task = _currentTask.value
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopAlarmIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopAlarmPendingIntent = PendingIntent.getService(
            this, 5, stopAlarmIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextTaskIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_NEXT_TASK
        }
        val nextTaskPendingIntent = PendingIntent.getService(
            this, 4, nextTaskIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmNotification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setContentTitle("⏰ Time's Up!")
            .setContentText("${task?.title ?: "Task"} time has expired")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setShowWhen(true)
            .setOnlyAlertOnce(false)
            .addAction(R.drawable.ic_alarm_off, "Stop Alarm", stopAlarmPendingIntent)
            .addAction(R.drawable.ic_next, "Next Task", nextTaskPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${task?.title ?: "Task"} time has expired. Tap to open the app or use the buttons below."))
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(ALARM_NOTIFICATION_ID, alarmNotification)
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        transitionTimer?.cancel()
        stopAlarm()
        
        // Cancel all notifications when service is destroyed
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
    }
} 
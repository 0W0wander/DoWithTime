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
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
        const val ACTION_RESET = "RESET"
        const val ACTION_NEXT_TASK = "NEXT_TASK"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
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
            ACTION_STOP_ALARM -> stopAlarm()
        }
        return START_NOT_STICKY
    }
    
    fun startTask(task: Task) {
        _currentTask.value = task
        _timeRemaining.value = task.durationSeconds * 1000L
        _showAlarm.value = false
        _isTransitioning.value = false
        startTimer()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    private fun startTimer() {
        if (_isRunning.value) return
        
        _isRunning.value = true
        _isPaused.value = false
        
        countDownTimer = object : CountDownTimer(_timeRemaining.value, 1000) {
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
    }
    
    private fun stopAlarm() {
        _showAlarm.value = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    private fun nextTask() {
        stopAlarm()
        startTransition()
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
                // The ViewModel will handle moving to the next task
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Timer notifications"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
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
        
        when {
            _showAlarm.value -> {
                builder.addAction(R.drawable.ic_launcher_foreground, "Stop Alarm", stopAlarmPendingIntent)
                    .addAction(R.drawable.ic_launcher_foreground, "Next Task", nextTaskPendingIntent)
            }
            _isTransitioning.value -> {
                builder.addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            }
            else -> {
                builder.addAction(
                    R.drawable.ic_launcher_foreground,
                    if (_isRunning.value) "Pause" else "Start",
                    startPausePendingIntent
                )
                    .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
                    .addAction(R.drawable.ic_launcher_foreground, "Reset", resetPendingIntent)
            }
        }
        
        return builder.build()
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
    }
} 
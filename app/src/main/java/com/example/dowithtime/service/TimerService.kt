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
    private var mediaPlayer: MediaPlayer? = null
    
    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask
    
    private val _timeRemaining = MutableStateFlow(0L)
    val timeRemaining: StateFlow<Long> = _timeRemaining
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    
    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused
    
    companion object {
        const val CHANNEL_ID = "TimerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
        const val ACTION_RESET = "RESET"
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
        }
        return START_NOT_STICKY
    }
    
    fun startTask(task: Task) {
        _currentTask.value = task
        _timeRemaining.value = task.durationMinutes * 60 * 1000L
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
                playAlarm()
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
        _isRunning.value = false
        _isPaused.value = false
        _timeRemaining.value = 0
        _currentTask.value = null
        stopAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun resetTimer() {
        countDownTimer?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _currentTask.value?.let { task ->
            _timeRemaining.value = task.durationMinutes * 60 * 1000L
        }
        updateNotification()
    }
    
    private fun playAlarm() {
        // For now, using system default alarm sound
        // Later you can customize this to play custom sounds
        mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }
    
    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
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
        
        val title = task?.title ?: "No task"
        val text = if (_isRunning.value) "Time remaining: $timeText" else "Timer paused"
        
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
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                if (_isRunning.value) "Pause" else "Start",
                startPausePendingIntent
            )
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Reset", resetPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopAlarm()
    }
} 
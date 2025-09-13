package com.example.dowithtime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.dowithtime.service.TimerService
import com.example.dowithtime.ui.screens.DoScreen
import com.example.dowithtime.ui.screens.TodoListScreen
import com.example.dowithtime.ui.theme.DoWithTimeTheme
import com.example.dowithtime.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()
    private var timerService: TimerService? = null
    private var bound = false
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            bound = true
            viewModel.setTimerService(timerService!!)
        }
        
        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Global crash logger: write to Downloads/DoWithTime_crash.log
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloads != null) {
                    val logTxt = java.io.File(downloads, "log.txt")
                    val sw = java.io.StringWriter()
                    val pw = java.io.PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    pw.flush()
                    val content = "==== Crash at ${java.util.Date()} on thread ${thread.name} ====${System.lineSeparator()}" +
                        sw.toString() + System.lineSeparator()
                    logTxt.appendText(content)
                    // Clean up legacy separate crash log if present
                    try {
                        val oldLog = java.io.File(downloads, "DoWithTime_crash.log")
                        if (oldLog.exists()) oldLog.delete()
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) {
            } finally {
                // Re-throw to let the system handle the crash normally
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
        
        // Bind to TimerService
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        
        setContent {
            DoWithTimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DoWithTimeApp(viewModel)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}

@Composable
fun DoWithTimeApp(viewModel: TaskViewModel) {
    var currentScreen by remember { mutableStateOf("todo") }
    
    when (currentScreen) {
        "todo" -> TodoListScreen(
            viewModel = viewModel,
            onNavigateToDo = { currentScreen = "do" }
        )
        "do" -> DoScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = "todo" }
        )
    }
}
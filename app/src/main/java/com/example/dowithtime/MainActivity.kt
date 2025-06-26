package com.example.dowithtime

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
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
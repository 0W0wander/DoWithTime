package com.example.dowithtime.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dowithtime.R
import com.example.dowithtime.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun DoScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val currentTask by viewModel.currentTask.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val showAlarm by viewModel.showAlarm.collectAsState()
    val isTransitioning by viewModel.isTransitioning.collectAsState()
    val transitionTime by viewModel.transitionTime.collectAsState()
    
    // Double-click detection for skipping transition
    var lastClickTime by remember { mutableStateOf(0L) }
    val doubleClickThreshold = 300L // milliseconds
    
    // Stop timer when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            // Stop the timer when leaving the DoScreen
            viewModel.stopTimer()
        }
    }
    
    // Refresh current task when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshCurrentTask()
    }
    
    // Show alarm screen when time runs out
    if (showAlarm) {
        AlarmScreen(
            task = currentTask,
            onStopAlarm = { viewModel.stopAlarm() },
            onNextTask = { viewModel.nextTask() }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Do",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Current task info
        currentTask?.let { task ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTransitioning) "Next Task" else "Current Task",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDuration(task.durationSeconds),
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        } ?: run {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No tasks available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add some tasks first!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Timer display
        if (currentTask != null) {
            if (isTransitioning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    viewModel.skipTransition()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next task in",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${transitionTime}s",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Preparing next task...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Double-tap to skip",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                return
            } else {
                // Show normal timer
                val minutes = (timeRemaining / 1000) / 60
                val seconds = (timeRemaining / 1000) % 60
                val timeText = String.format("%02d:%02d", minutes, seconds)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeText,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Modern play/pause button (larger)
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(brush = com.example.dowithtime.ui.theme.TimerGradient)
                        .clickable {
                            if (isRunning) viewModel.pauseTimer() else viewModel.startCurrentTask()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isRunning) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = if (isRunning) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Modern Next Task button
                Button(
                    onClick = { viewModel.nextTask() },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(54.dp)
                        .background(brush = com.example.dowithtime.ui.theme.PrimaryGradient, shape = CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Next Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Reset button
                OutlinedButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder,
                    shape = CircleShape
                ) {
                    Text(
                        text = "Reset",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        // Back to Tasks button (more visible)
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = ButtonDefaults.outlinedButtonBorder,
            shape = CircleShape
        ) {
            Text(
                text = "Back to Tasks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDuration(durationSeconds: Int): String {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0s"
    }
} 
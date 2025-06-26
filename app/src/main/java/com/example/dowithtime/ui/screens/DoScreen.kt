package com.example.dowithtime.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dowithtime.R
import com.example.dowithtime.viewmodel.TaskViewModel

@Composable
fun DoScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val currentTask by viewModel.currentTask.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Do",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Current task info
        currentTask?.let { task ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Task",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = task.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${task.durationMinutes} minutes",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } ?: run {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
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
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Timer display
        if (currentTask != null) {
            val minutes = (timeRemaining / 1000) / 60
            val seconds = (timeRemaining / 1000) % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)
            
            Text(
                text = timeText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isRunning) "Time remaining" 
                       else if (isPaused) "Paused" 
                       else "Ready to start",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Big play/pause button
        if (currentTask != null) {
            Button(
                onClick = {
                    if (isRunning) {
                        viewModel.pauseTimer()
                    } else {
                        viewModel.startCurrentTask()
                    }
                },
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary
                )
            ) {
                Image(
                    painter = painterResource(
                        if (isRunning) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Control buttons
        if (currentTask != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetTimer() }
                ) {
                    Text("Reset")
                }
                
                OutlinedButton(
                    onClick = { viewModel.stopTimer() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_stop),
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Back button
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Tasks")
        }
    }
} 
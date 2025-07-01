import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  IconButton,
  Typography,
  AppBar,
  Toolbar,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  PlayArrow as PlayArrowIcon,
  Pause as PauseIcon,
  Stop as StopIcon,
  Refresh as RefreshIcon,
  SkipNext as SkipNextIcon
} from '@mui/icons-material';
import { useTimer } from '../hooks/useTimer';
import { useTaskManager } from '../hooks/useTaskManager';
import { formatDuration, formatTimeRemaining } from '../utils/timer';

interface DoScreenProps {
  onNavigateBack: () => void;
}

export const DoScreen: React.FC<DoScreenProps> = ({ onNavigateBack }) => {
  const {
    timerState,
    startTimer,
    pauseTimer,
    resumeTimer,
    stopTimer,
    resetTimer,
    skipTransition,
    nextTask,
    stopAlarm
  } = useTimer();

  const { getCurrentTasks, toggleTaskCompletion } = useTaskManager();
  const [currentTaskIndex, setCurrentTaskIndex] = useState(0);
  const [showAlarmDialog, setShowAlarmDialog] = useState(false);

  const currentTasks = getCurrentTasks().filter(task => !task.isCompleted);
  
  // Handle transition state
  useEffect(() => {
    if (timerState.isTransitioning && timerState.transitionTime > 0) {
      const interval = setInterval(() => {
        if (timerState.transitionTime <= 1) {
          // Transition finished, move to next task
          if (currentTaskIndex < currentTasks.length - 1) {
            setCurrentTaskIndex(currentTaskIndex + 1);
            const nextTask = currentTasks[currentTaskIndex + 1];
            if (nextTask) {
              startTimer(nextTask);
            }
          } else {
            // All tasks completed
            onNavigateBack();
          }
        }
      }, 1000);
      
      return () => clearInterval(interval);
    }
  }, [timerState.isTransitioning, timerState.transitionTime, currentTaskIndex, currentTasks, startTimer, onNavigateBack]);

  useEffect(() => {
    if (timerState.showAlarm) {
      setShowAlarmDialog(true);
    }
  }, [timerState.showAlarm]);

  const handleStartCurrentTask = () => {
    if (currentTasks[currentTaskIndex]) {
      startTimer(currentTasks[currentTaskIndex]);
    }
  };

  const handleNextTask = () => {
    setShowAlarmDialog(false);
    stopAlarm();
    
    // Complete the current task
    if (currentTask) {
      if (currentTask.isDaily) {
        // For daily tasks, mark as completed for today but don't remove from list
        toggleTaskCompletion(currentTask.id);
      } else {
        // For regular tasks, mark as completed and remove from list
        toggleTaskCompletion(currentTask.id);
      }
    }
    
    // Move to next task
    if (currentTaskIndex < currentTasks.length - 1) {
      setCurrentTaskIndex(currentTaskIndex + 1);
      // Start the next task automatically
      const nextTask = currentTasks[currentTaskIndex + 1];
      if (nextTask) {
        startTimer(nextTask);
      }
    } else {
      // All tasks completed
      onNavigateBack();
    }
  };

  const handleStopAlarm = () => {
    setShowAlarmDialog(false);
    stopAlarm();
  };

  const handleBack = () => {
    stopTimer();
    onNavigateBack();
  };

  const currentTask = currentTasks[currentTaskIndex];

  if (!currentTask) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
        <AppBar position="static">
          <Toolbar>
            <IconButton edge="start" color="inherit" onClick={handleBack}>
              <ArrowBackIcon />
            </IconButton>
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Do
            </Typography>
          </Toolbar>
        </AppBar>
        
        <Box sx={{ 
          display: 'flex', 
          flexDirection: 'column', 
          alignItems: 'center', 
          justifyContent: 'center', 
          flexGrow: 1,
          p: 3
        }}>
          <Typography variant="h5" sx={{ mb: 2 }}>
            No tasks available
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Add some tasks first!
          </Typography>
          <Button variant="contained" onClick={handleBack}>
            Go Back
          </Button>
        </Box>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton edge="start" color="inherit" onClick={handleBack}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Do
          </Typography>
        </Toolbar>
      </AppBar>

      <Box sx={{ 
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        justifyContent: 'center', 
        flexGrow: 1,
        p: 3
      }}>
        {/* Transition Screen */}
        {timerState.isTransitioning && (
          <Box sx={{ 
            display: 'flex', 
            flexDirection: 'column', 
            alignItems: 'center', 
            justifyContent: 'center',
            textAlign: 'center'
          }}>
            <Typography variant="h4" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
              Next task in
            </Typography>
            <Typography variant="h1" sx={{ mb: 2, fontWeight: 'bold', color: 'primary.main' }}>
              {timerState.transitionTime}s
            </Typography>
            <Typography variant="h6" sx={{ mb: 2, color: 'text.secondary' }}>
              Preparing next task...
            </Typography>
            <Button 
              variant="outlined" 
              onClick={skipTransition}
              sx={{ mt: 2 }}
            >
              Skip Transition
            </Button>
          </Box>
        )}
        
                {/* Current Task Info */}
        {!timerState.isTransitioning && (
          <>
            <Box sx={{ textAlign: 'center', mb: 4 }}>
              <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
                Current Task
              </Typography>
              <Typography variant="h4" sx={{ mb: 1, fontWeight: 'bold', color: 'primary.main' }}>
                {currentTask.title}
              </Typography>
              <Typography variant="body1" color="text.secondary">
                {formatDuration(currentTask.durationSeconds)}
              </Typography>
            </Box>

            {/* Timer Display */}
            <Paper 
              elevation={3} 
              sx={{ 
                p: 4, 
                borderRadius: 4, 
                textAlign: 'center',
                mb: 4,
                minWidth: 300
              }}
            >
              <Typography variant="h2" sx={{ fontWeight: 'bold', mb: 2 }}>
                {formatTimeRemaining(timerState.timeRemaining)}
              </Typography>
              
              {/* Control Buttons */}
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, mb: 2 }}>
                <IconButton 
                  onClick={resetTimer}
                  size="large"
                  color="secondary"
                >
                  <RefreshIcon />
                </IconButton>
                
                <IconButton 
                  onClick={stopTimer}
                  size="large"
                  color="error"
                >
                  <StopIcon />
                </IconButton>
                
                <IconButton 
                  onClick={handleNextTask}
                  size="large"
                  color="primary"
                >
                  <SkipNextIcon />
                </IconButton>
              </Box>
            </Paper>

            {/* Big Play/Pause Button */}
            <Box sx={{ mb: 4 }}>
              <IconButton
                onClick={timerState.isRunning ? pauseTimer : handleStartCurrentTask}
                sx={{
                  width: 120,
                  height: 120,
                  backgroundColor: 'primary.main',
                  color: 'white',
                  '&:hover': {
                    backgroundColor: 'primary.dark',
                  },
                  boxShadow: 3
                }}
                size="large"
              >
                {timerState.isRunning ? (
                  <PauseIcon sx={{ fontSize: 60 }} />
                ) : (
                  <PlayArrowIcon sx={{ fontSize: 60 }} />
                )}
              </IconButton>
            </Box>

            {/* Task Progress */}
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                Task {currentTaskIndex + 1} of {currentTasks.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {currentTasks.length - currentTaskIndex - 1} tasks remaining
              </Typography>
            </Box>
          </>
        )}
      </Box>

      {/* Alarm Dialog */}
      <Dialog 
        open={showAlarmDialog} 
        onClose={handleStopAlarm}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ textAlign: 'center' }}>
          ⏰ Time's Up!
        </DialogTitle>
        <DialogContent sx={{ textAlign: 'center' }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            {currentTask.title}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Your task time has ended. Great job staying focused!
          </Typography>
        </DialogContent>
        <DialogActions sx={{ justifyContent: 'center', pb: 3 }}>
          <Button onClick={handleStopAlarm} variant="outlined">
            Stop Alarm
          </Button>
          <Button onClick={handleNextTask} variant="contained">
            Next Task
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}; 
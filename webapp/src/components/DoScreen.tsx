import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  IconButton,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Chip
} from '@mui/material';
import {
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Stop as StopIcon,
  Refresh as ResetIcon,
  SkipNext as SkipNextIcon,
  ArrowBack as BackIcon,
  CheckCircle as CheckIcon,
  RadioButtonUnchecked as UncheckIcon
} from '@mui/icons-material';
import { useTaskManager } from '../hooks/useTaskManager';
import { useTimer } from '../hooks/useTimer';
import { formatDuration } from '../utils/timer';

interface DoScreenProps {
  onBackToList: () => void;
}

const DoScreen: React.FC<DoScreenProps> = ({ onBackToList }) => {
  const { appState } = useTaskManager();
  const {
    currentTaskIndex,
    timeRemaining,
    isRunning,
    isPaused,
    isTransitioning,
    startTimer,
    pauseTimer,
    resumeTimer,
    stopTimer,
    resetTimer,
    nextTask
  } = useTimer();

  // Ensure taskLists is always an array and add null checks
  const taskLists = appState?.taskLists || [];
  const currentList = taskLists.find(list => list.id === appState?.currentListId);
  const currentTasks = currentList ? currentList.tasks : (appState?.dailyTasks || []);
  const incompleteTasks = currentTasks.filter(task => !task.completed);
  const currentTask = incompleteTasks[currentTaskIndex];

  const [showTaskList, setShowTaskList] = useState(false);

  useEffect(() => {
    if (incompleteTasks.length === 0) {
      // All tasks completed, go back to list
      onBackToList();
    }
  }, [incompleteTasks.length, onBackToList]);

  const handleStart = () => {
    if (incompleteTasks.length > 0) {
      startTimer(incompleteTasks);
    }
  };

  const handlePause = () => {
    pauseTimer();
  };

  const handleResume = () => {
    resumeTimer();
  };

  const handleStop = () => {
    stopTimer();
  };

  const handleReset = () => {
    resetTimer();
  };

  const handleNext = () => {
    nextTask();
  };



  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const getProgress = () => {
    if (!currentTask || !isRunning) return 0;
    const totalTime = currentTask.durationSeconds;
    const elapsed = totalTime - timeRemaining;
    return (elapsed / totalTime) * 100;
  };

  if (incompleteTasks.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', mt: 4 }}>
        <Typography variant="h6" color="text.secondary">
          All tasks completed!
        </Typography>
        <Button
          variant="contained"
          onClick={onBackToList}
          sx={{ mt: 2 }}
        >
          Back to Tasks
        </Button>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto' }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <IconButton onClick={onBackToList}>
          <BackIcon />
        </IconButton>
        <Typography variant="h4" component="h1">
          {currentTask ? currentTask.title : 'Ready to Start'}
        </Typography>
      </Box>

      {/* Timer Display */}
      <Card sx={{ mb: 3 }}>
        <CardContent sx={{ textAlign: 'center', py: 4 }}>
          {isTransitioning ? (
            <>
              <Typography variant="h2" color="primary" sx={{ mb: 2 }}>
                {formatTime(timeRemaining)}
              </Typography>
              <Typography variant="h6" color="text.secondary">
                Next task in...
              </Typography>
            </>
          ) : (
            <>
              <Typography variant="h2" color="primary" sx={{ mb: 2 }}>
                {formatTime(timeRemaining)}
              </Typography>
              <Typography variant="h6" color="text.secondary" sx={{ mb: 2 }}>
                {currentTask ? `${currentTask.title} (${formatDuration(currentTask.durationSeconds)})` : 'No task selected'}
              </Typography>
              
              {isRunning && (
                <LinearProgress 
                  variant="determinate" 
                  value={getProgress()} 
                  sx={{ height: 8, borderRadius: 4, mb: 2 }}
                />
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Controls */}
      <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, mb: 3 }}>
        {!isRunning && !isPaused && (
          <Button
            variant="contained"
            size="large"
            startIcon={<PlayIcon />}
            onClick={handleStart}
            sx={{ minWidth: 120 }}
          >
            Start
          </Button>
        )}
        
        {isRunning && (
          <>
            <Button
              variant="outlined"
              size="large"
              startIcon={<PauseIcon />}
              onClick={handlePause}
            >
              Pause
            </Button>
            <Button
              variant="outlined"
              size="large"
              startIcon={<SkipNextIcon />}
              onClick={handleNext}
            >
              Next
            </Button>
          </>
        )}
        
        {isPaused && (
          <>
            <Button
              variant="contained"
              size="large"
              startIcon={<PlayIcon />}
              onClick={handleResume}
            >
              Resume
            </Button>
            <Button
              variant="outlined"
              size="large"
              startIcon={<StopIcon />}
              onClick={handleStop}
            >
              Stop
            </Button>
          </>
        )}
        
        {(isRunning || isPaused) && (
          <Button
            variant="outlined"
            size="large"
            startIcon={<ResetIcon />}
            onClick={handleReset}
          >
            Reset
          </Button>
        )}
      </Box>

      {/* Task List */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">
              Tasks ({incompleteTasks.length} remaining)
            </Typography>
            <Button
              variant="outlined"
              size="small"
              onClick={() => setShowTaskList(!showTaskList)}
            >
              {showTaskList ? 'Hide' : 'Show'} List
            </Button>
          </Box>
          
          {showTaskList && (
            <List>
              {incompleteTasks.map((task, index) => (
                <ListItem key={task.id}>
                  <ListItemIcon>
                    {index === currentTaskIndex && isRunning ? (
                      <CheckIcon color="primary" />
                    ) : (
                      <UncheckIcon />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={task.title}
                    secondary={formatDuration(task.durationSeconds)}
                  />
                  <Chip
                    label={index === currentTaskIndex ? 'Current' : `${index + 1}`}
                    color={index === currentTaskIndex ? 'primary' : 'default'}
                    size="small"
                  />
                </ListItem>
              ))}
            </List>
          )}
        </CardContent>
      </Card>

      {/* Task List Dialog */}
      <Dialog 
        open={showTaskList} 
        onClose={() => setShowTaskList(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Task List</DialogTitle>
        <DialogContent>
          <List>
            {incompleteTasks.map((task, index) => (
              <ListItem key={task.id}>
                <ListItemIcon>
                  {index === currentTaskIndex && isRunning ? (
                    <CheckIcon color="primary" />
                  ) : (
                    <UncheckIcon />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary={task.title}
                  secondary={formatDuration(task.durationSeconds)}
                />
                <Chip
                  label={index === currentTaskIndex ? 'Current' : `${index + 1}`}
                  color={index === currentTaskIndex ? 'primary' : 'default'}
                  size="small"
                />
              </ListItem>
            ))}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowTaskList(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DoScreen; 
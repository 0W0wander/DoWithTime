import { useState, useEffect, useCallback } from 'react';
import { Task } from '../types';

export const useTimer = () => {
  const [timerState, setTimerState] = useState({
    timeRemaining: 0,
    isRunning: false,
    isPaused: false,
    isTransitioning: false,
    currentTaskIndex: 0,
    tasks: [] as Task[]
  });

  const [intervalId, setIntervalId] = useState<NodeJS.Timeout | null>(null);

  const startTimer = useCallback((tasks: Task[]) => {
    if (tasks.length === 0) return;
    
    setTimerState(prev => ({
      ...prev,
      tasks,
      currentTaskIndex: 0,
      timeRemaining: tasks[0].durationSeconds,
      isRunning: true,
      isPaused: false,
      isTransitioning: false
    }));
  }, []);

  const pauseTimer = useCallback(() => {
    setTimerState(prev => ({ ...prev, isRunning: false, isPaused: true }));
  }, []);

  const resumeTimer = useCallback(() => {
    setTimerState(prev => ({ ...prev, isRunning: true, isPaused: false }));
  }, []);

  const stopTimer = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      isRunning: false,
      isPaused: false,
      isTransitioning: false,
      timeRemaining: 0
    }));
  }, []);

  const resetTimer = useCallback(() => {
    if (timerState.tasks.length === 0) return;
    
    setTimerState(prev => ({
      ...prev,
      timeRemaining: prev.tasks[prev.currentTaskIndex]?.durationSeconds || 0,
      isRunning: false,
      isPaused: false,
      isTransitioning: false
    }));
  }, [timerState.tasks]);

  const nextTask = useCallback(() => {
    setTimerState(prev => {
      const nextIndex = prev.currentTaskIndex + 1;
      if (nextIndex < prev.tasks.length) {
        return {
          ...prev,
          currentTaskIndex: nextIndex,
          timeRemaining: prev.tasks[nextIndex].durationSeconds,
          isRunning: true,
          isPaused: false,
          isTransitioning: false
        };
      } else {
        // All tasks completed
        return {
          ...prev,
          isRunning: false,
          isPaused: false,
          isTransitioning: false,
          timeRemaining: 0
        };
      }
    });
  }, []);

  const skipTransition = useCallback(() => {
    setTimerState(prev => ({ ...prev, isTransitioning: false }));
  }, []);

  const stopAlarm = useCallback(() => {
    // This would handle alarm stopping logic
    console.log('Alarm stopped');
  }, []);

  // Timer countdown effect
  useEffect(() => {
    if (timerState.isRunning && !timerState.isPaused && timerState.timeRemaining > 0) {
      const id = setInterval(() => {
        setTimerState(prev => {
          if (prev.timeRemaining <= 1) {
            // Timer finished, start transition
            return {
              ...prev,
              isRunning: false,
              isTransitioning: true,
              timeRemaining: 10 // 10 second transition
            };
          }
          return {
            ...prev,
            timeRemaining: prev.timeRemaining - 1
          };
        });
      }, 1000);
      setIntervalId(id);
    } else {
      if (intervalId) {
        clearInterval(intervalId);
        setIntervalId(null);
      }
    }

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [timerState.isRunning, timerState.isPaused, timerState.timeRemaining, intervalId]);

  // Transition countdown effect
  useEffect(() => {
    if (timerState.isTransitioning && timerState.timeRemaining > 0) {
      const id = setInterval(() => {
        setTimerState(prev => {
          if (prev.timeRemaining <= 1) {
            // Transition finished, move to next task
            return {
              ...prev,
              isTransitioning: false,
              timeRemaining: 0
            };
          }
          return {
            ...prev,
            timeRemaining: prev.timeRemaining - 1
          };
        });
      }, 1000);
      setIntervalId(id);
    }

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [timerState.isTransitioning, timerState.timeRemaining, intervalId]);

  return {
    // Direct properties for easier access
    currentTaskIndex: timerState.currentTaskIndex,
    timeRemaining: timerState.timeRemaining,
    isRunning: timerState.isRunning,
    isPaused: timerState.isPaused,
    isTransitioning: timerState.isTransitioning,
    
    // Functions
    startTimer,
    pauseTimer,
    resumeTimer,
    stopTimer,
    resetTimer,
    nextTask,
    skipTransition,
    stopAlarm
  };
}; 
import { useState, useEffect, useRef, useCallback } from 'react';
import { Task, TimerState } from '../types';
import { playAlarmSound, sendNotification, requestNotificationPermission } from '../utils/timer';

export const useTimer = () => {
  const [timerState, setTimerState] = useState<TimerState>({
    isRunning: false,
    isPaused: false,
    timeRemaining: 0,
    currentTask: null,
    showAlarm: false,
    isTransitioning: false,
    transitionTime: 10
  });

  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const transitionIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const startTimer = useCallback((task: Task) => {
    setTimerState(prev => ({
      ...prev,
      currentTask: task,
      timeRemaining: task.durationSeconds * 1000,
      isRunning: true,
      isPaused: false,
      showAlarm: false,
      isTransitioning: false
    }));
  }, []);

  const pauseTimer = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      isRunning: false,
      isPaused: true
    }));
  }, []);

  const resumeTimer = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      isRunning: true,
      isPaused: false
    }));
  }, []);

  const stopTimer = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      isRunning: false,
      isPaused: false,
      timeRemaining: 0,
      currentTask: null,
      showAlarm: false,
      isTransitioning: false
    }));
  }, []);

  const resetTimer = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      timeRemaining: prev.currentTask ? prev.currentTask.durationSeconds * 1000 : 0,
      isRunning: false,
      isPaused: false,
      showAlarm: false,
      isTransitioning: false
    }));
  }, []);

  const skipTransition = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      isTransitioning: false,
      transitionTime: 10
    }));
  }, []);

  const nextTask = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      showAlarm: false,
      isTransitioning: true,
      transitionTime: 10
    }));
  }, []);

  const stopAlarm = useCallback(() => {
    setTimerState(prev => ({
      ...prev,
      showAlarm: false
    }));
  }, []);

  // Timer countdown effect
  useEffect(() => {
    if (timerState.isRunning && timerState.timeRemaining > 0) {
      intervalRef.current = setInterval(() => {
        setTimerState(prev => {
          const newTimeRemaining = prev.timeRemaining - 1000;
          
          if (newTimeRemaining <= 0) {
            // Timer finished
            playAlarmSound();
            sendNotification('Time\'s up!', `Task "${prev.currentTask?.title}" is complete!`);
            
            return {
              ...prev,
              timeRemaining: 0,
              isRunning: false,
              showAlarm: true,
              isTransitioning: false
            };
          }
          
          return {
            ...prev,
            timeRemaining: newTimeRemaining
          };
        });
      }, 1000);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [timerState.isRunning, timerState.timeRemaining]);

  // Transition timer effect
  useEffect(() => {
    if (timerState.isTransitioning && timerState.transitionTime > 0) {
      transitionIntervalRef.current = setInterval(() => {
        setTimerState(prev => {
          const newTransitionTime = prev.transitionTime - 1;
          
          if (newTransitionTime <= 0) {
            return {
              ...prev,
              isTransitioning: false,
              transitionTime: 10
            };
          }
          
          return {
            ...prev,
            transitionTime: newTransitionTime
          };
        });
      }, 1000);
    } else {
      if (transitionIntervalRef.current) {
        clearInterval(transitionIntervalRef.current);
        transitionIntervalRef.current = null;
      }
    }

    return () => {
      if (transitionIntervalRef.current) {
        clearInterval(transitionIntervalRef.current);
      }
    };
  }, [timerState.isTransitioning, timerState.transitionTime]);

  // Request notification permission on mount
  useEffect(() => {
    requestNotificationPermission();
  }, []);

  return {
    timerState,
    startTimer,
    pauseTimer,
    resumeTimer,
    stopTimer,
    resetTimer,
    skipTransition,
    nextTask,
    stopAlarm
  };
}; 
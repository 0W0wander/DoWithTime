import { Task, TaskList, AppState } from '../types';

const STORAGE_KEY = 'dowithtime_data';

export const saveToStorage = (data: AppState): void => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (error) {
    console.error('Failed to save to localStorage:', error);
  }
};

export const loadFromStorage = (): AppState => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to load from localStorage:', error);
  }
  
  // Return default state if nothing is stored
  return {
    tasks: [],
    dailyTasks: [],
    taskLists: [{ id: 1, name: 'Default List' }],
    currentListId: 1,
    wasInDailyList: false,
    timerState: {
      isRunning: false,
      isPaused: false,
      timeRemaining: 0,
      currentTask: null,
      showAlarm: false,
      isTransitioning: false,
      transitionTime: 10
    }
  };
};

export const exportData = (): string => {
  const data = localStorage.getItem(STORAGE_KEY);
  return data || '';
};

export const importData = (data: string): boolean => {
  try {
    const parsed = JSON.parse(data);
    localStorage.setItem(STORAGE_KEY, data);
    return true;
  } catch (error) {
    console.error('Failed to import data:', error);
    return false;
  }
}; 
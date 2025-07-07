import { AppState } from '../types';

const STORAGE_KEY = 'dowithtime_app_state';

export const saveToStorage = (appState: AppState): void => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appState));
  } catch (error) {
    console.error('Failed to save to localStorage:', error);
  }
};

export const loadFromStorage = (): AppState => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      
      // Ensure we have the correct structure with additional safety checks
      return {
        dailyTasks: Array.isArray(parsed.dailyTasks) ? parsed.dailyTasks : [],
        taskLists: Array.isArray(parsed.taskLists) ? parsed.taskLists : [],
        currentListId: parsed.currentListId || null,
        isDarkMode: Boolean(parsed.isDarkMode)
      };
    }
  } catch (error) {
    console.error('Failed to load from localStorage:', error);
  }

  // Return default state if nothing is stored or there's an error
  return {
    dailyTasks: [],
    taskLists: [],
    currentListId: null,
    isDarkMode: false
  };
};

export const clearStorage = (): void => {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (error) {
    console.error('Failed to clear localStorage:', error);
  }
};

export const exportData = (): string => {
  try {
    const data = loadFromStorage();
    return JSON.stringify(data, null, 2);
  } catch (error) {
    console.error('Failed to export data:', error);
    return '';
  }
};

export const importData = (jsonData: string): boolean => {
  try {
    const data = JSON.parse(jsonData);
    
    // Validate the data structure
    if (data && typeof data === 'object') {
      const validatedData: AppState = {
        dailyTasks: Array.isArray(data.dailyTasks) ? data.dailyTasks : [],
        taskLists: Array.isArray(data.taskLists) ? data.taskLists : [],
        currentListId: data.currentListId || null,
        isDarkMode: Boolean(data.isDarkMode)
      };
      
      saveToStorage(validatedData);
      return true;
    }
    return false;
  } catch (error) {
    console.error('Failed to import data:', error);
    return false;
  }
}; 
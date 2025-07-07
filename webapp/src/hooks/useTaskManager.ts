import { useState, useEffect, useCallback } from 'react';
import { Task, TaskList, AppState } from '../types';
import { saveToStorage, loadFromStorage } from '../utils/storage';
import { autoSync, uploadToCloud, setupRealtimeSync, getSyncStatus } from '../utils/cloudSync';

const initialState: AppState = {
  dailyTasks: [],
  taskLists: [],
  currentListId: null,
  isDarkMode: false
};

export const useTaskManager = () => {
  const [appState, setAppState] = useState<AppState>(initialState);
  const [isLoading, setIsLoading] = useState(true);
  const [syncStatus, setSyncStatus] = useState<{
    isOnline: boolean;
    lastSync: Date | null;
    deviceId: string;
  }>({
    isOnline: false,
    lastSync: null,
    deviceId: ''
  });

  // Load data and auto-sync on app start
  useEffect(() => {
    const initializeApp = async () => {
      try {
        setIsLoading(true);
        
        // Load local data first
        const localData = loadFromStorage();
        
        // Auto-sync with cloud
        const syncedData = await autoSync(localData);
        
        setAppState(syncedData);
        
        // Get sync status
        const status = await getSyncStatus();
        setSyncStatus(status);
        
        // Set up real-time sync
        const unsubscribe = setupRealtimeSync(
          (cloudData) => {
            console.log('Real-time sync: data updated from cloud');
            setAppState(cloudData);
            saveToStorage(cloudData);
          },
          (error) => {
            console.error('Real-time sync error:', error);
          }
        );
        
        // Cleanup on unmount
        return () => {
          if (unsubscribe) {
            unsubscribe();
          }
        };
      } catch (error) {
        console.error('Failed to initialize app:', error);
        // Fallback to local data
        const localData = loadFromStorage();
        setAppState(localData);
      } finally {
        setIsLoading(false);
      }
    };

    initializeApp();
  }, []);

  // Save to storage and cloud whenever app state changes
  const saveData = useCallback(async (newState: AppState) => {
    try {
      // Save to local storage
      saveToStorage(newState);
      
      // Upload to cloud
      await uploadToCloud(newState);
      
      // Update sync status
      const status = await getSyncStatus();
      setSyncStatus(status);
    } catch (error) {
      console.error('Failed to save data:', error);
    }
  }, []);

  // Update app state and save
  const updateState = useCallback((updater: (prev: AppState) => AppState) => {
    setAppState(prev => {
      const newState = updater(prev);
      saveData(newState);
      return newState;
    });
  }, [saveData]);

  // Task management functions
  const addTask = useCallback((task: Task) => {
    updateState(prev => ({
      ...prev,
      dailyTasks: [...(Array.isArray(prev.dailyTasks) ? prev.dailyTasks : []), task]
    }));
  }, [updateState]);

  const updateTask = useCallback((taskId: string, updates: Partial<Task>) => {
    updateState(prev => ({
      ...prev,
      dailyTasks: (Array.isArray(prev.dailyTasks) ? prev.dailyTasks : []).map(task =>
        task.id === taskId ? { ...task, ...updates } : task
      )
    }));
  }, [updateState]);

  const deleteTask = useCallback((taskId: string) => {
    updateState(prev => ({
      ...prev,
      dailyTasks: (Array.isArray(prev.dailyTasks) ? prev.dailyTasks : []).filter(task => task.id !== taskId)
    }));
  }, [updateState]);

  const toggleTaskComplete = useCallback((taskId: string) => {
    updateState(prev => ({
      ...prev,
      dailyTasks: (Array.isArray(prev.dailyTasks) ? prev.dailyTasks : []).map(task =>
        task.id === taskId ? { ...task, completed: !task.completed } : task
      )
    }));
  }, [updateState]);

  const reorderTasks = useCallback((fromIndex: number, toIndex: number) => {
    updateState(prev => {
      const dailyTasksArray = Array.isArray(prev.dailyTasks) ? prev.dailyTasks : [];
      const newTasks = [...dailyTasksArray];
      const [movedTask] = newTasks.splice(fromIndex, 1);
      newTasks.splice(toIndex, 0, movedTask);
      return { ...prev, dailyTasks: newTasks };
    });
  }, [updateState]);

  // Task list management
  const addTaskList = useCallback((taskList: TaskList) => {
    updateState(prev => ({
      ...prev,
      taskLists: [...(Array.isArray(prev.taskLists) ? prev.taskLists : []), taskList],
      currentListId: prev.currentListId || taskList.id
    }));
  }, [updateState]);

  const updateTaskList = useCallback((listId: string, updates: Partial<TaskList>) => {
    updateState(prev => ({
      ...prev,
      taskLists: (Array.isArray(prev.taskLists) ? prev.taskLists : []).map(list =>
        list.id === listId ? { ...list, ...updates } : list
      )
    }));
  }, [updateState]);

  const deleteTaskList = useCallback((listId: string) => {
    updateState(prev => {
      const taskListsArray = Array.isArray(prev.taskLists) ? prev.taskLists : [];
      return {
        ...prev,
        taskLists: taskListsArray.filter(list => list.id !== listId),
        currentListId: prev.currentListId === listId ? 
          (taskListsArray.length > 1 ? taskListsArray[0].id : null) : 
          prev.currentListId
      };
    });
  }, [updateState]);

  const setCurrentList = useCallback((listId: string) => {
    updateState(prev => ({
      ...prev,
      currentListId: listId
    }));
  }, [updateState]);

  // Theme management
  const toggleDarkMode = useCallback(() => {
    updateState(prev => ({
      ...prev,
      isDarkMode: !prev.isDarkMode
    }));
  }, [updateState]);

  // Manual sync function
  const manualSync = useCallback(async () => {
    try {
      setIsLoading(true);
      const syncedData = await autoSync(appState);
      setAppState(syncedData);
      
      const status = await getSyncStatus();
      setSyncStatus(status);
    } catch (error) {
      console.error('Manual sync failed:', error);
    } finally {
      setIsLoading(false);
    }
  }, [appState]);

  return {
    appState,
    isLoading,
    syncStatus,
    addTask,
    updateTask,
    deleteTask,
    toggleTaskComplete,
    reorderTasks,
    addTaskList,
    updateTaskList,
    deleteTaskList,
    setCurrentList,
    toggleDarkMode,
    manualSync
  };
}; 
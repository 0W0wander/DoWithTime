import { useState, useEffect, useCallback } from 'react';
import { Task, TaskList, AppState } from '../types';
import { saveToStorage, loadFromStorage } from '../utils/storage';

export const useTaskManager = () => {
  const [appState, setAppState] = useState<AppState>(() => loadFromStorage());

  // Save to storage whenever state changes
  useEffect(() => {
    saveToStorage(appState);
  }, [appState]);

  const addTask = useCallback((title: string, durationSeconds: number, isDaily: boolean = false, listId: number = 1) => {
    const newTask: Task = {
      id: Date.now(),
      title,
      durationSeconds,
      isCompleted: false,
      order: appState.tasks.length,
      isDaily,
      listId,
      completedToday: false
    };

    setAppState(prev => ({
      ...prev,
      tasks: isDaily ? [...prev.tasks, newTask] : [...prev.tasks, newTask],
      dailyTasks: isDaily ? [...prev.dailyTasks, newTask] : prev.dailyTasks
    }));
  }, [appState.tasks.length, appState.dailyTasks.length]);

  const updateTask = useCallback((taskId: number, updates: Partial<Task>) => {
    setAppState(prev => ({
      ...prev,
      tasks: prev.tasks.map(task => 
        task.id === taskId ? { ...task, ...updates } : task
      ),
      dailyTasks: prev.dailyTasks.map(task => 
        task.id === taskId ? { ...task, ...updates } : task
      )
    }));
  }, []);

  const deleteTask = useCallback((taskId: number) => {
    setAppState(prev => ({
      ...prev,
      tasks: prev.tasks.filter(task => task.id !== taskId),
      dailyTasks: prev.dailyTasks.filter(task => task.id !== taskId)
    }));
  }, []);

  const toggleTaskCompletion = useCallback((taskId: number) => {
    setAppState(prev => ({
      ...prev,
      tasks: prev.tasks.map(task => 
        task.id === taskId ? { ...task, isCompleted: !task.isCompleted } : task
      ),
      dailyTasks: prev.dailyTasks.map(task => 
        task.id === taskId ? { ...task, isCompleted: !task.isCompleted } : task
      )
    }));
  }, []);

  const reorderTasks = useCallback((taskIds: number[]) => {
    setAppState(prev => {
      const newTasks = taskIds.map((id, index) => {
        const task = prev.tasks.find(t => t.id === id);
        return task ? { ...task, order: index } : null;
      }).filter(Boolean) as Task[];

      return {
        ...prev,
        tasks: newTasks
      };
    });
  }, []);

  const addTaskList = useCallback((name: string) => {
    const newList: TaskList = {
      id: Date.now(),
      name
    };

    setAppState(prev => ({
      ...prev,
      taskLists: [...prev.taskLists, newList]
    }));
  }, []);

  const updateTaskList = useCallback((listId: number, name: string) => {
    setAppState(prev => ({
      ...prev,
      taskLists: prev.taskLists.map(list => 
        list.id === listId ? { ...list, name } : list
      )
    }));
  }, []);

  const deleteTaskList = useCallback((listId: number) => {
    setAppState(prev => ({
      ...prev,
      taskLists: prev.taskLists.filter(list => list.id !== listId),
      tasks: prev.tasks.filter(task => task.listId !== listId),
      currentListId: prev.currentListId === listId ? 1 : prev.currentListId
    }));
  }, []);

  const selectList = useCallback((listId: number) => {
    setAppState(prev => ({
      ...prev,
      currentListId: listId
    }));
  }, []);

  const setWasInDailyList = useCallback((wasInDailyList: boolean) => {
    setAppState(prev => ({
      ...prev,
      wasInDailyList
    }));
  }, []);

  const getCurrentTasks = useCallback(() => {
    if (appState.wasInDailyList) {
      return appState.dailyTasks;
    }
    return appState.tasks.filter(task => task.listId === appState.currentListId);
  }, [appState.wasInDailyList, appState.dailyTasks, appState.tasks, appState.currentListId]);

  const getCurrentList = useCallback(() => {
    return appState.taskLists.find(list => list.id === appState.currentListId);
  }, [appState.taskLists, appState.currentListId]);

  const resetDailyTasks = useCallback(() => {
    setAppState(prev => ({
      ...prev,
      dailyTasks: prev.dailyTasks.map(task => ({
        ...task,
        isCompleted: false,
        completedToday: false
      }))
    }));
  }, []);

  return {
    appState,
    addTask,
    updateTask,
    deleteTask,
    toggleTaskCompletion,
    reorderTasks,
    addTaskList,
    updateTaskList,
    deleteTaskList,
    selectList,
    setWasInDailyList,
    getCurrentTasks,
    getCurrentList,
    resetDailyTasks
  };
}; 
export interface Task {
  id: string;
  title: string;
  durationSeconds: number;
  completed: boolean;
  order: number;
}

export interface TaskList {
  id: string;
  name: string;
  tasks: Task[];
}

export interface AppState {
  dailyTasks: Task[];
  taskLists: TaskList[];
  currentListId: string | null;
  isDarkMode: boolean;
}

export interface TimerState {
  timeRemaining: number;
  isRunning: boolean;
  isPaused: boolean;
  isTransitioning: boolean;
  currentTaskIndex: number;
  tasks: Task[];
} 
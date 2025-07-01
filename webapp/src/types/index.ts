export interface Task {
  id: number;
  title: string;
  durationSeconds: number;
  isCompleted: boolean;
  order: number;
  isDaily: boolean;
  listId: number;
  completedToday: boolean;
}

export interface TaskList {
  id: number;
  name: string;
}

export interface TimerState {
  isRunning: boolean;
  isPaused: boolean;
  timeRemaining: number;
  currentTask: Task | null;
  showAlarm: boolean;
  isTransitioning: boolean;
  transitionTime: number;
}

export interface AppState {
  tasks: Task[];
  dailyTasks: Task[];
  taskLists: TaskList[];
  currentListId: number;
  wasInDailyList: boolean;
  timerState: TimerState;
} 
import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Checkbox,
  Fab,
  Chip,
  Divider,
  CircularProgress
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  PlayArrow as PlayArrowIcon
} from '@mui/icons-material';
import { useTaskManager } from '../hooks/useTaskManager';
import { Task, TaskList } from '../types';

interface TodoListScreenProps {
  onStartTasks: () => void;
}

const TodoListScreen: React.FC<TodoListScreenProps> = ({ onStartTasks }) => {
  const {
    appState,
    isLoading,
    addTask,
    updateTask,
    deleteTask,
    toggleTaskComplete,
    addTaskList,
    updateTaskList,
    deleteTaskList,
    setCurrentList
  } = useTaskManager();

  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [newTaskDuration, setNewTaskDuration] = useState('25');
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [newListName, setNewListName] = useState('');
  const [isAddListDialogOpen, setIsAddListDialogOpen] = useState(false);

  // Ensure taskLists is always an array and add null checks
  const taskLists = appState?.taskLists || [];
  const currentList = taskLists.find(list => list.id === appState?.currentListId);
  const currentTasks = currentList ? currentList.tasks : (appState?.dailyTasks || []);

  // Show loading state
  if (isLoading) {
    return (
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '50vh' 
      }}>
        <CircularProgress />
      </Box>
    );
  }

  const handleAddTask = () => {
    if (newTaskTitle.trim() && newTaskDuration) {
      const newTask: Task = {
        id: Date.now().toString(),
        title: newTaskTitle.trim(),
        durationSeconds: parseInt(newTaskDuration) * 60,
        completed: false,
        order: currentTasks.length
      };

      if (currentList) {
        // Add to current list
        const updatedList: TaskList = {
          ...currentList,
          tasks: [...currentList.tasks, newTask]
        };
        updateTaskList(currentList.id, updatedList);
      } else {
        // Add to daily tasks
        addTask(newTask);
      }

      setNewTaskTitle('');
      setNewTaskDuration('25');
      setIsAddDialogOpen(false);
    }
  };

  const handleEditTask = () => {
    if (editingTask && newTaskTitle.trim() && newTaskDuration) {
      const updatedTask = {
        ...editingTask,
        title: newTaskTitle.trim(),
        durationSeconds: parseInt(newTaskDuration) * 60
      };

      if (currentList) {
        const updatedList: TaskList = {
          ...currentList,
          tasks: currentList.tasks.map(task => 
            task.id === editingTask.id ? updatedTask : task
          )
        };
        updateTaskList(currentList.id, updatedList);
      } else {
        updateTask(editingTask.id, updatedTask);
      }

      setEditingTask(null);
      setNewTaskTitle('');
      setNewTaskDuration('25');
      setIsEditDialogOpen(false);
    }
  };

  const handleDeleteTask = (taskId: string) => {
    if (currentList) {
      const updatedList: TaskList = {
        ...currentList,
        tasks: currentList.tasks.filter(task => task.id !== taskId)
      };
      updateTaskList(currentList.id, updatedList);
    } else {
      deleteTask(taskId);
    }
  };

  const handleAddList = () => {
    if (newListName.trim()) {
      const newList: TaskList = {
        id: Date.now().toString(),
        name: newListName.trim(),
        tasks: []
      };
      addTaskList(newList);
      setNewListName('');
      setIsAddListDialogOpen(false);
    }
  };

  const formatDuration = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    return `${minutes} min`;
  };

  const canStartTasks = currentTasks.length > 0 && currentTasks.some(task => !task.completed);

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto' }}>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1">
          {currentList ? currentList.name : 'Daily Tasks'}
        </Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<PlayArrowIcon />}
          onClick={onStartTasks}
          disabled={!canStartTasks}
        >
          Start Tasks
        </Button>
      </Box>

      {/* Task Lists */}
      {taskLists.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Task Lists
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Chip
              label="Daily Tasks"
              color={!currentList ? "primary" : "default"}
              onClick={() => setCurrentList('')}
              clickable
            />
            {taskLists.map(list => (
              <Chip
                key={list.id}
                label={list.name}
                color={currentList?.id === list.id ? "primary" : "default"}
                onClick={() => setCurrentList(list.id)}
                onDelete={() => deleteTaskList(list.id)}
                clickable
              />
            ))}
            <Chip
              label="+ Add List"
              variant="outlined"
              onClick={() => setIsAddListDialogOpen(true)}
              clickable
            />
          </Box>
        </Box>
      )}

      {/* Tasks */}
      {currentTasks.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="h6" color="text.secondary" sx={{ mb: 2 }}>
              No tasks yet
            </Typography>
            <Typography color="text.secondary" sx={{ mb: 2 }}>
              {currentList ? 'Add tasks to this list to get started' : 'Add daily tasks to get started'}
            </Typography>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setIsAddDialogOpen(true)}
            >
              Add First Task
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent>
            <List>
              {currentTasks.map((task, index) => (
                <React.Fragment key={task.id}>
                  <ListItem
                    sx={{
                      opacity: task.completed ? 0.6 : 1,
                      textDecoration: task.completed ? 'line-through' : 'none'
                    }}
                  >
                    <Checkbox
                      checked={task.completed}
                      onChange={() => {
                        if (currentList) {
                          // Toggle completion in the current list
                          const updatedList: TaskList = {
                            ...currentList,
                            tasks: currentList.tasks.map(t => 
                              t.id === task.id ? { ...t, completed: !t.completed } : t
                            )
                          };
                          updateTaskList(currentList.id, updatedList);
                        } else {
                          // Toggle completion in daily tasks
                          toggleTaskComplete(task.id);
                        }
                      }}
                    />
                    <ListItemText
                      primary={task.title}
                      secondary={formatDuration(task.durationSeconds)}
                    />
                    <ListItemSecondaryAction>
                      <IconButton
                        edge="end"
                        onClick={() => {
                          setEditingTask(task);
                          setNewTaskTitle(task.title);
                          setNewTaskDuration(Math.floor(task.durationSeconds / 60).toString());
                          setIsEditDialogOpen(true);
                        }}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        edge="end"
                        onClick={() => handleDeleteTask(task.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </ListItemSecondaryAction>
                  </ListItem>
                  {index < currentTasks.length - 1 && <Divider />}
                </React.Fragment>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      {/* Add Task FAB */}
      <Fab
        color="primary"
        aria-label="add task"
        sx={{ position: 'fixed', bottom: 16, right: 16 }}
        onClick={() => setIsAddDialogOpen(true)}
      >
        <AddIcon />
      </Fab>

      {/* Add Task Dialog */}
      <Dialog open={isAddDialogOpen} onClose={() => setIsAddDialogOpen(false)}>
        <DialogTitle>Add New Task</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Task Title"
            fullWidth
            variant="outlined"
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Duration (minutes)"
            type="number"
            fullWidth
            variant="outlined"
            value={newTaskDuration}
            onChange={(e) => setNewTaskDuration(e.target.value)}
            inputProps={{ min: 1, max: 480 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsAddDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAddTask} variant="contained">Add Task</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Task Dialog */}
      <Dialog open={isEditDialogOpen} onClose={() => setIsEditDialogOpen(false)}>
        <DialogTitle>Edit Task</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Task Title"
            fullWidth
            variant="outlined"
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Duration (minutes)"
            type="number"
            fullWidth
            variant="outlined"
            value={newTaskDuration}
            onChange={(e) => setNewTaskDuration(e.target.value)}
            inputProps={{ min: 1, max: 480 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsEditDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleEditTask} variant="contained">Save Changes</Button>
        </DialogActions>
      </Dialog>

      {/* Add List Dialog */}
      <Dialog open={isAddListDialogOpen} onClose={() => setIsAddListDialogOpen(false)}>
        <DialogTitle>Add New Task List</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="List Name"
            fullWidth
            variant="outlined"
            value={newListName}
            onChange={(e) => setNewListName(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIsAddListDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAddList} variant="contained">Add List</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TodoListScreen; 
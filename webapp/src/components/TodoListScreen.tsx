import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Drawer,
  Fab,
  IconButton,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  TextField,
  Typography,
  AppBar,
  Toolbar,
  Divider,
  Menu,
  MenuItem,
  Switch,
  FormControlLabel
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Menu as MenuIcon,
  MoreVert as MoreVertIcon,
  PlayArrow as PlayArrowIcon,
  DragIndicator as DragIndicatorIcon
} from '@mui/icons-material';
import { useTaskManager } from '../hooks/useTaskManager';
import { formatDuration } from '../utils/timer';
import { exportData, importData } from '../utils/storage';

interface TodoListScreenProps {
  onNavigateToDo: () => void;
}

export const TodoListScreen: React.FC<TodoListScreenProps> = ({ onNavigateToDo }) => {
  const {
    appState,
    addTask,
    updateTask,
    deleteTask,
    toggleTaskCompletion,
    addTaskList,
    updateTaskList,
    deleteTaskList,
    selectList,
    setWasInDailyList,
    getCurrentTasks,
    getCurrentList
  } = useTaskManager();

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editingTask, setEditingTask] = useState<any>(null);
  const [showRenameDialog, setShowRenameDialog] = useState(false);
  const [renameListId, setRenameListId] = useState<number | null>(null);
  const [showAddListDialog, setShowAddListDialog] = useState(false);
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [newTaskDuration, setNewTaskDuration] = useState('');
  const [newListName, setNewListName] = useState('');
  const [isDailyTask, setIsDailyTask] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [showSyncDialog, setShowSyncDialog] = useState(false);
  const [syncData, setSyncData] = useState('');

  const currentTasks = getCurrentTasks();
  const currentList = getCurrentList();

  const handleAddTask = () => {
    if (newTaskTitle.trim() && newTaskDuration) {
      const duration = parseInt(newTaskDuration);
      if (duration > 0) {
        addTask(
          newTaskTitle.trim(),
          duration,
          isDailyTask,
          appState.wasInDailyList ? 1 : appState.currentListId
        );
        setNewTaskTitle('');
        setNewTaskDuration('');
        setIsDailyTask(false);
        setShowAddDialog(false);
      }
    }
  };

  const handleEditTask = () => {
    if (editingTask && newTaskTitle.trim() && newTaskDuration) {
      const duration = parseInt(newTaskDuration);
      if (duration > 0) {
        updateTask(editingTask.id, {
          title: newTaskTitle.trim(),
          durationSeconds: duration
        });
        setShowEditDialog(false);
        setEditingTask(null);
        setNewTaskTitle('');
        setNewTaskDuration('');
      }
    }
  };

  const handleAddList = () => {
    if (newListName.trim()) {
      addTaskList(newListName.trim());
      setNewListName('');
      setShowAddListDialog(false);
    }
  };

  const handleRenameList = () => {
    if (renameListId && newListName.trim()) {
      updateTaskList(renameListId, newListName.trim());
      setShowRenameDialog(false);
      setRenameListId(null);
      setNewListName('');
    }
  };

  const openEditDialog = (task: any) => {
    setEditingTask(task);
    setNewTaskTitle(task.title);
    setNewTaskDuration(task.durationSeconds.toString());
    setShowEditDialog(true);
  };

  const openRenameDialog = (listId: number, currentName: string) => {
    setRenameListId(listId);
    setNewListName(currentName);
    setShowRenameDialog(true);
  };

  const handleStartTasks = () => {
    const incompleteTasks = currentTasks.filter(task => !task.isCompleted);
    if (incompleteTasks.length > 0) {
      onNavigateToDo();
    }
  };

  const handleExportData = () => {
    const data = exportData();
    navigator.clipboard.writeText(data).then(() => {
      alert('Data copied to clipboard! You can now paste this into your Android app.');
    });
  };

  const handleImportData = () => {
    if (syncData.trim()) {
      const success = importData(syncData);
      if (success) {
        alert('Data imported successfully!');
        setShowSyncDialog(false);
        setSyncData('');
        window.location.reload(); // Refresh to show new data
      } else {
        alert('Failed to import data. Please check the format.');
      }
    }
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton
            edge="start"
            color="inherit"
            onClick={() => setDrawerOpen(true)}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            {appState.wasInDailyList ? 'Dailies' : currentList?.name || 'Tasks'}
          </Typography>
          <Button color="inherit" onClick={() => setShowSyncDialog(true)}>
            Sync
          </Button>
          <Button color="inherit" onClick={handleStartTasks}>
            Start Tasks
          </Button>
        </Toolbar>
      </AppBar>

      <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
        {currentTasks.length === 0 ? (
          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Typography variant="h6" color="text.secondary">
              No tasks yet
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Add your first task to get started!
            </Typography>
          </Box>
        ) : (
          <List>
            {currentTasks.map((task) => (
              <Card key={task.id} sx={{ mb: 2 }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', flexGrow: 1 }}>
                      <IconButton size="small" sx={{ mr: 1 }}>
                        <DragIndicatorIcon />
                      </IconButton>
                      <Box sx={{ flexGrow: 1 }}>
                        <Typography
                          variant="h6"
                          sx={{
                            textDecoration: task.isCompleted ? 'line-through' : 'none',
                            color: task.isCompleted ? 'text.secondary' : 'text.primary'
                          }}
                        >
                          {task.title}
                        </Typography>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                          <Chip
                            label={formatDuration(task.durationSeconds)}
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                          {task.isDaily && (
                            <Chip label="Daily" size="small" color="secondary" />
                          )}
                        </Box>
                      </Box>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <Switch
                        checked={task.isCompleted}
                        onChange={() => toggleTaskCompletion(task.id)}
                        color="primary"
                      />
                      <IconButton onClick={() => openEditDialog(task)}>
                        <EditIcon />
                      </IconButton>
                      <IconButton onClick={() => deleteTask(task.id)}>
                        <DeleteIcon />
                      </IconButton>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            ))}
          </List>
        )}
      </Box>

      <Fab
        color="primary"
        aria-label="add"
        sx={{ position: 'fixed', bottom: 16, right: 16 }}
        onClick={() => setShowAddDialog(true)}
      >
        <AddIcon />
      </Fab>

      {/* Navigation Drawer */}
      <Drawer
        anchor="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      >
        <Box sx={{ width: 300, p: 2 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            DoWithTime
          </Typography>
          
          <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 'bold' }}>
            Dailies
          </Typography>
          <Button
            fullWidth
            variant={appState.wasInDailyList ? 'contained' : 'text'}
            onClick={() => {
              setWasInDailyList(true);
              setDrawerOpen(false);
            }}
            sx={{ justifyContent: 'flex-start', mb: 1 }}
          >
            Dailies
          </Button>
          
          <Divider sx={{ my: 2 }} />
          
          <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 'bold' }}>
            Your Lists
          </Typography>
          {appState.taskLists.map((list) => (
            <Box key={list.id} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Button
                fullWidth
                variant={!appState.wasInDailyList && list.id === appState.currentListId ? 'contained' : 'text'}
                onClick={() => {
                  setWasInDailyList(false);
                  selectList(list.id);
                  setDrawerOpen(false);
                }}
                sx={{ justifyContent: 'flex-start' }}
              >
                {list.name}
              </Button>
              <IconButton size="small" onClick={() => openRenameDialog(list.id, list.name)}>
                <EditIcon fontSize="small" />
              </IconButton>
              {appState.taskLists.length > 1 && (
                <IconButton size="small" onClick={() => deleteTaskList(list.id)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
          ))}
          
          <Button
            fullWidth
            variant="outlined"
            onClick={() => {
              setShowAddListDialog(true);
              setDrawerOpen(false);
            }}
            sx={{ mt: 2 }}
          >
            Add New List
          </Button>
        </Box>
      </Drawer>

      {/* Add Task Dialog */}
      <Dialog open={showAddDialog} onClose={() => setShowAddDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add New Task</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Task Title"
            fullWidth
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Duration (seconds)"
            type="number"
            fullWidth
            value={newTaskDuration}
            onChange={(e) => setNewTaskDuration(e.target.value)}
            sx={{ mb: 2 }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={isDailyTask}
                onChange={(e) => setIsDailyTask(e.target.checked)}
              />
            }
            label="Daily Task"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowAddDialog(false)}>Cancel</Button>
          <Button onClick={handleAddTask} variant="contained">Add Task</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Task Dialog */}
      <Dialog open={showEditDialog} onClose={() => setShowEditDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Task</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Task Title"
            fullWidth
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Duration (seconds)"
            type="number"
            fullWidth
            value={newTaskDuration}
            onChange={(e) => setNewTaskDuration(e.target.value)}
            sx={{ mb: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowEditDialog(false)}>Cancel</Button>
          <Button onClick={handleEditTask} variant="contained">Save Changes</Button>
        </DialogActions>
      </Dialog>

      {/* Add List Dialog */}
      <Dialog open={showAddListDialog} onClose={() => setShowAddListDialog(false)}>
        <DialogTitle>Add New List</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="List Name"
            fullWidth
            value={newListName}
            onChange={(e) => setNewListName(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowAddListDialog(false)}>Cancel</Button>
          <Button onClick={handleAddList} variant="contained">Add List</Button>
        </DialogActions>
      </Dialog>

      {/* Rename List Dialog */}
      <Dialog open={showRenameDialog} onClose={() => setShowRenameDialog(false)}>
        <DialogTitle>Rename List</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="List Name"
            fullWidth
            value={newListName}
            onChange={(e) => setNewListName(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowRenameDialog(false)}>Cancel</Button>
          <Button onClick={handleRenameList} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>

      {/* Sync Dialog */}
      <Dialog open={showSyncDialog} onClose={() => setShowSyncDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Sync Data</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Export your data to copy to your Android app, or import data from your Android app.
          </Typography>
          
          <Box sx={{ mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 1 }}>Export Data</Typography>
            <Typography variant="body2" sx={{ mb: 2 }}>
              Click the button below to copy your current data to clipboard. Then paste it into your Android app.
            </Typography>
            <Button variant="contained" onClick={handleExportData} fullWidth>
              Export to Clipboard
            </Button>
          </Box>
          
          <Divider sx={{ my: 2 }} />
          
          <Box>
            <Typography variant="h6" sx={{ mb: 1 }}>Import Data</Typography>
            <Typography variant="body2" sx={{ mb: 2 }}>
              Paste data from your Android app below to import it here.
            </Typography>
            <TextField
              multiline
              rows={6}
              fullWidth
              placeholder="Paste your Android app data here..."
              value={syncData}
              onChange={(e) => setSyncData(e.target.value)}
              sx={{ mb: 2 }}
            />
            <Button variant="contained" onClick={handleImportData} fullWidth>
              Import Data
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowSyncDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}; 
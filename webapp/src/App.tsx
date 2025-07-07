import React, { useState } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline, Box, AppBar, Toolbar, Typography, IconButton, Chip, CircularProgress } from '@mui/material';
import { Brightness4, Brightness7, Sync, CloudOff } from '@mui/icons-material';
import TodoListScreen from './components/TodoListScreen';
import DoScreen from './components/DoScreen';
import { useTaskManager } from './hooks/useTaskManager';

function App() {
  const { 
    appState, 
    isLoading, 
    syncStatus, 
    toggleDarkMode, 
    manualSync 
  } = useTaskManager();
  
  const [currentScreen, setCurrentScreen] = useState<'todo' | 'do'>('todo');

  const theme = createTheme({
    palette: {
      mode: appState.isDarkMode ? 'dark' : 'light',
      primary: {
        main: '#1976d2',
      },
      secondary: {
        main: '#dc004e',
      },
    },
  });

  const formatLastSync = (date: Date | null) => {
    if (!date) return 'Never';
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  if (isLoading) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          minHeight="100vh"
          flexDirection="column"
          gap={2}
        >
          <CircularProgress size={60} />
          <Typography variant="h6" color="text.secondary">
            Syncing with cloud...
          </Typography>
        </Box>
      </ThemeProvider>
    );
  }

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ flexGrow: 1 }}>
        <AppBar position="static">
          <Toolbar>
            <Typography 
              variant="h6" 
              component="div" 
              sx={{ flexGrow: 1, cursor: 'pointer' }}
              onClick={() => setCurrentScreen('todo')}
            >
              DoWithTime
            </Typography>
            
            {/* Sync Status */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mr: 2 }}>
              {syncStatus.isOnline ? (
                <Chip
                  icon={<Sync />}
                  label={formatLastSync(syncStatus.lastSync)}
                  size="small"
                  color="success"
                  variant="outlined"
                />
              ) : (
                <Chip
                  icon={<CloudOff />}
                  label="Offline"
                  size="small"
                  color="error"
                  variant="outlined"
                />
              )}
            </Box>

            {/* Manual Sync Button */}
            <IconButton 
              color="inherit" 
              onClick={manualSync}
              disabled={isLoading}
            >
              <Sync />
            </IconButton>

            {/* Theme Toggle */}
            <IconButton color="inherit" onClick={toggleDarkMode}>
              {appState.isDarkMode ? <Brightness7 /> : <Brightness4 />}
            </IconButton>
          </Toolbar>
        </AppBar>

        <Box sx={{ p: 2 }}>
          {currentScreen === 'todo' ? (
            <TodoListScreen onStartTasks={() => setCurrentScreen('do')} />
          ) : (
            <DoScreen onBackToList={() => setCurrentScreen('todo')} />
          )}
        </Box>
      </Box>
    </ThemeProvider>
  );
}

export default App; 
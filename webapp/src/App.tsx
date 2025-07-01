import React, { useState } from 'react';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import { TodoListScreen } from './components/TodoListScreen';
import { DoScreen } from './components/DoScreen';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#90caf9',
    },
    secondary: {
      main: '#f48fb1',
    },
    background: {
      default: '#121212',
      paper: '#1e1e1e',
    },
    text: {
      primary: '#ffffff',
      secondary: '#b3b3b3',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#1e1e1e',
          border: '1px solid #333',
        },
      },
    },
  },
});

type Screen = 'todo' | 'do';

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('todo');

  const handleNavigateToDo = () => {
    setCurrentScreen('do');
  };

  const handleNavigateBack = () => {
    setCurrentScreen('todo');
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {currentScreen === 'todo' ? (
        <TodoListScreen onNavigateToDo={handleNavigateToDo} />
      ) : (
        <DoScreen onNavigateBack={handleNavigateBack} />
      )}
    </ThemeProvider>
  );
}

export default App; 
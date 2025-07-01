# DoWithTime Web App

A web version of the DoWithTime productivity app that combines todo lists with timers to help you stay focused and complete tasks on time.

## Features

### Task Management

- Create tasks with custom time durations
- Organize tasks into different lists
- Mark tasks as completed
- Delete and edit tasks
- Daily tasks support

### Timer Functionality

- Big, easy-to-use play button to start tasks
- Visual countdown timer
- Pause, resume, and reset functionality
- Automatic alarm when time runs out

### Data Persistence

- Local storage for data persistence
- Export/import functionality for data backup
- Automatic saving of all changes

### Modern UI

- Material-UI design system
- Responsive design for desktop and mobile
- Intuitive navigation between screens

## Getting Started

### Prerequisites

- Node.js (version 14 or higher)
- npm or yarn

### Installation

1. Navigate to the webapp directory:

```bash
cd webapp
```

2. Install dependencies:

```bash
npm install
```

3. Start the development server:

```bash
npm start
```

4. Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

### Building for Production

To create a production build:

```bash
npm run build
```

This creates an optimized build in the `build` folder.

## How to Use

1. **Add Tasks**: Click the "+" button to create new tasks with time durations
2. **Organize**: Use the drawer menu to switch between different task lists
3. **Start Working**: Click "Start Tasks" to begin your first task
4. **Focus**: Use the big play button to start the timer
5. **Complete**: Mark tasks as done or let the timer run out
6. **Continue**: Automatically move to the next task

## Project Structure

```
webapp/
├── public/                 # Static files
├── src/
│   ├── components/         # React components
│   │   ├── TodoListScreen.tsx
│   │   └── DoScreen.tsx
│   ├── hooks/             # Custom React hooks
│   │   ├── useTaskManager.ts
│   │   └── useTimer.ts
│   ├── types/             # TypeScript type definitions
│   │   └── index.ts
│   ├── utils/             # Utility functions
│   │   ├── storage.ts
│   │   └── timer.ts
│   ├── App.tsx            # Main app component
│   └── index.tsx          # Entry point
├── package.json
├── tsconfig.json
└── README.md
```

## Data Storage

The app uses browser localStorage to persist your tasks and settings. This means:

- Your data is stored locally in your browser
- Data persists between browser sessions
- No server or internet connection required
- Data is private and secure

## Browser Support

- Chrome (recommended)
- Firefox
- Safari
- Edge

## Future Enhancements

- Cloud sync functionality
- Custom alarm sounds
- Task categories and tags
- Statistics and progress tracking
- Dark/light theme toggle
- PWA (Progressive Web App) features
- Mobile app version

## Contributing

Feel free to contribute to this project by:

- Reporting bugs
- Suggesting new features
- Submitting pull requests

## License

This project is open source and available under the MIT License.

# Firebase Setup for Auto-Sync

This guide will help you set up Firebase Firestore for automatic synchronization between your web app and Android app.

## Step 1: Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter a project name (e.g., "dowithtime-sync")
4. Choose whether to enable Google Analytics (optional)
5. Click "Create project"

## Step 2: Enable Firestore Database

1. In your Firebase project, click on "Firestore Database" in the left sidebar
2. Click "Create database"
3. Choose "Start in test mode" (for development)
4. Select a location close to your users
5. Click "Done"

## Step 3: Get Your Web App Configuration

1. In Firebase Console, click the gear icon next to "Project Overview"
2. Select "Project settings"
3. Scroll down to "Your apps" section
4. Click the web icon (</>) to add a web app
5. Enter an app nickname (e.g., "DoWithTime Web")
6. Click "Register app"
7. Copy the configuration object that looks like this:

```javascript
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abcdef",
};
```

## Step 4: Update Your Web App Configuration

1. Open `webapp/src/firebase/config.ts`
2. Replace the placeholder configuration with your actual Firebase config:

```typescript
const firebaseConfig = {
  apiKey: "your-actual-api-key",
  authDomain: "your-actual-project.firebaseapp.com",
  projectId: "your-actual-project-id",
  storageBucket: "your-actual-project.appspot.com",
  messagingSenderId: "your-actual-sender-id",
  appId: "your-actual-app-id",
};
```

## Step 5: Set Up Firestore Security Rules

1. In Firebase Console, go to Firestore Database
2. Click on "Rules" tab
3. Replace the default rules with these (for development):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow read/write access to all documents for now
    // In production, you should add proper authentication
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

4. Click "Publish"

## Step 6: Install Dependencies

In your webapp directory, install Firebase:

```bash
cd webapp
npm install firebase
```

## Step 7: Test the Setup

1. Start your web app: `npm start`
2. Add some tasks
3. Check the Firebase Console > Firestore Database to see if data is being saved
4. Open the app in another browser/device to test sync

## Security Considerations

For production use, you should:

1. **Add Authentication**: Implement user accounts to secure data
2. **Update Security Rules**: Restrict access to authenticated users only
3. **Enable App Check**: Prevent abuse from unauthorized clients
4. **Set up proper indexes**: For better query performance

Example production security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /devices/{deviceId} {
      allow read, write: if request.auth != null &&
        request.auth.uid == deviceId;
    }
  }
}
```

## Troubleshooting

### "Permission denied" errors

- Check your Firestore security rules
- Make sure you're in test mode or have proper authentication

### "Firebase not initialized" errors

- Verify your configuration in `config.ts`
- Make sure Firebase is imported correctly

### Sync not working

- Check browser console for errors
- Verify your internet connection
- Check if Firebase project is in the correct region

## Next Steps

Once Firebase is set up, your web app will:

- Automatically sync data when you open the app
- Upload changes to the cloud in real-time
- Download newer data from other devices
- Show sync status in the app header

The Android app will need similar Firebase setup to complete the cross-platform sync.

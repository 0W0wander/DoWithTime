import { 
  doc, 
  setDoc, 
  getDoc, 
  onSnapshot,
  serverTimestamp,
  DocumentSnapshot,
  DocumentData
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { AppState } from '../types';

// Add shared doc reference
const getSharedDoc = () => doc(db, 'shared', 'tasks');

// Upload data to cloud
export const uploadToCloud = async (data: AppState): Promise<void> => {
  try {
    const sharedDoc = getSharedDoc();
    await setDoc(sharedDoc, {
      data: data,
      lastUpdated: serverTimestamp(),
      version: '1.0'
    });
    console.log('Data uploaded to cloud successfully');
  } catch (error) {
    console.error('Error uploading to cloud:', error);
    throw error;
  }
};

// Download data from cloud
export const downloadFromCloud = async (): Promise<AppState | null> => {
  try {
    const sharedDoc = getSharedDoc();
    const docSnap = await getDoc(sharedDoc);
    
    if (docSnap.exists()) {
      const cloudData = docSnap.data();
      const data = cloudData.data as AppState;
      
      // Validate and sanitize the data structure
      if (data && typeof data === 'object') {
        return {
          dailyTasks: Array.isArray(data.dailyTasks) ? data.dailyTasks : [],
          taskLists: Array.isArray(data.taskLists) ? data.taskLists : [],
          currentListId: data.currentListId || null,
          isDarkMode: Boolean(data.isDarkMode)
        };
      }
      return null;
    } else {
      console.log('No cloud data found');
      return null;
    }
  } catch (error) {
    console.error('Error downloading from cloud:', error);
    throw error;
  }
};

// Get last update timestamp from cloud
export const getCloudLastUpdated = async (): Promise<Date | null> => {
  try {
    const sharedDoc = getSharedDoc();
    const docSnap = await getDoc(sharedDoc);
    
    if (docSnap.exists()) {
      const cloudData = docSnap.data();
      return cloudData.lastUpdated?.toDate() || null;
    }
    return null;
  } catch (error) {
    console.error('Error getting cloud timestamp:', error);
    return null;
  }
};

// Auto-sync: compare timestamps and sync the newer data
export const autoSync = async (localData: AppState): Promise<AppState> => {
  try {
    // Validate local data structure first
    const validatedLocalData: AppState = {
      dailyTasks: Array.isArray(localData.dailyTasks) ? localData.dailyTasks : [],
      taskLists: Array.isArray(localData.taskLists) ? localData.taskLists : [],
      currentListId: localData.currentListId || null,
      isDarkMode: Boolean(localData.isDarkMode)
    };

    const cloudLastUpdated = await getCloudLastUpdated();
    const localLastUpdated = new Date(localStorage.getItem('dowithtime_last_updated') || '0');
    
    if (!cloudLastUpdated) {
      // No cloud data, upload local data
      await uploadToCloud(validatedLocalData);
      localStorage.setItem('dowithtime_last_updated', new Date().toISOString());
      return validatedLocalData;
    }
    
    if (localLastUpdated > cloudLastUpdated) {
      // Local data is newer, upload to cloud
      await uploadToCloud(validatedLocalData);
      localStorage.setItem('dowithtime_last_updated', new Date().toISOString());
      return validatedLocalData;
    } else {
      // Cloud data is newer, download from cloud
      const cloudData = await downloadFromCloud();
      if (cloudData) {
        localStorage.setItem('dowithtime_last_updated', new Date().toISOString());
        return cloudData;
      }
    }
    
    return validatedLocalData;
  } catch (error) {
    console.error('Auto-sync failed:', error);
    // Return validated local data if sync fails
    return {
      dailyTasks: Array.isArray(localData.dailyTasks) ? localData.dailyTasks : [],
      taskLists: Array.isArray(localData.taskLists) ? localData.taskLists : [],
      currentListId: localData.currentListId || null,
      isDarkMode: Boolean(localData.isDarkMode)
    };
  }
};

// Set up real-time sync listener
export const setupRealtimeSync = (
  onDataChange: (data: AppState) => void,
  onError: (error: Error) => void
) => {
  try {
    const sharedDoc = getSharedDoc();
    return onSnapshot(sharedDoc, (doc: DocumentSnapshot<DocumentData>) => {
      if (doc.exists()) {
        const cloudData = doc.data();
        onDataChange(cloudData.data as AppState);
      }
    }, (error: Error) => {
      console.error('Realtime sync error:', error);
      onError(error);
    });
  } catch (error) {
    console.error('Error setting up real-time sync:', error);
    onError(error as Error);
  }
};

// Get sync status
export const getSyncStatus = async (): Promise<{
  isOnline: boolean;
  lastSync: Date | null;
  deviceId: string;
}> => {
  try {
    const cloudLastUpdated = await getCloudLastUpdated();
    const localLastUpdated = localStorage.getItem('dowithtime_last_updated');
    
    return {
      isOnline: true,
      lastSync: cloudLastUpdated || (localLastUpdated ? new Date(localLastUpdated) : null),
      deviceId: 'shared',
    };
  } catch (error) {
    return {
      isOnline: false,
      lastSync: null,
      deviceId: 'shared',
    };
  }
}; 
import { getFirestore } from 'firebase/firestore';

// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyBKkcNTMDaQF8KWq5J5nqfzmiUT4MTosTQ",
  authDomain: "dowithtime.firebaseapp.com",
  projectId: "dowithtime",
  storageBucket: "dowithtime.firebasestorage.app",
  messagingSenderId: "431685277442",
  appId: "1:431685277442:web:7b5843dd9e1f077ef3941b"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firestore
export const db = getFirestore(app);

export default app; 
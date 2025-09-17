# Firebase Setup Instructions

## 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: "Smart Academic Tracker" 
4. Enable Google Analytics (recommended)
5. Choose or create Analytics account
6. Click "Create project"

## 2. Add Android App to Firebase Project

1. Click "Add app" and select Android
2. Enter package name: `com.smartacademictracker`
3. Enter app nickname: "Smart Academic Tracker"
4. Enter SHA-1 certificate fingerprint (for release builds)
5. Click "Register app"
6. Download `google-services.json` file
7. Place `google-services.json` in `app/` directory

## 3. Enable Firebase Services

### Authentication
1. Go to Authentication > Sign-in method
2. Enable "Email/Password" provider
3. Optionally enable "Google" provider for social login

### Firestore Database
1. Go to Firestore Database
2. Click "Create database"
3. Choose "Start in test mode" (we'll add security rules later)
4. Select your preferred location
5. Click "Done"

### Analytics & Crashlytics
1. Go to Analytics > Dashboard (should be enabled by default)
2. Go to Crashlytics > Get started
3. Follow the setup instructions (already configured in build files)

## 4. Security Rules for Firestore

Replace the default Firestore rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own user document
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Only admins can manage subjects
    match /subjects/{document} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
    
    // Students can read their own grades, teachers can read/write grades for their subjects
    match /grades/{document} {
      allow read: if request.auth != null && 
        (resource.data.studentId == request.auth.uid || 
         resource.data.teacherId == request.auth.uid ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN');
      allow write: if request.auth != null && 
        (resource.data.teacherId == request.auth.uid ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN');
    }
    
    // Teacher applications
    match /teacher_applications/{document} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'TEACHER';
      allow update: if request.auth != null && 
        (resource.data.teacherId == request.auth.uid ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN');
    }
    
    // Enrollments
    match /enrollments/{document} {
      allow read: if request.auth != null && 
        (resource.data.studentId == request.auth.uid ||
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['TEACHER', 'ADMIN']);
      allow write: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
  }
}
```

## 5. Test the Setup

1. Build and run the app
2. Create a test account with different roles
3. Verify authentication works
4. Check that data is being stored in Firestore

## 6. Production Considerations

1. Generate SHA-1 fingerprint for release builds
2. Update security rules for production
3. Set up proper user management
4. Configure app distribution for testing
5. Set up monitoring and alerts

## Environment Variables (Optional)

For CI/CD and different environments, consider using:
- `google-services.json` for different environments
- Firebase Remote Config for feature flags
- Environment-specific Firebase projects

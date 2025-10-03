# 🔥 Firebase Setup Guide

This guide will help you set up Firebase for the Smart Academic Performance Tracker application.

## 📋 Prerequisites

- Google account
- Android Studio installed
- Firebase project access

## 🚀 Step-by-Step Setup

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project"
3. Enter project name: `smart-academic-performance-tracker`
4. Enable Google Analytics (recommended)
5. Choose or create Analytics account
6. Click "Create project"

### 2. Add Android App

1. In Firebase Console, click "Add app" and select Android
2. Enter package name: `com.smartacademictracker`
3. Enter app nickname: `Smart Academic Performance Tracker`
4. Enter SHA-1 fingerprint (optional for now)
5. Click "Register app"

### 3. Download Configuration File

1. Download `google-services.json`
2. Place it in the `app/` directory of your project
3. **Important**: Replace the template file with your actual configuration

### 4. Enable Firebase Services

#### Authentication
1. Go to "Authentication" → "Sign-in method"
2. Enable "Email/Password" provider
3. Enable "Google" provider (optional)
4. Configure authorized domains

#### Firestore Database
1. Go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for development)
4. Select a location close to your users
5. Click "Done"

#### Analytics (Optional)
1. Go to "Analytics" → "Events"
2. Enable automatic collection
3. Configure custom events if needed

#### Crashlytics (Optional)
1. Go to "Crashlytics"
2. Enable Crashlytics
3. Follow the setup instructions

### 5. Configure Security Rules

#### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Grades - teachers can write, students can read their own
    match /grades/{gradeId} {
      allow read: if request.auth != null && 
        (resource.data.studentId == request.auth.uid || 
         resource.data.teacherId == request.auth.uid);
      allow write: if request.auth != null && 
        resource.data.teacherId == request.auth.uid;
    }
    
    // Subjects - authenticated users can read
    match /subjects/{subjectId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Audit trail - read-only for authenticated users
    match /audit_trail/{auditId} {
      allow read: if request.auth != null;
      allow write: if false; // Only server-side writes
    }
  }
}
```

### 6. Environment Configuration

Create a `local.properties` file in the root directory:
```properties
# Firebase Configuration
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_API_KEY=your-firebase-api-key

# Optional: Analytics
ANALYTICS_ENABLED=true
CRASHLYTICS_ENABLED=true
```

## 🔐 Security Best Practices

### 1. Production Security Rules
- Implement proper role-based access control
- Add data validation rules
- Set up audit logging
- Configure rate limiting

### 2. API Key Security
- Never commit API keys to version control
- Use environment variables for sensitive data
- Rotate keys regularly
- Monitor API usage

### 3. Authentication Security
- Enable email verification
- Implement password strength requirements
- Set up account lockout policies
- Monitor suspicious activity

## 🧪 Testing Configuration

### 1. Test Data Setup
```javascript
// Add test users
db.collection('users').add({
  id: 'test-student-1',
  email: 'student@test.com',
  role: 'student',
  name: 'Test Student',
  // ... other fields
});
```

### 2. Test Environment
- Use separate Firebase project for testing
- Configure test-specific security rules
- Set up automated test data cleanup

## 🚨 Troubleshooting

### Common Issues

1. **"google-services.json not found"**
   - Ensure file is in `app/` directory
   - Check file name spelling
   - Verify file is not in `.gitignore`

2. **Authentication not working**
   - Check SHA-1 fingerprint
   - Verify package name matches
   - Ensure Authentication is enabled

3. **Firestore permission denied**
   - Check security rules
   - Verify user authentication
   - Review role-based access

4. **Build errors**
   - Clean and rebuild project
   - Check Firebase SDK versions
   - Verify Gradle configuration

### Debug Steps

1. Check Firebase Console for errors
2. Review Android Studio logs
3. Test with Firebase emulator
4. Verify network connectivity

## 📞 Support

If you encounter issues:
1. Check [Firebase Documentation](https://firebase.google.com/docs)
2. Review [Android Setup Guide](https://firebase.google.com/docs/android/setup)
3. Create an issue in this repository
4. Contact Firebase Support

## 🔄 Updates

Keep your Firebase configuration updated:
- Regularly update Firebase SDK versions
- Monitor Firebase Console for updates
- Review and update security rules
- Test configuration changes in development first

---

**Important**: Never commit your actual `google-services.json` file to version control. Use the template provided and configure it locally.

# 👨‍💻 Developer Setup Guide

This guide will help developers set up the Smart Academic Performance Tracker project for development.

## 📋 Prerequisites

- **Android Studio**: Ladybug+ (latest version)
- **JDK**: Version 17 or higher
- **Android SDK**: API Level 35
- **Git**: For version control
- **Firebase Account**: For backend services

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/jungisafk/smart-academic-performance-tracker.git
cd smart-academic-performance-tracker
```

### 2. Firebase Setup
1. Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable Authentication (Email/Password, Google sign-in)
3. Enable Firestore Database
4. Enable Crashlytics and Analytics (optional)
5. Download `google-services.json` and place it in the `app/` directory
6. **Important**: Never commit your actual `google-services.json` to version control

### 3. Build the Project
```bash
./gradlew build
```

### 4. Run the App
```bash
./gradlew installDebug
```

## 🏗️ Project Structure

```
app/
├── src/main/java/com/smartacademictracker/
│   ├── data/
│   │   ├── model/              # Data models (Grade, User, etc.)
│   │   ├── repository/         # Data repositories
│   │   ├── local/             # Room database entities and DAOs
│   │   ├── network/           # Network monitoring
│   │   ├── sync/              # Sync management
│   │   ├── monitoring/        # Grade monitoring services
│   │   ├── notification/      # Notification services
│   │   └── utils/             # Utility classes
│   ├── presentation/
│   │   ├── student/           # Student screens and ViewModels
│   │   ├── teacher/           # Teacher screens and ViewModels
│   │   ├── admin/             # Admin screens and ViewModels
│   │   ├── common/            # Shared UI components
│   │   └── offline/           # Offline status screens
│   ├── navigation/            # Navigation setup
│   ├── di/                    # Dependency injection modules
│   └── utils/                 # Application utilities
├── src/main/res/              # Resources (layouts, drawables, etc.)
└── build.gradle.kts           # App-level build configuration
```

## 🔧 Development Environment

### Android Studio Configuration
1. **Import Project**: Open Android Studio and import the project
2. **Sync Gradle**: Let Android Studio sync the Gradle files
3. **Configure SDK**: Ensure Android SDK 35 is installed
4. **Set JDK**: Configure JDK 17 as the project SDK

### Gradle Configuration
- **Gradle Version**: 8.11.1
- **Android Gradle Plugin**: 8.5.2
- **Kotlin Version**: 1.9.10
- **Compose BOM**: 2024.02.00

### Dependencies
Key dependencies include:
- **Jetpack Compose**: Modern UI framework
- **Firebase**: Backend services
- **Room**: Local database
- **Dagger Hilt**: Dependency injection
- **Navigation Compose**: Navigation
- **WorkManager**: Background tasks
- **Coil**: Image loading
- **MPAndroidChart**: Charts and graphs

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Test Structure
- **Unit Tests**: `src/test/java/`
- **Instrumented Tests**: `src/androidTest/java/`
- **Test Data**: Mock data for testing

## 🔐 Security Considerations

### Firebase Security Rules
Ensure proper Firestore security rules are configured:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Implement proper role-based access control
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    // Add more rules as needed
  }
}
```

### API Keys
- Never commit API keys to version control
- Use environment variables for sensitive data
- Configure proper `.gitignore` rules

## 📱 Building for Production

### Release Build
```bash
./gradlew assembleRelease
```

### Signing Configuration
1. Generate a keystore for release signing
2. Configure signing in `app/build.gradle.kts`
3. Never commit keystore files to version control

### ProGuard/R8
- R8 is enabled for release builds
- ProGuard rules are configured in `proguard-rules.pro`
- Test release builds thoroughly

## 🐛 Debugging

### Common Issues
1. **Build Errors**: Clean and rebuild project
2. **Firebase Issues**: Check `google-services.json` configuration
3. **Sync Issues**: Verify network connectivity and Firebase rules
4. **Offline Issues**: Check Room database setup

### Debug Tools
- **Android Studio Debugger**: Step-through debugging
- **Firebase Console**: Monitor backend operations
- **Room Inspector**: Database inspection
- **Network Inspector**: Network request monitoring

## 📊 Performance Monitoring

### Firebase Analytics
- Monitor app usage and performance
- Track user engagement
- Identify performance bottlenecks

### Crashlytics
- Monitor app crashes and errors
- Get detailed crash reports
- Track app stability

## 🔄 Git Workflow

### Branch Strategy
- **main**: Production-ready code
- **develop**: Development branch
- **feature/**: Feature branches
- **hotfix/**: Hotfix branches

### Commit Guidelines
- Use conventional commit messages
- Include detailed commit descriptions
- Reference issues in commit messages

### Pull Request Process
1. Create feature branch from `develop`
2. Implement changes with tests
3. Create pull request with detailed description
4. Code review and approval
5. Merge to `develop` branch

## 📚 Documentation

### Code Documentation
- Use KDoc for public APIs
- Include usage examples
- Document complex algorithms

### API Documentation
- Document repository interfaces
- Include data model documentation
- Provide usage examples

## 🚀 Deployment

### Firebase Hosting (Optional)
- Deploy web version if needed
- Configure Firebase Hosting
- Set up CI/CD pipeline

### Play Store Deployment
- Prepare release build
- Configure Play Console
- Upload and publish app

## 🤝 Contributing

### Development Process
1. Fork the repository
2. Create feature branch
3. Implement changes with tests
4. Submit pull request
5. Address review feedback

### Code Standards
- Follow Kotlin coding conventions
- Use meaningful variable names
- Include proper error handling
- Write comprehensive tests

## 📞 Support

### Getting Help
- Check existing issues in repository
- Create new issue with detailed description
- Contact maintainers for urgent issues

### Resources
- [Android Developer Documentation](https://developer.android.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)

---

**Happy Coding! 🎉**

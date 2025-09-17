# 📚 Smart Academic Performance Tracker

A comprehensive Android application built with Kotlin and Jetpack Compose for tracking academic performance across different user roles: Students, Teachers, and Administrators.

## 🎯 Features

### 👨‍🎓 Student Features
- **Dashboard**: Overview of academic progress and quick actions
- **Grades View**: View grades for enrolled subjects
- **Subject Enrollment**: View enrolled subjects and their details
- **Performance Analytics**: Track academic performance over time

### 👨‍🏫 Teacher Features
- **Subject Management**: Manage assigned subjects
- **Grade Input**: Input student grades for quizzes, exams, activities, and projects
- **Subject Applications**: Apply for available subjects
- **Student Analytics**: View student performance in their subjects

### 👨‍💼 Admin Features
- **Subject Management**: Add, edit, and manage all subjects
- **Application Management**: Approve/reject teacher applications for subjects
- **User Management**: Oversee all users in the system
- **System Analytics**: Comprehensive system-wide analytics

## 🛠️ Tech Stack

### Frontend (Android)
- **Language**: Kotlin 2.0.20
- **UI Framework**: Jetpack Compose (BOM 2024.09.02)
- **Navigation**: Navigation Compose 2.8.0
- **State Management**: ViewModel + Kotlin Coroutines 1.9.0
- **Dependency Injection**: Hilt 2.52
- **Material Design**: Material3 Compose 1.3.0
- **Image Loading**: Coil Compose 2.7.0

### Backend (Firebase)
- **Authentication**: Firebase Authentication
- **Database**: Firebase Firestore
- **Analytics**: Firebase Analytics + Crashlytics
- **Remote Config**: Firebase Remote Config

### Build Configuration
- **Gradle**: AGP 8.5.2
- **Target SDK**: 35
- **Min SDK**: 24
- **JVM Target**: 17
- **KSP**: 2.0.21-1.0.25

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug+ (latest)
- JDK 17 or higher
- Android SDK 35
- Firebase project (see setup instructions below)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/smart-academic-tracker.git
   cd smart-academic-tracker
   ```

2. **Firebase Setup**
   - Follow the detailed instructions in `firebase-setup-instructions.md`
   - Download `google-services.json` and place it in the `app/` directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open the project in Android Studio and click Run.

## 📱 App Architecture

The app follows Clean Architecture principles with MVVM pattern:

```
app/
├── data/
│   ├── model/          # Data models (User, Subject, Grade, etc.)
│   └── repository/     # Repository implementations
├── di/                 # Dependency injection modules
├── navigation/         # Navigation setup and routes
├── presentation/       # UI layer
│   ├── auth/          # Authentication screens
│   ├── student/       # Student-specific screens
│   ├── teacher/       # Teacher-specific screens
│   └── admin/         # Admin-specific screens
└── ui/theme/          # Material3 theming
```

## 🔐 User Roles & Permissions

### Student
- View own grades and subjects
- View enrolled subjects
- Access personal analytics

### Teacher
- Input grades for assigned subjects
- Apply for available subjects
- View student performance in their subjects
- Manage their subject applications

### Admin
- Full system access
- Manage all subjects and users
- Approve/reject teacher applications
- Access system-wide analytics
- User management capabilities

## 🎨 UI/UX Features

- **Material3 Design**: Modern, accessible design system
- **Dark/Light Theme**: Automatic theme switching
- **Responsive Layout**: Optimized for phones and tablets
- **Role-based Navigation**: Different navigation flows for each user role
- **Intuitive Dashboard**: Quick access to key features
- **Real-time Updates**: Live data synchronization with Firebase

## 🔧 Configuration

### Gradle Configuration
The project uses `libs.versions.toml` for centralized dependency management:

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
compose-bom = "2024.09.02"
hilt = "2.52"
# ... other versions
```

### Firebase Security Rules
Comprehensive security rules ensure data access is properly controlled based on user roles. See `firebase-setup-instructions.md` for details.

## 📊 Data Models

### Core Models
- **User**: User information with role-based access
- **Subject**: Academic subjects with teacher assignments
- **Grade**: Student grades with different types (Quiz, Exam, Activity, etc.)
- **TeacherApplication**: Applications for subject assignments
- **Enrollment**: Student-subject enrollment records

### Grade Types
- Quiz
- Exam
- Activity
- Project
- Homework
- Participation

## 🧪 Testing

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Instrumentation Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Firebase Test Lab
The project is configured for Firebase Test Lab for comprehensive device testing.

## 📈 Analytics & Monitoring

- **Firebase Analytics**: Track user engagement and app usage
- **Firebase Crashlytics**: Monitor app stability and crashes
- **Performance Monitoring**: Track app performance metrics
- **Custom Events**: Track specific user actions and flows

## 🔒 Security

- **Firebase Authentication**: Secure user authentication
- **Firestore Security Rules**: Role-based data access control
- **Input Validation**: Client and server-side validation
- **Data Encryption**: Firebase handles data encryption at rest and in transit

## 🚀 Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Firebase App Distribution
Configure Firebase App Distribution for beta testing:
```bash
./gradlew assembleDebug appDistributionUploadDebug
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For support, email support@smartacademictracker.com or create an issue in the repository.

## 🙏 Acknowledgments

- [Firebase](https://firebase.google.com/) for backend services
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI toolkit
- [Material Design](https://material.io/design) for design system
- [Hilt](https://dagger.dev/hilt/) for dependency injection

---

Made with ❤️ for academic excellence

# Smart Academic Performance Tracker

A comprehensive Android application built with Kotlin and Jetpack Compose for managing academic performance across different user roles (Students, Teachers, and Administrators). The app provides a complete solution for academic management with role-based access control, real-time data synchronization, and modern UI/UX design.

## 🚀 Current Features

### 👨‍🎓 Student Features
- **Dashboard**: View enrolled subjects, academic progress, and quick access to key functions
- **Subject Management**: 
  - Browse available subjects filtered by year level and course
  - Apply for subjects with application tracking
  - View applied subjects with status updates (Pending/Approved/Rejected)
  - Track application history and teacher feedback
- **Grade Tracking**: View grades for quizzes, activities, and exams
- **Profile Management**: Update personal information, year level, and course details
- **Real-time Notifications**: Get notified of application status changes

### 👨‍🏫 Teacher Features
- **Dashboard**: Overview of active subjects, total students, and quick actions
- **Subject Management**:
  - Apply for available subjects
  - Manage assigned subjects
  - View subject details and student enrollment
- **Student Application Management**:
  - Review student applications for their subjects
  - Approve or reject applications with comments
  - Track application history and status
- **Grade Management**:
  - Input student grades for different assessment types
  - Calculate student averages
  - Manage grade submissions
- **Student Overview**: View enrolled students and their performance

### 👨‍💼 Admin Features
- **Dashboard**: System overview with statistics and management tools
- **Subject Management**:
  - Create and manage subjects
  - Assign subjects to year levels and courses
  - Set subject capacity and requirements
  - Edit subject details and descriptions
- **Course & Year Level Management**:
  - Create and manage courses (ICT/IT, etc.)
  - Manage year levels (1st-4th year)
  - Assign courses to year levels
- **Teacher Application Management**:
  - Review teacher applications for subjects
  - Approve or reject applications
  - Assign teachers to subjects
- **User Management**: Oversee system operations and user accounts

## 🛠️ Tech Stack

### Frontend (Android)
- **Language**: Kotlin 2.0.20
- **UI Framework**: Jetpack Compose (BOM 2024.09.02)
- **Navigation**: Navigation-Compose 2.8.0
- **State Management**: AndroidX Lifecycle ViewModel 2.8.4 + Kotlin Coroutines 1.9.0
- **Dependency Injection**: Hilt 2.52
- **Material Design**: Material3 Compose 1.3.0
- **Image Loading**: Coil-Compose 2.7.0

### Backend (Firebase)
- **Authentication**: Firebase Authentication (Email/Password)
- **Database**: Firebase Firestore with real-time synchronization
- **Analytics**: Firebase Analytics + Crashlytics
- **Storage**: Firebase Storage
- **Remote Config**: Firebase Remote Config
- **Security**: Firestore Security Rules for role-based access

### Project Configuration
- **Gradle**: AGP 8.10.1
- **JVM Target**: 17
- **KSP**: 2.0.20-1.0.25
- **Play Services**: 4.4.2

## 📱 Key Screenshots

### Authentication
- Sign In/Sign Up with role selection
- Clean, modern UI with Material3 design

### Student Interface
- Subject application with filtering by year level and course
- Applied subjects tracking with status updates
- Grade viewing and academic progress

### Teacher Interface
- Student application management
- Grade input and management
- Subject overview and student tracking

### Admin Interface
- Comprehensive subject and course management
- Teacher application approval system
- System statistics and user management

## 🏗️ Architecture

The app follows **Clean Architecture** principles with:

- **MVVM Pattern**: Separation of concerns with ViewModels
- **Repository Pattern**: Data abstraction layer for Firebase operations
- **Dependency Injection**: Hilt for managing dependencies
- **Reactive Programming**: Kotlin Flows for data streams
- **Role-based Security**: Firestore security rules for data protection

## 📁 Project Structure

```
app/src/main/java/com/smartacademictracker/
├── data/
│   ├── model/                    # Data models (User, Subject, Grade, etc.)
│   ├── repository/               # Repository implementations
│   └── utils/                    # Utility classes and sample data
├── di/                          # Dependency injection modules
├── navigation/                   # Navigation components
├── presentation/
│   ├── auth/                    # Authentication screens
│   ├── admin/                   # Admin-specific screens
│   ├── student/                 # Student-specific screens
│   ├── teacher/                 # Teacher-specific screens
│   └── common/                  # Shared UI components
└── ui/theme/                    # UI theming and styling
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 35
- Firebase project setup

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/smart-academic-performance-tracker.git
   cd smart-academic-performance-tracker
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Firebase Setup**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password)
   - Enable Firestore Database
   - Enable Analytics and Crashlytics
   - Download `google-services.json` and place it in the `app/` directory
   - Configure Firestore Security Rules (see Security section)

4. **Build and Run**
   - Sync project with Gradle files
   - Build the project
   - Run on device or emulator

## 🔧 Configuration

### Firebase Configuration
1. Create a Firebase project
2. Add your Android app to the project
3. Download `google-services.json`
4. Place it in the `app/` directory
5. Enable required Firebase services

### Firestore Security Rules
The app uses comprehensive security rules for role-based access:

```javascript
// Example security rules structure
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Student applications
    match /student_applications/{applicationId} {
      allow create: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'STUDENT';
      allow read, update: if request.auth != null && 
        (resource.data.studentId == request.auth.uid || 
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['TEACHER', 'ADMIN']);
    }
    
    // Subjects collection
    match /subjects/{subjectId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
    }
  }
}
```

### Build Configuration
The project is configured for:
- Minimum SDK: 24
- Target SDK: 35
- Compile SDK: 35
- JVM Target: 17

## 📊 Data Models

### User
- Basic user information with role-based access
- Year level and course assignment for students
- Authentication data and profile management

### Subject
- Subject details (name, code, description)
- Teacher assignment and capacity management
- Year level and course categorization
- Academic year and semester tracking

### StudentApplication
- Student subject application tracking
- Status management (Pending/Approved/Rejected)
- Teacher review and feedback system
- Application history and timestamps

### Grade
- Student performance tracking
- Different grade types (quizzes, activities, exams)
- Weighted calculations and averages
- Teacher assignment and timestamps

### Course & YearLevel
- Course management (ICT/IT, etc.)
- Year level organization (1st-4th year)
- Subject categorization and filtering

## 🎨 UI/UX Features

- **Material3 Design**: Modern, adaptive UI components
- **Dark/Light Theme**: Automatic theme switching
- **Responsive Layout**: Optimized for different screen sizes
- **Accessibility**: Screen reader support and proper contrast
- **Smooth Animations**: Delightful user interactions
- **Real-time Updates**: Live data synchronization
- **Error Handling**: Comprehensive error states and user feedback

## 🔐 Security

- **Firebase Authentication**: Secure user authentication
- **Role-based Access**: Proper permission management with Firestore rules
- **Data Validation**: Input validation and sanitization
- **Secure Storage**: Encrypted local data storage
- **API Key Protection**: Sensitive files excluded from version control

## 🧪 Testing

The project includes:
- Unit tests for ViewModels
- Integration tests for repositories
- UI tests for critical user flows
- Firebase security rules testing

## 📈 Performance

- **Lazy Loading**: Efficient list rendering
- **Image Optimization**: Coil for image loading
- **Memory Management**: Proper lifecycle handling
- **Network Optimization**: Efficient Firebase queries
- **Caching**: Local data caching for offline support
- **Background Processing**: Coroutines for async operations

## 🚧 Future Features & Roadmap

### Phase 1: Enhanced Academic Management
- [ ] **Advanced Grade Analytics**
  - Grade distribution charts and statistics
  - Performance trend analysis
  - Comparative performance reports
  - Grade prediction algorithms

- [ ] **Assignment Management**
  - Create and manage assignments
  - Due date tracking and reminders
  - File upload and submission system
  - Plagiarism detection integration

- [ ] **Attendance Tracking**
  - QR code-based attendance system
  - Attendance reports and analytics
  - Absence tracking and notifications
  - Integration with grade calculations

### Phase 2: Communication & Collaboration
- [ ] **Messaging System**
  - Teacher-student communication
  - Group messaging for classes
  - File sharing and attachments
  - Push notifications

- [ ] **Announcement System**
  - School-wide announcements
  - Class-specific notifications
  - Emergency alerts
  - Scheduled announcements

- [ ] **Discussion Forums**
  - Subject-specific discussion boards
  - Q&A sections
  - Peer-to-peer learning
  - Moderated discussions

### Phase 3: Advanced Analytics & Reporting
- [ ] **Comprehensive Reporting**
  - Academic performance reports
  - Teacher evaluation reports
  - Student progress tracking
  - Custom report generation

- [ ] **Data Visualization**
  - Interactive charts and graphs
  - Performance dashboards
  - Trend analysis tools
  - Export capabilities (PDF, Excel)

- [ ] **Predictive Analytics**
  - Student performance prediction
  - At-risk student identification
  - Course recommendation system
  - Learning outcome forecasting

### Phase 4: Mobile & Cross-Platform
- [ ] **Offline Support**
  - Complete offline functionality
  - Data synchronization when online
  - Offline grade input
  - Cached data management

- [ ] **Cross-Platform Support**
  - iOS app development
  - Web application
  - Desktop application
  - API for third-party integrations

- [ ] **Advanced Mobile Features**
  - Biometric authentication
  - Camera integration for document scanning
  - Voice notes and recording
  - Augmented reality features

### Phase 5: AI & Machine Learning
- [ ] **AI-Powered Features**
  - Intelligent grade prediction
  - Automated feedback generation
  - Smart course recommendations
  - Learning pattern analysis

- [ ] **Chatbot Integration**
  - Student support chatbot
  - FAQ automation
  - 24/7 assistance
  - Multi-language support

- [ ] **Advanced Analytics**
  - Machine learning insights
  - Behavioral analysis
  - Performance optimization
  - Personalized learning paths

### Phase 6: Enterprise & Scalability
- [ ] **Multi-Institution Support**
  - Multiple school management
  - Centralized administration
  - Cross-institution analytics
  - Federation support

- [ ] **Advanced Security**
  - Two-factor authentication
  - Single sign-on (SSO)
  - Advanced encryption
  - Compliance management

- [ ] **Integration Ecosystem**
  - Learning Management System (LMS) integration
  - Student Information System (SIS) connectivity
  - Third-party tool integrations
  - API marketplace

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Kotlin coding conventions
- Write comprehensive tests
- Update documentation
- Ensure security best practices
- Follow Material Design guidelines

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Your Name** - *Initial work* - [YourGitHub](https://github.com/yourusername)

## 🙏 Acknowledgments

- Material Design team for the amazing design system
- Jetpack Compose team for the modern UI toolkit
- Firebase team for the robust backend services
- Android community for continuous support and inspiration

## 📞 Support

If you have any questions or need help, please:
- Open an issue on GitHub
- Contact the development team
- Check the documentation
- Review the FAQ section

## 🔄 Version History

### v1.0.0 (Current)
- Initial release with core functionality
- Student, Teacher, and Admin roles
- Subject management and applications
- Grade tracking and management
- Firebase integration
- Material3 UI design

### Upcoming Releases
- v1.1.0: Enhanced analytics and reporting
- v1.2.0: Communication features
- v2.0.0: AI-powered features and advanced analytics

---

**Built with ❤️ using Kotlin and Jetpack Compose**

*Last updated: January 2025*
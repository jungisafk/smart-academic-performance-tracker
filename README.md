# Smart Academic Performance Tracker

A comprehensive Android application built with Kotlin and Jetpack Compose for managing academic grade tracking across different user roles (Students, Teachers, Administrators, and Registrar). The app enables teachers to input Prelim, Midterm, and Final grades, provides students with real-time grade viewing and performance analytics, ensures automated computation of final averages, and offers administrators comprehensive oversight of grade submissions with offline functionality for greater accessibility.

## 🚀 Current Features

### 👨‍🎓 Student Features
- **Grade Dashboard**: Real-time viewing of Prelim, Midterm, and Final grades across all subjects
- **Performance Analytics**: 
  - Automated final average computation based on teacher inputs
  - Visual performance insights with charts and graphs
  - Performance status indicators (Passing/At-Risk/Failing)
  - Historical grade trends and comparisons
- **Real-time Updates**: Instant grade notifications when teachers submit new grades
- **Offline Access**: View previously loaded grades without internet connection
- **Academic Tracking**: Monitor academic performance trends and progress over time

### 👨‍🏫 Teacher Features
- **Grade Input Dashboard**: Efficient interface for inputting Prelim, Midterm, and Final grades
- **Automated Calculations**: System automatically computes final averages using standard academic formulas
- **Grade Management**:
  - Batch grade input for entire classes
  - Individual student grade updates
  - Grade validation and error checking
  - Submission tracking and confirmation
- **Offline Functionality**: Input grades without internet connection, sync when online
- **Student Performance Overview**: Monitor student progress and identify at-risk students
- **Submission Management**: Track grade submission deadlines and completion status

### 👨‍💼 Admin & Registrar Features
- **Grade Oversight Dashboard**: Comprehensive monitoring of all grade submissions across the system
- **Submission Monitoring**:
  - Track teacher grade submission timeliness and accuracy
  - Monitor completion status of all grade entries
  - Generate compliance and quality assurance reports
- **Data Integrity Management**:
  - Audit trail of all grade changes and submissions
  - Automated validation of grade accuracy and completeness
  - Anomaly detection and quality control measures
- **Academic Administration**:
  - User management (Students, Teachers, Registrar staff)
  - Subject and course management
  - Academic period and semester setup
- **Reporting & Analytics**:
  - Comprehensive grade submission reports
  - Teacher performance and compliance metrics
  - System-wide academic performance analytics

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

### Phase 1: Enhanced Grade Analytics
- [ ] **Advanced Visual Analytics**
  - Interactive grade distribution charts and statistics
  - Performance trend analysis with detailed graphs
  - Comparative performance reports across subjects
  - Historical performance tracking and visualization

- [ ] **Enhanced Reporting System**
  - Comprehensive grade reports for students, teachers, and administrators
  - Customizable report generation with filters
  - Export capabilities (PDF, Excel, CSV)
  - Automated report scheduling and distribution

- [ ] **Improved Offline Functionality**
  - Complete offline grade input and viewing
  - Enhanced data synchronization capabilities
  - Conflict resolution for offline changes
  - Improved offline performance and reliability

### Phase 2: System Enhancement & User Experience
- [ ] **Enhanced User Interface**
  - Improved user experience and navigation
  - Advanced accessibility features
  - Customizable dashboard layouts
  - Enhanced mobile responsiveness

- [ ] **Communication Features**
  - Grade notification system
  - Teacher-student messaging for grade inquiries
  - System announcements and updates
  - Email integration for important notifications

- [ ] **Advanced Security & Compliance**
  - Enhanced data encryption and security measures
  - Audit trail improvements
  - Compliance with academic data protection standards
  - Advanced user authentication options

### Phase 3: Cross-Platform & Integration
- [ ] **Multi-Platform Support**
  - Web application for desktop access
  - Tablet-optimized interface
  - Cross-platform data synchronization
  - Responsive design for all devices

- [ ] **System Integration**
  - Integration with existing Student Information Systems (SIS)
  - Export capabilities to external academic systems
  - API development for third-party integrations
  - Backup and recovery system enhancements

- [ ] **Advanced Analytics**
  - Comprehensive academic performance analytics
  - Teacher effectiveness metrics
  - System usage and performance monitoring
  - Data-driven insights for academic improvement

### Phase 4: Enterprise & Scalability
- [ ] **Multi-Institution Support**
  - Multiple school management capabilities
  - Centralized administration dashboard
  - Cross-institution grade analytics
  - Standardized reporting across institutions

- [ ] **Advanced Security & Compliance**
  - Two-factor authentication implementation
  - Single sign-on (SSO) integration
  - Advanced data encryption standards
  - Educational data compliance (FERPA, etc.)

- [ ] **Performance & Scalability**
  - System performance optimization for large user bases
  - Advanced caching and data management
  - Load balancing and high availability
  - Comprehensive monitoring and alerting systems

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
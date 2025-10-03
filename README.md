# 📚 Smart Academic Performance Tracker

A comprehensive Android application for tracking and managing academic performance across educational institutions. Built with modern Android development practices using Jetpack Compose, Firebase, and MVVM architecture.

## 🚀 Features

### 👨‍🎓 Student Features
- **Real-time Dashboard**: Live grade updates with Prelim/Midterm/Final grade display
- **Performance Tracking**: Historical grade trends and performance comparisons
- **Grade Analytics**: Visual insights and performance indicators
- **Real-time Notifications**: Instant alerts for grade updates
- **Offline Access**: View grades even without internet connection

### 👨‍🏫 Teacher Features
- **Enhanced Grade Input**: Period-specific grade entry (Prelim/Midterm/Final)
- **Batch Grade Input**: Efficient class-wide grade entry
- **Grade Validation**: Comprehensive validation with 0-100 range checking
- **Submission Tracking**: Monitor grade submission status
- **Offline Grade Input**: Local storage with automatic sync
- **Performance Analytics**: Class performance insights

### 👨‍💼 Admin Features
- **Comprehensive Oversight**: System-wide grade monitoring dashboard
- **User Management**: Enhanced student/teacher administration
- **Academic Period Setup**: Semester/year configuration
- **System Configuration**: App-wide settings and parameters
- **Audit Trail**: Complete history of all grade changes
- **Quality Assurance**: Data validation and anomaly detection

## 🏗️ Architecture

### Tech Stack
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Dagger Hilt
- **Backend**: Firebase (Firestore, Authentication, Analytics)
- **Local Storage**: Room Database
- **Navigation**: Navigation Compose
- **Async Operations**: Kotlin Coroutines & Flow
- **Image Loading**: Coil
- **Charts**: MPAndroidChart

### Key Components
- **Grade Calculation Engine**: Centralized calculation service with standard academic formula
- **Offline Support**: Complete offline functionality with conflict resolution
- **Audit Trail System**: Comprehensive tracking of all grade changes
- **Real-time Sync**: Automatic synchronization between local and remote data
- **Role-based Access**: Secure access control for different user types

## 📊 Grade Structure

### Grade Periods
- **Preliminary**: 30% weight
- **Midterm**: 30% weight  
- **Final**: 40% weight

### Grade Status
- **INCOMPLETE**: Missing grades
- **PASSING**: ≥75% average
- **AT_RISK**: 60-74% average
- **FAILING**: <60% average

### Calculation Formula
```
Final Average = (Prelim × 0.30) + (Midterm × 0.30) + (Final × 0.40)
```

## 🔧 Setup Instructions

### Prerequisites
- Android Studio Ladybug+ (latest)
- JDK 17
- Android SDK 35
- Firebase project

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/smart-academic-performance-tracker.git
   cd smart-academic-performance-tracker
   ```

2. **Firebase Setup**
   - Create a new Firebase project
   - Enable Authentication (Email/Password, Google sign-in)
   - Enable Firestore Database
   - Enable Crashlytics and Analytics
   - Download `google-services.json` and place it in the `app/` directory

3. **Build the project**
   ```bash
   ./gradlew build
   ```

## 📱 Screenshots

*Screenshots will be added here*

## 🗂️ Project Structure

```
app/
├── src/main/java/com/smartacademictracker/
│   ├── data/
│   │   ├── model/              # Data models
│   │   ├── repository/         # Data repositories
│   │   ├── local/             # Room database
│   │   ├── network/           # Network monitoring
│   │   └── sync/              # Sync management
│   ├── presentation/
│   │   ├── student/           # Student screens
│   │   ├── teacher/           # Teacher screens
│   │   ├── admin/             # Admin screens
│   │   └── common/            # Shared components
│   ├── navigation/            # Navigation setup
│   ├── di/                    # Dependency injection
│   └── utils/                 # Utility classes
├── src/main/res/              # Resources
└── build.gradle.kts           # App-level build configuration
```

## 🔐 Security Features

- **Role-based Access Control**: Secure user authentication and authorization
- **Data Validation**: Server-side and client-side validation
- **Audit Logging**: Complete audit trail for all grade changes
- **Secure Storage**: Encrypted local storage for sensitive data
- **Firebase Security Rules**: Comprehensive Firestore security rules

## 🚀 Performance Features

- **Offline Support**: Complete offline functionality
- **Background Sync**: Automatic data synchronization
- **Caching Strategy**: Efficient data caching for performance
- **Optimized Queries**: Efficient Firestore operations
- **Memory Management**: Proper lifecycle management

## 🧪 Testing

The project includes comprehensive testing:
- **Unit Tests**: Grade calculation engine tests
- **Integration Tests**: Repository and ViewModel tests
- **UI Tests**: Critical user flow testing
- **Firebase Test Lab**: Device testing automation

## 📈 Roadmap

### Completed Features ✅
- [x] Core grade structure refactoring
- [x] Student dashboard with real-time updates
- [x] Teacher grade input interface
- [x] Admin oversight dashboard
- [x] Offline functionality
- [x] Audit trail system
- [x] Performance analytics

### Upcoming Features 🚧
- [ ] Advanced reporting system
- [ ] Grade distribution analytics
- [ ] Parent portal integration
- [ ] Mobile app for parents
- [ ] Advanced notification system

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Lead Developer**: [Your Name]
- **UI/UX Designer**: [Designer Name]
- **Backend Developer**: [Backend Developer Name]

## 📞 Support

For support, email support@smartacademictracker.com or create an issue in this repository.

## 🙏 Acknowledgments

- Firebase team for excellent backend services
- Jetpack Compose team for modern UI framework
- Android community for continuous support and feedback

---

**Note**: This project is for educational purposes. Ensure you have proper Firebase configuration and security rules in place before deploying to production.
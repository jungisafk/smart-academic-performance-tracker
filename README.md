# Smart Academic Performance Tracker

A comprehensive Android application for managing academic performance, built with Kotlin, Jetpack Compose, and Firebase.

**Author**: jungisafk  
**Last Updated**: November 13, 2025

## 🎯 Project Overview

The Smart Academic Performance Tracker is a modern educational management system designed to streamline academic operations for students, teachers, and administrators. The app provides a complete solution for grade management, academic period tracking, and institutional administration with a secure ID-based authentication system.

## 🏗️ Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Backend**: Firebase (Authentication, Firestore, Storage)
- **Navigation**: Navigation Compose
- **State Management**: StateFlow, Flow
- **Offline Support**: Room Database with sync capabilities

## 🔐 Authentication System

### ID-Based Authentication
The app uses institutional ID-based authentication instead of email addresses:

- **Student ID Format**: `YYYY-NNNN` (e.g., `2024-2563`)
  - YYYY = Enrollment year
  - NNNN = 4-digit unique number
  
- **Teacher ID Format**: `T-YYYY-NNN` (e.g., `T-2024-001`)
  - T = Teacher prefix
  - YYYY = Year
  - NNN = 3-digit unique number

- **Admin ID Format**: `ADMIN-YYYY-NNN` (e.g., `ADMIN-2024-001`)

### Account Activation Workflow
1. **Pre-Registration**: Admin pre-registers students/teachers with their institutional IDs
2. **Account Activation**: Users activate their accounts by entering their ID and creating a password
3. **Auto-Population**: System automatically populates name, course, year level, and other details
4. **Sign In**: Users sign in using their ID and password

### Password Requirements
- At least 8 characters
- One uppercase letter (A-Z)
- One lowercase letter (a-z)
- One number (0-9)
- Real-time password strength validation with visual checklist

## 🚀 Key Features

### 👨‍🎓 Student Features
- **Dashboard**: Personal academic overview with grade summaries and quick actions
- **Grade Tracking**: View grades by subject with detailed breakdowns (Prelim, Midterm, Final)
- **Subject Management**: Browse available subjects and enrollment status
- **Application System**: Apply for subjects with status tracking
- **Performance Analytics**: Grade history, comparison, and study progress with visual charts
- **Profile Management**: Personal information and academic records
- **Notifications**: Real-time notification system with unread count badges

### 👨‍🏫 Teacher Features
- **Dashboard**: Teaching overview with subject statistics and student counts
- **Grade Input**: Comprehensive grade management system with student list view and dialog-based input
- **Subject Management**: 
  - View assigned subjects with section details
  - Apply for available subjects
  - Track application status
- **Student Management**: 
  - View sections and enrolled students
  - Manage student applications (approve/reject)
  - Remove students from sections with reason tracking
- **Analytics**: Class performance analytics with filters and visualizations
- **Notifications**: Real-time notifications with badge counts

### 👨‍💼 Admin Features
- **Dashboard**: System overview with comprehensive statistics and quick actions
- **Pre-Registration Management**: 
  - Pre-register students and teachers
  - Track activation status (Pending/Activated)
  - Filter and search pre-registered users
- **Academic Structure**: Manage courses, year levels, and subjects
- **Teacher Management**: 
  - Review teacher applications
  - Assign teachers to subject sections
  - Manage section assignments
- **Academic Periods**: Create and manage academic calendars with data viewer
- **Application Management**: Review and process all student and teacher applications
- **Grade Monitoring**: System-wide grade analytics
- **Data Management**: User management and system maintenance

## 📱 User Roles & Permissions

### Student Role
- View personal grades and academic progress
- Apply for subjects
- Track application status
- Access performance analytics
- View notifications

### Teacher Role
- Input and manage grades (Prelim, Midterm, Final)
- Review and approve/reject student applications
- Manage assigned sections and students
- View class analytics
- Apply for available subjects
- View notifications

### Admin Role
- Full system access
- Pre-register students and teachers
- Manage academic structure (courses, year levels, subjects)
- Assign teachers to sections
- Review all applications
- Manage academic periods
- System configuration and data maintenance

## 🛠️ Technical Implementation

### Data Models
- **User**: Authentication and profile management with ID fields
- **PreRegisteredStudent**: Pre-registration data for students
- **PreRegisteredTeacher**: Pre-registration data for teachers
- **LoginAttempt**: Security tracking for failed login attempts
- **Subject**: Course and subject information
- **Grade**: Grade tracking with multiple periods
- **Enrollment**: Student-subject relationships
- **AcademicPeriod**: Academic calendar management
- **Application**: Student and teacher applications
- **SectionAssignment**: Teacher-section assignments

### Key Components
- **Repository Pattern**: Centralized data access
- **Offline-First**: Room database with Firebase sync
- **Security**: Comprehensive Firestore security rules with role-based access
- **Validation**: 
  - ID format validation (Student/Teacher/Admin)
  - Password strength validation with real-time feedback
  - Rate limiting and account lockout protection
- **Error Handling**: Comprehensive error management
- **Logging**: Debug and production logging

## 🔧 Setup & Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Kotlin 1.8.0+
- JDK 11+
- Firebase project setup

### Installation Steps
1. Clone the repository
2. Open in Android Studio
3. Configure Firebase:
   - Add `google-services.json` to `app/` directory (not included in repository for security)
   - Configure Firestore security rules
   - Set up Firebase Authentication
4. Build and run the project

### Firebase Configuration
- **Authentication**: ID-based authentication with email mapping
- **Firestore**: Database with comprehensive security rules
- **Storage**: File upload capabilities
- **Security Rules**: Role-based access control (Student, Teacher, Admin)

### Important Notes
- `google-services.json` is excluded from the repository for security
- `firestore.rules` is excluded from the repository
- Node.js scripts (`.js` files) are excluded as they may contain credentials
- Service account keys are excluded

## 📊 Current Status

### ✅ Completed Features
- [x] ID-based authentication system (Student ID, Teacher ID, Admin ID)
- [x] Pre-registration and account activation workflow
- [x] Password validation with real-time checklist
- [x] User authentication and role management
- [x] Student dashboard with grade tracking
- [x] Teacher dashboard with subject overview
- [x] Teacher grade input system with student list view
- [x] Teacher student management (sections and applications)
- [x] Teacher analytics with filters and visualizations
- [x] Admin dashboard with comprehensive statistics
- [x] Admin pre-registration management (students and teachers)
- [x] Admin teacher management and section assignments
- [x] Admin academic periods management with data viewer
- [x] Application system for students and teachers
- [x] Academic periods management
- [x] Notification system with unread counts
- [x] Offline-first architecture
- [x] Data validation and cleanup
- [x] Real-time data synchronization
- [x] UI/UX improvements across all dashboards

### 🔄 In Progress
- [ ] Advanced analytics and reporting
- [ ] Mobile app optimization
- [ ] Performance optimization for large datasets

### 📋 Future Enhancements
- [ ] Real-time push notifications
- [ ] Advanced reporting and exports
- [ ] Mobile app store deployment
- [ ] Additional export formats (Excel, PDF)
- [ ] Advanced user management features
- [ ] System backup automation
- [ ] Bulk operations for admin

## 🐛 Known Issues & Fixes

### Recent Fixes (November 2025)
- ✅ Implemented ID-based authentication system
- ✅ Fixed Quick Actions button visibility on Teacher Dashboard
- ✅ Fixed Subject Management card text overflow
- ✅ Improved Teacher Analytics UI consistency
- ✅ Enhanced Teacher Student Management with modern design
- ✅ Fixed Firebase permission errors for admin operations
- ✅ Fixed pre-registered user filter functionality
- ✅ Improved password validation UI with real-time checklist
- ✅ Updated student ID format (YYYY-NNNN)
- ✅ Fixed account activation workflow
- ✅ Enhanced admin dashboard UI and navigation
- ✅ Fixed teacher application visibility issues
- ✅ Improved section assignment functionality

### Current Issues
- 🔧 Some deprecated API warnings (non-critical)
- 🔧 Performance optimization for large datasets in progress

## 🧪 Testing

### Test Coverage
- Unit tests for ViewModels
- Repository testing
- UI component testing
- Integration testing with Firebase
- Authentication flow testing

### Manual Testing
- ID-based authentication workflow
- Account activation process
- User role functionality
- Grade input and calculation
- Application workflow
- Data synchronization
- Offline functionality
- Pre-registration and activation

## 📈 Performance Metrics

- **Build Time**: ~45 seconds
- **App Size**: Optimized for production
- **Memory Usage**: Efficient state management
- **Network**: Offline-first with smart sync
- **Database**: Optimized Firestore queries with proper indexing

## 🔒 Security

- **Authentication**: ID-based authentication with Firebase Auth
- **Rate Limiting**: 5 failed login attempts before account lockout
- **Data Validation**: Server-side and client-side validation
- **Security Rules**: Comprehensive Firestore security with role-based access
- **Data Integrity**: Audit trails and validation
- **Privacy**: No sensitive data exposure (credentials excluded from repository)

## 📚 Documentation

- **ID-Based Authentication Guide**: `ID_BASED_AUTHENTICATION_IMPLEMENTATION_GUIDE.md`
- **Implementation Summary**: `IMPLEMENTATION_SUMMARY.md`
- **ID Format Reference**: `ID_FORMAT_REFERENCE.md`
- **Testing Guide**: `TESTING_GUIDE_ID_AUTHENTICATION.md`
- **API Documentation**: Comprehensive code documentation
- **User Guides**: Role-specific usage instructions
- **Developer Guide**: Setup and contribution guidelines
- **Architecture**: System design and patterns

## 🤝 Contributing

### Development Guidelines
1. Follow Kotlin coding standards
2. Use Jetpack Compose best practices
3. Implement proper error handling
4. Add comprehensive logging
5. Write unit tests for new features
6. Update documentation
7. Ensure sensitive files are excluded (see `.gitignore`)

### Code Review Process
1. Feature branch creation
2. Code review and testing
3. Merge to main branch
4. Documentation updates

## 📞 Support

For technical support or questions:
- Check the documentation files
- Review known issues
- Create an issue in the repository
- Contact the development team

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🎉 Acknowledgments

- Firebase team for excellent backend services
- Jetpack Compose team for modern UI framework
- Android community for best practices
- Educational institutions for feedback and requirements

---

**Last Updated**: November 13, 2025  
**Version**: 2.0.0  
**Status**: Production Ready

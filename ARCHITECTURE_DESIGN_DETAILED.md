# Smart Academic Performance Tracker - Architecture Design

## Figure 1: System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USER ROLES LAYER                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────┐      ┌──────────┐      ┌──────────┐                         │
│   │  Admin   │      │ Teacher │      │ Student │                         │
│   └────┬─────┘      └────┬─────┘      └────┬─────┘                         │
│        │                  │                  │                               │
│        └──────────────────┼──────────────────┘                               │
│                           │                                                  │
│                           ▼                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              │ Access
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      ANDROID APPLICATION LAYER                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────┐          │
│   │              Presentation Layer (UI)                         │          │
│   │  • Admin Screens (Dashboard, User Management, etc.)        │          │
│   │  • Teacher Screens (Subjects, Grades, Students)            │          │
│   │  • Student Screens (Grades, Subjects, Profile)             │          │
│   └───────────────────────┬─────────────────────────────────────┘          │
│                           │                                                  │
│                           ▼                                                  │
│   ┌─────────────────────────────────────────────────────────────┐          │
│   │              ViewModel Layer (Business Logic)               │          │
│   │  • AdminViewModel, TeacherViewModel, StudentViewModel      │          │
│   │  • ProfileViewModel, NotificationViewModel                 │          │
│   │  • State management and UI logic                           │          │
│   └───────────────────────┬─────────────────────────────────────┘          │
│                           │                                                  │
│                           ▼                                                  │
│   ┌─────────────────────────────────────────────────────────────┐          │
│   │              Repository Layer (Data Access)                 │          │
│   │  • UserRepository, SubjectRepository, GradeRepository      │          │
│   │  • EnrollmentRepository, NotificationRepository            │          │
│   │  • AcademicPeriodRepository, SectionAssignmentRepository   │          │
│   └───────────────────────┬─────────────────────────────────────┘          │
│                           │                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              │ API Calls
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FIREBASE SERVICES LAYER                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐       │
│   │  Authentication │  │    Firestore     │  │ Cloud Messaging  │       │
│   │  • User Login   │  │  • Real-time DB  │  │  • Push Notif    │       │
│   │  • Password Reset│  │  • Collections   │  │  • In-app Notif  │       │
│   └──────────────────┘  └──────────────────┘  └──────────────────┘       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
                              │ Data Operations
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          FIRESTORE DATABASE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Collections:                                                              │
│   • users                    • student_enrollments                          │
│   • subjects                 • section_assignments                          │
│   • courses                  • academic_periods                             │
│   • year_levels              • student_applications                         │
│   • grades                   • teacher_applications                         │
│   • notifications            • pre_registered_students                      │
│   • audit_trail              • pre_registered_teachers                      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Architecture Components

### 1. User Roles Layer
- **Admin**: Full system access, user management, academic period management
- **Teacher**: Subject management, grade input, student enrollment management
- **Student**: View grades, apply for subjects, view notifications

### 2. Android Application Layer

#### Presentation Layer (UI)
- **Admin Screens**: Dashboard, User Management, Academic Management, Grade Monitoring
- **Teacher Screens**: My Subjects, Grade Input, Student Management, Analytics
- **Student Screens**: Home, Subjects, Grades, Profile, Notifications
- Built with **Jetpack Compose** for modern UI

#### ViewModel Layer (Business Logic)
- Manages UI state and business logic
- Handles user interactions and data flow
- Uses **Hilt** for dependency injection
- Examples:
  - `AdminDashboardViewModel`
  - `TeacherGradeInputViewModel`
  - `StudentGradesViewModel`
  - `ProfileViewModel`
  - `NotificationViewModel`

#### Repository Layer (Data Access)
- Abstracts data source operations
- Provides clean API for ViewModels
- Handles Firestore operations
- Examples:
  - `UserRepository`
  - `SubjectRepository`
  - `GradeRepository`
  - `StudentEnrollmentRepository`
  - `NotificationRepository`

### 3. Firebase Services Layer

#### Firebase Authentication
- User authentication (ID-based login)
- Password reset functionality
- Session management

#### Firestore Database
- Real-time data synchronization
- NoSQL document database
- Collections for all entities
- Security rules for access control

#### Firebase Cloud Messaging
- Push notifications
- In-app notifications
- Notification delivery tracking

### 4. Database Layer (Firestore)

#### Main Collections:
1. **users** - User accounts (Admin, Teacher, Student)
2. **subjects** - Academic subjects/courses
3. **courses** - Academic programs/departments
4. **year_levels** - Year level classifications
5. **grades** - Student grade records
6. **student_enrollments** - Student enrollment records
7. **section_assignments** - Teacher-to-section assignments
8. **academic_periods** - Academic period/semester management
9. **notifications** - System notifications
10. **audit_trail** - Grade modification audit logs
11. **student_applications** - Student subject applications
12. **teacher_applications** - Teacher subject applications
13. **pre_registered_students** - Pre-registration data for students
14. **pre_registered_teachers** - Pre-registration data for teachers

## Data Flow

1. **User Action**: Admin/Teacher/Student performs an action in the UI
2. **ViewModel Processing**: ViewModel handles the action and calls appropriate Repository
3. **Repository Operation**: Repository executes Firestore operations
4. **Firebase Service**: Firebase handles authentication, database operations, or messaging
5. **Database Update**: Firestore database is updated
6. **Real-time Sync**: Changes are synchronized back to the app via listeners
7. **UI Update**: ViewModel updates UI state, triggering recomposition

## Key Features

- **MVVM Architecture**: Separation of concerns with ViewModels managing business logic
- **Real-time Updates**: Firestore listeners provide live data synchronization
- **Role-based Access**: Different UI and permissions for Admin, Teacher, and Student
- **Offline Support**: Firestore provides offline persistence
- **Push Notifications**: Firebase Cloud Messaging for system notifications
- **Security**: Firestore security rules enforce access control
- **Dependency Injection**: Hilt for clean dependency management

## Technology Stack

- **Frontend**: Android (Kotlin), Jetpack Compose
- **Backend**: Firebase (Firestore, Authentication, Cloud Messaging)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **State Management**: StateFlow, Flow
- **Navigation**: Jetpack Navigation Component
- **Database**: Firestore (NoSQL)


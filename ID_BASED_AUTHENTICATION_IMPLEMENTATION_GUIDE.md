# Student/Teacher ID-Based Authentication - Implementation Guide

**Project**: Smart Academic Performance Tracker  
**Date**: November 12, 2025  
**Implementation Status**: Phase 1 Complete (Core Backend & Models)

---

## 📋 Implementation Overview

This document outlines the implementation of ID-based authentication for students and teachers, allowing them to use their institutional IDs (e.g., Student ID: "2024-12345", Teacher ID: "T-2024-001") instead of email addresses for authentication.

---

## ✅ COMPLETED COMPONENTS

### 1. **Data Models** ✓

#### PreRegisteredStudent Model
- Location: `app/src/main/java/com/smartacademictracker/data/model/PreRegisteredStudent.kt`
- Purpose: Stores student information before account activation
- Key Fields:
  - `studentId`: Unique institutional ID
  - Personal information (firstName, lastName, middleName, suffix)
  - Academic information (courseId, yearLevelId, section, enrollmentYear)
  - Registration status tracking

#### PreRegisteredTeacher Model
- Location: `app/src/main/java/com/smartacademictracker/data/model/PreRegisteredTeacher.kt`
- Purpose: Stores teacher information before account activation
- Key Fields:
  - `teacherId`: Unique institutional ID
  - Personal information
  - Department and employment information
  - Registration status tracking

#### Updated User Model
- Location: `app/src/main/java/com/smartacademictracker/data/model/User.kt`
- **New Fields Added**:
  - `studentId`, `teacherId`, `employeeId`
  - `middleName`, `suffix`
  - `section`, `enrollmentYear` (students)
  - `employmentType`, `position`, `specialization`, `dateHired` (teachers)
  - `lastLoginAt`, `passwordChangedAt`, `mustChangePassword`
  - `accountSource` ("MANUAL" or "PRE_REGISTERED")

#### LoginAttempt Model
- Location: `app/src/main/java/com/smartacademictracker/data/model/LoginAttempt.kt`
- Purpose: Track failed login attempts for security
- Features: Rate limiting, account lockout

#### Updated OfflineUser Entity
- Location: `app/src/main/java/com/smartacademictracker/data/local/entity/OfflineUser.kt`
- Updated to include all new ID-based authentication fields

---

### 2. **Validation Utilities** ✓

#### IdValidator
- Location: `app/src/main/java/com/smartacademictracker/util/IdValidator.kt`
- **Features**:
  - Validates Student ID format: `YYYY-NNNNN` (e.g., "2024-12345")
  - Validates Teacher ID format: `T-YYYY-NNN` (e.g., "T-2024-001") or `EMP-NNNNN`
  - Extracts year from IDs
  - Generates next sequential IDs
  - Format detection helpers

#### PasswordValidator
- Location: `app/src/main/java/com/smartacademictracker/util/PasswordValidator.kt`
- **Requirements**:
  - Minimum 8 characters
  - At least one uppercase letter
  - At least one lowercase letter
  - At least one number
  - At least one special character
  - Common password check
  - Sequential characters check
- **Features**:
  - Password strength scoring (0-100)
  - Strength labels (Weak, Fair, Good, Strong, Very Strong)
  - Detailed error messages

---

### 3. **Repository Layer** ✓

#### PreRegisteredRepository
- Location: `app/src/main/java/com/smartacademictracker/data/repository/PreRegisteredRepository.kt`
- **Student Operations**:
  - Get pre-registered student by ID
  - Get all/filter pre-registered students
  - Add/Update/Delete pre-registered students
  - Bulk import students
  - Mark as registered
- **Teacher Operations**:
  - Similar CRUD operations for teachers
  - Bulk import teachers
  - Mark as registered
- **Common Operations**:
  - Check if ID exists

#### LoginAttemptTracker
- Location: `app/src/main/java/com/smartacademictracker/data/repository/LoginAttemptTracker.kt`
- **Features**:
  - Check if login is allowed
  - Record failed attempts
  - Clear attempts on success
  - Account lockout (30 minutes after 5 failed attempts)
  - Auto-reset after 15 minutes of inactivity
  - Admin functions (unlock accounts, view locked accounts)

#### Updated UserRepository
- Location: `app/src/main/java/com/smartacademictracker/data/repository/UserRepository.kt`
- **New Methods**:
  - `signInWithId()`: Sign in using institutional ID
  - `activateAccount()`: First-time account activation
  - `convertIdToEmail()`: Convert ID to email format for Firebase
  - `createUserFromPreRegStudent()`: Create user from pre-reg data
  - `createUserFromPreRegTeacher()`: Create user from pre-reg data
  - `checkUserIdExists()`: Validate ID exists in pre-registration
  - `getUserByInstitutionalId()`: Get user by student/teacher ID

---

### 4. **ViewModel Layer** ✓

#### Updated AuthViewModel
- Location: `app/src/main/java/com/smartacademictracker/presentation/auth/AuthViewModel.kt`
- **New Methods**:
  - `signInWithId()`: Handle ID-based sign-in
  - `activateAccount()`: Handle account activation
  - `checkUserIdExists()`: Validate ID
  - `clearAccountActivatedFlag()`: Clear activation state
- **Updated State**:
  - Added `isAccountActivated` flag

---

### 5. **UI Layer** ✓

#### AccountActivationScreen
- Location: `app/src/main/java/com/smartacademictracker/presentation/auth/AccountActivationScreen.kt`
- **Features**:
  - User type selection (Student/Teacher)
  - Institutional ID input with validation
  - Password creation with strength indicator
  - Password confirmation
  - Real-time validation feedback
  - Beautiful, modern UI design
  - Success handling with auto-redirect

---

### 6. **Security** ✓

#### Firestore Security Rules
- Location: `firestore_security_rules_id_auth.rules`
- **Key Rules**:
  - Pre-registered collections: Admin-only access
  - Login attempts: Public (for rate limiting)
  - Users: Self-read, restricted write
  - Role-based access for all other collections
  - Comprehensive security for grades, enrollments, applications

---

## 🔄 IMPLEMENTATION FLOW

### Account Activation Flow
```
1. Admin pre-registers student/teacher
   ↓
2. Student/Teacher visits AccountActivationScreen
   ↓
3. Enters institutional ID (validated against pre-registration)
   ↓
4. Creates password (validated for strength)
   ↓
5. System creates Firebase Auth account
   ↓
6. System creates User document with pre-registered data
   ↓
7. System marks as registered in pre-registration collection
   ↓
8. User redirected to sign-in screen
```

### Sign-In Flow with Rate Limiting
```
1. User enters institutional ID and password
   ↓
2. System checks if account is locked (rate limiting)
   ↓
3. ID converted to email format for Firebase
   ↓
4. Firebase authentication
   ↓
5. On success: Clear login attempts, update last login
   ↓
6. On failure: Record failed attempt, show remaining attempts
   ↓
7. After 5 failed attempts: Lock account for 30 minutes
```

---

## 🎯 ID FORMAT STANDARDS

### Student ID Format
- **Pattern**: `YYYY-NNNNN`
- **Example**: `2024-12345`
- **Parts**:
  - `YYYY`: 4-digit enrollment year
  - `NNNNN`: 5-digit sequential number

### Teacher ID Format (Option 1)
- **Pattern**: `T-YYYY-NNN`
- **Example**: `T-2024-001`
- **Parts**:
  - `T`: Teacher prefix
  - `YYYY`: 4-digit hiring year
  - `NNN`: 3-digit sequential number

### Teacher ID Format (Option 2)
- **Pattern**: `EMP-NNNNN`
- **Example**: `EMP-12345`
- **Parts**:
  - `EMP`: Employee prefix
  - `NNNNN`: 5-digit employee number

---

## 📝 REMAINING TASKS

### Priority 1: Core Functionality

#### 1. Modify SignInScreen (TODO #10)
- Update to accept institutional ID instead of email
- Add user type selector (Student/Teacher)
- Implement ID validation
- Add "Activate Account" link
- Display rate limiting messages
- **Estimated Time**: 2-3 hours

#### 2. Update Navigation (TODO #15)
- Add AccountActivationScreen to navigation graph
- Update deep linking if applicable
- Test navigation flow
- **Estimated Time**: 1 hour

### Priority 2: Admin Management

#### 3. AdminPreRegisteredStudentsScreen (TODO #12)
- Display list of pre-registered students
- Filter by status (registered/pending)
- Add new student form
- Edit student information
- Delete students
- View registration status
- Bulk import interface
- **Estimated Time**: 6-8 hours

#### 4. AdminPreRegisteredTeachersScreen (TODO #13)
- Similar to student screen but for teachers
- Department-specific filtering
- Employment type management
- **Estimated Time**: 6-8 hours

#### 5. CSV/Excel Import (TODO #14)
- File upload interface
- CSV parsing
- Data validation
- Bulk import processing
- Error handling and reporting
- Import result summary
- **Estimated Time**: 4-6 hours

### Priority 3: Testing & Documentation

#### 6. Testing (TODO #18)
- Unit tests for validators
- Integration tests for repositories
- UI tests for screens
- Test with sample data
- Load testing for bulk import
- **Estimated Time**: 8-10 hours

---

## 🔧 CONFIGURATION REQUIRED

### Firebase Configuration
1. **Update School Domain**:
   ```kotlin
   // In UserRepository.kt
   private const val SCHOOL_DOMAIN = "smartacademic.edu"  // Change to your institution
   ```

2. **Deploy Firestore Rules**:
   ```bash
   firebase deploy --only firestore:rules
   ```

3. **Configure Firebase Console**:
   - Enable Email/Password authentication
   - Set up email templates (optional)

### Database Initialization
1. **Create Initial Collections**:
   - `pre_registered_students`
   - `pre_registered_teachers`
   - `login_attempts`

2. **Create Firestore Indexes** (if needed for queries)

---

## 📊 SAMPLE DATA FOR TESTING

### Sample Pre-Registered Student
```kotlin
PreRegisteredStudent(
    studentId = "2024-00001",
    firstName = "Juan",
    lastName = "Dela Cruz",
    middleName = "Santos",
    courseId = "course_001",
    courseName = "Bachelor of Science in Information Technology",
    courseCode = "BSIT",
    yearLevelId = "year_001",
    yearLevelName = "1st Year",
    section = "A",
    enrollmentYear = "2024-2025",
    createdBy = "admin_uid",
    createdByName = "Admin User",
    isRegistered = false
)
```

### Sample Pre-Registered Teacher
```kotlin
PreRegisteredTeacher(
    teacherId = "T-2024-001",
    firstName = "Maria",
    lastName = "Santos",
    middleName = "Reyes",
    departmentCourseId = "course_001",
    departmentCourseName = "Information Technology",
    departmentCourseCode = "IT",
    employmentType = EmploymentType.FULL_TIME,
    position = "Associate Professor",
    specialization = "Software Engineering",
    createdBy = "admin_uid",
    createdByName = "Admin User",
    isRegistered = false
)
```

---

## 🛡️ SECURITY CONSIDERATIONS

### Password Security
- ✅ Minimum 8 characters with complexity requirements
- ✅ Password strength indicator
- ✅ Common password blocking
- ✅ Sequential character detection

### Account Security
- ✅ Rate limiting (5 attempts)
- ✅ Account lockout (30 minutes)
- ✅ Attempt reset (15 minutes inactivity)
- ✅ Last login tracking

### Data Security
- ✅ Firebase Authentication
- ✅ Firestore Security Rules
- ✅ Role-based access control
- ✅ Audit trail (createdBy, updatedBy)

### ID Security
- ✅ ID format validation
- ✅ Cannot change ID after registration
- ✅ Duplicate ID prevention
- ✅ Pre-registration required

---

## 📚 USAGE GUIDE

### For Administrators

#### Adding a Pre-Registered Student
1. Access Admin Panel
2. Navigate to "Pre-Registered Students"
3. Click "Add Student"
4. Fill in student information
5. System generates or validates student ID
6. Save student record
7. Student can now activate their account

#### Bulk Import Students
1. Prepare CSV file with columns:
   - studentId, firstName, lastName, courseId, yearLevelId, enrollmentYear
2. Navigate to "Pre-Registered Students"
3. Click "Import from CSV"
4. Select file
5. Review import preview
6. Confirm import
7. View import results

### For Students

#### First-Time Account Activation
1. Visit the app
2. Click "Activate Account"
3. Select "Student"
4. Enter Student ID (provided by institution)
5. Create a strong password
6. Confirm password
7. Click "Activate Account"
8. Redirected to sign-in

#### Regular Sign-In
1. Visit the app
2. Select "Student"
3. Enter Student ID
4. Enter password
5. Click "Sign In"

### For Teachers

#### First-Time Account Activation
1. Visit the app
2. Click "Activate Account"
3. Select "Teacher"
4. Enter Teacher ID (provided by institution)
5. Create a strong password
6. Confirm password
7. Click "Activate Account"
8. Redirected to sign-in

#### Regular Sign-In
1. Visit the app
2. Select "Teacher"
3. Enter Teacher ID
4. Enter password
5. Click "Sign In"

---

## 🐛 TROUBLESHOOTING

### Common Issues

#### "Student/Teacher ID not found"
- **Cause**: ID not in pre-registration database
- **Solution**: Contact administrator to add ID

#### "Account already activated"
- **Cause**: ID already has an active account
- **Solution**: Use sign-in instead of activation

#### "Account locked"
- **Cause**: Too many failed login attempts
- **Solution**: Wait 30 minutes or contact administrator

#### "Invalid ID format"
- **Cause**: ID doesn't match expected pattern
- **Solution**: Check ID format, ensure correct entry

---

## 🎓 BENEFITS OF THIS IMPLEMENTATION

### For Students
- ✅ Easy-to-remember institutional ID
- ✅ No need to manage email accounts
- ✅ Automatic profile population
- ✅ Consistent with school systems

### For Teachers
- ✅ Professional identification
- ✅ Department auto-association
- ✅ Streamlined onboarding

### For Administrators
- ✅ Centralized user management
- ✅ Bulk import capabilities
- ✅ Pre-registration control
- ✅ Audit trail
- ✅ Security compliance

### For Institution
- ✅ Institutional standard compliance
- ✅ Data consistency
- ✅ Privacy (no personal emails exposed)
- ✅ Professional appearance
- ✅ Scalability

---

## 📞 NEXT STEPS

1. **Review Implementation**
   - Check all completed components
   - Verify data models match requirements
   - Test validation logic

2. **Complete Remaining UI**
   - Update SignInScreen
   - Create admin management screens
   - Implement CSV import interface

3. **Testing**
   - Unit tests
   - Integration tests
   - End-to-end testing
   - User acceptance testing

4. **Deployment**
   - Deploy Firestore rules
   - Update Firebase configuration
   - Migrate existing users (if any)
   - Train administrators

5. **Documentation**
   - User manual
   - Admin guide
   - API documentation
   - Training materials

---

## 📧 SUPPORT

For implementation questions or issues:
1. Review this guide
2. Check code comments
3. Review Firebase documentation
4. Consult security best practices

---

**Status**: Phase 1 Complete - Core backend, data models, repositories, and account activation screen implemented. Ready for UI completion and admin screens.

**Next Priority**: Update SignInScreen to use ID-based authentication (TODO #10)


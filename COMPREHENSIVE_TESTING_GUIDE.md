# 🧪 Smart Academic Performance Tracker - Comprehensive Testing Guide

**Author**: Sentillas  
**Last Updated**: October 11, 2025  
**Version**: 1.1.0

## 🎯 **OVERVIEW**

This guide provides comprehensive testing procedures for all functionalities of the Smart Academic Performance Tracker app, including admin-teacher-student relationships and in-depth debugging procedures.

---

## 📱 **COMPLETE FUNCTIONALITY LIST**

### **🔐 AUTHENTICATION SYSTEM**

#### **1. User Registration & Login**
- **Sign Up**: Create accounts for Students, Teachers, and Admins
- **Sign In**: Role-based authentication with automatic dashboard routing
- **Role Selection**: Proper role assignment during registration
- **Session Management**: Persistent login sessions

#### **2. User Profile Management**
- **Profile Viewing**: View personal information
- **Profile Editing**: Update user details
- **Sign Out**: Secure logout functionality

---

### **👨‍🎓 STUDENT FUNCTIONALITIES**

#### **1. Student Dashboard**
- **Academic Overview**: Current grades, subjects, and performance
- **Quick Actions**: Access to all student features
- **Performance Indicators**: Visual status indicators

#### **2. Grade Management**
- **View Grades**: Individual subject grades (Prelim, Midterm, Final)
- **Grade History**: Historical grade tracking
- **Grade Comparison**: Compare grades across subjects/semesters
- **Performance Analytics**: Charts and trend analysis
- **Study Progress**: Academic progress tracking

#### **3. Subject Management**
- **My Subjects**: View enrolled subjects
- **Subject Applications**: Apply for new subjects
- **Application Status**: Track application progress
- **Enrollment Management**: View enrollment details

#### **4. Application System**
- **Browse Subjects**: Hierarchical subject selection (Course → Year Level → Subjects)
- **Apply for Subjects**: Submit subject applications
- **Application Tracking**: Monitor application status
- **Application Details**: View detailed application information

---

### **👨‍🏫 TEACHER FUNCTIONALITIES**

#### **1. Teacher Dashboard**
- **Teaching Overview**: Assigned subjects and class statistics
- **Quick Actions**: Access to all teacher features
- **Class Statistics**: Student enrollment and performance metrics

#### **2. Grade Input System**
- **Individual Grade Input**: Enter grades for specific students
- **Batch Grade Input**: Enter grades for multiple students
- **Enhanced Grade Input**: Advanced grade management with comments
- **Grade Validation**: Automatic grade range validation (0-100)
- **Grade Calculation**: Automatic final average computation

#### **3. Subject Management**
- **My Subjects**: View assigned subjects
- **Subject Applications**: Apply for subject assignments
- **Student Management**: Manage students in assigned subjects
- **Section Management**: Handle multiple sections per subject

#### **4. Application Management**
- **Student Applications**: Review student subject applications
- **Application Approval**: Approve/reject student applications
- **Application Comments**: Add feedback to applications
- **Enrollment Creation**: Automatic enrollment upon approval

#### **5. Analytics & Reporting**
- **Class Analytics**: Performance statistics for classes
- **Grade Distribution**: Visual grade distribution charts
- **Student Progress**: Individual student performance tracking
- **Grade Export**: Export grades to Excel/PDF formats

#### **6. Additional Features**
- **Submission Tracking**: Monitor assignment submissions
- **Attendance Management**: Track class attendance
- **Grade Comments**: Add feedback to individual grades
- **Grade Curve Tools**: Statistical grade adjustment tools

---

### **👨‍💼 ADMIN FUNCTIONALITIES**

#### **1. Admin Dashboard**
- **System Overview**: Comprehensive system statistics
- **Quick Actions**: Access to all admin features
- **System Health**: Monitor system performance and data integrity

#### **2. Academic Structure Management**
- **Course Management**: Create, edit, and manage courses
- **Year Level Management**: Manage year levels (1-4)
- **Subject Management**: Create and manage subjects
- **Hierarchical Management**: Complete academic structure hierarchy

#### **3. User Management**
- **User Overview**: View all system users
- **User Creation**: Create new user accounts
- **User Management**: Edit user information and roles
- **User Status**: Activate/deactivate user accounts

#### **4. Application Management**
- **All Applications**: View all student and teacher applications
- **Application Processing**: Approve/reject applications
- **Application Oversight**: Monitor application workflow
- **Application Statistics**: Application metrics and reports

#### **5. Academic Period Management**
- **Period Creation**: Create academic periods and semesters
- **Period Management**: Manage active academic periods
- **Period Data**: View academic period statistics
- **Calendar Management**: Academic calendar oversight

#### **6. System Monitoring**
- **Grade Monitoring**: System-wide grade analytics
- **Data Integrity**: Monitor data quality and consistency
- **System Performance**: Track system usage and performance
- **Event Logging**: System activity and audit logs

#### **7. Teacher Section Assignment**
- **Section Assignment**: Assign teachers to subject sections
- **Section Management**: Manage multiple sections per subject
- **Teacher Assignment**: Assign teachers to specific subjects
- **Section Statistics**: Monitor section enrollment and performance

---

## 🧪 **COMPREHENSIVE TESTING PROCEDURES**

### **PHASE 1: AUTHENTICATION & USER SETUP**

#### **Test 1.1: User Registration**
```bash
# Test Steps:
1. Launch the app
2. Navigate to Sign Up
3. Test each role registration:
   - Student: Email, password, name, course, year level
   - Teacher: Email, password, name
   - Admin: Email, password, name
4. Verify successful registration
5. Check role assignment in Firebase
```

#### **Test 1.2: User Login**
```bash
# Test Steps:
1. Test login for each role
2. Verify automatic dashboard routing:
   - Student → Student Dashboard
   - Teacher → Teacher Dashboard  
   - Admin → Admin Dashboard
3. Test session persistence
4. Test logout functionality
```

### **PHASE 2: ADMIN SETUP & CONFIGURATION**

#### **Test 2.1: Academic Structure Setup**
```bash
# Admin Test Steps:
1. Login as Admin
2. Navigate to "Academic Structure"
3. Create Course:
   - Course Name: "Bachelor of Science in Information Technology"
   - Course Code: "BSIT"
   - Duration: 4 years
4. Create Year Levels:
   - Year 1, Year 2, Year 3, Year 4
5. Create Subjects:
   - Subject Name: "Programming 1"
   - Subject Code: "PROG101"
   - Credits: 3
   - Semester: First Semester
   - Year Level: Year 1
   - Teacher: Assign teacher
```

#### **Test 2.2: Academic Period Setup**
```bash
# Admin Test Steps:
1. Navigate to "Academic Periods"
2. Create Academic Period:
   - Name: "Academic Year 2024-2025"
   - Semester: First Semester
   - Start Date: August 2024
   - End Date: December 2024
3. Set as Active Period
4. Verify period is active
```

#### **Test 2.3: User Management**
```bash
# Admin Test Steps:
1. Navigate to "Manage Users"
2. View all users (Students, Teachers, Admins)
3. Test user creation
4. Test user role management
5. Test user status management
```

### **PHASE 3: TEACHER FUNCTIONALITY TESTING**

#### **Test 3.1: Teacher Subject Assignment**
```bash
# Admin Test Steps:
1. Navigate to "Teacher Section Assignment"
2. Assign teacher to subject:
   - Select Teacher
   - Select Subject
   - Assign Section
3. Verify assignment in teacher dashboard
```

#### **Test 3.2: Teacher Subject Management**
```bash
# Teacher Test Steps:
1. Login as Teacher
2. Navigate to "My Subjects"
3. Verify assigned subjects are visible
4. Test subject details view
5. Test section management
```

#### **Test 3.3: Student Application Review**
```bash
# Teacher Test Steps:
1. Navigate to "Student Applications"
2. View applications for assigned subjects
3. Test application approval:
   - Select application
   - Add comments
   - Approve application
4. Test application rejection:
   - Select application
   - Add rejection reason
   - Reject application
5. Verify enrollment creation upon approval
```

#### **Test 3.4: Grade Input System**
```bash
# Teacher Test Steps:
1. Navigate to "Input Grades"
2. Select subject and student
3. Test grade input:
   - Prelim Grade: 85
   - Midterm Grade: 90
   - Final Grade: 88
4. Verify automatic calculation:
   - Final Average = (85×0.30) + (90×0.30) + (88×0.40) = 87.7
5. Test grade validation (0-100 range)
6. Test grade comments
```

#### **Test 3.5: Batch Grade Input**
```bash
# Teacher Test Steps:
1. Navigate to "Batch Grade Input"
2. Select subject and grade period
3. Enter grades for multiple students
4. Verify all grades are saved
5. Test grade export functionality
```

#### **Test 3.6: Teacher Analytics**
```bash
# Teacher Test Steps:
1. Navigate to "Analytics"
2. View class performance statistics
3. Test grade distribution charts
4. Test student progress tracking
5. Test grade export to Excel/PDF
```

### **PHASE 4: STUDENT FUNCTIONALITY TESTING**

#### **Test 4.1: Student Subject Application**
```bash
# Student Test Steps:
1. Login as Student
2. Navigate to "Apply for Subjects"
3. Test hierarchical subject selection:
   - Select Course
   - Select Year Level
   - Select Subject
4. Submit application
5. Verify application status
```

#### **Test 4.2: Student Grade Viewing**
```bash
# Student Test Steps:
1. Navigate to "View Grades"
2. View grades for enrolled subjects
3. Test grade breakdown:
   - Prelim, Midterm, Final grades
   - Final average calculation
   - Letter grade assignment
4. Test grade history
5. Test grade comparison
```

#### **Test 4.3: Student Analytics**
```bash
# Student Test Steps:
1. Navigate to "Analytics"
2. View performance charts
3. Test grade trends
4. Test performance tracking
5. Test study progress
```

#### **Test 4.4: Student Enrollment**
```bash
# Student Test Steps:
1. Navigate to "My Enrollments"
2. View enrolled subjects
3. Verify enrollment status
4. Test enrollment details
```

### **PHASE 5: ADMIN-TEACHER-STUDENT RELATIONSHIP TESTING**

#### **Test 5.1: Complete Application Workflow**
```bash
# End-to-End Test:
1. Admin creates academic structure
2. Admin assigns teacher to subject
3. Student applies for subject
4. Teacher reviews and approves application
5. Student gets enrolled automatically
6. Teacher inputs grades
7. Student views grades
8. Admin monitors entire process
```

#### **Test 5.2: Grade Calculation Workflow**
```bash
# Grade Calculation Test:
1. Teacher inputs Prelim: 80, Midterm: 85, Final: 90
2. System calculates: (80×0.30) + (85×0.30) + (90×0.40) = 85.5
3. Student views calculated grade
4. Admin monitors grade submission
5. Verify grade aggregation updates
```

#### **Test 5.3: Privacy & Security Testing**
```bash
# Security Test:
1. Teacher A can only see applications for their subjects
2. Teacher B cannot see Teacher A's student applications
3. Students can only see their own grades
4. Admins can see all data
5. Test role-based access control
```

---

## 🔍 **IN-DEPTH DEBUGGING PROCEDURES**

### **DEBUGGING TOOLS & TECHNIQUES**

#### **1. Logcat Monitoring**
```bash
# Enable Debug Logging:
1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. Filter by package: com.smartacademictracker
4. Monitor DEBUG messages for:
   - Authentication flows
   - Data loading operations
   - Navigation events
   - Error conditions
```

#### **2. Firebase Console Monitoring**
```bash
# Firebase Debugging:
1. Open Firebase Console
2. Go to Firestore Database
3. Monitor collections:
   - users
   - subjects
   - grades
   - applications
   - enrollments
4. Check for data consistency
5. Monitor security rule violations
```

#### **3. Network Monitoring**
```bash
# Network Debugging:
1. Use Android Studio Network Inspector
2. Monitor Firebase API calls
3. Check for failed requests
4. Monitor data synchronization
5. Verify offline capabilities
```

### **COMMON DEBUGGING SCENARIOS**

#### **Scenario 1: Authentication Issues**
```bash
# Debug Steps:
1. Check Firebase Authentication in console
2. Verify user roles in Firestore
3. Check navigation routing logic
4. Monitor LaunchedEffect triggers
5. Verify session persistence
```

#### **Scenario 2: Data Loading Issues**
```bash
# Debug Steps:
1. Check Firestore security rules
2. Verify user permissions
3. Monitor repository calls
4. Check ViewModel state management
5. Verify data synchronization
```

#### **Scenario 3: Grade Calculation Issues**
```bash
# Debug Steps:
1. Check GradeCalculationEngine
2. Verify grade period weights
3. Monitor aggregate updates
4. Check grade validation
5. Verify final average calculation
```

#### **Scenario 4: Application Workflow Issues**
```bash
# Debug Steps:
1. Check application status updates
2. Verify enrollment creation
3. Monitor teacher permissions
4. Check student application visibility
5. Verify admin oversight capabilities
```

### **PERFORMANCE DEBUGGING**

#### **1. Memory Usage**
```bash
# Memory Debugging:
1. Use Android Studio Profiler
2. Monitor memory usage during navigation
3. Check for memory leaks
4. Monitor ViewModel lifecycle
5. Verify proper cleanup
```

#### **2. Network Performance**
```bash
# Network Debugging:
1. Monitor Firebase response times
2. Check for network timeouts
3. Verify offline capabilities
4. Monitor data synchronization
5. Check for duplicate requests
```

#### **3. UI Performance**
```bash
# UI Debugging:
1. Monitor Compose recomposition
2. Check for unnecessary state updates
3. Verify lazy loading
4. Monitor scroll performance
5. Check for UI blocking operations
```

---

## 📊 **TESTING CHECKLIST**

### **✅ AUTHENTICATION & USER MANAGEMENT**
- [ ] User registration (Student, Teacher, Admin)
- [ ] User login with role-based routing
- [ ] Session persistence
- [ ] Profile management
- [ ] Sign out functionality

### **✅ ADMIN FUNCTIONALITIES**
- [ ] Academic structure creation
- [ ] Course management
- [ ] Year level management
- [ ] Subject management
- [ ] Academic period management
- [ ] User management
- [ ] Application oversight
- [ ] Grade monitoring
- [ ] Teacher section assignment

### **✅ TEACHER FUNCTIONALITIES**
- [ ] Subject assignment viewing
- [ ] Student application review
- [ ] Application approval/rejection
- [ ] Grade input (individual and batch)
- [ ] Grade calculation verification
- [ ] Student management
- [ ] Analytics and reporting
- [ ] Grade export functionality

### **✅ STUDENT FUNCTIONALITIES**
- [ ] Subject application submission
- [ ] Grade viewing
- [ ] Performance analytics
- [ ] Enrollment management
- [ ] Application status tracking
- [ ] Grade history and comparison

### **✅ RELATIONSHIP TESTING**
- [ ] Admin-teacher-student workflow
- [ ] Application approval process
- [ ] Grade input and viewing
- [ ] Data privacy and security
- [ ] Role-based access control

### **✅ SYSTEM TESTING**
- [ ] Offline functionality
- [ ] Data synchronization
- [ ] Error handling
- [ ] Performance optimization
- [ ] Security validation

---

## 🚨 **TROUBLESHOOTING GUIDE**

### **Common Issues & Solutions**

#### **Issue 1: "Permission Denied" Errors**
```bash
# Solution:
1. Check Firestore security rules
2. Verify user authentication
3. Check user role permissions
4. Update security rules if needed
5. Redeploy rules to Firebase
```

#### **Issue 2: Data Not Loading**
```bash
# Solution:
1. Check network connectivity
2. Verify Firebase configuration
3. Check user permissions
4. Monitor Firestore queries
5. Verify data structure
```

#### **Issue 3: Navigation Issues**
```bash
# Solution:
1. Check navigation routes
2. Verify parameter passing
3. Monitor LaunchedEffect triggers
4. Check ViewModel state
5. Verify screen composables
```

#### **Issue 4: Grade Calculation Errors**
```bash
# Solution:
1. Check grade validation
2. Verify calculation engine
3. Monitor aggregate updates
4. Check grade periods
5. Verify final average computation
```

---

## 📈 **TESTING METRICS**

### **Performance Benchmarks**
- **App Launch Time**: < 3 seconds
- **Screen Navigation**: < 1 second
- **Data Loading**: < 2 seconds
- **Grade Calculation**: < 500ms
- **Firebase Sync**: < 5 seconds

### **Success Criteria**
- **100% Feature Coverage**: All functionalities tested
- **Zero Critical Bugs**: No blocking issues
- **Security Validation**: All privacy controls working
- **Performance Standards**: All benchmarks met
- **User Experience**: Smooth navigation and interactions

---

This comprehensive testing guide ensures complete coverage of all app functionalities, proper testing of admin-teacher-student relationships, and thorough debugging procedures for optimal app performance and reliability.

# 🔄 Smart Academic Performance Tracker - Testing Workflow

**Author**: Sentillas  
**Last Updated**: October 22, 2025  
**Version**: 1.1.0

## 🎯 **STEP-BY-STEP TESTING WORKFLOW**

### **PHASE 1: ENVIRONMENT SETUP**

#### **Step 1.1: Development Environment**
```bash
# Prerequisites:
1. Android Studio Arctic Fox or later
2. Kotlin 1.8.0+
3. JDK 11+
4. Firebase project setup
5. Physical device or emulator

# Setup Steps:
1. Clone repository
2. Open in Android Studio
3. Configure Firebase:
   - Add google-services.json
   - Enable Authentication
   - Enable Firestore
   - Configure security rules
4. Build project
5. Install on device/emulator
```

#### **Step 1.2: Firebase Configuration**
```bash
# Firebase Setup:
1. Create Firebase project
2. Enable Authentication (Email/Password)
3. Enable Firestore Database
4. Configure security rules
5. Set up Firebase CLI
6. Deploy security rules
```

#### **Step 1.3: Test Data Preparation**
```bash
# Create Test Users:
1. Create Admin user
2. Create Teacher user
3. Create Student user
4. Verify user roles in Firebase
5. Test authentication
```

---

### **PHASE 2: ADMIN SETUP & CONFIGURATION**

#### **Step 2.1: Admin Login & Dashboard**
```bash
# Test Steps:
1. Launch app
2. Login as Admin
3. Verify Admin Dashboard loads
4. Check system overview statistics
5. Verify quick action buttons
6. Test navigation to all admin features
```

#### **Step 2.2: Academic Structure Creation**
```bash
# Create Course:
1. Navigate to "Academic Structure"
2. Click "Add Course"
3. Enter course details:
   - Name: "Bachelor of Science in Information Technology"
   - Code: "BSIT"
   - Duration: 4 years
4. Save course
5. Verify course appears in hierarchical view

# Create Year Levels:
1. Select created course
2. Click "Add Year Level"
3. Create year levels:
   - Year 1
   - Year 2
   - Year 3
   - Year 4
4. Save each year level
5. Verify year levels appear under course

# Create Subjects:
1. Select course and year level
2. Click "Add Subject"
3. Create subjects:
   - Programming 1 (PROG101) - 3 credits - First Semester
   - Programming 2 (PROG102) - 3 credits - Second Semester
   - Database Management (DBMS101) - 3 credits - First Semester
4. Assign teacher to each subject
5. Save subjects
6. Verify subjects appear in hierarchical view
```

#### **Step 2.3: Academic Period Setup**
```bash
# Create Academic Period:
1. Navigate to "Academic Periods"
2. Click "Add Academic Period"
3. Enter period details:
   - Name: "Academic Year 2024-2025"
   - Academic Year: "2024-2025"
   - Semester: "First Semester"
   - Start Date: August 1, 2024
   - End Date: December 15, 2024
   - Description: "First semester of academic year 2024-2025"
4. Set as active period
5. Save period
6. Verify period is active in dashboard
```

#### **Step 2.4: User Management**
```bash
# Create Additional Users:
1. Navigate to "Manage Users"
2. Create additional teachers:
   - Teacher 1: Programming subjects
   - Teacher 2: Database subjects
3. Create additional students:
   - Student 1: BSIT Year 1
   - Student 2: BSIT Year 1
4. Verify users appear in user list
5. Test user role management
```

---

### **PHASE 3: TEACHER FUNCTIONALITY TESTING**

#### **Step 3.1: Teacher Login & Dashboard**
```bash
# Test Steps:
1. Logout from Admin
2. Login as Teacher
3. Verify Teacher Dashboard loads
4. Check assigned subjects display
5. Verify quick action buttons
6. Test navigation to all teacher features
```

#### **Step 3.2: Teacher Subject Management**
```bash
# My Subjects:
1. Navigate to "My Subjects"
2. Verify assigned subjects are visible
3. Check subject details:
   - Subject name and code
   - Credits and semester
   - Year level and course
   - Student enrollment count
4. Test section management
5. Verify student enrollment list
```

#### **Step 3.3: Student Application Review**
```bash
# Review Applications:
1. Navigate to "Student Applications"
2. Verify applications for assigned subjects are visible
3. Test application details:
   - Student information
   - Subject information
   - Application date
   - Application status
4. Test application approval:
   - Select application
   - Add approval comments
   - Approve application
   - Verify enrollment creation
5. Test application rejection:
   - Select application
   - Add rejection reason
   - Reject application
   - Verify status update
```

#### **Step 3.4: Grade Input System**
```bash
# Individual Grade Input:
1. Navigate to "Input Grades"
2. Select subject
3. Select student
4. Enter grades:
   - Prelim: 85
   - Midterm: 90
   - Final: 88
5. Add grade comments
6. Save grades
7. Verify automatic calculation:
   - Final Average = (85×0.30) + (90×0.30) + (88×0.40) = 87.7
8. Verify letter grade assignment
9. Verify grade status determination

# Batch Grade Input:
1. Navigate to "Batch Grade Input"
2. Select subject
3. Select grade period (Prelim)
4. Enter grades for multiple students
5. Validate all grades
6. Save all grades
7. Verify calculations for all students
```

#### **Step 3.5: Teacher Analytics**
```bash
# Class Analytics:
1. Navigate to "Analytics"
2. View class performance statistics
3. Check grade distribution charts
4. Test student progress tracking
5. Test grade export functionality:
   - Export to Excel
   - Export to PDF
6. Verify analytics data accuracy
```

---

### **PHASE 4: STUDENT FUNCTIONALITY TESTING**

#### **Step 4.1: Student Login & Dashboard**
```bash
# Test Steps:
1. Logout from Teacher
2. Login as Student
3. Verify Student Dashboard loads
4. Check academic overview
5. Verify quick action buttons
6. Test navigation to all student features
```

#### **Step 4.2: Subject Application Process**
```bash
# Apply for Subjects:
1. Navigate to "Apply for Subjects"
2. Test hierarchical subject selection:
   - Select Course: "Bachelor of Science in Information Technology"
   - Select Year Level: "Year 1"
   - Select Subject: "Programming 1"
3. Submit application
4. Verify application confirmation
5. Test application status tracking
6. Repeat for multiple subjects
```

#### **Step 4.3: Grade Viewing**
```bash
# View Grades:
1. Navigate to "View Grades"
2. Check enrolled subjects
3. View grade breakdown:
   - Prelim, Midterm, Final grades
   - Final average calculation
   - Letter grade assignment
   - Grade status indicators
4. Test grade history
5. Test grade comparison
6. Verify real-time grade updates
```

#### **Step 4.4: Student Analytics**
```bash
# Performance Analytics:
1. Navigate to "Analytics"
2. View performance charts
3. Test grade trends
4. Test performance tracking
5. Test study progress
6. Verify analytics data accuracy
```

---

### **PHASE 5: END-TO-END WORKFLOW TESTING**

#### **Step 5.1: Complete Application Workflow**
```bash
# End-to-End Test:
1. Admin creates academic structure
2. Admin assigns teacher to subject
3. Student applies for subject
4. Teacher reviews application
5. Teacher approves application
6. Student gets enrolled automatically
7. Teacher inputs grades
8. Student views grades
9. Admin monitors entire process
10. Verify data consistency across all roles
```

#### **Step 5.2: Grade Calculation Workflow**
```bash
# Grade Calculation Test:
1. Teacher inputs grades:
   - Prelim: 80
   - Midterm: 85
   - Final: 90
2. System calculates final average:
   - (80×0.30) + (85×0.30) + (90×0.40) = 85.5
3. Student views calculated grade
4. Admin monitors grade submission
5. Verify grade aggregation updates
6. Test grade export functionality
```

#### **Step 5.3: Privacy & Security Testing**
```bash
# Security Test:
1. Teacher A can only see applications for their subjects
2. Teacher B cannot see Teacher A's student applications
3. Students can only see their own grades
4. Admins can see all data
5. Test role-based access control
6. Verify data isolation
7. Test security rule enforcement
```

---

### **PHASE 6: DEBUGGING & TROUBLESHOOTING**

#### **Step 6.1: Logcat Monitoring**
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
5. Check for any error messages
6. Monitor performance metrics
```

#### **Step 6.2: Firebase Console Monitoring**
```bash
# Firebase Debugging:
1. Open Firebase Console
2. Go to Firestore Database
3. Monitor collections:
   - users: Check user data consistency
   - subjects: Verify subject structure
   - grades: Check grade calculations
   - applications: Monitor application flow
   - enrollments: Verify enrollment creation
4. Check for data consistency
5. Monitor security rule violations
6. Verify real-time updates
```

#### **Step 6.3: Network Monitoring**
```bash
# Network Debugging:
1. Use Android Studio Network Inspector
2. Monitor Firebase API calls
3. Check for failed requests
4. Monitor data synchronization
5. Verify offline capabilities
6. Check response times
7. Monitor data usage
```

---

### **PHASE 7: PERFORMANCE TESTING**

#### **Step 7.1: Performance Benchmarks**
```bash
# Test Performance:
1. App Launch Time: < 3 seconds
2. Screen Navigation: < 1 second
3. Data Loading: < 2 seconds
4. Grade Calculation: < 500ms
5. Firebase Sync: < 5 seconds
6. Memory Usage: Monitor for leaks
7. CPU Usage: Monitor for spikes
8. Battery Usage: Monitor efficiency
```

#### **Step 7.2: Stress Testing**
```bash
# Stress Test:
1. Create multiple users
2. Generate large amounts of data
3. Test concurrent operations
4. Monitor system performance
5. Test offline/online transitions
6. Verify data consistency
7. Test error handling
```

---

### **PHASE 8: FINAL VALIDATION**

#### **Step 8.1: Feature Completeness**
```bash
# Verify All Features:
1. Authentication system
2. Admin functionalities
3. Teacher functionalities
4. Student functionalities
5. Application workflow
6. Grade calculation
7. Analytics and reporting
8. Data privacy and security
9. Performance optimization
10. Error handling
```

#### **Step 8.2: Data Integrity**
```bash
# Verify Data Integrity:
1. Check data consistency
2. Verify calculations
3. Test data synchronization
4. Monitor security rules
5. Test offline capabilities
6. Verify user permissions
7. Check data isolation
8. Test error recovery
```

#### **Step 8.3: User Experience**
```bash
# Verify User Experience:
1. Test navigation flow
2. Verify UI responsiveness
3. Check error messages
4. Test loading states
5. Verify success animations
6. Test accessibility
7. Check performance
8. Verify usability
```

---

## 📊 **TESTING RESULTS DOCUMENTATION**

### **Test Results Template**
```bash
# For each test:
1. Test Name: [Test Description]
2. Expected Result: [What should happen]
3. Actual Result: [What actually happened]
4. Status: [PASS/FAIL]
5. Issues Found: [Any problems]
6. Resolution: [How issues were fixed]
7. Notes: [Additional observations]
```

### **Performance Metrics**
```bash
# Record Performance:
1. App Launch Time: [X] seconds
2. Screen Navigation: [X] seconds
3. Data Loading: [X] seconds
4. Grade Calculation: [X] seconds
5. Firebase Sync: [X] seconds
6. Memory Usage: [X] MB
7. CPU Usage: [X] %
8. Battery Usage: [X] %
```

### **Bug Report Template**
```bash
# For each bug:
1. Bug ID: [Unique identifier]
2. Severity: [Critical/High/Medium/Low]
3. Description: [What the bug is]
4. Steps to Reproduce: [How to reproduce]
5. Expected Result: [What should happen]
6. Actual Result: [What actually happens]
7. Environment: [Device, OS, etc.]
8. Screenshots: [If applicable]
9. Resolution: [How it was fixed]
10. Status: [Open/In Progress/Resolved]
```

---

This comprehensive testing workflow ensures complete coverage of all app functionalities, proper testing of admin-teacher-student relationships, and thorough debugging procedures for optimal app performance and reliability.

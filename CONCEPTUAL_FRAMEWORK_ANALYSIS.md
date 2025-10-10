# Smart Academic Performance Tracker - Conceptual Framework Analysis

**Generated:** Based on comprehensive codebase scan  
**Version:** 1.1.0

---

## 📋 Executive Summary

This document provides a complete Input-Output-Process conceptual framework for all three user roles (Student, Teacher, Admin) in the Smart Academic Performance Tracker application. The analysis is based on a comprehensive scan of the codebase, including all screens, ViewModels, repositories, data models, and business logic.

---

## 🎓 STUDENT - Conceptual Framework

### **INPUTS**

#### 1. **Authentication & Profile Data**
- Email and password (Sign In/Sign Up)
- Personal information (firstName, lastName)
- Role selection: STUDENT
- Course and Year Level selection (during registration/profile setup)

#### 2. **Subject Application Data**
- Course selection (hierarchical: Course → Year Level → Subject)
- Subject ID and Subject Code
- Application reason (optional text)
- Section preference (if multiple sections available)

#### 3. **View/Query Parameters**
- Subject ID (for detailed subject view)
- Application ID (for application status tracking)
- Academic Period filter (current/previous periods)
- Semester filter (First/Second/Summer)

#### 4. **User Actions**
- Navigation selections (dashboard, grades, subjects, analytics)
- Refresh actions (pull-to-refresh)
- Filter/sort preferences

---

### **PROCESSES**

#### 1. **Authentication Process**
```
User Input (Email/Password) 
  → Firebase Authentication 
  → UserRepository.getCurrentUser() 
  → Role Verification (STUDENT)
  → Dashboard Navigation
```

#### 2. **Grade Viewing Process**
```
Student ID + Subject ID + Academic Period
  → GradeRepository.getGradesByStudentAndSubject()
  → GradeCalculationEngine.calculateFinalAverage()
    - Prelim (30%) + Midterm (30%) + Final (40%)
  → Determine GradeStatus (PASSING/AT_RISK/FAILING)
  → Calculate Letter Grade (1.0-5.0 scale)
  → Display in UI with visual indicators
```

#### 3. **Subject Application Process**
```
Student selects Course → Year Level → Subject
  → HierarchicalStudentSubjectApplicationViewModel
  → Validate: Student not already enrolled
  → Create StudentApplication (status: PENDING)
  → StudentApplicationRepository.createApplication()
  → NotificationSenderService.notifyTeacherOfStudentApplication()
  → Return application ID for tracking
```

#### 4. **Application Status Tracking Process**
```
Application ID
  → StudentApplicationRepository.getApplicationById()
  → Check status: PENDING/APPROVED/REJECTED
  → If APPROVED: Check Enrollment creation
  → Display status with teacher comments (if any)
```

#### 5. **Analytics & Performance Tracking Process**
```
Student ID + Academic Period
  → StudentAnalyticsViewModel
  → Aggregate grades across all subjects
  → Calculate:
    - Overall GPA
    - Grade trends (Prelim → Midterm → Final)
    - Performance status distribution
    - Subject-wise comparisons
  → Generate charts and visualizations
  → Display performance insights
```

#### 6. **Grade History & Comparison Process**
```
Student ID + Time Range
  → GradeRepository.getGradesByStudent()
  → Filter by academic periods
  → Group by subject and semester
  → Calculate historical averages
  → Compare across periods/subjects
  → Display trends and comparisons
```

#### 7. **Enrollment Viewing Process**
```
Student ID
  → EnrollmentRepository.getEnrollmentsByStudent()
  → Join with Subject data
  → Filter by active academic period
  → Display enrolled subjects with details
```

#### 8. **Notification Receiving Process**
```
User ID
  → NotificationRepository.getNotificationsByUser()
  → Filter unread notifications
  → Display in notification center
  → Mark as read when viewed
  → Trigger local notifications for new items
```

---

### **OUTPUTS**

#### 1. **Dashboard Display**
- Overall GPA (calculated from all subjects)
- Total enrolled subjects count
- Performance status summary (Passing/At-Risk/Failing subjects)
- Quick action cards (View Grades, My Subjects, Apply for Subjects, etc.)
- Recent activity/notifications preview

#### 2. **Grade Information**
- **Per Subject:**
  - Prelim Grade (0-100)
  - Midterm Grade (0-100)
  - Final Grade (0-100)
  - Final Average (calculated: Prelim×0.3 + Midterm×0.3 + Final×0.4)
  - Letter Grade (1.0-5.0 scale)
  - Status Indicator (Passing/At-Risk/Failing/Incomplete)
  - Grade completion percentage
- **Aggregate:**
  - Overall GPA across all subjects
  - Grade distribution charts
  - Performance trend graphs

#### 3. **Subject Information**
- Enrolled subjects list
- Subject details (name, code, teacher, credits, semester)
- Enrollment status (active/inactive)
- Subject performance summary

#### 4. **Application Status**
- Application ID
- Subject applied for
- Application status (PENDING/APPROVED/REJECTED)
- Applied date
- Reviewed date (if reviewed)
- Teacher comments (if any)
- Review outcome details

#### 5. **Analytics & Reports**
- **Performance Charts:**
  - Grade trend line charts (Prelim → Midterm → Final)
  - Subject comparison bar charts
  - Performance status pie charts
  - Historical performance trends
- **Statistics:**
  - Overall GPA
  - Grade distribution
  - Performance indicators
  - Study progress tracking

#### 6. **Notifications**
- Grade update notifications
- Application status change notifications
- System announcements
- Deadline reminders
- Performance alerts

#### 7. **Enrollment Details**
- List of enrolled subjects
- Enrollment dates
- Subject information
- Academic period association

---

## 👨‍🏫 TEACHER - Conceptual Framework

### **INPUTS**

#### 1. **Authentication & Profile Data**
- Email and password (Sign In/Sign Up)
- Personal information (firstName, lastName)
- Role selection: TEACHER

#### 2. **Grade Input Data**
- Subject ID (selected from assigned subjects)
- Student ID
- Grade Period (PRELIM/MIDTERM/FINAL)
- Score (0-100)
- Max Score (default: 100)
- Description/Comments (optional)
- Academic Period ID

#### 3. **Batch Grade Input Data**
- Subject ID
- Grade Period
- List of student-grade pairs
- Validation flags

#### 4. **Application Review Data**
- Application ID
- Review decision (APPROVE/REJECT)
- Teacher comments (optional)
- Enrollment creation (if approved)

#### 5. **Subject Application Data**
- Subject ID (to apply for teaching assignment)
- Application reason (optional)

#### 6. **Attendance Data**
- Subject ID
- Student ID
- Date
- Attendance Status (PRESENT/ABSENT/LATE/EXCUSED/TARDY)
- Session Type (REGULAR/LABORATORY/LECTURE/TUTORIAL/EXAM)

#### 7. **Submission Tracking Data**
- Subject ID
- Student ID
- Assignment details
- Submission Status (PENDING/SUBMITTED/LATE/GRADED/RETURNED)
- Submission Type (ONLINE/FILE_UPLOAD/TEXT/OFFLINE)
- Grade and feedback (if graded)

#### 8. **Grade Export Parameters**
- Subject ID
- Export format (Excel/PDF)
- Export type (Subject Summary/Individual Reports/Class Summary)
- Academic Period filter

#### 9. **Grade Curve Parameters**
- Subject ID
- Curve type (LINEAR/PERCENTAGE/SQUARE_ROOT/BELL/TARGET_AVERAGE)
- Curve parameters (adjustment values)

---

### **PROCESSES**

#### 1. **Grade Input Process (Individual)**
```
Subject ID + Student ID + Grade Period + Score
  → Validate: Grade range (0-100)
  → Validate: Teacher assigned to subject
  → Validate: Academic period active
  → Create/Update Grade object
  → GradeRepository.createGrade() or updateGrade()
  → GradeCalculationEngine.calculateFinalAverage()
  → Update StudentGradeAggregate
  → NotificationSenderService.sendGradeUpdateNotification()
  → Sync to offline storage (if offline)
  → Return success/error result
```

#### 2. **Batch Grade Input Process**
```
Subject ID + Grade Period + List[Student-Grade pairs]
  → Validate all grades (0-100 range)
  → Validate teacher assignment
  → Batch create/update grades
  → Calculate final averages for all students
  → Update all StudentGradeAggregates
  → Send batch notifications to students
  → Sync to offline storage
  → Return batch result with success/failure counts
```

#### 3. **Student Application Review Process**
```
Application ID + Decision (APPROVE/REJECT) + Comments
  → TeacherStudentApplicationsViewModel
  → Validate: Teacher assigned to subject
  → Update StudentApplication status
  → If APPROVED:
    → Create Enrollment record
    → EnrollmentRepository.createEnrollment()
    → NotificationSenderService.sendApplicationStatusNotification()
  → If REJECTED:
    → Update application with rejection reason
    → Send rejection notification
  → Return review result
```

#### 4. **Subject Application Process (Teacher applying for subject)**
```
Subject ID + Application Reason
  → TeacherSubjectsViewModel.applyForSubject()
  → Create TeacherApplication (status: PENDING)
  → TeacherApplicationRepository.createApplication()
  → NotificationSenderService.notifyAdminsOfTeacherApplication()
  → Return application ID
```

#### 5. **Grade Export Process**
```
Subject ID + Export Type + Format
  → GradeExportRepository
  → Fetch all grades for subject
  → Fetch student and subject details
  → Generate export document:
    - Subject Grade Export: All students with grades
    - Individual Reports: Per-student detailed reports
    - Class Summary: Aggregate statistics
  → Format as Excel/PDF
  → Track export in export_history
  → Return export file/URL
```

#### 6. **Attendance Recording Process**
```
Subject ID + Student ID + Date + Status
  → AttendanceRepository.recordAttendance()
  → Validate: Teacher assigned to subject
  → Create/Update Attendance record
  → Calculate attendance statistics
  → Return success result
```

#### 7. **Submission Tracking Process**
```
Subject ID + Student ID + Submission Data
  → AssignmentSubmissionRepository.createSubmission()
  → Validate: Teacher assigned to subject
  → Create submission record
  → Update submission status
  → If graded: Update grade and send notification
  → Return submission ID
```

#### 8. **Grade Curve Application Process**
```
Subject ID + Curve Type + Parameters
  → GradeCurveRepository
  → Fetch all grades for subject
  → Apply curve algorithm:
    - Linear: Add/subtract fixed amount
    - Percentage: Multiply by percentage
    - Square Root: Apply square root
    - Bell: Normalize to bell curve
    - Target Average: Adjust to target
  → Update all affected grades
  → Recalculate final averages
  → Send notifications to students
  → Return curve application result
```

#### 9. **Analytics Generation Process**
```
Subject ID + Academic Period
  → TeacherAnalyticsViewModel
  → Aggregate grade data:
    - Class average per period
    - Grade distribution
    - Performance statistics
    - At-risk student identification
  → Generate charts and visualizations
  → Display analytics dashboard
```

#### 10. **Student Management Process**
```
Subject ID
  → EnrollmentRepository.getEnrollmentsBySubject()
  → Filter by active academic period
  → Display student list
  → Allow: View student details, Remove student (if permitted)
  → Update enrollment status
```

---

### **OUTPUTS**

#### 1. **Dashboard Display**
- Assigned subjects count
- Total students across all subjects
- Pending student applications count
- Class statistics overview
- Quick action cards

#### 2. **Grade Input Interface**
- Student list for selected subject
- Grade input fields (Prelim/Midterm/Final)
- Auto-calculated final averages
- Validation feedback
- Save/Submit buttons
- Batch input capabilities

#### 3. **Grade Management Results**
- Success/failure status for grade operations
- Updated grade displays
- Final average calculations
- Letter grade assignments
- Status indicators

#### 4. **Application Review Interface**
- List of pending student applications
- Application details (student, subject, reason)
- Approve/Reject actions
- Comment input field
- Review confirmation

#### 5. **Subject Management**
- Assigned subjects list
- Subject details (name, code, sections, student count)
- Quick actions (Input Grades, View Analytics, Export)

#### 6. **Analytics & Reports**
- **Class Performance:**
  - Class average per grade period
  - Grade distribution charts
  - Performance trends
  - At-risk student identification
- **Export Documents:**
  - Excel/PDF grade reports
  - Individual student reports
  - Class summary reports
  - Export history

#### 7. **Attendance Records**
- Attendance list by subject and date
- Attendance statistics
- Attendance summaries
- Attendance reports

#### 8. **Submission Tracking**
- Submission list by subject
- Submission status
- Submission statistics
- Late submission alerts

#### 9. **Grade Curve Results**
- Updated grades after curve application
- Curve statistics
- Before/after comparisons

#### 10. **Notifications Sent**
- Grade update notifications (to students)
- Application review notifications (to students)
- System notifications (to teacher)

---

## 👨‍💼 ADMIN - Conceptual Framework

### **INPUTS**

#### 1. **Authentication & Profile Data**
- Email and password (Sign In/Sign Up)
- Personal information (firstName, lastName)
- Role selection: ADMIN

#### 2. **Academic Structure Data**
- **Course:**
  - Course name
  - Course code
  - Duration (2-6 years)
- **Year Level:**
  - Year level number (1-4)
  - Course ID (parent)
- **Subject:**
  - Subject name
  - Subject code
  - Description
  - Credits (1-6)
  - Semester (FIRST/SECOND/SUMMER)
  - Year Level ID (parent)
  - Course ID (parent)
  - Max students
  - Number of sections

#### 3. **Academic Period Data**
- Period name
- Academic year (e.g., "2024-2025")
- Semester (FIRST/SECOND/SUMMER)
- Start date
- End date
- Description
- Active flag (only one can be active)

#### 4. **User Management Data**
- User ID
- User role (STUDENT/TEACHER/ADMIN)
- User status (active/inactive)
- Course and Year Level assignment (for students)
- Profile updates

#### 5. **Application Review Data**
- Application ID (Teacher/Student applications)
- Review decision (APPROVE/REJECT)
- Admin comments
- Teacher assignment (for teacher applications)
- Enrollment creation (for student applications)

#### 6. **Teacher Section Assignment Data**
- Subject ID
- Teacher ID
- Section name/identifier
- Academic Period ID

#### 7. **Grade Monitoring Parameters**
- Academic Period filter
- Subject filter
- Teacher filter
- Date range filter

#### 8. **Data Management Operations**
- Data export parameters
- Data cleanup operations
- System maintenance tasks

---

### **PROCESSES**

#### 1. **Academic Structure Management Process**
```
Course Creation:
  Course Data → CourseRepository.createCourse()
  → Validate: Unique course code
  → Save to Firestore
  → Return course ID

Year Level Creation:
  Year Level Data + Course ID → YearLevelRepository.createYearLevel()
  → Validate: Year level 1-4, Course exists
  → Save to Firestore
  → Return year level ID

Subject Creation:
  Subject Data + Year Level ID + Course ID → SubjectRepository.createSubject()
  → Validate: Unique subject code, Year level exists, Course exists
  → Generate sections if numberOfSections > 1
  → Save to Firestore
  → Return subject ID
```

#### 2. **Academic Period Management Process**
```
Period Creation:
  Period Data → AcademicPeriodRepository.createPeriod()
  → Validate: Date range, Unique name
  → If setting as active: Deactivate all other periods
  → Save to Firestore
  → NotificationSenderService.sendAcademicPeriodActivatedNotification()
  → Return period ID

Period Activation:
  Period ID → AcademicPeriodRepository.setActivePeriod()
  → Deactivate current active period
  → Activate selected period
  → Update all related records
  → Send notifications
```

#### 3. **User Management Process**
```
User Creation/Update:
  User Data → UserRepository.createUser() / updateUser()
  → Validate: Unique email, Valid role
  → If STUDENT: Validate course and year level
  → Save to Firestore
  → Return user ID

User Status Management:
  User ID + Status → UserRepository.updateUserStatus()
  → Update active flag
  → Update related records (enrollments, applications)
  → Return result
```

#### 4. **Application Review Process (Teacher Applications)**
```
Application ID + Decision + Teacher Assignment
  → AdminApplicationsViewModel
  → Update TeacherApplication status
  → If APPROVED:
    → Assign teacher to subject (SubjectRepository.updateSubject())
    → Update subject with teacherId and teacherName
    → NotificationSenderService.sendApplicationStatusNotification()
  → If REJECTED:
    → Update application with rejection reason
    → Send rejection notification
  → Return review result
```

#### 5. **Application Review Process (Student Applications)**
```
Application ID + Decision
  → AdminStudentApplicationsViewModel
  → Update StudentApplication status
  → If APPROVED:
    → Create Enrollment record
    → EnrollmentRepository.createEnrollment()
    → NotificationSenderService.sendApplicationStatusNotification()
  → If REJECTED:
    → Update application with rejection reason
    → Send rejection notification
  → Return review result
```

#### 6. **Teacher Section Assignment Process**
```
Subject ID + Teacher ID + Section Name
  → TeacherSectionAssignmentViewModel
  → Validate: Subject exists, Teacher exists
  → Create/Update SectionAssignment record
  → Update subject sections list
  → Return assignment result
```

#### 7. **Grade Monitoring Process**
```
Academic Period + Filters
  → AdminGradeMonitoringViewModel
  → Aggregate grade data:
    - Total grades submitted
    - Completion rate by teacher/subject
    - Grade distribution statistics
    - Submission timeliness
    - Data quality metrics
  → Generate monitoring reports
  → Display analytics dashboard
```

#### 8. **Academic Period Data Viewing Process**
```
Academic Period ID
  → AcademicPeriodDataViewModel
  → Aggregate all data for period:
    - Total subjects
    - Total enrollments
    - Total grades
    - Total applications
    - Active users
  → Generate period summary
  → Display comprehensive data view
```

#### 9. **System Statistics Aggregation Process**
```
No input (system-wide)
  → AdminDashboardViewModel
  → Aggregate from all collections:
    - Total users (by role)
    - Total subjects
    - Total enrollments
    - Total applications (pending/approved/rejected)
    - Active academic period
  → Calculate system health metrics
  → Display dashboard
```

#### 10. **Data Integrity Checking Process**
```
No input (system-wide)
  → DataIntegrityChecker
  → Validate:
    - Orphaned records
    - Missing references
    - Data consistency
    - Grade calculation accuracy
  → Generate integrity report
  → Flag issues for resolution
```

---

### **OUTPUTS**

#### 1. **Dashboard Display**
- **System Statistics:**
  - Total subjects count
  - Active subjects count
  - Total students count
  - Total teachers count
  - Total enrollments count
  - Pending applications count
- **Academic Period Status:**
  - Active period name
  - Current semester
  - Current academic year
- **Quick Actions:**
  - Teacher Applications
  - Academic Structure Management
  - User Management
  - Grade Monitoring
  - Academic Periods

#### 2. **Academic Structure Management**
- **Hierarchical View:**
  - Courses → Year Levels → Subjects
  - Expandable tree structure
  - Add/Edit/Delete capabilities
- **Creation Forms:**
  - Course creation form
  - Year Level creation form
  - Subject creation form
- **Validation Feedback:**
  - Success/error messages
  - Duplicate detection
  - Reference validation

#### 3. **Academic Period Management**
- **Period List:**
  - All academic periods
  - Active period indicator
  - Period details (dates, semester, year)
- **Period Creation Form:**
  - Input fields for all period data
  - Date pickers
  - Active period toggle
- **Period Activation:**
  - Confirmation dialogs
  - Status updates

#### 4. **User Management**
- **User List:**
  - All users (filterable by role)
  - User details (name, email, role, status)
  - Course/Year Level (for students)
- **User Actions:**
  - Create new user
  - Edit user details
  - Activate/Deactivate user
  - Assign course/year level

#### 5. **Application Management**
- **Teacher Applications:**
  - List of pending/approved/rejected applications
  - Application details (teacher, subject, reason)
  - Approve/Reject actions
  - Teacher assignment interface
- **Student Applications:**
  - List of pending/approved/rejected applications
  - Application details (student, subject, reason)
  - Approve/Reject actions
  - Enrollment creation confirmation

#### 6. **Grade Monitoring**
- **Monitoring Dashboard:**
  - Grade submission status by teacher
  - Completion rates
  - Submission timeliness
  - Grade distribution statistics
- **Analytics:**
  - Grade trends
  - Performance metrics
  - Data quality indicators
- **Reports:**
  - Submission reports
  - Quality assurance reports
  - Anomaly detection alerts

#### 7. **Academic Period Data**
- **Comprehensive Data View:**
  - All subjects in period
  - All enrollments
  - All grades
  - All applications
  - All users
- **Statistics:**
  - Period summary statistics
  - Data completeness metrics
  - Activity summaries

#### 8. **Teacher Section Assignment**
- **Assignment Interface:**
  - Subject list
  - Teacher list
  - Section assignment interface
- **Assignment Results:**
  - Confirmation of assignments
  - Section listings
  - Teacher-subject mappings

#### 9. **System Reports**
- **Data Integrity Reports:**
  - Orphaned records
  - Missing references
  - Inconsistencies
- **Usage Statistics:**
  - User activity
  - Feature usage
  - System performance

#### 10. **Notifications Sent**
- Academic period activation notifications
- System announcements
- Application review notifications (to teachers/students)

---

## 🔄 CROSS-ROLE PROCESSES

### **1. Notification System**
```
Trigger Event (Grade Update, Application Status Change, etc.)
  → NotificationSenderService
  → NotificationTemplateService.createNotificationFromTemplate()
  → NotificationRepository.createNotification()
  → LocalNotificationService.showNotification()
  → Firebase Cloud Messaging (if configured)
  → User receives notification
```

### **2. Offline Sync Process**
```
User Action (Offline)
  → OfflineGradeRepository.saveGrade()
  → Store in Room database (local)
  → Mark as PENDING sync
  → When online:
    → GradeSyncManager.syncPendingGrades()
    → Check for conflicts
    → Resolve conflicts (if any)
    → Update Firestore
    → Mark as SYNCED
    → Update local database
```

### **3. Grade Calculation Process (Universal)**
```
Prelim Grade + Midterm Grade + Final Grade
  → GradeCalculationEngine.calculateFinalAverage()
  → Formula: (Prelim × 0.3) + (Midterm × 0.3) + (Final × 0.4)
  → Determine GradeStatus:
    - >= 75: PASSING
    - >= 60: AT_RISK
    - < 60: FAILING
    - Null: INCOMPLETE
  → Calculate Letter Grade (1.0-5.0 scale)
  → Update StudentGradeAggregate
  → Return calculated values
```

### **4. Enrollment Creation Process**
```
Application Approval (Student Application)
  → Create Enrollment record:
    - studentId
    - subjectId
    - enrolledAt timestamp
    - semester
    - academicYear
    - active = true
  → EnrollmentRepository.createEnrollment()
  → Link student to subject
  → Enable grade tracking
  → Return enrollment ID
```

---

## 📊 DATA FLOW SUMMARY

### **Student Data Flow**
```
Student Input → Application → Teacher Review → Admin Approval → Enrollment → Grade Input → Grade Display → Analytics
```

### **Teacher Data Flow**
```
Teacher Input → Subject Assignment → Grade Input → Grade Calculation → Notification → Student View → Analytics
```

### **Admin Data Flow**
```
Admin Input → Structure Creation → Period Management → Application Review → User Management → System Monitoring → Reports
```

---

## 🔐 SECURITY & VALIDATION

### **Input Validation**
- **Grades:** 0-100 range validation
- **Email:** Format validation
- **Academic Periods:** Date range validation
- **User Roles:** Enum validation
- **References:** Foreign key validation (course, year level, subject)

### **Access Control**
- **Students:** Can only view their own data
- **Teachers:** Can only access assigned subjects
- **Admins:** Full system access
- **Firestore Security Rules:** Enforce role-based access

### **Data Integrity**
- **Grade Calculations:** Automated, cannot be manually overridden
- **Enrollment Validation:** Prevent duplicate enrollments
- **Application Validation:** Prevent duplicate applications
- **Reference Integrity:** Validate all foreign key references

---

## 📈 KEY METRICS & INDICATORS

### **For Students**
- Overall GPA
- Grade completion percentage
- Performance status (Passing/At-Risk/Failing)
- Subject count
- Application status

### **For Teachers**
- Assigned subjects count
- Total students
- Grade submission completion rate
- Pending applications count
- Class average statistics

### **For Admins**
- Total users (by role)
- Total subjects
- Total enrollments
- Pending applications
- Grade submission rates
- System health metrics

---

## 🎯 CONCLUSION

This conceptual framework provides a comprehensive view of all inputs, processes, and outputs for each user role in the Smart Academic Performance Tracker. The system is designed with clear separation of concerns, role-based access control, and automated processes to ensure data accuracy and system reliability.

**Key Strengths:**
- Automated grade calculations
- Real-time notifications
- Offline support with sync
- Comprehensive analytics
- Role-based security
- Hierarchical academic structure

**System Architecture:**
- MVVM pattern
- Repository pattern for data access
- Offline-first with Room database
- Firebase backend for cloud sync
- Jetpack Compose for modern UI

---

**Document Version:** 1.0  
**Last Updated:** Based on codebase scan  
**Total Features Analyzed:** 100+ features across 3 user roles


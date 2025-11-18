# Data Dictionary for Smart Academic Performance Tracker

This document provides a comprehensive data dictionary for all entities in the Smart Academic Performance Tracker application.

---

## Table 1: Data Dictionary for "users" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each user (Firebase Auth UID) | STRING | PK |
| email | User's registered email address | STRING | |
| studentId | Unique student identifier (e.g., "2024-1234") | STRING | |
| teacherId | Unique teacher identifier (e.g., "T-2024-001") | STRING | |
| employeeId | Alternative employee identifier for teachers | STRING | |
| firstName | User's first name | STRING | |
| lastName | User's last name | STRING | |
| middleName | User's middle name | STRING | |
| suffix | Name suffix (Jr., Sr., III, etc.) | STRING | |
| role | User access level (STUDENT, TEACHER, ADMIN) | STRING | |
| profileImageUrl | URL to user's profile image | STRING | |
| createdAt | Timestamp when the user account was created | LONG | |
| active | Whether the user account is active | BOOLEAN | |
| yearLevelId | Reference to YearLevel document ID (for students) | STRING | FK |
| courseId | Reference to Course document ID (for students) | STRING | FK |
| section | Student section (e.g., "A", "B", "1A") | STRING | |
| enrollmentYear | Academic year enrolled (e.g., "2024-2025") | STRING | |
| lastAcademicPeriodId | Last academic period the student was in | STRING | FK |
| departmentCourseId | Reference to Course document ID (for teachers) | STRING | FK |
| employmentType | Employment type (Full-time, Part-time, etc.) | STRING | |
| position | Position title (Professor, Instructor, etc.) | STRING | |
| specialization | Field of expertise | STRING | |
| dateHired | Date hired (Format: "YYYY-MM-DD") | STRING | |
| yearLevelName | Computed field: Year level name for display | STRING | |
| courseName | Computed field: Course name for display | STRING | |
| courseCode | Computed field: Course code for display | STRING | |
| departmentCourseName | Computed field: Department name for display | STRING | |
| departmentCourseCode | Computed field: Department code for display | STRING | |
| lastLoginAt | Timestamp of last login | LONG | |
| passwordChangedAt | Timestamp when password was last changed | LONG | |
| mustChangePassword | Whether user must change password | BOOLEAN | |
| accountSource | Account creation source (MANUAL or PRE_REGISTERED) | STRING | |

---

## Table 2: Data Dictionary for "subjects" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each subject | STRING | PK |
| name | Subject name | STRING | |
| code | Subject code (e.g., "IT101", "CS201") | STRING | |
| description | Subject description | STRING | |
| teacherId | Reference to User document ID (assigned teacher) | STRING | FK |
| teacherName | Teacher's name for display | STRING | |
| credits | Number of credit units | INT | |
| semester | Semester type (FIRST_SEMESTER, SECOND_SEMESTER, SUMMER_CLASS) | ENUM | |
| academicYear | Academic year (e.g., "2024-2025") | STRING | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| active | Whether the subject is active | BOOLEAN | |
| createdAt | Timestamp when the subject was created | LONG | |
| yearLevelId | Reference to YearLevel document ID | STRING | FK |
| courseId | Reference to Course document ID | STRING | FK |
| maxStudents | Maximum number of students that can enroll | INT | |
| numberOfSections | Number of sections for this subject | INT | |
| sections | List of generated section names (e.g., ["IT101A", "IT101B"]) | ARRAY | |
| subjectType | Subject type (MAJOR or MINOR) | ENUM | |
| yearLevelName | Computed field: Year level name for display | STRING | |
| courseName | Computed field: Course name for display | STRING | |
| courseCode | Computed field: Course code for display | STRING | |

---

## Table 3: Data Dictionary for "courses" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each course | STRING | PK |
| name | Course name (e.g., "Information and Communication Technology") | STRING | |
| code | Course code (e.g., "ICT", "IT") | STRING | |
| description | Course description | STRING | |
| duration | Duration in years | INT | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| active | Whether the course is active | BOOLEAN | |
| createdAt | Timestamp when the course was created | LONG | |

---

## Table 4: Data Dictionary for "year_levels" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each year level | STRING | PK |
| courseId | Reference to Course document ID | STRING | FK |
| name | Year level name (e.g., "1st Year", "2nd Year") | STRING | |
| level | Numeric level (1, 2, 3, 4) | INT | |
| description | Year level description | STRING | |
| hasSummerClass | Whether this year level has summer classes | BOOLEAN | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| active | Whether the year level is active | BOOLEAN | |
| createdAt | Timestamp when the year level was created | LONG | |

---

## Table 5: Data Dictionary for "grades" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each grade | STRING | PK |
| studentId | Reference to User document ID (student) | STRING | FK |
| studentName | Student's name for display | STRING | |
| subjectId | Reference to Subject document ID | STRING | FK |
| subjectName | Subject name for display | STRING | |
| teacherId | Reference to User document ID (teacher) | STRING | FK |
| gradePeriod | Grade period (PRELIM, MIDTERM, FINAL) | ENUM | |
| score | Grade score obtained | DOUBLE | |
| maxScore | Maximum possible score | DOUBLE | |
| percentage | Calculated percentage score | DOUBLE | |
| letterGrade | Letter grade (A, B, C, D, F) | STRING | |
| description | Grade description or notes | STRING | |
| dateRecorded | Timestamp when grade was recorded | LONG | |
| semester | Semester name | STRING | |
| academicYear | Academic year | STRING | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| isLocked | Whether the grade is locked from editing | BOOLEAN | |
| editRequested | Whether teacher requested permission to edit | BOOLEAN | |
| lockedAt | Timestamp when grade was locked | LONG | |
| lockedBy | User ID who locked the grade | STRING | FK |
| unlockedBy | Admin ID who unlocked the grade | STRING | FK |
| unlockedAt | Timestamp when grade was unlocked | LONG | |

---

## Table 6: Data Dictionary for "student_enrollments" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each enrollment | STRING | PK |
| studentId | Reference to User document ID (student) | STRING | FK |
| studentName | Student's name for display | STRING | |
| studentEmail | Student's email address | STRING | |
| subjectId | Reference to Subject document ID | STRING | FK |
| subjectName | Subject name for display | STRING | |
| subjectCode | Subject code for display | STRING | |
| sectionName | Section name (e.g., "IT101A", "IT101B") | STRING | |
| teacherId | Reference to User document ID (teacher) | STRING | FK |
| teacherName | Teacher's name for display | STRING | |
| teacherEmail | Teacher's email address | STRING | |
| courseId | Reference to Course document ID | STRING | FK |
| courseName | Course name for display | STRING | |
| yearLevelId | Reference to YearLevel document ID | STRING | FK |
| yearLevelName | Year level name for display | STRING | |
| semester | Semester type | ENUM | |
| academicYear | Academic year | STRING | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| enrollmentDate | Timestamp when student was enrolled | LONG | |
| status | Enrollment status (ACTIVE, DROPPED, COMPLETED, FAILED, KICKED) | ENUM | |
| enrolledBy | User ID who enrolled the student | STRING | FK |
| enrolledByName | Name of user who enrolled the student | STRING | |
| notes | Enrollment notes | STRING | |
| createdAt | Timestamp when the enrollment was created | LONG | |

---

## Table 7: Data Dictionary for "section_assignments" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each assignment | STRING | PK |
| subjectId | Reference to Subject document ID | STRING | FK |
| courseId | Reference to Course document ID | STRING | FK |
| sectionName | Section name (e.g., "IT101A", "IT101B") | STRING | |
| teacherId | Reference to User document ID (teacher) | STRING | FK |
| teacherName | Teacher's name for display | STRING | |
| teacherEmail | Teacher's email address | STRING | |
| assignedAt | Timestamp when assignment was made | LONG | |
| assignedBy | Admin user ID who made the assignment | STRING | FK |
| assignedByName | Admin's name who made the assignment | STRING | |
| status | Assignment status (ACTIVE, INACTIVE, TERMINATED) | ENUM | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| notes | Optional notes about the assignment | STRING | |

---

## Table 8: Data Dictionary for "academic_periods" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each academic period | STRING | PK |
| name | Academic period name (e.g., "Academic Year 2024-2025") | STRING | |
| academicYear | Academic year (e.g., "2024-2025") | STRING | |
| semester | Semester type (FIRST_SEMESTER, SECOND_SEMESTER, SUMMER_CLASS) | ENUM | |
| startDate | Start date timestamp | LONG | |
| endDate | End date timestamp | LONG | |
| isActive | Whether this period is the active period | BOOLEAN | |
| description | Academic period description | STRING | |
| createdAt | Timestamp when the period was created | LONG | |
| createdBy | User ID who created this period | STRING | FK |
| createdByName | Name of user who created this period | STRING | |

---

## Table 9: Data Dictionary for "student_applications" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each application | STRING | PK |
| studentId | Reference to User document ID (student) | STRING | FK |
| studentName | Student's name for display | STRING | |
| studentEmail | Student's email address | STRING | |
| subjectId | Reference to Subject document ID | STRING | FK |
| subjectName | Subject name for display | STRING | |
| subjectCode | Subject code for display | STRING | |
| applicationReason | Reason for application | STRING | |
| status | Application status (PENDING, APPROVED, REJECTED) | ENUM | |
| appliedAt | Timestamp when application was submitted | LONG | |
| reviewedAt | Timestamp when application was reviewed | LONG | |
| reviewedBy | Teacher ID who reviewed the application | STRING | FK |
| teacherComments | Comments from reviewing teacher | STRING | |
| yearLevelId | Reference to YearLevel document ID | STRING | FK |
| courseId | Reference to Course document ID | STRING | FK |
| yearLevelName | Computed field: Year level name for display | STRING | |
| courseName | Computed field: Course name for display | STRING | |
| courseCode | Computed field: Course code for display | STRING | |

---

## Table 10: Data Dictionary for "subject_applications" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each application | STRING | PK |
| studentId | Reference to User document ID (student) | STRING | FK |
| studentName | Student's name for display | STRING | |
| subjectId | Reference to Subject document ID | STRING | FK |
| subjectName | Subject name for display | STRING | |
| sectionName | Specific section the student is applying for | STRING | |
| courseId | Reference to Course document ID | STRING | FK |
| courseName | Course name for display | STRING | |
| yearLevelId | Reference to YearLevel document ID | STRING | FK |
| yearLevelName | Year level name for display | STRING | |
| semester | Semester type | ENUM | |
| academicYear | Academic year | STRING | |
| academicPeriodId | Reference to active academic period | STRING | FK |
| status | Application status (PENDING, APPROVED, REJECTED) | ENUM | |
| appliedDate | Timestamp when application was submitted | LONG | |
| processedDate | Timestamp when application was processed | LONG | |
| processedBy | User ID who processed the application | STRING | FK |
| remarks | Processing remarks | STRING | |
| reviewedDate | Timestamp when application was reviewed | LONG | |
| reviewerName | Name of reviewer | STRING | |
| notes | Application notes | STRING | |
| createdAt | Timestamp when the application was created | LONG | |

---

## Table 11: Data Dictionary for "teacher_applications" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each application | STRING | PK |
| teacherId | Reference to User document ID (teacher) | STRING | FK |
| teacherName | Teacher's name for display | STRING | |
| teacherEmail | Teacher's email address | STRING | |
| subjectId | Reference to Subject document ID | STRING | FK |
| subjectName | Subject name for display | STRING | |
| subjectCode | Subject code for display | STRING | |
| sectionName | Specific section the teacher is applying for | STRING | |
| applicationReason | Reason for application | STRING | |
| status | Application status (PENDING, APPROVED, REJECTED) | ENUM | |
| appliedAt | Timestamp when application was submitted | LONG | |
| reviewedAt | Timestamp when application was reviewed | LONG | |
| reviewedBy | Admin ID who reviewed the application | STRING | FK |
| adminComments | Comments from reviewing admin | STRING | |
| academicPeriodId | Reference to active academic period | STRING | FK |

---

## Table 12: Data Dictionary for "pre_registered_students" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each pre-registration | STRING | PK |
| studentId | Unique student identifier (e.g., "2024-1234") | STRING | |
| firstName | Student's first name | STRING | |
| lastName | Student's last name | STRING | |
| middleName | Student's middle name | STRING | |
| suffix | Name suffix (Jr., Sr., III, etc.) | STRING | |
| courseId | Reference to Course document ID | STRING | FK |
| courseName | Course name for display | STRING | |
| courseCode | Course code for display | STRING | |
| yearLevelId | Reference to YearLevel document ID | STRING | FK |
| yearLevelName | Year level name for display | STRING | |
| section | Student section (e.g., "A", "B", "1A") | STRING | |
| enrollmentYear | Academic year enrolled (e.g., "2024-2025") | STRING | |
| email | Optional institutional email | STRING | |
| phoneNumber | Optional phone number | STRING | |
| dateOfBirth | Date of birth (Format: "YYYY-MM-DD") | STRING | |
| address | Optional address | STRING | |
| isRegistered | Whether account is activated | BOOLEAN | |
| registeredAt | Timestamp when account was activated | LONG | |
| firebaseUserId | Firebase Auth UID after activation | STRING | FK |
| createdBy | Admin user ID who added this record | STRING | FK |
| createdByName | Admin name for display | STRING | |
| createdAt | Timestamp when record was created | LONG | |
| updatedAt | Timestamp when record was updated | LONG | |
| updatedBy | User ID who last updated the record | STRING | FK |
| notes | Admin notes about the student | STRING | |
| active | Whether the record is active | BOOLEAN | |
| emailSent | Whether welcome email was sent | BOOLEAN | |
| emailSentAt | Timestamp when email was sent | LONG | |

---

## Table 13: Data Dictionary for "pre_registered_teachers" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each pre-registration | STRING | PK |
| teacherId | Unique teacher identifier (e.g., "T-2024-001") | STRING | |
| firstName | Teacher's first name | STRING | |
| lastName | Teacher's last name | STRING | |
| middleName | Teacher's middle name | STRING | |
| suffix | Name suffix (Jr., Sr., III, etc.) | STRING | |
| departmentCourseId | Reference to Course document ID (department) | STRING | FK |
| departmentCourseName | Department name for display | STRING | |
| departmentCourseCode | Department code for display | STRING | |
| employmentType | Employment type (FULL_TIME, PART_TIME, etc.) | ENUM | |
| position | Position title (Professor, Instructor, etc.) | STRING | |
| specialization | Field of expertise | STRING | |
| email | Optional institutional email | STRING | |
| phoneNumber | Optional phone number | STRING | |
| dateOfBirth | Date of birth (Format: "YYYY-MM-DD") | STRING | |
| address | Optional address | STRING | |
| dateHired | Date hired (Format: "YYYY-MM-DD") | STRING | |
| employeeNumber | Alternative employee identifier | STRING | |
| isRegistered | Whether account is activated | BOOLEAN | |
| registeredAt | Timestamp when account was activated | LONG | |
| firebaseUserId | Firebase Auth UID after activation | STRING | FK |
| createdBy | Admin user ID who added this record | STRING | FK |
| createdByName | Admin name for display | STRING | |
| createdAt | Timestamp when record was created | LONG | |
| updatedAt | Timestamp when record was updated | LONG | |
| updatedBy | User ID who last updated the record | STRING | FK |
| notes | Admin notes about the teacher | STRING | |
| active | Whether the record is active | BOOLEAN | |
| emailSent | Whether welcome email was sent | BOOLEAN | |
| emailSentAt | Timestamp when email was sent | LONG | |

---

## Table 14: Data Dictionary for "notifications" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each notification | STRING | PK |
| userId | Reference to User document ID (recipient) | STRING | FK |
| title | Notification title | STRING | |
| message | Notification message content | STRING | |
| type | Notification type (GRADE_UPDATE, APPLICATION_APPROVED, etc.) | ENUM | |
| priority | Notification priority (LOW, NORMAL, HIGH, URGENT) | ENUM | |
| isRead | Whether notification has been read | BOOLEAN | |
| isDelivered | Whether notification has been delivered | BOOLEAN | |
| createdAt | Timestamp when notification was created | TIMESTAMP | |
| readAt | Timestamp when notification was read | TIMESTAMP | |
| data | Additional data as key-value pairs | MAP | |
| actionUrl | URL for notification action | STRING | |
| academicPeriodId | Reference to active academic period | STRING | FK |

---

## Table 15: Data Dictionary for "audit_trail" Entity

| Attribute | Description | Data Type | PK/FK |
|-----------|-------------|-----------|-------|
| id | Unique identifier for each audit entry | STRING | PK |
| gradeId | Reference to Grade document ID | STRING | FK |
| studentId | Reference to User document ID (student) | STRING | FK |
| studentName | Student's name for display | STRING | |
| subjectId | Reference to Subject document ID | STRING | FK |
| subjectName | Subject name for display | STRING | |
| teacherId | Reference to User document ID (teacher) | STRING | FK |
| teacherName | Teacher's name for display | STRING | |
| action | Audit action type (CREATED, UPDATED, DELETED, etc.) | ENUM | |
| oldValue | Previous grade value | DOUBLE | |
| newValue | New grade value | DOUBLE | |
| oldLetterGrade | Previous letter grade | STRING | |
| newLetterGrade | New letter grade | STRING | |
| gradePeriod | Grade period (PRELIM, MIDTERM, FINAL) | ENUM | |
| timestamp | Timestamp when action occurred | LONG | |
| semester | Semester name | STRING | |
| academicYear | Academic year | STRING | |
| reason | Reason for the change | STRING | |
| ipAddress | IP address of the user who made the change | STRING | |
| userAgent | User agent string of the client | STRING | |

---

## Notes

1. **Primary Keys (PK)**: All entities use `id` as the primary key, which is a STRING containing the Firestore document ID.

2. **Foreign Keys (FK)**: Foreign key relationships are indicated in the PK/FK column. These reference other entities' `id` fields.

3. **Timestamps**: Most entities include `createdAt` timestamps stored as LONG (milliseconds since epoch) or TIMESTAMP (Firebase Timestamp).

4. **Computed Fields**: Some entities include computed/denormalized fields (e.g., `yearLevelName`, `courseName`) that are populated by the repository layer for display purposes.

5. **Enums**: Several attributes use ENUM types. Common enums include:
   - `UserRole`: STUDENT, TEACHER, ADMIN
   - `Semester`: FIRST_SEMESTER, SECOND_SEMESTER, SUMMER_CLASS
   - `GradePeriod`: PRELIM, MIDTERM, FINAL
   - `EnrollmentStatus`: ACTIVE, DROPPED, COMPLETED, FAILED, KICKED
   - `AssignmentStatus`: ACTIVE, INACTIVE, TERMINATED
   - `ApplicationStatus`: PENDING, APPROVED, REJECTED
   - `NotificationType`: Various notification types
   - `NotificationPriority`: LOW, NORMAL, HIGH, URGENT
   - `AuditAction`: CREATED, UPDATED, DELETED, etc.
   - `SubjectType`: MAJOR, MINOR
   - `EmploymentType`: FULL_TIME, PART_TIME, CONTRACT, etc.

6. **Data Storage**: This application uses Firebase Firestore as the backend database, which is a NoSQL document database. All data types are mapped accordingly.


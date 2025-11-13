# User Types Comparison - Quick Reference

## 📊 Three User Types at a Glance

| Feature | 👨‍🎓 Student | 👨‍🏫 Teacher | 👨‍💼 Administrator |
|---------|------------|------------|-------------------|
| **ID Format** | `YYYY-NNNNN` | `T-YYYY-NNN` or `EMP-NNNNN` | `A-YYYY-NNN` |
| **Example ID** | `2024-12345` | `T-2024-001` | `A-2024-001` |
| **ID Prefix** | Year | `T-` or `EMP-` | `A-` |
| **Sequential Digits** | 5 digits (00001-99999) | 3 digits (001-999) | 3 digits (001-999) |
| **Theme Color** | Blue (#2196F3) | Blue (#2196F3) | Purple (#9C27B0) |
| **Email Format** | `s{id}@sjp2cd.edu.ph` | `{id}@sjp2cd.edu.ph` | `{id}@sjp2cd.edu.ph` |
| **Account Activation** | ✅ Available | ✅ Available | ❌ Not Available |
| **Self-Registration** | ✅ Via pre-registration | ✅ Via pre-registration | ❌ Admin-created only |
| **Sign Up Link** | ✅ Visible | ✅ Visible | ❌ Hidden |
| **Dashboard Route** | `student_dashboard` | `teacher_dashboard` | `admin_dashboard` |
| **Pre-Registration Collection** | `pre_registered_students` | `pre_registered_teachers` | N/A |
| **Required Fields** | Name, Course, Year Level | Name, Department | Name, Email |
| **Max Users per Year** | 99,999 | 999 (per format) | 999 |
| **Rate Limiting** | ✅ Enabled | ✅ Enabled | ✅ Enabled |
| **Password Reset** | ✅ Firebase Email | ✅ Firebase Email | ✅ Firebase Email |

---

## 🔍 ID Format Details

### Student ID: `YYYY-NNNNN`
```
Components:
├── YYYY: 4-digit enrollment year (e.g., 2024)
└── NNNNN: 5-digit sequential number (e.g., 12345)

Example: 2024-12345
Email:   s2024-12345@sjp2cd.edu.ph
```

### Teacher ID: `T-YYYY-NNN`
```
Components:
├── T: Teacher prefix
├── YYYY: 4-digit hiring year (e.g., 2024)
└── NNN: 3-digit sequential number (e.g., 001)

Example: T-2024-001
Email:   t-2024-001@sjp2cd.edu.ph

Alternative: EMP-NNNNN
Example: EMP-12345
Email:   emp-12345@sjp2cd.edu.ph
```

### Admin ID: `A-YYYY-NNN`
```
Components:
├── A: Admin prefix
├── YYYY: 4-digit year (e.g., 2024)
└── NNN: 3-digit sequential number (e.g., 001)

Example: A-2024-001
Email:   a-2024-001@sjp2cd.edu.ph
```

---

## 🎨 Visual Design Comparison

### Sign-In Screen Layout

```
┌─────────────────────────────────────┐
│   Smart Academic Tracker Sign In    │
├─────────────────────────────────────┤
│                                     │
│  I am a:                            │
│  ┌────────────┐  ┌────────────┐   │
│  │  Student   │  │  Teacher   │   │ ← Blue chips
│  └────────────┘  └────────────┘   │
│  ┌─────────────────────────────┐  │
│  │     Administrator 🛡️        │  │ ← Purple chip (full width)
│  └─────────────────────────────┘  │
│                                     │
│  [ID Input Field]                   │ ← Dynamic label
│  [Password Field]                   │
│                                     │
│  [Sign In Button]                   │ ← Color matches selection
│                                     │
│  Conditional Options:               │
│  - Students/Teachers: Show          │
│    "Activate Account" and           │
│    "Sign Up" links                  │
│  - Admin: Show "Administrator       │
│    access only" message             │
└─────────────────────────────────────┘
```

### Color Schemes

**Student/Teacher (Blue Theme)**
```
Primary Color:   #2196F3 (Blue)
Focus Border:    #2196F3
Button:          #2196F3
Icon:            #2196F3
```

**Administrator (Purple Theme)**
```
Primary Color:   #9C27B0 (Purple)
Focus Border:    #9C27B0
Button:          #9C27B0
Icon:            #9C27B0
```

---

## 🔐 Access Levels Comparison

| Permission | Student | Teacher | Admin |
|------------|---------|---------|-------|
| **View Own Data** | ✅ | ✅ | ✅ |
| **View Course Data** | ✅ (enrolled) | ✅ (assigned) | ✅ (all) |
| **Submit Grades** | ❌ | ✅ | ✅ (override) |
| **Manage Students** | ❌ | ❌ | ✅ |
| **Manage Teachers** | ❌ | ❌ | ✅ |
| **Pre-Registration Management** | ❌ | ❌ | ✅ |
| **System Settings** | ❌ | ❌ | ✅ |
| **User Reports** | ❌ | ✅ (own classes) | ✅ (all) |
| **Audit Logs** | ❌ | ❌ | ✅ |

---

## 📱 User Experience Flow

### Student Login Flow
```
1. Select "Student"
2. Enter Student ID (YYYY-NNNNN)
3. Enter Password
4. Click "Sign In"
5. → Student Dashboard

Alternative: First Time User
1. Click "First Time? Activate Account"
2. Enter Student ID
3. Set Password
4. Account Activated
5. → Student Dashboard
```

### Teacher Login Flow
```
1. Select "Teacher"
2. Enter Teacher ID (T-YYYY-NNN or EMP-NNNNN)
3. Enter Password
4. Click "Sign In"
5. → Teacher Dashboard

Alternative: First Time User
1. Click "First Time? Activate Account"
2. Enter Teacher ID
3. Set Password
4. Account Activated
5. → Teacher Dashboard
```

### Admin Login Flow
```
1. Select "Administrator"
2. Enter Admin ID (A-YYYY-NNN)
3. Enter Password
4. Click "Sign In"
5. → Admin Dashboard

Note: No activation flow for admins
Accounts must be pre-created by super-admin
```

---

## 🛡️ Security Comparison

| Security Feature | Student | Teacher | Admin |
|-----------------|---------|---------|-------|
| **Pre-Registration Required** | ✅ | ✅ | ❌ |
| **Self-Service Activation** | ✅ | ✅ | ❌ |
| **Manual Creation** | ❌ | ❌ | ✅ |
| **Rate Limiting** | 5 attempts / 30 min | 5 attempts / 30 min | 5 attempts / 30 min |
| **Password Requirements** | Strong (8+ chars, mixed) | Strong (8+ chars, mixed) | Strong (8+ chars, mixed) |
| **Firebase Auth** | ✅ | ✅ | ✅ |
| **Role-Based Access** | ✅ | ✅ | ✅ |
| **Firestore Security Rules** | ✅ | ✅ | ✅ (strictest) |

---

## 📊 Data Model Comparison

### Student User Document
```json
{
  "id": "firebase_uid",
  "email": "s2024-12345@sjp2cd.edu.ph",
  "studentId": "2024-12345",
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "role": "STUDENT",
  "courseId": "course_001",
  "courseName": "Computer Science",
  "yearLevelId": "year_001",
  "yearLevelName": "1st Year",
  "section": "A",
  "enrollmentYear": "2024-2025"
}
```

### Teacher User Document
```json
{
  "id": "firebase_uid",
  "email": "t-2024-001@sjp2cd.edu.ph",
  "teacherId": "T-2024-001",
  "firstName": "Maria",
  "lastName": "Santos",
  "role": "TEACHER",
  "departmentCourseId": "course_001",
  "departmentCourseName": "Computer Science",
  "position": "Professor",
  "employmentType": "FULL_TIME",
  "specialization": "Database Systems"
}
```

### Admin User Document
```json
{
  "id": "firebase_uid",
  "email": "a-2024-001@sjp2cd.edu.ph",
  "adminId": "A-2024-001",
  "firstName": "Carlos",
  "lastName": "Admin",
  "role": "ADMIN",
  "isActive": true,
  "createdAt": 1699804800000
}
```

---

## 🔄 Account Creation Process

### Student/Teacher (Pre-Registration System)
```
Step 1: Admin adds record to pre_registered_students/teachers
   ↓
Step 2: Student/Teacher clicks "Activate Account"
   ↓
Step 3: Enter Student/Teacher ID
   ↓
Step 4: System validates ID against pre-registration
   ↓
Step 5: User sets password
   ↓
Step 6: System creates Firebase Auth user
   ↓
Step 7: System creates Firestore user document
   ↓
Step 8: Mark pre-registration as "isRegistered: true"
   ↓
Step 9: User can now log in
```

### Admin (Direct Creation)
```
Step 1: Super-admin accesses Firebase Console/Backend
   ↓
Step 2: Create Firebase Auth user
   │   Email: a-2024-001@sjp2cd.edu.ph
   │   Password: [Set securely]
   ↓
Step 3: Create Firestore user document
   │   Collection: users
   │   role: "ADMIN"
   ↓
Step 4: Admin can now log in
   │   No activation needed
```

---

## 🎯 Use Cases

### Student Use Cases
- ✅ View grades and academic performance
- ✅ Access course materials
- ✅ Track attendance
- ✅ View schedules
- ✅ Submit assignments (if implemented)
- ✅ View academic progress reports

### Teacher Use Cases
- ✅ Manage assigned classes
- ✅ Input/update student grades
- ✅ View class rosters
- ✅ Track student performance
- ✅ Generate class reports
- ✅ Manage attendance

### Admin Use Cases
- ✅ Manage all users (students, teachers, admins)
- ✅ Pre-register students and teachers
- ✅ View system-wide reports
- ✅ Configure courses and year levels
- ✅ Manage academic year settings
- ✅ Access audit logs
- ✅ System configuration

---

## 📈 Scalability

| Aspect | Student | Teacher | Admin |
|--------|---------|---------|-------|
| **Max IDs per Year** | 99,999 | 999 (T-format) / 99,999 (EMP-format) | 999 |
| **Firestore Collection** | `users` | `users` | `users` |
| **Pre-Registration Collection** | `pre_registered_students` | `pre_registered_teachers` | N/A |
| **Index Requirements** | `studentId` | `teacherId` | `adminId` |
| **Query Performance** | Optimized with indexes | Optimized with indexes | Optimized with indexes |

---

## 🆘 Quick Troubleshooting

### Student/Teacher Login Issues
| Problem | Solution |
|---------|----------|
| "ID not found" | Check pre-registration exists |
| "Account not activated" | Complete activation process first |
| "Invalid ID format" | Check format matches YYYY-NNNNN or T-YYYY-NNN |
| "Account locked" | Wait 30 minutes or contact admin |

### Admin Login Issues
| Problem | Solution |
|---------|----------|
| "Invalid credentials" | Verify admin account exists in Firebase |
| "Access denied" | Check Firestore user document has role: "ADMIN" |
| "Invalid ID format" | Check format matches A-YYYY-NNN |
| "Not redirecting" | Verify admin_dashboard route exists |

---

## 📝 Quick Reference Commands

### Generate IDs (Kotlin)
```kotlin
// Student ID
val studentId = IdValidator.generateNextStudentId(2024, 12345)
// Result: "2024-12346"

// Teacher ID
val teacherId = IdValidator.generateNextTeacherId(2024, 1)
// Result: "T-2024-002"

// Admin ID
val adminId = IdValidator.generateNextAdminId(2024, 0)
// Result: "A-2024-001"
```

### Validate IDs (Kotlin)
```kotlin
// Student
val result = IdValidator.validateStudentId("2024-12345")

// Teacher
val result = IdValidator.validateTeacherId("T-2024-001")

// Admin
val result = IdValidator.validateAdminId("A-2024-001")
```

### Check ID Type (Kotlin)
```kotlin
if (IdValidator.isStudentIdFormat(id)) { /* Handle student */ }
if (IdValidator.isTeacherIdFormat(id)) { /* Handle teacher */ }
if (IdValidator.isAdminIdFormat(id)) { /* Handle admin */ }
```

---

## 🎉 Summary

The Smart Academic Tracker now supports **three distinct user types** with:
- ✅ Unique ID formats for each role
- ✅ Role-specific UI themes and flows
- ✅ Comprehensive validation and security
- ✅ Scalable architecture
- ✅ Clear separation of concerns

Choose the right user type for your needs and enjoy a tailored experience!

---

**Last Updated:** November 12, 2025  
**Version:** 1.1.0  
**Document Type:** Quick Reference Guide


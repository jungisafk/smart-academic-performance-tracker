# 🧪 ID-Based Authentication - Testing Guide

**Project**: Smart Academic Performance Tracker  
**Feature**: Student/Teacher ID-Based Authentication  
**Date**: November 12, 2025

---

## 📋 Test Preparation

### 1. **Firebase Setup**
```bash
# Deploy security rules
firebase deploy --only firestore:rules

# Verify deployment
firebase firestore:rules:list
```

### 2. **Configuration Check**
- ✅ School domain configured in `UserRepository.kt`
- ✅ Firebase project connected
- ✅ Internet connectivity available

---

## 🧪 TEST SCENARIOS

### **Scenario 1: Admin Pre-Registers Student**

#### Test Data:
```json
{
  "studentId": "2024-00001",
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "middleName": "Santos",
  "courseId": "course_it",
  "courseName": "Bachelor of Science in Information Technology",
  "courseCode": "BSIT",
  "yearLevelId": "year_1",
  "yearLevelName": "1st Year",
  "section": "A",
  "enrollmentYear": "2024-2025",
  "isRegistered": false
}
```

#### Steps:
1. **Login as Admin**
   - Navigate to Admin Dashboard
   - Verify admin access

2. **Access Pre-Registration**
   - Click "Pre-Registered Students" (or navigate to screen)
   - Screen should load with empty list or existing students

3. **Add New Student**
   - Click FAB (+) button
   - Fill in form with test data above
   - Click "Add Student"
   - Verify success message
   - Student appears in list with "Awaiting Activation" badge

4. **Verify Data**
   - Check Firestore `pre_registered_students` collection
   - Confirm document created with correct fields
   - `isRegistered` should be `false`

**Expected Result**: ✅ Student added successfully, shows in pending list

---

### **Scenario 2: Student Activates Account**

#### Prerequisites:
- Student "2024-00001" exists in pre-registration

#### Steps:
1. **Navigate to Activation**
   - Open app
   - Click "First Time? Activate Account" button

2. **Enter Student Information**
   - Select "Student" user type
   - Enter Student ID: `2024-00001`
   - Verify ID validation (should show green if valid)

3. **Create Password**
   - Enter password: `Student@2024`
   - Observe password strength indicator
   - Should show "Strong" or "Very Strong"
   - Confirm password: `Student@2024`
   - Should show "Passwords match" message

4. **Activate Account**
   - Click "Activate Account" button
   - Observe loading indicator
   - Success message should appear
   - Auto-redirect to sign-in screen after 2 seconds

5. **Verify Firebase**
   - Check Firebase Authentication
   - User should be created with email: `s2024-00001@sjp2cd.edu.ph`
   - Check Firestore `users` collection
   - User document should have all profile data populated:
     - studentId: "2024-00001"
     - firstName: "Juan"
     - lastName: "Dela Cruz"
     - courseName: "Bachelor of Science in Information Technology"
     - yearLevelName: "1st Year"
     - section: "A"

6. **Verify Pre-Registration Update**
   - Check Firestore `pre_registered_students` collection
   - Document should have:
     - `isRegistered`: true
     - `registeredAt`: timestamp
     - `firebaseUserId`: user's Firebase UID

**Expected Result**: ✅ Account activated, profile auto-populated, can now sign in

---

### **Scenario 3: Student Sign-In**

#### Prerequisites:
- Student "2024-00001" account activated

#### Steps:
1. **Navigate to Sign-In**
   - Open sign-in screen
   - Verify UI shows ID-based fields

2. **Enter Credentials**
   - Select "Student" user type
   - Enter Student ID: `2024-00001`
   - Enter password: `Student@2024`

3. **Sign In**
   - Click "Sign In" button
   - Observe loading indicator
   - Success message appears
   - Redirects to Student Dashboard

4. **Verify Dashboard**
   - Student Dashboard loads
   - Welcome message shows student name
   - Profile info displays correct data:
     - Name: Juan Santos Dela Cruz
     - Course: BSIT - 1st Year
     - Section: A

**Expected Result**: ✅ Successful sign-in, correct profile data displayed

---

### **Scenario 4: Failed Login - Wrong Password**

#### Steps:
1. **Enter Credentials**
   - Student ID: `2024-00001`
   - Password: `WrongPassword`

2. **Attempt Sign-In**
   - Click "Sign In"
   - Error message appears
   - Should show: "Invalid credentials. 4 attempt(s) remaining."

3. **Retry 4 More Times**
   - Each failure shows decreasing attempts
   - After 5th failure:
     - Error: "Account is locked. Please try again in 30 minutes."
     - Sign-in button disabled

4. **Verify Lockout**
   - Check Firestore `login_attempts` collection
   - Document for student ID should have:
     - `attempts`: 5
     - `lockedUntil`: timestamp (30 min from now)

**Expected Result**: ✅ Account locked after 5 failed attempts

---

### **Scenario 5: Invalid Student ID**

#### Steps:
1. **Enter Invalid ID**
   - Student ID: `2024-123` (too short)
   - Observe validation error
   - Should show: "Invalid Student ID format. Expected format: YYYY-NNNNN"

2. **Try Different Invalid Formats**
   - `24-12345` - Error: "Invalid year"
   - `2024/12345` - Error: "Invalid format"
   - `2024 12345` - Error: "Invalid format"

**Expected Result**: ✅ Validation prevents invalid IDs

---

### **Scenario 6: Duplicate Student ID**

#### Steps:
1. **Admin Adds Duplicate**
   - Try to add student with existing ID: `2024-00001`
   - Click "Add Student"
   - Error message: "Student ID 2024-00001 already exists"

**Expected Result**: ✅ Duplicate prevention works

---

### **Scenario 7: Teacher Pre-Registration & Activation**

#### Test Data:
```json
{
  "teacherId": "T-2024-001",
  "firstName": "Maria",
  "lastName": "Santos",
  "middleName": "Reyes",
  "departmentCourseId": "course_it",
  "departmentCourseName": "Information Technology",
  "departmentCourseCode": "IT",
  "employmentType": "FULL_TIME",
  "position": "Associate Professor",
  "specialization": "Software Engineering",
  "isRegistered": false
}
```

#### Steps:
1. **Admin Pre-Registers Teacher**
   - Navigate to "Pre-Registered Teachers"
   - Add teacher with data above
   - Verify added successfully

2. **Teacher Activates Account**
   - Go to Activation screen
   - Select "Teacher" user type
   - Enter Teacher ID: `T-2024-001`
   - Create password: `Teacher@2024`
   - Activate account

3. **Verify Teacher Profile**
   - Sign in as teacher
   - Check profile shows:
     - Name: Maria Reyes Santos
     - Department: IT - Information Technology
     - Position: Associate Professor
     - Employment: Full-time

**Expected Result**: ✅ Teacher registration and activation works identically to student

---

### **Scenario 8: Search and Filter**

#### Prerequisites:
- Multiple pre-registered students (some activated, some pending)

#### Steps:
1. **Search by ID**
   - Enter: `2024-00001`
   - Should filter to matching student

2. **Search by Name**
   - Enter: `Juan`
   - Should show all students with "Juan" in name

3. **Filter by Status**
   - Click "Pending" chip
   - Should show only non-activated students
   - Click "Activated" chip
   - Should show only activated students
   - Click "All" chip
   - Should show all students

**Expected Result**: ✅ Search and filtering work correctly

---

### **Scenario 9: Password Strength Validation**

#### Test Passwords:
1. **Weak Password**: `password`
   - Should show: "Password must contain uppercase, number, special char"
   - Strength: Weak (red)

2. **Fair Password**: `Password1`
   - Should show: "Password must contain special character"
   - Strength: Fair (orange)

3. **Good Password**: `Password1!`
   - Validation passes
   - Strength: Good (yellow)

4. **Strong Password**: `P@ssw0rd123!`
   - Validation passes
   - Strength: Strong (green)

**Expected Result**: ✅ Password validation and strength indicator work

---

### **Scenario 10: Account Lockout Auto-Reset**

#### Steps:
1. **Lock Account**
   - Make 5 failed login attempts
   - Account locked for 30 minutes

2. **Wait 15 Minutes of Inactivity**
   - Don't make any attempts
   - After 15 minutes, attempts should reset

3. **Try Login**
   - Should allow login attempt
   - Attempts counter reset to 0

**Expected Result**: ✅ Auto-reset after inactivity works

---

## 🔍 EDGE CASES TO TEST

### 1. **Network Failures**
- Turn off internet during activation
- Should show error message
- Should not create partial accounts

### 2. **Already Activated Account**
- Try to activate an already-activated student ID
- Should show: "Account already activated. Please sign in."

### 3. **Missing Pre-Registration**
- Try to activate with ID not in pre-registration
- Should show: "Student ID not found. Please contact administration."

### 4. **Concurrent Activations**
- Two devices try to activate same ID simultaneously
- Only one should succeed

### 5. **Special Characters in Names**
- Names with: ñ, é, ü, apostrophes, hyphens
- Should handle correctly

---

## ✅ ACCEPTANCE CRITERIA

### Core Functionality:
- [ ] Admin can pre-register students
- [ ] Admin can pre-register teachers
- [ ] Students can activate accounts using student ID
- [ ] Teachers can activate accounts using teacher ID
- [ ] Activated users can sign in with their ID
- [ ] Profile data auto-populated from pre-registration
- [ ] Cannot change ID after registration

### Security:
- [ ] ID format validation works
- [ ] Password strength validation works
- [ ] Rate limiting works (5 attempts)
- [ ] Account lockout works (30 minutes)
- [ ] Auto-reset works (15 minutes inactivity)
- [ ] Cannot activate already-activated account
- [ ] Cannot activate non-existent ID

### UI/UX:
- [ ] ID validation shows real-time feedback
- [ ] Password strength indicator works
- [ ] Success messages display correctly
- [ ] Error messages are clear and helpful
- [ ] Loading indicators show during operations
- [ ] Auto-redirect after activation works

### Data Integrity:
- [ ] Firestore documents created correctly
- [ ] Firebase Auth accounts created correctly
- [ ] Pre-registration marked as registered
- [ ] No duplicate IDs allowed
- [ ] All fields populated correctly

---

## 📊 PERFORMANCE TESTING

### Load Testing:
1. **Bulk Import**
   - Import 100 students via CSV
   - Should complete in < 30 seconds
   - All records created correctly

2. **Large List**
   - View list with 500+ pre-registered students
   - Should load in < 3 seconds
   - Smooth scrolling

3. **Search Performance**
   - Search in list of 500+ students
   - Results appear in < 500ms

---

## 🐛 KNOWN ISSUES

Document any issues found during testing:

| Issue | Severity | Status | Notes |
|-------|----------|--------|-------|
| _Example: Slow loading with 1000+ records_ | _Medium_ | _Open_ | _Consider pagination_ |

---

## 📝 TEST CHECKLIST

### Before Testing:
- [ ] Firebase rules deployed
- [ ] School domain configured
- [ ] Test data prepared
- [ ] Admin account available

### During Testing:
- [ ] Follow scenarios in order
- [ ] Document all issues
- [ ] Take screenshots of errors
- [ ] Note performance issues

### After Testing:
- [ ] All scenarios passed
- [ ] Edge cases tested
- [ ] Performance acceptable
- [ ] Documentation updated

---

## 🎯 SUCCESS METRICS

- **Pass Rate**: > 95% of test scenarios pass
- **Performance**: All operations < 3 seconds
- **Security**: All security tests pass
- **UX**: No critical usability issues

---

## 📞 TESTING SUPPORT

### Sample Test Data Generator:
```kotlin
// Generate 10 test students
fun generateTestStudents(): List<PreRegisteredStudent> {
    return (1..10).map { i ->
        PreRegisteredStudent(
            studentId = "2024-${i.toString().padStart(5, '0')}",
            firstName = "Student",
            lastName = "$i",
            courseId = "course_it",
            courseName = "BSIT",
            courseCode = "BSIT",
            yearLevelId = "year_1",
            yearLevelName = "1st Year",
            enrollmentYear = "2024-2025",
            createdBy = "admin_test"
        )
    }
}
```

---

**Testing Status**: Ready for execution  
**Last Updated**: November 12, 2025  
**Tester**: ___________  
**Date Tested**: ___________


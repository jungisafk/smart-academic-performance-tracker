# 🆔 Institutional ID Format Reference

## Quick Reference Card

This document covers the ID formats for **three user types**: Students, Teachers, and Administrators.

### Student ID Format
```
Format: YYYY-NNNN
Example: 2024-1234

Where:
  YYYY = 4-digit enrollment year (2000-current+1)
  NNNN = 4-digit sequential number (0001-9999)
```

**Validation Regex**: `^\d{4}-\d{4}$`

**Examples**:
- ✅ `2024-1234` - Valid
- ✅ `2023-0001` - Valid (leading zeros)
- ✅ `2025-9999` - Valid (max number)
- ✅ `2022-2563` - Valid
- ❌ `24-1234` - Invalid (year too short)
- ❌ `2024-123` - Invalid (number too short)
- ❌ `2024-12345` - Invalid (number too long)
- ❌ `2024 1234` - Invalid (space instead of dash)

---

### Teacher ID Format (Option 1)
```
Format: T-YYYY-NNN
Example: T-2024-001

Where:
  T = Teacher prefix (uppercase)
  YYYY = 4-digit hiring year (2000-current+1)
  NNN = 3-digit sequential number (001-999)
```

**Validation Regex**: `^T-\d{4}-\d{3}$`

**Examples**:
- ✅ `T-2024-001` - Valid
- ✅ `T-2023-999` - Valid (max number)
- ✅ `t-2024-001` - Valid (auto-converted to uppercase)
- ❌ `T-24-001` - Invalid (year too short)
- ❌ `T-2024-01` - Invalid (number too short)
- ❌ `T-2024-1234` - Invalid (number too long)
- ❌ `2024-001` - Invalid (missing T prefix)

---

### Teacher ID Format (Option 2)
```
Format: EMP-NNNNN
Example: EMP-12345

Where:
  EMP = Employee prefix (uppercase)
  NNNNN = 5-digit employee number (00001-99999)
```

**Validation Regex**: `^EMP-\d{5}$`

**Examples**:
- ✅ `EMP-12345` - Valid
- ✅ `EMP-00001` - Valid (leading zeros)
- ✅ `emp-12345` - Valid (auto-converted to uppercase)
- ❌ `EMP-1234` - Invalid (number too short)
- ❌ `EMP-123456` - Invalid (number too long)
- ❌ `E-12345` - Invalid (wrong prefix)

---

### Administrator ID Format
```
Format: A-YYYY-NNN
Example: A-2024-001

Where:
  A = Admin prefix (uppercase)
  YYYY = 4-digit year (2000-current+1)
  NNN = 3-digit sequential number (001-999)
```

**Validation Regex**: `^A-\d{4}-\d{3}$`

**Examples**:
- ✅ `A-2024-001` - Valid
- ✅ `A-2023-999` - Valid (max number)
- ✅ `a-2024-001` - Valid (auto-converted to uppercase)
- ❌ `A-24-001` - Invalid (year too short)
- ❌ `A-2024-01` - Invalid (number too short)
- ❌ `A-2024-1234` - Invalid (number too long)
- ❌ `2024-001` - Invalid (missing A prefix)

**Security Note**: Admin accounts must be created through secure backend processes or Firebase Console. There is no self-service registration or account activation for administrators.

---

## Conversion to Email Format

For Firebase Authentication, IDs are converted to email format:

### Student ID → Email
```
Input:  2024-1234
Output: s2024-1234@sjp2cd.edu.ph
```

### Teacher ID → Email
```
Input:  T-2024-001
Output: t-2024-001@smartacademic.edu

Input:  EMP-12345
Output: temp-12345@smartacademic.edu
```

### Admin ID → Email
```
Input:  A-2024-001
Output: a-2024-001@smartacademic.edu
```

**Note**: Replace `smartacademic.edu` with your institution's domain in `UserRepository.kt` (currently set to `sjp2cd.edu.ph`)

---

## ID Generation Examples

### Kotlin Code
```kotlin
    // Generate next student ID for 2024
    val nextStudentId = IdValidator.generateNextStudentId(2024, 1234)
    // Result: "2024-1235"

// Generate next teacher ID for 2024
val nextTeacherId = IdValidator.generateNextTeacherId(2024, 1)
// Result: "T-2024-002"

// Generate next admin ID for 2024
val nextAdminId = IdValidator.generateNextAdminId(2024, 0)
// Result: "A-2024-001"
```

### Manual Generation
```
    Student:
    Year: 2024
    Last Number: 0050
    Next ID: 2024-0051

Teacher:
Year: 2024
Last Number: 005
Next ID: T-2024-006

Admin:
Year: 2024
Last Number: 002
Next ID: A-2024-003
```

---

## ID Validation in Code

```kotlin
    // Validate Student ID
    val result = IdValidator.validateStudentId("2024-1234")
if (result.isValid) {
    println("Valid student ID")
} else {
    println("Error: ${result.errorMessage}")
}

// Validate Teacher ID
val result = IdValidator.validateTeacherId("T-2024-001")
if (result.isValid) {
    println("Valid teacher ID")
} else {
    println("Error: ${result.errorMessage}")
}

// Validate Admin ID
val result = IdValidator.validateAdminId("A-2024-001")
if (result.isValid) {
    println("Valid admin ID")
} else {
    println("Error: ${result.errorMessage}")
}

    // Auto-detect ID type
    val studentId = "2024-1234"
val teacherId = "T-2024-001"
val adminId = "A-2024-001"

if (IdValidator.isStudentIdFormat(studentId)) {
    println("This is a student ID")
}
if (IdValidator.isTeacherIdFormat(teacherId)) {
    println("This is a teacher ID")
}
if (IdValidator.isAdminIdFormat(adminId)) {
    println("This is an admin ID")
}
```

---

## Best Practices

### For Administrators:
1. ✅ Use consistent ID format across all systems
2. ✅ Start new year sequences at 00001 or 001
3. ✅ Keep track of last used number
4. ✅ Reserve ranges for different campuses/departments (optional)
5. ✅ Document ID assignment process

### For Developers:
1. ✅ Always validate IDs before use
2. ✅ Format IDs consistently (uppercase, trim spaces)
3. ✅ Store IDs as strings, not numbers (preserve leading zeros)
4. ✅ Use IdValidator for all ID operations
5. ✅ Log ID generation for audit trail

### For Users:
1. ✅ Remember your ID (write it down safely)
2. ✅ Enter ID exactly as shown (with dashes)
3. ✅ Use uppercase for teacher IDs
4. ✅ Contact admin if ID is forgotten

---

## Customization Options

### Change Student ID Format
Edit `IdValidator.kt`:
```kotlin
    // Current: YYYY-NNNN
    private val STUDENT_ID_PATTERN = Regex("^\d{4}-\d{4}$")

    // Example: S-YYYY-NNNN (add S prefix)
    private val STUDENT_ID_PATTERN = Regex("^S-\d{4}-\d{4}$")
```

### Change Teacher ID Format
Edit `IdValidator.kt`:
```kotlin
// Current: T-YYYY-NNN
private val TEACHER_ID_PATTERN = Regex("^T-\d{4}-\d{3}$")

// Example: FAC-NNNNN (faculty format)
private val TEACHER_ID_PATTERN = Regex("^FAC-\d{5}$")
```

### Change Email Domain
Edit `UserRepository.kt`:
```kotlin
// Current
private const val SCHOOL_DOMAIN = "smartacademic.edu"

// Change to your institution
private const val SCHOOL_DOMAIN = "youruniversity.edu"
```

---

## Common Issues & Solutions

### Issue: "Invalid ID format"
**Solution**: Check ID matches exact format (including dashes)

### Issue: "ID already exists"
**Solution**: Use next sequential number

### Issue: "ID not found in pre-registration"
**Solution**: Admin must add ID to pre-registration first

### Issue: Leading zeros removed
**Solution**: Store IDs as strings, not numbers

### Issue: Case sensitivity problems
**Solution**: Always convert to uppercase for teacher IDs

---

## Testing IDs

### Valid Test IDs

    **Students**:
    - `2024-0001` (first student of 2024)
    - `2024-1234` (student #1234)
    - `2022-2563` (student #2563)
    - `2023-9999` (last student of 2023)

**Teachers**:
- `T-2024-001` (first teacher of 2024)
- `T-2023-100` (teacher #100 of 2023)
- `EMP-12345` (employee #12345)

**Admins**:
- `A-2024-001` (first admin of 2024)
- `A-2023-005` (admin #5 of 2023)
- `A-2025-999` (last possible admin number)

### Invalid Test IDs

    **Students**:
    - `24-1234` (year too short)
    - `2024-123` (number too short)
    - `2024/1234` (wrong separator)
    - `S2024-1234` (unexpected prefix)
    - `2024-12345` (number too long - 5 digits instead of 4)

**Teachers**:
- `T-24-001` (year too short)
- `T-2024-1` (number too short)
- `TEACHER-001` (wrong format)
- `T2024-001` (missing dash)

**Admins**:
- `A-24-001` (year too short)
- `A-2024-1` (number too short)
- `ADMIN-001` (wrong format)
- `A2024-001` (missing dash)
- `2024-001` (missing A prefix)

---

## CSV Import Format

### Students CSV
```csv
    studentId,firstName,lastName,courseId,yearLevelId,enrollmentYear
    2024-0001,Juan,Dela Cruz,course_001,year_001,2024-2025
    2024-0002,Maria,Santos,course_001,year_001,2024-2025
    2024-0003,Pedro,Reyes,course_002,year_001,2024-2025
```

### Teachers CSV
```csv
teacherId,firstName,lastName,departmentCourseId,position,employmentType
T-2024-001,Maria,Santos,course_001,Professor,FULL_TIME
T-2024-002,Juan,Garcia,course_002,Associate Professor,FULL_TIME
T-2024-003,Ana,Reyes,course_001,Instructor,PART_TIME
```

### Admins CSV
```csv
adminId,firstName,lastName,email,role
A-2024-001,Carlos,Admin,admin001@sjp2cd.edu.ph,ADMIN
A-2024-002,Maria,Superadmin,admin002@sjp2cd.edu.ph,ADMIN
A-2024-003,Juan,Administrator,admin003@sjp2cd.edu.ph,ADMIN
```

**Note**: Admin CSV imports should be restricted to super-admin level access only for security purposes.

---

**Last Updated**: November 12, 2025  
**Version**: 1.1 (Added Administrator ID support)


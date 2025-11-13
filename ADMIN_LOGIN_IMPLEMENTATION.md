# Admin Login Implementation Guide

## Overview

The sign-in screen has been updated to support **three user types**: Student, Teacher, and **Administrator**. This allows administrators to access the system using their dedicated Admin ID credentials.

---

## What's New

### 1. **Updated SignInScreen.kt**
   - Added **Admin** as a third user type option
   - Dynamic UI that adapts based on selected user type
   - Admin-specific styling (purple theme for admin selection)
   - Admin ID validation using the format `A-YYYY-NNN`
   - Conditional display of "First Time Activation" and "Sign Up" buttons (hidden for Admin users)

### 2. **Updated IdValidator.kt**
   - Added `validateAdminId()` function for Admin ID format validation
   - Admin ID format: `A-YYYY-NNN` (e.g., `A-2024-001`)
   - Added supporting utility functions:
     - `extractYearFromAdminId()` - Extracts year from Admin ID
     - `formatAdminId()` - Formats Admin ID to uppercase
     - `isAdminIdFormat()` - Checks if ID matches Admin pattern
     - `generateNextAdminId()` - Generates next sequential Admin ID

---

## Admin ID Format

### Format Specification
```
A-YYYY-NNN
```

- **A** - Admin prefix (uppercase)
- **YYYY** - 4-digit year (2000 to current year + 1)
- **NNN** - 3-digit sequential number (001-999)

### Valid Examples
- `A-2024-001`
- `A-2023-015`
- `A-2025-100`

### Invalid Examples
- `2024-001` - Missing "A-" prefix
- `A-24-001` - Year must be 4 digits
- `A-2024-1` - Sequential number must be 3 digits
- `a-2024-001` - Must be uppercase (auto-formatted by validator)

---

## How to Use Admin Login

### Step 1: Access the Sign-In Screen
1. Launch the Smart Academic Tracker application
2. Navigate to the Sign-In screen

### Step 2: Select "Administrator"
1. Look for the "I am a:" section
2. You'll see three options:
   - **Student** (Blue)
   - **Teacher** (Blue)
   - **Administrator** (Purple, full-width chip)
3. Click on **Administrator**

### Step 3: Enter Admin Credentials
1. **Admin ID Field:**
   - Label changes to "Admin ID"
   - Placeholder shows: `e.g., A-2024-001`
   - Icon changes to Admin Panel Settings icon (🛡️)
   - Border color changes to purple when focused
   
2. **Password Field:**
   - Enter your admin password
   - Icon color changes to purple for admin

### Step 4: Sign In
1. Click the purple **"Sign In"** button
2. System validates your Admin ID format
3. On success, you'll be redirected to the Admin Dashboard

---

## Visual Design Changes

### Color Scheme
- **Admin Theme Color:** Purple (`#9C27B0`)
- Applied to:
  - Administrator filter chip when selected
  - Admin ID text field focus border
  - Password field icon
  - Sign In button
  - Forgot Password link

### Layout
- **Student and Teacher:** Displayed side-by-side in one row
- **Administrator:** Full-width chip in second row for emphasis
- **Special Admin Features:**
  - "First Time? Activate Account" button is **hidden** for Admin
  - "Need manual registration?" section is **hidden** for Admin
  - Shows "Administrator access only" message instead

---

## Technical Implementation Details

### File Changes

#### 1. SignInScreen.kt (`app/src/main/java/com/smartacademictracker/presentation/auth/SignInScreen.kt`)

**Key Updates:**
```kotlin
// Updated user type selection layout
Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    // First row: Student and Teacher
    Row(...) {
        FilterChip(selected = selectedUserType == UserRole.STUDENT, ...)
        FilterChip(selected = selectedUserType == UserRole.TEACHER, ...)
    }
    
    // Second row: Admin (full width)
    FilterChip(
        selected = selectedUserType == UserRole.ADMIN,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF9C27B0), // Purple
            selectedLabelColor = Color.White
        ),
        ...
    )
}
```

**ID Validation:**
```kotlin
val idValidation = remember(userId, selectedUserType) {
    when (selectedUserType) {
        UserRole.STUDENT -> IdValidator.validateStudentId(userId)
        UserRole.TEACHER -> IdValidator.validateTeacherId(userId)
        UserRole.ADMIN -> IdValidator.validateAdminId(userId)
        else -> ValidationResult(false, "Invalid user type")
    }
}
```

**Conditional UI:**
```kotlin
// Show activation/signup only for non-admin users
if (selectedUserType != UserRole.ADMIN) {
    // First-time activation button
    OutlinedButton(onClick = onNavigateToActivation, ...)
    
    // Manual registration link
    Row(...) {
        Text("Need manual registration? ")
        TextButton(onClick = onNavigateToSignUp, ...)
    }
} else {
    // Admin-specific message
    Text("Administrator access only")
}
```

#### 2. IdValidator.kt (`app/src/main/java/com/smartacademictracker/util/IdValidator.kt`)

**New Admin ID Pattern:**
```kotlin
private val ADMIN_ID_PATTERN = Regex("^A-\\d{4}-\\d{3}$")
```

**Validation Function:**
```kotlin
fun validateAdminId(adminId: String): ValidationResult {
    if (adminId.isBlank()) {
        return ValidationResult(false, "Admin ID cannot be empty")
    }
    
    val trimmed = adminId.trim().uppercase()
    
    if (!ADMIN_ID_PATTERN.matches(trimmed)) {
        return ValidationResult(
            false,
            "Invalid Admin ID format. Expected format: A-YYYY-NNN (e.g., A-2024-001)"
        )
    }
    
    // Validate year
    val year = extractYearFromAdminId(trimmed)
    if (year != null) {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        if (year.toInt() < 2000 || year.toInt() > currentYear + 1) {
            return ValidationResult(
                false,
                "Invalid year in Admin ID. Year must be between 2000 and ${currentYear + 1}"
            )
        }
    }
    
    return ValidationResult(true, null)
}
```

---

## Setting Up Admin Accounts

### Prerequisites
Admin accounts must be created manually by a system administrator or through a secure backend process. Unlike students and teachers, admins do **not** have a pre-registration or account activation flow through the mobile app.

### Creating an Admin Account

#### Option 1: Firebase Console (Manual)
1. **Navigate to Firebase Console** → Authentication
2. **Create new user:**
   - Email: `A-2024-001@sjp2cd.edu.ph` (use Admin ID format)
   - Password: Set a strong password
3. **Navigate to Firestore** → `users` collection
4. **Create user document:**
   ```json
   {
     "id": "[Firebase Auth UID]",
     "email": "A-2024-001@sjp2cd.edu.ph",
     "adminId": "A-2024-001",
     "firstName": "Admin",
     "lastName": "User",
     "role": "ADMIN",
     "isActive": true,
     "createdAt": [timestamp],
     "lastLoginAt": null
   }
   ```

#### Option 2: Backend Script (Recommended for Production)
Create a secure server-side function that:
1. Validates admin creation request (requires super-admin privileges)
2. Generates Admin ID using `IdValidator.generateNextAdminId()`
3. Creates Firebase Auth user with email format: `{adminId}@sjp2cd.edu.ph`
4. Creates corresponding Firestore user document with `role: "ADMIN"`
5. Sends secure password setup link to admin's institutional email

---

## Security Considerations

### 1. **Admin Access Control**
- Admins can **only** be created through secure backend processes
- No self-registration or public activation for admin accounts
- Admin IDs follow a predictable pattern but are protected by Firebase Authentication

### 2. **Authentication Flow**
```
1. User enters Admin ID (e.g., A-2024-001)
2. System converts to email: A-2024-001@sjp2cd.edu.ph
3. Firebase Auth validates credentials
4. System checks user role in Firestore
5. If role === "ADMIN", grant admin dashboard access
```

### 3. **Rate Limiting**
- Admin login attempts are tracked via `LoginAttemptTracker`
- After 5 failed attempts, account is locked for 30 minutes
- Prevents brute-force attacks on admin accounts

### 4. **Firestore Security Rules**
Ensure your Firestore rules restrict admin-only operations:

```javascript
match /users/{userId} {
  // Only admins can read all user documents
  allow read: if request.auth != null && 
              get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
  
  // Users can read their own document
  allow read: if request.auth != null && request.auth.uid == userId;
  
  // Only admins can update user roles
  allow update: if request.auth != null &&
                get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
}

match /pre_registered_students/{docId} {
  allow read, write: if request.auth != null &&
                      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
}

match /pre_registered_teachers/{docId} {
  allow read, write: if request.auth != null &&
                      get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
}
```

---

## Testing the Admin Login

### Test Admin Account Setup

1. **Create Test Admin in Firebase:**
   - Email: `A-2024-001@sjp2cd.edu.ph`
   - Password: `TestAdmin123!@#` (meets strong password requirements)
   - Firestore user doc with `role: "ADMIN"`

2. **Test Login Flow:**
   ```
   Step 1: Launch app → Navigate to Sign-In
   Step 2: Select "Administrator" user type
   Step 3: Enter "A-2024-001" in Admin ID field
   Step 4: Enter "TestAdmin123!@#" in password field
   Step 5: Click "Sign In"
   Step 6: Verify redirect to Admin Dashboard
   ```

3. **Test Validation:**
   - Invalid ID formats (e.g., `2024-001`, `A-24-1`)
   - Incorrect password
   - Account lockout after 5 failed attempts

### Expected Behaviors

#### ✅ Success Case
- Admin ID validates correctly
- Purple "Sign In" button is enabled
- Successful authentication
- Redirect to Admin Dashboard
- Success message: "Sign in successful! Redirecting..."

#### ❌ Error Cases
- **Invalid ID Format:**
  - Red error message below ID field
  - "Invalid Admin ID format. Expected format: A-YYYY-NNN (e.g., A-2024-001)"
  
- **Wrong Password:**
  - Error message: "Invalid credentials. Please check your ID and password."
  - Remaining attempts shown (if rate limiting is active)
  
- **Account Locked:**
  - Error message: "Account locked due to too many failed attempts. Try again in 30m 0s."

---

## FAQ

### Q: Can admins activate their accounts like students and teachers?
**A:** No. Admin accounts must be created through secure backend processes or Firebase Console. There is no self-service activation for admins.

### Q: What if an admin forgets their password?
**A:** Admins can use the "Forgot Password?" link on the sign-in screen, which triggers Firebase's password reset email flow.

### Q: Can I change the Admin ID format?
**A:** Yes. Modify the `ADMIN_ID_PATTERN` regex in `IdValidator.kt` and update the validation messages accordingly.

### Q: Why is the admin chip purple?
**A:** Purple is used to visually distinguish administrative functions from regular user types (Student/Teacher are blue). This helps prevent accidental admin login attempts.

### Q: How do I create multiple admins?
**A:** Use `IdValidator.generateNextAdminId(year, lastNumber)` to generate sequential IDs:
```kotlin
val admin1 = IdValidator.generateNextAdminId(2024, 0)  // A-2024-001
val admin2 = IdValidator.generateNextAdminId(2024, 1)  // A-2024-002
val admin3 = IdValidator.generateNextAdminId(2024, 2)  // A-2024-003
```

### Q: What's the difference between Admin IDs and Teacher IDs?
**A:** 
- **Teacher ID:** `T-YYYY-NNN` (T prefix)
- **Admin ID:** `A-YYYY-NNN` (A prefix)
- Both use the same sequential number format but represent different roles and access levels.

---

## Next Steps

### 1. **Create Your First Admin Account**
   - Use Firebase Console to manually create an admin
   - Test login with the new Admin ID

### 2. **Update Firestore Security Rules**
   - Deploy the admin-specific security rules
   - Test that only admins can access restricted collections

### 3. **Implement Admin-Only Features**
   - Ensure Admin Dashboard has proper access controls
   - Implement admin-only screens (user management, reports, etc.)

### 4. **Production Considerations**
   - Create a secure backend endpoint for admin creation
   - Implement audit logging for admin actions
   - Set up admin role change notifications
   - Create admin password policy enforcement

---

## Support and Troubleshooting

### Common Issues

**Issue:** "Admin ID field shows Student/Teacher placeholder"
- **Solution:** Ensure you've selected "Administrator" from the user type options

**Issue:** "Sign In button is disabled"
- **Solution:** Check that:
  1. Admin ID follows `A-YYYY-NNN` format
  2. Password is not empty
  3. No validation errors are shown

**Issue:** "Can't find admin user after sign-in"
- **Solution:** Verify that the Firestore user document has `role: "ADMIN"` (exact case match)

**Issue:** "Admin gets redirected to student dashboard"
- **Solution:** Check the role check in `SignInScreen.kt` navigation logic:
  ```kotlin
  val destination = when (user.role) {
      "ADMIN" -> "admin_dashboard"  // Ensure this matches your route
      "TEACHER" -> "teacher_dashboard"
      else -> "student_dashboard"
  }
  ```

---

## Changelog

### Version 1.1.0 (Current)
- ✅ Added Administrator user type to SignInScreen
- ✅ Implemented Admin ID validation (`A-YYYY-NNN` format)
- ✅ Created admin-specific UI theme (purple)
- ✅ Added utility functions for Admin ID management
- ✅ Updated documentation with Admin login guide

### Version 1.0.0 (Previous)
- Student and Teacher ID-based authentication
- Pre-registration system for students and teachers
- Account activation flow

---

## References

- **SignInScreen.kt:** `app/src/main/java/com/smartacademictracker/presentation/auth/SignInScreen.kt`
- **IdValidator.kt:** `app/src/main/java/com/smartacademictracker/util/IdValidator.kt`
- **ID Format Reference:** `ID_FORMAT_REFERENCE.md`
- **Implementation Summary:** `IMPLEMENTATION_SUMMARY.md`
- **Testing Guide:** `TESTING_GUIDE_ID_AUTHENTICATION.md`

---

**Last Updated:** November 12, 2025  
**Prepared By:** Smart Academic Tracker Development Team


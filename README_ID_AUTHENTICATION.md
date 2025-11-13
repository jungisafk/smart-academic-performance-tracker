# 🆔 Student/Teacher ID-Based Authentication

## Quick Start Guide

### For Students & Teachers:

#### **First Time? Activate Your Account**
1. Open the app
2. Click **"First Time? Activate Account"**
3. Select your role (Student or Teacher)
4. Enter your institutional ID
5. Create a strong password
6. Click **"Activate Account"**

#### **Already Activated? Sign In**
1. Open the app
2. Select your role (Student or Teacher)
3. Enter your ID and password
4. Click **"Sign In"**

---

### For Administrators:

#### **Pre-Register Students**
1. Go to Admin Dashboard
2. Navigate to **"Pre-Registered Students"**
3. Click the **+** button
4. Fill in student information
5. Click **"Add Student"**

#### **Pre-Register Teachers**
1. Go to Admin Dashboard
2. Navigate to **"Pre-Registered Teachers"**
3. Click the **+** button
4. Fill in teacher information
5. Click **"Add Teacher"**

---

## ID Formats

### Student ID Format
```
Format: YYYY-NNNNN
Example: 2024-12345

Where:
  YYYY = Year (2024, 2025, etc.)
  NNNNN = 5-digit number (00001-99999)
```

### Teacher ID Format
```
Format: T-YYYY-NNN
Example: T-2024-001

Where:
  T = Teacher prefix
  YYYY = Year
  NNN = 3-digit number (001-999)
```

---

## Password Requirements

Your password must have:
- ✅ At least 8 characters
- ✅ One uppercase letter (A-Z)
- ✅ One lowercase letter (a-z)
- ✅ One number (0-9)
- ✅ One special character (!@#$%^&*)

---

## Features

### ✅ Secure Authentication
- Institutional ID-based login
- Strong password requirements
- Rate limiting (5 attempts)
- Account lockout protection
- Audit trail

### ✅ Easy Account Activation
- Pre-registration by admin
- One-time activation process
- Auto-populated profile data
- Password strength indicator

### ✅ Admin Management
- Pre-register students/teachers
- Search and filter users
- Track activation status
- Manage user data

---

## Troubleshooting

### "Invalid ID format"
- Check your ID matches the format (YYYY-NNNNN for students, T-YYYY-NNN for teachers)
- Ensure dashes are in the correct position
- Use uppercase letters for teacher IDs

### "ID not found"
- Your ID must be pre-registered by an administrator
- Contact your school administrator

### "Account already activated"
- Your account is already active
- Use the sign-in screen instead

### "Account locked"
- You've exceeded maximum login attempts (5)
- Wait 30 minutes before trying again
- Or contact administrator to unlock

---

## Need Help?

### Documentation:
- **Full Guide**: `ID_BASED_AUTHENTICATION_IMPLEMENTATION_GUIDE.md`
- **ID Formats**: `ID_FORMAT_REFERENCE.md`
- **Testing**: `TESTING_GUIDE_ID_AUTHENTICATION.md`

### Contact:
- System Administrator
- IT Help Desk
- School Registrar

---

## Technical Details

**School Domain**: `sjp2cd.edu.ph`  
**Authentication**: Firebase Authentication  
**Database**: Cloud Firestore  
**Security**: Role-based access control  

---

**Version**: 1.0.0  
**Last Updated**: November 12, 2025  
**Status**: Production Ready ✅


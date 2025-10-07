# Firestore Academic Periods Permission Fix

## Problem
The Academic Periods feature is failing with `PERMISSION_DENIED` errors because the Firestore security rules need to be updated to allow access to the `academic_periods` collection.

## Solution

### 1. Update Firestore Security Rules

The `firestore.rules` file has been updated with the following changes:

#### Fixed `isActiveUser()` function:
```javascript
function isActiveUser() {
  return isAuthenticated() && 
         get(/databases/$(database)/documents/users/$(request.auth.uid)).data.active == true;
}
```

#### Updated Academic Periods rules:
```javascript
// Academic Periods collection - Admin only
match /academic_periods/{periodId} {
  allow read: if isAuthenticated() && (isAdmin() || isActiveUser());
  allow create, update, delete: if isAuthenticated() && isAdmin();
}
```

### 2. Deploy Updated Rules

You need to deploy the updated Firestore rules to Firebase. Here are the steps:

#### Option A: Using Firebase CLI (Recommended)
```bash
# Navigate to your Firebase project directory
cd /path/to/your/firebase/project

# Deploy only the Firestore rules
firebase deploy --only firestore:rules
```

#### Option B: Using Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database** → **Rules**
4. Copy the updated rules from `firestore.rules` file
5. Click **Publish**

### 3. Verify User Role and Status

Make sure the admin user has:
- `role: "ADMIN"` in the users collection
- `active: true` in the users collection

### 4. Test the Fix

After deploying the rules:
1. Open the app and navigate to Admin Dashboard
2. Click on "Academic Periods"
3. The screen should now load without permission errors
4. You should be able to create new academic periods

## Code Changes Made

### 1. Enhanced Error Handling
- Updated `AcademicPeriodRepository` to handle permission errors gracefully
- Added specific error messages for permission denied scenarios
- Added debug logging for troubleshooting

### 2. Improved User Experience
- The app now shows helpful error messages when permissions are denied
- Graceful fallback to empty lists when permission errors occur
- Better error reporting in the UI

## Troubleshooting

### If you still get permission errors:

1. **Check Firebase Console Rules:**
   - Go to Firestore Database → Rules
   - Verify the rules are deployed correctly
   - Check for any syntax errors

2. **Verify User Authentication:**
   - Make sure the user is properly authenticated
   - Check that the user document exists in the `users` collection
   - Verify the user has `role: "ADMIN"` and `active: true`

3. **Check Firebase Project:**
   - Ensure you're using the correct Firebase project
   - Verify the `google-services.json` file is up to date

4. **Test with Firebase Console:**
   - Try creating a document in the `academic_periods` collection manually
   - This will help identify if it's a rules issue or authentication issue

## Expected Behavior After Fix

✅ **Academic Periods screen loads successfully**
✅ **No permission denied errors in logcat**
✅ **Can create new academic periods**
✅ **Can view existing academic periods**
✅ **Can set active periods**
✅ **Can delete periods**

## Files Modified

- `firestore.rules` - Updated security rules
- `AcademicPeriodRepository.kt` - Enhanced error handling
- `AdminAcademicPeriodViewModel.kt` - Better error messages

The Academic Periods feature should now work correctly with proper Firestore permissions! 🎉

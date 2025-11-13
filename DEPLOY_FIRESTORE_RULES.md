# 🚀 Deploy Firestore Rules - Account Activation Fix

## ⚠️ Important

The updated Firestore rules are in your local `firestore.rules` file, but **they need to be deployed to Firebase** for them to take effect.

---

## 📋 Deployment Steps

### Option 1: Using Firebase CLI (Recommended)

1. **Open Terminal/Command Prompt:**
   ```bash
   cd D:\SmartAcademicPerformanceTracker
   ```

2. **Deploy Firestore Rules:**
   ```bash
   firebase deploy --only firestore:rules
   ```

3. **Verify Deployment:**
   - You should see: `✔  Deploy complete!`
   - Check Firebase Console → Firestore → Rules to confirm

### Option 2: Using Firebase Console (Manual)

1. **Go to Firebase Console:**
   - https://console.firebase.google.com/project/performancetracker-da4c1/firestore/rules

2. **Copy Rules:**
   - Open `firestore.rules` file in your project
   - Copy all the content

3. **Paste in Console:**
   - Paste the rules into the Firebase Console editor
   - Click **"Publish"** button

4. **Verify:**
   - Rules should show as "Published"
   - Wait 1-2 minutes for rules to propagate

---

## ✅ Updated Rules

The rules now allow:

```javascript
// Pre-registered Students collection
match /pre_registered_students/{docId} {
  // Allow unauthenticated users to read unregistered student records
  allow read: if resource.data.isRegistered == false || (isAuthenticated() && isAdmin());
  // Only admins can write
  allow write: if isAuthenticated() && isAdmin();
}

// Pre-registered Teachers collection
match /pre_registered_teachers/{docId} {
  // Allow unauthenticated users to read unregistered teacher records
  allow read: if resource.data.isRegistered == false || (isAuthenticated() && isAdmin());
  // Only admins can write
  allow write: if isAuthenticated() && isAdmin();
}
```

---

## 🧪 Testing After Deployment

1. **Wait 1-2 minutes** for rules to propagate

2. **Test Account Activation:**
   - Open the app
   - Go to Account Activation screen
   - Enter Student ID: `2022-2563`
   - Should no longer get `PERMISSION_DENIED` error

3. **Verify in Logcat:**
   - Should see successful query
   - No more permission denied errors

---

## 🔍 Troubleshooting

### Still Getting Permission Denied?

1. **Check Rules Deployment:**
   - Go to Firebase Console → Firestore → Rules
   - Verify the rules show `resource.data.isRegistered == false`

2. **Check Document Data:**
   - Go to Firebase Console → Firestore → Data
   - Check `pre_registered_students` collection
   - Verify the document has `isRegistered: false`

3. **Check Field Name:**
   - Make sure the field is exactly `isRegistered` (case-sensitive)
   - Not `is_registered` or `IsRegistered`

4. **Wait for Propagation:**
   - Rules can take 1-2 minutes to propagate
   - Try again after waiting

---

## 📝 Quick Command Reference

```bash
# Deploy only Firestore rules
firebase deploy --only firestore:rules

# Deploy everything
firebase deploy

# Check Firebase login
firebase login

# List projects
firebase projects:list
```

---

**Status:** ⚠️ Rules need to be deployed to Firebase!


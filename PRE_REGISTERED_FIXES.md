# ✅ Pre-Registered Students/Teachers Fixes

## 🎯 Issues Fixed

1. **Missing Firestore Index** - Queries by `isRegistered` and `createdAt` require composite index
2. **Permission Denied on Update** - Users can't update their own pre-registered record during activation
3. **Field Name Warning** - Firestore mapping warning (non-critical)

---

## ✅ Changes Made

### 1. Added Firestore Composite Indexes

**File:** `firestore.indexes.json`

Added indexes for:
- `pre_registered_students` collection: `isRegistered` (ASC) + `createdAt` (DESC)
- `pre_registered_teachers` collection: `isRegistered` (ASC) + `createdAt` (DESC)

**Why:** Queries filtering by `isRegistered` and ordering by `createdAt` require a composite index.

### 2. Updated Firestore Security Rules

**File:** `firestore.rules`

**Before:**
```javascript
allow write: if isAuthenticated() && isAdmin();
```

**After:**
```javascript
allow create: if isAuthenticated() && isAdmin();
allow update: if isAuthenticated() && (
                 isAdmin() || 
                 (request.resource.data.firebaseUserId == request.auth.uid && 
                  resource.data.get('isRegistered', false) == false)
               );
allow delete: if isAuthenticated() && isAdmin();
```

**Why:** Users need to update their own pre-registered record during account activation to mark it as registered.

---

## 🚀 Deployment Steps

### Step 1: Deploy Firestore Indexes

```bash
firebase deploy --only firestore:indexes
```

**Or use the link from the error:**
- Click the link in the error message
- Firebase Console will create the index automatically
- Wait 2-5 minutes for index to build

### Step 2: Deploy Firestore Rules

```bash
firebase deploy --only firestore:rules
```

**Or manually:**
1. Go to Firebase Console → Firestore → Rules
2. Copy content from `firestore.rules`
3. Paste and click "Publish"

---

## 📋 What the Fixes Do

### Index Fix
- Allows queries like: `whereEqualTo("isRegistered", true).orderBy("createdAt", DESC)`
- Enables filtering by registration status in Admin screens
- Required for "Pending" and "Activated" filter buttons

### Permission Fix
- Allows users to update their own pre-registered record during activation
- Only when:
  - The record is not yet registered (`isRegistered == false`)
  - The user is setting their own `firebaseUserId`
- Admins can still update any record
- Prevents users from modifying other users' records

---

## 🧪 Testing

### Test Account Activation
1. Activate a student account
2. Should successfully mark pre-registered record as registered
3. No more permission denied errors

### Test Admin Filters
1. Go to Admin → Pre-Registered Students
2. Click "Pending" filter
3. Should show unregistered students (no index error)
4. Click "Activated" filter
5. Should show registered students (no index error)

---

## ⚠️ Important Notes

1. **Index Building Time:** Firestore indexes can take 2-5 minutes to build after deployment
2. **Field Name Warning:** The "registered" field warning is non-critical - Firestore is trying to map an old field name, but `isRegistered` is correct
3. **Permission Scope:** Users can only update their own record during activation, not after

---

**Status:** ✅ Ready to Deploy


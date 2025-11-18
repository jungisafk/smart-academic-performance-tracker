# Deploy Firestore Indexes

This guide explains how to automatically deploy Firestore composite indexes using Firebase CLI.

## Prerequisites

1. **Install Firebase CLI** (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. **Login to Firebase**:
   ```bash
   firebase login
   ```

3. **Initialize Firebase** (if not already done):
   ```bash
   firebase init firestore
   ```
   - Select your Firebase project
   - Choose to use existing `firestore.rules` and `firestore.indexes.json` files

## Deploying Indexes

### Option 1: Using PowerShell (Windows)
```powershell
.\deploy-indexes.ps1
```

### Option 2: Using Bash (Linux/Mac/Git Bash)
```bash
chmod +x deploy-indexes.sh
./deploy-indexes.sh
```

### Option 3: Using Firebase CLI directly
```bash
firebase deploy --only firestore:indexes
```

## What Gets Deployed

The `firestore.indexes.json` file contains composite indexes for:

1. **notifications** collection:
   - `userId` (ASC) + `createdAt` (DESC) + `__name__` (DESC)
   - `userId` (ASC) + `isRead` (ASC) + `createdAt` (DESC)

2. **student_enrollments** collection:
   - `studentId` (ASC) + `status` (ASC) + `__name__` (ASC)
   - `teacherId` (ASC) + `status` (ASC)
   - `subjectId` (ASC) + `sectionName` (ASC) + `status` (ASC)

3. **student_applications** collection:
   - `subjectId` (ASC) + `appliedAt` (DESC)
   - `reviewedBy` (ASC) + `appliedAt` (DESC)
   - `reviewedBy` (ASC) + `status` (ASC) + `appliedAt` (DESC)

4. **grades** collection:
   - `unlockedAt` (DESC)

5. **pre_registered_students** collection:
   - `courseId` (ASC) + `studentId` (ASC)

6. And more...

## After Deployment

1. **Check Index Status**: Go to Firebase Console → Firestore Database → Indexes tab
2. **Wait for Build**: Index creation can take a few minutes to several hours depending on data size
3. **Monitor Progress**: The Firebase Console will show the build status

## Troubleshooting

### Error: "Permission denied"
- Make sure you're logged in: `firebase login`
- Verify you have the correct project selected: `firebase use <project-id>`

### Error: "Index already exists"
- This is normal - Firebase will skip existing indexes

### Indexes not building
- Check Firebase Console for error messages
- Ensure your Firestore security rules allow the queries
- Verify the field names match exactly (case-sensitive)

## Notes

- Index creation is asynchronous and may take time
- Large collections may take hours to build indexes
- You can continue using your app while indexes are building (queries will be slower until indexes are ready)
- Firebase will automatically use indexes once they're built


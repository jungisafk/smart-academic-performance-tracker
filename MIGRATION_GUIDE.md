# Firestore Migration Guide - Department & Subject Type

This guide explains how to automatically update your Firebase Firestore database with the new department and subject type fields.

## 🚀 Quick Start

### Prerequisites
1. **Node.js** installed (v14 or higher)
2. **Firebase CLI** installed: `npm install -g firebase-tools`
3. **Firebase project** set up

### Step 1: Install Dependencies
```bash
npm install
```

### Step 2: Login to Firebase
```bash
firebase login
```

### Step 3: Set Your Firebase Project
```bash
firebase use <your-project-id>
```

To find your project ID, check `app/google-services.json`:
```json
{
  "project_info": {
    "project_id": "your-project-id-here"
  }
}
```

### Step 4: Run Migration Script
```bash
npm run migrate-firestore
```

Or directly:
```bash
node migrate_firestore_data.js
```

---

## 📋 What the Script Does

### 1. **Updates Subjects** (Automatic)
- Sets `subjectType = "MAJOR"` for all subjects that don't have `subjectType` set
- This is safe because MAJOR is the default behavior
- You can manually change MINOR subjects later via Admin UI

### 2. **Checks Teachers** (Reporting)
- Lists all teachers
- Shows which teachers have `departmentCourseId` set
- Shows which teachers need `departmentCourseId` assignment

### 3. **Interactive Department Assignment** (Optional)
- If teachers need departments, the script will ask if you want to assign them
- You can select from available courses/departments
- Or skip and assign later via Admin UI

---

## 🔧 Alternative: Manual Migration via Firebase Console

If you prefer to do it manually or the script doesn't work:

### For Subjects:
1. Go to Firebase Console → Firestore Database
2. Navigate to `subjects` collection
3. For each subject:
   - Click on document
   - Add field: `subjectType` (type: string)
   - Set value: `"MAJOR"` (or `"MINOR"` for general education subjects)
   - Save

### For Teachers:
1. Go to Firebase Console → Firestore Database
2. Navigate to `users` collection
3. Filter by `role = "TEACHER"`
4. For each teacher:
   - Click on document
   - Add field: `departmentCourseId` (type: string)
   - Set value to the course ID of their department
   - To find course IDs: Go to `courses` collection and copy the document ID
   - Save

---

## 📝 Using a Mapping File (Advanced)

You can create a `teacher_department_mapping.json` file to automate teacher department assignment:

```json
{
  "teacher_email_to_course_id": {
    "teacher1@example.com": "bsit_course_document_id",
    "teacher2@example.com": "nursing_course_document_id",
    "teacher3@example.com": "bsit_course_document_id"
  }
}
```

Then modify the script to read this file and automatically assign departments.

---

## ✅ Verification

After running the migration:

### Verify Subjects:
```bash
# In Firebase Console, check subjects collection
# All subjects should have subjectType field
```

### Verify Teachers:
```bash
# In Firebase Console, check users collection
# Filter by role = "TEACHER"
# All teachers should have departmentCourseId field (if assigned)
```

### Test in App:
1. Login as a teacher with `departmentCourseId` set
2. Go to "My Subjects" → "Available" tab
3. Should only see:
   - MAJOR subjects from their department
   - All MINOR subjects

---

## 🐛 Troubleshooting

### Error: "Failed to initialize Firebase Admin SDK"
**Solution:**
1. Make sure you've run `firebase login`
2. Make sure you've run `firebase use <project-id>`
3. Or provide `serviceAccountKey.json` file in project root

### Error: "Permission denied"
**Solution:**
1. Check Firestore security rules
2. Make sure you're logged in with an account that has admin access
3. For service account, ensure it has Firestore Admin permissions

### Teachers still see all subjects
**Solution:**
1. Verify `departmentCourseId` is set in Firestore
2. Verify `subjectType` is set for subjects
3. Clear app cache and restart
4. Check that filtering logic is working in ViewModel

---

## 📊 Migration Results

The script will output:
- Number of subjects updated
- Number of subjects skipped (already had subjectType)
- Total teachers found
- Teachers with departments
- Teachers needing departments

---

## 🔄 Re-running the Script

The script is **safe to run multiple times**:
- It only updates subjects that don't have `subjectType`
- It doesn't overwrite existing values
- It only reports on teachers (doesn't modify unless in interactive mode)

---

## 💡 Next Steps After Migration

1. **Review MINOR Subjects**: 
   - Check which subjects should be MINOR (general education)
   - Update them manually in Firebase Console or via Admin UI

2. **Assign Teacher Departments**:
   - Use Admin UI → Manage Users → Edit Teacher
   - Or use the interactive mode in the script

3. **Test the Features**:
   - Login as different teachers
   - Verify they only see appropriate subjects
   - Test applying for subjects

---

**Last Updated:** Based on implementation  
**Script Location:** `migrate_firestore_data.js`  
**Run Command:** `npm run migrate-firestore`


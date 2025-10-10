# 🚀 Quick Start: Firestore Migration

## Automatic Database Update

This guide will help you automatically update your Firestore database with the new `subjectType` and `departmentCourseId` fields.

---

## ⚡ Fastest Method (5 minutes)

### Step 1: Get Service Account Key
1. Go to: https://console.firebase.google.com/project/performancetracker-da4c1/settings/serviceaccounts/adminsdk
2. Click: **"Generate New Private Key"**
3. Save the file as: `serviceAccountKey.json`
4. Place it in: `D:\SmartAcademicPerformanceTracker\serviceAccountKey.json`

### Step 2: Run Migration
```bash
npm run migrate-firestore
```

That's it! The script will:
- ✅ Set `subjectType = "MAJOR"` for all subjects
- 📋 List teachers that need departments
- 🔧 Optionally assign departments interactively

---

## 📋 What Gets Updated

### Subjects (Automatic)
- All subjects without `subjectType` → Set to `"MAJOR"`
- You can manually change MINOR subjects later via Admin UI

### Teachers (Interactive)
- Lists all teachers
- Shows which ones need `departmentCourseId`
- Optionally assigns departments during script execution

---

## 🔍 After Migration

### Verify in Firebase Console:
1. **Subjects**: Check `subjects` collection - all should have `subjectType` field
2. **Teachers**: Check `users` collection (filter by `role = "TEACHER"`) - should have `departmentCourseId` if assigned

### Test in App:
1. Login as a teacher with `departmentCourseId` set
2. Go to "My Subjects" → "Available" tab
3. Should only see:
   - MAJOR subjects from their department
   - All MINOR subjects

---

## 🐛 Troubleshooting

**Error: "Could not load credentials"**
→ Use the service account key method (Step 1 above)

**Error: "Permission denied"**
→ Make sure service account has "Firestore Admin" role

**Need more help?**
→ See `SETUP_FIREBASE_AUTH.md` for detailed authentication options

---

## 📚 Related Files

- `migrate_firestore_data.js` - The migration script
- `MIGRATION_GUIDE.md` - Detailed migration guide
- `SETUP_FIREBASE_AUTH.md` - Authentication setup options
- `NEXT_STEPS_IMPLEMENTATION.md` - Complete implementation checklist

---

**Ready?** Get your service account key and run `npm run migrate-firestore`! 🚀


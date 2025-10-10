# Next Steps Implementation - Department & Subject Type Features

## ✅ Completed Implementation

All code changes have been completed. The following features are now implemented:

### 1. ✅ Data Model Updates
- **User Model**: Added `departmentCourseId`, `departmentCourseName`, and `departmentCourseCode` fields
- **Subject Model**: Added `subjectType` field with `SubjectType` enum (MAJOR/MINOR)

### 2. ✅ Filtering Logic
- **TeacherSubjectsViewModel**: Filters subjects based on teacher's department and subject type
- **HierarchicalTeacherSubjectApplicationViewModel**: Filters subjects based on department and type
- **Validation**: Prevents teachers from applying to MAJOR subjects outside their department

### 3. ✅ Admin UI - Subject Type Selection
- **AddSubjectScreen**: Added dropdown to select Subject Type (MAJOR/MINOR)
- **AddSubjectViewModel**: Updated to accept and pass `subjectType` parameter
- **SubjectRepository**: Updated `addSubject()` method to accept `subjectType`

### 4. ✅ Admin UI - Teacher Department Management
- **ManageUsersScreen**: 
  - Shows department information for teachers
  - Displays warning if teacher has no department assigned
  - Edit button opens department selection dialog
- **EditTeacherDepartmentDialog**: New dialog to select/change teacher's department
- **ManageUsersViewModel**: 
  - Loads courses for department selection
  - Populates `departmentCourseName` and `departmentCourseCode` computed fields
  - `updateTeacherDepartment()` method to save changes

### 5. ✅ Migration Scripts
- **migrate_teachers_department.kt**: Template script to update existing teachers
- **migrate_subjects_type.kt**: Template script to set subjectType for existing subjects

### 6. ✅ Performance Optimizations
- Removed redundant `DisposableEffect` that caused unnecessary reloads
- Made post-application reloads non-blocking (background coroutines)
- Optimized available subjects update (removes applied subjects instead of full reload)

---

## 📋 Next Steps Checklist

### Step 1: Update Existing Teachers in Firestore
**Priority: HIGH**  
**Status: ⏳ Pending - Automated Script Available**

**✅ AUTOMATED OPTION (Recommended):**
Run the automated migration script:
```bash
# 1. Make sure you're logged in to Firebase
firebase login

# 2. Set your project (check app/google-services.json for project_id)
firebase use <your-project-id>

# 3. Run the migration script
npm run migrate-firestore
```

The script will:
- Automatically set `subjectType = "MAJOR"` for all subjects
- List teachers that need `departmentCourseId` assignment
- Optionally assign departments interactively

**📖 See `MIGRATION_GUIDE.md` for detailed instructions.**

**Manual Option:**
1. Open Firebase Console → Firestore Database
2. Navigate to `users` collection
3. Filter by `role = "TEACHER"`
4. For each teacher:
   - Click on the document
   - Add field: `departmentCourseId` (type: string)
   - Set value to the course ID of their department (e.g., BSIT course ID, Nursing course ID)
   - Save

**Important Notes:**
- Teachers without `departmentCourseId` will only see MINOR subjects
- Teachers with `departmentCourseId` will see:
  - MAJOR subjects from their department
  - All MINOR subjects

---

### Step 2: Update Existing Subjects in Firestore
**Priority: HIGH**  
**Status: ✅ Automated - Run Migration Script**

**✅ AUTOMATED (Included in migration script):**
The migration script automatically sets `subjectType = "MAJOR"` for all subjects that don't have it.

**After running the script:**
- Review subjects that should be MINOR (general education)
- Update them manually in Firebase Console or via Admin UI

**Examples:**
- BSIT Programming subjects → `"MAJOR"` ✅ (set automatically)
- Nursing Anatomy subjects → `"MAJOR"` ✅ (set automatically)
- General Education (English, Math, PE) → `"MINOR"` (update manually)

**Manual Option:**
1. Open Firebase Console → Firestore Database
2. Navigate to `subjects` collection
3. For each subject:
   - Click on the document
   - Add/update field: `subjectType` (type: string)
   - Set value to `"MAJOR"` or `"MINOR"`
   - Save

**Important Notes:**
- If `subjectType` is not set, it defaults to `MAJOR` in code
- However, it's recommended to explicitly set it in Firestore for clarity

---

### Step 3: Test the New Features
**Priority: MEDIUM**  
**Status: ⏳ Pending Testing**

**Testing Checklist:**

#### Admin Features:
- [ ] Create a new subject and select Subject Type (MAJOR/MINOR)
- [ ] Verify subject is saved with correct `subjectType` in Firestore
- [ ] Edit a teacher's department in Manage Users screen
- [ ] Verify teacher's department is saved correctly
- [ ] Verify department name displays correctly in user card

#### Teacher Features:
- [ ] Login as a teacher with `departmentCourseId` set
- [ ] Navigate to "My Subjects" → "Available" tab
- [ ] Verify only MAJOR subjects from their department are visible
- [ ] Verify all MINOR subjects are visible
- [ ] Try to apply for a MAJOR subject from different department (should fail with error)
- [ ] Try to apply for a MINOR subject (should succeed)
- [ ] Verify application appears in "Applied" tab

#### Performance:
- [ ] Test navigation speed when applying for subjects
- [ ] Test navigation speed when going back from application screen
- [ ] Verify no blocking operations during navigation

---

### Step 4: Update User Registration (Optional Enhancement)
**Priority: LOW**  
**Status: ⏳ Future Enhancement**

**Current State:** Teachers can be assigned departments via Manage Users screen

**Future Enhancement:** Add department selection during teacher registration
- Update `SignUpScreen` to show department selection for teachers
- Update `SignUpViewModel` to accept `departmentCourseId` for teachers
- Update `UserRepository.createUser()` to accept `departmentCourseId`

---

## 🔍 Verification Steps

### Verify Teachers Have Departments:
```kotlin
// In Firebase Console or Admin SDK
// Query: users collection where role = "TEACHER"
// Check: All teachers should have departmentCourseId field set
```

### Verify Subjects Have Types:
```kotlin
// In Firebase Console or Admin SDK
// Query: subjects collection
// Check: All subjects should have subjectType field set ("MAJOR" or "MINOR")
```

### Verify Filtering Works:
1. Login as teacher with departmentCourseId = "bsit_course_id"
2. Check "Available Subjects" tab
3. Should only see:
   - MAJOR subjects where courseId = "bsit_course_id"
   - All MINOR subjects

---

## 📝 Important Notes

1. **Backward Compatibility:**
   - Existing subjects without `subjectType` default to `MAJOR` in code
   - Existing teachers without `departmentCourseId` will only see MINOR subjects
   - Both fields are optional in the data model for backward compatibility

2. **Data Migration:**
   - Migration scripts are provided as templates
   - You need to customize them with your actual data mappings
   - Manual migration via Firebase Console is also supported

3. **Performance:**
   - Navigation optimizations are in place
   - Data reloads happen in background
   - No blocking operations during navigation

4. **Security:**
   - Filtering is enforced at the ViewModel level
   - Validation prevents unauthorized applications
   - Firestore security rules should also be updated if needed

---

## 🐛 Troubleshooting

### Issue: Teachers see no available subjects
**Solution:** 
- Check if teacher has `departmentCourseId` set
- Check if subjects have `subjectType` set
- Check if subjects are in active academic period

### Issue: Teacher can apply to MAJOR subject from different department
**Solution:**
- Check validation logic in `applyForSubject()` methods
- Verify `departmentCourseId` is correctly set for teacher
- Verify `subjectType` is correctly set for subject

### Issue: Department name not showing in user card
**Solution:**
- Check if `departmentCourseId` is set for teacher
- Check if course exists in Firestore
- Verify `ManageUsersViewModel.loadUsers()` is populating computed fields

---

## ✅ Completion Status

- [x] Data models updated
- [x] Filtering logic implemented
- [x] Admin UI for subject type selection
- [x] Admin UI for teacher department management
- [x] Migration scripts created
- [x] Performance optimizations
- [ ] **Manual migration of existing teachers** (Pending)
- [ ] **Manual migration of existing subjects** (Pending)
- [ ] **Testing** (Pending)

---

**Last Updated:** Based on implementation completion  
**All Code Changes:** ✅ Complete  
**Manual Actions Required:** ⏳ Pending


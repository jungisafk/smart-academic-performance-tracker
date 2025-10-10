# ⚡ Quick Test Checklist

A streamlined checklist for testing all fixed features in priority order.

## 🔴 HIGH PRIORITY (Test First)

### 1. NotificationCenterViewModel - User ID Fix
- [ ] Login as Teacher/Admin
- [ ] Open Notification Center
- [ ] Verify notifications load (not empty/wrong user)
- [ ] Test "Mark All as Read" → ✅ Works
- [ ] Test "Delete All" → ✅ Works
- [ ] Check Logcat → ✅ No auth errors

**Time:** ~2 minutes

---

### 2. OfflineStatusViewModel - Conflict Resolution
- [ ] Turn OFF internet
- [ ] Login as Teacher
- [ ] Enter/modify grades offline
- [ ] Turn ON internet
- [ ] Modify same grades in Firebase Console (create conflict)
- [ ] Open Offline Status screen
- [ ] Verify conflicts appear → ✅ Conflicts listed
- [ ] Resolve conflict (USE_LOCAL) → ✅ Resolves successfully
- [ ] Verify conflict disappears → ✅ Removed from list

**Time:** ~5 minutes

---

### 3. GradeExportRepository - Export History
- [ ] Login as Teacher
- [ ] Generate Subject Grade Export → ✅ Created
- [ ] Generate Class Summary Export → ✅ Created
- [ ] Generate Individual Student Report → ✅ Created
- [ ] Check Export History (if UI exists) or Firebase Console
- [ ] Verify all 3 exports in history → ✅ All present
- [ ] Verify sorted by date (newest first) → ✅ Sorted correctly

**Time:** ~3 minutes

---

## 🟡 MEDIUM PRIORITY

### 4. GradeCompletionNotificationService - Subject Repository
- [ ] Login as Teacher
- [ ] Enter grades for ALL students in a subject (same period)
- [ ] Check Notification Center (as Admin)
- [ ] Verify notification has:
  - [ ] Real subject name (not "Subject $id") → ✅ Correct
  - [ ] Real teacher name (not empty) → ✅ Correct
  - [ ] Correct student count → ✅ Correct
- [ ] Check Teacher notification → ✅ Received

**Time:** ~3 minutes

---

### 5. Replace println() with Logging
- [ ] Open Android Studio Logcat
- [ ] Filter by: `TeacherAnalytics`, `StudentAnalytics`, `AdminStudentApps`, `TeacherSubjects`
- [ ] Navigate through app screens
- [ ] Verify logs appear with proper tags → ✅ Tagged correctly
- [ ] Check for ERROR level logs → ✅ Using Log.e()
- [ ] Verify NO println() output → ✅ None found

**Time:** ~2 minutes

---

### 6. Student Number in Exports
- [ ] Login as Teacher
- [ ] Generate Subject Grade Export
- [ ] Download/open exported file
- [ ] Check "Student Number" column
- [ ] Verify shows email (or studentId as fallback) → ✅ Not placeholder
- [ ] Generate Individual Report → ✅ Student number correct

**Time:** ~2 minutes

---

## 🟢 LOW PRIORITY

### 7. Individual Report - Subject/Teacher Fields
- [ ] Login as Teacher/Admin
- [ ] Generate Individual Student Report
- [ ] Download/open exported file
- [ ] Check report header/metadata:
  - [ ] Subject ID populated → ✅ Not empty
  - [ ] Subject Name populated → ✅ Not empty
  - [ ] Teacher ID populated → ✅ Not empty
  - [ ] Teacher Name populated → ✅ Not empty

**Time:** ~2 minutes

---

### 8. Exception Handling
- [ ] Open Logcat, filter by ERROR
- [ ] Turn OFF internet
- [ ] Try various operations (load data, save grades, etc.)
- [ ] Verify all errors logged → ✅ Errors appear
- [ ] Check error messages are user-friendly → ✅ Readable
- [ ] Turn ON internet, verify recovery → ✅ Works

**Time:** ~3 minutes

---

### 9. Attendance Tracking (Verification)
- [ ] Login as Teacher
- [ ] Open Attendance screen → ✅ Loads
- [ ] Record attendance (PRESENT/ABSENT/LATE) → ✅ Saves
- [ ] View attendance summary → ✅ Shows stats
- [ ] Test bulk recording → ✅ Works

**Time:** ~2 minutes

---

### 10. Assignment Submission Tracking (Verification)
- [ ] Login as Teacher
- [ ] Open Submission Tracking screen → ✅ Loads
- [ ] Create submission → ✅ Works
- [ ] Update status with feedback → ✅ Works
- [ ] View statistics → ✅ Shows data

**Time:** ~2 minutes

---

## 📊 Total Testing Time: ~25 minutes

## 🚀 Quick Test Script

```bash
# 1. Build and install
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Monitor logs (run in separate terminal)
adb logcat -s TeacherAnalytics:D StudentAnalytics:D AdminStudentApps:D TeacherSubjects:D GradeCompletionNotification:D GradeExport:D OfflineStatus:D *:E

# 3. Test in this order:
# High Priority → Medium Priority → Low Priority
```

## ✅ Success Criteria Summary

- **High Priority:** All 3 features work without errors
- **Medium Priority:** All 3 features work correctly
- **Low Priority:** All 4 features verified/working

**Overall:** 10/11 features testable (91.7% coverage)

---

## 🐛 If Something Fails

1. Check Logcat for error messages
2. Verify test data exists (users, subjects, grades)
3. Check Firebase Console for data
4. Verify internet connection (for online features)
5. Clear app data and retry: `adb shell pm clear com.smartacademictracker`

---

**Last Updated:** After fixing all low priority features


# Year Level Progression Testing Guide

## Overview
Year level progression automatically advances students to the next year level when transitioning from **Summer** to **First Semester** of a new academic year.

## Prerequisites

Before testing, ensure you have:

1. **Admin Account**: Logged in as an admin user
2. **Test Students**: At least 2-3 test students with:
   - Active accounts (`active = true`)
   - Assigned `yearLevelId` (e.g., "1st Year")
   - Assigned `courseId` (e.g., "CS", "IT")
   - Current year level should be less than 4th year (max level)
3. **Year Levels**: Year levels must exist for the course (1st Year, 2nd Year, 3rd Year, 4th Year)
4. **Academic Periods**: You'll need to create academic periods for testing

## Testing Steps

### Step 1: Prepare Test Data

1. **Create Test Students** (if not already created):
   - Go to Admin → Pre-Registered Students
   - Pre-register students with:
     - Student IDs (e.g., "2024-001", "2024-002")
     - Course Code (e.g., "CS")
     - Year Level (e.g., "1st Year")
   - Activate their accounts

2. **Verify Student Data**:
   - Check that students have:
     - `yearLevelId` set
     - `courseId` set
     - `active = true`
   - Note their current year level (e.g., "1st Year")

### Step 2: Create Academic Periods

1. **Navigate to Academic Periods**:
   - Log in as Admin
   - Go to **Admin Dashboard** → **Academic Periods** tab
   - Or navigate to **Academic Management** → **Academic Periods**

2. **Create First Academic Period (Summer)**:
   - Click the **"+"** button or **"Add Academic Period"** button (usually in the top-right)
   - Fill in the form:
     - **Name**: "Academic Year 2025-2026 Summer"
     - **Academic Year**: "2025-2026" (format: YYYY-YYYY)
     - **Semester**: Select **"Summer Class"** from dropdown (⚠️ **CRITICAL: Must be SUMMER_CLASS**)
     - **Start Date**: Click date picker and select a start date
     - **End Date**: Click date picker and select an end date
     - **Description**: "Summer 2025-2026" (optional)
   - Click **"Create"** or **"Save"** button
   - You should see a success message

3. **Set as Active Period**:
   - Go back to the Academic Periods list
   - Find the "2025-2026 Summer" period you just created
   - Look for an **"Activate"** or **"Set as Active"** button/icon
   - Click it to set this period as active
   - This becomes the **previous period** for progression
   - You should see a success message confirming activation

4. **Create Second Academic Period (First Semester of New Year)**:
   - Click the **"+"** button or **"Add Academic Period"** button again
   - Fill in the form:
     - **Name**: "Academic Year 2026-2027 First Semester"
     - **Academic Year**: "2026-2027" (⚠️ **MUST be different from previous year**)
     - **Semester**: Select **"1st Semester"** from dropdown (⚠️ **CRITICAL: Must be FIRST_SEMESTER**)
     - **Start Date**: Click date picker and select a start date
     - **End Date**: Click date picker and select an end date
     - **Description**: "First Semester 2026-2027" (optional)
   - Click **"Create"** or **"Save"** button
   - You should see a success message

### Step 3: Trigger Progression

1. **Set New Period as Active**:
   - Go back to the Academic Periods list
   - Find the "2026-2027 First Semester" period you just created
   - Look for an **"Activate"** or **"Set as Active"** button/icon
   - Click it to set this period as active
   - **⚠️ THIS ACTION TRIGGERS THE PROGRESSION AUTOMATICALLY**

2. **Check Success Message**:
   - Wait for the operation to complete (may take a few seconds)
   - You should see a success message like:
     ```
     "Academic period activated successfully. X student(s) advanced to next year level. 
     Subjects will now show for this semester."
     ```
   - If progression occurred, it will show the number of students advanced
   - If no students were advanced, check the logcat for reasons (see Troubleshooting section)

### Step 4: Verify Progression

1. **Check Student Profiles**:
   - Log in as a test student (or check student data in admin panel)
   - Go to Profile
   - Verify that the **Year Level** has been updated:
     - If student was "1st Year" → Should now be "2nd Year"
     - If student was "2nd Year" → Should now be "3rd Year"
     - If student was "3rd Year" → Should now be "4th Year"
     - If student was "4th Year" → Should remain "4th Year" (max level)

2. **Check Notifications**:
   - Log in as a test student
   - Go to Notifications
   - Look for a notification like:
     ```
     "Congratulations! You have completed the academic year and have been 
     advanced from [Old Year Level] to [New Year Level] for Academic Year 2026-2027."
     ```

3. **Check Logcat** (for debugging):
   - Filter by: `YearLevelProgressionService`
   - Look for logs like:
     ```
     "Processing year level progression for period: [periodId]"
     "Found X active students to process"
     "Advanced student [studentId] from [oldYearLevel] to [newYearLevel]"
     "Year level progression completed: ..."
     ```

### Step 5: Test Edge Cases

1. **Test Maximum Level (4th Year)**:
   - Create a student at "4th Year"
   - Trigger progression
   - Verify: Student should remain at "4th Year" (not advance further)
   - Check logcat: Should see "Skipping student: already at max level"

2. **Test Missing Year Level**:
   - Create a student without `yearLevelId`
   - Trigger progression
   - Verify: Student should be skipped
   - Check logcat: Should see "Skipping student: missing year level or course"

3. **Test Missing Course**:
   - Create a student without `courseId`
   - Trigger progression
   - Verify: Student should be skipped
   - Check logcat: Should see "Skipping student: missing year level or course"

4. **Test Non-Progression Transitions**:
   - Set active period: "2025-2026 First Semester"
   - Change to: "2025-2026 Second Semester"
   - Verify: **No progression should occur** (same academic year)
   - Change to: "2025-2026 Summer"
   - Verify: **No progression should occur** (not Summer → First Semester)
   - Change to: "2026-2027 Second Semester"
   - Verify: **No progression should occur** (not Summer → First Semester)

## Expected Behavior

### ✅ Progression Triggers When:
- Previous period: **SUMMER_CLASS** of academic year (e.g., "2025-2026")
- New period: **FIRST_SEMESTER** of **NEXT** academic year (e.g., "2026-2027")
- Academic years are **different**

### ❌ Progression Does NOT Trigger When:
- Same academic year (e.g., "2025-2026" → "2025-2026")
- Not Summer → First Semester transition
- First time setup (no previous active period)
- Same period reactivated

## Troubleshooting

### Issue: No students advanced
**Possible Causes:**
1. Students don't have `yearLevelId` or `courseId` set
2. Students are inactive (`active = false`)
3. Students are already at maximum level (4th year)
4. Next year level doesn't exist for the course
5. Transition doesn't match progression criteria (Summer → First Semester of new year)

**Solution:**
- Check student data in Firestore or admin panel
- Verify year levels exist for the course
- Check logcat for specific error messages

### Issue: Progression triggered incorrectly
**Possible Causes:**
- Academic periods have incorrect semester values
- Academic years are the same

**Solution:**
- Verify academic period data:
  - Previous period: `semester = SUMMER_CLASS`
  - New period: `semester = FIRST_SEMESTER`
  - Academic years are different

### Issue: Some students advanced, others didn't
**Possible Causes:**
- Some students missing `yearLevelId` or `courseId`
- Some students at maximum level
- Next year level doesn't exist for some courses

**Solution:**
- Check logcat for skipped/error messages
- Verify all students have required fields
- Check that year levels exist for all courses

## Quick Test Scenario

Here's a quick test scenario you can follow:

1. **Create Test Student**: 
   - Go to Admin → Pre-Registered Students
   - Pre-register student with ID: "2024-001", Course: "CS", Year Level: "1st Year"
   - Activate the account

2. **Create Period 1**: 
   - Go to Admin → Academic Periods
   - Click "+" → Fill form:
     - Name: "Academic Year 2025-2026 Summer"
     - Academic Year: "2025-2026"
     - Semester: "Summer Class" ⚠️
     - Set dates
   - Click "Create"

3. **Set Period 1 as Active**:
   - Find "2025-2026 Summer" in the list
   - Click "Activate" or "Set as Active"

4. **Create Period 2**: 
   - Click "+" → Fill form:
     - Name: "Academic Year 2026-2027 First Semester"
     - Academic Year: "2026-2027" ⚠️ (different year)
     - Semester: "1st Semester" ⚠️ (must be FIRST_SEMESTER)
     - Set dates
   - Click "Create"

5. **Set Period 2 as Active** ← **Progression triggers here**
   - Find "2026-2027 First Semester" in the list
   - Click "Activate" or "Set as Active"
   - Wait for success message showing number of students advanced

6. **Verify**: 
   - Log in as student "2024-001"
   - Go to Profile
   - Check Year Level: Should now be "2nd Year" (was "1st Year")

7. **Check Notification**: 
   - Still logged in as student
   - Go to Notifications
   - Should see notification about advancement

## Notes

- Progression happens **automatically** when you set the new period as active
- No manual intervention needed after setting the active period
- Students receive notifications about their advancement
- The system processes students in batches (50 at a time) for performance
- Progression only occurs once per academic period transition


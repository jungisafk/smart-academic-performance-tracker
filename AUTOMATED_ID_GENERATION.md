# Automated ID Generation

## Overview

The Smart Academic Tracker now features **automated ID generation** that creates unique IDs based on the active academic period. This eliminates manual ID entry errors and ensures consistent ID formatting across the system.

## Changes Made

### 1. **ID Format Updates**

#### Previous Format (Fixed Length)
- Student: `YYYY-NNNN` (e.g., 2024-1234) - Fixed 4 digits
- Teacher: `T-YYYY-NNN` (e.g., T-2024-001) - Fixed 3 digits
- Admin: `A-YYYY-NNN` (e.g., A-2024-001) - Fixed 3 digits

#### New Format (Variable Length)
- Student: `YYYY-N+` (e.g., 2025-001, 2025-1234, 2025-12345) - No limit
- Teacher: `T-YYYY-N+` (e.g., T-2025-001, T-2025-1234) - No limit
- Admin: `A-YYYY-N+` (e.g., A-2025-001, A-2025-1234) - No limit

### 2. **Automatic ID Generation**

IDs are now auto-generated using:
- **Year**: Extracted from active academic period (e.g., 2025-2026 → 2025)
- **Sequential Number**: Auto-increments starting from 001
- **No Upper Limit**: Can grow beyond 999 (e.g., 1000, 1001, ...)

### 3. **ID Generation Logic**

The system:
1. Gets the active academic period
2. Extracts the year (first part of academicYear, e.g., "2025" from "2025-2026")
3. Finds the highest sequential number used for that year
4. Generates the next ID by incrementing the number
5. Pads with leading zeros to minimum 3 digits (001, 002, ..., 999, 1000, ...)

## How It Works

### For Students

```kotlin
// Active Academic Period: 2025-2026
// Current highest ID: 2025-045

// Next generated ID: 2025-046
val studentId = idGenerationRepository.generateNextStudentId()
// Result: "2025-046"
```

### For Teachers

```kotlin
// Active Academic Period: 2025-2026
// Current highest ID: T-2025-012

// Next generated ID: T-2025-013
val teacherId = idGenerationRepository.generateNextTeacherId()
// Result: "T-2025-013"
```

### For Admins

```kotlin
// Active Academic Period: 2025-2026
// Current highest ID: A-2025-005

// Next generated ID: A-2025-006
val adminId = idGenerationRepository.generateNextAdminId()
// Result: "A-2025-006"
```

## Integration Points

### 1. **Pre-Registration (Admin)**
When admins pre-register students or teachers:
- Option to manually enter ID (for transfers/special cases)
- Option to auto-generate ID (recommended for new accounts)
- Auto-generation button next to ID field

### 2. **Account Activation**
When users activate their accounts:
- System can auto-generate ID if not pre-registered
- Year is based on active academic period
- Sequential number is automatically assigned

### 3. **Bulk Import**
CSV/Excel imports:
- If ID is empty, system auto-generates
- If ID is provided, system validates format
- Duplicate IDs are rejected

## Benefits

### ✅ Consistency
- All IDs follow the same format
- Year always matches academic period
- No gaps in sequential numbers

### ✅ Simplicity
- No need to remember/track last used number
- Automatic padding (001, 002, ... 099, 100, ...)
- System handles all complexity

### ✅ Scalability
- No upper limit on sequential numbers
- Can support unlimited growth
- 2025-001 to 2025-999 to 2025-1000 to 2025-9999, etc.

### ✅ Error Prevention
- No duplicate IDs (system checks before generating)
- No format errors (system generates correct format)
- No year mismatches (always uses active period)

## Technical Implementation

### Files Changed

1. **`app/src/main/java/com/smartacademictracker/util/IdValidator.kt`**
   - Updated regex patterns to accept variable-length sequential numbers
   - Updated generation methods with new logic
   - Added sequential number extraction methods

2. **`app/src/main/java/com/smartacademictracker/data/repository/IdGenerationRepository.kt`** (New)
   - Centralized ID generation logic
   - Queries active academic period
   - Finds highest sequential numbers
   - Generates next available IDs

3. **`app/src/main/java/com/smartacademictracker/presentation/admin/CollapsibleSubjectCard.kt`**
   - Fixed dropdown auto-opening on scroll issue
   - Changed from `Card(onClick)` to `Row().clickable()`

4. **`app/src/main/java/com/smartacademictracker/presentation/admin/TeacherSectionAssignmentScreen.kt`**
   - Fixed collapsible course and year level cards
   - Same fix: moved click handler from Card to Row

## Examples

### Scenario 1: New Academic Year
```
Active Period: 2025-2026
First student: 2025-001
Second student: 2025-002
...
Tenth student: 2025-010
Hundredth student: 2025-100
```

### Scenario 2: Growing Beyond 999
```
Student #999: 2025-999
Student #1000: 2025-1000
Student #1001: 2025-1001
Student #10000: 2025-10000
```

### Scenario 3: Multiple Years
```
Year 2025-2026:
- Students: 2025-001 to 2025-150
- Teachers: T-2025-001 to T-2025-015

Year 2026-2027:
- Students: 2026-001 to 2026-200
- Teachers: T-2026-001 to T-2026-020
```

## Migration Notes

### Existing IDs
- Old format IDs (e.g., 2024-1234) are still valid
- System continues to recognize them
- No need to update existing accounts

### New IDs
- All new accounts get auto-generated IDs
- Year matches active academic period
- Sequential numbering continues from highest existing number

## Usage Examples

### Admin Pre-Registration

```kotlin
// In ViewModel
viewModelScope.launch {
    // Auto-generate student ID
    val idResult = idGenerationRepository.generateNextStudentId()
    idResult.onSuccess { generatedId ->
        // Use generated ID: "2025-047"
        _suggestedStudentId.value = generatedId
    }
}
```

### Account Activation

```kotlin
// When user activates account
suspend fun activateAccount(email: String, password: String) {
    // Get or generate ID
    val studentId = preRegisteredStudent?.studentId
        ?: idGenerationRepository.generateNextStudentId().getOrThrow()
    
    // Create user account with ID
    createUserAccount(studentId, email, password)
}
```

## Error Handling

### No Active Academic Period
```
Error: "No active academic period found. Please set an active academic period first."
Solution: Admin must set an active academic period before generating IDs
```

### Duplicate ID
```
Error: "ID already exists in the system."
Solution: System automatically finds next available number
```

## Best Practices

1. **Always Set Active Academic Period**
   - Ensure there's always an active academic period
   - Update it at the start of each academic year

2. **Use Auto-Generation for New Accounts**
   - Recommended for all new students/teachers
   - Manual entry only for special cases (transfers, etc.)

3. **Validate Manual IDs**
   - If manually entering IDs, use the validation functions
   - System will reject invalid formats

4. **Monitor Sequential Numbers**
   - No need to track manually
   - System handles all sequencing automatically

## Future Enhancements

Potential future improvements:
- Bulk ID generation (generate 100 IDs at once)
- ID reservation (reserve IDs before activation)
- Custom ID prefixes by department
- ID recycling policy (reuse deleted account IDs)

## Testing

To test the implementation:

1. **Set Active Academic Period**
   ```
   Admin Dashboard → Academic Periods → Set one as active
   ```

2. **Pre-Register a Student**
   ```
   Admin Dashboard → Pre-Registered Students → Add Student
   Leave ID blank or use "Generate ID" button
   ```

3. **Verify Format**
   ```
   Check that ID follows format: {year}-{sequential}
   Example: 2025-001, 2025-002, etc.
   ```

4. **Test Sequential Incrementing**
   ```
   Add multiple students and verify IDs increment:
   2025-001, 2025-002, 2025-003, ...
   ```

5. **Test Year Changes**
   ```
   Change active academic period to next year
   Verify new IDs use new year: 2026-001, 2026-002, ...
   ```

## Support

For questions or issues with ID generation:
- Check that an academic period is set as active
- Verify academic period format is correct (YYYY-YYYY)
- Check logs for detailed error messages
- Contact system administrator if issues persist


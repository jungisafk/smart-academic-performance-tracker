# CSV Import Templates for Student and Teacher Pre-Registration

This document provides templates and guidelines for importing students and teachers via CSV files.

## Files Included

1. **student_import_template.csv** - Template for student bulk import
2. **teacher_import_template.csv** - Template for teacher bulk import

## Student Import Template

### Required Columns

| Column Name | Description | Format | Example |
|------------|-------------|--------|---------|
| **Student ID** | Unique student identifier | YYYY-SEQUENCE (1-5 digits) | `2024-001`, `2025-123`, `2030-1000` |
| **First Name** | Student's first name | Text | `John` |
| **Last Name** | Student's last name | Text | `Doe` |
| **Course Code** | Course code the student belongs to | Text | `CS`, `IT`, `ENG` |
| **Year Level** | Academic year level | Number (1-4) or Text | `1`, `2`, `3`, `4` or `1st Year`, `2nd Year` |

### Optional Columns

| Column Name | Description | Format | Example |
|------------|-------------|--------|---------|
| **Middle Name** | Student's middle name | Text | `Michael` |
| **Email** | Student's email address | Email format | `john.doe@example.com` |
| **Enrollment Year** | Academic year enrolled | YYYY-YYYY format | `2024-2025` |

### Student ID Format Requirements

⚠️ **IMPORTANT**: The Student ID must follow a specific format:

- **Format**: `YYYY-SEQUENCE`
- **Year Segment (First 4 digits)**: Any 4-digit year (e.g., 1952, 2001, 2099)
- **Sequence Number (Last 1-5 digits)**: 
  - Range: 1 to 99999
  - **Auto-padding rules**:
    - 1-2 digits: Automatically zero-padded to 3 digits (e.g., `1` → `001`, `23` → `023`, `7` → `007`)
    - 3-5 digits: Kept as-is (e.g., `999` stays `999`, `1000` stays `1000`, `15234` stays `15234`)
- **Examples**:
  - `2024-1` → Auto-formatted to `2024-001`
  - `2024-23` → Auto-formatted to `2024-023`
  - `2025-123` → Stays `2025-123`
  - `2030-1000` → Stays `2030-1000`
  - `2030-15234` → Stays `2030-15234`

### Student Import Example

```csv
Student ID,First Name,Last Name,Middle Name,Course Code,Year Level,Email,Enrollment Year
2024-001,John,Doe,Michael,CS,1,john.doe@example.com,2024-2025
2024-002,Jane,Smith,Ann,CS,1,jane.smith@example.com,2024-2025
2024-003,Bob,Johnson,,IT,2,bob.johnson@example.com,2024-2025
```

## Teacher Import Template

### Required Columns

| Column Name | Description | Format | Example |
|------------|-------------|--------|---------|
| **Teacher ID** | Unique teacher identifier | Text | `T-2024-001`, `EMP-12345` |
| **First Name** | Teacher's first name | Text | `Alice` |
| **Last Name** | Teacher's last name | Text | `Williams` |
| **Department Code** | Department/Course code | Text | `CS`, `IT`, `ENG` |

### Optional Columns

| Column Name | Description | Format | Example |
|------------|-------------|--------|---------|
| **Middle Name** | Teacher's middle name | Text | `Marie` |
| **Email** | Teacher's email address | Email format | `alice.williams@example.com` |

### Teacher Import Example

```csv
Teacher ID,First Name,Last Name,Middle Name,Department Code,Email
T-2024-001,Alice,Williams,Marie,CS,alice.williams@example.com
T-2024-002,Robert,Brown,James,IT,robert.brown@example.com
T-2024-003,Sarah,Davis,,CS,sarah.davis@example.com
```

## Important Notes

### Column Name Flexibility

The CSV parser is **case-insensitive** and accepts various column name formats. For example:
- `Student ID`, `student id`, `StudentID`, `student_id` are all accepted
- `First Name`, `first name`, `FirstName`, `firstname` are all accepted
- `Course Code`, `course code`, `CourseCode`, `coursecode` are all accepted

### Common Column Name Variations

**Student Import:**
- Student ID: `Student ID`, `StudentID`, `ID`, `student_id`
- First Name: `First Name`, `FirstName`, `First`, `Given Name`
- Last Name: `Last Name`, `LastName`, `Last`, `Surname`, `Family Name`
- Course Code: `Course Code`, `CourseCode`, `Course`, `Course Name`
- Year Level: `Year Level`, `YearLevel`, `Year`, `Level`, `Grade Level`

**Teacher Import:**
- Teacher ID: `Teacher ID`, `TeacherID`, `ID`, `Employee ID`, `EmployeeID`
- Department: `Department`, `Department Code`, `DepartmentCode`, `Course`, `Dept`

### Validation Rules

1. **Student ID Uniqueness**: Each Student ID must be unique. If a duplicate is found, the import will fail with an error message: "ID already exists. Please use a different sequence number."

2. **Required Fields**: 
   - **Students**: Student ID, First Name, Last Name, Course Code, and Year Level are required.
   - **Teachers**: Teacher ID, First Name, Last Name, and Department Code are required.

3. **Year Level Format**: Accepts numeric values (1, 2, 3, 4) or text formats ("1st Year", "2nd Year", etc.)

4. **Date Formats**: 
   - Enrollment Year: `YYYY-YYYY` (e.g., `2024-2025`)

5. **Empty Rows**: Empty rows are automatically skipped during import.

## Usage Instructions

1. Download the appropriate template file (`student_import_template.csv` or `teacher_import_template.csv`)
2. Fill in the data following the format guidelines
3. Ensure all required columns are present
4. Verify Student ID format (for students) matches the requirements
5. Import the CSV file through the admin bulk import screen
6. Review any validation errors and correct them before re-importing

## Troubleshooting

- **"Missing required columns"**: Ensure all required columns are present in the header row
- **"ID already exists"**: The Student ID is already in use. Use a different sequence number
- **"Invalid Student ID format"**: Check that the Student ID follows the YYYY-SEQUENCE format
- **"Course Code not found"**: The Course Code must match an existing course in the system
- **"Year Level invalid"**: Use numeric values (1-4) or standard text formats ("1st Year", etc.)


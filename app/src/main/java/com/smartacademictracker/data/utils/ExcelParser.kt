package com.smartacademictracker.data.utils

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Native Android XLSX parser using ZipInputStream and XmlPullParser
 * No external dependencies - fully compatible with Android runtime
 * 
 * XLSX files are ZIP archives containing XML files:
 * - xl/sharedStrings.xml: Shared string table
 * - xl/worksheets/sheet1.xml: Worksheet data
 */
object ExcelParser {
    
    private const val TAG = "ExcelParser"
    
    /**
     * Parse XLSX file and extract student data
     * @param inputStream Input stream of the XLSX file
     * @param fileName File name (should end with .xlsx)
     * @return Result containing list of parsed students or error
     */
    fun parseStudentExcel(inputStream: InputStream, fileName: String): Result<List<StudentRow>> {
        return try {
            Log.d(TAG, "Starting native XLSX parsing for file: $fileName")
            
            if (!fileName.endsWith(".xlsx", ignoreCase = true)) {
                return Result.failure(Exception("Only .xlsx format is supported. For .xls files, please convert to .xlsx or use CSV format."))
            }
            
            val xlsxData = parseXlsxFile(inputStream)
            
            if (xlsxData.rows.isEmpty()) {
                return Result.failure(Exception("Excel file is empty or contains no data."))
            }
            
            if (xlsxData.rows.size < 2) {
                return Result.failure(Exception("Excel file must have at least a header row and one data row."))
            }
            
            // First row is header
            val headerRow = xlsxData.rows[0]
            val headerMap = mutableMapOf<String, Int>()
            
            headerRow.forEachIndexed { index, value ->
                val headerValue = value.trim().lowercase()
                if (headerValue.isNotBlank()) {
                    headerMap[headerValue] = index
                }
            }
            
            Log.d(TAG, "Found columns: ${headerMap.keys}")
            
            // Find required columns (case-insensitive matching)
            val studentIdCol = findColumn(headerMap, listOf("student id", "studentid", "id", "student_id"))
            val firstNameCol = findColumn(headerMap, listOf("first name", "firstname", "first", "given name", "givenname"))
            val lastNameCol = findColumn(headerMap, listOf("last name", "lastname", "last", "surname", "family name", "familyname"))
            
            if (studentIdCol == null || firstNameCol == null || lastNameCol == null) {
                val missing = mutableListOf<String>()
                if (studentIdCol == null) missing.add("Student ID")
                if (firstNameCol == null) missing.add("First Name")
                if (lastNameCol == null) missing.add("Last Name")
                return Result.failure(Exception("Missing required columns: ${missing.joinToString(", ")}"))
            }
            
            // Find optional columns
            val middleNameCol = findColumn(headerMap, listOf("middle name", "middlename", "middle", "middle initial", "mi"))
            val emailCol = findColumn(headerMap, listOf("email", "e-mail", "email address"))
            val courseCodeCol = findColumn(headerMap, listOf("course code", "coursecode", "course", "course name", "coursename"))
            val yearLevelCol = findColumn(headerMap, listOf("year level", "yearlevel", "year", "level", "grade level"))
            val enrollmentYearCol = findColumn(headerMap, listOf("enrollment year", "enrollmentyear", "academic year", "academicyear", "school year"))
            
            val students = mutableListOf<StudentRow>()
            val errors = mutableListOf<String>()
            
            // Parse data rows (skip header at index 0)
            for (rowIndex in 1 until xlsxData.rows.size) {
                val row = xlsxData.rows[rowIndex]
                
                try {
                    val studentId = row.getOrNull(studentIdCol)?.trim()
                    val firstName = row.getOrNull(firstNameCol)?.trim()
                    val lastName = row.getOrNull(lastNameCol)?.trim()
                    
                    // Skip empty rows
                    if (studentId.isNullOrBlank() && firstName.isNullOrBlank() && lastName.isNullOrBlank()) {
                        continue
                    }
                    
                    if (studentId.isNullOrBlank()) {
                        errors.add("Row ${rowIndex + 1}: Missing Student ID")
                        continue
                    }
                    if (firstName.isNullOrBlank()) {
                        errors.add("Row ${rowIndex + 1}: Missing First Name for Student ID: $studentId")
                        continue
                    }
                    if (lastName.isNullOrBlank()) {
                        errors.add("Row ${rowIndex + 1}: Missing Last Name for Student ID: $studentId")
                        continue
                    }
                    
                    val student = StudentRow(
                        studentId = studentId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleNameCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        email = emailCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        courseCode = courseCodeCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        yearLevel = yearLevelCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        section = null, // Section is no longer used
                        enrollmentYear = enrollmentYearCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        phoneNumber = null, // Not in form
                        dateOfBirth = null, // Not in form
                        address = null // Not in form
                    )
                    
                    students.add(student)
                } catch (e: Exception) {
                    errors.add("Row ${rowIndex + 1}: Error parsing row - ${e.message}")
                    Log.e(TAG, "Error parsing row ${rowIndex + 1}", e)
                }
            }
            
            if (students.isEmpty() && errors.isNotEmpty()) {
                Result.failure(Exception("Failed to parse any students. Errors:\n${errors.take(10).joinToString("\n")}"))
            } else {
                Log.d(TAG, "Successfully parsed ${students.size} students from Excel. ${errors.size} errors encountered.")
                Result.success(students)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Excel file", e)
            Result.failure(Exception("Error parsing Excel file: ${e.message}"))
        }
    }
    
    /**
     * Parse XLSX file and extract teacher data
     */
    fun parseTeacherExcel(inputStream: InputStream, fileName: String): Result<List<TeacherRow>> {
        return try {
            Log.d(TAG, "Starting native XLSX parsing for teachers, file: $fileName")
            
            if (!fileName.endsWith(".xlsx", ignoreCase = true)) {
                return Result.failure(Exception("Only .xlsx format is supported. For .xls files, please convert to .xlsx or use CSV format."))
            }
            
            val xlsxData = parseXlsxFile(inputStream)
            
            if (xlsxData.rows.isEmpty()) {
                return Result.failure(Exception("Excel file is empty or contains no data."))
            }
            
            if (xlsxData.rows.size < 2) {
                return Result.failure(Exception("Excel file must have at least a header row and one data row."))
            }
            
            val headerRow = xlsxData.rows[0]
            val headerMap = mutableMapOf<String, Int>()
            
            headerRow.forEachIndexed { index, value ->
                val headerValue = value.trim().lowercase()
                if (headerValue.isNotBlank()) {
                    headerMap[headerValue] = index
                }
            }
            
            Log.d(TAG, "Found columns: ${headerMap.keys}")
            
            val teacherIdCol = findColumn(headerMap, listOf("teacher id", "teacherid", "id", "employee id", "employeeid"))
            val firstNameCol = findColumn(headerMap, listOf("first name", "firstname", "first", "given name", "givenname"))
            val lastNameCol = findColumn(headerMap, listOf("last name", "lastname", "last", "surname", "family name", "familyname"))
            
            if (teacherIdCol == null || firstNameCol == null || lastNameCol == null) {
                val missing = mutableListOf<String>()
                if (teacherIdCol == null) missing.add("Teacher ID")
                if (firstNameCol == null) missing.add("First Name")
                if (lastNameCol == null) missing.add("Last Name")
                return Result.failure(Exception("Missing required columns: ${missing.joinToString(", ")}"))
            }
            
            val middleNameCol = findColumn(headerMap, listOf("middle name", "middlename", "middle", "middle initial", "mi"))
            val emailCol = findColumn(headerMap, listOf("email", "e-mail", "email address"))
            val departmentCodeCol = findColumn(headerMap, listOf("department", "departmentcode", "course code", "course", "department name"))
            val employmentTypeCol = findColumn(headerMap, listOf("employment type", "employmenttype", "type"))
            val positionCol = findColumn(headerMap, listOf("position"))
            val specializationCol = findColumn(headerMap, listOf("specialization"))
            val phoneCol = findColumn(headerMap, listOf("phone number", "phonenumber", "phone", "mobile", "contact number"))
            val dobCol = findColumn(headerMap, listOf("date of birth", "dateofbirth", "dob", "birthdate", "birth date"))
            val addressCol = findColumn(headerMap, listOf("address", "home address", "homeaddress", "residence"))
            val dateHiredCol = findColumn(headerMap, listOf("date hired", "datehired"))
            val employeeNumberCol = findColumn(headerMap, listOf("employee number", "employeenumber"))
            
            val teachers = mutableListOf<TeacherRow>()
            val errors = mutableListOf<String>()
            
            for (rowIndex in 1 until xlsxData.rows.size) {
                val row = xlsxData.rows[rowIndex]
                
                try {
                    val teacherId = row.getOrNull(teacherIdCol)?.trim()
                    val firstName = row.getOrNull(firstNameCol)?.trim()
                    val lastName = row.getOrNull(lastNameCol)?.trim()
                    
                    if (teacherId.isNullOrBlank() && firstName.isNullOrBlank() && lastName.isNullOrBlank()) {
                        continue
                    }
                    
                    if (teacherId.isNullOrBlank()) {
                        errors.add("Row ${rowIndex + 1}: Missing Teacher ID")
                        continue
                    }
                    if (firstName.isNullOrBlank()) {
                        errors.add("Row ${rowIndex + 1}: Missing First Name for Teacher ID: $teacherId")
                        continue
                    }
                    if (lastName.isNullOrBlank()) {
                        errors.add("Row ${rowIndex + 1}: Missing Last Name for Teacher ID: $teacherId")
                        continue
                    }
                    
                    val teacher = TeacherRow(
                        teacherId = teacherId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleNameCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        email = emailCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        departmentCode = departmentCodeCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        employmentType = employmentTypeCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        position = positionCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        specialization = specializationCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        phoneNumber = phoneCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        dateOfBirth = dobCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        address = addressCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        dateHired = dateHiredCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } },
                        employeeNumber = employeeNumberCol?.let { row.getOrNull(it)?.trim()?.takeIf { it.isNotBlank() } }
                    )
                    
                    teachers.add(teacher)
                } catch (e: Exception) {
                    errors.add("Row ${rowIndex + 1}: Error parsing row - ${e.message}")
                    Log.e(TAG, "Error parsing row ${rowIndex + 1}", e)
                }
            }
            
            if (teachers.isEmpty() && errors.isNotEmpty()) {
                Result.failure(Exception("Failed to parse any teachers. Errors:\n${errors.take(10).joinToString("\n")}"))
            } else {
                Log.d(TAG, "Successfully parsed ${teachers.size} teachers from Excel. ${errors.size} errors encountered.")
                Result.success(teachers)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Excel file", e)
            Result.failure(Exception("Error parsing Excel file: ${e.message}"))
        }
    }
    
    /**
     * Parse XLSX file structure using native Android APIs
     * XLSX files are ZIP archives containing XML files
     */
    private fun parseXlsxFile(inputStream: InputStream): XlsxData {
        val sharedStrings = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        
        ZipInputStream(inputStream).use { zipStream ->
            var entry = zipStream.nextEntry
            
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        Log.d(TAG, "Parsing shared strings...")
                        sharedStrings.addAll(parseSharedStrings(zipStream))
                        Log.d(TAG, "Found ${sharedStrings.size} shared strings")
                    }
                    entry.name == "xl/worksheets/sheet1.xml" -> {
                        Log.d(TAG, "Parsing worksheet...")
                        rows.addAll(parseWorksheet(zipStream, sharedStrings))
                        Log.d(TAG, "Found ${rows.size} rows")
                    }
                }
                
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
        }
        
        return XlsxData(rows, sharedStrings)
    }
    
    /**
     * Parse shared strings table from xl/sharedStrings.xml
     * Shared strings are stored in a separate file and referenced by index
     */
    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        
        var eventType = parser.eventType
        var currentText = StringBuilder()
        var inText = false
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        inText = true
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t" && inText) {
                        strings.add(currentText.toString())
                        inText = false
                    }
                }
            }
            eventType = parser.next()
        }
        
        return strings
    }
    
    /**
     * Parse worksheet data from xl/worksheets/sheet1.xml
     * Cells can contain values directly or reference shared strings
     */
    private fun parseWorksheet(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        
        var eventType = parser.eventType
        var currentRow: MutableList<String>? = null
        var currentCellType: String? = null
        var currentCellRef: String? = null
        var currentValue = StringBuilder()
        var inValue = false
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            // Cell - get type (t) and reference (r) attributes
                            currentCellType = parser.getAttributeValue(null, "t")
                            currentCellRef = parser.getAttributeValue(null, "r")
                        }
                        "v" -> {
                            // Value
                            inValue = true
                            currentValue.clear()
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) {
                        currentValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> {
                            inValue = false
                        }
                        "c" -> {
                            // End of cell - process the value
                            val cellValue = when (currentCellType) {
                                "s" -> {
                                    // Shared string - lookup by index
                                    val index = currentValue.toString().toIntOrNull() ?: 0
                                    sharedStrings.getOrNull(index) ?: ""
                                }
                                else -> {
                                    // Inline value (number, boolean, date, etc.)
                                    currentValue.toString()
                                }
                            }
                            
                            if (currentRow != null) {
                                // Get column index from cell reference (e.g., "A1" -> 0, "B1" -> 1)
                                val colIndex = getColumnIndex(currentCellRef ?: "A1")
                                
                                // Pad with empty strings if necessary
                                while (currentRow.size <= colIndex) {
                                    currentRow.add("")
                                }
                                
                                currentRow[colIndex] = cellValue
                            }
                            
                            currentValue.clear()
                            currentCellType = null
                            currentCellRef = null
                        }
                        "row" -> {
                            // End of row
                            if (currentRow != null && currentRow.isNotEmpty()) {
                                rows.add(currentRow)
                            }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        
        return rows
    }
    
    /**
     * Convert Excel column reference to zero-based index
     * Examples: A -> 0, B -> 1, Z -> 25, AA -> 26, AB -> 27
     */
    private fun getColumnIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase()
        var index = 0
        
        for (char in letters) {
            index = index * 26 + (char - 'A' + 1)
        }
        
        return (index - 1).coerceAtLeast(0)
    }
    
    /**
     * Find column index by matching against possible column names (case-insensitive)
     */
    private fun findColumn(headerMap: Map<String, Int>, possibleNames: List<String>): Int? {
        for (name in possibleNames) {
            headerMap[name.lowercase()]?.let { return it }
        }
        return null
    }
    
    /**
     * Data class to hold parsed XLSX data
     */
    private data class XlsxData(
        val rows: List<List<String>>,
        val sharedStrings: List<String>
    )
}

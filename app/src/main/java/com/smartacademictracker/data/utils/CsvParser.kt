package com.smartacademictracker.data.utils

import android.util.Log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Utility class for parsing CSV files containing student data
 * Expected columns (case-insensitive, flexible order):
 * - Student ID / StudentID / ID
 * - First Name / FirstName / First
 * - Last Name / LastName / Last
 * - Middle Name / MiddleName / Middle (optional)
 * - Email (optional)
 * - Course Code / CourseCode / Course (optional, can use Course Name)
 * - Year Level / YearLevel / Year / Level (1, 2, 3, 4 or "1st Year", "2nd Year", etc.)
 * - Section (optional)
 * - Enrollment Year / EnrollmentYear / Academic Year (optional, defaults to current)
 * - Phone Number / PhoneNumber / Phone (optional)
 * - Date of Birth / DateOfBirth / DOB / Birthdate (optional, format: YYYY-MM-DD or DD/MM/YYYY)
 * - Address (optional)
 */
object CsvParser {
    
    private const val TAG = "CsvParser"
    
    /**
     * Parse CSV file and extract student data
     * @param inputStream Input stream of the CSV file
     * @return Result containing list of parsed students or error
     */
    fun parseStudentCsv(inputStream: InputStream): Result<List<StudentRow>> {
        return try {
            Log.d(TAG, "Starting CSV parsing...")
            val reader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
            
            val csvParser = try {
                CSVParser(reader, CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build())
            } catch (e: Exception) {
                Log.e(TAG, "Error creating CSVParser", e)
                return Result.failure(Exception("Error parsing CSV format: ${e.message}"))
            }
            
            val headerMap = csvParser.headerMap
            Log.d(TAG, "Header map size: ${headerMap.size}, Headers: ${headerMap.keys}")
            
            if (headerMap.isEmpty()) {
                csvParser.close()
                Log.e(TAG, "CSV file has no headers")
                return Result.failure(Exception("CSV file must have a header row. Please check your file format."))
            }
            
            Log.d(TAG, "Found columns: ${headerMap.keys}")
            
            // Find required columns (case-insensitive matching)
            val studentIdCol = findColumn(headerMap, listOf("student id", "studentid", "id", "student_id"))
            val firstNameCol = findColumn(headerMap, listOf("first name", "firstname", "first", "given name", "givenname"))
            val lastNameCol = findColumn(headerMap, listOf("last name", "lastname", "last", "surname", "family name", "familyname"))
            
            if (studentIdCol == null || firstNameCol == null || lastNameCol == null) {
                csvParser.close()
                val missing = mutableListOf<String>()
                if (studentIdCol == null) missing.add("Student ID")
                if (firstNameCol == null) missing.add("First Name")
                if (lastNameCol == null) missing.add("Last Name")
                return Result.failure(Exception("Missing required columns: ${missing.joinToString(", ")}"))
            }
            
            // Find optional columns
            val middleNameCol = findColumn(headerMap, listOf("middle name", "middlename", "middle", "middle initial", "middleinitial", "mi"))
            val emailCol = findColumn(headerMap, listOf("email", "e-mail", "email address", "emailaddress"))
            val courseCodeCol = findColumn(headerMap, listOf("course code", "coursecode", "course", "course name", "coursename"))
            val yearLevelCol = findColumn(headerMap, listOf("year level", "yearlevel", "year", "level", "grade level", "gradelevel"))
            val sectionCol = findColumn(headerMap, listOf("section", "class", "section name", "sectionname"))
            val enrollmentYearCol = findColumn(headerMap, listOf("enrollment year", "enrollmentyear", "academic year", "academicyear", "school year", "schoolyear"))
            val phoneCol = findColumn(headerMap, listOf("phone number", "phonenumber", "phone", "mobile", "contact number", "contactnumber"))
            val dobCol = findColumn(headerMap, listOf("date of birth", "dateofbirth", "dob", "birthdate", "birth date"))
            val addressCol = findColumn(headerMap, listOf("address", "home address", "homeaddress", "residence"))
            
            val students = mutableListOf<StudentRow>()
            val errors = mutableListOf<String>()
            
            var rowIndex = 1 // Start from 1 (header is 0)
            for (record in csvParser) {
                rowIndex++
                try {
                    val studentId = getCellValue(record, studentIdCol)?.trim()
                    val firstName = getCellValue(record, firstNameCol)?.trim()
                    val lastName = getCellValue(record, lastNameCol)?.trim()
                    
                    // Skip empty rows
                    if (studentId.isNullOrBlank() && firstName.isNullOrBlank() && lastName.isNullOrBlank()) {
                        continue
                    }
                    
                    if (studentId.isNullOrBlank()) {
                        errors.add("Row $rowIndex: Missing Student ID")
                        continue
                    }
                    if (firstName.isNullOrBlank()) {
                        errors.add("Row $rowIndex: Missing First Name for Student ID: $studentId")
                        continue
                    }
                    if (lastName.isNullOrBlank()) {
                        errors.add("Row $rowIndex: Missing Last Name for Student ID: $studentId")
                        continue
                    }
                    
                    val student = StudentRow(
                        studentId = studentId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = getCellValue(record, middleNameCol)?.trim()?.takeIf { it.isNotBlank() },
                        email = getCellValue(record, emailCol)?.trim()?.takeIf { it.isNotBlank() },
                        courseCode = getCellValue(record, courseCodeCol)?.trim()?.takeIf { it.isNotBlank() },
                        yearLevel = getCellValue(record, yearLevelCol)?.trim()?.takeIf { it.isNotBlank() },
                        section = getCellValue(record, sectionCol)?.trim()?.takeIf { it.isNotBlank() },
                        enrollmentYear = getCellValue(record, enrollmentYearCol)?.trim()?.takeIf { it.isNotBlank() },
                        phoneNumber = getCellValue(record, phoneCol)?.trim()?.takeIf { it.isNotBlank() },
                        dateOfBirth = getCellValue(record, dobCol)?.trim()?.takeIf { it.isNotBlank() },
                        address = getCellValue(record, addressCol)?.trim()?.takeIf { it.isNotBlank() }
                    )
                    
                    students.add(student)
                } catch (e: Exception) {
                    errors.add("Row $rowIndex: Error parsing row - ${e.message}")
                    Log.e(TAG, "Error parsing row $rowIndex", e)
                }
            }
            
            csvParser.close()
            
            if (students.isEmpty() && errors.isNotEmpty()) {
                Result.failure(Exception("Failed to parse any students. Errors:\n${errors.take(10).joinToString("\n")}"))
            } else {
                Log.d(TAG, "Successfully parsed ${students.size} students. ${errors.size} errors encountered.")
                Result.success(students)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Find column name by matching against possible column names (case-insensitive)
     */
    private fun findColumn(headerMap: Map<String, Int>, possibleNames: List<String>): String? {
        for (name in possibleNames) {
            headerMap.keys.find { it.equals(name, ignoreCase = true) }?.let { return it }
        }
        return null
    }
    
    /**
     * Get cell value as string from CSV record
     */
    private fun getCellValue(record: CSVRecord, columnName: String?): String? {
        if (columnName == null) return null
        return try {
            val value = record.get(columnName)
            if (value.isNullOrBlank()) null else value
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Data class representing a row from CSV file
 */
data class StudentRow(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val email: String? = null,
    val courseCode: String? = null,
    val yearLevel: String? = null,
    val section: String? = null,
    val enrollmentYear: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null
)


package com.smartacademictracker.data.utils

import android.util.Log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Utility class for parsing CSV files containing teacher data
 * Expected columns (case-insensitive, flexible order):
 * - Teacher ID / TeacherID / ID / Employee ID
 * - First Name / FirstName / First
 * - Last Name / LastName / Last
 * - Middle Name / MiddleName / Middle (optional)
 * - Email (optional)
 * - Department / Department Code / Course (optional)
 * - Employment Type / EmploymentType / Type (Full-time, Part-time, Contract, etc.) (optional)
 * - Position (optional, e.g., "Professor", "Instructor")
 * - Specialization (optional)
 * - Phone Number / PhoneNumber / Phone (optional)
 * - Date of Birth / DateOfBirth / DOB / Birthdate (optional, format: YYYY-MM-DD)
 * - Address (optional)
 * - Date Hired / DateHired / Hire Date (optional, format: YYYY-MM-DD)
 * - Employee Number / EmployeeNumber (optional)
 */
object TeacherCsvParser {
    
    private const val TAG = "TeacherCsvParser"
    
    /**
     * Parse CSV file and extract teacher data
     * @param inputStream Input stream of the CSV file
     * @return Result containing list of parsed teachers or error
     */
    fun parseTeacherCsv(inputStream: InputStream): Result<List<TeacherRow>> {
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
            val teacherIdCol = findColumn(headerMap, listOf("teacher id", "teacherid", "id", "employee id", "employeeid", "teacher_id"))
            val firstNameCol = findColumn(headerMap, listOf("first name", "firstname", "first", "given name", "givenname"))
            val lastNameCol = findColumn(headerMap, listOf("last name", "lastname", "last", "surname", "family name", "familyname"))
            
            if (teacherIdCol == null || firstNameCol == null || lastNameCol == null) {
                csvParser.close()
                val missing = mutableListOf<String>()
                if (teacherIdCol == null) missing.add("Teacher ID")
                if (firstNameCol == null) missing.add("First Name")
                if (lastNameCol == null) missing.add("Last Name")
                return Result.failure(Exception("Missing required columns: ${missing.joinToString(", ")}"))
            }
            
            // Find optional columns
            val middleNameCol = findColumn(headerMap, listOf("middle name", "middlename", "middle", "middle initial", "middleinitial", "mi"))
            val emailCol = findColumn(headerMap, listOf("email", "e-mail", "email address", "emailaddress"))
            val departmentCol = findColumn(headerMap, listOf("department", "department code", "departmentcode", "course", "dept"))
            val employmentTypeCol = findColumn(headerMap, listOf("employment type", "employmenttype", "type", "employment"))
            val positionCol = findColumn(headerMap, listOf("position", "rank", "title", "designation"))
            val specializationCol = findColumn(headerMap, listOf("specialization", "specialty", "field", "expertise"))
            val phoneCol = findColumn(headerMap, listOf("phone number", "phonenumber", "phone", "mobile", "contact number", "contactnumber"))
            val dobCol = findColumn(headerMap, listOf("date of birth", "dateofbirth", "dob", "birthdate", "birth date"))
            val addressCol = findColumn(headerMap, listOf("address", "home address", "homeaddress", "residence"))
            val dateHiredCol = findColumn(headerMap, listOf("date hired", "datehired", "hire date", "hiredate", "employed date"))
            val employeeNumberCol = findColumn(headerMap, listOf("employee number", "employeenumber", "emp number", "empnumber", "employee no"))
            
            val teachers = mutableListOf<TeacherRow>()
            val errors = mutableListOf<String>()
            
            var rowIndex = 1 // Start from 1 (header is 0)
            for (record in csvParser) {
                rowIndex++
                try {
                    val teacherId = getCellValue(record, teacherIdCol)?.trim()
                    val firstName = getCellValue(record, firstNameCol)?.trim()
                    val lastName = getCellValue(record, lastNameCol)?.trim()
                    
                    // Skip empty rows
                    if (teacherId.isNullOrBlank() && firstName.isNullOrBlank() && lastName.isNullOrBlank()) {
                        continue
                    }
                    
                    if (teacherId.isNullOrBlank()) {
                        errors.add("Row $rowIndex: Missing Teacher ID")
                        continue
                    }
                    if (firstName.isNullOrBlank()) {
                        errors.add("Row $rowIndex: Missing First Name for Teacher ID: $teacherId")
                        continue
                    }
                    if (lastName.isNullOrBlank()) {
                        errors.add("Row $rowIndex: Missing Last Name for Teacher ID: $teacherId")
                        continue
                    }
                    
                    val teacher = TeacherRow(
                        teacherId = teacherId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = getCellValue(record, middleNameCol)?.trim()?.takeIf { it.isNotBlank() },
                        email = getCellValue(record, emailCol)?.trim()?.takeIf { it.isNotBlank() },
                        departmentCode = getCellValue(record, departmentCol)?.trim()?.takeIf { it.isNotBlank() },
                        employmentType = getCellValue(record, employmentTypeCol)?.trim()?.takeIf { it.isNotBlank() },
                        position = getCellValue(record, positionCol)?.trim()?.takeIf { it.isNotBlank() },
                        specialization = getCellValue(record, specializationCol)?.trim()?.takeIf { it.isNotBlank() },
                        phoneNumber = getCellValue(record, phoneCol)?.trim()?.takeIf { it.isNotBlank() },
                        dateOfBirth = getCellValue(record, dobCol)?.trim()?.takeIf { it.isNotBlank() },
                        address = getCellValue(record, addressCol)?.trim()?.takeIf { it.isNotBlank() },
                        dateHired = getCellValue(record, dateHiredCol)?.trim()?.takeIf { it.isNotBlank() },
                        employeeNumber = getCellValue(record, employeeNumberCol)?.trim()?.takeIf { it.isNotBlank() }
                    )
                    
                    teachers.add(teacher)
                } catch (e: Exception) {
                    errors.add("Row $rowIndex: Error parsing row - ${e.message}")
                    Log.e(TAG, "Error parsing row $rowIndex", e)
                }
            }
            
            csvParser.close()
            
            if (teachers.isEmpty() && errors.isNotEmpty()) {
                Result.failure(Exception("Failed to parse any teachers. Errors:\n${errors.take(10).joinToString("\n")}"))
            } else {
                Log.d(TAG, "Successfully parsed ${teachers.size} teachers. ${errors.size} errors encountered.")
                Result.success(teachers)
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
 * Data class representing a row from CSV file for teachers
 */
data class TeacherRow(
    val teacherId: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val email: String? = null,
    val departmentCode: String? = null,
    val employmentType: String? = null,
    val position: String? = null,
    val specialization: String? = null,
    val phoneNumber: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val dateHired: String? = null,
    val employeeNumber: String? = null
)


package com.smartacademictracker.data.utils

import android.util.Log
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Utility class for parsing CSV files containing grade data
 * Expected columns (case-insensitive, flexible order):
 * - Student Name / StudentName / Name
 * - Prelim / Preliminary / Prelim Grade
 * - Midterm / Midterm Grade
 * - Final / Final Grade
 */
object GradeCsvParser {
    
    private const val TAG = "GradeCsvParser"
    
    /**
     * Parse CSV file and extract grade data
     * @param inputStream Input stream of the CSV file
     * @return Result containing list of parsed grade rows or error
     */
    fun parseGradeCsv(inputStream: InputStream): Result<List<GradeRow>> {
        return try {
            Log.d(TAG, "Starting grade CSV parsing...")
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
            val studentNameCol = findColumn(headerMap, listOf("student name", "studentname", "name", "student"))
            val prelimCol = findColumn(headerMap, listOf("prelim", "preliminary", "prelim grade", "prelimgrade"))
            val midtermCol = findColumn(headerMap, listOf("midterm", "midterm grade", "midtermgrade"))
            val finalCol = findColumn(headerMap, listOf("final", "final grade", "finalgrade"))
            
            if (studentNameCol == null) {
                csvParser.close()
                return Result.failure(Exception("Missing required column: Student Name"))
            }
            
            val grades = mutableListOf<GradeRow>()
            val errors = mutableListOf<String>()
            
            var rowIndex = 1 // Start from 1 (header is 0)
            for (record in csvParser) {
                rowIndex++
                try {
                    val studentName = getCellValue(record, studentNameCol)?.trim()
                    
                    // Skip empty rows
                    if (studentName.isNullOrBlank()) {
                        continue
                    }
                    
                    // Parse grades (optional - can be empty)
                    val prelimStr = getCellValue(record, prelimCol)?.trim()
                    val midtermStr = getCellValue(record, midtermCol)?.trim()
                    val finalStr = getCellValue(record, finalCol)?.trim()
                    
                    val prelim = prelimStr?.toDoubleOrNull()
                    val midterm = midtermStr?.toDoubleOrNull()
                    val final = finalStr?.toDoubleOrNull()
                    
                    // Validate grade ranges if provided
                    if (prelim != null && (prelim < 0 || prelim > 100)) {
                        errors.add("Row $rowIndex: Prelim grade must be between 0 and 100 for student: $studentName")
                        continue
                    }
                    if (midterm != null && (midterm < 0 || midterm > 100)) {
                        errors.add("Row $rowIndex: Midterm grade must be between 0 and 100 for student: $studentName")
                        continue
                    }
                    if (final != null && (final < 0 || final > 100)) {
                        errors.add("Row $rowIndex: Final grade must be between 0 and 100 for student: $studentName")
                        continue
                    }
                    
                    val gradeRow = GradeRow(
                        studentName = studentName,
                        prelim = prelim,
                        midterm = midterm,
                        final = final
                    )
                    
                    grades.add(gradeRow)
                } catch (e: Exception) {
                    errors.add("Row $rowIndex: Error parsing row - ${e.message}")
                    Log.e(TAG, "Error parsing row $rowIndex", e)
                }
            }
            
            csvParser.close()
            
            if (grades.isEmpty() && errors.isNotEmpty()) {
                Result.failure(Exception("Failed to parse any grades. Errors:\n${errors.take(10).joinToString("\n")}"))
            } else {
                Log.d(TAG, "Successfully parsed ${grades.size} grade rows. ${errors.size} errors encountered.")
                if (errors.isNotEmpty()) {
                    Log.w(TAG, "Parsing errors: ${errors.joinToString("\n")}")
                }
                Result.success(grades)
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
 * Data class representing a row from grade CSV file
 */
data class GradeRow(
    val studentName: String,
    val prelim: Double? = null,
    val midterm: Double? = null,
    val final: Double? = null
)


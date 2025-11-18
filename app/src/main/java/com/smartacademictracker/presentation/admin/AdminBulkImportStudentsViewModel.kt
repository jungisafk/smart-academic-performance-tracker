package com.smartacademictracker.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.PreRegisteredStudent
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.PreRegisteredRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.utils.CsvParser
import com.smartacademictracker.data.utils.ExcelParser
import com.smartacademictracker.data.utils.StudentRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class AdminBulkImportStudentsViewModel @Inject constructor(
    private val preRegisteredRepository: PreRegisteredRepository,
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BulkImportUiState())
    val uiState: StateFlow<BulkImportUiState> = _uiState.asStateFlow()
    
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    private val _yearLevelsCache = mutableMapOf<String, List<YearLevel>>()
    
    init {
        loadCourses()
    }
    
    private fun loadCourses() {
        viewModelScope.launch {
            courseRepository.getAllCourses().onSuccess { courses ->
                _courses.value = courses
            }
        }
    }
    
    fun parseFile(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, parsedStudents = emptyList())
            
            try {
                val parseResult = when {
                    fileName.endsWith(".csv", ignoreCase = true) -> {
                        CsvParser.parseStudentCsv(inputStream)
                    }
                    fileName.endsWith(".xlsx", ignoreCase = true) || fileName.endsWith(".xls", ignoreCase = true) -> {
                        ExcelParser.parseStudentExcel(inputStream, fileName)
                    }
                    else -> {
                        Result.failure(Exception("Unsupported file format. Please use CSV (.csv) or Excel (.xlsx, .xls) files."))
                    }
                }
                
                parseResult.onSuccess { studentRows ->
                    val students = studentRows.mapNotNull { studentRow ->
                        convertCsvRowToStudent(studentRow)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        parsedStudents = students,
                        csvRows = studentRows
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to parse file. Please check the file format."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error reading file: ${e.message ?: "Unknown error"}"
                )
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
        }
    }
    
    private suspend fun convertCsvRowToStudent(csvRow: StudentRow): PreRegisteredStudent? {
        return try {
            // Get current admin user
            val currentUser = userRepository.getCurrentUser().getOrNull()
            val adminId = currentUser?.id ?: ""
            val adminName = currentUser?.firstName + " " + (currentUser?.lastName ?: "")
            
            // Find course by code or name
            var course: Course? = null
            var yearLevel: YearLevel? = null
            
            if (!csvRow.courseCode.isNullOrBlank()) {
                // Try to find course by code
                courseRepository.getCourseByCode(csvRow.courseCode).onSuccess {
                    course = it
                }
                
                // If not found by code, try to find by name (case-insensitive)
                if (course == null) {
                    _courses.value.find { 
                        it.name.equals(csvRow.courseCode, ignoreCase = true) ||
                        it.code.equals(csvRow.courseCode, ignoreCase = true)
                    }?.let { course = it }
                }
            }
            
            // Parse year level
            if (course != null && !csvRow.yearLevel.isNullOrBlank()) {
                val yearLevels = _yearLevelsCache.getOrPut(course!!.id) {
                    yearLevelRepository.getYearLevelsByCourse(course!!.id).getOrNull() ?: emptyList()
                }
                
                // Try to parse as number (1, 2, 3, 4)
                val levelNumber = csvRow.yearLevel.toIntOrNull()
                if (levelNumber != null) {
                    yearLevel = yearLevels.find { it.level == levelNumber }
                } else {
                    // Try to match by name (e.g., "1st Year", "2nd Year")
                    yearLevel = yearLevels.find { 
                        it.name.equals(csvRow.yearLevel, ignoreCase = true) ||
                        it.name.contains(csvRow.yearLevel, ignoreCase = true)
                    }
                }
            }
            
            // Get current enrollment year if not provided
            val enrollmentYear = csvRow.enrollmentYear ?: "2024-2025"
            
            // Validate email is required
            if (csvRow.email.isNullOrBlank()) {
                throw Exception("Email is required for student ${csvRow.studentId} (${csvRow.firstName} ${csvRow.lastName})")
            }
            
            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(csvRow.email.trim()).matches()) {
                throw Exception("Invalid email format for student ${csvRow.studentId}: ${csvRow.email}")
            }
            
            PreRegisteredStudent(
                studentId = csvRow.studentId,
                firstName = csvRow.firstName,
                lastName = csvRow.lastName,
                middleName = csvRow.middleName,
                email = csvRow.email.trim(),
                courseId = course?.id ?: "",
                courseName = course?.name ?: "",
                courseCode = course?.code ?: csvRow.courseCode ?: "",
                yearLevelId = yearLevel?.id ?: "",
                yearLevelName = yearLevel?.name ?: csvRow.yearLevel ?: "",
                section = null,
                enrollmentYear = enrollmentYear,
                phoneNumber = csvRow.phoneNumber,
                dateOfBirth = csvRow.dateOfBirth,
                address = csvRow.address,
                createdBy = adminId,
                createdByName = adminName,
                isRegistered = false
            )
        } catch (e: Exception) {
            // Error converting row, skip it
            null
        }
    }
    
    fun importStudents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null, importResult = null)
            
            try {
                val students = _uiState.value.parsedStudents
                
                if (students.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "No students to import"
                    )
                    return@launch
                }
                
                val result = preRegisteredRepository.bulkAddPreRegisteredStudents(students)
                
                result.onSuccess { importResult ->
                    val successMsg = if (importResult.failureCount == 0) {
                        "Successfully imported ${importResult.successCount} student(s)!"
                    } else {
                        "Imported ${importResult.successCount} student(s). ${importResult.failureCount} failed."
                    }
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importResult = importResult,
                        successMessage = successMsg
                    )
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = exception.message ?: "Failed to import students"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Error during import: ${e.message}"
                )
            }
        }
    }
    
    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BulkImportUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val parsedStudents: List<PreRegisteredStudent> = emptyList(),
    val csvRows: List<StudentRow> = emptyList(),
    val importResult: com.smartacademictracker.data.repository.BulkImportResult? = null
)


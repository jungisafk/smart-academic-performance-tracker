package com.smartacademictracker.presentation.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.EmploymentType
import com.smartacademictracker.data.model.PreRegisteredTeacher
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.PreRegisteredRepository
import com.smartacademictracker.data.repository.UserRepository
import com.smartacademictracker.data.utils.ExcelParser
import com.smartacademictracker.data.utils.TeacherCsvParser
import com.smartacademictracker.data.utils.TeacherRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class AdminBulkImportTeachersViewModel @Inject constructor(
    private val preRegisteredRepository: PreRegisteredRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BulkImportTeachersUiState())
    val uiState: StateFlow<BulkImportTeachersUiState> = _uiState.asStateFlow()
    
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    
    init {
        loadCourses()
    }
    
    private fun loadCourses() {
        viewModelScope.launch {
            courseRepository.getAllCourses().onSuccess { courses ->
                _courses.value = courses
                Log.d("BulkImportTeachersVM", "Loaded ${courses.size} courses")
            }
        }
    }
    
    fun parseFile(inputStream: InputStream, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, parsedTeachers = emptyList())
            
            try {
                val parseResult = when {
                    fileName.endsWith(".csv", ignoreCase = true) -> {
                        Log.d("BulkImportTeachersVM", "Starting CSV file parsing...")
                        TeacherCsvParser.parseTeacherCsv(inputStream)
                    }
                    fileName.endsWith(".xlsx", ignoreCase = true) || fileName.endsWith(".xls", ignoreCase = true) -> {
                        Log.d("BulkImportTeachersVM", "Starting Excel file parsing...")
                        ExcelParser.parseTeacherExcel(inputStream, fileName)
                    }
                    else -> {
                        Result.failure(Exception("Unsupported file format. Please use CSV (.csv) or Excel (.xlsx, .xls) files."))
                    }
                }
                
                parseResult.onSuccess { teacherRows ->
                    Log.d("BulkImportTeachersVM", "File parsing successful: ${teacherRows.size} rows parsed")
                    
                    // Convert CSV rows to PreRegisteredTeacher objects
                    // Note: convertCsvRowToTeacher is a suspend function, so we need to collect results in a loop
                    val teachers = mutableListOf<PreRegisteredTeacher>()
                    for (teacherRow in teacherRows) {
                        val teacher = convertCsvRowToTeacher(teacherRow)
                        if (teacher != null) {
                            teachers.add(teacher)
                        }
                    }
                    
                    Log.d("BulkImportTeachersVM", "Converted ${teachers.size} teachers from ${teacherRows.size} rows")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        parsedTeachers = teachers,
                        csvRows = teacherRows
                    )
                }.onFailure { exception ->
                    Log.e("BulkImportTeachersVM", "File parsing failed", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to parse file. Please check the file format."
                    )
                }
            } catch (e: Exception) {
                Log.e("BulkImportTeachersVM", "Exception during file parsing", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error reading file: ${e.message ?: "Unknown error"}"
                )
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    Log.w("BulkImportTeachersVM", "Error closing input stream", e)
                }
            }
        }
    }
    
    private suspend fun convertCsvRowToTeacher(csvRow: TeacherRow): PreRegisteredTeacher? {
        return try {
            // Get current admin user
            val currentUser = userRepository.getCurrentUser().getOrNull()
            val adminId = currentUser?.id ?: ""
            val adminName = currentUser?.firstName + " " + (currentUser?.lastName ?: "")
            
            // Find department course by code or name
            var departmentCourse: Course? = null
            
            if (!csvRow.departmentCode.isNullOrBlank()) {
                // Try to find course by code
                courseRepository.getCourseByCode(csvRow.departmentCode).onSuccess {
                    departmentCourse = it
                }
                
                // If not found by code, try to find by name (case-insensitive)
                if (departmentCourse == null) {
                    _courses.value.find { 
                        it.name.equals(csvRow.departmentCode, ignoreCase = true) ||
                        it.code.equals(csvRow.departmentCode, ignoreCase = true)
                    }?.let { departmentCourse = it }
                }
            }
            
            // Parse employment type
            val employmentType = when (csvRow.employmentType?.lowercase()) {
                "full-time", "fulltime", "full time", "permanent" -> EmploymentType.FULL_TIME
                "part-time", "parttime", "part time" -> EmploymentType.PART_TIME
                "contract" -> EmploymentType.CONTRACT
                "temporary", "temp" -> EmploymentType.TEMPORARY
                "adjunct" -> EmploymentType.ADJUNCT
                "visiting" -> EmploymentType.VISITING
                else -> EmploymentType.FULL_TIME // Default
            }
            
            PreRegisteredTeacher(
                teacherId = csvRow.teacherId,
                firstName = csvRow.firstName,
                lastName = csvRow.lastName,
                middleName = csvRow.middleName,
                email = csvRow.email,
                departmentCourseId = departmentCourse?.id ?: "",
                departmentCourseName = departmentCourse?.name ?: "",
                departmentCourseCode = departmentCourse?.code ?: csvRow.departmentCode ?: "",
                employmentType = employmentType,
                position = csvRow.position,
                specialization = csvRow.specialization,
                phoneNumber = csvRow.phoneNumber,
                dateOfBirth = csvRow.dateOfBirth,
                address = csvRow.address,
                dateHired = csvRow.dateHired,
                employeeNumber = csvRow.employeeNumber,
                createdBy = adminId,
                createdByName = adminName,
                isRegistered = false
            )
        } catch (e: Exception) {
            Log.e("BulkImportTeachersVM", "Error converting CSV row to teacher: ${e.message}", e)
            null
        }
    }
    
    fun importTeachers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null, importResult = null)
            
            try {
                val teachers = _uiState.value.parsedTeachers
                
                if (teachers.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "No teachers to import"
                    )
                    return@launch
                }
                
                val result = preRegisteredRepository.bulkAddPreRegisteredTeachers(teachers)
                
                result.onSuccess { importResult ->
                    val successMsg = if (importResult.failureCount == 0) {
                        "Successfully imported ${importResult.successCount} teacher(s)!"
                    } else {
                        "Imported ${importResult.successCount} teacher(s). ${importResult.failureCount} failed."
                    }
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importResult = importResult,
                        successMessage = successMsg
                    )
                    Log.d("BulkImportTeachersVM", "Import completed: ${importResult.successCount} success, ${importResult.failureCount} failures")
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = exception.message ?: "Failed to import teachers"
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

data class BulkImportTeachersUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val parsedTeachers: List<PreRegisteredTeacher> = emptyList(),
    val csvRows: List<TeacherRow> = emptyList(),
    val importResult: com.smartacademictracker.data.repository.BulkImportResult? = null
)


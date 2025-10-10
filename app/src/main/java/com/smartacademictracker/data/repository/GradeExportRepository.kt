package com.smartacademictracker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.GradeExport
import com.smartacademictracker.data.model.StudentGradeExport
import com.smartacademictracker.data.model.ExportFormat
import com.smartacademictracker.data.model.ExportType
import com.smartacademictracker.data.utils.GradeCalculationEngine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeExportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: com.smartacademictracker.data.repository.UserRepository,
    private val subjectRepository: com.smartacademictracker.data.repository.SubjectRepository
) {
    private val gradesCollection = firestore.collection("grades")
    private val subjectsCollection = firestore.collection("subjects")
    private val enrollmentsCollection = firestore.collection("enrollments")
    private val exportHistoryCollection = firestore.collection("export_history")

    suspend fun generateSubjectGradeExport(
        subjectId: String,
        academicYear: String,
        semester: String
    ): Result<GradeExport> {
        return try {
            // Get subject information
            val subjectDoc = subjectsCollection.document(subjectId).get().await()
            val subject = subjectDoc.toObject(com.smartacademictracker.data.model.Subject::class.java)
                ?: throw Exception("Subject not found")
            
            // Get enrollments for this subject
            val enrollmentsSnapshot = enrollmentsCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val enrollments = enrollmentsSnapshot.toObjects(com.smartacademictracker.data.model.Enrollment::class.java)
            
            // Get grades for all students in this subject
            val studentGrades = mutableListOf<StudentGradeExport>()
            
            for (enrollment in enrollments) {
                val gradesSnapshot = gradesCollection
                    .whereEqualTo("studentId", enrollment.studentId)
                    .whereEqualTo("subjectId", subjectId)
                    .get()
                    .await()
                val grades = gradesSnapshot.toObjects(com.smartacademictracker.data.model.Grade::class.java)
                
                val prelimGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.PRELIM }?.percentage
                val midtermGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.MIDTERM }?.percentage
                val finalGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.FINAL }?.percentage
                
                val finalAverage = GradeCalculationEngine.calculateFinalAverage(
                    prelimGrade, midtermGrade, finalGrade
                )
                
                val letterGrade = GradeCalculationEngine.calculateLetterGrade(finalAverage ?: 0.0)
                val status = GradeCalculationEngine.determineGradeStatus(finalAverage ?: 0.0).displayName
                
                // Get student number from user data (email) or fallback to studentId
                val studentNumber = getStudentNumber(enrollment.studentId)
                
                studentGrades.add(
                    StudentGradeExport(
                        studentId = enrollment.studentId,
                        studentName = enrollment.studentName,
                        studentNumber = studentNumber,
                        prelimGrade = prelimGrade,
                        midtermGrade = midtermGrade,
                        finalGrade = finalGrade,
                        finalAverage = finalAverage,
                        letterGrade = letterGrade,
                        status = status
                    )
                )
            }
            
            val gradeExport = GradeExport(
                subjectId = subjectId,
                subjectName = subject.name,
                teacherId = subject.teacherId ?: "",
                teacherName = subject.teacherName ?: "",
                academicYear = academicYear,
                semester = semester,
                students = studentGrades.sortedBy { it.studentName }
            )
            
            // Track export in history
            trackExport(
                teacherId = subject.teacherId ?: "",
                exportType = ExportType.SUBJECT_GRADES,
                exportFormat = ExportFormat.EXCEL, // Default format
                subjectId = subjectId,
                subjectName = subject.name,
                academicYear = academicYear,
                semester = semester
            )
            
            Result.success(gradeExport)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateClassSummaryExport(
        teacherId: String,
        academicYear: String,
        semester: String
    ): Result<List<GradeExport>> {
        return try {
            // Get all subjects taught by this teacher
            val subjectsSnapshot = subjectsCollection
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("academicYear", academicYear)
                .whereEqualTo("semester", semester)
                .get()
                .await()
            val subjects = subjectsSnapshot.toObjects(com.smartacademictracker.data.model.Subject::class.java)
            
            val classSummaries = mutableListOf<GradeExport>()
            
            for (subject in subjects) {
                val exportResult = generateSubjectGradeExport(
                    subjectId = subject.id,
                    academicYear = academicYear,
                    semester = semester
                )
                
                if (exportResult.isSuccess) {
                    classSummaries.add(exportResult.getOrThrow())
                }
            }
            
            // Track class summary export
            if (classSummaries.isNotEmpty()) {
                trackExport(
                    teacherId = teacherId,
                    exportType = ExportType.CLASS_SUMMARY,
                    exportFormat = ExportFormat.EXCEL,
                    subjectId = "",
                    subjectName = "Class Summary - ${subjects.size} subjects",
                    academicYear = academicYear,
                    semester = semester
                )
            }
            
            Result.success(classSummaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateIndividualStudentReport(
        studentId: String,
        academicYear: String,
        semester: String
    ): Result<GradeExport> {
        return try {
            // Get all enrollments for this student
            val enrollmentsSnapshot = enrollmentsCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val enrollments = enrollmentsSnapshot.toObjects(com.smartacademictracker.data.model.Enrollment::class.java)
                .filter { it.academicYear == academicYear && it.semester == semester }
            
            val studentGrades = mutableListOf<StudentGradeExport>()
            
            // Get student number from user data (email) or fallback to studentId
            val studentNumber = getStudentNumber(studentId)
            
            // Track first subject/teacher info for report header
            var firstSubjectId: String? = null
            var firstSubjectName: String? = null
            var firstTeacherId: String? = null
            var firstTeacherName: String? = null
            
            // Fetch subject information for the first enrollment to populate report header
            if (enrollments.isNotEmpty() && enrollments.first().subjectId.isNotEmpty()) {
                val firstEnrollment = enrollments.first()
                val subjectResult = subjectRepository.getSubjectById(firstEnrollment.subjectId)
                subjectResult.onSuccess { subject ->
                    firstSubjectId = subject.id
                    firstSubjectName = subject.name
                    firstTeacherId = subject.teacherId ?: ""
                    firstTeacherName = subject.teacherName ?: ""
                    
                    // If teacher name is empty but teacherId exists, fetch from UserRepository
                    if (firstTeacherName.isNullOrEmpty() && firstTeacherId.isNotEmpty()) {
                        val teacherResult = userRepository.getUserById(firstTeacherId)
                        teacherResult.onSuccess { teacher ->
                            firstTeacherName = "${teacher.firstName} ${teacher.lastName}".trim()
                        }
                    }
                }.onFailure {
                    // If subject fetch fails, log but continue with empty values
                    Log.d("GradeExport", "Failed to fetch subject for individual report: ${firstEnrollment.subjectId}")
                }
            }
            
            for (enrollment in enrollments) {
                
                val gradesSnapshot = gradesCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("subjectId", enrollment.subjectId)
                    .get()
                    .await()
                val grades = gradesSnapshot.toObjects(com.smartacademictracker.data.model.Grade::class.java)
                
                val prelimGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.PRELIM }?.percentage
                val midtermGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.MIDTERM }?.percentage
                val finalGrade = grades.find { it.gradePeriod == com.smartacademictracker.data.model.GradePeriod.FINAL }?.percentage
                
                val finalAverage = GradeCalculationEngine.calculateFinalAverage(
                    prelimGrade, midtermGrade, finalGrade
                )
                
                val letterGrade = GradeCalculationEngine.calculateLetterGrade(finalAverage ?: 0.0)
                val status = GradeCalculationEngine.determineGradeStatus(finalAverage ?: 0.0).displayName
                
                studentGrades.add(
                    StudentGradeExport(
                        studentId = studentId,
                        studentName = enrollment.studentName,
                        studentNumber = studentNumber,
                        prelimGrade = prelimGrade,
                        midtermGrade = midtermGrade,
                        finalGrade = finalGrade,
                        finalAverage = finalAverage,
                        letterGrade = letterGrade,
                        status = status
                    )
                )
            }
            
            val gradeExport = GradeExport(
                subjectId = firstSubjectId ?: "",
                subjectName = firstSubjectName ?: "Individual Student Report",
                teacherId = firstTeacherId ?: "",
                teacherName = firstTeacherName ?: "",
                academicYear = academicYear,
                semester = semester,
                students = studentGrades.sortedBy { it.studentName }
            )
            
            // Track individual student report export
            // Note: teacherId is empty for individual reports, so we track with studentId
            trackExport(
                teacherId = studentId, // Using studentId as identifier for individual reports
                exportType = ExportType.INDIVIDUAL_REPORTS,
                exportFormat = ExportFormat.EXCEL,
                subjectId = "",
                subjectName = "Individual Student Report",
                academicYear = academicYear,
                semester = semester
            )
            
            Result.success(gradeExport)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExportHistory(teacherId: String): Result<List<ExportHistoryItem>> {
        return try {
            val snapshot = exportHistoryCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            
            val historyItems = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(ExportHistoryItem::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            // Sort in memory to avoid composite index requirement
            val sortedHistory = historyItems.sortedByDescending { it.exportDate }
            
            Result.success(sortedHistory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get student number from user data.
     * Attempts to fetch user and use email as student number, falls back to studentId if unavailable.
     */
    private suspend fun getStudentNumber(studentId: String): String {
        return try {
            val userResult = userRepository.getUserById(studentId)
            userResult.getOrNull()?.email?.takeIf { it.isNotEmpty() } ?: studentId
        } catch (e: Exception) {
            Log.d("GradeExport", "Failed to fetch user for student number, using studentId: ${e.message}")
            studentId
        }
    }
    
    /**
     * Track an export in the history collection
     */
    private suspend fun trackExport(
        teacherId: String,
        exportType: ExportType,
        exportFormat: ExportFormat,
        subjectId: String,
        subjectName: String,
        academicYear: String,
        semester: String,
        fileSize: Long = 0,
        downloadUrl: String = ""
    ) {
        try {
            val historyItem = ExportHistoryItem(
                teacherId = teacherId,
                exportType = exportType,
                exportFormat = exportFormat,
                subjectId = subjectId,
                subjectName = subjectName,
                academicYear = academicYear,
                semester = semester,
                exportDate = System.currentTimeMillis(),
                fileSize = fileSize,
                downloadUrl = downloadUrl
            )
            exportHistoryCollection.add(historyItem).await()
        } catch (e: Exception) {
            // Log error but don't fail the export operation
            Log.e("GradeExport", "Failed to track export history: ${e.message}")
        }
    }
}

data class ExportHistoryItem(
    val id: String = "",
    val teacherId: String = "",
    val exportType: ExportType = ExportType.SUBJECT_GRADES,
    val exportFormat: ExportFormat = ExportFormat.EXCEL,
    val subjectId: String = "",
    val subjectName: String = "",
    val academicYear: String = "",
    val semester: String = "",
    val exportDate: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val downloadUrl: String = ""
)

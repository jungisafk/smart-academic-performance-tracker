package com.smartacademictracker.data.repository

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
    private val firestore: FirebaseFirestore
) {
    private val gradesCollection = firestore.collection("grades")
    private val subjectsCollection = firestore.collection("subjects")
    private val enrollmentsCollection = firestore.collection("enrollments")

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
                
                studentGrades.add(
                    StudentGradeExport(
                        studentId = enrollment.studentId,
                        studentName = enrollment.studentName,
                        studentNumber = enrollment.studentId, // Using studentId as studentNumber for now
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
                        studentNumber = enrollment.studentId, // Using studentId as studentNumber for now
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
                subjectId = "",
                subjectName = "Individual Student Report",
                teacherId = "",
                teacherName = "",
                academicYear = academicYear,
                semester = semester,
                students = studentGrades.sortedBy { it.studentName }
            )
            
            Result.success(gradeExport)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExportHistory(teacherId: String): Result<List<ExportHistoryItem>> {
        return try {
            // This would typically come from a separate collection tracking export history
            // For now, return empty list
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
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

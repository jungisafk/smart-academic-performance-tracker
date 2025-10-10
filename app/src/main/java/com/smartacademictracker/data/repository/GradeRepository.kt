package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartacademictracker.data.model.AuditAction
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.utils.GradeCalculationEngine
import com.smartacademictracker.data.validation.GradeValidationService
import com.smartacademictracker.data.audit.SecurityAuditLogger
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auditTrailRepository: AuditTrailRepository,
    private val gradeValidationService: GradeValidationService,
    private val securityAuditLogger: SecurityAuditLogger,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) {
    private val gradesCollection = firestore.collection("grades")
    private val gradeAggregatesCollection = firestore.collection("grade_aggregates")

    suspend fun createGrade(grade: Grade): Result<Grade> {
        return createGrade(grade, "system", "SYSTEM")
    }
    
    suspend fun createGrade(grade: Grade, userId: String, userRole: String): Result<Grade> {
        return try {
            // Get active academic period context
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                return Result.failure(Exception("No active academic period found. Please set an active academic period first."))
            }
            
            // Add academic period reference to the grade
            val gradeWithPeriod = grade.copy(academicPeriodId = academicContext.periodId)
            
            // Validate grade before creation
            val validationResult = gradeValidationService.validateGrade(gradeWithPeriod)
            if (!validationResult.isValid) {
                return Result.failure(Exception("Grade validation failed: ${validationResult.errors.joinToString(", ")}"))
            }
            
            val gradeWithCalculatedValues = gradeWithPeriod.copy(
                percentage = gradeWithPeriod.calculatePercentage(),
                letterGrade = gradeWithPeriod.calculateLetterGrade()
            )
            val docRef = gradesCollection.add(gradeWithCalculatedValues).await()
            val createdGrade = gradeWithCalculatedValues.copy(id = docRef.id)
            gradesCollection.document(docRef.id).set(createdGrade).await()
            
            // Create audit trail entry
            auditTrailRepository.createAuditEntry(
                grade = createdGrade,
                action = AuditAction.CREATED,
                newValue = createdGrade.score,
                newLetterGrade = createdGrade.letterGrade
            )
            
            // Log security event
            securityAuditLogger.logGradeModificationEvent(
                eventType = com.smartacademictracker.data.audit.GradeModificationEventType.GRADE_CREATED,
                userId = userId,
                userRole = userRole,
                gradeId = createdGrade.id,
                studentId = createdGrade.studentId,
                subjectId = createdGrade.subjectId,
                newValue = createdGrade.score
            )
            
            // Recompute student's aggregate for this subject after grade creation
            try {
                createOrUpdateStudentGradeAggregate(
                    studentId = createdGrade.studentId,
                    subjectId = createdGrade.subjectId,
                    studentName = createdGrade.studentName,
                    subjectName = createdGrade.subjectName,
                    teacherId = createdGrade.teacherId,
                    semester = createdGrade.semester,
                    academicYear = createdGrade.academicYear
                )
            } catch (_: Exception) { /* no-op */ }

            Result.success(createdGrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGrade(grade: Grade): Result<Unit> {
        return try {
            // Get the old grade for audit trail
            val oldGradeDoc = gradesCollection.document(grade.id).get().await()
            val oldGrade = oldGradeDoc.toObject(Grade::class.java)
            
            val updatedGrade = grade.copy(
                percentage = grade.calculatePercentage(),
                letterGrade = grade.calculateLetterGrade()
            )
            gradesCollection.document(grade.id).set(updatedGrade).await()
            
            // Create audit trail entry
            auditTrailRepository.createAuditEntry(
                grade = updatedGrade,
                action = AuditAction.UPDATED,
                oldValue = oldGrade?.score,
                newValue = updatedGrade.score,
                oldLetterGrade = oldGrade?.letterGrade,
                newLetterGrade = updatedGrade.letterGrade
            )
            
            // Recompute student's aggregate for this subject after grade update
            try {
                createOrUpdateStudentGradeAggregate(
                    studentId = updatedGrade.studentId,
                    subjectId = updatedGrade.subjectId,
                    studentName = updatedGrade.studentName,
                    subjectName = updatedGrade.subjectName,
                    teacherId = updatedGrade.teacherId,
                    semester = updatedGrade.semester,
                    academicYear = updatedGrade.academicYear
                )
            } catch (_: Exception) { /* no-op */ }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGrade(gradeId: String): Result<Unit> {
        return try {
            // Get the grade before deletion for audit trail
            val gradeDoc = gradesCollection.document(gradeId).get().await()
            val grade = gradeDoc.toObject(Grade::class.java)
            
            gradesCollection.document(gradeId).delete().await()
            
            // Create audit trail entry
            if (grade != null) {
                auditTrailRepository.createAuditEntry(
                    grade = grade,
                    action = AuditAction.DELETED,
                    oldValue = grade.score,
                    oldLetterGrade = grade.letterGrade
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradeById(gradeId: String): Result<Grade> {
        return try {
            val document = gradesCollection.document(gradeId).get().await()
            val grade = document.toObject(Grade::class.java)
                ?: throw Exception("Grade not found")
            Result.success(grade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByStudent(studentId: String): Result<List<Grade>> {
        return try {
            // Get active academic period context
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                return Result.success(emptyList())
            }
            
            val snapshot = gradesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("academicPeriodId", academicContext.periodId)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedGrades = grades.sortedByDescending { it.dateRecorded }
            Result.success(sortedGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllGrades(): Result<List<Grade>> {
        return try {
            // Get active academic period context
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                return Result.success(emptyList())
            }
            
            val snapshot = gradesCollection
                .whereEqualTo("academicPeriodId", academicContext.periodId)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedGrades = grades.sortedByDescending { it.dateRecorded }
            Result.success(sortedGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByStudentAndSubject(studentId: String, subjectId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedGrades = grades.sortedByDescending { it.dateRecorded }
            Result.success(sortedGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesBySubject(subjectId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedGrades = grades.sortedByDescending { it.dateRecorded }
            Result.success(sortedGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByTeacher(teacherId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedGrades = grades.sortedByDescending { it.dateRecorded }
            Result.success(sortedGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByPeriod(gradePeriod: GradePeriod): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("gradePeriod", gradePeriod.name)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            // Sort in memory to avoid composite index requirement
            val sortedGrades = grades.sortedByDescending { it.dateRecorded }
            Result.success(sortedGrades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGradesByStudentSubjectAndPeriod(
        studentId: String, 
        subjectId: String, 
        gradePeriod: GradePeriod
    ): Result<Grade?> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("gradePeriod", gradePeriod.name)
                .limit(1)
                .get()
                .await()
            val grade = snapshot.documents.firstOrNull()?.toObject(Grade::class.java)
            Result.success(grade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== STUDENT GRADE AGGREGATE METHODS =====
    
    suspend fun createOrUpdateStudentGradeAggregate(
        studentId: String,
        subjectId: String,
        studentName: String,
        subjectName: String,
        teacherId: String,
        semester: String,
        academicYear: String
    ): Result<StudentGradeAggregate> {
        return try {
            val gradesResult = getGradesByStudentAndSubject(studentId, subjectId)
            val grades = gradesResult.getOrThrow()
            
            val aggregate = GradeCalculationEngine.createStudentGradeAggregate(
                grades = grades,
                studentId = studentId,
                studentName = studentName,
                subjectId = subjectId,
                subjectName = subjectName,
                teacherId = teacherId,
                semester = semester,
                academicYear = academicYear
            )
            
            gradeAggregatesCollection.document(aggregate.id).set(aggregate).await()
            Result.success(aggregate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStudentGradeAggregate(
        studentId: String,
        subjectId: String,
        semester: String,
        academicYear: String
    ): Result<StudentGradeAggregate?> {
        return try {
            val aggregateId = "${studentId}_${subjectId}_${semester}_${academicYear}"
            val document = gradeAggregatesCollection.document(aggregateId).get().await()
            val aggregate = document.toObject(StudentGradeAggregate::class.java)
            Result.success(aggregate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStudentGradeAggregatesByStudent(studentId: String): Result<List<StudentGradeAggregate>> {
        return try {
            // Get active academic period context
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                return Result.success(emptyList())
            }
            
            val snapshot = gradeAggregatesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("academicPeriodId", academicContext.periodId)
                .get()
                .await()
            val aggregates = snapshot.toObjects(StudentGradeAggregate::class.java)
                .sortedByDescending { it.lastUpdated }
            Result.success(aggregates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStudentGradeAggregatesBySubject(subjectId: String): Result<List<StudentGradeAggregate>> {
        return try {
            val snapshot = gradeAggregatesCollection
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            val aggregates = snapshot.toObjects(StudentGradeAggregate::class.java)
                .sortedByDescending { it.lastUpdated }
            Result.success(aggregates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStudentGradeAggregatesByTeacher(teacherId: String): Result<List<StudentGradeAggregate>> {
        return try {
            val snapshot = gradeAggregatesCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            val aggregates = snapshot.toObjects(StudentGradeAggregate::class.java)
                .sortedByDescending { it.lastUpdated }
            Result.success(aggregates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== CALCULATION METHODS =====
    
    suspend fun calculateStudentFinalAverageForSubject(
        studentId: String, 
        subjectId: String
    ): Result<Double?> {
        return try {
            val prelimResult = getGradesByStudentSubjectAndPeriod(studentId, subjectId, GradePeriod.PRELIM)
            val midtermResult = getGradesByStudentSubjectAndPeriod(studentId, subjectId, GradePeriod.MIDTERM)
            val finalResult = getGradesByStudentSubjectAndPeriod(studentId, subjectId, GradePeriod.FINAL)
            
            val prelimGrade = prelimResult.getOrNull()?.score
            val midtermGrade = midtermResult.getOrNull()?.score
            val finalGrade = finalResult.getOrNull()?.score
            
            val finalAverage = GradeCalculationEngine.calculateFinalAverage(
                prelimGrade, midtermGrade, finalGrade
            )
            
            Result.success(finalAverage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun calculateClassAverageForSubject(subjectId: String): Result<Map<GradePeriod, Double?>> {
        return try {
            val gradesResult = getGradesBySubject(subjectId)
            val grades = gradesResult.getOrThrow()
            
            val averages = GradePeriod.values().associateWith { period ->
                val periodGrades = grades.filter { it.gradePeriod == period }
                GradeCalculationEngine.calculateClassAverage(periodGrades)
            }
            
            Result.success(averages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

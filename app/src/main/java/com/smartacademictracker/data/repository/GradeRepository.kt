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
import com.smartacademictracker.data.notification.NotificationSenderService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auditTrailRepository: AuditTrailRepository,
    private val gradeValidationService: GradeValidationService,
    private val securityAuditLogger: SecurityAuditLogger,
    private val academicPeriodFilterService: AcademicPeriodFilterService,
    private val notificationSenderService: NotificationSenderService
) {
    private val gradesCollection = firestore.collection("grades")
    private val gradeAggregatesCollection = firestore.collection("grade_aggregates")

    suspend fun createGrade(grade: Grade): Result<Grade> {
        return createGrade(grade, "system", "SYSTEM")
    }
    
    suspend fun createGrade(grade: Grade, userId: String, userRole: String, skipNotification: Boolean = false): Result<Grade> {
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
            // Lock the grade when it's saved (unless it's being created by admin)
            val gradeToSave = if (userRole == "ADMIN") {
                gradeWithCalculatedValues
            } else {
                gradeWithCalculatedValues.copy(
                    isLocked = true,
                    lockedAt = System.currentTimeMillis(),
                    lockedBy = userId
                )
            }
            
            // Debug: Log lock status before saving
            println("DEBUG: GradeRepository.createGrade - Creating grade with isLocked: ${gradeToSave.isLocked}, lockedBy: ${gradeToSave.lockedBy}, userRole: $userRole")
            
            // Use explicit field mapping to ensure @PropertyName annotations are respected
            val gradeMap = hashMapOf<String, Any?>(
                "studentId" to gradeToSave.studentId,
                "studentName" to gradeToSave.studentName,
                "subjectId" to gradeToSave.subjectId,
                "subjectName" to gradeToSave.subjectName,
                "teacherId" to gradeToSave.teacherId,
                "gradePeriod" to gradeToSave.gradePeriod.name,
                "score" to gradeToSave.score,
                "maxScore" to gradeToSave.maxScore,
                "percentage" to gradeToSave.percentage,
                "letterGrade" to gradeToSave.letterGrade,
                "description" to gradeToSave.description,
                "dateRecorded" to gradeToSave.dateRecorded,
                "semester" to gradeToSave.semester,
                "academicYear" to gradeToSave.academicYear,
                "academicPeriodId" to gradeToSave.academicPeriodId,
                "locked" to gradeToSave.isLocked, // Use "locked" not "isLocked" for Firestore
                "editRequested" to gradeToSave.editRequested,
                "lockedAt" to gradeToSave.lockedAt,
                "lockedBy" to gradeToSave.lockedBy,
                "unlockedBy" to gradeToSave.unlockedBy,
                "unlockedAt" to gradeToSave.unlockedAt
            )
            
            val docRef = gradesCollection.add(gradeMap).await()
            val createdGrade = gradeToSave.copy(id = docRef.id)
            
            // Verify the saved document
            val savedDoc = gradesCollection.document(docRef.id).get().await()
            val savedData = savedDoc.data
            println("DEBUG: GradeRepository.createGrade - Saved document data: locked=${savedData?.get("locked")}, editRequested=${savedData?.get("editRequested")}, lockedBy=${savedData?.get("lockedBy")}")
            
            // Re-read to verify mapping
            val verifiedGrade = savedDoc.toObject(Grade::class.java)
            println("DEBUG: GradeRepository.createGrade - Verified grade: isLocked=${verifiedGrade?.isLocked}, editRequested=${verifiedGrade?.editRequested}, lockedBy=${verifiedGrade?.lockedBy}")
            
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
            
            // Notify student about the new grade (skip for CSV imports - handled in ViewModel)
            if (!skipNotification) {
                notificationSenderService.sendGradeUpdateNotification(
                    userId = createdGrade.studentId,
                    subjectName = createdGrade.subjectName,
                    grade = createdGrade.letterGrade,
                    period = createdGrade.gradePeriod.displayName,
                    score = createdGrade.score,
                    maxScore = createdGrade.maxScore
                )
            }

            Result.success(createdGrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGrade(grade: Grade): Result<Unit> {
        val result = updateGrade(grade, "system", "SYSTEM")
        return result.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }
    
    suspend fun updateGrade(grade: Grade, userId: String, userRole: String, skipNotification: Boolean = false): Result<Grade> {
        return try {
            // Get the old grade for audit trail and lock check
            val oldGradeDoc = gradesCollection.document(grade.id).get().await()
            val oldGrade = oldGradeDoc.toObject(Grade::class.java)
                ?: return Result.failure(Exception("Grade not found"))
            
            // Check if grade is locked and user has permission to edit
            // Debug: Log lock status
            println("DEBUG: GradeRepository.updateGrade - Grade ID: ${grade.id}")
            println("DEBUG: GradeRepository.updateGrade - isLocked: ${oldGrade.isLocked}, unlockedBy: ${oldGrade.unlockedBy}, userRole: $userRole")
            
            if (oldGrade.isLocked && userRole != "ADMIN") {
                // Only allow edit if edit was requested and approved (unlocked)
                if (oldGrade.unlockedBy == null) {
                    println("DEBUG: GradeRepository.updateGrade - BLOCKING EDIT: Grade is locked and not unlocked")
                    return Result.failure(Exception("Grade is locked. Please request admin permission to edit."))
                } else {
                    println("DEBUG: GradeRepository.updateGrade - ALLOWING EDIT: Grade was unlocked by admin")
                }
            } else if (!oldGrade.isLocked) {
                println("DEBUG: GradeRepository.updateGrade - ALLOWING EDIT: Grade is not locked")
            } else {
                println("DEBUG: GradeRepository.updateGrade - ALLOWING EDIT: User is ADMIN")
            }
            
            // Preserve lock fields from old grade, but re-lock if it was unlocked
            val updatedGrade = grade.copy(
                percentage = grade.calculatePercentage(),
                letterGrade = grade.calculateLetterGrade(),
                // Preserve lock fields from old grade
                isLocked = if (oldGrade.unlockedBy != null) {
                    // Grade was unlocked, re-lock it after update
                    true
                } else {
                    // Preserve existing lock status
                    oldGrade.isLocked
                },
                editRequested = false, // Clear edit request after update
                lockedAt = if (oldGrade.unlockedBy != null) {
                    // Grade was unlocked, set new lock timestamp
                    System.currentTimeMillis()
                } else {
                    // Preserve existing lock timestamp
                    oldGrade.lockedAt
                },
                lockedBy = if (oldGrade.unlockedBy != null) {
                    // Grade was unlocked, set new locker
                    userId
                } else {
                    // Preserve existing locker
                    oldGrade.lockedBy
                },
                unlockedBy = null, // Clear unlock info after update
                unlockedAt = null // Clear unlock timestamp after update
            )
            
            // Use explicit field mapping to ensure @PropertyName annotations are respected
            val updateMap = hashMapOf<String, Any?>(
                "studentId" to updatedGrade.studentId,
                "studentName" to updatedGrade.studentName,
                "subjectId" to updatedGrade.subjectId,
                "subjectName" to updatedGrade.subjectName,
                "teacherId" to updatedGrade.teacherId,
                "gradePeriod" to updatedGrade.gradePeriod.name,
                "score" to updatedGrade.score,
                "maxScore" to updatedGrade.maxScore,
                "percentage" to updatedGrade.percentage,
                "letterGrade" to updatedGrade.letterGrade,
                "description" to updatedGrade.description,
                "dateRecorded" to updatedGrade.dateRecorded,
                "semester" to updatedGrade.semester,
                "academicYear" to updatedGrade.academicYear,
                "academicPeriodId" to updatedGrade.academicPeriodId,
                "locked" to updatedGrade.isLocked, // Use "locked" not "isLocked" for Firestore
                "editRequested" to updatedGrade.editRequested,
                "lockedAt" to updatedGrade.lockedAt,
                "lockedBy" to updatedGrade.lockedBy,
                "unlockedBy" to updatedGrade.unlockedBy,
                "unlockedAt" to updatedGrade.unlockedAt
            )
            
            println("DEBUG: GradeRepository.updateGrade - Updating with locked=${updateMap["locked"]}, editRequested=${updateMap["editRequested"]}, lockedBy=${updateMap["lockedBy"]}")
            gradesCollection.document(grade.id).set(updateMap).await()
            
            // Verify the update
            val updatedDoc = gradesCollection.document(grade.id).get().await()
            val updatedData = updatedDoc.data
            println("DEBUG: GradeRepository.updateGrade - Updated document: locked=${updatedData?.get("locked")}, editRequested=${updatedData?.get("editRequested")}")
            
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
            
            // Notify student about the grade update (only if grade actually changed and not skipping)
            if (!skipNotification && (oldGrade?.score != updatedGrade.score || oldGrade?.gradePeriod != updatedGrade.gradePeriod)) {
                notificationSenderService.sendGradeUpdateNotification(
                    userId = updatedGrade.studentId,
                    subjectName = updatedGrade.subjectName,
                    grade = updatedGrade.letterGrade,
                    period = updatedGrade.gradePeriod.displayName,
                    score = updatedGrade.score,
                    maxScore = updatedGrade.maxScore
                )
            }

            Result.success(updatedGrade)
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
    
    /**
     * Get real-time flow of grades for a specific subject
     */
    fun getGradesBySubjectFlow(subjectId: String): Flow<List<Grade>> = callbackFlow {
        println("DEBUG: GradeRepository.getGradesBySubjectFlow - Setting up real-time listener for subject: $subjectId")
        
        val listenerRegistration = gradesCollection
            .whereEqualTo("subjectId", subjectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("DEBUG: GradeRepository.getGradesBySubjectFlow - Error in listener: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val grades = snapshot.toObjects(Grade::class.java)
                    // Sort in memory to avoid composite index requirement
                    val sortedGrades = grades.sortedByDescending { it.dateRecorded }
                    println("DEBUG: GradeRepository.getGradesBySubjectFlow - Real-time update: Found ${sortedGrades.size} grades for subject $subjectId")
                    trySend(sortedGrades)
                }
            }
        
        awaitClose {
            println("DEBUG: GradeRepository.getGradesBySubjectFlow - Removing real-time listener for subject: $subjectId")
            listenerRegistration.remove()
        }
    }
    
    /**
     * Get real-time flow of grades for a specific student
     */
    fun getGradesByStudentFlow(studentId: String): Flow<List<Grade>> = callbackFlow {
        try {
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            val listener = gradesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("academicPeriodId", academicContext.periodId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val grades = snapshot.toObjects(Grade::class.java)
                        val sortedGrades = grades.sortedByDescending { it.dateRecorded }
                        trySend(sortedGrades)
                    }
                }
            
            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            close(e)
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
    
    /**
     * Request permission to edit a locked grade
     * Sets editRequested flag to true
     */
    suspend fun requestGradeEdit(gradeId: String): Result<Unit> {
        return try {
            println("DEBUG: GradeRepository.requestGradeEdit - Requesting edit for grade ID: $gradeId")
            val gradeDoc = gradesCollection.document(gradeId).get().await()
            
            // Check raw Firestore data first
            val rawData = gradeDoc.data
            println("DEBUG: GradeRepository.requestGradeEdit - Raw Firestore data: locked=${rawData?.get("locked")}, editRequested=${rawData?.get("editRequested")}, lockedBy=${rawData?.get("lockedBy")}, unlockedBy=${rawData?.get("unlockedBy")}")
            
            val grade = gradeDoc.toObject(Grade::class.java)
                ?: return Result.failure(Exception("Grade not found"))
            
            println("DEBUG: GradeRepository.requestGradeEdit - Parsed grade: isLocked=${grade.isLocked}, editRequested=${grade.editRequested}, lockedBy=${grade.lockedBy}, unlockedBy=${grade.unlockedBy}")
            
            // A grade is considered locked if:
            // 1. It has locked=true in Firestore, OR
            // 2. It exists (has an ID) and hasn't been unlocked by admin (unlockedBy is null)
            // This handles both old grades (without locked field) and new grades (with locked field)
            val rawLocked = rawData?.get("locked") as? Boolean
            val rawUnlockedBy = rawData?.get("unlockedBy")
            val isActuallyLocked = when {
                // If explicitly unlocked by admin, it's not locked
                rawUnlockedBy != null -> false
                // If locked field exists and is true, it's locked
                rawLocked == true -> true
                // If locked field exists and is false, check if it was ever saved (has lockedBy or dateRecorded)
                rawLocked == false -> {
                    // If it has a lockedBy or was saved before locking was implemented, treat as locked
                    rawData?.get("lockedBy") != null || rawData?.get("dateRecorded") != null
                }
                // If locked field doesn't exist (old grade), treat as locked if it has a dateRecorded (was saved)
                else -> rawData?.get("dateRecorded") != null
            }
            
            println("DEBUG: GradeRepository.requestGradeEdit - isActuallyLocked: $isActuallyLocked (rawLocked=$rawLocked, rawUnlockedBy=$rawUnlockedBy)")
            
            if (!isActuallyLocked) {
                println("DEBUG: GradeRepository.requestGradeEdit - Grade is not locked, cannot request edit")
                return Result.failure(Exception("Grade is not locked. Only locked grades can have edit requests."))
            }
            
            if (grade.editRequested) {
                println("DEBUG: GradeRepository.requestGradeEdit - Edit permission already requested")
                return Result.failure(Exception("Edit permission already requested"))
            }
            
            println("DEBUG: GradeRepository.requestGradeEdit - Updating editRequested field to true")
            gradesCollection.document(gradeId).update(
                mapOf("editRequested" to true)
            ).await()
            
            // Also update locked field to true if it's not already set (for old grades)
            if (rawLocked != true) {
                println("DEBUG: GradeRepository.requestGradeEdit - Updating locked field to true for old grade")
                val currentUserId = rawData?.get("lockedBy") as? String ?: rawData?.get("teacherId") as? String
                val currentLockedAt = rawData?.get("lockedAt") as? Long ?: System.currentTimeMillis()
                gradesCollection.document(gradeId).update(
                    mapOf(
                        "locked" to true,
                        "lockedBy" to (currentUserId ?: "system"),
                        "lockedAt" to currentLockedAt
                    )
                ).await()
            }
            
            // Verify the update
            val updatedDoc = gradesCollection.document(gradeId).get().await()
            val updatedData = updatedDoc.data
            println("DEBUG: GradeRepository.requestGradeEdit - Updated document: locked=${updatedData?.get("locked")}, editRequested=${updatedData?.get("editRequested")}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("DEBUG: GradeRepository.requestGradeEdit - Error: ${e.message}")
            println("DEBUG: GradeRepository.requestGradeEdit - Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Unlock a grade for editing (Admin only)
     * This allows the teacher to edit the grade once
     */
    suspend fun unlockGradeForEdit(gradeId: String, adminId: String): Result<Unit> {
        return try {
            val gradeDoc = gradesCollection.document(gradeId).get().await()
            val grade = gradeDoc.toObject(Grade::class.java)
                ?: return Result.failure(Exception("Grade not found"))
            
            if (!grade.isLocked) {
                return Result.failure(Exception("Grade is not locked"))
            }
            
            gradesCollection.document(gradeId).update(
                mapOf(
                    "locked" to false, // Use "locked" not "isLocked" because of @PropertyName annotation
                    "editRequested" to false,
                    "unlockedBy" to adminId,
                    "unlockedAt" to System.currentTimeMillis()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reject/decline a grade edit request (Admin only)
     * This clears the editRequested flag without unlocking the grade
     */
    suspend fun rejectGradeEditRequest(gradeId: String, adminId: String): Result<Unit> {
        return try {
            val gradeDoc = gradesCollection.document(gradeId).get().await()
            val grade = gradeDoc.toObject(Grade::class.java)
                ?: return Result.failure(Exception("Grade not found"))
            
            if (!grade.editRequested) {
                return Result.failure(Exception("No edit request found for this grade"))
            }
            
            // Clear the editRequested flag without unlocking the grade
            gradesCollection.document(gradeId).update(
                mapOf(
                    "editRequested" to false
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all grades with edit requests (for admin)
     */
    suspend fun getGradesWithEditRequests(): Result<List<Grade>> {
        return try {
            println("DEBUG: GradeRepository.getGradesWithEditRequests - Querying for grades with editRequested=true")
            try {
                // Query by editRequested first (this should work without composite index)
                val snapshot = gradesCollection
                    .whereEqualTo("editRequested", true)
                    .get()
                    .await()
                
                val allGradesWithRequests = snapshot.toObjects(Grade::class.java)
                println("DEBUG: GradeRepository.getGradesWithEditRequests - Found ${allGradesWithRequests.size} grades with editRequested=true")
                
                // Get raw data to check locked status for old grades
                val gradesWithRawData = snapshot.documents.mapNotNull { doc ->
                    val grade = doc.toObject(Grade::class.java) ?: return@mapNotNull null
                    val rawData = doc.data
                    val rawLocked = rawData?.get("locked") as? Boolean
                    val rawUnlockedBy = rawData?.get("unlockedBy")
                    
                    // A grade is considered locked if:
                    // 1. It has locked=true in Firestore, OR
                    // 2. It exists and hasn't been unlocked by admin (unlockedBy is null)
                    // This handles both old grades (without locked field) and new grades (with locked field)
                    val isActuallyLocked = when {
                        // If explicitly unlocked by admin, it's not locked
                        rawUnlockedBy != null -> false
                        // If locked field exists and is true, it's locked
                        rawLocked == true -> true
                        // If locked field exists and is false, check if it was ever saved (has lockedBy or dateRecorded)
                        rawLocked == false -> {
                            rawData?.get("lockedBy") != null || rawData?.get("dateRecorded") != null
                        }
                        // If locked field doesn't exist (old grade), treat as locked if it has a dateRecorded (was saved)
                        else -> rawData?.get("dateRecorded") != null
                    }
                    
                    println("DEBUG: GradeRepository.getGradesWithEditRequests - Grade ID: ${grade.id}, Student: ${grade.studentName}, Subject: ${grade.subjectName}, editRequested: ${grade.editRequested}, rawLocked: $rawLocked, isActuallyLocked: $isActuallyLocked")
                    
                    if (isActuallyLocked) grade else null
                }
                
                println("DEBUG: GradeRepository.getGradesWithEditRequests - Found ${gradesWithRawData.size} locked grades with edit requests (filtered from ${allGradesWithRequests.size} total)")
                gradesWithRawData.forEach { grade ->
                    println("DEBUG: GradeRepository.getGradesWithEditRequests - Grade ID: ${grade.id}, Student: ${grade.studentName}, Subject: ${grade.subjectName}, editRequested: ${grade.editRequested}, isLocked: ${grade.isLocked}")
                }
                Result.success(gradesWithRawData)
            } catch (queryError: Exception) {
                println("DEBUG: GradeRepository.getGradesWithEditRequests - Query error: ${queryError.message}")
                // Fallback: try to get all grades and filter in memory (not recommended for large datasets)
                println("DEBUG: GradeRepository.getGradesWithEditRequests - Attempting fallback: getting all grades")
                val allGradesSnapshot = gradesCollection
                    .limit(1000) // Limit to prevent memory issues
                    .get()
                    .await()
                
                val allGrades = allGradesSnapshot.toObjects(Grade::class.java)
                val filteredGrades = allGrades.filter { grade ->
                    grade.editRequested && (grade.isLocked || grade.unlockedBy == null)
                }
                println("DEBUG: GradeRepository.getGradesWithEditRequests - Fallback found ${filteredGrades.size} grades with edit requests")
                Result.success(filteredGrades)
            }
        } catch (e: Exception) {
            println("DEBUG: GradeRepository.getGradesWithEditRequests - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Get real-time flow of grades with edit requests (for admin)
     */
    fun getGradesWithEditRequestsFlow(): Flow<List<Grade>> = callbackFlow {
        println("DEBUG: GradeRepository.getGradesWithEditRequestsFlow - Setting up real-time listener for grade edit requests")
        
        val listenerRegistration = gradesCollection
            .whereEqualTo("editRequested", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("DEBUG: GradeRepository.getGradesWithEditRequestsFlow - Error in listener: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val allGradesWithRequests = snapshot.toObjects(Grade::class.java)
                    println("DEBUG: GradeRepository.getGradesWithEditRequestsFlow - Real-time update: Found ${allGradesWithRequests.size} grades with editRequested=true")
                    
                    // Get raw data to check locked status for old grades
                    val gradesWithRawData = snapshot.documents.mapNotNull { doc ->
                        val grade = doc.toObject(Grade::class.java) ?: return@mapNotNull null
                        val rawData = doc.data
                        val rawLocked = rawData?.get("locked") as? Boolean
                        val rawUnlockedBy = rawData?.get("unlockedBy")
                        
                        // A grade is considered locked if:
                        // 1. It has locked=true in Firestore, OR
                        // 2. It exists and hasn't been unlocked by admin (unlockedBy is null)
                        val isActuallyLocked = when {
                            rawUnlockedBy != null -> false
                            rawLocked == true -> true
                            rawLocked == false -> {
                                rawData?.get("lockedBy") != null || rawData?.get("dateRecorded") != null
                            }
                            else -> rawData?.get("dateRecorded") != null
                        }
                        
                        if (isActuallyLocked) grade else null
                    }
                    
                    println("DEBUG: GradeRepository.getGradesWithEditRequestsFlow - Found ${gradesWithRawData.size} locked grades with edit requests")
                    trySend(gradesWithRawData)
                } else if (snapshot != null && snapshot.isEmpty) {
                    println("DEBUG: GradeRepository.getGradesWithEditRequestsFlow - No grades with edit requests found")
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            println("DEBUG: GradeRepository.getGradesWithEditRequestsFlow - Removing real-time listener")
            listenerRegistration.remove()
        }
    }
    
    /**
     * Re-lock a grade after editing (called after successful update)
     */
    suspend fun relockGrade(gradeId: String, userId: String): Result<Unit> {
        return try {
            gradesCollection.document(gradeId).update(
                mapOf(
                    "locked" to true, // Use "locked" not "isLocked" because of @PropertyName annotation
                    "lockedAt" to System.currentTimeMillis(),
                    "lockedBy" to userId,
                    "editRequested" to false,
                    "unlockedBy" to null,
                    "unlockedAt" to null
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all grades that have been unlocked (edit request history)
     * These are grades that were previously requested and approved
     */
    suspend fun getUnlockedGradesHistory(limit: Int = 50): Result<List<Grade>> {
        return try {
            // Query for grades with unlockedAt field (which is set when unlocked)
            // We'll filter in memory to ensure we only get unlocked grades
            val snapshot = gradesCollection
                .whereGreaterThan("unlockedAt", 0L)
                .orderBy("unlockedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val grades = snapshot.toObjects(Grade::class.java)
                .filter { it.unlockedBy != null && it.unlockedAt != null }
            Result.success(grades)
        } catch (e: Exception) {
            // If the query fails (e.g., no index), try alternative approach
            try {
                // Fallback: Get all grades and filter in memory (less efficient but works)
                val allSnapshot = gradesCollection
                    .limit(1000) // Get a reasonable batch
                    .get()
                    .await()
                
                val allGrades = allSnapshot.toObjects(Grade::class.java)
                val unlockedGrades = allGrades
                    .filter { it.unlockedBy != null && it.unlockedAt != null }
                    .sortedByDescending { it.unlockedAt }
                    .take(limit)
                
                Result.success(unlockedGrades)
            } catch (fallbackError: Exception) {
                Result.failure(e) // Return original error
            }
        }
    }
}

package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.GradeCurve
import com.smartacademictracker.data.model.CurveType
import com.smartacademictracker.data.model.CurveApplication
import com.smartacademictracker.data.model.CurveStatistics
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class GradeCurveRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val curvesCollection = firestore.collection("grade_curves")
    private val gradesCollection = firestore.collection("grades")

    suspend fun createCurve(curve: GradeCurve): Result<GradeCurve> {
        return try {
            val docRef = curvesCollection.add(curve).await()
            val createdCurve = curve.copy(id = docRef.id)
            curvesCollection.document(docRef.id).set(createdCurve).await()
            Result.success(createdCurve)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurvesBySubject(subjectId: String): Result<List<GradeCurve>> {
        return try {
            val snapshot = curvesCollection
                .whereEqualTo("subjectId", subjectId)
                .orderBy("appliedDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val curves = snapshot.toObjects(GradeCurve::class.java)
            Result.success(curves)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyCurveToGrades(
        subjectId: String,
        gradePeriod: GradePeriod,
        curve: GradeCurve
    ): Result<List<CurveApplication>> {
        return try {
            // Get all grades for the subject and period
            val snapshot = gradesCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("gradePeriod", gradePeriod.name)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            
            val curveApplications = mutableListOf<CurveApplication>()
            
            for (grade in grades) {
                val originalGrade = grade.percentage
                val curvedGrade = applyCurveToGrade(originalGrade, curve)
                val adjustment = curvedGrade - originalGrade
                
                curveApplications.add(
                    CurveApplication(
                        originalGrade = originalGrade,
                        curvedGrade = curvedGrade,
                        adjustment = adjustment,
                        studentId = grade.studentId,
                        studentName = grade.studentName
                    )
                )
            }
            
            Result.success(curveApplications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateCurveStatistics(
        subjectId: String,
        gradePeriod: GradePeriod
    ): Result<CurveStatistics> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("gradePeriod", gradePeriod.name)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            
            if (grades.isEmpty()) {
                return Result.failure(Exception("No grades found"))
            }
            
            val gradeValues = grades.map { it.percentage }
            val average = gradeValues.average()
            val standardDeviation = calculateStandardDeviation(gradeValues)
            
            val gradeDistribution = calculateGradeDistribution(gradeValues)
            val passingRate = (gradeValues.count { it >= 70.0 }.toDouble() / gradeValues.size) * 100
            
            val statistics = CurveStatistics(
                originalAverage = average,
                curvedAverage = average, // Will be updated after curve application
                originalStandardDeviation = standardDeviation,
                curvedStandardDeviation = standardDeviation, // Will be updated after curve application
                gradeDistribution = gradeDistribution,
                totalStudents = grades.size,
                passingRate = passingRate
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun previewCurve(
        subjectId: String,
        gradePeriod: GradePeriod,
        curve: GradeCurve
    ): Result<Pair<List<CurveApplication>, CurveStatistics>> {
        return try {
            val curveApplicationsResult = applyCurveToGrades(subjectId, gradePeriod, curve)
            val statisticsResult = calculateCurveStatistics(subjectId, gradePeriod)
            
            if (curveApplicationsResult.isSuccess && statisticsResult.isSuccess) {
                val curveApplications = curveApplicationsResult.getOrThrow()
                val originalStatistics = statisticsResult.getOrThrow()
                
                // Calculate curved statistics
                val curvedGrades = curveApplications.map { it.curvedGrade }
                val curvedAverage = curvedGrades.average()
                val curvedStandardDeviation = calculateStandardDeviation(curvedGrades)
                val curvedGradeDistribution = calculateGradeDistribution(curvedGrades)
                val curvedPassingRate = (curvedGrades.count { it >= 70.0 }.toDouble() / curvedGrades.size) * 100
                
                val curvedStatistics = originalStatistics.copy(
                    curvedAverage = curvedAverage,
                    curvedStandardDeviation = curvedStandardDeviation,
                    gradeDistribution = curvedGradeDistribution,
                    passingRate = curvedPassingRate
                )
                
                Result.success(Pair(curveApplications, curvedStatistics))
            } else {
                Result.failure(Exception("Failed to preview curve"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun applyCurveToGrade(originalGrade: Double, curve: GradeCurve): Double {
        return when (curve.curveType) {
            CurveType.LINEAR -> {
                val curved = originalGrade + curve.adjustmentFactor
                curved.coerceIn(curve.minGrade, curve.maxGrade)
            }
            CurveType.PERCENTAGE -> {
                val curved = originalGrade * (1 + curve.adjustmentFactor / 100)
                curved.coerceIn(curve.minGrade, curve.maxGrade)
            }
            CurveType.SQUARE_ROOT -> {
                val curved = sqrt(originalGrade * 10)
                curved.coerceIn(curve.minGrade, curve.maxGrade)
            }
            CurveType.BELL_CURVE -> {
                // Bell curve implementation - adjust to normal distribution
                val curved = originalGrade + curve.adjustmentFactor
                curved.coerceIn(curve.minGrade, curve.maxGrade)
            }
            CurveType.TARGET_AVERAGE -> {
                val adjustment = curve.targetAverage - originalGrade
                val curved = originalGrade + (adjustment * curve.adjustmentFactor)
                curved.coerceIn(curve.minGrade, curve.maxGrade)
            }
        }
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    private fun calculateGradeDistribution(grades: List<Double>): Map<String, Int> {
        return mapOf(
            "A (90-100)" to grades.count { it >= 90 },
            "B (80-89)" to grades.count { it in 80.0..89.9 },
            "C (70-79)" to grades.count { it in 70.0..79.9 },
            "D (60-69)" to grades.count { it in 60.0..69.9 },
            "F (0-59)" to grades.count { it < 60 }
        )
    }

    suspend fun saveCurveApplication(
        subjectId: String,
        gradePeriod: GradePeriod,
        curveApplications: List<CurveApplication>
    ): Result<Unit> {
        return try {
            // Update grades with curved values
            for (application in curveApplications) {
                val gradeQuery = gradesCollection
                    .whereEqualTo("subjectId", subjectId)
                    .whereEqualTo("studentId", application.studentId)
                    .whereEqualTo("gradePeriod", gradePeriod.name)
                    .limit(1)
                
                val snapshot = gradeQuery.get().await()
                if (!snapshot.isEmpty) {
                    val gradeDoc = snapshot.documents.first()
                    gradeDoc.reference.update(
                        mapOf(
                            "percentage" to application.curvedGrade,
                            "curvedGrade" to application.curvedGrade,
                            "originalGrade" to application.originalGrade,
                            "curveAdjustment" to application.adjustment
                        )
                    ).await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

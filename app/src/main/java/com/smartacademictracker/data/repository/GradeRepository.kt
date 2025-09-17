package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradeType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val gradesCollection = firestore.collection("grades")

    suspend fun createGrade(grade: Grade): Result<Grade> {
        return try {
            val gradeWithCalculatedValues = grade.copy(
                percentage = grade.calculatePercentage(),
                letterGrade = grade.calculateLetterGrade()
            )
            val docRef = gradesCollection.add(gradeWithCalculatedValues).await()
            val createdGrade = gradeWithCalculatedValues.copy(id = docRef.id)
            gradesCollection.document(docRef.id).set(createdGrade).await()
            Result.success(createdGrade)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGrade(grade: Grade): Result<Unit> {
        return try {
            val updatedGrade = grade.copy(
                percentage = grade.calculatePercentage(),
                letterGrade = grade.calculateLetterGrade()
            )
            gradesCollection.document(grade.id).set(updatedGrade).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGrade(gradeId: String): Result<Unit> {
        return try {
            gradesCollection.document(gradeId).delete().await()
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
            val snapshot = gradesCollection
                .whereEqualTo("studentId", studentId)
                .orderBy("dateRecorded", Query.Direction.DESCENDING)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            Result.success(grades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByStudentAndSubject(studentId: String, subjectId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .orderBy("dateRecorded", Query.Direction.DESCENDING)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            Result.success(grades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesBySubject(subjectId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("subjectId", subjectId)
                .orderBy("dateRecorded", Query.Direction.DESCENDING)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            Result.success(grades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByTeacher(teacherId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("dateRecorded", Query.Direction.DESCENDING)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            Result.success(grades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGradesByType(gradeType: GradeType): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("gradeType", gradeType.name)
                .orderBy("dateRecorded", Query.Direction.DESCENDING)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            Result.success(grades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateStudentAverageForSubject(studentId: String, subjectId: String): Result<Double> {
        return try {
            val gradesResult = getGradesByStudentAndSubject(studentId, subjectId)
            val grades = gradesResult.getOrThrow()
            
            if (grades.isEmpty()) {
                Result.success(0.0)
            } else {
                val average = grades.map { it.percentage }.average()
                Result.success(average)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateStudentOverallAverage(studentId: String): Result<Double> {
        return try {
            val gradesResult = getGradesByStudent(studentId)
            val grades = gradesResult.getOrThrow()
            
            if (grades.isEmpty()) {
                Result.success(0.0)
            } else {
                val average = grades.map { it.percentage }.average()
                Result.success(average)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Semester
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val subjectsCollection = firestore.collection("subjects")

    suspend fun createSubject(subject: Subject): Result<Subject> {
        return try {
            val docRef = subjectsCollection.add(subject).await()
            val createdSubject = subject.copy(id = docRef.id)
            subjectsCollection.document(docRef.id).set(createdSubject).await()
            Result.success(createdSubject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubject(subject: Subject): Result<Unit> {
        return try {
            subjectsCollection.document(subject.id).set(subject).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        return try {
            subjectsCollection.document(subjectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubjectById(subjectId: String): Result<Subject> {
        return try {
            val document = subjectsCollection.document(subjectId).get().await()
            val subject = document.toObject(Subject::class.java)
                ?: throw Exception("Subject not found")
            Result.success(subject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSubjects(): Result<List<Subject>> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            val subjects = snapshot.toObjects(Subject::class.java)
            Result.success(subjects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSubject(
        name: String,
        code: String,
        description: String,
        credits: Int,
        semester: String,
        academicYear: String,
        courseId: String,
        yearLevelId: String
    ): Result<Subject> {
        return try {
            val subject = Subject(
                name = name,
                code = code,
                description = description,
                credits = credits,
                semester = Semester.valueOf(semester),
                academicYear = academicYear,
                courseId = courseId,
                yearLevelId = yearLevelId
            )
            val docRef = subjectsCollection.add(subject).await()
            val createdSubject = subject.copy(id = docRef.id)
            subjectsCollection.document(docRef.id).set(createdSubject).await()
            Result.success(createdSubject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubjectsByTeacher(teacherId: String): Result<List<Subject>> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("active", true)
                .get()
                .await()
            val subjects = snapshot.toObjects(Subject::class.java)
            Result.success(subjects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableSubjects(): Result<List<Subject>> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("teacherId", null)
                .whereEqualTo("active", true)
                .get()
                .await()
            val subjects = snapshot.toObjects(Subject::class.java)
            Result.success(subjects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableSubjectsForTeacher(teacherId: String): Result<List<Subject>> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("teacherId", null)
                .whereEqualTo("active", true)
                .get()
                .await()
            val subjects = snapshot.toObjects(Subject::class.java)
            Result.success(subjects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignTeacherToSubject(subjectId: String, teacherId: String, teacherName: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "teacherId" to teacherId,
                "teacherName" to teacherName
            )
            subjectsCollection.document(subjectId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeTeacherFromSubject(subjectId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "teacherId" to null,
                "teacherName" to null
            )
            subjectsCollection.document(subjectId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

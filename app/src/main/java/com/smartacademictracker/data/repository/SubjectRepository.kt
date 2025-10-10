package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Semester
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.service.AcademicPeriodFilterService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val academicPeriodFilterService: AcademicPeriodFilterService
) {
    private val subjectsCollection = firestore.collection("subjects")
    private val yearLevelsCollection = firestore.collection("year_levels")
    private val coursesCollection = firestore.collection("courses")

    suspend fun createSubject(subject: Subject): Result<Subject> {
        return try {
            // Get active academic period context
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                return Result.failure(Exception("No active academic period found. Please set an active academic period first."))
            }
            
            // Add academic period reference to the subject
            val subjectWithPeriod = subject.copy(academicPeriodId = academicContext.periodId)
            
            val docRef = subjectsCollection.add(subjectWithPeriod).await()
            val createdSubject = subjectWithPeriod.copy(id = docRef.id)
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
            // Get active academic period context
            val academicContext = academicPeriodFilterService.getAcademicPeriodContext()
            if (!academicContext.isActive) {
                return Result.success(emptyList())
            }
            
            val snapshot = subjectsCollection
                .whereEqualTo("active", true)
                .whereEqualTo("academicPeriodId", academicContext.periodId)
                .get()
                .await()
            
            // Handle potential deserialization errors for corrupted data
            val subjects = mutableListOf<Subject>()
            for (document in snapshot.documents) {
                try {
                    val subject = document.toObject(Subject::class.java)
                    if (subject != null) {
                        subjects.add(subject)
                    }
                } catch (e: Exception) {
                    println("DEBUG: SubjectRepository - Skipping corrupted subject ${document.id}: ${e.message}")
                    // Optionally, you could fix the corrupted data here
                    // For now, we'll just skip it and continue
                }
            }
            
            // Populate computed fields (yearLevelName, courseName, courseCode)
            val subjectsWithComputedFields = subjects.map { subject ->
                subject.copy(
                    yearLevelName = getYearLevelName(subject.yearLevelId),
                    courseName = getCourseName(subject.courseId),
                    courseCode = getCourseCode(subject.courseId)
                )
            }
            
            
            Result.success(subjectsWithComputedFields)
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
        yearLevelId: String,
        numberOfSections: Int = 1,
        subjectType: com.smartacademictracker.data.model.SubjectType = com.smartacademictracker.data.model.SubjectType.MAJOR
    ): Result<Subject> {
        return try {
            val sections = generateSections(code, numberOfSections)
            val subject = Subject(
                name = name,
                code = code,
                description = description,
                credits = credits,
                semester = convertStringToSemester(semester),
                academicYear = academicYear,
                courseId = courseId,
                yearLevelId = yearLevelId,
                numberOfSections = numberOfSections,
                sections = sections,
                subjectType = subjectType
            )
            // Use the createSubject method which includes academic period filtering
            createSubject(subject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateSections(subjectCode: String, numberOfSections: Int): List<String> {
        val sections = mutableListOf<String>()
        for (i in 0 until numberOfSections) {
            val sectionLetter = ('A' + i).toString()
            sections.add("$subjectCode$sectionLetter")
        }
        return sections
    }
    
    private fun convertStringToSemester(semesterString: String): Semester {
        return when (semesterString) {
            "1st Semester" -> Semester.FIRST_SEMESTER
            "2nd Semester" -> Semester.SECOND_SEMESTER
            "Summer Class" -> Semester.SUMMER_CLASS
            else -> throw IllegalArgumentException("Invalid semester: $semesterString")
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
            
            // Populate computed fields (yearLevelName, courseName, courseCode)
            val subjectsWithComputedFields = subjects.map { subject ->
                subject.copy(
                    yearLevelName = getYearLevelName(subject.yearLevelId),
                    courseName = getCourseName(subject.courseId),
                    courseCode = getCourseCode(subject.courseId)
                )
            }
            
            
            Result.success(subjectsWithComputedFields)
        } catch (e: Exception) {
            println("DEBUG: SubjectRepository - Error getting subjects for teacher: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun getYearLevelName(yearLevelId: String): String {
        return try {
            if (yearLevelId.isEmpty()) return ""
            val yearLevelDoc = yearLevelsCollection.document(yearLevelId).get().await()
            val yearLevel = yearLevelDoc.toObject(YearLevel::class.java)
            yearLevel?.name ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private suspend fun getCourseName(courseId: String): String {
        return try {
            if (courseId.isEmpty()) return ""
            val courseDoc = coursesCollection.document(courseId).get().await()
            val course = courseDoc.toObject(Course::class.java)
            course?.name ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    private suspend fun getCourseCode(courseId: String): String {
        return try {
            if (courseId.isEmpty()) return ""
            val courseDoc = coursesCollection.document(courseId).get().await()
            val course = courseDoc.toObject(Course::class.java)
            course?.code ?: ""
        } catch (e: Exception) {
            ""
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
    
    suspend fun cleanupCorruptedSubjects(): Result<Int> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("active", true)
                .get()
                .await()
            
            var corruptedCount = 0
            for (document in snapshot.documents) {
                try {
                    document.toObject(Subject::class.java)
                } catch (e: Exception) {
                    println("DEBUG: SubjectRepository - Found corrupted subject ${document.id}, deleting...")
                    subjectsCollection.document(document.id).delete().await()
                    corruptedCount++
                }
            }
            
            Result.success(corruptedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

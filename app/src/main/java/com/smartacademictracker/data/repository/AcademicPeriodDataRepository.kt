package com.smartacademictracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartacademictracker.data.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicPeriodDataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val academicPeriodRepository: AcademicPeriodRepository
) {
    private val coursesCollection = firestore.collection("courses")
    private val yearLevelsCollection = firestore.collection("year_levels")
    private val subjectsCollection = firestore.collection("subjects")
    private val sectionAssignmentsCollection = firestore.collection("section_assignments")
    private val teacherApplicationsCollection = firestore.collection("teacher_applications")
    private val studentApplicationsCollection = firestore.collection("subject_applications")
    private val gradesCollection = firestore.collection("grades")
    private val usersCollection = firestore.collection("users")

    /**
     * Get all data for a specific academic period
     */
    suspend fun getAcademicPeriodData(periodId: String): Result<AcademicPeriodData> {
        return try {
            println("DEBUG: AcademicPeriodDataRepository - Getting data for period: $periodId")
            
            // Get academic period details
            val academicPeriodResult = academicPeriodRepository.getAcademicPeriodById(periodId)
            val academicPeriod = academicPeriodResult.getOrNull()
            
            // Get all data for this academic period
            val coursesResult = getCoursesByPeriod(periodId)
            val yearLevelsResult = getYearLevelsByPeriod(periodId)
            val subjectsResult = getSubjectsByPeriod(periodId)
            val sectionAssignmentsResult = getSectionAssignmentsByPeriod(periodId)
            val teacherApplicationsResult = getTeacherApplicationsByPeriod(periodId)
            val studentApplicationsResult = getStudentApplicationsByPeriod(periodId)
            val gradesResult = getGradesByPeriod(periodId)
            val usersResult = getUsersByPeriod(periodId)
            
            // Calculate statistics
            val statistics = calculateStatistics(
                courses = coursesResult.getOrNull() ?: emptyList(),
                yearLevels = yearLevelsResult.getOrNull() ?: emptyList(),
                subjects = subjectsResult.getOrNull() ?: emptyList(),
                sectionAssignments = sectionAssignmentsResult.getOrNull() ?: emptyList(),
                teacherApplications = teacherApplicationsResult.getOrNull() ?: emptyList(),
                studentApplications = studentApplicationsResult.getOrNull() ?: emptyList(),
                grades = gradesResult.getOrNull() ?: emptyList(),
                users = usersResult.getOrNull() ?: emptyList()
            )
            
            val academicPeriodData = AcademicPeriodData(
                periodId = periodId,
                academicPeriod = academicPeriod,
                courses = coursesResult.getOrNull() ?: emptyList(),
                yearLevels = yearLevelsResult.getOrNull() ?: emptyList(),
                subjects = subjectsResult.getOrNull() ?: emptyList(),
                sectionAssignments = sectionAssignmentsResult.getOrNull() ?: emptyList(),
                teacherApplications = teacherApplicationsResult.getOrNull() ?: emptyList(),
                studentApplications = studentApplicationsResult.getOrNull() ?: emptyList(),
                grades = gradesResult.getOrNull() ?: emptyList(),
                users = usersResult.getOrNull() ?: emptyList(),
                statistics = statistics
            )
            
            println("DEBUG: AcademicPeriodDataRepository - Retrieved data for period $periodId")
            println("DEBUG: - Courses: ${academicPeriodData.courses.size}")
            println("DEBUG: - Year Levels: ${academicPeriodData.yearLevels.size}")
            println("DEBUG: - Subjects: ${academicPeriodData.subjects.size}")
            println("DEBUG: - Section Assignments: ${academicPeriodData.sectionAssignments.size}")
            println("DEBUG: - Teacher Applications: ${academicPeriodData.teacherApplications.size}")
            println("DEBUG: - Student Applications: ${academicPeriodData.studentApplications.size}")
            println("DEBUG: - Grades: ${academicPeriodData.grades.size}")
            println("DEBUG: - Users: ${academicPeriodData.users.size}")
            
            Result.success(academicPeriodData)
        } catch (e: Exception) {
            println("DEBUG: AcademicPeriodDataRepository - Error getting period data: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all academic periods with their summary data
     */
    suspend fun getAllAcademicPeriodSummaries(): Result<List<AcademicPeriodSummary>> {
        return try {
            val periodsResult = academicPeriodRepository.getAllAcademicPeriods()
            if (periodsResult.isSuccess) {
                val periods = periodsResult.getOrNull() ?: emptyList()
                val summaries = periods.map { period ->
                    val dataResult = getAcademicPeriodData(period.id)
                    val data = dataResult.getOrNull()
                    AcademicPeriodSummary(
                        periodId = period.id,
                        periodName = period.name,
                        academicYear = period.academicYear,
                        semester = period.semester.displayName,
                        startDate = period.startDate,
                        endDate = period.endDate,
                        isActive = period.isActive,
                        statistics = data?.statistics ?: AcademicPeriodStatistics()
                    )
                }
                Result.success(summaries)
            } else {
                Result.failure<List<AcademicPeriodSummary>>(periodsResult.exceptionOrNull() ?: Exception("Failed to load periods"))
            }
        } catch (e: Exception) {
            Result.failure<List<AcademicPeriodSummary>>(e)
        }
    }

    /**
     * Get courses for a specific academic period
     */
    private suspend fun getCoursesByPeriod(periodId: String): Result<List<Course>> {
        return try {
            val snapshot = coursesCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val courses = snapshot.toObjects(Course::class.java)
            // Deduplicate courses by code to avoid showing duplicates
            val uniqueCourses = courses.distinctBy { it.code }
            println("DEBUG: AcademicPeriodDataRepository - Found ${courses.size} courses, ${uniqueCourses.size} unique courses for period $periodId")
            Result.success(uniqueCourses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get year levels for a specific academic period
     */
    private suspend fun getYearLevelsByPeriod(periodId: String): Result<List<YearLevel>> {
        return try {
            val snapshot = yearLevelsCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val yearLevels = snapshot.toObjects(YearLevel::class.java)
            // Deduplicate year levels by level number
            val uniqueYearLevels = yearLevels.distinctBy { it.level }
            println("DEBUG: AcademicPeriodDataRepository - Found ${yearLevels.size} year levels, ${uniqueYearLevels.size} unique year levels for period $periodId")
            Result.success(uniqueYearLevels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get subjects for a specific academic period
     */
    private suspend fun getSubjectsByPeriod(periodId: String): Result<List<Subject>> {
        return try {
            val snapshot = subjectsCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val subjects = snapshot.toObjects(Subject::class.java)
            // Deduplicate subjects by code
            val uniqueSubjects = subjects.distinctBy { it.code }
            println("DEBUG: AcademicPeriodDataRepository - Found ${subjects.size} subjects, ${uniqueSubjects.size} unique subjects for period $periodId")
            Result.success(uniqueSubjects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get section assignments for a specific academic period
     */
    private suspend fun getSectionAssignmentsByPeriod(periodId: String): Result<List<SectionAssignment>> {
        return try {
            val snapshot = sectionAssignmentsCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val assignments = snapshot.toObjects(SectionAssignment::class.java)
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get teacher applications for a specific academic period
     */
    private suspend fun getTeacherApplicationsByPeriod(periodId: String): Result<List<TeacherApplication>> {
        return try {
            val snapshot = teacherApplicationsCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val applications = snapshot.toObjects(TeacherApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get student applications for a specific academic period
     */
    private suspend fun getStudentApplicationsByPeriod(periodId: String): Result<List<SubjectApplication>> {
        return try {
            val snapshot = studentApplicationsCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val applications = snapshot.toObjects(SubjectApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get grades for a specific academic period
     */
    private suspend fun getGradesByPeriod(periodId: String): Result<List<Grade>> {
        return try {
            val snapshot = gradesCollection
                .whereEqualTo("academicPeriodId", periodId)
                .get()
                .await()
            val grades = snapshot.toObjects(Grade::class.java)
            Result.success(grades)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get users for a specific academic period (users created during this period)
     */
    private suspend fun getUsersByPeriod(periodId: String): Result<List<User>> {
        return try {
            // Get the academic period to get its date range
            val periodResult = academicPeriodRepository.getAcademicPeriodById(periodId)
            if (periodResult.isSuccess) {
                val period = periodResult.getOrNull()
                if (period != null) {
                    val snapshot = usersCollection
                        .whereGreaterThanOrEqualTo("createdAt", period.startDate)
                        .whereLessThanOrEqualTo("createdAt", period.endDate)
                        .get()
                        .await()
                    val users = snapshot.toObjects(User::class.java)
                    Result.success(users)
                } else {
                    Result.failure<List<User>>(Exception("Academic period not found"))
                }
            } else {
                Result.failure<List<User>>(periodResult.exceptionOrNull() ?: Exception("Failed to load period"))
            }
        } catch (e: Exception) {
            Result.failure<List<User>>(e)
        }
    }

    /**
     * Calculate statistics for the academic period
     */
    private fun calculateStatistics(
        courses: List<Course>,
        yearLevels: List<YearLevel>,
        subjects: List<Subject>,
        sectionAssignments: List<SectionAssignment>,
        teacherApplications: List<TeacherApplication>,
        studentApplications: List<SubjectApplication>,
        grades: List<Grade>,
        users: List<User>
    ): AcademicPeriodStatistics {
        val totalSections = subjects.sumOf { it.sections.size }
        val activeSectionAssignments = sectionAssignments.count { it.status == AssignmentStatus.ACTIVE }
        val pendingTeacherApplications = teacherApplications.count { it.status == ApplicationStatus.PENDING }
        val pendingStudentApplications = studentApplications.count { it.status == ApplicationStatus.PENDING }
        
        val teachers = users.filter { it.role == UserRole.TEACHER.value }
        val students = users.filter { it.role == UserRole.STUDENT.value }
        val admins = users.filter { it.role == UserRole.ADMIN.value }
        
        return AcademicPeriodStatistics(
            totalCourses = courses.size,
            totalYearLevels = yearLevels.size,
            totalSubjects = subjects.size,
            totalSections = totalSections,
            totalTeachers = teachers.size,
            totalStudents = students.size,
            totalAdmins = admins.size,
            totalTeacherApplications = teacherApplications.size,
            totalStudentApplications = studentApplications.size,
            totalGrades = grades.size,
            activeSectionAssignments = activeSectionAssignments,
            pendingTeacherApplications = pendingTeacherApplications,
            pendingStudentApplications = pendingStudentApplications
        )
    }
}

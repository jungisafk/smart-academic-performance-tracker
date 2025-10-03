package com.smartacademictracker.data.utils

import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.Semester
import com.smartacademictracker.data.repository.SubjectRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleDataGenerator @Inject constructor(
    private val subjectRepository: SubjectRepository
) {
    
    suspend fun generateSampleSubjects(): Result<Unit> {
        return try {
            val sampleSubjects = createSampleSubjects()
            
            // Check if subjects already exist
            val existingSubjects = subjectRepository.getAllSubjects().getOrNull() ?: emptyList()
            if (existingSubjects.isNotEmpty()) {
                return Result.success(Unit) // Subjects already exist
            }
            
            // Add each subject
            for (subject in sampleSubjects) {
                subjectRepository.createSubject(subject)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createSampleSubjects(): List<Subject> {
        return listOf(
            // 1st Year ICT/IT Subjects
            Subject(
                name = "Introduction to Information Technology",
                code = "IT101",
                description = "Fundamental concepts of information technology, computer systems, and digital literacy.",
                credits = 3,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "1st_year_id", // This will be replaced with actual ID when courses/year levels are created
                courseId = "ict_course_id", // This will be replaced with actual ID when courses/year levels are created
                yearLevelName = "1st Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 40
            ),
            Subject(
                name = "Programming Fundamentals",
                code = "IT102",
                description = "Introduction to programming concepts using Python and basic algorithms.",
                credits = 4,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "1st_year_id",
                courseId = "ict_course_id",
                yearLevelName = "1st Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 35
            ),
            Subject(
                name = "Computer Hardware and Software",
                code = "IT103",
                description = "Understanding computer components, operating systems, and software installation.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "1st_year_id",
                courseId = "ict_course_id",
                yearLevelName = "1st Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 30
            ),
            Subject(
                name = "Web Development Basics",
                code = "IT104",
                description = "Introduction to HTML, CSS, and JavaScript for web development.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "1st_year_id",
                courseId = "ict_course_id",
                yearLevelName = "1st Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 35
            ),
            
            // 2nd Year ICT/IT Subjects
            Subject(
                name = "Object-Oriented Programming",
                code = "IT201",
                description = "Advanced programming concepts using Java and object-oriented design principles.",
                credits = 4,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "2nd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "2nd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 30
            ),
            Subject(
                name = "Database Management Systems",
                code = "IT202",
                description = "Introduction to database design, SQL, and data management concepts.",
                credits = 3,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "2nd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "2nd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 25
            ),
            Subject(
                name = "Data Structures and Algorithms",
                code = "IT203",
                description = "Study of fundamental data structures and algorithm design techniques.",
                credits = 4,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "2nd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "2nd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 25
            ),
            Subject(
                name = "Network Fundamentals",
                code = "IT204",
                description = "Introduction to computer networks, protocols, and network administration.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "2nd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "2nd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 30
            ),
            
            // 3rd Year ICT/IT Subjects
            Subject(
                name = "Software Engineering",
                code = "IT301",
                description = "Software development lifecycle, project management, and software design patterns.",
                credits = 4,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "3rd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "3rd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 25
            ),
            Subject(
                name = "Mobile Application Development",
                code = "IT302",
                description = "Development of mobile applications using modern frameworks and tools.",
                credits = 3,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "3rd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "3rd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 20
            ),
            Subject(
                name = "Cybersecurity Fundamentals",
                code = "IT303",
                description = "Introduction to cybersecurity, threats, and security best practices.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "3rd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "3rd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 25
            ),
            Subject(
                name = "Cloud Computing",
                code = "IT304",
                description = "Cloud platforms, services, and deployment strategies for modern applications.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "3rd_year_id",
                courseId = "ict_course_id",
                yearLevelName = "3rd Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 20
            ),
            
            // 4th Year ICT/IT Subjects
            Subject(
                name = "Capstone Project",
                code = "IT401",
                description = "Final year project integrating all learned concepts in a comprehensive software solution.",
                credits = 6,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "4th_year_id",
                courseId = "ict_course_id",
                yearLevelName = "4th Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 15
            ),
            Subject(
                name = "Artificial Intelligence and Machine Learning",
                code = "IT402",
                description = "Introduction to AI concepts, machine learning algorithms, and practical applications.",
                credits = 4,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "4th_year_id",
                courseId = "ict_course_id",
                yearLevelName = "4th Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 20
            ),
            Subject(
                name = "IT Project Management",
                code = "IT403",
                description = "Project management methodologies, tools, and techniques for IT projects.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "4th_year_id",
                courseId = "ict_course_id",
                yearLevelName = "4th Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 25
            ),
            Subject(
                name = "Emerging Technologies",
                code = "IT404",
                description = "Study of cutting-edge technologies and their impact on the IT industry.",
                credits = 3,
                semester = Semester.SECOND_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "4th_year_id",
                courseId = "ict_course_id",
                yearLevelName = "4th Year",
                courseName = "Information and Communication Technology",
                courseCode = "ICT",
                maxStudents = 20
            ),
            
            // IT Course Subjects (similar but with different focus)
            Subject(
                name = "Information Systems Analysis",
                code = "IS101",
                description = "Analysis and design of information systems for business applications.",
                credits = 3,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "1st_year_id",
                courseId = "it_course_id",
                yearLevelName = "1st Year",
                courseName = "Information Technology",
                courseCode = "IT",
                maxStudents = 30
            ),
            Subject(
                name = "Business Process Management",
                code = "IS201",
                description = "Understanding and optimizing business processes using IT solutions.",
                credits = 3,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "2nd_year_id",
                courseId = "it_course_id",
                yearLevelName = "2nd Year",
                courseName = "Information Technology",
                courseCode = "IT",
                maxStudents = 25
            ),
            Subject(
                name = "Enterprise Resource Planning",
                code = "IS301",
                description = "Implementation and management of ERP systems in organizations.",
                credits = 4,
                semester = Semester.FIRST_SEMESTER,
                academicYear = "2024-2025",
                yearLevelId = "3rd_year_id",
                courseId = "it_course_id",
                yearLevelName = "3rd Year",
                courseName = "Information Technology",
                courseCode = "IT",
                maxStudents = 20
            )
        )
    }
}

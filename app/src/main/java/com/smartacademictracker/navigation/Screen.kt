package com.smartacademictracker.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    
    // Student Screens
    object StudentDashboard : Screen("student_dashboard")
    object StudentGrades : Screen("student_grades")
    object StudentSubjects : Screen("student_subjects")
    object StudentSubjectApplication : Screen("student_subject_application")
    object StudentProfile : Screen("student_profile")
    object StudentAnalytics : Screen("student_analytics")
    object StudentPerformanceTracking : Screen("student_performance_tracking")
    object StudentSubjectDetail : Screen("student_subject_detail/{subjectId}") {
        fun createRoute(subjectId: String) = "student_subject_detail/$subjectId"
    }
    
    // Teacher Screens
    object TeacherDashboard : Screen("teacher_dashboard")
    object TeacherSubjects : Screen("teacher_subjects")
    object TeacherGradeInput : Screen("teacher_grade_input/{subjectId}") {
        fun createRoute(subjectId: String) = "teacher_grade_input/$subjectId"
    }
    object EnhancedTeacherGradeInput : Screen("enhanced_teacher_grade_input/{subjectId}") {
        fun createRoute(subjectId: String) = "enhanced_teacher_grade_input/$subjectId"
    }
    object BatchGradeInput : Screen("batch_grade_input/{subjectId}/{gradePeriod}") {
        fun createRoute(subjectId: String, gradePeriod: String) = "batch_grade_input/$subjectId/$gradePeriod"
    }
    object SubmissionTracking : Screen("submission_tracking/{subjectId}") {
        fun createRoute(subjectId: String) = "submission_tracking/$subjectId"
    }
    object TeacherApplications : Screen("teacher_applications")
    object TeacherStudentApplications : Screen("teacher_student_applications")
    object TeacherAnalytics : Screen("teacher_analytics")
    object ApplyForSubject : Screen("apply_for_subject/{subjectId}") {
        fun createRoute(subjectId: String) = "apply_for_subject/$subjectId"
    }
    
    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard")
    object AdminSubjects : Screen("admin_subjects")
    object AdminApplications : Screen("admin_applications")
    object AdminCourseManagement : Screen("admin_course_management")
    object AdminYearLevelManagement : Screen("admin_year_level_management")
    object AdminGradeMonitoring : Screen("admin_grade_monitoring")
    object AdminAcademicPeriods : Screen("admin_academic_periods")
    object AddSubject : Screen("add_subject")
    object AddCourse : Screen("add_course")
    object AddYearLevel : Screen("add_year_level")
    object AddAcademicPeriod : Screen("add_academic_period")
    object ManageUsers : Screen("manage_users")
    
    // Common Screens
    object Profile : Screen("profile")
}

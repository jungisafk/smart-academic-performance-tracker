package com.smartacademictracker.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    
    // Student Screens
    object StudentDashboard : Screen("student_dashboard")
    object StudentGrades : Screen("student_grades")
    object StudentSubjectDetail : Screen("student_subject_detail/{subjectId}") {
        fun createRoute(subjectId: String) = "student_subject_detail/$subjectId"
    }
    
    // Teacher Screens
    object TeacherDashboard : Screen("teacher_dashboard")
    object TeacherSubjects : Screen("teacher_subjects")
    object TeacherGradeInput : Screen("teacher_grade_input/{subjectId}") {
        fun createRoute(subjectId: String) = "teacher_grade_input/$subjectId"
    }
    object TeacherApplications : Screen("teacher_applications")
    object ApplyForSubject : Screen("apply_for_subject/{subjectId}") {
        fun createRoute(subjectId: String) = "apply_for_subject/$subjectId"
    }
    
    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard")
    object AdminSubjects : Screen("admin_subjects")
    object AdminApplications : Screen("admin_applications")
    object AddSubject : Screen("add_subject")
    object ManageUsers : Screen("manage_users")
    
    // Common Screens
    object Profile : Screen("profile")
}

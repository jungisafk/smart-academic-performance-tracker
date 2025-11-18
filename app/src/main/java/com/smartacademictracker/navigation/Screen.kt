package com.smartacademictracker.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object AccountActivation : Screen("account_activation")
    object ForgotPassword : Screen("forgot_password/{userType}") {
        fun createRoute(userType: com.smartacademictracker.data.model.UserRole) = "forgot_password/${userType.name}"
    }
    
    // Student Screens
    object StudentDashboard : Screen("student_dashboard")
    object StudentHome : Screen("student_home")
    object StudentGrades : Screen("student_grades")
    object StudentGradesTab : Screen("student_grades_tab")
    object StudentSubjects : Screen("student_subjects")
    object StudentSubjectsTab : Screen("student_subjects_tab")
    object StudentSubjectApplication : Screen("student_subject_application")
    object StudentProfile : Screen("student_profile")
    object StudentProfileTab : Screen("student_profile_tab")
    object StudentAnalytics : Screen("student_analytics")
    object StudentSubjectDetail : Screen("student_subject_detail/{subjectId}") {
        fun createRoute(subjectId: String) = "student_subject_detail/$subjectId"
    }
    object StudentApplicationDetail : Screen("student_application_detail/{applicationId}") {
        fun createRoute(applicationId: String) = "student_application_detail/$applicationId"
    }
    object StudentGradeHistory : Screen("student_grade_history")
    object StudentGradeComparison : Screen("student_grade_comparison")
    object StudentStudyProgress : Screen("student_study_progress")
    
    // Teacher Screens
    object TeacherDashboard : Screen("teacher_dashboard")
    object TeacherHome : Screen("teacher_home")
    object TeacherMySubjects : Screen("teacher_my_subjects")
    object TeacherStudentManagementTab : Screen("teacher_student_management_tab")
    object TeacherProfileTab : Screen("teacher_profile_tab")
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
    object AdminHome : Screen("admin_home")
    object AdminUserManagement : Screen("admin_user_management")
    object AdminAcademicManagement : Screen("admin_academic_management")
    object AdminProfileTab : Screen("admin_profile_tab")
    object AdminApplications : Screen("admin_applications")
    object AdminStudentApplications : Screen("admin_student_applications")
    object AdminCourseManagement : Screen("admin_course_management")
    object AdminYearLevelManagement : Screen("admin_year_level_management")
    object HierarchicalAcademicManagement : Screen("hierarchical_academic_management")
    object AdminGradeStatus : Screen("admin_grade_status")
    object AdminGradeEditRequests : Screen("admin_grade_edit_requests")
    object AdminAcademicPeriods : Screen("admin_academic_periods")
    object AddSubject : Screen("add_subject?courseId={courseId}&yearLevelId={yearLevelId}") {
        fun createRoute(courseId: String, yearLevelId: String) =
            "add_subject?courseId=$courseId&yearLevelId=$yearLevelId"
    }
    object AddCourse : Screen("add_course")
    object AddYearLevel : Screen("add_year_level?courseId={courseId}") {
        fun createRoute(courseId: String) = "add_year_level?courseId=$courseId"
    }
    object AddAcademicPeriod : Screen("add_academic_period")
    object AcademicPeriodData : Screen("academic_period_data")
    object ManageUsers : Screen("manage_users")
    object StudentEnrollment : Screen("student_enrollment")
    object TeacherStudentManagement : Screen("teacher_student_management")
    object TeacherSectionAssignment : Screen("teacher_section_assignment")
    object AdminPreRegisteredStudents : Screen("admin_pre_registered_students")
    object AdminPreRegisteredTeachers : Screen("admin_pre_registered_teachers")
    object AdminPreRegistered : Screen("admin_pre_registered")
    object AdminBulkImportStudents : Screen("admin_bulk_import_students")
    object AdminBulkImportTeachers : Screen("admin_bulk_import_teachers")
    
    // Common Screens
    object Profile : Screen("profile")
    object ChangePassword : Screen("change_password")
    
    // Notification Screens
    object NotificationTest : Screen("notification_test")
    object NotificationCenter : Screen("notification_center")
    object NotificationPreferences : Screen("notification_preferences")
    object Notifications : Screen("notifications")
}

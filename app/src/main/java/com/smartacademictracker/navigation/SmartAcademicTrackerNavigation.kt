package com.smartacademictracker.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.presentation.auth.AuthViewModel
import com.smartacademictracker.presentation.auth.SignInScreen
import com.smartacademictracker.presentation.auth.SignUpScreen
import com.smartacademictracker.presentation.student.StudentDashboardScreen
import com.smartacademictracker.presentation.student.StudentGradesScreen
import com.smartacademictracker.presentation.student.StudentSubjectsScreen
import com.smartacademictracker.presentation.student.StudentSubjectApplicationScreen
import com.smartacademictracker.presentation.student.StudentProfileScreen
import com.smartacademictracker.presentation.student.StudentPerformanceTrackingScreen
import com.smartacademictracker.presentation.teacher.TeacherDashboardScreen
import com.smartacademictracker.presentation.teacher.TeacherSubjectsScreen
import com.smartacademictracker.presentation.teacher.TeacherApplicationsScreen
import com.smartacademictracker.presentation.teacher.TeacherStudentApplicationsScreen
import com.smartacademictracker.presentation.teacher.TeacherGradeInputScreen
import com.smartacademictracker.presentation.admin.AdminDashboardScreen
import com.smartacademictracker.presentation.admin.AdminSubjectsScreen
import com.smartacademictracker.presentation.admin.AdminApplicationsScreen
import com.smartacademictracker.presentation.admin.AdminCourseManagementScreen
import com.smartacademictracker.presentation.admin.AdminYearLevelManagementScreen
import com.smartacademictracker.presentation.admin.AddSubjectScreen
import com.smartacademictracker.presentation.admin.AddCourseScreen
import com.smartacademictracker.presentation.admin.AddYearLevelScreen
import com.smartacademictracker.presentation.admin.ManageUsersScreen
import com.smartacademictracker.presentation.admin.AdminGradeMonitoringScreen
import com.smartacademictracker.presentation.admin.AdminAcademicPeriodScreen
import com.smartacademictracker.presentation.student.StudentAnalyticsScreen
import com.smartacademictracker.presentation.teacher.TeacherAnalyticsScreen

@Composable
fun SmartAcademicTrackerNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()
    
    // Handle navigation when user state changes
    LaunchedEffect(currentUser, uiState.isSignedIn) {
        println("DEBUG: Navigation LaunchedEffect triggered - currentUser: ${currentUser?.email}, isSignedIn: ${uiState.isSignedIn}")
        
        if (uiState.isSignedIn && currentUser != null) {
            val user = currentUser!!
            println("DEBUG: Navigating to dashboard for user: ${user.email} with role: ${user.role}")

            // Small delay to show success message
            kotlinx.coroutines.delay(200)

            val destination = when (user.role) {
                "STUDENT" -> Screen.StudentDashboard.route
                "TEACHER" -> Screen.TeacherDashboard.route
                "ADMIN" -> Screen.AdminDashboard.route
                else -> Screen.StudentDashboard.route
            }

            println("DEBUG: Navigating to: $destination")
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }

            // Clear the sign-in success state to prevent navigation loops
            authViewModel.clearSignInSuccess()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.SignIn.route
    ) {
        // Authentication Screens
        composable(Screen.SignIn.route) {
            SignInScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onSignInSuccess = {
                    // Navigation is handled by LaunchedEffect above
                },
                onNavigateToDashboard = { destination ->
                    println("DEBUG: Direct navigation to: $destination")
                    when (destination) {
                        "student_dashboard" -> navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        "teacher_dashboard" -> navController.navigate(Screen.TeacherDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        "admin_dashboard" -> navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToSignIn = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    // Navigation is handled by LaunchedEffect above
                }
            )
        }
        
        // Student Screens
        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(
                onNavigateToGrades = {
                    navController.navigate(Screen.StudentGrades.route)
                },
                onNavigateToSubjects = {
                    navController.navigate(Screen.StudentSubjects.route)
                },
                onNavigateToSubjectApplication = {
                    navController.navigate(Screen.StudentSubjectApplication.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.StudentProfile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.StudentAnalytics.route)
                },
                onNavigateToPerformanceTracking = {
                    navController.navigate(Screen.StudentPerformanceTracking.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.StudentGrades.route) {
            StudentGradesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSubjectDetail = { subjectId ->
                    navController.navigate(Screen.StudentSubjectDetail.createRoute(subjectId))
                }
            )
        }
        
        composable(Screen.StudentSubjects.route) {
            StudentSubjectsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSubjectDetail = { subjectId ->
                    navController.navigate(Screen.StudentSubjectDetail.createRoute(subjectId))
                }
            )
        }
        
        composable(Screen.StudentSubjectApplication.route) {
            StudentSubjectApplicationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToApplicationDetail = { applicationId ->
                    // TODO: Implement application detail navigation
                }
            )
        }
        
        composable(Screen.StudentProfile.route) {
            StudentProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.StudentAnalytics.route) {
            StudentAnalyticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentPerformanceTracking.route) {
            StudentPerformanceTrackingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentSubjectDetail.route) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            // TODO: Implement StudentSubjectDetailScreen
            Text("Student Subject Detail Screen - Coming Soon for subject: $subjectId")
        }
        
        // Teacher Screens
        composable(Screen.TeacherDashboard.route) {
            TeacherDashboardScreen(
                onNavigateToSubjects = {
                    navController.navigate(Screen.TeacherSubjects.route)
                },
                onNavigateToApplications = {
                    navController.navigate(Screen.TeacherApplications.route)
                },
                onNavigateToStudentApplications = {
                    navController.navigate(Screen.TeacherStudentApplications.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.TeacherSubjects.route) {
            TeacherSubjectsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGradeInput = { subjectId ->
                    navController.navigate(Screen.TeacherGradeInput.createRoute(subjectId))
                }
            )
        }
        
        composable(Screen.TeacherApplications.route) {
            TeacherApplicationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.TeacherStudentApplications.route) {
            TeacherStudentApplicationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.TeacherGradeInput.route) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            TeacherGradeInputScreen(
                subjectId = subjectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.TeacherAnalytics.route) {
            TeacherAnalyticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Admin Screens
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateToSubjects = {
                    navController.navigate(Screen.AdminSubjects.route)
                },
                onNavigateToApplications = {
                    navController.navigate(Screen.AdminApplications.route)
                },
                onNavigateToCourseManagement = {
                    navController.navigate(Screen.AdminCourseManagement.route)
                },
                onNavigateToYearLevelManagement = {
                    navController.navigate(Screen.AdminYearLevelManagement.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToGradeMonitoring = {
                    navController.navigate(Screen.AdminGradeMonitoring.route)
                },
                onNavigateToAcademicPeriods = {
                    navController.navigate(Screen.AdminAcademicPeriods.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.AdminSubjects.route) {
            AdminSubjectsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddSubject = {
                    navController.navigate(Screen.AddSubject.route)
                },
                onNavigateToEditSubject = { subjectId ->
                    // TODO: Implement edit subject navigation
                }
            )
        }
        
        composable(Screen.AdminApplications.route) {
            AdminApplicationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AdminCourseManagement.route) {
            AdminCourseManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddCourse = {
                    navController.navigate(Screen.AddCourse.route)
                },
                onNavigateToEditCourse = { courseId ->
                    // TODO: Implement edit course navigation
                }
            )
        }
        
        composable(Screen.AdminYearLevelManagement.route) {
            AdminYearLevelManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddYearLevel = {
                    navController.navigate(Screen.AddYearLevel.route)
                },
                onNavigateToEditYearLevel = { yearLevelId ->
                    // TODO: Implement edit year level navigation
                }
            )
        }
        
        composable(Screen.ManageUsers.route) {
            ManageUsersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AdminGradeMonitoring.route) {
            AdminGradeMonitoringScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AdminAcademicPeriods.route) {
            AdminAcademicPeriodScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddPeriod = {
                    navController.navigate(Screen.AddAcademicPeriod.route)
                }
            )
        }
        
        composable(Screen.AddAcademicPeriod.route) {
            // TODO: Implement AddAcademicPeriodScreen
            Text("Add Academic Period Screen - Coming Soon")
        }
        
        composable(Screen.AddSubject.route) {
            AddSubjectScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubjectAdded = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddCourse.route) {
            AddCourseScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCourseAdded = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddYearLevel.route) {
            AddYearLevelScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onYearLevelAdded = {
                    navController.popBackStack()
                }
            )
        }
    }
}

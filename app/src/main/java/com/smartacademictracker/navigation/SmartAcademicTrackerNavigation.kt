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
import com.smartacademictracker.presentation.teacher.TeacherDashboardScreen
import com.smartacademictracker.presentation.admin.AdminDashboardScreen
import com.smartacademictracker.presentation.admin.AdminSubjectsScreen
import com.smartacademictracker.presentation.admin.AdminApplicationsScreen
import com.smartacademictracker.presentation.admin.AddSubjectScreen

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
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.StudentGrades.route) {
            // TODO: Implement StudentGradesScreen
            Text("Student Grades Screen - Coming Soon")
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
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.TeacherSubjects.route) {
            // TODO: Implement TeacherSubjectsScreen
            Text("Teacher Subjects Screen - Coming Soon")
        }
        
        composable(Screen.TeacherApplications.route) {
            // TODO: Implement TeacherApplicationsScreen
            Text("Teacher Applications Screen - Coming Soon")
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
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
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
        
        composable(Screen.ManageUsers.route) {
            // TODO: Implement ManageUsersScreen
            Text("Manage Users Screen - Coming Soon")
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
    }
}

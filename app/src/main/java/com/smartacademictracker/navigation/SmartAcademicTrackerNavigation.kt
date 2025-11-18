package com.smartacademictracker.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.presentation.auth.AuthViewModel
import com.smartacademictracker.presentation.auth.SignInScreen
import com.smartacademictracker.presentation.auth.AccountActivationScreen
import com.smartacademictracker.presentation.auth.ForgotPasswordScreen
import com.smartacademictracker.presentation.student.StudentDashboardScreen
import com.smartacademictracker.presentation.student.StudentHomeScreen
import com.smartacademictracker.presentation.student.StudentMainScreen
import com.smartacademictracker.presentation.student.StudentSubjectsTabScreen
import com.smartacademictracker.presentation.student.StudentGradesTabScreen
import com.smartacademictracker.presentation.student.StudentGradesScreen
import com.smartacademictracker.presentation.student.StudentSubjectsScreen
import com.smartacademictracker.presentation.student.StudentSubjectApplicationScreen
import com.smartacademictracker.presentation.student.StudentProfileScreen
import com.smartacademictracker.presentation.student.StudentPerformanceTrackingScreen
import com.smartacademictracker.presentation.teacher.TeacherDashboardScreen
import com.smartacademictracker.presentation.teacher.TeacherMainScreen
import com.smartacademictracker.presentation.teacher.TeacherHomeScreen
import com.smartacademictracker.presentation.teacher.TeacherSubjectsScreen
import com.smartacademictracker.presentation.teacher.TeacherApplicationsScreen
import com.smartacademictracker.presentation.teacher.TeacherStudentApplicationsScreen
import com.smartacademictracker.presentation.teacher.TeacherGradeInputScreen
import com.smartacademictracker.presentation.admin.AddAcademicPeriodScreen
import com.smartacademictracker.presentation.admin.AdminAcademicPeriodScreen
import com.smartacademictracker.presentation.admin.AdminDashboardScreen
import com.smartacademictracker.presentation.admin.AdminMainScreen
import com.smartacademictracker.presentation.admin.AdminApplicationsScreen
import com.smartacademictracker.presentation.admin.AdminStudentApplicationsScreen
import com.smartacademictracker.presentation.admin.HierarchicalAcademicManagementScreen
import com.smartacademictracker.presentation.admin.AddSubjectScreen
import com.smartacademictracker.presentation.admin.AddCourseScreen
import com.smartacademictracker.presentation.admin.AddYearLevelScreen
import com.smartacademictracker.presentation.admin.ManageUsersScreen
import com.smartacademictracker.presentation.admin.AdminGradeStatusScreen
import com.smartacademictracker.presentation.admin.AdminScreenWithBottomNav
import com.smartacademictracker.navigation.wrapAdminScreenWithBottomNav
import com.smartacademictracker.presentation.admin.AdminGradeEditRequestsScreen
import com.smartacademictracker.presentation.admin.AdminAcademicPeriodScreen
import com.smartacademictracker.presentation.admin.TeacherSectionAssignmentScreen
import com.smartacademictracker.presentation.admin.AcademicPeriodDataScreen
import com.smartacademictracker.presentation.admin.AdminPreRegisteredStudentsScreen
import com.smartacademictracker.presentation.admin.AdminPreRegisteredTeachersScreen
import com.smartacademictracker.presentation.admin.AdminPreRegisteredScreen
import com.smartacademictracker.presentation.admin.AdminBulkImportStudentsScreen
import com.smartacademictracker.presentation.admin.AdminBulkImportTeachersScreen
import com.smartacademictracker.presentation.student.StudentEnrollmentScreen
import com.smartacademictracker.presentation.teacher.TeacherStudentManagementScreen
import com.smartacademictracker.presentation.student.StudentAnalyticsScreen
import com.smartacademictracker.presentation.student.StudentApplicationDetailScreen
import com.smartacademictracker.presentation.student.StudentGradeHistoryScreen
import com.smartacademictracker.presentation.student.StudentGradeComparisonScreen
import com.smartacademictracker.presentation.student.StudentStudyProgressScreen
import com.smartacademictracker.presentation.student.HierarchicalStudentSubjectApplicationScreen
import com.smartacademictracker.presentation.teacher.TeacherAnalyticsScreen
import com.smartacademictracker.presentation.teacher.HierarchicalTeacherSubjectApplicationScreen
import com.smartacademictracker.presentation.profile.ProfileScreen
import com.smartacademictracker.presentation.profile.ChangePasswordScreen
import com.smartacademictracker.presentation.notification.NotificationTestScreen
import com.smartacademictracker.presentation.notification.NotificationCenterScreen
import com.smartacademictracker.presentation.notification.NotificationPreferencesScreen
import com.smartacademictracker.presentation.notification.NotificationScreen

@Composable
fun SmartAcademicTrackerNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by authViewModel.uiState.collectAsState()
    
    // Handle navigation when user state changes
    LaunchedEffect(currentUser, uiState.isSignedIn) {
        if (uiState.isSignedIn && currentUser != null) {
            val user = currentUser!!

            // Small delay to show success message
            kotlinx.coroutines.delay(200)

            val destination = when (user.role) {
                "STUDENT" -> Screen.StudentHome.route
                "TEACHER" -> Screen.TeacherHome.route
                "ADMIN" -> Screen.AdminHome.route
                else -> Screen.StudentDashboard.route
            }

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
                onNavigateToActivation = {
                    navController.navigate(Screen.AccountActivation.route)
                },
                onSignInSuccess = {
                    // Navigation is handled by LaunchedEffect above
                },
                onNavigateToDashboard = { destination ->
                    when (destination) {
                        "student_dashboard" -> navController.navigate(Screen.StudentHome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        "teacher_dashboard" -> navController.navigate(Screen.TeacherHome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        "admin_dashboard" -> navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToForgotPassword = { userType ->
                    navController.navigate(Screen.ForgotPassword.createRoute(userType))
                }
            )
        }
        
        composable(Screen.ForgotPassword.route) { backStackEntry ->
            val userTypeString = backStackEntry.arguments?.getString("userType") ?: UserRole.STUDENT.name
            val userType = try {
                UserRole.valueOf(userTypeString)
            } catch (e: Exception) {
                UserRole.STUDENT
            }
            ForgotPasswordScreen(
                userType = userType,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AccountActivation.route) {
            AccountActivationScreen(
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onActivationSuccess = {
                    // Will redirect to sign-in automatically via screen logic
                }
            )
        }
        
        // Student Screens - Main Screen with Bottom Navigation
        composable(Screen.StudentHome.route) {
            StudentMainScreen(
                navController = navController,
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "StudentMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        // Legacy StudentDashboard route - redirect to StudentHome
        composable(Screen.StudentDashboard.route) {
            navController.navigate(Screen.StudentHome.route) {
                popUpTo(Screen.StudentDashboard.route) { inclusive = true }
            }
        }
        
        // Student Tab Screens - These are now handled internally by StudentMainScreen
        // Keeping routes for navigation to detail screens only
        
        composable(Screen.StudentGrades.route) {
            StudentGradesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentSubjects.route) {
            StudentSubjectsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentSubjectApplication.route) {
            HierarchicalStudentSubjectApplicationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToApplicationDetail = { applicationId ->
                    navController.navigate(Screen.StudentApplicationDetail.createRoute(applicationId))
                }
            )
        }
        
        composable(Screen.StudentApplicationDetail.route) { backStackEntry ->
            val applicationId = backStackEntry.arguments?.getString("applicationId") ?: ""
            StudentApplicationDetailScreen(
                applicationId = applicationId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentGradeHistory.route) {
            StudentGradeHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentGradeComparison.route) {
            StudentGradeComparisonScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.StudentStudyProgress.route) {
            StudentStudyProgressScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                },
                onNavigateToChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
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
        
        composable(Screen.StudentSubjectDetail.route) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            // TODO: Implement StudentSubjectDetailScreen
            Text("Student Subject Detail Screen - Coming Soon for subject: $subjectId")
        }
        
        // Teacher Screens - Main Screen with Bottom Navigation
        composable(Screen.TeacherHome.route) {
            TeacherMainScreen(
                navController = navController,
                onNavigateToSubjects = {
                    navController.navigate(Screen.TeacherMySubjects.route)
                },
                onNavigateToStudentManagement = {
                    navController.navigate(Screen.TeacherStudentManagementTab.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.TeacherAnalytics.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "TeacherMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        // Keep TeacherDashboard for backward compatibility (redirects to TeacherHome)
        composable(Screen.TeacherDashboard.route) {
            TeacherMainScreen(
                navController = navController,
                onNavigateToSubjects = {
                    navController.navigate(Screen.TeacherMySubjects.route)
                },
                onNavigateToStudentManagement = {
                    navController.navigate(Screen.TeacherStudentManagementTab.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.TeacherAnalytics.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.TeacherMySubjects.route) {
            TeacherMainScreen(
                navController = navController,
                onNavigateToSubjects = {
                    navController.navigate(Screen.TeacherMySubjects.route)
                },
                onNavigateToStudentManagement = {
                    navController.navigate(Screen.TeacherStudentManagementTab.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.TeacherAnalytics.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.TeacherStudentManagementTab.route) {
            TeacherMainScreen(
                navController = navController,
                onNavigateToSubjects = {
                    navController.navigate(Screen.TeacherMySubjects.route)
                },
                onNavigateToStudentManagement = {
                    navController.navigate(Screen.TeacherStudentManagementTab.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.TeacherAnalytics.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.TeacherProfileTab.route) {
            TeacherMainScreen(
                navController = navController,
                onNavigateToSubjects = {
                    navController.navigate(Screen.TeacherMySubjects.route)
                },
                onNavigateToStudentManagement = {
                    navController.navigate(Screen.TeacherStudentManagementTab.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.TeacherAnalytics.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
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
                    navController.navigate(Screen.TeacherMySubjects.route) {
                        popUpTo(Screen.TeacherHome.route) { inclusive = false }
                    }
                },
                onNavigateToGradeInput = { subjectId ->
                    navController.navigate(Screen.TeacherGradeInput.createRoute(subjectId))
                },
                showBackButton = true
            )
        }
        
        composable(Screen.TeacherApplications.route) {
            HierarchicalTeacherSubjectApplicationScreen(
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
        
        // Admin Screens - Main Screen with Bottom Navigation
        composable(Screen.AdminHome.route) {
            AdminMainScreen(
                navController = navController,
                onNavigateToApplications = {
                    navController.navigate(Screen.AdminApplications.route)
                },
                onNavigateToHierarchicalAcademicManagement = {
                    navController.navigate(Screen.HierarchicalAcademicManagement.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToGradeMonitoring = {
                    navController.navigate(Screen.AdminGradeStatus.route)
                },
                onNavigateToGradeEditRequests = {
                    navController.navigate(Screen.AdminGradeEditRequests.route)
                },
                onNavigateToAcademicPeriods = {
                    navController.navigate(Screen.AdminAcademicPeriods.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToPreRegistered = {
                    navController.navigate(Screen.AdminPreRegistered.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "AdminMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        composable(Screen.AdminUserManagement.route) {
            AdminMainScreen(
                navController = navController,
                onNavigateToApplications = {
                    navController.navigate(Screen.AdminApplications.route)
                },
                onNavigateToHierarchicalAcademicManagement = {
                    navController.navigate(Screen.HierarchicalAcademicManagement.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToGradeMonitoring = {
                    navController.navigate(Screen.AdminGradeStatus.route)
                },
                onNavigateToGradeEditRequests = {
                    navController.navigate(Screen.AdminGradeEditRequests.route)
                },
                onNavigateToAcademicPeriods = {
                    navController.navigate(Screen.AdminAcademicPeriods.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToPreRegistered = {
                    navController.navigate(Screen.AdminPreRegistered.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "AdminMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        composable(Screen.AdminAcademicManagement.route) {
            AdminMainScreen(
                navController = navController,
                onNavigateToApplications = {
                    navController.navigate(Screen.AdminApplications.route)
                },
                onNavigateToHierarchicalAcademicManagement = {
                    navController.navigate(Screen.HierarchicalAcademicManagement.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToGradeMonitoring = {
                    navController.navigate(Screen.AdminGradeStatus.route)
                },
                onNavigateToGradeEditRequests = {
                    navController.navigate(Screen.AdminGradeEditRequests.route)
                },
                onNavigateToAcademicPeriods = {
                    navController.navigate(Screen.AdminAcademicPeriods.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToPreRegistered = {
                    navController.navigate(Screen.AdminPreRegistered.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "AdminMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        composable(Screen.AdminProfileTab.route) {
            AdminMainScreen(
                navController = navController,
                onNavigateToApplications = {
                    navController.navigate(Screen.AdminApplications.route)
                },
                onNavigateToHierarchicalAcademicManagement = {
                    navController.navigate(Screen.HierarchicalAcademicManagement.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToGradeMonitoring = {
                    navController.navigate(Screen.AdminGradeStatus.route)
                },
                onNavigateToGradeEditRequests = {
                    navController.navigate(Screen.AdminGradeEditRequests.route)
                },
                onNavigateToAcademicPeriods = {
                    navController.navigate(Screen.AdminAcademicPeriods.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToPreRegistered = {
                    navController.navigate(Screen.AdminPreRegistered.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "AdminMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        // Keep AdminDashboard for backward compatibility (redirects to AdminHome)
        composable(Screen.AdminDashboard.route) {
            AdminMainScreen(
                navController = navController,
                onNavigateToApplications = {
                    navController.navigate(Screen.AdminApplications.route)
                },
                onNavigateToHierarchicalAcademicManagement = {
                    navController.navigate(Screen.HierarchicalAcademicManagement.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToGradeMonitoring = {
                    navController.navigate(Screen.AdminGradeStatus.route)
                },
                onNavigateToGradeEditRequests = {
                    navController.navigate(Screen.AdminGradeEditRequests.route)
                },
                onNavigateToAcademicPeriods = {
                    navController.navigate(Screen.AdminAcademicPeriods.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToPreRegistered = {
                    navController.navigate(Screen.AdminPreRegistered.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "AdminMainScreen: onNavigateToChangePassword called")
                    navController.navigate(Screen.ChangePassword.route)
                }
            )
        }
        
        composable(Screen.AdminPreRegistered.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Pre-Registered Users",
                content = { paddingValues ->
                    AdminPreRegisteredScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToStudentBulkImport = {
                            navController.navigate(Screen.AdminBulkImportStudents.route)
                        },
                        onNavigateToTeacherBulkImport = {
                            navController.navigate(Screen.AdminBulkImportTeachers.route)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminPreRegisteredStudents.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Pre-Registered Students",
                content = { paddingValues ->
                    AdminPreRegisteredStudentsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToBulkImport = {
                            navController.navigate(Screen.AdminBulkImportStudents.route)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminBulkImportStudents.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Bulk Import Students",
                content = { paddingValues ->
                    AdminBulkImportStudentsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onImportSuccess = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminBulkImportTeachers.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Bulk Import Teachers",
                content = { paddingValues ->
                    AdminBulkImportTeachersScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onImportSuccess = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminPreRegisteredTeachers.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Pre-Registered Teachers",
                content = { paddingValues ->
                    AdminPreRegisteredTeachersScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToBulkImport = {
                            navController.navigate(Screen.AdminBulkImportTeachers.route)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminApplications.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Teacher Applications",
                content = { paddingValues ->
                    AdminApplicationsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminStudentApplications.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Student Applications",
                content = { paddingValues ->
                    AdminStudentApplicationsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.TeacherSectionAssignment.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Teacher Section Assignment",
                content = { paddingValues ->
                    TeacherSectionAssignmentScreen(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.HierarchicalAcademicManagement.route) {
            val viewModel: com.smartacademictracker.presentation.admin.HierarchicalAcademicManagementViewModel = hiltViewModel()
            
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Academic Structure",
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshData()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                content = { paddingValues ->
                    HierarchicalAcademicManagementScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddCourse = {
                            navController.navigate(Screen.AddCourse.route)
                        },
                        onNavigateToAddYearLevel = { courseId ->
                            navController.navigate("add_year_level?courseId=$courseId")
                        },
                        onNavigateToAddSubject = { courseId, yearLevelId ->
                            val route = Screen.AddSubject.createRoute(courseId, yearLevelId)
                            navController.navigate(route)
                        },
                        onNavigateToAddMinorSubject = { yearLevelId ->
                            // For MINOR subjects, pass empty courseId
                            val route = Screen.AddSubject.createRoute("", yearLevelId)
                            navController.navigate(route)
                        },
                        onNavigateToEditCourse = { courseId ->
                            // TODO: Implement edit course navigation
                        },
                        onNavigateToEditYearLevel = { yearLevelId ->
                            // TODO: Implement edit year level navigation
                        },
                        onNavigateToEditSubject = { subjectId ->
                            // TODO: Implement edit subject navigation
                        },
                        onNavigateToAcademicPeriods = {
                            navController.navigate(Screen.AdminAcademicPeriods.route)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.ManageUsers.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Manage Users",
                content = { paddingValues ->
                    ManageUsersScreen(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminGradeStatus.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Grade Status",
                content = { paddingValues ->
                    AdminGradeStatusScreen(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminGradeEditRequests.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Grade Edit Requests",
                content = { paddingValues ->
                    AdminGradeEditRequestsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AdminAcademicPeriods.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Academic Periods",
                content = { paddingValues ->
                    AdminAcademicPeriodScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAddPeriod = {
                            navController.navigate(Screen.AddAcademicPeriod.route)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AddAcademicPeriod.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Add Academic Period",
                content = { paddingValues ->
                    AddAcademicPeriodScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onPeriodAdded = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AcademicPeriodData.route) {
            val viewModel: com.smartacademictracker.presentation.admin.AcademicPeriodDataViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Academic Period Data",
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadAcademicPeriodsAndSummaries()
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                content = { paddingValues ->
                    AcademicPeriodDataScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
    
    composable(Screen.StudentEnrollment.route) {
        StudentEnrollmentScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
    
        composable(Screen.TeacherStudentManagement.route) {
            TeacherStudentManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.TeacherAnalytics.route)
                },
                showBackButton = true
            )
        }
        
        composable(Screen.AddSubject.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val yearLevelId = backStackEntry.arguments?.getString("yearLevelId") ?: ""
            AddSubjectScreen(
                courseId = courseId,
                yearLevelId = yearLevelId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubjectAdded = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddCourse.route) {
            wrapAdminScreenWithBottomNav(
                navController = navController,
                title = "Add New Course",
                content = { paddingValues ->
                    AddCourseScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onCourseAdded = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            )
        }
        
        composable(Screen.AddYearLevel.route) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            AddYearLevelScreen(
                courseId = courseId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onYearLevelAdded = {
                    // Refresh the hierarchical management screen when year level is added
                    navController.popBackStack()
                }
            )
        }
        
        // Profile Screen (Common for all roles)
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "onNavigateToChangePassword called")
                    android.util.Log.d("SmartAcademicTrackerNavigation", "ChangePassword route: ${Screen.ChangePassword.route}")
                    android.util.Log.d("SmartAcademicTrackerNavigation", "Current route: ${navController.currentDestination?.route}")
                    try {
                        navController.navigate(Screen.ChangePassword.route)
                        android.util.Log.d("SmartAcademicTrackerNavigation", "Navigation to ChangePassword successful")
                    } catch (e: Exception) {
                        android.util.Log.e("SmartAcademicTrackerNavigation", "Error navigating to ChangePassword", e)
                    }
                }
            )
        }
        
        // Change Password Screen
        composable(Screen.ChangePassword.route) {
            android.util.Log.d("SmartAcademicTrackerNavigation", "ChangePassword composable route reached")
            ChangePasswordScreen(
                onNavigateBack = {
                    android.util.Log.d("SmartAcademicTrackerNavigation", "ChangePassword onNavigateBack called")
                    navController.popBackStack()
                },
                navController = navController
            )
        }
        
        // Notification Screens
        composable(Screen.NotificationTest.route) {
            NotificationTestScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.NotificationCenter.route) {
            NotificationCenterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNotification = { notificationId ->
                    // Handle notification detail navigation if needed
                }
            )
        }
        
        composable(Screen.NotificationPreferences.route) {
            NotificationPreferencesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Notifications.route) {
            NotificationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

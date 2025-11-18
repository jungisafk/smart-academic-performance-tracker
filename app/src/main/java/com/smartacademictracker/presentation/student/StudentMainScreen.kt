package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.smartacademictracker.navigation.Screen
import com.smartacademictracker.presentation.profile.ProfileScreen

@Composable
fun StudentMainScreen(
    navController: NavHostController,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onSignOut: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(StudentBottomNavItem.Home.route) }
    
    Scaffold(
        bottomBar = {
            StudentBottomNavigationBar(
                currentRoute = selectedTab,
                onNavigate = { route ->
                    selectedTab = route
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                StudentBottomNavItem.Home.route -> {
                    StudentHomeScreen(
                        onNavigateToSubjects = {
                            selectedTab = StudentBottomNavItem.Subjects.route
                        },
                        onNavigateToGrades = {
                            selectedTab = StudentBottomNavItem.Grades.route
                        },
                        onNavigateToNotifications = onNavigateToNotifications
                    )
                }
                StudentBottomNavItem.Subjects.route -> {
                    StudentSubjectsTabScreen(
                        onNavigateBack = {},
                        onNavigateToSubjectDetail = { subjectId ->
                            navController.navigate(Screen.StudentSubjectDetail.createRoute(subjectId))
                        },
                        onNavigateToApplicationDetail = { applicationId ->
                            navController.navigate(Screen.StudentApplicationDetail.createRoute(applicationId))
                        },
                        showBackButton = false
                    )
                }
                StudentBottomNavItem.Grades.route -> {
                    StudentGradesTabScreen(
                        onNavigateBack = {},
                        showBackButton = false
                    )
                }
                StudentBottomNavItem.Profile.route -> {
                    com.smartacademictracker.presentation.student.StudentProfileScreen(
                        onNavigateBack = {},
                        onSignOut = onSignOut,
                        onNavigateToChangePassword = onNavigateToChangePassword,
                        showBackButton = false // Hide back button in bottom nav
                    )
                }
            }
        }
    }
}

@Composable
fun StudentProfileScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {}
) {
    ProfileScreen(
        onNavigateBack = onNavigateBack,
        onSignOut = onSignOut,
        onNavigateToChangePassword = onNavigateToChangePassword,
        showBackButton = false // Hide back button in bottom nav
    )
}


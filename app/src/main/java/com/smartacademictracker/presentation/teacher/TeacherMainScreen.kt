package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smartacademictracker.navigation.Screen

@Composable
fun TeacherMainScreen(
    navController: NavHostController,
    onNavigateToSubjects: () -> Unit,
    onNavigateToStudentManagement: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onSignOut: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine current tab based on route
    val currentTab = when (currentRoute) {
        Screen.TeacherHome.route, Screen.TeacherDashboard.route -> TeacherBottomNavItem.Home.route
        Screen.TeacherMySubjects.route, Screen.TeacherSubjects.route -> TeacherBottomNavItem.MySubjects.route
        Screen.TeacherStudentManagementTab.route, Screen.TeacherStudentManagement.route -> TeacherBottomNavItem.StudentManagement.route
        Screen.TeacherProfileTab.route -> TeacherBottomNavItem.Profile.route
        else -> TeacherBottomNavItem.Home.route
    }
    
    Scaffold(
        bottomBar = {
            TeacherBottomNavigationBar(
                currentRoute = currentTab,
                onNavigate = { route ->
                    val targetRoute = when (route) {
                        TeacherBottomNavItem.Home.route -> Screen.TeacherHome.route
                        TeacherBottomNavItem.MySubjects.route -> Screen.TeacherMySubjects.route
                        TeacherBottomNavItem.StudentManagement.route -> Screen.TeacherStudentManagementTab.route
                        TeacherBottomNavItem.Profile.route -> Screen.TeacherProfileTab.route
                        else -> Screen.TeacherHome.route
                    }
                    
                    // If switching to a different tab, navigate to root and clear back stack
                    // If already on the same tab, navigate to root (pop back if on sub-screen)
                    val isSwitchingTabs = currentTab != route
                    val isOnTargetRoot = currentRoute == targetRoute
                    
                    if (isSwitchingTabs) {
                        // Switch to different tab - pop to TeacherHome (common root) then navigate to target
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.TeacherHome.route) {
                                inclusive = false
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    } else if (!isOnTargetRoot) {
                        // Same tab but on a sub-screen - pop back to root
                        navController.navigate(targetRoute) {
                            popUpTo(targetRoute) {
                                inclusive = false
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                    // If already on target root, do nothing
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                TeacherBottomNavItem.Home.route -> {
                    TeacherHomeScreen(
                        onNavigateToStudentManagement = onNavigateToStudentManagement,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToNotifications = onNavigateToNotifications
                    )
                }
                TeacherBottomNavItem.MySubjects.route -> {
                    TeacherSubjectsScreen(
                        onNavigateBack = {
                            navController.navigate(Screen.TeacherMySubjects.route) {
                                popUpTo(Screen.TeacherHome.route) { inclusive = false }
                            }
                        },
                        onNavigateToGradeInput = { subjectId ->
                            navController.navigate(Screen.TeacherGradeInput.createRoute(subjectId))
                        },
                        showBackButton = false
                    )
                }
                TeacherBottomNavItem.StudentManagement.route -> {
                    TeacherStudentManagementScreen(
                        onNavigateBack = {
                            navController.navigate(Screen.TeacherStudentManagementTab.route) {
                                popUpTo(Screen.TeacherHome.route) { inclusive = false }
                            }
                        },
                        onNavigateToAnalytics = {
                            navController.navigate(Screen.TeacherAnalytics.route)
                        },
                        showBackButton = false
                    )
                }
                TeacherBottomNavItem.Profile.route -> {
                    TeacherProfileScreen(
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onSignOut = onSignOut,
                        onNavigateToChangePassword = onNavigateToChangePassword
                    )
                }
            }
        }
    }
}

@Composable
fun TeacherProfileScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {}
) {
    TeacherProfileScreenContent(
        onNavigateBack = onNavigateBack,
        onSignOut = onSignOut,
        onNavigateToChangePassword = onNavigateToChangePassword,
        showBackButton = false // Hide back button in bottom nav
    )
}


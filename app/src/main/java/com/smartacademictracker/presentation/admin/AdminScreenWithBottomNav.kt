package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smartacademictracker.navigation.Screen

@Composable
fun AdminScreenWithBottomNav(
    navController: NavHostController,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToHierarchicalAcademicManagement: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToGradeMonitoring: () -> Unit,
    onNavigateToGradeEditRequests: () -> Unit,
    onNavigateToAcademicPeriods: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPreRegistered: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine current tab based on route
    val currentTab = when (currentRoute) {
        Screen.AdminHome.route, Screen.AdminDashboard.route -> AdminBottomNavItem.Home.route
        Screen.AdminUserManagement.route -> AdminBottomNavItem.UserManagement.route
        Screen.AdminAcademicManagement.route -> AdminBottomNavItem.AcademicManagement.route
        Screen.AdminProfileTab.route -> AdminBottomNavItem.Profile.route
        // For other admin screens, determine the tab based on the screen type
        Screen.AdminApplications.route,
        Screen.AdminStudentApplications.route,
        Screen.ManageUsers.route,
        Screen.AdminPreRegistered.route,
        Screen.AdminPreRegisteredStudents.route,
        Screen.AdminPreRegisteredTeachers.route,
        Screen.AdminBulkImportStudents.route,
        Screen.AdminBulkImportTeachers.route -> AdminBottomNavItem.UserManagement.route
        Screen.AdminGradeStatus.route,
        Screen.AdminGradeEditRequests.route,
        Screen.AdminAcademicPeriods.route,
        Screen.HierarchicalAcademicManagement.route,
        Screen.TeacherSectionAssignment.route,
        Screen.AddSubject.route,
        Screen.AddCourse.route,
        Screen.AddYearLevel.route,
        Screen.AddAcademicPeriod.route,
        Screen.AcademicPeriodData.route -> AdminBottomNavItem.AcademicManagement.route
        else -> AdminBottomNavItem.Home.route
    }
    
    Scaffold(
        topBar = topBar,
        bottomBar = {
            AdminBottomNavigationBar(
                currentRoute = currentTab,
                onNavigate = { route ->
                    val targetRoute = when (route) {
                        AdminBottomNavItem.Home.route -> Screen.AdminHome.route
                        AdminBottomNavItem.UserManagement.route -> Screen.AdminUserManagement.route
                        AdminBottomNavItem.AcademicManagement.route -> Screen.AdminAcademicManagement.route
                        AdminBottomNavItem.Profile.route -> Screen.AdminProfileTab.route
                        else -> Screen.AdminHome.route
                    }
                    
                    // If switching to a different tab, navigate to root and clear back stack
                    // If already on the same tab, navigate to root (pop back if on sub-screen)
                    val isSwitchingTabs = currentTab != route
                    val isOnTargetRoot = currentRoute == targetRoute
                    
                    if (isSwitchingTabs) {
                        // Switch to different tab - pop to AdminHome (common root) then navigate to target
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.AdminHome.route) {
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
        content(paddingValues)
    }
}


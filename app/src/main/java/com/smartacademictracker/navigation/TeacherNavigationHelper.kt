package com.smartacademictracker.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.smartacademictracker.presentation.teacher.TeacherBottomNavigationBar
import com.smartacademictracker.presentation.teacher.TeacherBottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreenWithBottomNav(
    navController: NavHostController,
    title: String,
    showBackButton: Boolean = true,
    actions: @Composable (RowScope) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions
            )
        },
        bottomBar = {
            TeacherBottomNavigationBar(
                currentRoute = currentTab,
                onNavigate = { route ->
                    // Check if we're currently on a route that's not part of the main teacher navigation
                    // (e.g., change_password, edit_profile, etc.)
                    val isOnSubScreen = currentRoute != null && 
                        currentRoute != Screen.TeacherHome.route &&
                        currentRoute != Screen.TeacherMySubjects.route &&
                        currentRoute != Screen.TeacherStudentManagementTab.route &&
                        currentRoute != Screen.TeacherProfileTab.route
                    
                    if (isOnSubScreen) {
                        // If we're on a sub-screen, navigate back to TeacherHome first
                        navController.navigate(Screen.TeacherHome.route) {
                            popUpTo(Screen.TeacherHome.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    } else {
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
                            try {
                                navController.navigate(targetRoute) {
                                    popUpTo(Screen.TeacherHome.route) {
                                        inclusive = false
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            } catch (e: IllegalArgumentException) {
                                // Route doesn't exist in current navigation graph
                                android.util.Log.w("TeacherNavigationHelper", "Route $targetRoute not found, navigating to TeacherHome instead")
                                navController.navigate(Screen.TeacherHome.route) {
                                    popUpTo(Screen.TeacherHome.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        } else if (!isOnTargetRoot) {
                            // Same tab but on a sub-screen - pop back to root
                            try {
                                navController.navigate(targetRoute) {
                                    popUpTo(targetRoute) {
                                        inclusive = false
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            } catch (e: IllegalArgumentException) {
                                // Route doesn't exist, navigate to TeacherHome
                                android.util.Log.w("TeacherNavigationHelper", "Route $targetRoute not found, navigating to TeacherHome instead")
                                navController.navigate(Screen.TeacherHome.route) {
                                    popUpTo(Screen.TeacherHome.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                        // If already on target root, do nothing
                    }
                }
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}


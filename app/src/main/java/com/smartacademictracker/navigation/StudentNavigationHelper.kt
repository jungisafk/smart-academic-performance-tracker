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
import com.smartacademictracker.presentation.student.StudentBottomNavigationBar
import com.smartacademictracker.presentation.student.StudentBottomNavItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreenWithBottomNav(
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
        Screen.StudentHome.route, Screen.StudentDashboard.route -> StudentBottomNavItem.Home.route
        Screen.StudentSubjectsTab.route, Screen.StudentSubjects.route -> StudentBottomNavItem.Subjects.route
        Screen.StudentGradesTab.route, Screen.StudentGrades.route -> StudentBottomNavItem.Grades.route
        Screen.StudentProfileTab.route, Screen.StudentProfile.route -> StudentBottomNavItem.Profile.route
        else -> StudentBottomNavItem.Home.route
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
            StudentBottomNavigationBar(
                currentRoute = currentTab,
                onNavigate = { route ->
                    // Check if we're currently on a route that's not part of the main student navigation
                    // (e.g., change_password, edit_profile, etc.)
                    val isOnSubScreen = currentRoute != null && 
                        currentRoute != Screen.StudentHome.route &&
                        currentRoute != Screen.StudentSubjectsTab.route &&
                        currentRoute != Screen.StudentGradesTab.route &&
                        currentRoute != Screen.StudentProfileTab.route &&
                        currentRoute != Screen.StudentProfile.route
                    
                    if (isOnSubScreen) {
                        // If we're on a sub-screen, navigate back to StudentHome first
                        // The StudentHome screen will handle tab selection internally
                        navController.navigate(Screen.StudentHome.route) {
                            popUpTo(Screen.StudentHome.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                        // Note: Tab selection will be handled by StudentMainScreen's internal state
                        // We can't directly set the tab from here, so we navigate to home
                        // The user will need to click the tab again after reaching home
                    } else {
                        // We're already on a main screen route, navigate normally
                        val targetRoute = when (route) {
                            StudentBottomNavItem.Home.route -> Screen.StudentHome.route
                            StudentBottomNavItem.Subjects.route -> Screen.StudentSubjectsTab.route
                            StudentBottomNavItem.Grades.route -> Screen.StudentGradesTab.route
                            StudentBottomNavItem.Profile.route -> Screen.StudentProfileTab.route
                            else -> Screen.StudentHome.route
                        }
                        
                        // Navigate to target route
                        try {
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.StudentHome.route) {
                                    inclusive = false
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } catch (e: IllegalArgumentException) {
                            // Route doesn't exist in current navigation graph
                            // Navigate to StudentHome instead
                            android.util.Log.w("StudentNavigationHelper", "Route $targetRoute not found, navigating to StudentHome instead")
                            navController.navigate(Screen.StudentHome.route) {
                                popUpTo(Screen.StudentHome.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}


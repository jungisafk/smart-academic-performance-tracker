package com.smartacademictracker.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.smartacademictracker.presentation.admin.AdminScreenWithBottomNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun wrapAdminScreenWithBottomNav(
    navController: NavHostController,
    title: String,
    showBackButton: Boolean = true,
    actions: @Composable (RowScope) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AdminScreenWithBottomNav(
        navController = navController,
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
        content = content,
        onNavigateToApplications = { navController.navigate(Screen.AdminApplications.route) },
        onNavigateToHierarchicalAcademicManagement = { navController.navigate(Screen.HierarchicalAcademicManagement.route) },
        onNavigateToUsers = { navController.navigate(Screen.ManageUsers.route) },
        onNavigateToGradeMonitoring = { navController.navigate(Screen.AdminGradeStatus.route) },
        onNavigateToGradeEditRequests = { navController.navigate(Screen.AdminGradeEditRequests.route) },
        onNavigateToAcademicPeriods = { navController.navigate(Screen.AdminAcademicPeriods.route) },
        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
        onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
        onNavigateToPreRegistered = { navController.navigate(Screen.AdminPreRegistered.route) }
    )
}


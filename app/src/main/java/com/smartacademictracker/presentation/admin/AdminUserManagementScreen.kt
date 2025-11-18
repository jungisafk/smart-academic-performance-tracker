package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.common.NotificationIconWithBadge
import com.smartacademictracker.presentation.notification.NotificationViewModel

@Composable
fun AdminUserManagementScreen(
    onNavigateToApplications: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToPreRegistered: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
        notificationViewModel.loadNotifications()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "User Management",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "Manage users, teachers, and registrations",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    NotificationIconWithBadge(
                        unreadCount = unreadCount,
                        onClick = onNavigateToNotifications
                    )
                }
            }
            
            // Quick Actions Section
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    
                    val actions = listOf(
                        QuickActionData("Teacher Management", onNavigateToApplications, Icons.Default.PersonAdd, true),
                        QuickActionData("Manage Users", onNavigateToUsers, Icons.Default.Person, false),
                        QuickActionData("Pre-Register", onNavigateToPreRegistered, Icons.Default.School, false)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        actions.chunked(2).forEach { rowActions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowActions.forEach { actionData ->
                                    val badgeCount = when (actionData.title) {
                                        "Teacher Management" -> uiState.pendingTeacherApplications
                                        else -> 0
                                    }
                                    QuickActionButton(
                                        title = actionData.title,
                                        onClick = actionData.onClick,
                                        icon = actionData.icon,
                                        isYellow = actionData.isYellow,
                                        badgeCount = badgeCount,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Add spacer if odd number of items
                                if (rowActions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


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
import com.smartacademictracker.presentation.common.NoActiveAcademicPeriodCard
import com.smartacademictracker.presentation.common.NotificationIconWithBadge
import com.smartacademictracker.presentation.notification.NotificationViewModel

@Composable
fun AdminHomeScreen(
    onNavigateToApplications: () -> Unit = {},
    onNavigateToHierarchicalAcademicManagement: () -> Unit = {},
    onNavigateToAcademicPeriods: () -> Unit = {},
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Loading dashboard data...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF666666)
                    )
                }
            }
        } else if (uiState.error != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        } else {
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
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            Text(
                                text = "Welcome back! Here's your system overview",
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
                
                // Current Academic Period Section
                item {
                    CurrentAcademicPeriodCard(
                        activePeriod = uiState.activeAcademicPeriod,
                        currentSemester = uiState.currentSemester,
                        currentAcademicYear = uiState.currentAcademicYear,
                        onCreateAcademicPeriod = onNavigateToAcademicPeriods
                    )
                }
                
                // System Overview Section
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
                                text = "System Overview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        // Use Column and Row instead of LazyVerticalGrid to avoid infinite height constraints
                        val overviewItems = listOf(
                            Triple("Total Subjects", uiState.totalSubjects.toString(), Icons.Default.MenuBook),
                            Triple("Active Subjects", uiState.activeSubjects.toString(), Icons.Default.School),
                            Triple("Total Students", uiState.totalStudents.toString(), Icons.Default.Person),
                            Triple("Total Teachers", uiState.totalTeachers.toString(), Icons.Default.PersonAdd),
                            Triple("Total Enrollments", uiState.totalEnrollments.toString(), Icons.Default.Assignment),
                            Triple("Pending Applications", uiState.pendingApplications.toString(), Icons.Default.Description)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            overviewItems.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { (label, value, icon) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            SystemOverviewCard(
                                                label = label,
                                                value = value,
                                                icon = icon
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Quick Actions Section (Only 2 actions)
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
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Teacher Management",
                                onClick = onNavigateToApplications,
                                icon = Icons.Default.PersonAdd,
                                isYellow = true,
                                badgeCount = uiState.pendingTeacherApplications,
                                modifier = Modifier.weight(1f)
                            )
                            
                            QuickActionButton(
                                title = "Academic Structure",
                                onClick = onNavigateToHierarchicalAcademicManagement,
                                icon = Icons.Default.MenuBook,
                                isYellow = false,
                                badgeCount = 0,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}


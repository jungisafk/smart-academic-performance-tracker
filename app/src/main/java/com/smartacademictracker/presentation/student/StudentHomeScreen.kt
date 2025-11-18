package com.smartacademictracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.auth.AuthViewModel
import com.smartacademictracker.presentation.common.NotificationIconWithBadge
import com.smartacademictracker.presentation.notification.NotificationViewModel

@Composable
fun StudentHomeScreen(
    onNavigateToSubjects: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: StudentDashboardViewModel = hiltViewModel(),
    analyticsViewModel: StudentAnalyticsViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val analyticsState by analyticsViewModel.uiState.collectAsState()
    val gradeAggregates by analyticsViewModel.gradeAggregates.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    // Load data when screen is composed, using ViewModels as keys to prevent unnecessary reloads
    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.loadDashboardData()
    }
    
    LaunchedEffect(analyticsViewModel) {
        analyticsViewModel.loadAnalyticsData()
    }
    
    LaunchedEffect(notificationViewModel) {
        notificationViewModel.loadNotifications()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Header Section
            StudentScreenHeader(
                title = "Student Home",
                showBackButton = false,
                actions = {
                    NotificationIconWithBadge(
                        unreadCount = unreadCount,
                        onClick = onNavigateToNotifications
                    )
                    
                    IconButton(
                        onClick = {
                            dashboardViewModel.refreshData()
                            analyticsViewModel.refreshData()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            )
        }
        
        item {
            // Welcome Banner (Name Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${currentUser?.firstName} ${currentUser?.lastName}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Track your academic progress and view your grades",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Student Badge Icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC107)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        item {
            // Overview Section
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Enrolled Subjects Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Enrolled Subjects",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (dashboardState.isLoading) "..." else dashboardState.enrolledSubjects.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                        }
                    }
                    
                    // Total Subject Passed Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Total Subject Passed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666),
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        dashboardState.isLoading -> "..."
                                        else -> dashboardState.totalSubjectsPassed.toString()
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC107)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            // Compact Analytics Section
            if (!analyticsState.isLoading && gradeAggregates.isNotEmpty()) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Performance Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Overall Average
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Overall Average",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (analyticsState.overallAverage != null) {
                                        String.format("%.1f", analyticsState.overallAverage) + "%"
                                    } else "--",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                            
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Color(0xFFE0E0E0))
                            )
                            
                            // Passing Subjects
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Passing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = analyticsState.passingSubjects.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            
                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(Color(0xFFE0E0E0))
                            )
                            
                            // At Risk Subjects
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "At Risk",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = analyticsState.atRiskSubjects.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC107)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


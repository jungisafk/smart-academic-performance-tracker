package com.smartacademictracker.presentation.teacher

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.auth.AuthViewModel
import com.smartacademictracker.presentation.common.NotificationIconWithBadge
import com.smartacademictracker.presentation.notification.NotificationViewModel

@Composable
fun TeacherHomeScreen(
    onNavigateToStudentManagement: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: TeacherDashboardViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.loadDashboardData()
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
        // Header Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Teacher Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                NotificationIconWithBadge(
                    unreadCount = unreadCount,
                    onClick = onNavigateToNotifications
                )
            }
        }
        
        // Welcome Banner (Professor Name Card)
        item {
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
                            text = "Prof. ${currentUser?.firstName} ${currentUser?.lastName}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Manage your subjects and input student grades",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Teacher Icon
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
        
        // Subject Overview Section
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
                        text = "Subject Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // Active Subjects Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                        Column {
                            Text(
                                text = "Active Subjects",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                            Text(
                                text = if (dashboardState.isLoading) "Loading..." else dashboardState.activeSubjects.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                }
                
                // Total Students Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Total Students",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                            Text(
                                text = if (dashboardState.isLoading) "Loading..." else dashboardState.totalStudents.toString(),
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
}



package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.teacher.TeacherDashboardScreen
import com.smartacademictracker.presentation.common.NoActiveAcademicPeriodCard

data class QuickActionData(
    val title: String,
    val onClick: () -> Unit,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isYellow: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToApplications: () -> Unit = {},
    onNavigateToHierarchicalAcademicManagement: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToGradeMonitoring: () -> Unit = {},
    onNavigateToAcademicPeriods: () -> Unit = {},
    onNavigateToAcademicPeriodData: () -> Unit = {},
    onNavigateToTeacherSectionAssignment: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
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
                CircularProgressIndicator(
                    color = Color(0xFF2196F3)
                )
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
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
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
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(300.dp)
                        ) {
                            items(listOf(
                                Triple("Total Subjects", uiState.totalSubjects.toString(), Icons.Default.MenuBook),
                                Triple("Active Subjects", uiState.activeSubjects.toString(), Icons.Default.School),
                                Triple("Total Students", uiState.totalStudents.toString(), Icons.Default.Person),
                                Triple("Total Teachers", uiState.totalTeachers.toString(), Icons.Default.PersonAdd),
                                Triple("Total Enrollments", uiState.totalEnrollments.toString(), Icons.Default.Assignment),
                                Triple("Pending Applications", uiState.pendingApplications.toString(), Icons.Default.Description)
                            )) { (label, value, icon) ->
                                SystemOverviewCard(
                                    label = label,
                                    value = value,
                                    icon = icon
                                )
                            }
                        }
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
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(400.dp)
                        ) {
                            items(listOf(
                                QuickActionData("Teacher Applications", onNavigateToApplications, Icons.Default.PersonAdd, true),
                                QuickActionData("Academic Structure", onNavigateToHierarchicalAcademicManagement, Icons.Default.MenuBook, false),
                                QuickActionData("Manage Users", onNavigateToUsers, Icons.Default.Person, false),
                                QuickActionData("Grade Monitoring", onNavigateToGradeMonitoring, Icons.Default.BarChart, false),
                                QuickActionData("Academic Periods", onNavigateToAcademicPeriods, Icons.Default.CalendarToday, false),
                                QuickActionData("Teacher Assignments", onNavigateToTeacherSectionAssignment, Icons.Default.Assignment, false),
                                QuickActionData("Period Data Viewer", onNavigateToAcademicPeriodData, Icons.Default.Description, false),
                                QuickActionData("Profile", onNavigateToProfile, Icons.Default.Person, false)
                            )) { actionData ->
                                QuickActionButton(
                                    title = actionData.title,
                                    onClick = actionData.onClick,
                                    icon = actionData.icon,
                                    isYellow = actionData.isYellow
                                )
                            }
                        }
                        
                        // Bottom row with Notifications and Refresh Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionButton(
                                title = "Notifications",
                                onClick = onNavigateToNotifications,
                                icon = Icons.Default.Notifications,
                                isYellow = false,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                title = "Refresh Data",
                                onClick = { viewModel.refreshData() },
                                icon = Icons.Default.Refresh,
                                isYellow = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Progress Dots
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == 0) Color(0xFF2196F3) else Color(0xFFE0E0E0)
                                    )
                            )
                            if (index < 2) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemOverviewCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (label.contains("Subjects") || label.contains("Students") || label.contains("Enrollments")) 
                            Color(0xFF2196F3) 
                        else Color(0xFFFFC107)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
fun CurrentAcademicPeriodCard(
    activePeriod: String,
    currentSemester: String,
    currentAcademicYear: String,
    onCreateAcademicPeriod: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Academic Period",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            if (activePeriod.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Academic Year",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentAcademicYear,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Semester",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentSemester,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                NoActiveAcademicPeriodCard(
                    onCreateAcademicPeriod = onCreateAcademicPeriod
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isYellow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isYellow) Color(0xFFFFC107) else Color(0xFF2196F3)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isYellow) Color(0xFF333333) else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isYellow) Color(0xFF333333) else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
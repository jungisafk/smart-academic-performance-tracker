package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.auth.AuthViewModel
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    onNavigateToGrades: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToSubjectApplication: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPerformanceTracking: () -> Unit,
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: StudentDashboardViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    // Load dashboard data when screen is composed
    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboardData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Dashboard") },
                actions = {
                    IconButton(onClick = { dashboardViewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${currentUser?.firstName} ${currentUser?.lastName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Track your academic progress and view your grades",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                // Quick Actions Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "View Grades",
                        icon = Icons.Default.Star,
                        onClick = onNavigateToGrades,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "My Subjects",
                        icon = Icons.Default.Menu,
                        onClick = onNavigateToSubjects,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Apply for Subjects",
                        icon = Icons.Default.School,
                        onClick = onNavigateToSubjectApplication,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Analytics",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Performance",
                        icon = Icons.Default.TrendingUp,
                        onClick = onNavigateToPerformanceTracking,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Profile",
                        icon = Icons.Default.Person,
                        onClick = onNavigateToProfile,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                // Grade Overview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Enrolled Subjects",
                        value = if (dashboardState.isLoading) "..." else dashboardState.enrolledSubjects.toString(),
                        icon = Icons.Default.Menu,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Average Grade",
                        value = if (dashboardState.isLoading) "..." else String.format("%.1f", dashboardState.averageGrade),
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                // Grade Period Overview
                if (dashboardState.recentGrades.isNotEmpty()) {
                    Text(
                        text = "Recent Grade Updates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (dashboardState.recentGrades.isNotEmpty()) {
                items(dashboardState.recentGrades.take(3)) { grade ->
                    RecentGradeCard(
                        grade = grade,
                        onClick = { onNavigateToGrades() }
                    )
                }
            }
            
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No recent activity",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your recent grades and updates will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentGradeCard(
    grade: Grade,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grade Period Icon
            Icon(
                imageVector = when (grade.gradePeriod) {
                    GradePeriod.PRELIM -> Icons.Default.Schedule
                    GradePeriod.MIDTERM -> Icons.Default.Star
                    GradePeriod.FINAL -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (grade.gradePeriod) {
                    GradePeriod.PRELIM -> MaterialTheme.colorScheme.primary
                    GradePeriod.MIDTERM -> MaterialTheme.colorScheme.secondary
                    GradePeriod.FINAL -> MaterialTheme.colorScheme.tertiary
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = grade.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${grade.gradePeriod.displayName} - ${String.format("%.1f", grade.percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Grade Score
            Text(
                text = "${grade.score}/${grade.maxScore}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    grade.percentage >= 90 -> MaterialTheme.colorScheme.primary
                    grade.percentage >= 75 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.teacher.TeacherDashboardScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToSubjects: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {},
    onNavigateToCourseManagement: () -> Unit = {},
    onNavigateToYearLevelManagement: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToGradeMonitoring: () -> Unit = {},
    onNavigateToAcademicPeriods: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AdminStatsCard(
                        title = "System Overview",
                        stats = listOf(
                            "Total Subjects" to uiState.totalSubjects.toString(),
                            "Active Subjects" to uiState.activeSubjects.toString(),
                            "Total Students" to uiState.totalStudents.toString(),
                            "Total Teachers" to uiState.totalTeachers.toString(),
                            "Total Enrollments" to uiState.totalEnrollments.toString(),
                            "Pending Applications" to uiState.pendingApplications.toString()
                        )
                    )
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToSubjects,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Manage Subjects")
                            }
                            Button(
                                onClick = onNavigateToApplications,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Applications")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToCourseManagement,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Courses")
                            }
                            Button(
                                onClick = onNavigateToYearLevelManagement,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Year Levels")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToUsers,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Manage Users")
                            }
                            Button(
                                onClick = onNavigateToGradeMonitoring,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Grade Monitoring")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToAcademicPeriods,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Academic Periods")
                            }
                            Button(
                                onClick = onNavigateToProfile,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Profile")
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { viewModel.refreshData() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Refresh Data")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatsCard(
    title: String,
    stats: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            stats.chunked(2).forEach { rowStats ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowStats.forEach { (label, value) ->
                        StatItem(
                            label = label,
                            value = value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
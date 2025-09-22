package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToSubjects: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToCourseManagement: () -> Unit,
    onNavigateToYearLevelManagement: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboardData()
    }

    // Refresh data when screen is composed (e.g., when navigating back)
    DisposableEffect(Unit) {
        dashboardViewModel.refreshData()
        onDispose { }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(
                        onClick = { dashboardViewModel.refreshData() },
                        enabled = !dashboardState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
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
                            text = "Admin ${currentUser?.firstName} ${currentUser?.lastName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manage the academic system and oversee operations",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Management Actions",
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
                        title = "Manage Subjects",
                        icon = Icons.Default.Menu,
                        onClick = onNavigateToSubjects,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Applications",
                        icon = Icons.Default.List,
                        onClick = onNavigateToApplications,
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
                        title = "Manage Courses",
                        icon = Icons.Default.School,
                        onClick = onNavigateToCourseManagement,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Manage Year Levels",
                        icon = Icons.Default.List,
                        onClick = onNavigateToYearLevelManagement,
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
                        title = "Manage Users",
                        icon = Icons.Default.Person,
                        onClick = onNavigateToUsers,
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionCard(
                        title = "Analytics",
                        icon = Icons.Default.Info,
                        onClick = { /* TODO: Navigate to analytics */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Text(
                    text = "System Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                // Statistics Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Students",
                        value = if (dashboardState.isLoading) "..." else dashboardState.totalStudents.toString(),
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Total Teachers",
                        value = if (dashboardState.isLoading) "..." else dashboardState.totalTeachers.toString(),
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Active Subjects",
                        value = if (dashboardState.isLoading) "..." else dashboardState.activeSubjects.toString(),
                        icon = Icons.Default.Menu,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Pending Applications",
                        value = if (dashboardState.isLoading) "..." else dashboardState.pendingApplications.toString(),
                        icon = Icons.Default.List,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Enrollments",
                        value = if (dashboardState.isLoading) "..." else dashboardState.totalEnrollments.toString(),
                        icon = Icons.Default.School,
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatCard(
                        title = "Available Subjects",
                        value = if (dashboardState.isLoading) "..." else (dashboardState.activeSubjects - dashboardState.pendingApplications).toString(),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
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
                            text = "System activities and updates will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Error Message
            dashboardState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
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

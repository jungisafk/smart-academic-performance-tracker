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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.common.NoActiveAcademicPeriodCard
import com.smartacademictracker.presentation.common.NotificationIconWithBadge
import com.smartacademictracker.presentation.notification.NotificationViewModel

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
    onNavigateToGradeEditRequests: () -> Unit = {},
    onNavigateToAcademicPeriods: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPreRegistered: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    
    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
        notificationViewModel.loadNotifications()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Show loading state when data is loading
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
                        Column(
                            modifier = Modifier.weight(1f)
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
                        
                        NotificationIconWithBadge(
                            unreadCount = unreadCount,
                            onClick = onNavigateToNotifications
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
                        
                        // Calculate responsive height for System Overview grid
                        val systemOverviewCardCount = 6
                        val systemOverviewColumns = 2
                        val systemOverviewRows = (systemOverviewCardCount + systemOverviewColumns - 1) / systemOverviewColumns
                        val systemOverviewCardHeight = 100.dp // Height of each SystemOverviewCard
                        val systemOverviewVerticalSpacing = 12.dp * (systemOverviewRows - 1)
                        val systemOverviewGridHeight = (systemOverviewCardHeight * systemOverviewRows) + systemOverviewVerticalSpacing
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(systemOverviewGridHeight)
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
                        
                        // Calculate responsive height for the grid
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        val buttonCount = 9 // Updated: Added Grade Edit Requests button
                        val columns = 2
                        val rows = (buttonCount + columns - 1) / columns // Ceiling division
                        val horizontalPadding = 16.dp * 2 // Left and right padding
                        val horizontalSpacing = 12.dp * (columns - 1) // Spacing between columns
                        val buttonWidth = (screenWidth - horizontalPadding - horizontalSpacing) / columns
                        val buttonHeight = buttonWidth / 1.5f // Aspect ratio 1.5:1 (more compact)
                        val verticalSpacing = 12.dp * (rows - 1) // Spacing between rows
                        val gridHeight = (buttonHeight * rows) + verticalSpacing
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(gridHeight)
                        ) {
                            items(listOf(
                                QuickActionData("Teacher Management", onNavigateToApplications, Icons.Default.PersonAdd, true),
                                QuickActionData("Academic Structure", onNavigateToHierarchicalAcademicManagement, Icons.Default.MenuBook, false),
                                QuickActionData("Manage Users", onNavigateToUsers, Icons.Default.Person, false),
                                QuickActionData("Grade Monitoring", onNavigateToGradeMonitoring, Icons.Default.BarChart, false),
                                QuickActionData("Grade Edit Requests", onNavigateToGradeEditRequests, Icons.Default.Edit, false),
                                QuickActionData("Academic Periods", onNavigateToAcademicPeriods, Icons.Default.CalendarToday, false),
                                QuickActionData("Pre-Register", onNavigateToPreRegistered, Icons.Default.School, false),
                                QuickActionData("Notifications", onNavigateToNotifications, Icons.Default.Notifications, false),
                                QuickActionData("Profile", onNavigateToProfile, Icons.Default.Person, false)
                            )) { actionData ->
                                val badgeCount = when (actionData.title) {
                                    "Teacher Management" -> uiState.pendingTeacherApplications
                                    "Grade Edit Requests" -> uiState.pendingGradeEditRequests
                                    "Notifications" -> unreadCount
                                    else -> 0
                                }
                                QuickActionButton(
                                    title = actionData.title,
                                    onClick = actionData.onClick,
                                    icon = actionData.icon,
                                    isYellow = actionData.isYellow,
                                    badgeCount = badgeCount,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isSmallScreen = screenWidth < 360 // Small phones
    val isMediumScreen = screenWidth < 600 // Medium phones/small tablets
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isSmallScreen) 12.dp else 20.dp,
                vertical = if (isSmallScreen) 16.dp else 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Academic Period",
                    style = if (isSmallScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (activePeriod.isNotEmpty()) {
                // Use responsive layout: stack vertically on small screens, horizontal on larger
                if (isSmallScreen) {
                    // Stack vertically on very small screens
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoItem(
                            label = "Academic Year",
                            value = currentAcademicYear,
                            isSmallScreen = isSmallScreen,
                            isMediumScreen = isMediumScreen
                        )
                        InfoItem(
                            label = "Semester",
                            value = currentSemester,
                            isSmallScreen = isSmallScreen,
                            isMediumScreen = isMediumScreen
                        )
                        StatusItem(isSmallScreen = isSmallScreen)
                    }
                } else {
                    // Use horizontal layout with proper spacing on larger screens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        InfoItem(
                            label = "Academic Year",
                            value = currentAcademicYear,
                            isSmallScreen = isSmallScreen,
                            isMediumScreen = isMediumScreen,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(if (isMediumScreen) 8.dp else 16.dp))
                        
                        InfoItem(
                            label = "Semester",
                            value = currentSemester,
                            isSmallScreen = isSmallScreen,
                            isMediumScreen = isMediumScreen,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(if (isMediumScreen) 8.dp else 16.dp))
                        
                        StatusItem(
                            isSmallScreen = isSmallScreen,
                            modifier = Modifier.weight(1f)
                        )
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
private fun InfoItem(
    label: String,
    value: String,
    isSmallScreen: Boolean,
    isMediumScreen: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = if (isSmallScreen) {
                MaterialTheme.typography.titleMedium
            } else if (isMediumScreen) {
                MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
            } else {
                MaterialTheme.typography.headlineSmall
            },
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatusItem(
    isSmallScreen: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Status",
            style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
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
                style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isYellow: Boolean = false,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier)
            .aspectRatio(1.5f), // More compact aspect ratio
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isYellow) Color(0xFFFFC107) else Color(0xFF2196F3)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isYellow) Color(0xFF333333) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                // Badge showing count
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-3).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Medium,
                color = if (isYellow) Color(0xFF333333) else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 13.sp,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
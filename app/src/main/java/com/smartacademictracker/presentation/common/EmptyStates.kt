package com.smartacademictracker.presentation.common

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Comprehensive empty state components for different scenarios
 */

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            actionText?.let { text ->
                onAction?.let { action ->
                    Button(
                        onClick = action,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGrades(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Grades Yet",
        message = "Your grades will appear here once your teachers start recording them. Check back later!",
        icon = Icons.Default.Star,
        actionText = "Refresh",
        onAction = onRefresh,
        modifier = modifier
    )
}

@Composable
fun EmptyStudents(
    onAddStudent: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Students Found",
        message = "There are no students enrolled in this subject yet. Add students to get started.",
        icon = Icons.Default.Person,
        actionText = "Add Students",
        onAction = onAddStudent,
        modifier = modifier
    )
}

@Composable
fun EmptySubjects(
    onAddSubject: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Subjects Available",
        message = "There are no subjects available yet. Create your first subject to get started.",
        icon = Icons.Default.Book,
        actionText = "Add Subject",
        onAction = onAddSubject,
        modifier = modifier
    )
}

@Composable
fun EmptyApplications(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Applications",
        message = "There are no pending applications at the moment. New applications will appear here.",
        icon = Icons.Default.Assignment,
        actionText = "Refresh",
        onAction = onRefresh,
        modifier = modifier
    )
}

@Composable
fun EmptyNotifications(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Notifications",
        message = "You're all caught up! New notifications will appear here when they arrive.",
        icon = Icons.Default.Notifications,
        modifier = modifier
    )
}

@Composable
fun EmptySearchResults(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Results Found",
        message = "No results found for \"$searchQuery\". Try adjusting your search terms or filters.",
        icon = Icons.Default.Search,
        actionText = "Clear Search",
        onAction = onClearSearch,
        modifier = modifier
    )
}

@Composable
fun EmptyOfflineData(
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Offline Data",
        message = "You haven't downloaded any data for offline use yet. Sync your data to access it offline.",
        icon = Icons.Default.OfflinePin,
        actionText = "Sync Now",
        onAction = onSync,
        modifier = modifier
    )
}

@Composable
fun EmptyAnalytics(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Data to Display",
        message = "Analytics will appear here once you have enough data to generate insights.",
        icon = Icons.Default.Analytics,
        modifier = modifier
    )
}

@Composable
fun EmptyPerformanceTracking(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Performance Data",
        message = "Your performance tracking will appear here once you have grades to track.",
        icon = Icons.Default.TrendingUp,
        actionText = "Refresh",
        onAction = onRefresh,
        modifier = modifier
    )
}

@Composable
fun EmptySubmissionTracking(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Submissions to Track",
        message = "Grade submission tracking will appear here once students are enrolled and grades are being recorded.",
        icon = Icons.Default.AssignmentTurnedIn,
        modifier = modifier
    )
}

@Composable
fun EmptyAuditTrail(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Audit Records",
        message = "Audit trail will appear here once grade changes are made in the system.",
        icon = Icons.Default.History,
        modifier = modifier
    )
}

@Composable
fun EmptyUserManagement(
    onAddUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Users Found",
        message = "No users are registered in the system yet. Add users to get started.",
        icon = Icons.Default.People,
        actionText = "Add User",
        onAction = onAddUser,
        modifier = modifier
    )
}

@Composable
fun EmptyAcademicPeriods(
    onAddPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Academic Periods",
        message = "No academic periods are configured yet. Set up your academic periods to organize grades by semester or year.",
        icon = Icons.Default.CalendarMonth,
        actionText = "Add Period",
        onAction = onAddPeriod,
        modifier = modifier
    )
}

@Composable
fun EmptyGradeMonitoring(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Grade Data",
        message = "Grade monitoring data will appear here once teachers start recording grades.",
        icon = Icons.Default.Monitor,
        actionText = "Refresh",
        onAction = onRefresh,
        modifier = modifier
    )
}

@Composable
fun EmptyBatchGrades(
    onStartBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Batch Grades",
        message = "Start entering grades for multiple students at once to use batch grade input.",
        icon = Icons.Default.Group,
        actionText = "Start Batch Input",
        onAction = onStartBatch,
        modifier = modifier
    )
}

@Composable
fun EmptyStateWithIllustration(
    title: String,
    message: String,
    illustration: @Composable () -> Unit,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            illustration()
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            actionText?.let { text ->
                onAction?.let { action ->
                    Button(
                        onClick = action,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text)
                    }
                }
            }
        }
    }
}

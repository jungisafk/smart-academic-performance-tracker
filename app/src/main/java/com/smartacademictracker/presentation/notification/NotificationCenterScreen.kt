package com.smartacademictracker.presentation.notification

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
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.presentation.common.FullScreenLoading
import com.smartacademictracker.presentation.common.FullScreenError
import com.smartacademictracker.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNotification: (String) -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                    }
                    IconButton(onClick = { viewModel.refreshNotifications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                FullScreenLoading()
            }
            uiState.error != null -> {
                val errorMessage = uiState.error ?: "Unknown error occurred"
                FullScreenError(
                    error = errorMessage,
                    onRetry = { viewModel.loadNotifications() }
                )
            }
            notifications.isEmpty() -> {
                EmptyState(
                    title = "No Notifications",
                    message = "You don't have any notifications yet.",
                    icon = Icons.Default.Notifications
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onNotificationClick = { 
                                viewModel.markAsRead(notification.id)
                                onNavigateToNotification(notification.id)
                            },
                            onDeleteClick = { viewModel.deleteNotification(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = onNotificationClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = getNotificationIcon(notification.type),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = getNotificationColor(notification.type)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatNotificationDate(notification.createdAt.seconds * 1000),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (!notification.isRead) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("New", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getNotificationIcon(type: NotificationType) = when (type) {
    NotificationType.GRADE_UPDATE -> Icons.Default.Grade
    NotificationType.APPLICATION_APPROVED -> Icons.Default.CheckCircle
    NotificationType.APPLICATION_REJECTED -> Icons.Default.Cancel
    NotificationType.DEADLINE_REMINDER -> Icons.Default.Schedule
    NotificationType.SYSTEM_ANNOUNCEMENT -> Icons.Default.Announcement
    NotificationType.PERFORMANCE_ALERT -> Icons.Default.Warning
    NotificationType.GRADE_SUBMISSION_DEADLINE -> Icons.Default.Assignment
    NotificationType.ACADEMIC_PERIOD_ACTIVATED -> Icons.Default.School
    else -> Icons.Default.Notifications
}

@Composable
private fun getNotificationColor(type: NotificationType) = when (type) {
    NotificationType.GRADE_UPDATE -> MaterialTheme.colorScheme.primary
    NotificationType.APPLICATION_APPROVED -> MaterialTheme.colorScheme.tertiary
    NotificationType.APPLICATION_REJECTED -> MaterialTheme.colorScheme.error
    NotificationType.DEADLINE_REMINDER -> MaterialTheme.colorScheme.secondary
    NotificationType.SYSTEM_ANNOUNCEMENT -> MaterialTheme.colorScheme.primary
    NotificationType.PERFORMANCE_ALERT -> MaterialTheme.colorScheme.error
    NotificationType.GRADE_SUBMISSION_DEADLINE -> MaterialTheme.colorScheme.secondary
    NotificationType.ACADEMIC_PERIOD_ACTIVATED -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatNotificationDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> "${diff / 604800000}w ago"
    }
}

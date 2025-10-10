package com.smartacademictracker.presentation.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Notification
import com.smartacademictracker.data.model.NotificationType
import com.smartacademictracker.data.model.NotificationPriority
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { 
                Text(
                    text = "Notifications${if (unreadCount > 0) " ($unreadCount)" else ""}"
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.markAllAsRead() }) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                }
                IconButton(onClick = { viewModel.clearAllNotifications() }) {
                    Icon(Icons.Default.ClearAll, contentDescription = "Clear all")
                }
                IconButton(onClick = { viewModel.createTestNotifications() }) {
                    Icon(Icons.Default.Add, contentDescription = "Create test notifications")
                }
                IconButton(onClick = { viewModel.loadNotifications() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "You'll see notifications here when they arrive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = { viewModel.markAsRead(notification.id) },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }

        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Handle error display
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 2.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notification icon based on type
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = getNotificationColor(notification.type),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Unread indicator
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(notification.createdAt.seconds * 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    if (!notification.isRead) {
                        TextButton(onClick = onMarkAsRead) {
                            Text("Mark as read")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Priority indicator
            if (notification.priority == NotificationPriority.HIGH || notification.priority == NotificationPriority.URGENT) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = notification.priority.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun getNotificationIcon(type: NotificationType) = when (type) {
    NotificationType.GRADE_UPDATE, NotificationType.GRADE_REMINDER -> Icons.Default.Grade
    NotificationType.APPLICATION_APPROVED, NotificationType.SUBJECT_APPLICATION_APPROVED -> Icons.Default.CheckCircle
    NotificationType.APPLICATION_REJECTED, NotificationType.SUBJECT_APPLICATION_REJECTED -> Icons.Default.Cancel
    NotificationType.STUDENT_LEFT_CLASS, NotificationType.STUDENT_KICKED_FROM_CLASS -> Icons.Default.PersonRemove
    NotificationType.TEACHER_KICKED_STUDENT -> Icons.Default.AdminPanelSettings
    NotificationType.ALL_GRADES_SUBMITTED, NotificationType.GRADE_COMPLETION_NOTIFICATION -> Icons.Default.AssignmentTurnedIn
    NotificationType.DEADLINE_REMINDER -> Icons.Default.Schedule
    NotificationType.SYSTEM_ANNOUNCEMENT -> Icons.Default.Campaign
    NotificationType.PERFORMANCE_ALERT -> Icons.Default.Warning
    else -> Icons.Default.Notifications
}

@Composable
private fun getNotificationColor(type: NotificationType) = when (type) {
    NotificationType.GRADE_UPDATE, NotificationType.GRADE_REMINDER -> MaterialTheme.colorScheme.primary
    NotificationType.APPLICATION_APPROVED, NotificationType.SUBJECT_APPLICATION_APPROVED -> MaterialTheme.colorScheme.primary
    NotificationType.APPLICATION_REJECTED, NotificationType.SUBJECT_APPLICATION_REJECTED -> MaterialTheme.colorScheme.error
    NotificationType.STUDENT_LEFT_CLASS, NotificationType.STUDENT_KICKED_FROM_CLASS -> MaterialTheme.colorScheme.secondary
    NotificationType.TEACHER_KICKED_STUDENT -> MaterialTheme.colorScheme.tertiary
    NotificationType.ALL_GRADES_SUBMITTED, NotificationType.GRADE_COMPLETION_NOTIFICATION -> MaterialTheme.colorScheme.primary
    NotificationType.DEADLINE_REMINDER -> MaterialTheme.colorScheme.secondary
    NotificationType.SYSTEM_ANNOUNCEMENT -> MaterialTheme.colorScheme.primary
    NotificationType.PERFORMANCE_ALERT -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

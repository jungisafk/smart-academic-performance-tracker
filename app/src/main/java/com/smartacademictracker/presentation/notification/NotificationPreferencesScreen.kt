package com.smartacademictracker.presentation.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.NotificationPreferences
import com.smartacademictracker.presentation.common.FullScreenLoading
import com.smartacademictracker.presentation.common.FullScreenError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPreferences()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.savePreferences() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
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
                    onRetry = { viewModel.loadPreferences() }
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // General Notification Settings
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "General Settings",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            SwitchPreference(
                                title = "Email Notifications",
                                subtitle = "Receive notifications via email",
                                checked = preferences.emailNotifications,
                                onCheckedChange = { viewModel.updateEmailNotifications(it) }
                            )
                            
                            SwitchPreference(
                                title = "Push Notifications",
                                subtitle = "Receive push notifications on your device",
                                checked = preferences.pushNotifications,
                                onCheckedChange = { viewModel.updatePushNotifications(it) }
                            )
                            
                            SwitchPreference(
                                title = "In-App Notifications",
                                subtitle = "Show notifications within the app",
                                checked = preferences.inAppNotifications,
                                onCheckedChange = { viewModel.updateInAppNotifications(it) }
                            )
                        }
                    }

                    // Notification Types
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Notification Types",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            SwitchPreference(
                                title = "Grade Updates",
                                subtitle = "Get notified when grades are updated",
                                checked = preferences.gradeUpdateNotifications,
                                onCheckedChange = { viewModel.updateGradeUpdateNotifications(it) }
                            )
                            
                            SwitchPreference(
                                title = "Application Status",
                                subtitle = "Get notified about application approvals/rejections",
                                checked = preferences.applicationStatusNotifications,
                                onCheckedChange = { viewModel.updateApplicationStatusNotifications(it) }
                            )
                            
                            SwitchPreference(
                                title = "Deadline Reminders",
                                subtitle = "Get reminded about upcoming deadlines",
                                checked = preferences.deadlineReminderNotifications,
                                onCheckedChange = { viewModel.updateDeadlineReminderNotifications(it) }
                            )
                            
                            SwitchPreference(
                                title = "System Announcements",
                                subtitle = "Receive system-wide announcements",
                                checked = preferences.systemAnnouncementNotifications,
                                onCheckedChange = { viewModel.updateSystemAnnouncementNotifications(it) }
                            )
                            
                            SwitchPreference(
                                title = "Performance Alerts",
                                subtitle = "Get alerts about performance issues",
                                checked = preferences.performanceAlertNotifications,
                                onCheckedChange = { viewModel.updatePerformanceAlertNotifications(it) }
                            )
                        }
                    }

                    // Quiet Hours
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Quiet Hours",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Set quiet hours to avoid notifications during specific times",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Start Time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                OutlinedTextField(
                                    value = preferences.quietHoursStart ?: "",
                                    onValueChange = { viewModel.updateQuietHoursStart(it) },
                                    placeholder = { Text("HH:mm") },
                                    modifier = Modifier.width(120.dp),
                                    singleLine = true
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "End Time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                OutlinedTextField(
                                    value = preferences.quietHoursEnd ?: "",
                                    onValueChange = { viewModel.updateQuietHoursEnd(it) },
                                    placeholder = { Text("HH:mm") },
                                    modifier = Modifier.width(120.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

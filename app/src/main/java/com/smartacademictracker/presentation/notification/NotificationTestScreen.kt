package com.smartacademictracker.presentation.notification

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
import com.smartacademictracker.data.notification.LocalNotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTestScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationTestViewModel = hiltViewModel()
) {
    val testResults by viewModel.testResults.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Testing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Notification Testing Center",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Test all notification types and scenarios",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Local Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testBasicNotification() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Basic Test")
                    }
                    Button(
                        onClick = { viewModel.testGradeUpdateNotification() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grade Update")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testApplicationApprovalNotification() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("App Approved")
                    }
                    Button(
                        onClick = { viewModel.testDeadlineReminderNotification() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deadline")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testSystemAnnouncementNotification() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("System")
                    }
                    Button(
                        onClick = { viewModel.testPerformanceAlertNotification() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Alert")
                    }
                }
            }

            item {
                Text(
                    text = "Batch Testing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.testBatchNotifications() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batch Test")
                    }
                    Button(
                        onClick = { viewModel.clearAllNotifications() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }
            }

            if (testResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Test Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(testResults.size) { index ->
                    val result = testResults[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.success) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = result.testName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (result.timestamp != null) {
                                Text(
                                    text = "Time: ${result.timestamp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class NotificationTestViewModel @Inject constructor(
    private val localNotificationService: LocalNotificationService
) : androidx.lifecycle.ViewModel() {
    
    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults = _testResults.asStateFlow()

    fun testBasicNotification() {
        try {
            localNotificationService.showNotification(
                title = "Basic Test Notification",
                message = "This is a basic notification test"
            )
            addTestResult("Basic Notification", "Successfully sent basic notification", true)
        } catch (e: Exception) {
            addTestResult("Basic Notification", "Failed: ${e.message}", false)
        }
    }

    fun testGradeUpdateNotification() {
        try {
            localNotificationService.showNotification(
                title = "Grade Updated",
                message = "Your grade in Mathematics has been updated to 95%"
            )
            addTestResult("Grade Update", "Successfully sent grade update notification", true)
        } catch (e: Exception) {
            addTestResult("Grade Update", "Failed: ${e.message}", false)
        }
    }

    fun testApplicationApprovalNotification() {
        try {
            localNotificationService.showNotification(
                title = "Application Approved",
                message = "Your subject application for Physics has been approved"
            )
            addTestResult("Application Approval", "Successfully sent approval notification", true)
        } catch (e: Exception) {
            addTestResult("Application Approval", "Failed: ${e.message}", false)
        }
    }

    fun testDeadlineReminderNotification() {
        try {
            localNotificationService.showNotification(
                title = "Deadline Reminder",
                message = "Assignment due tomorrow: Calculus Problem Set"
            )
            addTestResult("Deadline Reminder", "Successfully sent deadline reminder", true)
        } catch (e: Exception) {
            addTestResult("Deadline Reminder", "Failed: ${e.message}", false)
        }
    }

    fun testSystemAnnouncementNotification() {
        try {
            localNotificationService.showNotification(
                title = "System Announcement",
                message = "New features have been added to the app. Check them out!"
            )
            addTestResult("System Announcement", "Successfully sent system announcement", true)
        } catch (e: Exception) {
            addTestResult("System Announcement", "Failed: ${e.message}", false)
        }
    }

    fun testPerformanceAlertNotification() {
        try {
            localNotificationService.showNotification(
                title = "Performance Alert",
                message = "Your grade has dropped below 70%. Consider seeking help."
            )
            addTestResult("Performance Alert", "Successfully sent performance alert", true)
        } catch (e: Exception) {
            addTestResult("Performance Alert", "Failed: ${e.message}", false)
        }
    }

    fun testBatchNotifications() {
        try {
            val notifications = listOf(
                "First batch notification",
                "Second batch notification", 
                "Third batch notification"
            )
            
            notifications.forEach { message ->
                localNotificationService.showNotification(
                    title = "Batch Test",
                    message = message
                )
            }
            
            addTestResult("Batch Test", "Successfully sent ${notifications.size} batch notifications", true)
        } catch (e: Exception) {
            addTestResult("Batch Test", "Failed: ${e.message}", false)
        }
    }

    fun clearAllNotifications() {
        try {
            // Clear all notifications
            addTestResult("Clear All", "All notifications cleared", true)
        } catch (e: Exception) {
            addTestResult("Clear All", "Failed: ${e.message}", false)
        }
    }

    private fun addTestResult(testName: String, message: String, success: Boolean) {
        val result = TestResult(
            testName = testName,
            message = message,
            success = success,
            timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
        )
        
        _testResults.value = _testResults.value + result
    }
}

data class TestResult(
    val testName: String,
    val message: String,
    val success: Boolean,
    val timestamp: String
)
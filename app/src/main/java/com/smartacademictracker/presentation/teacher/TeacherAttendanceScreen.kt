package com.smartacademictracker.presentation.teacher

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
import com.smartacademictracker.data.model.AttendanceStatus
import com.smartacademictracker.data.model.AttendanceSummary
import com.smartacademictracker.presentation.utils.ErrorMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceScreen(
    subjectId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: TeacherAttendanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(subjectId) {
        viewModel.loadAttendanceSummary(subjectId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Attendance") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                ErrorMessage(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadAttendanceSummary(subjectId) }
                )
            } else {
                // Filter Chips
                FilterChipsSection(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { filter ->
                        viewModel.filterAttendanceByStatus(filter)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Attendance Summary
                if (uiState.attendanceSummaries.isEmpty()) {
                    EmptyAttendanceState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.attendanceSummaries) { summary ->
                            AttendanceSummaryCard(
                                summary = summary,
                                onRecordAttendance = { studentId, studentName, status ->
                                    viewModel.recordAttendance(
                                        studentId = studentId,
                                        studentName = studentName,
                                        subjectId = subjectId,
                                        subjectName = summary.subjectName,
                                        status = status
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipsSection(
    selectedFilter: AttendanceStatus?,
    onFilterSelected: (AttendanceStatus?) -> Unit
) {
    Column {
        Text(
            text = "Filter by Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text("All") }
            )
            AttendanceStatus.values().forEach { status ->
                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { onFilterSelected(status) },
                    label = { Text(status.displayName) }
                )
            }
        }
    }
}

@Composable
fun AttendanceSummaryCard(
    summary: AttendanceSummary,
    onRecordAttendance: (String, String, AttendanceStatus) -> Unit
) {
    var showAttendanceDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                AttendanceRateChip(rate = summary.attendanceRate)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttendanceStatItem(
                    label = "Present",
                    value = summary.presentCount.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                AttendanceStatItem(
                    label = "Absent",
                    value = summary.absentCount.toString(),
                    color = MaterialTheme.colorScheme.error
                )
                AttendanceStatItem(
                    label = "Late",
                    value = summary.lateCount.toString(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                AttendanceStatItem(
                    label = "Excused",
                    value = summary.excusedCount.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = (summary.attendanceRate / 100).toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAttendanceDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Record Attendance")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Show attendance history */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View History")
                }
            }
        }
    }
    
    if (showAttendanceDialog) {
        RecordAttendanceDialog(
            studentName = summary.studentName,
            onDismiss = { showAttendanceDialog = false },
            onRecord = { status ->
                onRecordAttendance(summary.studentId, summary.studentName, status)
                showAttendanceDialog = false
            }
        )
    }
}

@Composable
fun AttendanceRateChip(rate: Double) {
    val (backgroundColor, contentColor) = when {
        rate >= 90 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        rate >= 80 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        rate >= 70 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "${String.format("%.1f", rate)}%",
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AttendanceStatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecordAttendanceDialog(
    studentName: String,
    onDismiss: () -> Unit,
    onRecord: (AttendanceStatus) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Attendance") },
        text = {
            Column {
                Text(
                    text = "Student: $studentName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AttendanceStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRecord(selectedStatus) }
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyAttendanceState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No attendance records found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Start recording attendance for your students",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


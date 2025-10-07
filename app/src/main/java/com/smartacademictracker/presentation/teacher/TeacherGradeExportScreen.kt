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
import com.smartacademictracker.data.model.ExportFormat
import com.smartacademictracker.data.model.ExportType
import com.smartacademictracker.presentation.utils.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeExportScreen(
    subjectId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: TeacherGradeExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Grades") },
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
            // Export Type Selection
            ExportTypeSection(
                selectedType = uiState.selectedType,
                onTypeSelected = { type ->
                    viewModel.setExportType(type)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export Format Selection
            ExportFormatSection(
                selectedFormat = uiState.selectedFormat,
                onFormatSelected = { format ->
                    viewModel.setExportFormat(format)
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Academic Period Selection
            AcademicPeriodSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Export Actions
            ExportActionsSection(
                uiState = uiState,
                onGenerateExport = {
                    viewModel.generateExport(
                        subjectId = subjectId,
                        exportType = uiState.selectedType,
                        exportFormat = uiState.selectedFormat,
                        academicYear = "2024-2025", // This should come from a state
                        semester = "1st Semester" // This should come from a state
                    )
                },
                onDownload = { viewModel.downloadExport() },
                onEmail = { viewModel.emailExport() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Messages
            if (uiState.downloadComplete || uiState.emailSent) {
                SuccessMessage(
                    message = uiState.downloadMessage ?: uiState.emailMessage ?: "",
                    onDismiss = { viewModel.clearMessages() }
                )
            }
            
            if (uiState.error != null) {
                ErrorMessageWithDismiss(
                    message = uiState.error ?: "Unknown error",
                    onDismiss = { viewModel.clearMessages() }
                )
            }
        }
    }
}

@Composable
fun ExportTypeSection(
    selectedType: ExportType,
    onTypeSelected: (ExportType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ExportType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = getExportTypeDescription(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportFormatSection(
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExportFormat.values().forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { onFormatSelected(format) },
                        label = { Text(format.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
fun AcademicPeriodSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Academic Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = "2024-2025",
                    onValueChange = { /* TODO: Handle academic year change */ },
                    label = { Text("Academic Year") },
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
                
                OutlinedTextField(
                    value = "1st Semester",
                    onValueChange = { /* TODO: Handle semester change */ },
                    label = { Text("Semester") },
                    modifier = Modifier.weight(1f),
                    readOnly = true
                )
            }
        }
    }
}

@Composable
fun ExportActionsSection(
    uiState: TeacherGradeExportUiState,
    onGenerateExport: () -> Unit,
    onDownload: () -> Unit,
    onEmail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Export Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onGenerateExport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Generate Export")
                    }
                    
                    if (uiState.exportData != null) {
                        Button(
                            onClick = onDownload,
                            enabled = !uiState.isDownloading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("Download")
                            }
                        }
                        
                        Button(
                            onClick = onEmail,
                            enabled = !uiState.isEmailing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isEmailing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("Email")
                            }
                        }
                    }
                }
            }
        }
    }
}



private fun getExportTypeDescription(type: ExportType): String {
    return when (type) {
        ExportType.SUBJECT_GRADES -> "Export grades for a specific subject"
        ExportType.CLASS_SUMMARY -> "Export summary for all subjects taught"
        ExportType.INDIVIDUAL_REPORTS -> "Export individual student reports"
        ExportType.COMPARATIVE_ANALYSIS -> "Export comparative analysis data"
    }
}

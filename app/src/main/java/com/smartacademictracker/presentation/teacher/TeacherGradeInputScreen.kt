package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.utils.GradeCalculationEngine
import com.smartacademictracker.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeInputScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    viewModel: TeacherGradeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val enrollments by viewModel.enrollments.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectAndStudents(subjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                subject?.let { subject ->
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading State
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Grade Period Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Grade Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradePeriod.values().forEach { period ->
                            FilterChip(
                                onClick = { viewModel.setSelectedPeriod(period) },
                                label = { 
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = period.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${(period.weight * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selectedPeriod == period) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                },
                                selected = selectedPeriod == period,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Students List
            if (enrollments.isEmpty()) {
                EmptyState(
                    title = "No students enrolled",
                    message = "There are no students enrolled in this subject yet.",
                    icon = Icons.Default.Person
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(enrollments) { enrollment ->
                        StudentGradeInputCard(
                            enrollment = enrollment,
                            selectedPeriod = selectedPeriod,
                            existingGrade = viewModel.getGradeForStudentAndPeriod(
                                enrollment.studentId, 
                                selectedPeriod
                            ),
                            gradeAggregate = viewModel.getStudentGradeAggregate(enrollment.studentId),
                            allGrades = grades.filter { it.studentId == enrollment.studentId },
                            isLoading = uiState.savingGrades.contains(enrollment.studentId),
                            onGradeChange = { studentId, gradeValue ->
                                viewModel.updateGradeForPeriod(studentId, selectedPeriod, gradeValue)
                            }
                        )
                    }
                }
            }
        }

        // Error Message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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

        // Success Message
        uiState.successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StudentGradeInputCard(
    enrollment: Enrollment,
    selectedPeriod: GradePeriod,
    existingGrade: Grade?,
    gradeAggregate: StudentGradeAggregate?,
    allGrades: List<Grade>,
    isLoading: Boolean,
    onGradeChange: (String, Double) -> Unit
) {
    var gradeValue by remember(existingGrade) { 
        mutableStateOf(existingGrade?.score?.toString() ?: "") 
    }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Student Header with Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = enrollment.studentName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student ID: ${enrollment.studentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Grade Status Indicator
                gradeAggregate?.let { aggregate ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (aggregate.status) {
                                GradeStatus.PASSING -> Icons.Default.CheckCircle
                                GradeStatus.AT_RISK -> Icons.Default.Warning
                                GradeStatus.FAILING -> Icons.Default.Error
                                GradeStatus.INCOMPLETE -> Icons.Default.Schedule
                            },
                            contentDescription = aggregate.status.displayName,
                            tint = when (aggregate.status) {
                                GradeStatus.PASSING -> Color(0xFF4CAF50)
                                GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                GradeStatus.FAILING -> Color(0xFFF44336)
                                GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = aggregate.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (aggregate.status) {
                                GradeStatus.PASSING -> Color(0xFF4CAF50)
                                GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                GradeStatus.FAILING -> Color(0xFFF44336)
                                GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All Period Grades Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GradePeriod.values().forEach { period ->
                    val periodGrade = allGrades.find { it.gradePeriod == period }
                    val isCurrentPeriod = period == selectedPeriod
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCurrentPeriod && periodGrade != null -> MaterialTheme.colorScheme.primaryContainer
                                isCurrentPeriod -> MaterialTheme.colorScheme.secondaryContainer
                                periodGrade != null -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isCurrentPeriod) {
                            androidx.compose.foundation.BorderStroke(
                                2.dp, 
                                MaterialTheme.colorScheme.primary
                            )
                        } else null
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = period.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrentPeriod) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${(period.weight * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (periodGrade != null) {
                                Text(
                                    text = GradeCalculationEngine.formatGrade(periodGrade.score, 1),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentPeriod) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    if (period != GradePeriod.FINAL) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Period Grade Input
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Enter ${selectedPeriod.displayName} Grade (${(selectedPeriod.weight * 100).toInt()}%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = gradeValue,
                            onValueChange = { 
                                gradeValue = it
                                isEditing = true
                            },
                            label = { Text("Grade (0-100)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            supportingText = {
                                val value = gradeValue.toDoubleOrNull()
                                when {
                                    value == null && gradeValue.isNotBlank() -> {
                                        Text(
                                            "Please enter a valid number",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    value != null && (value < 0 || value > 100) -> {
                                        Text(
                                            "Grade must be between 0 and 100",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    value != null -> {
                                        Text(
                                            "Letter Grade: ${GradeCalculationEngine.calculateLetterGrade(value)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            isError = gradeValue.isNotBlank() && 
                                     (gradeValue.toDoubleOrNull() == null || 
                                      gradeValue.toDoubleOrNull()?.let { it < 0 || it > 100 } == true)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Button(
                            onClick = {
                                val value = gradeValue.toDoubleOrNull()
                                if (value != null && value >= 0 && value <= 100) {
                                    onGradeChange(enrollment.studentId, value)
                                    isEditing = false
                                }
                            },
                            enabled = !isLoading && isEditing && 
                                     gradeValue.toDoubleOrNull()?.let { it in 0.0..100.0 } == true
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save Grade",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (existingGrade != null) "Update" else "Save")
                        }
                    }
                }
            }
            
            // Final Average Display (if available)
            gradeAggregate?.finalAverage?.let { finalAverage ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (gradeAggregate.status) {
                            GradeStatus.PASSING -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            GradeStatus.AT_RISK -> Color(0xFFFF9800).copy(alpha = 0.1f)
                            GradeStatus.FAILING -> Color(0xFFF44336).copy(alpha = 0.1f)
                            GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E).copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Final Average:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = GradeCalculationEngine.formatGrade(finalAverage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (gradeAggregate.status) {
                                    GradeStatus.PASSING -> Color(0xFF4CAF50)
                                    GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                    GradeStatus.FAILING -> Color(0xFFF44336)
                                    GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                                }
                            )
                            Surface(
                                color = when (gradeAggregate.status) {
                                    GradeStatus.PASSING -> Color(0xFF4CAF50)
                                    GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                    GradeStatus.FAILING -> Color(0xFFF44336)
                                    GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = gradeAggregate.letterGrade,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


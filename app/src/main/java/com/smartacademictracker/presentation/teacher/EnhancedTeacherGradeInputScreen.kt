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
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.OfflinePin
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
fun EnhancedTeacherGradeInputScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    viewModel: EnhancedTeacherGradeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val enrollments by viewModel.enrollments.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val batchMode by viewModel.batchMode.collectAsState()
    val offlineMode by viewModel.offlineMode.collectAsState()

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectAndStudents(subjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with enhanced controls
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
            
            // Mode indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (offlineMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.OfflinePin,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                if (batchMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Batch",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
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
            // Enhanced Grade Period Selection with validation
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                            text = "Select Grade Period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Batch mode toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Batch Mode",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = batchMode,
                                onCheckedChange = { viewModel.toggleBatchMode() }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradePeriod.values().forEach { period ->
                            val periodGrades = grades.filter { it.gradePeriod == period }
                            val isComplete = periodGrades.isNotEmpty() && 
                                periodGrades.all { it.score > 0 }
                            
                            FilterChip(
                                onClick = { viewModel.setSelectedPeriod(period) },
                                label = { 
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = period.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isComplete) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Complete",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
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
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = if (isComplete) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Students List with enhanced input
            if (enrollments.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "No Students",
                    message = "No students are enrolled in this subject yet."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(enrollments) { enrollment ->
                        val studentGrades = grades.filter { 
                            it.studentId == enrollment.studentId && it.gradePeriod == selectedPeriod 
                        }
                        val existingGrade = studentGrades.firstOrNull()
                        
                        EnhancedStudentGradeCard(
                            enrollment = enrollment,
                            existingGrade = existingGrade,
                            selectedPeriod = selectedPeriod,
                            batchMode = batchMode,
                            onGradeChange = { score, maxScore ->
                                viewModel.updateGrade(
                                    enrollment.studentId,
                                    selectedPeriod,
                                    score,
                                    maxScore
                                )
                            },
                            onSaveGrade = {
                                viewModel.saveGrade(enrollment.studentId, selectedPeriod)
                            },
                            onValidateGrade = { score, maxScore ->
                                viewModel.validateGrade(score, maxScore)
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
    }
}

@Composable
fun EnhancedStudentGradeCard(
    enrollment: Enrollment,
    existingGrade: Grade?,
    selectedPeriod: GradePeriod,
    batchMode: Boolean,
    onGradeChange: (Double, Double) -> Unit,
    onSaveGrade: () -> Unit,
    onValidateGrade: (Double, Double) -> ValidationResult
) {
    var score by remember { mutableStateOf(existingGrade?.score?.toString() ?: "") }
    var maxScore by remember { mutableStateOf(existingGrade?.maxScore?.toString() ?: "100") }
    var isEditing by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf(ValidationResult.Valid) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Student Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Student Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = enrollment.studentName.first().toString().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = enrollment.studentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${enrollment.studentId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Grade Status Indicator
                val gradeStatus = when {
                    existingGrade != null -> GradeStatusIndicator.COMPLETED
                    isEditing -> GradeStatusIndicator.EDITING
                    else -> GradeStatusIndicator.PENDING
                }
                
                GradeStatusIndicator(status = gradeStatus)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grade Input Section
            if (batchMode) {
                // Batch input mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Score input
                    OutlinedTextField(
                        value = score,
                        onValueChange = { newScore ->
                            score = newScore
                            val scoreValue = newScore.toDoubleOrNull() ?: 0.0
                            val maxScoreValue = maxScore.toDoubleOrNull() ?: 100.0
                            validationResult = onValidateGrade(scoreValue, maxScoreValue)
                        },
                        label = { Text("Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = validationResult != ValidationResult.Valid,
                        supportingText = if (validationResult != ValidationResult.Valid) {
                            { Text(validationResult.message) }
                        } else null
                    )
                    
                    // Max score input
                    OutlinedTextField(
                        value = maxScore,
                        onValueChange = { newMaxScore ->
                            maxScore = newMaxScore
                            val scoreValue = score.toDoubleOrNull() ?: 0.0
                            val maxScoreValue = newMaxScore.toDoubleOrNull() ?: 100.0
                            validationResult = onValidateGrade(scoreValue, maxScoreValue)
                        },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = validationResult != ValidationResult.Valid
                    )
                }
            } else {
                // Individual input mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = score,
                        onValueChange = { newScore ->
                            score = newScore
                            val scoreValue = newScore.toDoubleOrNull() ?: 0.0
                            val maxScoreValue = maxScore.toDoubleOrNull() ?: 100.0
                            validationResult = onValidateGrade(scoreValue, maxScoreValue)
                        },
                        label = { Text("Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = validationResult != ValidationResult.Valid,
                        supportingText = if (validationResult != ValidationResult.Valid) {
                            { Text(validationResult.message) }
                        } else null
                    )
                    
                    Text(
                        text = "/",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = maxScore,
                        onValueChange = { newMaxScore ->
                            maxScore = newMaxScore
                            val scoreValue = score.toDoubleOrNull() ?: 0.0
                            val maxScoreValue = newMaxScore.toDoubleOrNull() ?: 100.0
                            validationResult = onValidateGrade(scoreValue, maxScoreValue)
                        },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = validationResult != ValidationResult.Valid
                    )
                    
                    // Save button
                    FloatingActionButton(
                        onClick = {
                            val scoreValue = score.toDoubleOrNull() ?: 0.0
                            val maxScoreValue = maxScore.toDoubleOrNull() ?: 100.0
                            if (validationResult == ValidationResult.Valid) {
                                onGradeChange(scoreValue, maxScoreValue)
                                onSaveGrade()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (validationResult == ValidationResult.Valid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save Grade",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Grade percentage display
            if (score.isNotEmpty() && maxScore.isNotEmpty()) {
                val scoreValue = score.toDoubleOrNull() ?: 0.0
                val maxScoreValue = maxScore.toDoubleOrNull() ?: 100.0
                val percentage = if (maxScoreValue > 0) (scoreValue / maxScoreValue) * 100 else 0.0
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Percentage: ${String.format("%.1f", percentage)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            percentage >= 90 -> MaterialTheme.colorScheme.primary
                            percentage >= 75 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    
                    if (percentage > 0) {
                        Text(
                            text = GradeCalculationEngine.calculateLetterGrade(percentage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                percentage >= 90 -> MaterialTheme.colorScheme.primary
                                percentage >= 75 -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeStatusIndicator(status: GradeStatusIndicator) {
    Surface(
        color = when (status) {
            GradeStatusIndicator.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
            GradeStatusIndicator.EDITING -> MaterialTheme.colorScheme.secondaryContainer
            GradeStatusIndicator.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    GradeStatusIndicator.COMPLETED -> Icons.Default.CheckCircle
                    GradeStatusIndicator.EDITING -> Icons.Default.Schedule
                    GradeStatusIndicator.PENDING -> Icons.Default.Warning
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when (status) {
                    GradeStatusIndicator.COMPLETED -> MaterialTheme.colorScheme.primary
                    GradeStatusIndicator.EDITING -> MaterialTheme.colorScheme.secondary
                    GradeStatusIndicator.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (status) {
                    GradeStatusIndicator.COMPLETED -> "Completed"
                    GradeStatusIndicator.EDITING -> "Editing"
                    GradeStatusIndicator.PENDING -> "Pending"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class GradeStatusIndicator {
    COMPLETED,
    EDITING,
    PENDING
}

data class ValidationResult(
    val isValid: Boolean,
    val message: String
) {
    companion object {
        val Valid = ValidationResult(true, "")
        fun Invalid(message: String) = ValidationResult(false, message)
    }
}

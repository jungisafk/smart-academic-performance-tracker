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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Clear
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
import com.smartacademictracker.data.utils.GradeCalculationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchGradeInputScreen(
    subjectId: String,
    gradePeriod: GradePeriod,
    onNavigateBack: () -> Unit,
    viewModel: BatchGradeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val enrollments by viewModel.enrollments.collectAsState()
    val batchGrades by viewModel.batchGrades.collectAsState()
    val validationResults by viewModel.validationResults.collectAsState()

    LaunchedEffect(subjectId, gradePeriod) {
        viewModel.loadSubjectAndStudents(subjectId, gradePeriod)
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
                    text = "Batch Grade Input",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${subject?.name} - ${gradePeriod.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Batch actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.clearAllGrades() }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear All")
                }
                
                IconButton(
                    onClick = { viewModel.saveAllGrades() }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save All")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Batch input controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Batch Input Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Set all scores to same value
                    OutlinedTextField(
                        value = uiState.batchScore,
                        onValueChange = { viewModel.setBatchScore(it) },
                        label = { Text("Batch Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = uiState.batchMaxScore,
                        onValueChange = { viewModel.setBatchMaxScore(it) },
                        label = { Text("Max Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = { viewModel.applyBatchScores() },
                        enabled = uiState.batchScore.isNotEmpty() && uiState.batchMaxScore.isNotEmpty()
                    ) {
                        Text("Apply to All")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setAllToPassing() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set All to 75")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.setAllToExcellent() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set All to 90")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.clearAllGrades() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicator
        val completedCount = batchGrades.values.count { it.score > 0 }
        val totalCount = enrollments.size
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$completedCount / $totalCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Students list
        if (enrollments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students enrolled",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(enrollments) { enrollment ->
                    val batchGrade = batchGrades.values.find { it.studentId == enrollment.studentId }
                    val validationResult = validationResults[enrollment.studentId]
                    
                    BatchStudentGradeCard(
                        enrollment = enrollment,
                        batchGrade = batchGrade,
                        validationResult = validationResult,
                        onGradeChange = { score, maxScore ->
                            viewModel.updateBatchGrade(
                                enrollment.studentId,
                                score,
                                maxScore
                            )
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

@Composable
fun BatchStudentGradeCard(
    enrollment: Enrollment,
    batchGrade: BatchGrade?,
    validationResult: ValidationResult?,
    onGradeChange: (Double, Double) -> Unit
) {
    var score by remember { mutableStateOf(batchGrade?.score?.toString() ?: "") }
    var maxScore by remember { mutableStateOf(batchGrade?.maxScore?.toString() ?: "100") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Student Info
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
                
                // Status indicator
                val status = when {
                    batchGrade != null && batchGrade.score > 0 -> BatchGradeStatus.COMPLETED
                    validationResult != null && !validationResult.isValid -> BatchGradeStatus.ERROR
                    else -> BatchGradeStatus.PENDING
                }
                
                BatchGradeStatusIndicator(status = status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grade input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = score,
                    onValueChange = { newScore ->
                        score = newScore
                        val scoreValue = newScore.toDoubleOrNull() ?: 0.0
                        val maxScoreValue = maxScore.toDoubleOrNull() ?: 100.0
                        onGradeChange(scoreValue, maxScoreValue)
                    },
                    label = { Text("Score") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = validationResult?.isValid == false,
                    supportingText = if (validationResult?.isValid == false) {
                        { Text(validationResult.message) }
                    } else null
                )
                
                Text(
                    text = "/",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { newMaxScore ->
                        maxScore = newMaxScore
                        val scoreValue = score.toDoubleOrNull() ?: 0.0
                        val maxScoreValue = newMaxScore.toDoubleOrNull() ?: 100.0
                        onGradeChange(scoreValue, maxScoreValue)
                    },
                    label = { Text("Max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = validationResult?.isValid == false
                )
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
fun BatchGradeStatusIndicator(status: BatchGradeStatus) {
    Surface(
        color = when (status) {
            BatchGradeStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
            BatchGradeStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
            BatchGradeStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    BatchGradeStatus.COMPLETED -> Icons.Default.CheckCircle
                    BatchGradeStatus.ERROR -> Icons.Default.Error
                    BatchGradeStatus.PENDING -> Icons.Default.Schedule
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when (status) {
                    BatchGradeStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    BatchGradeStatus.ERROR -> MaterialTheme.colorScheme.error
                    BatchGradeStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (status) {
                    BatchGradeStatus.COMPLETED -> "Done"
                    BatchGradeStatus.ERROR -> "Error"
                    BatchGradeStatus.PENDING -> "Pending"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class BatchGradeStatus {
    COMPLETED,
    ERROR,
    PENDING
}

data class BatchGrade(
    val studentId: String,
    val score: Double,
    val maxScore: Double,
    val percentage: Double,
    val letterGrade: String
)

package com.smartacademictracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.utils.GradeCalculationEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubjectDetail: (String) -> Unit = {},
    viewModel: StudentGradesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadGrades()
    }

    // Refresh grades when screen is composed
    DisposableEffect(Unit) {
        viewModel.refreshGrades()
        onDispose { }
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
            Text(
                text = "My Grades",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.refreshGrades() },
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // Grades List
            if (gradeAggregates.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No grades found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your grades will appear here once your teachers start recording them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(gradeAggregates) { gradeAggregate ->
                        SubjectGradeCard(
                            gradeAggregate = gradeAggregate,
                            grades = viewModel.getGradesBySubject(gradeAggregate.subjectId),
                            onNavigateToSubject = { onNavigateToSubjectDetail(gradeAggregate.subjectId) }
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
fun GradeCard(
    grade: Grade,
    onNavigateToSubject: () -> Unit
) {
    Card(
        onClick = onNavigateToSubject,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = grade.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = grade.description.ifBlank { grade.gradePeriod.displayName },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Grade Badge
                Surface(
                    color = when {
                        grade.percentage >= 90 -> MaterialTheme.colorScheme.primaryContainer
                        grade.percentage >= 80 -> MaterialTheme.colorScheme.secondaryContainer
                        grade.percentage >= 70 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = grade.letterGrade,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            grade.percentage >= 90 -> MaterialTheme.colorScheme.onPrimaryContainer
                            grade.percentage >= 80 -> MaterialTheme.colorScheme.onSecondaryContainer
                            grade.percentage >= 70 -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Score: ${grade.score.toInt()}/${grade.maxScore.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Percentage: ${String.format("%.1f", grade.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Date: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(grade.dateRecorded))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SubjectGradeCard(
    gradeAggregate: StudentGradeAggregate,
    grades: List<Grade>,
    onNavigateToSubject: () -> Unit
) {
    Card(
        onClick = onNavigateToSubject,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Subject Header with Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = gradeAggregate.subjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Academic Year: ${gradeAggregate.academicYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (gradeAggregate.status) {
                            GradeStatus.PASSING -> Icons.Default.CheckCircle
                            GradeStatus.AT_RISK -> Icons.Default.Warning
                            GradeStatus.FAILING -> Icons.Default.Error
                            GradeStatus.INCOMPLETE -> Icons.Default.Schedule
                        },
                        contentDescription = gradeAggregate.status.displayName,
                        tint = when (gradeAggregate.status) {
                            GradeStatus.PASSING -> Color(0xFF4CAF50) // Green
                            GradeStatus.AT_RISK -> Color(0xFFFF9800) // Orange
                            GradeStatus.FAILING -> Color(0xFFF44336) // Red
                            GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E) // Gray
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = gradeAggregate.status.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (gradeAggregate.status) {
                            GradeStatus.PASSING -> Color(0xFF4CAF50)
                            GradeStatus.AT_RISK -> Color(0xFFFF9800)
                            GradeStatus.FAILING -> Color(0xFFF44336)
                            GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Individual Period Grades
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GradePeriod.values().forEach { period ->
                    val periodGrade = grades.find { it.gradePeriod == period }
                    GradePeriodCard(
                        period = period,
                        grade = periodGrade,
                        modifier = Modifier.weight(1f)
                    )
                    if (period != GradePeriod.FINAL) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Final Average Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (gradeAggregate.status) {
                        GradeStatus.PASSING -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        GradeStatus.AT_RISK -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        GradeStatus.FAILING -> Color(0xFFF44336).copy(alpha = 0.1f)
                        GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E).copy(alpha = 0.1f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Final Average",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = GradeCalculationEngine.formatGrade(gradeAggregate.finalAverage),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (gradeAggregate.status) {
                                GradeStatus.PASSING -> Color(0xFF4CAF50)
                                GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                GradeStatus.FAILING -> Color(0xFFF44336)
                                GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                            }
                        )
                        if (gradeAggregate.finalAverage != null) {
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
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    
                    // Completion Progress
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (gradeAggregate.getCompletionPercentage() / 100).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = when (gradeAggregate.status) {
                            GradeStatus.PASSING -> Color(0xFF4CAF50)
                            GradeStatus.AT_RISK -> Color(0xFFFF9800)
                            GradeStatus.FAILING -> Color(0xFFF44336)
                            GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                        },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${gradeAggregate.getCompletionPercentage().toInt()}% Complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GradePeriodCard(
    period: GradePeriod,
    grade: Grade?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (grade != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = period.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${(period.weight * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (grade != null) {
                Text(
                    text = GradeCalculationEngine.formatGrade(grade.score, 1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = grade.letterGrade,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Not graded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

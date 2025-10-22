package com.smartacademictracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.TrendingUp
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color(0xFF666666)
                            )
                        }
                        Text(
                            text = "My Grades",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.refreshGrades() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }
            
            // Summary Card
            if (!uiState.isLoading && (gradeAggregates.isNotEmpty() || grades.isNotEmpty())) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Academic Performance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = if (gradeAggregates.isNotEmpty()) {
                                        "${gradeAggregates.size} Subject${if (gradeAggregates.size != 1) "s" else ""}"
                                    } else {
                                        "${grades.size} Grade${if (grades.size != 1) "s" else ""}"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Track your academic progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            // Grade Icon
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Grade,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Loading State
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading grades...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            } else {
                // Grades List
                if (gradeAggregates.isEmpty() && grades.isEmpty()) {
                    // Empty State
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No grades found",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your grades will appear here once your teachers start recording them.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    if (gradeAggregates.isNotEmpty()) {
                        // Show grade aggregates (grouped by subject)
                        items(gradeAggregates) { gradeAggregate ->
                            EnhancedExpandableSubjectGradeCard(
                                gradeAggregate = gradeAggregate,
                                grades = viewModel.getGradesBySubject(gradeAggregate.subjectId)
                            )
                        }
                    } else {
                        // Group individual grades by subject and show as expandable cards
                        val groupedGrades = grades.groupBy { it.subjectId }
                        items(groupedGrades.entries.toList()) { (subjectId, subjectGrades) ->
                            val subjectName = subjectGrades.firstOrNull()?.subjectName ?: "Unknown Subject"
                            EnhancedExpandableSubjectSummaryCard(
                                subjectId = subjectId,
                                subjectName = subjectName,
                                grades = subjectGrades
                            )
                        }
                    }
                }
            }

            // Error Message
            uiState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeCard(
    grade: Grade
) {
    Card(
        // Non-clickable per requirement
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
    grades: List<Grade>
) {
    Card(
        // Non-clickable per requirement
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

@Composable
fun ExpandableSubjectGradeCard(
    gradeAggregate: StudentGradeAggregate,
    grades: List<Grade>
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
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
            // Subject Header with Average Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                
                // Average Grade Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = when (gradeAggregate.status) {
                            GradeStatus.PASSING -> MaterialTheme.colorScheme.primaryContainer
                            GradeStatus.AT_RISK -> MaterialTheme.colorScheme.secondaryContainer
                            GradeStatus.FAILING -> MaterialTheme.colorScheme.errorContainer
                            GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = String.format("%.1f", gradeAggregate.finalAverage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (gradeAggregate.status) {
                                GradeStatus.PASSING -> MaterialTheme.colorScheme.onPrimaryContainer
                                GradeStatus.AT_RISK -> MaterialTheme.colorScheme.onSecondaryContainer
                                GradeStatus.FAILING -> MaterialTheme.colorScheme.onErrorContainer
                                GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            // Expandable Individual Grades
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Individual Grades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Individual Period Grades
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GradePeriod.values().forEach { period ->
                        val periodGrade = grades.find { it.gradePeriod == period }
                        GradePeriodCard(
                            period = period,
                            grade = periodGrade
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSubjectSummaryCard(
    subjectId: String,
    subjectName: String,
    grades: List<Grade>
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Calculate average grade
    val averageGrade = if (grades.isNotEmpty()) {
        grades.map { it.percentage }.average()
    } else 0.0
    
    // Determine status based on average
    val status = when {
        averageGrade >= 90 -> GradeStatus.PASSING
        averageGrade >= 80 -> GradeStatus.PASSING
        averageGrade >= 70 -> GradeStatus.AT_RISK
        averageGrade > 0 -> GradeStatus.FAILING
        else -> GradeStatus.INCOMPLETE
    }
    
    Card(
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
            // Subject Header with Average Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${grades.size} grade${if (grades.size != 1) "s" else ""} recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Average Grade Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = when (status) {
                            GradeStatus.PASSING -> MaterialTheme.colorScheme.primaryContainer
                            GradeStatus.AT_RISK -> MaterialTheme.colorScheme.secondaryContainer
                            GradeStatus.FAILING -> MaterialTheme.colorScheme.errorContainer
                            GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = String.format("%.1f", averageGrade),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                GradeStatus.PASSING -> MaterialTheme.colorScheme.onPrimaryContainer
                                GradeStatus.AT_RISK -> MaterialTheme.colorScheme.onSecondaryContainer
                                GradeStatus.FAILING -> MaterialTheme.colorScheme.onErrorContainer
                                GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            // Expandable Individual Grades
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Individual Grades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show individual grades
                grades.sortedByDescending { it.dateRecorded }.forEach { grade ->
                    GradeCard(
                        grade = grade
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EnhancedExpandableSubjectGradeCard(
    gradeAggregate: StudentGradeAggregate,
    grades: List<Grade>
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Subject Header with Average Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = gradeAggregate.subjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "Academic Year: ${gradeAggregate.academicYear}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                
                // Average Grade Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (gradeAggregate.status) {
                                GradeStatus.PASSING -> Color(0xFFE8F5E8)
                                GradeStatus.AT_RISK -> Color(0xFFFFF3E0)
                                GradeStatus.FAILING -> Color(0xFFFFEBEE)
                                GradeStatus.INCOMPLETE -> Color(0xFFF5F5F5)
                            }
                        )
                    ) {
                        Text(
                            text = String.format("%.1f", gradeAggregate.finalAverage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (gradeAggregate.status) {
                                GradeStatus.PASSING -> Color(0xFF4CAF50)
                                GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                GradeStatus.FAILING -> Color(0xFFD32F2F)
                                GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }
            
            // Expandable Individual Grades
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Individual Grades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Individual Period Grades
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GradePeriod.values().forEach { period ->
                        val periodGrade = grades.find { it.gradePeriod == period }
                        EnhancedGradePeriodCard(
                            period = period,
                            grade = periodGrade
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedExpandableSubjectSummaryCard(
    subjectId: String,
    subjectName: String,
    grades: List<Grade>
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Calculate average grade
    val averageGrade = if (grades.isNotEmpty()) {
        grades.map { it.percentage }.average()
    } else 0.0
    
    // Determine status based on average
    val status = when {
        averageGrade >= 90 -> GradeStatus.PASSING
        averageGrade >= 80 -> GradeStatus.PASSING
        averageGrade >= 70 -> GradeStatus.AT_RISK
        averageGrade > 0 -> GradeStatus.FAILING
        else -> GradeStatus.INCOMPLETE
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Subject Header with Average Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "${grades.size} grade${if (grades.size != 1) "s" else ""} recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                
                // Average Grade Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (status) {
                                GradeStatus.PASSING -> Color(0xFFE8F5E8)
                                GradeStatus.AT_RISK -> Color(0xFFFFF3E0)
                                GradeStatus.FAILING -> Color(0xFFFFEBEE)
                                GradeStatus.INCOMPLETE -> Color(0xFFF5F5F5)
                            }
                        )
                    ) {
                        Text(
                            text = String.format("%.1f", averageGrade),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                GradeStatus.PASSING -> Color(0xFF4CAF50)
                                GradeStatus.AT_RISK -> Color(0xFFFF9800)
                                GradeStatus.FAILING -> Color(0xFFD32F2F)
                                GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }
            
            // Expandable Individual Grades
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Individual Grades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show individual grades
                grades.sortedByDescending { it.dateRecorded }.forEach { grade ->
                    EnhancedGradeCard(grade = grade)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EnhancedGradeCard(
    grade: Grade
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = grade.description.ifBlank { grade.gradePeriod.displayName },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
                
                // Grade Badge
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            grade.percentage >= 90 -> Color(0xFFE8F5E8)
                            grade.percentage >= 80 -> Color(0xFFE3F2FD)
                            grade.percentage >= 70 -> Color(0xFFFFF3E0)
                            else -> Color(0xFFFFEBEE)
                        }
                    )
                ) {
                    Text(
                        text = grade.letterGrade,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            grade.percentage >= 90 -> Color(0xFF4CAF50)
                            grade.percentage >= 80 -> Color(0xFF2196F3)
                            grade.percentage >= 70 -> Color(0xFFFF9800)
                            else -> Color(0xFFD32F2F)
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
                    color = Color(0xFF666666)
                )
                Text(
                    text = "Percentage: ${String.format("%.1f", grade.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Date: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(grade.dateRecorded))}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun EnhancedGradePeriodCard(
    period: GradePeriod,
    grade: Grade?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (grade != null) {
                Color(0xFFE3F2FD)
            } else {
                Color(0xFFF5F5F5)
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
                textAlign = TextAlign.Center,
                color = Color(0xFF333333)
            )
            Text(
                text = "${(period.weight * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (grade != null) {
                Text(
                    text = GradeCalculationEngine.formatGrade(grade.score, 1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Text(
                    text = grade.letterGrade,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color(0xFFE0E0E0),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF9E9E9E)
                    )
                }
                Text(
                    text = "Not graded",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

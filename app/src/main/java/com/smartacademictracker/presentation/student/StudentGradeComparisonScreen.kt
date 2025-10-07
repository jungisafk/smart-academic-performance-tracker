package com.smartacademictracker.presentation.student

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
import com.smartacademictracker.presentation.utils.getGradeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradeComparisonScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StudentGradeComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSubjectComparisons()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Comparison") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ComparisonSummaryCard(
                        totalSubjects = uiState.subjectComparisons.size,
                        bestSubject = uiState.bestSubject,
                        worstSubject = uiState.worstSubject,
                        overallAverage = uiState.overallAverage
                    )
                }
                
                if (uiState.subjectComparisons.isEmpty()) {
                    item {
                        EmptyComparisonCard()
                    }
                } else {
                    items(uiState.subjectComparisons) { comparison ->
                        SubjectComparisonCard(comparison = comparison)
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonSummaryCard(
    totalSubjects: Int,
    bestSubject: String,
    worstSubject: String,
    overallAverage: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat(
                    label = "Subjects",
                    value = totalSubjects.toString()
                )
                SummaryStat(
                    label = "Average",
                    value = String.format("%.2f", overallAverage)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BestWorstItem(
                    label = "Best",
                    subject = bestSubject,
                    isBest = true
                )
                BestWorstItem(
                    label = "Needs Improvement",
                    subject = worstSubject,
                    isBest = false
                )
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BestWorstItem(label: String, subject: String, isBest: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(
            text = subject,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubjectComparisonCard(comparison: SubjectComparison) {
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
                    text = comparison.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = String.format("%.2f", comparison.averageGrade),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = getGradeColor(comparison.averageGrade)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GradePeriodItem(
                    period = "Prelim",
                    grade = comparison.prelimGrade,
                    isPresent = comparison.prelimGrade != null
                )
                GradePeriodItem(
                    period = "Midterm",
                    grade = comparison.midtermGrade,
                    isPresent = comparison.midtermGrade != null
                )
                GradePeriodItem(
                    period = "Final",
                    grade = comparison.finalGrade,
                    isPresent = comparison.finalGrade != null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Status: ${comparison.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(comparison.status)
                )
                Text(
                    text = "Rank: #${comparison.rank}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GradePeriodItem(period: String, grade: Double?, isPresent: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = period,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isPresent) String.format("%.1f", grade ?: 0.0) else "N/A",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isPresent) getGradeColor(grade ?: 0.0) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyComparisonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Grade Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Subject comparisons will appear here once you have grades from multiple subjects.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun getStatusColor(status: String): androidx.compose.ui.graphics.Color {
    return when (status) {
        "Passing" -> MaterialTheme.colorScheme.primary
        "At Risk" -> MaterialTheme.colorScheme.tertiary
        "Failing" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

data class SubjectComparison(
    val subjectName: String,
    val averageGrade: Double,
    val prelimGrade: Double?,
    val midtermGrade: Double?,
    val finalGrade: Double?,
    val status: String,
    val rank: Int
)

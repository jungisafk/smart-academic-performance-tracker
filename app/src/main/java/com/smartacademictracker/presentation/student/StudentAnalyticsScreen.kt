package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.common.ChartUtils
import com.smartacademictracker.presentation.common.GradeProgressIndicator
import com.smartacademictracker.presentation.common.SubjectPerformanceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAnalyticsData()
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
                text = "Performance Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.refreshData() },
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall Performance Summary
                item {
                    OverallPerformanceCard(
                        totalSubjects = gradeAggregates.size,
                        averageGrade = uiState.overallAverage,
                        passingSubjects = uiState.passingSubjects,
                        atRiskSubjects = uiState.atRiskSubjects
                    )
                }
                
                // Subject Performance Cards
                items(gradeAggregates) { gradeAggregate ->
                    SubjectPerformanceCard(
                        subjectName = gradeAggregate.subjectName,
                        currentAverage = gradeAggregate.finalAverage,
                        prelimGrade = gradeAggregate.prelimGrade,
                        midtermGrade = gradeAggregate.midtermGrade,
                        finalGrade = gradeAggregate.finalGrade
                    )
                }
                
                // Grade Trend Chart (if we have multiple subjects)
                if (gradeAggregates.size > 1) {
                    item {
                        SubjectComparisonChart(
                            subjects = gradeAggregates.map { 
                                it.subjectName to (it.finalAverage ?: 0.0) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverallPerformanceCard(
    totalSubjects: Int,
    averageGrade: Double?,
    passingSubjects: Int,
    atRiskSubjects: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Overall Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            if (averageGrade != null) {
                GradeProgressIndicator(
                    currentGrade = averageGrade,
                    targetGrade = 75.0
                )
            } else {
                Text(
                    text = "No grades available yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Performance Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PerformanceStatItem("Total Subjects", totalSubjects.toString())
                PerformanceStatItem("Passing", passingSubjects.toString())
                PerformanceStatItem("At Risk", atRiskSubjects.toString())
            }
        }
    }
}

@Composable
private fun PerformanceStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
fun SubjectComparisonChart(
    subjects: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Subject Performance Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            ChartUtils.SubjectComparisonChart(
                subjects = subjects,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

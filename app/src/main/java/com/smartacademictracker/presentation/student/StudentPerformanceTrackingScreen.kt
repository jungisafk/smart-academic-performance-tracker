package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPerformanceTrackingScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentPerformanceTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val performanceData by viewModel.performanceData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPerformanceData()
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
                text = "Performance Tracking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overall Performance Summary
                item {
                    PerformanceSummaryCard(
                        overallAverage = performanceData.overallAverage,
                        trend = performanceData.trend,
                        improvement = performanceData.improvement
                    )
                }

                // Subject Performance Comparison
                item {
                    Text(
                        text = "Subject Performance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(performanceData.subjectPerformance) { subjectPerf ->
                    SubjectPerformanceCard(
                        subjectName = subjectPerf.subjectName,
                        currentAverage = subjectPerf.currentAverage,
                        previousAverage = subjectPerf.previousAverage,
                        trend = subjectPerf.trend,
                        status = subjectPerf.status
                    )
                }

                // Grade Trends by Period
                item {
                    Text(
                        text = "Grade Trends by Period",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(performanceData.periodTrends) { periodTrend ->
                    PeriodTrendCard(
                        period = periodTrend.period,
                        average = periodTrend.average,
                        trend = periodTrend.trend,
                        count = periodTrend.count
                    )
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
fun PerformanceSummaryCard(
    overallAverage: Double,
    trend: PerformanceTrend,
    improvement: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = String.format("%.1f", overallAverage),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (trend) {
                            PerformanceTrend.IMPROVING -> Icons.Default.TrendingUp
                            PerformanceTrend.DECLINING -> Icons.Default.TrendingDown
                            PerformanceTrend.STABLE -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when (trend) {
                            PerformanceTrend.IMPROVING -> Color(0xFF4CAF50)
                            PerformanceTrend.DECLINING -> Color(0xFFF44336)
                            PerformanceTrend.STABLE -> Color(0xFF9E9E9E)
                        }
                    )
                    Text(
                        text = when (trend) {
                            PerformanceTrend.IMPROVING -> "Improving"
                            PerformanceTrend.DECLINING -> "Declining"
                            PerformanceTrend.STABLE -> "Stable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (trend) {
                            PerformanceTrend.IMPROVING -> Color(0xFF4CAF50)
                            PerformanceTrend.DECLINING -> Color(0xFFF44336)
                            PerformanceTrend.STABLE -> Color(0xFF9E9E9E)
                        }
                    )
                }
            }
            
            if (improvement != 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (improvement > 0) "+${String.format("%.1f", improvement)}%" else "${String.format("%.1f", improvement)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (improvement > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun SubjectPerformanceCard(
    subjectName: String,
    currentAverage: Double,
    previousAverage: Double?,
    trend: PerformanceTrend,
    status: GradeStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${String.format("%.1f", currentAverage)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        GradeStatus.PASSING -> Color(0xFF4CAF50)
                        GradeStatus.AT_RISK -> Color(0xFFFF9800)
                        GradeStatus.FAILING -> Color(0xFFF44336)
                        GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: ${String.format("%.1f", currentAverage)}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (previousAverage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (trend) {
                                PerformanceTrend.IMPROVING -> Icons.Default.TrendingUp
                                PerformanceTrend.DECLINING -> Icons.Default.TrendingDown
                                PerformanceTrend.STABLE -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = when (trend) {
                                PerformanceTrend.IMPROVING -> Color(0xFF4CAF50)
                                PerformanceTrend.DECLINING -> Color(0xFFF44336)
                                PerformanceTrend.STABLE -> Color(0xFF9E9E9E)
                            }
                        )
                        Text(
                            text = "Previous: ${String.format("%.1f", previousAverage)}%",
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
fun PeriodTrendCard(
    period: GradePeriod,
    average: Double,
    trend: PerformanceTrend,
    count: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = period.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count grades",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${String.format("%.1f", average)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Icon(
                    imageVector = when (trend) {
                        PerformanceTrend.IMPROVING -> Icons.Default.TrendingUp
                        PerformanceTrend.DECLINING -> Icons.Default.TrendingDown
                        PerformanceTrend.STABLE -> Icons.Default.TrendingFlat
                    },
                    contentDescription = null,
                    tint = when (trend) {
                        PerformanceTrend.IMPROVING -> Color(0xFF4CAF50)
                        PerformanceTrend.DECLINING -> Color(0xFFF44336)
                        PerformanceTrend.STABLE -> Color(0xFF9E9E9E)
                    }
                )
            }
        }
    }
}

enum class PerformanceTrend {
    IMPROVING,
    DECLINING,
    STABLE
}

data class PerformanceData(
    val overallAverage: Double,
    val trend: PerformanceTrend,
    val improvement: Double,
    val subjectPerformance: List<SubjectPerformance>,
    val periodTrends: List<PeriodTrend>
)

data class SubjectPerformance(
    val subjectName: String,
    val currentAverage: Double,
    val previousAverage: Double?,
    val trend: PerformanceTrend,
    val status: GradeStatus
)

data class PeriodTrend(
    val period: GradePeriod,
    val average: Double,
    val trend: PerformanceTrend,
    val count: Int
)

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.GradeStatus
import com.smartacademictracker.data.model.StudentGradeAggregate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGradeMonitoringScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminGradeMonitoringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    var selectedStatus by remember { mutableStateOf<GradeStatus?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadGradeData()
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
                text = "Grade Monitoring",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overview Cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradeOverviewCard(
                        title = "Total Students",
                        value = uiState.totalStudents.toString(),
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f)
                    )
                    GradeOverviewCard(
                        title = "At Risk",
                        value = uiState.atRiskStudents.toString(),
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradeOverviewCard(
                        title = "Passing",
                        value = uiState.passingStudents.toString(),
                        icon = Icons.Default.TrendingUp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    GradeOverviewCard(
                        title = "Failing",
                        value = uiState.failingStudents.toString(),
                        icon = Icons.Default.TrendingDown,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Filter
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Filter by Status:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedStatus = null },
                        label = { Text("All") },
                        selected = selectedStatus == null
                    )
                    FilterChip(
                        onClick = { selectedStatus = GradeStatus.AT_RISK },
                        label = { Text("At Risk") },
                        selected = selectedStatus == GradeStatus.AT_RISK
                    )
                    FilterChip(
                        onClick = { selectedStatus = GradeStatus.FAILING },
                        label = { Text("Failing") },
                        selected = selectedStatus == GradeStatus.FAILING
                    )
                    FilterChip(
                        onClick = { selectedStatus = GradeStatus.PASSING },
                        label = { Text("Passing") },
                        selected = selectedStatus == GradeStatus.PASSING
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
            // Grade Aggregates List
            val filteredAggregates = gradeAggregates.filter { aggregate ->
                selectedStatus == null || aggregate.status == selectedStatus
            }

            if (filteredAggregates.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No grade data found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Grade aggregates will appear here once teachers submit grades",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAggregates) { aggregate ->
                        GradeAggregateCard(aggregate = aggregate)
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
fun GradeOverviewCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GradeAggregateCard(
    aggregate: StudentGradeAggregate
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (aggregate.status) {
                GradeStatus.PASSING -> MaterialTheme.colorScheme.primaryContainer
                GradeStatus.AT_RISK -> MaterialTheme.colorScheme.tertiaryContainer
                GradeStatus.FAILING -> MaterialTheme.colorScheme.errorContainer
                GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                        text = aggregate.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = aggregate.subjectName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = when (aggregate.status) {
                        GradeStatus.PASSING -> MaterialTheme.colorScheme.primary
                        GradeStatus.AT_RISK -> MaterialTheme.colorScheme.tertiary
                        GradeStatus.FAILING -> MaterialTheme.colorScheme.error
                        GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = aggregate.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (aggregate.status) {
                            GradeStatus.PASSING -> MaterialTheme.colorScheme.onPrimary
                            GradeStatus.AT_RISK -> MaterialTheme.colorScheme.onTertiary
                            GradeStatus.FAILING -> MaterialTheme.colorScheme.onError
                            GradeStatus.INCOMPLETE -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grade Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GradeDetailItem(
                    label = "Prelim",
                    value = aggregate.prelimGrade?.toString() ?: "N/A"
                )
                GradeDetailItem(
                    label = "Midterm",
                    value = aggregate.midtermGrade?.toString() ?: "N/A"
                )
                GradeDetailItem(
                    label = "Final",
                    value = aggregate.finalGrade?.toString() ?: "N/A"
                )
                GradeDetailItem(
                    label = "Average",
                    value = aggregate.finalAverage?.let { "%.1f".format(it) } ?: "N/A",
                    isHighlighted = true
                )
            }
            
            // Completion Status
            Spacer(modifier = Modifier.height(8.dp))
            val completionPercentage = aggregate.getCompletionPercentage()
            Text(
                text = "Completion: ${"%.0f".format(completionPercentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GradeDetailItem(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isHighlighted) 
                MaterialTheme.typography.titleMedium 
            else 
                MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlighted) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

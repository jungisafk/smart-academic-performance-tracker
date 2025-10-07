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
import com.smartacademictracker.data.model.CurveType
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.GradeCurve
import com.smartacademictracker.data.model.CurveApplication
import com.smartacademictracker.presentation.utils.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeCurveScreen(
    subjectId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: TeacherGradeCurveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(subjectId) {
        viewModel.loadCurves(subjectId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grade Curve Tools") },
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
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                ErrorMessage(
                    message = uiState.error ?: "Unknown error",
                    onRetry = { viewModel.loadCurves(subjectId) }
                )
            } else {
                // Grade Period Selection
                GradePeriodSection(
                    selectedPeriod = uiState.selectedGradePeriod,
                    onPeriodSelected = { period ->
                        viewModel.setGradePeriod(period)
                        viewModel.loadCurveStatistics(subjectId, period)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Statistics Card
                uiState.statistics?.let { statistics ->
                    StatisticsCard(statistics = statistics)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Curve Configuration
                CurveConfigurationSection(
                    uiState = uiState,
                    onCurveTypeSelected = { type ->
                        viewModel.setCurveType(type)
                    },
                    onParametersChanged = { adjustment, target, max, min ->
                        viewModel.updateCurveParameters(adjustment, target, max, min)
                    },
                    onPreviewCurve = {
                        val curve = GradeCurve(
                            subjectId = subjectId,
                            curveType = uiState.selectedCurveType,
                            adjustmentFactor = uiState.adjustmentFactor,
                            targetAverage = uiState.targetAverage,
                            maxGrade = uiState.maxGrade,
                            minGrade = uiState.minGrade
                        )
                        viewModel.previewCurve(subjectId, uiState.selectedGradePeriod, curve)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Curve Preview
                if (uiState.curvePreview.isNotEmpty()) {
                    CurvePreviewSection(
                        curveApplications = uiState.curvePreview,
                        previewStatistics = uiState.previewStatistics,
                        onApplyCurve = {
                            val curve = GradeCurve(
                                subjectId = subjectId,
                                curveType = uiState.selectedCurveType,
                                adjustmentFactor = uiState.adjustmentFactor,
                                targetAverage = uiState.targetAverage,
                                maxGrade = uiState.maxGrade,
                                minGrade = uiState.minGrade
                            )
                            viewModel.applyCurve(subjectId, uiState.selectedGradePeriod, curve)
                        },
                        onClearPreview = { viewModel.clearPreview() }
                    )
                }
                
                // Applied Curves History
                if (uiState.curves.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AppliedCurvesSection(curves = uiState.curves)
                }
                
                // Success/Error Messages
                if (uiState.curveApplied) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SuccessMessage(
                        message = "Curve applied successfully!",
                        onDismiss = { viewModel.clearMessages() }
                    )
                }
            }
        }
    }
}

@Composable
fun GradePeriodSection(
    selectedPeriod: GradePeriod,
    onPeriodSelected: (GradePeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Grade Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GradePeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsCard(
    statistics: com.smartacademictracker.data.model.CurveStatistics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    label = "Average",
                    value = String.format("%.1f", statistics.originalAverage),
                    color = MaterialTheme.colorScheme.primary
                )
                StatisticItem(
                    label = "Std Dev",
                    value = String.format("%.1f", statistics.originalStandardDeviation),
                    color = MaterialTheme.colorScheme.secondary
                )
                StatisticItem(
                    label = "Passing Rate",
                    value = String.format("%.1f%%", statistics.passingRate),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatisticItem(
                    label = "Students",
                    value = statistics.totalStudents.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun CurveConfigurationSection(
    uiState: TeacherGradeCurveUiState,
    onCurveTypeSelected: (CurveType) -> Unit,
    onParametersChanged: (Double, Double, Double, Double) -> Unit,
    onPreviewCurve: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Curve Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Curve Type Selection
            Text(
                text = "Curve Type",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CurveType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.selectedCurveType == type,
                        onClick = { onCurveTypeSelected(type) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Parameters
            when (uiState.selectedCurveType) {
                CurveType.LINEAR -> {
                    OutlinedTextField(
                        value = uiState.adjustmentFactor.toString(),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let { adjustment ->
                                onParametersChanged(adjustment, uiState.targetAverage, uiState.maxGrade, uiState.minGrade)
                            }
                        },
                        label = { Text("Adjustment Factor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CurveType.PERCENTAGE -> {
                    OutlinedTextField(
                        value = uiState.adjustmentFactor.toString(),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let { adjustment ->
                                onParametersChanged(adjustment, uiState.targetAverage, uiState.maxGrade, uiState.minGrade)
                            }
                        },
                        label = { Text("Percentage Increase (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CurveType.TARGET_AVERAGE -> {
                    OutlinedTextField(
                        value = uiState.targetAverage.toString(),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let { target ->
                                onParametersChanged(uiState.adjustmentFactor, target, uiState.maxGrade, uiState.minGrade)
                            }
                        },
                        label = { Text("Target Average") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    // Other curve types don't need additional parameters
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onPreviewCurve,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview Curve")
            }
        }
    }
}

@Composable
fun CurvePreviewSection(
    curveApplications: List<CurveApplication>,
    previewStatistics: com.smartacademictracker.data.model.CurveStatistics?,
    onApplyCurve: () -> Unit,
    onClearPreview: () -> Unit
) {
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
                    text = "Curve Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onClearPreview) {
                    Text("Clear")
                }
            }
            
            previewStatistics?.let { stats ->
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem(
                        label = "New Average",
                        value = String.format("%.1f", stats.curvedAverage),
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatisticItem(
                        label = "New Std Dev",
                        value = String.format("%.1f", stats.curvedStandardDeviation),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatisticItem(
                        label = "New Passing Rate",
                        value = String.format("%.1f%%", stats.passingRate),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApplyCurve,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Curve")
                }
                
                OutlinedButton(
                    onClick = onClearPreview,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun AppliedCurvesSection(curves: List<GradeCurve>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Applied Curves",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(curves) { curve ->
                    AppliedCurveCard(curve = curve)
                }
            }
        }
    }
}

@Composable
fun AppliedCurveCard(curve: GradeCurve) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = curve.curveType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Surface(
                    color = if (curve.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (curve.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (curve.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Applied: ${formatDate(curve.appliedDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        .format(Date(timestamp))
}

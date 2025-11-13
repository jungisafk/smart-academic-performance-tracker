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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                            text = "Performance Analytics",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.refreshData() },
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
            if (!uiState.isLoading && gradeAggregates.isNotEmpty()) {
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
                                    text = "${gradeAggregates.size} Subject${if (gradeAggregates.size != 1) "s" else ""} Analyzed",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Track your academic progress and trends",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            // Analytics Icon
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
                
                // Filter Section
                item {
                    AnalyticsFilterSection(
                        gradeAggregates = gradeAggregates,
                        selectedSubjectId = uiState.selectedSubjectId,
                        onSubjectSelected = { subjectId ->
                            viewModel.setSelectedSubject(subjectId)
                        }
                    )
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
                                text = "Loading analytics...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            } else if (uiState.error != null) {
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
                                text = uiState.error ?: "Unknown error",
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                // Get filtered aggregates based on selected subject
                val filteredAggregates = if (uiState.selectedSubjectId == null) {
                    gradeAggregates
                } else {
                    gradeAggregates.filter { it.subjectId == uiState.selectedSubjectId }
                }
                
                // Overall Performance Summary
                item {
                    EnhancedOverallPerformanceCard(
                        totalSubjects = filteredAggregates.size,
                        averageGrade = uiState.overallAverage,
                        passingSubjects = uiState.passingSubjects,
                        atRiskSubjects = uiState.atRiskSubjects
                    )
                }
                
                // Subject Performance Cards with Grade Trend Charts
                items(filteredAggregates) { gradeAggregate ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SubjectPerformanceCard(
                            subjectName = gradeAggregate.subjectName,
                            currentAverage = gradeAggregate.finalAverage,
                            prelimGrade = gradeAggregate.prelimGrade,
                            midtermGrade = gradeAggregate.midtermGrade,
                            finalGrade = gradeAggregate.finalGrade
                        )
                        
                        // Grade Trend Chart for this subject
                        if (gradeAggregate.prelimGrade != null || gradeAggregate.midtermGrade != null || gradeAggregate.finalGrade != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${gradeAggregate.subjectName} - Grade Trend",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF333333)
                                        )
                                    }
                                    
                                    ChartUtils.GradeTrendChart(
                                        prelimGrade = gradeAggregate.prelimGrade,
                                        midtermGrade = gradeAggregate.midtermGrade,
                                        finalGrade = gradeAggregate.finalGrade,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Overall Performance Distribution Chart
                if (filteredAggregates.isNotEmpty()) {
                    item {
                        PerformanceDistributionChart(
                            passingCount = uiState.passingSubjects,
                            atRiskCount = uiState.atRiskSubjects,
                            failingCount = uiState.failingSubjects
                        )
                    }
                }
                
                // Subject Comparison Chart (if we have multiple subjects and showing overall)
                if (uiState.selectedSubjectId == null && gradeAggregates.size > 1) {
                    item {
                        EnhancedSubjectComparisonChart(
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

@Composable
fun PerformanceDistributionChart(
    passingCount: Int,
    atRiskCount: Int,
    failingCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Performance Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            val total = passingCount + atRiskCount + failingCount
            if (total > 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Passing
                    PerformanceBarItem(
                        label = "Passing",
                        count = passingCount,
                        total = total,
                        color = Color(0xFF4CAF50)
                    )
                    
                    // At Risk
                    PerformanceBarItem(
                        label = "At Risk",
                        count = atRiskCount,
                        total = total,
                        color = Color(0xFFFF9800)
                    )
                    
                    // Failing
                    PerformanceBarItem(
                        label = "Failing",
                        count = failingCount,
                        total = total,
                        color = Color(0xFFF44336)
                    )
                }
            } else {
                Text(
                    text = "No performance data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
fun PerformanceBarItem(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (count.toFloat() / total.toFloat()) * 100f else 0f
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Text(
                text = "$count (${String.format("%.1f", percentage)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun EnhancedOverallPerformanceCard(
    totalSubjects: Int,
    averageGrade: Double?,
    passingSubjects: Int,
    atRiskSubjects: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Overall Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            if (averageGrade != null) {
                GradeProgressIndicator(
                    currentGrade = averageGrade,
                    targetGrade = 75.0
                )
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Text(
                        text = "No grades available yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Performance Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EnhancedPerformanceStatItem("Total Subjects", totalSubjects.toString(), Icons.Default.TrendingUp, Color(0xFF2196F3))
                EnhancedPerformanceStatItem("Passing", passingSubjects.toString(), Icons.Default.CheckCircle, Color(0xFF4CAF50))
                EnhancedPerformanceStatItem("At Risk", atRiskSubjects.toString(), Icons.Default.Warning, Color(0xFFFF9800))
            }
        }
    }
}

@Composable
private fun EnhancedPerformanceStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun EnhancedSubjectComparisonChart(
    subjects: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Subject Performance Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            ChartUtils.SubjectComparisonChart(
                subjects = subjects,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsFilterSection(
    gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>,
    selectedSubjectId: String?,
    onSubjectSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter by Subject",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            // Filter Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedSubjectId?.let { subjectId ->
                        gradeAggregates.find { it.subjectId == subjectId }?.subjectName ?: "Overall"
                    } ?: "Overall",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedTextColor = Color(0xFF333333),
                        unfocusedTextColor = Color(0xFF333333)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Overall option
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Overall",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onSubjectSelected(null)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (selectedSubjectId == null) Color(0xFF2196F3) else Color(0xFF333333)
                        )
                    )
                    
                    // Subject options
                    gradeAggregates.forEach { aggregate ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = aggregate.subjectName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                onSubjectSelected(aggregate.subjectId)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = if (selectedSubjectId == aggregate.subjectId) Color(0xFF2196F3) else Color(0xFF333333)
                            )
                        )
                    }
                }
            }
        }
    }
}

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.GradeStatus
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGradeMonitoringScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminGradeMonitoringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadGradeData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Grade Monitoring",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshGradeData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Show loading indicator only when no data exists yet
            if (uiState.isLoading && gradeAggregates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2196F3),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Data is loading...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Filter Analytics Section
                item {
                    FilterAnalyticsCard(
                        uiState = uiState,
                        onCourseSelected = { viewModel.setCourseFilter(it) },
                        onYearLevelSelected = { viewModel.setYearLevelFilter(it) },
                        onSubjectSelected = { viewModel.setSubjectFilter(it) },
                        onSectionSelected = { viewModel.setSectionFilter(it) },
                        onClearAll = { viewModel.clearAllFilters() }
                    )
                }

                // Performance Overview Section
                item {
                    PerformanceOverviewCard(
                        uiState = uiState,
                        gradeAggregates = gradeAggregates
                    )
                }

                // Grade Distribution Section
                item {
                    GradeDistributionCard(gradeAggregates = gradeAggregates)
                }

                // Performance Trends Section
                item {
                    PerformanceTrendsCard(gradeAggregates = gradeAggregates)
                }
            }
        }

        // Error Message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
fun FilterAnalyticsCard(
    uiState: AdminGradeMonitoringUiState,
    onCourseSelected: (String?) -> Unit,
    onYearLevelSelected: (String?) -> Unit,
    onSubjectSelected: (String?) -> Unit,
    onSectionSelected: (String?) -> Unit,
    onClearAll: () -> Unit
) {
    var expandedCourse by remember { mutableStateOf(false) }
    var expandedYearLevel by remember { mutableStateOf(false) }
    var expandedSubject by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Filter Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = Color(0xFF2196F3))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Dropdowns - Course, Year Level, Subject, Section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Course Dropdown
                FilterDropdown(
                    label = "Course",
                    selectedValue = uiState.selectedCourse ?: "All Courses",
                    options = listOf("All Courses") + uiState.availableCourses,
                    expanded = expandedCourse,
                    onExpandedChange = { expandedCourse = it },
                    onOptionSelected = { 
                        expandedCourse = false
                        onCourseSelected(if (it == "All Courses") null else it)
                    },
                    isHighlighted = uiState.selectedCourse != null
                )
                
                // Year Level Dropdown
                FilterDropdown(
                    label = "Year Level",
                    selectedValue = uiState.selectedYearLevel ?: "All Year Levels",
                    options = listOf("All Year Levels") + uiState.availableYearLevels,
                    expanded = expandedYearLevel,
                    onExpandedChange = { expandedYearLevel = it },
                    onOptionSelected = { 
                        expandedYearLevel = false
                        onYearLevelSelected(if (it == "All Year Levels") null else it)
                    },
                    isHighlighted = uiState.selectedYearLevel != null
                )
                
                // Subject Dropdown
                FilterDropdown(
                    label = "Subject",
                    selectedValue = uiState.selectedSubject ?: "All Subjects",
                    options = listOf("All Subjects") + uiState.availableSubjects,
                    expanded = expandedSubject,
                    onExpandedChange = { expandedSubject = it },
                    onOptionSelected = { 
                        expandedSubject = false
                        onSubjectSelected(if (it == "All Subjects") null else it)
                    },
                    isHighlighted = uiState.selectedSubject != null
                )
                
                // Section Dropdown
                FilterDropdown(
                    label = "Section",
                    selectedValue = uiState.selectedSection ?: "All Sections",
                    options = listOf("All Sections") + uiState.availableSections,
                    expanded = expandedSection,
                    onExpandedChange = { expandedSection = it },
                    onOptionSelected = { 
                        expandedSection = false
                        onSectionSelected(if (it == "All Sections") null else it)
                    },
                    isHighlighted = uiState.selectedSection != null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    isHighlighted: Boolean = false
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .then(
                    if (isHighlighted) {
                        Modifier.border(
                            1.dp,
                            Color(0xFF2196F3),
                            RoundedCornerShape(4.dp)
                        )
                    } else Modifier
                ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isHighlighted) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = if (isHighlighted) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun PerformanceOverviewCard(
    uiState: AdminGradeMonitoringUiState,
    gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>
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
            // Title with blue bar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2196F3))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Performance Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Three metric cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Students",
                    value = uiState.totalStudents.toString(),
                    icon = Icons.Default.People,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Passing",
                    value = uiState.passingStudents.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "At Risk",
                    value = uiState.atRiskStudents.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gradeAggregates.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF2196F3).copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No Performance Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                            Text(
                                text = "Grades will appear here once teachers add them",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF999999),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        PerformanceBarChart(gradeAggregates = gradeAggregates)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PerformanceBarChart(gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>) {
    val passingCount = gradeAggregates.count { it.status == GradeStatus.PASSING }
    val atRiskCount = gradeAggregates.count { it.status == GradeStatus.AT_RISK }
    val failingCount = gradeAggregates.count { it.status == GradeStatus.FAILING }
    val incompleteCount = gradeAggregates.count { it.status == GradeStatus.INCOMPLETE }
    
    val total = gradeAggregates.size
    val maxValue = max(1, max(passingCount, max(atRiskCount, max(failingCount, incompleteCount))))
    
    val categories = listOf("Passing", "At Risk", "Failing", "Incomplete")
    val values = listOf(passingCount, atRiskCount, failingCount, incompleteCount)
    val colors = listOf(
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFF44336),
        Color(0xFF9E9E9E)
    )
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Performance Distribution",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            categories.forEachIndexed { index, category ->
                val value = values[index]
                val heightRatio = if (maxValue > 0) value.toFloat() / maxValue else 0f
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .weight(1f, fill = false)
                            .height((180.dp * heightRatio).coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(colors[index])
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GradeDistributionCard(
    gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>
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
            // Title with blue bar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2196F3))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Grade Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (gradeAggregates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF2196F3).copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No data to display",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                GradeDistributionPieChart(gradeAggregates = gradeAggregates)
            }
        }
    }
}

@Composable
fun GradeDistributionPieChart(gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>) {
    val passingCount = gradeAggregates.count { it.status == GradeStatus.PASSING }
    val atRiskCount = gradeAggregates.count { it.status == GradeStatus.AT_RISK }
    val failingCount = gradeAggregates.count { it.status == GradeStatus.FAILING }
    val incompleteCount = gradeAggregates.count { it.status == GradeStatus.INCOMPLETE }
    
    val total = gradeAggregates.size
    if (total == 0) return
    
    val data = listOf(
        Pair("Passing", passingCount) to Color(0xFF4CAF50),
        Pair("At Risk", atRiskCount) to Color(0xFFFF9800),
        Pair("Failing", failingCount) to Color(0xFFF44336),
        Pair("Incomplete", incompleteCount) to Color(0xFF9E9E9E)
    ).filter { it.first.second > 0 }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pie chart visualization
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val radius = size.minDimension / 2f - 20f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    
                    data.forEach { (label, color) ->
                        val count = label.second
                        val sweepAngle = (count.toFloat() / total) * 360f
                        
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                        
                        startAngle += sweepAngle
                    }
                }
                
                // Center text
                Text(
                    text = "$total\nStudents",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    textAlign = TextAlign.Center
                )
            }
            
            // Legend
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                data.forEach { (label, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(color, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${label.first}: ${label.second}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF333333),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceTrendsCard(
    gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>
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
            // Title with blue bar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2196F3))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Performance Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (gradeAggregates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF2196F3).copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No data to display",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                PerformanceTrendsLineChart(gradeAggregates = gradeAggregates)
            }
        }
    }
}

@Composable
fun PerformanceTrendsLineChart(gradeAggregates: List<com.smartacademictracker.data.model.StudentGradeAggregate>) {
    // Group by period and calculate averages
    val prelimAvg = gradeAggregates.mapNotNull { it.prelimGrade }.average().let { if (it.isNaN()) 0.0 else it }
    val midtermAvg = gradeAggregates.mapNotNull { it.midtermGrade }.average().let { if (it.isNaN()) 0.0 else it }
    val finalAvg = gradeAggregates.mapNotNull { it.finalGrade }.average().let { if (it.isNaN()) 0.0 else it }
    
    val periods = listOf("Prelim", "Midterm", "Final")
    val averages = listOf(prelimAvg, midtermAvg, finalAvg)
    // Calculate dynamic range based on actual data
    val minValue = minOf(averages.minOrNull() ?: 70.0, 70.0).coerceAtLeast(0.0)
    val maxValue = maxOf(averages.maxOrNull() ?: 100.0, 100.0)
    // Add padding for better visualization
    val rangeMin = minValue - 2.0
    val rangeMax = maxValue + 5.0
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Average Grades by Period",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padding = 50.dp.toPx()
                val chartWidth = size.width - padding * 2
                val chartHeight = size.height - padding * 2 - 30.dp.toPx() // Space for labels
                val startX = padding
                val startY = padding
                val endX = size.width - padding
                val endY = size.height - padding - 30.dp.toPx()
                
                // Draw grid lines
                for (i in 0..4) {
                    val y = startY + (chartHeight / 4) * i
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Draw line chart using dynamic range
                val path = Path()
                val range = rangeMax - rangeMin
                periods.forEachIndexed { index, _ ->
                    val x = startX + (chartWidth / (periods.size - 1)) * index
                    val value = averages[index]
                    // Normalize value to range (0-1) based on dynamic min/max
                    val normalizedValue = if (range > 0) ((value - rangeMin) / range).toFloat().coerceIn(0f, 1f) else 0.5f
                    val y = endY - (chartHeight * normalizedValue)
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Draw point
                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                
                // Draw line
                drawPath(
                    path = path,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            
            // Value labels above points
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 50.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEachIndexed { index, _ ->
                    val value = averages[index]
                    val chartHeight = 180.dp - 30.dp
                    val range = rangeMax - rangeMin
                    // Normalize value to range (0-1) based on dynamic min/max
                    val valueRatio = if (range > 0) ((value - rangeMin) / range).toFloat().coerceIn(0f, 1f) else 0.5f
                    val labelY = chartHeight - (chartHeight * valueRatio) - 20.dp
                    
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                            Text(
                                text = String.format("%.1f", value),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333),
                                modifier = Modifier.offset(x = 0.dp, y = labelY.coerceAtLeast(0.dp))
                            )
                    }
                }
            }
            
            // Period labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 50.dp)
                    .offset(y = 190.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                periods.forEach { period ->
                    Text(
                        text = period,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

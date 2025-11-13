package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.utils.GradeCalculationEngine
import com.smartacademictracker.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeInputScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    viewModel: TeacherGradeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val enrollments by viewModel.enrollments.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    var selectedStudentIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectAndStudents(subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        subject?.let {
                            Text(
                                text = it.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            }
        } else if (enrollments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    title = "No students enrolled",
                    message = "There are no students enrolled in this subject yet.",
                    icon = Icons.Default.Person
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header with column labels
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Student Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Average",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(80.dp)
                        )
                    }
                }

                // Students List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(enrollments) { index, enrollment ->
                        val aggregate = gradeAggregates.find { it.studentId == enrollment.studentId }
                        val averageGrade = aggregate?.finalAverage?.let { 
                            GradeCalculationEngine.calculateLetterGrade(it)
                        } ?: "INC"
                        
                        StudentGradeRow(
                            studentName = enrollment.studentName,
                            averageGrade = averageGrade,
                            onClick = { selectedStudentIndex = index }
                        )
                    }
                }
            }
        }

        // Student Grade Input Dialog
        selectedStudentIndex?.let { index ->
            val enrollment = enrollments.getOrNull(index)
            if (enrollment != null) {
                val studentGrades = grades.filter { it.studentId == enrollment.studentId }
                val aggregate = gradeAggregates.find { it.studentId == enrollment.studentId }
                
                StudentGradeInputDialog(
                    enrollment = enrollment,
                    studentGrades = studentGrades,
                    aggregate = aggregate,
                    currentIndex = index,
                    totalStudents = enrollments.size,
                    onDismiss = { selectedStudentIndex = null },
                    onNavigatePrevious = {
                        if (index > 0) {
                            selectedStudentIndex = index - 1
                        }
                    },
                    onNavigateNext = {
                        if (index < enrollments.size - 1) {
                            selectedStudentIndex = index + 1
                        }
                    },
                    onSaveGrade = { period, grade ->
                        viewModel.updateGradeForPeriod(enrollment.studentId, period, grade)
                    }
                )
            }
        }
    }
}

@Composable
fun StudentGradeRow(
    studentName: String,
    averageGrade: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = studentName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = averageGrade,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    averageGrade == "INC" -> Color(0xFF9E9E9E)
                    averageGrade.toDoubleOrNull()?.let { it <= 3.0 } == true -> Color(0xFF4CAF50)
                    averageGrade.toDoubleOrNull()?.let { it <= 3.5 } == true -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
            )
        }
    }
}

@Composable
fun StudentGradeInputDialog(
    enrollment: Enrollment,
    studentGrades: List<Grade>,
    aggregate: StudentGradeAggregate?,
    currentIndex: Int,
    totalStudents: Int,
    onDismiss: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onSaveGrade: (GradePeriod, Double) -> Unit
) {
    var prelimGrade by remember { 
        mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }?.score?.toString() ?: "")
    }
    var midtermGrade by remember { 
        mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }?.score?.toString() ?: "")
    }
    var finalGrade by remember { 
        mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.FINAL }?.score?.toString() ?: "")
    }
    var showConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = enrollment.studentName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student ${currentIndex + 1} of $totalStudents",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Prelim Grade
                OutlinedTextField(
                    value = prelimGrade,
                    onValueChange = { prelimGrade = it },
                    label = { Text("Preliminary (30%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        prelimGrade.toDoubleOrNull()?.let { grade ->
                            Text(
                                text = "1-5 Scale: ${GradeCalculationEngine.calculateLetterGrade(grade)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Midterm Grade
                OutlinedTextField(
                    value = midtermGrade,
                    onValueChange = { midtermGrade = it },
                    label = { Text("Midterm (30%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        midtermGrade.toDoubleOrNull()?.let { grade ->
                            Text(
                                text = "1-5 Scale: ${GradeCalculationEngine.calculateLetterGrade(grade)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Final Grade
                OutlinedTextField(
                    value = finalGrade,
                    onValueChange = { finalGrade = it },
                    label = { Text("Final (40%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        finalGrade.toDoubleOrNull()?.let { grade ->
                            Text(
                                text = "1-5 Scale: ${GradeCalculationEngine.calculateLetterGrade(grade)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Calculated Average
                val calculatedAverage = aggregate?.finalAverage?.let {
                    GradeCalculationEngine.calculateLetterGrade(it)
                } ?: run {
                    val prelim = prelimGrade.toDoubleOrNull()
                    val midterm = midtermGrade.toDoubleOrNull()
                    val final = finalGrade.toDoubleOrNull()
                    if (prelim != null && midterm != null && final != null) {
                        val avg = GradeCalculationEngine.calculateFinalAverage(prelim, midterm, final)
                        avg?.let { GradeCalculationEngine.calculateLetterGrade(it) } ?: "INC"
                    } else "INC"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Final Average:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = calculatedAverage,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                calculatedAverage == "INC" -> Color(0xFF9E9E9E)
                                calculatedAverage.toDoubleOrNull()?.let { it <= 3.0 } == true -> Color(0xFF4CAF50)
                                calculatedAverage.toDoubleOrNull()?.let { it <= 3.5 } == true -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous Button
                if (currentIndex > 0) {
                    IconButton(onClick = onNavigatePrevious) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Save Button
                Button(
                    onClick = {
                        showConfirmDialog = true
                    },
                    enabled = prelimGrade.toDoubleOrNull()?.let { it in 0.0..100.0 } == true ||
                             midtermGrade.toDoubleOrNull()?.let { it in 0.0..100.0 } == true ||
                             finalGrade.toDoubleOrNull()?.let { it in 0.0..100.0 } == true
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }

                // Next Button
                if (currentIndex < totalStudents - 1) {
                    IconButton(onClick = onNavigateNext) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Save",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to save the grades for ${enrollment.studentName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (prelimGrade.toDoubleOrNull() != null) {
                        Text(
                            text = "Preliminary: ${prelimGrade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (midtermGrade.toDoubleOrNull() != null) {
                        Text(
                            text = "Midterm: ${midtermGrade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (finalGrade.toDoubleOrNull() != null) {
                        Text(
                            text = "Final: ${finalGrade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prelimGrade.toDoubleOrNull()?.let { 
                            onSaveGrade(GradePeriod.PRELIM, it)
                        }
                        midtermGrade.toDoubleOrNull()?.let { 
                            onSaveGrade(GradePeriod.MIDTERM, it)
                        }
                        finalGrade.toDoubleOrNull()?.let { 
                            onSaveGrade(GradePeriod.FINAL, it)
                        }
                        showConfirmDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

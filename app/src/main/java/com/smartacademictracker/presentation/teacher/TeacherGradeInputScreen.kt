package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradeType
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

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectAndStudents(subjectId)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                subject?.let { subject ->
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Grade Type Selection
            var selectedGradeType by remember { mutableStateOf(GradeType.QUIZ) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select Grade Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradeType.values().forEach { gradeType ->
                            FilterChip(
                                onClick = { selectedGradeType = gradeType },
                                label = { Text(gradeType.name) },
                                selected = selectedGradeType == gradeType,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Students List
            if (enrollments.isEmpty()) {
                EmptyState(
                    title = "No students enrolled",
                    message = "There are no students enrolled in this subject yet.",
                    icon = Icons.Default.Person
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(enrollments) { enrollment ->
                        StudentGradeCard(
                            enrollment = enrollment,
                            gradeType = selectedGradeType,
                            existingGrade = grades.find { 
                                it.studentId == enrollment.studentId && it.gradeType == selectedGradeType 
                            },
                            onGradeChange = { studentId, gradeValue ->
                                viewModel.updateGrade(studentId, selectedGradeType, gradeValue)
                            }
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

        // Success Message
        uiState.successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StudentGradeCard(
    enrollment: Enrollment,
    gradeType: GradeType,
    existingGrade: Grade?,
    onGradeChange: (String, Double) -> Unit
) {
    var gradeValue by remember { mutableStateOf(existingGrade?.score?.toString() ?: "") }
    var isEditing by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = enrollment.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student ID: ${enrollment.studentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (existingGrade != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Grade: ${existingGrade.score}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = gradeValue,
                    onValueChange = { 
                        gradeValue = it
                        isEditing = true
                    },
                    label = { Text("${gradeType.name} Grade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        val value = gradeValue.toDoubleOrNull()
                        if (value != null && value >= 0 && value <= 100) {
                            onGradeChange(enrollment.studentId, value)
                            isEditing = false
                        }
                    },
                    enabled = isEditing && gradeValue.toDoubleOrNull() != null && 
                             gradeValue.toDoubleOrNull()!! in 0.0..100.0
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save Grade",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }
    }
}


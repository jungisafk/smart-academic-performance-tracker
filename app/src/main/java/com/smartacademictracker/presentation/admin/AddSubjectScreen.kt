package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    courseId: String = "",
    yearLevelId: String = "",
    onNavigateBack: () -> Unit,
    onSubjectAdded: () -> Unit,
    viewModel: AddSubjectViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("3") }
    var numberOfSections by remember { mutableStateOf("1") }
    
    // Automatically set subject type based on courseId
    // If courseId is provided: MAJOR (subject belongs to a course)
    // If courseId is empty: MINOR (cross-departmental subject)
    val isMinorSubject = courseId.isEmpty()
    var subjectType by remember { 
        mutableStateOf(
            if (isMinorSubject) 
                com.smartacademictracker.data.model.SubjectType.MINOR 
            else 
                com.smartacademictracker.data.model.SubjectType.MAJOR
        )
    }
    var expandedSubjectType by remember { mutableStateOf(false) }
    
    // Academic Period Selection
    var selectedAcademicPeriodId by remember { mutableStateOf<String?>(null) }
    var expandedAcademicPeriod by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val academicPeriods by viewModel.academicPeriods.collectAsState()
    
    // Get active academic period ID for default selection
    val activePeriodId = remember {
        academicPeriods.firstOrNull { it.isActive }?.id
    }

    LaunchedEffect(courseId, yearLevelId) {
        
        // Automatically set subject type based on courseId
        subjectType = if (courseId.isEmpty()) {
            com.smartacademictracker.data.model.SubjectType.MINOR
        } else {
            com.smartacademictracker.data.model.SubjectType.MAJOR
        }
        
        if (courseId.isNotEmpty()) {
            viewModel.setCourseId(courseId)
        }
        if (yearLevelId.isNotEmpty()) {
            viewModel.setYearLevelId(yearLevelId)
        }
    }

    val isFormValid = name.isNotBlank() && 
                     code.isNotBlank() && 
                     credits.isNotBlank() && 
                     numberOfSections.isNotBlank()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSubjectAdded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
                text = "Add Subject",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Subject Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Subject Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Subject Code
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Subject Code *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    minLines = 3,
                    maxLines = 3
                )

                // Credits
                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it },
                    label = { Text("Credits *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Number of Sections
                OutlinedTextField(
                    value = numberOfSections,
                    onValueChange = { newValue ->
                        // Only allow numeric input
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            numberOfSections = newValue
                        }
                    },
                    label = { Text("Number of Sections *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true,
                    supportingText = { 
                        Text("Will create sections: ${if (code.isNotBlank() && numberOfSections.isNotBlank()) {
                            val sections = (0 until (numberOfSections.toIntOrNull() ?: 1)).map { i ->
                                "${code}${('A' + i)}"
                            }
                            sections.joinToString(", ")
                        } else "Enter subject code and number of sections"}") 
                    }
                )

                // Subject Type (Auto-set, read-only)
                OutlinedTextField(
                    value = subjectType.displayName,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    label = { Text("Subject Type *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    supportingText = {
                        Text(
                            text = when (subjectType) {
                                com.smartacademictracker.data.model.SubjectType.MAJOR -> 
                                    "Automatically set to MAJOR (subject belongs to a course)"
                                com.smartacademictracker.data.model.SubjectType.MINOR -> 
                                    "Automatically set to MINOR (cross-departmental subject)"
                            }
                        )
                    }
                )

                // Academic Period Selection
                ExposedDropdownMenuBox(
                    expanded = expandedAcademicPeriod,
                    onExpandedChange = { expandedAcademicPeriod = !expandedAcademicPeriod },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    val selectedPeriod = academicPeriods.find { it.id == selectedAcademicPeriodId }
                    val displayText = selectedPeriod?.let { 
                        "${it.name} (${it.semester.displayName} - ${it.academicYear})${if (it.isActive) " [Active]" else ""}"
                    } ?: activePeriodId?.let {
                        val activePeriod = academicPeriods.find { it.id == activePeriodId }
                        activePeriod?.let { "${it.name} (${it.semester.displayName} - ${it.academicYear}) [Active - Default]" } ?: "Use Active Period (Default)"
                    } ?: "Select Academic Period (Required)"
                    
                    OutlinedTextField(
                        value = displayText,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Academic Period *") },
                        trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        supportingText = {
                            Text(
                                text = if (selectedAcademicPeriodId == null) {
                                    "Leave empty to use active period, or select a specific period to setup subjects in advance"
                                } else {
                                    "Subject will be created for the selected academic period"
                                }
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAcademicPeriod,
                        onDismissRequest = { expandedAcademicPeriod = false }
                    ) {
                        // Option to use active period (default)
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = activePeriodId?.let {
                                        val activePeriod = academicPeriods.find { period -> period.id == activePeriodId }
                                        activePeriod?.let { "${it.name} (${it.semester.displayName} - ${it.academicYear}) [Active - Default]" } ?: "Use Active Period (Default)"
                                    } ?: "No Active Period Available",
                                    fontWeight = if (selectedAcademicPeriodId == null) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedAcademicPeriodId = null
                                expandedAcademicPeriod = false
                            }
                        )
                        
                        Divider()
                        
                        // List all academic periods
                        academicPeriods.forEach { period ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = "${period.name} (${period.semester.displayName} - ${period.academicYear})${if (period.isActive) " [Active]" else ""}",
                                        fontWeight = if (selectedAcademicPeriodId == period.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedAcademicPeriodId = period.id
                                    expandedAcademicPeriod = false
                                }
                            )
                        }
                    }
                }

                // Success Message
                if (uiState.isSuccess) {
                    Text(
                        text = "Subject added successfully!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Error Message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Add Button
                Button(
                    onClick = {
                        viewModel.addSubject(
                            name = name.trim(),
                            code = code.trim(),
                            description = description.trim(),
                            credits = credits.toIntOrNull() ?: 3,
                            numberOfSections = numberOfSections.toIntOrNull() ?: 1,
                            subjectType = subjectType,
                            selectedAcademicPeriodId = selectedAcademicPeriodId
                        )
                    },
                    enabled = !uiState.isLoading && isFormValid && !uiState.isSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (uiState.isSuccess) {
                        Text("Subject Added!")
                    } else {
                        Text("Add Subject")
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentAcademicPeriodCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Academic Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "This subject will be automatically assigned to the current active academic period and semester.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

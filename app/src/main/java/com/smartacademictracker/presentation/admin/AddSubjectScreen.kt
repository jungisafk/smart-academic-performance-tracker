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
    var subjectType by remember { mutableStateOf(com.smartacademictracker.data.model.SubjectType.MAJOR) }
    var expandedSubjectType by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(courseId, yearLevelId) {
        println("DEBUG: AddSubjectScreen - Received courseId: '$courseId', yearLevelId: '$yearLevelId'")
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

                // Subject Type
                ExposedDropdownMenuBox(
                    expanded = expandedSubjectType,
                    onExpandedChange = { expandedSubjectType = !expandedSubjectType },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = subjectType.displayName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Subject Type *") },
                        trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        supportingText = {
                            Text(
                                text = when (subjectType) {
                                    com.smartacademictracker.data.model.SubjectType.MAJOR -> 
                                        "Only teachers of this course can see and apply"
                                    com.smartacademictracker.data.model.SubjectType.MINOR -> 
                                        "All teachers can see and apply"
                                }
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubjectType,
                        onDismissRequest = { expandedSubjectType = false }
                    ) {
                        com.smartacademictracker.data.model.SubjectType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    subjectType = type
                                    expandedSubjectType = false
                                }
                            )
                        }
                    }
                }

                // Current Academic Period Info
                CurrentAcademicPeriodCard()

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
                            subjectType = subjectType
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

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    onNavigateBack: () -> Unit,
    onCourseAdded: () -> Unit,
    viewModel: AddCourseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onCourseAdded()
        }
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
                text = "Add New Course",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Course Name
            OutlinedTextField(
                value = uiState.courseName,
                onValueChange = viewModel::setCourseName,
                label = { Text("Course Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.courseNameError != null,
                supportingText = uiState.courseNameError?.let { { Text(it) } }
            )

            // Course Code
            OutlinedTextField(
                value = uiState.courseCode,
                onValueChange = viewModel::setCourseCode,
                label = { Text("Course Code") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.courseCodeError != null,
                supportingText = uiState.courseCodeError?.let { { Text(it) } }
            )

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Duration Dropdown
            var expandedDuration by remember { mutableStateOf(false) }
            val durationOptions = listOf(2, 3, 4, 5, 6)
            
            ExposedDropdownMenuBox(
                expanded = expandedDuration,
                onExpandedChange = { expandedDuration = !expandedDuration },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.duration.toString(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Duration (Years)") },
                    trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.durationError != null,
                    supportingText = uiState.durationError?.let { { Text(it) } }
                )
                ExposedDropdownMenu(
                    expanded = expandedDuration,
                    onDismissRequest = { expandedDuration = false }
                ) {
                    durationOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("$option years") },
                            onClick = {
                                viewModel.setDuration(option)
                                expandedDuration = false
                            }
                        )
                    }
                }
            }
            
            // Note about automatic year level creation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "ℹ️ Year levels will be automatically created based on the course duration. For example, a 4-year course will create 4 year levels (1st Year, 2nd Year, 3rd Year, 4th Year).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Add Course Button
            Button(
                onClick = { viewModel.addCourse() },
                enabled = !uiState.isLoading && uiState.courseName.isNotBlank() && uiState.courseCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adding Course...")
                } else {
                    Text("Add Course")
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

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

            // Duration
            OutlinedTextField(
                value = uiState.duration.toString(),
                onValueChange = { viewModel.setDuration(it.toIntOrNull() ?: 4) },
                label = { Text("Duration (Years)") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.durationError != null,
                supportingText = uiState.durationError?.let { { Text(it) } }
            )

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

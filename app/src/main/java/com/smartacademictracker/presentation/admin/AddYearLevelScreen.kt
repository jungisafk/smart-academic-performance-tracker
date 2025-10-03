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
fun AddYearLevelScreen(
    courseId: String = "",
    onNavigateBack: () -> Unit,
    onYearLevelAdded: () -> Unit,
    viewModel: AddYearLevelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(courseId) {
        println("DEBUG: AddYearLevelScreen - Received courseId: '$courseId'")
        if (courseId.isNotEmpty()) {
            viewModel.setCourseId(courseId)
            println("DEBUG: AddYearLevelScreen - Set courseId in ViewModel: '$courseId'")
        } else {
            println("DEBUG: AddYearLevelScreen - courseId is empty!")
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onYearLevelAdded()
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
                text = "Add New Year Level",
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
            // Year Level Name
            OutlinedTextField(
                value = uiState.yearLevelName,
                onValueChange = viewModel::setYearLevelName,
                label = { Text("Year Level Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.yearLevelNameError != null,
                supportingText = uiState.yearLevelNameError?.let { { Text(it) } }
            )

            // Level Number
            OutlinedTextField(
                value = uiState.level.toString(),
                onValueChange = { viewModel.setLevel(it.toIntOrNull() ?: 1) },
                label = { Text("Level Number") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.levelError != null,
                supportingText = uiState.levelError?.let { { Text(it) } }
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

            Spacer(modifier = Modifier.height(16.dp))

            // Add Year Level Button
            Button(
                onClick = { viewModel.addYearLevel() },
                enabled = !uiState.isLoading && uiState.yearLevelName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adding Year Level...")
                } else {
                    Text("Add Year Level")
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

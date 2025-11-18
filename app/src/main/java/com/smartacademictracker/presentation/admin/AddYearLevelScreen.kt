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
fun AddYearLevelScreen(
    courseId: String = "",
    onNavigateBack: () -> Unit,
    onYearLevelAdded: () -> Unit,
    viewModel: AddYearLevelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(courseId) {
        
        if (courseId.isNotEmpty()) {
            viewModel.setCourseId(courseId)
            
        } else {
            
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
            // Year Level Dropdown (1-4)
            var expandedYearLevel by remember { mutableStateOf(false) }
            val yearLevelOptions = listOf("1st Year", "2nd Year", "3rd Year", "4th Year")
            
            ExposedDropdownMenuBox(
                expanded = expandedYearLevel,
                onExpandedChange = { expandedYearLevel = !expandedYearLevel },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.yearLevelName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Year Level") },
                    trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.yearLevelNameError != null,
                    supportingText = uiState.yearLevelNameError?.let { { Text(it) } }
                )
                ExposedDropdownMenu(
                    expanded = expandedYearLevel,
                    onDismissRequest = { expandedYearLevel = false }
                ) {
                    yearLevelOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.setYearLevelName(option)
                                expandedYearLevel = false
                            }
                        )
                    }
                }
            }

            // Level Number Dropdown (1-4)
            var expandedLevel by remember { mutableStateOf(false) }
            val levelOptions = listOf(1, 2, 3, 4)
            
            ExposedDropdownMenuBox(
                expanded = expandedLevel,
                onExpandedChange = { expandedLevel = !expandedLevel },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.level.toString(),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Level Number") },
                    trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = uiState.levelError != null,
                    supportingText = uiState.levelError?.let { { Text(it) } }
                )
                ExposedDropdownMenu(
                    expanded = expandedLevel,
                    onDismissRequest = { expandedLevel = false }
                ) {
                    levelOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("$option") },
                            onClick = {
                                viewModel.setLevel(option)
                                expandedLevel = false
                            }
                        )
                    }
                }
            }

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

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Semester
import com.smartacademictracker.presentation.utils.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAcademicPeriodScreen(
    onNavigateBack: () -> Unit,
    onPeriodAdded: () -> Unit,
    viewModel: AddAcademicPeriodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPeriodAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Academic Period") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Form Fields
            OutlinedTextField(
                value = uiState.periodName,
                onValueChange = viewModel::setPeriodName,
                label = { Text("Period Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.periodNameError != null,
                supportingText = uiState.periodNameError?.let { { Text(it) } },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.academicYear,
                onValueChange = viewModel::setAcademicYear,
                label = { Text("Academic Year *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.academicYearError != null,
                supportingText = uiState.academicYearError?.let { { Text(it) } },
                singleLine = true,
                placeholder = { Text("e.g., 2024-2025") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Semester Dropdown
            SemesterDropdown(
                selectedSemester = uiState.selectedSemester,
                onSemesterSelected = viewModel::setSemester
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Range Section
            DateRangeSection(
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onStartDateChanged = viewModel::setStartDate,
                onEndDateChanged = viewModel::setEndDate,
                startDateError = uiState.startDateError,
                endDateError = uiState.endDateError,
                selectedSemester = uiState.selectedSemester
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Active Period Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.isActive,
                    onCheckedChange = { isChecked ->
                        println("DEBUG: AddAcademicPeriodScreen - Checkbox changed to: $isChecked")
                        viewModel.setAsActive(isChecked)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Set as active period",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = viewModel::addAcademicPeriod,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && 
                             uiState.periodName.isNotBlank() && 
                             uiState.academicYear.isNotBlank() &&
                             uiState.startDate > 0 && 
                             uiState.endDate > 0
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Period")
                    }
                }
            }
        }
    }

    // Show error dialog
    uiState.error?.let { error ->
        ErrorMessageWithDismiss(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterDropdown(
    selectedSemester: Semester,
    onSemesterSelected: (Semester) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val semesterOptions = listOf(
        Semester.FIRST_SEMESTER,
        Semester.SECOND_SEMESTER,
        Semester.SUMMER_CLASS
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedSemester.displayName,
            onValueChange = { },
            readOnly = true,
            label = { Text("Semester *") },
            trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            semesterOptions.forEach { semester ->
                DropdownMenuItem(
                    text = { Text(semester.displayName) },
                    onClick = {
                        onSemesterSelected(semester)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DateRangeSection(
    startDate: Long,
    endDate: Long,
    onStartDateChanged: (Long) -> Unit,
    onEndDateChanged: (Long) -> Unit,
    startDateError: String?,
    endDateError: String?,
    selectedSemester: Semester
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = if (startDate > 0) formatDate(startDate) else "",
                    onValueChange = { },
                    label = { Text("Start Date *") },
                    modifier = Modifier.weight(1f),
                    isError = startDateError != null,
                    supportingText = startDateError?.let { { Text(it) } },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // Set start date to current date
                                onStartDateChanged(System.currentTimeMillis())
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = if (endDate > 0) formatDate(endDate) else "",
                    onValueChange = { },
                    label = { Text("End Date *") },
                    modifier = Modifier.weight(1f),
                    isError = endDateError != null,
                    supportingText = endDateError?.let { { Text(it) } },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                // Set end date based on semester type
                                val startDate = if (startDate > 0) startDate else System.currentTimeMillis()
                                val endDate = when {
                                    // First/Second Semester: ~4 months
                                    selectedSemester == Semester.FIRST_SEMESTER || selectedSemester == Semester.SECOND_SEMESTER -> 
                                        startDate + (120L * 24 * 60 * 60 * 1000) // 120 days
                                    // Summer Class: ~1 month
                                    selectedSemester == Semester.SUMMER_CLASS -> 
                                        startDate + (30L * 24 * 60 * 60 * 1000) // 30 days
                                    else -> startDate + (30L * 24 * 60 * 60 * 1000) // Default 30 days
                                }
                                onEndDateChanged(endDate)
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    }
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

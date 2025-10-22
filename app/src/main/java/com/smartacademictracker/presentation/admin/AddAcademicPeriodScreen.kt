package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Add Academic Period",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Create a new academic period for the system",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        // School Icon
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            
            // Form Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Form Fields
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = uiState.periodName,
                            onValueChange = viewModel::setPeriodName,
                            label = { Text("Period Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.periodNameError != null,
                            supportingText = uiState.periodNameError?.let { { Text(it) } },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
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
                            placeholder = { Text("YYYY-YYYY (e.g., 2024-2025)") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Semester Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Semester Selection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        EnhancedSemesterDropdown(
                            selectedSemester = uiState.selectedSemester,
                            onSemesterSelected = viewModel::setSemester
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Date Range Section
                EnhancedDateRangeSection(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    onStartDateChanged = viewModel::setStartDate,
                    onEndDateChanged = viewModel::setEndDate,
                    startDateError = uiState.startDateError,
                    endDateError = uiState.endDateError,
                    selectedSemester = uiState.selectedSemester
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Description Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Additional Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::setDescription,
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active Period Checkbox
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = uiState.isActive,
                                    onCheckedChange = { isChecked ->
                                        println("DEBUG: AddAcademicPeriodScreen - Checkbox changed to: $isChecked")
                                        viewModel.setAsActive(isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF2196F3),
                                        uncheckedColor = Color(0xFF666666)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Set as active period",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF333333)
                                    )
                                    Text(
                                        text = "This will make this period the current active period",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = viewModel::addAcademicPeriod,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading && 
                                 uiState.periodName.isNotBlank() && 
                                 uiState.academicYear.isNotBlank() &&
                                 uiState.startDate > 0 && 
                                 uiState.endDate > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Creating...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Create Period",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                    value = if (startDate > 0) formatDateLocal(startDate) else "",
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
                    value = if (endDate > 0) formatDateLocal(endDate) else "",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSemesterDropdown(
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
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
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
fun EnhancedDateRangeSection(
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = if (startDate > 0) formatDateLocal(startDate) else "",
                    onValueChange = { },
                    label = { Text("Start Date *") },
                    modifier = Modifier.weight(1f),
                    isError = startDateError != null,
                    supportingText = startDateError?.let { { Text(it) } },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onStartDateChanged(System.currentTimeMillis())
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                
                OutlinedTextField(
                    value = if (endDate > 0) formatDateLocal(endDate) else "",
                    onValueChange = { },
                    label = { Text("End Date *") },
                    modifier = Modifier.weight(1f),
                    isError = endDateError != null,
                    supportingText = endDateError?.let { { Text(it) } },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val startDate = if (startDate > 0) startDate else System.currentTimeMillis()
                                val endDate = when {
                                    selectedSemester == Semester.FIRST_SEMESTER || selectedSemester == Semester.SECOND_SEMESTER -> 
                                        startDate + (120L * 24 * 60 * 60 * 1000)
                                    selectedSemester == Semester.SUMMER_CLASS -> 
                                        startDate + (30L * 24 * 60 * 60 * 1000)
                                    else -> startDate + (30L * 24 * 60 * 60 * 1000)
                                }
                                onEndDateChanged(endDate)
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }
        }
    }
}

private fun formatDateLocal(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

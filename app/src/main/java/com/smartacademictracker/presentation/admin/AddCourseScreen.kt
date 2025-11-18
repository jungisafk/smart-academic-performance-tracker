package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    onNavigateBack: () -> Unit,
    onCourseAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddCourseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onCourseAdded()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

                // Form Fields Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Course Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        
                        // Course Name
                        OutlinedTextField(
                            value = uiState.courseName,
                            onValueChange = viewModel::setCourseName,
                            label = { Text("Course Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.courseNameError != null,
                            supportingText = uiState.courseNameError?.let { { Text(it) } },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        // Course Code
                        OutlinedTextField(
                            value = uiState.courseCode,
                            onValueChange = viewModel::setCourseCode,
                            label = { Text("Course Code *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.courseCodeError != null,
                            supportingText = uiState.courseCodeError?.let { { Text(it) } },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        // Description
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
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Course Duration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
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
                                label = { Text("Duration (Years) *") },
                                trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                isError = uiState.durationError != null,
                                supportingText = uiState.durationError?.let { { Text(it) } },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2196F3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
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
                        
                        // Information Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Year levels will be automatically created based on the course duration. For example, a 4-year course will create 4 year levels (1st Year, 2nd Year, 3rd Year, 4th Year).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Add Course Button
                Button(
                    onClick = { viewModel.addCourse() },
                    enabled = !uiState.isLoading && uiState.courseName.isNotBlank() && uiState.courseCode.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Adding Course...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.School,
                            contentDescription = "Add Course",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Add Course",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Error Message
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
        }
    }
}

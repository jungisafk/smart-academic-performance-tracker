package com.smartacademictracker.presentation.admin

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.PreRegisteredTeacher
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBulkImportTeachersScreen(
    onNavigateBack: () -> Unit,
    onImportSuccess: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier,
    viewModel: AdminBulkImportTeachersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            
            return@rememberLauncherForActivityResult
        }
        
        
        
        try {
            selectedFileUri = uri
            fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            } ?: uri.lastPathSegment ?: "Unknown file"
            
            
            
            // Parse the file
            val fileFileName = fileName ?: "Unknown file" // Store in local variable and handle null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    
                    viewModel.parseFile(inputStream, fileFileName)
                } else {
                    
                    viewModel.setError("Failed to open file. Please try selecting the file again.")
                }
            } catch (e: Exception) {
                
                viewModel.setError("Error reading file: ${e.message ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            
            viewModel.setError("Error processing file: ${e.message ?: "Unknown error"}")
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Text(
                            text = "File Format",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supported format: CSV (.csv)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your file should have the following columns (case-insensitive):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Teacher ID or Employee ID (Required)", style = MaterialTheme.typography.bodySmall)
                        Text("• First Name (Required)", style = MaterialTheme.typography.bodySmall)
                        Text("• Last Name (Required)", style = MaterialTheme.typography.bodySmall)
                        Text("• Middle Name (Optional)", style = MaterialTheme.typography.bodySmall)
                        Text("• Department or Course Code (Required)", style = MaterialTheme.typography.bodySmall)
                        Text("• Email (Optional)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // File Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { 
                    
                    try {
                        // Try multiple MIME types to support different file managers
                        filePickerLauncher.launch("*/*")
                    } catch (e: Exception) {
                        
                        viewModel.setError("Error opening file picker: ${e.message}")
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (fileName != null) fileName!! else "Select File",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (fileName == null) {
                            Text(
                                text = "Click to browse and select a CSV file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.UploadFile,
                        contentDescription = "Select File",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
            }
            
            // Preview Section
            if (uiState.parsedTeachers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Preview (${uiState.parsedTeachers.size} teachers found)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Show first 5 teachers as preview
                        uiState.parsedTeachers.take(5).forEachIndexed { index, teacher ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}. ${teacher.firstName} ${teacher.lastName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "ID: ${teacher.teacherId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (teacher.departmentCourseCode.isNotEmpty()) {
                                        Text(
                                            text = "Department: ${teacher.departmentCourseCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (uiState.parsedTeachers.size > 5) {
                            Text(
                                text = "... and ${uiState.parsedTeachers.size - 5} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Import Button
            if (uiState.parsedTeachers.isNotEmpty()) {
                Button(
                    onClick = { viewModel.importTeachers() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importing...")
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import ${uiState.parsedTeachers.size} Teachers")
                    }
                }
            }
            
            // Results Card
            if (uiState.importResult != null) {
                val result = uiState.importResult!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.failureCount == 0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Import Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Successfully imported: ${result.successCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50)
                        )
                        if (result.failureCount > 0) {
                            Text(
                                text = "Failed: ${result.failureCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF9800)
                            )
                            if (result.errors.isNotEmpty()) {
                                Text(
                                    text = "Errors:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                result.errors.take(10).forEach { error ->
                                    Text(
                                        text = "• $error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (result.errors.size > 10) {
                                    Text(
                                        text = "... and ${result.errors.size - 10} more errors",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Error Display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Snackbar Host
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    
    // Success message and auto-navigate
    uiState.successMessage?.let { successMsg ->
        LaunchedEffect(successMsg) {
            // Show success snackbar
            snackbarHostState.showSnackbar(
                message = successMsg,
                duration = SnackbarDuration.Long
            )
            // Wait a bit longer to let user see the message, then navigate back
            kotlinx.coroutines.delay(2500)
            onImportSuccess()
        }
    }
}


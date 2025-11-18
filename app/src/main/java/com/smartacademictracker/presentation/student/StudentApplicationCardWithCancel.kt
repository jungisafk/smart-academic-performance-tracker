package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartacademictracker.data.model.StudentApplication
import com.smartacademictracker.data.model.StudentApplicationStatus

@Composable
fun StudentApplicationCardWithCancel(
    application: StudentApplication,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (application.status) {
                StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = application.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${application.subjectCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Status: ${application.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = when (application.status) {
                        StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.secondary
                        StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = application.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (application.status) {
                            StudentApplicationStatus.PENDING -> MaterialTheme.colorScheme.onSecondary
                            StudentApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                            StudentApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (application.applicationReason.isNotBlank()) {
                Text(
                    text = "Reason: ${application.applicationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Cancel button for pending applications
            if (application.status == StudentApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Application")
                }
            }
        }
    }
}


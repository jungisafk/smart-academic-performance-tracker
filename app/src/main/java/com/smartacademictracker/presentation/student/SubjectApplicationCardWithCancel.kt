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
import com.smartacademictracker.data.model.SubjectApplication
import com.smartacademictracker.data.model.ApplicationStatus

@Composable
fun SubjectApplicationCardWithCancel(
    application: SubjectApplication,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (application.status) {
                ApplicationStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
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
                    if (application.sectionName.isNotEmpty()) {
                        Text(
                            text = "Section: ${application.sectionName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Status: ${application.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Badge
                Surface(
                    color = when (application.status) {
                        ApplicationStatus.PENDING -> MaterialTheme.colorScheme.secondary
                        ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.error
                        ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = application.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (application.status) {
                            ApplicationStatus.PENDING -> MaterialTheme.colorScheme.onSecondary
                            ApplicationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                            ApplicationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                            ApplicationStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cancel button for pending applications
            if (application.status == ApplicationStatus.PENDING) {
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


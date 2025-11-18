package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartacademictracker.data.model.SectionAssignment
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.TeacherApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleSubjectCard(
    subject: Subject,
    sectionAssignments: List<SectionAssignment>,
    teacherApplications: List<TeacherApplication>,
    onAssignTeacher: (String, String, String) -> Unit,
    onRemoveAssignment: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Code: ${subject.code} • ${sectionAssignments.size} assigned • ${teacherApplications.size} applications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subject.sections.isNotEmpty()) {
                        Text(
                            text = "Sections: ${subject.sections.size} (${subject.sections.joinToString(", ")})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Collapsible Content - Full Subject Details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                EnhancedSubjectSectionCard(
                    subject = subject,
                    sectionAssignments = sectionAssignments,
                    teacherApplications = teacherApplications,
                    onAssignTeacher = onAssignTeacher,
                    onRemoveAssignment = onRemoveAssignment
                )
            }
        }
    }
}


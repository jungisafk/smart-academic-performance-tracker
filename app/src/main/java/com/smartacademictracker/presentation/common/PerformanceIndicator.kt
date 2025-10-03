package com.smartacademictracker.presentation.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartacademictracker.data.model.GradeStatus

data class PerformanceConfig(
    val backgroundColor: Color,
    val textColor: Color,
    val icon: ImageVector,
    val label: String
)

@Composable
fun PerformanceStatusIndicator(
    status: GradeStatus,
    modifier: Modifier = Modifier
) {
    val config = when (status) {
        GradeStatus.PASSING -> 
            PerformanceConfig(
                backgroundColor = Color(0xFF4CAF50),
                textColor = Color.White,
                icon = Icons.Default.CheckCircle,
                label = "Passing"
            )
        GradeStatus.AT_RISK -> 
            PerformanceConfig(
                backgroundColor = Color(0xFFFF9800),
                textColor = Color.White,
                icon = Icons.Default.Warning,
                label = "At Risk"
            )
        GradeStatus.FAILING -> 
            PerformanceConfig(
                backgroundColor = Color(0xFFF44336),
                textColor = Color.White,
                icon = Icons.Default.Error,
                label = "Failing"
            )
        GradeStatus.INCOMPLETE -> 
            PerformanceConfig(
                backgroundColor = Color(0xFF9E9E9E),
                textColor = Color.White,
                icon = Icons.Default.Schedule,
                label = "Incomplete"
            )
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = config.backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = config.label,
                color = config.textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GradeProgressIndicator(
    currentGrade: Double,
    targetGrade: Double = 75.0,
    modifier: Modifier = Modifier
) {
    val progress = (currentGrade / targetGrade).coerceIn(0.0, 1.0)
    val status = when {
        currentGrade >= 75.0 -> GradeStatus.PASSING
        currentGrade >= 60.0 -> GradeStatus.AT_RISK
        currentGrade > 0.0 -> GradeStatus.FAILING
        else -> GradeStatus.INCOMPLETE
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Grade: ${String.format("%.1f", currentGrade)}",
                style = MaterialTheme.typography.bodyMedium
            )
            PerformanceStatusIndicator(status = status)
        }
        
        LinearProgressIndicator(
            progress = progress.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when (status) {
                GradeStatus.PASSING -> Color(0xFF4CAF50)
                GradeStatus.AT_RISK -> Color(0xFFFF9800)
                GradeStatus.FAILING -> Color(0xFFF44336)
                GradeStatus.INCOMPLETE -> Color(0xFF9E9E9E)
            },
            trackColor = Color(0xFFE0E0E0)
        )
        
        Text(
            text = "Target: ${String.format("%.1f", targetGrade)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubjectPerformanceCard(
    subjectName: String,
    currentAverage: Double?,
    prelimGrade: Double?,
    midtermGrade: Double?,
    finalGrade: Double?,
    modifier: Modifier = Modifier
) {
    val status = when {
        currentAverage == null -> GradeStatus.INCOMPLETE
        currentAverage >= 75.0 -> GradeStatus.PASSING
        currentAverage >= 60.0 -> GradeStatus.AT_RISK
        else -> GradeStatus.FAILING
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                PerformanceStatusIndicator(status = status)
            }
            
            if (currentAverage != null) {
                GradeProgressIndicator(
                    currentGrade = currentAverage,
                    targetGrade = 75.0
                )
            } else {
                Text(
                    text = "No grades available yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Grade breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GradeBreakdownItem("Prelim", prelimGrade)
                GradeBreakdownItem("Midterm", midtermGrade)
                GradeBreakdownItem("Final", finalGrade)
            }
        }
    }
}

@Composable
private fun GradeBreakdownItem(
    label: String,
    grade: Double?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = grade?.let { String.format("%.1f", it) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

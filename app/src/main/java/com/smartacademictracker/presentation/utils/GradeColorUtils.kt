package com.smartacademictracker.presentation.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun getGradeColor(percentage: Double): Color {
    return when {
        percentage >= 90 -> MaterialTheme.colorScheme.primary
        percentage >= 80 -> MaterialTheme.colorScheme.tertiary
        percentage >= 70 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
}

package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Consistent header for student screens, matching teacher's header style
 * with padding: horizontal = 16.dp, vertical = 8.dp
 */
@Composable
fun StudentScreenHeader(
    title: String,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    showRefreshButton: Boolean = false,
    onRefresh: () -> Unit = {},
    isLoading: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF666666)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
            
            if (showRefreshButton) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF666666)
                    )
                }
            }
        }
    }
}


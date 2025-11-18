package com.smartacademictracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StudentGradesTabScreen(
    onNavigateBack: () -> Unit = {},
    showBackButton: Boolean = false,
    gradesViewModel: StudentGradesViewModel = hiltViewModel(),
    analyticsViewModel: StudentAnalyticsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val gradesUiState by gradesViewModel.uiState.collectAsState()
    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    
    // Load data when screen is first composed - use ViewModel as key to prevent reloading
    // ViewModels are stable across recompositions, so this will only run once
    LaunchedEffect(gradesViewModel) {
        gradesViewModel.loadGrades()
    }
    
    LaunchedEffect(analyticsViewModel) {
        analyticsViewModel.loadAnalyticsData()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            StudentScreenHeader(
                title = "Grades",
                showBackButton = showBackButton,
                onNavigateBack = onNavigateBack,
                showRefreshButton = true,
                onRefresh = {
                    if (selectedTab == 0) {
                        gradesViewModel.refreshGrades()
                    } else {
                        analyticsViewModel.refreshData()
                    }
                },
                isLoading = gradesUiState.isLoading || analyticsUiState.isLoading
            )
            
            // Tab Row - Full width, no horizontal padding
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f),
                    text = { Text("View Grades") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f),
                    text = { Text("Analytics") }
                )
            }
            
            // Content based on selected tab - Full width, no horizontal padding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // View Grades Tab - Use existing StudentGradesScreen content
                        StudentGradesContent(
                            onNavigateBack = {},
                            viewModel = gradesViewModel,
                            showBackButton = false
                        )
                    }
                    1 -> {
                        // Analytics Tab - Use existing StudentAnalyticsScreen content
                        StudentAnalyticsContent(
                            onNavigateBack = {},
                            viewModel = analyticsViewModel,
                            showBackButton = false
                        )
                    }
                }
            }
        }
    }
}

// Wrapper for StudentGradesScreen content
@Composable
private fun StudentGradesContent(
    onNavigateBack: () -> Unit,
    viewModel: StudentGradesViewModel,
    showBackButton: Boolean
) {
    // Import and use the existing StudentGradesScreen but without the header
    // Since we can't easily extract just the content, we'll use a simplified version
    val uiState by viewModel.uiState.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF2196F3))
        }
    } else {
        // Use the existing StudentGradesScreen but hide its header
        // Mark as embedded to prevent duplicate data loading
        StudentGradesScreen(
            onNavigateBack = onNavigateBack,
            viewModel = viewModel,
            isEmbedded = true
        )
    }
}

// Wrapper for StudentAnalyticsScreen content
@Composable
private fun StudentAnalyticsContent(
    onNavigateBack: () -> Unit,
    viewModel: StudentAnalyticsViewModel,
    showBackButton: Boolean
) {
    // Use the existing StudentAnalyticsScreen but hide its header
    // Mark as embedded to prevent duplicate data loading
    StudentAnalyticsScreen(
        onNavigateBack = onNavigateBack,
        viewModel = viewModel,
        isEmbedded = true
    )
}


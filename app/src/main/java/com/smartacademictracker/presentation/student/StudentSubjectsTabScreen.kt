package com.smartacademictracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.StudentEnrollment
import com.smartacademictracker.data.model.Subject
import com.smartacademictracker.data.model.StudentApplication
import kotlinx.coroutines.delay

@Composable
fun StudentSubjectsTabScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSubjectDetail: (String) -> Unit = {},
    onNavigateToApplicationDetail: (String) -> Unit = {},
    showBackButton: Boolean = false,
    subjectsViewModel: StudentSubjectsViewModel = hiltViewModel(),
    applicationViewModel: StudentSubjectApplicationViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val subjectsUiState by subjectsViewModel.uiState.collectAsState()
    val enrollments by subjectsViewModel.enrollments.collectAsState()
    
    val applicationUiState by applicationViewModel.uiState.collectAsState()
    val availableSubjects by applicationViewModel.availableSubjects.collectAsState()
    val myApplications by applicationViewModel.myApplications.collectAsState()
    
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    
    // Load data when screen is composed - use ViewModel as key to prevent reloading
    LaunchedEffect(subjectsViewModel, applicationViewModel) {
        subjectsViewModel.loadEnrollments()
        applicationViewModel.loadAvailableSubjects()
        applicationViewModel.loadMyApplications()
    }
    
    // Show success snackbar when application is successful
    LaunchedEffect(applicationUiState.isApplicationSuccess) {
        if (applicationUiState.isApplicationSuccess) {
            selectedTab = 1
            showSuccessSnackbar = true
            delay(3000)
            applicationViewModel.clearApplicationSuccess()
            showSuccessSnackbar = false
        }
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
                title = "Subjects",
                showBackButton = showBackButton,
                onNavigateBack = onNavigateBack,
                showRefreshButton = true,
                onRefresh = {
                    if (selectedTab == 0) {
                        subjectsViewModel.refreshEnrollments()
                    } else {
                        applicationViewModel.refreshData()
                    }
                },
                isLoading = subjectsUiState.isLoading || applicationUiState.isLoading
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Subjects") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Apply for Subjects") }
                    )
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> StudentSubjectsScreen(
                            onNavigateBack = onNavigateBack,
                            onNavigateToSubjectDetail = onNavigateToSubjectDetail,
                            viewModel = subjectsViewModel
                        )
                        1 -> StudentSubjectApplicationScreen(
                            onNavigateBack = onNavigateBack,
                            onNavigateToApplicationDetail = onNavigateToApplicationDetail,
                            viewModel = applicationViewModel
                        )
                    }
                }
            }
        }
    }
}


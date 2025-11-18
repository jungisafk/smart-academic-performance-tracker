package com.smartacademictracker.presentation.admin

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPreRegisteredScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStudentBulkImport: () -> Unit = {},
    onNavigateToTeacherBulkImport: () -> Unit = {},
    modifier: Modifier = Modifier,
    studentsViewModel: AdminPreRegisteredStudentsViewModel = hiltViewModel(),
    teachersViewModel: AdminPreRegisteredTeachersViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Refresh data when refreshTrigger changes (manual refresh button or after bulk import)
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            studentsViewModel.loadPreRegisteredStudents(null)
            teachersViewModel.loadPreRegisteredTeachers(null)
        }
    }
    
    // Initial load when screen is first shown
    LaunchedEffect(Unit) {
        studentsViewModel.loadPreRegisteredStudents(null)
        teachersViewModel.loadPreRegisteredTeachers(null)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF2196F3),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = tabPositions[selectedTab].left)
                            .width(tabPositions[selectedTab].width),
                        color = Color(0xFF2196F3),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Students")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAddAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Teachers")
                        }
                    }
                )
            }
            
        // Tab Content
        when (selectedTab) {
            0 -> AdminPreRegisteredStudentsScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToBulkImport = onNavigateToStudentBulkImport,
                viewModel = studentsViewModel,
                showTopBar = false
            )
            1 -> AdminPreRegisteredTeachersScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToBulkImport = onNavigateToTeacherBulkImport,
                viewModel = teachersViewModel,
                showTopBar = false
            )
        }
    }
}


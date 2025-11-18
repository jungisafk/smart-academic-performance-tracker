package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.presentation.utils.*
import com.smartacademictracker.presentation.admin.AcademicPeriodDataViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAcademicPeriodScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddPeriod: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminAcademicPeriodViewModel = hiltViewModel(),
    periodDataViewModel: AcademicPeriodDataViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    val academicPeriods by viewModel.academicPeriods.collectAsState()
    val activePeriod by viewModel.activePeriod.collectAsState()
    val summary by viewModel.summary.collectAsState()

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadAcademicPeriods()
    }

    // Handle success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            // Clear success message after a delay
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Clear error message after a delay
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Periods")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        periodDataViewModel.loadAcademicPeriodsAndSummaries()
                    },
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
                            Text("Data Viewer")
                        }
                    }
                )
            }
            
            // Content
            when (selectedTab) {
                0 -> {
                    // Periods Tab Content
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(
                                bottom = 160.dp // Space for FAB (56dp height + 16dp bottom padding + 16dp spacing) + bottom nav (~80dp) + extra spacing
                            )
                        ) {
                // Current Academic Period Section
                item {
                    EnhancedCurrentAcademicPeriodCard(
                        activePeriod = activePeriod,
                        summary = summary
                    )
                }

                // Academic Periods List
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading academic periods...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                } else if (academicPeriods.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Academic Periods",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Create your first academic period to start managing semesters and academic years.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onNavigateToAddPeriod,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                ) {
                                    Text(
                                        "Create First Period",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(academicPeriods) { period ->
                        // Only show as active if it matches the active period from repository
                        val isActive = activePeriod?.id == period.id
                        var showEditDialog by remember { mutableStateOf(false) }
                        
                        EnhancedAcademicPeriodCard(
                            period = period.copy(isActive = isActive),
                            onEdit = { showEditDialog = true },
                            onDelete = { viewModel.deleteAcademicPeriod(period.id) },
                            onSetActive = { viewModel.setActivePeriod(period.id) }
                        )
                        
                        // Edit Dialog
                        if (showEditDialog) {
                            EditAcademicPeriodDialog(
                                period = period.copy(isActive = isActive),
                                onDismiss = { showEditDialog = false },
                                onSave = { name, description, isActive ->
                                    viewModel.updateAcademicPeriod(period.id, name, description, isActive)
                                    showEditDialog = false
                                }
                            )
                        }
                    }
                }
            }
                        
            // Floating Action Button - Only show in Periods tab
            FloatingActionButton(
                onClick = onNavigateToAddPeriod,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFFFFC107)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Period",
                    tint = Color.White
                )
            }
        }
                }
                1 -> {
                    // Data Viewer Tab Content
                    val periodDataUiState by periodDataViewModel.uiState.collectAsState()
                    val periodSummaries by periodDataViewModel.periodSummaries.collectAsState()
                    val selectedPeriodData by periodDataViewModel.selectedPeriodData.collectAsState()
                    
                    LaunchedEffect(Unit) {
                        periodDataViewModel.loadAcademicPeriodsAndSummaries()
                        // Load active period data by default
                        activePeriod?.let {
                            periodDataViewModel.loadPeriodData(it.id)
                        }
                    }
                    
                    LaunchedEffect(activePeriod?.id) {
                        activePeriod?.let {
                            periodDataViewModel.loadPeriodData(it.id)
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .padding(bottom = 80.dp) // Add bottom padding for bottom nav
                    ) {
                        if (periodDataUiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF2196F3))
                            }
                        } else if (selectedPeriodData != null) {
                            // Show detailed data for selected period
                            val data = selectedPeriodData!!
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 24.dp) // Additional bottom padding
                            ) {
                                // Period Info Card
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Text(
                                                text = data.academicPeriod?.name ?: "Unknown Period",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "${data.academicPeriod?.academicYear ?: ""} - ${data.academicPeriod?.semester ?: ""}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                                
                                // Statistics Card
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Text(
                                                text = "Statistics",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF333333)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                StatisticItem("Students", data.statistics.totalStudents.toString())
                                                StatisticItem("Teachers", data.statistics.totalTeachers.toString())
                                                StatisticItem("Subjects", data.statistics.totalSubjects.toString())
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                StatisticItem("Courses", data.statistics.totalCourses.toString())
                                                StatisticItem("Year Levels", data.statistics.totalYearLevels.toString())
                                                StatisticItem("Sections", data.statistics.totalSections.toString())
                                            }
                                        }
                                    }
                                }
                                
                                // All Periods List
                                if (periodSummaries.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "All Periods",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF333333),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(periodSummaries) { summary ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { periodDataViewModel.loadPeriodData(summary.periodId) },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (summary.periodId == selectedPeriodData?.periodId) 
                                                    Color(0xFFE3F2FD) else Color.White
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = summary.periodName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (summary.isActive) {
                                                        Surface(
                                                            color = Color(0xFF4CAF50),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "Active",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Students: ${summary.statistics.totalStudents} | Subjects: ${summary.statistics.totalSubjects}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color(0xFF666666)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Show period summaries if no data selected
                            if (periodSummaries.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No period data available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF666666)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(periodSummaries) { summary ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { periodDataViewModel.loadPeriodData(summary.periodId) }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = summary.periodName,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (summary.isActive) {
                                                        Surface(
                                                            color = Color(0xFF4CAF50),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "Active",
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Students: ${summary.statistics.totalStudents} | Subjects: ${summary.statistics.totalSubjects}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show error dialog
    uiState.error?.let { error ->
        ErrorMessageWithDismiss(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Show success message
    uiState.successMessage?.let { message ->
        SuccessMessage(
            message = message,
            onDismiss = { viewModel.clearSuccessMessage() }
        )
    }
}

@Composable
fun StatisticItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(1f / 3f)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun CurrentAcademicPeriodCard(
    activePeriod: AcademicPeriod?,
    summary: com.smartacademictracker.data.model.AcademicPeriodOverview
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Academic Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (activePeriod != null) {
                Text(
                    text = activePeriod.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${activePeriod.academicYear} - ${activePeriod.semester.displayName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDateRange(activePeriod.startDate, activePeriod.endDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No active period set",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyAcademicPeriodsCard(
    onAddPeriod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Academic Periods",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Create your first academic period to start managing semesters and academic years.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAddPeriod,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create First Period")
            }
        }
    }
}

@Composable
fun AcademicPeriodsList(
    academicPeriods: List<AcademicPeriod>,
    activePeriod: AcademicPeriod?,
    onSetActive: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(academicPeriods) { period ->
            AcademicPeriodCard(
                period = period,
                isActive = period.id == activePeriod?.id,
                onSetActive = { onSetActive(period.id) },
                onDelete = { onDelete(period.id) }
            )
        }
    }
}

@Composable
fun AcademicPeriodCard(
    period: AcademicPeriod,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = period.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${period.academicYear} - ${period.semester.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateRange(period.startDate, period.endDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    if (!isActive) {
                        TextButton(onClick = onSetActive) {
                            Text("Set Active")
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (period.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = period.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Currently Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

@Composable
fun EditAcademicPeriodDialog(
    period: AcademicPeriod,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var periodName by remember { mutableStateOf(period.name) }
    var description by remember { mutableStateOf(period.description) }
    var isActive by remember { mutableStateOf(period.isActive) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Academic Period",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Period Name Field
                OutlinedTextField(
                    value = periodName,
                    onValueChange = { periodName = it },
                    label = { Text("Period Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                
                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                
                // Active Period Checkbox
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF2196F3),
                                uncheckedColor = Color(0xFF666666)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Set as active period",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                            Text(
                                text = "This will make this period the current active period",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                // Read-only fields (for information)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Read-only Information",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "Academic Year: ${period.academicYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "Semester: ${period.semester.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "Date Range: ${formatDate(period.startDate)} - ${formatDate(period.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (periodName.isNotBlank()) {
                        onSave(periodName, description, isActive)
                    }
                },
                enabled = periodName.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatDateRange(startDate: Long, endDate: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val start = dateFormat.format(Date(startDate))
    val end = dateFormat.format(Date(endDate))
    return "$start - $end"
}

@Composable
fun EnhancedCurrentAcademicPeriodCard(
    activePeriod: AcademicPeriod?,
    summary: com.smartacademictracker.data.model.AcademicPeriodOverview
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Current Academic Period",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (activePeriod != null) {
                Text(
                    text = activePeriod.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = "${activePeriod.academicYear} • ${activePeriod.semester.displayName}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "${formatDate(activePeriod.startDate)} - ${formatDate(activePeriod.endDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            } else {
                Text(
                    text = "No active period set",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun EnhancedAcademicPeriodCard(
    period: AcademicPeriod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
                        text = period.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "${period.academicYear} • ${period.semester.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "${formatDate(period.startDate)} - ${formatDate(period.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                
                // Status Badge
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (period.isActive) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
                    )
                ) {
                    Text(
                        text = if (period.isActive) "Active" else "Inactive",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (period.isActive) Color(0xFF4CAF50) else Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            if (period.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = period.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons - Consistent outlined style with responsive sizing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!period.isActive) {
                    OutlinedButton(
                        onClick = onSetActive,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFF4CAF50)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "Set Active",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFF2196F3)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Edit",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFD32F2F)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

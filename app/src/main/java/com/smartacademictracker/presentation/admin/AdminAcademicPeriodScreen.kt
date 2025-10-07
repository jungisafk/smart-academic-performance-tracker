package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.AcademicPeriod
import com.smartacademictracker.presentation.utils.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAcademicPeriodScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddPeriod: () -> Unit,
    viewModel: AdminAcademicPeriodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val academicPeriods by viewModel.academicPeriods.collectAsState()
    val activePeriod by viewModel.activePeriod.collectAsState()
    val summary by viewModel.summary.collectAsState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academic Periods") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddPeriod) {
                        Icon(Icons.Default.Add, contentDescription = "Add Period")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPeriod,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Period")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Current Academic Period Section
            CurrentAcademicPeriodCard(
                activePeriod = activePeriod,
                summary = summary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Academic Periods List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (academicPeriods.isEmpty()) {
                EmptyAcademicPeriodsCard(
                    onAddPeriod = onNavigateToAddPeriod
                )
            } else {
                AcademicPeriodsList(
                    academicPeriods = academicPeriods,
                    activePeriod = activePeriod,
                    onSetActive = { periodId ->
                        viewModel.setActivePeriod(periodId)
                    },
                    onDelete = { periodId ->
                        viewModel.deleteAcademicPeriod(periodId)
                    }
                )
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
fun CurrentAcademicPeriodCard(
    activePeriod: AcademicPeriod?,
    summary: com.smartacademictracker.data.model.AcademicPeriodSummary
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

private fun formatDateRange(startDate: Long, endDate: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val start = dateFormat.format(Date(startDate))
    val end = dateFormat.format(Date(endDate))
    return "$start - $end"
}
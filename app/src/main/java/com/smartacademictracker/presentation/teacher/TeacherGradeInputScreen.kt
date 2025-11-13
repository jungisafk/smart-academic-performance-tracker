package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.Enrollment
import com.smartacademictracker.data.model.Grade
import com.smartacademictracker.data.model.GradePeriod
import com.smartacademictracker.data.model.StudentGradeAggregate
import com.smartacademictracker.data.utils.GradeCalculationEngine
import com.smartacademictracker.presentation.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGradeInputScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    viewModel: TeacherGradeInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val subject by viewModel.subject.collectAsState()
    val enrollments by viewModel.enrollments.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gradeAggregates by viewModel.gradeAggregates.collectAsState()
    var selectedStudentIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectAndStudents(subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject?.name ?: "Loading...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        subject?.let {
                            Text(
                                text = it.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else if (enrollments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    title = "No students enrolled",
                    message = "There are no students enrolled in this subject yet.",
                    icon = Icons.Default.Person
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header with column labels
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Student Number column
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .border(1.dp, Color.White.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeText(
                                text = "#",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Student Name column
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .border(1.dp, Color.White.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeText(
                                text = "Student Name",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Prelim column
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .border(1.dp, Color.White.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeText(
                                text = "Prelim",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Midterm column
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .border(1.dp, Color.White.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeText(
                                text = "Midterm",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Final column
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeText(
                                text = "Final",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Final Grade column
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .border(1.dp, Color.White.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeText(
                                text = "Final Grade",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Students List - Sort by last name alphabetically
                val sortedEnrollments = remember(enrollments) {
                    enrollments.sortedBy { enrollment ->
                        val nameParts = enrollment.studentName.trim().split("\\s+".toRegex())
                        nameParts.lastOrNull() ?: enrollment.studentName
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(sortedEnrollments) { index, enrollment ->
                        val studentGrades = grades.filter { it.studentId == enrollment.studentId }
                        val prelimGrade = studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }
                        val midtermGrade = studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }
                        val finalGrade = studentGrades.find { it.gradePeriod == GradePeriod.FINAL }
                        val aggregate = gradeAggregates.find { it.studentId == enrollment.studentId }
                        val finalAverageGrade = aggregate?.finalAverage?.let { 
                            GradeCalculationEngine.calculateLetterGrade(it)
                        } ?: "INC"
                        
                        // Parse name into first and last name
                        val nameParts = enrollment.studentName.trim().split("\\s+".toRegex())
                        val firstName = nameParts.dropLast(1).joinToString(" ").ifEmpty { enrollment.studentName }
                        val lastName = nameParts.lastOrNull() ?: ""
                        
                        // Find original index in unsorted list for dialog navigation
                        val originalIndex = enrollments.indexOfFirst { it.id == enrollment.id }
                        
                        StudentGradeRow(
                            studentNumber = index + 1,
                            firstName = firstName,
                            lastName = lastName,
                            prelimGrade = prelimGrade?.letterGrade ?: "—",
                            midtermGrade = midtermGrade?.letterGrade ?: "—",
                            finalGrade = finalGrade?.letterGrade ?: "—",
                            finalAverageGrade = finalAverageGrade,
                            onClick = { 
                                selectedStudentIndex = if (originalIndex >= 0) originalIndex else index
                            }
                        )
                    }
                }
            }
        }

        // Student Grade Input Dialog
        selectedStudentIndex?.let { index ->
            val enrollment = enrollments.getOrNull(index)
            if (enrollment != null) {
                val studentGrades = grades.filter { it.studentId == enrollment.studentId }
                val aggregate = gradeAggregates.find { it.studentId == enrollment.studentId }
                
                StudentGradeInputDialog(
                    enrollment = enrollment,
                    studentGrades = studentGrades,
                    aggregate = aggregate,
                    currentIndex = index,
                    totalStudents = enrollments.size,
                    uiState = uiState,
                    onDismiss = { selectedStudentIndex = null },
                    onNavigatePrevious = {
                        if (index > 0) {
                            selectedStudentIndex = index - 1
                        }
                    },
                    onNavigateNext = {
                        if (index < enrollments.size - 1) {
                            selectedStudentIndex = index + 1
                        }
                    },
                    onSaveGrade = { period, grade ->
                        viewModel.updateGradeForPeriod(enrollment.studentId, period, grade)
                    },
                    onRequestEdit = { gradeId ->
                        viewModel.requestGradeEdit(gradeId)
                    },
                    onClearMessage = {
                        viewModel.clearSuccessMessage()
                        viewModel.clearError()
                    }
                )
            }
        }
    }
}

@Composable
fun StudentGradeRow(
    studentNumber: Int,
    firstName: String,
    lastName: String,
    prelimGrade: String,
    midtermGrade: String,
    finalGrade: String,
    finalAverageGrade: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Number column
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeText(
                    text = studentNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Student Name column
            Box(
                modifier = Modifier
                    .weight(2f)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AutoSizeText(
                            text = firstName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1
                        )
                        AutoSizeText(
                            text = lastName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
            }
            // Prelim column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeText(
                    text = prelimGrade,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = getGradeColor(prelimGrade),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Midterm column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeText(
                    text = midtermGrade,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = getGradeColor(midtermGrade),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Final column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeText(
                    text = finalGrade,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = getGradeColor(finalGrade),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Final Grade column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFE0E0E0))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AutoSizeText(
                    text = finalAverageGrade,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = getGradeColor(finalAverageGrade),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun getGradeColor(grade: String): Color {
    return when {
        grade == "INC" || grade == "—" -> Color(0xFF9E9E9E)
        grade.toDoubleOrNull()?.let { it <= 3.0 } == true -> Color(0xFF4CAF50)
        grade.toDoubleOrNull()?.let { it <= 3.5 } == true -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

/**
 * Auto-scaling text that fits within the container by reducing font size if needed
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified
) {
    var textStyle by remember(text, style) { mutableStateOf(style) }
    
    Text(
        text = text,
        style = textStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        color = color,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth && textStyle.fontSize.value > 8.sp.value) {
                textStyle = textStyle.copy(fontSize = (textStyle.fontSize.value * 0.9f).sp)
            }
        },
        modifier = modifier
    )
}

@Composable
fun StudentGradeInputDialog(
    enrollment: Enrollment,
    studentGrades: List<Grade>,
    aggregate: StudentGradeAggregate?,
    currentIndex: Int,
    totalStudents: Int,
    uiState: TeacherGradeInputUiState,
    onDismiss: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onSaveGrade: (GradePeriod, Double) -> Unit,
    onRequestEdit: (String) -> Unit = {},
    onClearMessage: () -> Unit = {}
) {
    // Get screen width and height for responsive design
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallScreen = screenWidth < 360.dp
    val isShortScreen = screenHeight < 700.dp
    val dialogPadding = if (isSmallScreen) 12.dp else if (isShortScreen) 16.dp else 20.dp
    val contentSpacing = if (isSmallScreen) 6.dp else if (isShortScreen) 8.dp else 12.dp
    // Initialize grade values from studentGrades
    var prelimGrade by remember { 
        mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }?.score?.toString() ?: "")
    }
    var midtermGrade by remember { 
        mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }?.score?.toString() ?: "")
    }
    var finalGrade by remember { 
        mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.FINAL }?.score?.toString() ?: "")
    }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showLockedGradeAlert by remember { mutableStateOf<GradePeriod?>(null) }
    var showInvalidGradeAlert by remember { mutableStateOf<String?>(null) }
    
    // Update grade values when studentGrades change (e.g., after saving)
    // This ensures the text fields reflect saved grades and lock status updates immediately
    LaunchedEffect(studentGrades.map { "${it.id}_${it.score}" }.joinToString()) {
        val prelim = studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }
        val midterm = studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }
        val final = studentGrades.find { it.gradePeriod == GradePeriod.FINAL }
        
        // Update text fields when grades are saved (when a grade with an ID exists)
        // This ensures lock status updates immediately after saving
        prelim?.let { grade ->
            if (grade.id.isNotEmpty()) {
                grade.score.toString().let { savedPrelim ->
                    // Update if different or if field was empty
                    if (prelimGrade != savedPrelim) {
                        prelimGrade = savedPrelim
                    }
                }
            }
        }
        midterm?.let { grade ->
            if (grade.id.isNotEmpty()) {
                grade.score.toString().let { savedMidterm ->
                    if (midtermGrade != savedMidterm) {
                        midtermGrade = savedMidterm
                    }
                }
            }
        }
        final?.let { grade ->
            if (grade.id.isNotEmpty()) {
                grade.score.toString().let { savedFinal ->
                    if (finalGrade != savedFinal) {
                        finalGrade = savedFinal
                    }
                }
            }
        }
    }
    
    // Auto-dismiss success/error messages
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            onClearMessage()
        }
    }
    
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(5000)
            onClearMessage()
        }
    }
    
    // Auto-dismiss locked grade alert after showing feedback
    LaunchedEffect(uiState.successMessage, uiState.error) {
        if (showLockedGradeAlert != null) {
            if (uiState.successMessage != null || uiState.error != null) {
                kotlinx.coroutines.delay(2000)
                showLockedGradeAlert = null
            }
        }
    }
    
    // Calculate lock status for all grades (recalculated when studentGrades changes)
    // A grade is considered locked if it exists (was previously saved) and hasn't been unlocked by admin
    // This handles both new grades with lock fields and old grades without lock fields
    // Use a key based on grade IDs and lock status to ensure recalculation when grades are updated
    // Create a key that includes all relevant grade properties to detect changes
    val gradesKey = studentGrades.map { "${it.id}_${it.isLocked}_${it.unlockedBy}_${it.editRequested}" }.joinToString() + (uiState.successMessage ?: "")
    
    // Use remember with mutableStateOf to force recomposition when grades change
    var prelimGradeObj by remember(gradesKey) { mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }) }
    var isPrelimLocked by remember(gradesKey) { 
        mutableStateOf(prelimGradeObj != null && prelimGradeObj!!.id.isNotEmpty() && prelimGradeObj!!.unlockedBy == null)
    }
    var prelimEditRequested by remember(gradesKey) { mutableStateOf(prelimGradeObj?.editRequested == true) }
    
    var midtermGradeObj by remember(gradesKey) { mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }) }
    var isMidtermLocked by remember(gradesKey) { 
        mutableStateOf(midtermGradeObj != null && midtermGradeObj!!.id.isNotEmpty() && midtermGradeObj!!.unlockedBy == null)
    }
    var midtermEditRequested by remember(gradesKey) { mutableStateOf(midtermGradeObj?.editRequested == true) }
    
    var finalGradeObj by remember(gradesKey) { mutableStateOf(studentGrades.find { it.gradePeriod == GradePeriod.FINAL }) }
    var isFinalLocked by remember(gradesKey) { 
        mutableStateOf(finalGradeObj != null && finalGradeObj!!.id.isNotEmpty() && finalGradeObj!!.unlockedBy == null)
    }
    var finalEditRequested by remember(gradesKey) { mutableStateOf(finalGradeObj?.editRequested == true) }
    
    // Update lock status when grades change
    LaunchedEffect(gradesKey) {
        prelimGradeObj = studentGrades.find { it.gradePeriod == GradePeriod.PRELIM }
        isPrelimLocked = prelimGradeObj != null && prelimGradeObj!!.id.isNotEmpty() && prelimGradeObj!!.unlockedBy == null
        prelimEditRequested = prelimGradeObj?.editRequested == true
        
        midtermGradeObj = studentGrades.find { it.gradePeriod == GradePeriod.MIDTERM }
        isMidtermLocked = midtermGradeObj != null && midtermGradeObj!!.id.isNotEmpty() && midtermGradeObj!!.unlockedBy == null
        midtermEditRequested = midtermGradeObj?.editRequested == true
        
        finalGradeObj = studentGrades.find { it.gradePeriod == GradePeriod.FINAL }
        isFinalLocked = finalGradeObj != null && finalGradeObj!!.id.isNotEmpty() && finalGradeObj!!.unlockedBy == null
        finalEditRequested = finalGradeObj?.editRequested == true
    }

    // Calculated Average
    val calculatedAverage = aggregate?.finalAverage?.let {
        GradeCalculationEngine.calculateLetterGrade(it)
    } ?: run {
        val prelim = prelimGrade.toDoubleOrNull()
        val midterm = midtermGrade.toDoubleOrNull()
        val final = finalGrade.toDoubleOrNull()
        if (prelim != null && midterm != null && final != null) {
            val avg = GradeCalculationEngine.calculateFinalAverage(prelim, midterm, final)
            avg?.let { GradeCalculationEngine.calculateLetterGrade(it) } ?: "INC"
        } else "INC"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isSmallScreen) 0.95f else 0.9f)
                .fillMaxHeight(if (isSmallScreen) 0.95f else if (isShortScreen) 0.92f else 0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dialogPadding)
            ) {
                // Header: Student Name and Count (compact)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = enrollment.studentName,
                        style = if (isSmallScreen) MaterialTheme.typography.titleSmall else if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(if (isSmallScreen || isShortScreen) 2.dp else 4.dp))
                    Text(
                        text = "Student ${currentIndex + 1} of $totalStudents",
                        style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(if (isSmallScreen || isShortScreen) 4.dp else 8.dp))
                
                // Content - Use weight to fill available space and make scrollable if needed
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(contentSpacing)
                ) {
                // Prelim Grade (lock status already calculated above)
                OutlinedTextField(
                    value = prelimGrade,
                    onValueChange = { 
                        if (!isPrelimLocked) {
                            prelimGrade = it
                            // Validate and show alert if exceeds limit or is negative
                            it.toDoubleOrNull()?.let { gradeValue ->
                                when {
                                    gradeValue < 0.0 -> {
                                        showInvalidGradeAlert = "Preliminary grade cannot be negative. The minimum allowed grade is 0."
                                    }
                                    gradeValue > 100.0 -> {
                                        showInvalidGradeAlert = "Preliminary grade cannot exceed 100. The maximum allowed grade is 100."
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("Preliminary (30%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isPrelimLocked,
                    readOnly = isPrelimLocked,
                    isError = prelimGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true,
                    trailingIcon = {
                        if (isPrelimLocked) {
                            val currentPrelimGrade = prelimGradeObj
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (!prelimEditRequested && currentPrelimGrade != null) {
                                    IconButton(
                                        onClick = { onRequestEdit(currentPrelimGrade.id) },
                                        enabled = !uiState.isLoading,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color(0xFFFF9800),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Request Edit",
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    supportingText = {
                        Column {
                            prelimGrade.toDoubleOrNull()?.let { grade ->
                                when {
                                    grade < 0.0 -> {
                                        Text(
                                            text = "Grade cannot be negative",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    grade > 100.0 -> {
                                        Text(
                                            text = "Grade cannot exceed 100",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "1-5 Scale: ${GradeCalculationEngine.calculateLetterGrade(grade)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (isPrelimLocked) {
                                Text(
                                    text = if (prelimEditRequested) "Edit permission requested" else "Grade is locked",
                                    color = if (prelimEditRequested) Color(0xFFFF9800) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                
                // Show "Request Pending" status below the field when request is made
                if (isPrelimLocked && prelimEditRequested) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isSmallScreen || isShortScreen) 6.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(if (isSmallScreen || isShortScreen) 16.dp else 18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Request Pending",
                                style = if (isSmallScreen || isShortScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                // Midterm Grade (lock status already calculated above)
                OutlinedTextField(
                    value = midtermGrade,
                    onValueChange = { 
                        if (!isMidtermLocked) {
                            midtermGrade = it
                            // Validate and show alert if exceeds limit or is negative
                            it.toDoubleOrNull()?.let { gradeValue ->
                                when {
                                    gradeValue < 0.0 -> {
                                        showInvalidGradeAlert = "Midterm grade cannot be negative. The minimum allowed grade is 0."
                                    }
                                    gradeValue > 100.0 -> {
                                        showInvalidGradeAlert = "Midterm grade cannot exceed 100. The maximum allowed grade is 100."
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("Midterm (30%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isMidtermLocked,
                    readOnly = isMidtermLocked,
                    isError = midtermGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true,
                    trailingIcon = {
                        if (isMidtermLocked) {
                            val currentMidtermGrade = midtermGradeObj
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (!midtermEditRequested && currentMidtermGrade != null) {
                                    IconButton(
                                        onClick = { onRequestEdit(currentMidtermGrade.id) },
                                        enabled = !uiState.isLoading,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color(0xFFFF9800),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Request Edit",
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    supportingText = {
                        Column {
                            midtermGrade.toDoubleOrNull()?.let { grade ->
                                when {
                                    grade < 0.0 -> {
                                        Text(
                                            text = "Grade cannot be negative",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    grade > 100.0 -> {
                                        Text(
                                            text = "Grade cannot exceed 100",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "1-5 Scale: ${GradeCalculationEngine.calculateLetterGrade(grade)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (isMidtermLocked) {
                                Text(
                                    text = if (midtermEditRequested) "Edit permission requested" else "Grade is locked",
                                    color = if (midtermEditRequested) Color(0xFFFF9800) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                
                // Show "Request Pending" status below the field when request is made
                if (isMidtermLocked && midtermEditRequested) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isSmallScreen || isShortScreen) 6.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(if (isSmallScreen || isShortScreen) 16.dp else 18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Request Pending",
                                style = if (isSmallScreen || isShortScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                // Final Grade (lock status already calculated above)
                OutlinedTextField(
                    value = finalGrade,
                    onValueChange = { 
                        if (!isFinalLocked) {
                            finalGrade = it
                            // Validate and show alert if exceeds limit or is negative
                            it.toDoubleOrNull()?.let { gradeValue ->
                                when {
                                    gradeValue < 0.0 -> {
                                        showInvalidGradeAlert = "Final grade cannot be negative. The minimum allowed grade is 0."
                                    }
                                    gradeValue > 100.0 -> {
                                        showInvalidGradeAlert = "Final grade cannot exceed 100. The maximum allowed grade is 100."
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("Final (40%)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isFinalLocked,
                    readOnly = isFinalLocked,
                    isError = finalGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true,
                    trailingIcon = {
                        if (isFinalLocked) {
                            val currentFinalGrade = finalGradeObj
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (!finalEditRequested && currentFinalGrade != null) {
                                    IconButton(
                                        onClick = { onRequestEdit(currentFinalGrade.id) },
                                        enabled = !uiState.isLoading,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color(0xFFFF9800),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Request Edit",
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    supportingText = {
                        Column {
                            finalGrade.toDoubleOrNull()?.let { grade ->
                                when {
                                    grade < 0.0 -> {
                                        Text(
                                            text = "Grade cannot be negative",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    grade > 100.0 -> {
                                        Text(
                                            text = "Grade cannot exceed 100",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "1-5 Scale: ${GradeCalculationEngine.calculateLetterGrade(grade)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (isFinalLocked) {
                                Text(
                                    text = if (finalEditRequested) "Edit permission requested" else "Grade is locked",
                                    color = if (finalEditRequested) Color(0xFFFF9800) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                
                // Show "Request Pending" status below the field when request is made
                if (isFinalLocked && finalEditRequested) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isSmallScreen || isShortScreen) 6.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(if (isSmallScreen || isShortScreen) 16.dp else 18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Request Pending",
                                style = if (isSmallScreen || isShortScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
                
                // Show success/error feedback at the bottom of the dialog
                uiState.successMessage?.let { message ->
                    Spacer(modifier = Modifier.height(if (isSmallScreen || isShortScreen) 4.dp else 6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(if (isSmallScreen || isShortScreen) 8.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen || isShortScreen) 6.dp else 8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isSmallScreen || isShortScreen) 16.dp else 18.dp)
                            )
                            Text(
                                text = message,
                                style = if (isSmallScreen || isShortScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(if (isSmallScreen || isShortScreen) 4.dp else 6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(if (isSmallScreen || isShortScreen) 8.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen || isShortScreen) 6.dp else 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isSmallScreen || isShortScreen) 16.dp else 18.dp)
                            )
                            Text(
                                text = error,
                                style = if (isSmallScreen || isShortScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                }
                
                // Final Average Section (outside scrollable area)
                Spacer(modifier = Modifier.height(if (isSmallScreen || isShortScreen) 2.dp else 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isSmallScreen) 8.dp else if (isShortScreen) 10.dp else 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Final Average:",
                            style = if (isSmallScreen) MaterialTheme.typography.bodyMedium else if (isShortScreen) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = calculatedAverage,
                            style = if (isSmallScreen) MaterialTheme.typography.titleMedium else if (isShortScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                calculatedAverage == "INC" -> Color(0xFF9E9E9E)
                                calculatedAverage.toDoubleOrNull()?.let { it <= 3.0 } == true -> Color(0xFF4CAF50)
                                calculatedAverage.toDoubleOrNull()?.let { it <= 3.5 } == true -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(if (isSmallScreen || isShortScreen) 4.dp else 8.dp))
                
                // Action Buttons Section (Fixed at bottom)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isSmallScreen || isShortScreen) 6.dp else 8.dp)
                ) {
                    // Save Button
                    val anyGradeLocked = isPrelimLocked || isMidtermLocked || isFinalLocked
                    // Check if any grade exceeds the limit or is negative
                    val hasInvalidGrade = prelimGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true ||
                                         midtermGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true ||
                                         finalGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true
                    
                    Button(
                        onClick = {
                            // Check if any grade exceeds limit or is negative
                            when {
                                prelimGrade.toDoubleOrNull()?.let { it < 0.0 } == true -> {
                                    showInvalidGradeAlert = "Preliminary grade cannot be negative. The minimum allowed grade is 0."
                                }
                                prelimGrade.toDoubleOrNull()?.let { it > 100.0 } == true -> {
                                    showInvalidGradeAlert = "Preliminary grade cannot exceed 100. The maximum allowed grade is 100."
                                }
                                midtermGrade.toDoubleOrNull()?.let { it < 0.0 } == true -> {
                                    showInvalidGradeAlert = "Midterm grade cannot be negative. The minimum allowed grade is 0."
                                }
                                midtermGrade.toDoubleOrNull()?.let { it > 100.0 } == true -> {
                                    showInvalidGradeAlert = "Midterm grade cannot exceed 100. The maximum allowed grade is 100."
                                }
                                finalGrade.toDoubleOrNull()?.let { it < 0.0 } == true -> {
                                    showInvalidGradeAlert = "Final grade cannot be negative. The minimum allowed grade is 0."
                                }
                                finalGrade.toDoubleOrNull()?.let { it > 100.0 } == true -> {
                                    showInvalidGradeAlert = "Final grade cannot exceed 100. The maximum allowed grade is 100."
                                }
                                else -> {
                                    // Check if trying to edit a locked grade
                                    val prelimChanged = prelimGrade.toDoubleOrNull()?.let { 
                                        val existing = prelimGradeObj?.score
                                        existing != null && existing != it
                                    } == true
                                    val midtermChanged = midtermGrade.toDoubleOrNull()?.let { 
                                        val existing = midtermGradeObj?.score
                                        existing != null && existing != it
                                    } == true
                                    val finalChanged = finalGrade.toDoubleOrNull()?.let { 
                                        val existing = finalGradeObj?.score
                                        existing != null && existing != it
                                    } == true
                                    
                                    // Check which locked grade is being edited
                                    when {
                                        prelimChanged && isPrelimLocked -> {
                                            showLockedGradeAlert = GradePeriod.PRELIM
                                        }
                                        midtermChanged && isMidtermLocked -> {
                                            showLockedGradeAlert = GradePeriod.MIDTERM
                                        }
                                        finalChanged && isFinalLocked -> {
                                            showLockedGradeAlert = GradePeriod.FINAL
                                        }
                                        else -> {
                                            showConfirmDialog = true
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && !hasInvalidGrade && (
                            prelimGrade.toDoubleOrNull()?.let { it in 0.0..100.0 } == true ||
                            midtermGrade.toDoubleOrNull()?.let { it in 0.0..100.0 } == true ||
                            finalGrade.toDoubleOrNull()?.let { it in 0.0..100.0 } == true
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Saving...",
                                style = if (isSmallScreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Save",
                                style = if (isSmallScreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    // Close Link
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Close",
                            style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Button
                        if (currentIndex > 0) {
                            OutlinedButton(
                                onClick = onNavigatePrevious,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2196F3)
                                )
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "< Previous",
                                    style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Next Button
                        if (currentIndex < totalStudents - 1) {
                            OutlinedButton(
                                onClick = onNavigateNext,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2196F3)
                                )
                            ) {
                                Text(
                                    "Next >",
                                    style = if (isSmallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(if (isSmallScreen) 16.dp else 18.dp))
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Save",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to save the grades for ${enrollment.studentName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (prelimGrade.toDoubleOrNull() != null) {
                        Text(
                            text = "Preliminary: ${prelimGrade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (midtermGrade.toDoubleOrNull() != null) {
                        Text(
                            text = "Midterm: ${midtermGrade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (finalGrade.toDoubleOrNull() != null) {
                        Text(
                            text = "Final: ${finalGrade}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Validate grades before saving
                        when {
                            prelimGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true -> {
                                showConfirmDialog = false
                                showInvalidGradeAlert = when {
                                    prelimGrade.toDoubleOrNull()?.let { it < 0.0 } == true -> 
                                        "Preliminary grade cannot be negative. The minimum allowed grade is 0."
                                    else -> 
                                        "Preliminary grade cannot exceed 100. The maximum allowed grade is 100."
                                }
                            }
                            midtermGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true -> {
                                showConfirmDialog = false
                                showInvalidGradeAlert = when {
                                    midtermGrade.toDoubleOrNull()?.let { it < 0.0 } == true -> 
                                        "Midterm grade cannot be negative. The minimum allowed grade is 0."
                                    else -> 
                                        "Midterm grade cannot exceed 100. The maximum allowed grade is 100."
                                }
                            }
                            finalGrade.toDoubleOrNull()?.let { it < 0.0 || it > 100.0 } == true -> {
                                showConfirmDialog = false
                                showInvalidGradeAlert = when {
                                    finalGrade.toDoubleOrNull()?.let { it < 0.0 } == true -> 
                                        "Final grade cannot be negative. The minimum allowed grade is 0."
                                    else -> 
                                        "Final grade cannot exceed 100. The maximum allowed grade is 100."
                                }
                            }
                            else -> {
                                // Check if trying to edit a locked grade before saving
                                val prelimChanged = prelimGrade.toDoubleOrNull()?.let { 
                                    val existing = prelimGradeObj?.score
                                    existing != null && existing != it
                                } == true
                                val midtermChanged = midtermGrade.toDoubleOrNull()?.let { 
                                    val existing = midtermGradeObj?.score
                                    existing != null && existing != it
                                } == true
                                val finalChanged = finalGrade.toDoubleOrNull()?.let { 
                                    val existing = finalGradeObj?.score
                                    existing != null && existing != it
                                } == true
                                
                                // Check which locked grade is being edited
                                when {
                                    prelimChanged && isPrelimLocked -> {
                                        showConfirmDialog = false
                                        showLockedGradeAlert = GradePeriod.PRELIM
                                    }
                                    midtermChanged && isMidtermLocked -> {
                                        showConfirmDialog = false
                                        showLockedGradeAlert = GradePeriod.MIDTERM
                                    }
                                    finalChanged && isFinalLocked -> {
                                        showConfirmDialog = false
                                        showLockedGradeAlert = GradePeriod.FINAL
                                    }
                                    else -> {
                                        // Save all grades (only valid ones)
                                        prelimGrade.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }?.let { 
                                            onSaveGrade(GradePeriod.PRELIM, it)
                                        }
                                        midtermGrade.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }?.let { 
                                            onSaveGrade(GradePeriod.MIDTERM, it)
                                        }
                                        finalGrade.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }?.let { 
                                            onSaveGrade(GradePeriod.FINAL, it)
                                        }
                                        showConfirmDialog = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Locked Grade Alert Dialog
    showLockedGradeAlert?.let { lockedPeriod ->
        val lockedGradeObj = when (lockedPeriod) {
            GradePeriod.PRELIM -> prelimGradeObj
            GradePeriod.MIDTERM -> midtermGradeObj
            GradePeriod.FINAL -> finalGradeObj
        }
        
        AlertDialog(
            onDismissRequest = { showLockedGradeAlert = null },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Grade is Locked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This ${lockedPeriod.displayName} grade has already been submitted and is locked.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To edit this grade, you need to request permission from an administrator.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show success/error feedback
                    uiState.successMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        lockedGradeObj?.let { grade ->
                            onRequestEdit(grade.id)
                            // Don't dismiss immediately - wait for feedback
                        }
                    },
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isLoading) "Requesting..." else "Ask Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockedGradeAlert = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Invalid Grade Alert Dialog
    showInvalidGradeAlert?.let { message ->
        AlertDialog(
            onDismissRequest = { showInvalidGradeAlert = null },
            icon = {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Invalid Grade",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Invalid Grade",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showInvalidGradeAlert = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
}

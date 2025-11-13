package com.smartacademictracker.presentation.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.PreRegisteredStudent
import com.smartacademictracker.data.repository.PreRegisteredRepository
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminPreRegisteredStudentsViewModel @Inject constructor(
    private val preRegisteredRepository: PreRegisteredRepository,
    private val courseRepository: CourseRepository,
    private val yearLevelRepository: YearLevelRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PreRegStudentsUiState())
    val uiState: StateFlow<PreRegStudentsUiState> = _uiState.asStateFlow()
    
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()
    
    private val _yearLevels = MutableStateFlow<List<YearLevel>>(emptyList())
    val yearLevels: StateFlow<List<YearLevel>> = _yearLevels.asStateFlow()
    
    init {
        loadPreRegisteredStudents()
        loadCourses()
    }
    
    fun loadPreRegisteredStudents(filterRegistered: Boolean? = null) {
        viewModelScope.launch {
            android.util.Log.d("PreRegisteredVM", "=== loadPreRegisteredStudents ===")
            android.util.Log.d("PreRegisteredVM", "filterRegistered parameter: $filterRegistered")
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = if (filterRegistered != null) {
                    android.util.Log.d("PreRegisteredVM", "Calling getPreRegisteredStudentsByStatus($filterRegistered)")
                    preRegisteredRepository.getPreRegisteredStudentsByStatus(filterRegistered)
                } else {
                    android.util.Log.d("PreRegisteredVM", "Calling getAllPreRegisteredStudents()")
                    preRegisteredRepository.getAllPreRegisteredStudents()
                }
                
                result.onSuccess { students ->
                    android.util.Log.d("PreRegisteredVM", "Success: Loaded ${students.size} students")
                    android.util.Log.d("PreRegisteredVM", "Students breakdown:")
                    students.forEach { student ->
                        android.util.Log.d("PreRegisteredVM", "  - ${student.studentId}: isRegistered=${student.isRegistered}, firebaseUserId=${student.firebaseUserId}")
                    }
                    
                    val registeredCount = students.count { it.isRegistered }
                    val pendingCount = students.count { !it.isRegistered }
                    android.util.Log.d("PreRegisteredVM", "Summary: Total=${students.size}, Registered=$registeredCount, Pending=$pendingCount")
                    
                    _uiState.value = _uiState.value.copy(
                        students = students,
                        filteredStudents = students,
                        isLoading = false
                    )
                }.onFailure { e ->
                    android.util.Log.e("PreRegisteredVM", "Error loading students: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load students"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PreRegisteredVM", "Exception loading students: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load students"
                )
            }
        }
    }
    
    private fun loadCourses() {
        viewModelScope.launch {
            courseRepository.getAllCourses().onSuccess { courseList ->
                _courses.value = courseList
            }
        }
    }
    
    fun loadYearLevelsForCourse(courseId: String) {
        viewModelScope.launch {
            yearLevelRepository.getYearLevelsByCourse(courseId).onSuccess { levels ->
                _yearLevels.value = levels
            }
        }
    }
    
    fun addPreRegisteredStudent(student: PreRegisteredStudent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = preRegisteredRepository.addPreRegisteredStudent(student)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddDialog = false
                    )
                    loadPreRegisteredStudents()
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to add student"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add student"
                )
            }
        }
    }
    
    fun deleteStudent(docId: String) {
        viewModelScope.launch {
            preRegisteredRepository.deletePreRegisteredStudent(docId).onSuccess {
                loadPreRegisteredStudents()
            }
        }
    }
    
    fun filterStudents(query: String) {
        val filtered = if (query.isBlank()) {
            _uiState.value.students
        } else {
            _uiState.value.students.filter {
                it.studentId.contains(query, ignoreCase = true) ||
                it.firstName.contains(query, ignoreCase = true) ||
                it.lastName.contains(query, ignoreCase = true) ||
                it.courseName.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(filteredStudents = filtered)
    }
    
    fun setShowAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddDialog = show)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class PreRegStudentsUiState(
    val students: List<PreRegisteredStudent> = emptyList(),
    val filteredStudents: List<PreRegisteredStudent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPreRegisteredStudentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBulkImport: () -> Unit = {},
    viewModel: AdminPreRegisteredStudentsViewModel = hiltViewModel(),
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val yearLevels by viewModel.yearLevels.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var filterRegistered by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(searchQuery) {
        viewModel.filterStudents(searchQuery)
    }
    
        LaunchedEffect(filterRegistered) {
            android.util.Log.d("AdminPreRegisteredScreen", "=== LaunchedEffect filterRegistered ===")
            android.util.Log.d("AdminPreRegisteredScreen", "filterRegistered value: $filterRegistered")
            viewModel.loadPreRegisteredStudents(filterRegistered)
        }
    
    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Pre-Registered Students") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadPreRegisteredStudents(filterRegistered) }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                        IconButton(onClick = onNavigateToBulkImport) {
                            Icon(Icons.Default.Upload, "Bulk Import")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2196F3),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddDialog(true) },
                containerColor = Color(0xFF4CAF50)
            ) {
                Icon(Icons.Default.Add, "Add Student", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search and Filter Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search") },
                        placeholder = { Text("Student ID, Name, Course...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterRegistered == null,
                            onClick = { filterRegistered = null },
                            label = { Text("All") },
                            leadingIcon = if (filterRegistered == null) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = filterRegistered == false,
                            onClick = { 
                                android.util.Log.d("AdminPreRegisteredScreen", "Pending filter clicked")
                                filterRegistered = false 
                            },
                            label = { Text("Pending") },
                            leadingIcon = if (filterRegistered == false) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFC107)
                            )
                        )
                        FilterChip(
                            selected = filterRegistered == true,
                            onClick = { 
                                android.util.Log.d("AdminPreRegisteredScreen", "Activated filter clicked")
                                filterRegistered = true 
                            },
                            label = { Text("Activated") },
                            leadingIcon = if (filterRegistered == true) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50)
                            )
                        )
                    }
                }
            }
            
            // Statistics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        title = "Total",
                        value = uiState.students.size.toString(),
                        icon = Icons.Default.People,
                        color = Color(0xFF2196F3)
                    )
                    StatItem(
                        title = "Pending",
                        value = uiState.students.count { !it.isRegistered }.toString(),
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFFC107)
                    )
                    StatItem(
                        title = "Activated",
                        value = uiState.students.count { it.isRegistered }.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            // Students List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No students found", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.filteredStudents) { student ->
                        StudentCard(
                            student = student,
                            onDelete = { viewModel.deleteStudent(student.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
    
    // Add Student Dialog
    if (uiState.showAddDialog) {
        AddStudentDialog(
            courses = courses,
            yearLevels = yearLevels,
            onLoadYearLevels = { viewModel.loadYearLevelsForCourse(it) },
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = { student -> viewModel.addPreRegisteredStudent(student) }
        )
    }
    
    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    student: PreRegisteredStudent,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // DEBUG: Log student status
    LaunchedEffect(student.studentId) {
        android.util.Log.d("StudentCard", "=== Rendering StudentCard ===")
        android.util.Log.d("StudentCard", "Student ID: ${student.studentId}")
        android.util.Log.d("StudentCard", "isRegistered: ${student.isRegistered}")
        android.util.Log.d("StudentCard", "firebaseUserId: ${student.firebaseUserId}")
        android.util.Log.d("StudentCard", "Will show status: ${if (student.isRegistered) "Activated" else "Awaiting Activation"}")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (student.isRegistered) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            student.studentId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (student.isRegistered) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Activated",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${student.firstName} ${student.middleName ?: ""} ${student.lastName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFF44336))
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.School,
                    text = "${student.courseCode} - ${student.yearLevelName}"
                )
            }
            
            if (!student.isRegistered) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Awaiting Activation",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF57C00),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF2196F3)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1976D2)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    courses: List<Course>,
    yearLevels: List<YearLevel>,
    onLoadYearLevels: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (PreRegisteredStudent) -> Unit
) {
    var studentId by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var selectedYearLevel by remember { mutableStateOf<YearLevel?>(null) }
    var enrollmentYear by remember { mutableStateOf("2024-2025") }
    
    LaunchedEffect(selectedCourse) {
        selectedCourse?.let { onLoadYearLevels(it.id) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pre-Registered Student") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("Student ID") },
                        placeholder = { Text("e.g., 2024-1234") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = middleName,
                        onValueChange = { middleName = it },
                        label = { Text("Middle Name (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (Optional)") },
                        placeholder = { Text("e.g., student@university.edu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    var courseExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = courseExpanded,
                        onExpandedChange = { courseExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCourse?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Course") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courseExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = courseExpanded,
                            onDismissRequest = { courseExpanded = false }
                        ) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.code} - ${course.name}") },
                                    onClick = {
                                        selectedCourse = course
                                        selectedYearLevel = null
                                        courseExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    var yearLevelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = yearLevelExpanded,
                        onExpandedChange = { yearLevelExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedYearLevel?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year Level") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearLevelExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = selectedCourse != null
                        )
                        ExposedDropdownMenu(
                            expanded = yearLevelExpanded,
                            onDismissRequest = { yearLevelExpanded = false }
                        ) {
                            yearLevels.forEach { yearLevel ->
                                DropdownMenuItem(
                                    text = { Text(yearLevel.name) },
                                    onClick = {
                                        selectedYearLevel = yearLevel
                                        yearLevelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = enrollmentYear,
                        onValueChange = { enrollmentYear = it },
                        label = { Text("Enrollment Year") },
                        placeholder = { Text("e.g., 2024-2025") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val student = PreRegisteredStudent(
                        studentId = studentId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName.ifBlank { null },
                        email = email.ifBlank { null },
                        courseId = selectedCourse?.id ?: "",
                        courseName = selectedCourse?.name ?: "",
                        courseCode = selectedCourse?.code ?: "",
                        yearLevelId = selectedYearLevel?.id ?: "",
                        yearLevelName = selectedYearLevel?.name ?: "",
                        section = null,
                        enrollmentYear = enrollmentYear,
                        createdBy = "", // Will be set by repository
                        createdByName = "", // Will be set by repository
                        isRegistered = false
                    )
                    onConfirm(student)
                },
                enabled = studentId.isNotBlank() &&
                        firstName.isNotBlank() &&
                        lastName.isNotBlank() &&
                        selectedCourse != null &&
                        selectedYearLevel != null
            ) {
                Text("Add Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

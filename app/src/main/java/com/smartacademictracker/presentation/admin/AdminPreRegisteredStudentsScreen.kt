package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Always load ALL students first
                val allStudentsResult = preRegisteredRepository.getAllPreRegisteredStudents()

                allStudentsResult.onSuccess { allStudents ->
                    val registeredCount = allStudents.count { it.isRegistered }
                    val pendingCount = allStudents.count { !it.isRegistered }

                    // Apply filter to filteredStudents only, keep all students in students
                    val filtered = if (filterRegistered != null) {
                        allStudents.filter { it.isRegistered == filterRegistered }
                    } else {
                        allStudents
                    }

                    _uiState.value = _uiState.value.copy(
                        students = allStudents, // Always keep full list for statistics
                        filteredStudents = filtered, // Filtered list for display
                        searchFilteredStudents = allStudents, // Reset search results when reloading
                        isLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load students"
                    )
                }
            } catch (e: Exception) {
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
        val searchFiltered = if (query.isBlank()) {
            _uiState.value.students
        } else {
            _uiState.value.students.filter {
                it.studentId.contains(query, ignoreCase = true) ||
                        it.firstName.contains(query, ignoreCase = true) ||
                        it.lastName.contains(query, ignoreCase = true) ||
                        it.courseName.contains(query, ignoreCase = true)
            }
        }
        // Store search results separately and apply current status filter
        _uiState.value = _uiState.value.copy(
            searchFilteredStudents = searchFiltered,
            filteredStudents = searchFiltered // Will be filtered by status if needed
        )
    }
    
    fun applyStatusFilter(filterRegistered: Boolean?, searchQuery: String) {
        val baseList = if (searchQuery.isNotBlank()) {
            // Use search-filtered results as base
            _uiState.value.searchFilteredStudents
        } else {
            // Use all students as base
            _uiState.value.students
        }
        val filtered = if (filterRegistered != null) {
            baseList.filter { it.isRegistered == filterRegistered }
        } else {
            baseList
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
    val searchFilteredStudents: List<PreRegisteredStudent> = emptyList(), // Base search results
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPreRegisteredStudentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBulkImport: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AdminPreRegisteredStudentsViewModel = hiltViewModel(),
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val yearLevels by viewModel.yearLevels.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterRegistered by remember { mutableStateOf<Boolean?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        viewModel.filterStudents(searchQuery)
        // Re-apply status filter if one is active
        viewModel.applyStatusFilter(filterRegistered, searchQuery)
    }

    LaunchedEffect(filterRegistered) {
        if (searchQuery.isNotBlank()) {
            // If search is active, apply filter to search results without reloading
            viewModel.applyStatusFilter(filterRegistered, searchQuery)
        } else {
            // If no search, reload from database with filter
            viewModel.loadPreRegisteredStudents(filterRegistered)
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2196F3),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
        ) { padding ->
            Column(
                modifier = modifier
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSearchBar) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Student ID, Name, Course...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showSearchBar = false
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.Default.Close, "Close")
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Add Students Button (Icon only when search is active)
                            Box {
                                IconButton(
                                    onClick = { showAddMenu = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Students",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Add Student")
                                            }
                                        },
                                        onClick = {
                                            viewModel.setShowAddDialog(true)
                                            showAddMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Upload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Import Students")
                                            }
                                        },
                                        onClick = {
                                            onNavigateToBulkImport()
                                            showAddMenu = false
                                        }
                                    )
                                }
                            }
                        } else {
                            // Search Icon Button
                            IconButton(
                                onClick = { showSearchBar = true }
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Refresh Button (only shown when search is not active)
                            IconButton(
                                onClick = { viewModel.loadPreRegisteredStudents(filterRegistered) }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Add Students Button with Dropdown Menu (full button when search is not active)
                            Box {
                                Button(
                                    onClick = { showAddMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Students",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Students")
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Add Student")
                                            }
                                        },
                                        onClick = {
                                            viewModel.setShowAddDialog(true)
                                            showAddMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Upload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Import Students")
                                            }
                                        },
                                        onClick = {
                                            onNavigateToBulkImport()
                                            showAddMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Statistics Card (Clickable Filter)
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
                        // Use search-filtered students if search is active (for consistent stats),
                        // otherwise use all students
                        val studentsForStats = if (searchQuery.isNotBlank()) {
                            uiState.searchFilteredStudents
                        } else {
                            uiState.students
                        }
                        
                        StatItem(
                            title = "Total",
                            value = studentsForStats.size.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF2196F3),
                            onClick = { filterRegistered = null },
                            isSelected = filterRegistered == null,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            title = "Pending",
                            value = studentsForStats.count { !it.isRegistered }.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = Color(0xFFFFC107),
                            onClick = { filterRegistered = false },
                            isSelected = filterRegistered == false,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            title = "Activated",
                            value = studentsForStats.count { it.isRegistered }.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            onClick = { filterRegistered = true },
                            isSelected = filterRegistered == true,
                            modifier = Modifier.weight(1f)
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
    } else {
        // Content without Scaffold (when wrapped with bottom nav)
        Scaffold { padding ->
            Column(
                modifier = modifier.fillMaxSize()
            ) {
                // Search and Filter Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showSearchBar) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ID, Name, Course") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showSearchBar = false
                                        searchQuery = ""
                                    }) {
                                        Icon(Icons.Default.Close, "Close")
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            // Add Students Button (Icon only when search is active)
                            Box {
                                IconButton(
                                    onClick = { showAddMenu = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Students",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Add Student")
                                            }
                                        },
                                        onClick = {
                                            viewModel.setShowAddDialog(true)
                                            showAddMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Upload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Import Students")
                                            }
                                        },
                                        onClick = {
                                            onNavigateToBulkImport()
                                            showAddMenu = false
                                        }
                                    )
                                }
                            }
                        } else {
                            // Search Icon Button
                            IconButton(
                                onClick = { showSearchBar = true }
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Refresh Button (only shown when search is not active)
                            IconButton(
                                onClick = { viewModel.loadPreRegisteredStudents(filterRegistered) }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Add Students Button with Dropdown Menu (full button when search is not active)
                            Box {
                                Button(
                                    onClick = { showAddMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Students",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Students")
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.PersonAdd,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Add Student")
                                            }
                                        },
                                        onClick = {
                                            viewModel.setShowAddDialog(true)
                                            showAddMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Upload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text("Import Students")
                                            }
                                        },
                                        onClick = {
                                            onNavigateToBulkImport()
                                            showAddMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Statistics Card (Clickable Filter)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Use search-filtered students if search is active (for consistent stats),
                        // otherwise use all students
                        val studentsForStats = if (searchQuery.isNotBlank()) {
                            uiState.searchFilteredStudents
                        } else {
                            uiState.students
                        }
                        
                        StatItem(
                            title = "Total",
                            value = studentsForStats.size.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF2196F3),
                            onClick = { filterRegistered = null },
                            isSelected = filterRegistered == null,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            title = "Pending",
                            value = studentsForStats.count { !it.isRegistered }.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = Color(0xFFFFC107),
                            onClick = { filterRegistered = false },
                            isSelected = filterRegistered == false,
                            modifier = Modifier.weight(1f)
                        )
                        StatItem(
                            title = "Activated",
                            value = studentsForStats.count { it.isRegistered }.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            onClick = { filterRegistered = true },
                            isSelected = filterRegistered == true,
                            modifier = Modifier.weight(1f)
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
                    ) {
                        items(uiState.filteredStudents) { student ->
                            StudentCard(
                                student = student,
                                onDelete = { viewModel.deleteStudent(student.id) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
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
    color: Color,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    student: PreRegisteredStudent,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    icon = Icons.Default.School,
                    text = "${student.courseCode} - ${student.yearLevelName}"
                )

                if (!student.isRegistered) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Awaiting Activation",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF57C00),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                "Activated",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
    var idError by remember { mutableStateOf<String?>(null) }
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
                // ID Format Warning Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Student ID Format Requirements",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = "Format: YYYY-SEQUENCE",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• Year: Any 4-digit year (e.g., 1952, 2001, 2099)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    text = "• Sequence: 1-99999 (1-2 digits auto-padded: 1→001, 23→023)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    text = "• Examples: 2024-001, 2025-123, 2030-1000, 2030-15234",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { newValue: String ->
                            studentId = newValue
                            // Validate format as user types
                            if (newValue.isNotBlank()) {
                                // Check if it's a valid format that can be formatted
                                val formatted = com.smartacademictracker.util.IdValidator.formatStudentId(newValue)
                                if (formatted != null) {
                                    // Format the ID (this will zero-pad the sequence)
                                    if (formatted != newValue) {
                                        studentId = formatted
                                    }
                                    // Validate formatted ID
                                    val validation = com.smartacademictracker.util.IdValidator.validateStudentId(formatted)
                                    idError = if (!validation.isValid) validation.errorMessage else null
                                } else {
                                    // Check if it's a partial input (e.g., "2024-", "2024-1", or "2024-12345")
                                    if (newValue.matches(Regex("^\\d{4}-\\d{0,5}$")) || newValue.matches(Regex("^\\d{0,4}$"))) {
                                        idError = null // Still typing, no error yet
                                    } else if (newValue.contains("-")) {
                                        idError = "Invalid format. Expected: YYYY-SEQUENCE (e.g., 2024-001, 2025-123, 2030-1000). Maximum sequence: 99999"
                                    } else {
                                        idError = null
                                    }
                                }
                            } else {
                                idError = null
                            }
                        },
                        label = { Text("Student ID") },
                        placeholder = { Text("e.g., 2024-1, 2024-001, or 2030-1000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = idError != null,
                        supportingText = idError?.let { errorMsg ->
                            { Text(errorMsg) }
                        }
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
                    var emailError by remember { mutableStateOf<String?>(null) }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            emailError = null
                        },
                        label = { Text("Email *") },
                        placeholder = { Text("e.g., student@university.edu") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = emailError?.let { errorMsg ->
                            { Text(errorMsg) }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        )
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
                    // Format and validate ID before submission
                    val formattedId = com.smartacademictracker.util.IdValidator.formatStudentId(studentId)
                    if (formattedId == null) {
                        idError = "Invalid Student ID format. Expected: YYYY-SEQUENCE (e.g., 2024-001, 2025-123, 2030-1000). Maximum sequence: 99999"
                        return@Button
                    }
                    
                    val validation = com.smartacademictracker.util.IdValidator.validateStudentId(formattedId)
                    val finalId = formattedId
                    
                    if (!validation.isValid) {
                        idError = validation.errorMessage
                        return@Button
                    }
                    
                    // Validate email is required
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isBlank()) {
                        return@Button // Email validation will be shown by the field
                    }
                    
                    // Validate email format
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        return@Button // Email validation will be shown by the field
                    }
                    
                    val student = PreRegisteredStudent(
                        studentId = finalId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName.ifBlank { null },
                        email = trimmedEmail,
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
                        email.trim().isNotBlank() &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
                        selectedCourse != null &&
                        selectedYearLevel != null &&
                        idError == null
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
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartacademictracker.data.model.PreRegisteredTeacher
import com.smartacademictracker.data.model.EmploymentType
import com.smartacademictracker.data.repository.PreRegisteredRepository
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.model.Course
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminPreRegisteredTeachersViewModel @Inject constructor(
    private val preRegisteredRepository: PreRegisteredRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreRegTeachersUiState())
    val uiState: StateFlow<PreRegTeachersUiState> = _uiState.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    init {
        loadPreRegisteredTeachers()
        loadCourses()
    }

    fun loadPreRegisteredTeachers(filterRegistered: Boolean? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Always load ALL teachers first
                val allTeachersResult = preRegisteredRepository.getAllPreRegisteredTeachers()

                allTeachersResult.onSuccess { allTeachers ->
                    // Apply filter to filteredTeachers only, keep all teachers in teachers
                    val filtered = if (filterRegistered != null) {
                        allTeachers.filter { it.isRegistered == filterRegistered }
                    } else {
                        allTeachers
                    }

                    _uiState.value = _uiState.value.copy(
                        teachers = allTeachers, // Always keep full list for statistics
                        filteredTeachers = filtered, // Filtered list for display
                        searchFilteredTeachers = allTeachers, // Reset search results when reloading
                        isLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load teachers"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load teachers"
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

    fun addPreRegisteredTeacher(teacher: PreRegisteredTeacher) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = preRegisteredRepository.addPreRegisteredTeacher(teacher)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showAddDialog = false
                    )
                    loadPreRegisteredTeachers()
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to add teacher"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add teacher"
                )
            }
        }
    }

    fun deleteTeacher(docId: String) {
        viewModelScope.launch {
            preRegisteredRepository.deletePreRegisteredTeacher(docId).onSuccess {
                loadPreRegisteredTeachers()
            }
        }
    }

    fun filterTeachers(query: String) {
        val searchFiltered = if (query.isBlank()) {
            _uiState.value.teachers
        } else {
            _uiState.value.teachers.filter {
                it.teacherId.contains(query, ignoreCase = true) ||
                        it.firstName.contains(query, ignoreCase = true) ||
                        it.lastName.contains(query, ignoreCase = true) ||
                        it.departmentCourseName.contains(query, ignoreCase = true)
            }
        }
        // Store search results separately and apply current status filter
        _uiState.value = _uiState.value.copy(
            searchFilteredTeachers = searchFiltered,
            filteredTeachers = searchFiltered // Will be filtered by status if needed
        )
    }

    fun applyStatusFilter(filterRegistered: Boolean?, searchQuery: String) {
        val baseList = if (searchQuery.isNotBlank()) {
            // Use search-filtered results as base
            _uiState.value.searchFilteredTeachers
        } else {
            // Use all teachers as base
            _uiState.value.teachers
        }
        val filtered = if (filterRegistered != null) {
            baseList.filter { it.isRegistered == filterRegistered }
        } else {
            baseList
        }
        _uiState.value = _uiState.value.copy(filteredTeachers = filtered)
    }

    fun setShowAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddDialog = show)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class PreRegTeachersUiState(
    val teachers: List<PreRegisteredTeacher> = emptyList(),
    val filteredTeachers: List<PreRegisteredTeacher> = emptyList(),
    val searchFilteredTeachers: List<PreRegisteredTeacher> = emptyList(), // Base search results
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPreRegisteredTeachersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBulkImport: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AdminPreRegisteredTeachersViewModel = hiltViewModel(),
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterRegistered by remember { mutableStateOf<Boolean?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        viewModel.filterTeachers(searchQuery)
        // Re-apply status filter if one is active
        viewModel.applyStatusFilter(filterRegistered, searchQuery)
    }

    LaunchedEffect(filterRegistered) {
        if (searchQuery.isNotBlank()) {
            // If search is active, apply filter to search results without reloading
            viewModel.applyStatusFilter(filterRegistered, searchQuery)
        } else {
            // If no search, reload from database with filter
            viewModel.loadPreRegisteredTeachers(filterRegistered)
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pre-Registered Teachers") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadPreRegisteredTeachers(filterRegistered) }) {
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
                                placeholder = { Text("ID, Name, Department") },
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

                            // Add Teachers Button (Icon only when search is active)
                            Box {
                                IconButton(
                                    onClick = { showAddMenu = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Teachers",
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
                                                Text("Add Teacher")
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
                                                Text("Import Teachers")
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
                                onClick = { viewModel.loadPreRegisteredTeachers(filterRegistered) }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Add Teachers Button with Dropdown Menu (full button when search is not active)
                            Box {
                                Button(
                                    onClick = { showAddMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Teachers",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Teachers")
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
                                                Text("Add Teacher")
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
                                                Text("Import Teachers")
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Use search-filtered teachers if search is active (for consistent stats),
                        // otherwise use all teachers
                        val teachersForStats = if (searchQuery.isNotBlank()) {
                            uiState.searchFilteredTeachers
                        } else {
                            uiState.teachers
                        }

                        TeacherStatItem(
                            title = "Total",
                            value = teachersForStats.size.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF2196F3),
                            onClick = { filterRegistered = null },
                            isSelected = filterRegistered == null,
                            modifier = Modifier.weight(1f)
                        )
                        TeacherStatItem(
                            title = "Pending",
                            value = teachersForStats.count { !it.isRegistered }.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = Color(0xFFFFC107),
                            onClick = { filterRegistered = false },
                            isSelected = filterRegistered == false,
                            modifier = Modifier.weight(1f)
                        )
                        TeacherStatItem(
                            title = "Activated",
                            value = teachersForStats.count { it.isRegistered }.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            onClick = { filterRegistered = true },
                            isSelected = filterRegistered == true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Teachers List
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredTeachers.isEmpty()) {
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
                            Text("No teachers found", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(uiState.filteredTeachers) { teacher ->
                            TeacherCard(
                                teacher = teacher,
                                onDelete = { viewModel.deleteTeacher(teacher.id) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    } else {
        // Content without Scaffold (when wrapped with bottom nav)
        Scaffold(
        ) { padding ->
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
                                placeholder = { Text("Teacher ID, Name, Department...") },
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

                            // Add Teachers Button (Icon only when search is active)
                            Box {
                                IconButton(
                                    onClick = { showAddMenu = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Teachers",
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
                                                Text("Add Teacher")
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
                                                Text("Import Teachers")
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
                                onClick = { viewModel.loadPreRegisteredTeachers(filterRegistered) }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Add Teachers Button with Dropdown Menu (full button when search is not active)
                            Box {
                                Button(
                                    onClick = { showAddMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Teachers",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Teachers")
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
                                                Text("Add Teacher")
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
                                                Text("Import Teachers")
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
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Use search-filtered teachers if search is active (for consistent stats),
                        // otherwise use all teachers
                        val teachersForStats = if (searchQuery.isNotBlank()) {
                            uiState.searchFilteredTeachers
                        } else {
                            uiState.teachers
                        }

                        TeacherStatItem(
                            title = "Total",
                            value = teachersForStats.size.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF2196F3),
                            onClick = { filterRegistered = null },
                            isSelected = filterRegistered == null,
                            modifier = Modifier.weight(1f)
                        )
                        TeacherStatItem(
                            title = "Pending",
                            value = teachersForStats.count { !it.isRegistered }.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = Color(0xFFFFC107),
                            onClick = { filterRegistered = false },
                            isSelected = filterRegistered == false,
                            modifier = Modifier.weight(1f)
                        )
                        TeacherStatItem(
                            title = "Activated",
                            value = teachersForStats.count { it.isRegistered }.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            onClick = { filterRegistered = true },
                            isSelected = filterRegistered == true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Teachers List
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredTeachers.isEmpty()) {
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
                            Text("No teachers found", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(uiState.filteredTeachers) { teacher ->
                            TeacherCard(
                                teacher = teacher,
                                onDelete = { viewModel.deleteTeacher(teacher.id) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Teacher Dialog
    if (uiState.showAddDialog) {
        AddTeacherDialog(
            courses = courses,
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = { teacher -> viewModel.addPreRegisteredTeacher(teacher) }
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
fun TeacherStatItem(
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
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null
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
fun TeacherCard(
    teacher: PreRegisteredTeacher,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (teacher.isRegistered) Color(0xFFE8F5E9) else Color.White
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
                            teacher.teacherId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (teacher.isRegistered) {
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
                        "${teacher.firstName} ${teacher.middleName ?: ""} ${teacher.lastName}",
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
                    text = teacher.departmentCourseCode
                )

                if (!teacher.isRegistered) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeacherDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onConfirm: (PreRegisteredTeacher) -> Unit
) {
    var teacherId by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var middleName by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf<Course?>(null) }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pre-Registered Teacher") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = teacherId,
                        onValueChange = { teacherId = it },
                        label = { Text("Teacher ID") },
                        placeholder = { Text("e.g., T-2024-001") },
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
                    var departmentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = departmentExpanded,
                        onExpandedChange = { departmentExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDepartment?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Department") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(departmentExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = departmentExpanded,
                            onDismissRequest = { departmentExpanded = false }
                        ) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.code} - ${course.name}") },
                                    onClick = {
                                        selectedDepartment = course
                                        departmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                        placeholder = { Text("e.g., teacher@school.edu") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate email is required
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isBlank()) {
                        return@Button // Email validation will be shown by the field
                    }
                    
                    // Validate email format
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        return@Button // Email validation will be shown by the field
                    }
                    
                    val teacher = PreRegisteredTeacher(
                        teacherId = teacherId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName.ifBlank { null },
                        departmentCourseId = selectedDepartment?.id ?: "",
                        departmentCourseName = selectedDepartment?.name ?: "",
                        departmentCourseCode = selectedDepartment?.code ?: "",
                        employmentType = EmploymentType.FULL_TIME, // Default value
                        position = null,
                        specialization = null,
                        email = trimmedEmail,
                        createdBy = "", // Will be set by repository
                        createdByName = "", // Will be set by repository
                        isRegistered = false
                    )
                    onConfirm(teacher)
                },
                enabled = teacherId.isNotBlank() &&
                        firstName.isNotBlank() &&
                        lastName.isNotBlank() &&
                        email.trim().isNotBlank() &&
                        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
                        selectedDepartment != null
            ) {
                Text("Add Teacher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
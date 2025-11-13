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
                val result = if (filterRegistered != null) {
                    preRegisteredRepository.getPreRegisteredTeachersByStatus(filterRegistered)
                } else {
                    preRegisteredRepository.getAllPreRegisteredTeachers()
                }
                
                result.onSuccess { teachers ->
                    _uiState.value = _uiState.value.copy(
                        teachers = teachers,
                        filteredTeachers = teachers,
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
        val filtered = if (query.isBlank()) {
            _uiState.value.teachers
        } else {
            _uiState.value.teachers.filter {
                it.teacherId.contains(query, ignoreCase = true) ||
                it.firstName.contains(query, ignoreCase = true) ||
                it.lastName.contains(query, ignoreCase = true) ||
                it.departmentCourseName.contains(query, ignoreCase = true)
            }
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPreRegisteredTeachersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBulkImport: () -> Unit = {},
    viewModel: AdminPreRegisteredTeachersViewModel = hiltViewModel(),
    showTopBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val courses by viewModel.courses.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var filterRegistered by remember { mutableStateOf<Boolean?>(null) }
    
    LaunchedEffect(searchQuery) {
        viewModel.filterTeachers(searchQuery)
    }
    
    LaunchedEffect(filterRegistered) {
        viewModel.loadPreRegisteredTeachers(filterRegistered)
    }
    
    Scaffold(
        topBar = {
            if (showTopBar) {
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
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddDialog(true) },
                containerColor = Color(0xFF4CAF50)
            ) {
                Icon(Icons.Default.Add, "Add Teacher", tint = Color.White)
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
                        placeholder = { Text("Teacher ID, Name, Department...") },
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
                            onClick = { filterRegistered = false },
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
                            onClick = { filterRegistered = true },
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
                    TeacherStatItem(
                        title = "Total",
                        value = uiState.teachers.size.toString(),
                        icon = Icons.Default.People,
                        color = Color(0xFF2196F3)
                    )
                    TeacherStatItem(
                        title = "Pending",
                        value = uiState.teachers.count { !it.isRegistered }.toString(),
                        icon = Icons.Default.HourglassEmpty,
                        color = Color(0xFFFFC107)
                    )
                    TeacherStatItem(
                        title = "Activated",
                        value = uiState.teachers.count { it.isRegistered }.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
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
                        .padding(horizontal = 16.dp)
                ) {
                    items(uiState.filteredTeachers) { teacher ->
                        TeacherCard(
                            teacher = teacher,
                            onDelete = { viewModel.deleteTeacher(teacher.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
fun TeacherCard(
    teacher: PreRegisteredTeacher,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                teacher.position?.let {
                    InfoChip(icon = Icons.Default.Work, text = it)
                }
                InfoChip(
                    icon = Icons.Default.Badge,
                    text = teacher.employmentType.displayName
                )
            }
            
            if (!teacher.isRegistered) {
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
    var selectedEmploymentType by remember { mutableStateOf(EmploymentType.FULL_TIME) }
    var position by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    
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
                    var employmentExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = employmentExpanded,
                        onExpandedChange = { employmentExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEmploymentType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Employment Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(employmentExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = employmentExpanded,
                            onDismissRequest = { employmentExpanded = false }
                        ) {
                            EmploymentType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        selectedEmploymentType = type
                                        employmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text("Position (Optional)") },
                        placeholder = { Text("e.g., Professor, Instructor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = specialization,
                        onValueChange = { specialization = it },
                        label = { Text("Specialization (Optional)") },
                        placeholder = { Text("e.g., Software Engineering") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val teacher = PreRegisteredTeacher(
                        teacherId = teacherId,
                        firstName = firstName,
                        lastName = lastName,
                        middleName = middleName.ifBlank { null },
                        departmentCourseId = selectedDepartment?.id ?: "",
                        departmentCourseName = selectedDepartment?.name ?: "",
                        departmentCourseCode = selectedDepartment?.code ?: "",
                        employmentType = selectedEmploymentType,
                        position = position.ifBlank { null },
                        specialization = specialization.ifBlank { null },
                        createdBy = "", // Will be set by repository
                        createdByName = "", // Will be set by repository
                        isRegistered = false
                    )
                    onConfirm(teacher)
                },
                enabled = teacherId.isNotBlank() &&
                        firstName.isNotBlank() &&
                        lastName.isNotBlank() &&
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


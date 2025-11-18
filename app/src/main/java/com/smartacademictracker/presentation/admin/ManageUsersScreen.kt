package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.presentation.common.LoadingStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    val teacherAssignments by viewModel.teacherAssignments.collectAsState()
    val studentEnrollments by viewModel.studentEnrollments.collectAsState()
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Load data in background - don't block navigation
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.border(
                    width = 2.dp,
                    color = Color.Red,
                    shape = RoundedCornerShape(0.dp)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    top = 0.dp,
                    bottom = padding.calculateBottomPadding(),
                    start = padding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = padding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
                .background(Color(0xFFF8F9FA))
        ) {
            // Show loading state with label
            if (uiState.isLoading && users.isEmpty()) {
                LoadingStateCard(
                    title = "Loading Users",
                    message = "Please wait while we load user accounts and their information"
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    // Reduced space and compact layout
                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact Search and Filter Section - moved closer to header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            // Search Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showSearchBar) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search users...") },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                showSearchBar = false
                                                searchQuery = ""
                                            }) {
                                                Icon(Icons.Default.Close, "Close")
                                            }
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                } else {
                                    IconButton(
                                        onClick = { showSearchBar = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color(0xFF666666),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Show filter chips inline when search is not active
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        item {
                                            EnhancedFilterChip(
                                                onClick = { selectedRole = null },
                                                label = "All",
                                                selected = selectedRole == null,
                                                icon = Icons.Default.People,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                        item {
                                            EnhancedFilterChip(
                                                onClick = { selectedRole = UserRole.STUDENT },
                                                label = "Students",
                                                selected = selectedRole == UserRole.STUDENT,
                                                icon = Icons.Default.School,
                                                color = Color(0xFF2196F3)
                                            )
                                        }
                                        item {
                                            EnhancedFilterChip(
                                                onClick = { selectedRole = UserRole.TEACHER },
                                                label = "Teachers",
                                                selected = selectedRole == UserRole.TEACHER,
                                                icon = Icons.Default.Work,
                                                color = Color(0xFFFF9800)
                                            )
                                        }
                                        item {
                                            EnhancedFilterChip(
                                                onClick = { selectedRole = UserRole.ADMIN },
                                                label = "Admins",
                                                selected = selectedRole == UserRole.ADMIN,
                                                icon = Icons.Default.Security,
                                                color = Color(0xFF9C27B0)
                                            )
                                        }
                                    }
                                }
                            }

                            // Show filter chips below search when search is active
                            if (showSearchBar) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        EnhancedFilterChip(
                                            onClick = { selectedRole = null },
                                            label = "All",
                                            selected = selectedRole == null,
                                            icon = Icons.Default.People,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    item {
                                        EnhancedFilterChip(
                                            onClick = { selectedRole = UserRole.STUDENT },
                                            label = "Students",
                                            selected = selectedRole == UserRole.STUDENT,
                                            icon = Icons.Default.School,
                                            color = Color(0xFF2196F3)
                                        )
                                    }
                                    item {
                                        EnhancedFilterChip(
                                            onClick = { selectedRole = UserRole.TEACHER },
                                            label = "Teachers",
                                            selected = selectedRole == UserRole.TEACHER,
                                            icon = Icons.Default.Work,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                    item {
                                        EnhancedFilterChip(
                                            onClick = { selectedRole = UserRole.ADMIN },
                                            label = "Admins",
                                            selected = selectedRole == UserRole.ADMIN,
                                            icon = Icons.Default.Security,
                                            color = Color(0xFF9C27B0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Enhanced Loading State
                    if (uiState.isLoading) {
                        LoadingStateCard(
                            title = "Loading Users",
                            message = "Please wait while we load user accounts and their information"
                        )
                    } else {
                        // Users List
                        val filteredUsers = users.filter { user ->
                            val matchesSearch = searchQuery.isEmpty() ||
                                    user.firstName.contains(searchQuery, ignoreCase = true) ||
                                    user.lastName.contains(searchQuery, ignoreCase = true) ||
                                    user.email.contains(searchQuery, ignoreCase = true)

                            // Create a local variable for selectedRole
                            val currentSelectedRole = selectedRole
                            val matchesRole = currentSelectedRole == null ||
                                    user.role == currentSelectedRole.name // Now use currentSelectedRole

                            matchesSearch && matchesRole
                        }

                        if (filteredUsers.isEmpty()) {
                            // Enhanced Empty State
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Icon with background
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = "No Users",
                                            modifier = Modifier.size(40.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "No users found",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (searchQuery.isNotEmpty() || selectedRole != null)
                                            "Try adjusting your search or filters"
                                        else
                                            "Users will appear here once they register",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredUsers) { user ->
                                    var showEditDialog by remember { mutableStateOf(false) }

                                    EnhancedUserCard(
                                        user = user,
                                        teacherAssignments = if (user.role == "TEACHER") {
                                            teacherAssignments[user.id] ?: emptyList()
                                        } else {
                                            emptyList()
                                        },
                                        studentEnrollments = if (user.role == "STUDENT") {
                                            studentEnrollments[user.id] ?: emptyList()
                                        } else {
                                            emptyList()
                                        },
                                        onEdit = {
                                            if (user.role == "TEACHER") {
                                                showEditDialog = true
                                            }
                                        }
                                    )

                                    if (showEditDialog && user.role == "TEACHER") {
                                        EditTeacherDepartmentDialog(
                                            user = user,
                                            courses = viewModel.courses.collectAsState().value,
                                            onDismiss = { showEditDialog = false },
                                            onSave = { departmentCourseId ->
                                                viewModel.updateTeacherDepartment(user.id, departmentCourseId)
                                                showEditDialog = false
                                            },
                                            isProcessing = user.id in viewModel.uiState.collectAsState().value.processingUsers
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Enhanced Error Message
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    color = Color(0xFFD32F2F),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedFilterChip(
    onClick: () -> Unit,
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    FilterChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) Color.White else color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White else color,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        selected = selected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(36.dp)
    )
}

@Composable
fun EnhancedUserCard(
    user: User,
    teacherAssignments: List<TeacherAssignmentInfo> = emptyList(),
    studentEnrollments: List<StudentEnrollmentInfo> = emptyList(),
    onEdit: () -> Unit
) {
    var showAssignments by remember { mutableStateOf(false) }
    var showEnrollments by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getRoleColor(user.role).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getRoleIcon(user.role),
                            contentDescription = "User Role",
                            modifier = Modifier.size(20.dp),
                            tint = getRoleColor(user.role)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Enhanced Role Badge
                        Surface(
                            color = getRoleColor(user.role).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = getRoleIcon(user.role),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = getRoleColor(user.role)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user.role.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = getRoleColor(user.role),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Enhanced Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD))
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit User",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Additional Info
            if (user.courseName != null || user.yearLevelName != null || user.departmentCourseName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (user.courseName != null || user.yearLevelName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Info",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buildString {
                                        user.courseName?.let { append("Course: $it") }
                                        if (user.courseName != null && user.yearLevelName != null) append(" • ")
                                        user.yearLevelName?.let { append("Year: $it") }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        // Show department for teachers
                        if (user.role == "TEACHER" && user.departmentCourseName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Work,
                                    contentDescription = "Department",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Department: ${user.departmentCourseName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (user.role == "TEACHER" && user.departmentCourseName == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "No Department",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No department assigned - Click Edit to assign",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF9800),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }

            // Show assigned subjects/sections for teachers
            if (user.role == "TEACHER" && teacherAssignments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Assignment,
                                    contentDescription = "Assignments",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Assigned Subjects/Sections (${teacherAssignments.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showAssignments = !showAssignments },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showAssignments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showAssignments) "Collapse" else "Expand",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (showAssignments) {
                            Spacer(modifier = Modifier.height(8.dp))
                            teacherAssignments.forEach { assignment ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color(0xFF2196F3)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = assignment.subjectName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF333333)
                                            )
                                            Text(
                                                text = "${assignment.subjectCode} - ${assignment.sectionName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF666666)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (user.role == "TEACHER" && teacherAssignments.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "No Assignments",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No subject/section assignments",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Show enrollments for students (only if enrolled)
            if (user.role == "STUDENT" && studentEnrollments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = "Enrollments",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enrolled Courses/Sections (${studentEnrollments.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { showEnrollments = !showEnrollments },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (showEnrollments) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showEnrollments) "Collapse" else "Expand",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (showEnrollments) {
                            Spacer(modifier = Modifier.height(8.dp))
                            studentEnrollments.forEach { enrollment ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = Color(0xFF2196F3)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = enrollment.subjectName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF333333)
                                                )
                                                Text(
                                                    text = "${enrollment.subjectCode} - ${enrollment.sectionName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF666666)
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF666666)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Course: ${enrollment.courseName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF666666)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (user.role == "STUDENT" && studentEnrollments.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "No Enrollments",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Not enrolled in any courses",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Enhanced Status
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (user.active) Color(0xFF4CAF50) else Color(0xFFF44336))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (user.active) "Active" else "Inactive",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (user.active) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

private fun getRoleColor(role: String): Color {
    return when (role.uppercase()) {
        "STUDENT" -> Color(0xFF2196F3)
        "TEACHER" -> Color(0xFFFF9800)
        "ADMIN" -> Color(0xFF9C27B0)
        else -> Color(0xFF666666)
    }
}

private fun getRoleIcon(role: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (role.uppercase()) {
        "STUDENT" -> Icons.Default.School
        "TEACHER" -> Icons.Default.Work
        "ADMIN" -> Icons.Default.Security
        else -> Icons.Default.Person
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTeacherDepartmentDialog(
    user: User,
    courses: List<com.smartacademictracker.data.model.Course>,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    isProcessing: Boolean
) {
    var selectedCourseId by remember { mutableStateOf(user.departmentCourseId ?: "") }
    var expandedCourse by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Teacher Department",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Teacher: ${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Select the department/course this teacher belongs to. This determines which MAJOR subjects they can see and apply for.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCourse,
                    onExpandedChange = { expandedCourse = !expandedCourse },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = courses.find { it.id == selectedCourseId }?.name ?: "Select Department",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Department/Course *") },
                        trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        supportingText = {
                            Text(
                                text = if (selectedCourseId.isEmpty())
                                    "Required: Teachers need a department to see MAJOR subjects"
                                else
                                    "MINOR subjects are visible to all teachers regardless of department"
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCourse,
                        onDismissRequest = { expandedCourse = false }
                    ) {
                        // Option to clear department
                        DropdownMenuItem(
                            text = { Text("None (Not Recommended)") },
                            onClick = {
                                selectedCourseId = ""
                                expandedCourse = false
                            }
                        )
                        Divider()
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.name} (${course.code})") },
                                onClick = {
                                    selectedCourseId = course.id
                                    expandedCourse = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(if (selectedCourseId.isEmpty()) null else selectedCourseId) },
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
}
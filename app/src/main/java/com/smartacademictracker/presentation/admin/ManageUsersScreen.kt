package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.User
import com.smartacademictracker.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManageUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val users by viewModel.users.collectAsState()
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Header Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Manage Users",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "View and manage user accounts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        // User Management Icon
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            
            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                // Enhanced Search and Filter Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Search & Filter",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        
                        // Enhanced Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search users...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF666666)
                                )
                            }
                        )

                        // Enhanced Role Filter
                        Text(
                            text = "Filter by Role:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EnhancedFilterChip(
                                onClick = { selectedRole = null },
                                label = "All",
                                selected = selectedRole == null,
                                icon = Icons.Default.People,
                                color = Color(0xFF4CAF50)
                            )
                            EnhancedFilterChip(
                                onClick = { selectedRole = UserRole.STUDENT },
                                label = "Students",
                                selected = selectedRole == UserRole.STUDENT,
                                icon = Icons.Default.School,
                                color = Color(0xFF2196F3)
                            )
                            EnhancedFilterChip(
                                onClick = { selectedRole = UserRole.TEACHER },
                                label = "Teachers",
                                selected = selectedRole == UserRole.TEACHER,
                                icon = Icons.Default.Work,
                                color = Color(0xFFFF9800)
                            )
                            EnhancedFilterChip(
                                onClick = { selectedRole = UserRole.ADMIN },
                                label = "Admins",
                                selected = selectedRole == UserRole.ADMIN,
                                icon = Icons.Default.AdminPanelSettings,
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Enhanced Loading State
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading users...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                    }
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredUsers) { user ->
                                EnhancedUserCard(
                                    user = user,
                                    onEdit = { /* TODO: Implement edit user */ }
                                )
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    fontWeight = FontWeight.Medium
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
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun EnhancedUserCard(
    user: User,
    onEdit: () -> Unit
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(getRoleColor(user.role).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getRoleIcon(user.role),
                            contentDescription = "User Role",
                            modifier = Modifier.size(24.dp),
                            tint = getRoleColor(user.role)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
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
                                    text = user.role,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = getRoleColor(user.role)
                                )
                            }
                        }
                    }
                }

                // Enhanced Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD))
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit User",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Additional Info
            if (user.courseName != null || user.yearLevelName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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
            }

            // Enhanced Status
            Spacer(modifier = Modifier.height(12.dp))
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
        "ADMIN" -> Icons.Default.AdminPanelSettings
        else -> Icons.Default.Person
    }
}
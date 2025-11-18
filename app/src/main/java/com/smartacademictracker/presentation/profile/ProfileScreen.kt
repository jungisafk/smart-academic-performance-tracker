package com.smartacademictracker.presentation.profile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToChangePassword: () -> Unit = {},
    showBackButton: Boolean = true,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by profileViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    // Only show back button if showBackButton is true
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // Add bottom padding for bottom nav
        ) {
            item {
                // Profile Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Avatar
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // User Name
                        Text(
                            text = "${currentUser?.firstName} ${currentUser?.lastName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // User Email
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // User Role
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = currentUser?.role ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Account Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                val user = currentUser
                val personalInfoItems = mutableListOf<Pair<String, String>>(
                    "First Name" to (user?.firstName ?: "Not provided"),
                    "Last Name" to (user?.lastName ?: "Not provided"),
                    "Email" to (user?.email ?: "Not provided"),
                    "Role" to (user?.role ?: "Not provided")
                )
                
                // Add course and year level for students
                if (user?.role == "STUDENT") {
                    val courseCode = user.courseCode
                    val courseInfo = courseCode ?: "Not provided"
                    personalInfoItems.add("Course" to courseInfo)
                    
                    val yearLevelInfo = user.yearLevelName ?: "Not provided"
                    personalInfoItems.add("Year Level" to yearLevelInfo)
                }
                
                // Add department/course for teachers
                if (user?.role == "TEACHER") {
                    val departmentCode = user.departmentCourseCode
                    val departmentInfo = departmentCode ?: "Not provided"
                    personalInfoItems.add("Department" to departmentInfo)
                }
                
                ProfileInfoCard(
                    title = "Personal Information",
                    items = personalInfoItems
                )
            }
            
            item {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                ProfileActionCard(
                    title = "Change Password",
                    description = "Update your account password",
                    icon = Icons.Default.Lock,
                    onClick = {
                        Log.d("ProfileScreen", "Change Password button clicked")
                        Log.d("ProfileScreen", "onNavigateToChangePassword callback: ${onNavigateToChangePassword::class.java.name}")
                        onNavigateToChangePassword()
                        Log.d("ProfileScreen", "onNavigateToChangePassword callback executed")
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                Log.d("ProfileActionCard", "Card clicked for: $title")
                Log.d("ProfileActionCard", "onClick lambda: ${onClick::class.java.name}")
                try {
                    onClick()
                    Log.d("ProfileActionCard", "onClick executed successfully for: $title")
                } catch (e: Exception) {
                    Log.e("ProfileActionCard", "Error executing onClick for $title", e)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.presentation.student.EnhancedProfileInfoRow
import com.smartacademictracker.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(
    onNavigateBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    showBackButton: Boolean = false,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Only show back button if showBackButton is true
                if (showBackButton) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color(0xFF666666)
                        )
                    }
                }
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )
            }

            // Profile Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp), // Add bottom padding for bottom nav
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC107)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(50.dp),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "${currentUser?.firstName} ${currentUser?.lastName}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Personal Information
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
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        EnhancedProfileInfoRow(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = currentUser?.email ?: "Not available"
                        )
                        
                        EnhancedProfileInfoRow(
                            icon = Icons.Default.Person,
                            label = "First Name",
                            value = currentUser?.firstName ?: "Not available"
                        )
                        
                        EnhancedProfileInfoRow(
                            icon = Icons.Default.Person,
                            label = "Last Name",
                            value = currentUser?.lastName ?: "Not available"
                        )
                        
                        EnhancedProfileInfoRow(
                            icon = Icons.Default.Badge,
                            label = "Role",
                            value = "Admin"
                        )
                    }
                }

                // Account Information
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
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Account Information",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        }
                        
                        EnhancedProfileInfoRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Account Created",
                            value = currentUser?.let { user ->
                                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                    .format(java.util.Date(user.createdAt))
                            } ?: "Not available"
                        )
                    }
                }

                // Actions Section
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                // Change Password Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToChangePassword() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF2196F3)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Change Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333)
                            )
                            Text(
                                text = "Update your account password",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                        
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF999999)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Out Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sign Out",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

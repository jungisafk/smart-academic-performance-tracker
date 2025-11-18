package com.smartacademictracker.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.util.IdValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onNavigateToActivation: () -> Unit = {},
    onSignInSuccess: () -> Unit,
    onNavigateToDashboard: (String) -> Unit = {},
    onNavigateToForgotPassword: (com.smartacademictracker.data.model.UserRole) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedUserType by remember { mutableStateOf(UserRole.STUDENT) }
    
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Clear sign-in success state when screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.clearSignInSuccess()
    }
    
    // Clear input fields when user type changes
    LaunchedEffect(selectedUserType) {
        userId = ""
        password = ""
        passwordVisible = false
    }
    
    LaunchedEffect(currentUser, uiState.isSignedIn) {
        currentUser?.let { user ->
            if (uiState.isSignedIn) {
                onSignInSuccess()
                
                // Also try direct navigation as backup
                val destination = when (user.role) {
                    "STUDENT" -> "student_dashboard"
                    "TEACHER" -> "teacher_dashboard"
                    "ADMIN" -> "admin_dashboard"
                    else -> "student_dashboard"
                }
                onNavigateToDashboard(destination)
            }
        }
    }
    
    // Validate user ID
    val idValidation = remember(userId, selectedUserType) {
        if (userId.isBlank()) {
            com.smartacademictracker.util.ValidationResult(true, null)
        } else {
            when (selectedUserType) {
                UserRole.STUDENT -> IdValidator.validateStudentId(userId)
                UserRole.TEACHER -> IdValidator.validateTeacherId(userId)
                UserRole.ADMIN -> IdValidator.validateAdminId(userId)
                else -> com.smartacademictracker.util.ValidationResult(false, "Invalid user type")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2196F3))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Title
            Text(
                text = "Smart Academic Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Subtitle
            Text(
                text = "Track your academic journey",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Sign In Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    // User Type Selection - UPDATED WITH ADMIN
                    Text(
                        text = "I am a:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    // Updated layout: 2 rows for better spacing
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First row: Student and Teacher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedUserType == UserRole.STUDENT,
                                onClick = { selectedUserType = UserRole.STUDENT },
                                label = { Text("Student") },
                                leadingIcon = if (selectedUserType == UserRole.STUDENT) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2196F3),
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedUserType == UserRole.TEACHER,
                                onClick = { selectedUserType = UserRole.TEACHER },
                                label = { Text("Teacher") },
                                leadingIcon = if (selectedUserType == UserRole.TEACHER) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2196F3),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        
                        // Second row: Admin (full width for emphasis)
                        FilterChip(
                            selected = selectedUserType == UserRole.ADMIN,
                            onClick = { selectedUserType = UserRole.ADMIN },
                            label = { 
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Administrator")
                                }
                            },
                            leadingIcon = if (selectedUserType == UserRole.ADMIN) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF9C27B0),  // Purple for admin
                                selectedLabelColor = Color.White,
                                labelColor = Color(0xFF9C27B0)
                            )
                        )
                    }

                    // User ID Field - UPDATED WITH ADMIN
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it.trim() },
                        label = { 
                            Text(
                                when (selectedUserType) {
                                    UserRole.STUDENT -> "Student ID"
                                    UserRole.TEACHER -> "Teacher ID"
                                    UserRole.ADMIN -> "Admin ID"
                                    else -> "User ID"
                                },
                                color = Color(0xFF666666)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                when (selectedUserType) {
                                    UserRole.STUDENT -> "e.g., 2024-1234"
                                    UserRole.TEACHER -> "e.g., T-2024-001"
                                    UserRole.ADMIN -> "e.g., A-2024-001"
                                    else -> "Enter your ID"
                                },
                                color = Color(0xFF999999)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                when (selectedUserType) {
                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = when (selectedUserType) {
                                    UserRole.ADMIN -> Color(0xFF9C27B0)
                                    else -> Color(0xFF2196F3)
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when (selectedUserType) {
                                UserRole.ADMIN -> Color(0xFF9C27B0)
                                else -> Color(0xFF2196F3)
                            },
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        isError = userId.isNotEmpty() && !idValidation.isValid,
                        supportingText = {
                            if (userId.isNotEmpty() && !idValidation.isValid) {
                                Text(
                                    idValidation.errorMessage ?: "",
                                    color = Color(0xFFF44336),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { 
                            Text(
                                "Password",
                                color = Color(0xFF666666)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Enter your password",
                                color = Color(0xFF999999)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock, 
                                contentDescription = null,
                                tint = when (selectedUserType) {
                                    UserRole.ADMIN -> Color(0xFF9C27B0)
                                    else -> Color(0xFF2196F3)
                                }
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF999999)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when (selectedUserType) {
                                UserRole.ADMIN -> Color(0xFF9C27B0)
                                else -> Color(0xFF2196F3)
                            },
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    
                    // Forgot Password Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { 
                                onNavigateToForgotPassword(selectedUserType)
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Text(
                                "Forgot Password?",
                                color = when (selectedUserType) {
                                    UserRole.ADMIN -> Color(0xFF9C27B0)
                                    else -> Color(0xFF2196F3)
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Success Message
                    if (uiState.isSignedIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in successful! Redirecting...",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Error Message
                    uiState.error?.let { error ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Sign In Button - UPDATED COLOR FOR ADMIN
                    Button(
                        onClick = {
                            viewModel.signInWithId(
                                userId = userId,
                                password = password,
                                userType = selectedUserType
                            )
                        },
                        enabled = !uiState.isLoading && 
                                 userId.isNotBlank() && 
                                 password.isNotBlank() &&
                                 idValidation.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedUserType) {
                                UserRole.ADMIN -> Color(0xFF9C27B0)  // Purple for admin
                                else -> Color(0xFF2196F3)
                            }
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Separator - Only show for non-admin
                    if (selectedUserType != UserRole.ADMIN) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0)
                            )
                            Text(
                                text = "or",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color(0xFF999999),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE0E0E0)
                            )
                        }

                        // First-time activation link - Only for students and teachers
                        OutlinedButton(
                            onClick = onNavigateToActivation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF4CAF50)
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "First Time? Activate Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Admin-specific message
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Administrator access only",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Progress Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107))
                )
            }
        }
    }
}


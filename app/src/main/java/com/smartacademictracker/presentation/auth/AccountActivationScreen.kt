package com.smartacademictracker.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.smartacademictracker.util.PasswordValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActivationScreen(
    onNavigateToSignIn: () -> Unit,
    onActivationSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedUserType by remember { mutableStateOf(UserRole.STUDENT) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Clear activation success state when screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.clearAccountActivatedFlag()
    }
    
    // Clear input fields when user type changes
    LaunchedEffect(selectedUserType) {
        userId = ""
        password = ""
        confirmPassword = ""
        passwordVisible = false
        confirmPasswordVisible = false
    }
    
    // Navigate on successful activation
    LaunchedEffect(uiState.isAccountActivated) {
        if (uiState.isAccountActivated) {
            kotlinx.coroutines.delay(2000)
            onNavigateToSignIn()
        }
    }
    
    // Validate user ID
    val idValidation = remember(userId, selectedUserType) {
        when (selectedUserType) {
            UserRole.STUDENT -> IdValidator.validateStudentId(userId)
            UserRole.TEACHER -> IdValidator.validateTeacherId(userId)
            else -> com.smartacademictracker.util.ValidationResult(false, "Invalid user type")
        }
    }
    
    // Validate password
    val passwordValidation = remember(password) {
        if (password.isEmpty()) {
            null
        } else {
            PasswordValidator.validate(password)
        }
    }
    
    // Check if passwords match
    val passwordsMatch = password == confirmPassword
    
    val isFormValid = userId.isNotBlank() &&
            idValidation.isValid &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            passwordsMatch &&
            (passwordValidation?.isValid == true) &&
            !uiState.isAccountActivated

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
                    .background(Color(0xFF2196F3), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Activate Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "First-time login? Use your institutional ID to activate your account",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Activation Card
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
                    // User Type Selection
                    Text(
                        text = "I am a:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
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

                    // User ID Field
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it.trim() },
                        label = {
                            Text(
                                when (selectedUserType) {
                                    UserRole.STUDENT -> "Student ID"
                                    UserRole.TEACHER -> "Teacher ID"
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
                                    else -> "Enter your ID"
                                },
                                color = Color(0xFF999999)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
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
                            Text("Create Password", color = Color(0xFF666666))
                        },
                        placeholder = {
                            Text("Enter a strong password", color = Color(0xFF999999))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide" else "Show",
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
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        isError = password.isNotEmpty() && passwordValidation?.isValid == false
                    )
                    
                    // Password Requirements Checklist
                    if (password.isNotEmpty()) {
                        val requirements = PasswordValidator.checkPasswordRequirements(password)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Password Requirements:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                requirements.forEach { requirement ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (requirement.isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (requirement.isMet) Color(0xFF4CAF50) else Color(0xFF999999),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = requirement.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (requirement.isMet) Color(0xFF4CAF50) else Color(0xFF666666),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Password Strength Indicator
                    if (password.isNotEmpty() && passwordValidation != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Strength: ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                            Text(
                                passwordValidation.strengthLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = when (passwordValidation.strength) {
                                    in 0..30 -> Color(0xFFF44336)
                                    in 31..60 -> Color(0xFFFF9800)
                                    in 61..80 -> Color(0xFFFFEB3B)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = passwordValidation.strength / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = when (passwordValidation.strength) {
                                in 0..30 -> Color(0xFFF44336)
                                in 31..60 -> Color(0xFFFF9800)
                                in 61..80 -> Color(0xFFFFEB3B)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = {
                            Text("Confirm Password", color = Color(0xFF666666))
                        },
                        placeholder = {
                            Text("Re-enter your password", color = Color(0xFF999999))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide" else "Show",
                                    tint = Color(0xFF999999)
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                Text(
                                    "Passwords don't match",
                                    color = Color(0xFFF44336),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (confirmPassword.isNotEmpty() && passwordsMatch) {
                                Text(
                                    "Passwords match",
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    // Success Message
                    if (uiState.isAccountActivated) {
                        Text(
                            text = "✓ Account activated successfully! Redirecting to sign in...",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Error Message
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            color = Color(0xFFF44336),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Activate Button
                    Button(
                        onClick = {
                            viewModel.activateAccount(
                                userId = userId,
                                password = password,
                                confirmPassword = confirmPassword,
                                userType = selectedUserType
                            )
                        },
                        enabled = !uiState.isLoading && isFormValid && !uiState.isAccountActivated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else if (uiState.isAccountActivated) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Activated!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Activate Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Already have account link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Already activated? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        TextButton(
                            onClick = onNavigateToSignIn,
                            enabled = !uiState.isLoading
                        ) {
                            Text(
                                "Sign In",
                                color = Color(0xFF2196F3),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


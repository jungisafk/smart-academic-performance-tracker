package com.smartacademictracker.presentation.profile

import android.util.Log
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
import androidx.navigation.NavHostController
import com.smartacademictracker.presentation.auth.AuthViewModel
import com.smartacademictracker.util.PasswordValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit,
    navController: NavHostController? = null,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    Log.d("ChangePasswordScreen", "ChangePasswordScreen composable called")
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by profileViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        Log.d("ChangePasswordScreen", "ChangePasswordScreen LaunchedEffect executed")
    }
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    // Handle success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            kotlinx.coroutines.delay(1500)
            onNavigateBack()
        }
    }
    
    // Validate password
    val passwordValidation = remember(newPassword) {
        if (newPassword.isEmpty()) {
            null
        } else {
            PasswordValidator.validate(newPassword)
        }
    }
    
    // Check if passwords match
    val passwordsMatch = newPassword == confirmPassword
    
    val isFormValid = currentPassword.isNotBlank() && 
                      newPassword.isNotBlank() && 
                      confirmPassword.isNotBlank() &&
                      passwordsMatch &&
                      (passwordValidation?.isValid == true) &&
                      !uiState.isLoading
    
    // Determine user role and apply appropriate wrapper
    val userRole = currentUser?.role
    
    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Update your account password",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Change Password Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { 
                            Text(
                                "Current Password",
                                color = Color(0xFF666666)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showCurrentPassword) "Hide password" else "Show password",
                                    tint = Color(0xFF999999)
                                )
                            }
                        },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { 
                            Text(
                                "New Password",
                                color = Color(0xFF666666)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showNewPassword) "Hide password" else "Show password",
                                    tint = Color(0xFF999999)
                                )
                            }
                        },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        isError = newPassword.isNotEmpty() && passwordValidation?.isValid == false,
                        supportingText = {
                            if (newPassword.isNotEmpty() && passwordValidation?.isValid == false) {
                                Column {
                                    passwordValidation?.errors?.forEach { error ->
                                        Text(
                                            error,
                                            color = Color(0xFFD32F2F),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    )
                    
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { 
                            Text(
                                "Confirm New Password",
                                color = Color(0xFF666666)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                    tint = Color(0xFF999999)
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
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
                                    "Passwords do not match",
                                    color = Color(0xFFD32F2F),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                    
                    if (uiState.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.error ?: "",
                                    color = Color(0xFFD32F2F),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    
                    if (uiState.isSuccess) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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
                                    text = "Password changed successfully!",
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            profileViewModel.changePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Change Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Apply role-based wrapper
    when (userRole) {
        "ADMIN" -> {
            if (navController != null) {
                com.smartacademictracker.navigation.wrapAdminScreenWithBottomNav(
                    navController = navController,
                    title = "Change Password",
                    showBackButton = true,
                    content = content
                )
            } else {
                // Fallback: use Scaffold with TopAppBar
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Change Password") },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        }
        "TEACHER" -> {
            if (navController != null) {
                com.smartacademictracker.navigation.TeacherScreenWithBottomNav(
                    navController = navController,
                    title = "Change Password",
                    showBackButton = true,
                    content = content
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Change Password") },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        }
        "STUDENT" -> {
            if (navController != null) {
                com.smartacademictracker.navigation.StudentScreenWithBottomNav(
                    navController = navController,
                    title = "Change Password",
                    showBackButton = true,
                    content = content
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Change Password") },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    content(paddingValues)
                }
            }
        }
        else -> {
            // Default: simple Scaffold
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Change Password") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                content(paddingValues)
            }
        }
    }
}

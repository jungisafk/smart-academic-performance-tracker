package com.smartacademictracker.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    userType: UserRole,
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var identifier by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            // Clear success message after 5 seconds
            kotlinx.coroutines.delay(5000)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Forgot Password",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = when (userType) {
                        UserRole.ADMIN -> Color(0xFF9C27B0)
                        else -> Color(0xFF2196F3)
                    },
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = when (userType) {
                            UserRole.ADMIN -> Color(0xFF9C27B0)
                            else -> Color(0xFF2196F3)
                        }
                    )

                    // Title
                    Text(
                        text = "Reset Your Password",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Description
                    Text(
                        text = "Enter your ${if (userType == UserRole.STUDENT) "Student ID" else if (userType == UserRole.TEACHER) "Teacher ID" else "Email"} or email address. We'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    
                    // Important Note
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚠ Important:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF57C00)
                            )
                            Text(
                                text = "Use the same ID you use to log in. The reset link will be sent to the email associated with your account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email/ID Input
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = {
                            Text(
                                if (userType == UserRole.STUDENT) "Student ID or Email"
                                else if (userType == UserRole.TEACHER) "Teacher ID or Email"
                                else "Email Address"
                            )
                        },
                        placeholder = {
                            Text(
                                if (userType == UserRole.STUDENT) "e.g., 2024-001 or student@example.com"
                                else if (userType == UserRole.TEACHER) "e.g., T-2024-001 or teacher@example.com"
                                else "admin@example.com"
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when (userType) {
                                UserRole.ADMIN -> Color(0xFF9C27B0)
                                else -> Color(0xFF2196F3)
                            },
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    // Error Message
                    uiState.error?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Success Message
                    uiState.successMessage?.let { message ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = message,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Send Reset Link Button
                    Button(
                        onClick = {
                            viewModel.sendPasswordReset(identifier, userType)
                        },
                        enabled = !uiState.isLoading && identifier.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (userType) {
                                UserRole.ADMIN -> Color(0xFF9C27B0)
                                else -> Color(0xFF2196F3)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Send Reset Link",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Back to Sign In
                    TextButton(
                        onClick = onNavigateBack,
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            "Back to Sign In",
                            color = when (userType) {
                                UserRole.ADMIN -> Color(0xFF9C27B0)
                                else -> Color(0xFF2196F3)
                            }
                        )
                    }
                }
            }
        }
    }
}


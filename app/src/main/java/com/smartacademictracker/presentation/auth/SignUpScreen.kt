package com.smartacademictracker.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartacademictracker.data.model.UserRole
import com.smartacademictracker.data.model.Course
import com.smartacademictracker.data.model.YearLevel
import com.smartacademictracker.data.repository.CourseRepository
import com.smartacademictracker.data.repository.YearLevelRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToSignIn: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Course and Year Level selection for students
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var selectedYearLevel by remember { mutableStateOf<YearLevel?>(null) }
    
    // Department course selection for teachers
    var selectedTeacherDepartment by remember { mutableStateOf<Course?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Clear sign-up success state when screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.clearSignUpSuccess()
    }
    
    // Load courses when student or teacher role is selected
    LaunchedEffect(selectedRole) {
        if (selectedRole == UserRole.STUDENT || selectedRole == UserRole.TEACHER) {
            viewModel.loadCourses()
        } else {
            // Clear course data when switching away from student/teacher role
            viewModel.clearCourseData()
            selectedCourse = null
            selectedYearLevel = null
            selectedTeacherDepartment = null
        }
    }
    
    LaunchedEffect(uiState.isSignUpSuccess) {
        if (uiState.isSignUpSuccess) {
            // Show success message for 2 seconds, then redirect to login
            kotlinx.coroutines.delay(2000)
            onNavigateToSignIn()
        }
    }

    val isFormValid = email.isNotBlank() && 
                     password.isNotBlank() && 
                     confirmPassword.isNotBlank() &&
                     firstName.isNotBlank() && 
                     lastName.isNotBlank() &&
                     password == confirmPassword &&
                     password.length >= 6 &&
                     !uiState.isSignUpSuccess &&
                     (selectedRole != UserRole.STUDENT || (selectedCourse != null && selectedYearLevel != null)) &&
                     (selectedRole != UserRole.TEACHER || selectedTeacherDepartment != null)

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
                // Book icon placeholder - using a simple rectangle to represent a book
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
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
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Sign Up Card
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
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // First Name Field
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { 
                            Text(
                                "First Name",
                                color = Color(0xFF666666)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Enter your first name",
                                color = Color(0xFF999999)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null,
                                tint = Color(0xFF999999)
                            )
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    // Last Name Field
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { 
                            Text(
                                "Last Name",
                                color = Color(0xFF666666)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Enter your last name",
                                color = Color(0xFF999999)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null,
                                tint = Color(0xFF999999)
                            )
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { 
                            Text(
                                "Email",
                                color = Color(0xFF666666)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Enter your email",
                                color = Color(0xFF999999)
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email, 
                                contentDescription = null,
                                tint = Color(0xFF999999)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
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
                                tint = Color(0xFF2196F3)
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
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        supportingText = { 
                            Text(
                                "Minimum 6 characters",
                                color = Color(0xFF999999),
                                style = MaterialTheme.typography.bodySmall
                            ) 
                        }
                    )

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { 
                            Text(
                                "Confirm Password",
                                color = Color(0xFF666666)
                            ) 
                        },
                        placeholder = { 
                            Text(
                                "Confirm your password",
                                color = Color(0xFF999999)
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
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
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
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                Text(
                                    "Passwords don't match", 
                                    color = Color(0xFFF44336),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )

                    // Role Selection
                    Text(
                        text = "Select Role",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    UserRole.values().forEach { role ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .selectable(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRole == role) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                            ),
                            border = if (selectedRole == role) 
                                androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3)) 
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF2196F3)
                                    )
                                )
                                Text(
                                    text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = if (selectedRole == role) Color(0xFF2196F3) else Color(0xFF666666),
                                    fontWeight = if (selectedRole == role) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Course and Year Level Selection for Students
                    if (selectedRole == UserRole.STUDENT) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Course Selection
                        Text(
                            text = "Select Course",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        if (uiState.isLoadingCourses) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Loading courses...",
                                    color = Color(0xFF666666)
                                )
                            }
                        } else {
                            var courseDropdownExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = courseDropdownExpanded,
                                onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCourse?.name ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { 
                                        Text(
                                            "Select Course",
                                            color = Color(0xFF666666)
                                        ) 
                                    },
                                    placeholder = { 
                                        Text(
                                            "Select Course",
                                            color = Color(0xFF999999)
                                        ) 
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.School, 
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2196F3),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = courseDropdownExpanded,
                                    onDismissRequest = { courseDropdownExpanded = false }
                                ) {
                                    if (uiState.courses.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No courses available") },
                                            onClick = { courseDropdownExpanded = false }
                                        )
                                    } else {
                                        uiState.courses.forEach { course ->
                                            DropdownMenuItem(
                                                text = { Text("${course.name} (${course.code})") },
                                                onClick = {
                                                    selectedCourse = course
                                                    selectedYearLevel = null // Reset year level when course changes
                                                    courseDropdownExpanded = false
                                                    viewModel.loadYearLevelsForCourse(course.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Year Level Selection
                            Text(
                                text = "Select Year Level",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF333333),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            )
                            
                            if (uiState.isLoadingYearLevels) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2196F3)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Loading year levels...",
                                        color = Color(0xFF666666)
                                    )
                                }
                            } else {
                                var yearLevelDropdownExpanded by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                    expanded = yearLevelDropdownExpanded,
                                    onExpandedChange = { yearLevelDropdownExpanded = !yearLevelDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedYearLevel?.name ?: "",
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { 
                                            Text(
                                                "Select Year Level",
                                                color = Color(0xFF666666)
                                            ) 
                                        },
                                        placeholder = { 
                                            Text(
                                                "Select Year Level",
                                                color = Color(0xFF999999)
                                            ) 
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.School, 
                                                contentDescription = null,
                                                tint = Color(0xFF2196F3)
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearLevelDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                            .padding(bottom = 24.dp),
                                        enabled = selectedCourse != null,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2196F3),
                                            unfocusedBorderColor = Color(0xFFE0E0E0)
                                        )
                                    )
                                    
                                    ExposedDropdownMenu(
                                        expanded = yearLevelDropdownExpanded,
                                        onDismissRequest = { yearLevelDropdownExpanded = false }
                                    ) {
                                        uiState.yearLevels.forEach { yearLevel ->
                                            DropdownMenuItem(
                                                text = { Text(yearLevel.name) },
                                                onClick = {
                                                    selectedYearLevel = yearLevel
                                                    yearLevelDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Teaching Department Selection for Teachers
                    if (selectedRole == UserRole.TEACHER) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Department Selection
                        Text(
                            text = "Select Teaching Department",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        Text(
                            text = "Choose the department/course you will be teaching. This determines which MAJOR subjects you can see and apply for.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )
                        
                        if (uiState.isLoadingCourses) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF2196F3)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Loading departments...",
                                    color = Color(0xFF666666)
                                )
                            }
                        } else {
                            var teacherDepartmentDropdownExpanded by remember { mutableStateOf(false) }
                            
                            ExposedDropdownMenuBox(
                                expanded = teacherDepartmentDropdownExpanded,
                                onExpandedChange = { teacherDepartmentDropdownExpanded = !teacherDepartmentDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedTeacherDepartment?.let { "${it.code} - ${it.name}" } ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { 
                                        Text(
                                            "Select Teaching Department",
                                            color = Color(0xFF666666)
                                        ) 
                                    },
                                    placeholder = { 
                                        Text(
                                            "Select Teaching Department",
                                            color = Color(0xFF999999)
                                        ) 
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.School, 
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDepartmentDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .padding(bottom = 24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2196F3),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = teacherDepartmentDropdownExpanded,
                                    onDismissRequest = { teacherDepartmentDropdownExpanded = false }
                                ) {
                                    if (uiState.courses.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No departments available") },
                                            onClick = { teacherDepartmentDropdownExpanded = false }
                                        )
                                    } else {
                                        uiState.courses.forEach { course ->
                                            DropdownMenuItem(
                                                text = { Text("${course.code} - ${course.name}") },
                                                onClick = {
                                                    selectedTeacherDepartment = course
                                                    teacherDepartmentDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Success Message
                    if (uiState.isSignUpSuccess) {
                        Text(
                            text = "Account created successfully! Redirecting to login...",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodySmall,
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

                    // Create Account Button
                    Button(
                        onClick = {
                            viewModel.signUp(
                                email = email.trim(),
                                password = password,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                role = selectedRole,
                                courseId = if (selectedRole == UserRole.STUDENT) selectedCourse?.id else null,
                                yearLevelId = if (selectedRole == UserRole.STUDENT) selectedYearLevel?.id else null,
                                departmentCourseId = if (selectedRole == UserRole.TEACHER) selectedTeacherDepartment?.id else null
                            )
                        },
                        enabled = !uiState.isLoading && isFormValid && !uiState.isSignUpSuccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else if (uiState.isSignUpSuccess) {
                            Text(
                                "Account Created!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Navigate to Sign In
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Already have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        TextButton(
                            onClick = onNavigateToSignIn,
                            enabled = !uiState.isLoading && !uiState.isSignUpSuccess
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

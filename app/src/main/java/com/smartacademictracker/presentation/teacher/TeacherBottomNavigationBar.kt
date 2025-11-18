package com.smartacademictracker.presentation.teacher

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class TeacherBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : TeacherBottomNavItem(
        route = "teacher_home",
        title = "Home",
        icon = Icons.Default.Home
    )
    object MySubjects : TeacherBottomNavItem(
        route = "teacher_my_subjects",
        title = "My Subjects",
        icon = Icons.Default.MenuBook
    )
    object StudentManagement : TeacherBottomNavItem(
        route = "teacher_student_management_tab",
        title = "Students",
        icon = Icons.Default.Group
    )
    object Profile : TeacherBottomNavItem(
        route = "teacher_profile_tab",
        title = "Profile",
        icon = Icons.Default.AccountCircle
    )
}

@Composable
fun TeacherBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        TeacherBottomNavItem.Home,
        TeacherBottomNavItem.MySubjects,
        TeacherBottomNavItem.StudentManagement,
        TeacherBottomNavItem.Profile
    )
    
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-8).dp), // Reduce top margin by moving up
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(text = item.title)
                },
                selected = currentRoute == item.route,
                onClick = {
                    onNavigate(item.route)
                }
            )
        }
    }
}


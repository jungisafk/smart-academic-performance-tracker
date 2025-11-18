package com.smartacademictracker.presentation.student

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class StudentBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : StudentBottomNavItem(
        route = "student_home",
        title = "Home",
        icon = Icons.Default.Home
    )
    object Subjects : StudentBottomNavItem(
        route = "student_subjects_tab",
        title = "Subjects",
        icon = Icons.Default.MenuBook
    )
    object Grades : StudentBottomNavItem(
        route = "student_grades_tab",
        title = "Grades",
        icon = Icons.Default.Star
    )
    object Profile : StudentBottomNavItem(
        route = "student_profile_tab",
        title = "Profile",
        icon = Icons.Default.AccountCircle
    )
}

@Composable
fun StudentBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        StudentBottomNavItem.Home,
        StudentBottomNavItem.Subjects,
        StudentBottomNavItem.Grades,
        StudentBottomNavItem.Profile
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


package com.smartacademictracker.presentation.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class AdminBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : AdminBottomNavItem(
        route = "admin_home",
        title = "Home",
        icon = Icons.Default.Home
    )
    object UserManagement : AdminBottomNavItem(
        route = "admin_user_management",
        title = "Users",
        icon = Icons.Default.Person
    )
    object AcademicManagement : AdminBottomNavItem(
        route = "admin_academic_management",
        title = "Academic",
        icon = Icons.Default.MenuBook
    )
    object Profile : AdminBottomNavItem(
        route = "admin_profile_tab",
        title = "Profile",
        icon = Icons.Default.AccountCircle
    )
}

@Composable
fun AdminBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        AdminBottomNavItem.Home,
        AdminBottomNavItem.UserManagement,
        AdminBottomNavItem.AcademicManagement,
        AdminBottomNavItem.Profile
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


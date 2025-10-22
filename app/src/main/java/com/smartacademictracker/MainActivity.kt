package com.smartacademictracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.smartacademictracker.navigation.SmartAcademicTrackerNavigation
import com.smartacademictracker.presentation.notification.NotificationPermissionHandler
import com.smartacademictracker.ui.theme.SmartAcademicTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartAcademicTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Handle notification permissions
                    NotificationPermissionHandler(
                        onPermissionGranted = {
                            // Permission granted, continue with app
                        },
                        onPermissionDenied = {
                            // Permission denied, continue with app but without notifications
                        }
                    )
                    
                    SmartAcademicTrackerNavigation(navController = navController)
                }
            }
        }
    }
}

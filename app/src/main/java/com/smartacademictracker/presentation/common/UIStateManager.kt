package com.smartacademictracker.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Comprehensive UI state management for consistent loading, error, and empty states
 */

@Composable
fun UIStateManager(
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    loadingMessage: String = "Loading...",
    emptyState: @Composable () -> Unit,
    errorState: @Composable (String) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            FullScreenLoading(
                message = loadingMessage,
                modifier = modifier
            )
        }
        error != null -> {
            errorState(error)
        }
        isEmpty -> {
            emptyState()
        }
        else -> {
            content()
        }
    }
}

@Composable
fun UIStateManagerWithRetry(
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    loadingMessage: String = "Loading...",
    onRetry: () -> Unit,
    emptyState: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    UIStateManager(
        isLoading = isLoading,
        error = error,
        isEmpty = isEmpty,
        loadingMessage = loadingMessage,
        emptyState = emptyState,
        errorState = { errorMessage ->
            FullScreenError(
                error = errorMessage,
                onRetry = onRetry,
                modifier = modifier
            )
        },
        content = content,
        modifier = modifier
    )
}

@Composable
fun CardUIStateManager(
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    loadingMessage: String = "Loading...",
    onRetry: (() -> Unit)? = null,
    emptyState: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            when {
                isLoading -> {
                    InlineLoading(
                        message = loadingMessage,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                error != null -> {
                    ErrorCard(
                        error = error,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                isEmpty -> {
                    emptyState()
                }
                else -> {
                    content()
                }
            }
        }
    }
}

@Composable
fun ListUIStateManager(
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    loadingMessage: String = "Loading...",
    onRetry: (() -> Unit)? = null,
    emptyState: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            ListLoading(
                itemCount = 3,
                modifier = modifier
            )
        }
        error != null -> {
            FullScreenError(
                error = error,
                onRetry = onRetry,
                modifier = modifier
            )
        }
        isEmpty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                emptyState()
            }
        }
        else -> {
            content()
        }
    }
}

@Composable
fun FormUIStateManager(
    isLoading: Boolean,
    error: String?,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (error != null) {
            InlineError(
                error = error,
                onDismiss = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        LoadingOverlay(
            isLoading = isLoading,
            message = "Saving..."
        ) {
            content()
        }
    }
}

@Composable
fun ButtonUIStateManager(
    isLoading: Boolean,
    error: String?,
    onRetry: (() -> Unit)? = null,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (error != null) {
            ValidationError(
                message = error,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        ButtonLoading(
            isLoading = isLoading,
            content = {
                Button(
                    onClick = onButtonClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NetworkUIStateManager(
    isOnline: Boolean,
    isLoading: Boolean,
    error: String?,
    isEmpty: Boolean,
    onRetry: () -> Unit,
    onGoOffline: () -> Unit,
    emptyState: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!isOnline) {
            NetworkError(
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        UIStateManagerWithRetry(
            isLoading = isLoading,
            error = error,
            isEmpty = isEmpty,
            onRetry = onRetry,
            emptyState = emptyState,
            content = content,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SyncUIStateManager(
    isSyncing: Boolean,
    syncError: String?,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isSyncing) {
            RefreshLoading(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (syncError != null) {
            ErrorCard(
                error = syncError,
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        content()
    }
}

@Composable
fun ValidationUIStateManager(
    validationErrors: List<String>,
    isLoading: Boolean,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (validationErrors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Please fix the following errors:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
                    )
                    
                    validationErrors.forEach { error ->
                        ValidationError(
                            message = error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        LoadingOverlay(
            isLoading = isLoading,
            message = "Processing..."
        ) {
            content()
        }
    }
}

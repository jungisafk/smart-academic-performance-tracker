package com.smartacademictracker.data.local.entity

/**
 * Conflict resolution data
 */
data class ConflictResolution(
    val localVersion: Long,
    val serverVersion: Long,
    val resolutionStrategy: ResolutionStrategy,
    val resolvedBy: String, // User ID who resolved the conflict
    val resolvedAt: Long = System.currentTimeMillis()
)

/**
 * Conflict resolution strategies
 */
enum class ResolutionStrategy {
    USE_LOCAL,      // Use local version
    USE_SERVER,     // Use server version
    MERGE,          // Merge both versions
    MANUAL          // Manual resolution required
}

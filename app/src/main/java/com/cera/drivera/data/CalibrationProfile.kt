package com.cera.drivera.data

import java.io.Serializable

/**
 * Data class representing a single driver's calibration profile.
 * Stores personal EAR thresholds and driver information.
 */
data class CalibrationProfile(
    val profileId: String,                    // Unique ID (UUID)
    val driverName: String,                   // Driver's name
    val earThresholdOpen: Double,            // EAR value when eyes are fully open
    val earThresholdClosed: Double,          // EAR value when eyes are fully closed
    val calculatedThreshold: Double,         // Personal EAR threshold = (open + closed) / 2
    val createdAt: Long,                     // Timestamp when profile was created
    val lastUsedAt: Long,                    // Timestamp when profile was last used
    val isActive: Boolean = false            // Whether this is the currently active profile
) : Serializable

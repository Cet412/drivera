package com.cera.drivera.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Manages all calibration profile persistence and operations.
 * Handles saving, loading, and switching between driver profiles.
 */
class CalibrationPreferencesManager(private val context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("drivera_calibration", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_PROFILES = "calibration_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_IS_FIRST_BOOT = "is_first_boot"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
    
    /**
     * Check if this is the first time the app is launched.
     * Used to show welcome/onboarding screen.
     */
    fun isFirstBoot(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_BOOT, true)
    }
    
    /**
     * Mark first boot as complete after onboarding is done.
     */
    fun markFirstBootComplete() {
        prefs.edit().apply {
            putBoolean(KEY_IS_FIRST_BOOT, false)
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            apply()
        }
    }
    
    /**
     * Save a new calibration profile.
     */
    fun saveProfile(profile: CalibrationProfile) {
        val profiles = getAllProfiles().toMutableList()
        
        // Remove existing profile with same ID if updating
        profiles.removeAll { it.profileId == profile.profileId }
        
        // Add new profile
        profiles.add(profile)
        
        // Persist to SharedPreferences
        val json = gson.toJson(profiles)
        prefs.edit().putString(KEY_PROFILES, json).apply()
    }
    
    /**
     * Get all saved calibration profiles.
     */
    fun getAllProfiles(): List<CalibrationProfile> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CalibrationProfile>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get active (currently selected) profile.
     */
    fun getActiveProfile(): CalibrationProfile? {
        val activeProfileId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null) ?: return null
        return getAllProfiles().find { it.profileId == activeProfileId }
    }
    
    /**
     * Set a profile as active.
     */
    fun setActiveProfile(profileId: String) {
        val profiles = getAllProfiles().map { profile ->
            if (profile.profileId == profileId) {
                profile.copy(
                    isActive = true,
                    lastUsedAt = System.currentTimeMillis()
                )
            } else {
                profile.copy(isActive = false)
            }
        }
        
        val json = gson.toJson(profiles)
        prefs.edit().apply {
            putString(KEY_PROFILES, json)
            putString(KEY_ACTIVE_PROFILE_ID, profileId)
            apply()
        }
    }
    
    /**
     * Get the EAR threshold for the active profile.
     * Returns default 0.16 if no profile is active.
     */
    fun getActiveThreshold(): Double {
        return getActiveProfile()?.calculatedThreshold ?: 0.16
    }
    
    /**
     * Delete a profile by ID.
     */
    fun deleteProfile(profileId: String) {
        val profiles = getAllProfiles().filter { it.profileId != profileId }
        val json = gson.toJson(profiles)
        prefs.edit().putString(KEY_PROFILES, json).apply()
        
        // If deleted profile was active, clear active profile
        if (prefs.getString(KEY_ACTIVE_PROFILE_ID, null) == profileId) {
            prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
        }
    }
    
    /**
     * Create a new profile with given data.
     */
    fun createProfile(
        driverName: String,
        earOpen: Double,
        earClosed: Double
    ): CalibrationProfile {
        val profileId = UUID.randomUUID().toString()
        val threshold = (earOpen + earClosed) / 2.0
        val now = System.currentTimeMillis()
        
        return CalibrationProfile(
            profileId = profileId,
            driverName = driverName,
            earThresholdOpen = earOpen,
            earThresholdClosed = earClosed,
            calculatedThreshold = threshold,
            createdAt = now,
            lastUsedAt = now,
            isActive = true
        )
    }
    
    /**
     * Get count of saved profiles.
     */
    fun getProfileCount(): Int {
        return getAllProfiles().size
    }
    
    /**
     * Clear all profiles and reset to first boot state.
     * (Use with caution - typically for reset/debug)
     */
    fun clearAllProfiles() {
        prefs.edit().apply {
            remove(KEY_PROFILES)
            remove(KEY_ACTIVE_PROFILE_ID)
            remove(KEY_ONBOARDING_COMPLETED)
            putBoolean(KEY_IS_FIRST_BOOT, true)
            apply()
        }
    }
}
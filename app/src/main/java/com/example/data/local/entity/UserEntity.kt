package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.UserProfile

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val photoURL: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val xp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalFocusMinutes: Int = 0,
    val completedTasks: Int = 0,
    val completedHabits: Int = 0,
    val completedGoals: Int = 0,
    val themePreference: String = "System",
    val reduceMotion: Boolean = false,
    val focusReminders: Boolean = true,
    val taskReminders: Boolean = true,
    val habitReminders: Boolean = true,
    val focusSounds: Boolean = true,
    val completionSound: Boolean = true
) {
    fun toUserProfile(): UserProfile = UserProfile(
        uid = uid,
        name = name,
        email = email,
        photoURL = photoURL,
        authProvider = "SQLite Local Auth",
        createdAt = createdAt,
        updatedAt = updatedAt,
        xp = xp,
        level = level,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalFocusMinutes = totalFocusMinutes,
        completedTasks = completedTasks,
        completedHabits = completedHabits,
        completedGoals = completedGoals,
        themePreference = themePreference,
        reduceMotion = reduceMotion,
        focusReminders = focusReminders,
        taskReminders = taskReminders,
        habitReminders = habitReminders,
        focusSounds = focusSounds,
        completionSound = completionSound
    )

    companion object {
        fun fromUserProfile(profile: UserProfile, passwordHash: String): UserEntity = UserEntity(
            uid = profile.uid,
            name = profile.name,
            email = profile.email,
            passwordHash = passwordHash,
            photoURL = profile.photoURL,
            createdAt = profile.createdAt,
            updatedAt = profile.updatedAt,
            xp = profile.xp,
            level = profile.level,
            currentStreak = profile.currentStreak,
            longestStreak = profile.longestStreak,
            totalFocusMinutes = profile.totalFocusMinutes,
            completedTasks = profile.completedTasks,
            completedHabits = profile.completedHabits,
            completedGoals = profile.completedGoals,
            themePreference = profile.themePreference,
            reduceMotion = profile.reduceMotion,
            focusReminders = profile.focusReminders,
            taskReminders = profile.taskReminders,
            habitReminders = profile.habitReminders,
            focusSounds = profile.focusSounds,
            completionSound = profile.completionSound
        )
    }
}

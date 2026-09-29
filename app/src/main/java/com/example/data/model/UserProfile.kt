package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoURL: String? = null,
    val authProvider: String = "Email & Password",
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
    // Settings & Preferences
    val themePreference: String = "System", // "System", "Light", "Dark"
    val reduceMotion: Boolean = false,
    val focusReminders: Boolean = true,
    val taskReminders: Boolean = true,
    val habitReminders: Boolean = true,
    val focusSounds: Boolean = true,
    val completionSound: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "photoURL" to photoURL,
        "authProvider" to authProvider,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "xp" to xp,
        "level" to level,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "totalFocusMinutes" to totalFocusMinutes,
        "completedTasks" to completedTasks,
        "completedHabits" to completedHabits,
        "completedGoals" to completedGoals,
        "themePreference" to themePreference,
        "reduceMotion" to reduceMotion,
        "focusReminders" to focusReminders,
        "taskReminders" to taskReminders,
        "habitReminders" to habitReminders,
        "focusSounds" to focusSounds,
        "completionSound" to completionSound
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String = ""): UserProfile {
            return UserProfile(
                uid = (map["uid"] as? String) ?: docId,
                name = (map["name"] as? String) ?: "",
                email = (map["email"] as? String) ?: "",
                photoURL = map["photoURL"] as? String,
                authProvider = (map["authProvider"] as? String) ?: "Email & Password",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                xp = (map["xp"] as? Number)?.toInt() ?: 0,
                level = (map["level"] as? Number)?.toInt() ?: 1,
                currentStreak = (map["currentStreak"] as? Number)?.toInt() ?: 0,
                longestStreak = (map["longestStreak"] as? Number)?.toInt() ?: 0,
                totalFocusMinutes = (map["totalFocusMinutes"] as? Number)?.toInt() ?: 0,
                completedTasks = (map["completedTasks"] as? Number)?.toInt() ?: 0,
                completedHabits = (map["completedHabits"] as? Number)?.toInt() ?: 0,
                completedGoals = (map["completedGoals"] as? Number)?.toInt() ?: 0,
                themePreference = (map["themePreference"] as? String) ?: "System",
                reduceMotion = (map["reduceMotion"] as? Boolean) ?: false,
                focusReminders = (map["focusReminders"] as? Boolean) ?: true,
                taskReminders = (map["taskReminders"] as? Boolean) ?: true,
                habitReminders = (map["habitReminders"] as? Boolean) ?: true,
                focusSounds = (map["focusSounds"] as? Boolean) ?: true,
                completionSound = (map["completionSound"] as? Boolean) ?: true
            )
        }
    }
}

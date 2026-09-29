package com.example.data.model

data class Habit(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val frequency: String = "Daily", // Daily, Weekdays, Weekend
    val category: String = "Health",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completionHistory: List<String> = emptyList(), // List of "YYYY-MM-DD"
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "name" to name,
        "description" to description,
        "frequency" to frequency,
        "category" to category,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "completionHistory" to completionHistory,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, docId: String = ""): Habit {
            val history = (map["completionHistory"] as? List<String>) ?: emptyList()

            return Habit(
                id = (map["id"] as? String) ?: docId,
                userId = (map["userId"] as? String) ?: "",
                name = (map["name"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                frequency = (map["frequency"] as? String) ?: "Daily",
                category = (map["category"] as? String) ?: "Health",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completionHistory = history,
                currentStreak = (map["currentStreak"] as? Number)?.toInt() ?: 0,
                longestStreak = (map["longestStreak"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

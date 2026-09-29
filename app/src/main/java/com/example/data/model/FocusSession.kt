package com.example.data.model

data class FocusSession(
    val id: String = "",
    val userId: String = "",
    val type: String = "focus", // "focus", "short_break", "long_break"
    val durationMinutes: Int = 25,
    val startedAt: Long = System.currentTimeMillis() - 25 * 60 * 1000,
    val completedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "Study",
    val scene: String = "Forest"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "type" to type,
        "durationMinutes" to durationMinutes,
        "startedAt" to startedAt,
        "completedAt" to completedAt,
        "createdAt" to createdAt,
        "category" to category,
        "scene" to scene
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String = ""): FocusSession {
            return FocusSession(
                id = (map["id"] as? String) ?: docId,
                userId = (map["userId"] as? String) ?: "",
                type = (map["type"] as? String) ?: "focus",
                durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: 25,
                startedAt = (map["startedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completedAt = (map["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                category = (map["category"] as? String) ?: "Study",
                scene = (map["scene"] as? String) ?: "Forest"
            )
        }
    }
}

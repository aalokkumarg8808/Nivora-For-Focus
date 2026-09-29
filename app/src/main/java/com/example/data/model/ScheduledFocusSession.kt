package com.example.data.model

data class ScheduledFocusSession(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Study",
    val scheduledTimeMillis: Long = System.currentTimeMillis() + 30 * 60 * 1000L,
    val durationMinutes: Int = 25,
    val reminderMinutesBefore: Int = 10, // 5, 10, 15 min before
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val reminderTriggerTimeMillis: Long
        get() = scheduledTimeMillis - (reminderMinutesBefore * 60 * 1000L)

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "category" to category,
        "scheduledTimeMillis" to scheduledTimeMillis,
        "durationMinutes" to durationMinutes,
        "reminderMinutesBefore" to reminderMinutesBefore,
        "isCompleted" to isCompleted,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, docId: String = ""): ScheduledFocusSession {
            return ScheduledFocusSession(
                id = (map["id"] as? String) ?: docId,
                userId = (map["userId"] as? String) ?: "",
                title = (map["title"] as? String) ?: "",
                category = (map["category"] as? String) ?: "Study",
                scheduledTimeMillis = (map["scheduledTimeMillis"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 30 * 60 * 1000L),
                durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: 25,
                reminderMinutesBefore = (map["reminderMinutesBefore"] as? Number)?.toInt() ?: 10,
                isCompleted = (map["isCompleted"] as? Boolean) ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

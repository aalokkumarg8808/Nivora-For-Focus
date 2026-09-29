package com.example.data.model

data class Subtask(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "completed" to completed
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Subtask {
            return Subtask(
                id = (map["id"] as? String) ?: "",
                title = (map["title"] as? String) ?: "",
                completed = (map["completed"] as? Boolean) ?: false
            )
        }
    }
}

data class Task(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Study", // Study, Personal, Health, Work
    val priority: String = "Medium", // High, Medium, Low (Retained for data compatibility)
    val dueDate: String = "", // YYYY-MM-DD
    val dueTime: String = "", // e.g. "10:30 AM"
    val reminderEnabled: Boolean = false,
    val reminderRepeat: String = "Once", // "Once", "Daily", "Custom Days"
    val reminderDays: List<Int> = emptyList(), // Calendar days of week (e.g. Calendar.MONDAY = 2, Calendar.WEDNESDAY = 4, Calendar.FRIDAY = 6)
    val notificationId: Int? = null,
    val notificationIds: List<Int> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val subtasks: List<Subtask> = emptyList()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "description" to description,
        "category" to category,
        "priority" to priority,
        "dueDate" to dueDate,
        "dueTime" to dueTime,
        "reminderEnabled" to reminderEnabled,
        "reminderRepeat" to reminderRepeat,
        "reminderDays" to reminderDays,
        "notificationId" to notificationId,
        "notificationIds" to notificationIds,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "completed" to completed,
        "completedAt" to completedAt,
        "subtasks" to subtasks.map { it.toMap() }
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, docId: String = ""): Task {
            val subtaskList = (map["subtasks"] as? List<Map<String, Any?>>)?.map {
                Subtask.fromMap(it)
            } ?: emptyList()

            val reminderDaysList = (map["reminderDays"] as? List<*>)?.mapNotNull {
                (it as? Number)?.toInt()
            } ?: emptyList()

            val notifIdsList = (map["notificationIds"] as? List<*>)?.mapNotNull {
                (it as? Number)?.toInt()
            } ?: emptyList()

            return Task(
                id = (map["id"] as? String) ?: docId,
                userId = (map["userId"] as? String) ?: "",
                title = (map["title"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                category = (map["category"] as? String) ?: "Study",
                priority = (map["priority"] as? String) ?: "Medium",
                dueDate = (map["dueDate"] as? String) ?: "",
                dueTime = (map["dueTime"] as? String) ?: "",
                reminderEnabled = (map["reminderEnabled"] as? Boolean) ?: false,
                reminderRepeat = (map["reminderRepeat"] as? String) ?: "Once",
                reminderDays = reminderDaysList,
                notificationId = (map["notificationId"] as? Number)?.toInt(),
                notificationIds = notifIdsList,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completed = (map["completed"] as? Boolean) ?: false,
                completedAt = (map["completedAt"] as? Number)?.toLong(),
                subtasks = subtaskList
            )
        }
    }
}

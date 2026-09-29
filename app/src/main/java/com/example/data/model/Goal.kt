package com.example.data.model

data class Milestone(
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
        fun fromMap(map: Map<String, Any?>): Milestone {
            return Milestone(
                id = (map["id"] as? String) ?: "",
                title = (map["title"] as? String) ?: "",
                completed = (map["completed"] as? Boolean) ?: false
            )
        }
    }
}

data class Goal(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val deadline: String = "", // YYYY-MM-DD
    val milestones: List<Milestone> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val progress: Float
        get() = if (milestones.isEmpty()) 0f else milestones.count { it.completed }.toFloat() / milestones.size

    val isCompleted: Boolean
        get() = milestones.isNotEmpty() && milestones.all { it.completed }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "title" to title,
        "description" to description,
        "deadline" to deadline,
        "milestones" to milestones.map { it.toMap() },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "completedAt" to completedAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>, docId: String = ""): Goal {
            val milestoneList = (map["milestones"] as? List<Map<String, Any?>>)?.map {
                Milestone.fromMap(it)
            } ?: emptyList()

            return Goal(
                id = (map["id"] as? String) ?: docId,
                userId = (map["userId"] as? String) ?: "",
                title = (map["title"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                deadline = (map["deadline"] as? String) ?: "",
                milestones = milestoneList,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                completedAt = (map["completedAt"] as? Number)?.toLong()
            )
        }
    }
}

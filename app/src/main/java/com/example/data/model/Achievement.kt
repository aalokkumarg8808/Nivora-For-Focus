package com.example.data.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "iconName" to iconName,
        "xpReward" to xpReward,
        "isUnlocked" to isUnlocked,
        "unlockedAt" to unlockedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, fallbackId: String): Achievement {
            return Achievement(
                id = (map["id"] as? String) ?: fallbackId,
                title = (map["title"] as? String) ?: "",
                description = (map["description"] as? String) ?: "",
                iconName = (map["iconName"] as? String) ?: "sprout",
                xpReward = (map["xpReward"] as? Number)?.toInt() ?: 50,
                isUnlocked = (map["isUnlocked"] as? Boolean) ?: false,
                unlockedAt = (map["unlockedAt"] as? Number)?.toLong()
            )
        }

        val DEFINED_ACHIEVEMENTS = listOf(
            Achievement(
                id = "first_sprout",
                title = "First Sprout",
                description = "Complete your first task and take the first step.",
                iconName = "sprout",
                xpReward = 50
            ),
            Achievement(
                id = "deep_root",
                title = "Deep Root",
                description = "Complete your first focus session.",
                iconName = "timer",
                xpReward = 50
            ),
            Achievement(
                id = "habit_seed",
                title = "Habit Seeder",
                description = "Check off your first daily habit.",
                iconName = "check",
                xpReward = 40
            ),
            Achievement(
                id = "botanist_streak",
                title = "Botanist Streak",
                description = "Maintain a 3-day active streak.",
                iconName = "flame",
                xpReward = 100
            ),
            Achievement(
                id = "week_of_growth",
                title = "Week of Growth",
                description = "Reach a 7-day productivity streak.",
                iconName = "trophy",
                xpReward = 200
            ),
            Achievement(
                id = "focus_master",
                title = "Deep Forest",
                description = "Accumulate 100+ total minutes of focused work.",
                iconName = "tree",
                xpReward = 150
            ),
            Achievement(
                id = "goal_crusher",
                title = "Bloom Reached",
                description = "Complete all milestones of a goal.",
                iconName = "target",
                xpReward = 250
            ),
            Achievement(
                id = "master_gardener",
                title = "Master Gardener",
                description = "Complete 10 total tasks.",
                iconName = "flower",
                xpReward = 300
            )
        )
    }
}

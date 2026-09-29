package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import com.example.data.sync.SupabaseSyncManager
import com.example.notification.FocusNotificationManager
import com.example.util.PlantUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PlantRepository(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("plant_data_prefs", Context.MODE_PRIVATE)

    private val scope = CoroutineScope(Dispatchers.IO)

    val syncManager = SupabaseSyncManager(context, authRepository)

    // Real-time StateFlows
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _focusSessions = MutableStateFlow<List<FocusSession>>(emptyList())
    val focusSessions: StateFlow<List<FocusSession>> = _focusSessions.asStateFlow()

    private val _scheduledSessions = MutableStateFlow<List<ScheduledFocusSession>>(emptyList())
    val scheduledSessions: StateFlow<List<ScheduledFocusSession>> = _scheduledSessions.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(Achievement.DEFINED_ACHIEVEMENTS)
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    // Unlock event for celebration animation
    private val _newlyUnlockedAchievement = MutableSharedFlow<Achievement>(replay = 0)
    val newlyUnlockedAchievement: SharedFlow<Achievement> = _newlyUnlockedAchievement.asSharedFlow()

    private var activeUserId: String? = null

    init {
        // Wire sync listener so cloud sync results update local StateFlows
        syncManager.onDataSyncedListener = { syncedUid ->
            if (activeUserId == syncedUid) {
                loadLocalData(syncedUid)
                checkAchievements()
            }
        }

        scope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    if (activeUserId != user.uid) {
                        activeUserId = user.uid
                        switchUser(user.uid)
                    }
                } else {
                    activeUserId = null
                    _tasks.value = emptyList()
                    _habits.value = emptyList()
                    _goals.value = emptyList()
                    _focusSessions.value = emptyList()
                    _scheduledSessions.value = emptyList()
                    _achievements.value = Achievement.DEFINED_ACHIEVEMENTS
                }
            }
        }
    }

    private fun switchUser(uid: String) {
        loadLocalData(uid)
        // Trigger initial cloud sync & restore in background
        scope.launch {
            syncManager.syncAll(uid)
        }
    }

    fun getActiveUserId(): String? {
        val uid = activeUserId ?: authRepository.getActiveUserId()
        if (activeUserId == null && uid != null) {
            activeUserId = uid
            loadLocalData(uid)
        }
        return uid
    }

    fun triggerCloudSync() {
        val uid = getActiveUserId() ?: return
        syncManager.scheduleSync(uid, delayMs = 0)
    }

    // --- Task Operations ---
    fun addTask(task: Task) {
        val uid = getActiveUserId() ?: return
        val taskId = if (task.id.isBlank()) UUID.randomUUID().toString() else syncManager.ensureUuid(task.id)
        val notifId = task.notificationId ?: (Math.abs((taskId + task.title).hashCode()) % 900000 + 1000)
        val notifIds = if (task.reminderRepeat == "Custom Days" && task.reminderDays.isNotEmpty()) {
            task.reminderDays.map { notifId + (it * 1000) }
        } else {
            listOf(notifId)
        }
        val newTask = task.copy(
            id = taskId,
            userId = uid,
            notificationId = notifId,
            notificationIds = notifIds,
            createdAt = if (task.createdAt > 0) task.createdAt else System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val updated = listOf(newTask) + _tasks.value.filter { it.id != taskId }
        _tasks.value = updated
        syncManager.saveLocalTasks(uid, updated)

        if (newTask.reminderEnabled && newTask.dueTime.isNotBlank()) {
            FocusNotificationManager.scheduleTaskReminder(context, newTask)
        }

        // Direct cloud write immediately + schedule debounce
        syncManager.syncTaskDirectAsync(uid, newTask)
        syncManager.scheduleSync(uid)
    }

    fun updateTask(task: Task) {
        val uid = getActiveUserId() ?: return
        val existingTask = _tasks.value.firstOrNull { it.id == task.id }
        if (existingTask != null) {
            FocusNotificationManager.cancelTaskReminder(context, existingTask)
        }

        val notifId = task.notificationId ?: (existingTask?.notificationId ?: (Math.abs((task.id + task.title).hashCode()) % 900000 + 1000))
        val notifIds = if (task.reminderRepeat == "Custom Days" && task.reminderDays.isNotEmpty()) {
            task.reminderDays.map { notifId + (it * 1000) }
        } else {
            listOf(notifId)
        }
        val updatedTask = task.copy(
            userId = uid,
            notificationId = notifId,
            notificationIds = notifIds,
            updatedAt = System.currentTimeMillis()
        )

        val updated = _tasks.value.map { if (it.id == task.id) updatedTask else it }
        _tasks.value = updated
        syncManager.saveLocalTasks(uid, updated)

        if (updatedTask.reminderEnabled && updatedTask.dueTime.isNotBlank() && !updatedTask.completed) {
            FocusNotificationManager.scheduleTaskReminder(context, updatedTask)
        }

        syncManager.syncTaskDirectAsync(uid, updatedTask)
        syncManager.scheduleSync(uid)
    }

    fun toggleTask(taskId: String) {
        val uid = getActiveUserId() ?: return
        var newlyCompleted = false
        var targetTask: Task? = null
        val updated = _tasks.value.map { task ->
            if (task.id == taskId) {
                val nextCompleted = !task.completed
                if (nextCompleted) {
                    newlyCompleted = true
                    FocusNotificationManager.cancelTaskReminder(context, task)
                } else if (task.reminderEnabled && task.dueTime.isNotBlank()) {
                    FocusNotificationManager.scheduleTaskReminder(context, task.copy(completed = false))
                }
                val modified = task.copy(
                    completed = nextCompleted,
                    completedAt = if (nextCompleted) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                )
                targetTask = modified
                modified
            } else task
        }
        _tasks.value = updated
        syncManager.saveLocalTasks(uid, updated)

        targetTask?.let { syncManager.syncTaskDirectAsync(uid, it) }
        syncManager.scheduleSync(uid)

        if (newlyCompleted) {
            awardXP(PlantUtils.TASK_COMPLETION_XP, tasksDelta = 1)
        }
        checkAchievements()
        syncManager.scheduleSync(uid)
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        val uid = activeUserId ?: return
        var targetTask: Task? = null
        val updated = _tasks.value.map { task ->
            if (task.id == taskId) {
                val updatedSubtasks = task.subtasks.map { sub ->
                    if (sub.id == subtaskId) sub.copy(completed = !sub.completed) else sub
                }
                val modified = task.copy(subtasks = updatedSubtasks, updatedAt = System.currentTimeMillis())
                targetTask = modified
                modified
            } else task
        }
        _tasks.value = updated
        syncManager.saveLocalTasks(uid, updated)
        targetTask?.let { syncManager.syncTaskDirectAsync(uid, it) }
        syncManager.scheduleSync(uid)
    }

    fun deleteTask(taskId: String) {
        val uid = getActiveUserId() ?: return
        val canonicalId = syncManager.ensureUuid(taskId)
        val target = _tasks.value.firstOrNull { it.id == taskId || it.id == canonicalId }
        if (target != null) {
            FocusNotificationManager.cancelTaskReminder(context, target)
        }
        val updated = _tasks.value.filterNot { it.id == taskId || it.id == canonicalId }
        _tasks.value = updated
        syncManager.saveLocalTasks(uid, updated)

        syncManager.markRecordDeleted(uid, "tasks", canonicalId)
        syncManager.scheduleSync(uid)
    }

    // --- Habit Operations ---
    fun addHabit(habit: Habit) {
        val uid = getActiveUserId() ?: return
        val habitId = if (habit.id.isBlank()) UUID.randomUUID().toString() else syncManager.ensureUuid(habit.id)
        val newHabit = habit.copy(
            id = habitId,
            userId = uid,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val updated = listOf(newHabit) + _habits.value
        _habits.value = updated
        syncManager.saveLocalHabits(uid, updated)

        syncManager.syncHabitDirectAsync(uid, newHabit)
        syncManager.scheduleSync(uid)
    }

    fun toggleHabitForDate(habitId: String, dateStr: String) {
        val uid = getActiveUserId() ?: return
        var newlyLogged = false
        var targetHabit: Habit? = null
        val updated = _habits.value.map { habit ->
            if (habit.id == habitId) {
                val history = habit.completionHistory.toMutableList()
                if (history.contains(dateStr)) {
                    history.remove(dateStr)
                } else {
                    history.add(dateStr)
                    newlyLogged = true
                }
                val (streak, longest) = PlantUtils.calculateStreak(history)
                val modified = habit.copy(
                    completionHistory = history,
                    currentStreak = streak,
                    longestStreak = maxOf(habit.longestStreak, longest),
                    updatedAt = System.currentTimeMillis()
                )
                targetHabit = modified
                modified
            } else habit
        }
        _habits.value = updated
        syncManager.saveLocalHabits(uid, updated)

        if (newlyLogged) {
            syncManager.syncHabitLogDirectAsync(uid, habitId, dateStr)
        }
        targetHabit?.let { syncManager.syncHabitDirectAsync(uid, it) }

        if (newlyLogged) {
            awardXP(PlantUtils.HABIT_COMPLETION_XP, habitsDelta = 1)
        }
        checkAchievements()
        syncManager.scheduleSync(uid)
    }

    fun deleteHabit(habitId: String) {
        val uid = getActiveUserId() ?: return
        val canonicalId = syncManager.ensureUuid(habitId)
        val updated = _habits.value.filterNot { it.id == habitId || it.id == canonicalId }
        _habits.value = updated
        syncManager.saveLocalHabits(uid, updated)

        syncManager.markRecordDeleted(uid, "habits", canonicalId)
        syncManager.scheduleSync(uid)
    }

    // --- Goal Operations ---
    fun addGoal(goal: Goal) {
        val uid = getActiveUserId() ?: return
        val goalId = if (goal.id.isBlank()) UUID.randomUUID().toString() else syncManager.ensureUuid(goal.id)
        val newMilestones = goal.milestones.map { m ->
            m.copy(id = if (m.id.isBlank()) UUID.randomUUID().toString() else syncManager.ensureUuid(m.id))
        }
        val newGoal = goal.copy(
            id = goalId,
            userId = uid,
            milestones = newMilestones,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val updated = listOf(newGoal) + _goals.value
        _goals.value = updated
        syncManager.saveLocalGoals(uid, updated)

        syncManager.syncGoalDirectAsync(uid, newGoal)
        syncManager.scheduleSync(uid)
    }

    fun toggleMilestone(goalId: String, milestoneId: String) {
        val uid = getActiveUserId() ?: return
        var goalNewlyCompleted = false
        var targetGoal: Goal? = null
        val updated = _goals.value.map { goal ->
            if (goal.id == goalId) {
                val updatedMilestones = goal.milestones.map { m ->
                    if (m.id == milestoneId) m.copy(completed = !m.completed) else m
                }
                val allDone = updatedMilestones.isNotEmpty() && updatedMilestones.all { it.completed }
                val wasDone = goal.isCompleted
                if (allDone && !wasDone) {
                    goalNewlyCompleted = true
                }
                val modified = goal.copy(
                    milestones = updatedMilestones,
                    completedAt = if (allDone) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                )
                targetGoal = modified
                modified
            } else goal
        }
        _goals.value = updated
        syncManager.saveLocalGoals(uid, updated)

        targetGoal?.let { syncManager.syncGoalDirectAsync(uid, it) }

        if (goalNewlyCompleted) {
            awardXP(PlantUtils.GOAL_COMPLETION_XP, goalsDelta = 1)
        }
        checkAchievements()
        syncManager.scheduleSync(uid)
    }

    fun deleteGoal(goalId: String) {
        val uid = getActiveUserId() ?: return
        val canonicalId = syncManager.ensureUuid(goalId)
        val updated = _goals.value.filterNot { it.id == goalId || it.id == canonicalId }
        _goals.value = updated
        syncManager.saveLocalGoals(uid, updated)

        syncManager.markRecordDeleted(uid, "goals", canonicalId)
        syncManager.scheduleSync(uid)
    }

    // --- Focus Sessions ---
    fun saveFocusSession(session: FocusSession) {
        val uid = getActiveUserId() ?: return
        val sessionId = if (session.id.isBlank()) UUID.randomUUID().toString() else syncManager.ensureUuid(session.id)
        val newSession = session.copy(
            id = sessionId,
            userId = uid,
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(newSession) + _focusSessions.value
        _focusSessions.value = updated
        syncManager.saveLocalSessions(uid, updated)

        syncManager.syncFocusSessionDirectAsync(uid, newSession)

        awardXP(PlantUtils.FOCUS_SESSION_XP, focusMinutesDelta = newSession.durationMinutes)
        checkAchievements()
        syncManager.scheduleSync(uid)
    }

    // --- Scheduled Focus Sessions & Reminders ---
    fun addScheduledFocusSession(session: ScheduledFocusSession) {
        val uid = activeUserId ?: return
        val sessionId = if (session.id.isBlank()) "sched_" + System.currentTimeMillis() else session.id
        val newSession = session.copy(
            id = sessionId,
            userId = uid,
            createdAt = System.currentTimeMillis()
        )
        val updated = (listOf(newSession) + _scheduledSessions.value).sortedBy { it.scheduledTimeMillis }
        _scheduledSessions.value = updated
        saveScheduledLocally(uid, updated)

        FocusNotificationManager.scheduleReminder(context, newSession)
    }

    fun deleteScheduledFocusSession(sessionId: String) {
        val uid = activeUserId ?: return
        val target = _scheduledSessions.value.firstOrNull { it.id == sessionId }
        if (target != null) {
            FocusNotificationManager.cancelReminder(context, target)
        }
        val updated = _scheduledSessions.value.filterNot { it.id == sessionId }
        _scheduledSessions.value = updated
        saveScheduledLocally(uid, updated)
    }

    fun completeScheduledFocusSession(sessionId: String) {
        val uid = activeUserId ?: return
        val target = _scheduledSessions.value.firstOrNull { it.id == sessionId } ?: return
        FocusNotificationManager.cancelReminder(context, target)

        val updatedSession = target.copy(isCompleted = true)
        val updated = _scheduledSessions.value.map { if (it.id == sessionId) updatedSession else it }
        _scheduledSessions.value = updated
        saveScheduledLocally(uid, updated)

        saveFocusSession(
            FocusSession(
                type = "focus",
                durationMinutes = target.durationMinutes,
                category = target.category,
                startedAt = target.scheduledTimeMillis,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    // --- XP & Dynamic Level Updates ---
    private fun awardXP(
        xpDelta: Int,
        tasksDelta: Int = 0,
        habitsDelta: Int = 0,
        goalsDelta: Int = 0,
        focusMinutesDelta: Int = 0
    ) {
        val user = authRepository.currentUser.value ?: return
        val newXp = user.xp + xpDelta
        val newLevel = PlantUtils.calculateLevel(newXp)
        val allHabitDates = _habits.value.flatMap { it.completionHistory }
        val (calcCurrentStreak, calcLongest) = PlantUtils.calculateStreak(allHabitDates)

        val updatedUser = user.copy(
            xp = newXp,
            level = newLevel,
            totalFocusMinutes = user.totalFocusMinutes + focusMinutesDelta,
            completedTasks = user.completedTasks + tasksDelta,
            completedHabits = user.completedHabits + habitsDelta,
            completedGoals = user.completedGoals + goalsDelta,
            currentStreak = maxOf(user.currentStreak, calcCurrentStreak),
            longestStreak = maxOf(user.longestStreak, calcLongest),
            updatedAt = System.currentTimeMillis()
        )
        scope.launch {
            authRepository.updateProfile(updatedUser)
        }
    }

    private fun checkAchievements() {
        val user = authRepository.currentUser.value ?: return
        val uid = user.uid
        val totalCompletedTasks = _tasks.value.count { it.completed }
        val totalFocusMinutes = _focusSessions.value.sumOf { it.durationMinutes }
        val totalCompletedHabitLogs = _habits.value.flatMap { it.completionHistory }.size
        val allHabitDates = _habits.value.flatMap { it.completionHistory }
        val (streak, _) = PlantUtils.calculateStreak(allHabitDates)
        val completedGoalsCount = _goals.value.count { it.isCompleted }
        val totalSessions = _focusSessions.value.size

        var anyUnlocked = false
        val newlyUnlockedList = mutableListOf<Achievement>()
        val currentList = _achievements.value.map { ach ->
            if (ach.isUnlocked) {
                ach
            } else {
                val shouldUnlock = when (ach.id) {
                    "first_sprout" -> totalCompletedTasks >= 1
                    "deep_root" -> totalSessions >= 1
                    "habit_seed" -> totalCompletedHabitLogs >= 1
                    "botanist_streak" -> streak >= 3
                    "week_of_growth" -> streak >= 7
                    "focus_master" -> totalFocusMinutes >= 100
                    "goal_crusher" -> completedGoalsCount >= 1
                    "master_gardener" -> totalCompletedTasks >= 10
                    else -> false
                }
                if (shouldUnlock) {
                    anyUnlocked = true
                    val unlocked = ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                    newlyUnlockedList.add(unlocked)
                    scope.launch {
                        _newlyUnlockedAchievement.emit(unlocked)
                    }
                    awardXP(ach.xpReward)
                    unlocked
                } else ach
            }
        }
        _achievements.value = currentList
        if (anyUnlocked && uid.isNotBlank()) {
            syncManager.saveLocalAchievements(uid, currentList)
            newlyUnlockedList.forEach { syncManager.syncAchievementDirectAsync(uid, it) }
            syncManager.scheduleSync(uid)
        }
    }

    // --- Local Persistence Helpers ---
    private fun loadLocalData(uid: String) {
        try {
            val tasksList = syncManager.getLocalTasks(uid)
            _tasks.value = tasksList
            tasksList.filter { !it.completed && it.reminderEnabled && it.dueTime.isNotBlank() }
                .forEach { FocusNotificationManager.scheduleTaskReminder(context, it) }

            val habitsList = syncManager.getLocalHabits(uid)
            _habits.value = habitsList

            val goalsList = syncManager.getLocalGoals(uid)
            _goals.value = goalsList

            val sessionsList = syncManager.getLocalSessions(uid)
            _focusSessions.value = sessionsList

            val achievementsList = syncManager.getLocalAchievements(uid)
            _achievements.value = achievementsList

            val scheduledJson = prefs.getString("scheduled_focus_$uid", null)
            if (scheduledJson != null) {
                val array = JSONArray(scheduledJson)
                val list = mutableListOf<ScheduledFocusSession>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(jsonToScheduled(obj))
                }
                _scheduledSessions.value = list
                list.filter { !it.isCompleted && it.reminderTriggerTimeMillis > System.currentTimeMillis() }
                    .forEach { FocusNotificationManager.scheduleReminder(context, it) }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun saveScheduledLocally(uid: String, list: List<ScheduledFocusSession>) {
        try {
            val array = JSONArray()
            list.forEach { session ->
                val obj = JSONObject().apply {
                    put("id", session.id)
                    put("userId", session.userId)
                    put("title", session.title)
                    put("category", session.category)
                    put("scheduledTimeMillis", session.scheduledTimeMillis)
                    put("durationMinutes", session.durationMinutes)
                    put("reminderMinutesBefore", session.reminderMinutesBefore)
                    put("isCompleted", session.isCompleted)
                    put("createdAt", session.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString("scheduled_focus_$uid", array.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun jsonToScheduled(obj: JSONObject): ScheduledFocusSession {
        return ScheduledFocusSession(
            id = obj.getString("id"),
            userId = obj.optString("userId", ""),
            title = obj.getString("title"),
            category = obj.optString("category", "Study"),
            scheduledTimeMillis = obj.optLong("scheduledTimeMillis", System.currentTimeMillis()),
            durationMinutes = obj.optInt("durationMinutes", 25),
            reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 10),
            isCompleted = obj.optBoolean("isCompleted", false),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.PlantRepository
import com.example.notification.FocusNotificationManager
import com.example.util.PlantUtils
import com.example.util.SoundscapeManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenTab {
    HOME,
    TASKS,
    FOCUS,
    HABITS,
    PROFILE,
    GOALS,
    INSIGHTS
}

enum class TasksSubTab {
    TASKS,
    HABITS
}

enum class TimerMode(val title: String, val minutes: Int) {
    FOCUS("Focus", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

class PlantViewModel(application: Application) : AndroidViewModel(application) {
    val authRepository = AuthRepository(application)
    val plantRepository = PlantRepository(application, authRepository)

    // Auth State
    val currentUser = authRepository.currentUser
    val isAuthLoading = authRepository.isLoading
    val authError = authRepository.authError

    // Navigation State
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Sub-tab within Tasks screen (Tasks vs Habits)
    private val _tasksSubTab = MutableStateFlow(TasksSubTab.TASKS)
    val tasksSubTab: StateFlow<TasksSubTab> = _tasksSubTab.asStateFlow()

    fun setTasksSubTab(subTab: TasksSubTab) {
        _tasksSubTab.value = subTab
    }

    // Selected Date for dynamic calendar filtering
    private val _selectedDate = MutableStateFlow(PlantUtils.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Task filtering
    private val _selectedTaskCategory = MutableStateFlow("All")
    val selectedTaskCategory: StateFlow<String> = _selectedTaskCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Bottom sheet dialog states
    private val _showCreateSheet = MutableStateFlow(false)
    val showCreateSheet: StateFlow<Boolean> = _showCreateSheet.asStateFlow()

    private val _showAddTaskDialog = MutableStateFlow(false)
    val showAddTaskDialog: StateFlow<Boolean> = _showAddTaskDialog.asStateFlow()

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _showAddHabitDialog = MutableStateFlow(false)
    val showAddHabitDialog: StateFlow<Boolean> = _showAddHabitDialog.asStateFlow()

    private val _showAddGoalDialog = MutableStateFlow(false)
    val showAddGoalDialog: StateFlow<Boolean> = _showAddGoalDialog.asStateFlow()

    private val _showScheduleDialog = MutableStateFlow(false)
    val showScheduleDialog: StateFlow<Boolean> = _showScheduleDialog.asStateFlow()

    private val _showEditProfileDialog = MutableStateFlow(false)
    val showEditProfileDialog: StateFlow<Boolean> = _showEditProfileDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showEditFocusDialog = MutableStateFlow(false)
    val showEditFocusDialog: StateFlow<Boolean> = _showEditFocusDialog.asStateFlow()

    private val _selectedAchievementDetail = MutableStateFlow<Achievement?>(null)
    val selectedAchievementDetail: StateFlow<Achievement?> = _selectedAchievementDetail.asStateFlow()

    // Achievement unlock celebration
    private val _unlockedAchievementPopup = MutableStateFlow<Achievement?>(null)
    val unlockedAchievementPopup: StateFlow<Achievement?> = _unlockedAchievementPopup.asStateFlow()

    // Global Theme Preference State (System, Light, Dark)
    private val _themePreference = MutableStateFlow(authRepository.getSavedThemePreference())
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun setThemePreference(theme: String) {
        _themePreference.value = theme
        authRepository.saveThemePreference(theme)
    }

    // Focus Timer State
    private val _timerMode = MutableStateFlow(TimerMode.FOCUS)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _timerDurationSeconds = MutableStateFlow(25 * 60)
    val timerDurationSeconds: StateFlow<Int> = _timerDurationSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(25 * 60)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _focusCategory = MutableStateFlow("Study")
    val focusCategory: StateFlow<String> = _focusCategory.asStateFlow()

    private val _focusTitle = MutableStateFlow("Deep Focus")
    val focusTitle: StateFlow<String> = _focusTitle.asStateFlow()

    private val _focusScene = MutableStateFlow("Forest")
    val focusScene: StateFlow<String> = _focusScene.asStateFlow()

    private val _notifySessionEnd = MutableStateFlow(true)
    val notifySessionEnd: StateFlow<Boolean> = _notifySessionEnd.asStateFlow()

    val soundscapePlaying: StateFlow<Boolean> = SoundscapeManager.isPlaying
    val soundscapeVolume: StateFlow<Float> = SoundscapeManager.volume
    val soundscapeCurrentScene: StateFlow<String> = SoundscapeManager.currentSoundscape

    private var countDownTimer: CountDownTimer? = null

    // Raw Data Flows from Repository
    val allTasks = plantRepository.tasks
    val allHabits = plantRepository.habits
    val allGoals = plantRepository.goals
    val allFocusSessions = plantRepository.focusSessions
    val allScheduledSessions = plantRepository.scheduledSessions
    val allAchievements = plantRepository.achievements

    val upcomingScheduledSessions = plantRepository.scheduledSessions.map { list ->
        list.filter { !it.isCompleted }.sortedBy { it.scheduledTimeMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Calculations derived from actual data
    val todayTasks = combine(allTasks, _selectedDate) { tasks, selectedDateStr ->
        val todayStr = PlantUtils.getTodayDateString()
        tasks.filter { it.dueDate.isBlank() || it.dueDate == selectedDateStr || it.dueDate == todayStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCompletedCount = todayTasks.map { list ->
        list.count { it.completed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTotalCount = todayTasks.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayCompletionPercentage = combine(todayCompletedCount, todayTotalCount) { done, total ->
        if (total == 0) 0 else ((done.toFloat() / total) * 100).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTasksRemaining = combine(todayTotalCount, todayCompletedCount) { total, done ->
        maxOf(0, total - done)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overdueTasksCount = allTasks.map { tasks ->
        val today = PlantUtils.getTodayDateString()
        tasks.count { !it.completed && it.dueDate.isNotBlank() && it.dueDate < today }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Real-time active productivity dates (tasks, habits, focus sessions)
    val activeProductivityDates = combine(
        allTasks,
        allHabits,
        allFocusSessions
    ) { tasks, habits, sessions ->
        val dates = mutableSetOf<String>()
        habits.forEach { habit ->
            dates.addAll(habit.completionHistory)
        }
        sessions.forEach { session ->
            if (session.completedAt > 0) {
                dates.add(PlantUtils.getDateStringFromMillis(session.completedAt))
            }
        }
        tasks.filter { it.completed }.forEach { task ->
            if (task.completedAt != null && task.completedAt > 0) {
                dates.add(PlantUtils.getDateStringFromMillis(task.completedAt))
            } else if (task.dueDate.isNotBlank()) {
                dates.add(task.dueDate)
            }
        }
        dates as Set<String>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Real dynamic streak calculation
    val dynamicStreak = combine(activeProductivityDates, currentUser) { dates, user ->
        val (calcCurrent, calcLongest) = PlantUtils.calculateStreak(dates.toList())
        val profileCurrent = user?.currentStreak ?: 0
        val profileLongest = user?.longestStreak ?: 0
        val current = maxOf(calcCurrent, profileCurrent)
        val longest = maxOf(current, maxOf(calcLongest, profileLongest))
        current to longest
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    // Focus Sessions today (Accurate real completed focus sessions)
    val todayFocusSessions = allFocusSessions.map { sessions ->
        val todayStr = PlantUtils.getTodayDateString()
        sessions.filter { session ->
            PlantUtils.getDateStringFromMillis(session.completedAt) == todayStr
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayFocusMinutes = todayFocusSessions.map { list ->
        list.sumOf { it.durationMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayFocusSessionCount = todayFocusSessions.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayAvgSessionMinutes = combine(todayFocusMinutes, todayFocusSessionCount) { totalMin, count ->
        if (count > 0) totalMin / count else 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFocusMinutesAllTime = allFocusSessions.map { sessions ->
        sessions.sumOf { it.durationMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFocusSessionsAllTime = allFocusSessions.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyFocusMinutes = allFocusSessions.map { sessions ->
        val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
        sessions.filter { it.completedAt >= oneWeekAgo }.sumOf { it.durationMinutes }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Habits stats
    val todayHabitsCompleted = combine(allHabits, _selectedDate) { habits, selectedDateStr ->
        habits.count { it.completionHistory.contains(selectedDateStr) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayHabitsTotal = allHabits.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayHabitsRemaining = combine(todayHabitsTotal, todayHabitsCompleted) { total, done ->
        maxOf(0, total - done)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val habitsBestStreak = allHabits.map { habits ->
        habits.maxOfOrNull { it.currentStreak } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Goals stats
    val activeGoalsCount = allGoals.map { goals ->
        goals.count { !it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedGoalsCount = allGoals.map { goals ->
        goals.count { it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overallGoalProgressPct = allGoals.map { goals ->
        if (goals.isEmpty()) 0
        else ((goals.map { it.progress }.average()) * 100).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filteredTasks = combine(allTasks, _selectedTaskCategory, _searchQuery) { tasks, category, query ->
        tasks.filter { task ->
            val matchesCategory = (category == "All" || task.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() || task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            plantRepository.newlyUnlockedAchievement.collect { achievement ->
                _unlockedAchievementPopup.value = achievement
            }
        }
    }

    fun setTab(tab: ScreenTab) {
        if (tab == ScreenTab.HABITS) {
            _tasksSubTab.value = TasksSubTab.HABITS
            _currentTab.value = ScreenTab.TASKS
        } else {
            _currentTab.value = tab
        }
    }

    fun setSelectedDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun setSelectedCategory(category: String) {
        _selectedTaskCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCreateSheet(show: Boolean) {
        _showCreateSheet.value = show
    }

    fun toggleAddTaskDialog(show: Boolean) {
        _showAddTaskDialog.value = show
    }

    fun setEditingTask(task: Task?) {
        _editingTask.value = task
    }

    fun toggleAddHabitDialog(show: Boolean) {
        _showAddHabitDialog.value = show
    }

    fun toggleAddGoalDialog(show: Boolean) {
        _showAddGoalDialog.value = show
    }

    fun toggleScheduleDialog(show: Boolean) {
        _showScheduleDialog.value = show
    }

    fun scheduleFocusSession(
        title: String,
        category: String,
        durationMinutes: Int,
        scheduledTimeMillis: Long,
        reminderMinutesBefore: Int
    ) {
        if (title.isBlank()) return
        val session = ScheduledFocusSession(
            title = title.trim(),
            category = category,
            durationMinutes = durationMinutes,
            scheduledTimeMillis = scheduledTimeMillis,
            reminderMinutesBefore = reminderMinutesBefore
        )
        plantRepository.addScheduledFocusSession(session)
        _showScheduleDialog.value = false
    }

    fun deleteScheduledSession(sessionId: String) {
        plantRepository.deleteScheduledFocusSession(sessionId)
    }

    fun completeScheduledSession(sessionId: String) {
        plantRepository.completeScheduledFocusSession(sessionId)
    }

    fun startScheduledFocusNow(session: ScheduledFocusSession) {
        setFocusCategory(session.category)
        pauseTimer()
        val secs = session.durationMinutes * 60
        _timerDurationSeconds.value = secs
        _timerRemainingSeconds.value = secs
        _timerMode.value = TimerMode.FOCUS
        setTab(ScreenTab.FOCUS)
        startTimer()
    }

    fun dismissAchievementPopup() {
        _unlockedAchievementPopup.value = null
    }

    fun toggleEditProfileDialog(show: Boolean) {
        _showEditProfileDialog.value = show
    }

    fun toggleSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun selectAchievementDetail(achievement: Achievement?) {
        _selectedAchievementDetail.value = achievement
    }

    fun updateUserProfile(name: String, photoUrl: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.updateProfileInfo(name, photoUrl)
            onResult(res.isSuccess)
        }
    }

    fun updateUserSettings(
        themePreference: String,
        reduceMotion: Boolean,
        focusReminders: Boolean,
        taskReminders: Boolean,
        habitReminders: Boolean,
        focusSounds: Boolean,
        completionSound: Boolean
    ) {
        _themePreference.value = themePreference
        authRepository.saveThemePreference(themePreference)
        val user = currentUser.value ?: return
        val updated = user.copy(
            themePreference = themePreference,
            reduceMotion = reduceMotion,
            focusReminders = focusReminders,
            taskReminders = taskReminders,
            habitReminders = habitReminders,
            focusSounds = focusSounds,
            completionSound = completionSound,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            authRepository.updateProfile(updated)
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.changePassword(newPassword)
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Failed to update password")
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.deleteAccount()
            setTab(ScreenTab.HOME)
            onSuccess()
        }
    }

    // --- Task Actions ---
    fun addTask(
        title: String,
        category: String,
        dueDate: String = "",
        dueTime: String = "",
        reminderEnabled: Boolean = false,
        reminderRepeat: String = "Once",
        reminderDays: List<Int> = emptyList(),
        priority: String = "Medium",
        subtasks: List<String> = emptyList()
    ) {
        if (title.isBlank()) return
        val subs = subtasks.filter { it.isNotBlank() }.mapIndexed { index, name ->
            Subtask(id = "sub_${System.currentTimeMillis()}_$index", title = name, completed = false)
        }
        val task = Task(
            title = title.trim(),
            category = category,
            priority = priority,
            dueDate = dueDate.ifBlank { PlantUtils.getTodayDateString() },
            dueTime = dueTime.trim(),
            reminderEnabled = reminderEnabled,
            reminderRepeat = reminderRepeat,
            reminderDays = reminderDays,
            subtasks = subs
        )
        plantRepository.addTask(task)
        _showAddTaskDialog.value = false
    }

    fun updateTask(task: Task) {
        plantRepository.updateTask(task)
        _editingTask.value = null
    }

    fun toggleTask(taskId: String) {
        plantRepository.toggleTask(taskId)
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        plantRepository.toggleSubtask(taskId, subtaskId)
    }

    fun deleteTask(taskId: String) {
        plantRepository.deleteTask(taskId)
    }

    // --- Habit Actions ---
    fun addHabit(name: String, frequency: String, category: String) {
        if (name.isBlank()) return
        val habit = Habit(
            name = name.trim(),
            frequency = frequency,
            category = category
        )
        plantRepository.addHabit(habit)
        _showAddHabitDialog.value = false
    }

    fun toggleHabitForDate(habitId: String, dateStr: String) {
        plantRepository.toggleHabitForDate(habitId, dateStr)
    }

    fun deleteHabit(habitId: String) {
        plantRepository.deleteHabit(habitId)
    }

    // --- Goal Actions ---
    fun addGoal(title: String, deadline: String, milestones: List<String>) {
        if (title.isBlank()) return
        val mList = milestones.filter { it.isNotBlank() }.mapIndexed { index, name ->
            Milestone(id = "ms_${System.currentTimeMillis()}_$index", title = name, completed = false)
        }
        val goal = Goal(
            title = title.trim(),
            deadline = deadline,
            milestones = mList
        )
        plantRepository.addGoal(goal)
        _showAddGoalDialog.value = false
    }

    fun toggleMilestone(goalId: String, milestoneId: String) {
        plantRepository.toggleMilestone(goalId, milestoneId)
    }

    fun deleteGoal(goalId: String) {
        plantRepository.deleteGoal(goalId)
    }

    // --- Timer Actions ---
    fun setTimerMode(mode: TimerMode) {
        pauseTimer()
        _timerMode.value = mode
        val totalSecs = mode.minutes * 60
        _timerDurationSeconds.value = totalSecs
        _timerRemainingSeconds.value = totalSecs
    }

    fun setFocusCategory(cat: String) {
        _focusCategory.value = cat
    }

    fun setFocusDuration(durationMinutes: Int) {
        if (!_isTimerRunning.value) {
            val validMins = durationMinutes.coerceIn(1, 720)
            val totalSecs = validMins * 60
            _timerDurationSeconds.value = totalSecs
            _timerRemainingSeconds.value = totalSecs
        }
    }

    fun setFocusTitle(title: String) {
        _focusTitle.value = title
    }

    fun setFocusScene(scene: String) {
        _focusScene.value = scene
        if (SoundscapeManager.isPlaying.value || (scene != "None" && _isTimerRunning.value)) {
            SoundscapeManager.play(scene)
        }
    }

    fun setNotifySessionEnd(enabled: Boolean) {
        _notifySessionEnd.value = enabled
    }

    fun toggleEditFocusDialog(show: Boolean) {
        _showEditFocusDialog.value = show
    }

    fun updateFocusSessionConfig(
        title: String,
        durationMinutes: Int,
        category: String,
        scene: String,
        notifyEnd: Boolean
    ) {
        _focusTitle.value = if (title.isBlank()) "Focus Session" else title.trim()
        _focusCategory.value = category
        _focusScene.value = scene
        _notifySessionEnd.value = notifyEnd

        if (!_isTimerRunning.value) {
            val totalSecs = durationMinutes.coerceAtLeast(1) * 60
            _timerDurationSeconds.value = totalSecs
            _timerRemainingSeconds.value = totalSecs
        }
        _showEditFocusDialog.value = false
    }

    fun toggleSoundscapePlayback() {
        if (SoundscapeManager.isPlaying.value) {
            SoundscapeManager.pause()
        } else {
            val scene = if (_focusScene.value == "None") "Forest" else _focusScene.value
            _focusScene.value = scene
            SoundscapeManager.play(scene)
        }
    }

    fun setSoundscapeVolume(vol: Float) {
        SoundscapeManager.setVolume(vol)
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true

        if (_focusScene.value != "None" && !SoundscapeManager.isPlaying.value) {
            SoundscapeManager.play(_focusScene.value)
        }

        val remainingMillis = _timerRemainingSeconds.value * 1000L
        countDownTimer = object : CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _timerRemainingSeconds.value = (millisUntilFinished / 1000L).toInt()
            }

            override fun onFinish() {
                _timerRemainingSeconds.value = 0
                _isTimerRunning.value = false
                completeTimerSession()
            }
        }.start()
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        _isTimerRunning.value = false
        SoundscapeManager.pause()
    }

    fun resetTimer() {
        pauseTimer()
        _timerRemainingSeconds.value = _timerDurationSeconds.value
    }

    private fun completeTimerSession() {
        val mode = _timerMode.value
        val durationMins = _timerDurationSeconds.value / 60
        val sessionCat = _focusCategory.value
        val sessionScene = _focusScene.value

        if (mode == TimerMode.FOCUS) {
            val session = FocusSession(
                type = "focus",
                durationMinutes = if (durationMins > 0) durationMins else 25,
                category = sessionCat,
                scene = sessionScene,
                startedAt = System.currentTimeMillis() - (durationMins * 60 * 1000L),
                completedAt = System.currentTimeMillis()
            )
            plantRepository.saveFocusSession(session)

            if (_notifySessionEnd.value) {
                FocusNotificationManager.showFocusCompleteNotification(
                    context = getApplication(),
                    category = sessionCat,
                    durationMinutes = session.durationMinutes
                )
            }
        }
        SoundscapeManager.pause()
        resetTimer()
    }

    // --- Auth Actions ---
    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = authRepository.login(email, pass)
            if (res.isSuccess) onSuccess()
        }
    }

    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = authRepository.register(name, email, pass)
            if (res.isSuccess) onSuccess()
        }
    }

    fun logout() {
        authRepository.logout()
        setTab(ScreenTab.HOME)
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}

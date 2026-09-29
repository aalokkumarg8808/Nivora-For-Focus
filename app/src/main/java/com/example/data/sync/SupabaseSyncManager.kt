package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.util.Log
import com.example.data.local.PlantDatabase
import com.example.data.local.entity.UserEntity
import com.example.data.model.*
import com.example.data.remote.supabase.SupabaseAuthService
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseConfig
import com.example.data.repository.AuthRepository
import com.example.util.AvatarOptimizer
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class RemoteTaskDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val reminderEnabled: Boolean,
    val reminderRepeat: String,
    val reminderDays: List<Int>,
    val category: String,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
) {
    fun toDomainTask(currentUserId: String): Task = Task(
        id = id,
        userId = currentUserId,
        title = title,
        description = description,
        category = category,
        dueDate = date,
        dueTime = time,
        reminderEnabled = reminderEnabled,
        reminderRepeat = reminderRepeat,
        reminderDays = reminderDays,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completed = completed,
        completedAt = if (completed) updatedAt else null
    )
}

data class RemoteHabitDto(
    val id: String,
    val userId: String,
    val title: String,
    val frequency: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
) {
    fun toDomainHabit(currentUserId: String, history: List<String>): Habit = Habit(
        id = id,
        userId = currentUserId,
        name = title,
        frequency = frequency,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completionHistory = history
    )
}

data class RemoteGoalDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val deadline: String,
    val progress: Float,
    val completed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
) {
    fun toDomainGoal(currentUserId: String, milestones: List<Milestone>): Goal = Goal(
        id = id,
        userId = currentUserId,
        title = title,
        description = description,
        deadline = deadline,
        milestones = milestones,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = if (completed) updatedAt else null
    )
}

/**
 * Single synchronization manager responsible for SQLite ↔ Supabase cloud data synchronization.
 * Handles local-first persistence, immediate cloud push, background bidirectional pull,
 * deterministic conflict resolution, soft-deletions, and avatar sync.
 */
class SupabaseSyncManager(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "SupabaseSyncManager"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("plant_sync_prefs", Context.MODE_PRIVATE)

    private val dataPrefs: SharedPreferences =
        context.getSharedPreferences("plant_data_prefs", Context.MODE_PRIVATE)

    private val userDao = PlantDatabase.getInstance(context).userDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val JPEG_MEDIA_TYPE = "image/jpeg".toMediaType()

    // Debounce map for scheduled sync
    private val pendingSyncJobs = ConcurrentHashMap<String, Job>()

    // Callback for repository update when sync finishes
    var onDataSyncedListener: ((userId: String) -> Unit)? = null

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val activeUid = authRepository.currentUser.value?.uid
                    if (!activeUid.isNullOrBlank()) {
                        Log.d(TAG, "Network restored, triggering cloud sync for user: $activeUid")
                        scheduleSync(activeUid, delayMs = 1000)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun ensureAuthToken() {
        if (SupabaseClient.getAuthToken().isNullOrBlank()) {
            val token = authRepository.getStoredAccessToken()
            if (!token.isNullOrBlank()) {
                SupabaseClient.setAuthToken(token)
            }
        }
    }

    /**
     * Schedules a debounced synchronization.
     */
    fun scheduleSync(userId: String, delayMs: Long = 600) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return
        ensureAuthToken()

        pendingSyncJobs[userId]?.cancel()
        pendingSyncJobs[userId] = scope.launch {
            delay(delayMs)
            syncAll(userId)
        }
    }

    /**
     * Full bidirectional synchronization for the given user.
     */
    suspend fun syncAll(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return@withContext false
        ensureAuthToken()

        try {
            Log.d(TAG, "Starting full Supabase cloud sync for user: $userId")

            syncProfileAndAvatar(userId)
            syncTasks(userId)
            syncHabitsAndLogs(userId)
            syncGoalsAndMilestones(userId)
            syncFocusSessions(userId)
            syncAchievements(userId)

            withContext(Dispatchers.Main) {
                onDataSyncedListener?.invoke(userId)
            }

            Log.d(TAG, "Full Supabase cloud sync completed successfully for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during full Supabase cloud sync", e)
            false
        }
    }

    // =========================================================================
    // 1. DIRECT WRITE METHODS (IMMEDIATE CLOUD WRITES)
    // =========================================================================

    fun syncTaskDirectAsync(userId: String, task: Task) {
        ensureAuthToken()
        scope.launch {
            syncTaskDirect(userId, task)
        }
    }

    suspend fun syncTaskDirect(userId: String, task: Task): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return@withContext false
        ensureAuthToken()
        try {
            val taskUuid = ensureUuid(task.id)
            val jsonArray = JSONArray().apply {
                put(taskToJsonObject(userId, task.copy(id = taskUuid, userId = userId)))
            }

            val url = "${SupabaseConfig.restEndpoint}tasks?on_conflict=id"
            val request = Request.Builder()
                .url(url)
                .post(jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || code == 200 || code == 201) {
                Log.d(TAG, "Supabase sync success: tasks (id=$taskUuid, title=${task.title})")
                true
            } else {
                Log.e(TAG, "Supabase sync failure: tasks - HTTP $code: $body")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync exception: tasks", e)
            false
        }
    }

    fun syncHabitDirectAsync(userId: String, habit: Habit) {
        scope.launch {
            syncHabitDirect(userId, habit)
        }
    }

    suspend fun syncHabitDirect(userId: String, habit: Habit): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return@withContext false
        try {
            val habitUuid = ensureUuid(habit.id)
            val jsonArray = JSONArray().apply {
                put(habitToJsonObject(userId, habit.copy(id = habitUuid, userId = userId)))
            }

            val url = "${SupabaseConfig.restEndpoint}habits?on_conflict=id"
            val request = Request.Builder()
                .url(url)
                .post(jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || code == 200 || code == 201) {
                Log.d(TAG, "Supabase sync success: habits")
                true
            } else {
                Log.e(TAG, "Supabase sync failure: habits - HTTP $code: $body")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync exception: habits", e)
            false
        }
    }

    fun syncHabitLogDirectAsync(userId: String, habitId: String, date: String) {
        scope.launch {
            syncHabitLogDirect(userId, habitId, date)
        }
    }

    suspend fun syncHabitLogDirect(userId: String, habitId: String, date: String): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return@withContext false
        try {
            val habitUuid = ensureUuid(habitId)
            val logId = UUID.nameUUIDFromBytes("${habitUuid}_$date".toByteArray()).toString()
            val jsonArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", logId)
                    put("habit_id", habitUuid)
                    put("user_id", userId)
                    put("date", date)
                    put("completed", true)
                    put("created_at", millisToIso(System.currentTimeMillis()))
                    put("updated_at", millisToIso(System.currentTimeMillis()))
                })
            }

            val url = "${SupabaseConfig.restEndpoint}habit_logs?on_conflict=habit_id,date"
            val request = Request.Builder()
                .url(url)
                .post(jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || code == 200 || code == 201) {
                Log.d(TAG, "Supabase sync success: habit_logs")
                true
            } else {
                Log.e(TAG, "Supabase sync failure: habit_logs - HTTP $code: $body")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync exception: habit_logs", e)
            false
        }
    }

    fun syncGoalDirectAsync(userId: String, goal: Goal) {
        scope.launch {
            syncGoalDirect(userId, goal)
        }
    }

    suspend fun syncGoalDirect(userId: String, goal: Goal): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return@withContext false
        try {
            val goalUuid = ensureUuid(goal.id)
            val goalsArray = JSONArray().apply {
                put(goalToJsonObject(userId, goal.copy(id = goalUuid, userId = userId)))
            }

            val url = "${SupabaseConfig.restEndpoint}goals?on_conflict=id"
            val request = Request.Builder()
                .url(url)
                .post(goalsArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || code == 200 || code == 201) {
                Log.d(TAG, "Supabase sync success: goals")

                if (goal.milestones.isNotEmpty()) {
                    val msArray = JSONArray()
                    goal.milestones.forEach { m ->
                        msArray.put(JSONObject().apply {
                            put("id", ensureUuid(m.id))
                            put("goal_id", goalUuid)
                            put("user_id", userId)
                            put("title", m.title)
                            put("completed", m.completed)
                            put("created_at", millisToIso(goal.createdAt))
                            put("updated_at", millisToIso(goal.updatedAt))
                        })
                    }
                    val msUrl = "${SupabaseConfig.restEndpoint}milestones?on_conflict=id"
                    val msReq = Request.Builder()
                        .url(msUrl)
                        .post(msArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .header("Prefer", "resolution=merge-duplicates,return=representation")
                        .build()
                    val msResp = SupabaseClient.httpClient.newCall(msReq).execute()
                    if (msResp.isSuccessful) {
                        Log.d(TAG, "Supabase sync success: milestones")
                    } else {
                        Log.e(TAG, "Supabase sync failure: milestones - HTTP ${msResp.code}: ${msResp.body?.string()}")
                    }
                }
                true
            } else {
                Log.e(TAG, "Supabase sync failure: goals - HTTP $code: $body")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync exception: goals", e)
            false
        }
    }

    fun syncFocusSessionDirectAsync(userId: String, session: FocusSession) {
        scope.launch {
            syncFocusSessionDirect(userId, session)
        }
    }

    suspend fun syncFocusSessionDirect(userId: String, session: FocusSession): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return@withContext false
        try {
            val sessionUuid = ensureUuid(session.id)
            val jsonArray = JSONArray().apply {
                put(sessionToJsonObject(userId, session.copy(id = sessionUuid, userId = userId)))
            }

            val url = "${SupabaseConfig.restEndpoint}focus_sessions?on_conflict=id"
            val request = Request.Builder()
                .url(url)
                .post(jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || code == 200 || code == 201) {
                Log.d(TAG, "Supabase sync success: focus_sessions")
                true
            } else {
                Log.e(TAG, "Supabase sync failure: focus_sessions - HTTP $code: $body")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync exception: focus_sessions", e)
            false
        }
    }

    fun syncAchievementDirectAsync(userId: String, achievement: Achievement) {
        scope.launch {
            syncAchievementDirect(userId, achievement)
        }
    }

    suspend fun syncAchievementDirect(userId: String, achievement: Achievement): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || userId.isBlank() || !achievement.isUnlocked) return@withContext false
        try {
            val achUuid = UUID.nameUUIDFromBytes("${userId}_${achievement.id}".toByteArray()).toString()
            val jsonArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", achUuid)
                    put("user_id", userId)
                    put("achievement_id", achievement.id)
                    put("unlocked_at", millisToIso(achievement.unlockedAt ?: System.currentTimeMillis()))
                    put("created_at", millisToIso(achievement.unlockedAt ?: System.currentTimeMillis()))
                })
            }

            val url = "${SupabaseConfig.restEndpoint}achievements?on_conflict=user_id,achievement_id"
            val request = Request.Builder()
                .url(url)
                .post(jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val code = response.code
            val body = response.body?.string() ?: ""

            if (response.isSuccessful || code == 200 || code == 201) {
                Log.d(TAG, "Supabase sync success: achievements")
                true
            } else {
                Log.e(TAG, "Supabase sync failure: achievements - HTTP $code: $body")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync exception: achievements", e)
            false
        }
    }

    // =========================================================================
    // 2. PROFILE & AVATAR SYNC
    // =========================================================================
    private suspend fun syncProfileAndAvatar(userId: String) {
        try {
            val localUserEntity = userDao.getUserById(userId)
            val remoteProfile = SupabaseAuthService.fetchRemoteProfile(userId)

            if (remoteProfile != null) {
                if (localUserEntity == null || remoteProfile.updatedAt > localUserEntity.updatedAt) {
                    var finalPhotoUrl = remoteProfile.photoURL

                    if (!finalPhotoUrl.isNullOrBlank() && finalPhotoUrl.contains("avatars/")) {
                        val localAvatar = downloadAvatar(userId, finalPhotoUrl)
                        if (localAvatar != null && localAvatar.exists()) {
                            finalPhotoUrl = Uri.fromFile(localAvatar).toString()
                        }
                    }

                    val updatedLocal = remoteProfile.copy(photoURL = finalPhotoUrl)
                    userDao.insertUser(UserEntity.fromUserProfile(updatedLocal, passwordHash = ""))
                } else if (localUserEntity != null && localUserEntity.updatedAt > remoteProfile.updatedAt) {
                    SupabaseAuthService.upsertRemoteProfile(localUserEntity.toUserProfile())
                }
            } else if (localUserEntity != null) {
                SupabaseAuthService.upsertRemoteProfile(localUserEntity.toUserProfile())
            }
            Log.d(TAG, "Supabase sync success: profiles")
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sync failure: profiles", e)
        }
    }

    suspend fun uploadAvatar(userId: String, avatarFile: File): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured || !avatarFile.exists()) {
            return@withContext Result.failure(Exception("Supabase not configured or avatar file missing"))
        }

        try {
            val strictPath = AvatarOptimizer.getStrictAvatarPath(userId) // <userId>/avatar.jpg
            val uploadUrl = "${SupabaseConfig.storageEndpoint}object/avatars/$strictPath"

            val fileBytes = avatarFile.readBytes()
            val requestBody = fileBytes.toRequestBody(JPEG_MEDIA_TYPE)

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .header("x-upsert", "true")
                .header("Content-Type", "image/jpeg")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 200 || response.code == 201) {
                val publicUrl = "${SupabaseConfig.storageEndpoint}object/public/avatars/$strictPath"

                val current = authRepository.currentUser.value
                if (current != null) {
                    val updated = current.copy(photoURL = publicUrl, updatedAt = System.currentTimeMillis())
                    SupabaseAuthService.upsertRemoteProfile(updated)
                }

                Log.d(TAG, "Supabase sync success: avatar upload")
                Result.success(publicUrl)
            } else {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Supabase sync failure: avatar upload - HTTP ${response.code}: $errBody")
                Result.failure(Exception("Failed to upload avatar (${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Avatar upload exception", e)
            Result.failure(e)
        }
    }

    fun downloadAvatar(userId: String, remoteUrl: String): File? {
        return try {
            val url = if (remoteUrl.startsWith("http")) {
                remoteUrl
            } else {
                val strictPath = AvatarOptimizer.getStrictAvatarPath(userId)
                "${SupabaseConfig.storageEndpoint}object/public/avatars/$strictPath"
            }

            val request = Request.Builder().url(url).get().build()
            val response = SupabaseClient.httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val avatarsDir = File(context.filesDir, "profile_avatars").apply { mkdirs() }
                    val localFile = File(avatarsDir, "avatar_${userId}.jpg")
                    FileOutputStream(localFile).use { it.write(bytes) }
                    return localFile
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading avatar", e)
            null
        }
    }

    // =========================================================================
    // 3. TASKS BATCH SYNC
    // =========================================================================
    private suspend fun syncTasks(userId: String) {
        try {
            val url = "${SupabaseConfig.restEndpoint}tasks?user_id=eq.$userId&select=*"
            val request = Request.Builder().url(url).get().build()
            val response = SupabaseClient.httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val remoteTasks = mutableListOf<RemoteTaskDto>()
            if (response.isSuccessful && responseBody.isNotBlank()) {
                val array = JSONArray(responseBody)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    remoteTasks.add(parseRemoteTask(obj))
                }
            }

            val localTasks = getLocalTasks(userId).toMutableList()
            val deletedIds = getDeletedIds(userId, "tasks")

            val mergedTasksMap = mutableMapOf<String, Task>()
            val toUpload = mutableListOf<Task>()

            localTasks.forEach { t ->
                val canonicalId = ensureUuid(t.id)
                mergedTasksMap[canonicalId] = t.copy(id = canonicalId, userId = userId)
            }

            remoteTasks.forEach { r ->
                val id = r.id
                if (r.deletedAt != null || deletedIds.contains(id)) {
                    mergedTasksMap.remove(id)
                } else {
                    val existingLocal = mergedTasksMap[id]
                    if (existingLocal == null) {
                        mergedTasksMap[id] = r.toDomainTask(userId)
                    } else {
                        if (r.updatedAt > existingLocal.updatedAt) {
                            mergedTasksMap[id] = r.toDomainTask(userId).copy(
                                notificationId = existingLocal.notificationId,
                                notificationIds = existingLocal.notificationIds
                            )
                        } else if (existingLocal.updatedAt > r.updatedAt) {
                            toUpload.add(existingLocal)
                        }
                    }
                }
            }

            val remoteIds = remoteTasks.map { it.id }.toSet()
            mergedTasksMap.values.forEach { t ->
                if (!remoteIds.contains(t.id) && !deletedIds.contains(t.id)) {
                    toUpload.add(t)
                }
            }

            val mergedList = mergedTasksMap.values.toList().sortedByDescending { it.createdAt }
            saveLocalTasks(userId, mergedList)

            if (toUpload.isNotEmpty()) {
                uploadTasksBatch(userId, toUpload.distinctBy { it.id })
            }

            if (deletedIds.isNotEmpty()) {
                syncDeletedRecords(userId, "tasks", deletedIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync tasks", e)
        }
    }

    private fun uploadTasksBatch(userId: String, tasks: List<Task>) {
        try {
            val array = JSONArray()
            tasks.forEach { t ->
                array.put(taskToJsonObject(userId, t))
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.restEndpoint}tasks?on_conflict=id")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Supabase sync success: tasks")
            } else {
                Log.e(TAG, "Supabase sync failure: tasks batch - HTTP ${response.code}: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload tasks batch", e)
        }
    }

    // =========================================================================
    // 4. HABITS & HABIT LOGS BATCH SYNC
    // =========================================================================
    private suspend fun syncHabitsAndLogs(userId: String) {
        try {
            val habitsUrl = "${SupabaseConfig.restEndpoint}habits?user_id=eq.$userId&select=*"
            val habitsReq = Request.Builder().url(habitsUrl).get().build()
            val habitsResp = SupabaseClient.httpClient.newCall(habitsReq).execute()
            val habitsBody = habitsResp.body?.string() ?: ""

            val remoteHabits = mutableListOf<RemoteHabitDto>()
            if (habitsResp.isSuccessful && habitsBody.isNotBlank()) {
                val array = JSONArray(habitsBody)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    remoteHabits.add(parseRemoteHabit(obj))
                }
            }

            val logsUrl = "${SupabaseConfig.restEndpoint}habit_logs?user_id=eq.$userId&select=*"
            val logsReq = Request.Builder().url(logsUrl).get().build()
            val logsResp = SupabaseClient.httpClient.newCall(logsReq).execute()
            val logsBody = logsResp.body?.string() ?: ""

            val remoteLogsMap = mutableMapOf<String, MutableSet<String>>()
            if (logsResp.isSuccessful && logsBody.isNotBlank()) {
                val array = JSONArray(logsBody)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val habitId = obj.optString("habit_id", "")
                    val date = obj.optString("date", "")
                    if (habitId.isNotBlank() && date.isNotBlank()) {
                        remoteLogsMap.getOrPut(habitId) { mutableSetOf() }.add(date)
                    }
                }
            }

            val localHabits = getLocalHabits(userId).toMutableList()
            val deletedIds = getDeletedIds(userId, "habits")

            val mergedHabitsMap = mutableMapOf<String, Habit>()
            val habitsToUpload = mutableListOf<Habit>()
            val logsToUpload = mutableListOf<Pair<String, String>>()

            localHabits.forEach { h ->
                val canonicalId = ensureUuid(h.id)
                mergedHabitsMap[canonicalId] = h.copy(id = canonicalId, userId = userId)
            }

            remoteHabits.forEach { r ->
                val id = r.id
                if (r.deletedAt != null || deletedIds.contains(id)) {
                    mergedHabitsMap.remove(id)
                } else {
                    val local = mergedHabitsMap[id]
                    val remoteHistory = remoteLogsMap[id] ?: emptySet()

                    if (local == null) {
                        val combinedHistory = remoteHistory.toList()
                        mergedHabitsMap[id] = r.toDomainHabit(userId, combinedHistory)
                    } else {
                        val combinedHistory = (local.completionHistory.toSet() + remoteHistory).toList()
                        val (streak, longest) = calculateStreaks(combinedHistory)

                        val chosen = if (r.updatedAt > local.updatedAt) {
                            r.toDomainHabit(userId, combinedHistory).copy(
                                currentStreak = streak,
                                longestStreak = maxOf(local.longestStreak, longest)
                            )
                        } else {
                            if (local.updatedAt > r.updatedAt) {
                                habitsToUpload.add(local)
                            }
                            local.copy(
                                completionHistory = combinedHistory,
                                currentStreak = streak,
                                longestStreak = maxOf(local.longestStreak, longest)
                            )
                        }
                        mergedHabitsMap[id] = chosen

                        val missingOnRemote = local.completionHistory.toSet() - remoteHistory
                        missingOnRemote.forEach { d -> logsToUpload.add(id to d) }
                    }
                }
            }

            val remoteHabitIds = remoteHabits.map { it.id }.toSet()
            mergedHabitsMap.values.forEach { h ->
                if (!remoteHabitIds.contains(h.id) && !deletedIds.contains(h.id)) {
                    habitsToUpload.add(h)
                    h.completionHistory.forEach { d -> logsToUpload.add(h.id to d) }
                }
            }

            val mergedList = mergedHabitsMap.values.toList().sortedByDescending { it.createdAt }
            saveLocalHabits(userId, mergedList)

            if (habitsToUpload.isNotEmpty()) {
                uploadHabitsBatch(userId, habitsToUpload.distinctBy { it.id })
            }

            if (logsToUpload.isNotEmpty()) {
                uploadHabitLogsBatch(userId, logsToUpload.distinct())
            }

            if (deletedIds.isNotEmpty()) {
                syncDeletedRecords(userId, "habits", deletedIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habits and logs", e)
        }
    }

    private fun uploadHabitsBatch(userId: String, habits: List<Habit>) {
        try {
            val array = JSONArray()
            habits.forEach { h ->
                array.put(habitToJsonObject(userId, h))
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.restEndpoint}habits?on_conflict=id")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Supabase sync success: habits")
            } else {
                Log.e(TAG, "Supabase sync failure: habits batch - HTTP ${response.code}: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload habits batch", e)
        }
    }

    private fun uploadHabitLogsBatch(userId: String, logs: List<Pair<String, String>>) {
        try {
            val array = JSONArray()
            logs.forEach { (habitId, date) ->
                val logId = UUID.nameUUIDFromBytes("${habitId}_$date".toByteArray()).toString()
                array.put(JSONObject().apply {
                    put("id", logId)
                    put("habit_id", ensureUuid(habitId))
                    put("user_id", userId)
                    put("date", date)
                    put("completed", true)
                    put("created_at", millisToIso(System.currentTimeMillis()))
                    put("updated_at", millisToIso(System.currentTimeMillis()))
                })
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.restEndpoint}habit_logs?on_conflict=habit_id,date")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Supabase sync success: habit_logs")
            } else {
                Log.e(TAG, "Supabase sync failure: habit_logs batch - HTTP ${response.code}: ${response.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload habit logs batch", e)
        }
    }

    // =========================================================================
    // 5. GOALS & MILESTONES BATCH SYNC
    // =========================================================================
    private suspend fun syncGoalsAndMilestones(userId: String) {
        try {
            val goalsUrl = "${SupabaseConfig.restEndpoint}goals?user_id=eq.$userId&select=*"
            val goalsReq = Request.Builder().url(goalsUrl).get().build()
            val goalsResp = SupabaseClient.httpClient.newCall(goalsReq).execute()
            val goalsBody = goalsResp.body?.string() ?: ""

            val remoteGoals = mutableListOf<RemoteGoalDto>()
            if (goalsResp.isSuccessful && goalsBody.isNotBlank()) {
                val array = JSONArray(goalsBody)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    remoteGoals.add(parseRemoteGoal(obj))
                }
            }

            val msUrl = "${SupabaseConfig.restEndpoint}milestones?user_id=eq.$userId&select=*"
            val msReq = Request.Builder().url(msUrl).get().build()
            val msResp = SupabaseClient.httpClient.newCall(msReq).execute()
            val msBody = msResp.body?.string() ?: ""

            val remoteMsMap = mutableMapOf<String, MutableList<Milestone>>()
            if (msResp.isSuccessful && msBody.isNotBlank()) {
                val array = JSONArray(msBody)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val goalId = obj.optString("goal_id", "")
                    val msId = obj.optString("id", "")
                    val title = obj.optString("title", "")
                    val completed = obj.optBoolean("completed", false)
                    if (goalId.isNotBlank() && msId.isNotBlank()) {
                        remoteMsMap.getOrPut(goalId) { mutableListOf() }
                            .add(Milestone(id = msId, title = title, completed = completed))
                    }
                }
            }

            val localGoals = getLocalGoals(userId).toMutableList()
            val deletedIds = getDeletedIds(userId, "goals")

            val mergedGoalsMap = mutableMapOf<String, Goal>()
            val goalsToUpload = mutableListOf<Goal>()

            localGoals.forEach { g ->
                val canonicalId = ensureUuid(g.id)
                mergedGoalsMap[canonicalId] = g.copy(id = canonicalId, userId = userId)
            }

            remoteGoals.forEach { r ->
                val id = r.id
                if (r.deletedAt != null || deletedIds.contains(id)) {
                    mergedGoalsMap.remove(id)
                } else {
                    val local = mergedGoalsMap[id]
                    val remoteMilestones = remoteMsMap[id] ?: emptyList()

                    if (local == null) {
                        mergedGoalsMap[id] = r.toDomainGoal(userId, remoteMilestones)
                    } else {
                        val mergedMilestones = if (remoteMilestones.isNotEmpty()) remoteMilestones else local.milestones
                        val chosen = if (r.updatedAt > local.updatedAt) {
                            r.toDomainGoal(userId, mergedMilestones)
                        } else {
                            if (local.updatedAt > r.updatedAt) {
                                goalsToUpload.add(local)
                            }
                            local.copy(milestones = mergedMilestones)
                        }
                        mergedGoalsMap[id] = chosen
                    }
                }
            }

            val remoteGoalIds = remoteGoals.map { it.id }.toSet()
            mergedGoalsMap.values.forEach { g ->
                if (!remoteGoalIds.contains(g.id) && !deletedIds.contains(g.id)) {
                    goalsToUpload.add(g)
                }
            }

            val mergedList = mergedGoalsMap.values.toList().sortedByDescending { it.createdAt }
            saveLocalGoals(userId, mergedList)

            if (goalsToUpload.isNotEmpty()) {
                uploadGoalsBatch(userId, goalsToUpload.distinctBy { it.id })
            }

            if (deletedIds.isNotEmpty()) {
                syncDeletedRecords(userId, "goals", deletedIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync goals & milestones", e)
        }
    }

    private fun uploadGoalsBatch(userId: String, goals: List<Goal>) {
        try {
            val goalsArray = JSONArray()
            val msArray = JSONArray()

            goals.forEach { g ->
                val goalUuid = ensureUuid(g.id)
                goalsArray.put(goalToJsonObject(userId, g))

                g.milestones.forEach { m ->
                    val msUuid = ensureUuid(m.id)
                    msArray.put(JSONObject().apply {
                        put("id", msUuid)
                        put("goal_id", goalUuid)
                        put("user_id", userId)
                        put("title", m.title)
                        put("completed", m.completed)
                        put("created_at", millisToIso(g.createdAt))
                        put("updated_at", millisToIso(g.updatedAt))
                    })
                }
            }

            val req = Request.Builder()
                .url("${SupabaseConfig.restEndpoint}goals?on_conflict=id")
                .post(goalsArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()
            val resp = SupabaseClient.httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                Log.d(TAG, "Supabase sync success: goals")
            } else {
                Log.e(TAG, "Supabase sync failure: goals batch - HTTP ${resp.code}: ${resp.body?.string()}")
            }

            if (msArray.length() > 0) {
                val msReq = Request.Builder()
                    .url("${SupabaseConfig.restEndpoint}milestones?on_conflict=id")
                    .post(msArray.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .header("Prefer", "resolution=merge-duplicates,return=representation")
                    .build()
                val msResp = SupabaseClient.httpClient.newCall(msReq).execute()
                if (msResp.isSuccessful) {
                    Log.d(TAG, "Supabase sync success: milestones")
                } else {
                    Log.e(TAG, "Supabase sync failure: milestones batch - HTTP ${msResp.code}: ${msResp.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload goals batch", e)
        }
    }

    // =========================================================================
    // 6. FOCUS SESSIONS BATCH SYNC
    // =========================================================================
    private suspend fun syncFocusSessions(userId: String) {
        try {
            val url = "${SupabaseConfig.restEndpoint}focus_sessions?user_id=eq.$userId&select=*"
            val req = Request.Builder().url(url).get().build()
            val resp = SupabaseClient.httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: ""

            val remoteSessions = mutableListOf<FocusSession>()
            if (resp.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    remoteSessions.add(parseRemoteFocusSession(obj, userId))
                }
            }

            val localSessions = getLocalSessions(userId)
            val mergedSessionsMap = mutableMapOf<String, FocusSession>()
            val toUpload = mutableListOf<FocusSession>()

            localSessions.forEach { s ->
                val canonicalId = ensureUuid(s.id)
                mergedSessionsMap[canonicalId] = s.copy(id = canonicalId, userId = userId)
            }

            remoteSessions.forEach { r ->
                if (!mergedSessionsMap.containsKey(r.id)) {
                    mergedSessionsMap[r.id] = r
                }
            }

            val remoteIds = remoteSessions.map { it.id }.toSet()
            mergedSessionsMap.values.forEach { s ->
                if (!remoteIds.contains(s.id)) {
                    toUpload.add(s)
                }
            }

            val mergedList = mergedSessionsMap.values.toList().sortedByDescending { it.startedAt }
            saveLocalSessions(userId, mergedList)

            if (toUpload.isNotEmpty()) {
                uploadSessionsBatch(userId, toUpload.distinctBy { it.id })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync focus sessions", e)
        }
    }

    private fun uploadSessionsBatch(userId: String, sessions: List<FocusSession>) {
        try {
            val array = JSONArray()
            sessions.forEach { s ->
                array.put(sessionToJsonObject(userId, s))
            }

            val req = Request.Builder()
                .url("${SupabaseConfig.restEndpoint}focus_sessions?on_conflict=id")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val resp = SupabaseClient.httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                Log.d(TAG, "Supabase sync success: focus_sessions")
            } else {
                Log.e(TAG, "Supabase sync failure: focus_sessions batch - HTTP ${resp.code}: ${resp.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload sessions batch", e)
        }
    }

    // =========================================================================
    // 7. ACHIEVEMENTS BATCH SYNC
    // =========================================================================
    private suspend fun syncAchievements(userId: String) {
        try {
            val url = "${SupabaseConfig.restEndpoint}achievements?user_id=eq.$userId&select=*"
            val req = Request.Builder().url(url).get().build()
            val resp = SupabaseClient.httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: ""

            val remoteUnlockedMap = mutableMapOf<String, Long>()
            if (resp.isSuccessful && body.isNotBlank()) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val achId = obj.optString("achievement_id", "")
                    val unlockedAt = isoToMillis(obj.optString("unlocked_at", ""))
                    if (achId.isNotBlank()) {
                        remoteUnlockedMap[achId] = unlockedAt
                    }
                }
            }

            val localAchievements = getLocalAchievements(userId)
            val mergedAchievements = localAchievements.map { ach ->
                val remoteUnlockedAt = remoteUnlockedMap[ach.id]
                if (remoteUnlockedAt != null && remoteUnlockedAt > 0) {
                    ach.copy(isUnlocked = true, unlockedAt = remoteUnlockedAt)
                } else if (ach.isUnlocked) {
                    ach
                } else {
                    ach
                }
            }

            saveLocalAchievements(userId, mergedAchievements)

            val toUpload = mergedAchievements.filter { it.isUnlocked && !remoteUnlockedMap.containsKey(it.id) }
            if (toUpload.isNotEmpty()) {
                uploadAchievementsBatch(userId, toUpload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync achievements", e)
        }
    }

    private fun uploadAchievementsBatch(userId: String, achievements: List<Achievement>) {
        try {
            val array = JSONArray()
            achievements.forEach { ach ->
                val achUuid = UUID.nameUUIDFromBytes("${userId}_${ach.id}".toByteArray()).toString()
                array.put(JSONObject().apply {
                    put("id", achUuid)
                    put("user_id", userId)
                    put("achievement_id", ach.id)
                    put("unlocked_at", millisToIso(ach.unlockedAt ?: System.currentTimeMillis()))
                    put("created_at", millisToIso(ach.unlockedAt ?: System.currentTimeMillis()))
                })
            }

            val req = Request.Builder()
                .url("${SupabaseConfig.restEndpoint}achievements?on_conflict=user_id,achievement_id")
                .post(array.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .build()

            val resp = SupabaseClient.httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                Log.d(TAG, "Supabase sync success: achievements")
            } else {
                Log.e(TAG, "Supabase sync failure: achievements batch - HTTP ${resp.code}: ${resp.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload achievements batch", e)
        }
    }

    // =========================================================================
    // 8. SOFT-DELETE HANDLING
    // =========================================================================
    fun markRecordDeleted(userId: String, table: String, id: String) {
        if (!SupabaseConfig.isConfigured || userId.isBlank()) return
        ensureAuthToken()
        val canonicalId = ensureUuid(id)
        val currentDeleted = getDeletedIds(userId, table).toMutableSet()
        currentDeleted.add(canonicalId)
        saveDeletedIds(userId, table, currentDeleted)

        scope.launch {
            ensureAuthToken()
            syncDeletedRecords(userId, table, setOf(canonicalId))
        }
    }

    private fun syncDeletedRecords(userId: String, table: String, ids: Set<String>) {
        if (!SupabaseConfig.isConfigured || userId.isBlank() || ids.isEmpty()) return
        ensureAuthToken()
        try {
            ids.forEach { id ->
                val patchObj = JSONObject().apply {
                    put("deleted_at", millisToIso(System.currentTimeMillis()))
                    put("updated_at", millisToIso(System.currentTimeMillis()))
                }
                val url = "${SupabaseConfig.restEndpoint}$table?id=eq.$id&user_id=eq.$userId"
                val req = Request.Builder()
                    .url(url)
                    .patch(patchObj.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .header("Prefer", "return=representation")
                    .build()
                val resp = SupabaseClient.httpClient.newCall(req).execute()
                val code = resp.code
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful || code == 200 || code == 204) {
                    Log.d(TAG, "Supabase sync success: $table deletion (id=$id)")
                } else {
                    Log.e(TAG, "Supabase sync failure: $table deletion (id=$id) - HTTP $code: $body")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync deleted records for table $table", e)
        }
    }

    private fun getDeletedIds(userId: String, table: String): Set<String> {
        return prefs.getStringSet("deleted_${table}_$userId", emptySet()) ?: emptySet()
    }

    private fun saveDeletedIds(userId: String, table: String, ids: Set<String>) {
        prefs.edit().putStringSet("deleted_${table}_$userId", ids).apply()
    }

    // =========================================================================
    // 9. LOCAL STORAGE HELPERS
    // =========================================================================
    fun getLocalTasks(userId: String): List<Task> {
        val json = dataPrefs.getString("tasks_$userId", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Task>()
            for (i in 0 until array.length()) {
                list.add(jsonToTask(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLocalTasks(userId: String, list: List<Task>) {
        try {
            val array = JSONArray()
            list.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", ensureUuid(task.id))
                    put("userId", userId)
                    put("title", task.title)
                    put("description", task.description)
                    put("category", task.category)
                    put("priority", task.priority)
                    put("dueDate", task.dueDate)
                    put("dueTime", task.dueTime)
                    put("reminderEnabled", task.reminderEnabled)
                    put("reminderRepeat", task.reminderRepeat)
                    val rDays = JSONArray()
                    task.reminderDays.forEach { rDays.put(it) }
                    put("reminderDays", rDays)
                    if (task.notificationId != null) put("notificationId", task.notificationId)
                    val nIds = JSONArray()
                    task.notificationIds.forEach { nIds.put(it) }
                    put("notificationIds", nIds)
                    put("createdAt", task.createdAt)
                    put("updatedAt", task.updatedAt)
                    put("completed", task.completed)
                    put("completedAt", task.completedAt ?: 0L)
                    val subs = JSONArray()
                    task.subtasks.forEach { sub ->
                        subs.put(JSONObject().apply {
                            put("id", sub.id)
                            put("title", sub.title)
                            put("completed", sub.completed)
                        })
                    }
                    put("subtasks", subs)
                }
                array.put(obj)
            }
            dataPrefs.edit().putString("tasks_$userId", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local tasks", e)
        }
    }

    fun getLocalHabits(userId: String): List<Habit> {
        val json = dataPrefs.getString("habits_$userId", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Habit>()
            for (i in 0 until array.length()) {
                list.add(jsonToHabit(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLocalHabits(userId: String, list: List<Habit>) {
        try {
            val array = JSONArray()
            list.forEach { habit ->
                val obj = JSONObject().apply {
                    put("id", ensureUuid(habit.id))
                    put("userId", userId)
                    put("name", habit.name)
                    put("description", habit.description)
                    put("frequency", habit.frequency)
                    put("category", habit.category)
                    put("createdAt", habit.createdAt)
                    put("updatedAt", habit.updatedAt)
                    put("currentStreak", habit.currentStreak)
                    put("longestStreak", habit.longestStreak)
                    val history = JSONArray()
                    habit.completionHistory.forEach { history.put(it) }
                    put("completionHistory", history)
                }
                array.put(obj)
            }
            dataPrefs.edit().putString("habits_$userId", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local habits", e)
        }
    }

    fun getLocalGoals(userId: String): List<Goal> {
        val json = dataPrefs.getString("goals_$userId", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Goal>()
            for (i in 0 until array.length()) {
                list.add(jsonToGoal(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLocalGoals(userId: String, list: List<Goal>) {
        try {
            val array = JSONArray()
            list.forEach { goal ->
                val obj = JSONObject().apply {
                    put("id", ensureUuid(goal.id))
                    put("userId", userId)
                    put("title", goal.title)
                    put("description", goal.description)
                    put("deadline", goal.deadline)
                    put("createdAt", goal.createdAt)
                    put("updatedAt", goal.updatedAt)
                    put("completedAt", goal.completedAt ?: 0L)
                    val milestones = JSONArray()
                    goal.milestones.forEach { m ->
                        milestones.put(JSONObject().apply {
                            put("id", ensureUuid(m.id))
                            put("title", m.title)
                            put("completed", m.completed)
                        })
                    }
                    put("milestones", milestones)
                }
                array.put(obj)
            }
            dataPrefs.edit().putString("goals_$userId", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local goals", e)
        }
    }

    fun getLocalSessions(userId: String): List<FocusSession> {
        val json = dataPrefs.getString("sessions_$userId", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<FocusSession>()
            for (i in 0 until array.length()) {
                list.add(jsonToSession(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLocalSessions(userId: String, list: List<FocusSession>) {
        try {
            val array = JSONArray()
            list.forEach { session ->
                val obj = JSONObject().apply {
                    put("id", ensureUuid(session.id))
                    put("userId", userId)
                    put("type", session.type)
                    put("durationMinutes", session.durationMinutes)
                    put("startedAt", session.startedAt)
                    put("completedAt", session.completedAt)
                    put("createdAt", session.createdAt)
                    put("category", session.category)
                    put("scene", session.scene)
                }
                array.put(obj)
            }
            dataPrefs.edit().putString("sessions_$userId", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local sessions", e)
        }
    }

    fun getLocalAchievements(userId: String): List<Achievement> {
        val json = dataPrefs.getString("achievements_$userId", null)
        if (json == null) return Achievement.DEFINED_ACHIEVEMENTS
        return try {
            val array = JSONArray(json)
            val map = mutableMapOf<String, Long?>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val isUnlocked = obj.optBoolean("isUnlocked", false)
                val unlockedAt = if (obj.has("unlockedAt")) obj.optLong("unlockedAt") else null
                if (isUnlocked) map[id] = unlockedAt
            }
            Achievement.DEFINED_ACHIEVEMENTS.map { def ->
                if (map.containsKey(def.id)) {
                    def.copy(isUnlocked = true, unlockedAt = map[def.id])
                } else def
            }
        } catch (e: Exception) {
            Achievement.DEFINED_ACHIEVEMENTS
        }
    }

    fun saveLocalAchievements(userId: String, list: List<Achievement>) {
        try {
            val array = JSONArray()
            list.forEach { ach ->
                val obj = JSONObject().apply {
                    put("id", ach.id)
                    put("title", ach.title)
                    put("description", ach.description)
                    put("iconName", ach.iconName)
                    put("xpReward", ach.xpReward)
                    put("isUnlocked", ach.isUnlocked)
                    if (ach.unlockedAt != null) put("unlockedAt", ach.unlockedAt)
                }
                array.put(obj)
            }
            dataPrefs.edit().putString("achievements_$userId", array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local achievements", e)
        }
    }

    // =========================================================================
    // 10. OBJECT TO JSON HELPERS
    // =========================================================================
    private fun taskToJsonObject(userId: String, t: Task): JSONObject {
        val rDays = JSONArray()
        t.reminderDays.forEach { rDays.put(it) }

        return JSONObject().apply {
            put("id", ensureUuid(t.id))
            put("user_id", userId)
            put("title", t.title)
            put("description", t.description)
            put("date", t.dueDate)
            put("time", t.dueTime)
            put("reminder_enabled", t.reminderEnabled)
            put("reminder_repeat", t.reminderRepeat)
            put("reminder_days", rDays)
            put("category", t.category)
            put("completed", t.completed)
            put("created_at", millisToIso(t.createdAt))
            put("updated_at", millisToIso(t.updatedAt))
            put("deleted_at", JSONObject.NULL)
        }
    }

    private fun habitToJsonObject(userId: String, h: Habit): JSONObject {
        return JSONObject().apply {
            put("id", ensureUuid(h.id))
            put("user_id", userId)
            put("title", h.name)
            put("frequency", h.frequency)
            put("category", h.category)
            put("created_at", millisToIso(h.createdAt))
            put("updated_at", millisToIso(h.updatedAt))
            put("deleted_at", JSONObject.NULL)
        }
    }

    private fun goalToJsonObject(userId: String, g: Goal): JSONObject {
        return JSONObject().apply {
            put("id", ensureUuid(g.id))
            put("user_id", userId)
            put("title", g.title)
            put("description", g.description)
            put("deadline", g.deadline)
            put("progress", if (g.milestones.isEmpty()) 0.0 else (g.milestones.count { it.completed }.toDouble() / g.milestones.size))
            put("completed", g.isCompleted)
            put("created_at", millisToIso(g.createdAt))
            put("updated_at", millisToIso(g.updatedAt))
            put("deleted_at", JSONObject.NULL)
        }
    }

    private fun sessionToJsonObject(userId: String, s: FocusSession): JSONObject {
        return JSONObject().apply {
            put("id", ensureUuid(s.id))
            put("user_id", userId)
            put("duration_minutes", s.durationMinutes)
            put("started_at", millisToIso(s.startedAt))
            put("completed_at", millisToIso(s.completedAt))
            put("completed", true)
            put("category", s.category)
            put("scene", s.scene)
            put("created_at", millisToIso(s.createdAt))
            put("updated_at", millisToIso(s.completedAt))
        }
    }

    // =========================================================================
    // 11. DTO PARSING AND HELPERS
    // =========================================================================
    private fun parseRemoteTask(obj: JSONObject): RemoteTaskDto {
        val rDaysArray = obj.optJSONArray("reminder_days")
        val rDays = mutableListOf<Int>()
        if (rDaysArray != null) {
            for (i in 0 until rDaysArray.length()) {
                rDays.add(rDaysArray.getInt(i))
            }
        }
        return RemoteTaskDto(
            id = obj.getString("id"),
            userId = obj.getString("user_id"),
            title = obj.getString("title"),
            description = obj.optString("description", ""),
            date = obj.optString("date", ""),
            time = obj.optString("time", ""),
            reminderEnabled = obj.optBoolean("reminder_enabled", false),
            reminderRepeat = obj.optString("reminder_repeat", "Once"),
            reminderDays = rDays,
            category = obj.optString("category", "Study"),
            completed = obj.optBoolean("completed", false),
            createdAt = isoToMillis(obj.optString("created_at", "")),
            updatedAt = isoToMillis(obj.optString("updated_at", "")),
            deletedAt = if (obj.isNull("deleted_at") || obj.optString("deleted_at", "").isBlank()) null else isoToMillis(obj.optString("deleted_at", ""))
        )
    }

    private fun parseRemoteHabit(obj: JSONObject): RemoteHabitDto {
        return RemoteHabitDto(
            id = obj.getString("id"),
            userId = obj.getString("user_id"),
            title = obj.getString("title"),
            frequency = obj.optString("frequency", "Daily"),
            category = obj.optString("category", "Health"),
            createdAt = isoToMillis(obj.optString("created_at", "")),
            updatedAt = isoToMillis(obj.optString("updated_at", "")),
            deletedAt = if (obj.isNull("deleted_at") || obj.optString("deleted_at", "").isBlank()) null else isoToMillis(obj.optString("deleted_at", ""))
        )
    }

    private fun parseRemoteGoal(obj: JSONObject): RemoteGoalDto {
        return RemoteGoalDto(
            id = obj.getString("id"),
            userId = obj.getString("user_id"),
            title = obj.getString("title"),
            description = obj.optString("description", ""),
            deadline = obj.optString("deadline", ""),
            progress = obj.optDouble("progress", 0.0).toFloat(),
            completed = obj.optBoolean("completed", false),
            createdAt = isoToMillis(obj.optString("created_at", "")),
            updatedAt = isoToMillis(obj.optString("updated_at", "")),
            deletedAt = if (obj.isNull("deleted_at") || obj.optString("deleted_at", "").isBlank()) null else isoToMillis(obj.optString("deleted_at", ""))
        )
    }

    private fun parseRemoteFocusSession(obj: JSONObject, defaultUserId: String): FocusSession {
        val startedAt = isoToMillis(obj.optString("started_at", ""))
        val completedAt = isoToMillis(obj.optString("completed_at", ""))
        return FocusSession(
            id = obj.getString("id"),
            userId = obj.optString("user_id", defaultUserId),
            type = "focus",
            durationMinutes = obj.optInt("duration_minutes", 25),
            startedAt = if (startedAt > 0) startedAt else System.currentTimeMillis() - 25 * 60 * 1000,
            completedAt = if (completedAt > 0) completedAt else System.currentTimeMillis(),
            createdAt = isoToMillis(obj.optString("created_at", "")),
            category = obj.optString("category", "Study"),
            scene = obj.optString("scene", "Forest")
        )
    }

    private fun jsonToTask(obj: JSONObject): Task {
        val subArray = obj.optJSONArray("subtasks")
        val subList = mutableListOf<Subtask>()
        if (subArray != null) {
            for (i in 0 until subArray.length()) {
                val s = subArray.getJSONObject(i)
                subList.add(Subtask(s.getString("id"), s.getString("title"), s.getBoolean("completed")))
            }
        }
        val rDaysArray = obj.optJSONArray("reminderDays")
        val rDays = mutableListOf<Int>()
        if (rDaysArray != null) {
            for (i in 0 until rDaysArray.length()) {
                rDays.add(rDaysArray.getInt(i))
            }
        }
        val nIdsArray = obj.optJSONArray("notificationIds")
        val nIds = mutableListOf<Int>()
        if (nIdsArray != null) {
            for (i in 0 until nIdsArray.length()) {
                nIds.add(nIdsArray.getInt(i))
            }
        }
        val notifId = if (obj.has("notificationId")) obj.optInt("notificationId") else null
        return Task(
            id = ensureUuid(obj.getString("id")),
            userId = obj.optString("userId", ""),
            title = obj.getString("title"),
            description = obj.optString("description", ""),
            category = obj.optString("category", "Study"),
            priority = obj.optString("priority", "Medium"),
            dueDate = obj.optString("dueDate", ""),
            dueTime = obj.optString("dueTime", ""),
            reminderEnabled = obj.optBoolean("reminderEnabled", false),
            reminderRepeat = obj.optString("reminderRepeat", "Once"),
            reminderDays = rDays,
            notificationId = notifId,
            notificationIds = nIds,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            completed = obj.optBoolean("completed", false),
            completedAt = if (obj.has("completedAt") && obj.getLong("completedAt") > 0) obj.getLong("completedAt") else null,
            subtasks = subList
        )
    }

    private fun jsonToHabit(obj: JSONObject): Habit {
        val hArray = obj.optJSONArray("completionHistory")
        val history = mutableListOf<String>()
        if (hArray != null) {
            for (i in 0 until hArray.length()) {
                history.add(hArray.getString(i))
            }
        }
        return Habit(
            id = ensureUuid(obj.getString("id")),
            userId = obj.optString("userId", ""),
            name = obj.getString("name"),
            description = obj.optString("description", ""),
            frequency = obj.optString("frequency", "Daily"),
            category = obj.optString("category", "Health"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            completionHistory = history,
            currentStreak = obj.optInt("currentStreak", 0),
            longestStreak = obj.optInt("longestStreak", 0)
        )
    }

    private fun jsonToGoal(obj: JSONObject): Goal {
        val mArray = obj.optJSONArray("milestones")
        val mList = mutableListOf<Milestone>()
        if (mArray != null) {
            for (i in 0 until mArray.length()) {
                val s = mArray.getJSONObject(i)
                mList.add(Milestone(ensureUuid(s.getString("id")), s.getString("title"), s.getBoolean("completed")))
            }
        }
        return Goal(
            id = ensureUuid(obj.getString("id")),
            userId = obj.optString("userId", ""),
            title = obj.getString("title"),
            description = obj.optString("description", ""),
            deadline = obj.optString("deadline", ""),
            milestones = mList,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            completedAt = if (obj.has("completedAt") && obj.getLong("completedAt") > 0) obj.getLong("completedAt") else null
        )
    }

    private fun jsonToSession(obj: JSONObject): FocusSession {
        return FocusSession(
            id = ensureUuid(obj.getString("id")),
            userId = obj.optString("userId", ""),
            type = "focus",
            durationMinutes = obj.optInt("durationMinutes", 25),
            startedAt = obj.optLong("startedAt", System.currentTimeMillis()),
            completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            category = obj.optString("category", "Study"),
            scene = obj.optString("scene", "Forest")
        )
    }

    private fun calculateStreaks(dates: List<String>): Pair<Int, Int> {
        if (dates.isEmpty()) return 0 to 0
        val sorted = dates.distinct().sorted()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var maxStreak = 1
        var currStreak = 1

        var prevCal: Calendar? = null

        for (dStr in sorted) {
            try {
                val date = sdf.parse(dStr) ?: continue
                val cal = Calendar.getInstance().apply { time = date }
                if (prevCal != null) {
                    val diffDays = (cal.timeInMillis - prevCal.timeInMillis) / (24 * 60 * 60 * 1000)
                    if (diffDays == 1L) {
                        currStreak++
                        if (currStreak > maxStreak) maxStreak = currStreak
                    } else if (diffDays > 1L) {
                        currStreak = 1
                    }
                }
                prevCal = cal
            } catch (e: Exception) {
                // Ignore
            }
        }
        return currStreak to maxStreak
    }

    // =========================================================================
    // 12. UUID AND TIMESTAMP HELPERS
    // =========================================================================
    fun ensureUuid(input: String): String {
        if (input.isBlank()) return UUID.randomUUID().toString()
        return try {
            UUID.fromString(input)
            input
        } catch (e: Exception) {
            UUID.nameUUIDFromBytes(input.toByteArray()).toString()
        }
    }

    fun millisToIso(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    fun isoToMillis(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(iso)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Try next
            }
        }
        return System.currentTimeMillis()
    }
}

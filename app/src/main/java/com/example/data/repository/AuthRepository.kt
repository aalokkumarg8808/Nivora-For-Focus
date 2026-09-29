package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.PlantDatabase
import com.example.data.local.entity.UserEntity
import com.example.data.model.UserProfile
import com.example.data.remote.supabase.SupabaseAuthService
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("plant_auth_prefs", Context.MODE_PRIVATE)

    private val database = PlantDatabase.getInstance(context)
    private val userDao = database.userDao()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        // Check saved session on app launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val savedUid = prefs.getString("active_user_uid", null)
                val savedToken = prefs.getString("supabase_access_token", null)

                if (!savedToken.isNullOrBlank()) {
                    SupabaseClient.setAuthToken(savedToken)
                }

                if (!savedUid.isNullOrBlank()) {
                    val localUser = userDao.getUserById(savedUid)
                    if (localUser != null) {
                        _currentUser.value = localUser.toUserProfile()
                    } else if (SupabaseConfig.isConfigured) {
                        // If SQLite cache was cleared, fetch profile from Supabase PostgreSQL
                        val remoteProfile = SupabaseAuthService.fetchRemoteProfile(savedUid)
                        if (remoteProfile != null) {
                            userDao.insertUser(UserEntity.fromUserProfile(remoteProfile, passwordHash = ""))
                            _currentUser.value = remoteProfile
                        } else {
                            clearSession()
                            _currentUser.value = null
                        }
                    } else {
                        clearSession()
                        _currentUser.value = null
                    }
                } else {
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring session", e)
                _currentUser.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authError.value = null
        _isLoading.value = true

        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        if (trimmedName.isBlank()) {
            val err = "Name cannot be empty."
            _authError.value = err
            _isLoading.value = false
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (trimmedEmail.isBlank()) {
            val err = "Email cannot be empty."
            _authError.value = err
            _isLoading.value = false
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (trimmedPassword.isBlank() || trimmedPassword.length < 6) {
            val err = "Password must be at least 6 characters long."
            _authError.value = err
            _isLoading.value = false
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        try {
            if (SupabaseConfig.isConfigured) {
                // Authenticate through Supabase Auth
                val authResult = SupabaseAuthService.signUp(trimmedEmail, trimmedPassword, trimmedName)
                if (authResult.isFailure) {
                    val err = authResult.exceptionOrNull()?.localizedMessage ?: "Failed to create account."
                    _authError.value = err
                    _isLoading.value = false
                    return@withContext Result.failure(authResult.exceptionOrNull() ?: Exception(err))
                }

                val session = authResult.getOrThrow()
                val uid = session.userId

                // Store token in SupabaseClient and SharedPreferences
                if (!session.accessToken.isNullOrBlank()) {
                    SupabaseClient.setAuthToken(session.accessToken)
                    prefs.edit()
                        .putString("supabase_access_token", session.accessToken)
                        .putString("supabase_refresh_token", session.refreshToken ?: "")
                        .apply()
                }

                // Cache in SQLite Room (without storing raw password)
                val newEntity = UserEntity(
                    uid = uid,
                    name = session.name.ifBlank { trimmedName },
                    email = session.email.ifBlank { trimmedEmail },
                    passwordHash = "", // Supabase Auth handles authentication security
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(newEntity)

                // Save active session in SharedPreferences
                prefs.edit().putString("active_user_uid", uid).apply()

                val profile = newEntity.toUserProfile()
                _currentUser.value = profile
                _isLoading.value = false

                // Best-effort remote profile initial sync
                launch(Dispatchers.IO) {
                    SupabaseAuthService.upsertRemoteProfile(profile)
                }

                Result.success(profile)
            } else {
                // Fallback local mode if Supabase credentials are not yet configured
                val uid = UUID.randomUUID().toString()
                val newEntity = UserEntity(
                    uid = uid,
                    name = trimmedName,
                    email = trimmedEmail,
                    passwordHash = "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(newEntity)
                prefs.edit().putString("active_user_uid", uid).apply()
                val profile = newEntity.toUserProfile()
                _currentUser.value = profile
                _isLoading.value = false
                Result.success(profile)
            }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Failed to create account. Please try again."
            _authError.value = errMsg
            _isLoading.value = false
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        _authError.value = null
        _isLoading.value = true

        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isBlank()) {
            val err = "Email cannot be empty."
            _authError.value = err
            _isLoading.value = false
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (trimmedPassword.isBlank()) {
            val err = "Password cannot be empty."
            _authError.value = err
            _isLoading.value = false
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        try {
            if (SupabaseConfig.isConfigured) {
                // Authenticate through Supabase Auth
                val authResult = SupabaseAuthService.signInWithPassword(trimmedEmail, trimmedPassword)
                if (authResult.isFailure) {
                    val err = authResult.exceptionOrNull()?.localizedMessage ?: "Login failed. Please check your credentials."
                    _authError.value = err
                    _isLoading.value = false
                    return@withContext Result.failure(authResult.exceptionOrNull() ?: Exception(err))
                }

                val session = authResult.getOrThrow()
                val uid = session.userId

                // Save session tokens
                if (!session.accessToken.isNullOrBlank()) {
                    SupabaseClient.setAuthToken(session.accessToken)
                    prefs.edit()
                        .putString("supabase_access_token", session.accessToken)
                        .putString("supabase_refresh_token", session.refreshToken ?: "")
                        .apply()
                }

                prefs.edit().putString("active_user_uid", uid).apply()

                // Check local SQLite cache first
                var localUser = userDao.getUserById(uid)
                val profile: UserProfile

                if (localUser != null) {
                    profile = localUser.toUserProfile()
                    _currentUser.value = profile
                } else {
                    // Cache missed (fresh install or cleared app data): fetch from Supabase profiles table
                    val remoteProfile = SupabaseAuthService.fetchRemoteProfile(uid)
                    if (remoteProfile != null) {
                        profile = remoteProfile
                        val cachedEntity = UserEntity.fromUserProfile(remoteProfile, passwordHash = "")
                        userDao.insertUser(cachedEntity)
                    } else {
                        // If no remote profile row exists yet, initialize a local record
                        val displayName = session.name.ifBlank { trimmedEmail.substringBefore("@") }
                        val newEntity = UserEntity(
                            uid = uid,
                            name = displayName,
                            email = session.email.ifBlank { trimmedEmail },
                            passwordHash = "",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        userDao.insertUser(newEntity)
                        profile = newEntity.toUserProfile()
                    }
                    _currentUser.value = profile
                }

                _isLoading.value = false
                Result.success(profile)
            } else {
                // Fallback local mode
                val userEntity = userDao.getUserByEmail(trimmedEmail)
                if (userEntity == null) {
                    val err = "No account found with this email. Please check your email or create an account."
                    _authError.value = err
                    _isLoading.value = false
                    return@withContext Result.failure(IllegalArgumentException(err))
                }

                prefs.edit().putString("active_user_uid", userEntity.uid).apply()
                val profile = userEntity.toUserProfile()
                _currentUser.value = profile
                _isLoading.value = false
                Result.success(profile)
            }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: "Login failed. Please check your credentials."
            _authError.value = errMsg
            _isLoading.value = false
            Result.failure(e)
        }
    }

    fun getActiveUserId(): String? {
        return _currentUser.value?.uid ?: prefs.getString("active_user_uid", null)
    }

    fun getStoredAccessToken(): String? {
        return prefs.getString("supabase_access_token", null)
    }

    fun logout() {
        clearSession()
        _currentUser.value = null
        _authError.value = null
    }

    private fun clearSession() {
        SupabaseClient.setAuthToken(null)
        prefs.edit()
            .remove("active_user_uid")
            .remove("supabase_access_token")
            .remove("supabase_refresh_token")
            .apply()
    }

    suspend fun updateProfile(profile: UserProfile): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val existing = userDao.getUserById(profile.uid)
            val updatedEntity = if (existing != null) {
                existing.copy(
                    name = profile.name,
                    photoURL = profile.photoURL,
                    updatedAt = System.currentTimeMillis(),
                    xp = profile.xp,
                    level = profile.level,
                    currentStreak = profile.currentStreak,
                    longestStreak = profile.longestStreak,
                    totalFocusMinutes = profile.totalFocusMinutes,
                    completedTasks = profile.completedTasks,
                    completedHabits = profile.completedHabits,
                    completedGoals = profile.completedGoals,
                    themePreference = profile.themePreference,
                    reduceMotion = profile.reduceMotion,
                    focusReminders = profile.focusReminders,
                    taskReminders = profile.taskReminders,
                    habitReminders = profile.habitReminders,
                    focusSounds = profile.focusSounds,
                    completionSound = profile.completionSound
                )
            } else {
                UserEntity.fromUserProfile(profile, passwordHash = "")
            }
            userDao.insertUser(updatedEntity)
            val updatedProfile = updatedEntity.toUserProfile()
            _currentUser.value = updatedProfile

            // Sync to Supabase profiles table in background
            if (SupabaseConfig.isConfigured) {
                launch(Dispatchers.IO) {
                    SupabaseAuthService.upsertRemoteProfile(updatedProfile)
                }
            }

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateProfileInfo(name: String, photoURL: String?): Result<UserProfile> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("No user logged in"))
        val updated = current.copy(
            name = name.trim(),
            photoURL = photoURL,
            updatedAt = System.currentTimeMillis()
        )
        val result = updateProfile(updated)

        // If local avatar file, upload to Supabase Storage in background
        if (photoURL != null && photoURL.startsWith("file:") && SupabaseConfig.isConfigured) {
            launch(Dispatchers.IO) {
                try {
                    val uri = android.net.Uri.parse(photoURL)
                    val path = uri.path
                    if (path != null) {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            val syncManager = com.example.data.sync.SupabaseSyncManager(context, this@AuthRepository)
                            val uploadResult = syncManager.uploadAvatar(current.uid, file)
                            if (uploadResult.isSuccess) {
                                val publicUrl = uploadResult.getOrThrow()
                                val syncedProfile = updated.copy(photoURL = publicUrl, updatedAt = System.currentTimeMillis())
                                userDao.insertUser(UserEntity.fromUserProfile(syncedProfile, passwordHash = ""))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload avatar in background", e)
                }
            }
        }
        result
    }

    suspend fun changePassword(newPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("No user logged in"))
        try {
            if (SupabaseConfig.isConfigured) {
                val res = SupabaseAuthService.updatePassword(newPassword)
                if (res.isFailure) {
                    return@withContext res
                }
            }
            val userEntity = userDao.getUserById(current.uid)
            if (userEntity != null) {
                userDao.updateUser(userEntity.copy(updatedAt = System.currentTimeMillis()))
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Boolean> = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext Result.failure(Exception("No user logged in"))
        try {
            userDao.deleteUser(current.uid)
            logout()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun getSavedThemePreference(): String {
        return _currentUser.value?.themePreference
            ?: prefs.getString("app_theme_preference", "System")
            ?: "System"
    }

    fun saveThemePreference(theme: String) {
        prefs.edit().putString("app_theme_preference", theme).apply()
        val current = _currentUser.value
        if (current != null && current.themePreference != theme) {
            val updated = current.copy(themePreference = theme, updatedAt = System.currentTimeMillis())
            CoroutineScope(Dispatchers.IO).launch {
                updateProfile(updated)
            }
        }
    }
}

package com.example.data.remote.supabase

import android.util.Log
import com.example.data.model.UserProfile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class SupabaseAuthSession(
    val userId: String,
    val email: String,
    val name: String,
    val accessToken: String?,
    val refreshToken: String?
)

object SupabaseAuthService {
    private const val TAG = "SupabaseAuthService"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Signs up a new user via Supabase Auth GoTrue endpoint.
     */
    fun signUp(email: String, pass: String, name: String): Result<SupabaseAuthSession> {
        return try {
            val url = "${SupabaseConfig.authEndpoint}signup"
            val bodyJson = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass.trim())
                val dataObj = JSONObject().apply {
                    put("name", name.trim())
                    put("display_name", name.trim())
                }
                put("data", dataObj)
            }

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.optString("access_token", "").ifBlank { null }
                val refreshToken = json.optString("refresh_token", "").ifBlank { null }

                val userObj = json.optJSONObject("user") ?: json
                val userId = userObj.optString("id", "")
                val userEmail = userObj.optString("email", email)

                val userMeta = userObj.optJSONObject("user_metadata")
                val resolvedName = userMeta?.optString("name", name)
                    ?: userMeta?.optString("display_name", name)
                    ?: name

                if (userId.isNotBlank()) {
                    Result.success(
                        SupabaseAuthSession(
                            userId = userId,
                            email = userEmail,
                            name = resolvedName.ifBlank { name },
                            accessToken = accessToken,
                            refreshToken = refreshToken
                        )
                    )
                } else {
                    Result.failure(Exception("Registration completed but no user ID returned"))
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during signup", e)
            Result.failure(Exception("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Signup error", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with email and password.
     */
    fun signInWithPassword(email: String, pass: String): Result<SupabaseAuthSession> {
        return try {
            val url = "${SupabaseConfig.authEndpoint}token?grant_type=password"
            val bodyJson = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass.trim())
            }

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val accessToken = json.optString("access_token", "").ifBlank { null }
                val refreshToken = json.optString("refresh_token", "").ifBlank { null }

                val userObj = json.optJSONObject("user") ?: json
                val userId = userObj.optString("id", "")
                val userEmail = userObj.optString("email", email)

                val userMeta = userObj.optJSONObject("user_metadata")
                val resolvedName = userMeta?.optString("name", "")
                    ?: userMeta?.optString("display_name", "")
                    ?: ""

                if (userId.isNotBlank()) {
                    Result.success(
                        SupabaseAuthSession(
                            userId = userId,
                            email = userEmail,
                            name = resolvedName,
                            accessToken = accessToken,
                            refreshToken = refreshToken
                        )
                    )
                } else {
                    Result.failure(Exception("Login succeeded but failed to parse user information"))
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during login", e)
            Result.failure(Exception("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(e)
        }
    }

    /**
     * Updates user password on Supabase Auth.
     */
    fun updatePassword(newPassword: String): Result<Boolean> {
        return try {
            val url = "${SupabaseConfig.authEndpoint}user"
            val bodyJson = JSONObject().apply {
                put("password", newPassword.trim())
            }

            val request = Request.Builder()
                .url(url)
                .put(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Password update error", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the user profile from Supabase PostgreSQL profiles table.
     */
    fun fetchRemoteProfile(userId: String): UserProfile? {
        return try {
            val url = "${SupabaseConfig.restEndpoint}profiles?id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val array = JSONArray(responseBody)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return UserProfile(
                        uid = obj.optString("id", userId),
                        name = obj.optString("name", ""),
                        email = obj.optString("email", ""),
                        photoURL = if (obj.isNull("photo_url")) null else obj.optString("photo_url", null),
                        authProvider = "Supabase Auth",
                        xp = obj.optInt("xp", 0),
                        level = obj.optInt("level", 1),
                        currentStreak = obj.optInt("current_streak", 0),
                        longestStreak = obj.optInt("longest_streak", 0),
                        totalFocusMinutes = obj.optInt("total_focus_minutes", 0),
                        completedTasks = obj.optInt("completed_tasks", 0),
                        completedHabits = obj.optInt("completed_habits", 0),
                        completedGoals = obj.optInt("completed_goals", 0),
                        themePreference = obj.optString("theme_preference", "System"),
                        reduceMotion = obj.optBoolean("reduce_motion", false),
                        focusReminders = obj.optBoolean("focus_reminders", true),
                        taskReminders = obj.optBoolean("task_reminders", true),
                        habitReminders = obj.optBoolean("habit_reminders", true),
                        focusSounds = obj.optBoolean("focus_sounds", true),
                        completionSound = obj.optBoolean("completion_sound", true)
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote profile", e)
            null
        }
    }

    /**
     * Upserts/updates the user profile in Supabase PostgreSQL profiles table.
     */
    fun upsertRemoteProfile(profile: UserProfile): Result<Boolean> {
        return try {
            val url = "${SupabaseConfig.restEndpoint}profiles?id=eq.${profile.uid}"
            val bodyJson = JSONObject().apply {
                put("id", profile.uid)
                put("name", profile.name)
                put("email", profile.email)
                if (profile.photoURL != null) {
                    put("photo_url", profile.photoURL)
                }
                put("xp", profile.xp)
                put("level", profile.level)
                put("current_streak", profile.currentStreak)
                put("longest_streak", profile.longestStreak)
                put("total_focus_minutes", profile.totalFocusMinutes)
                put("completed_tasks", profile.completedTasks)
                put("completed_habits", profile.completedHabits)
                put("completed_goals", profile.completedGoals)
                put("theme_preference", profile.themePreference)
                put("reduce_motion", profile.reduceMotion)
                put("focus_reminders", profile.focusReminders)
                put("task_reminders", profile.taskReminders)
                put("habit_reminders", profile.habitReminders)
                put("focus_sounds", profile.focusSounds)
                put("completion_sound", profile.completionSound)
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url(url)
                .patch(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Prefer", "resolution=merge-duplicates")
                .build()

            val response = SupabaseClient.httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update remote profile (${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating remote profile", e)
            Result.failure(e)
        }
    }

    /**
     * Parses human-readable error messages from Supabase Auth responses.
     */
    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            if (responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val msg = json.optString("msg", "").ifBlank {
                    json.optString("message", "").ifBlank {
                        json.optString("error_description", "").ifBlank {
                            json.optString("error", "")
                        }
                    }
                }

                if (msg.isNotBlank()) {
                    when {
                        msg.contains("Invalid login credentials", ignoreCase = true) ->
                            "Incorrect email or password. Please verify your credentials."
                        msg.contains("User already registered", ignoreCase = true) ||
                        msg.contains("already registered", ignoreCase = true) ||
                        msg.contains("already exists", ignoreCase = true) ->
                            "An account with this email address already exists. Please login."
                        msg.contains("Password should be at least", ignoreCase = true) ->
                            "Password must be at least 6 characters long."
                        msg.contains("rate limit", ignoreCase = true) ->
                            "Too many requests. Please wait a moment and try again."
                        else -> msg
                    }
                } else {
                    "Authentication error ($statusCode). Please try again."
                }
            } else {
                "Authentication failed with status $statusCode."
            }
        } catch (e: Exception) {
            "Authentication failed. Please verify your connection and credentials."
        }
    }
}

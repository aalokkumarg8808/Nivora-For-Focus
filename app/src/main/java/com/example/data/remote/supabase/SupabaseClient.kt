package com.example.data.remote.supabase

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Minimal Supabase HTTP client configuration for cloud synchronization.
 * Handles auth headers, publishable API key injection, detailed network diagnostics,
 * and timeouts for PostgREST & Auth endpoints.
 */
object SupabaseClient {
    private const val TAG = "SupabaseClient"

    @Volatile
    private var activeAuthToken: String? = null

    fun setAuthToken(token: String?) {
        activeAuthToken = token
        if (token != null) {
            Log.d(TAG, "Supabase activeAuthToken updated (session active, length=${token.length})")
        } else {
            Log.d(TAG, "Supabase activeAuthToken cleared (anonymous/fallback mode)")
        }
    }

    fun getAuthToken(): String? = activeAuthToken

    private val authHeaderInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        val publishableKey = SupabaseConfig.publishableKey
        if (publishableKey.isNotBlank()) {
            requestBuilder.header("apikey", publishableKey)
        }

        val token = activeAuthToken
        val isUserSession = !token.isNullOrBlank()
        val bearerToken = if (isUserSession) token else publishableKey
        if (!bearerToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $bearerToken")
        }

        // Only add default headers if not explicitly specified by the caller
        if (originalRequest.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }

        if (originalRequest.header("Prefer") == null) {
            requestBuilder.header("Prefer", "return=representation")
        }

        val builtRequest = requestBuilder.build()
        val path = builtRequest.url.encodedPath
        val method = builtRequest.method
        val authMode = if (isUserSession) "UserSessionJWT" else "PublishableKey"

        val response: Response
        try {
            response = chain.proceed(builtRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase network request exception [$method $path] (Auth: $authMode): ${e.message}", e)
            throw e
        }

        if (!response.isSuccessful) {
            try {
                // Safely peek response body up to 64KB without consuming original stream
                val errorBody = response.peekBody(64 * 1024).string()
                Log.e(TAG, "Supabase request failed [$method $path] HTTP ${response.code} (Auth: $authMode) - Response: $errorBody")
            } catch (e: Exception) {
                Log.e(TAG, "Supabase request failed [$method $path] HTTP ${response.code} (Auth: $authMode) - could not read error body")
            }
        } else {
            Log.d(TAG, "Supabase request OK [$method $path] HTTP ${response.code} (Auth: $authMode)")
        }

        response
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            Log.d("SupabaseHTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}

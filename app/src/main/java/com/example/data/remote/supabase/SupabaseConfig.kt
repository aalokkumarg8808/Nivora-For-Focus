package com.example.data.remote.supabase

import com.example.BuildConfig

/**
 * Configuration holder for Supabase credentials injected via BuildConfig / .env.
 */
object SupabaseConfig {
    val url: String
        get() = BuildConfig.SUPABASE_URL.trim()

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = url.isNotBlank() &&
                publishableKey.isNotBlank() &&
                !url.contains("your-project", ignoreCase = true) &&
                !publishableKey.contains("your-supabase-publishable-key", ignoreCase = true)

    val restEndpoint: String
        get() = if (url.endsWith("/")) "${url}rest/v1/" else "$url/rest/v1/"

    val authEndpoint: String
        get() = if (url.endsWith("/")) "${url}auth/v1/" else "$url/auth/v1/"

    val storageEndpoint: String
        get() = if (url.endsWith("/")) "${url}storage/v1/" else "$url/storage/v1/"
}

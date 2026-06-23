package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object SupabaseService {
    private const val TAG = "SupabaseService"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Retrieve credentials from BuildConfig safely
    val url: String = try { BuildConfig.SUPABASE_URL } catch (e: Exception) { "" }
    val key: String = try { BuildConfig.SUPABASE_KEY } catch (e: Exception) { "" }

    /**
     * Check if the Supabase configuration parameters have been correctly configured by the user.
     * Prevents requests to placeholder URLs.
     */
    fun isConfigured(): Boolean {
        return url.isNotEmpty() && 
               url.startsWith("http") && 
               !url.contains("YOUR_SUPABASE_URL_PLACEHOLDER") &&
               key.isNotEmpty() && 
               !key.contains("YOUR_SUPABASE_KEY_PLACEHOLDER")
    }

    /**
     * Sign Up a new user in Supabase using GoTrue endpoint.
     */
    suspend fun signUp(name: String, email: String, psw: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext SupabaseAuthResult.Error("Supabase is not configured yet. Configure it in the Secrets panel.")
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", psw)
                put("data", JSONObject().apply {
                    put("name", name)
                })
            }

            val request = Request.Builder()
                .url("$url/auth/v1/signup")
                .header("apikey", key)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "signUp response code: ${response.code}, body: $bodyStr")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val userObj = jsonObj.optJSONObject("user")
                    val userId = userObj?.optString("id") ?: ""
                    val accessToken = jsonObj.optString("access_token") ?: ""
                    
                    if (userId.isNotEmpty()) {
                        SupabaseAuthResult.Success(userId, name, email, accessToken)
                    } else {
                        SupabaseAuthResult.Error("Sign up completed but user session details are empty.")
                    }
                } else {
                    val errMsg = parseError(bodyStr)
                    SupabaseAuthResult.Error(errMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp network failure", e)
            SupabaseAuthResult.Error("Network failure: ${e.message}")
        }
    }

    /**
     * Sign In an existing user using GoTrue endpoint.
     */
    suspend fun signIn(email: String, psw: String): SupabaseAuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext SupabaseAuthResult.Error("Supabase is not configured yet. Configure it in the Secrets panel.")
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", psw)
            }

            val request = Request.Builder()
                .url("$url/auth/v1/token?grant_type=password")
                .header("apikey", key)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "signIn response code: ${response.code}")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val userObj = jsonObj.optJSONObject("user")
                    val userId = userObj?.optString("id") ?: ""
                    val metadata = userObj?.optJSONObject("user_metadata")
                    val name = metadata?.optString("name") ?: "Sovereign Runner"
                    val accessToken = jsonObj.optString("access_token") ?: ""

                    if (userId.isNotEmpty()) {
                        SupabaseAuthResult.Success(userId, name, email, accessToken)
                    } else {
                        SupabaseAuthResult.Error("Session authenticated but userId is missing.")
                    }
                } else {
                    val errMsg = parseError(bodyStr)
                    SupabaseAuthResult.Error(errMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "signIn network failure", e)
            SupabaseAuthResult.Error("Network failure: ${e.message}")
        }
    }

    /**
     * Trigger a mock-supported or real password reset recovery link from Supabase.
     */
    suspend fun recoverPassword(email: String): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext "Local database fallback active. Simulation email sent to $email."
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
            }

            val request = Request.Builder()
                .url("$url/auth/v1/recover")
                .header("apikey", key)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    "A secure reset password link has been sent to $email."
                } else {
                    val errMsg = parseError(bodyStr)
                    throw IOException(errMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "recoverPassword network failure", e)
            "Error: ${e.message}"
        }
    }

    /**
     * Upsert user profile to 'profiles' table.
     */
    suspend fun upsertProfile(userId: String, name: String, email: String, avatarUrl: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false

        try {
            val json = JSONObject().apply {
                put("id", userId)
                put("name", name)
                put("email", email)
                put("avatarUrl", avatarUrl)
            }

            val request = Request.Builder()
                .url("$url/rest/v1/profiles")
                .header("apikey", key)
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "upsertProfile error", e)
            false
        }
    }

    /**
     * Post a newly logged session to 'activities' table on Supabase.
     */
    suspend fun insertActivity(userId: String, activityId: String, distance: Double, duration: Int, calories: Double, routeJson: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false

        try {
            val json = JSONObject().apply {
                put("id", activityId)
                put("user_id", userId)
                put("distance", distance)
                put("duration", duration)
                put("calories", calories)
                put("route", routeJson)
                put("created_at", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$url/rest/v1/activities")
                .header("apikey", key)
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertActivity error", e)
            false
        }
    }

    /**
     * Post a newly captured territory to 'territories' table on Supabase.
     */
    suspend fun insertTerritory(userId: String, territoryId: String, polygonJson: String, area: Double): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false

        try {
            val json = JSONObject().apply {
                put("id", territoryId)
                put("user_id", userId)
                put("polygon", polygonJson)
                put("area", area)
                put("captured_at", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$url/rest/v1/territories")
                .header("apikey", key)
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertTerritory error", e)
            false
        }
    }

    private fun parseError(body: String): String {
        return try {
            val obj = JSONObject(body)
            obj.optString("error_description", obj.optString("message", "An unexpected Supabase error occurred."))
        } catch (e: Exception) {
            "Error code from auth backend: $body"
        }
    }
}

sealed interface SupabaseAuthResult {
    data class Success(val userId: String, val name: String, val email: String, val token: String) : SupabaseAuthResult
    data class Error(val message: String) : SupabaseAuthResult
}

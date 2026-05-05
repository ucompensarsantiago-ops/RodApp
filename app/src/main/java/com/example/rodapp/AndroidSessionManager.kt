package com.example.rodapp

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidSessionManager(context: Context) : SessionManager {

    private val prefs = context.applicationContext
        .getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadSession(): UserSession? {
        val serialized = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    companion object {
        private const val KEY_SESSION = "session"
    }
}

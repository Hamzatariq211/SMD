package com.devs.i210396_i211384.network

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_IS_PROFILE_SETUP = "is_profile_setup"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, userId: String, isProfileSetup: Boolean) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_PROFILE_SETUP, isProfileSetup)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun isProfileSetup(): Boolean = prefs.getBoolean(KEY_IS_PROFILE_SETUP, false)

    fun setProfileSetup(isSetup: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PROFILE_SETUP, isSetup).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}


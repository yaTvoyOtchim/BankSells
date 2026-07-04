package com.example.vtbsales.session

import android.content.Context

class SessionStore(
    context: Context,
    preferencesName: String = "vtb_session"
) {
    private val preferences = context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun saveLastUserId(userId: String) {
        preferences.edit().putString(KEY_LAST_USER_ID, userId).apply()
    }

    fun lastUserId(): String? =
        preferences.getString(KEY_LAST_USER_ID, null)

    fun saveFcmToken(token: String) {
        preferences.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun fcmToken(): String? =
        preferences.getString(KEY_FCM_TOKEN, null)

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_LAST_USER_ID = "last_user_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}

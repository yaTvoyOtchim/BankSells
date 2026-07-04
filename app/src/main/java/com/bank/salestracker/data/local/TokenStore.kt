package com.bank.salestracker.data.local

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bank.salestracker.data.model.RegistrationStatus
import com.bank.salestracker.data.model.Role
import com.bank.salestracker.data.model.User

/**
 * Токены и данные сессии хранятся ТОЛЬКО в зашифрованном хранилище
 * (ключ — в Android Keystore). Для банка это обязательное требование.
 */
class TokenStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val deviceId: String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    var accessToken: String?
        get() = prefs.getString("access", null)
        set(v) = prefs.edit().putString("access", v).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh", null)
        set(v) = prefs.edit().putString("refresh", v).apply()

    fun saveUser(u: User) = prefs.edit()
        .putString("u_id", u.id)
        .putString("u_emp", u.employeeId)
        .putString("u_name", u.fullName)
        .putString("u_branch", u.branch)
        .putString("u_role", u.role.name)
        .putString("u_status", u.registrationStatus.name)
        .putString("u_org", u.orgUnitId)
        .apply()

    fun user(): User? {
        val id = prefs.getString("u_id", null) ?: return null
        return User(
            id = id,
            employeeId = prefs.getString("u_emp", "")!!,
            fullName = prefs.getString("u_name", "")!!,
            branch = prefs.getString("u_branch", "")!!,
            role = runCatching { Role.valueOf(prefs.getString("u_role", "EMPLOYEE")!!) }.getOrDefault(Role.EMPLOYEE),
            registrationStatus = runCatching {
                RegistrationStatus.valueOf(prefs.getString("u_status", "ACTIVE")!!)
            }.getOrDefault(RegistrationStatus.ACTIVE),
            orgUnitId = prefs.getString("u_org", null)
        )
    }

    val isLoggedIn: Boolean get() = refreshToken != null

    fun clear() = prefs.edit().clear().apply()
}

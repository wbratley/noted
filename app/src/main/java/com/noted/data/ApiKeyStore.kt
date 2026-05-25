package com.noted.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class ApiKeyStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "noted_secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKey(key: String) = prefs.edit().putString("api_key", key).apply()
    fun getKey(): String? = prefs.getString("api_key", null)

    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean("claude_enabled", enabled).apply()
    fun isEnabled(): Boolean = prefs.getBoolean("claude_enabled", false)
}

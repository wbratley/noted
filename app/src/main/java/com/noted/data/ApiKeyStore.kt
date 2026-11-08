package com.noted.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

enum class AiProvider { ANTHROPIC, DEEPSEEK }

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

    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean("ai_enabled", enabled).apply()

    /** Falls back to the legacy "claude_enabled" key so existing installs keep their setting. */
    fun isEnabled(): Boolean = prefs.getBoolean("ai_enabled", prefs.getBoolean("claude_enabled", false))

    fun setProvider(provider: AiProvider) = prefs.edit().putString("ai_provider", provider.name).apply()

    fun getProvider(): AiProvider =
        runCatching { AiProvider.valueOf(prefs.getString("ai_provider", null) ?: "") }.getOrNull()
            ?: AiProvider.ANTHROPIC
}

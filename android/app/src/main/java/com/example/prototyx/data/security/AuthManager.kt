package com.example.prototyx.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager(context: Context) {
    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to standard SharedPreferences if Keystore error occurs on device
        context.getSharedPreferences("fallback_auth_prefs", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return try {
            sharedPreferences.getString(KEY_TOKEN, null)
        } catch (e: Exception) {
            null
        }
    }

    fun clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
    }

    fun hasAcceptedDisclaimer(): Boolean {
        return try {
            sharedPreferences.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        } catch (e: Exception) {
            false
        }
    }

    fun setAcceptedDisclaimer(accepted: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }
}

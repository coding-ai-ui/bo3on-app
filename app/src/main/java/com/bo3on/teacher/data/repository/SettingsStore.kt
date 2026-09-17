package com.bo3on.teacher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bo3on.teacher.data.model.TeacherSettings
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

private val Context.settingsDataStore by preferencesDataStore("teacher_settings")
class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("settings")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val flow = context.settingsDataStore.data.map { prefs -> prefs[key]?.let { json.decodeFromString<TeacherSettings>(it) } ?: TeacherSettings() }
    suspend fun update(transform: (TeacherSettings) -> TeacherSettings) {
        context.settingsDataStore.edit { prefs ->
            val old = prefs[key]?.let { json.decodeFromString<TeacherSettings>(it) } ?: TeacherSettings()
            prefs[key] = json.encodeToString(TeacherSettings.serializer(), transform(old))
        }
    }
    suspend fun setPin(pin: String) {
        require(pin.matches(Regex("[0-9]{4,8}"))) { "أدخل رمزًا من 4 إلى 8 أرقام" }
        val salt = Base64.encodeToString(ByteArray(24).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
        val hash = hash(pin, salt)
        update { it.copy(pinHash = hash, pinSalt = salt, failedAttempts = 0, lockedUntil = 0) }
    }
    companion object {
        private fun hash(pin: String, salt: String): String {
            val spec = PBEKeySpec(pin.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 120_000, 256)
            return try { Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, Base64.NO_WRAP) } finally { spec.clearPassword() }
        }
        fun verify(pin: String, settings: TeacherSettings) = settings.pinHash.isNotEmpty() && MessageDigest.isEqual(hash(pin, settings.pinSalt).toByteArray(), settings.pinHash.toByteArray())
    }
}

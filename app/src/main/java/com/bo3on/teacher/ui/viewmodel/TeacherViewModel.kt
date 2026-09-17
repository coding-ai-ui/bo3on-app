package com.bo3on.teacher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bo3on.teacher.Bo3onApplication
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as Bo3onApplication
    val repo = app.repo
    val settingsStore = app.settings
    val data = repo.data.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TeacherData())
    val settings = settingsStore.flow.stateIn(viewModelScope, SharingStarted.Eagerly, TeacherSettings())
    val ready = MutableStateFlow(false)
    val unlocked = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val completed = MutableStateFlow<Long?>(null)
    init { viewModelScope.launch { val s = settingsStore.flow.first(); unlocked.value = s.pinHash.isEmpty(); ready.value = true } }
    fun action(message: String = "تم الحفظ بنجاح ✓", block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() }; if (message.isNotEmpty()) messages.emit(message) }
            catch (e: Exception) { if (e is CancellationException) throw e; messages.emit(e.message?.takeIf { it.any { c -> c in '\u0600'..'\u06ff' } } ?: "تعذّر تنفيذ العملية. تحقق من البيانات وحاول مجددًا") }
            finally { busy.value = false }
        }
    }
    fun lesson(id: Long) = action("تم تسجيل الحصة بنجاح ✓") { if (repo.registerLesson(id) && settings.value.completionAlert) completed.value = id }
    fun updateSettings(transform: (TeacherSettings) -> TeacherSettings) = action { settingsStore.update(transform) }
    fun unlock(pin: String) = action("") {
        val s = settings.value
        require(System.currentTimeMillis() >= s.lockedUntil) { "محاولات كثيرة؛ انتظر دقيقة ثم حاول مجددًا" }
        if (SettingsStore.verify(pin, s)) {
            settingsStore.update { it.copy(failedAttempts = 0, lockedUntil = 0) }; unlocked.value = true
        } else {
            settingsStore.update { it.copy(failedAttempts = it.failedAttempts + 1, lockedUntil = if (it.failedAttempts >= 4) System.currentTimeMillis() + 60_000 else 0) }
            error("رمز PIN غير صحيح")
        }
    }
    fun lock() { if (settings.value.pinHash.isNotEmpty()) unlocked.value = false }
}

package com.bo3on.teacher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bo3on.teacher.data.repository.SettingsStore
import com.bo3on.teacher.ui.components.*
import com.bo3on.teacher.ui.viewmodel.TeacherViewModel
import com.bo3on.teacher.utils.readAvatar
import kotlinx.coroutines.*

fun biometric(activity: FragmentActivity, success: () -> Unit, error: (String) -> Unit) {
    val available = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (available != BiometricManager.BIOMETRIC_SUCCESS) { error("فعّل بصمة آمنة من إعدادات الهاتف أولًا"); return }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { success() }
        override fun onAuthenticationError(code: Int, message: CharSequence) { if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON && code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_CANCELED) error("تعذّر التحقق بالبصمة. استخدم رمز PIN") }
    })
    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("الأستاذ بوعون").setSubtitle("تحقق من هويتك لفتح التطبيق").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText("استخدام PIN").build())
}

@Composable fun LockScreen(vm: TeacherViewModel, activity: FragmentActivity) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(20.dp)); Text("الأستاذ بوعون", style = MaterialTheme.typography.headlineLarge)
        Text("أدخل رمز الحماية لفتح بياناتك")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("رمز PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ vm.unlock(pin); pin = "" }, enabled = !busy && pin.length >= 4, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("فتح التطبيق") }
        if (settings.biometric) OutlinedButton({ biometric(activity, { vm.unlocked.value = true }, { message -> vm.messages.tryEmit(message) }) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Fingerprint, null); Text("فتح بالبصمة") }
    }
}

@Composable fun SettingsScreen(vm: TeacherViewModel, activity: FragmentActivity, navigate: (String) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var phone by rememberSaveable(settings.phone) { mutableStateOf(settings.phone) }
    var price by rememberSaveable(settings.defaultPrice) { mutableStateOf(settings.defaultPrice.toString()) }
    var size by rememberSaveable(settings.packageSize) { mutableStateOf(settings.packageSize.toString()) }
    var pinDialog by remember { mutableStateOf(false) }
    var removePin by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var notificationPermission by remember { mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> notificationPermission = granted; vm.messages.tryEmit(if (granted) "تم تفعيل إذن الإشعارات" else "الإشعارات غير مسموحة على هذا الجهاز") }
    val avatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.action { val encoded = readAvatar(context, uri); vm.settingsStore.update { it.copy(avatar = encoded) } } }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.action("تم حفظ النسخة الاحتياطية ✓") { val raw = vm.repo.backup(); requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(raw) } } }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri != null) vm.action("تم تصدير بيانات الطلاب ✓") { val raw = vm.repo.csv(); requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(raw) } } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }
    Page {
        Text("الإعدادات", style = MaterialTheme.typography.headlineMedium)
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { Avatar("بوعون", settings.avatar, 64.dp); Column { Text("الأستاذ بوعون", style = MaterialTheme.typography.titleLarge); Text("إدارة الحصص والطلاب", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            TextButton({ avatar.launch("image/*") }, enabled = !busy) { Text("تغيير الصورة الشخصية") }
            Field("رقم الهاتف", phone, { phone = it })
            Field("السعر الافتراضي بالدينار", price, { price = it }, numeric = true)
            Field("عدد الحصص في الاشتراك", size, { size = it }, numeric = true)
            Text("يُطبّق العدد الجديد على الدفعات الجديدة فقط. تبقى الاشتراكات السابقة كما هي.", style = MaterialTheme.typography.bodySmall)
            Button({ vm.action { val p = price.toLongOrNull() ?: 0; val n = size.toIntOrNull() ?: 0; require(p in 1..100_000_000 && n in 1..100) { "تحقق من السعر وعدد الحصص" }; require(phone.isBlank() || phone.matches(Regex("[+0-9 ()-]{6,20}"))) { "رقم الهاتف غير صحيح" }; vm.settingsStore.update { it.copy(phone = phone, defaultPrice = p, packageSize = n) } } }, enabled = !busy) { Text("حفظ بيانات الأستاذ") }
        }
        Section("المظهر")
        Choice("سمة التطبيق", settings.theme, listOf("light", "dark", "system"), { when (it) { "light" -> "فاتح"; "dark" -> "داكن"; else -> "حسب النظام" } }) { value -> vm.updateSettings { it.copy(theme = value) } }
        Section("الإشعارات")
        Panel {
            SettingSwitch("تذكير الحصص", settings.lessonReminders, !busy) { value -> vm.updateSettings { it.copy(lessonReminders = value) } }
            SettingSwitch("تذكير الدفعات", settings.paymentReminders, !busy) { value -> vm.updateSettings { it.copy(paymentReminders = value) } }
            SettingSwitch("تنبيه عند اكتمال الاشتراك", settings.completionAlert, !busy) { value -> vm.updateSettings { it.copy(completionAlert = value) } }
            if (!notificationPermission && Build.VERSION.SDK_INT >= 33) TextButton({ requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("السماح بإشعارات الهاتف") }
            Text("تصل تذكيرات المجموعات خلال الساعة السابقة للحصة. قد يؤخرها وضع توفير البطارية.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Section("الأمان")
        Panel {
            TextButton({ removePin = false; pinDialog = true }) { Text(if (settings.pinHash.isEmpty()) "تفعيل رمز PIN" else "تغيير رمز PIN") }
            if (settings.pinHash.isNotEmpty()) {
                TextButton({ removePin = true; pinDialog = true }) { Text("إيقاف رمز PIN") }
                SettingSwitch("فتح بالبصمة", settings.biometric, !busy) { value -> if (value) biometric(activity, { vm.updateSettings { it.copy(biometric = true) } }, { vm.messages.tryEmit(it) }) else vm.updateSettings { it.copy(biometric = false) } }
                TextButton({ vm.lock() }) { Text("قفل التطبيق الآن") }
            }
            Text("يُقفل التطبيق عند التشغيل وبعد مغادرته لمدة 30 ثانية. احتفظ برمزك؛ لا توجد خدمة لاسترجاعه.", style = MaterialTheme.typography.bodySmall)
        }
        Section("البيانات")
        Panel {
            Text("احفظ نسخة في مكان آمن. تشمل المدارس والطلاب والصور والمجموعات والحضور والحصص والدفعات والملاحظات. إعدادات الهاتف ورمز الحماية لا تُنقل.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton({ backup.launch("bo3on-${java.time.LocalDate.now()}.json") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.SaveAlt, null); Text("نسخة احتياطية") }
            OutlinedButton({ restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Restore, null); Text("استعادة نسخة") }
            OutlinedButton({ csv.launch("bo3on-students.csv") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileDownload, null); Text("تصدير بيانات الطلاب") }
            TextButton({ navigate("students") }) { Text("استعادة طالب من تصفية المؤرشفين") }
            data.schools.filter { it.archived }.forEach { school -> TextButton({ vm.action("تمت استعادة المدرسة") { vm.repo.dao.archiveSchool(school.id, false) } }) { Text("استعادة ${school.name}") } }
        }
        Text("بوعون • الإصدار 1.0\nرياضيات • فيزياء\nبياناتك محفوظة على هذا الجهاز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (pinDialog) PinDialog(vm, removePin) { pinDialog = false }
    restoreUri?.let { uri -> Confirm("استعادة النسخة الاحتياطية؟", "ستُستبدل بيانات التطبيق الحالية بالكامل ببيانات الملف. احفظ نسخة من بياناتك الحالية قبل المتابعة. يبقى رمز الحماية وإعدادات الهاتف كما هي.", "استعادة", { restoreUri = null }) {
        vm.action("تمت استعادة البيانات بنجاح ✓") {
            val text = requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { reader -> val builder = StringBuilder(); val buffer = CharArray(8192); var count = reader.read(buffer); while (count != -1) { builder.append(buffer, 0, count); require(builder.length <= 50_000_000) { "النسخة الاحتياطية كبيرة جدًا" }; count = reader.read(buffer) }; builder.toString() }
            vm.repo.restore(text)
        }; restoreUri = null
    } }
}

@Composable private fun SettingSwitch(label: String, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label, Modifier.weight(1f)); Switch(checked, change, enabled = enabled) } }

@Composable private fun PinDialog(vm: TeacherViewModel, remove: Boolean, dismiss: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var old by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (remove) "إيقاف الحماية" else "رمز الحماية") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (settings.pinHash.isNotEmpty()) PinField("الرمز الحالي", old) { old = it }
        if (!remove) { PinField("الرمز الجديد (4–8 أرقام)", pin) { pin = it }; PinField("تأكيد الرمز", confirm) { confirm = it } }
    } }, confirmButton = { TextButton({ vm.action {
        require(System.currentTimeMillis() >= settings.lockedUntil) { "انتظر دقيقة قبل إعادة المحاولة" }
        if (settings.pinHash.isNotEmpty() && !SettingsStore.verify(old, settings)) { vm.settingsStore.update { it.copy(failedAttempts = it.failedAttempts + 1, lockedUntil = if (it.failedAttempts >= 4) System.currentTimeMillis() + 60_000 else 0) }; error("رمز الحماية الحالي غير صحيح") }
        if (remove) vm.settingsStore.update { it.copy(pinHash = "", pinSalt = "", biometric = false, failedAttempts = 0, lockedUntil = 0) }
        else { require(pin == confirm) { "الرمزان غير متطابقين" }; vm.settingsStore.setPin(pin) }
        withContext(Dispatchers.Main) { dismiss() }
    } }, enabled = !busy) { Text("حفظ") } }, dismissButton = { TextButton(dismiss) { Text("إلغاء") } })
}
@Composable private fun PinField(label: String, value: String, change: (String) -> Unit) { OutlinedTextField(value, { change(it.filter { c -> c in '0'..'9' }.take(8)) }, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true) }

package com.bo3on.teacher.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.ui.components.*
import com.bo3on.teacher.ui.viewmodel.TeacherViewModel
import com.bo3on.teacher.utils.*
import kotlinx.coroutines.*
import java.time.*

@Composable fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = !multiline, minLines = if (multiline) 3 else 1, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text), shape = MaterialTheme.shapes.small)
}
@Composable fun DateField(label: String, date: LocalDate, onChange: (LocalDate) -> Unit, pastOnly: Boolean = true) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).also { if (pastOnly) it.datePicker.maxDate = System.currentTimeMillis() }.show()
    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("$label: ${dateText(start(date))}") }
}
@Composable fun TimeField(time: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = { val t = LocalTime.parse(time); TimePickerDialog(context, { _, h, m -> onChange("%02d:%02d".format(java.util.Locale.US, h, m)) }, t.hour, t.minute, true).show() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Schedule, null); Spacer(Modifier.width(8.dp)); Text("الوقت: $time") }
}

@Composable fun SchoolForm(vm: TeacherViewModel, id: Long, onDone: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val school = data.schools.find { it.id == id }
    var name by rememberSaveable(id) { mutableStateOf(school?.name ?: "") }
    var note by rememberSaveable(id) { mutableStateOf(school?.note ?: "") }
    Page {
        Text(if (id == 0L) "إضافة مدرسة" else "تعديل المدرسة", style = MaterialTheme.typography.headlineMedium)
        Text("نظّم طلابك حسب مدارسهم", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Field("اسم المدرسة *", name, { name = it })
        Field("ملاحظة عن المدرسة", note, { note = it }, multiline = true)
        Button(onClick = { vm.action { vm.repo.saveSchool(School(id, name, note)); withContext(Dispatchers.Main) { onDone() } } }, enabled = !busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("حفظ المدرسة") }
    }
}

@Composable fun StudentForm(vm: TeacherViewModel, id: Long, onDone: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val old = data.students.find { it.id == id }
    val schools = data.schools.filter { !it.archived }
    val context = LocalContext.current
    var name by rememberSaveable(id) { mutableStateOf(old?.name ?: "") }
    var phone by rememberSaveable(id) { mutableStateOf(old?.phone ?: "") }
    var parent by rememberSaveable(id) { mutableStateOf(old?.parentPhone ?: "") }
    var school by rememberSaveable(id) { mutableStateOf(old?.schoolId ?: schools.firstOrNull()?.id ?: 0L) }
    var subject by rememberSaveable(id) { mutableStateOf(old?.subject ?: Subject.MATH) }
    var level by rememberSaveable(id) { mutableStateOf(old?.level ?: EducationLevel.MIDDLE) }
    var year by rememberSaveable(id) { mutableIntStateOf(old?.year ?: 1) }
    var price by rememberSaveable(id) { mutableStateOf((old?.price ?: settings.defaultPrice).toString()) }
    var group by rememberSaveable(id) { mutableStateOf(old?.groupId) }
    var avatar by rememberSaveable(id) { mutableStateOf(old?.avatar ?: "") }
    var note by rememberSaveable(id) { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.action("") { val encoded = readAvatar(context, uri); withContext(Dispatchers.Main) { avatar = encoded } } }
    val groups = data.groups.filter { !it.archived && it.schoolId == school && it.subject == subject && it.level == level && it.year == year }
    Page {
        Text(if (id == 0L) "إضافة طالب" else "تعديل الطالب", style = MaterialTheme.typography.headlineMedium)
        if (schools.isEmpty()) Text("أضف مدرسة من الرئيسية أولًا", color = MaterialTheme.colorScheme.error)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { Avatar(name, avatar, 64.dp); TextButton({ imagePicker.launch("image/*") }) { Text("اختيار صورة") }; if (avatar.isNotBlank()) TextButton({ avatar = "" }) { Text("إزالة") } }
        Field("الاسم الكامل *", name, { name = it })
        Field("رقم الهاتف", phone, { phone = it })
        Field("رقم ولي الأمر", parent, { parent = it })
        Choice("المدرسة *", school, schools.map { it.id }, { data.schools.find { s -> s.id == it }?.name ?: "اختر مدرسة" }) { school = it; group = null }
        Choice("المادة *", subject, Subject.entries.toList(), { it.title }) { subject = it; group = null }
        Choice("الطور *", level, EducationLevel.entries.toList(), { it.title }) { level = it; year = 1; group = null }
        Choice("السنة *", year, (1..level.years).toList(), { yearLabel(level, it) }) { year = it; group = null }
        Choice("المجموعة", group, listOf<Long?>(null) + groups.map { it.id }, { gid -> groups.find { it.id == gid }?.name ?: "دون مجموعة" }) { group = it }
        Field("سعر الاشتراك (${settings.packageSize} حصص) *", price, { price = it }, numeric = true)
        Field(if (id == 0L) "ملاحظات" else "إضافة ملاحظة جديدة", note, { note = it }, multiline = true)
        Button(onClick = { vm.action(if (id == 0L) "تمت إضافة الطالب بنجاح ✓" else "تم تعديل الطالب ✓") { vm.repo.saveStudent(Student(id, name, phone, parent, school, subject, level, year, price.toLongOrNull() ?: 0, group, old?.registeredAt ?: System.currentTimeMillis(), avatar, old?.archived ?: false), note); withContext(Dispatchers.Main) { onDone() } } }, enabled = !busy && name.isNotBlank() && school > 0, modifier = Modifier.fillMaxWidth()) { Text("حفظ الطالب") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun GroupForm(vm: TeacherViewModel, id: Long, onDone: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val old = data.groups.find { it.id == id }
    val schools = data.schools.filter { !it.archived }
    var name by rememberSaveable(id) { mutableStateOf(old?.name ?: "") }
    var school by rememberSaveable(id) { mutableLongStateOf(old?.schoolId ?: schools.firstOrNull()?.id ?: 0L) }
    var subject by rememberSaveable(id) { mutableStateOf(old?.subject ?: Subject.MATH) }
    var level by rememberSaveable(id) { mutableStateOf(old?.level ?: EducationLevel.MIDDLE) }
    var year by rememberSaveable(id) { mutableIntStateOf(old?.year ?: 1) }
    var days by rememberSaveable(id) { mutableStateOf(old?.days ?: "7,2") }
    var time by rememberSaveable(id) { mutableStateOf(old?.time ?: "17:00") }
    Page {
        Text(if (id == 0L) "إضافة مجموعة" else "تعديل المجموعة", style = MaterialTheme.typography.headlineMedium)
        Field("اسم المجموعة *", name, { name = it })
        Choice("المدرسة *", school, schools.map { it.id }, { data.school(it) }) { school = it }
        Choice("المادة", subject, Subject.entries.toList(), { it.title }) { subject = it }
        Choice("الطور", level, EducationLevel.entries.toList(), { it.title }) { level = it; year = 1 }
        Choice("السنة", year, (1..level.years).toList(), { yearLabel(level, it) }) { year = it }
        Text("الأيام الأسبوعية", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { dayNames.forEach { (key, title) -> FilterChip(selected = key.toString() in days.split(','), onClick = { val set = days.split(',').filter { it.isNotBlank() }.toMutableSet(); if (!set.add(key.toString())) set.remove(key.toString()); days = set.joinToString(",") }, label = { Text(title) }) } }
        TimeField(time) { time = it }
        Text("اربط الطلاب بالمجموعة من نموذج إضافة أو تعديل الطالب. تغيير المدرسة أو المادة أو السنة يفصل الطلاب السابقين عن المجموعة.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { vm.action { vm.repo.saveGroup(TeachingGroup(id, name, school, subject, level, year, days, time)); withContext(Dispatchers.Main) { onDone() } } }, enabled = !busy && name.isNotBlank() && school > 0 && days.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("حفظ المجموعة") }
    }
}

@Composable fun PaymentDialog(vm: TeacherViewModel, id: Long, onDismiss: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val s = data.summary(id) ?: return
    var amount by rememberSaveable(id) { mutableStateOf(s.student.price.toString()) }
    var capacity by rememberSaveable(id) { mutableStateOf(settings.packageSize.toString()) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var method by rememberSaveable { mutableStateOf("نقدًا") }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text("تسجيل دفعة") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.student.name, style = MaterialTheme.typography.titleMedium)
            if (s.paid && s.count < s.capacity) Text("سيبدأ اشتراك جديد من الصفر. تبقى الحصص المتبقية (${s.capacity - s.count}) في سجل الاشتراك السابق ولا تنتقل للاشتراك الجديد.", color = MaterialTheme.colorScheme.error)
            Field("المبلغ بالدينار *", amount, { amount = it }, numeric = true)
            Field("عدد الحصص *", capacity, { capacity = it }, numeric = true)
            DateField("تاريخ الدفع", date, { date = it })
            Choice("طريقة الدفع", method, listOf("نقدًا", "بريدي موب", "تحويل", "أخرى"), { it }) { method = it }
        }
    }, confirmButton = { TextButton(onClick = { vm.action("تم تسجيل الدفع ✓") { vm.repo.pay(id, amount.toLongOrNull() ?: 0, capacity.toIntOrNull() ?: 0, start(date), method); withContext(Dispatchers.Main) { onDismiss() } } }, enabled = !busy) { Text("تأكيد الدفع") } }, dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("إلغاء") } })
}

@Composable fun NoteDialog(vm: TeacherViewModel, studentId: Long, old: StudentNote?, onDismiss: () -> Unit) {
    var text by rememberSaveable(old?.id) { mutableStateOf(old?.text ?: "") }
    val busy by vm.busy.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ملاحظات الأستاذ بوعون") }, text = { Field("الملاحظة", text, { text = it }, multiline = true) }, confirmButton = { TextButton(onClick = { vm.action { vm.repo.saveNote(StudentNote(old?.id ?: 0, studentId, text, old?.createdAt ?: System.currentTimeMillis())); withContext(Dispatchers.Main) { onDismiss() } } }, enabled = !busy && text.isNotBlank()) { Text("حفظ") } }, dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable fun EditLessonDialog(vm: TeacherViewModel, lesson: Lesson, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(day(lesson.occurredAt)) }
    var time by remember { mutableStateOf(timeText(lesson.occurredAt)) }
    val busy by vm.busy.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("تعديل الحصة") }, text = { Column { DateField("التاريخ", date, { date = it }); TimeField(time) { time = it } } }, confirmButton = { TextButton(onClick = { vm.action { vm.repo.editLesson(lesson.id, date.atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()); withContext(Dispatchers.Main) { onDismiss() } } }, enabled = !busy) { Text("حفظ") } }, dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

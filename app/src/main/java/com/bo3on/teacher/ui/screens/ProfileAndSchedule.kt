package com.bo3on.teacher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.data.repository.TeacherData
import com.bo3on.teacher.ui.components.*
import com.bo3on.teacher.ui.viewmodel.TeacherViewModel
import com.bo3on.teacher.utils.*
import java.time.*

@OptIn(ExperimentalLayoutApi::class)
@Composable fun StudentProfile(vm: TeacherViewModel, id: Long, navigate: (String) -> Unit, lesson: (Long) -> Unit, payment: (Long) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val s = data.summary(id) ?: return
    val student = s.student
    val context = LocalContext.current
    var noteOpen by remember { mutableStateOf(false) }
    var noteEdit by remember { mutableStateOf<StudentNote?>(null) }
    var deleteNote by remember { mutableStateOf<StudentNote?>(null) }
    var deleteLesson by remember { mutableStateOf<Lesson?>(null) }
    var editLesson by remember { mutableStateOf<Lesson?>(null) }
    var archive by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val lessons = data.lessons.filter { it.studentId == id }
    val payments = data.payments.filter { it.studentId == id }
    val notes = data.notes.filter { it.studentId == id }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { Avatar(student.name, student.avatar, 76.dp); Column(Modifier.weight(1f)) { Text(student.name, style = MaterialTheme.typography.headlineMedium); Text("${student.subject.title} • ${yearLabel(student.level, student.year)}", color = MaterialTheme.colorScheme.onSurfaceVariant); if (student.archived) Text("طالب مؤرشف", color = MaterialTheme.colorScheme.error) else Status(s) } } }
        item { Panel {
            Section("الحصص الحالية")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${s.count} من ${s.capacity}", style = MaterialTheme.typography.headlineMedium); Text("${s.count * 100 / s.capacity}%", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.headlineMedium) }
            LinearProgressIndicator(progress = { s.count.toFloat() / s.capacity }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = if (s.due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            if (s.due) Text("انتهت الحصص • يجب تسجيل دفعة جديدة", color = MaterialTheme.colorScheme.error)
            if (s.payment == null) Text("سجّل أول دفعة لبدء متابعة الحصص", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Info("سعر الاشتراك", money(student.price)); Info("آخر دفعة", s.payment?.let { dateText(it.paidAt) } ?: "لا توجد دفعة")
        } }
        if (!student.archived) item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ lesson(id) }) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Text("تسجيل حصة") }
            FilledTonalButton({ payment(id) }) { Icon(Icons.Default.Payments, null, Modifier.size(18.dp)); Text("تسجيل دفعة") }
            OutlinedButton({ noteEdit = null; noteOpen = true }) { Text("إضافة ملاحظة") }
            OutlinedButton({ vm.action("") { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { contact(context, student.phone.ifBlank { student.parentPhone }, false) } } }) { Icon(Icons.Default.Call, null, Modifier.size(18.dp)); Text("اتصال") }
            OutlinedButton({ vm.action("") { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { contact(context, student.phone.ifBlank { student.parentPhone }, true) } } }) { Text("واتساب") }
        } }
        item { Panel {
            Section("بيانات الطالب", if (!student.archived) "تعديل" else null, { navigate("studentEdit/$id") })
            Info("الاسم الكامل", student.name); Info("الهاتف", student.phone); Info("ولي الأمر", student.parentPhone); Info("المدرسة", data.school(student.schoolId)); Info("المادة", student.subject.title); Info("الطور", student.level.title); Info("السنة", yearLabel(student.level, student.year)); Info("المجموعة", data.groups.find { it.id == student.groupId }?.name ?: "دون مجموعة"); Info("تاريخ التسجيل", dateText(student.registeredAt))
        } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("سجل الحصص", "سجل الدفعات", "الملاحظات").forEachIndexed { i, title -> FilterChip(tab == i, { tab = i }, label = { Text(title) }) } } }
        when (tab) {
            0 -> {
                if (lessons.isEmpty()) item { EmptyState("لا توجد حصص مسجلة", "تظهر هنا جميع الحصص من كل الاشتراكات.") }
                items(lessons, key = { "lesson-${it.id}" }) { l -> Panel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الحصة ${l.number}", style = MaterialTheme.typography.titleMedium); Text(timeText(l.occurredAt), color = MaterialTheme.colorScheme.secondary) }; Text("${dateText(l.occurredAt)} • ${l.subject.title}"); Text("${l.schoolName} • ${yearLabel(l.level, l.year)} • اشتراك #${l.paymentId}", style = MaterialTheme.typography.bodySmall); Row { if (l.attendanceId == null) TextButton({ editLesson = l }) { Text("تعديل") }; TextButton({ deleteLesson = l }) { Text("حذف", color = MaterialTheme.colorScheme.error) } } } }
            }
            1 -> {
                if (payments.isEmpty()) item { EmptyState("لا توجد دفعات", "سجّل الدفعة الأولى لبدء الاشتراك.") }
                items(payments, key = { "payment-${it.id}" }) { p -> Panel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(money(p.amount), style = MaterialTheme.typography.titleLarge); Text("✓ مدفوع", color = MaterialTheme.colorScheme.secondary) }; Text("${dateText(p.paidAt)} • ${p.method}"); Text("${data.lessons.count { it.paymentId == p.id }} / ${p.capacity} حصص • ${if (p.id == s.payment?.id) "الاشتراك الحالي" else "اشتراك سابق"}", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            2 -> {
                item { Section("ملاحظات الأستاذ بوعون", "إضافة", { noteEdit = null; noteOpen = true }) }
                if (notes.isEmpty()) item { EmptyState("لا توجد ملاحظات بعد", "دوّن تقدم الطالب وما يحتاج إلى مراجعته.") }
                items(notes, key = { "note-${it.id}" }) { n -> Panel { Text(dateText(n.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(n.text); Row { TextButton({ noteEdit = n; noteOpen = true }) { Text("تعديل") }; TextButton({ deleteNote = n }) { Text("حذف", color = MaterialTheme.colorScheme.error) } } } }
            }
        }
        item { if (student.archived) OutlinedButton({ vm.action("تمت استعادة الطالب ✓") { vm.repo.restoreStudent(id) } }, Modifier.fillMaxWidth()) { Text("استعادة الطالب") } else TextButton({ archive = true }, Modifier.fillMaxWidth()) { Text("حذف الطالب", color = MaterialTheme.colorScheme.error) } }
    }
    if (noteOpen) NoteDialog(vm, id, noteEdit) { noteOpen = false }
    editLesson?.let { EditLessonDialog(vm, it) { editLesson = null } }
    deleteNote?.let { n -> Confirm("حذف الملاحظة؟", "سيتم حذف هذه الملاحظة نهائيًا.", "حذف", { deleteNote = null }) { vm.action("تم حذف الملاحظة") { vm.repo.dao.deleteNote(n.id) }; deleteNote = null } }
    deleteLesson?.let { l -> Confirm("حذف الحصة؟", "سيُصحّح عدّاد الاشتراك الذي تنتمي إليه الحصة، مع الحفاظ على الدفعة. إذا كانت مرتبطة بالحضور تصبح ملغاة.", "حذف الحصة", { deleteLesson = null }) { vm.action("تم حذف الحصة وتصحيح العدّاد") { vm.repo.deleteLesson(l.id) }; deleteLesson = null } }
    if (archive) Confirm("حذف ${student.name}؟", "سيُنقل الطالب للأرشيف مع الاحتفاظ بجميع الحصص والدفعات. يمكنك استعادته من تصفية المؤرشفين.", "نقل للأرشيف", { archive = false }) { vm.action("تمت أرشفة الطالب") { vm.repo.dao.archiveStudent(id, true) }; archive = false }
}

@Composable fun TodayScreen(vm: TeacherViewModel, navigate: (String) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    var date by remember { mutableStateOf(LocalDate.now()) }
    var attendance by remember { mutableStateOf<Triple<Long, Long, String>?>(null) }
    val groups = data.groups.filter { !it.archived && date.dayOfWeek.value.toString() in it.days.split(',') }.sortedBy { it.time }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Section("حصص اليوم", "المجموعات", { navigate("groups") }) }
        item { DateField("البرنامج", date, { date = it }, pastOnly = true) }
        item { Text("الحضور يسجّل حصة واحدة. الغياب والإلغاء لا يستهلكان حصة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (groups.isEmpty()) item { EmptyState("يوم بلا حصص مجدولة", "أنشئ مجموعة، حدّد الأيام والوقت، ثم أضف الطلاب إليها.", "إضافة مجموعة") { navigate("groupEdit/0") } }
        groups.forEach { group ->
            item(key = "group-${group.id}") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(group.time, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary); TextButton({ navigate("group/${group.id}") }) { Text(group.name) } } }
            val students = data.active.filter { it.student.groupId == group.id }
            if (students.isEmpty()) item { Text("لا يوجد طلاب في هذه المجموعة. اربط الطلاب بها من بيانات الطالب.") }
            items(students, key = { "${group.id}-${it.student.id}" }) { s ->
                val status = data.attendance.find { it.studentId == s.student.id && it.groupId == group.id && it.date == date.toString() }?.status
                Panel {
                    Row(Modifier.fillMaxWidth().clickable { navigate("profile/${s.student.id}") }, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Avatar(s.student.name, s.student.avatar); Column { Text(s.student.name, style = MaterialTheme.typography.titleMedium); Text("${s.student.subject.title} • ${yearLabel(s.student.level, s.student.year)}", style = MaterialTheme.typography.bodySmall) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("حاضر", "غائب", "ملغاة").forEach { st -> FilterChip(status == st, { if (status != st) attendance = Triple(s.student.id, group.id, st) }, label = { Text(st) }) } }
                }
            }
        }
        val manual = data.lessons.filter { day(it.occurredAt) == date && it.attendanceId == null }
        if (manual.isNotEmpty()) item { Section("حصص مسجلة بشكل فردي") }
        items(manual, key = { "manual-${it.id}" }) { l -> Panel { Text(data.students.find { it.id == l.studentId }?.name ?: "طالب", style = MaterialTheme.typography.titleMedium); Text("${timeText(l.occurredAt)} • ${l.subject.title} • الحصة ${l.number}"); TextButton({ navigate("profile/${l.studentId}") }) { Text("فتح الملف") } } }
    }
    attendance?.let { (student, group, status) -> Confirm("تأكيد الحضور", "${data.summary(student)?.student?.name}: $status. تغيير حضور سابق يصحّح عدّاد الحصص تلقائيًا.", "تسجيل", { attendance = null }) {
        vm.action("تم حفظ الحضور ✓") { if (vm.repo.markAttendance(student, group, date, status) && vm.settings.value.completionAlert) vm.completed.value = student }; attendance = null
    } }
}

@Composable fun GroupsScreen(data: TeacherData, navigate: (String) -> Unit) {
    Page { Section("المجموعات", "+ إضافة", { navigate("groupEdit/0") }); if (data.groups.none { !it.archived }) EmptyState("مجموعاتك الدراسية", "نظّم مواعيد الطلاب والحضور من مكان واحد.", "إضافة مجموعة") { navigate("groupEdit/0") }; data.groups.filter { !it.archived }.forEach { GroupTile(it, data, navigate) } }
}

@Composable fun GroupDetails(vm: TeacherViewModel, id: Long, navigate: (String) -> Unit, lesson: (Long) -> Unit, payment: (Long) -> Unit, back: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val group = data.groups.find { it.id == id } ?: return
    var remove by remember { mutableStateOf(false) }
    Page {
        Text(group.name, style = MaterialTheme.typography.headlineMedium)
        Panel { Info("المدرسة", data.school(group.schoolId)); Info("المادة", group.subject.title); Info("السنة", yearLabel(group.level, group.year)); Info("الأيام", daysText(group.days)); Info("الوقت", group.time) }
        Row { TextButton({ navigate("groupEdit/$id") }) { Text("تعديل المجموعة") }; TextButton({ navigate("today") }) { Text("تسجيل الحضور") }; TextButton({ remove = true }) { Text("حذف", color = MaterialTheme.colorScheme.error) } }
        Section("طلاب المجموعة")
        val students = data.active.filter { it.student.groupId == id }
        if (students.isEmpty()) EmptyState("المجموعة جاهزة", "افتح بيانات الطالب واختر هذه المجموعة لربطه بها.", "عرض الطلاب") { navigate("students") }
        students.forEach { s -> StudentCard(s, "", { navigate("profile/${s.student.id}") }, { lesson(s.student.id) }, { payment(s.student.id) }) }
    }
    if (remove) Confirm("حذف المجموعة؟", "سيُفصل الطلاب عن المجموعة وتُحفظ سجلات الحضور والحصص السابقة.", "حذف", { remove = false }) { vm.action("تم حذف المجموعة") { vm.repo.archiveGroup(id); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { back() } }; remove = false }
}

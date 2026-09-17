package com.bo3on.teacher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.data.repository.TeacherData
import com.bo3on.teacher.ui.components.*
import com.bo3on.teacher.utils.*
import java.time.*

@Composable fun Dashboard(data: TeacherData, settings: TeacherSettings, navigate: (String) -> Unit, lesson: (Long) -> Unit, payment: (Long) -> Unit) {
    val today = LocalDate.now()
    val groups = data.groups.filter { !it.archived && today.dayOfWeek.value.toString() in it.days.split(',') }
    val todayCount = groups.sumOf { g -> data.active.count { it.student.groupId == g.id } } + data.lessons.count { day(it.occurredAt) == today && it.attendanceId == null }
    val monthly = data.payments.filter { YearMonth.from(day(it.paidAt)) == YearMonth.from(today) }.sumOf { it.amount }
    Page {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("الأستاذ بوعون", style = MaterialTheme.typography.titleMedium); Text(dateText(start(today)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Avatar("بوعون", settings.avatar)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Navy, Color(0xFF234D69))), RoundedCornerShape(28.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("رياضيات  •  فيزياء", color = Color(0xFF8CE3CD), style = MaterialTheme.typography.labelLarge)
            Text("مرحبًا أستاذ بوعون 👋", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("إليك ملخص دروسك اليوم", color = Color(0xFFD3DFEA))
            FilledTonalButton({ navigate("today") }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = .13f), contentColor = Color.White)) { Text("عرض برنامج اليوم"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp)) }
        }
        OutlinedCard(onClick = { navigate("students") }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.Search, null); Text("ابحث عن طالب، مدرسة، أو مادة...", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Stat("عدد الطلاب", data.active.size.toString(), Icons.Default.People, Modifier.weight(1f), onClick = { navigate("students") }); Stat("حصص اليوم", todayCount.toString(), Icons.Default.CalendarMonth, Modifier.weight(1f), onClick = { navigate("today") }) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Stat("يجب عليهم الدفع", data.due.size.toString(), Icons.Default.AccountBalanceWallet, Modifier.weight(1f), MaterialTheme.colorScheme.error) { navigate("due") }; Stat("إيرادات هذا الشهر", money(monthly), Icons.Default.TrendingUp, Modifier.weight(1f), onClick = { navigate("finance") }) }
        Section("يحتاج انتباهك")
        Panel {
            if (data.due.isEmpty()) Text("كل الاشتراكات الجارية ضمن حصصها", color = MaterialTheme.colorScheme.secondary)
            else TextButton({ navigate("due") }) { Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text("${data.due.size} طلاب أكملوا حصصهم ويجب عليهم الدفع", color = MaterialTheme.colorScheme.error) }
            Text("${groups.size} مجموعات في برنامج اليوم", color = MaterialTheme.colorScheme.onSurfaceVariant)
            val week = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            Text("تم تسجيل ${data.payments.count { day(it.paidAt) >= week && day(it.paidAt) <= today }} دفعات هذا الأسبوع", color = MaterialTheme.colorScheme.secondary)
        }
        Section("المدارس", "+ إضافة مدرسة", { navigate("schoolEdit/0") })
        val schools = data.schools.filter { !it.archived }
        if (schools.isEmpty()) EmptyState("بداية منظّمة", "أضف مدرستك الأولى ثم طلاب الرياضيات والفيزياء.", "إضافة مدرسة") { navigate("schoolEdit/0") }
        schools.forEach { s -> Card(onClick = { navigate("school/${s.id}") }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.secondary); Column(Modifier.weight(1f)) { Text(s.name, style = MaterialTheme.typography.titleMedium); Text("${data.active.count { it.student.schoolId == s.id }} طالب", style = MaterialTheme.typography.bodySmall) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null) } } }
        Section("آخر الطلاب", "عرض الكل", { navigate("students") })
        if (data.active.isEmpty()) EmptyState("طلابك في مكان واحد", "أضف طالبًا لتبدأ متابعة الحصص والاشتراكات.", "إضافة طالب") { navigate("studentEdit/0") }
        data.active.take(3).forEach { s -> StudentCard(s, s.student.subject.title, { navigate("profile/${s.student.id}") }, { lesson(s.student.id) }, { payment(s.student.id) }) }
        Section("حصص اليوم", "البرنامج", { navigate("today") })
        if (groups.isEmpty()) Text("لا توجد مجموعات مجدولة اليوم", color = MaterialTheme.colorScheme.onSurfaceVariant)
        groups.take(3).forEach { g -> GroupTile(g, data, navigate) }
        Section("المجموعات الأخيرة", "إدارة المجموعات", { navigate("groups") })
        data.groups.filter { !it.archived }.sortedByDescending { it.id }.take(3).forEach { GroupTile(it, data, navigate) }
        if (data.groups.none { !it.archived }) EmptyState("رتّب أسبوعك", "أنشئ مجموعة وحدّد أيامها ووقتها.", "إضافة مجموعة") { navigate("groupEdit/0") }
    }
}

@Composable fun GroupTile(g: TeachingGroup, data: TeacherData, navigate: (String) -> Unit) {
    Card(onClick = { navigate("group/${g.id}") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(g.name, style = MaterialTheme.typography.titleMedium); Text(g.time, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }; Text("${g.subject.title} • ${yearLabel(g.level, g.year)}"); Text("${daysText(g.days)} • ${data.active.count { it.student.groupId == g.id }} طالب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable fun BrowseSchool(data: TeacherData, school: Long, subject: Subject?, level: EducationLevel?, navigate: (String) -> Unit, archive: () -> Unit) {
    val item = data.schools.find { it.id == school } ?: return
    val students = data.active.filter { it.student.schoolId == school }
    Page {
        Text(item.name, style = MaterialTheme.typography.headlineMedium)
        if (item.note.isNotBlank()) Text(item.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (subject == null) {
            Row { TextButton({ navigate("schoolEdit/$school") }) { Icon(Icons.Default.Edit, null); Text("تعديل") }; TextButton(archive) { Icon(Icons.Default.Archive, null); Text("حذف المدرسة") } }
            Section("اختر المادة")
            Subject.entries.forEach { s -> BrowseTile(s.title, "${students.count { it.student.subject == s }} طالب", if (s == Subject.MATH) Icons.Default.Functions else Icons.Default.Science) { navigate("level/$school/${s.name}") } }
        } else if (level == null) {
            Text(subject.title, color = MaterialTheme.colorScheme.secondary)
            Section("اختر الطور")
            EducationLevel.entries.forEach { l -> BrowseTile(l.title, if (l == EducationLevel.MIDDLE) "السنة الأولى إلى الرابعة متوسط" else "السنة الأولى إلى الثالثة ثانوي", Icons.Default.School) { navigate("year/$school/${subject.name}/${l.name}") } }
        } else {
            Text("${subject.title} • ${level.title}", color = MaterialTheme.colorScheme.secondary)
            Section("اختر السنة")
            (1..level.years).forEach { y -> BrowseTile(yearLabel(level, y), "${students.count { it.student.subject == subject && it.student.level == level && it.student.year == y }} طالب", Icons.Default.AutoStories) { navigate("students/$school/${subject.name}/${level.name}/$y") } }
        }
    }
}
@Composable private fun BrowseTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(onClick, Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null) } }
}

@Composable fun StudentList(data: TeacherData, navigate: (String) -> Unit, lesson: (Long) -> Unit, payment: (Long) -> Unit, school: Long = 0, subject: Subject? = null, level: EducationLevel? = null, year: Int = 0, selectAction: String? = null) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("الكل") }
    fun normalize(s: String) = s.lowercase().replace(Regex("[\u064B-\u065F\u0640]"), "").replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
    val results = remember(data, search, filter, school, subject, level, year) {
        data.summaries.filter { s ->
            val student = s.student
            (if (filter == "المؤرشفون") student.archived else !student.archived) &&
                (school == 0L || student.schoolId == school) && (subject == null || student.subject == subject) && (level == null || student.level == level) && (year == 0 || student.year == year) &&
                (filter != "يجب الدفع" || s.due) && (filter != "مدفوع" || s.paid) &&
                normalize("${student.name} ${student.phone} ${student.parentPhone} ${data.school(student.schoolId)} ${student.subject.title} ${student.level.title} ${yearLabel(student.level, student.year)}").contains(normalize(search.trim()))
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(if (selectAction == "lesson") "اختر طالبًا لتسجيل حصة" else if (selectAction == "payment") "اختر طالبًا لتسجيل دفعة" else "الطلاب", style = MaterialTheme.typography.headlineMedium) }
        if (subject != null && level != null) item { Text("${subject.title} • ${yearLabel(level, year)}\n${data.school(school)}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("ابحث عن طالب...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.medium) }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("الكل", "نشط", "يجب الدفع", "مدفوع", "المؤرشفون").forEach { f -> FilterChip(filter == f, { filter = f }, label = { Text(f) }) } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${results.size} طالب", color = MaterialTheme.colorScheme.onSurfaceVariant); TextButton({ navigate("studentEdit/0") }) { Text("+ إضافة طالب") } } }
        if (results.isEmpty()) item { EmptyState("لا توجد نتائج", "أضف طالبًا أو جرّب تغيير كلمات البحث والتصفية.") }
        items(results, key = { it.student.id }) { s -> StudentCard(s, "${s.student.subject.title} • ${yearLabel(s.student.level, s.student.year)} • ${data.school(s.student.schoolId)}", { when (selectAction) { "lesson" -> lesson(s.student.id); "payment" -> payment(s.student.id); else -> navigate("profile/${s.student.id}") } }, { lesson(s.student.id) }, { payment(s.student.id) }) }
    }
}

@Composable fun DueScreen(data: TeacherData, navigate: (String) -> Unit, payment: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("يجب الدفع", style = MaterialTheme.typography.headlineMedium) }
        item { Panel { Info("عدد الطلاب", data.due.size.toString()); Info("إجمالي المبالغ المنتظرة", money(data.due.sumOf { it.student.price })) } }
        if (data.due.isEmpty()) item { EmptyState("لا توجد اشتراكات منتهية", "ستظهر هنا الاشتراكات فور اكتمال عدد حصصها.") }
        items(data.due, key = { it.student.id }) { s -> StudentCard(s, "${s.student.subject.title} • ${yearLabel(s.student.level, s.student.year)} • ${money(s.student.price)}", { navigate("profile/${s.student.id}") }, { payment(s.student.id) }, { payment(s.student.id) }) }
    }
}

@Composable fun FinanceScreen(data: TeacherData, navigate: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("الشهر") }
    val today = LocalDate.now()
    val week = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val month = today.withDayOfMonth(1)
    val since = when (filter) { "اليوم" -> today; "الأسبوع" -> week; "السنة" -> today.withDayOfYear(1); else -> month }
    val payments = data.payments.filter { day(it.paidAt) >= since && day(it.paidAt) <= today }.sortedByDescending { it.paidAt }
    fun revenue(from: LocalDate) = data.payments.filter { day(it.paidAt) >= from && day(it.paidAt) <= today }.sumOf { it.amount }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Section("المالية", "الإحصائيات", { navigate("stats") }) }
        item { Panel { Text("إيرادات هذا الشهر", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(money(revenue(month)), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.secondary); Info("إيرادات اليوم", money(revenue(today))); Info("إيرادات الأسبوع", money(revenue(week))); Info("المبالغ المنتظرة", money(data.due.sumOf { it.student.price })); TextButton({ navigate("due") }) { Text("عرض الطلاب الذين يجب عليهم الدفع") } } }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("اليوم", "الأسبوع", "الشهر", "السنة").forEach { f -> FilterChip(filter == f, { filter = f }, label = { Text(f) }) } } }
        item { Text("${payments.size} دفعات • ${money(payments.sumOf { it.amount })}", style = MaterialTheme.typography.titleMedium) }
        if (payments.isEmpty()) item { EmptyState("لا توجد دفعات في هذه الفترة", "تُحتسب الإيرادات من الدفعات المسجلة فقط.") }
        items(payments, key = { it.id }) { p -> Panel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(data.students.find { it.id == p.studentId }?.name ?: "طالب", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(money(p.amount), color = MaterialTheme.colorScheme.secondary) }; Text("${dateText(p.paidAt)} • ${p.method} • ${p.capacity} حصص", style = MaterialTheme.typography.bodySmall); TextButton({ navigate("profile/${p.studentId}") }) { Text("فتح الملف") } } }
    }
}

@Composable fun StatisticsScreen(data: TeacherData) {
    val active = data.active
    val month = YearMonth.now()
    Page {
        Text("الإحصائيات", style = MaterialTheme.typography.headlineMedium)
        Panel { Info("عدد الطلاب", active.size.toString()); Info("حصص هذا الشهر", data.lessons.count { YearMonth.from(day(it.occurredAt)) == month }.toString()); Info("عدد الدفعات • كل الفترات", data.payments.size.toString()); Info("الإيرادات • كل الفترات", money(data.payments.sumOf { it.amount })) }
        Section("الطلاب حسب المادة")
        Panel { Subject.entries.forEach { s -> Distribution(s.title, active.count { it.student.subject == s }, active.size) } }
        Section("الطلاب حسب الطور")
        Panel { EducationLevel.entries.forEach { l -> Distribution(l.title, active.count { it.student.level == l }, active.size) } }
        Section("الطلاب حسب السنة")
        Panel { EducationLevel.entries.forEach { l -> (1..l.years).forEach { y -> Distribution(yearLabel(l, y), active.count { it.student.level == l && it.student.year == y }, active.size) } } }
    }
}
@Composable private fun Distribution(label: String, value: Int, total: Int) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Info(label, value.toString()); LinearProgressIndicator(progress = { if (total == 0) 0f else value.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(10.dp), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant); Spacer(Modifier.height(8.dp)) } }

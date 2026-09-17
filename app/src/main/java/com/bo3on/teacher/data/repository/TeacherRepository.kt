package com.bo3on.teacher.data.repository

import androidx.room.withTransaction
import com.bo3on.teacher.data.database.TeacherDatabase
import com.bo3on.teacher.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.*

data class TeacherData(val schools: List<School> = emptyList(), val students: List<Student> = emptyList(), val groups: List<TeachingGroup> = emptyList(), val payments: List<Payment> = emptyList(), val lessons: List<Lesson> = emptyList(), val attendance: List<Attendance> = emptyList(), val notes: List<StudentNote> = emptyList()) {
    val summaries: List<StudentSummary> by lazy {
        val latest = payments.groupBy { it.studentId }.mapValues { it.value.maxBy { p -> p.id } }
        val counts = lessons.groupingBy { it.paymentId }.eachCount()
        students.map { s -> StudentSummary(s, latest[s.id], counts[latest[s.id]?.id] ?: 0) }
    }
    val active get() = summaries.filter { !it.student.archived }
    val due get() = active.filter { it.due }
    fun summary(id: Long) = summaries.find { it.student.id == id }
    fun school(id: Long) = schools.find { it.id == id }?.name ?: "مدرسة مؤرشفة"
}

@Serializable data class Backup(val version: Int = 1, val createdAt: Long = System.currentTimeMillis(), val schools: List<School>, val students: List<Student>, val groups: List<TeachingGroup>, val payments: List<Payment>, val lessons: List<Lesson>, val attendance: List<Attendance>, val notes: List<StudentNote>)

class TeacherRepository(val db: TeacherDatabase) {
    val dao = db.dao()
    val data: Flow<TeacherData> = combine(combine(dao.schools(), dao.students(), dao.groups(), dao.payments(), dao.lessons()) { s, st, g, p, l -> TeacherData(s, st, g, p, l) }, dao.attendance(), dao.notes()) { d, a, n -> d.copy(attendance = a, notes = n) }
    suspend fun saveSchool(school: School) { require(school.name.isNotBlank()) { "اسم المدرسة مطلوب" }; dao.school(school.copy(name = school.name.trim())) }
    suspend fun saveStudent(student: Student, note: String = "") = db.withTransaction {
        require(student.name.isNotBlank()) { "اسم الطالب مطلوب" }
        require(student.price in 1..100_000_000) { "أدخل سعرًا صحيحًا بالدينار" }
        require(student.year in 1..student.level.years) { "السنة الدراسية غير صحيحة" }
        require(dao.school(student.schoolId)?.archived == false) { "اختر مدرسة نشطة" }
        require(listOf(student.phone, student.parentPhone).all { it.isBlank() || it.matches(Regex("[+0-9 ()-]{6,20}")) }) { "رقم الهاتف غير صحيح" }
        student.groupId?.let { id ->
            val g = dao.group(id)
            require(g != null && !g.archived && g.schoolId == student.schoolId && g.subject == student.subject && g.level == student.level && g.year == student.year) { "المجموعة لا تطابق بيانات الطالب" }
        }
        val inserted = dao.student(student.copy(name = student.name.trim()))
        if (note.isNotBlank()) dao.note(StudentNote(studentId = if (student.id == 0L) inserted else student.id, text = note.trim()))
    }
    suspend fun saveGroup(group: TeachingGroup) = db.withTransaction {
        require(group.name.isNotBlank()) { "اسم المجموعة مطلوب" }
        require(group.year in 1..group.level.years) { "السنة غير صحيحة" }
        require(dao.school(group.schoolId)?.archived == false) { "اختر مدرسة نشطة" }
        LocalTime.parse(group.time)
        require(group.days.split(',').all { it.toIntOrNull() in 1..7 }) { "اختر أيام المجموعة" }
        val old = if (group.id > 0) dao.group(group.id) else null
        if (old != null && (old.schoolId != group.schoolId || old.subject != group.subject || old.level != group.level || old.year != group.year)) dao.detachGroup(group.id)
        dao.group(group.copy(name = group.name.trim()))
    }
    suspend fun archiveSchool(id: Long) = db.withTransaction { dao.archiveSchoolStudents(id); dao.archiveSchoolGroups(id); dao.archiveSchool(id, true) }
    suspend fun restoreStudent(id: Long) = db.withTransaction {
        dao.student(id)?.let { student ->
            dao.archiveSchool(student.schoolId, false)
            val group = student.groupId?.let { dao.group(it) }?.takeUnless { it.archived }
            dao.student(student.copy(archived = false, groupId = group?.id))
        }
    }
    suspend fun archiveGroup(id: Long) = db.withTransaction { dao.detachGroup(id); dao.archiveGroup(id) }

    suspend fun pay(studentId: Long, amount: Long, capacity: Int, date: Long, method: String) = db.withTransaction {
        require(dao.student(studentId)?.archived == false) { "الطالب غير نشط" }
        require(amount in 1..100_000_000 && capacity in 1..100) { "تحقق من المبلغ وعدد الحصص" }
        require(date <= System.currentTimeMillis() + 60_000) { "تاريخ الدفع لا يمكن أن يكون في المستقبل" }
        require(method in listOf("نقدًا", "بريدي موب", "تحويل", "أخرى"))
        dao.payment(Payment(studentId = studentId, amount = amount, capacity = capacity, paidAt = date, method = method))
    }
    suspend fun registerLesson(studentId: Long, time: Long = System.currentTimeMillis(), attendanceId: Long? = null): Boolean = db.withTransaction {
        val student = requireNotNull(dao.student(studentId)) { "الطالب غير موجود" }
        require(!student.archived) { "الطالب مؤرشف" }
        val payment = requireNotNull(dao.currentPayment(studentId)) { "سجّل دفعة لبدء الاشتراك أولًا" }
        val count = dao.packageLessons(payment.id).size
        require(count < payment.capacity) { "انتهت الحصص؛ سجّل دفعة جديدة" }
        require(time >= payment.paidAt) { "تاريخ الحصة يسبق بداية الاشتراك" }
        dao.lesson(Lesson(studentId = studentId, paymentId = payment.id, occurredAt = time, subject = student.subject, schoolName = requireNotNull(dao.school(student.schoolId)).name, level = student.level, year = student.year, number = count + 1, attendanceId = attendanceId))
        count + 1 == payment.capacity
    }
    suspend fun markAttendance(studentId: Long, groupId: Long, date: LocalDate, status: String): Boolean = db.withTransaction {
        require(status in listOf("حاضر", "غائب", "ملغاة"))
        require(date <= LocalDate.now()) { "لا يمكن تسجيل حضور مستقبلي" }
        val student = requireNotNull(dao.student(studentId))
        val group = requireNotNull(dao.group(groupId))
        require(!student.archived && !group.archived && student.groupId == groupId) { "الطالب ليس في هذه المجموعة" }
        val old = dao.attendanceFor(studentId, groupId, date.toString())
        if (old?.status == status) return@withTransaction false
        val a = Attendance(id = old?.id ?: 0, studentId = studentId, groupId = groupId, date = date.toString(), status = status)
        val inserted = dao.attendance(a)
        val id = old?.id ?: inserted
        dao.attendanceLesson(id)?.let { deleteLesson(it.id, false) }
        val occurredAt = if (date == LocalDate.now()) System.currentTimeMillis() else date.atTime(LocalTime.parse(group.time)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (status == "حاضر") registerLesson(studentId, occurredAt, id) else false
    }
    suspend fun deleteLesson(id: Long, clearAttendance: Boolean = true) = db.withTransaction {
        val lesson = dao.lesson(id) ?: return@withTransaction
        dao.deleteLesson(id)
        if (clearAttendance) lesson.attendanceId?.let { aid ->
            val old = dao.attendance().first().find { it.id == aid }
            if (old != null) dao.attendance(old.copy(status = "ملغاة"))
        }
        renumber(lesson.paymentId)
    }
    suspend fun editLesson(id: Long, time: Long) = db.withTransaction {
        val l = requireNotNull(dao.lesson(id))
        val p = dao.payments().first().first { it.id == l.paymentId }
        require(time >= p.paidAt && time <= System.currentTimeMillis()) { "اختر تاريخًا بين بداية الاشتراك والآن" }
        require(l.attendanceId == null) { "هذه الحصة مرتبطة بالحضور؛ صحّحها من صفحة اليوم" }
        dao.editLesson(id, time); renumber(l.paymentId)
    }
    private suspend fun renumber(paymentId: Long) { dao.packageLessons(paymentId).forEachIndexed { index, l -> dao.numberLesson(l.id, index + 1) } }
    suspend fun saveNote(note: StudentNote) { require(note.text.isNotBlank()) { "اكتب الملاحظة أولًا" }; dao.note(note.copy(text = note.text.trim())) }

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    suspend fun backup(): String = db.withTransaction {
        val s = dao.schools().first(); val st = dao.students().first(); val g = dao.groups().first()
        val p = dao.payments().first(); val l = dao.lessons().first(); val a = dao.attendance().first(); val n = dao.notes().first()
        json.encodeToString(Backup.serializer(), Backup(schools = s, students = st, groups = g, payments = p, lessons = l, attendance = a, notes = n))
    }
    suspend fun restore(raw: String) {
        require(raw.length <= 50_000_000) { "حجم النسخة أكبر من الحد المسموح" }
        val b = json.decodeFromString<Backup>(raw)
        require(b.version == 1) { "إصدار النسخة غير مدعوم" }
        fun ids(xs: List<Long>) = xs.all { it > 0 } && xs.distinct().size == xs.size
        require(listOf(b.schools.map { it.id }, b.students.map { it.id }, b.groups.map { it.id }, b.payments.map { it.id }, b.lessons.map { it.id }, b.attendance.map { it.id }, b.notes.map { it.id }).all { ids(it) }) { "معرّفات النسخة غير صالحة" }
        val schools = b.schools.associateBy { it.id }; val students = b.students.associateBy { it.id }; val groups = b.groups.associateBy { it.id }; val payments = b.payments.associateBy { it.id }; val attendance = b.attendance.associateBy { it.id }
        require(b.schools.all { it.name.isNotBlank() })
        require(b.groups.all { g -> g.schoolId in schools && g.name.isNotBlank() && g.year in 1..g.level.years && runCatching { LocalTime.parse(g.time) }.isSuccess && g.days.split(',').all { it.toIntOrNull() in 1..7 } })
        require(b.students.all { s -> s.schoolId in schools && s.name.isNotBlank() && s.price in 1..100_000_000 && s.year in 1..s.level.years && (s.groupId == null || groups[s.groupId]?.let { it.schoolId == s.schoolId && it.subject == s.subject && it.level == s.level && it.year == s.year } == true) })
        require(b.payments.all { it.studentId in students && it.capacity in 1..100 && it.amount in 1..100_000_000 })
        require(b.attendance.all { it.studentId in students && it.groupId in groups && it.status in listOf("حاضر", "غائب", "ملغاة") && runCatching { LocalDate.parse(it.date) }.isSuccess })
        require(b.notes.all { it.studentId in students && it.text.isNotBlank() })
        require(b.lessons.all { l -> l.studentId in students && payments[l.paymentId]?.studentId == l.studentId && l.occurredAt >= payments.getValue(l.paymentId).paidAt && (l.attendanceId == null || attendance[l.attendanceId]?.let { it.studentId == l.studentId && it.status == "حاضر" } == true) })
        require(b.lessons.groupBy { it.paymentId }.all { (id, lessons) -> lessons.size <= payments.getValue(id).capacity && lessons.map { it.number }.sorted() == (1..lessons.size).toList() })
        db.withTransaction {
            dao.clearLessons(); dao.clearAttendance(); dao.clearPayments(); dao.clearNotes(); dao.clearStudents(); dao.clearGroups(); dao.clearSchools()
            b.schools.forEach { dao.school(it) }; b.groups.forEach { dao.group(it) }; b.students.forEach { dao.student(it) }; b.payments.forEach { dao.payment(it) }; b.attendance.forEach { dao.attendance(it) }; b.lessons.forEach { dao.lesson(it) }; b.notes.forEach { dao.note(it) }
        }
    }
    suspend fun csv(): String {
        val d = data.first()
        fun cell(value: Any) = "\"${value.toString().replace("\"", "\"\"").let { if (it.startsWith('=') || it.startsWith('+') || it.startsWith('-') || it.startsWith('@')) "'$it" else it }}\""
        return "\uFEFFالاسم,الهاتف,ولي الأمر,المدرسة,المادة,السنة,السعر,الحصص,الحالة\r\n" + d.summaries.joinToString("\r\n") { s -> listOf(s.student.name, s.student.phone, s.student.parentPhone, d.school(s.student.schoolId), s.student.subject.title, yearLabel(s.student.level, s.student.year), s.student.price, "${s.count}/${s.capacity}", if (s.student.archived) "مؤرشف" else s.status).joinToString(",") { cell(it) } }
    }
}

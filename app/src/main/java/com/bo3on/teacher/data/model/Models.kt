package com.bo3on.teacher.data.model

import androidx.room.*
import kotlinx.serialization.Serializable

@Serializable enum class Subject(val title: String) { MATH("الرياضيات"), PHYSICS("الفيزياء") }
@Serializable enum class EducationLevel(val title: String, val years: Int) { MIDDLE("المتوسط", 4), SECONDARY("الثانوي", 3) }
fun yearLabel(level: EducationLevel, year: Int) = "$year ${if (level == EducationLevel.MIDDLE) "متوسط" else "ثانوي"}"

@Serializable @Entity(tableName = "schools")
data class School(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val note: String = "", val archived: Boolean = false)

@Serializable @Entity(tableName = "groups", foreignKeys = [ForeignKey(entity = School::class, parentColumns = ["id"], childColumns = ["schoolId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("schoolId")])
data class TeachingGroup(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val schoolId: Long, val subject: Subject, val level: EducationLevel, val year: Int, val days: String, val time: String, val archived: Boolean = false)

@Serializable @Entity(tableName = "students", foreignKeys = [ForeignKey(entity = School::class, parentColumns = ["id"], childColumns = ["schoolId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = TeachingGroup::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.SET_NULL)], indices = [Index("schoolId"), Index("groupId")])
data class Student(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val phone: String = "", val parentPhone: String = "", val schoolId: Long, val subject: Subject, val level: EducationLevel, val year: Int, val price: Long, val groupId: Long? = null, val registeredAt: Long = System.currentTimeMillis(), val avatar: String = "", val archived: Boolean = false)

@Serializable @Entity(tableName = "payments", foreignKeys = [ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId")])
data class Payment(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val amount: Long, val capacity: Int = 4, val paidAt: Long, val method: String, val createdAt: Long = System.currentTimeMillis())

@Serializable @Entity(tableName = "attendance", foreignKeys = [ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = TeachingGroup::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId"), Index("groupId"), Index(value = ["studentId", "groupId", "date"], unique = true)])
data class Attendance(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val groupId: Long, val date: String, val status: String)

@Serializable @Entity(tableName = "lessons", foreignKeys = [ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = Payment::class, parentColumns = ["id"], childColumns = ["paymentId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = Attendance::class, parentColumns = ["id"], childColumns = ["attendanceId"], onDelete = ForeignKey.SET_NULL)], indices = [Index("studentId"), Index("paymentId"), Index(value = ["attendanceId"], unique = true)])
data class Lesson(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val paymentId: Long, val occurredAt: Long, val subject: Subject, val schoolName: String, val level: EducationLevel, val year: Int, val number: Int, val attendanceId: Long? = null)

@Serializable @Entity(tableName = "notes", foreignKeys = [ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId")])
data class StudentNote(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val text: String, val createdAt: Long = System.currentTimeMillis())

@Serializable data class TeacherSettings(val theme: String = "system", val phone: String = "", val defaultPrice: Long = 4000, val packageSize: Int = 4, val lessonReminders: Boolean = true, val paymentReminders: Boolean = true, val completionAlert: Boolean = true, val biometric: Boolean = false, val pinHash: String = "", val pinSalt: String = "", val avatar: String = "", val failedAttempts: Int = 0, val lockedUntil: Long = 0)

data class StudentSummary(val student: Student, val payment: Payment?, val count: Int) {
    val capacity get() = payment?.capacity ?: 4
    val due get() = payment != null && count >= capacity
    val paid get() = payment != null && !due
    val status get() = if (due) "يجب الدفع" else if (paid) "مدفوع" else "لم يبدأ الاشتراك"
}

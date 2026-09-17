package com.bo3on.teacher.data.database

import android.content.Context
import androidx.room.*
import com.bo3on.teacher.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao interface TeacherDao {
    @Query("SELECT * FROM schools ORDER BY name") fun schools(): Flow<List<School>>
    @Query("SELECT * FROM students ORDER BY registeredAt DESC") fun students(): Flow<List<Student>>
    @Query("SELECT * FROM groups ORDER BY time") fun groups(): Flow<List<TeachingGroup>>
    @Query("SELECT * FROM payments ORDER BY id DESC") fun payments(): Flow<List<Payment>>
    @Query("SELECT * FROM lessons ORDER BY occurredAt DESC") fun lessons(): Flow<List<Lesson>>
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") fun notes(): Flow<List<StudentNote>>
    @Query("SELECT * FROM attendance") fun attendance(): Flow<List<Attendance>>
    @Query("SELECT * FROM students WHERE id=:id") suspend fun student(id: Long): Student?
    @Query("SELECT * FROM schools WHERE id=:id") suspend fun school(id: Long): School?
    @Query("SELECT * FROM groups WHERE id=:id") suspend fun group(id: Long): TeachingGroup?
    @Query("SELECT * FROM payments WHERE studentId=:id ORDER BY id DESC LIMIT 1") suspend fun currentPayment(id: Long): Payment?
    @Query("SELECT * FROM lessons WHERE paymentId=:id ORDER BY occurredAt, id") suspend fun packageLessons(id: Long): List<Lesson>
    @Query("SELECT * FROM attendance WHERE studentId=:student AND groupId=:group AND date=:date") suspend fun attendanceFor(student: Long, group: Long, date: String): Attendance?
    @Query("SELECT * FROM lessons WHERE attendanceId=:id") suspend fun attendanceLesson(id: Long): Lesson?
    @Query("SELECT * FROM lessons WHERE id=:id") suspend fun lesson(id: Long): Lesson?
    @Upsert suspend fun school(value: School): Long
    @Upsert suspend fun student(value: Student): Long
    @Upsert suspend fun group(value: TeachingGroup): Long
    @Insert suspend fun payment(value: Payment): Long
    @Insert suspend fun lesson(value: Lesson): Long
    @Upsert suspend fun attendance(value: Attendance): Long
    @Upsert suspend fun note(value: StudentNote): Long
    @Query("UPDATE lessons SET occurredAt=:time WHERE id=:id") suspend fun editLesson(id: Long, time: Long)
    @Query("UPDATE lessons SET number=:number WHERE id=:id") suspend fun numberLesson(id: Long, number: Int)
    @Query("DELETE FROM lessons WHERE id=:id") suspend fun deleteLesson(id: Long)
    @Query("DELETE FROM notes WHERE id=:id") suspend fun deleteNote(id: Long)
    @Query("UPDATE students SET archived=:archived WHERE id=:id") suspend fun archiveStudent(id: Long, archived: Boolean)
    @Query("UPDATE schools SET archived=:archived WHERE id=:id") suspend fun archiveSchool(id: Long, archived: Boolean)
    @Query("UPDATE students SET archived=1 WHERE schoolId=:id") suspend fun archiveSchoolStudents(id: Long)
    @Query("UPDATE groups SET archived=1 WHERE schoolId=:id") suspend fun archiveSchoolGroups(id: Long)
    @Query("UPDATE students SET groupId=NULL WHERE groupId=:id") suspend fun detachGroup(id: Long)
    @Query("UPDATE groups SET archived=1 WHERE id=:id") suspend fun archiveGroup(id: Long)
    @Query("DELETE FROM lessons") suspend fun clearLessons()
    @Query("DELETE FROM attendance") suspend fun clearAttendance()
    @Query("DELETE FROM payments") suspend fun clearPayments()
    @Query("DELETE FROM notes") suspend fun clearNotes()
    @Query("DELETE FROM students") suspend fun clearStudents()
    @Query("DELETE FROM groups") suspend fun clearGroups()
    @Query("DELETE FROM schools") suspend fun clearSchools()
}

@Database(entities = [School::class, Student::class, TeachingGroup::class, Payment::class, Lesson::class, Attendance::class, StudentNote::class], version = 1, exportSchema = true)
abstract class TeacherDatabase : RoomDatabase() {
    abstract fun dao(): TeacherDao
    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, TeacherDatabase::class.java, "bo3on.db").build()
    }
}

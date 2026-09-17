package com.bo3on.teacher

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bo3on.teacher.data.database.TeacherDatabase
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.data.repository.*
import com.bo3on.teacher.utils.start
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RepositoryTest {
    private lateinit var db: TeacherDatabase
    private lateinit var repo: TeacherRepository
    private val today get() = start(LocalDate.now())
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TeacherDatabase::class.java).allowMainThreadQueries().build()
        repo = TeacherRepository(db)
        repo.saveSchool(School(1, "ثانوية ابن خلدون"))
        repo.saveStudent(Student(1, "محمد أمين", schoolId = 1, subject = Subject.MATH, level = EducationLevel.SECONDARY, year = 2, price = 4000))
    }
    @After fun cleanup() { db.close() }
    private suspend fun pay(capacity: Int = 4) = repo.pay(1, 4000, capacity, today, "نقدًا")
    private suspend fun progress() = repo.data.first().summary(1)!!
    private suspend fun rejected(block: suspend () -> Unit) { try { block(); fail("Expected validation failure") } catch (_: IllegalArgumentException) {} }

    @Test fun fourLessonsRequireRenewalAndNewPaymentPreservesHistory() = runBlocking {
        pay()
        assertEquals(0, progress().count)
        repeat(3) { assertFalse(repo.registerLesson(1)) }
        assertTrue(repo.registerLesson(1))
        assertTrue(progress().due)
        rejected { repo.registerLesson(1) }
        pay()
        assertEquals(0, progress().count)
        assertTrue(progress().paid)
        val data = repo.data.first()
        assertEquals(4, data.lessons.size)
        assertEquals(2, data.payments.size)
    }
    @Test fun packageCapacityIsSnapshotAndOldLessonDeletionDoesNotTouchNewPackage() = runBlocking {
        pay(); repeat(4) { repo.registerLesson(1) }
        val old = repo.data.first().lessons.first()
        pay(6); repo.registerLesson(1)
        repo.deleteLesson(old.id)
        assertEquals(1, progress().count)
        assertEquals(6, progress().capacity)
        val previous = repo.data.first().lessons.filter { it.paymentId == old.paymentId }
        assertEquals(listOf(1, 2, 3), previous.map { it.number }.sorted())
    }
    @Test fun currentLessonDeletionReopensExhaustedPackage() = runBlocking {
        pay(); repeat(4) { repo.registerLesson(1) }
        repo.deleteLesson(repo.data.first().lessons.first().id)
        assertEquals(3, progress().count); assertFalse(progress().due)
        assertTrue(repo.registerLesson(1))
    }
    @Test fun noUnpaidLessonsAndValidationProtectsData() = runBlocking {
        rejected { repo.registerLesson(1) }
        rejected { repo.pay(1, 0, 4, today, "نقدًا") }
        rejected { repo.pay(1, 4000, 0, today, "نقدًا") }
        rejected { repo.saveStudent(repo.dao.student(1)!!.copy(year = 4)) }
        rejected { repo.saveStudent(repo.dao.student(1)!!.copy(schoolId = 99)) }
        assertEquals(0, repo.data.first().payments.size)
    }
    @Test fun simultaneousTapsCannotExceedCapacity() = runBlocking {
        pay()
        val results = (1..10).map { async(Dispatchers.IO) { runCatching { repo.registerLesson(1) }.isSuccess } }.awaitAll()
        assertEquals(4, results.count { it }); assertEquals(4, progress().count)
    }
    @Test fun attendanceIsIdempotentAndCorrectionsAdjustProgress() = runBlocking {
        val group = TeachingGroup(1, "المجموعة أ", 1, Subject.MATH, EducationLevel.SECONDARY, 2, "1,3", "17:00")
        repo.saveGroup(group); repo.saveStudent(repo.dao.student(1)!!.copy(groupId = 1)); pay()
        repo.markAttendance(1, 1, LocalDate.now(), "حاضر")
        repo.markAttendance(1, 1, LocalDate.now(), "حاضر")
        assertEquals(1, progress().count)
        repo.markAttendance(1, 1, LocalDate.now(), "غائب")
        assertEquals(0, progress().count)
        repo.markAttendance(1, 1, LocalDate.now(), "حاضر")
        assertEquals(1, progress().count)
        repo.deleteLesson(repo.data.first().lessons.single().id)
        assertEquals("ملغاة", repo.data.first().attendance.single().status)
    }
    @Test fun backupRoundTripAndMalformedRestoreNeverErasesData() = runBlocking {
        pay(); repo.registerLesson(1); repo.saveNote(StudentNote(studentId = 1, text = "تحسن مستواه"))
        val raw = repo.backup()
        repo.registerLesson(1)
        repo.restore(raw)
        assertEquals(1, progress().count); assertEquals(1, repo.data.first().notes.size)
        rejected { repo.restore(raw.replace("\"schoolId\":1", "\"schoolId\":99")) }
        assertEquals(1, progress().count)
        assertEquals(1, repo.data.first().payments.size)
    }
    @Test fun archiveAndRestoreRetainAllFinancialHistory() = runBlocking {
        pay(); repo.registerLesson(1)
        repo.archiveSchool(1)
        assertTrue(repo.data.first().active.isEmpty())
        assertEquals(1, repo.data.first().payments.size)
        repo.restoreStudent(1)
        assertEquals(1, repo.data.first().active.size)
        assertFalse(repo.dao.school(1)!!.archived)
        assertEquals(1, progress().count)
    }
    @Test fun studentAndSchoolEditingPreserveLessonSnapshots() = runBlocking {
        pay(); repo.registerLesson(1)
        repo.saveSchool(School(1, "ثانوية جديدة", "ملاحظة"))
        repo.saveStudent(repo.dao.student(1)!!.copy(name = "محمد أمين بوعلام", year = 3))
        val data = repo.data.first()
        assertEquals("ثانوية جديدة", data.schools.single().name)
        assertEquals("محمد أمين بوعلام", data.students.single().name)
        assertEquals("ثانوية ابن خلدون", data.lessons.single().schoolName)
        assertEquals(2, data.lessons.single().year)
    }
    @Test fun fileDatabaseSurvivesCloseAndReopen(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        var fileDb = Room.databaseBuilder(context, TeacherDatabase::class.java, name).allowMainThreadQueries().build()
        var r = TeacherRepository(fileDb)
        r.saveSchool(School(1, "متوسطة الأمير عبد القادر"))
        r.saveStudent(Student(1, "إيمان", schoolId = 1, subject = Subject.PHYSICS, level = EducationLevel.MIDDLE, year = 4, price = 3000))
        r.pay(1, 3000, 4, today, "بريدي موب"); r.registerLesson(1)
        fileDb.close()
        fileDb = Room.databaseBuilder(context, TeacherDatabase::class.java, name).allowMainThreadQueries().build(); r = TeacherRepository(fileDb)
        assertEquals(1, r.data.first().summary(1)!!.count)
        assertEquals(3000L, r.data.first().payments.single().amount)
        fileDb.close(); context.deleteDatabase(name)
    }
    @Test fun pinIsSaltedAndVerified() = runBlocking {
        val store = SettingsStore(ApplicationProvider.getApplicationContext())
        store.setPin("1234")
        val s = store.flow.first()
        assertNotEquals("1234", s.pinHash)
        assertTrue(SettingsStore.verify("1234", s))
        assertFalse(SettingsStore.verify("9999", s))
        store.setPin("1234")
        assertNotEquals(s.pinHash, store.flow.first().pinHash)
    }
}

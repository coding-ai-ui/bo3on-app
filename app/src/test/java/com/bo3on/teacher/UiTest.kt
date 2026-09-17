package com.bo3on.teacher

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.data.repository.TeacherRepository
import com.bo3on.teacher.utils.start
import com.bo3on.teacher.ui.viewmodel.TeacherViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "ar-rDZ-ldrtl-w412dp-h915dp-mdpi", application = Bo3onApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class UiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<Bo3onApplication>()
    private val repo get() = app.repo
    @Before fun seed() = runBlocking {
        withContext(Dispatchers.IO) {
            app.database.clearAllTables()
            app.settings.update { TeacherSettings(theme = "light") }
            repo.saveSchool(School(1, "ثانوية ابن خلدون", "حصص الرياضيات والفيزياء"))
            repo.saveSchool(School(2, "متوسطة الأمير عبد القادر"))
            repo.saveGroup(TeachingGroup(1, "المجموعة أ", 1, Subject.MATH, EducationLevel.SECONDARY, 2, LocalDate.now().dayOfWeek.value.toString(), "17:00"))
            repo.saveStudent(Student(1, "محمد أمين", "0550123456", schoolId = 1, subject = Subject.MATH, level = EducationLevel.SECONDARY, year = 2, price = 4000, groupId = 1))
            repo.saveStudent(Student(2, "سارة بن علي", schoolId = 1, subject = Subject.PHYSICS, level = EducationLevel.SECONDARY, year = 3, price = 4000))
            repo.saveStudent(Student(3, "إيمان", schoolId = 2, subject = Subject.MATH, level = EducationLevel.MIDDLE, year = 4, price = 3000))
            repo.pay(1, 4000, 4, start(LocalDate.now()), "نقدًا")
            repo.pay(2, 4000, 4, start(LocalDate.now()), "بريدي موب")
            repeat(3) { repo.registerLesson(1) }
            repeat(4) { repo.registerLesson(2) }
            repo.saveNote(StudentNote(studentId = 1, text = "تحسن مستواه في المعادلات، يحتاج إلى مراجعة الهندسة."))
            assertEquals(3, repo.data.first().summary(1)!!.count)
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("مرحبًا أستاذ بوعون 👋").fetchSemanticsNodes().isNotEmpty() }
        val vm = ViewModelProvider(compose.activity)[TeacherViewModel::class.java]
        compose.waitUntil(15_000) { vm.data.value.summary(1)?.count == 3 && vm.data.value.lessons.size == 7 }
        compose.waitForIdle()
    }
    private fun shot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.runOnIdle {
            val view = compose.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val out = File("build/reports/screenshots/$name.png"); out.parentFile!!.mkdirs()
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @After fun close() {
        compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        compose.waitForIdle()
        app.database.close()
    }
    @Test fun dashboardAndDarkModeRenderInRtl() {
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL, compose.activity.resources.configuration.layoutDirection)
        shot("01-home-light")
        runBlocking { app.settings.update { it.copy(theme = "dark") } }
        compose.waitForIdle()
        shot("02-home-dark")
    }
    @Test fun bottomNavigationSearchProfileAndHistory() {
        compose.onNodeWithText("الطلاب", useUnmergedTree = true).performClick()
        compose.onNodeWithText("ابحث عن طالب...").performTextInput("محمد")
        compose.onNodeWithText("محمد أمين").assertExists()
        compose.onNodeWithText("فتح الملف").performClick()
        compose.onNodeWithText("الحصص الحالية").assertExists()
        compose.onNodeWithText("3 من 4").assertExists()
        shot("03-student-profile")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("سجل الدفعات"))
        compose.onNodeWithText("سجل الدفعات").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("3 / 4 حصص • الاشتراك الحالي"))
        compose.onNodeWithText("3 / 4 حصص • الاشتراك الحالي").assertExists()
        compose.onNodeWithText("المالية", useUnmergedTree = true).performClick()
        compose.onNodeWithText("إيرادات هذا الشهر").assertExists()
        shot("04-finance")
        compose.onNodeWithContentDescription("اليوم").performClick()
        compose.onNodeWithText("المجموعة أ").assertExists()
        shot("05-today")
    }
    @Test fun hierarchyAndStudentFormNavigation() {
        compose.onNodeWithText("ثانوية ابن خلدون").performScrollTo().performClick()
        compose.onNodeWithText("الرياضيات", useUnmergedTree = true).performClick()
        compose.onNodeWithText("الثانوي").performClick()
        compose.onNodeWithText("2 ثانوي").performClick()
        compose.onNodeWithText("محمد أمين").assertExists()
        shot("06-year-students")
        compose.onNodeWithText("+ إضافة طالب").performClick()
        compose.onNodeWithText("الاسم الكامل *").assertExists()
        shot("07-student-form")
    }
    @Test fun fourthLessonDialogAndPaymentRenewal() {
        compose.onNodeWithText("الطلاب", useUnmergedTree = true).performClick()
        compose.onNodeWithText("ابحث عن طالب...").performTextInput("محمد")
        compose.onNodeWithText("فتح الملف").performClick()
        compose.onNodeWithText("تسجيل حصة").performClick()
        compose.onNodeWithText("تسجيل", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("اكتملت حصص الاشتراك").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("تسجيل الدفع الآن").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("تأكيد الدفع").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(10_000) { compose.onAllNodesWithText("0 من 4").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val data = repo.data.first()
            assertEquals(4, data.lessons.count { it.studentId == 1L })
            assertEquals(2, data.payments.count { it.studentId == 1L })
        }
    }
    @Test fun addAndArchiveSchoolThroughUi() {
        compose.onNodeWithText("+ إضافة مدرسة").performScrollTo().performClick()
        compose.onNodeWithText("اسم المدرسة *").performTextInput("مدرسة الاختبار")
        compose.onNodeWithText("حفظ المدرسة").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("مرحبًا أستاذ بوعون 👋").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("مدرسة الاختبار").performScrollTo().performClick()
        compose.onNodeWithText("حذف المدرسة").performClick()
        compose.onNodeWithText("نقل للأرشيف").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("مرحبًا أستاذ بوعون 👋").fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            val data = repo.data.first()
            assertTrue(data.schools.single { it.name == "مدرسة الاختبار" }.archived)
        }
    }
}

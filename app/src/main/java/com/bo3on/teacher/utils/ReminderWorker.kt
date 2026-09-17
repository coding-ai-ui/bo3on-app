package com.bo3on.teacher.utils

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bo3on.teacher.Bo3onApplication
import com.bo3on.teacher.MainActivity
import com.bo3on.teacher.R
import kotlinx.coroutines.flow.first
import java.time.*

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as Bo3onApplication
        val settings = app.settings.flow.first()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = app.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("lessons", "تذكيرات الأستاذ بوعون", NotificationManager.IMPORTANCE_DEFAULT))
        val data = app.repo.data.first()
        val today = LocalDate.now()
        val prefs = app.getSharedPreferences("delivered_notifications", Context.MODE_PRIVATE)
        val intent = PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun notify(key: String, title: String, body: String) {
            if (prefs.getString(key, "") == today.toString()) return
            manager.notify(key.hashCode(), NotificationCompat.Builder(app, "lessons").setSmallIcon(R.drawable.ic_logo).setContentTitle(title).setContentText(body).setContentIntent(intent).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
            prefs.edit().putString(key, today.toString()).apply()
        }
        if (settings.paymentReminders && data.due.isNotEmpty() && LocalTime.now().hour >= 9) notify("payment", "تذكير الدفعات", "هناك ${data.due.size} طلاب أكملوا حصصهم. افتح التطبيق للمراجعة")
        if (settings.lessonReminders) data.groups.filter { !it.archived && today.dayOfWeek.value.toString() in it.days.split(',') }.forEach { group ->
            val minutes = Duration.between(LocalTime.now(), LocalTime.parse(group.time)).toMinutes()
            if (minutes in 0..60 && data.active.any { it.student.groupId == group.id }) notify("group-${group.id}", "حصة قادمة • ${group.time}", "${group.name} • ${group.subject.title}")
        }
        return Result.success()
    }
}

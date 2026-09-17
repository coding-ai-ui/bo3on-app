package com.bo3on.teacher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

fun money(amount: Long) = NumberFormat.getIntegerInstance(Locale.US).format(amount) + " دج"
fun dateText(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ar")))
fun timeText(time: Long): String = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
fun day(time: Long): LocalDate = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
fun start(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
val dayNames = mapOf(7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة", 6 to "السبت")
fun daysText(days: String) = days.split(',').mapNotNull { dayNames[it.toIntOrNull()] }.joinToString("، ")
fun contact(context: Context, phone: String, whatsapp: Boolean) {
    require(phone.isNotBlank()) { "أضف رقم الهاتف في بيانات الطالب أولًا" }
    var normalized = phone.filter { it.isDigit() || it == '+' }
    if (normalized.startsWith("00")) normalized = normalized.drop(2)
    if (whatsapp && normalized.startsWith('0') && normalized.length == 10) normalized = "213" + normalized.drop(1)
    val uri = if (whatsapp) Uri.parse("https://wa.me/${normalized.trimStart('+')}") else Uri.parse("tel:$normalized")
    try { context.startActivity(Intent(if (whatsapp) Intent.ACTION_VIEW else Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: android.content.ActivityNotFoundException) { error("لا يوجد تطبيق مناسب لفتح الاتصال") }
}
fun readAvatar(context: Context, uri: Uri): String {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
    require(opts.outWidth > 0) { "اختر صورة صالحة" }
    opts.inJustDecodeBounds = false
    opts.inSampleSize = (maxOf(opts.outWidth, opts.outHeight) / 384).coerceAtLeast(1)
    val bmp = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) } ?: error("تعذر قراءة الصورة")
    val scale = 256f / maxOf(bmp.width, bmp.height)
    val small = Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true)
    val out = ByteArrayOutputStream(); small.compress(Bitmap.CompressFormat.JPEG, 85, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

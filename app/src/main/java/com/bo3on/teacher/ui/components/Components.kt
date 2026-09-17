package com.bo3on.teacher.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.bo3on.teacher.R
import com.bo3on.teacher.data.model.*

val Navy = Color(0xFF142C49)
val Emerald = Color(0xFF00866A)
val DueRed = Color(0xFFC33E4C)
val Amber = Color(0xFFAD671B)
private val ArabicFont = FontFamily(Font(R.font.tajawal_regular), Font(R.font.tajawal_bold, FontWeight.Bold))

@Composable fun Bo3onTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "dark" || mode == "system" && isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(primary = Color(0xFF9CC6FF), primaryContainer = Color(0xFF2B4666), onPrimaryContainer = Color(0xFFDCE8F5), secondary = Color(0xFF5CDBB8), secondaryContainer = Color(0xFF1B403A), onSecondaryContainer = Color(0xFFB5F1E0), background = Color(0xFF0D1624), surface = Color(0xFF152233), surfaceVariant = Color(0xFF243448), error = Color(0xFFFFA7AE)) else lightColorScheme(primary = Navy, primaryContainer = Color(0xFFDCE8F5), onPrimaryContainer = Navy, secondary = Emerald, secondaryContainer = Color(0xFFE0F3ED), onSecondaryContainer = Color(0xFF005340), background = Color(0xFFF4F6FA), surface = Color.White, surfaceVariant = Color(0xFFE8EDF3), error = DueRed)
    val base = Typography()
    val typography = Typography(displaySmall = base.displaySmall.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), headlineLarge = base.headlineLarge.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), headlineMedium = base.headlineMedium.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), titleLarge = base.titleLarge.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), titleMedium = base.titleMedium.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), titleSmall = base.titleSmall.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), bodyLarge = base.bodyLarge.copy(fontFamily = ArabicFont, lineHeight = 26.sp), bodyMedium = base.bodyMedium.copy(fontFamily = ArabicFont, lineHeight = 22.sp), bodySmall = base.bodySmall.copy(fontFamily = ArabicFont), labelLarge = base.labelLarge.copy(fontFamily = ArabicFont, fontWeight = FontWeight.Bold), labelMedium = base.labelMedium.copy(fontFamily = ArabicFont), labelSmall = base.labelSmall.copy(fontFamily = ArabicFont))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)), content = content)
    }
}
@Composable fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable fun Section(title: String, action: String? = null, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null) TextButton(onClick) { Text(action) }
    }
}
@Composable fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}
@Composable fun EmptyState(title: String, detail: String, action: String? = null, onClick: () -> Unit = {}) {
    Panel { Icon(Icons.Default.AutoStories, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary); Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant); if (action != null) Button(onClick) { Text(action) } }
}
@Composable fun Avatar(name: String, encoded: String = "", size: Dp = 48.dp) {
    val bitmap = remember(encoded) { runCatching { if (encoded.isEmpty()) null else Base64.decode(encoded, Base64.NO_WRAP).let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }.getOrNull() }
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.trim().take(1), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
@Composable fun Stat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary, onClick: (() -> Unit)? = null) {
    Card(modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(24.dp), tint = color)
            Text(value, style = if (value.contains("دج")) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, maxLines = 2)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun Status(summary: StudentSummary) {
    val color = if (summary.due) MaterialTheme.colorScheme.error else if (summary.paid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = color.copy(alpha = .1f), shape = CircleShape) { Text(summary.status, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
}
@Composable fun StudentCard(s: StudentSummary, subtitle: String = "", onOpen: () -> Unit, onLesson: () -> Unit, onPayment: () -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(s.student.name, s.student.avatar)
            Column(Modifier.weight(1f)) { Text(s.student.name, style = MaterialTheme.typography.titleMedium); if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Status(s)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الحصص الحالية", style = MaterialTheme.typography.bodySmall); Text("${s.count} / ${s.capacity} حصص", style = MaterialTheme.typography.labelLarge) }
        LinearProgressIndicator(progress = { s.count.toFloat() / s.capacity }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = if (s.due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!s.student.archived) FilledTonalButton(if (s.paid) onLesson else onPayment, modifier = Modifier.weight(1f)) { Icon(if (s.paid) Icons.Default.Add else Icons.Default.Payments, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (s.paid) "حصة" else "تسجيل دفعة") }
            TextButton(onOpen, Modifier.weight(1f)) { Text("فتح الملف") }
        }
    }
}
@Composable fun Confirm(title: String, text: String, confirm: String = "تأكيد", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) }, confirmButton = { TextButton(onConfirm) { Text(confirm) } }, dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}
@Composable fun <T> Choice(label: String, value: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(text(value)); Icon(Icons.Default.ExpandMore, null) } }
        }
        DropdownMenu(expanded, { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(text(option)) }, onClick = { onSelect(option); expanded = false }) } }
    }
}
@Composable fun Info(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value.ifBlank { "غير محدد" }, Modifier.weight(1f), textAlign = TextAlign.End) } }

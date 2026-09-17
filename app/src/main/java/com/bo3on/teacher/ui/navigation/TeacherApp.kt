package com.bo3on.teacher.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.bo3on.teacher.R
import com.bo3on.teacher.data.model.*
import com.bo3on.teacher.ui.components.*
import com.bo3on.teacher.ui.screens.*
import com.bo3on.teacher.ui.viewmodel.TeacherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TeacherApp(vm: TeacherViewModel, activity: FragmentActivity) {
    val ready by vm.ready.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val completed by vm.completed.collectAsStateWithLifecycle()
    var splash by remember { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { delay(650); splash = false }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    if (!ready || splash) {
        Surface(Modifier.fillMaxSize(), color = Navy) { Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(painterResource(R.drawable.ic_logo), null, Modifier.size(130.dp), tint = androidx.compose.ui.graphics.Color.Unspecified); Text("بوعون", style = MaterialTheme.typography.displaySmall, color = androidx.compose.ui.graphics.Color.White); Text("رياضيات • فيزياء", color = androidx.compose.ui.graphics.Color(0xFF8CE3CD)) } }
        return
    }
    if (!unlocked) { Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding -> Box(Modifier.padding(padding)) { LockScreen(vm, activity) } }; return }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    var fab by remember { mutableStateOf(false) }
    var lessonId by remember { mutableStateOf<Long?>(null) }
    var paymentId by remember { mutableStateOf<Long?>(null) }
    var archiveSchool by remember { mutableStateOf<Long?>(null) }
    val navigate: (String) -> Unit = { dest -> nav.navigate(dest) { launchSingleTop = true } }
    val openLesson: (Long) -> Unit = { id -> if (data.summary(id)?.paid == true) lessonId = id else paymentId = id }
    val openPayment: (Long) -> Unit = { paymentId = it }
    val mainRoutes = listOf("home", "today", "students", "finance", "settings")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("بوعون", style = MaterialTheme.typography.titleLarge) }, navigationIcon = { if (route !in mainRoutes) IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } else Icon(painterResource(R.drawable.ic_logo), null, Modifier.padding(start = 16.dp).size(36.dp), tint = androidx.compose.ui.graphics.Color.Unspecified) }, actions = { IconButton({ navigate("groups") }) { Icon(Icons.Default.Groups, "المجموعات") }; IconButton({ navigate("stats") }) { Icon(Icons.Default.BarChart, "الإحصائيات") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            val labels = listOf("الرئيسية", "اليوم", "الطلاب", "المالية", "الإعدادات")
            val icons = listOf(Icons.Default.Home, Icons.Default.CalendarMonth, Icons.Default.People, Icons.Default.AccountBalanceWallet, Icons.Default.Settings)
            mainRoutes.forEachIndexed { index, dest -> NavigationBarItem(selected = route == dest, onClick = { nav.navigate(dest) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(icons[index], labels[index]) }, label = { Text(labels[index], maxLines = 1) }) }
        } },
        floatingActionButton = { if (!route.contains("Edit") && route != "settings") FloatingActionButton({ fab = true }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) { Icon(Icons.Default.Add, "إضافة") } },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = "home") {
                composable("home") { Dashboard(data, settings, navigate, openLesson, openPayment) }
                composable("students") { StudentList(data, navigate, openLesson, openPayment) }
                composable("today") { TodayScreen(vm, navigate) }
                composable("finance") { FinanceScreen(data, navigate) }
                composable("settings") { SettingsScreen(vm, activity, navigate) }
                composable("stats") { StatisticsScreen(data) }
                composable("due") { DueScreen(data, navigate, openPayment) }
                composable("groups") { GroupsScreen(data, navigate) }
                composable("select/{action}") { StudentList(data, navigate, openLesson, openPayment, selectAction = it.arguments?.getString("action")) }
                composable("profile/{id}") { StudentProfile(vm, it.arguments!!.getString("id")!!.toLong(), navigate, openLesson, openPayment) }
                composable("studentEdit/{id}") { StudentForm(vm, it.arguments!!.getString("id")!!.toLong()) { nav.popBackStack() } }
                composable("schoolEdit/{id}") { SchoolForm(vm, it.arguments!!.getString("id")!!.toLong()) { nav.popBackStack() } }
                composable("groupEdit/{id}") { GroupForm(vm, it.arguments!!.getString("id")!!.toLong()) { nav.popBackStack() } }
                composable("group/{id}") { GroupDetails(vm, it.arguments!!.getString("id")!!.toLong(), navigate, openLesson, openPayment) { nav.popBackStack() } }
                composable("school/{id}") { val id = it.arguments!!.getString("id")!!.toLong(); BrowseSchool(data, id, null, null, navigate) { archiveSchool = id } }
                composable("level/{id}/{subject}") { BrowseSchool(data, it.arguments!!.getString("id")!!.toLong(), Subject.valueOf(it.arguments!!.getString("subject")!!), null, navigate) {} }
                composable("year/{id}/{subject}/{level}") { BrowseSchool(data, it.arguments!!.getString("id")!!.toLong(), Subject.valueOf(it.arguments!!.getString("subject")!!), EducationLevel.valueOf(it.arguments!!.getString("level")!!), navigate) {} }
                composable("students/{id}/{subject}/{level}/{year}") { StudentList(data, navigate, openLesson, openPayment, it.arguments!!.getString("id")!!.toLong(), Subject.valueOf(it.arguments!!.getString("subject")!!), EducationLevel.valueOf(it.arguments!!.getString("level")!!), it.arguments!!.getString("year")!!.toInt()) }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
    if (fab) ModalBottomSheet(onDismissRequest = { fab = false }) { Column(Modifier.padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("إجراء سريع", style = MaterialTheme.typography.titleLarge); listOf("إضافة طالب" to "studentEdit/0", "إضافة مدرسة" to "schoolEdit/0", "تسجيل حصة" to "select/lesson", "تسجيل دفعة" to "select/payment", "إضافة مجموعة" to "groupEdit/0").forEach { (label, route) -> TextButton({ fab = false; navigate(route) }, Modifier.fillMaxWidth()) { Text(label) } } } }
    lessonId?.let { id -> Confirm("تسجيل حصة جديدة؟", "تسجيل حصة جديدة لـ${data.summary(id)?.student?.name}؟", "تسجيل", { lessonId = null }) { vm.lesson(id); lessonId = null } }
    paymentId?.let { id -> PaymentDialog(vm, id) { paymentId = null } }
    completed?.let { id -> AlertDialog(onDismissRequest = { vm.completed.value = null }, title = { Text("اكتملت حصص الاشتراك") }, text = { Text("${data.summary(id)?.student?.name} أكمل ${data.summary(id)?.capacity ?: 4} حصص. يجب تسجيل دفعة جديدة قبل بدء اشتراك جديد.") }, confirmButton = { TextButton({ vm.completed.value = null; scope.launch { withFrameNanos { }; paymentId = id } }) { Text("تسجيل الدفع الآن") } }, dismissButton = { TextButton({ vm.completed.value = null }) { Text("لاحقًا") } }) }
    archiveSchool?.let { id -> Confirm("حذف المدرسة؟", "ستُؤرشف المدرسة وطلابها ومجموعاتها مع الاحتفاظ بجميع السجلات المالية والدراسية. يمكن استعادة المدرسة من الإعدادات والطلاب من الأرشيف.", "نقل للأرشيف", { archiveSchool = null }) { vm.action("تمت أرشفة المدرسة") { vm.repo.archiveSchool(id); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { nav.popBackStack() } }; archiveSchool = null } }
}

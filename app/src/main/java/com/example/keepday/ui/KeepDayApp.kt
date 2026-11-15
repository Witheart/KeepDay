package com.example.keepday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.calendar.CalendarReminder
import com.example.keepday.data.Habit
import com.example.keepday.data.HabitStore
import com.example.keepday.data.SettingsStore
import com.example.keepday.ui.detail.HabitDetailScreen
import com.example.keepday.ui.editor.HabitEditorDialog
import com.example.keepday.ui.settings.SettingsSheet
import com.example.keepday.ui.stats.StatsScreen
import com.example.keepday.ui.today.TodayScreen
import com.example.keepday.widget.TodayWidgetProvider
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class AppTab(val label: String, val glyph: String) {
    Today(label = "今日", glyph = "☀"),
    Stats(label = "统计", glyph = "◎")
}

@Composable
fun KeepDayApp() {
    val habits by HabitStore.habits.collectAsState()
    val pendingFirst by SettingsStore.pendingFirst.collectAsState()
    val calendarReminder by SettingsStore.calendarReminder.collectAsState()
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var tab by remember { mutableStateOf(AppTab.Today) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var editorTarget by remember { mutableStateOf<Habit?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var backfillTarget by remember { mutableStateOf<Habit?>(null) }

    fun applyCalendarReminder(hour: Int, minute: Int) {
        if (CalendarReminder.createOrUpdate(context, hour, minute)) {
            SettingsStore.updateCalendarReminder(hour, minute, true)
            scope.launch {
                snackbarHostState.showSnackbar("已在系统日历创建每天 $hour:${minute.toString().padStart(2, '0')} 的提醒")
            }
        } else {
            // 没有可写日历或权限异常：退回到系统日历的新建事件界面，由用户确认
            val opened = CalendarReminder.openInsertIntent(context, hour, minute)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (opened) "请在日历页面确认保存这条每日提醒"
                    else "没有可用的日历应用"
                )
            }
        }
    }

    var pendingCalendarTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        val time = pendingCalendarTime
        pendingCalendarTime = null
        when {
            granted && time != null -> applyCalendarReminder(time.first, time.second)
            !granted -> scope.launch {
                snackbarHostState.showSnackbar("未授予日历权限，无法创建提醒")
            }
        }
    }

    fun setCalendarReminder(hour: Int, minute: Int) {
        if (CalendarReminder.hasPermission(context)) {
            applyCalendarReminder(hour, minute)
        } else {
            pendingCalendarTime = hour to minute
            calendarPermissionLauncher.launch(CalendarReminder.permissions())
        }
    }

    fun removeCalendarReminder() {
        val removed = CalendarReminder.deleteExisting(context)
        SettingsStore.updateCalendarReminder(calendarReminder.hour, calendarReminder.minute, false)
        scope.launch {
            snackbarHostState.showSnackbar(if (removed) "已删除日历提醒" else "日历里没有找到提醒")
        }
    }

    // 和系统日历对齐一次状态：用户可能在日历应用里手动删掉了事件
    LaunchedEffect(Unit) {
        if (CalendarReminder.hasPermission(context)) {
            val exists = CalendarReminder.existingEventId(context) != null
            if (exists != calendarReminder.created) {
                SettingsStore.updateCalendarReminder(
                    calendarReminder.hour, calendarReminder.minute, exists
                )
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        val index = habits.indexOfFirst { it.id == habit.id }
        HabitStore.delete(habit.id)
        if (detailId == habit.id) detailId = null
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已删除「${habit.name}」",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                HabitStore.restore(habit, index)
            }
        }
    }

    // 当天全部完成时给一次正反馈，只在状态翻转时触发
    val doneToday = habits.count { it.isDone(today) }
    var celebrated by remember { mutableStateOf(false) }
    LaunchedEffect(doneToday, habits.size) {
        val allDone = habits.isNotEmpty() && doneToday == habits.size
        if (allDone && !celebrated) {
            celebrated = true
            snackbarHostState.showSnackbar(message = "🎉 今天的习惯全部完成，继续保持")
        } else if (!allDone) {
            celebrated = false
        }
    }

    // 数据变化后刷新桌面小组件
    LaunchedEffect(habits) {
        TodayWidgetProvider.updateAll(context)
    }

    val detailHabit = habits.firstOrNull { it.id == detailId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (detailHabit == null) {
                KeepDayBottomBar(current = tab, onSelect = { tab = it })
            }
        },
        floatingActionButton = {
            if (detailHabit == null && tab == AppTab.Today) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editorTarget = null
                        showEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(22.dp),
                    icon = { Text("＋", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    text = { Text("添加习惯", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = Triple(detailHabit?.id, tab, habits.size), label = "screen") { state ->
                val currentDetail = habits.firstOrNull { it.id == state.first }
                when {
                    currentDetail != null -> HabitDetailScreen(
                        habit = currentDetail,
                        today = today,
                        onBack = { detailId = null },
                        onToggleDate = { date -> HabitStore.toggle(currentDetail.id, date) },
                        onEdit = {
                            editorTarget = currentDetail
                            showEditor = true
                        },
                        onDelete = { deleteHabit(currentDetail) }
                    )

                    state.second == AppTab.Stats -> StatsScreen(
                        habits = habits,
                        today = today,
                        onAdd = {
                            editorTarget = null
                            showEditor = true
                        }
                    )

                    else -> TodayScreen(
                        habits = habits,
                        today = today,
                        onToggle = { habit -> HabitStore.toggle(habit.id) },
                        onOpenDetail = { habit -> detailId = habit.id },
                        onEdit = { habit ->
                            editorTarget = habit
                            showEditor = true
                        },
                        onDelete = { habit -> deleteHabit(habit) },
                        onAdd = {
                            editorTarget = null
                            showEditor = true
                        },
                        onOpenSettings = { showSettings = true },
                        onTogglePin = { habit -> HabitStore.togglePin(habit.id) },
                        onMove = { habit, offset -> HabitStore.move(habit.id, offset) },
                        onToggleBackfill = { habit -> backfillTarget = habit },
                        pendingFirst = pendingFirst
                    )
                }
            }
        }
    }

    if (showEditor) {
        HabitEditorDialog(
            initial = editorTarget,
            onDismiss = { showEditor = false },
            onSubmit = { name, emoji, accentIndex, weekGoal, strict, allowBackfill ->
                val target = editorTarget
                val success = if (target == null) {
                    HabitStore.add(name, emoji, accentIndex, weekGoal, strict, allowBackfill)
                } else {
                    HabitStore.edit(target.id, name, emoji, accentIndex, weekGoal, strict)
                }
                if (success) showEditor = false
                success
            }
        )
    }

    backfillTarget?.let { habit ->
        AlertDialog(
            onDismissRequest = { backfillTarget = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "关闭补卡",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "关闭后「${habit.name}」只能打当天的卡，漏掉的日期再也补不回来，" +
                        "并且无法重新开启。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HabitStore.toggleBackfill(habit.id)
                        backfillTarget = null
                        scope.launch { snackbarHostState.showSnackbar("已关闭「${habit.name}」的补卡") }
                    }
                ) {
                    Text(
                        text = "确认关闭",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { backfillTarget = null }) {
                    Text(text = "取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            pendingFirst = pendingFirst,
            habitsCount = habits.size,
            calendarReminder = calendarReminder,
            calendarPermissionGranted = CalendarReminder.hasPermission(context),
            onSetCalendarReminder = { hour, minute -> setCalendarReminder(hour, minute) },
            onRemoveCalendarReminder = { removeCalendarReminder() },
            onPendingFirstChange = { SettingsStore.updatePendingFirst(it) },
            onClearAll = {
                HabitStore.clearAll()
                detailId = null
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun KeepDayBottomBar(current: AppTab, onSelect: (AppTab) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { item ->
                BottomBarItem(
                    item = item,
                    selected = item == current,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(item: AppTab, selected: Boolean, onClick: () -> Unit) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "tabColor"
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.glyph, color = contentColor, fontSize = 15.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

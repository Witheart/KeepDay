package com.example.keepday.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keepday.data.Habit
import com.example.keepday.ui.theme.AccentGradient
import com.example.keepday.ui.theme.HabitAccents

val emojiChoices = listOf(
    "💧", "🏃", "📚", "🧘", "🌙", "💪", "🥗", "✍️", "🎸", "🧹", "🎯", "☀️"
)

/**
 * 新建 / 编辑习惯。传入 [initial] 即为编辑模式。
 * [onSubmit] 返回 false 表示名称非法（空或重名），弹窗会给出提示且不关闭。
 */
@Composable
fun HabitEditorDialog(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, emoji: String, accentIndex: Int, weekGoal: Int, strict: Boolean) -> Boolean
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: emojiChoices.first()) }
    var accentIndex by remember { mutableStateOf(initial?.accentIndex ?: 0) }
    var weekGoal by remember { mutableStateOf(initial?.weekGoal ?: 7) }
    var strict by remember { mutableStateOf(initial?.strict ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (initial == null) "新建习惯" else "编辑习惯",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    singleLine = true,
                    isError = error != null,
                    placeholder = { Text("例如：早起喝一杯水") },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(16.dp))
                Label("选个图标")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = emojiChoices) { item ->
                        EmojiOption(emoji = item, selected = item == emoji, onClick = { emoji = item })
                    }
                }

                Spacer(Modifier.height(16.dp))
                Label("选个颜色")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitAccents.forEachIndexed { index, accent ->
                        ColorOption(
                            accent = accent,
                            selected = index == accentIndex,
                            onClick = { accentIndex = index }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Label("坚持强度")
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (strict) MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "必须坚持打卡",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "开启后每天都要打卡，漏打一天连续天数清零（历史记录保留）",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (strict) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = strict,
                            onCheckedChange = {
                                strict = it
                                if (it) weekGoal = 7
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onError,
                                checkedTrackColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (strict) {
                    Text(
                        text = "严格模式下必须每天打卡，无需再设置每周目标",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Label("每周目标")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { day ->
                            GoalOption(
                                days = day,
                                selected = day == weekGoal,
                                accentIndex = accentIndex,
                                onClick = { weekGoal = day }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (weekGoal == 7) "每天都要打卡" else "每周至少打卡 $weekGoal 天",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ok = onSubmit(name, emoji, accentIndex, if (strict) 7 else weekGoal, strict)
                    if (!ok) error = "名称不能为空，也不能和已有习惯重名"
                }
            ) {
                Text(text = if (initial == null) "创建" else "保存", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmojiOption(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}

@Composable
private fun ColorOption(accent: AccentGradient, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent.start, accent.end)))
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun GoalOption(days: Int, selected: Boolean, accentIndex: Int, onClick: () -> Unit) {
    val accent = HabitAccents[accentIndex.mod(HabitAccents.size)]
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(
                if (selected) accent.start.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(if (selected) Modifier.border(1.5.dp, accent.start, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$days",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent.start else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

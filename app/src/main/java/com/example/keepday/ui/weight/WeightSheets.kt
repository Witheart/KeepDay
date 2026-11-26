package com.example.keepday.ui.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.keepday.data.weight.WeightSetting
import com.example.keepday.data.weight.WeightUnits
import java.time.LocalDate

private const val MIN_KG = 20f
private const val MAX_KG = 300f

/** 记录 / 补录体重。输入单位是 kg，页面展示按斤换算。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputSheet(
    date: LocalDate,
    today: LocalDate,
    existingKg: Float?,
    lastKg: Float?,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember {
        mutableStateOf(
            existingKg?.let { WeightUnits.format(it) }
                ?: lastKg?.let { WeightUnits.format(it) }
                ?: ""
        )
    }
    val parsed = text.toFloatOrNull()
    val valid = parsed != null && parsed in MIN_KG..MAX_KG
    val isBackfill = date != today

    fun adjust(delta: Float) {
        val base = text.toFloatOrNull() ?: lastKg ?: 60f
        val next = (base + delta).coerceIn(MIN_KG, MAX_KG)
        text = WeightUnits.format(next)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = when {
                    isBackfill -> "补录 ${date.monthValue} 月 ${date.dayOfMonth} 日"
                    existingKg != null -> "修改今天的体重"
                    else -> "记录今天的体重"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "输入单位 kg，页面按斤展示（1 kg = 2 斤）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.filter { it.isDigit() || it == '.' }.take(5)
                    },
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    placeholder = { Text("例如 63.4") },
                    suffix = { Text("kg", style = MaterialTheme.typography.labelMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.headlineSmall,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                StepButton(text = "−0.1") { adjust(-0.1f) }
                Spacer(Modifier.width(6.dp))
                StepButton(text = "+0.1") { adjust(0.1f) }
            }

            if (existingKg != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "该日期已有记录，保存会覆盖原数值",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { parsed?.let(onSave) },
                enabled = valid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (existingKg != null) "更新" else "保存",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 体重规则设置：严格模式、补录、目标体重。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightSettingsSheet(
    setting: WeightSetting,
    onToggleStrict: (Boolean) -> Unit,
    onDisableBackfill: () -> Unit,
    onUpdateGoal: (Float?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var goalText by remember {
        mutableStateOf(setting.goalKg?.let { WeightUnits.format(it) } ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "体重设置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            SettingRow(
                title = "必须每天记录",
                subtitle = "漏记一天会清零连续记录天数，历史数据保留"
            ) {
                Switch(
                    checked = setting.strict,
                    onCheckedChange = onToggleStrict,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            SettingRow(
                title = "允许补录",
                subtitle = if (setting.allowBackfill) "关闭后只能记录当天，且不可恢复"
                else "已关闭，只能记录当天"
            ) {
                Switch(
                    checked = setting.allowBackfill,
                    onCheckedChange = { enable -> if (!enable) onDisableBackfill() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "目标体重（kg）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { input ->
                        goalText = input.filter { it.isDigit() || it == '.' }.take(5)
                    },
                    singleLine = true,
                    placeholder = { Text("例如 60") },
                    suffix = { Text("kg", style = MaterialTheme.typography.labelMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                TextButton(
                    onClick = {
                        val kg = goalText.toFloatOrNull()
                        onUpdateGoal(if (kg != null && kg in MIN_KG..MAX_KG) kg else null)
                    }
                ) {
                    Text(
                        text = "保存",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (setting.goalKg != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "当前目标：${WeightUnits.format(setting.goalKg * 2)} 斤，清空输入框可取消目标",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            action()
        }
    }
}

package com.example.keepday

import com.example.keepday.data.BackupManager
import com.example.keepday.data.HabitStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** 备份导出/恢复的往返一致性。 */
class BackupRoundTripTest {

    @Before
    fun setUp() {
        HabitStore.clearAll()
    }

    @Test
    fun `导出后再导入能完整还原习惯与打卡记录`() {
        HabitStore.add("早起喝水", "💧", 0, weekGoal = 7, strict = true, allowBackfill = false)
        HabitStore.add("阅读", "📚", 3, weekGoal = 5, strict = false, allowBackfill = true)
        val water = HabitStore.habits.value.first { it.name == "早起喝水" }
        val reading = HabitStore.habits.value.first { it.name == "阅读" }
        HabitStore.toggle(water.id, LocalDate.now())
        // 阅读允许补卡，可以补昨天；喝水关闭了补卡，只能打今天
        HabitStore.toggle(reading.id, LocalDate.now())
        HabitStore.toggle(reading.id, LocalDate.now().minusDays(1))

        val before = HabitStore.habits.value
        val backup = BackupManager.exportJson()

        HabitStore.clearAll()
        assertEquals(0, HabitStore.habits.value.size)

        val result = BackupManager.importJson(backup)
        assertTrue(result.success)
        assertEquals(2, result.habitCount)

        val after = HabitStore.habits.value
        assertEquals(before.size, after.size)

        val restoredWater = after.first { it.name == "早起喝水" }
        assertEquals(1, restoredWater.days.size)
        assertTrue(restoredWater.strict)
        assertFalse(restoredWater.allowBackfill)
        assertNotNull(restoredWater.checkedAt(LocalDate.now()))

        val restoredReading = after.first { it.name == "阅读" }
        assertEquals(2, restoredReading.days.size)
        assertEquals(5, restoredReading.weekGoal)
        assertNotNull(restoredReading.checkedAt(LocalDate.now().minusDays(1)))
    }

    @Test
    fun `不是 KeepDay 的备份会被拒绝且不改动数据`() {
        HabitStore.add("原有习惯", "🌙", 1)
        val result = BackupManager.importJson("""{"app":"OtherApp","habits":[]}""")
        assertFalse(result.success)
        assertEquals(1, HabitStore.habits.value.size)
    }

    @Test
    fun `非法 JSON 不会清空数据`() {
        HabitStore.add("原有习惯", "🌙", 1)
        val result = BackupManager.importJson("not a json")
        assertFalse(result.success)
        assertEquals(1, HabitStore.habits.value.size)
    }
}

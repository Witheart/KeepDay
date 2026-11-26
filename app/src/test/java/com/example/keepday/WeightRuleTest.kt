package com.example.keepday

import com.example.keepday.data.weight.WeightEntry
import com.example.keepday.data.weight.WeightMath
import com.example.keepday.data.weight.WeightSetting
import com.example.keepday.data.weight.WeightUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 体重打卡：连续记录、补录限制、严格模式与统计口径。 */
class WeightRuleTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 26)

    private fun entry(date: LocalDate, kg: Float, recordedAt: LocalDate = date): WeightEntry =
        WeightEntry(
            epochDay = date.toEpochDay(),
            kg = kg,
            recordedAt = recordedAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    private fun setting(
        strict: Boolean = false,
        allowBackfill: Boolean = true,
        streakResetDay: LocalDate? = null,
        createdDay: LocalDate = today.minusDays(30)
    ) = WeightSetting(
        strict = strict,
        allowBackfill = allowBackfill,
        streakResetDay = streakResetDay?.toEpochDay(),
        createdDay = createdDay.toEpochDay()
    )

    /* ---------------------------- 连续记录 ---------------------------- */

    @Test
    fun `连续记录三天连续天数为三`() {
        val entries = listOf(
            entry(today, 63.0f),
            entry(today.minusDays(1), 63.4f),
            entry(today.minusDays(2), 63.6f)
        )
        assertEquals(3, WeightMath.streakAt(entries, setting(), today))
    }

    @Test
    fun `今天还没记录时从昨天开始数`() {
        val entries = listOf(
            entry(today.minusDays(1), 63.4f),
            entry(today.minusDays(2), 63.6f)
        )
        assertEquals(2, WeightMath.streakAt(entries, setting(), today))
    }

    @Test
    fun `断签后连续记录从清零点重新计算`() {
        val entries = listOf(
            entry(today, 63.0f),
            entry(today.minusDays(1), 63.4f),
            entry(today.minusDays(2), 63.6f),
            entry(today.minusDays(3), 63.8f)
        )
        assertEquals(4, WeightMath.streakAt(entries, setting(), today))
        assertEquals(
            2,
            WeightMath.streakAt(entries, setting(streakResetDay = today.minusDays(1)), today)
        )
    }

    /* ---------------------------- 记录限制 ---------------------------- */

    @Test
    fun `允许补录时可以记录历史日期`() {
        assertTrue(WeightMath.canRecord(today.minusDays(3), setting(), today))
        assertTrue(WeightMath.canRecord(today, setting(), today))
    }

    @Test
    fun `关闭补录后只能记录当天`() {
        val closed = setting(allowBackfill = false)
        assertFalse(WeightMath.canRecord(today.minusDays(1), closed, today))
        assertTrue(WeightMath.canRecord(today, closed, today))
    }

    @Test
    fun `未来日期一律不可记录`() {
        assertFalse(WeightMath.canRecord(today.plusDays(1), setting(), today))
    }

    /* ---------------------------- 严格模式 ---------------------------- */

    @Test
    fun `漏记一天判定断签`() {
        val entries = listOf(
            entry(today.minusDays(2), 63.4f),
            entry(today.minusDays(3), 63.6f)
        )
        assertTrue(
            WeightMath.strictResetDue(
                entries,
                setting(strict = true, createdDay = today.minusDays(3)),
                today
            )
        )
    }

    @Test
    fun `连续记录则不断签`() {
        val entries = listOf(
            entry(today.minusDays(1), 63.4f),
            entry(today.minusDays(2), 63.6f)
        )
        assertFalse(
            WeightMath.strictResetDue(
                entries,
                setting(strict = true, createdDay = today.minusDays(2)),
                today
            )
        )
    }

    @Test
    fun `未开启严格模式不判断签`() {
        assertFalse(WeightMath.strictResetDue(emptyList(), setting(strict = false), today))
    }

    /* ------------------------------ 统计 ------------------------------ */

    @Test
    fun `窗口内的增减以斤为单位`() {
        val entries = listOf(
            entry(today, 63.0f),
            entry(today.minusDays(10), 65.0f)
        )
        val stats = WeightMath.statsFor(entries, today, windowDays = 30)
        assertEquals(-4.0f, stats.changeJin!!, 0.01f)
        assertEquals(63.0f, stats.minEntry!!.kg, 0.01f)
        assertEquals(65.0f, stats.maxEntry!!.kg, 0.01f)
        assertEquals(2, stats.recordedDays)
    }

    @Test
    fun `窗口外的数据不计入`() {
        val entries = listOf(
            entry(today, 63.0f),
            entry(today.minusDays(40), 70.0f)
        )
        val stats = WeightMath.statsFor(entries, today, windowDays = 30)
        assertEquals(1, stats.recordedDays)
        assertNull(stats.changeJin)
    }

    @Test
    fun `与七天前比较`() {
        val entries = listOf(
            entry(today, 63.0f),
            entry(today.minusDays(7), 64.0f)
        )
        assertEquals(-2.0f, WeightMath.changeAgainstPrevious(entries, today, 7)!!, 0.01f)
        assertNull(WeightMath.changeAgainstPrevious(entries, today, 14))
    }

    @Test
    fun `斤与公斤换算`() {
        assertEquals(126.8f, WeightUnits.kgToJin(63.4f), 0.01f)
        assertEquals(63.4f, WeightUnits.jinToKg(126.8f), 0.01f)
    }

    @Test
    fun `补录标记根据录入时间判定`() {
        val backfilled = entry(today.minusDays(2), 63.4f, recordedAt = today)
        assertTrue(backfilled.isBackfilled)
        val normal = entry(today, 63.4f)
        assertFalse(normal.isBackfilled)
    }
}

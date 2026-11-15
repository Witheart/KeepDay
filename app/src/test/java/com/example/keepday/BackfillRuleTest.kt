package com.example.keepday

import com.example.keepday.data.Habit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 补卡开关：关闭后只能打当天的卡。 */
class BackfillRuleTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 15)

    private fun habit(allowBackfill: Boolean) = Habit(
        id = "test",
        name = "测试习惯",
        emoji = "💧",
        accentIndex = 0,
        allowBackfill = allowBackfill
    )

    @Test
    fun `允许补卡时可以补过去的日期`() {
        val subject = habit(allowBackfill = true)
        assertTrue(subject.canCheckIn(today.minusDays(1), today))
        assertTrue(subject.canCheckIn(today.minusDays(30), today))
        assertTrue(subject.canCheckIn(today, today))
    }

    @Test
    fun `关闭补卡后历史日期不可打卡`() {
        val subject = habit(allowBackfill = false)
        assertFalse(subject.canCheckIn(today.minusDays(1), today))
        assertFalse(subject.canCheckIn(today.minusDays(30), today))
    }

    @Test
    fun `关闭补卡后当天仍然可以打卡`() {
        val subject = habit(allowBackfill = false)
        assertTrue(subject.canCheckIn(today, today))
    }

    @Test
    fun `任何情况下都不能给未来打卡`() {
        assertFalse(habit(allowBackfill = true).canCheckIn(today.plusDays(1), today))
        assertFalse(habit(allowBackfill = false).canCheckIn(today.plusDays(1), today))
    }
}

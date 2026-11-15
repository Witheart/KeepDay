package com.example.keepday

import com.example.keepday.data.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 严格模式：只有真正漏打才清零连续天数，补卡与历史记录都不受影响。 */
class StrictModeRuleTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 15)

    private fun habit(
        strict: Boolean = true,
        streakResetDay: LocalDate? = null,
        createdAt: LocalDate = today.minusDays(10),
        doneDays: List<LocalDate> = emptyList()
    ) = Habit(
        id = "test",
        name = "测试习惯",
        emoji = "💧",
        accentIndex = 0,
        strict = strict,
        streakResetDay = streakResetDay?.toEpochDay(),
        createdAt = createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        days = doneDays.map { it.toEpochDay() }.toSet()
    )

    /* ------------------------------ 断签判定 ------------------------------ */

    @Test
    fun `未开启严格模式时永不判为断签`() {
        val subject = habit(strict = false, doneDays = emptyList())
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `昨天漏打需要清零`() {
        val subject = habit(doneDays = listOf(today.minusDays(2)))
        assertTrue(subject.strictResetDue(today))
    }

    @Test
    fun `连续打卡则不清零`() {
        val subject = habit(
            createdAt = today.minusDays(3),
            doneDays = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        )
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `中间漏打一天也要清零`() {
        val subject = habit(doneDays = listOf(today.minusDays(3), today.minusDays(1)))
        assertTrue(subject.strictResetDue(today))
    }

    @Test
    fun `今天还没打卡不算断签`() {
        val subject = habit(
            createdAt = today.minusDays(3),
            doneDays = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        )
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `断签清零后当天不会重复触发`() {
        val reset = habit(streakResetDay = today, doneDays = listOf(today.minusDays(1)))
        assertFalse(reset.strictResetDue(today))
        // 明天再看时，今天没打卡才应该再次触发
        assertTrue(reset.strictResetDue(today.plusDays(1)))
    }

    /* ------------------------------ 连续天数 ------------------------------ */

    @Test
    fun `开启严格模式不会截断已有的连续天数`() {
        // 刚开启严格模式，历史记录（含补卡）照常计入连续
        val subject = habit(
            streakResetDay = null,
            createdAt = today,
            doneDays = listOf(today, today.minusDays(1), today.minusDays(2))
        )
        assertEquals(3, subject.streakAt(today))
    }

    @Test
    fun `补卡可以增加普通习惯的连续天数`() {
        val subject = habit(
            strict = false,
            createdAt = today,
            doneDays = listOf(today, today.minusDays(1))
        )
        assertEquals(2, subject.streakAt(today))
    }

    @Test
    fun `断签后连续天数归零但历史记录保留`() {
        val subject = habit(
            streakResetDay = today,
            doneDays = listOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        )
        assertEquals(0, subject.streakAt(today))
        assertEquals(3, subject.totalDays)
    }

    @Test
    fun `断签后从清零日开始重新累计`() {
        val subject = habit(
            streakResetDay = today,
            doneDays = listOf(today, today.minusDays(5))
        )
        assertEquals(1, subject.streakAt(today))

        val tomorrow = today.plusDays(1)
        val next = subject.copy(days = subject.days + tomorrow.toEpochDay())
        assertEquals(2, next.streakAt(tomorrow))
    }

    @Test
    fun `断签后补卡不会恢复已清零的连续天数`() {
        val subject = habit(
            streakResetDay = today,
            doneDays = listOf(today.minusDays(2), today.minusDays(1))
        )
        assertEquals(0, subject.streakAt(today))
    }
}

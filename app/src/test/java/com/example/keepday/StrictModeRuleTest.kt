package com.example.keepday

import com.example.keepday.data.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 严格模式：漏打一天连续天数清零，但历史打卡记录保留。 */
class StrictModeRuleTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 15)

    private fun habit(
        strict: Boolean = true,
        strictSince: LocalDate? = today.minusDays(3),
        createdAt: LocalDate = today.minusDays(10),
        doneDays: List<LocalDate> = emptyList()
    ) = Habit(
        id = "test",
        name = "测试习惯",
        emoji = "💧",
        accentIndex = 0,
        strict = strict,
        strictSince = strictSince?.toEpochDay(),
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
    fun `从起点到今天连续打卡则不清零`() {
        val subject = habit(
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
    fun `起点就是今天时不判断签`() {
        val subject = habit(strictSince = today, doneDays = emptyList())
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `今天还没打卡不算断签`() {
        val subject = habit(
            doneDays = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        )
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `清零后起点设为今天，第二天才会再次触发`() {
        val reset = habit(strictSince = today, doneDays = emptyList())
        assertFalse(reset.strictResetDue(today))
        assertTrue(reset.strictResetDue(today.plusDays(1)))
    }

    /* ------------------------------ 连续天数 ------------------------------ */

    @Test
    fun `连续三天打卡连续天数为三`() {
        val subject = habit(
            strictSince = null,
            doneDays = listOf(today, today.minusDays(1), today.minusDays(2))
        )
        assertEquals(3, subject.streakAt(today))
    }

    @Test
    fun `断签后连续天数归零但历史记录保留`() {
        val subject = habit(
            // 今天凌晨刚被清零
            strictSince = today,
            doneDays = listOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        )
        assertEquals(0, subject.streakAt(today))
        assertEquals(3, subject.totalDays)
    }

    @Test
    fun `清零后从今天重新累计`() {
        val subject = habit(
            strictSince = today,
            doneDays = listOf(today, today.minusDays(5))
        )
        assertEquals(1, subject.streakAt(today))

        val tomorrow = today.plusDays(1)
        val next = subject.copy(days = subject.days + tomorrow.toEpochDay())
        assertEquals(2, next.streakAt(tomorrow))
    }

    @Test
    fun `严格模式下补卡不会恢复已清零的连续天数`() {
        val subject = habit(
            strictSince = today,
            doneDays = listOf(today.minusDays(2), today.minusDays(1))
        )
        assertEquals(0, subject.streakAt(today))
    }

    @Test
    fun `普通习惯补卡后连续天数可以接上`() {
        val subject = habit(
            strict = false,
            strictSince = null,
            doneDays = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        )
        assertEquals(3, subject.streakAt(today))
    }
}

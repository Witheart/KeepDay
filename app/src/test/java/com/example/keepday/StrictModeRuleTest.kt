package com.example.keepday

import com.example.keepday.data.Habit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 严格模式（断签清零）判定的核心规则。 */
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
        createdAt = createdAt.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        days = doneDays.map { it.toEpochDay() }.toSet()
    )

    @Test
    fun `未开启严格模式时永不判为断签`() {
        val subject = habit(strict = false, doneDays = emptyList())
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `昨天漏打需要清零`() {
        // 只打了前天，昨天漏了
        val subject = habit(doneDays = listOf(today.minusDays(2)))
        assertTrue(subject.strictResetDue(today))
    }

    @Test
    fun `从生效日到今天连续打卡则不清零`() {
        val subject = habit(
            doneDays = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        )
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `中间漏打一天也要清零`() {
        val subject = habit(
            doneDays = listOf(today.minusDays(3), today.minusDays(1))
        )
        assertTrue(subject.strictResetDue(today))
    }

    @Test
    fun `生效日就是今天时不判断签`() {
        val subject = habit(strictSince = today, doneDays = emptyList())
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `今天还没打卡不算断签`() {
        // 昨天及之前都打了，今天还没打
        val subject = habit(
            doneDays = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        )
        assertFalse(subject.strictResetDue(today))
    }

    @Test
    fun `清零后判定起点设为今天，不会重复触发`() {
        val reset = habit(strictSince = today, doneDays = emptyList())
        assertFalse(reset.strictResetDue(today))
        // 明天回来时，今天没打卡才应该再次触发
        assertTrue(reset.strictResetDue(today.plusDays(1)))
    }
}

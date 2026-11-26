package com.example.keepday

import com.example.keepday.ui.egg.BirthdayEgg
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 生日彩蛋的触发日期。 */
class BirthdayEggTest {

    @Test
    fun `国历十一月二十七日触发`() {
        assertTrue(BirthdayEgg.isGregorianBirthday(LocalDate.of(2026, 11, 27)))
        assertTrue(BirthdayEgg.isGregorianBirthday(LocalDate.of(2030, 11, 27)))
    }

    @Test
    fun `其它日期不触发`() {
        assertFalse(BirthdayEgg.isGregorianBirthday(LocalDate.of(2026, 11, 26)))
        assertFalse(BirthdayEgg.isGregorianBirthday(LocalDate.of(2026, 11, 28)))
        assertFalse(BirthdayEgg.isGregorianBirthday(LocalDate.of(2026, 12, 27)))
    }

    @Test
    fun `isBirthday 在国历当天为真`() {
        assertTrue(BirthdayEgg.isBirthday(LocalDate.of(2026, 11, 27)))
    }

    @Test
    fun `isBirthday 在普通日期为假`() {
        // 单元测试跑在 JVM 上，农历换算不可用时会安全退化为 false
        assertFalse(BirthdayEgg.isBirthday(LocalDate.of(2026, 6, 1)))
    }
}

package com.bounce.features.reminders

import com.bounce.features.schedule.HabitSchedule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val monday = LocalDate.of(2026, 11, 16)

    @Test fun quietHoursAcrossMidnight() {
        val quiet = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
        assertTrue(quiet.contains(LocalTime.of(23, 0)))
        assertTrue(quiet.contains(LocalTime.of(6, 59)))
        assertFalse(quiet.contains(LocalTime.of(7, 0)))
        assertFalse(quiet.contains(LocalTime.NOON))
    }

    @Test fun daytimeQuietHours() {
        val quiet = QuietHours(LocalTime.of(13, 0), LocalTime.of(15, 0))
        assertTrue(quiet.contains(LocalTime.of(14, 0)))
        assertFalse(quiet.contains(LocalTime.of(15, 0)))
    }

    @Test fun equalQuietHourEndpointsDisableQuietHours() {
        val quiet = QuietHours(LocalTime.NOON, LocalTime.NOON)
        assertFalse(quiet.contains(LocalTime.NOON))
    }

    @Test fun nextReminderIsTodayWhenTimeHasNotPassed() {
        val schedule = HabitSchedule(1, monday)
        val now = monday.atTime(8, 0).atZone(zone)
        val next = nextReminder(schedule, LocalTime.of(9, 0), zone, now)
        assertEquals(monday, next?.toLocalDate())
        assertEquals(LocalTime.of(9, 0), next?.toLocalTime())
    }

    @Test fun nextReminderMovesToTomorrowAfterTimePassed() {
        val schedule = HabitSchedule(1, monday)
        val now = monday.atTime(10, 0).atZone(zone)
        assertEquals(
            monday.plusDays(1),
            nextReminder(schedule, LocalTime.of(9, 0), zone, now)?.toLocalDate()
        )
    }

    @Test fun restDayIsSkipped() {
        val schedule = HabitSchedule(1, monday).withRestDay(monday, true)
        val now = monday.atTime(8, 0).atZone(zone)
        assertEquals(
            monday.plusDays(1),
            nextReminder(schedule, LocalTime.of(9, 0), zone, now)?.toLocalDate()
        )
    }

    @Test fun quietHourReminderIsSuppressed() {
        val schedule = HabitSchedule(1, monday)
        val now = monday.atStartOfDay(zone)
        assertNull(
            nextReminder(
                schedule, LocalTime.of(23, 0), zone, now,
                QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
            )
        )
    }

    @Test fun scheduleOutsideWindowReturnsNull() {
        val schedule = HabitSchedule(1, monday.plusDays(30))
        val now = monday.atStartOfDay(zone)
        assertNull(nextReminder(schedule, LocalTime.of(9, 0), zone, now, searchLimitDays = 7))
    }
}

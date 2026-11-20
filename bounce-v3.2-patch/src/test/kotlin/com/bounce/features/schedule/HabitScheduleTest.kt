package com.bounce.features.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitScheduleTest {
    private val monday = LocalDate.of(2026, 11, 16)

    @Test fun dailyHabitIsDueEveryDayAfterStart() {
        val schedule = HabitSchedule(1, monday)
        assertFalse(schedule.isDue(monday.minusDays(1)))
        assertTrue(schedule.isDue(monday))
        assertTrue(schedule.isDue(monday.plusDays(3)))
    }

    @Test fun selectedWeekdaysOnly() {
        val schedule = HabitSchedule(
            1, monday, SchedulePattern.SELECTED_WEEKDAYS,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )
        assertTrue(schedule.isDue(monday))
        assertFalse(schedule.isDue(monday.plusDays(1)))
        assertTrue(schedule.isDue(monday.plusDays(2)))
    }

    @Test fun intervalStartsFromAnchorDay() {
        val schedule = HabitSchedule(1, monday, SchedulePattern.EVERY_N_DAYS, intervalDays = 3)
        assertTrue(schedule.isDue(monday))
        assertFalse(schedule.isDue(monday.plusDays(2)))
        assertTrue(schedule.isDue(monday.plusDays(3)))
    }

    @Test fun restDayIsNotDue() {
        val schedule = HabitSchedule(1, monday).withRestDay(monday.plusDays(1), true)
        assertFalse(schedule.isDue(monday.plusDays(1)))
        assertTrue(schedule.withRestDay(monday.plusDays(1), false).isDue(monday.plusDays(1)))
    }

    @Test fun nextDueSkipsRestDays() {
        val schedule = HabitSchedule(1, monday).withRestDay(monday, true)
        assertEquals(monday.plusDays(1), schedule.nextDue(monday))
    }

    @Test fun nextDueRespectsWindow() {
        val schedule = HabitSchedule(1, monday, SchedulePattern.EVERY_N_DAYS, intervalDays = 10)
        assertNull(schedule.nextDue(monday.plusDays(1), searchLimitDays = 3))
    }

    @Test fun weeklySummaryExcludesFutureRestAndUnscheduledDays() {
        val schedule = HabitSchedule(
            1, monday, SchedulePattern.SELECTED_WEEKDAYS,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
        ).withRestDay(monday.plusDays(1), true)
        val totals = listOf(
            HabitDailyTotal(1, monday, 1),
            HabitDailyTotal(1, monday, 3), // highest cumulative snapshot wins
            HabitDailyTotal(1, monday.plusDays(1), 10), // rest day ignored
            HabitDailyTotal(2, monday.plusDays(2), 10), // another habit ignored
            HabitDailyTotal(1, monday.plusDays(3), 10) // unscheduled ignored
        )
        val result = calculateHabitWeek(schedule, totals, monday, monday.plusDays(2), dailyTarget = 3)
        assertEquals(2, result.eligibleDays)
        assertEquals(1, result.activeDays)
        assertEquals(1, result.completedDays)
        assertEquals(50, result.activePercent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptySelectedWeekdaysAreRejected() {
        HabitSchedule(1, monday, SchedulePattern.SELECTED_WEEKDAYS)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroIntervalRejected() {
        HabitSchedule(1, monday, SchedulePattern.EVERY_N_DAYS, intervalDays = 0)
    }
}

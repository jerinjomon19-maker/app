package com.bounce.features.engagement

import com.bounce.features.schedule.HabitSchedule
import com.bounce.features.schedule.SchedulePattern
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitEngagementTest {
    private val monday = LocalDate.of(2026, 11, 16)

    @Test fun missedTodayDoesNotImmediatelyBreakStreak() {
        val schedule = HabitSchedule(1, monday)
        val result = calculateConsistency(
            schedule, setOf(monday, monday.plusDays(1)), monday.plusDays(2), 3
        )
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
        assertEquals(66, result.completionRatePercent)
    }

    @Test fun yesterdayMissBreaksStreak() {
        val schedule = HabitSchedule(1, monday)
        val result = calculateConsistency(
            schedule, setOf(monday), monday.plusDays(2), 3
        )
        assertEquals(0, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test fun unscheduledDaysDoNotBreakStreak() {
        val schedule = HabitSchedule(
            1, monday, SchedulePattern.SELECTED_WEEKDAYS,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )
        val result = calculateConsistency(
            schedule, setOf(monday, monday.plusDays(2)), monday.plusDays(2), 3
        )
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.dueDays)
    }

    @Test fun restDaysDoNotBreakStreak() {
        val schedule = HabitSchedule(1, monday).withRestDay(monday.plusDays(1), true)
        val result = calculateConsistency(
            schedule, setOf(monday, monday.plusDays(2)), monday.plusDays(2), 3
        )
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.dueDays)
    }

    @Test fun futureCompletionsAreNotCounted() {
        val schedule = HabitSchedule(1, monday)
        val result = calculateConsistency(schedule, setOf(monday.plusDays(1)), monday, 1)
        assertEquals(0, result.completedDays)
        assertEquals(0, result.currentStreak)
    }

    @Test fun nextActionSkipsCompletedAndRestingHabits() {
        val completed = TodayHabit(HabitSchedule(1, monday), "Water", 8, 8, 0)
        val resting = TodayHabit(
            HabitSchedule(2, monday).withRestDay(monday, true), "Workout", 0, 1, 1
        )
        val next = TodayHabit(HabitSchedule(3, monday), "Study", 0, 1, 2)
        assertEquals(next, nextHabitAction(listOf(next, completed, resting), monday))
    }

    @Test fun noActionWhenEverythingComplete() {
        val completed = TodayHabit(HabitSchedule(1, monday), "Water", 8, 8, 0)
        assertNull(nextHabitAction(listOf(completed), monday))
    }

    @Test fun routineShowsFirstIncompleteStep() {
        val steps = listOf(
            RoutineStep(3, "Study", 2),
            RoutineStep(1, "Water", 0),
            RoutineStep(2, "Stretch", 1)
        )
        val result = routineProgress(steps, setOf(1L))
        assertEquals(1, result.completedSteps)
        assertEquals(3, result.totalSteps)
        assertEquals(2L, result.nextStep?.habitId)
    }

    @Test fun routineDeduplicatesHabitIds() {
        val steps = listOf(RoutineStep(1, "Water", 0), RoutineStep(1, "Water again", 1))
        assertEquals(1, routineProgress(steps, setOf(1L)).totalSteps)
    }
}

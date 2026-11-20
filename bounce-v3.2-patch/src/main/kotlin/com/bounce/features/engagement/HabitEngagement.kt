package com.bounce.features.engagement

import com.bounce.features.schedule.HabitSchedule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Privacy-friendly, fully offline engagement calculations. */
data class HabitConsistency(
    val dueDays: Int,
    val completedDays: Int,
    val currentStreak: Int,
    val longestStreak: Int
) {
    val completionRatePercent: Int
        get() = if (dueDays == 0) 0 else completedDays * 100 / dueDays
}

/**
 * A streak counts scheduled days, not calendar days.
 * Rest days and days without a scheduled habit do not break it.
 * An unfinished *today* is treated as pending until the day ends.
 *
 * Completed dates must come from the actual habit's persisted progress.
 * The window bounds both the longest streak and current streak.
 */
fun calculateConsistency(
    schedule: HabitSchedule,
    completedDates: Set<LocalDate>,
    today: LocalDate,
    lookbackDays: Int = 90
): HabitConsistency {
    require(lookbackDays in 1..3650) { "lookbackDays must be between 1 and 3650" }
    val start = today.minusDays((lookbackDays - 1).toLong())
    val due = (0 until lookbackDays)
        .map { start.plusDays(it.toLong()) }
        .filter(schedule::isDue)

    var completed = 0
    var longest = 0
    var running = 0
    for (date in due) {
        if (date in completedDates) {
            completed++
            running++
            if (running > longest) longest = running
        } else {
            running = 0
        }
    }

    var current = 0
    for (date in due.asReversed()) {
        if (date == today && date !in completedDates) continue
        if (date !in completedDates) break
        current++
    }

    return HabitConsistency(due.size, completed, current, longest)
}

/** One immutable snapshot per habit, ready to render as a Today quick action. */
data class TodayHabit(
    val schedule: HabitSchedule,
    val title: String,
    val progress: Int,
    val target: Int,
    val sortOrder: Int
) {
    init {
        require(target > 0) { "target must be positive" }
        require(progress >= 0) { "progress must not be negative" }
    }
    val remaining: Int get() = (target - progress).coerceAtLeast(0)
    val isComplete: Boolean get() = remaining == 0
}

/**
 * Only suggests an action when the habit is scheduled today and unfinished.
 * Stable ordering prevents Today cards from jumping as progress is saved.
 */
fun nextHabitAction(habits: List<TodayHabit>, today: LocalDate): TodayHabit? =
    habits.asSequence()
        .filter { it.schedule.isDue(today) && !it.isComplete }
        .sortedWith(compareBy<TodayHabit> { it.sortOrder }.thenBy { it.schedule.habitId })
        .firstOrNull()

data class RoutineStep(val habitId: Long, val label: String, val order: Int)

data class RoutineProgress(val completedSteps: Int, val totalSteps: Int, val nextStep: RoutineStep?)

/** Optional habit stacking without timers, servers or forced app usage. */
fun routineProgress(steps: List<RoutineStep>, completedHabitIds: Set<Long>): RoutineProgress {
    val ordered = steps.distinctBy { it.habitId }
        .sortedWith(compareBy<RoutineStep> { it.order }.thenBy { it.habitId })
    return RoutineProgress(
        completedSteps = ordered.count { it.habitId in completedHabitIds },
        totalSteps = ordered.size,
        nextStep = ordered.firstOrNull { it.habitId !in completedHabitIds }
    )
}

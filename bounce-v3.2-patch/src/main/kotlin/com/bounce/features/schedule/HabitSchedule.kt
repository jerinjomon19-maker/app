package com.bounce.features.schedule

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Standalone Bounce v3.2 scheduling engine.
 * No database schema changes are made by this file.
 * Requires Android API 26+ (java.time).
 */
enum class SchedulePattern {
    DAILY,
    SELECTED_WEEKDAYS,
    EVERY_N_DAYS
}

data class HabitSchedule(
    val habitId: Long,
    val startsOn: LocalDate,
    val pattern: SchedulePattern = SchedulePattern.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = 1,
    val restDays: Set<LocalDate> = emptySet()
) {
    init {
        require(intervalDays > 0) { "intervalDays must be positive" }
        require(pattern != SchedulePattern.SELECTED_WEEKDAYS || weekdays.isNotEmpty()) {
            "Select at least one weekday"
        }
    }

    fun isPlanned(date: LocalDate): Boolean {
        if (date.isBefore(startsOn)) return false
        return when (pattern) {
            SchedulePattern.DAILY -> true
            SchedulePattern.SELECTED_WEEKDAYS -> date.dayOfWeek in weekdays
            SchedulePattern.EVERY_N_DAYS ->
                ChronoUnit.DAYS.between(startsOn, date) % intervalDays == 0L
        }
    }

    /** Rest days are intentionally not counted as missed days. */
    fun isDue(date: LocalDate): Boolean = isPlanned(date) && date !in restDays

    fun withRestDay(date: LocalDate, enabled: Boolean): HabitSchedule =
        copy(restDays = if (enabled) restDays + date else restDays - date)

    /** Returns null when there is no due day within the supplied search window. */
    fun nextDue(fromInclusive: LocalDate, searchLimitDays: Int = 366): LocalDate? {
        require(searchLimitDays > 0) { "searchLimitDays must be positive" }
        val first = if (fromInclusive.isBefore(startsOn)) startsOn else fromInclusive
        for (offset in 0 until searchLimitDays) {
            val date = first.plusDays(offset.toLong())
            if (isDue(date)) return date
        }
        return null
    }
}

/**
 * One snapshot of the cumulative progress for a habit on a date.
 * When duplicate snapshots exist, the highest total is used.
 */
data class HabitDailyTotal(
    val habitId: Long,
    val date: LocalDate,
    val totalForDay: Int
)

data class HabitWeeklySummary(
    val eligibleDays: Int,
    val activeDays: Int,
    val completedDays: Int
) {
    val activePercent: Int
        get() = if (eligibleDays == 0) 0 else activeDays * 100 / eligibleDays
}

/**
 * Excludes future days, rest days, unscheduled days and other habits.
 * weekStartsOn must be the first day of the desired seven-day period.
 */
fun calculateHabitWeek(
    schedule: HabitSchedule,
    dailyTotals: List<HabitDailyTotal>,
    weekStartsOn: LocalDate,
    today: LocalDate,
    dailyTarget: Int = 1
): HabitWeeklySummary {
    require(dailyTarget > 0) { "dailyTarget must be positive" }
    val totalsByDate = dailyTotals
        .asSequence()
        .filter { it.habitId == schedule.habitId }
        .groupBy { it.date }
        .mapValues { (_, snapshots) ->
            snapshots.maxOf { it.totalForDay.coerceAtLeast(0) }
        }

    var eligible = 0
    var active = 0
    var completed = 0
    for (offset in 0L..6L) {
        val date = weekStartsOn.plusDays(offset)
        if (date.isAfter(today) || !schedule.isDue(date)) continue
        eligible++
        val total = totalsByDate[date] ?: 0
        if (total > 0) active++
        if (total >= dailyTarget) completed++
    }
    return HabitWeeklySummary(eligible, active, completed)
}

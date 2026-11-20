package com.bounce.features.reminders

import com.bounce.features.schedule.HabitSchedule
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Reminder policy only. This does not post notifications or request permissions.
 * Scheduling with WorkManager/AlarmManager belongs in the Android app layer.
 */
data class QuietHours(val start: LocalTime, val end: LocalTime) {
    fun contains(time: LocalTime): Boolean {
        if (start == end) return false // equal endpoints mean disabled
        return if (start.isBefore(end)) {
            !time.isBefore(start) && time.isBefore(end)
        } else {
            !time.isBefore(start) || time.isBefore(end)
        }
    }
}

/**
 * Computes the next eligible wall-clock reminder in the habit's time zone.
 * - No notification is generated on rest days or unscheduled days.
 * - Reminder times within quiet hours are skipped, never moved silently.
 * - DST gaps/overlaps use java.time's standard ZoneId resolution.
 */
fun nextReminder(
    schedule: HabitSchedule,
    reminderTime: LocalTime,
    zone: ZoneId,
    fromInclusive: ZonedDateTime,
    quietHours: QuietHours? = null,
    searchLimitDays: Int = 366
): ZonedDateTime? {
    require(searchLimitDays in 1..3660) { "Invalid search window" }
    if (quietHours?.contains(reminderTime) == true) return null

    val localFrom = fromInclusive.withZoneSameInstant(zone)
    val firstDate = localFrom.toLocalDate()
    for (offset in 0 until searchLimitDays) {
        val date = firstDate.plusDays(offset.toLong())
        if (!schedule.isDue(date)) continue
        val candidate = date.atTime(reminderTime).atZone(zone)
        if (!candidate.isBefore(localFrom)) return candidate
    }
    return null
}

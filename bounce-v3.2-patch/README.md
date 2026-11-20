# Bounce v3.2 — scheduling feature patch

This folder is a **standalone Kotlin feature patch**, not the complete Bounce Android app. The connected GitHub repository did not contain the v3.1 Android Studio project when this patch was created. No original app files or logo assets have been changed.

## Included

- `HabitSchedule.kt`: pure Kotlin offline scheduling engine for daily, selected-weekday, and every-N-day habits; optional rest dates; next due day; weekly progress summaries.
- `HabitScheduleTest.kt`: nine JUnit 4 test cases. **Tests have not been executed** in this environment.

## Integrate with the real v3.1 project

1. Copy the main source into your existing Android app's source tree, adapting the package if necessary.
2. Copy the tests into the matching `src/test` tree and run `./gradlew testDebugUnitTest`.
3. Persist `HabitSchedule` with Room using a new schedule table keyed by habit ID. **Do not replace or reset the existing habits database.** Add a Room migration and backfill daily schedules for existing habits.
4. Persist rest days separately (unique `habitId + date`), or serialize them using an explicit converter with a migration strategy.
5. Map your existing progress records to cumulative `HabitDailyTotal` values. If the database stores incremental events, **sum events before calling the weekly calculator**; the helper assumes each entry is a cumulative daily snapshot and uses the maximum to de-duplicate snapshots.
6. Use `isDue(date)` for Today filtering and reminders; retain all history records on rest days. Use `nextDue` to schedule the next reminder with WorkManager or AlarmManager as appropriate.
7. Do not modify existing launcher icons, splash assets, colors or brand vectors.

## Remaining features / release gates

Actionable reminders, widgets, secure backup, and new Compose UI are **not implemented** by this patch. Implement them after reviewing the real app source and its database/notification architecture.

Android compilation, integration tests, physical-device alarm tests, backup/restore validation, accessibility checks, and release signing remain outstanding.

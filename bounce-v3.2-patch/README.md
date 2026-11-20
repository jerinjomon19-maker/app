# Bounce v3.2 — offline feature code pack

This folder contains **real Kotlin source files and JUnit tests**, but **not a complete Android Studio application**. The connected GitHub repository did not contain the original Bounce v3.1 source project. Original Bounce logo, app icon, splash resources, database, and existing UI have not been changed.

## Source files

| File | What it implements |
| --- | --- |
| `schedule/HabitSchedule.kt` | Daily, weekday and interval schedules; rest days; next due date; weekly habit totals |
| `engagement/HabitEngagement.kt` | Scheduled-day streaks; 90-day consistency; next incomplete Today action; ordered habit routines |
| `reminders/ReminderPolicy.kt` | Local reminder planning with rest days, time zones, and optional quiet hours |

## Tests written (not executed)

- `HabitScheduleTest.kt` — 9 JUnit 4 test methods
- `HabitEngagementTest.kt` — 9 JUnit 4 test methods
- `ReminderPolicyTest.kt` — 8 JUnit 4 test methods

Total: **26 test methods written**, **zero test runs claimed**. Kotlin compilation and physical Android device testing have not been performed.

## Integration into the real Bounce project

1. Obtain the original Bounce v3.1 Android Studio source ZIP or push it to this repository.
2. Copy the `src/main/kotlin` files into the app module, adapting package paths. The scheduling and engagement files use `java.time` and target Android API 26+.
3. Copy the tests into the app's `src/test/kotlin` folder and run `./gradlew testDebugUnitTest`. JUnit 4 is required.
4. Add a **new** Room schedule table keyed by habit ID, and migrate/backfill old habits as daily by default. Never drop or reset the existing habit tables. Store rest days with a unique habit/date key.
5. Map existing progress records to `HabitDailyTotal`. It expects **cumulative snapshots**, so sum increments first if your database stores individual progress events.
6. Read persisted progress and schedule data to drive Today, Journey and History. Persist ordered routines separately, preserving existing habit IDs.
7. Use `nextReminder` to calculate the next eligible wall-clock time. **This function does not schedule notifications**: integrate with the existing Android notification worker/alarm receiver and check Android 13+ notification permission and exact alarm policies. Reconcile after reboot, timezone changes and restore.
8. Keep the original launcher icons, logo vectors, splash resources, cover images and charcoal UI unchanged.

## Not implemented by this code pack

No new Compose screens, widgets, notification actions, encryption, account system, advertising SDK, or completed APK. The original project is required to integrate those safely.

## Release gates

- Successful Gradle build and unit tests
- Room migration test using a copy of a real v3.1 database
- Physical-device notification, reboot, battery optimization, permission and timezone testing
- Widget/quick action idempotency checks
- Accessibility, backup/restore, crash and performance testing
- Signed release build and Play Store policy review

This pack is **development code**, not a production-ready release.

# QA Report

## Automated/static checks performed

- Fixed the Material Slider `stepSize` declaration to use the Android framework attribute (`android:stepSize`); the previous `app:stepSize` declaration caused Android resource-linking failure.
- Added `mavenCentral()` to the project-level Gradle repositories so non-Google build dependencies can be resolved.
- All project XML resources were parsed successfully after the fixes.

- Project archive extracted successfully.
- Required Gradle wrapper files are present.
- Android manifest was reviewed for exported components and foreground-service declarations.
- Java sources were scanned for obvious TODO/FIXME markers, unsafe credential patterns, and missing layout IDs.
- All IDs referenced by the main Java activities were checked against the layouts in `app/src/main/res/layout`.
- The polished UI layouts were reviewed for scrollability and adaptive-width behavior.
- Brightness-service behavior was reviewed against the Settings enable/disable preference.
- Internal activity broadcast receivers were changed to `RECEIVER_NOT_EXPORTED`.
- The special-use foreground service now declares the required service-level subtype property.
- The project README was updated to describe the actual test status.

## Build verification note

The project was inspected and the previously reported resource-linking failure was corrected. A full Gradle build could not be executed in this sandbox because the Gradle 8.13 distribution is not cached and outbound access to `services.gradle.org` is unavailable. Final device/emulator smoke testing should be performed in Android Studio after Gradle sync.

## v1.1.3 fixes (this pass)

- **Fixed the "back button doesn't work" loop around brightness settings.** `MainActivity.sync_brightness_service()` was called from both `onCreate()` and `onResume()` and, whenever Brightness Control was enabled but the "Modify system settings" permission hadn't been granted, it automatically re-launched the system permission screen (`ACTION_MANAGE_WRITE_SETTINGS`) every single time. Pressing back from that screen returned to `MainActivity`, which immediately re-triggered the same redirect — so back appeared broken. The auto-redirect from `MainActivity` was removed; the one-time, user-initiated prompt already present in the Settings toggle (`SettingsActivity`) is kept. `MainActivity` now only *starts* the service when permission is already granted, it never forces navigation.
- **Settings screen now activates the service immediately once permission is granted**, via a new `onResume()` check in `SettingsFragment`, instead of waiting for the user to navigate back to `MainActivity` first. This check only starts/stops the background service — it never launches another screen, so it cannot reintroduce a loop.
- **Fixed `minSdk 36`**, which would have made the app un-installable on virtually every real device (API 36 is the newest possible platform). Lowered to `minSdk 26`, matching the API-gated code paths already in the app (foreground services, notification channels, `Build.VERSION_CODES.O`/`M`/`Q` checks).
- Removed the now-unused `android.net.Uri` import from `MainActivity`.
- Bumped `versionCode`/`versionName` to 3 / "1.1.3".

## v1.1.4 fix (this pass)

- **Fixed the settings back-arrow being untappable on modern Android/emulators.** `targetSdk 36` makes edge-to-edge display mandatory (there is no opt-out), and `settings_activity.xml`'s toolbar sat flush at y=0 with no inset handling — so on any device/emulator image with edge-to-edge enforced, the back arrow was rendered underneath the status bar, overlapping the clock/battery area, where touches don't reliably reach it. This wasn't emulator-specific; it affects any Android 15/16-class device or emulator image. Added `EdgeToEdge.enable()` plus a window-insets listener in `SettingsActivity`, `MainActivity`, `ConfigPeriodActivity`, and `DebugActivity` that pads each screen's root view by the system bar insets, so toolbars, buttons, and content sit clear of the status bar and gesture-nav area.

## v1.1.5 fix (this pass)

- **Fixed "Intelligent Sound Manager" appearing twice at the top of the main screen.** The toolbar's title was only being cleared by directly blanking the `Toolbar` view's text (`toolbar.setTitle("")`), which AppCompat can re-apply from the action bar's own state. It's now suppressed properly through the `ActionBar` API (`setDisplayShowTitleEnabled(false)`) in addition to the blank text, so the toolbar can never show a title alongside the app's own "Intelligent Sound Manager" heading text below it. Also hid the system caption bar via `WindowInsetsControllerCompat`, since tablet/desktop-windowing device and emulator configurations can draw their own app-name caption above the app's content, which would look like a second, OS-level copy of the title on top of the app's own header.
- **Renamed the package** from `com.felixr.intelligentsoundmanager` to `com.ethanr.intelligentsoundmanager` (source directories, `applicationId`, `namespace`, manifest action strings, and the instrumentation test's package-name assertion all updated together).
- **Rewrote the README** for GitHub/portfolio use and corrected the stale "Version 1.1.0" line it still had.

## Manual smoke-test checklist

1. Launch the app.
2. Open Single Profile and verify the end-time picker, volume sliders, favorite-contact switch, Cancel, and OK.
3. Configure and enable a Single Profile; verify sound changes and restoration.
4. Configure a Periodic Profile; test both a normal daytime interval and an interval crossing midnight.
5. Use End Now and verify sound restoration.
6. Restart the app and verify active-profile state is restored correctly.
7. Reboot the device/emulator and verify scheduled state recovery where supported.
8. Enable Brightness Control in Settings and verify the system-write permission flow.
9. Disable Brightness Control and verify the foreground service stops.
10. Add the home-screen widget and verify its controls.

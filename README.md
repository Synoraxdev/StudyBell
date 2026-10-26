# StudyBell

Offline student alarm app (Kotlin, Jetpack Compose, Material 3 monochrome). The alarm rings in a loop until you
**press and hold** a fingerprint-style button for 1, 2 or 3 seconds. No accounts, ads, analytics or internet.

* Name / developer: `app/src/main/res/values/strings.xml` (`app_name`, `developer_name`)
* Package / application id: `appPackage` in `app/build.gradle.kts` (move the source folder and `package` lines to match)
* Colors: `core/theme/Theme.kt`
* SDKs: **minSdk 26 (Android 8.0)**, **compileSdk/targetSdk 35 (Android 15)**

## Three different things (do not mix them up)

| What | What it is |
|---|---|
| **Source ZIP** | This project (`StudyBell-source.zip`). Not installable. Push it to GitHub. |
| **Workflow artifact ZIP** | What GitHub lets you download after a build. It is a ZIP that *contains* the APK. Unzip it first. |
| **Installable APK** | `app-debug.apk` inside that artifact. A **debug-signed** build for testing, not a signed production release. |

## Quick start: get the APK from GitHub

1. Create an empty GitHub repository, then in this folder:
   ```bash
   git init && git add . && git commit -m "StudyBell"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
2. Open the repo → **Actions** → *Android CI (debug APK)* → the latest run (it starts automatically on push).
3. When it is green, scroll to **Artifacts** and download `StudyBell-debug-apk` (a ZIP).
4. Unzip it and copy `app-debug.apk` to your phone. Open it and allow "Install unknown apps" for your file manager/browser when asked.

The workflow runs unit tests, lint, then builds the debug APK. Lint never blocks the APK (`abortOnError=false`); its report is in the `reports` artifact.

### Gradle wrapper note

`gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9) is included, but the binary `gradle-wrapper.jar` could not be generated in the
environment that produced this project (no network). The CI works without it (it installs Gradle 8.9 itself). To add it once, on a machine
with Gradle installed:

```bash
gradle wrapper --gradle-version 8.9
git add gradlew gradlew.bat gradle/wrapper && git commit -m "Add Gradle wrapper"
```

## Build locally

Requirements: JDK 17, Android SDK (platform 35, build-tools), Android Studio Ladybug or newer (optional).

```bash
./gradlew testDebugUnitTest       # unit tests
./gradlew lintDebug               # lint
./gradlew assembleDebug           # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
(Use `gradle` instead of `./gradlew` until you generate the wrapper.) Or just open the folder in Android Studio and press Run.

## Release signing (optional)

Never commit keystores or passwords. Create a keystore once:
```bash
keytool -genkeypair -v -keystore studybell-release.jks -alias studybell -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 studybell-release.jks > keystore.b64      # macOS: base64 -i studybell-release.jks
```
In GitHub → Settings → Secrets and variables → Actions add: `KEYSTORE_BASE64` (contents of keystore.b64), `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`. Then run **Actions → Signed release APK (manual) → Run workflow** and download `StudyBell-release-apk`.
Locally, export `STUDYBELL_KEYSTORE_PATH`, `STUDYBELL_KEYSTORE_PASSWORD`, `STUDYBELL_KEY_ALIAS`, `STUDYBELL_KEY_PASSWORD` and run `assembleRelease`.
Without these variables the release build is unsigned and not installable. The debug APK from the normal workflow is **not** a production release.

## Ringtone and licensing

`app/src/main/res/raw/default_ringtone.mp3` is the file you supplied (`iphone.mp3`, 11.6 s, 128 kbps MP3). **I cannot verify who owns it.**
If it is Apple's iPhone tone it is copyrighted, so confirm you have the right to redistribute it (or replace it with a tone you own / a
CC0 tone; keep the filename `default_ringtone.mp3`) before publishing the app or repository publicly.

## Permissions (only what is used)

| Permission | Why | How it is handled |
|---|---|---|
| `POST_NOTIFICATIONS` (runtime, Android 13+) | Ringing alarm notification / return to alarm | Requested from the setup checklist; status re-read on resume |
| `USE_FULL_SCREEN_INTENT` (special access on Android 14+) | Ringing screen over the lock screen | Status shown; button opens the system page |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep audio playing while ringing | Manifest only |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms after restart | Manifest only |
| `VIBRATE` | Vibration | Manifest only |
| `WAKE_LOCK` | `MediaPlayer.setWakeMode` while playing | Released with the player |

Not used: exact-alarm permissions (`setAlarmClock` does not need them), overlay, storage, contacts, camera, microphone, location, biometrics.
Custom audio uses the system pickers (no storage permission) and a persistable read grant.

## Behavior notes and limits

* Alarms are registered with `AlarmManager.setAlarmClock()`. Each alarm has its own PendingIntent (unique data URI + request code = alarm id).
* Repeating alarms schedule their next occurrence **at the moment they fire**, so the chain survives even if the ringing process is killed. One-time alarms switch themselves off when they fire.
* One-time alarm whose time has passed → next valid occurrence (shown in the editor before saving). Weekday alarms → next selected local weekday.
* **Daylight saving:** alarms keep their local wall-clock time. If that time does not exist (spring forward) the alarm rings at the shifted time (02:30 → 03:30); if it occurs twice (fall back) it rings at the first one.
* **Overlap policy:** one audio player. The first alarm rings; others that fire meanwhile queue in firing order and start after the current one is dismissed. Duplicate deliveries are ignored.
* Back/closing the ringing screen does **not** dismiss: the alarm keeps ringing, the notification and a banner on Home return you to it.
* Reboot: alarms are restored after the user unlocks the phone (the database is encrypted until then). Time/timezone changes and app updates also re-register alarms.
* The app cannot ring if the phone is off, the app was force-stopped, the alarm volume is 0, or an OEM kills it. It warns when it can detect muted alarm volume. Do Not Disturb normally allows alarms; check "Alarms" is allowed.
* If the OS kills the service mid-ring the alarm is not resurrected.
* **Accessibility exception:** holding cannot be done with many assistive technologies, so the button also exposes one semantic "Stop alarm now" action (TalkBack double-tap) that stops the alarm without the hold.

## Troubleshooting

* *Alarm screen does not appear over the lock screen:* Settings → Alarm readiness → allow full-screen alarms and notifications.
* *Alarm late/missing:* set battery to Unrestricted, enable Autostart (Xiaomi/Oppo/Vivo/Huawei), do not swipe-force-stop.
* *Silent alarm:* raise the **alarm** volume (not media/ringer).
* *Custom ringtone does not play:* the file became unavailable; the bundled tone is used instead. Pick the file again.
* *CI fails on a dependency or Kotlin error:* open the failed step log; versions are pinned in `build.gradle.kts` / `app/build.gradle.kts`.

More: `docs/ARCHITECTURE.md`, `docs/DEVICE_CHECKLIST.md`, `docs/VALIDATION_REPORT.md`.

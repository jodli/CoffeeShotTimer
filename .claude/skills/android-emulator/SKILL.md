---
name: android-emulator
description: Drive an Android emulator or device from the shell with adb to see a change in the running app. Use when asked to run, screenshot, click through, or visually check an Android app, or to compare before/after, light/dark, landscape or another locale.
---

# Driving an Android emulator

Suggestions from what has worked so far, not rules. Adapt them to the project and improve this file when something works better.

## Setup

- Tools live in the SDK: `$ANDROID_HOME/platform-tools` (adb) and `$ANDROID_HOME/emulator`. If the shell doesn't find them, export `JAVA_HOME` and `ANDROID_HOME` and put both dirs on `PATH` inside the command, since shell state doesn't persist between calls.
- Check for a device with `adb devices`. Boot an AVD in the background if none is running: `emulator -avd <name> -no-snapshot-save &`, then `adb wait-for-device`.
- Build and install with the project's own command (`just install`, `./gradlew :app:install<Flavor>Debug`). If `gradlew` isn't executable, `sh ./gradlew` works.

## This repo

- Dev debug build: `sh ./gradlew :app:installDevDebug`, package `com.jodli.coffeeshottimer.dev.debug`, activity `com.jodli.coffeeshottimer.MainActivity`.
- Demo data: the dev build has a hidden debug dialog on the About screen with a "fill database" action.
- Checks before committing: `just check` (detekt, lint, unit tests).

## Find the app

- The installed package can differ from `applicationId` (flavor and build-type suffixes): `adb shell pm list packages | grep <name>`.
- Launcher activity: `adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER <pkg>`.
- Start clean: `adb shell am force-stop <pkg>; adb shell am start -n <pkg>/<activity>`. `monkey` launches were unreliable.

## Look

- Screenshot: `adb exec-out screencap -p > shot.png`. Downscale before reading it to save tokens: `sips -Z 900 shot.png --out small.png` (macOS) or ImageMagick.
- Keep captures in a scratch dir, with names that say the state (`idle`, `running`, `dark`, `land`, `de`).
- Waits: `sleep 2-5` after launches, theme switches and rotations. Cold starts after a uimode change are slower.

## Act

- Tap by label, not guessed coordinates: dump the UI and read the bounds.
  `adb exec-out uiautomator dump /dev/tty | grep -o 'text="Save"[^>]*bounds="[^"]*"'`
  Tap the centre of `[x1,y1][x2,y2]`. Compose elements without text may only have a `content-desc`.
- Input: `adb shell input tap X Y`, `input swipe X1 Y1 X2 Y2 300` (scroll or drag), `input text 'abc'`, `input keyevent BACK|HOME|ENTER`.
- Long press: a swipe that doesn't move, e.g. `input swipe X Y X Y 800`.

## Vary conditions

- Dark mode: `adb shell cmd uimode night yes|no`
- Landscape: `settings put system accelerometer_rotation 0` then `settings put system user_rotation 1` (0 to go back)
- Per-app locale: `adb shell cmd locale set-app-locales <pkg> --locales de-DE`, then restart the app
- Font scale: `adb shell settings put system font_scale 1.3`
- Reset everything you changed afterwards.

## Workflow that worked

1. Capture a baseline of the target screens before editing.
2. Script the capture run as one shell command (launch, tap, wait, capture per state), so before and after use identical steps.
3. Make the change, reinstall, rerun the same script, compare.
4. Batch the states into one round, fix everything you see, confirm with at most one more round.

## Limits

- Screenshots are stills: animations, frame rate and timing can't be judged reliably. Bursts of captures catch some motion.
- Haptics, real camera, and TalkBack behaviour need a real device or the user.
- Test data: look for a debug "fill database" action or seed script rather than typing data in.

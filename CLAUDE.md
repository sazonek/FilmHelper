# Film Helper

Android toolbox for film photography (Kotlin, Jetpack Compose, Material 3). One person uses it, on their own phone. See README.md for the feature list and roadmap.

## Environment (Windows)

- `JAVA_HOME` is not set system-wide. Before running Gradle, point it at Android Studio's JDK:
  `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'` (PowerShell)
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (set in the git-ignored `local.properties`).
- Use the SDK's adb: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`. The `adb` on PATH (`C:\Tools\adb.exe`) is older, and mixing the two restarts the adb server.

## Build, test, install

```powershell
.\gradlew.bat testDebugUnitTest          # unit tests (all calculation code)
.\gradlew.bat installDebug               # "Film Helper Dev", com.filmhelper.debug
.\gradlew.bat installRelease             # the app the user actually uses, com.filmhelper
```

- Release builds use R8 and are signed with `release.jks` through `keystore.properties`. Both are git-ignored: never commit, print or regenerate them. A new key would force an uninstall that wipes the app's data.
- Debug and release can be installed side by side on the phone.

## Checking changes on the phone

The user's phone is connected over USB. After a UI change, install it and look at it:

- `adb exec-out screencap -p > shot.png` (run through `cmd /c` in PowerShell so the PNG isn't corrupted), then read the image.
- A black screenshot means the phone is locked or the screen is off (`dumpsys window` → `isKeyguardShowing`). Ask the user to unlock it; don't try to unlock it yourself.
- Check `adb logcat -d -b crash` for crashes, especially in release builds (R8).
- Put the app back the way the user left it after tapping through it.

## Code conventions

```
app/src/main/java/com/filmhelper/
  core/      Scales.kt (dials), Format.kt (durations, numbers, parsing)
  flash/     FlashCalculator (math), FlashViewModel, FlashScreen
  exposure/  Exposure (ND, Sunny 16 math), ExposureScreen
  ui/        Controls.kt (shared controls), theme/
```

- Photographic settings are a `Stop(label, value, ev)` on a `StopScale`. Compare, step and snap values in stops (`ev`, with `apertureEv`/`isoEv`/`timeEv`), never by raw value. Use the existing scales: third-stop aperture, ISO and shutter, and `Scales.flashPower`.
- Keep math in plain Kotlin objects with no Android imports, and unit-test every formula in `app/src/test`.
- Screens: `ViewModel` + `StateFlow` for real state. Text inputs stay as typed strings and go through `parsePositive`, which accepts both `2.8` and `2,8`. Simple tool screens can keep state in `rememberSaveable`.
- Reuse the controls in `ui/Controls.kt` (`CompactStepper`, `StepperRow`, `NumberField`, `Picker`, `SectionCard`, `ResultCard`) before adding new ones.
- New dependencies go in `gradle/libs.versions.toml`. CameraX, Room, Navigation Compose and DataStore are planned, not yet added.

## UI preferences (from the user)

- Every screen should fit on one phone screen (1080×2392, 420 dpi) without scrolling. Keep the scroll modifier only for smaller screens. The user notices even a few dp of pointless scroll.
- No fold-out or "more options" sections: options are ordinary controls next to the others.
- Prefer compact controls two per row (`CompactStepper`) and short labels that fit on one line.
- Flash tab workflow: gear is fixed (GN, ISO, aperture) and the subject moves. The user mostly solves for power by changing distance, so the result comes first and distance second.
- Defaults the user picked: GN 14 m, ISO 400, f/5.6, full-stop power steps, lowest power 1/64.
- Result cards: one big value plus one short detail line. Don't repeat the same number on several lines.

## Decisions

- Reciprocity correction was removed on purpose (commit `cd719f7`). The full implementation, with film data and tests, is in `bbb192d` if it's wanted again.
- Package name `com.filmhelper`. minSdk 26, compile/target SDK 37.

## Git

- Work on `main`, which tracks `origin/main` on GitHub (`sazonek/FilmHelper`).
- Commit and push only when asked, one logical change per commit.

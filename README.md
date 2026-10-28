# Film Helper

An Android toolbox for film photographers: manual flash and exposure calculators, with a roll log and light meter planned.

## Stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose, Material 3 |
| Build | Gradle 9.8 (wrapper), Android Gradle Plugin 9.5 with built-in Kotlin, version catalog in `gradle/libs.versions.toml` |
| SDK | compileSdk / targetSdk 37, minSdk 26 (Android 8.0) |
| State | `ViewModel` + `StateFlow` |
| Tests | JUnit 4 unit tests for all calculation code |

Planned additions as features need them: Navigation Compose, Room (roll log), DataStore (saved flash/gear presets), CameraX (light meter).

## Features

### Flash (v0.1)
Pick the value to solve for (distance, aperture, power or ISO), enter the rest, and get the nearest marked setting.

- Guide number at ISO 100, in metres or feet (switching converts the values)
- Power from 1/1 down to 1/16 … 1/256, in full or 1/3 stops (`1/8 +0.7` style labels)
- Light loss for diffusers, bounce or gels, in 1/3 stops
- Shows the exact value, how far the nearest setting is off (EV) and warns when the answer is beyond the dial
- Distance answers include the ±1/3 EV range

Formula: `GN × √(ISO/100) × 2^(−(power stops + loss)/2) = distance × f-number`.

### Exposure (v0.1)
- **Reciprocity**: Ilford/Kentmere exponent formulas, Fujifilm Acros II, or a custom exponent; quick-reference table
- **ND filter**: metered shutter + ND stops, optional reciprocity correction
- **Sunny 16**: shutter speed for a scene, ISO and chosen aperture

### Roadmap
- Roll and shot log (cameras, lenses, film, per-frame settings, GPS, CSV export)
- Development timer with push/pull and temperature compensation
- Camera light meter (CameraX), spot metering
- Saved flash presets (GN per flash and zoom setting)

## Project layout

```
app/src/main/java/com/filmhelper/
  core/        dials (f-stops, ISO, shutter, flash power) and formatting
  flash/       flash calculator, ViewModel and screen
  exposure/    reciprocity, ND and Sunny 16 math and screen
  ui/          shared Compose controls and theme
```

## Building

Open the folder in Android Studio, or from a terminal (Android Studio's bundled JDK works):

```bash
./gradlew testDebugUnitTest assembleDebug
```

Install on a phone with USB debugging enabled:

```bash
./gradlew installDebug
```

---
name: compose-catalog
description: Build, install, and run the Compose Material Catalog application on connected devices or emulators.
---

# Compose Material Catalog Skill

This skill explains how to build, install, and run the Compose Material Catalog application on connected devices and emulators.

---

## Important Rules

1. **Target the Application Module**:
   - The application module is `:compose:integration-tests:material-catalog`.
   - Do **NOT** attempt to run `installDebug` on library modules like `:compose:material3:material3:integration-tests:material3-catalog` or `:compose:material:material:integration-tests:material-catalog` (they are libraries and do not have an `installDebug` task).
2. **Scoping**:
   - Always use `PROJECT_PREFIX=:compose:` to scope Gradle configuration and avoid slow repo-wide configuration.

---

## Installation via Gradle

To build and install the debug APK directly to a connected device or running emulator:

```bash
PROJECT_PREFIX=:compose: ./gradlew :compose:integration-tests:material-catalog:installDebug
```

---

## Installation via ADB

If you build the APK (e.g. via `PROJECT_PREFIX=:compose: ./gradlew :compose:integration-tests:material-catalog:assembleDebug`) and want to install it manually using `adb`:

- **APK Location**:
  `../../out/androidx/compose/integration-tests/material-catalog/build/intermediates/apk/debug/material-catalog-debug.apk`
- **Important**: The catalog APK is marked `testOnly`, so `adb install` requires the **`-t`** flag.

```bash
adb install -t -r -d ../../out/androidx/compose/integration-tests/material-catalog/build/intermediates/apk/debug/material-catalog-debug.apk
```

---

## Launching the Catalog App

- **Package Name**: `androidx.compose.material.catalog`
- **Main Activity**: `androidx.compose.material.catalog.CatalogActivity` (or `.CatalogActivity`)

To launch the catalog app on the device via ADB:

```bash
adb shell am start -n androidx.compose.material.catalog/.CatalogActivity
```

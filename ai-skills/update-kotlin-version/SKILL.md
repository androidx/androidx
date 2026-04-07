---
name: update-kotlin-version
description: This skill should be used when the user asks to "update Kotlin version", "upgrade Kotlin", "change Kotlin to X.Y.Z", or mentions updating the Kotlin compiler version in the project.
version: 1.0.0
---

# Update Kotlin Version — Compose Multiplatform Core

This skill helps update the Kotlin version across the project. The process involves updating several files to ensure consistency.

## Important Rule

`JETBRAINS_COMPILE_KOTLIN_VERSION` must not be the latest version, but rather the newest - 1. For example, if we use Kotlin 2.3.20 as the latest, `JETBRAINS_COMPILE_KOTLIN_VERSION` should be `KOTLIN_2_2`.

## Required Information

Before starting, ask the user for:
- The target Kotlin version (e.g., "2.3.20")

## Update Process

Perform the following steps in order:

### 1. Update `gradle/libs.versions.toml`

Update three main entries in the `[versions]` section:

a. **Update `composeCompilerPlugin`** to the new Kotlin version:
```toml
composeCompilerPlugin = "X.Y.Z"
```

b. **Update `kotlin`** to the new Kotlin version:
```toml
kotlin = "X.Y.Z"
```

c. **Update or add patch versions** (under the comment `# Use the most up-to-date patch`):
- If a version entry for this major.minor already exists (e.g., `kotlin23` for 2.3.x), update it
- If this is a new major.minor version, add a new entry (e.g., `kotlin24 = "2.4.Z"`)

Example:
```toml
# Use the most up-to-date patch
kotlin21 = "2.1.20"
kotlin22 = "2.2.20"
kotlin23 = "2.3.20"
kotlin = "2.3.20"
```

### 2. Update `buildSrc/public/src/main/kotlin/org/jetbrains/androidx/build/JetBrainsCompatibilityVersions.kt`

Update `JETBRAINS_COMPILE_KOTLIN_VERSION` to be the **previous major.minor version**, not the latest:

```kotlin
val JETBRAINS_COMPILE_KOTLIN_VERSION = KOTLIN_X_Y
```

For example:
- If updating to Kotlin 2.3.20, set to `KOTLIN_2_2`
- If updating to Kotlin 2.4.0, set to `KOTLIN_2_3`

### 3. Check `buildSrc/public/src/main/kotlin/androidx/build/AndroidXConfiguration.kt`

If this is a new major.minor version (e.g., moving from 2.3 to 2.4):

a. Add a new enum entry in the `KotlinTarget` enum:
```kotlin
KOTLIN_2_4(KotlinVersion.KOTLIN_2_4, "kotlin24"),
```

b. Update the `LATEST` entry to point to the new version:
```kotlin
LATEST(KOTLIN_2_4);
```

If this is just a patch update (e.g., 2.3.10 → 2.3.20), no changes are needed in this file.

### 4. Verify the Changes

After making all updates:
1. Confirm that `JETBRAINS_COMPILE_KOTLIN_VERSION` is set to the previous major.minor version, not the latest
2. Verify that all three version entries in `libs.versions.toml` are updated consistently
3. If a new major.minor version was added, confirm the `KotlinTarget` enum includes it

## Key Files

- `gradle/libs.versions.toml` - Version catalog with Kotlin versions
- `buildSrc/public/src/main/kotlin/org/jetbrains/androidx/build/JetBrainsCompatibilityVersions.kt` - Compatibility version settings
- `buildSrc/public/src/main/kotlin/androidx/build/AndroidXConfiguration.kt` - Kotlin target configuration (updated only for new major.minor versions)

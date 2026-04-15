---
name: health-connect
description: Comprehensive guide for Health Connect Jetpack SDK development. Includes AOSP workflow, feature implementation, naming standards, testing, and release phase requirements (Alpha/Beta/Experimental). Use when adding or modifying any Health Connect APIs in AndroidX.
---

# Health Connect Jetpack SDK Development

## Overview
This skill defines the end-to-end workflow for developing Health Connect features in the AndroidX repository (`frameworks/support/health/connect/`).

## 1. Development Environment & Workflow
*   **Root Directory**: Always work from `frameworks/support/`.
*   **Git Strategy**:
    *   Start a new branch: `repo start <branch_name> .`
    *   First commit: `git commit`
    *   Subsequent changes: `git commit --amend`
    *   Upload for review: `repo upload --cbr -t .`
*   **Sync to google3**: Submission in AOSP triggers a "Safe Review" CL in Piper via Copybara. Approval in Piper triggers auto-submit and TAP.

## 2. Feature Implementation Workflow
### Step 1: Platform API Availability
*   Ensure backing platform APIs are available. If not, perform a self-service prebuilt drop (`go/androidx/playbook/update_sdk`).
*   Map the version in `HealthConnectFeatures.kt` (e.g., Baklava/Android 37 or specific U/V extensions).

### Step 2: Define Feature & Versioning
*   Add `FEATURE_*` constant to `HealthConnectFeatures.kt`.
*   Update `FEATURE_TO_VERSION_INFO_MAP`.
*   **Access Control**: Annotate every new public symbol with `@RestrictTo(RestrictTo.Scope.LIBRARY)` during development to prevent unintentional release.

### Step 3: Implement Models & Client
*   **Model Packaging**: Create required Request/Response classes in a feature-specific package (e.g., `androidx.health.connect.client.<feature_name>`). This is the preferred approach for modularity and clarity.
*   Add methods to `HealthConnectClient.kt`.
*   **Intent Constants**: Descriptive internal constants (e.g., `ACTION_HEALTH_CONNECT_MATCHMAKING`) should be used for platform delegation.

### Step 4: Logic & Delegation
*   **Platform-Only Implementation**: New features are developed for the platform (Android U+ / API 34+) using the system service. **Do not** add new feature logic to the legacy APK-based path (`HealthConnectClientImpl.kt`).
*   Implement logic in `HealthConnectClientUpsideDownImpl.kt`.
*   Check platform API availability before calling platform methods; throw `UnsupportedOperationException` if unavailable.
*   **Note**: This architectural shift ensures features leverage the system service and modern OS extensions.

## 3. Release Phases (Alpha vs. Beta)
### Alpha Release
*   Remove `@RestrictTo(RestrictTo.Scope.LIBRARY)`.
*   If the API is still evolving or not ready for final stable release, mark with an experimental annotation (e.g., `@ExperimentalMatchmakingApi`).
*   Submit for API review (`go/hc-jetpack-sdk-review-process`).

### Beta/RC/Stable Release
*   **Experimental Annotation**: If the SDK version is Beta, RC, or Stable, new APIs **must** be marked with an experimental annotation *unless* they are fully ready for a stable commitment.
*   Remove `@RestrictTo` only after experimental annotations and API reviews are complete.

## 4. Critical Commands
| Task | Command |
| :--- | :--- |
| **Format** | `./gradlew :ktFormat` or `./gradlew :ktCheckFile --format --file <path>` |
| **Build** | `./gradlew :health:connect:connect-client:assemble` |
| **Update API** | `./gradlew :health:connect:connect-client:updateApi` |
| **Unit Test** | `./gradlew :health:connect:connect-client:test` |
| **Instrumentation Test** | `./gradlew :health:connect:connect-client:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<class>` |

## 5. Testing Standards
*   **Emulator/Device Tests**: Required for platform-delegated APIs. Use real devices or emulators with the latest system images.
*   **Suppression**:
    *   Use `assumeTrue(Build.VERSION.SDK_INT >= X)` for platform release features.
    *   Use `assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.U) >= Y)` for extension features.
*   **Fakes**: Always update `FakeHealthConnectClient.kt` for new APIs.


# Project: Material Android Compose

This directory contains the core implementation of the Material 3 design system for Jetpack Compose. All code generated or modified must strictly follow the Material 3 Spec and AOSP coding standards.

## General Instructions
* When modifying any .kt file, format it via the following command: `./gradlew :ktCheckFile --format --file <file>`. If more than one file needs formatting, continue adding `--file <next-file>` to the command.
* For any of the instructions below, assume that any of the commands you run should be run in the androidx-main/frameworks/support directory
* If changing the API in any way or if asked to update the public API for any changes under compose/material3, run `./gradlew compose:material3:material3:updateApi` and `./gradlew :compose:material3:material3:updateAbiNative`
* After making a change, make sure that it builds by running `./gradlew compose:material3:material3:compileDebugKotlinAndroid` from /androidx-main/frameworks/support.
* When moving files, use git mv to keep version control history.
* Do not make a git commit unless specifically requested.

## Coding Guidelines
* Always make changes opt-in and non-breaking to APIs if possible
* Follow the style guidelines at https://developer.android.com/develop/ui/compose/api-guidelines and https://developer.android.com/kotlin/style-guide
* Warn the user if you are changing the dependency version of another library to a non-stable release. Before the Material3 library can enter the pre-release beta/rc then stable, all dependency APIs and libraries need to be: 1) not experimental 2) released in a beta/rc/stable release.

## Implementation Workflows
Before implementation, determine the scope of the request:
* New Material Component: A standalone UI element defined in the Material 3 specification (e.g., a new type of Card or Picker). This requires the full workflow (Source, Test, Sample, Catalog).
* Feature Update: Adding parameters, state, or logic to an existing component. This requires updating the source and existing tests/samples.
* Utility/Internal Composable: A helper composable that is not a public-facing Material component. This requires source and unit tests, but no samples or catalog entries.

You **must** complete the implementation workflow as described in the sections below. Explicit instructions to edit a specific file (e.g., "Add to Foo.kt") define which files should be updated, but do **not** exempt you from creating or updating the required source, tests, samples, and catalog entries.

### Creating New Material Components
When creating a New Component that does not exist, follow these steps:
1. Source: Create the composable in compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/.
2. Screenshot Tests: Create a test file in compose/material3/material3/src/androidDeviceTest/kotlin/androidx/compose/material3/. Include:
   * Light & Dark mode screenshots.
   * Right-to-Left (RTL) layout screenshots.
   * A // TODO for behavioral/interaction tests.
3. Samples: Add a usage example in compose/material3/material3/samples/src/main/java/androidx/compose/material3/samples/.
4. Catalog Integration: Register the component and its examples in:
   * compose/material3/material3/integration-tests/material3-catalog/src/main/java/androidx/compose/material3/catalog/library/model/Examples.kt
   * compose/material3/catalog/library/model/Components.kt
5. Documentation: Use the @sample KDoc tag in the source code to link to the new sample.
6. API Update: Update the API signature files as per the general instructions.

### Creating New Material Variant
If you are not sure if the requested component is a variant or a new component, ask the user before following the steps. If the user's request is a variant of an existing component, follow the same steps as for "Creating New Material Components", but with the existing component's files.

### Adding Features to Existing Components
If the request is an update to an existing component:
1. Modify Source: Update the existing file in compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/.
2. Update Tests: Add new test cases to the existing test file in compose/material3/material3/src/androidDeviceTest/kotlin/androidx/compose/material3/.
3. Update Samples: If the feature changes how the component is used, update the relevant sample file in compose/material3/material3/samples/src/main/java/androidx/compose/material3/samples/.
4. API Update: If the API has changed, update the API signature files as per the general instructions.

### Non-Component Composables
If the composable is a private helper or a non-Material utility:
1. Update the relevant file in compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ or create a new file if a relevant file doesn’t exist.
2. Update tests in compose/material3/material3/src/androidDeviceTest/kotlin/androidx/compose/material3/.
3. Do not add to Samples
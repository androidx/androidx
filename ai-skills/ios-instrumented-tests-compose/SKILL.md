---
name: ios-instrumented-tests
description: This skill should be used when the user asks about "running iOS tests", "iOS instrumented tests", "how to run ios tests", "launch specific ios test", "add new ios instrumented test" and these tests are located in the "compose/ui/ui/src/uikitInstrumentedTest" directory.
version: 1.0.1
---

iOS Instrumented tests are designed to run XCTests written on Kotlin using the iOS Simulator as an environment.

# Key Test Source Locations
(from the repository root)
`compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/Configuration.kt` - location of the configuration file which determines which tests to run.
`compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/test` - Test environment and utilities to configure tests
`testutils/testutils-xctest/` - Contains a Kotlin wrapper that allows converting Kotlin tests to XCTests as well as an Objective-C project that provides API for low-level touches and other input gesture simulation.

# Running iOS Instrumented Tests

Perform steps exactly in the following order:

## Configure which tests should be started
In the file compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/Configuration.kt write all tests that must be launched. Add corresponding imports in the header. Save the file. If it's needed to run all instrumented, pass nothing to the setupXCTestSuite. 
Follow the examples in commented code in the file. Note: configuration can launch either entire test classes or multiple test cases from a single test class only.

## Running All iOS Tests

1. Get a list of all devices and select the most appropriate one. Prefer the latest iPhone with the latest iOS if nothing else said. Select and remember corresponding simulator id for the next step.
```
xcrun xctrace list devices
```

2. Navigate to the launcher repository root:

```bash
cd compose/ui/ui/src/uikitInstrumentedTest/launcher
```

3. Build and run tests
```
xcodebuild test -scheme Launcher -project Launcher.xcodeproj -destination 'platform=iOS Simulator,id=<Simulator ID>'
```

# Creating iOS Instrumented Tests

The main class that creates environment to run tests is `compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/test/UIKitInstrumentedTest.kt`

When writing a new test or updating an existing one, prefer using the helpers from the following places:
- `compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/test/AccessibilityTestNode.kt`
- `compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/test/UIKitInstrumentedTest.kt`
- `compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/test`

In the test code avoid using the API from the "testutils-xctest" project directly.
If there is no corresponding API wrapper in the `compose/ui/ui/src/uikitInstrumentedTest/kotlin/androidx/compose/ui/test`, add it.

## When a test is created or updated

After the test is created or updated, run it using the steps from the "Running iOS Instrumented Tests".

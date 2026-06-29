---
name: run_tests
description: A skill for identifying and running tests (Unit, Instrumentation, FTL) in the AndroidX repository.
---

# Run Tests Skill

This skill provides comprehensive instructions for executing tests within the AndroidX repository. It covers module discovery, unit testing, instrumentation testing on connected devices, and remote testing via Firebase Test Lab (FTL).

## 1. How to Find and Run a Specific Test

If you have a specific failing test (e.g., `BasicTextFieldTest#longText_doesNotCrash_singleLine`) and want to execute it:

1.  **Find the Test File**: Use code search, `find_declaration`, or `find_files` (e.g., search for `BasicTextFieldTest.kt`).
2.  **Identify the Module**: The file path determines the Gradle project (e.g., `compose/foundation/foundation/src/...` belongs to `:compose:foundation:foundation`).
3.  **Identify Test Type**:
    *   If the test is in a `test`, `androidHostTest`, or `jvmTest` folder, it is a Unit Test.
    *   If the test is in `androidTest` or `androidDeviceTest`, it is an Instrumentation Test.
4.  **Identify Module Type**: Check if the module is KMP or standard Android (see details below) by inspecting `build.gradle` for `androidXMultiplatform {` or running `./gradlew <module>:tasks | grep "test"`.
5.  **Formulate the Task**: Select the proper Gradle task (e.g., `testAndroidHostTest` vs `test`, `connectedAndroidDeviceTest` vs `connectedAndroidTest`).
6.  **Filter by Class/Method**:
    *   **Class**: Append filtering arguments. E.g., for instrumentation: `-Pandroid.testInstrumentationRunnerArguments.class=androidx...BasicTextFieldTest`. For unit tests: `--tests "androidx...BasicTextFieldTest"`.
    *   **Method**: For instrumentation, append `#<method_name>` to the class name (e.g., `...BasicTextFieldTest#longText_doesNotCrash_singleLine`). For unit tests, append `.<method_name>` to the class name in `--tests` (e.g., `--tests "...BasicTextFieldTest.longText_doesNotCrash_singleLine"`).

## 2. Module Discovery

Before running tests, you must identify the Gradle project name (module) associated with the code you are testing.

*   **Mapping a path to a project**: Most directories in this repository are Gradle projects. Use the directory structure as a guide (e.g., `appcompat/appcompat` corresponds to `:appcompat:appcompat`).
*   **Verification**: Run `./gradlew projects` to see a full list of projects. Because the output is very long, consider chaining a `grep` command to filter the output if you are looking for a specific module (e.g., `./gradlew projects | grep appcompat`).
*   **Module Type**: Be aware if the module is a standard Android library (like `appcompat`) or a Kotlin Multiplatform (KMP) library (like `compose:foundation:foundation`). Task names differ significantly. To determine if a module is KMP, you can run `./gradlew <project-name>:tasks | grep "test"`: if you see tasks like `testAndroidHostTest` or `jvmTest`, it is a KMP module. If you see standard `test` and `connectedAndroidTest` tasks, it is a standard Android module. Alternatively, inspect its `build.gradle` file for `androidXMultiplatform {`.

## 3. Unit Testing (JVM)

Unit tests run on the host machine and are the fastest way to verify logic.

*   **Standard Android Modules**:
    ```bash
    ./gradlew <project-name>:test
    ```
*   **KMP Modules**:
    ```bash
    ./gradlew <project-name>:testAndroidHostTest
    ./gradlew <project-name>:jvmStubsTest
    ```
*   **Filtering**:
    Use the `--tests` filter. You can also append `.<method_name>` for specific methods.
    ```bash
    ./gradlew <project-name>:test --tests "androidx.example.MyTest"
    ./gradlew <project-name>:testAndroidHostTest --tests "androidx.example.MyTest"
    ./gradlew <project-name>:test --tests "androidx.example.MyTest.myMethod"
    ```

## 4. Instrumentation Testing (Connected Devices)

These tests run on a physical device or emulator connected via ADB.

*   **Standard Android Modules**:
    ```bash
    ./gradlew <project-name>:connectedAndroidTest
    ```
*   **KMP Modules**:
    ```bash
    ./gradlew <project-name>:connectedAndroidDeviceTest
    ```
*   **Filtering**:
    Use the `android.testInstrumentationRunnerArguments.class` property. For specific methods, append `#<method_name>`.
    ```bash
    ./gradlew <project-name>:connectedAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=androidx.example.MyTest
    ./gradlew <project-name>:connectedAndroidDeviceTest \
        -Pandroid.testInstrumentationRunnerArguments.class=androidx.example.MyTest#myMethod
    ```

## 5. Remote Testing (Firebase Test Lab)

AndroidX provides specialized tasks for running **instrumentation tests** on Firebase Test Lab (FTL). FTL is not used for local unit tests. The task suffix changes based on module type.

*   **Standard Android Modules**: `ftl<device><api>releaseAndroidTest`
*   **KMP Modules**: `ftl<device><api>androidDeviceTest`
*   **Listing Available Combinations**:
    To see the full list of available FTL tasks for a specific project, you can run:
    ```bash
    ./gradlew <project-name>:tasks --all | grep ftl
    ```
*   **Common Device/API combinations**:
    - `mediumphoneapi36`
    - `mediumphoneapi35`
    - `mediumphoneapi34`
    - `mediumphoneapi30`
    - `nexus5api23`
*   **Example (KMP)**:
    ```bash
    ./gradlew <project-name>:ftlmediumphoneapi35androidDeviceTest --className="androidx.example.MyTest"
    ```

### 5.1. Reproducing Flakes on FTL

To verify a flaky test, you can run it multiple times on Firebase Test Lab using the `ftlOnApis` task variant.

1.  **Parameterize the Test**: To run a test N times, temporarily modify the test class to be parameterized:
    *   Add `@RunWith(Parameterized::class)` to the class.
    *   Update the constructor to accept a repetition parameter: `class MyTest(private val repetition: Int)`.
    *   Add the companion object for data generation:
        ```kotlin
        import org.junit.runners.Parameterized

        companion object {
            private const val RUNS = 100 // Adjust as needed
            @JvmStatic
            @Parameterized.Parameters
            fun data(): Array<Int> = Array(RUNS) { 0 }
        }
        ```
    *   *Note*: If there are many tests in the class but you only want to focus on one, comment out the `@Test` annotation on the irrelevant ones. DO NOT REPLACE ANY LINES OF CODE OR VARIABLES NAMES in the test class aside from making it parameterized. Test class name should remain the same.
2.  **Run Locally (Optional)**: Verify it runs a few times locally before deploying to FTL. E.g., for KMP:
    ```bash
    ./gradlew <project-name>:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=androidx.example.MyTest
    ```
3.  **Run on FTL**: Use the `ftlOnApis` task. Specify the API level(s) with `--api` and add a longer timeout `--testTimeout=1h` (if the API level is not provided, you must ask the user for it).
    ```bash
    # Example for KMP (append 'releaseAndroidTest' instead of 'androidDeviceTest' for standard Android)
    ./gradlew <project-name>:ftlOnApisandroidDeviceTest --testTimeout=1h --api 28 --api 30 --className=androidx.example.MyTest
    ```

## 6. Screenshot / Visual Tests

Screenshot tests (e.g., using `AndroidXScreenshotTestRule`) compare the rendered UI against approved "golden" reference images to detect visual regressions.

*   **Emulator Requirement**: Screenshot tests must be executed on a specific emulator configuration to ensure consistent rendering. Locally, you **must** use a **Medium Phone API 35** emulator.
*   **Managing the Emulator**: Use the [android-cli](https://developer.android.com/tools/agents/android-cli) skill to manage and run the emulator.
    *   To list available virtual devices:
        ```bash
        android emulator list
        ```
    *   To launch the required emulator:
        ```bash
        android emulator start <device_name>
        ```
*   **Running the Tests**: Once the emulator is running and connected, execute the screenshot tests as standard instrumentation tests using Gradle (see Section 4).
*   **Updating Screenshot Goldens**:
    If a screenshot test fails due to intentional UI changes, you must update the golden reference images in the `support-goldens` repository (which is checked out as a sibling `golden` directory to `frameworks/support`).

    1.  **Run Tests (Trigger Failure)**: Execute the screenshot tests locally on the **Medium Phone API 35** emulator. If you are introducing a new screenshot or expecting a change, the test must fail to write output files. If the test passes but you want to recreate the golden, delete the local golden image first to trigger a `MISSING_REFERENCE` failure.
    2.  **Pull Test Outputs**: When a test fails, `ScreenshotTestRule` writes the actual screenshot, expected screenshot, and a mapping `.textproto` file to the device. The output directory is defined in `ScreenshotTestRule.kt` as:
        ```kotlin
        val deviceOutputDirectory
            get() =
                File(
                    InstrumentationRegistry.getInstrumentation().getContext().externalCacheDir,
                    "androidx_screenshots",
                )
        ```
        To pull these output files to your workstation, run:
        ```bash
        adb pull /sdcard/Android/data/<test_package_name>/cache/androidx_screenshots/
        ```
        *(Note: `<test_package_name>` is the identifier of the test APK, e.g., `androidx.compose.material.test`)*
    3.  **Map and Copy to Goldens Repo**: Locate the generated `*_diffResult_goldResult.textproto` file for the failed test. It explicitly maps the actual screenshot filename on the device to its repo destination. For example:
        - `image_location_test`: `"androidx.compose.material.CheckboxScreenshotTest_checkBoxTest_checked_emulator_b294d33ecd4f4764_actual.png"`
        - `image_location_golden`: `"compose/material/material/checkbox_checked_emulator.png"`

        Rename the pulled `*_actual.png` image to match the filename in `image_location_golden` (e.g., `checkbox_checked_emulator.png`) and copy it to its location in the sibling `golden` project repository (e.g., `../../golden/compose/material/material/`).
    4.  **Submit Linked CLs via Shared Topic**:
        To submit your code changes and golden image updates together (ensuring presubmit runs them as a unit), upload them to Gerrit using a **shared topic**:
        - Code CL (under `frameworks/support`): `repo upload --cbr -t <my_shared_topic>`
        - Golden CL (under `golden/`): `repo upload --cbr -t <my_shared_topic>`

        > [!NOTE]
        > Some modules may use custom wrapper rules (such as `RemoteScreenshotTestRule` in `wear/compose/remote/remote-material3`). These custom rules delegate to the same core `ScreenshotTestRule` library under the hood. They write their outputs to the same `androidx_screenshots` cache directory on the device and generate identical `.textproto` mapping files.


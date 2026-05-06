---
name: build_screenshot_tests
description: A skill for building instrumented screenshot tests in Remote Compose using RemoteComposeScreenshotTestRule.
---

# Building Screenshot Tests using RemoteComposeScreenshotTestRule

> [!IMPORTANT]
> **AI INSTRUCTION:** Do not assume you should use `RemoteComposeScreenshotTestRule` for all tests. If the user asks you to write a screenshot test, you MUST first explicitly ask them: "Would you like me to use the `RemoteComposeScreenshotTestRule` for this test?" Proceed with using this skill only if they confirm.

This skill provides guidelines for building screenshot tests in the `@compose/remote/remote-creation-compose` project.

## Purpose

`RemoteComposeScreenshotTestRule` is a JUnit rule that allows taking screenshots of Remote Compose components. It handles:
1.  Capturing the remote document.
2.  Rendering it using `RemoteDocumentPlayer`.
3.  Verifying the rendered image against a "golden" screenshot.

## Setup

1.  **Test Class Annotations**:
    Annotate the test class with `@MediumTest`, `@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)`, and `@RunWith(AndroidJUnit4::class)`.

2.  **Add the Rule**:
    Define the rule inside the test class.
    ```kotlin
    @get:Rule
    val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.999),
        )
    }
    ```
    *Note: `SCREENSHOT_GOLDEN_DIRECTORY` is usually defined in the module, e.g., `androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY`.*

## How to use `RemoteComposeScreenshotTestRule`

### 1. Basic Usage (Direct Testing)

Use `runScreenshotTest` and provide a lambda with the Remote Composable content you want to test.

```kotlin
@Test
fun simpleTest() {
    composeTestRule.runScreenshotTest {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize().background(Color.Red)
        )
    }
}
```

### 2. Overriding Configuration

You can override profile, layout direction, or offer outer content if needed.

```kotlin
@Test
fun alignByBaseline() {
    composeTestRule.runScreenshotTest(profile = TestProfiles.androidXExperimental) {
        RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
            RemoteRow(modifier = RemoteModifier.fillMaxWidth()) {
                RemoteText(
                    text = "Large String",
                    fontSize = 40.rsp,
                    modifier = RemoteModifier.alignByBaseline(),
                )
                RemoteText(
                    text = "Small String",
                    fontSize = 14.rsp,
                    modifier = RemoteModifier.alignByBaseline(),
                )
            }
        }
    }
}
```

### 3. Grid Testing (Optional)

For testing multiple variations efficiently, you can combine this rule with `GridScreenshotUI`.
Refer to the `build_grid_screenshot_tests` skill for detailed instructions on using the grid utility.

## Best Practices

-   **Thresholds**: Use `MSSIMMatcher(threshold = 0.999)` for high-precision matching.
-   **Golden Directory**: Ensure `moduleDirectory` points to the correct golden assets folder.
-   **Remote Scope**: Inside the `runScreenshotTest` lambda, you are in a `@RemoteComposable @Composable` scope. Make sure to use `RemoteModifier` and Remote components (e.g. `RemoteBox`, `RemoteText`).

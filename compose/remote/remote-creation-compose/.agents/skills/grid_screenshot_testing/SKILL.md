---
name: build_grid_screenshot_tests
description: A skill for building instrumented tests in Remote Compose using GridScreenshotUI.
---

# Building Instrumented Tests using GridScreenshotUI

> [!IMPORTANT]
> **AI INSTRUCTION:** Do not assume you should use `GridScreenshotUI` for all tests in this directory. If the user asks you to write a screenshot test, you MUST first explicitly ask them: "Would you like me to use the `GridScreenshotUI` utility for this test?" Proceed with using this skill only if they confirm.

This skill provides guidelines for building screenshot tests using `GridScreenshotUI` in the `@compose/remote/remote-creation-compose` project.

## Purpose

`GridScreenshotUI` is a utility class designed to lay out multiple small remote UI components in a grid. This is particularly useful for screenshot testing as it allows you to capture many variations of a component (e.g., different alignments, modifiers, or arrangements) in a single screenshot, making tests more efficient and easier to compare visually.

## How to use `GridScreenshotUI`

1. **Test Class Setup**:
   Create a test class annotated with `@MediumTest`, `@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)`, and `@RunWith(AndroidJUnit4::class)`.

2. **Add the Screenshot Rule**:
   Define a `RemoteComposeScreenshotTestRule` explicitly specifying the module directory and matcher.
   ```kotlin
   @get:Rule
   val composeTestRule: RemoteComposeScreenshotTestRule by lazy {
       RemoteComposeScreenshotTestRule(
           moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
           matcher = MSSIMMatcher(threshold = 0.999),
       )
   }
   ```

3. **Instantiate GridScreenshotUI**:
   Create an instance of `GridScreenshotUI` in your test class.
   ```kotlin
   private val gridScreenshotUI = GridScreenshotUI()
   ```

4. **Define your UI variations**:
   Create a method or variable that provides a list of `Pair<String, @RemoteComposable @Composable () -> Unit>`. The string is the label for the variation, and the lambda is the actual Compose UI content.
   You can use the `.toInput()` extension functions defined in `GridScreenshotUI.Companion` to easily convert a list of composables into the required pair format if you don't want to specify labels manually.

5. **Write the Test**:
   Use `composeTestRule.runScreenshotTest` and call `gridScreenshotUI.GridContent(...)` with your list of variations.
   ```kotlin
   @Test
   fun exampleGridTest() =
       composeTestRule.runScreenshotTest {
           gridScreenshotUI.GridContent(getLayoutAlignmentUIs())
       }
   ```

## Best Practices
- **Reuse Dimensions**: Use `GridScreenshotUI.Companion.DefaultContainerSize` to maintain consistent container dimensions across tests.
- **RTL Testing**: You can easily test RTL (Right-to-Left) layouts by passing `layoutDirection = LayoutDirection.Rtl` to `GridContent`.
  ```kotlin
  gridScreenshotUI.GridContent(
      getLayoutAlignmentUIs(),
      layoutDirection = LayoutDirection.Rtl,
  )
  ```
- **Helper methods**: Create builder methods to generate the list of `Pair` items if you have a combinatorial explosion of parameters (like trying out all combinations of `Arrangements` and `Alignments` through `sequence`).
  ```kotlin
  private fun getLayoutAlignmentUIs(): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
      sequence {
          for (alignment in alignments) {
              for (arrangement in arrangements) {
                  yield(
                      "${alignment.propertyName()} ${arrangement.propertyName()}" to
                          @RemoteComposable @Composable {
                              RemoteRow(
                                  modifier = RemoteModifier.size(DefaultContainerSize),
                                  horizontalArrangement = arrangement,
                                  verticalAlignment = alignment,
                              ) {
                                  // Your content here
                              }
                          }
                  )
              }
          }
      }.toList()
  ```

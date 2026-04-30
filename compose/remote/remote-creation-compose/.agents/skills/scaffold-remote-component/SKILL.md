---
name: Scaffold Remote Component
description: Scaffold a new RemoteCompose remote creation component with test, sample, and preview
---

# Scaffold Remote Component

This skill scaffolds a new RemoteCompose Creation component/modifier, automatically generating the required boilerplate for the component implementation, sample, preview, and screenshot tests.

Since this skill is located within `remote-creation-compose`, it is specifically scoped to generate components only for this module.

### Required Input
Ensure you know the name of the new component (e.g., `RemoteBox`). If the user hasn't specified one, ask for it before proceeding. Let this be `$COMPONENT_NAME`.

**Key Architectural Rules to Remember**:
* **Descriptive Naming**: Names of preview or test variants must describe what they *have* (e.g. `${COMPONENT_NAME}WithColors`) rather than negating what they omit.
* **Separate Composables**: Distinct preview states must be broken out into separated top-level `@Composable` functions.
* **Component Testing**: Tests must directly invoke the explicit `*Preview` composables or state composables defined in the preview file.
* **onClick Parameters**: Use `Action.Empty` to mock any dummy `onClick` actions in samples, previews, and tests.

### Step 1: Create Component Implementation
Create the file relative to the root of the remote-creation-compose project, depending on whether it is a layout (e.g. `RemoteBox`, `RemoteColumn`) or a modifier (e.g. `AlphaModifier`).
`src/main/java/androidx/compose/remote/creation/compose/layout/$COMPONENT_NAME.kt`
OR
`src/main/java/androidx/compose/remote/creation/compose/modifier/$COMPONENT_NAME.kt`

Use this base template:
```kotlin
package androidx.compose.remote.creation.compose.layout // OR .modifier

import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier

@Composable
public fun $COMPONENT_NAME(
    modifier: RemoteModifier = RemoteModifier,
    // Add other relevant parameters here
) {
    // TODO: Implement the component
}
```

Remember to link the Sample composable in the component's KDoc using the `@sample` tag, once the sample is created in Step 2.

### Step 2: Create the Sample
Create the sample file relative to the root of the remote-creation-compose project.
`samples/src/main/java/androidx/compose/remote/creation/compose/samples/${COMPONENT_NAME}Sample.kt`

Use this base template:
```kotlin
package androidx.compose.remote.creation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper
import androidx.compose.ui.tooling.preview.PreviewWrapper

@Sampled
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun ${COMPONENT_NAME}Sample() {
    // TODO: Implement the sample function
}
```

Ensure this sample is linked using `@sample` in the KDocs of the corresponding component/modifier created in Step 1.

### Step 3: Create the Preview
Create the preview file relative to the root of the remote-creation-compose project:
`samples/src/main/java/androidx/compose/remote/creation/compose/previews/${COMPONENT_NAME}Preview.kt`

Use this base template, separating out components into clear descriptive variations describing what they contain:

```kotlin
package androidx.compose.remote.creation.compose.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.remote.creation.compose.previews.utils.RemoteComponentPreviewWrapper

@Preview
@PreviewWrapper(RemoteComponentPreviewWrapper::class)
@Composable
fun ${COMPONENT_NAME}DefaultPreview() {
    // TODO: Apply default preview implementation
}
```

### Step 4: Create the Screenshot Test
Create the test file relative to the root of the remote-creation-compose project:
`src/androidTest/java/androidx/compose/remote/creation/compose/layout/${COMPONENT_NAME}ScreenshotTest.kt`
OR
`src/androidTest/java/androidx/compose/remote/creation/compose/modifier/${COMPONENT_NAME}ScreenshotTest.kt`

Use this boilerplate, ensuring a separate `@Test` method exists for each explicit preview variation you generated:
```kotlin
package androidx.compose.remote.creation.compose.layout // OR .modifier

import androidx.compose.remote.creation.compose.layout.previews.${COMPONENT_NAME}Default
import androidx.compose.remote.creation.compose.modifier.samples.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ${COMPONENT_NAME}ScreenshotTest {
    @get:Rule
    val screenshotRule = RemoteScreenshotTestRule(
        moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
        context = ApplicationProvider.getApplicationContext(),
    )

    @Test
    fun test${COMPONENT_NAME}Default() {
        screenshotRule.runScreenshotTest {
            ${COMPONENT_NAME}Default()
        }
    }
}
```

### Step 5: Create Screenshot Test for Sample
Create the test file relative to the root of the remote-creation-compose project, depending on whether it is a layout or a modifier:
`src/androidTest/java/androidx/compose/remote/creation/compose/layout/samples/${COMPONENT_NAME}SampleScreenshotTest.kt`
OR
`src/androidTest/java/androidx/compose/remote/creation/compose/modifier/samples/${COMPONENT_NAME}SampleScreenshotTest.kt`

Use this boilerplate for the test:
```kotlin
package androidx.compose.remote.creation.compose.layout.samples // OR .modifier.samples

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteAlignment.Companion.Center
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.samples.${COMPONENT_NAME}Sample
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteScreenshotTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ${COMPONENT_NAME}SampleScreenshotTest {
    @get:Rule
    val composeTestRule: RemoteScreenshotTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @Test
    fun ${COMPONENT_NAME}Sample() =
        composeTestRule.runScreenshotTest {
            RemoteBox(modifier = RemoteModifier.fillMaxSize(), contentAlignment = Center) {
                ${COMPONENT_NAME}Sample()
            }
        }
}
```

### Step 6: Wrap up
Notify the user that the component boilerplate has been generated and ask if they'd like to implement specific behavior for the $COMPONENT_NAME component now.

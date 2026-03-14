---
name: Scaffold Remote Component
description: Scaffold a new RemoteCompose wear material3 component with test, sample, and preview
---

# Scaffold Remote Component

This skill scaffolds a new RemoteCompose Wear Material 3 component, automatically generating the required boilerplate for the component implementation, sample, preview, and screenshot tests.

Since this skill is located within `remote-material3`, it is specifically scoped to generate components only for this module.

### Required Input
Ensure you know the name of the new component (e.g., `RemoteSlider`). If the user hasn't specified one, ask for it before proceeding. Let this be `$COMPONENT_NAME`.

**Key Architectural Rules to Remember**:
* **Descriptive Naming**: Names of preview or test variants must describe what they *have* (e.g. `${COMPONENT_NAME}WithTitleSubtitle`) rather than negating what they omit (e.g. `${COMPONENT_NAME}NoTime`).
* **Separate Composables**: Distinct preview states must be broken out into separated top-level `@Composable` functions, rather than cramming multiple variations into a single `RemoteColumn` under one `State()` method.
* **Component Testing**: Tests must directly invoke the explicit `*Preview` / state composables defined in the preview file, rather than generically calling `Sample` files.
* **onClick Parameters**: Use `Action.Empty` to mock any dummy `onClick` actions in samples, previews, and tests.

### Step 1: Create Component Implementation
Create the file at relative to the root of the remote-material3 project:
`src/main/java/androidx/wear/compose/remote/material3/$COMPONENT_NAME.kt`

Use this base template:
```kotlin
package androidx.wear.compose.remote.material3

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

### Step 2: Create the Sample
Create the sample file relative to the root of the remote-material3 project:
`samples/src/main/java/androidx/wear/compose/remote/material3/samples/${COMPONENT_NAME}Sample.kt`

Use this base template:
```kotlin
package androidx.wear.compose.remote.material3.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.wear.compose.remote.material3.$COMPONENT_NAME

@Sampled
@Composable
fun ${COMPONENT_NAME}Sample(modifier: RemoteModifier = RemoteModifier) {
    $COMPONENT_NAME(
        modifier = modifier
    )
}

@WearPreviewDevices
@Composable
fun ${COMPONENT_NAME}SamplePreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) {
    Container { ${COMPONENT_NAME}Sample() }
}
```

### Step 3: Create the Preview
Create the preview file relative to the root of the remote-material3 project:
`samples/src/main/java/androidx/wear/compose/remote/material3/previews/${COMPONENT_NAME}Preview.kt`

Use this base template, separating out components into clear descriptive variations describing what they contain.

```kotlin
package androidx.wear.compose.remote.material3.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.material3.ui.util.WearPreviewDevices
import androidx.wear.compose.remote.material3.${COMPONENT_NAME}
import androidx.compose.remote.creation.compose.preview.RemotePreview
import androidx.compose.remote.creation.compose.preview.Profile
import androidx.compose.remote.creation.compose.preview.ProfilePreviewParameterProvider

@WearPreviewDevices
@Composable
fun ${COMPONENT_NAME}DefaultPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { ${COMPONENT_NAME}Default() } }

@WearPreviewDevices
@Composable
fun ${COMPONENT_NAME}WithVariantPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) = RemotePreview(profile = profile) { Container { ${COMPONENT_NAME}WithVariant() } }

@Composable
@RemoteComposable
fun ${COMPONENT_NAME}Default() {
    ${COMPONENT_NAME}()
}

@Composable
@RemoteComposable
fun ${COMPONENT_NAME}WithVariant() {
    ${COMPONENT_NAME}(/* Configure explicitly */)
}

@Composable
@RemoteComposable
private fun Container(
    modifier: RemoteModifier = RemoteModifier.fillMaxSize().padding(16.rdp),
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
}
```

### Step 4: Create the Screenshot Test
Create the test file relative to the root of the remote-material3 project:
`src/androidTest/java/androidx/wear/compose/remote/material3/${COMPONENT_NAME}Test.kt`

Use this boilerplate, ensuring a separate `@Test` method exists for each explicit preview variation you generated in Step 3:
```kotlin
package androidx.wear.compose.remote.material3

import androidx.wear.compose.remote.material3.previews.${COMPONENT_NAME}Default
import androidx.wear.compose.remote.material3.previews.${COMPONENT_NAME}WithVariant
import androidx.compose.remote.creation.CreationDisplayInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ${COMPONENT_NAME}Test {
    @get:Rule
    val remoteComposeTestRule = RemoteComposeScreenshotTestRule(
        moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
    )

    private val creationDisplayInfo = CreationDisplayInfo(
        500,
        500,
        ApplicationProvider.getApplicationContext().resources.displayMetrics.densityDpi
    )

    @Test
    fun ${COMPONENT_NAME}Default() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            ${COMPONENT_NAME}Default()
        }
    }

    @Test
    fun ${COMPONENT_NAME}WithVariant() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            ${COMPONENT_NAME}WithVariant()
        }
    }
}
```

### Step 5: Create the Screenshot Test for Sample
Create screenshot test for the sample at:
`src/androidTest/java/androidx/wear/compose/remote/material3/samples/`

Use this boilerplate:
```kotlin
package androidx.wear.compose.remote.material3.samples

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.wear.compose.remote.material3.samples.${COMPONENT_NAME}Sample
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class ${COMPONENT_NAME}SampleTest {
    @get:Rule
    val remoteComposeTestRule = RemoteComposeScreenshotTestRule(
        moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
    )

    private val creationDisplayInfo = CreationDisplayInfo(
        500,
        500,
        ApplicationProvider.getApplicationContext().resources.displayMetrics.densityDpi
    )

    @Test
    fun ${COMPONENT_NAME}SampleTest() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            ${COMPONENT_NAME}Sample()
        }
    }
}
```

### Step 6: Wrap up
Notify the user that the component boilerplate has been generated and ask if they'd like to implement specific behavior for the $COMPONENT_NAME component now.

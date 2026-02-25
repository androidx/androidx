# RemoteCompose Wear Material 3 Development Guide

## Project Structure

| Category       | Module                                         | Code Path                    |
|----------------|------------------------------------------------|------------------------------|
| **Components** | `wear:compose:remote:remote-material3`         | `src/main/java`              |
| **Demo**       | `wear:compose:remote:integration-tests:demos`  | `src/main/java`              |
| **Previews**   | `wear:compose:remote:remote-material3-samples` | `src/main/java/.../previews` |
| **Samples**    | `wear:compose:remote:remote-material3-samples` | `src/main/java/.../samples`  |
| **Tests**      | `wear:compose:remote:remote-material3`         | `src/androidTest/java`       |

## Testing Patterns

Tests are located in `src/androidTest/java/` and typically use `RemoteComposeScreenshotTestRule` for
screenshot testing.

### Guidelines

- **Use Test Rule**: ALWAYS use `RemoteComposeScreenshotTestRule` to manage screenshot testing.
- **Define Golden Directory**: Use `SCREENSHOT_GOLDEN_DIRECTORY` (defined in `TestConstants.kt`) to specify where golden images are stored.
- **Configure Display**: Use `CreationDisplayInfo` to define the display metrics for the test (e.g., 500x500 dimension).

### Example Structure

```kotlin
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteIconFromResTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
        )

    private val creationDisplayInfo =
        CreationDisplayInfo(
            500,
            500,
            ApplicationProvider.getApplicationContext().resources.displayMetrics.densityDpi
        )

    @Test
    fun iconsFromRes() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            // Your Remote Composables here
            RemoteRow {
                Icon(resId = R.drawable.test_vector)
            }
        }
    }
}
```

## Sample Patterns

Samples are located in `samples/src/main/java/.../samples/` and are used to demonstrate component
usage.

### Guidelines

- **Location**: ALWAYS locate Samples in **Demos** and **Tests**.
    - This ensures coverage by automated tests and manual checking capability.
- **Usage**: Use Samples in **Previews** whenever possible.
- **Co-location**: ALWAYS place Previews of samples in the same file as the sample.
- **Annotation**: Tag the sample function with `@Sampled`.
- **Preview Integration**: key samples MUST include a corresponding preview function annotated with
  `@WearPreviewDevices`.
- **Naming Convention**: Use `[ComponentName]Sample.kt` for the file and `[ComponentName]SimpleSample`
  (or similar) for the function name.

### Example Structure

```kotlin
@Sampled
@Composable
fun RemoteIconSimpleSample(modifier: RemoteModifier = RemoteModifier) {
    RemoteIcon(
        modifier = modifier.size(24.rdp),
        imageVector = ImageVector.vectorResource(R.drawable.gs_map_wght500rond100_vd_theme_24),
        contentDescription = null,
    )
}

@WearPreviewDevices
@Composable
fun RemoteIconSimpleSamplePreview() = RemotePreview { Container { RemoteIconSimpleSample() } }
```

## Preview Patterns

Previews are located in `samples/src/main/java/.../previews/` and are essential for tooling support.

### Guidelines

- **Visibility**: Ensure ALL Previews are present in **Demos** and **Tests**.
    - Developers must be able to view all variants via previews in the IDE or the demo app.
- **Testing**: Test ALL variants in an automated way.
- **Annotations**:
    - Add `@file:Suppress("RestrictedApiAndroidX")` (often needed for internal APIs).
    - Annotate the preview function with `@WearPreviewDevices`.
- **RemotePreview**: Wrap the composable to be previewed in a `RemotePreview`.
- **Profile Parameters**: Use `@PreviewParameter(ProfilePreviewParameterProvider::class)` to test
  against different profiles if needed.
- **Container**: Use a `Container` helper composable to center content.

### Example Structure

```kotlin
@WearPreviewDevices
@Composable
private fun RemoteIconPreview(
    @PreviewParameter(ProfilePreviewParameterProvider::class) profile: Profile
) =
    RemotePreview(profile = profile) {
        Container {
            RemoteIcon(imageVector = TestImageVectors.VolumeUp, contentDescription = null)
        }
    }
```

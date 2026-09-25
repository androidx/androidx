# Testing Remote Compose

This document describes best practices and guidelines for testing Remote Compose across `compose/remote` and `wear/compose/remote`.

## Test Rules Overview

Remote Compose provides specialized test rules across two main packages. Always prefer these rules over manual harness setup (such as combining `createComposeRule()`, manual `rememberRemoteDocument`, and `AndroidXScreenshotTestRule`).

### 1. Rendering & Screenshot Test Rules (`androidx.compose.remote.player.compose.test.utils`)
Available in `:compose:remote:remote-player-compose-testutils`:

- **`RemoteScreenshotTestRule`**: The primary rule for screenshot and golden verification of `@RemoteComposable` content.
  - Automatically handles document creation, player hosting, test clock synchronization, and golden image comparison via `AndroidXScreenshotTestRule`.
  - Automatically dumps the captured `.rc` binary document and player draw commands as test artifacts.
  - Supports configuration wrappers (`creationComposableWrapper` and `playComposableWrapper`) using `ComposableWrappers` (e.g. `ComposableWrappers.rtl`, `ComposableWrappers.layoutDirection(...)`).
  - Usage:
    ```kotlin
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    @Test
    fun buttonScreenshot() {
        remoteComposeTestRule.runScreenshotTest {
            RemoteButton(onClick = testAction) {
                RemoteText("Click Me".rs)
            }
        }
    }
    ```

- **`RemoteDocScreenshotTestRule`**: Use when you already have a pre-existing `CoreDocument` (e.g., loaded from disk, test assets, or procedurally built) and want to render and verify it against golden screenshots.
  - Usage:
    ```kotlin
    @get:Rule
    val remoteDocTestRule =
        RemoteDocScreenshotTestRule(moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun documentScreenshot() {
        remoteDocTestRule.runScreenshotTest(
            coreDocument = document,
            context = context,
        )
    }
    ```

- **`RemoteInteractionTestRule`**: Use for testing user interactions on the player (such as clicks, gestures, and named actions) and recording dispatched events via `clickEvents`.

- **`RcPlayerTestRule`** (`androidx.compose.remote.player.compose.RcPlayerTestRule` / `androidx.compose.remote.player.compose.embedded.RcPlayerTestRule`): Dedicated rule for testing the embedded player (`RcPlayer`).
  - Automatically enables `RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true` for the duration of the test (replacing manual `EnableEmbeddedPlayerRule` + `createComposeRule()`).
  - Provides `setRemoteContent { ... }`, which captures a `@RemoteComposable` block into a `CoreDocument`, hosts it in `RcPlayer`, awaits initial document creation, and returns the captured `CoreDocument`.
  - Implements `ComposeContentTestRule`, so standard Compose test finders/assertions (`onNodeWithText`, `onRoot().captureToImage()`, `mainClock`, etc.) can be invoked directly on the rule.
  - Usage:
    ```kotlin
    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun playerRendersContent() {
        val document = rule.setRemoteContent {
            RemoteText("Hello Embedded Player".rs)
        }
        rule.onNodeWithText("Hello Embedded Player").assertIsDisplayed()
    }
    ```

---

### 2. Core Testing Rules (`androidx.compose.remote.testing`)
Available in `:compose:remote:remote-testing`:

- **`RemoteCaptureTestRule`**: Use to generate a platform-independent `CoreDocument` from `@RemoteComposable` content without hosting or rendering it in a player.
  - Ideal for testing serialization, binary operations, buffer structures, or display hierarchies.
  - Always declare as a JUnit test rule with `@get:Rule` and call `captureDocument` within `runTest`:
    ```kotlin
    @get:Rule val captureRule = RemoteCaptureTestRule()

    @Test
    fun clickModifierIsAdded(): Unit = runTest {
        val document =
            captureRule.captureDocument(context = context) {
                RemoteButton(onClick = testAction) {
                    RemoteText("Test".rs)
                }
            }
        assertThat(document.displayHierarchy()).contains("CLICK_MODIFIER")
    }
    ```

- **`RemoteContentTestRule`**: Base rule for hosting and rendering Remote Compose content without screenshot verification.
  - Use for UI testing, accessibility (`uiAutomator`), semantics inspection, and state changes.
  - Exposes `composeTestRule: ComposeContentTestRule` and helper `captureRootToImage()`.
  - Usage:
    ```kotlin
    @get:Rule val remoteContentTestRule = RemoteContentTestRule()

    @Test
    fun buttonAccessibility() {
        remoteContentTestRule.setContent(remoteCreationDisplayInfo = displayInfo) {
            RemoteButton(onClick = testAction) { RemoteText("Accessibility".rs) }
        }

        uiAutomator {
            val button = onElement { text == "Accessibility" }
            assertThat(button.isFocusable).isTrue()
        }
    }
    ```

- **`RemoteDocContentTestRule`**: Use for hosting an existing `CoreDocument` in Compose tests without golden screenshot verification.

---

## Best Practices & Guidelines

### 1. Prefer Specialized Test Rules
- **For Embedded Player (`RcPlayer`) Tests**: ALWAYS use `RcPlayerTestRule` and `rule.setRemoteContent { ... }`. Do NOT combine `EnableEmbeddedPlayerRule()` + `createComposeRule()` with manual `captureSingleRemoteDocument` / `RcPlayer(...)` composition inside `rule.setContent { ... }`.
- **For Screenshot Tests**: ALWAYS use `RemoteScreenshotTestRule` (or `RemoteDocScreenshotTestRule`). Avoid manual setups using `createComposeRule()`, manual `rememberRemoteDocument`, and `AndroidXScreenshotTestRule`.
- **For RTL / Layout Direction Tests**: Use `RemoteScreenshotTestRule` with `creationComposableWrapper = ComposableWrappers.rtl` and/or `playComposableWrapper = ComposableWrappers.rtl`. Avoid custom in-test `rememberRemoteDocument` wrappers.
- **For Accessibility and Semantics**: Use `RemoteContentTestRule` and `uiAutomator`.
- **For Document Hierarchy / Buffer Assertions**: Use `RemoteCaptureTestRule` with `@get:Rule` and `runTest`. Do not instantiate `RemoteCaptureTestRule` ad-hoc inside `runBlocking`.
- **For Side-by-Side Comparison Tests (e.g. Compose vs Remote Compose)**: Pre-capture the remote document using `RemoteCaptureTestRule.captureDocument(...)` prior to setting content, rather than calling `rememberRemoteDocument` inside `composeTestRule.setContent`, to prevent race conditions during asynchronous document compilation.

### 2. Avoid Direct Usage of Restricted APIs in Tests
- **Avoid using restricted APIs (`@RestrictTo`)**: Avoid using APIs annotated with `@RestrictTo` (e.g., `RestrictTo.Scope.LIBRARY_GROUP`) in test sources. Since tests are compiled as separate modules, they may reside outside the allowed scope of these restrictions, causing compilation failures on presubmit or buildbot. Always prefer public, unrestricted APIs.
  - *Example*: If you must create display info directly, ensure you use the public `createCreationDisplayInfo(context)` or `RemoteCreationDisplayInfo` constructors.

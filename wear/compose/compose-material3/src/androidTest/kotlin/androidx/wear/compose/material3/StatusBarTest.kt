/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalLayoutApi::class)

package androidx.wear.compose.material3

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.foundation.ScrollInfoProvider
import kotlin.OptIn
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class StatusBarTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun isStatusBarEnabled_doesNotCrash() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val result = isStatusBarEnabled(targetContext)
        // Verify invocation completes safely without throwing uncaught exceptions
        Assert.assertNotNull(result)
    }

    @Test
    fun localStatusBarEnabled_canBeOverridden() {
        var actual: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                actual = LocalStatusBarEnabled.current
            }
        }
        Assert.assertEquals(true, actual)
    }

    @Test
    fun isStatusBarSupported_whenLocalStatusBarEnabled_propagatesToScaffold() {
        var showStatusBar = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    showStatusBar =
                        LocalScaffoldState.current.screenContent.currentShowStatusBar.value
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        Assert.assertTrue(
            "Expected status bar support to propagate when LocalStatusBarEnabled is true",
            showStatusBar,
        )
    }

    @Test
    fun statusBar_resolution_whenSupported_isTrue() {
        var resolvedStatus: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    resolvedStatus =
                        LocalScaffoldState.current.screenContent.currentShowStatusBar.value
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        Assert.assertEquals(true, resolvedStatus)
    }

    @Test
    fun statusBar_resolution_whenNotSupported_resolvesToFalse() {
        var resolvedStatus: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold(isStatusBarEnabled = true) {
                    resolvedStatus =
                        LocalScaffoldState.current.screenContent.currentShowStatusBar.value
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        Assert.assertEquals(false, resolvedStatus)
    }

    @Test
    fun appScaffold_whenNotSupported_displaysLocalTimeText() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold(isStatusBarEnabled = true, timeText = { Text("10:10") }) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("10:10").assertIsDisplayed()
    }

    @Test
    fun screenScaffold_padding_whenNotSupported_ignoresInsetPaddings() {
        var resolvedTopPadding = 0.dp
        var defaultTopPadding = 0.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold(isStatusBarEnabled = true) {
                    defaultTopPadding = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                    ScreenScaffold(statusBarMode = StatusBarMode.Enabled) { contentPadding ->
                        resolvedTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            "When status bar is not supported, top padding should ignore status bar insets and match scaffold default",
            defaultTopPadding,
            resolvedTopPadding,
        )
    }

    @Test
    fun statusBar_override_propagates() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(statusBarMode = StatusBarMode.Disabled) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(false, scaffoldState?.screenContent?.currentShowStatusBar?.value)
    }

    @Test
    fun appScaffold_orchestrator_coordination_scrollsAuto() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets
        val scrollState = mutableStateOf(false)
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float
                    get() = 10f

                override val isScrollAwayValid: Boolean
                    get() = true

                override val isScrollInProgress: Boolean
                    get() = scrollState.value

                override val isScrollable: Boolean
                    get() = true

                override val lastItemOffset: Float
                    get() = 0f
            }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                AppScaffold(isStatusBarEnabled = true) {
                    ScreenScaffold(
                        scrollInfoProvider = mockScrollInfo,
                        statusBarMode = StatusBarMode.Inherit,
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Initial State (Stationary) -> show() should be called
        Assert.assertTrue(
            "Initially stationary: show() should be called",
            testView.testController.showCount > 0,
        )
        Assert.assertEquals(
            "Initially stationary: hide() count should be 0",
            0,
            testView.testController.hideCount,
        )

        // Reset counters for the action phase
        testView.testController.showCount = 0
        testView.testController.hideCount = 0

        // 2. Scroll State to true -> hide() should be called
        composeTestRule.runOnUiThread { scrollState.value = true }
        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Scrolling list: hide() should be called",
            testView.testController.hideCount > 0,
        )
        Assert.assertEquals(
            "Scrolling list: show() count should be 0",
            0,
            testView.testController.showCount,
        )

        // Reset counters
        testView.testController.showCount = 0
        testView.testController.hideCount = 0

        // 3. Scroll stops (returns to idle/stationary)
        composeTestRule.runOnUiThread { scrollState.value = false }
        composeTestRule.waitForIdle()

        // Allow wait for IDLE_DELAY (2000ms)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(3000)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        // Idle stationary -> show() should yield triggers
        Assert.assertTrue(
            "Returning to stationary: show() should be called after delay",
            testView.testController.showCount > 0,
        )
    }

    @Test
    fun appScaffold_orchestrator_showStatusBarFalse_hidesSystemStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                AppScaffold(isStatusBarEnabled = false) { Box(modifier = Modifier.fillMaxSize()) }
            }
        }

        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "isStatusBarEnabled = false: hide() should be called upon initialization",
            testView.testController.hideCount > 0,
        )
        Assert.assertEquals(
            "isStatusBarEnabled = false: show() should never be called",
            0,
            testView.testController.showCount,
        )
    }

    @Test
    fun screenScaffold_padding_isolatedFromOverlayScreenWithDisabledMode() {
        var backgroundScreenTopPadding = 0.dp
        var expectedStatusBarTopPadding = 0.dp
        var showOverlayScreen by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    val density = LocalDensity.current
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    expectedStatusBarTopPadding =
                        insets.asPaddingValues(density).calculateTopPadding()

                    // Background screen with statusBarMode = Enabled and contentPadding = 0.dp
                    // so that top padding is entirely driven by status bar insets
                    ScreenScaffold(
                        statusBarMode = StatusBarMode.Enabled,
                        contentPadding = PaddingValues(0.dp),
                    ) { contentPadding ->
                        backgroundScreenTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // Overlay screen (e.g. Dialog) with statusBarMode = Disabled
                    if (showOverlayScreen) {
                        ScreenScaffold(statusBarMode = StatusBarMode.Disabled) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialBackgroundPadding = backgroundScreenTopPadding
        Assert.assertEquals(
            "Initial background screen padding should match system status bar top inset",
            expectedStatusBarTopPadding,
            initialBackgroundPadding,
        )

        // Open overlay with StatusBarMode.Disabled
        composeTestRule.runOnUiThread { showOverlayScreen = true }
        composeTestRule.waitForIdle()

        // Background screen layout padding MUST remain isolated and unchanged
        Assert.assertEquals(
            "Background screen padding should NOT change when an overlay screen with Disabled mode opens",
            initialBackgroundPadding,
            backgroundScreenTopPadding,
        )
    }

    @Test
    fun screenScaffold_padding_withCustomContentPadding_isolatedFromOverlayScreenWithDisabledMode() {
        var backgroundScreenTopPadding = 0.dp
        var expectedTopPadding = 0.dp
        var showOverlayScreen by mutableStateOf(false)
        val customTopPadding = 16.dp

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    val density = LocalDensity.current
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    val statusBarTopInset = insets.asPaddingValues(density).calculateTopPadding()
                    expectedTopPadding = maxOf(statusBarTopInset, customTopPadding)

                    // Background screen with statusBarMode = Enabled and custom contentPadding
                    ScreenScaffold(
                        statusBarMode = StatusBarMode.Enabled,
                        contentPadding = PaddingValues(top = customTopPadding),
                    ) { contentPadding ->
                        backgroundScreenTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // Overlay screen (e.g. Dialog) with statusBarMode = Disabled
                    if (showOverlayScreen) {
                        ScreenScaffold(statusBarMode = StatusBarMode.Disabled) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialBackgroundPadding = backgroundScreenTopPadding
        Assert.assertEquals(
            "Initial background screen padding should match maxOf(statusBarInset, customTopPadding)",
            expectedTopPadding,
            initialBackgroundPadding,
        )

        // Open overlay with StatusBarMode.Disabled
        composeTestRule.runOnUiThread { showOverlayScreen = true }
        composeTestRule.waitForIdle()

        // Background screen layout padding MUST remain isolated and unchanged
        Assert.assertEquals(
            "Background screen padding should NOT change when an overlay screen with Disabled mode opens",
            initialBackgroundPadding,
            backgroundScreenTopPadding,
        )
    }

    @Test
    fun screenScaffold_disabledMode_paddingIsStableAcrossFrames() {
        val observedPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    ScreenScaffold(
                        statusBarMode = StatusBarMode.Disabled,
                        contentPadding = PaddingValues(0.dp),
                    ) { contentPadding ->
                        observedPaddings.add(contentPadding.calculateTopPadding())
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        Assert.assertTrue("Observed at least one composition pass", observedPaddings.isNotEmpty())
        Assert.assertTrue(
            "Screen with StatusBarMode.Disabled should maintain stable 0.dp status bar padding across all frames without shifting, but observed: $observedPaddings",
            observedPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun screenScaffold_switchingFromEnabledToDisabledScreen_paddingIsStableAcrossFrames() {
        val observedDisabledPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()
        var showDisabledScreen by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    if (!showDisabledScreen) {
                        ScreenScaffold(
                            statusBarMode = StatusBarMode.Enabled,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        ScreenScaffold(
                            statusBarMode = StatusBarMode.Disabled,
                            contentPadding = PaddingValues(0.dp),
                        ) { contentPadding ->
                            observedDisabledPaddings.add(contentPadding.calculateTopPadding())
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // Navigate / switch from Enabled screen to Disabled screen
        composeTestRule.runOnUiThread { showDisabledScreen = true }
        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Observed at least one composition pass for new screen",
            observedDisabledPaddings.isNotEmpty(),
        )
        Assert.assertTrue(
            "Newly navigated screen with StatusBarMode.Disabled should maintain stable 0.dp status bar padding across all frames without shifting, but observed: $observedDisabledPaddings",
            observedDisabledPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun screenScaffold_inheritMode_insideDisabledContainer_paddingIsStableAcrossFrames() {
        val observedInnerPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    // Outer container (e.g. DialogBase) with StatusBarMode.Disabled
                    ScreenScaffold(
                        statusBarMode = StatusBarMode.Disabled,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        // Inner screen with StatusBarMode.Inherit
                        ScreenScaffold(
                            statusBarMode = StatusBarMode.Inherit,
                            contentPadding = PaddingValues(0.dp),
                        ) { contentPadding ->
                            observedInnerPaddings.add(contentPadding.calculateTopPadding())
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Observed at least one composition pass for inner screen",
            observedInnerPaddings.isNotEmpty(),
        )
        Assert.assertTrue(
            "Inner screen with Inherit mode inside Disabled container must maintain stable 0.dp status bar padding across all frames without shifting, but observed: $observedInnerPaddings",
            observedInnerPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun screenScaffold_padding_whenSupportedAndShowStatusBarTrue_appliesInsetPaddings() {
        var resolvedTopPadding = 0.dp
        var expectedStatusBarTopPercent = 0.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    val density = LocalDensity.current
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    expectedStatusBarTopPercent =
                        insets.asPaddingValues(density).calculateTopPadding()

                    ScreenScaffold(statusBarMode = StatusBarMode.Enabled) { contentPadding ->
                        resolvedTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        // The top padding should include the status bar top insets
        Assert.assertTrue(
            "Expected finalTopPadding ($resolvedTopPadding) to be >= system status bar top inset ($expectedStatusBarTopPercent)",
            resolvedTopPadding >= expectedStatusBarTopPercent,
        )
    }

    @Test
    fun screenScaffold_padding_whenSupportedAndShowStatusBarFalse_ignoresInsetPaddings() {
        var resolvedTopPadding = 0.dp
        var defaultTopPadding = 0.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    defaultTopPadding = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                    ScreenScaffold(statusBarMode = StatusBarMode.Disabled) { contentPadding ->
                        resolvedTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        // When showStatusBar is false, status bar insets should be ignored (0.dp), falling back
        // strictly
        // to ScreenScaffoldDefaults
        Assert.assertEquals(
            "Expected top padding to match scaffold defaults contentPadding top",
            defaultTopPadding,
            resolvedTopPadding,
        )
    }

    @Test
    fun statusBarOrchestrator_whenInitiallyUnattached_capturesVisibilityOnAttachAndRestores() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        Assert.assertNull(testView.rootWindowInsets)

        val orchestrator = StatusBarOrchestrator(testView)

        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets

        testView.simulateAttach()

        orchestrator.hide()
        Assert.assertEquals(1, testView.testController.hideCount)

        orchestrator.restoreInitialStatusBarState()
        Assert.assertEquals(1, testView.testController.showCount)
    }

    @Test
    fun statusBarOrchestrator_whenInitiallyUnattached_capturesVisibilityOnShowOrHideAndRestores() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        Assert.assertNull(testView.rootWindowInsets)

        val orchestrator = StatusBarOrchestrator(testView)

        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets

        orchestrator.hide()
        Assert.assertEquals(1, testView.testController.hideCount)

        orchestrator.restoreInitialStatusBarState()
        Assert.assertEquals(1, testView.testController.showCount)
    }

    @Test
    fun statusBarOrchestrator_whenHideCalledFirst_capturesInitialVisibilityAndRestores() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        Assert.assertNull(testView.rootWindowInsets)

        val orchestrator = StatusBarOrchestrator(testView)

        // Mock initial system status bar as visible before hide() is called
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets

        // hide() is called first
        orchestrator.hide()
        Assert.assertEquals("hide() count should be 1", 1, testView.testController.hideCount)
        Assert.assertEquals("show() count should be 0", 0, testView.testController.showCount)

        // restoreInitialStatusBarState() should restore the initially captured visible state
        orchestrator.restoreInitialStatusBarState()
        Assert.assertEquals(
            "restoreInitialStatusBarState() should invoke show() to restore initial state",
            1,
            testView.testController.showCount,
        )
    }

    @Test
    fun statusBar_nestedOverrides_innermostTakesPrecedence() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(statusBarMode = StatusBarMode.Enabled) {
                        ScreenScaffold(statusBarMode = StatusBarMode.Disabled) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            "Innermost ScreenScaffold override (false) should take precedence over parent (true)",
            false,
            scaffoldState?.screenContent?.currentShowStatusBar?.value,
        )
    }

    @Test
    fun statusBarOrchestrator_whenControllerNullInitially_retainsListenerAndAppliesVisibilityWhenControllerBecomesAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        var controllerAvailable = false
        testView.controllerProvider = { if (controllerAvailable) testView.testController else null }

        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets

        val orchestrator = StatusBarOrchestrator(testView)
        orchestrator.hide()

        // Controller is null initially, so hideCount should be 0
        Assert.assertEquals(0, testView.testController.hideCount)

        // Simulate layout change while controller is null
        testView.simulateLayout()
        Assert.assertEquals(0, testView.testController.hideCount)

        // Make controller available and trigger layout pass
        controllerAvailable = true
        testView.simulateLayout()

        // Verify hide() was called once controller became available
        Assert.assertEquals(1, testView.testController.hideCount)
    }

    @Test
    fun statusBarOrchestrator_whenUnattached_cleansUpBothListenersOnReadinessViaLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        Assert.assertNull(testView.rootWindowInsets)

        // Initialize orchestrator while unattached and without insets
        val orchestrator = StatusBarOrchestrator(testView)
        Assert.assertEquals(1, testView.attachListenerCount)
        Assert.assertEquals(1, testView.layoutListenerCount)

        // Attach view when insets are not yet available
        testView.simulateAttach()
        Assert.assertEquals(1, testView.attachListenerCount)
        Assert.assertEquals(1, testView.layoutListenerCount)

        // Insets arrive during layout pass: both listeners must be cleaned up
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets
        testView.simulateLayout()

        Assert.assertEquals(
            "Attach listener should be cleaned up when layout pass achieves readiness",
            0,
            testView.attachListenerCount,
        )
        Assert.assertEquals(
            "Layout listener should be cleaned up when layout pass achieves readiness",
            0,
            testView.layoutListenerCount,
        )
    }

    @Test
    fun statusBarOrchestrator_whenUnattached_cleansUpBothListenersOnReadinessViaAttach() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        Assert.assertNull(testView.rootWindowInsets)

        // Initialize orchestrator while unattached and without insets
        val orchestrator = StatusBarOrchestrator(testView)
        Assert.assertEquals(1, testView.attachListenerCount)
        Assert.assertEquals(1, testView.layoutListenerCount)

        // Insets available upon attachment: both listeners must be cleaned up
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets
        testView.simulateAttach()

        Assert.assertEquals(
            "Attach listener should be cleaned up when attach achieves readiness",
            0,
            testView.attachListenerCount,
        )
        Assert.assertEquals(
            "Layout listener should be cleaned up when attach achieves readiness",
            0,
            testView.layoutListenerCount,
        )
    }

    @Test
    fun statusBarOrchestrator_whenUnattached_cleansUpBothListenersOnRestore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        Assert.assertNull(testView.rootWindowInsets)

        // Initialize orchestrator while unattached and without insets
        val orchestrator = StatusBarOrchestrator(testView)
        Assert.assertEquals(1, testView.attachListenerCount)
        Assert.assertEquals(1, testView.layoutListenerCount)

        // Disposing / restoring before attachment must unregister both listeners
        orchestrator.restoreInitialStatusBarState()

        Assert.assertEquals(
            "Attach listener should be cleaned up on restoreInitialStatusBarState",
            0,
            testView.attachListenerCount,
        )
        Assert.assertEquals(
            "Layout listener should be cleaned up on restoreInitialStatusBarState",
            0,
            testView.layoutListenerCount,
        )
    }

    @Test
    fun screenScaffold_doesNotRecompose_onInitialFrame() {
        var compositionCount = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    ScreenScaffold(statusBarMode = StatusBarMode.Enabled) {
                        compositionCount++
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            "ScreenScaffold should only compose once on initial frame without spurious" +
                " recomposition",
            1,
            compositionCount,
        )
    }

    @Test
    fun screenScaffold_padding_whenParentConsumesInsets_excludesConsumedInsets() {
        var resolvedTopPadding = 0.dp
        var defaultTopPadding = 0.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(isStatusBarEnabled = true) {
                    defaultTopPadding = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    Box(modifier = Modifier.fillMaxSize().consumeWindowInsets(insets)) {
                        ScreenScaffold(statusBarMode = StatusBarMode.Enabled) { contentPadding ->
                            resolvedTopPadding = contentPadding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            "When parent layout consumes status bar insets, ScreenScaffold should exclude" +
                " them and match default contentPadding",
            defaultTopPadding,
            resolvedTopPadding,
        )
    }

    @Test
    fun orchestrator_dispose_cleansUpListenersWithoutMutatingInsets() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(targetContext)

        val orchestrator = StatusBarOrchestrator(testView)
        // Request show to establish listeners and desired state
        orchestrator.show()
        Assert.assertTrue(
            "Listener should be attached if view is not yet attached to window",
            testView.attachListenerCount > 0,
        )

        // Dispose orchestrator
        orchestrator.dispose()
        Assert.assertEquals(
            "Dispose should remove attach listeners",
            0,
            testView.attachListenerCount,
        )
        Assert.assertEquals(
            "Dispose should not invoke show() on controller",
            0,
            testView.testController.showCount,
        )
        Assert.assertEquals(
            "Dispose should not invoke hide() on controller",
            0,
            testView.testController.hideCount,
        )
    }

    @Test
    fun multiWindow_screensInSameWindow_shareOrchestratorAndRefCount() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hostWindow = TestView(targetContext)
        val otherWindow = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        hostWindow.mockRootWindowInsets = mockInsets
        otherWindow.mockRootWindowInsets = mockInsets

        val view1 =
            TestView(targetContext).apply {
                mockRootView = otherWindow
                mockRootWindowInsets = mockInsets
                controllerProvider = { otherWindow.testController }
            }
        val view2 =
            TestView(targetContext).apply {
                mockRootView = otherWindow
                mockRootWindowInsets = mockInsets
                controllerProvider = { otherWindow.testController }
            }

        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )
        screenContent.setAppWindowView(hostWindow)

        val key1 = Any()
        val key2 = Any()

        // Add first screen
        screenContent.addScreen(key1, timeText = null, view = view1)
        val orchestrator1 = screenContent.currentActiveOrchestrator.value

        // Add second screen sharing the same window
        screenContent.addScreen(key2, timeText = null, view = view2)
        val orchestrator2 = screenContent.currentActiveOrchestrator.value

        // Both screens should resolve to the exact same orchestrator instance
        Assert.assertSame(
            "Screens sharing same rootView should share the exact same orchestrator",
            orchestrator1,
            orchestrator2,
        )

        // Mutate status bar (hide)
        orchestrator2.hide()
        Assert.assertEquals(1, otherWindow.testController.hideCount)

        // Remove first screen: window is still in use by screen 2, so restore() should NOT be
        // called yet
        screenContent.removeScreen(key1)
        Assert.assertEquals(
            "Removing first screen should not restore window when second screen is still active",
            0,
            otherWindow.testController.showCount,
        )

        // Remove second screen: window is no longer in use, orchestrator is disposed without insets
        // mutation
        screenContent.removeScreen(key2)
        Assert.assertEquals(
            "Removing last screen in window must dispose orchestrator without mutating window insets",
            0,
            otherWindow.testController.showCount,
        )
    }

    @Test
    fun multiWindow_dialogWindow_drivesDialogOrchestratorAndDisposesWithoutRestoringOnDismiss() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appWindowView = TestView(targetContext)
        val dialogView = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        appWindowView.mockRootWindowInsets = mockInsets
        dialogView.mockRootWindowInsets = mockInsets

        var showDialog by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides appWindowView,
            ) {
                AppScaffold(isStatusBarEnabled = true) {
                    ScreenScaffold(statusBarMode = StatusBarMode.Enabled) {
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    if (showDialog) {
                        // Provide dialog's LocalView inside dialog content
                        CompositionLocalProvider(LocalView provides dialogView) {
                            ScreenScaffold(statusBarMode = StatusBarMode.Enabled) {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        // App window should be active initially
        Assert.assertTrue(
            "Initial app window should receive show()",
            appWindowView.testController.showCount > 0,
        )
        Assert.assertEquals(
            "Dialog window should not be touched yet",
            0,
            dialogView.testController.showCount,
        )

        // Open Dialog
        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()

        // Dialog window should now receive show()
        Assert.assertTrue(
            "Dialog window should receive show() when dialog screen is top-of-stack",
            dialogView.testController.showCount > 0,
        )

        // Reset dialog controller counts before closing
        dialogView.testController.showCount = 0
        dialogView.testController.hideCount = 0

        // Close Dialog
        composeTestRule.runOnUiThread { showDialog = false }
        composeTestRule.waitForIdle()

        // Dismissing dialog screen should dispose the dialog window without mutating dialog insets
        Assert.assertEquals(
            "Dismissing dialog screen should not mutate dialog window insets (lets WindowManager transition naturally)",
            0,
            dialogView.testController.showCount,
        )
        Assert.assertEquals(
            "Dismissing dialog screen should not mutate dialog window insets",
            0,
            dialogView.testController.hideCount,
        )
    }

    @Test
    fun appScaffold_appWindow_heldByAppScaffoldAndRestoredOnUnmount() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appWindowView = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        appWindowView.mockRootWindowInsets = mockInsets

        var mountAppScaffold by mutableStateOf(true)

        composeTestRule.setContent {
            if (mountAppScaffold) {
                CompositionLocalProvider(
                    LocalStatusBarEnabledForTest provides true,
                    LocalView provides appWindowView,
                ) {
                    AppScaffold(isStatusBarEnabled = true) {
                        ScreenScaffold(statusBarMode = StatusBarMode.Enabled) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        Assert.assertTrue(
            "App window should receive show()",
            appWindowView.testController.showCount > 0,
        )
        appWindowView.testController.showCount = 0

        // Unmount AppScaffold -> app window should be restored
        composeTestRule.runOnUiThread { mountAppScaffold = false }
        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Unmounting AppScaffold should restore app window",
            appWindowView.testController.showCount > 0,
        )
    }

    @Test
    fun appScaffold_appWindowView_disposedWhenAppWindowViewChanges() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appWindowViewA = TestView(targetContext)
        val appWindowViewB = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        appWindowViewA.mockRootWindowInsets = mockInsets
        appWindowViewB.mockRootWindowInsets = mockInsets

        var currentAppWindowView by mutableStateOf(appWindowViewA)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides currentAppWindowView,
            ) {
                AppScaffold(isStatusBarEnabled = true) {
                    ScreenScaffold(statusBarMode = StatusBarMode.Enabled) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        Assert.assertTrue(
            "Initial app window should receive show()",
            appWindowViewA.testController.showCount > 0,
        )
        Assert.assertEquals(
            "Second app window should not be touched yet",
            0,
            appWindowViewB.testController.showCount,
        )

        // Reset show count on view A before switching
        appWindowViewA.testController.showCount = 0

        // Switch app window view to appWindowViewB
        composeTestRule.runOnUiThread { currentAppWindowView = appWindowViewB }
        composeTestRule.waitForIdle()

        // appWindowViewA should be disposed without mutating insets
        Assert.assertEquals(
            "Switching app window view should dispose previous app window without insets mutation",
            0,
            appWindowViewA.testController.showCount,
        )
        // appWindowViewB should now be active
        Assert.assertTrue(
            "New app window should receive show()",
            appWindowViewB.testController.showCount > 0,
        )
    }

    @Test
    fun multiWindow_updateIfNeeded_switchesWindowsCleanly() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hostWindow = TestView(targetContext)
        val windowA = TestView(targetContext)
        val windowB = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        hostWindow.mockRootWindowInsets = mockInsets
        windowA.mockRootWindowInsets = mockInsets
        windowB.mockRootWindowInsets = mockInsets

        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )
        screenContent.setAppWindowView(hostWindow)

        val key = Any()
        screenContent.addScreen(key, timeText = null, view = windowA)
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, windowA.testController.hideCount)

        // Update screen to windowB: windowA should be disposed as it's no longer in use
        screenContent.updateIfNeeded(key, timeText = null, view = windowB)
        Assert.assertEquals(
            "Switching window via updateIfNeeded should dispose old window without insets mutation",
            0,
            windowA.testController.showCount,
        )

        // WindowB is now active
        val activeOrchestrator = screenContent.currentActiveOrchestrator.value
        activeOrchestrator.hide()
        Assert.assertEquals(1, windowB.testController.hideCount)

        // Removing screen disposes windowB without insets mutation
        screenContent.removeScreen(key)
        Assert.assertEquals(
            "Removing screen should dispose new window without insets mutation",
            0,
            windowB.testController.showCount,
        )
    }

    @Test
    fun multiWindow_screenSharingHostWindow_doesNotRestoreHostWindowWhenRemoved() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hostWindow = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        hostWindow.mockRootWindowInsets = mockInsets

        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )
        screenContent.setAppWindowView(hostWindow)

        val key = Any()
        screenContent.addScreen(key, timeText = null, view = hostWindow)
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, hostWindow.testController.hideCount)

        // Removing screen that shares hostWindow should NOT restore hostWindow because hostWindow
        // is still in use
        screenContent.removeScreen(key)
        Assert.assertEquals(
            "Host window should not be restored while still used by appWindowView",
            0,
            hostWindow.testController.showCount,
        )
    }

    @Test
    fun multiWindow_multipleScreensSharingSameWindow_disposesOnlyWhenAllScreensRemoved() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val hostWindow = TestView(targetContext)
        val dialogWindow = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        hostWindow.mockRootWindowInsets = mockInsets
        dialogWindow.mockRootWindowInsets = mockInsets

        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )
        screenContent.setAppWindowView(hostWindow)

        val key1 = Any()
        val key2 = Any()
        screenContent.addScreen(key1, timeText = null, view = dialogWindow)
        screenContent.addScreen(key2, timeText = null, view = dialogWindow)

        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, dialogWindow.testController.hideCount)

        // Removing key2 leaves key1 still using dialogWindow -> dialogWindow should NOT be disposed
        // yet
        screenContent.removeScreen(key2)
        Assert.assertEquals(
            "Dialog window should not be disposed while key1 is still using it",
            0,
            dialogWindow.testController.showCount,
        )

        // Removing key1 leaves no screens using dialogWindow -> dialogWindow is disposed without
        // mutating insets
        screenContent.removeScreen(key1)
        Assert.assertEquals(
            "Dialog window should be disposed without mutating insets once all screens using it are removed",
            0,
            dialogWindow.testController.showCount,
        )
    }

    @Test
    fun multiWindow_cleanupAllOrchestrators_restoresAllActiveWindows() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val windowA = TestView(targetContext)
        val windowB = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        windowA.mockRootWindowInsets = mockInsets
        windowB.mockRootWindowInsets = mockInsets

        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )
        screenContent.setAppWindowView(windowA)

        val keyA = Any()
        val keyB = Any()
        screenContent.addScreen(keyA, timeText = null, view = windowA)
        screenContent.currentActiveOrchestrator.value.hide()
        screenContent.addScreen(keyB, timeText = null, view = windowB)
        screenContent.currentActiveOrchestrator.value.hide()

        screenContent.cleanupAllOrchestrators()
        Assert.assertEquals(
            "cleanupAllOrchestrators should restore windowA",
            1,
            windowA.testController.showCount,
        )
        Assert.assertEquals(
            "cleanupAllOrchestrators should restore windowB",
            1,
            windowB.testController.showCount,
        )
    }

    @Test
    fun multiWindow_setAndClearAppWindowView_registersAndCleansUpDirectly() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val windowA = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        windowA.mockRootWindowInsets = mockInsets

        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        // Setting appWindowView makes it active fallback and hides status bar
        screenContent.setAppWindowView(windowA)
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, windowA.testController.hideCount)

        // Clearing appWindowView disposes windowA
        screenContent.clearAppWindowView(windowA)
        Assert.assertEquals(
            "Clearing app window view should dispose orchestrator without insets mutation",
            0,
            windowA.testController.showCount,
        )
    }

    @Test
    fun multiWindow_orchestratorIsForWindow_handlesAttachmentLifecycle() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val decorView = TestView(targetContext)
        val view1 = TestView(targetContext)
        val view2 = TestView(targetContext)

        val orchestrator = StatusBarOrchestrator(view1)

        // When unattached, isForWindow matches view1 by identity
        Assert.assertTrue(orchestrator.isForWindow(view1))
        Assert.assertFalse(orchestrator.isForWindow(view2))

        // When both attach to the same DecorView root window
        view1.mockRootView = decorView
        view2.mockRootView = decorView

        // Now isForWindow matches view2 because both share the same rootView (DecorView)
        Assert.assertTrue(orchestrator.isForWindow(view2))
    }

    @Test
    fun screenContent_resolveShowStatusBarForScreen_isolatesScreensInStack() {
        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val backgroundScreenKey = Any()
        val overlayScreenKey = Any()

        // 1. Add background screen with StatusBarMode.Enabled
        screenContent.addScreen(
            key = backgroundScreenKey,
            timeText = null,
            statusBarMode = StatusBarMode.Enabled,
        )

        // Background screen resolves to true, currentShowStatusBar is true
        Assert.assertTrue(
            screenContent.resolveShowStatusBarForScreen(backgroundScreenKey, StatusBarMode.Enabled)
        )
        Assert.assertTrue(screenContent.currentShowStatusBar.value)

        // 2. Add overlay screen with StatusBarMode.Disabled
        screenContent.addScreen(
            key = overlayScreenKey,
            timeText = null,
            statusBarMode = StatusBarMode.Disabled,
        )

        // Active top screen (overlay) sets currentShowStatusBar to false
        Assert.assertFalse(screenContent.currentShowStatusBar.value)
        // Overlay screen resolves to false
        Assert.assertFalse(
            screenContent.resolveShowStatusBarForScreen(overlayScreenKey, StatusBarMode.Disabled)
        )
        // BUT background screen's resolveShowStatusBarForScreen MUST remain true!
        Assert.assertTrue(
            screenContent.resolveShowStatusBarForScreen(backgroundScreenKey, StatusBarMode.Enabled)
        )
    }

    @Test
    fun screenContent_resolveShowStatusBarForScreen_inheritsFromParent() {
        val screenContent =
            ScreenContent(
                appShowStatusBar = mutableStateOf(true),
                isStatusBarSupported = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val parentKey = Any()
        val childKey = Any()

        // Add parent screen with Disabled mode
        screenContent.addScreen(
            key = parentKey,
            timeText = null,
            statusBarMode = StatusBarMode.Disabled,
        )

        // Add child screen with Inherit mode
        screenContent.addScreen(
            key = childKey,
            timeText = null,
            statusBarMode = StatusBarMode.Inherit,
        )

        // Child inherits Disabled (false) from parent
        Assert.assertFalse(
            screenContent.resolveShowStatusBarForScreen(childKey, StatusBarMode.Inherit)
        )
    }

    private class TestWindowInsetsController : WindowInsetsController {
        var showCount = 0
        var hideCount = 0

        override fun show(types: Int) {
            if (types == WindowInsets.Type.statusBars()) {
                showCount++
            }
        }

        override fun hide(types: Int) {
            if (types == WindowInsets.Type.statusBars()) {
                hideCount++
            }
        }

        override fun getSystemBarsBehavior(): Int = 0

        override fun setSystemBarsBehavior(behavior: Int) {}

        override fun setSystemBarsAppearance(appearance: Int, mask: Int) {}

        override fun getSystemBarsAppearance(): Int = 0

        override fun addOnControllableInsetsChangedListener(
            listener: WindowInsetsController.OnControllableInsetsChangedListener
        ) {}

        override fun removeOnControllableInsetsChangedListener(
            listener: WindowInsetsController.OnControllableInsetsChangedListener
        ) {}

        override fun controlWindowInsetsAnimation(
            types: Int,
            durationMillis: Long,
            interpolator: android.view.animation.Interpolator?,
            cancellationSignal: android.os.CancellationSignal?,
            listener: android.view.WindowInsetsAnimationControlListener,
        ) {}
    }

    private class TestView(context: Context) : View(context) {
        val testController = TestWindowInsetsController()
        var mockRootWindowInsets: WindowInsets? = null
        var mockRootView: View? = null
        var controllerProvider: (() -> WindowInsetsController?)? = null
        private val attachListeners = mutableListOf<OnAttachStateChangeListener>()
        private val layoutListeners = mutableListOf<OnLayoutChangeListener>()

        var mockIsAttachedToWindow: Boolean = false
        val attachListenerCount: Int
            get() = attachListeners.size

        val layoutListenerCount: Int
            get() = layoutListeners.size

        override fun isAttachedToWindow(): Boolean =
            mockIsAttachedToWindow || super.isAttachedToWindow()

        override fun getRootView(): View = mockRootView ?: this

        override fun getWindowInsetsController(): WindowInsetsController? =
            if (controllerProvider != null) controllerProvider?.invoke() else testController

        override fun getRootWindowInsets(): WindowInsets? =
            mockRootWindowInsets ?: super.getRootWindowInsets()

        override fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
            attachListeners.add(listener)
            super.addOnAttachStateChangeListener(listener)
        }

        override fun removeOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
            attachListeners.remove(listener)
            super.removeOnAttachStateChangeListener(listener)
        }

        override fun addOnLayoutChangeListener(listener: OnLayoutChangeListener) {
            layoutListeners.add(listener)
            super.addOnLayoutChangeListener(listener)
        }

        override fun removeOnLayoutChangeListener(listener: OnLayoutChangeListener) {
            layoutListeners.remove(listener)
            super.removeOnLayoutChangeListener(listener)
        }

        fun simulateAttach() {
            for (listener in attachListeners.toList()) {
                listener.onViewAttachedToWindow(this)
            }
        }

        fun simulateLayout() {
            for (listener in layoutListeners.toList()) {
                listener.onLayoutChange(this, 0, 0, 100, 100, 0, 0, 0, 0)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private val LocalStatusBarEnabledForTest: ProvidableCompositionLocal<Boolean>
    get() = LocalStatusBarEnabled as ProvidableCompositionLocal<Boolean>

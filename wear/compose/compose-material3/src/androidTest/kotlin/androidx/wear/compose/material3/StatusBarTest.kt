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
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import java.time.LocalDate
import java.time.LocalTime
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
    fun isStatusBarEnabled_whenTargetSdkBelow35_returnsFalse() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appInfo =
            ApplicationInfo(targetContext.applicationInfo).apply {
                targetSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            }
        val wrapper =
            object : ContextWrapper(targetContext) {
                override fun getApplicationInfo(): ApplicationInfo = appInfo
            }
        Assert.assertFalse(isStatusBarEnabled(wrapper))
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
                AppScaffold {
                    showStatusBar =
                        LocalScaffoldState.current
                            ?.screenContent
                            ?.shouldActiveWindowShowStatusBar
                            ?.value ?: false
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
                AppScaffold {
                    resolvedStatus =
                        LocalScaffoldState.current
                            ?.screenContent
                            ?.shouldActiveWindowShowStatusBar
                            ?.value
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
                AppScaffold {
                    resolvedStatus =
                        LocalScaffoldState.current
                            ?.screenContent
                            ?.shouldActiveWindowShowStatusBar
                            ?.value
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        Assert.assertEquals(false, resolvedStatus)
    }

    @Test
    fun appScaffold_whenNotSupported_displaysLocalTimeText() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold(timeText = { Text("10:10") }) {
                    scaffoldState = LocalScaffoldState.current
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("10:10").assertIsDisplayed()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldAppWindowShowStatusBar.value)
    }

    @Test
    fun appScaffold_dynamicTimeTextMutation_updatesDisplayedTimeText() {
        var timeTextContent by mutableStateOf<@Composable () -> Unit>({ Text("InitialTimeText") })

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold(timeText = timeTextContent) {
                    ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("InitialTimeText").assertIsDisplayed()

        composeTestRule.runOnUiThread { timeTextContent = { Text("UpdatedTimeText") } }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("UpdatedTimeText").assertIsDisplayed()
        composeTestRule.onNodeWithText("InitialTimeText").assertDoesNotExist()
    }

    @Test
    fun appScaffold_dynamicTimeTextMutation_withSuppressionToggling_updatesDisplayedTimeText() {
        var timeTextContent by mutableStateOf<@Composable () -> Unit>({ Text("InitialTimeText") })
        var suppress by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold(
                    timeText = timeTextContent,
                    modifier = if (suppress) Modifier.suppressStatusBar() else Modifier,
                ) {
                    ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("InitialTimeText").assertIsDisplayed()

        // 1. Suppress time bar -> time text silenced
        composeTestRule.runOnUiThread { suppress = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("InitialTimeText").assertDoesNotExist()

        // 2. Mutate timeText while suppressed -> remains silenced
        composeTestRule.runOnUiThread { timeTextContent = { Text("UpdatedTimeText") } }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("InitialTimeText").assertDoesNotExist()
        composeTestRule.onNodeWithText("UpdatedTimeText").assertDoesNotExist()

        // 3. Unsuppress -> latest updated time text is displayed, not stale initial
        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("UpdatedTimeText").assertIsDisplayed()
        composeTestRule.onNodeWithText("InitialTimeText").assertDoesNotExist()
    }

    @Test
    fun screenScaffold_padding_whenNotSupported_ignoresInsetPaddings() {
        var resolvedTopPadding = 0.dp
        var defaultTopPadding = 0.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                AppScaffold {
                    defaultTopPadding = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                    ScreenScaffold { contentPadding ->
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
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(timeText = { TimeText() }) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            false,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
    }

    @Test
    fun screenScaffold_customTimeText_disablesStatusBar() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(timeText = { TimeText() }) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            false,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
    }

    @Test
    fun screenScaffold_emptyTimeText_disablesStatusBar() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(timeText = {}) { Box(modifier = Modifier.fillMaxSize()) }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            false,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
    }

    @Test
    fun screenScaffold_defaultTimeText_enablesStatusBar() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            true,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
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
                AppScaffold {
                    ScreenScaffold(scrollInfoProvider = mockScrollInfo) {
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
    fun appScaffold_orchestrator_whenScrollingAndOffsetIsNaN_hidesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets
        val scrollState = mutableStateOf(false)
        val offsetState = mutableStateOf(Float.NaN)
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float
                    get() = offsetState.value

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
                AppScaffold {
                    ScreenScaffold(scrollInfoProvider = mockScrollInfo) {
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

        // Reset counters for the action phase
        testView.testController.showCount = 0
        testView.testController.hideCount = 0

        // 2. Start scrolling when anchorItemOffset is NaN -> hide() must be called
        composeTestRule.runOnUiThread {
            scrollState.value = true
            offsetState.value = Float.NaN
        }
        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Scrolling with NaN offset: hide() must be called",
            testView.testController.hideCount > 0,
        )
        Assert.assertEquals(
            "Scrolling with NaN offset: show() count must be 0",
            0,
            testView.testController.showCount,
        )

        // 3. While scrolling, scroll back to top (offset <= 0f) -> show() should be called
        testView.testController.showCount = 0
        testView.testController.hideCount = 0
        composeTestRule.runOnUiThread { offsetState.value = 0f }
        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Scrolling at top (offset <= 0): show() must be called",
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
                AppScaffold(timeText = {}) { Box(modifier = Modifier.fillMaxSize()) }
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
                AppScaffold {
                    val density = LocalDensity.current
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    expectedStatusBarTopPadding =
                        insets.asPaddingValues(density).calculateTopPadding()

                    // Background screen with statusBarMode = Enabled and contentPadding = 0.dp
                    // so that top padding is entirely driven by status bar insets
                    ScreenScaffold(contentPadding = PaddingValues(0.dp)) { contentPadding ->
                        backgroundScreenTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // Overlay screen (e.g. Dialog) with statusBarMode = Disabled
                    if (showOverlayScreen) {
                        ScreenScaffold(timeText = { TimeText() }) {
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
                AppScaffold {
                    val density = LocalDensity.current
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    val statusBarTopInset = insets.asPaddingValues(density).calculateTopPadding()
                    expectedTopPadding = maxOf(statusBarTopInset, customTopPadding)

                    // Background screen with statusBarMode = Enabled and custom contentPadding
                    ScreenScaffold(contentPadding = PaddingValues(top = customTopPadding)) {
                        contentPadding ->
                        backgroundScreenTopPadding = contentPadding.calculateTopPadding()
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // Overlay screen (e.g. Dialog) with statusBarMode = Disabled
                    if (showOverlayScreen) {
                        ScreenScaffold(timeText = { TimeText() }) {
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
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        ScreenScaffold(
                            timeText = { TimeText() },
                            contentPadding = PaddingValues(0.dp),
                        ) { contentPadding ->
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().layout { measurable, constraints ->
                                        observedPaddings.add(contentPadding.calculateTopPadding())
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        Assert.assertTrue("Observed at least one measure pass", observedPaddings.isNotEmpty())
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
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        if (!showDisabledScreen) {
                            ScreenScaffold(contentPadding = PaddingValues(0.dp)) {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        } else {
                            ScreenScaffold(
                                timeText = { TimeText() },
                                contentPadding = PaddingValues(0.dp),
                            ) { contentPadding ->
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize().layout { measurable, constraints ->
                                            observedDisabledPaddings.add(
                                                contentPadding.calculateTopPadding()
                                            )
                                            val placeable = measurable.measure(constraints)
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                )
                            }
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
            "Observed at least one measure pass for new screen",
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
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        // Outer container (e.g. DialogBase) with StatusBarMode.Disabled
                        ScreenScaffold(
                            timeText = { TimeText() },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            // Inner screen with StatusBarMode.Inherit
                            ScreenScaffold(contentPadding = PaddingValues(0.dp)) { contentPadding ->
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize().layout { measurable, constraints ->
                                            observedInnerPaddings.add(
                                                contentPadding.calculateTopPadding()
                                            )
                                            val placeable = measurable.measure(constraints)
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        Assert.assertTrue(
            "Observed at least one measure pass for inner screen",
            observedInnerPaddings.isNotEmpty(),
        )
        Assert.assertTrue(
            "Inner screen with Inherit mode inside Disabled container must maintain stable 0.dp status bar padding across all frames without shifting, but observed: $observedInnerPaddings",
            observedInnerPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun dialog_whenOpen_suppressesStatusBar() {
        var scaffoldState: ScaffoldState? = null
        var showDialog by mutableStateOf(false)
        var backgroundTopPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            backgroundTopPadding = padding.calculateTopPadding()
                            Dialog(
                                visible = showDialog,
                                onDismissRequest = { showDialog = false },
                            ) {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val initialPadding = checkNotNull(backgroundTopPadding)
        Assert.assertTrue(initialPadding > 0.dp)
        // When dialog is closed, background screen is active with Enabled ->
        // shouldActiveWindowShowStatusBar
        // is true
        Assert.assertEquals(
            true,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )

        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()

        // When dialog is open and top-most, it suppresses status bar ->
        // shouldActiveWindowShowStatusBar
        // is false
        Assert.assertEquals(
            "Dialog must suppress status bar when it is the top-most item",
            false,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
        // Background screen content padding remains isolated and unchanged
        Assert.assertEquals(initialPadding, backgroundTopPadding)

        composeTestRule.runOnUiThread { showDialog = false }
        composeTestRule.waitForIdle()

        // When dialog is dismissed, it restores the background screen state ->
        // shouldActiveWindowShowStatusBar
        // is true
        Assert.assertEquals(
            "Dismissing dialog must restore background screen status bar state",
            true,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
        Assert.assertEquals(initialPadding, backgroundTopPadding)
    }

    @Test
    fun dialog_withInnerScreenScaffoldInherit_inheritsDisabledFromDialog() {
        var scaffoldState: ScaffoldState? = null
        var showDialog by mutableStateOf(true)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold {
                        Dialog(visible = showDialog, onDismissRequest = { showDialog = false }) {
                            ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        // Inner screen with Inherit must inherit Disabled from enclosing Dialog
        Assert.assertEquals(
            "Inner ScreenScaffold with Inherit must inherit Disabled from Dialog",
            false,
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
        )
    }

    @Test
    fun dialog_whenOpen_doesNotTriggerAppScaffoldBackgroundTimeText() {
        var showDialog by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold {
                        Dialog(visible = showDialog, onDismissRequest = { showDialog = false }) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)

        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()

        // When dialog opens, AppScaffold in the background must NOT suddenly start drawing local
        // TimeText
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)
    }

    @Test
    fun screenScaffold_padding_whenSupportedAndShowStatusBarTrue_appliesInsetPaddings() {
        var resolvedTopPadding = 0.dp
        var expectedStatusBarTopPercent = 0.dp
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    val density = LocalDensity.current
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    expectedStatusBarTopPercent =
                        insets.asPaddingValues(density).calculateTopPadding()

                    ScreenScaffold { contentPadding ->
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
                AppScaffold {
                    defaultTopPadding = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                    ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
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
    fun statusBarOrchestrator_restoreInitialStatusBarState_clearsDesiredVisibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets
        testView.simulateAttach()

        val orchestrator = StatusBarOrchestrator(testView)
        orchestrator.hide()
        Assert.assertEquals(1, testView.testController.hideCount)

        orchestrator.restoreInitialStatusBarState()
        Assert.assertEquals(1, testView.testController.showCount)

        // Simulate subsequent layout pass or view event; orchestrator should not re-hide
        testView.simulateLayout()
        Assert.assertEquals(1, testView.testController.hideCount)
    }

    @Test
    fun statusBar_nestedOverrides_innermostTakesPrecedence() {
        var scaffoldState: ScaffoldState? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold {
                        ScreenScaffold(timeText = { TimeText() }) {
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
            scaffoldState?.screenContent?.shouldActiveWindowShowStatusBar?.value,
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
        StatusBarOrchestrator(testView)
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
        StatusBarOrchestrator(testView)
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
                AppScaffold {
                    ScreenScaffold {
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
                AppScaffold {
                    defaultTopPadding = ScreenScaffoldDefaults.contentPadding.calculateTopPadding()
                    val insets =
                        androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility
                    Box(modifier = Modifier.fillMaxSize().consumeWindowInsets(insets)) {
                        ScreenScaffold { contentPadding ->
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
                appWindowView = mutableStateOf(hostWindow),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val key1 = Any()
        val key2 = Any()

        // Add first screen
        screenContent.addStatusBar(
            key = key1,
            view = mutableStateOf(view1),
            showStatusBar = mutableStateOf(true),
        )
        val orchestrator1 = screenContent.currentActiveOrchestrator.value

        // Add second screen sharing the same window
        screenContent.addStatusBar(
            key = key2,
            view = mutableStateOf(view2),
            showStatusBar = mutableStateOf(true),
        )
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
        screenContent.removeStatusBar(key1)
        Assert.assertEquals(
            "Removing first screen should not restore window when second screen is still active",
            0,
            otherWindow.testController.showCount,
        )

        // Remove second screen: window is no longer in use, orchestrator is disposed without insets
        // mutation
        screenContent.removeStatusBar(key2)
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
                AppScaffold {
                    ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }

                    if (showDialog) {
                        // Provide dialog's LocalView inside dialog content
                        CompositionLocalProvider(LocalView provides dialogView) {
                            ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }
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
                    AppScaffold { ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) } }
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
                AppScaffold { ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) } }
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
    fun multiWindow_screenViewChanges_switchesWindowsCleanly() {
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
                appWindowView = mutableStateOf(hostWindow),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val key = Any()
        var currentScreenView by mutableStateOf<View>(windowA)
        screenContent.addStatusBar(
            key = key,
            view = derivedStateOf { currentScreenView },
            showStatusBar = mutableStateOf(true),
        )
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, windowA.testController.hideCount)

        // Update screen view to windowB: windowA should be disposed as it's no longer in use
        currentScreenView = windowB
        // WindowB is now active
        val activeOrchestrator = screenContent.currentActiveOrchestrator.value
        activeOrchestrator.hide()
        Assert.assertEquals(1, windowB.testController.hideCount)
        Assert.assertEquals(
            "Switching window via screen view state change should dispose old window without insets mutation",
            0,
            windowA.testController.showCount,
        )

        // Removing screen disposes windowB without insets mutation
        screenContent.removeStatusBar(key)
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
                appWindowView = mutableStateOf(hostWindow),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val key = Any()
        screenContent.addStatusBar(
            key = key,
            view = mutableStateOf(hostWindow),
            showStatusBar = mutableStateOf(true),
        )
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, hostWindow.testController.hideCount)

        // Removing screen that shares hostWindow should NOT restore hostWindow because hostWindow
        // is still in use
        screenContent.removeStatusBar(key)
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
                appWindowView = mutableStateOf(hostWindow),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val key1 = Any()
        val key2 = Any()
        screenContent.addStatusBar(
            key = key1,
            view = mutableStateOf(dialogWindow),
            showStatusBar = mutableStateOf(true),
        )
        screenContent.addStatusBar(
            key = key2,
            view = mutableStateOf(dialogWindow),
            showStatusBar = mutableStateOf(true),
        )

        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, dialogWindow.testController.hideCount)

        // Removing key2 leaves key1 still using dialogWindow -> dialogWindow should NOT be disposed
        // yet
        screenContent.removeStatusBar(key2)
        Assert.assertEquals(
            "Dialog window should not be disposed while key1 is still using it",
            0,
            dialogWindow.testController.showCount,
        )

        // Removing key1 leaves no screens using dialogWindow -> dialogWindow is disposed without
        // mutating insets
        screenContent.removeStatusBar(key1)
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
                appWindowView = mutableStateOf(windowA),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val keyA = Any()
        val keyB = Any()
        screenContent.addStatusBar(
            key = keyA,
            view = mutableStateOf(windowA),
            showStatusBar = mutableStateOf(true),
        )
        screenContent.currentActiveOrchestrator.value.hide()
        screenContent.addStatusBar(
            key = keyB,
            view = mutableStateOf(windowB),
            showStatusBar = mutableStateOf(true),
        )
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
    fun multiWindow_appWindowView_registersAndCleansUpDirectly() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val windowA = TestView(targetContext)
        val windowB = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        windowA.mockRootWindowInsets = mockInsets
        windowB.mockRootWindowInsets = mockInsets

        var currentAppWindowView by mutableStateOf(windowA)
        val screenContent =
            ScreenContent(
                appWindowView = derivedStateOf { currentAppWindowView },
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        // Setting appWindowView makes it active fallback and hides status bar
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, windowA.testController.hideCount)

        // Switching appWindowView disposes windowA without insets mutation
        currentAppWindowView = windowB
        screenContent.currentActiveOrchestrator.value.hide()
        Assert.assertEquals(1, windowB.testController.hideCount)
        Assert.assertEquals(
            "Switching app window view should dispose previous orchestrator without insets mutation",
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
    fun view_isSameWindow_matchesByIdentityAndRootView() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val decorView1 = TestView(targetContext)
        val decorView2 = TestView(targetContext)
        val view1 = TestView(targetContext)
        val view2 = TestView(targetContext)

        // Same instance matches
        Assert.assertTrue(view1.isSameWindow(view1))

        // Different unattached instances do not match
        Assert.assertFalse(view1.isSameWindow(view2))

        // Same root view matches
        view1.mockRootView = decorView1
        view2.mockRootView = decorView1
        Assert.assertTrue(view1.isSameWindow(view2))

        // Different root views do not match
        view2.mockRootView = decorView2
        Assert.assertFalse(view1.isSameWindow(view2))
    }

    @Test
    fun screenContent_shouldAppWindowShowStatusBar_isolatesLayeredViewsFromAppWindow() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appDecor = TestView(targetContext)
        val dialogDecor = TestView(targetContext)
        val appView = TestView(targetContext).apply { mockRootView = appDecor }
        val dialogView = TestView(targetContext).apply { mockRootView = dialogDecor }

        val screenContent =
            ScreenContent(
                appWindowView = mutableStateOf(appView),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val appScreenKey = Any()
        val dialogScreenKey = Any()

        // 1. App screen with showStatusBar = true
        screenContent.addStatusBar(
            key = appScreenKey,
            view = mutableStateOf(appView),
            showStatusBar = mutableStateOf(true),
        )
        Assert.assertTrue(screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(screenContent.shouldAppWindowShowStatusBar.value)

        // 2. Dialog screen added on top with showStatusBar = false
        screenContent.addStatusBar(
            key = dialogScreenKey,
            view = mutableStateOf(dialogView),
            showStatusBar = mutableStateOf(false),
        )

        // Top-of-stack (dialog) drives shouldActiveWindowShowStatusBar to false (for System status
        // bar
        // hide)
        Assert.assertFalse(screenContent.shouldActiveWindowShowStatusBar.value)
        // BUT App window status bar state remains true (TimeText won't show in app window)
        Assert.assertTrue(screenContent.shouldAppWindowShowStatusBar.value)
    }

    @Test
    fun multiWindow_layeredViewWithDisabledStatusBar_doesNotShowLocalTimeTextInAppScaffold() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val appWindowView = TestView(targetContext)
        val dialogView = TestView(targetContext)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        appWindowView.mockRootWindowInsets = mockInsets
        dialogView.mockRootWindowInsets = mockInsets

        var showDialog by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides appWindowView,
            ) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }

                    if (showDialog) {
                        CompositionLocalProvider(LocalView provides dialogView) {
                            ScreenScaffold(timeText = {}) { Box(modifier = Modifier.fillMaxSize()) }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        // App window has status bar enabled -> shouldAppWindowShowStatusBar is true
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)

        // Open Dialog with empty timeText (Disabled)
        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()

        // Dialog orchestrator should have received hide()
        Assert.assertTrue(
            "Dialog window should receive hide()",
            dialogView.testController.hideCount > 0,
        )

        // But local AppTimeText in AppScaffold must STILL NOT be displayed!
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)
    }

    @Test
    fun multiWindow_realDialogWithDisabledStatusBar_doesNotShowLocalTimeTextInAppScaffold() {
        var showDialog by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold { Box(modifier = Modifier.fillMaxSize()) }

                    Dialog(visible = showDialog, onDismissRequest = { showDialog = false }) {
                        ScreenScaffold(timeText = {}) {
                            Box(modifier = Modifier.fillMaxSize()) { Text("DialogContent") }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        // App window has status bar enabled
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)
        composeTestRule.onNodeWithText("DialogContent").assertDoesNotExist()

        // Open Real Dialog with empty timeText (Disabled)
        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()

        // Dialog content is displayed
        composeTestRule.onNodeWithText("DialogContent").assertIsDisplayed()

        // Local AppTimeText in AppScaffold must STILL NOT be displayed because AppScaffold's
        // window status bar remains enabled for the app window
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)

        // Dismiss Dialog
        composeTestRule.runOnUiThread { showDialog = false }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("DialogContent").assertDoesNotExist()
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)
    }

    @Test
    fun screenContent_shouldActiveWindowShowStatusBar_evaluatesStack() {
        val testView = mutableStateOf(View(ApplicationProvider.getApplicationContext()))
        val screenContent =
            ScreenContent(
                appWindowView = testView,
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val backgroundScreenKey = Any()
        val overlayScreenKey = Any()

        // 1. Add background screen with showStatusBar = true
        screenContent.addStatusBar(
            key = backgroundScreenKey,
            view = testView,
            showStatusBar = mutableStateOf(true),
        )

        // Background screen makes shouldActiveWindowShowStatusBar true
        Assert.assertTrue(screenContent.shouldActiveWindowShowStatusBar.value)

        // 2. Add overlay screen with showStatusBar = false
        screenContent.addStatusBar(
            key = overlayScreenKey,
            view = testView,
            showStatusBar = mutableStateOf(false),
        )

        // Active top screen (overlay) sets shouldActiveWindowShowStatusBar to false
        Assert.assertFalse(screenContent.shouldActiveWindowShowStatusBar.value)

        // 3. Remove overlay screen -> shouldActiveWindowShowStatusBar reverts to true
        screenContent.removeStatusBar(overlayScreenKey)
        Assert.assertTrue(screenContent.shouldActiveWindowShowStatusBar.value)
    }

    @Test
    fun screenContent_shouldActiveWindowShowStatusBar_fallsBackToDoesAppScaffoldWantStatusBar() {
        val testView = mutableStateOf(View(ApplicationProvider.getApplicationContext()))
        val screenContent =
            ScreenContent(
                appWindowView = testView,
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        // With no screens, falls back to doesAppScaffoldWantStatusBar (true)
        Assert.assertTrue(screenContent.shouldActiveWindowShowStatusBar.value)

        val screenKey = Any()
        screenContent.addStatusBar(
            key = screenKey,
            view = testView,
            showStatusBar = mutableStateOf(false),
        )
        // With screen added, reflects screen showStatusBar (false)
        Assert.assertFalse(screenContent.shouldActiveWindowShowStatusBar.value)

        // After removing screen, reverts to doesAppScaffoldWantStatusBar (true)
        screenContent.removeStatusBar(screenKey)
        Assert.assertTrue(screenContent.shouldActiveWindowShowStatusBar.value)
    }

    @Test
    fun screenContent_currentTimeText_whenStatusBarConfigured_doesNotFallbackToAppTimeTextOnSuppression() {
        val testView = mutableStateOf(View(ApplicationProvider.getApplicationContext()))
        val appTimeTextComposable: @Composable () -> Unit = { Text("AppDefaultTimeText") }
        val screenContent =
            ScreenContent(
                appWindowView = testView,
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf(appTimeTextComposable),
            )

        // Add a screen with showStatusBar = false (simulating Stepper suppression on the window)
        val screenKey = Any()
        screenContent.addStatusBar(
            key = screenKey,
            view = testView,
            showStatusBar = mutableStateOf(false),
        )

        Assert.assertFalse(screenContent.shouldAppWindowShowStatusBar.value)
        // With status bar configured (doesAppScaffoldWantStatusBar = true), suppression on the
        // window must
        // NOT cause currentTimeText to fall back to appTimeText
        Assert.assertNotSame(
            "currentTimeText should not fall back to appTimeText when status bar is suppressed",
            appTimeTextComposable,
            screenContent.currentTimeText.value,
        )
    }

    @Test
    fun screenContent_addStatusBar_doesNotAffectScreenContent() {
        val testView = mutableStateOf(View(ApplicationProvider.getApplicationContext()))
        val screenContent =
            ScreenContent(
                appWindowView = testView,
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val screenKey = Any()
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        screenContent.addScreenContent(
            key = screenKey,
            timeText = mutableStateOf(null),
            scrollInfoProvider = mutableStateOf(mockScrollInfo),
        )

        Assert.assertSame(mockScrollInfo, screenContent.currentScrollInfoProvider.value)

        // Adding a status bar layer (e.g. from Modifier.suppressStatusBar) should NOT affect
        // screen content or scroll info provider.
        val statusBarKey = Any()
        screenContent.addStatusBar(
            key = statusBarKey,
            view = testView,
            showStatusBar = mutableStateOf(false),
        )

        Assert.assertFalse(screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, screenContent.currentScrollInfoProvider.value)

        // Removing status bar restores status bar visibility while screen content remains intact.
        screenContent.removeStatusBar(statusBarKey)
        Assert.assertTrue(screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun screenContent_addScreenContent_doesNotAffectStatusBar() {
        val testView = mutableStateOf(View(ApplicationProvider.getApplicationContext()))
        val screenContent =
            ScreenContent(
                appWindowView = testView,
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val statusBarKey = Any()
        screenContent.addStatusBar(
            key = statusBarKey,
            view = testView,
            showStatusBar = mutableStateOf(false),
        )

        Assert.assertFalse(screenContent.shouldActiveWindowShowStatusBar.value)

        // Adding screen content (e.g. PagerScaffold settled page) should NOT affect
        // status bar visibility.
        val screenKey = Any()
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }
        screenContent.addScreenContent(
            key = screenKey,
            timeText = mutableStateOf(null),
            scrollInfoProvider = mutableStateOf(mockScrollInfo),
        )

        Assert.assertFalse(screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun screenContent_removeStatusBar_cleansUpOrchestratorWhileScreenContentRemains() {
        val hostWindow = View(ApplicationProvider.getApplicationContext())
        val secondaryWindow = View(ApplicationProvider.getApplicationContext())
        val screenContent =
            ScreenContent(
                appWindowView = mutableStateOf(hostWindow),
                doesAppScaffoldWantStatusBar = mutableStateOf(true),
                appTimeText = mutableStateOf({}),
            )

        val key = Any()
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        // Add screen content and status bar for the secondary window
        screenContent.addScreenContent(
            key = key,
            timeText = mutableStateOf(null),
            scrollInfoProvider = mutableStateOf(mockScrollInfo),
        )
        screenContent.addStatusBar(
            key = key,
            view = mutableStateOf(secondaryWindow),
            showStatusBar = mutableStateOf(false),
        )

        val orchestrator = screenContent.currentActiveOrchestrator.value
        Assert.assertTrue(orchestrator.isForWindow(secondaryWindow))

        // Removing only status bar should clean up the secondary window's orchestrator,
        // even though screen content remains on the stack.
        screenContent.removeStatusBar(key)
        Assert.assertTrue(screenContent.currentActiveOrchestrator.value.isForWindow(hostWindow))
        Assert.assertSame(mockScrollInfo, screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun stepper_whenDisplayedInScaffold_suppressesStatusBar() {
        var showStepper by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            if (showStepper) {
                                Stepper(
                                    value = 2f,
                                    onValueChange = {},
                                    steps = 3,
                                    increaseIcon = { Text("+") },
                                    decreaseIcon = { Text("-") },
                                ) {
                                    Text("StepperContent")
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)

        composeTestRule.runOnUiThread { showStepper = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)
        Assert.assertNotSame(
            "currentTimeText should not fall back to appTimeText when Stepper suppresses status bar",
            AppScaffoldDefaults.timeText,
            state.screenContent.currentTimeText.value,
        )

        composeTestRule.runOnUiThread { showStepper = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)
    }

    @Test
    fun stepper_whenDisplayedDirectlyInAppScaffold_suppressesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    Stepper(
                        value = 2f,
                        onValueChange = {},
                        steps = 3,
                        increaseIcon = { Text("+") },
                        decreaseIcon = { Text("-") },
                    ) {
                        Text("StepperDirectInAppScaffold")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
    }

    @Test
    fun stepper_whenInitiallyPresentInScaffold_suppressesStatusBar() {
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            Stepper(
                                value = 2f,
                                onValueChange = {},
                                steps = 3,
                                increaseIcon = { Text("+") },
                                decreaseIcon = { Text("-") },
                            ) {
                                Text("StepperContent")
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)
    }

    @Test
    fun boxSuppressStatusBar_whenInitiallyPresentInScaffold_suppressesStatusBarAndCollapsesPadding() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(
                    LocalStatusBarEnabledForTest provides true,
                    LocalView provides testView,
                ) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize().suppressStatusBar())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)
    }

    @Test
    fun stepper_whenNavigatedTo_maintainsSuppressionAfterScreenBecomesActive() {
        var screenIsActive by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        CompositionLocalProvider(LocalScreenIsActive provides screenIsActive) {
                            ScreenScaffold(
                                contentPadding =
                                    androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { padding ->
                                topPadding = padding.calculateTopPadding()
                                Stepper(
                                    value = 2f,
                                    onValueChange = {},
                                    steps = 3,
                                    increaseIcon = { Text("+") },
                                    decreaseIcon = { Text("-") },
                                ) {
                                    Text("StepperContent")
                                }
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)

        // Navigation transition completes: screenIsActive becomes true
        composeTestRule.runOnUiThread { screenIsActive = true }
        composeTestRule.waitForIdle()

        // Stepper is inside ScreenScaffold; status bar should remain suppressed
        Assert.assertFalse(
            "Stepper inside ScreenScaffold should suppress status bar after screen activates",
            state.screenContent.shouldActiveWindowShowStatusBar.value,
        )
        Assert.assertEquals(0.dp, topPadding)
    }

    @Test
    fun stepper_whenDisplayedOutOfScaffold_hardHidesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var showStepper by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                if (showStepper) {
                    Stepper(
                        value = 2f,
                        onValueChange = {},
                        steps = 3,
                        increaseIcon = { Text("+") },
                        decreaseIcon = { Text("-") },
                    ) {
                        Text("StepperContent")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)

        composeTestRule.runOnUiThread { showStepper = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        composeTestRule.runOnUiThread { showStepper = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun dialog_whenDisplayedOutOfScaffold_doesNotCrashAndHardHidesStatusBar() {
        var showDialog by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                Dialog(visible = showDialog, onDismissRequest = { showDialog = false }) {
                    Text("DialogOutOfScaffold")
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DialogOutOfScaffold").assertIsDisplayed()

        composeTestRule.runOnUiThread { showDialog = false }
        composeTestRule.waitForIdle()
    }

    @Test
    fun dialog_whenDisplayedInScaffold_suppressesStatusBarAndPreservesUnderlyingScreen() {
        var showDialog by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var underlyingPadding: androidx.compose.ui.unit.Dp? = null
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            scrollInfoProvider = mockScrollInfo,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { padding ->
                            underlyingPadding = padding.calculateTopPadding()
                            Dialog(
                                visible = showDialog,
                                onDismissRequest = { showDialog = false },
                            ) {
                                Text("DialogContent")
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
        val initialUnderlyingPadding = checkNotNull(underlyingPadding)
        Assert.assertTrue(initialUnderlyingPadding > 0.dp)

        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DialogContent").assertIsDisplayed()

        // Status bar is suppressed by dialog, but underlying screen's scrollInfoProvider and
        // contentPadding are preserved and isolated
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
        Assert.assertEquals(initialUnderlyingPadding, underlyingPadding)

        composeTestRule.runOnUiThread { showDialog = false }
        composeTestRule.waitForIdle()

        // Status bar restored, scrollInfoProvider and padding still preserved
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
        Assert.assertEquals(initialUnderlyingPadding, underlyingPadding)
    }

    @Test
    fun multipleSuppressionScreens_whenOverlapping_maintainsSuppressionUntilAllDismissed() {
        var showDialog by mutableStateOf(false)
        var showStepper by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var screenPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            screenPadding = padding.calculateTopPadding()
                            Dialog(
                                visible = showDialog,
                                onDismissRequest = { showDialog = false },
                            ) {
                                Text("DialogContent")
                            }
                            if (showStepper) {
                                Stepper(
                                    value = 1f,
                                    onValueChange = {},
                                    steps = 2,
                                    increaseIcon = { Text("+") },
                                    decreaseIcon = { Text("-") },
                                ) {
                                    Text("StepperContent")
                                }
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(screenPadding) > 0.dp)

        // Show Dialog -> window status bar suppressed, but screen padding is preserved
        composeTestRule.runOnUiThread { showDialog = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(screenPadding) > 0.dp)

        // Show Stepper while Dialog is still open -> still suppressed, and screen padding collapses
        // to 0.dp
        composeTestRule.runOnUiThread { showStepper = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, screenPadding)

        // Dismiss Stepper -> still suppressed by Dialog, but screen padding restored
        composeTestRule.runOnUiThread { showStepper = false }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(screenPadding) > 0.dp)

        // Dismiss Dialog -> restored
        composeTestRule.runOnUiThread { showDialog = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(screenPadding) > 0.dp)
    }

    @Test
    fun stepper_withCallerSuppressionModifier_doesNotDuplicateSuppression() {
        var scaffoldState: AppScaffoldState? = null
        var observedPadding: Dp? = null
        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current as? AppScaffoldState
                        ScreenScaffold(contentPadding = PaddingValues(0.dp)) { padding ->
                            observedPadding = padding.calculateTopPadding()
                            Stepper(
                                value = 1f,
                                onValueChange = {},
                                steps = 3,
                                increaseIcon = { Text("+") },
                                decreaseIcon = { Text("-") },
                                modifier = Modifier.suppressStatusBar(),
                            ) {
                                Text("StepperWithSuppression")
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, observedPadding)
    }

    @Test
    fun stepper_whenDisplayedInScaffold_preservesUnderlyingScreenScrollInfo() {
        var showStepper by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(scrollInfoProvider = mockScrollInfo) {
                        if (showStepper) {
                            Stepper(
                                value = 2f,
                                onValueChange = {},
                                steps = 3,
                                increaseIcon = { Text("+") },
                                decreaseIcon = { Text("-") },
                            ) {
                                Text("StepperContent")
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
        composeTestRule.runOnUiThread { showStepper = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)

        composeTestRule.runOnUiThread { showStepper = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun timePicker_whenDisplayedInScaffold_preservesUnderlyingScreenScrollInfo() {
        var showTimePicker by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(scrollInfoProvider = mockScrollInfo) {
                        if (showTimePicker) {
                            TimePicker(initialTime = LocalTime.of(10, 30), onTimePicked = {})
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
        composeTestRule.runOnUiThread { showTimePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)

        composeTestRule.runOnUiThread { showTimePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun datePicker_whenDisplayedInScaffold_preservesUnderlyingScreenScrollInfo() {
        var showDatePicker by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(scrollInfoProvider = mockScrollInfo) {
                        if (showDatePicker) {
                            DatePicker(initialDate = LocalDate.of(2026, 1, 1), onDatePicked = {})
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)

        composeTestRule.runOnUiThread { showDatePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)

        composeTestRule.runOnUiThread { showDatePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun screenNavigation_switchingActiveScreen_updatesStatusBarAndScrollInfo() {
        var activeScreen by mutableStateOf("A")
        var scaffoldState: ScaffoldState? = null
        val scrollInfoA =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }
        val scrollInfoB =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 20f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    CompositionLocalProvider(LocalScreenIsActive provides (activeScreen == "A")) {
                        ScreenScaffold(scrollInfoProvider = scrollInfoA) {}
                    }
                    CompositionLocalProvider(LocalScreenIsActive provides (activeScreen == "B")) {
                        ScreenScaffold(
                            timeText = { TimeText() },
                            scrollInfoProvider = scrollInfoB,
                        ) {}
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(scrollInfoA, state.screenContent.currentScrollInfoProvider.value)

        // Switch to Screen B
        composeTestRule.runOnUiThread { activeScreen = "B" }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(scrollInfoB, state.screenContent.currentScrollInfoProvider.value)

        // Switch back to Screen A
        composeTestRule.runOnUiThread { activeScreen = "A" }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(scrollInfoA, state.screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun screenScaffold_abruptDisposal_removesFromScreenAndStatusBarStacks() {
        var showScreen by mutableStateOf(true)
        var scaffoldState: ScaffoldState? = null
        val mockScrollInfo =
            object : ScrollInfoProvider {
                override val anchorItemOffset: Float = 10f
                override val isScrollAwayValid: Boolean = true
                override val isScrollInProgress: Boolean = false
                override val isScrollable: Boolean = true
                override val lastItemOffset: Float = 0f
            }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    if (showScreen) {
                        ScreenScaffold(
                            timeText = { TimeText() },
                            scrollInfoProvider = mockScrollInfo,
                        ) {}
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertSame(mockScrollInfo, state.screenContent.currentScrollInfoProvider.value)

        // Abruptly dispose the ScreenScaffold
        composeTestRule.runOnUiThread { showScreen = false }
        composeTestRule.waitForIdle()

        // Reverts to AppScaffold fallback
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertNull(state.screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun appScaffold_dynamicToggle_updatesInheritedScreens() {
        var doesAppScaffoldWantStatusBar by mutableStateOf(true)
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                val emptyTimeText: @Composable () -> Unit = {}
                AppScaffold(
                    timeText =
                        if (doesAppScaffoldWantStatusBar) AppScaffoldDefaults.timeText
                        else emptyTimeText
                ) {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold {}
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)

        composeTestRule.runOnUiThread { doesAppScaffoldWantStatusBar = false }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)

        composeTestRule.runOnUiThread { doesAppScaffoldWantStatusBar = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
    }

    @Test
    fun appScaffold_withNoScreenScaffolds_drivesStatusBarDirectly() {
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold(timeText = {}) {
                    scaffoldState = LocalScaffoldState.current
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertNull(state.screenContent.currentScrollInfoProvider.value)
    }

    @Test
    fun suppressStatusBar_whenInScaffold_registersSuppressionScreen() {
        var suppress by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            Box(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .then(
                                            if (suppress) Modifier.suppressStatusBar() else Modifier
                                        )
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)

        composeTestRule.runOnUiThread { suppress = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)

        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)
    }

    @Test
    fun suppressStatusBar_onScreenScaffold_activelySuppresses() {
        var scaffoldState: ScaffoldState? = null
        var topPadding: Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            modifier = Modifier.suppressStatusBar(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)
    }

    @Test
    fun suppressStatusBar_whenOutOfScaffold_hidesAndRestoresStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var suppress by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                Box(modifier = if (suppress) Modifier.suppressStatusBar() else Modifier)
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)

        composeTestRule.runOnUiThread { suppress = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun suppressStatusBar_whenScreenNotActive_doesNotMutateOrRestoreStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var suppress by mutableStateOf(true)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
                LocalScreenIsActive provides false,
            ) {
                Box(modifier = if (suppress) Modifier.suppressStatusBar() else Modifier)
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)
        Assert.assertEquals(0, testView.testController.showCount)

        // Dispose without ever activating screen
        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)
        Assert.assertEquals(0, testView.testController.showCount)
    }

    @Test
    fun statusBarOrchestrator_whenNoMutationsApplied_restoreDoesNotMutateWindow() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        val orchestrator = StatusBarOrchestrator(testView)
        orchestrator.restoreInitialStatusBarState()

        Assert.assertEquals(0, testView.testController.showCount)
        Assert.assertEquals(0, testView.testController.hideCount)
    }

    @Test
    fun suppressStatusBar_onScreenScaffold_suppressesStatusBar() {
        var suppress by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            modifier = if (suppress) Modifier.suppressStatusBar() else Modifier,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)

        composeTestRule.runOnUiThread { suppress = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)

        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)
    }

    @Test
    fun suppressStatusBar_onScreenScaffold_collapsesTopPaddingAndProvidesFalse() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var scaffoldState: ScaffoldState? = null
        var inheritedShow: Boolean? = null
        val observedPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(
                    LocalStatusBarEnabledForTest provides true,
                    LocalView provides testView,
                ) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            modifier = Modifier.suppressStatusBar(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { contentPadding ->
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().layout { measurable, constraints ->
                                        observedPaddings.add(contentPadding.calculateTopPadding())
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                            )
                            inheritedShow =
                                LocalScaffoldStatusBarState.current?.showStatusBar?.value
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(false, inheritedShow)
        Assert.assertTrue("Observed at least one measure pass", observedPaddings.isNotEmpty())
        Assert.assertTrue(
            "ScreenScaffold with Modifier.suppressStatusBar() must maintain stable 0.dp status bar padding across all frames, but observed: $observedPaddings",
            observedPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun suppressStatusBar_onAppScaffold_suppressesStatusBarAndInheritedState() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var scaffoldState: ScaffoldState? = null
        var innerInheritedShow: Boolean? = null
        val observedPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(
                    LocalStatusBarEnabledForTest provides true,
                    LocalView provides testView,
                ) {
                    AppScaffold(modifier = Modifier.suppressStatusBar()) {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { contentPadding ->
                            Box(
                                modifier =
                                    Modifier.fillMaxSize().layout { measurable, constraints ->
                                        observedPaddings.add(contentPadding.calculateTopPadding())
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                            )
                            innerInheritedShow =
                                LocalScaffoldStatusBarState.current?.showStatusBar?.value
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(false, innerInheritedShow)
        Assert.assertTrue("Observed at least one measure pass", observedPaddings.isNotEmpty())
        Assert.assertTrue(
            "Inner ScreenScaffold inside AppScaffold(modifier = Modifier.suppressStatusBar()) must maintain stable 0.dp status bar padding across all frames, but observed: $observedPaddings",
            observedPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun suppressStatusBar_onHorizontalPagerScaffold_suppressesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var scaffoldState: ScaffoldState? = null
        var innerInheritedShow: Boolean? = null
        val observedPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(
                    LocalStatusBarEnabledForTest provides true,
                    LocalView provides testView,
                ) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        val pagerState = rememberPagerState(pageCount = { 2 })
                        HorizontalPagerScaffold(
                            pagerState = pagerState,
                            modifier = Modifier.suppressStatusBar(),
                        ) {
                            ScreenScaffold(
                                contentPadding =
                                    androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { contentPadding ->
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize().layout { measurable, constraints ->
                                            observedPaddings.add(
                                                contentPadding.calculateTopPadding()
                                            )
                                            val placeable = measurable.measure(constraints)
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                )
                                innerInheritedShow =
                                    LocalScaffoldStatusBarState.current?.showStatusBar?.value
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(false, innerInheritedShow)
        Assert.assertTrue("Observed at least one measure pass", observedPaddings.isNotEmpty())
        Assert.assertTrue(
            "Page inside HorizontalPagerScaffold(modifier = Modifier.suppressStatusBar()) must maintain stable 0.dp status bar padding across all frames, but observed: $observedPaddings",
            observedPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun suppressStatusBar_onVerticalPagerScaffold_suppressesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var scaffoldState: ScaffoldState? = null
        var innerInheritedShow: Boolean? = null
        val observedPaddings = mutableListOf<androidx.compose.ui.unit.Dp>()

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(
                    LocalStatusBarEnabledForTest provides true,
                    LocalView provides testView,
                ) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        val pagerState = rememberPagerState(pageCount = { 2 })
                        VerticalPagerScaffold(
                            pagerState = pagerState,
                            modifier = Modifier.suppressStatusBar(),
                        ) {
                            ScreenScaffold(
                                contentPadding =
                                    androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { contentPadding ->
                                Box(
                                    modifier =
                                        Modifier.fillMaxSize().layout { measurable, constraints ->
                                            observedPaddings.add(
                                                contentPadding.calculateTopPadding()
                                            )
                                            val placeable = measurable.measure(constraints)
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                )
                                innerInheritedShow =
                                    LocalScaffoldStatusBarState.current?.showStatusBar?.value
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(false, innerInheritedShow)
        Assert.assertTrue("Observed at least one measure pass", observedPaddings.isNotEmpty())
        Assert.assertTrue(
            "Page inside VerticalPagerScaffold(modifier = Modifier.suppressStatusBar()) must maintain stable 0.dp status bar padding across all frames, but observed: $observedPaddings",
            observedPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun timePicker_whenDisplayedInScaffold_suppressesStatusBar() {
        var showTimePicker by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            if (showTimePicker) {
                                TimePicker(initialTime = LocalTime.of(10, 30), onTimePicked = {})
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)

        composeTestRule.runOnUiThread { showTimePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)

        composeTestRule.runOnUiThread { showTimePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)
    }

    @Test
    fun timePicker_whenDisplayedOutOfScaffold_hardHidesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var showTimePicker by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                if (showTimePicker) {
                    TimePicker(initialTime = LocalTime.of(10, 30), onTimePicked = {})
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)

        composeTestRule.runOnUiThread { showTimePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        composeTestRule.runOnUiThread { showTimePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun datePicker_whenDisplayedInScaffold_suppressesStatusBar() {
        var showDatePicker by mutableStateOf(false)
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            if (showDatePicker) {
                                DatePicker(
                                    initialDate = LocalDate.of(2026, 1, 1),
                                    onDatePicked = {},
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)

        composeTestRule.runOnUiThread { showDatePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)

        composeTestRule.runOnUiThread { showDatePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(topPadding) > 0.dp)
    }

    @Test
    fun datePicker_whenDisplayedOutOfScaffold_hardHidesStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var showDatePicker by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                if (showDatePicker) {
                    DatePicker(initialDate = LocalDate.of(2026, 1, 1), onDatePicked = {})
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)

        composeTestRule.runOnUiThread { showDatePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        composeTestRule.runOnUiThread { showDatePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun screenScaffold_standalone_whenSuppressed_hidesWindowStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var suppress by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                ScreenScaffold(
                    modifier = if (suppress) Modifier.suppressStatusBar() else Modifier,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)

        composeTestRule.runOnUiThread { suppress = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun screenScaffold_standalone_withTimePicker_hidesWindowStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var showTimePicker by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                ScreenScaffold(contentPadding = PaddingValues(0.dp)) {
                    if (showTimePicker) {
                        TimePicker(initialTime = LocalTime.of(10, 30), onTimePicked = {})
                    } else {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)

        composeTestRule.runOnUiThread { showTimePicker = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        composeTestRule.runOnUiThread { showTimePicker = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun screenScaffold_withCustomTimeTextAndSuppression_silencesTimeTextAndCollapsesPadding() {
        var scaffoldState: ScaffoldState? = null
        var topPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        ScreenScaffold(
                            modifier = Modifier.suppressStatusBar(),
                            timeText = { Text("CustomScreenTimeText") },
                            contentPadding = PaddingValues(0.dp),
                        ) { padding ->
                            topPadding = padding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, topPadding)
        composeTestRule.onNodeWithText("CustomScreenTimeText").assertDoesNotExist()
    }

    @Test
    fun screenScaffold_directSuppressStatusBar_compositionBeforeLayoutObservesInitialInset() {
        var frame0ShowStatusBar: Boolean? = null
        var finalTopPadding: androidx.compose.ui.unit.Dp? = null
        var finalShowStatusBar: Boolean? = null
        var compositionCount = 0

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        ScreenScaffold(
                            modifier = Modifier.suppressStatusBar(),
                            contentPadding = PaddingValues(0.dp),
                        ) { padding ->
                            compositionCount++
                            if (frame0ShowStatusBar == null) {
                                frame0ShowStatusBar =
                                    LocalScaffoldStatusBarState.current?.showStatusBar?.value
                            }
                            finalTopPadding = padding.calculateTopPadding()
                            finalShowStatusBar =
                                LocalScaffoldStatusBarState.current?.showStatusBar?.value
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Before layout pass, composition observes initial unsuppressed insets
        // because Modifier.Node attachment occurs during the layout phase.
        Assert.assertEquals(
            "Initial composition before layout pass observes unsuppressed showStatusBar = true",
            true,
            frame0ShowStatusBar,
        )

        // After layout pass attaches the suppression node, it recomposes and settles to suppressed.
        Assert.assertEquals(
            "Expected recomposition once suppression node attaches during layout",
            2,
            compositionCount,
        )
        Assert.assertEquals(
            "Final settled showStatusBar is false",
            false,
            finalShowStatusBar,
        )
        Assert.assertEquals(
            "Final settled top padding collapses to 0.dp",
            0.dp,
            finalTopPadding,
        )
    }

    @Test
    fun screenScaffold_dynamicRebindingOfLocalScaffoldStatusBarState_updatesDerivedState() {
        val falseState =
            ScaffoldStatusBarState(
                timeText = mutableStateOf(null),
                unsuppressedStatusBarMode = mutableStateOf(StatusBarMode.Disabled),
                isSupported = mutableStateOf(true),
                parentStatusBarState = mutableStateOf(null),
            )
        val trueState =
            ScaffoldStatusBarState(
                timeText = mutableStateOf(null),
                unsuppressedStatusBarMode = mutableStateOf(StatusBarMode.Enabled),
                isSupported = mutableStateOf(true),
                parentStatusBarState = mutableStateOf(null),
            )
        var parentStateInstance by mutableStateOf<ScaffoldStatusBarState?>(falseState)
        var observedShowStatusBar: Boolean? = null
        var observedTopPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    CompositionLocalProvider(
                        LocalScaffoldStatusBarState provides parentStateInstance
                    ) {
                        ScreenScaffold(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { padding ->
                            observedShowStatusBar =
                                LocalScaffoldStatusBarState.current?.showStatusBar?.value
                            observedTopPadding = padding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(false, observedShowStatusBar)
        Assert.assertEquals(0.dp, observedTopPadding)

        // Rebind to trueState
        parentStateInstance = trueState
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            "ScreenScaffold should reflect the newly provided LocalScaffoldStatusBarState instance",
            true,
            observedShowStatusBar,
        )
        Assert.assertTrue(
            "ScreenScaffold top padding should expand when status bar is enabled",
            checkNotNull(observedTopPadding) > 0.dp,
        )

        // Rebind back to falseState
        parentStateInstance = falseState
        composeTestRule.waitForIdle()
        Assert.assertEquals(
            "ScreenScaffold should react when rebound back to falseState",
            false,
            observedShowStatusBar,
        )
        Assert.assertEquals(0.dp, observedTopPadding)
    }

    @Test
    fun screenScaffold_directSuppressStatusBar_toggleFromTrueToFalse_unsuppressesStatusBar() {
        var enabled by mutableStateOf(true)
        var observedTopPadding: androidx.compose.ui.unit.Dp? = null
        var observedShowStatusBar: Boolean? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        ScreenScaffold(
                            modifier = if (enabled) Modifier.suppressStatusBar() else Modifier,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { padding ->
                            observedTopPadding = padding.calculateTopPadding()
                            observedShowStatusBar =
                                LocalScaffoldStatusBarState.current?.showStatusBar?.value
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Initial state (enabled = true)
        Assert.assertEquals(false, observedShowStatusBar)
        Assert.assertEquals(0.dp, observedTopPadding)

        // 2. Toggle to false -> unsuppresses
        enabled = false
        composeTestRule.waitForIdle()
        Assert.assertEquals(true, observedShowStatusBar)
        Assert.assertTrue(
            "Top padding must expand when unsuppressed",
            checkNotNull(observedTopPadding) > 0.dp,
        )

        // 3. Toggle back to true -> re-suppresses
        enabled = true
        composeTestRule.waitForIdle()
        Assert.assertEquals(false, observedShowStatusBar)
        Assert.assertEquals(0.dp, observedTopPadding)
    }

    @Test
    fun standaloneStatusBarCoordinator_onAttachedToWindow_disposesPreviousOrchestrator() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testView = TestView(context)
        val mockInsets =
            WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
        testView.mockRootWindowInsets = mockInsets
        testView.simulateAttach()

        val coordinator = StandaloneStatusBarCoordinator.getOrCreate(testView)
        coordinator.acquireSuppression()
        Assert.assertEquals(1, testView.testController.hideCount)

        // Re-attachment to window should safely dispose old orchestrator and create new one
        testView.simulateAttach()
        Assert.assertEquals(2, testView.testController.hideCount)

        // Releasing suppression restores the window status bar through active orchestrator
        coordinator.releaseSuppression()
        Assert.assertEquals(1, testView.testController.showCount)
    }

    @Test
    fun screenScaffold_contentPadding_implementsStructuralEqualityAndToString() {
        var observedPaddingValues: androidx.compose.foundation.layout.PaddingValues? = null
        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    ScreenScaffold(
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                start = 10.dp,
                                top = 5.dp,
                                end = 15.dp,
                                bottom = 20.dp,
                            )
                    ) { padding ->
                        observedPaddingValues = padding
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val padding = checkNotNull(observedPaddingValues)

        // Verifies equality with itself
        Assert.assertEquals(padding, padding)
        Assert.assertEquals(padding.hashCode(), padding.hashCode())

        // Verifies it is not equal to a standard PaddingValues instance
        val standardPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 10.dp,
                top = padding.calculateTopPadding(),
                end = 15.dp,
                bottom = 20.dp,
            )
        Assert.assertNotEquals(padding, standardPadding)

        Assert.assertEquals(
            10.dp,
            padding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        )
        Assert.assertEquals(
            15.dp,
            padding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        )
        Assert.assertEquals(
            20.dp,
            padding.calculateBottomPadding(),
        )
        val str = padding.toString()
        Assert.assertTrue(
            "toString should start with ScreenScaffoldPaddingValues",
            str.startsWith("ScreenScaffoldPaddingValues("),
        )
        Assert.assertTrue("toString should contain contentPadding", str.contains("contentPadding="))
        Assert.assertTrue("toString should contain safeInsets", str.contains("safeInsets="))
        Assert.assertTrue("toString should contain density", str.contains("density="))
        Assert.assertTrue(
            "toString should contain showStatusBarState",
            str.contains("showStatusBarState="),
        )
    }

    @Test
    fun horizontalPagerScaffold_standalone_whenSuppressed_hidesWindowStatusBar() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var suppress by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                val pagerState = rememberPagerState(pageCount = { 2 })
                HorizontalPagerScaffold(
                    pagerState = pagerState,
                    modifier = if (suppress) Modifier.suppressStatusBar() else Modifier,
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        composeTestRule.waitForIdle()
        // Prior to suppression, the physical status bar is unmutated.
        Assert.assertEquals(0, testView.testController.hideCount)

        // Standalone HorizontalPagerScaffold coordinates directly with
        // StandaloneStatusBarCoordinator
        // to hide the window status bar even when not enclosed in AppScaffold.
        composeTestRule.runOnUiThread { suppress = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        // Unsuppressing restores the window's initial status bar visibility state.
        composeTestRule.runOnUiThread { suppress = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun suppressStatusBar_multipleConcurrentStandaloneNodes_refCountsAndPreservesBaseline() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }
        var suppressFirst by mutableStateOf(false)
        var suppressSecond by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = if (suppressFirst) Modifier.suppressStatusBar() else Modifier)
                    Box(modifier = if (suppressSecond) Modifier.suppressStatusBar() else Modifier)
                }
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.hideCount)
        Assert.assertEquals(0, testView.testController.showCount)

        // 1. First standalone node requests suppression -> hides the window status bar.
        composeTestRule.runOnUiThread { suppressFirst = true }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)

        // 2. Second standalone node requests suppression concurrently ->
        // StandaloneStatusBarCoordinator
        // ref-counts the suppression request on the shared View, preventing baseline corruption.
        composeTestRule.runOnUiThread { suppressSecond = true }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.showCount)

        // 3. Dismiss the first node -> status bar must NOT be restored prematurely because the
        // second node is still actively holding suppression.
        composeTestRule.runOnUiThread { suppressFirst = false }
        composeTestRule.waitForIdle()
        Assert.assertEquals(0, testView.testController.showCount)

        // 4. Dismiss the second node -> now all active callers on this View have released
        // suppression, cleanly restoring the status bar to its true initial baseline.
        composeTestRule.runOnUiThread { suppressSecond = false }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun standaloneStatusBarCoordinator_doesNotLeakViewReference() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val weakView = createWeakViewAndRegisterCoordinator(targetContext)

        // Trigger garbage collection
        var gcAttempts = 0
        while (weakView.get() != null && gcAttempts < 10) {
            Runtime.getRuntime().gc()
            System.runFinalization()
            android.os.SystemClock.sleep(50)
            gcAttempts++
        }

        Assert.assertNull(
            "View registered with StandaloneStatusBarCoordinator must be garbage collectable " +
                "when external strong references are cleared (no cyclic value-to-key leak)",
            weakView.get(),
        )
    }

    private fun createWeakViewAndRegisterCoordinator(
        context: Context
    ): java.lang.ref.WeakReference<View> {
        val testView = TestView(context)
        StandaloneStatusBarCoordinator.getOrCreate(testView)
        return java.lang.ref.WeakReference(testView)
    }

    @Test
    fun standaloneStatusBarCoordinator_viewDetachedFromWindow_disposesOrchestratorAndClearsListeners() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }

        val coordinator = StandaloneStatusBarCoordinator.getOrCreate(testView)
        coordinator.acquireSuppression()
        testView.simulateLayout()

        // Prior to detachment, suppression is active and listeners are attached
        Assert.assertTrue(testView.testController.hideCount > 0)
        Assert.assertTrue(testView.layoutListenerCount > 0 || testView.attachListenerCount > 0)

        // Simulate view detachment from window
        testView.simulateDetach()

        // Verify orchestrator.dispose() cleared all view listeners, suppression is released,
        // and baseline is restored
        Assert.assertEquals(0, testView.layoutListenerCount)
        Assert.assertTrue(testView.testController.showCount > 0)
    }

    @Test
    fun suppressStatusBar_viewDetachedAndReattached_reappliesSuppressionAutomatically() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testView =
            TestView(targetContext).apply {
                mockIsAttachedToWindow = true
                mockRootWindowInsets =
                    WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build()
            }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalStatusBarEnabledForTest provides true,
                LocalView provides testView,
            ) {
                Box(modifier = Modifier.suppressStatusBar())
            }
        }
        composeTestRule.waitForIdle()
        Assert.assertTrue(testView.testController.hideCount > 0)
        val initialHideCount = testView.testController.hideCount

        // 1. Simulate view detachment -> window baseline is restored while detached
        testView.simulateDetach()
        Assert.assertTrue(testView.testController.showCount > 0)

        // 2. Simulate view re-attachment to window -> suppression is automatically re-applied
        // without requiring artificial Compose state mutations.
        testView.simulateAttach()
        testView.simulateLayout()

        // Verify suppression was successfully re-applied on the re-attached window
        Assert.assertTrue(testView.testController.hideCount > initialHideCount)
    }

    @Test
    fun screenScaffold_transitionFromDisabledToInherit_doesNotRenderComposeTimeText() {
        var screenTimeText by mutableStateOf<(@Composable () -> Unit)?>({ Text("ScreenDisabled") })
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(timeText = screenTimeText) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)

        // On screenTimeText != null, status bar is disabled for the screen, local screen timeText
        // is shown
        Assert.assertFalse(state.screenContent.shouldAppWindowShowStatusBar.value)
        composeTestRule.onNodeWithText("ScreenDisabled").assertIsDisplayed()

        // Transition to Inherit (timeText = null)
        composeTestRule.runOnUiThread { screenTimeText = null }
        composeTestRule.waitForIdle()

        // GSB is re-enabled for this screen
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)
        // ScreenDisabled is gone
        composeTestRule.onNodeWithText("ScreenDisabled").assertDoesNotExist()
    }

    @Test
    fun screenScaffold_transitionFromEmptyToInherit_doesNotRenderComposeTimeText() {
        var screenTimeText by mutableStateOf<(@Composable () -> Unit)?>({})
        var scaffoldState: ScaffoldState? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                AppScaffold {
                    scaffoldState = LocalScaffoldState.current
                    ScreenScaffold(timeText = screenTimeText) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)

        // Empty lambda ({}) disables status bar, but renders no visible content
        Assert.assertFalse(state.screenContent.shouldAppWindowShowStatusBar.value)

        // Transition to Inherit (timeText = null)
        composeTestRule.runOnUiThread { screenTimeText = null }
        composeTestRule.waitForIdle()

        // GSB is re-enabled for this screen
        Assert.assertTrue(state.screenContent.shouldAppWindowShowStatusBar.value)
    }

    @Test
    fun pagerWithSuppression_swipingBetweenPages_maintainsSuppressionOnPage2() {
        var scaffoldState: ScaffoldState? = null
        var currentPage by mutableIntStateOf(0)
        var page1Padding: androidx.compose.ui.unit.Dp? = null
        var page2Padding: androidx.compose.ui.unit.Dp? = null
        var page3Padding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        scaffoldState = LocalScaffoldState.current
                        val pagerState = rememberPagerState(pageCount = { 3 })
                        LaunchedEffect(currentPage) { pagerState.scrollToPage(currentPage) }
                        HorizontalPagerScaffold(pagerState = pagerState) {
                            HorizontalPager(state = pagerState) { page ->
                                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                    when (page) {
                                        0 ->
                                            ScreenScaffold(
                                                contentPadding =
                                                    androidx.compose.foundation.layout
                                                        .PaddingValues(0.dp)
                                            ) { padding ->
                                                page1Padding = padding.calculateTopPadding()
                                                Box(Modifier.fillMaxSize())
                                            }
                                        1 ->
                                            ScreenScaffold(
                                                contentPadding =
                                                    androidx.compose.foundation.layout
                                                        .PaddingValues(0.dp)
                                            ) { padding ->
                                                page2Padding = padding.calculateTopPadding()
                                                Stepper(
                                                    value = 2f,
                                                    onValueChange = {},
                                                    steps = 3,
                                                    increaseIcon = { Text("+") },
                                                    decreaseIcon = { Text("-") },
                                                ) {
                                                    Box(Modifier.fillMaxSize())
                                                }
                                            }
                                        else ->
                                            ScreenScaffold(
                                                contentPadding =
                                                    androidx.compose.foundation.layout
                                                        .PaddingValues(0.dp)
                                            ) { padding ->
                                                page3Padding = padding.calculateTopPadding()
                                                Box(Modifier.fillMaxSize())
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        val state = checkNotNull(scaffoldState)
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(page1Padding) > 0.dp)

        // Swipe to Page 2
        composeTestRule.runOnIdle { currentPage = 1 }
        composeTestRule.waitForIdle()
        Assert.assertFalse(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertEquals(0.dp, page2Padding)

        // Swipe to Page 3
        composeTestRule.runOnIdle { currentPage = 2 }
        composeTestRule.waitForIdle()
        Assert.assertTrue(state.screenContent.shouldActiveWindowShowStatusBar.value)
        Assert.assertTrue(checkNotNull(page3Padding) > 0.dp)

        // Swipe back to Page 2
        composeTestRule.runOnIdle { currentPage = 1 }
        composeTestRule.waitForIdle()
        Assert.assertFalse(
            "Page 2 should suppress status bar after swiping back from Page 3",
            state.screenContent.shouldActiveWindowShowStatusBar.value,
        )
        Assert.assertEquals(0.dp, page2Padding)
    }

    @Test
    fun appScaffold_withSuppression_silencesInnerScreenScaffoldCustomTimeText() {
        val testTag = "inner_screen_time_text"
        var observedPadding: androidx.compose.ui.unit.Dp? = null
        var isSuppressedObserved: Boolean? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold(modifier = Modifier.suppressStatusBar()) {
                        ScreenScaffold(
                            timeText = { TimeText(modifier = Modifier.testTag(testTag)) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { padding ->
                            observedPadding = padding.calculateTopPadding()
                            isSuppressedObserved =
                                LocalScaffoldStatusBarState.current?.isSuppressed?.value
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Time text composable should be completely silenced
        composeTestRule.onNodeWithTag(testTag).assertDoesNotExist()

        // Inner screen inherits suppression: isSuppressed is true, and top padding collapses to
        // 0.dp
        Assert.assertEquals(true, isSuppressedObserved)
        Assert.assertEquals(0.dp, observedPadding)
    }

    @Test
    fun pagerScaffold_withSuppression_silencesInnerScreenScaffoldCustomTimeText() {
        val testTag = "pager_page_time_text"
        var observedPadding: androidx.compose.ui.unit.Dp? = null
        var isSuppressedObserved: Boolean? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold {
                        val pagerState = rememberPagerState(pageCount = { 1 })
                        HorizontalPagerScaffold(
                            pagerState = pagerState,
                            modifier = Modifier.suppressStatusBar(),
                        ) {
                            ScreenScaffold(
                                timeText = { TimeText(modifier = Modifier.testTag(testTag)) },
                                contentPadding =
                                    androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) { padding ->
                                observedPadding = padding.calculateTopPadding()
                                isSuppressedObserved =
                                    LocalScaffoldStatusBarState.current?.isSuppressed?.value
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Time text composable should be silenced by pager suppression
        composeTestRule.onNodeWithTag(testTag).assertDoesNotExist()
        Assert.assertEquals(true, isSuppressedObserved)
        Assert.assertEquals(0.dp, observedPadding)
    }

    @Test
    fun appScaffold_withSuppressionDynamicToggle_restoresInnerScreenScaffoldCustomTimeText() {
        val testTag = "inner_screen_time_text"
        var isSuppressed by mutableStateOf(true)
        var observedPadding: androidx.compose.ui.unit.Dp? = null

        composeTestRule.setContent {
            WithMockStatusBarInsets {
                CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                    AppScaffold(
                        modifier = if (isSuppressed) Modifier.suppressStatusBar() else Modifier
                    ) {
                        ScreenScaffold(
                            timeText = { TimeText(modifier = Modifier.testTag(testTag)) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { padding ->
                            observedPadding = padding.calculateTopPadding()
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // 1. Initially suppressed: timeText does not exist and padding collapses to 0.dp
        composeTestRule.onNodeWithTag(testTag).assertDoesNotExist()
        Assert.assertEquals(0.dp, observedPadding)

        // 2. Toggle suppression off: custom timeText is restored
        isSuppressed = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(testTag).assertIsDisplayed()
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

        fun simulateDetach() {
            for (listener in attachListeners.toList()) {
                listener.onViewDetachedFromWindow(this)
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

private val ScaffoldState.screenContent: ScreenContent
    get() = (this as AppScaffoldState).screenContent

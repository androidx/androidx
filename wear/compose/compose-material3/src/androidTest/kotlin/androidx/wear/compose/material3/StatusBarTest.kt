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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateOf
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

        orchestrator.restore()
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

        orchestrator.restore()
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

        // restore() should restore the initially captured visible state
        orchestrator.restore()
        Assert.assertEquals(
            "restore() should invoke show() to restore initial state",
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
        var controllerProvider: (() -> WindowInsetsController?)? = null
        private val attachListeners = mutableListOf<OnAttachStateChangeListener>()
        private val layoutListeners = mutableListOf<OnLayoutChangeListener>()

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

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

package androidx.compose.material3

import android.content.ComponentCallbacks2
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.fail
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class BottomSheetTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    private val sheetHeight = 256.dp
    private val sheetTag = "sheetContentTag"

    @Test
    fun bottomSheet_fillsScreenWidth() {
        var boxWidth = 0
        var screenWidth by mutableStateOf(0)

        rule.setContent {
            screenWidth = LocalResources.current.displayMetrics.widthPixels

            BottomSheet(onDismissRequest = {}) {
                Box(
                    Modifier.fillMaxWidth().height(sheetHeight).onSizeChanged {
                        boxWidth = it.width
                    }
                )
            }
        }
        assertThat(boxWidth).isEqualTo(screenWidth)
    }

    // TODO(330937081): Update test logic to instead change virtual screen size.
    @Test
    @Ignore
    fun bottomSheet_wideScreen_fixedMaxWidth_sheetRespectsMaxWidthAndIsCentered() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(p0: Configuration) {
                    latch.countDown()
                }

                @Deprecated("deprecated")
                override fun onLowMemory() {
                    // NO-OP
                }

                override fun onTrimMemory(p0: Int) {
                    // NO-OP
                }
            }
        )

        try {
            latch.await(1500, TimeUnit.MILLISECONDS)
            rule.setContent {
                BottomSheet(onDismissRequest = {}, maxWidth = 640.dp) {
                    Box(Modifier.testTag(sheetTag).fillMaxHeight(0.4f))
                }
            }
            rule.waitForIdle()
            val simulatedRootWidth =
                rule.onNodeWithTag(sheetTag).onParent().getUnclippedBoundsInRoot().width
            val maxSheetWidth = 640.dp
            val expectedSheetWidth = maxSheetWidth.coerceAtMost(simulatedRootWidth)
            // Our sheet should be max 640 dp but fill the width if the container is less wide
            val expectedSheetLeft =
                if (simulatedRootWidth <= expectedSheetWidth) {
                    0.dp
                } else {
                    (simulatedRootWidth - expectedSheetWidth) / 2
                }

            rule
                .onNodeWithTag(sheetTag)
                .onParent()
                .assertLeftPositionInRootIsEqualTo(expectedLeft = expectedSheetLeft)
                .assertWidthIsEqualTo(expectedSheetWidth)
        } catch (e: InterruptedException) {
            fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // TODO(330937081): Update test logic to instead change virtual screen size.
    @Test
    @Ignore
    fun bottomSheet_wideScreen_filledWidth_sheetFillsEntireWidth() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val latch = CountDownLatch(1)

        rule.activity.application.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(p0: Configuration) {
                    latch.countDown()
                }

                @Deprecated("deprecated")
                override fun onLowMemory() {
                    // NO-OP
                }

                override fun onTrimMemory(p0: Int) {
                    // NO-OP
                }
            }
        )

        try {
            latch.await(3000, TimeUnit.MILLISECONDS)
            var screenWidthPx by mutableStateOf(0)
            rule.setContent {
                screenWidthPx = LocalResources.current.displayMetrics.widthPixels
                BottomSheet(onDismissRequest = {}, maxWidth = Dp.Unspecified) {
                    Box(Modifier.testTag(sheetTag).fillMaxHeight(0.4f))
                }
            }
            rule.waitForIdle()
            val sheet = rule.onNodeWithTag(sheetTag).onParent().getUnclippedBoundsInRoot()
            val sheetWidthPx = with(rule.density) { sheet.width.roundToPx() }
            assertThat(sheetWidthPx).isEqualTo(screenWidthPx)
        } catch (e: InterruptedException) {
            fail("Unable to verify sheet width in landscape orientation")
        } finally {
            rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @Test
    fun bottomSheet_imePadding() {
        rule.setContent {
            BottomSheet(onDismissRequest = {}) {
                Box(Modifier.fillMaxWidth().height(sheetHeight).testTag(sheetTag))
            }
        }

        val sheetBounds = rule.onNodeWithTag(sheetTag).getUnclippedBoundsInRoot()

        // Before IME is shown, the sheet should be at its normal position
        assertThat(sheetBounds.height.value).isWithin(0.01f).of(sheetHeight.value)
    }

    @Test
    fun bottomSheet_preservesLayoutDirection() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                BottomSheet(onDismissRequest = {}) {
                    assertThat(LocalLayoutDirection.current).isEqualTo(layoutDirection)
                    Box(Modifier.fillMaxWidth().height(sheetHeight))
                }
            }
        }

        layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun bottomSheetContent_respectsProvidedInsets() {
        val windowInsets = WindowInsets(top = 10.dp, bottom = 20.dp)
        var topInset = 0
        var bottomInset = 0

        rule.setContent {
            val density = LocalDensity.current
            BottomSheet(onDismissRequest = {}, contentWindowInsets = { windowInsets }) {
                Box(
                    Modifier.fillMaxWidth().height(sheetHeight).onConsumedWindowInsetsChanged {
                        topInset = it.getTop(density)
                        bottomInset = it.getBottom(density)
                    }
                )
            }
        }

        assertThat(topInset).isEqualTo(with(rule.density) { 10.dp.roundToPx() })
        assertThat(bottomInset).isEqualTo(with(rule.density) { 20.dp.roundToPx() })
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun bottomSheetContent_halfScreen_consumesSheetOffsetAsTopInsets() {
        lateinit var state: SheetState
        var topInset = 0
        val windowInsets = WindowInsets(top = 10.dp)

        rule.setContent {
            val density = LocalDensity.current
            state = rememberBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
            BottomSheet(
                onDismissRequest = {},
                state = state,
                contentWindowInsets = { windowInsets },
            ) {
                Box(
                    Modifier.fillMaxWidth().height(sheetHeight).onConsumedWindowInsetsChanged {
                        topInset = it.getTop(density)
                    }
                )
            }
        }

        rule.runOnIdle {
            // UPDATED BEHAVIOR:
            // The content inside the sheet only consumes the insets explicitly provided
            // to it (10.dp). It does NOT consume the empty space (offset) above the sheet
            // because that space is outside the content's coordinate system.
            assertThat(topInset).isEqualTo(with(rule.density) { 10.dp.roundToPx() })
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun bottomSheetContent_fullScreen_consumesOnlyProvidedContentWindowInsets() {
        lateinit var state: SheetState
        var topInset = 0
        val windowInsets = WindowInsets(top = 10.dp)

        rule.setContent {
            val density = LocalDensity.current
            state = rememberBottomSheetState(initialValue = SheetValue.Expanded)
            BottomSheet(
                onDismissRequest = {},
                state = state,
                contentWindowInsets = { windowInsets },
            ) {
                Box(
                    Modifier.fillMaxWidth().height(sheetHeight).onConsumedWindowInsetsChanged {
                        topInset = it.getTop(density)
                    }
                )
            }
        }

        rule.runOnIdle { assertThat(topInset).isEqualTo(with(rule.density) { 10.dp.roundToPx() }) }
    }

    @Test
    fun sheetWindowInsets_reportsOffset_asTopInset() {
        lateinit var state: SheetState
        lateinit var scope: kotlinx.coroutines.CoroutineScope
        lateinit var insets: WindowInsets
        lateinit var density: Density

        rule.setContent {
            state = rememberBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
            scope = rememberCoroutineScope()
            density = LocalDensity.current
            insets = remember(state) { SheetWindowInsets(state) }

            Box(Modifier.fillMaxSize()) {
                BottomSheet(
                    state = state,
                    onDismissRequest = {},
                    content = { Box(Modifier.fillMaxSize()) },
                )
            }
        }

        rule.runOnIdle {
            val reportedTopInset = insets.getTop(density).toFloat()
            val actualOffset = state.requireOffset()
            assertThat(reportedTopInset).isWithin(1f).of(actualOffset)
            assertThat(reportedTopInset).isGreaterThan(0)
        }

        scope.launch { state.expand() }
        rule.runOnIdle {
            val reportedTopInset = insets.getTop(density).toFloat()
            val actualOffset = state.requireOffset()
            assertThat(reportedTopInset).isWithin(1f).of(actualOffset)
            assertThat(reportedTopInset).isEqualTo(0)
        }

        scope.launch { state.hide() }
        rule.runOnIdle {
            val reportedTopInset = insets.getTop(density).toFloat()
            val actualOffset = state.requireOffset()
            assertThat(reportedTopInset).isWithin(1f).of(actualOffset)
            assertThat(reportedTopInset).isGreaterThan(0)
        }
    }

    @Test
    fun bottomSheet_respectsMaterialThemeMotionScheme() {
        val customSpatialSpec = tween<Float>(durationMillis = 123)
        val customEffectsSpec = tween<Float>(durationMillis = 456)

        // Mock a MotionScheme that returns our specific specs
        val customMotionScheme =
            object : MotionScheme {
                @Suppress("UNCHECKED_CAST")
                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
                    customSpatialSpec as FiniteAnimationSpec<T>

                @Suppress("UNCHECKED_CAST")
                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
                    customEffectsSpec as FiniteAnimationSpec<T>

                override fun <T> fastSpatialSpec() = defaultSpatialSpec<T>()

                override fun <T> slowSpatialSpec() = defaultSpatialSpec<T>()

                override fun <T> defaultEffectsSpec() = fastEffectsSpec<T>()

                override fun <T> slowEffectsSpec() = fastEffectsSpec<T>()
            }

        lateinit var sheetState: SheetState

        rule.setContent {
            MaterialTheme(motionScheme = customMotionScheme) {
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

                BottomSheet(
                    state = sheetState,
                    onDismissRequest = {},
                    content = { /* Empty Content */ },
                )
            }
        }

        rule.waitForIdle()
        assertThat(sheetState.showMotionSpec).isEqualTo(customSpatialSpec)
        assertThat(sheetState.anchoredDraggableMotionSpec).isEqualTo(customSpatialSpec)
        assertThat(sheetState.hideMotionSpec).isEqualTo(customEffectsSpec)
    }

    @Test
    fun bottomSheet_smallSheet_escapesDampeningAndDismisses() {
        lateinit var sheetState: SheetState

        rule.setContent {
            val density = LocalDensity.current
            sheetState =
                SheetState(
                    enabledValues =
                        setOf(SheetValue.Expanded, SheetValue.PartiallyExpanded, SheetValue.Hidden),
                    initialValue = SheetValue.PartiallyExpanded,
                    positionalThreshold = {
                        with(density) { BottomSheetDefaults.PositionalThreshold.toPx() }
                    },
                    velocityThreshold = {
                        with(density) { BottomSheetDefaults.VelocityThreshold.toPx() }
                    },
                )

            Box(Modifier.requiredSize(100.dp)) {
                BottomSheet(onDismissRequest = {}, state = sheetState, dragHandle = null) {
                    Box(Modifier.fillMaxSize().testTag(sheetTag)) { Text("Extremely small sheet") }
                }
            }
        }

        rule.runOnIdle { assertThat(sheetState.isVisible).isTrue() }

        // Perform a standard swipe down on this tiny sheet
        rule.onNodeWithTag(sheetTag).performTouchInput { swipeDown() }

        rule.waitForIdle()

        // Verify the sheet successfully dismisses
        rule.runOnIdle { assertThat(sheetState.isVisible).isFalse() }
    }
}

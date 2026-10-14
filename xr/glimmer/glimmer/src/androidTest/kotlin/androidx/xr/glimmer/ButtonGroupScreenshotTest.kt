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

package androidx.xr.glimmer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.inputDeviceCenter
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.samples.ButtonGroupSample
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot tests for [ButtonGroup].
 *
 * These only cover what layout and state assertions can't: the edge scrims, focus appearance, and
 * the content offset in each scroll regime. Scrolling, focus, clamping, and layout behavior are
 * covered by [ButtonGroupTest], and RTL mirroring is covered by [ButtonGroupRtlTest], so only a
 * couple of representative cases are captured in RTL here.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ButtonGroupScreenshotTest {

    @get:Rule(order = 0)
    val rule = createComposeRule(ComposeUiTestConfig(inputMode = InputMode.Keyboard))

    @get:Rule(order = 1) val glimmerRule = createGlimmerRule()

    @get:Rule(order = 2) val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun sample() {
        setContent { ButtonGroupSample() }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_sample_ltr")
    }

    /** The content fits in the viewport, so the content scroll is used directly as user scroll. */
    @Test
    fun contentFitsInViewport_initialState() {
        setContent { ContentFitsInViewportButtonGroup() }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_contentFitsInViewport_initialState_ltr")
    }

    /** The content is only slightly larger than the viewport (`LinearScrollConverter` regime). */
    @Test
    fun contentSlightlyLargerThanViewport_programmaticScrollToMiddleItem() {
        val state = ButtonGroupState()
        setContent {
            // 4 buttons (4 * 100.dp + 3 * 8.dp = 424.dp > 316.dp) falls in the short-scrollable
            // LinearScrollConverter regime.
            ButtonGroup(modifier = Modifier.width(ViewportOuterWidth), state = state) {
                repeat(4) { i ->
                    Button(onClick = {}, modifier = Modifier.width(100.dp)) { Text("$i") }
                }
            }
        }
        runBlocking { state.scrollToItem(1) }
        rule.waitForIdle()
        assertRootAgainstGolden(
            "buttonGroup_contentSlightlyLargerThanViewport_programmaticScrollToMiddleItem_ltr"
        )
    }

    /** The content is much larger than the viewport (`AdaptiveScrollConverter` regime). */
    @Test
    fun contentLargerThanViewport_initialState() {
        setContent { ContentLargerThanViewportButtonGroup() }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_contentLargerThanViewport_initialState_ltr")
    }

    @Test
    fun contentLargerThanViewport_initialState_rtl() {
        setContent(LayoutDirection.Rtl) { ContentLargerThanViewportButtonGroup() }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_contentLargerThanViewport_initialState_rtl")
    }

    /** Scrolled to the middle of the content, so scrims are shown at both edges. */
    @Test
    fun contentLargerThanViewport_programmaticScrollToMiddleItem() {
        val state = ButtonGroupState()
        setContent { ContentLargerThanViewportButtonGroup(state = state) }
        runBlocking { state.scrollToItem(3) }
        rule.waitForIdle()
        assertRootAgainstGolden(
            "buttonGroup_contentLargerThanViewport_programmaticScrollToMiddleItem_ltr"
        )
    }

    @Test
    fun contentLargerThanViewport_indirectScroll_draggingBetweenItems() {
        setContent { ContentLargerThanViewportButtonGroup() }
        // Start at center of item 0 (50.dp). Drag forward by 65.dp to 115.dp (inside item 1, which
        // spans [108.dp, 208.dp]), keeping the pointer down so it has not snapped to item 1's
        // center yet, while focus has already transferred to item 1.
        rule.sendGlimmerIndirectPointerInput {
            down(inputDeviceCenter)
            moveBy(Offset(with(ScreenshotTestDensity) { 65.dp.toPx() }, 0f))
        }
        rule.waitForIdle()
        assertRootAgainstGolden(
            "buttonGroup_contentLargerThanViewport_indirectScroll_draggingBetweenItems_ltr"
        )
    }

    @Test
    fun initialItemIndex_varyingItemSizes_customPadding() {
        setContent { VariedItemSizeButtonGroup() }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_initialItemIndex_varyingItemSizes_customPadding_ltr")
    }

    /** Asymmetric padding and item sizes, so any mistake in mirroring the scrims shows up. */
    @Test
    fun initialItemIndex_varyingItemSizes_customPadding_rtl() {
        setContent(LayoutDirection.Rtl) { VariedItemSizeButtonGroup() }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_initialItemIndex_varyingItemSizes_customPadding_rtl")
    }

    @Test
    fun unfocused_scrolledToMiddleItem() {
        val state = ButtonGroupState()
        setContent(addInitialFocusInterceptor = true) {
            ContentLargerThanViewportButtonGroup(state = state)
        }
        runBlocking { state.scrollToItem(2) }
        rule.waitForIdle()
        assertRootAgainstGolden("buttonGroup_unfocused_scrolledToMiddleItem_ltr")
    }

    /**
     * A [ButtonGroup] whose 3 children (`3 * 76.dp + 2 * 8.dp = 244.dp`) fit comfortably inside the
     * inner viewport (`316.dp`).
     */
    @Composable
    private fun ContentFitsInViewportButtonGroup(
        state: ButtonGroupState = rememberButtonGroupState()
    ) {
        ButtonGroup(modifier = Modifier.width(ViewportOuterWidth), state = state) {
            repeat(3) { i ->
                Button(onClick = {}, modifier = Modifier.width(76.dp)) { Text("$i") }
            }
        }
    }

    /**
     * A [ButtonGroup] whose 6 children (`6 * 100.dp + 5 * 8.dp = 640.dp`) exceed the width of the
     * inner viewport (`316.dp`).
     */
    @Composable
    private fun ContentLargerThanViewportButtonGroup(
        state: ButtonGroupState = rememberButtonGroupState()
    ) {
        ButtonGroup(modifier = Modifier.width(ViewportOuterWidth), state = state) {
            repeat(6) { i ->
                Button(onClick = {}, modifier = Modifier.width(100.dp)) { Text("$i") }
            }
        }
    }

    /**
     * A [ButtonGroup] with a non-zero initial item, varying item sizes, a custom arrangement and
     * alignment, and asymmetric content padding.
     */
    @Composable
    private fun VariedItemSizeButtonGroup() {
        val widths = listOf(64.dp, 110.dp, 80.dp, 130.dp, 72.dp)
        val heights = listOf(48.dp, 64.dp, 56.dp, 72.dp, 48.dp)
        ButtonGroup(
            modifier = Modifier.width(ViewportOuterWidth).height(88.dp),
            state = rememberButtonGroupState(initialItemIndex = 2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 48.dp, bottom = 8.dp),
        ) {
            widths.indices.forEach { i ->
                Button(
                    onClick = {},
                    modifier = Modifier.size(width = widths[i], height = heights[i]),
                ) {
                    Text("$i")
                }
            }
        }
    }

    private fun setContent(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        addInitialFocusInterceptor: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        rule.setGlimmerThemeContent(
            addInitialFocusInterceptor = addInitialFocusInterceptor,
            density = ScreenshotTestDensity,
        ) {
            CompositionLocalProvider(
                LocalViewConfiguration provides TestViewConfiguration(touchSlop = 0f),
                LocalLayoutDirection provides layoutDirection,
                content = content,
            )
        }
    }

    companion object {
        /**
         * Inner viewport width (`316.dp`) = 3 buttons of `100.dp` + 2 default gaps of `8.dp`. Outer
         * width (`404.dp`) = inner viewport width (`316.dp`) + default horizontal content padding
         * (`2 * 44.dp = 88.dp`).
         */
        private val ViewportOuterWidth = 404.dp

        private val ScreenshotTestDensity = Density(2f)
    }

    private fun assertRootAgainstGolden(goldenName: String) {
        // Increase the matcher threshold to ensure that diffs in shadows are captured.
        val matcherThreshold = 0.998
        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(matcherThreshold))
    }
}

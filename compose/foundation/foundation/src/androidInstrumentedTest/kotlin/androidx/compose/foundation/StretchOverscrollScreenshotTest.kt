/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.ScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.testutils.AnimationDurationScaleRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screenshot test for stretch overscroll (S+) - we currently only run screenshot tests on API 33,
 * so it is not possible to screenshot test glow overscroll in this way (we would have
 * synchronization issues anyway, since glow recedes on its own, and doesn't follow animation
 * scale).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class StretchOverscrollScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_FOUNDATION)

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule = AnimationDurationScaleRule.create()

    private val overscrollTag = "overscrollTag"

    /** Pull right to stretch from the left */
    @Test
    fun stretch_left() {
        stretchAndAssertAgainstGolden(Offset(150f, 0f), screenshotRule, "overscroll_left")
    }

    /** Pull down to stretch from the top */
    @Test
    fun stretch_top() {
        stretchAndAssertAgainstGolden(Offset(0f, 150f), screenshotRule, "overscroll_top")
    }

    /** Pull left to stretch from the right */
    @Test
    fun stretch_right() {
        stretchAndAssertAgainstGolden(Offset(-150f, 0f), screenshotRule, "overscroll_right")
    }

    /** Pull up to stretch from the bottom */
    @Test
    fun stretch_bottom() {
        stretchAndAssertAgainstGolden(Offset(0f, -150f), screenshotRule, "overscroll_bottom")
    }

    @Composable
    private fun OverscrollContent(
        orientation: Orientation,
        overscrollEffect: OverscrollEffect,
        onDrawOverscroll: () -> Unit
    ) {
        Box(Modifier.fillMaxSize().wrapContentSize(Alignment.Center)) {
            Box(Modifier.testTag(overscrollTag).clipToBounds()) {
                if (orientation == Orientation.Vertical) {
                    Row {
                        Stripes(
                            orientation = Orientation.Vertical,
                            showText = false,
                            modifier = Modifier.border(1.dp, Color.Black)
                        )
                        Stripes(
                            orientation = Orientation.Vertical,
                            showText = true,
                            modifier =
                                Modifier.border(1.dp, Color.Black)
                                    .overscroll(overscrollEffect)
                                    .drawBehind { onDrawOverscroll() }
                        )
                        Stripes(
                            orientation = Orientation.Vertical,
                            showText = false,
                            modifier = Modifier.border(1.dp, Color.Black)
                        )
                    }
                } else {
                    Column {
                        Stripes(
                            orientation = Orientation.Horizontal,
                            showText = false,
                            modifier = Modifier.border(1.dp, Color.Black)
                        )
                        Stripes(
                            orientation = Orientation.Horizontal,
                            showText = true,
                            modifier =
                                Modifier.border(1.dp, Color.Black)
                                    .overscroll(overscrollEffect)
                                    .drawBehind { onDrawOverscroll() }
                        )
                        Stripes(
                            orientation = Orientation.Horizontal,
                            showText = false,
                            modifier = Modifier.border(1.dp, Color.Black)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Stripes(
        orientation: Orientation,
        showText: Boolean,
        modifier: Modifier = Modifier
    ) {
        val mainAxisSize = 100.dp
        val crossAxisSize = 50.dp
        Box {
            if (orientation == Orientation.Vertical) {
                Column(modifier.size(width = crossAxisSize, height = mainAxisSize)) {
                    repeat(10) {
                        Stripe(index = it, width = crossAxisSize, height = mainAxisSize / 10)
                    }
                }
                if (showText) {
                    Box(Modifier.matchParentSize().wrapContentSize(Alignment.Center)) {
                        BasicText(
                            "Stretch",
                            Modifier.rotate(90f),
                            style = TextStyle(color = Color.White)
                        )
                    }
                }
            } else {
                Row(modifier.size(width = mainAxisSize, height = crossAxisSize)) {
                    repeat(10) {
                        Stripe(index = it, width = mainAxisSize / 10, height = crossAxisSize)
                    }
                }
                if (showText) {
                    Box(Modifier.matchParentSize().wrapContentSize(Alignment.Center)) {
                        BasicText("Stretch", style = TextStyle(color = Color.White))
                    }
                }
            }
        }
    }

    @Composable
    private fun Stripe(index: Int, width: Dp, height: Dp) {
        val color = if (index % 2 == 0) Color.Red else Color.Blue
        Spacer(Modifier.size(width = width, height = height).background(color))
    }

    private fun stretchAndAssertAgainstGolden(
        offset: Offset,
        screenshotRule: ScreenshotTestRule,
        goldenIdentifier: String
    ) {
        animationScaleRule.setAnimationDurationScale(1f)
        // Due to b/302303969 there are no guarantees runOnIdle() will wait for drawing to happen,
        // so we need to manually synchronize drawing
        var hasDrawn = false
        lateinit var overscrollEffect: AndroidEdgeEffectOverscrollEffect
        require(offset.x == 0f || offset.y == 0f) { "Only one direction is supported" }
        require(offset != Offset.Zero) { "Offset must be non-zero" }
        val orientation = if (offset.y != 0f) Orientation.Vertical else Orientation.Horizontal
        rule.setContent {
            overscrollEffect = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
            OverscrollContent(orientation, overscrollEffect, onDrawOverscroll = { hasDrawn = true })
        }
        rule.waitUntil { hasDrawn }
        rule.runOnIdle {
            overscrollEffect.applyToScroll(offset, NestedScrollSource.UserInput) { Offset.Zero }
            // Disable further invalidations to avoid infinitely invalidating and causing us to
            // never become idle for test synchronization
            overscrollEffect.invalidationEnabled = false
            hasDrawn = false
        }
        rule.waitUntil { hasDrawn }
        rule
            .onNodeWithTag(overscrollTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier, MSSIMMatcher(1.0))
    }
}

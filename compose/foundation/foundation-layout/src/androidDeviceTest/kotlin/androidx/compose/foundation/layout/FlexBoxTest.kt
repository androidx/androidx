/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.layout

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.FlexBoxScopeInstance.flex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFlexBoxApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class FlexBoxTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    // Baseline Tests

    @Test
    fun alignItemsBaseline_FirstBaseline() {
        val yPositions = mutableListOf<Float>()
        val baseline1 = 10
        val baseline2 = 30
        val height = 40

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    config = {
                        direction(FlexDirection.Row)
                        alignItems(FlexAlignItems.Baseline)
                    }
                ) {
                    BaselineTestLayout(
                        width = 50.dp,
                        height = height.dp,
                        baseline = baseline1.dp,
                        horizontalLine = FirstBaseline,
                        modifier =
                            Modifier.onGloballyPositioned {
                                yPositions.add(0, it.positionInParent().y)
                            },
                    )
                    BaselineTestLayout(
                        width = 50.dp,
                        height = height.dp,
                        baseline = baseline2.dp,
                        horizontalLine = FirstBaseline,
                        modifier =
                            Modifier.onGloballyPositioned {
                                yPositions.add(1, it.positionInParent().y)
                            },
                    )
                }
            }
        }

        rule.waitForIdle()
        // Max ascent = 30. Item 1 shifts down by 20.
        Truth.assertThat(yPositions).containsExactly(20f, 0f).inOrder()
    }

    @Test
    fun alignItemsToBaseline_FirstBaseline() {
        val yPositions = mutableListOf<Float>()
        val baseline1 = 10
        val baseline2 = 30
        val height = 40

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    config = {
                        direction(FlexDirection.Row)
                        alignItems(TestHorizontalLine)
                    }
                ) {
                    BaselineTestLayout(
                        width = 50.dp,
                        height = height.dp,
                        baseline = baseline1.dp,
                        horizontalLine = TestHorizontalLine,
                        modifier =
                            Modifier.onGloballyPositioned {
                                yPositions.add(0, it.positionInParent().y)
                            },
                    )
                    BaselineTestLayout(
                        width = 50.dp,
                        height = height.dp,
                        baseline = baseline2.dp,
                        horizontalLine = TestHorizontalLine,
                        modifier =
                            Modifier.onGloballyPositioned {
                                yPositions.add(1, it.positionInParent().y)
                            },
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(yPositions).containsExactly(20f, 0f).inOrder()
    }

    @Test
    fun alignItemsToBaseline_lambda() {
        val yPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    config = {
                        direction(FlexDirection.Row)
                        alignItems { it.measuredHeight / 2 }
                    }
                ) {
                    Box(
                        Modifier.size(20.dp).onGloballyPositioned {
                            yPositions.add(0, it.positionInParent().y)
                        }
                    )
                    Box(
                        Modifier.size(40.dp).onGloballyPositioned {
                            yPositions.add(1, it.positionInParent().y)
                        }
                    )
                }
            }
        }

        rule.waitForIdle()
        // Baselines: 10, 20. Max ascent = 20.
        // Item 1: 20 - 10 = 10. Item 2: 20 - 20 = 0.
        Truth.assertThat(yPositions).containsExactly(10f, 0f).inOrder()
    }

    @Test
    fun alignSelfToBaseline() {
        val yPositions = mutableListOf<Float>()
        val baseline1 = 10
        val baseline2 = 30
        val height = 40

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    config = {
                        direction(FlexDirection.Row)
                        alignItems(FlexAlignItems.Start)
                    }
                ) {
                    BaselineTestLayout(
                        width = 50.dp,
                        height = height.dp,
                        baseline = baseline1.dp,
                        horizontalLine = TestHorizontalLine,
                        modifier =
                            Modifier.flex { alignSelf(TestHorizontalLine) }
                                .onGloballyPositioned { yPositions.add(0, it.positionInParent().y) },
                    )
                    BaselineTestLayout(
                        width = 50.dp,
                        height = height.dp,
                        baseline = baseline2.dp,
                        horizontalLine = TestHorizontalLine,
                        modifier =
                            Modifier.flex { alignSelf(TestHorizontalLine) }
                                .onGloballyPositioned { yPositions.add(1, it.positionInParent().y) },
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(yPositions).containsExactly(20f, 0f).inOrder()
    }

    @Test
    fun alignItemsToBaseline_Column_VerticalLine() {
        val xPositions = mutableListOf<Float>()
        val baseline1 = 10
        val baseline2 = 30
        val width = 40

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    config = {
                        direction(FlexDirection.Column)
                        alignItems(TestVerticalLine)
                    }
                ) {
                    BaselineTestLayout(
                        width = width.dp,
                        height = 50.dp,
                        baseline = baseline1.dp,
                        modifier =
                            Modifier.onGloballyPositioned {
                                xPositions.add(0, it.positionInParent().x)
                            },
                    )
                    BaselineTestLayout(
                        width = width.dp,
                        height = 50.dp,
                        baseline = baseline2.dp,
                        modifier =
                            Modifier.onGloballyPositioned {
                                xPositions.add(1, it.positionInParent().x)
                            },
                    )
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(xPositions).containsExactly(20f, 0f).inOrder()
    }

    // LayoutDirection (LTR / RTL) Tests

    @Test
    fun row_rtl_start_mirrorsToRight() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction(FlexDirection.Row)
                            justifyContent(FlexJustifyContent.Start)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(index, it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // RTL: Row starts from right edge
        Truth.assertThat(xPositions).containsExactly(180f, 160f, 140f).inOrder()
    }

    @Test
    fun rowReverse_rtl_start_doubleReversalFlowsLeftToRight() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction(FlexDirection.RowReverse)
                            justifyContent(FlexJustifyContent.Start)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(index, it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // RowReverse + RTL: double reversal, items flow left-to-right
        Truth.assertThat(xPositions).containsExactly(0f, 20f, 40f).inOrder()
    }

    @Test
    fun row_rtl_spaceBetween() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxWidth(),
                        config = {
                            direction(FlexDirection.Row)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(index, it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // RTL SpaceBetween: first item at right, last at left
        Truth.assertThat(xPositions).containsExactly(180f, 90f, 0f).inOrder()
    }

    @Test
    fun column_rtl_alignItemsStart_crossAxisFlips() {
        val xPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides NoOpDensity,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(FlexDirection.Column)
                            alignItems(FlexAlignItems.Start)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    xPositions.add(index, it.positionInParent().x)
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Column + RTL: main axis (Y) unaffected, cross axis Start = right edge
        Truth.assertThat(xPositions).containsExactly(180f, 180f, 180f)
    }

    // Validation Tests

    @SuppressLint
    @Test
    fun invalidFlexGrow_negative() {
        val negativeValueModifier = Modifier.flex { grow(-1f) }

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent { FlexBox { Box(negativeValueModifier) } }
        }
    }

    @SuppressLint
    @Test
    fun invalidFlexGrow_nan() {
        val nanValueModifier = Modifier.flex { grow(Float.NaN) }

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent { FlexBox { Box(nanValueModifier) } }
        }
    }

    @SuppressLint
    @Test
    fun invalidFlexShrink_negative() {
        val negativeValueModifier = Modifier.flex { shrink(-1f) }

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent { FlexBox { Box(negativeValueModifier) } }
        }
    }

    @SuppressLint
    @Test
    fun invalidFlexShrink_nan() {
        val nanValueModifier = Modifier.flex { shrink(Float.NaN) }

        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent { FlexBox { Box(nanValueModifier) } }
        }
    }

    @OptIn(ExperimentalFlexBoxApi::class)
    @Test
    fun testFlexBox_wrap_maxIntrinsicWidth_reportsSumOfChildren() {
        var width = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.width(IntrinsicSize.Max)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { width = it.width },
                        config = {
                            direction(FlexDirection.Row)
                            wrap(FlexWrap.Wrap)
                        },
                    ) {
                        Box(Modifier.size(20.dp))
                        Box(Modifier.size(30.dp))
                        Box(Modifier.size(40.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        // Max intrinsic width should be the sum of all children widths (20 + 30 + 40 = 90)
        Truth.assertThat(width).isEqualTo(90)
    }

    @OptIn(ExperimentalFlexBoxApi::class)
    @Test
    fun testFlexBox_wrap_minIntrinsicWidth_reportsMaxChildWidth() {
        var width = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.width(IntrinsicSize.Min)) {
                    FlexBox(
                        modifier = Modifier.onSizeChanged { width = it.width },
                        config = {
                            direction(FlexDirection.Row)
                            wrap(FlexWrap.Wrap)
                        },
                    ) {
                        Box(Modifier.size(20.dp))
                        Box(Modifier.size(30.dp))
                        Box(Modifier.size(40.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        // Min intrinsic width should be the width of the single widest child (40)
        Truth.assertThat(width).isEqualTo(40)
    }

    companion object {
        private val NoOpDensity =
            object : Density {
                override val density = 1f
                override val fontScale = 1f
            }
    }
}

private val TestHorizontalLine = HorizontalAlignmentLine(::min)
private val TestVerticalLine = VerticalAlignmentLine(::min)

@Composable
private fun BaselineTestLayout(
    width: Dp,
    height: Dp,
    baseline: Dp,
    modifier: Modifier = Modifier,
    horizontalLine: HorizontalAlignmentLine = TestHorizontalLine,
    content: @Composable () -> Unit = {},
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { _, constraints ->
            val widthPx = max(width.roundToPx(), constraints.minWidth)
            val heightPx = max(height.roundToPx(), constraints.minHeight)
            layout(
                widthPx,
                heightPx,
                mapOf(
                    horizontalLine to baseline.roundToPx(),
                    TestVerticalLine to baseline.roundToPx(),
                ),
            ) {}
        },
    )
}

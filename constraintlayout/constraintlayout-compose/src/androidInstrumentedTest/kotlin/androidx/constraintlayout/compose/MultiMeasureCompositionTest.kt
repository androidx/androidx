/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MultiMeasureLayout
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val HEIGHT_FROM_CONTENT = 40
private const val HEIGHT_FROM_CALLER = 80

/**
 * This class tests a couple of assumptions that ConstraintLayout & MotionLayout need to operate
 * properly.
 *
 * See [MaxWrapContentWithMultiMeasure].
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class MultiMeasureCompositionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testCustomMultiMeasure_changesFromCompositionSource(): Unit = with(rule.density) {
        var callerCompositionCount = 0
        var contentCompositionCount = 0

        val minWidth = 40

        // Mutable state that is only read in the content, will not directly recompose our
        // MultiMeasure Composable
        val widthMultiplier = mutableStateOf(4)
        val baseWidth = 10

        // Mutable state that is read at the same scope of our MultiMeasure Composable, will cause
        // it to recompose, but does not directly affect the content
        val unusedValue = mutableStateOf(0)
        rule.setContent {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.LightGray)
            ) {
                ++callerCompositionCount
                unusedValue.value
                MaxWrapContentWithMultiMeasure {
                    ++contentCompositionCount
                    // Box with variable width, depends on the multiplier value
                    Box(
                        modifier = Modifier
                            .width((widthMultiplier.value * baseWidth).toDp())
                            .background(Color.Red)
                            .testTag("box0")
                    )
                    // Box with constant width
                    Box(
                        Modifier
                            .width(minWidth.toDp())
                            .background(Color.Blue)
                            .testTag("box1")
                    )
                }
            }
        }
        rule.waitForIdle()

        // Assert the initial layout, composed from the root, so height is HEIGHT_FROM_CALLER
        rule.onNodeWithTag("box0").apply {
            assertPositionInRootIsEqualTo(0.dp, 0.dp)
            assertWidthIsEqualTo(minWidth.toDp())
            assertHeightIsEqualTo(HEIGHT_FROM_CALLER.toDp())
        }
        rule.onNodeWithTag("box1").apply {
            assertPositionInRootIsEqualTo(0.dp, HEIGHT_FROM_CALLER.toDp())
            assertWidthIsEqualTo(minWidth.toDp())
            assertHeightIsEqualTo(HEIGHT_FROM_CALLER.toDp())
        }

        rule.runOnIdle {
            // Increase multiplier, this will cause the layout to recompose from the content
            widthMultiplier.value = widthMultiplier.value + 1
        }
        rule.waitForIdle()

        // MaxWrapContentWithMultiMeasure assigns different height when recomposed from the content
        rule.onNodeWithTag("box0").apply {
            assertPositionInRootIsEqualTo(0.dp, 0.dp)
            assertWidthIsEqualTo(50.toDp()) // baseWidth * widthMultiplier.value
            assertHeightIsEqualTo(HEIGHT_FROM_CONTENT.toDp())
        }
        rule.onNodeWithTag("box1").apply {
            assertPositionInRootIsEqualTo(0.dp, HEIGHT_FROM_CONTENT.toDp())
            assertWidthIsEqualTo(50.toDp()) // baseWidth * widthMultiplier.value
            assertHeightIsEqualTo(HEIGHT_FROM_CONTENT.toDp())
        }

        rule.runOnIdle {
            // Decrease multiplier
            widthMultiplier.value = 3
        }
        rule.waitForIdle()

        // Verify layout is still correct
        rule.onNodeWithTag("box0").apply {
            assertPositionInRootIsEqualTo(0.dp, 0.dp)
            assertWidthIsEqualTo(minWidth.toDp())
            assertHeightIsEqualTo(HEIGHT_FROM_CONTENT.toDp())
        }
        rule.onNodeWithTag("box1").apply {
            assertPositionInRootIsEqualTo(0.dp, HEIGHT_FROM_CONTENT.toDp())
            assertWidthIsEqualTo(minWidth.toDp())
            assertHeightIsEqualTo(HEIGHT_FROM_CONTENT.toDp())
        }

        rule.runOnIdle {
            // This causes a recomposition from the caller of our Composable
            unusedValue.value = 1
        }
        rule.waitForIdle()

        // MaxWrapContentWithMultiMeasure assigns different height when recomposed from the Caller
        rule.onNodeWithTag("box0").apply {
            assertPositionInRootIsEqualTo(0.dp, 0.dp)
            assertWidthIsEqualTo(minWidth.toDp())
            assertHeightIsEqualTo(HEIGHT_FROM_CALLER.toDp())
        }
        rule.onNodeWithTag("box1").apply {
            assertPositionInRootIsEqualTo(0.dp, HEIGHT_FROM_CALLER.toDp())
            assertWidthIsEqualTo(minWidth.toDp())
            assertHeightIsEqualTo(HEIGHT_FROM_CALLER.toDp())
        }

        rule.runOnIdle {
            assertEquals(2, callerCompositionCount)
            assertEquals(4, contentCompositionCount)
        }
    }

    /**
     * Column-like layout that assigns the max WrapContent width to all its children.
     *
     * Note that the height assigned height will depend on where the recomposition started: 40px if it
     * started from the content, 80px if it started from the Composable caller.
     */
    @Composable
    inline fun MaxWrapContentWithMultiMeasure(
        modifier: Modifier = Modifier,
        crossinline content: @Composable () -> Unit
    ) {
        val compTracker = remember { mutableStateOf(Unit, neverEqualPolicy()) }
        val compSource =
            remember { Ref<CompositionSource>().apply { value = CompositionSource.Unknown } }
        compSource.value = CompositionSource.Caller

        @Suppress("DEPRECATION")
        MultiMeasureLayout(
            modifier = modifier,
            measurePolicy = maxWidthPolicy(compTracker, compSource),
            content = {
                // Reassign the mutable state, so that readers recompose with the content
                compTracker.value = Unit

                if (compSource.value == CompositionSource.Unknown) {
                    compSource.value = CompositionSource.Content
                }
                content()
            }
        )
    }

    fun maxWidthPolicy(
        compTracker: State<Unit>,
        compSource: Ref<CompositionSource>
    ): MeasurePolicy =
        MeasurePolicy { measurables, constraints ->
            // This state read will force the MeasurePolicy to re-run whenever the content
            // recomposes, even if our Composable didn't
            compTracker.value

            val height = when (compSource.value) {
                CompositionSource.Content -> HEIGHT_FROM_CONTENT
                CompositionSource.Caller -> HEIGHT_FROM_CALLER
                CompositionSource.Unknown,
                null -> 0
            }

            compSource.value = CompositionSource.Unknown

            // Find the max WrapContent width
            val maxWrapWidth = measurables.map {
                it.measure(constraints.copy(minWidth = 0, minHeight = height))
            }.maxOf {
                it.width
            }

            // Remeasure, assign the maxWrapWidth to every child
            val placeables = measurables.map {
                it.measure(constraints.copy(minWidth = maxWrapWidth, minHeight = height))
            }

            // Wrap the layout height to the content in a column
            var layoutHeight = 0
            placeables.forEach { layoutHeight += it.height }

            // Position the children.
            layout(maxWrapWidth, layoutHeight) {
                var y = 0
                placeables.forEach { placeable ->
                    // Position left-aligned, one after another
                    placeable.place(x = 0, y = y)
                    y += placeable.height
                }
            }
        }

    enum class CompositionSource {
        Unknown,
        Caller,
        Content
    }
}

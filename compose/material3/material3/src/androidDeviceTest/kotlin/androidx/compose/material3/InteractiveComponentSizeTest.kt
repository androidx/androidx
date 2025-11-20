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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class InteractiveComponentSizeTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun minimumInteractiveComponentSize_applyWhenSmallerThanMinSize() {
        val testTag = "box"
        var minTouchTargetSize: DpSize = DpSize.Unspecified
        rule.setContent {
            minTouchTargetSize = LocalViewConfiguration.current.minimumTouchTargetSize
            Box(Modifier.testTag(testTag).minimumInteractiveComponentSize().size(24.dp))
        }

        rule
            .onNodeWithTag(testTag)
            .assertWidthIsEqualTo(minTouchTargetSize.width)
            .assertWidthIsEqualTo(minTouchTargetSize.height)
    }

    @Test
    fun minimumInteractiveComponentSize_sizeRemainsWhenLargerThanMinSize() {
        val testTag = "box"
        var targetSize: DpSize = DpSize.Unspecified
        rule.setContent {
            val minTouchTargetSize = LocalViewConfiguration.current.minimumTouchTargetSize
            targetSize = minTouchTargetSize * 2
            Box(Modifier.testTag(testTag).minimumInteractiveComponentSize().size(targetSize))
        }

        rule
            .onNodeWithTag(testTag)
            .assertWidthIsEqualTo(targetSize.width)
            .assertWidthIsEqualTo(targetSize.height)
    }

    @Test
    fun minimumInteractiveComponentSize_alignmentLines_whenContentIsSmaller() {
        val latch = CountDownLatch(1)
        val contentSize = 24.dp

        rule.setContent {
            val minSize = LocalMinimumInteractiveComponentSize.current
            val expectedLineValue =
                with(LocalDensity.current) {
                    ((minSize.toPx() - contentSize.toPx()) / 2f).roundToInt()
                }

            Layout(
                content = { Box(Modifier.minimumInteractiveComponentSize().size(contentSize)) }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)

                val topValue = placeable[MinimumInteractiveTopAlignmentLine]
                assertThat(topValue).isNotNull()
                assertThat(topValue).isEqualTo(expectedLineValue)

                val leftValue = placeable[MinimumInteractiveLeftAlignmentLine]
                assertThat(leftValue).isNotNull()
                assertThat(leftValue).isEqualTo(expectedLineValue)

                latch.countDown()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        Assert.assertTrue("Test timed out", latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun minimumInteractiveComponentSize_alignmentLines_whenContentIsLarger() {
        val latch = CountDownLatch(1)
        rule.setContent {
            // Have the content size larger than the default min size.
            val contentSize = LocalMinimumInteractiveComponentSize.current + 8.dp
            Layout(
                content = { Box(Modifier.minimumInteractiveComponentSize().size(contentSize)) }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)

                // When the component is larger, the extra space is 0, so the alignment line value
                // should be 0.
                val topValue = placeable[MinimumInteractiveTopAlignmentLine]
                assertThat(topValue).isNotNull()
                assertThat(topValue).isEqualTo(0)

                val leftValue = placeable[MinimumInteractiveLeftAlignmentLine]
                assertThat(leftValue).isNotNull()
                assertThat(leftValue).isEqualTo(0)

                latch.countDown()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        Assert.assertTrue("Test timed out", latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun minimumInteractiveComponentSize_alignmentLines_whenOnlyHeightIsSmaller() {
        val latch = CountDownLatch(1)

        rule.setContent {
            val minSize = LocalMinimumInteractiveComponentSize.current
            val contentWidth = minSize + 10.dp // larger than min size
            val contentHeight = minSize - 10.dp // smaller than min size

            val expectedTopLineValue =
                with(LocalDensity.current) {
                    ((minSize.toPx() - contentHeight.toPx()) / 2f).roundToInt()
                }

            Layout(
                content = {
                    Box(
                        Modifier.minimumInteractiveComponentSize().size(contentWidth, contentHeight)
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)

                val topValue = placeable[MinimumInteractiveTopAlignmentLine]
                assertThat(topValue).isNotNull()
                assertThat(topValue).isEqualTo(expectedTopLineValue)

                // Width is larger, so no horizontal space is added.
                val leftValue = placeable[MinimumInteractiveLeftAlignmentLine]
                assertThat(leftValue).isNotNull()
                assertThat(leftValue).isEqualTo(0)

                latch.countDown()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        Assert.assertTrue("Test timed out", latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun minimumInteractiveComponentSize_alignmentLines_whenOnlyWidthIsSmaller() {
        val latch = CountDownLatch(1)

        rule.setContent {
            val minSize = LocalMinimumInteractiveComponentSize.current
            val contentWidth = minSize - 10.dp // smaller than min size
            val contentHeight = minSize + 10.dp // larger than min size

            val expectedLeftLineValue =
                with(LocalDensity.current) {
                    ((minSize.toPx() - contentWidth.toPx()) / 2f).roundToInt()
                }

            Layout(
                content = {
                    Box(
                        Modifier.minimumInteractiveComponentSize().size(contentWidth, contentHeight)
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)

                // Height is larger, so no vertical space is added.
                val topValue = placeable[MinimumInteractiveTopAlignmentLine]
                assertThat(topValue).isNotNull()
                assertThat(topValue).isEqualTo(0)

                val leftValue = placeable[MinimumInteractiveLeftAlignmentLine]
                assertThat(leftValue).isNotNull()
                assertThat(leftValue).isEqualTo(expectedLeftLineValue)

                latch.countDown()
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        Assert.assertTrue("Test timed out", latch.await(1, TimeUnit.SECONDS))
    }
}

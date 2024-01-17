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

package androidx.tv.foundation.lazy.list

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.grid.keyPress
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyNestedScrollingTest {
    private val LazyTag = "LazyTag"

    @get:Rule
    val rule = createComposeRule()

    private val expectedDragOffset = 20f
    private val dragOffsetWithTouchSlop = expectedDragOffset + TestTouchSlop

    @Test
    fun column_nestedScrollingBackwardInitially() = runBlocking {
        val items = (1..3).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Vertical,
                    state = scrollable
                )
            ) {
                TvLazyColumn(
                    Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Box(Modifier.requiredSize(50.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = 100f + TestTouchSlop))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(100f)
        }
    }

    @Test
    fun column_nestedScrollingBackwardOnceWeScrolledForwardPreviously() = runBlocking {
        val items = (1..3).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Vertical,
                    state = scrollable
                )
            ) {
                TvLazyColumn(
                    modifier = Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Box(Modifier.requiredSize(50.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        // scroll forward
        rule.keyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN, 2)

        // scroll back so we again on 0 position
        // we scroll one extra dp to prevent rounding issues
        rule.keyPress(NativeKeyEvent.KEYCODE_DPAD_UP, 2)

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = dragOffsetWithTouchSlop))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(expectedDragOffset)
        }
    }

    @Test
    fun column_nestedScrollingForwardWhenTheFullContentIsInitiallyVisible() = runBlocking {
        val items = (1..2).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Vertical,
                    state = scrollable
                )
            ) {
                TvLazyColumn(
                    Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Box(Modifier.requiredSize(40.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = -dragOffsetWithTouchSlop))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }

    @Test
    fun column_nestedScrollingForwardWhenScrolledToTheEnd() = runBlocking {
        val items = (1..3).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Vertical,
                    state = scrollable
                )
            ) {
                TvLazyColumn(
                    modifier = Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Box(Modifier.requiredSize(50.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        // scroll till the end
        rule.keyPress(NativeKeyEvent.KEYCODE_DPAD_DOWN, 3)

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = -dragOffsetWithTouchSlop))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingBackwardInitially() = runBlocking {
        val items = (1..3).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Horizontal,
                    state = scrollable
                )
            ) {
                TvLazyRow(
                    modifier = Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Spacer(Modifier.requiredSize(50.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = dragOffsetWithTouchSlop, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingBackwardOnceWeScrolledForwardPreviously() = runBlocking {
        val items = (1..3).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Horizontal,
                    state = scrollable
                )
            ) {
                TvLazyRow(
                    modifier = Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Spacer(Modifier.requiredSize(50.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        // scroll forward
        rule.keyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT, 2)

        // scroll back so we again on 0 position
        // we scroll one extra dp to prevent rounding issues
        rule.keyPress(NativeKeyEvent.KEYCODE_DPAD_LEFT, 2)

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = dragOffsetWithTouchSlop, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingForwardWhenTheFullContentIsInitiallyVisible() = runBlocking {
        val items = (1..2).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Horizontal,
                    state = scrollable
                )
            ) {
                TvLazyRow(
                    modifier = Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Spacer(Modifier.requiredSize(40.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = -dragOffsetWithTouchSlop, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingForwardWhenScrolledToTheEnd() = runBlocking {
        val items = (1..3).toList()
        var draggedOffset = 0f
        val scrollable = ScrollableState {
            draggedOffset += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Horizontal,
                    state = scrollable
                )
            ) {
                TvLazyRow(
                    modifier = Modifier.requiredSize(100.dp).testTag(LazyTag),
                    pivotOffsets = PivotOffsets(parentFraction = 0f)
                ) {
                    items(items) {
                        Spacer(Modifier.requiredSize(50.dp).testTag("$it").focusable())
                    }
                }
            }
        }

        // scroll till the end
        rule.keyPress(NativeKeyEvent.KEYCODE_DPAD_RIGHT, 3)

        rule.onNodeWithTag(LazyTag)
            .performTouchInput {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = -dragOffsetWithTouchSlop, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }
}

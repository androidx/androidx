/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
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
                LazyColumn(
                    Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(50.dp)
                                .testTag("$it"))
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
                LazyColumn(
                    Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(50.dp)
                                .testTag("$it"))
                    }
                }
            }
        }

        // scroll forward
        rule.onNodeWithTag(LazyTag)
            .scrollBy(y = 20.dp, density = rule.density)

        // scroll back so we again on 0 position
        // we scroll one extra dp to prevent rounding issues
        rule.onNodeWithTag(LazyTag)
            .scrollBy(y = -(21.dp), density = rule.density)

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
                LazyColumn(
                    Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(40.dp)
                                .testTag("$it"))
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
                LazyColumn(
                    Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(50.dp)
                                .testTag("$it"))
                    }
                }
            }
        }

        // scroll till the end
        rule.onNodeWithTag(LazyTag)
            .scrollBy(y = 55.dp, density = rule.density)

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
                LazyRow(
                    modifier = Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)
                ) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(50.dp)
                                .testTag("$it"))
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
                LazyRow(
                    modifier = Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)
                ) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(50.dp)
                                .testTag("$it"))
                    }
                }
            }
        }

        // scroll forward
        rule.onNodeWithTag(LazyTag)
            .scrollBy(x = 20.dp, density = rule.density)

        // scroll back so we again on 0 position
        // we scroll one extra dp to prevent rounding issues
        rule.onNodeWithTag(LazyTag)
            .scrollBy(x = -(21.dp), density = rule.density)

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
                LazyRow(
                    modifier = Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)
                ) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(40.dp)
                                .testTag("$it"))
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
                LazyRow(
                    modifier = Modifier
                        .requiredSize(100.dp)
                        .testTag(LazyTag)
                ) {
                    items(items) {
                        Spacer(
                            Modifier
                                .requiredSize(50.dp)
                                .testTag("$it"))
                    }
                }
            }
        }

        // scroll till the end
        rule.onNodeWithTag(LazyTag)
            .scrollBy(x = 55.dp, density = rule.density)

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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun mouseScrollInLazyRow_nestedLazyRowInLazyColumn_scrollsVertically() = runBlocking {
        val items = (1..50).toList()
        var total = 0f
        val boxTab = "boxTab"

        val scrollable = ScrollableState {
            total += it
            it
        }
        rule.setContentWithTestViewConfiguration {
            Box(
                Modifier.scrollable(
                    orientation = Orientation.Vertical,
                    state = scrollable
                ).testTag(boxTab)
            ) {
                LazyColumn(
                    modifier = Modifier.requiredSize(200.dp)
                ) {
                    item {
                        LazyRow(
                            modifier = Modifier
                                .requiredSize(100.dp)
                                .testTag(LazyTag)
                        ) {
                            items(items) {
                                Spacer(Modifier.requiredSize(50.dp))
                            }
                        }
                    }
                    items(items) {
                        Spacer(Modifier.requiredSize(50.dp))
                    }
                }
            }
        }

        rule.onNodeWithTag(LazyTag).performMouseInput {
            this.scroll(100f)
        }

        rule.runOnIdle {
            // Mouse scroll is opposite, so we test is it less than zero (negative number).
            Truth.assertThat(total).isLessThan(0)
        }

        rule.onNodeWithTag(boxTab).performMouseInput {
            this.scroll(-100f)
        }

        rule.runOnIdle {
            Truth.assertThat(total).isLessThan(0.01f)
        }
    }
}

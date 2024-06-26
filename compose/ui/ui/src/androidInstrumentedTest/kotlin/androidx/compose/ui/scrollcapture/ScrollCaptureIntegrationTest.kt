/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.scrollcapture

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests real Foundation scrollable components' integration with scroll capture. */
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ScrollCaptureIntegrationTest {

    @get:Rule val rule = createComposeRule()

    private val captureTester = ScrollCaptureTester(rule)

    @Test
    fun search_finds_verticalScrollModifier() =
        captureTester.runTest {
            captureTester.setContent {
                with(LocalDensity.current) {
                    Box(Modifier.size(10.toDp()).verticalScroll(rememberScrollState())) {
                        Box(Modifier.size(100.toDp()))
                    }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect.width()).isEqualTo(10)
            assertThat(target.localVisibleRect.height()).isEqualTo(10)
        }

    @Test
    fun search_doesNotFind_horizontalScrollModifier() =
        captureTester.runTest {
            captureTester.setContent {
                with(LocalDensity.current) {
                    Box(Modifier.size(10.toDp()).horizontalScroll(rememberScrollState())) {
                        Box(Modifier.size(100.toDp()))
                    }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_finds_LazyColumn() =
        captureTester.runTest {
            captureTester.setContent {
                with(LocalDensity.current) {
                    LazyColumn(Modifier.size(10.toDp())) { item { Box(Modifier.size(100.toDp())) } }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect.width()).isEqualTo(10)
            assertThat(target.localVisibleRect.height()).isEqualTo(10)
        }

    @Test
    fun search_doesNotFind_LazyRow() =
        captureTester.runTest {
            captureTester.setContent {
                with(LocalDensity.current) {
                    LazyRow(Modifier.size(10.toDp())) { item { Box(Modifier.size(100.toDp())) } }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).isEmpty()
        }

    @Test
    fun search_finds_LazyVerticalGrid() =
        captureTester.runTest {
            captureTester.setContent {
                with(LocalDensity.current) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier.size(10.toDp())
                    ) {
                        item { Box(Modifier.size(100.toDp())) }
                    }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect.width()).isEqualTo(10)
            assertThat(target.localVisibleRect.height()).isEqualTo(10)
        }

    @Test
    fun search_finds_LazyVerticalStaggeredGrid() =
        captureTester.runTest {
            captureTester.setContent {
                with(LocalDensity.current) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(1),
                        modifier = Modifier.size(10.toDp())
                    ) {
                        item { Box(Modifier.size(100.toDp())) }
                    }
                }
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets).hasSize(1)
            val target = targets.single()
            assertThat(target.localVisibleRect.width()).isEqualTo(10)
            assertThat(target.localVisibleRect.height()).isEqualTo(10)
        }

    @Test
    fun search_doesNotFind_TextField1_singleLine() =
        captureTester.runTest {
            captureTester.setContent {
                BasicTextField(
                    "really long value to ensure that the field will scroll horizontally",
                    onValueChange = {},
                    singleLine = true,
                    modifier = Modifier.width(5.dp),
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets.isEmpty())
        }

    @Test
    fun search_doesNotFind_TextField1_multiLine_scrollable() =
        captureTester.runTest {
            captureTester.setContent {
                BasicTextField(
                    "lots\n\nof\n\nnewlines\n\nto\n\nmake\n\nvertically\n\nscrollable",
                    onValueChange = {},
                    singleLine = false,
                    maxLines = 2
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets.isEmpty())
        }

    @Test
    fun search_doesNotFind_TextField2_singleLine() =
        captureTester.runTest {
            val state =
                TextFieldState(
                    "really long value to ensure that the field will scroll horizontally"
                )
            captureTester.setContent {
                BasicTextField(
                    state,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.width(5.dp),
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets.isEmpty())
        }

    @Test
    fun search_doesNotFind_TextField2_multiLine_scrollable() =
        captureTester.runTest {
            val state =
                TextFieldState("lots\n\nof\n\nnewlines\n\nto\n\nmake\n\nvertically\n\nscrollable")
            captureTester.setContent {
                BasicTextField(
                    state,
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 2)
                )
            }

            val targets = captureTester.findCaptureTargets()
            assertThat(targets.isEmpty())
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun capture_LazyColumn_stickyHeadersDisabled_byLayout() =
        captureTester.runTest {
            val headerHeight = 5
            val state = LazyListState()
            var firstHeaderCoords by mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy())
            var firstItemCoords by mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy())

            fun assertHeaderNotStuck() {
                val headerBounds =
                    firstHeaderCoords?.takeIf { it.isAttached }?.boundsInParent() ?: return
                val itemBounds =
                    firstItemCoords?.takeIf { it.isAttached }?.boundsInParent() ?: return
                assertThat(headerBounds.bottom).isEqualTo(itemBounds.top)
            }

            captureTester.setContent {
                with(LocalDensity.current) {
                    LazyColumn(state = state, modifier = Modifier.size(10.toDp())) {
                        stickyHeader {
                            Box(
                                Modifier.background(Color.Red)
                                    .fillMaxWidth()
                                    .height(headerHeight.toDp())
                                    .onGloballyPositioned { firstHeaderCoords = it }
                            )
                            DisposableEffect(Unit) { onDispose { firstHeaderCoords = null } }
                        }
                        item {
                            Box(
                                Modifier.background(Color.Green)
                                    .size(10.toDp())
                                    .onGloballyPositioned { firstItemCoords = it }
                            )
                            DisposableEffect(Unit) { onDispose { firstItemCoords = null } }
                        }

                        stickyHeader {
                            Box(
                                Modifier.background(Color.Red)
                                    .fillMaxWidth()
                                    .height(headerHeight.toDp())
                            )
                        }
                        item { Box(Modifier.background(Color.Green).size(10.toDp())) }
                    }
                }
            }

            // Sticky headers render correctly trivially when starting from the top, so we need to
            // start a bit down the list.
            rule.awaitIdle()
            val scrolled = state.scrollBy(100f)

            val target = captureTester.findCaptureTargets().single()
            captureTester.capture(target, captureWindowHeight = 1) {
                repeat(scrolled.toInt()) {
                    shiftWindowBy(-1)
                    performCaptureDiscardingBitmap()
                    rule.awaitIdle()
                    assertHeaderNotStuck()
                }
            }
        }
}

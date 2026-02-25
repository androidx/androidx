/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalGridApi::class)

package androidx.compose.foundation.layout

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class GridTest : LayoutTest() {

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testGrid_itemModifierChange_triggersRelayout() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            var targetRow by mutableStateOf(1)

            // Latch for initial layout (Row 1)
            val initialLatch = CountDownLatch(1)
            // Latch for update layout (Row 2)
            val updateLatch = CountDownLatch(1)

            val childPosition = Ref<Offset>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    Box(
                        Modifier.gridItem(row = targetRow, column = 1)
                            .size(sizeDp)
                            .onGloballyPositioned { coordinates ->
                                childPosition.value = coordinates.localToRoot(Offset.Zero)
                                if (initialLatch.count > 0) {
                                    initialLatch.countDown()
                                } else {
                                    updateLatch.countDown()
                                }
                            }
                    )
                }
            }

            // 1. Verify Initial Position (Row 1 -> Index 0 -> 0px)
            assertTrue(
                "Timed out waiting for initial layout",
                initialLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(Offset(0f, 0f), childPosition.value)

            // 2. Update State
            targetRow = 2

            // 3. Verify Updated Position (Row 2 -> Index 1 -> 50px)
            assertTrue(
                "Timed out waiting for layout update",
                updateLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(Offset(0f, size.toFloat()), childPosition.value)
        }

    @Test
    fun testGrid_configStateChange_updatesLayout() =
        with(density) {
            var trackSize by mutableStateOf(50.dp)

            // Use separate latches for the initial pass and the update pass.
            val initialLayoutLatch = CountDownLatch(1)
            val updateLayoutLatch = CountDownLatch(1)

            val childSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // Reading 'trackSize' here registers a dependency.
                        // When 'trackSize' changes, this lambda re-executes, triggering
                        // the MeasurePolicy to re-measure.
                        column(GridTrackSize.Fixed(trackSize))
                        row(GridTrackSize.Fixed(trackSize))
                    }
                ) {
                    Box(
                        Modifier.gridItem(1, 1).fillMaxSize().onGloballyPositioned { coordinates ->
                            childSize.value = coordinates.size

                            // If the first latch is still open, this is the initial layout.
                            // Otherwise, we are in the update phase.
                            if (initialLayoutLatch.count > 0) {
                                initialLayoutLatch.countDown()
                            } else {
                                updateLayoutLatch.countDown()
                            }
                        }
                    )
                }
            }

            // Verify Initial State (50.dp)
            // Wait for the FIRST layout pass to complete
            assertTrue(
                "Timed out waiting for initial layout",
                initialLayoutLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(
                "Initial size incorrect",
                IntSize(50.dp.roundToPx(), 50.dp.roundToPx()),
                childSize.value,
            )

            // Change State
            trackSize = 100.dp

            // Verify Update (100.dp)
            // Wait for the SECOND layout pass (recomposition) to complete
            assertTrue(
                "Timed out waiting for layout update",
                updateLayoutLatch.await(1, TimeUnit.SECONDS),
            )
            assertEquals(
                "Grid did not update track size after config state changed",
                IntSize(100.dp.roundToPx(), 100.dp.roundToPx()),
                childSize.value,
            )
        }

    @Test
    fun testGrid_fixedTracks_sizeCorrectly() =
        with(density) {
            val size1 = 50
            val size2 = 100
            val size1Dp = size1.toDp()
            val size2Dp = size2.toDp()

            val positionedLatch = CountDownLatch(2)
            val childSize = Array(2) { Ref<IntSize>() }
            val childPosition = Array(2) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size1Dp))
                        column(GridTrackSize.Fixed(size2Dp))
                        row(GridTrackSize.Fixed(size1Dp))
                    }
                ) {
                    // R1, C1
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    // R1, C2
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // Check Item 1 (50x50) at (0,0)
            assertEquals(IntSize(size1, size1), childSize[0].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)

            // Check Item 2 (100x50) at (50,0)
            assertEquals(IntSize(size2, size1), childSize[1].value)
            assertEquals(Offset(size1.toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_fractionTracks() =
        with(density) {
            val totalSize = 200
            val totalSizeDp = totalSize.toDp()

            // Col 1: 25% = 50px
            // Col 2: 75% = 150px
            val expectedCol1 = (totalSize * 0.25f).roundToInt()
            val expectedCol2 = (totalSize * 0.75f).roundToInt()

            val positionedLatch = CountDownLatch(2)
            val childSize = Array(2) { Ref<IntSize>() }
            val childPosition = Array(2) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Percentage(0.25f))
                        column(GridTrackSize.Percentage(0.75f))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.size(totalSizeDp, 50.dp),
                ) {
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(IntSize(expectedCol1, 50.dp.roundToPx()), childSize[0].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)

            assertEquals(IntSize(expectedCol2, 50.dp.roundToPx()), childSize[1].value)
            assertEquals(Offset(expectedCol1.toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_percentageRows_resolvesAgainstHeight() =
        with(density) {
            // Scenario:
            // Fixed Height Container (200px).
            // Row 1: 25% (50px)
            // Row 2: 75% (150px)

            val totalHeight = 200
            val expectedRow1 = (totalHeight * 0.25f).roundToInt()
            val expectedRow2 = (totalHeight * 0.75f).roundToInt()

            val latch = CountDownLatch(2)
            val sizes = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(50.dp))
                        row(GridTrackSize.Percentage(0.25f))
                        row(GridTrackSize.Percentage(0.75f))
                    },
                    modifier = Modifier.height(totalHeight.toDp()),
                ) {
                    // Item in Row 1
                    Box(
                        Modifier.gridItem(1, 1).fillMaxSize().saveLayoutInfo(sizes[0], Ref(), latch)
                    )
                    // Item in Row 2
                    Box(
                        Modifier.gridItem(2, 1).fillMaxSize().saveLayoutInfo(sizes[1], Ref(), latch)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(expectedRow1, sizes[0].value?.height)
            assertEquals(expectedRow2, sizes[1].value?.height)
        }

    @Test
    fun testGrid_flexTracks() =
        with(density) {
            val totalWidth = 300
            val fixedWidth = 100

            // Remaining space = 200
            // Flex 1: 1fr = 200 * (1/4) = 50
            // Flex 2: 3fr = 200 * (3/4) = 150

            val positionedLatch = CountDownLatch(3)
            val childSize = Array(3) { Ref<IntSize>() }
            val childPosition = Array(3) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(fixedWidth.toDp()))
                        column(GridTrackSize.Flex(1.fr))
                        column(GridTrackSize.Flex(3.fr))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.width(totalWidth.toDp()),
                ) {
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 3)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[2], childPosition[2], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // Fixed Col
            assertEquals(IntSize(fixedWidth, 50.dp.roundToPx()), childSize[0].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)

            // Flex 1 (50px)
            assertEquals(IntSize(50, 50.dp.roundToPx()), childSize[1].value)
            assertEquals(Offset(fixedWidth.toFloat(), 0f), childPosition[1].value)

            // Flex 3 (150px)
            assertEquals(IntSize(150, 50.dp.roundToPx()), childSize[2].value)
            assertEquals(Offset((fixedWidth + 50).toFloat(), 0f), childPosition[2].value)
        }

    @Test
    fun testGrid_flexRespectsMinContent() =
        with(density) {
            val minContentSize = 100
            val totalSize = 150 // Only 50px left for flex
            val latch = CountDownLatch(2)
            val sizes = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.MinContent)
                        column(GridTrackSize.Flex(1.fr))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.width(totalSize.toDp()),
                ) {
                    // Item 1 (MinContent): Needs 100px
                    IntrinsicItem(
                        minWidth = minContentSize,
                        minIntrinsicWidth = minContentSize,
                        maxIntrinsicWidth = minContentSize,
                        modifier = Modifier.gridItem(1, 1).saveLayoutInfo(sizes[0], Ref(), latch),
                    )
                    // Item 2 (Flex): Gets remaining 50px
                    Box(
                        Modifier.gridItem(1, 2).fillMaxSize().saveLayoutInfo(sizes[1], Ref(), latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(minContentSize, sizes[0].value?.width)
            assertEquals(totalSize - minContentSize, sizes[1].value?.width)
        }

    @Test
    fun testGrid_flexTrack_minContent_lowerBound() =
        with(density) {
            val containerWidth = 50
            val itemMinSize = 100
            val latch = CountDownLatch(1)
            val childSize = Ref<IntSize>()

            show {
                // Container is 50px wide, but item needs 100px.
                // Flex track should expand to 100px (min-content), ignoring the 50px constraint.
                Box(Modifier.width(containerWidth.toDp())) {
                    Grid(
                        config = {
                            column(GridTrackSize.Flex(1.fr))
                            row(GridTrackSize.Fixed(50.dp))
                        }
                    ) {
                        IntrinsicItem(
                            minWidth = itemMinSize,
                            minIntrinsicWidth = itemMinSize,
                            maxIntrinsicWidth = itemMinSize,
                            modifier =
                                Modifier.gridItem(1, 1)
                                    .fillMaxHeight()
                                    .saveLayoutInfo(childSize, Ref(), latch),
                        )
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Flex track should not shrink below min-content size",
                itemMinSize,
                childSize.value?.width,
            )
        }

    @Test
    fun testGrid_flexTracks_distributeSpace_afterMinContent() =
        with(density) {
            // Scenario:
            // Container = 200px
            // Track 1 (1fr): Has large content (150px)
            // Track 2 (1fr): Has small content (10px)
            //
            // Calculation:
            // 1. Base Sizes (Pass 1):
            //    Track 1 = 150px
            //    Track 2 = 10px
            //    Used = 160px
            //
            // 2. Remaining Space (Pass 2):
            //    200px - 160px = 40px
            //
            // 3. Distribution (Equal weight 1fr):
            //    Track 1 adds 20px -> Final 170px
            //    Track 2 adds 20px -> Final 30px

            val containerWidth = 200
            val item1MinSize = 150
            val item2MinSize = 10
            val latch = CountDownLatch(2)
            val size1 = Ref<IntSize>()
            val size2 = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Flex(1.fr))
                        column(GridTrackSize.Flex(1.fr))
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.width(containerWidth.toDp()),
                ) {
                    // Item 1: Large Min Content
                    IntrinsicItem(
                        minWidth = item1MinSize,
                        minIntrinsicWidth = item1MinSize,
                        maxIntrinsicWidth = item1MinSize,
                        modifier =
                            Modifier.gridItem(1, 1)
                                .fillMaxWidth() // <--- ADD THIS
                                .saveLayoutInfo(size1, Ref(), latch),
                    )

                    // Item 2: Small Min Content
                    IntrinsicItem(
                        minWidth = item2MinSize,
                        minIntrinsicWidth = item2MinSize,
                        maxIntrinsicWidth = item2MinSize,
                        modifier =
                            Modifier.gridItem(1, 2)
                                .fillMaxWidth() // <--- ADD THIS
                                .saveLayoutInfo(size2, Ref(), latch),
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals("Track 1 should be Base(150) + Share(20)", 170, size1.value?.width)
            assertEquals("Track 2 should be Base(10) + Share(20)", 30, size2.value?.width)
        }

    @Test
    fun testGrid_mixedTrackTypes_resolutionOrder() =
        with(density) {
            val fixedSize = 50
            val contentSize = 30
            val totalSize = 200 // 50 (Fixed) + 30 (Auto) + Flex = 200 -> Flex = 120

            val latch = CountDownLatch(3)
            val sizes = Array(3) { Ref<IntSize>() }
            val positions = Array(3) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(fixedSize.toDp())) // Col 1: Fixed
                        column(GridTrackSize.Auto) // Col 2: Auto (Content based)
                        column(GridTrackSize.Flex(1.fr)) // Col 3: Flex (Remaining)
                        row(GridTrackSize.Fixed(50.dp))
                    },
                    modifier = Modifier.width(totalSize.toDp()),
                ) {
                    // Col 1: Fixed item
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(sizes[0], positions[0], latch)
                    )

                    // Col 2: Auto item (determines track width)
                    Box(
                        Modifier.gridItem(1, 2)
                            .width(contentSize.toDp())
                            .fillMaxHeight()
                            .saveLayoutInfo(sizes[1], positions[1], latch)
                    )

                    // Col 3: Flex item
                    Box(
                        Modifier.gridItem(1, 3)
                            .fillMaxSize()
                            .saveLayoutInfo(sizes[2], positions[2], latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Col 1: 50
            assertEquals(fixedSize, sizes[0].value?.width)
            // Col 2: 30 (sized by content)
            assertEquals(contentSize, sizes[1].value?.width)
            // Col 3: 200 - 50 - 30 = 120
            assertEquals(120, sizes[2].value?.width)
        }

    @Test
    fun testGrid_gaps() =
        with(density) {
            val size = 50
            val gap = 10
            val gapDp = gap.toDp()
            val sizeDp = size.toDp()

            val positionedLatch = CountDownLatch(2)
            val childPosition = Array(2) { Ref<Offset>() }
            // Use explicit dummy refs instead of passing null to avoid overload ambiguity
            val dummySize = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        column(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                        gap(gapDp)
                    }
                ) {
                    // Item 1: (0, 0)
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[0], childPosition[0], positionedLatch)
                    )
                    // Item 2: (50 + 10, 0) = (60, 0)
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), childPosition[0].value)
            assertEquals(Offset((size + gap).toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_flexWithGaps() =
        with(density) {
            val size = 50
            val gap = 10
            // Available space for tracks: 50 - 10 = 40.
            // 1.fr + 1.fr = 2 parts. 40 / 2 = 20 per track.
            val expectedColWidth = (size - gap) / 2

            val positionedLatch = CountDownLatch(2)
            val childSize = Array(2) { Ref<IntSize>() }
            val childPosition = Array(2) { Ref<Offset>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Flex(1.fr))
                        column(GridTrackSize.Flex(1.fr))
                        row(GridTrackSize.Fixed(size.toDp()))
                        gap(gap.toDp())
                    },
                    modifier = Modifier.requiredSize(size.toDp()),
                ) {
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[0], childPosition[0], positionedLatch)
                    )
                    Box(
                        Modifier.gridItem(1, 2)
                            .fillMaxSize()
                            .saveLayoutInfo(childSize[1], childPosition[1], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
            assertEquals(IntSize(expectedColWidth, size), childSize[0].value)
            assertEquals(IntSize(expectedColWidth, size), childSize[1].value)
            assertEquals(Offset(0f, 0f), childPosition[0].value)
            assertEquals(Offset((expectedColWidth + gap).toFloat(), 0f), childPosition[1].value)
        }

    @Test
    fun testGrid_spanningWithGaps() {
        val size = 50
        val gap = 10
        // Spanning 2 columns: size + gap + size = 50 + 10 + 50 = 110
        val expectedWidth = size * 2 + gap

        val positionedLatch = CountDownLatch(1)
        val childSize = Ref<IntSize>()
        val dummyPos = Ref<Offset>()

        show {
            Grid(
                config = {
                    repeat(3) { column(GridTrackSize.Fixed(size.toDp())) }
                    row(GridTrackSize.Fixed(size.toDp()))
                    gap(gap.toDp())
                }
            ) {
                Box(
                    Modifier.gridItem(row = 1, column = 1, columnSpan = 2)
                        .fillMaxSize()
                        .saveLayoutInfo(childSize, dummyPos, positionedLatch)
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(expectedWidth, size), childSize.value)
    }

    @Test
    fun testGrid_implicitTracks_respectGaps() =
        with(density) {
            val size = 50
            val gap = 10
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }

            // Create a dummy ref instead of passing null
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size.toDp())) // 1 Explicit Col
                        row(GridTrackSize.Fixed(size.toDp()))
                        gap(gap.toDp())
                    }
                ) {
                    // Item 1: Col 1
                    Box(
                        Modifier.gridItem(1, 1)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos[0], latch)
                    )
                    // Item 2: Col 2 (Implicit). Should be at 50 + 10 = 60.
                    Box(
                        Modifier.gridItem(1, 2)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos[1], latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset((size + gap).toFloat(), 0f), pos[1].value)
        }

    @Test
    fun testGrid_gapPrecedence_specificOverridesGeneric() =
        with(density) {
            // Scenario:
            // gap(10) sets both.
            // rowGap(20) overrides row gap.
            // columnGap(5) overrides column gap.
            // Result: RowGap = 20, ColumnGap = 5.

            val size = 50
            val baseGap = 10
            val rowGapOverride = 20
            val colGapOverride = 5

            val sizeDp = size.toDp()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(2) { column(GridTrackSize.Fixed(sizeDp)) }
                        repeat(2) { row(GridTrackSize.Fixed(sizeDp)) }

                        gap(baseGap.toDp()) // Sets both to 10
                        rowGap(rowGapOverride.toDp()) // Overrides row to 20
                        columnGap(colGapOverride.toDp()) // Overrides col to 5
                    }
                ) {
                    // (0,0)
                    Box(Modifier.gridItem(1, 1).size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // (0,1) -> X should be Size + ColGap(5)
                    Box(Modifier.gridItem(1, 2).size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                    // (1,0) -> Y should be Size + RowGap(20)
                    Box(Modifier.gridItem(2, 1).size(sizeDp).saveLayoutInfo(dummy, pos[2], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset((size + colGapOverride).toFloat(), 0f), pos[1].value)
            assertEquals(Offset(0f, (size + rowGapOverride).toFloat()), pos[2].value)
        }

    @Test
    fun testGrid_alignment() =
        with(density) {
            val cellSize = 100
            val itemSize = 40
            // Center: (100 - 40) / 2 = 30
            val expectedOffset = 30f
            val cellSizeDp = cellSize.toDp()
            val itemSizeDp = itemSize.toDp()

            val positionedLatch = CountDownLatch(1)
            val childPosition = Ref<Offset>()
            val dummySize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(cellSizeDp))
                        row(GridTrackSize.Fixed(cellSizeDp))
                    }
                ) {
                    // Item smaller than cell, aligned center
                    Box(
                        Modifier.gridItem(1, 1, alignment = Alignment.Center)
                            .size(itemSizeDp)
                            .saveLayoutInfo(dummySize, childPosition, positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(expectedOffset, expectedOffset), childPosition.value)
        }

    @Test
    fun testGrid_alignment_spanning() =
        with(density) {
            val colSize = 50
            val rowSize = 60
            val gap = 10
            val itemSize = 40

            // Calculate total area dimensions including the gap
            val spannedWidth = (colSize * 2) + gap
            val spannedHeight = (rowSize * 2) + gap

            // Alignment.BottomEnd calculation:
            // x = ContainerWidth - ItemWidth
            // y = ContainerHeight - ItemHeight
            val expectedX = (spannedWidth - itemSize).toFloat()
            val expectedY = (spannedHeight - itemSize).toFloat()

            val positionedLatch = CountDownLatch(1)
            val childPosition = Ref<Offset>()
            val dummySize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(2) { column(GridTrackSize.Fixed(colSize.toDp())) }
                        repeat(2) { row(GridTrackSize.Fixed(rowSize.toDp())) }
                        gap(gap.toDp())
                    }
                ) {
                    Box(
                        Modifier.gridItem(
                                1,
                                1,
                                rowSpan = 2,
                                columnSpan = 2,
                                alignment = Alignment.BottomEnd,
                            )
                            .size(itemSize.toDp())
                            .saveLayoutInfo(dummySize, childPosition, positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
            assertEquals(Offset(expectedX, expectedY), childPosition.value)
        }

    @Test
    fun testGrid_alignment_rtl() =
        with(density) {
            val cellSize = 100
            val itemSize = 40
            val cellSizeDp = cellSize.toDp()
            val itemSizeDp = itemSize.toDp()

            val positionedLatch = CountDownLatch(2)
            val startAlignedPos = Ref<Offset>()
            val absLeftAlignedPos = Ref<Offset>()

            show {
                // Force RTL layout direction
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(cellSizeDp))
                            row(GridTrackSize.Fixed(cellSizeDp))
                        }
                    ) {
                        // 1. Alignment.TopStart (Relative 2D Alignment)
                        // In RTL, "Start" is visually on the RIGHT.
                        // Cell Width 100. Item 40.
                        // Expect visual position: 100 - 40 = 60px from visual Left.
                        Box(
                            Modifier.gridItem(1, 1, alignment = Alignment.TopStart)
                                .size(itemSizeDp)
                                .saveLayoutInfo(Ref(), startAlignedPos, positionedLatch)
                        )

                        // 2. AbsoluteAlignment.TopLeft (Absolute 2D Alignment)
                        // "Left" is always visually Left, regardless of layout direction.
                        // Expect visual position: 0px from visual Left.
                        Box(
                            // Use TopLeft (2D) instead of Left (1D) to satisfy the Alignment
                            // type requirement
                            Modifier.gridItem(
                                    1,
                                    1,
                                    alignment = androidx.compose.ui.AbsoluteAlignment.TopLeft,
                                )
                                .size(itemSizeDp)
                                .saveLayoutInfo(Ref(), absLeftAlignedPos, positionedLatch)
                        )
                    }
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // 1. Start (Right side of container)
            assertEquals(
                "Alignment.TopStart in RTL should be visually on the Right (60px from left)",
                Offset(60f, 0f),
                startAlignedPos.value,
            )

            // 2. Absolute Left (Left side of container)
            assertEquals(
                "AbsoluteAlignment.TopLeft in RTL should visually remain on the Left (0px)",
                Offset(0f, 0f),
                absLeftAlignedPos.value,
            )
        }

    @Test
    fun testGrid_contentBasedSizing() {
        // Col 1: MinContent (Fixed at Min)
        // Col 2: MaxContent (Fixed at Max)
        // Col 3: Auto (Behaves like MinMax)

        // Setup for Col 3 (Auto):
        // Container has extra space. Auto should grow to Max.

        val itemMin = 50
        val itemMax = 100
        val latch = CountDownLatch(3)
        val sizes = Array(3) { Ref<IntSize>() }

        show {
            Grid(
                config = {
                    column(GridTrackSize.MinContent)
                    column(GridTrackSize.MaxContent)
                    column(GridTrackSize.Auto)
                },
                // Large container to allow Auto to grow
                modifier = Modifier.width(500.dp),
            ) {
                // Item 1 (MinContent)
                IntrinsicItem(
                    minWidth = itemMin,
                    minIntrinsicWidth = itemMin,
                    maxIntrinsicWidth = itemMax,
                    modifier =
                        Modifier.gridItem(1, 1).fillMaxSize().saveLayoutInfo(sizes[0], Ref(), latch),
                )
                // Item 2 (MaxContent)
                IntrinsicItem(
                    minWidth = itemMin,
                    minIntrinsicWidth = itemMin,
                    maxIntrinsicWidth = itemMax,
                    modifier =
                        Modifier.gridItem(1, 2).fillMaxSize().saveLayoutInfo(sizes[1], Ref(), latch),
                )
                // Item 3 (Auto)
                IntrinsicItem(
                    minWidth = itemMin,
                    minIntrinsicWidth = itemMin,
                    maxIntrinsicWidth = itemMax,
                    modifier =
                        Modifier.gridItem(1, 3).fillMaxSize().saveLayoutInfo(sizes[2], Ref(), latch),
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        assertEquals("MinContent Track", itemMin, sizes[0].value?.width)
        assertEquals("MaxContent Track", itemMax, sizes[1].value?.width)
        assertEquals("Auto Track (Loose constraints)", itemMax, sizes[2].value?.width)
    }

    @Test
    fun testGrid_autoTrack_resolvesToMinMax() =
        with(density) {
            // Scenario:
            // Item has MinWidth = 50, MaxWidth = 100.
            // Track is Auto.

            // Case 1: Infinite Space (Scrollable) -> Should be Max (100)
            // Case 2: Tight Constraint (60) -> Should be 60 (Min 50 + 10 remaining)
            // Case 3: Large Constraint (200) -> Should be Max (100) (Capped at max content)

            val itemMin = 50
            val itemMax = 100
            val latch = CountDownLatch(3)
            val sizes = Array(3) { Ref<IntSize>() }

            show {
                Column {
                    // Case 1: Infinite Width
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Grid(config = { column(GridTrackSize.Auto) }) {
                            IntrinsicItem(
                                minWidth = itemMin,
                                minIntrinsicWidth = itemMin,
                                maxIntrinsicWidth = itemMax,
                                modifier =
                                    Modifier.gridItem(1, 1)
                                        .fillMaxHeight()
                                        .saveLayoutInfo(sizes[0], Ref(), latch),
                            )
                        }
                    }

                    // Case 2: Tight Constraint (60dp)
                    // Available = 60. Base (Min) = 50. Remaining = 10.
                    // Growth Potential = 50.
                    // Distributed = 10. Final = 50 + 10 = 60.
                    Box(Modifier.width(60.toDp())) {
                        Grid(config = { column(GridTrackSize.Auto) }) {
                            IntrinsicItem(
                                minWidth = itemMin,
                                minIntrinsicWidth = itemMin,
                                maxIntrinsicWidth = itemMax,
                                modifier =
                                    Modifier.gridItem(1, 1)
                                        .fillMaxHeight()
                                        .saveLayoutInfo(sizes[1], Ref(), latch),
                            )
                        }
                    }

                    // Case 3: Large Constraint (200dp)
                    // Available = 200. Base (Min) = 50. Remaining = 150.
                    // Growth Potential = 50.
                    // Distributed = 50 (Capped at potential). Final = 50 + 50 = 100.
                    Box(Modifier.width(200.toDp())) {
                        Grid(config = { column(GridTrackSize.Auto) }) {
                            IntrinsicItem(
                                minWidth = itemMin,
                                minIntrinsicWidth = itemMin,
                                maxIntrinsicWidth = itemMax,
                                modifier =
                                    Modifier.gridItem(1, 1)
                                        .fillMaxHeight()
                                        .saveLayoutInfo(sizes[2], Ref(), latch),
                            )
                        }
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Infinite container: Auto should be MaxContent",
                itemMax,
                sizes[0].value?.width,
            )
            assertEquals(
                "Tight container: Auto should grow from Min to fit available",
                60,
                sizes[1].value?.width,
            )
            assertEquals(
                "Large container: Auto should cap at MaxContent",
                itemMax,
                sizes[2].value?.width,
            )
        }

    @Test
    fun testGrid_autoTracks_distributeSpaceProportionally() =
        with(density) {
            // Scenario:
            // Container Width = 100px.
            // Two Auto Columns.
            // Item 1: Min 20, Max 40. (Growth Potential = 20)
            // Item 2: Min 20, Max 100. (Growth Potential = 80)

            // Logic:
            // 1. Base Sizes (MinContent): Col 1 = 20, Col 2 = 20. Total Used = 40.
            // 2. Remaining Space: 100 - 40 = 60.
            // 3. Total Potential: 20 + 80 = 100.
            // 4. Distribution:
            //    Col 1 Share: (20 / 100) * 60 = 12. Final Size: 20 + 12 = 32.
            //    Col 2 Share: (80 / 100) * 60 = 48. Final Size: 20 + 48 = 68.

            val containerWidth = 100
            val latch = CountDownLatch(2)
            val sizes = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Auto)
                        column(GridTrackSize.Auto)
                    },
                    modifier = Modifier.width(containerWidth.toDp()),
                ) {
                    // Item 1
                    IntrinsicItem(
                        minWidth = 20,
                        minIntrinsicWidth = 20,
                        maxIntrinsicWidth = 40,
                        modifier = Modifier.gridItem(1, 1).saveLayoutInfo(sizes[0], Ref(), latch),
                    )
                    // Item 2
                    IntrinsicItem(
                        minWidth = 20,
                        minIntrinsicWidth = 20,
                        maxIntrinsicWidth = 100,
                        modifier = Modifier.gridItem(1, 2).saveLayoutInfo(sizes[1], Ref(), latch),
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals("Col 1 (Small Potential)", 32, sizes[0].value?.width)
            assertEquals("Col 2 (Large Potential)", 68, sizes[1].value?.width)
        }

    @Test
    fun testGrid_auto_vs_flex_prioritization() =
        with(density) {
            // Scenario: Auto vs Flex.
            // Auto tracks are resolved (Pass 1.8) BEFORE Flex tracks (Pass 2).
            // Container = 200px.
            // Col 1: Auto (Item Min 50, Max 100).
            // Col 2: 1.fr

            // Logic:
            // 1. Base Auto = 50 (Min).
            // 2. Expand Auto:
            //    Remaining = 200 - 50 = 150.
            //    Auto wants to grow by 50 (to reach 100).
            //    It takes 50. Final Auto = 100.
            // 3. Flex Distribution:
            //    Remaining = 200 - 100 (Auto) = 100.
            //    Flex takes 100.

            val containerWidth = 200
            val itemMin = 50
            val itemMax = 100
            val latch = CountDownLatch(2)
            val sizes = Array(2) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Auto)
                        column(GridTrackSize.Flex(1.fr))
                    },
                    modifier = Modifier.width(containerWidth.toDp()),
                ) {
                    // Item in Auto Track
                    IntrinsicItem(
                        minWidth = itemMin,
                        minIntrinsicWidth = itemMin,
                        maxIntrinsicWidth = itemMax,
                        modifier = Modifier.gridItem(1, 1).saveLayoutInfo(sizes[0], Ref(), latch),
                    )
                    // Item in Flex Track
                    Box(
                        Modifier.gridItem(1, 2).fillMaxSize().saveLayoutInfo(sizes[1], Ref(), latch)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Auto track should reach MaxContent before Flex runs",
                100,
                sizes[0].value?.width,
            )
            assertEquals("Flex track should take remaining space", 100, sizes[1].value?.width)
        }

    @Test
    fun testGrid_implicitTracks_shrinkToFitContent() =
        with(density) {
            // Scenario:
            // Item placed in Col 10.
            // Cols 1-9 should be implicit Auto.
            // If they have no content, they should have width 0.
            // Col 10 should have width 50.
            // Total Grid Width should be 50 (0+0...+50).

            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(1)
            val pos = Ref<Offset>()
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // No explicit columns
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // Place far away at Column 5.
                    // Columns 1, 2, 3, 4 are Implicit Auto and Empty -> Size 0.
                    Box(
                        Modifier.gridItem(column = 5).size(sizeDp).saveLayoutInfo(dummy, pos, latch)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Position should be 0 because previous columns collapsed.
            // Note: If gaps were added, they would add up (4 * gap).
            assertEquals(Offset(0f, 0f), pos.value)
        }

    @Test
    fun testGrid_rowSpan_expandsRowsToFitContent() =
        with(density) {
            // Scenario:
            // 2 Columns (Fixed 50)
            // 2 Rows (Auto)
            // Item 1 (Col 1, Row 1): Small (10px height)
            // Item 2 (Col 1, Row 2): Small (10px height)
            // Item 3 (Col 2, Row 1, Span 2): Tall (100px height)
            //
            // Expected Behavior:
            // Without spanning logic: Rows would be 10px each (total 20px). Item 3 would overflow.
            // With spanning logic: Item 3 needs 100px.
            // Deficit = 100 - (10 + 10) = 80px.
            // Distribute 80px / 2 rows = +40px each.
            // Final Row Heights: 10 + 40 = 50px each.
            // Total Grid Height: 100px.

            val colWidth = 50.dp
            val smallItemHeight = 10.dp
            val tallItemHeight = 100.dp
            val expectedTotalHeight = 100.dp.roundToPx()

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(colWidth))
                        column(GridTrackSize.Fixed(colWidth))
                        row(GridTrackSize.Auto)
                        row(GridTrackSize.Auto)
                    },
                    modifier =
                        Modifier.onGloballyPositioned {
                            gridSize.value = it.size
                            latch.countDown()
                        },
                ) {
                    // Col 1, Row 1
                    Box(Modifier.gridItem(1, 1).size(colWidth, smallItemHeight))
                    // Col 1, Row 2
                    Box(Modifier.gridItem(2, 1).size(colWidth, smallItemHeight))

                    // Col 2, Row 1, Span 2 (The driver of expansion)
                    Box(
                        Modifier.gridItem(row = 1, column = 2, rowSpan = 2)
                            .size(colWidth, tallItemHeight)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Grid height should expand to accommodate the tall row-spanning item",
                expectedTotalHeight,
                gridSize.value?.height,
            )
        }

    @Test
    fun testGrid_rowSpan_expandsFlexRows() =
        with(density) {
            // Scenario:
            // Container Height = 100px (Fixed constraint)
            // 2 Rows (Flex 1fr)
            // Item 1 (Row 1): Empty
            // Item 2 (Row 2): Empty
            // Item 3 (Span 2): Tall (200px)
            //
            // Expected Behavior:
            // Flex logic initially splits 100px -> 50px each.
            // Spanning logic sees Item 3 needs 200px.
            // Deficit = 200 - 100 = 100px.
            // Rows should grow to 100px each.
            // Total Height = 200px (Grid expands beyond parent constraint if content demands it).

            val containerHeight = 100.dp
            val tallItemHeight = 200.dp
            val expectedTotalHeight = 200.dp.roundToPx()

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Wrap in a box with fixed height to simulate constraints,
                // but allow Grid to be larger (unbounded internal checks)
                Box(
                    Modifier.height(containerHeight)
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                ) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(50.dp))
                            row(GridTrackSize.Flex(1.fr))
                            row(GridTrackSize.Flex(1.fr))
                        },
                        modifier =
                            Modifier.onGloballyPositioned {
                                gridSize.value = it.size
                                latch.countDown()
                            },
                    ) {
                        // Spanning item forcing expansion
                        Box(
                            Modifier.gridItem(row = 1, column = 1, rowSpan = 2)
                                .size(50.dp, tallItemHeight)
                        )
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Flex rows should expand beyond 1fr share if spanning item requires it",
                expectedTotalHeight,
                gridSize.value?.height,
            )
        }

    @Test
    fun testGrid_autoPlacement_rowFlow() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        column(GridTrackSize.Fixed(sizeDp))
                        // Rows are implicit/auto
                        flow = GridFlow.Row
                    }
                ) {
                    // We use Modifier.size because implicit Auto tracks need content size to expand
                    // Item 1 -> (0,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // Item 2 -> (50,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                    // Item 3 -> Wraps to next row -> (0, 50)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[2], latch))
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(size.toFloat(), 0f), pos[1].value)
            assertEquals(Offset(0f, size.toFloat()), pos[2].value)
        }

    @Test
    fun testGrid_autoPlacement_columnFlow() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        flow = GridFlow.Column
                        row(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                        // Cols are implicit/auto
                    }
                ) {
                    // Item 1 -> (0,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // Item 2 -> (0,50)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                    // Item 3 -> Wraps to next col -> (50,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[2], latch))
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, size.toFloat()), pos[1].value)
            assertEquals(Offset(size.toFloat(), 0f), pos[2].value)
        }

    @Test
    fun testGrid_autoPlacement_wrapping_respectsGaps() =
        with(density) {
            val size = 50
            val gap = 10
            val sizeDp = size.toDp()
            val gapDp = gap.toDp()
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp)) // Only 1 column
                        row(GridTrackSize.Fixed(sizeDp))
                        gap(gapDp)
                        flow = GridFlow.Row
                    }
                ) {
                    // Item 1: (0,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // Item 2: Wraps to (0,1). Should include vertical gap.
                    // Y Position = Row 1 Height (50) + Gap (10) = 60
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, (size + gap).toFloat()), pos[1].value)
        }

    @Test
    fun testGrid_columnFlow_wrapsCorrectly() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        flow = GridFlow.Column
                        // 2 Explicit Rows
                        row(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // Item 1 -> (0,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // Item 2 -> (0,50)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                    // Item 3 -> Wraps to Next Column -> (50,0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[2], latch))
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, size.toFloat()), pos[1].value)
            assertEquals(Offset(size.toFloat(), 0f), pos[2].value)
        }

    @Test
    fun testGrid_columnFlow_createsImplicitColumns() =
        with(density) {
            val itemSize = 50
            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // 2 Explicit Rows. Flow = Column.
                        row(GridTrackSize.Fixed(itemSize.toDp()))
                        row(GridTrackSize.Fixed(itemSize.toDp()))
                        flow = GridFlow.Column
                    },
                    modifier =
                        Modifier.onGloballyPositioned {
                            gridSize.value = it.size
                            latch.countDown()
                        },
                ) {
                    // 4 items.
                    // Items 1, 2 fill Col 1 (Rows 1, 2)
                    // Items 3, 4 should create implicit Col 2 (Rows 1, 2)
                    repeat(4) { Box(Modifier.size(itemSize.toDp())) }
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Expected: 2 Rows (Explicit) x 2 Columns (1 Explicit + 1 Implicit)
            // Implicit columns default to Auto -> itemSize (50)
            assertEquals(IntSize(itemSize * 2, itemSize * 2), gridSize.value)
        }

    @Test
    fun testGrid_columnFlow_implicitRows() =
        with(density) {
            // Scenario:
            // Flow = Column.
            // Explicitly define 1 Column (so we know the width).
            // Do NOT define any Rows.
            // Item 1: (0,0)
            // Item 2: Should stack below at (1,0) -> Creating Implicit Row 2
            // Item 3: Should stack below at (2,0) -> Creating Implicit Row 3

            val size = 50.dp
            val sizePx = size.roundToPx().toFloat()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        flow = GridFlow.Column
                        // Define column width so layout isn't 0 width
                        column(GridTrackSize.Fixed(size))
                        // Do NOT define rows.
                        // This allows the column to grow infinitely downwards.
                    }
                ) {
                    // (0,0)
                    Box(Modifier.size(size).saveLayoutInfo(dummy, pos[0], latch))
                    // (1, 0) -> Implicit Row 2
                    Box(Modifier.size(size).saveLayoutInfo(dummy, pos[1], latch))
                    // (2, 0) -> Implicit Row 3
                    Box(Modifier.size(size).saveLayoutInfo(dummy, pos[2], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // All X should be 0 (Column 0)
            // Y should increment by sizePx
            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, sizePx), pos[1].value)
            assertEquals(Offset(0f, sizePx * 2), pos[2].value)
        }

    @Test
    fun testGrid_mixedPlacement_skipsOccupiedCells() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(3) { column(GridTrackSize.Fixed(sizeDp)) }
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // 1. Explicit Item at (0, 1) (Middle Column)
                    Box(Modifier.gridItem(1, 2).size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))

                    // 2. Auto Item 1 -> Should go to (0, 0)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))

                    // 3. Auto Item 2 -> Should skip (0, 1) and go to (0, 2)
                    Box(Modifier.size(sizeDp).saveLayoutInfo(dummy, pos[2], latch))
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Explicit
            assertEquals(Offset(size.toFloat(), 0f), pos[0].value)
            // Auto 1
            assertEquals(Offset(0f, 0f), pos[1].value)
            // Auto 2
            assertEquals(Offset((size * 2).toFloat(), 0f), pos[2].value)
        }

    @Test
    fun testGrid_explicitPlacement_allowsOverlaps() =
        with(density) {
            // Scenario:
            // Two items explicitly placed in (1, 1).
            // They should occupy the same space. The grid should not throw or shift them.

            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(sizeDp))
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // Item 1
                    Box(Modifier.gridItem(1, 1).size(sizeDp).saveLayoutInfo(dummy, pos[0], latch))
                    // Item 2 (Same Cell)
                    Box(Modifier.gridItem(1, 1).size(sizeDp).saveLayoutInfo(dummy, pos[1], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, 0f), pos[1].value)
        }

    @Test
    fun testGrid_explicitPlacement_doesNotMoveAutoCursor() =
        with(density) {
            // Scenario:
            // 3 Columns.
            // Item 1: Auto (0,0). Cursor moves to (0,1).
            // Item 2: Explicit far away (0, 2). Cursor should REMAIN at (0,1).
            // Item 3: Auto. Should fill the gap at (0,1), NOT start after Item 2.

            val size = 50.dp
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(3) { column(GridTrackSize.Fixed(size)) }
                        row(GridTrackSize.Fixed(size))
                    }
                ) {
                    // 1. Auto -> (0,0)
                    Box(Modifier.size(size).saveLayoutInfo(dummy, pos[0], latch))
                    // 2. Explicit -> (0,2). Should NOT move cursor.
                    Box(Modifier.gridItem(1, 3).size(size).saveLayoutInfo(dummy, pos[1], latch))
                    // 3. Auto -> Should fill (0,1)
                    Box(Modifier.size(size).saveLayoutInfo(dummy, pos[2], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Calculate pixel size of a single track first
            val sizePx = size.roundToPx().toFloat()

            // Auto 1 (0,0)
            assertEquals(Offset(0f, 0f), pos[0].value)
            // Explicit (Col 3 -> Index 2)
            assertEquals(Offset(sizePx * 2, 0f), pos[1].value)
            // Auto 2 (Col 2 -> Index 1)
            assertEquals(Offset(sizePx, 0f), pos[2].value)
        }

    @Test
    fun testGrid_fixedRow_autoCol_skipsOccupied() =
        with(density) {
            // Scenario:
            // 3 Columns.
            // Item 1: Explicitly placed at (0, 0).
            // Item 2: Fixed Row 0. Should skip (0,0) and go to (0,1).
            // Item 3: Fixed Row 0. Should skip (0,0) and (0,1) and go to (0,2).

            val size = 50.dp
            val latch = CountDownLatch(3)
            val pos = Array(3) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(3) { column(GridTrackSize.Fixed(size)) }
                        row(GridTrackSize.Fixed(size))
                    }
                ) {
                    // 1. Occupy (0,0) explicitly
                    Box(Modifier.gridItem(1, 1).size(size).saveLayoutInfo(dummy, pos[0], latch))
                    // 2. Request Row 1 (Auto Col). Should find Col 2.
                    Box(Modifier.gridItem(row = 1).size(size).saveLayoutInfo(dummy, pos[1], latch))
                    // 3. Request Row 1 (Auto Col). Should find Col 3.
                    Box(Modifier.gridItem(row = 1).size(size).saveLayoutInfo(dummy, pos[2], latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Calculate pixel size of a single track first
            val sizePx = size.roundToPx().toFloat()

            // (0,0)
            assertEquals(Offset(0f, 0f), pos[0].value)
            // (50, 0)
            assertEquals(Offset(sizePx, 0f), pos[1].value)
            // (100, 0) -> Sum of two tracks
            assertEquals(Offset(sizePx * 2, 0f), pos[2].value)
        }

    @Test
    fun testGrid_fixedCol_autoRow_skipsOccupied() =
        with(density) {
            // Scenario:
            // 1 Column, 3 Rows.
            // Item 1: Explicitly placed at (0, 0).
            // Item 2: Fixed Col 0. Should skip (0,0) and go to (1,0).

            val size = 50.dp
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size))
                        repeat(3) { row(GridTrackSize.Fixed(size)) }
                    }
                ) {
                    // 1. Occupy (0,0) explicitly
                    Box(Modifier.gridItem(1, 1).size(size).saveLayoutInfo(dummy, pos[0], latch))
                    // 2. Request Col 1 (Auto Row). Should skip Row 1 and land in Row 2.
                    Box(
                        Modifier.gridItem(column = 1)
                            .size(size)
                            .saveLayoutInfo(dummy, pos[1], latch)
                    )
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Calculate pixel size of a single track first
            val sizePx = size.roundToPx().toFloat()

            assertEquals(Offset(0f, 0f), pos[0].value)
            assertEquals(Offset(0f, sizePx), pos[1].value)
        }

    @Test
    fun testGrid_mixedFlow_fixedRowInColumnFlow() =
        with(density) {
            // Scenario:
            // Flow = Column.
            // Item 1: Fixed Row 1.
            // Since flow is column, the logic must look for the *first available column* in that
            // fixed row.

            val size = 50.dp
            val latch = CountDownLatch(1)
            val pos = Ref<Offset>()
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size))
                        column(GridTrackSize.Fixed(size))
                        row(GridTrackSize.Fixed(size))
                        row(GridTrackSize.Fixed(size))
                        flow = GridFlow.Column
                    }
                ) {
                    // Occupy (1,0) - (Row 2, Col 1)
                    Box(Modifier.gridItem(2, 1).size(size))

                    // Request Row 2.
                    // Since (2,1) is occupied, it should find (2,2).
                    Box(Modifier.gridItem(row = 2).size(size).saveLayoutInfo(dummy, pos, latch))
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Calculate pixel size of a single track first
            val sizePx = size.roundToPx().toFloat()

            // Should be at Row 2 (Index 1), Col 2 (Index 1) -> (50, 50)
            assertEquals(Offset(sizePx, sizePx), pos.value)
        }

    @Test
    fun testGrid_negativeIndices_placeCorrectly() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()

            val positionedLatch = CountDownLatch(4)
            val childPosition = Array(4) { Ref<Offset>() }
            val dummySize = Array(4) { Ref<IntSize>() }

            show {
                Grid(
                    config = {
                        repeat(3) { column(GridTrackSize.Fixed(sizeDp)) }
                        repeat(3) { row(GridTrackSize.Fixed(sizeDp)) }
                    }
                ) {
                    // 1. Top-Left (1, 1) -> (0, 0)
                    Box(
                        Modifier.gridItem(1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[0], childPosition[0], positionedLatch)
                    )
                    // 2. Top-Right (1, -1) -> (100, 0) (Last Column)
                    Box(
                        Modifier.gridItem(1, -1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[1], childPosition[1], positionedLatch)
                    )
                    // 3. Bottom-Left (-1, 1) -> (0, 100) (Last Row)
                    Box(
                        Modifier.gridItem(-1, 1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[2], childPosition[2], positionedLatch)
                    )
                    // 4. Bottom-Right (-1, -1) -> (100, 100) (Last Row, Last Column)
                    Box(
                        Modifier.gridItem(-1, -1)
                            .fillMaxSize()
                            .saveLayoutInfo(dummySize[3], childPosition[3], positionedLatch)
                    )
                }
            }
            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // 1. (0, 0)
            assertEquals(Offset(0f, 0f), childPosition[0].value)
            // 2. (100, 0) -> Col index 2 * 50
            assertEquals(Offset((size * 2).toFloat(), 0f), childPosition[1].value)
            // 3. (0, 100) -> Row index 2 * 50
            assertEquals(Offset(0f, (size * 2).toFloat()), childPosition[2].value)
            // 4. (100, 100)
            assertEquals(Offset((size * 2).toFloat(), (size * 2).toFloat()), childPosition[3].value)
        }

    @Test
    fun testGrid_negativeIndex_refersToExplicitBounds() =
        with(density) {
            val size = 50
            val latch = CountDownLatch(1)
            val pos = Ref<Offset>()

            // Create a dummy ref instead of passing null
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(2) {
                            column(GridTrackSize.Fixed(size.toDp()))
                        } // Explicit Cols: 0, 1
                        row(GridTrackSize.Fixed(size.toDp()))
                    }
                ) {
                    // Create an implicit 3rd column (Index 2)
                    Box(Modifier.gridItem(1, 3).size(size.toDp()))

                    // Place item at column = -1.
                    // Should map to Explicit Col 1 (the 2nd column), NOT the implicit 3rd column.
                    Box(
                        Modifier.gridItem(column = -1)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos, latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Expect pos at 2nd column (Index 1) -> 50px
            assertEquals(Offset(size.toFloat(), 0f), pos.value)
        }

    @Test
    fun testGrid_invalidNegativeIndices_fallbackToAuto() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(2)
            val pos1 = Ref<Offset>()
            val pos2 = Ref<Offset>()
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // 2x2 Grid
                        repeat(2) { column(GridTrackSize.Fixed(sizeDp)) }
                        repeat(2) { row(GridTrackSize.Fixed(sizeDp)) }
                    }
                ) {
                    // Case 1: Valid Negative (-1 -> Index 1)
                    Box(
                        Modifier.gridItem(row = -1, column = -1)
                            .size(sizeDp)
                            .saveLayoutInfo(dummy, pos1, latch)
                    )

                    // Case 2: Invalid Negative (-5 -> Index -3 -> Invalid)
                    // Should be treated as "Unspecified" and auto-placed.
                    // Since (1,1) is empty (Item 1 is at 1,1 0-based), it should go to (0,0).
                    Box(
                        Modifier.gridItem(row = -5, column = -5)
                            .size(sizeDp)
                            .saveLayoutInfo(dummy, pos2, latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Item 1 (Valid -1,-1): Bottom-Right (50, 50)
            assertEquals(Offset(size.toFloat(), size.toFloat()), pos1.value)

            // Item 2 (Invalid -5,-5): Auto-placed to first available slot (0,0)
            assertEquals(Offset(0f, 0f), pos2.value)
        }

    @Test
    fun testGrid_spanning() {
        val colSize = 50
        val rowSize = 50

        val positionedLatch = CountDownLatch(1)
        val childSize = Ref<IntSize>()
        val childPosition = Ref<Offset>()

        show {
            Grid(
                config = {
                    repeat(3) { column(GridTrackSize.Fixed(colSize.toDp())) }
                    repeat(3) { row(GridTrackSize.Fixed(rowSize.toDp())) }
                }
            ) {
                // Item at R2, C2 spanning 2 rows and 2 columns
                // Should be at (50, 50) with size (100, 100)
                Box(
                    Modifier.gridItem(row = 2, column = 2, rowSpan = 2, columnSpan = 2)
                        .fillMaxSize()
                        .saveLayoutInfo(childSize, childPosition, positionedLatch)
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertEquals(IntSize(colSize * 2, rowSize * 2), childSize.value)
        assertEquals(Offset(colSize.toFloat(), rowSize.toFloat()), childPosition.value)
    }

    @Test
    fun testGrid_spanEntireGrid() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(1)
            val itemSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        repeat(4) { column(GridTrackSize.Fixed(sizeDp)) }
                        row(GridTrackSize.Fixed(sizeDp))
                    }
                ) {
                    // Spans all 4 columns
                    Box(
                        Modifier.gridItem(1, 1, columnSpan = 4)
                            .fillMaxSize()
                            .saveLayoutInfo(itemSize, Ref(), latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(size * 4, itemSize.value?.width)
        }

    @Test
    fun testGrid_spanning_intoImplicitTracks() =
        with(density) {
            val size = 50
            val latch = CountDownLatch(1)
            val itemSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size.toDp())) // 1 Explicit Column
                        row(GridTrackSize.Fixed(size.toDp()))
                    }
                ) {
                    // Place at Col 1, Span 2.
                    // Should cover Explicit Col 1 + Implicit Col 2.
                    // Implicit Col 2 should size to Auto. Since this item spans,
                    // and Auto tracks ignore spanning items for intrinsic sizing (as per your
                    // design),
                    // the implicit track might collapse to 0 OR resize if logic allows.
                    // *Correction*: Your logic adds `Auto` tracks. If no other item is in Col 2,
                    // it will be size 0.
                    // Let's add a non-spanning item in Col 2 to give it size.

                    // Item A: Spans Col 1 and Col 2
                    Box(
                        Modifier.gridItem(1, 1, columnSpan = 2)
                            .fillMaxSize()
                            .saveLayoutInfo(itemSize, Ref(), latch)
                    )

                    // Item B: Sits in Implicit Col 2 to force it to have size
                    Box(Modifier.gridItem(1, 2).size(size.toDp()))
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Width = Col 1 (50) + Col 2 (50 from Item B) = 100
            assertEquals(size * 2, itemSize.value?.width)
        }

    @Test
    fun testGrid_itemSpanLargerThanExplicitGrid_doesNotLoop() =
        with(density) {
            val size = 50
            val sizeDp = size.toDp()
            val latch = CountDownLatch(1)
            val pos = Ref<Offset>()
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // 2 Explicit Columns
                        column(GridTrackSize.Fixed(sizeDp))
                        column(GridTrackSize.Fixed(sizeDp))
                        flow = GridFlow.Row
                    }
                ) {
                    // Item spans 3 columns (Exceeds explicit count of 2)
                    // Should be placed at (0,0) and create implicit tracks
                    Box(
                        Modifier.gridItem(columnSpan = 3)
                            .size(sizeDp)
                            .saveLayoutInfo(dummy, pos, latch)
                    )
                }
            }
            assertTrue("Timed out - likely infinite loop", latch.await(1, TimeUnit.SECONDS))

            assertEquals(Offset(0f, 0f), pos.value)
        }

    @Test
    fun testGrid_respectsMinConstraints_expandsToFill() =
        with(density) {
            val smallTrackSize = 50.dp
            val largeParentSize = 100.dp
            val expectedSize = 100.dp.roundToPx()

            val positionedLatch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(smallTrackSize))
                        row(GridTrackSize.Fixed(smallTrackSize))
                    },
                    // Force the Grid to be larger than its content
                    modifier =
                        Modifier.size(largeParentSize).onGloballyPositioned { coordinates ->
                            gridSize.value = coordinates.size
                            positionedLatch.countDown()
                        },
                ) { /* empty */
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Grid should expand to satisfy min constraints",
                IntSize(expectedSize, expectedSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_respectsMaxConstraints_coercesSize() =
        with(density) {
            val largeTrackSize = 200.dp
            val smallParentSize = 100.dp
            val expectedSize = 100.dp.roundToPx()

            val positionedLatch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Wrap in Box with propagateMinConstraints=false to test pure max constraints
                Box(Modifier.size(smallParentSize)) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(largeTrackSize))
                            row(GridTrackSize.Fixed(largeTrackSize))
                        },
                        modifier =
                            Modifier.onGloballyPositioned { coordinates ->
                                gridSize.value = coordinates.size
                                positionedLatch.countDown()
                            },
                    ) { /* empty */
                    }
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Grid should respect max constraints even if tracks are larger",
                IntSize(expectedSize, expectedSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_respectsConstraints_whenContentOverflows() =
        with(density) {
            val parentSize = 100
            val contentSize = 200 // Larger than parent

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Parent container restricts size to 100x100
                Box(Modifier.size(parentSize.toDp())) {
                    Grid(
                        config = {
                            // Grid wants to be 200x200
                            column(GridTrackSize.Fixed(contentSize.toDp()))
                            row(GridTrackSize.Fixed(contentSize.toDp()))
                        },
                        modifier =
                            Modifier.onGloballyPositioned {
                                gridSize.value = it.size
                                latch.countDown()
                            },
                    ) {
                        Box(Modifier.gridItem(1, 1).fillMaxSize())
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Assert that Grid reported the PARENT'S size (clamped), not the content size
            assertEquals(
                "Grid should be clamped to parent max width/height",
                IntSize(parentSize, parentSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_respectsConstraints_whenContentUnderflows() =
        with(density) {
            val minSize = 200
            val contentSize = 50 // Smaller than parent min

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Parent enforces minimum size of 200x200 (e.g. fillMaxSize)
                Box(Modifier.requiredSize(minSize.toDp())) {
                    Grid(
                        config = {
                            column(GridTrackSize.Fixed(contentSize.toDp()))
                            row(GridTrackSize.Fixed(contentSize.toDp()))
                        },
                        modifier =
                            Modifier.fillMaxSize() // Request to fill parent
                                .onGloballyPositioned {
                                    gridSize.value = it.size
                                    latch.countDown()
                                },
                    ) {
                        Box(Modifier.gridItem(1, 1).fillMaxSize())
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Assert that Grid expanded to meet the minimum constraints
            assertEquals(
                "Grid should expand to meet min constraints",
                IntSize(minSize, minSize),
                gridSize.value,
            )
        }

    @Test
    fun testGrid_percentageTrack_inIndefiniteContainer_fallbacksToAuto() =
        with(density) {
            val positionedLatch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Wrap in a scrolling Row to provide infinite width constraint
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Grid(
                        config = {
                            // 50% of Infinity cannot be calculated.
                            // Fallback to Auto (MaxContent) and fit the item.
                            column(GridTrackSize.Percentage(0.5f))
                            row(GridTrackSize.Fixed(50.dp))
                        },
                        modifier =
                            Modifier.onGloballyPositioned { coordinates ->
                                gridSize.value = coordinates.size
                                positionedLatch.countDown()
                            },
                    ) {
                        // The item is 10.dp wide. The track should expand to fit this.
                        Box(Modifier.gridItem(1, 1).size(10.dp))
                    }
                }
            }

            assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

            // Width should be 10.dp (Size of the content), NOT 0.
            // Height should be 50.dp (Fixed)
            assertEquals(IntSize(10.dp.roundToPx(), 50.dp.roundToPx()), gridSize.value)
        }

    @Test
    fun testGrid_flexColumn_inInfiniteConstraints_withGap_doesNotExpand() =
        with(density) {
            // Scenario: Grid is inside a Row + horizontalScroll (Infinite Width).
            // It has a Flex column and a GAP.
            //
            // Bug Trigger:
            // 1. Available Width = Infinity.
            // 2. Gap = 10px.
            // 3. Calculation: availableTrackSpace = Infinity - 10 = 2,147,483,637.
            // 4. Check: (availableTrackSpace == Infinity) is FALSE.
            // 5. Result: Logic thinks it has 2 billion pixels of space to distribute.
            // 6. Flex track expands to ~2 billion pixels.

            val gap = 10.dp
            val itemSize = 50.dp
            val expectedWidth = itemSize.roundToPx() // Should shrink to fit content

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                // Parent provides Infinite Width constraint
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Grid(
                        config = {
                            column(GridTrackSize.Flex(1.fr)) // Should behave like MinContent/Auto
                            column(
                                GridTrackSize.Fixed(0.dp)
                            ) // Dummy column to ensure gap is applied
                            row(GridTrackSize.Fixed(50.dp))
                            columnGap(gap)
                        },
                        modifier =
                            Modifier.onGloballyPositioned {
                                gridSize.value = it.size
                                latch.countDown()
                            },
                    ) {
                        // Item in Flex column
                        Box(Modifier.gridItem(1, 1).size(itemSize))
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))

            assertEquals(
                "Flex column in infinite constraints should fallback to min-content size",
                expectedWidth + gap.roundToPx(), // 50 (Item) + 10 (Gap) + 0 (Col 2)
                gridSize.value?.width,
            )
        }

    @Test
    fun testGrid_zeroSizeTrack_layoutCorrectly() =
        with(density) {
            val size = 50
            val latch = CountDownLatch(2)
            val pos = Array(2) { Ref<Offset>() }
            val dummy = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(size.toDp()))
                        column(GridTrackSize.Fixed(0.dp)) // Zero width column
                        column(GridTrackSize.Fixed(size.toDp()))
                        row(GridTrackSize.Fixed(size.toDp()))
                    }
                ) {
                    // Item 1: Col 1
                    Box(
                        Modifier.gridItem(1, 1)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos[0], latch)
                    )
                    // Item 2: Col 3 (Skipping Col 2 which is 0 width)
                    Box(
                        Modifier.gridItem(1, 3)
                            .size(size.toDp())
                            .saveLayoutInfo(dummy, pos[1], latch)
                    )
                }
            }
            assertTrue(latch.await(1, TimeUnit.SECONDS))

            // Item 1 at 0
            assertEquals(Offset(0f, 0f), pos[0].value)
            // Item 2 at 50 + 0 = 50
            assertEquals(Offset(size.toFloat(), 0f), pos[1].value)
        }

    @Test
    fun testGrid_zeroSizeChildren() {
        val trackSize = 50
        val latch = CountDownLatch(1)
        val gridSize = Ref<IntSize>()

        show {
            Grid(
                config = {
                    column(GridTrackSize.Fixed(trackSize.toDp()))
                    row(GridTrackSize.Fixed(trackSize.toDp()))
                },
                modifier =
                    Modifier.onGloballyPositioned {
                        gridSize.value = it.size
                        latch.countDown()
                    },
            ) {
                // Place a zero-sized item.
                // It should still occupy the logical cell (1,1), but draw nothing.
                // The Grid should still size itself to the Fixed tracks (50x50).
                Box(Modifier.gridItem(1, 1).size(0.dp))
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(trackSize, trackSize), gridSize.value)
    }

    @Test
    fun testGrid_itemFillsCell_whenRequested() {
        val trackSize = 100
        val latch = CountDownLatch(1)
        val childSize = Ref<IntSize>()

        show {
            Grid(
                config = {
                    column(GridTrackSize.Fixed(trackSize.toDp()))
                    row(GridTrackSize.Fixed(trackSize.toDp()))
                }
            ) {
                // Item has no intrinsic size, but requests fillMaxSize().
                // It should fill the definition of the track (100x100).
                Box(Modifier.gridItem(1, 1).fillMaxSize().saveLayoutInfo(childSize, Ref(), latch))
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(trackSize, trackSize), childSize.value)
    }

    @Test
    fun testGrid_nestedGrid() =
        with(density) {
            val outerSize = 100
            val outerSizeDp = outerSize.toDp()

            // Inner grid will be placed in a 100x100 cell.
            // It will have 2 columns of 50 each.
            val latch = CountDownLatch(1)
            val innerItemSize = Ref<IntSize>()

            show {
                // Outer Grid
                Grid(
                    config = {
                        column(GridTrackSize.Fixed(outerSizeDp))
                        row(GridTrackSize.Fixed(outerSizeDp))
                    }
                ) {
                    // Inner Grid placed at (1,1) of Outer Grid
                    Grid(
                        modifier = Modifier.gridItem(1, 1).fillMaxSize(),
                        config = {
                            column(GridTrackSize.Flex(1.fr))
                            column(GridTrackSize.Flex(1.fr))
                            row(GridTrackSize.Flex(1.fr))
                        },
                    ) {
                        // Item inside Inner Grid (Col 1)
                        Box(
                            Modifier.gridItem(1, 1)
                                .fillMaxSize()
                                .saveLayoutInfo(innerItemSize, Ref(), latch)
                        )
                        // Item inside Inner Grid (Col 2) just to fill space
                        Box(Modifier.gridItem(1, 2).fillMaxSize())
                    }
                }
            }

            assertTrue(latch.await(1, TimeUnit.SECONDS))
            assertEquals(IntSize(50, 100), innerItemSize.value)
        }

    @Test
    fun testGrid_stressTest_manyItems() =
        with(density) {
            val itemSize = 10
            val itemSizeDp = itemSize.toDp()
            val itemCount = 100
            val cols = 10

            // 100 items / 10 cols = 10 rows.
            // Total Height = 10 rows * 10px = 100px.
            val expectedHeight = 100

            val latch = CountDownLatch(1)
            val gridSize = Ref<IntSize>()

            show {
                Grid(
                    config = {
                        // 10 Fixed columns
                        repeat(cols) { column(GridTrackSize.Fixed(itemSizeDp)) }
                        // Implicit rows
                        flow = GridFlow.Row
                    },
                    modifier =
                        Modifier.onGloballyPositioned {
                            gridSize.value = it.size
                            latch.countDown()
                        },
                ) {
                    repeat(itemCount) { Box(Modifier.size(itemSizeDp)) }
                }
            }

            assertTrue(latch.await(3, TimeUnit.SECONDS))

            assertEquals(10 * itemSize, gridSize.value?.width)
            assertEquals(expectedHeight, gridSize.value?.height)
        }

    @Test(expected = IllegalArgumentException::class)
    fun testGrid_invalidIndices_throws() {
        show {
            Grid(
                config = {
                    column(GridTrackSize.Fixed(10.dp))
                    row(GridTrackSize.Fixed(10.dp))
                }
            ) {
                Box(Modifier.gridItem(100000, 1)) // Out of bounds
            }
        }
    }

    @Test
    fun testGrid_config_accessConstraints() =
        with(density) {
            val size = 100
            val sizeDp = size.toDp()
            val latch = CountDownLatch(1)

            // Capture values from inside the config lambda
            val capturedMaxWidth = Ref<Dp>()
            val capturedMaxHeight = Ref<Dp>()
            val capturedConstraints = Ref<Constraints>()

            show {
                Box(Modifier.size(sizeDp)) {
                    Grid(
                        config = {
                            val maxWidthDp = constraints.maxWidth.toDp()
                            val maxHeightDp = constraints.maxHeight.toDp()
                            capturedMaxWidth.value = maxWidthDp
                            capturedMaxHeight.value = maxHeightDp
                            capturedConstraints.value = constraints

                            // Define minimal tracks so layout pass completes
                            column(GridTrackSize.Fixed(10.dp))
                            row(GridTrackSize.Fixed(10.dp))
                        }
                    ) {
                        // Place a dummy item to ensure measurement happens
                        Box(
                            Modifier.gridItem(1, 1).size(10.dp).onGloballyPositioned {
                                latch.countDown()
                            }
                        )
                    }
                }
            }

            assertTrue("Timed out waiting for layout", latch.await(1, TimeUnit.SECONDS))

            // Verify explicit Constraints (Exact pixels)
            // We use the pixel size directly since we created the Box with sizeDp derived from it
            assertEquals(size, capturedConstraints.value?.maxWidth)
            assertEquals(size, capturedConstraints.value?.maxHeight)

            // Verify Dp properties.
            // Note: We compare pixel values because Dp floating point precision might
            // result in 100.dp vs 100.0001.dp depending on density math.
            assertEquals(size, capturedMaxWidth.value?.roundToPx())
            assertEquals(size, capturedMaxHeight.value?.roundToPx())
        }

    @Test
    fun testGrid_responsive_columnsChangeWithWidth() =
        with(density) {
            // Scenario: Responsive Breakpoint
            // < 200dp width -> 1 Column
            // >= 200dp width -> 2 Columns

            val widthCompact = 100.dp
            val widthExpanded = 300.dp
            var parentWidth by mutableStateOf(widthCompact)

            val initialLatch = CountDownLatch(1)
            val updateLatch = CountDownLatch(1)
            val itemSize = Ref<IntSize>()

            show {
                Box(Modifier.width(parentWidth)) {
                    Grid(
                        config = {
                            val maxWidthDp = constraints.maxWidth.toDp()
                            val cols = if (maxWidthDp < 200.dp) 1 else 2

                            repeat(cols) { column(GridTrackSize.Flex(1.fr)) }
                            row(GridTrackSize.Auto)
                        }
                    ) {
                        // Place item in Column 1.
                        // We measure this item to verify the column width.
                        Box(
                            Modifier.gridItem(1, 1)
                                .fillMaxWidth()
                                .height(50.dp)
                                .onGloballyPositioned { coordinates ->
                                    itemSize.value = coordinates.size
                                    if (initialLatch.count > 0) {
                                        initialLatch.countDown()
                                    } else {
                                        updateLatch.countDown()
                                    }
                                }
                        )
                    }
                }
            }

            // 1. Verify Initial State (Compact: 100dp -> 1 Column)
            assertTrue(
                "Timed out waiting for initial layout",
                initialLatch.await(1, TimeUnit.SECONDS),
            )

            // 1 Column = Full Width
            assertEquals(widthCompact.roundToPx(), itemSize.value?.width)

            // 2. Resize Parent (Expanded: 300dp)
            parentWidth = widthExpanded

            // 3. Verify Updated State (Expanded: 300dp -> 2 Columns)
            assertTrue(
                "Timed out waiting for layout update",
                updateLatch.await(1, TimeUnit.SECONDS),
            )

            val totalPx = widthExpanded.roundToPx()
            val expectedColWidth = (totalPx * 0.5f).roundToInt()

            assertEquals(expectedColWidth, itemSize.value?.width)
        }

    @Composable
    private fun IntrinsicItem(
        minWidth: Int,
        minIntrinsicWidth: Int,
        maxIntrinsicWidth: Int,
        modifier: Modifier = Modifier,
    ) {
        Layout(
            modifier,
            measurePolicy =
                object : MeasurePolicy {
                    override fun MeasureScope.measure(
                        measurables: List<Measurable>,
                        constraints: Constraints,
                    ): MeasureResult {
                        // Try to reach maxIntrinsicWidth, but respect parent constraints.
                        val width =
                            constraints.constrainWidth(maxIntrinsicWidth).coerceAtLeast(minWidth)
                        return layout(width, constraints.minHeight) {}
                    }

                    override fun IntrinsicMeasureScope.minIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int,
                    ) = minIntrinsicWidth

                    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                        measurables: List<IntrinsicMeasurable>,
                        height: Int,
                    ) = maxIntrinsicWidth
                },
        )
    }
}

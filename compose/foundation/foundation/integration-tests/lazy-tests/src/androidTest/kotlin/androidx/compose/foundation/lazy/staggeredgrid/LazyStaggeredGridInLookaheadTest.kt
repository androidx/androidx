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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.UnspecifiedOffset
import androidx.compose.foundation.lazy.list.assertIsNotPlaced
import androidx.compose.foundation.lazy.list.assertIsPlaced
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyStaggeredGridInLookaheadTest(private val orientation: Orientation) :
    BaseLazyStaggeredGridWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "orientation: {0}")
        fun initParameters(): Array<Any> = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }

    @Test
    fun testLookaheadPositionWithOnlyInBoundChanges() {
        testLookaheadPositionWithPlacementAnimator(
            initialList = listOf(0, 1, 2, 3),
            targetList = listOf(3, 2, 1, 0),
            lanes = 1,
            initialExpectedLookaheadPositions =
                if (vertical) {
                    listOf(IntOffset(0, 0), IntOffset(0, 100), IntOffset(0, 200), IntOffset(0, 300))
                } else {
                    listOf(IntOffset(0, 0), IntOffset(100, 0), IntOffset(200, 0), IntOffset(300, 0))
                },
            targetExpectedLookaheadPositions =
                if (vertical) {
                    listOf(IntOffset(0, 300), IntOffset(0, 200), IntOffset(0, 100), IntOffset(0, 0))
                } else {
                    listOf(IntOffset(300, 0), IntOffset(200, 0), IntOffset(100, 0), IntOffset(0, 0))
                },
        )
    }

    @Test
    fun testLookaheadPositionWithCustomStartingIndex() {
        testLookaheadPositionWithPlacementAnimator(
            initialList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            targetList = listOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
            lanes = 2,
            initialExpectedLookaheadPositions =
                if (vertical) {
                    listOf(
                        null,
                        null,
                        IntOffset(0, 0),
                        IntOffset(100, 0),
                        IntOffset(0, 100),
                        IntOffset(100, 100),
                        IntOffset(0, 200),
                        IntOffset(100, 200),
                        // For items outside the view port *before* the visible items, we only have
                        // a contract for their mainAxis position. The crossAxis position for those
                        // items is subject to change.
                        IntOffset(UnspecifiedOffset, 300),
                        IntOffset(UnspecifiedOffset, 300),
                    )
                } else {
                    listOf(
                        null,
                        null,
                        IntOffset(0, 0),
                        IntOffset(0, 100),
                        IntOffset(100, 0),
                        IntOffset(100, 100),
                        IntOffset(200, 0),
                        IntOffset(200, 100),
                        // For items outside the view port *before* the visible items, we only have
                        // a contract for their mainAxis position. The crossAxis position for those
                        // items is subject to change.
                        IntOffset(300, UnspecifiedOffset),
                        IntOffset(300, UnspecifiedOffset),
                    )
                },
            targetExpectedLookaheadPositions =
                if (vertical) {
                    listOf(
                        IntOffset(100, 300),
                        IntOffset(0, 300),
                        IntOffset(100, 200),
                        IntOffset(0, 200),
                        IntOffset(100, 100),
                        IntOffset(0, 100),
                        IntOffset(100, 0),
                        IntOffset(0, 0),
                        IntOffset(0, -100),
                        IntOffset(100, -100),
                    )
                } else {
                    listOf(
                        IntOffset(300, 100),
                        IntOffset(300, 0),
                        IntOffset(200, 100),
                        IntOffset(200, 0),
                        IntOffset(100, 100),
                        IntOffset(100, 0),
                        IntOffset(0, 100),
                        IntOffset(0, 0),
                        IntOffset(-100, 0),
                        IntOffset(-100, 100),
                    )
                },
            startingIndex = 2,
            crossAxisSize = 200,
        )
    }

    @Test
    fun testLookaheadPositionWithTwoInBoundTwoOutBound() {
        testLookaheadPositionWithPlacementAnimator(
            initialList = listOf(0, 1, 2, 3, 4, 5),
            targetList = listOf(5, 4, 2, 1, 3, 0),
            initialExpectedLookaheadPositions =
                if (vertical) {
                    listOf(
                        null,
                        null,
                        IntOffset(0, 0),
                        IntOffset(0, 100),
                        IntOffset(0, 200),
                        IntOffset(0, 300),
                    )
                } else {
                    listOf(
                        null,
                        null,
                        IntOffset(0, 0),
                        IntOffset(100, 0),
                        IntOffset(200, 0),
                        IntOffset(300, 0),
                    )
                },
            targetExpectedLookaheadPositions =
                if (vertical) {
                    listOf(
                        IntOffset(0, 300),
                        IntOffset(0, 100),
                        IntOffset(0, 0),
                        IntOffset(0, 200),
                        IntOffset(0, -100),
                        IntOffset(0, -200),
                    )
                } else {
                    listOf(
                        IntOffset(300, 0),
                        IntOffset(100, 0),
                        IntOffset(0, 0),
                        IntOffset(200, 0),
                        IntOffset(-100, 0),
                        IntOffset(-200, 0),
                    )
                },
            startingIndex = 2,
        )
    }

    @Test
    fun testLookaheadPositionWithFourInBoundFourOutBound() {
        testLookaheadPositionWithPlacementAnimator(
            initialList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            targetList = listOf(8, 9, 7, 6, 4, 5, 2, 1, 3, 0),
            initialExpectedLookaheadPositions =
                if (vertical) {
                    listOf(
                        null,
                        null,
                        null,
                        null,
                        IntOffset(0, 0),
                        IntOffset(100, 0),
                        IntOffset(0, 100),
                        IntOffset(100, 100),
                        IntOffset(0, 200),
                        IntOffset(100, 200),
                    )
                } else {
                    listOf(
                        null,
                        null,
                        null,
                        null,
                        IntOffset(0, 0),
                        IntOffset(0, 100),
                        IntOffset(100, 0),
                        IntOffset(100, 100),
                        IntOffset(200, 0),
                        IntOffset(200, 100),
                    )
                },
            targetExpectedLookaheadPositions =
                if (vertical) {
                    listOf(
                        IntOffset(100, 200),
                        IntOffset(100, 100),
                        IntOffset(0, 100),
                        IntOffset(0, 200),
                        IntOffset(0, 0),
                        IntOffset(100, 0),
                        // For items outside the view port *before* the visible items, we only have
                        // a contract for their mainAxis position. The crossAxis position for those
                        // items is subject to change.
                        IntOffset(UnspecifiedOffset, -100),
                        IntOffset(UnspecifiedOffset, -100),
                        IntOffset(UnspecifiedOffset, -200),
                        IntOffset(UnspecifiedOffset, -200),
                    )
                } else {
                    listOf(
                        IntOffset(200, 100),
                        IntOffset(100, 100),
                        IntOffset(100, 0),
                        IntOffset(200, 0),
                        IntOffset(0, 0),
                        IntOffset(0, 100),
                        // For items outside the view port *before* the visible items, we only have
                        // a contract for their mainAxis position. The crossAxis position for those
                        // items is subject to change.
                        IntOffset(-100, UnspecifiedOffset),
                        IntOffset(-100, UnspecifiedOffset),
                        IntOffset(-200, UnspecifiedOffset),
                        IntOffset(-200, UnspecifiedOffset),
                    )
                },
            startingIndex = 4,
            lanes = 2,
            crossAxisSize = 200,
        )
    }

    private fun testLookaheadPositionWithPlacementAnimator(
        initialList: List<Int>,
        targetList: List<Int>,
        lanes: Int = 1,
        initialExpectedLookaheadPositions: List<IntOffset?>,
        targetExpectedLookaheadPositions: List<IntOffset?>,
        startingIndex: Int = 0,
        crossAxisSize: Int? = null,
    ) {
        val itemSize = 100
        var list by mutableStateOf(initialList)
        val lookaheadPosition = mutableMapOf<Int, IntOffset>()
        val approachPosition = mutableMapOf<Int, IntOffset>()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LazyStaggeredGridInLookaheadScope(
                    list = list,
                    lanes = lanes,
                    startingIndex = startingIndex,
                    lookaheadPosition = lookaheadPosition,
                    approachPosition = approachPosition,
                    itemSize = itemSize,
                    crossAxisSize = crossAxisSize,
                )
            }
        }
        rule.runOnIdle {
            repeat(list.size) {
                assertOffsetEquals(initialExpectedLookaheadPositions[it], lookaheadPosition[it])
                assertOffsetEquals(initialExpectedLookaheadPositions[it], approachPosition[it])
            }
            lookaheadPosition.clear()
            approachPosition.clear()
            list = targetList
        }
        rule.waitForIdle()
        repeat(20) {
            rule.mainClock.advanceTimeByFrame()
            repeat(list.size) {
                assertOffsetEquals(targetExpectedLookaheadPositions[it], lookaheadPosition[it])
            }
        }
        repeat(list.size) {
            if (
                lookaheadPosition[it]?.let { offset ->
                    (if (vertical) offset.y else offset.x) + itemSize >= 0
                } != false
            ) {
                assertOffsetEquals(lookaheadPosition[it], approachPosition[it])
            }
        }
    }

    private fun assertOffsetEquals(expected: IntOffset?, actual: IntOffset?) {
        if (expected == null || actual == null) return assertEquals(expected, actual)
        if (expected.x == UnspecifiedOffset || actual.x == UnspecifiedOffset) {
            // Only compare y offset
            assertEquals(expected.y, actual.y)
        } else if (expected.y == UnspecifiedOffset || actual.y == UnspecifiedOffset) {
            assertEquals(expected.x, actual.x)
        } else {
            assertEquals(expected, actual)
        }
    }

    @Composable
    private fun LazyStaggeredGridInLookaheadScope(
        list: List<Int>,
        lanes: Int,
        startingIndex: Int,
        lookaheadPosition: MutableMap<Int, IntOffset>,
        approachPosition: MutableMap<Int, IntOffset>,
        itemSize: Int,
        crossAxisSize: Int? = null,
    ) {
        LookaheadScope {
            LazyStaggeredGrid(
                lanes = lanes,
                if (vertical) {
                    Modifier.requiredHeight(itemSize.dp * (list.size - startingIndex) / lanes)
                        .then(
                            if (crossAxisSize != null) Modifier.requiredWidth(crossAxisSize.dp)
                            else Modifier
                        )
                } else {
                    Modifier.requiredWidth(itemSize.dp * (list.size - startingIndex) / lanes)
                        .then(
                            if (crossAxisSize != null) Modifier.requiredHeight(crossAxisSize.dp)
                            else Modifier
                        )
                },
                state = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = startingIndex),
            ) {
                items(list, key = { it }) { item ->
                    Box(
                        Modifier.animateItem(
                                fadeInSpec = null,
                                fadeOutSpec = null,
                                placementSpec = tween<IntOffset>(160),
                            )
                            .trackPositions(
                                lookaheadPosition,
                                approachPosition,
                                this@LookaheadScope,
                                item,
                            )
                            .requiredSize(itemSize.dp)
                    )
                }
            }
        }
    }

    private fun Modifier.trackPositions(
        lookaheadPosition: MutableMap<Int, IntOffset>,
        approachPosition: MutableMap<Int, IntOffset>,
        lookaheadScope: LookaheadScope,
        item: Int,
    ): Modifier =
        this.layout { measurable, constraints ->
            measurable.measure(constraints).run {
                layout(width, height) {
                    if (isLookingAhead) {
                        lookaheadPosition[item] =
                            with(lookaheadScope) {
                                coordinates!!
                                    .findRootCoordinates()
                                    .localLookaheadPositionOf(coordinates!!)
                                    .round()
                            }
                    } else {
                        approachPosition[item] = coordinates!!.positionInRoot().round()
                    }
                    place(0, 0)
                }
            }
        }

    @Test
    fun animContentSizeWithPlacementAnimator() {
        val itemSize = 100
        val lookaheadPosition = mutableMapOf<Int, IntOffset>()
        val approachPosition = mutableMapOf<Int, IntOffset>()
        var large by mutableStateOf(false)
        var animateSizeChange by mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    LazyStaggeredGrid(
                        lanes = 2,
                        if (vertical) // Define cross axis size
                         Modifier.requiredWidth(200.dp)
                        else Modifier.requiredHeight(200.dp),
                    ) {
                        items(8, key = { it }) {
                            Box(
                                Modifier.animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null,
                                        placementSpec = tween(160, easing = LinearEasing),
                                    )
                                    .trackPositions(
                                        lookaheadPosition,
                                        approachPosition,
                                        this@LookaheadScope,
                                        it,
                                    )
                                    .then(
                                        if (animateSizeChange)
                                            Modifier.animateContentSize(tween(160))
                                        else Modifier
                                    )
                                    .requiredSize(if (large) itemSize.dp * 2 else itemSize.dp)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        repeat(8) {
            if (vertical) {
                assertEquals(it / 2 * itemSize, lookaheadPosition[it]?.y)
                assertEquals(it / 2 * itemSize, approachPosition[it]?.y)
                assertEquals(it % 2 * 100, lookaheadPosition[it]?.x)
                assertEquals(it % 2 * 100, approachPosition[it]?.x)
            } else {
                assertEquals(it / 2 * itemSize, lookaheadPosition[it]?.x)
                assertEquals(it / 2 * itemSize, approachPosition[it]?.x)
                assertEquals(it % 2 * 100, lookaheadPosition[it]?.y)
                assertEquals(it % 2 * 100, approachPosition[it]?.y)
            }
        }

        rule.mainClock.autoAdvance = false
        large = true
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        repeat(20) { frame ->
            val fraction = (frame * 16 / 160f).coerceAtMost(1f)
            repeat(8) {
                if (vertical) {
                    assertEquals(it / 2 * itemSize * 2, lookaheadPosition[it]?.y)
                    assertEquals(
                        (it / 2 * itemSize * (1 + fraction)).roundToInt(),
                        approachPosition[it]?.y,
                    )
                } else {
                    assertEquals(it / 2 * itemSize * 2, lookaheadPosition[it]?.x)
                    assertEquals(
                        (it / 2 * itemSize * (1 + fraction)).roundToInt(),
                        approachPosition[it]?.x,
                    )
                }
            }
            rule.mainClock.advanceTimeByFrame()
        }

        // Enable animateContentSize
        animateSizeChange = true
        large = false
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        repeat(20) { frame ->
            val fraction = (frame * 16 / 160f).coerceAtMost(1f)
            repeat(4) {
                // Verify that item target offsets are not affected by animateContentSize
                if (vertical) {
                    assertEquals(it / 2 * itemSize, lookaheadPosition[it]?.y)
                    assertEquals(
                        (it / 2 * (2 - fraction) * itemSize).roundToInt(),
                        approachPosition[it]?.y,
                    )
                } else {
                    assertEquals(it / 2 * itemSize, lookaheadPosition[it]?.x)
                    assertEquals(
                        (it / 2 * (2 - fraction) * itemSize).roundToInt(),
                        approachPosition[it]?.x,
                    )
                }
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun animVisibilityWithPlacementAnimator() {
        val lookaheadPosition = mutableMapOf<Int, IntOffset>()
        val approachPosition = mutableMapOf<Int, IntOffset>()
        var visible by mutableStateOf(false)
        val itemSize = 100
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    LazyStaggeredGrid(lanes = 1) {
                        items(4, key = { it }) {
                            if (vertical) {
                                Column(
                                    Modifier.animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null,
                                            placementSpec = tween(160, easing = LinearEasing),
                                        )
                                        .trackPositions(
                                            lookaheadPosition,
                                            approachPosition,
                                            this@LookaheadScope,
                                            it,
                                        )
                                ) {
                                    Box(Modifier.requiredSize(itemSize.dp))
                                    AnimatedVisibility(visible = visible) {
                                        Box(Modifier.requiredSize(itemSize.dp))
                                    }
                                }
                            } else {
                                Row(
                                    Modifier.animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = null,
                                            placementSpec = tween(160, easing = LinearEasing),
                                        )
                                        .trackPositions(
                                            lookaheadPosition,
                                            approachPosition,
                                            this@LookaheadScope,
                                            it,
                                        )
                                ) {
                                    Box(Modifier.requiredSize(itemSize.dp))
                                    AnimatedVisibility(visible = visible) {
                                        Box(Modifier.requiredSize(itemSize.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        repeat(4) {
            assertEquals(it * itemSize, lookaheadPosition[it]?.mainAxisPosition)
            assertEquals(it * itemSize, approachPosition[it]?.mainAxisPosition)
        }

        rule.mainClock.autoAdvance = false
        visible = true
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        repeat(20) { frame ->
            val fraction = (frame * 16 / 160f).coerceAtMost(1f)
            repeat(4) {
                assertEquals(it * itemSize * 2, lookaheadPosition[it]?.mainAxisPosition)
                assertEquals(
                    (it * itemSize * (1 + fraction)).roundToInt(),
                    approachPosition[it]?.mainAxisPosition,
                )
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun resizeLazyStaggeredGridOnlyDuringApproach() {
        val itemSize = 100
        val lookaheadPositions = mutableMapOf<Int, Offset>()
        val approachPositions = mutableMapOf<Int, Offset>()
        var approachSize by mutableStateOf(itemSize * 2)
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    LazyStaggeredGrid(
                        lanes = 2,
                        Modifier.layout { measurable, _ ->
                            val constraints =
                                if (isLookingAhead) {
                                    Constraints.fixed(4 * itemSize, 4 * itemSize)
                                } else {
                                    Constraints.fixed(approachSize, approachSize)
                                }
                            measurable.measure(constraints).run {
                                layout(width, height) { place(0, 0) }
                            }
                        },
                    ) {
                        items(8) {
                            Box(
                                Modifier.requiredSize(itemSize.dp).layout { measurable, constraints
                                    ->
                                    measurable.measure(constraints).run {
                                        layout(width, height) {
                                            if (isLookingAhead) {
                                                lookaheadPositions[it] =
                                                    coordinates!!
                                                        .findRootCoordinates()
                                                        .localLookaheadPositionOf(coordinates!!)
                                            } else {
                                                approachPositions[it] =
                                                    coordinates!!.positionInRoot()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            repeat(8) {
                assertEquals((it / 2) * itemSize, lookaheadPositions[it]?.mainAxisPosition)
            }
            assertEquals(0, approachPositions[0]?.mainAxisPosition)
            assertEquals(0, approachPositions[1]?.mainAxisPosition)
            assertEquals(itemSize, approachPositions[2]?.mainAxisPosition)
            assertEquals(itemSize, approachPositions[3]?.mainAxisPosition)
            assertEquals(null, approachPositions[4]?.mainAxisPosition)
            assertEquals(null, approachPositions[5]?.mainAxisPosition)
            assertEquals(null, approachPositions[6]?.mainAxisPosition)
            assertEquals(null, approachPositions[7]?.mainAxisPosition)
        }
        approachSize = (2.9f * itemSize).toInt()
        rule.runOnIdle {
            repeat(8) { assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition) }
            assertEquals(0, approachPositions[0]?.mainAxisPosition)
            assertEquals(0, approachPositions[1]?.mainAxisPosition)
            assertEquals(itemSize, approachPositions[2]?.mainAxisPosition)
            assertEquals(itemSize, approachPositions[3]?.mainAxisPosition)
            assertEquals(itemSize * 2, approachPositions[4]?.mainAxisPosition)
            assertEquals(itemSize * 2, approachPositions[5]?.mainAxisPosition)
            assertEquals(null, approachPositions[6]?.mainAxisPosition)
            assertEquals(null, approachPositions[7]?.mainAxisPosition)
        }
        approachSize = (3.4f * itemSize).toInt()
        rule.runOnIdle {
            repeat(8) {
                assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
            }
        }

        // Shrinking approach size
        approachSize = (2.7f * itemSize).toInt()
        approachPositions.clear()
        rule.runOnIdle {
            repeat(8) {
                assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                if (it < 6) {
                    assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
                } else {
                    assertEquals(null, approachPositions[it]?.mainAxisPosition)
                }
            }
        }

        // Shrinking approach size
        approachSize = (1.2f * itemSize).toInt()
        approachPositions.clear()
        rule.runOnIdle {
            repeat(8) {
                assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
                } else {
                    assertEquals(null, approachPositions[it]?.mainAxisPosition)
                }
            }
        }
    }

    @Test
    fun lookaheadSizeSmallerThanPostLookahead() {
        val itemSize = 100
        val lookaheadPositions = mutableMapOf<Int, Offset>()
        val approachPositions = mutableMapOf<Int, Offset>()
        val lookaheadSize by mutableStateOf(itemSize * 2)
        var approachSize by mutableStateOf(itemSize * 4)
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    LazyStaggeredGrid(
                        lanes = 2,
                        Modifier.layout { measurable, _ ->
                            val constraints =
                                if (isLookingAhead) {
                                    Constraints.fixed(lookaheadSize, lookaheadSize)
                                } else {
                                    Constraints.fixed(approachSize, approachSize)
                                }
                            measurable.measure(constraints).run {
                                layout(width, height) { place(0, 0) }
                            }
                        },
                    ) {
                        items(8) {
                            Box(
                                Modifier.requiredSize(itemSize.dp).layout { measurable, constraints
                                    ->
                                    measurable.measure(constraints).run {
                                        layout(width, height) {
                                            if (isLookingAhead) {
                                                lookaheadPositions[it] =
                                                    coordinates!!
                                                        .findRootCoordinates()
                                                        .localLookaheadPositionOf(coordinates!!)
                                            } else {
                                                approachPositions[it] =
                                                    coordinates!!.positionInRoot()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        // approachSize was initialized to 4 * ItemSize
        rule.runOnIdle {
            repeat(8) {
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                } else {
                    assertTrue(lookaheadPositions[it]?.mainAxisPosition!! >= it / 2 * itemSize)
                }
                assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
            }
        }
        approachSize = (2.9f * itemSize).toInt()
        approachPositions.clear()
        rule.runOnIdle {
            repeat(8) {
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                } else {
                    assertTrue(lookaheadPositions[it]?.mainAxisPosition!! >= it / 2 * itemSize)
                }
                if (it < 6) {
                    assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
                } else {
                    assertEquals(null, approachPositions[it]?.mainAxisPosition)
                }
            }
        }
        approachSize = 2 * itemSize
        approachPositions.clear()
        rule.runOnIdle {
            repeat(8) {
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                    assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
                } else {
                    assertTrue(lookaheadPositions[it]?.mainAxisPosition!! >= it / 2 * itemSize)
                    assertEquals(null, approachPositions[it]?.mainAxisPosition)
                }
            }
        }

        // Growing approach size
        approachSize = (2.7f * itemSize).toInt()
        approachPositions.clear()
        rule.runOnIdle {
            repeat(8) {
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                } else {
                    assertTrue(lookaheadPositions[it]?.mainAxisPosition!! >= it / 2 * itemSize)
                }
                if (it < 6) {
                    assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
                } else {
                    assertEquals(null, approachPositions[it]?.mainAxisPosition)
                }
            }
        }

        // Shrinking approach size
        approachSize = (1.2f * itemSize).toInt()
        approachPositions.clear()
        rule.runOnIdle {
            repeat(8) {
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, lookaheadPositions[it]?.mainAxisPosition)
                } else {
                    assertTrue(lookaheadPositions[it]?.mainAxisPosition!! >= it / 2 * itemSize)
                }
                if (it < 4) {
                    assertEquals(it / 2 * itemSize, approachPositions[it]?.mainAxisPosition)
                } else {
                    assertEquals(null, approachPositions[it]?.mainAxisPosition)
                }
            }
        }
    }

    @Test
    fun approachItemsComposed() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    LazyStaggeredGrid(lanes = 2, Modifier.requiredSize(300.dp)) {
                        items(24, key = { it }) {
                            Box(
                                Modifier.testTag("$it")
                                    .then(
                                        if (it == 0) {
                                            Modifier.layout { measurable, constraints ->
                                                val p = measurable.measure(constraints)
                                                val size = if (isLookingAhead) 300 else 30
                                                layout(size, size) { p.place(0, 0) }
                                            }
                                        } else Modifier.size(30.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        // Based on lookahead item 0 & 1 would be the only item needed, but approach calculation
        // indicates 10 items will be needed to fill the viewport.
        for (i in 0 until 20) {
            rule.onNodeWithTag("$i").assertIsPlaced()
        }
        for (i in 20 until 24) {
            rule.onNodeWithTag("$i").assertDoesNotExist()
        }
    }

    @Test
    fun approachItemsComposedBasedOnScrollDelta() {
        var lookaheadSize by mutableStateOf(30)
        var approachSize by mutableStateOf(lookaheadSize)
        lateinit var state: LazyStaggeredGridState
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    state = LazyStaggeredGridState()
                    LazyStaggeredGrid(lanes = 2, Modifier.requiredSize(300.dp), state) {
                        items(24, key = { it }) {
                            Box(
                                Modifier.testTag("$it")
                                    .then(
                                        if (it == 4) {
                                            Modifier.layout { measurable, constraints ->
                                                val p = measurable.measure(constraints)
                                                val size =
                                                    if (isLookingAhead) lookaheadSize
                                                    else approachSize
                                                layout(size, size) { p.place(0, 0) }
                                            }
                                        } else Modifier.size(30.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()

        for (i in 0 until 24) {
            if (i < 20) {
                rule.onNodeWithTag("$i").assertIsPlaced()
            } else {
                rule.onNodeWithTag("$i").assertDoesNotExist()
            }
        }

        lookaheadSize = 300
        rule.runOnIdle { runBlocking { state.scrollBy(60f) } }
        rule.waitForIdle()

        rule.onNodeWithTag("0").assertIsNotPlaced()
        rule.onNodeWithTag("1").assertIsNotPlaced()
        rule.onNodeWithTag("2").assertIsNotPlaced()
        rule.onNodeWithTag("3").assertIsNotPlaced()
        for (i in 4 until 24) {
            rule.onNodeWithTag("$i").assertIsPlaced()
        }

        approachSize = 300
        rule.waitForIdle()
        for (i in 0 until 24) {
            if (i in 4..14) {
                rule.onNodeWithTag("$i").assertIsPlaced()
            } else {
                rule.onNodeWithTag("$i").assertIsNotPlaced()
            }
        }
    }

    @Test
    fun testDisposeHappensAfterNoLongerNeededByEitherPass() {

        val disposed = mutableListOf<Boolean>().apply { repeat(20) { this.add(false) } }
        var lookaheadHeight by mutableIntStateOf(1000)
        var approachHeight by mutableIntStateOf(1000)
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    LazyVerticalStaggeredGrid(
                        StaggeredGridCells.Fixed(2),
                        Modifier.layout { m, _ ->
                            val c =
                                if (isLookingAhead) Constraints.fixed(400, lookaheadHeight)
                                else Constraints.fixed(400, approachHeight)
                            m.measure(c).run { layout(width, lookaheadHeight) { place(0, 0) } }
                        },
                    ) {
                        items(20) {
                            Box(Modifier.height(100.dp).fillMaxWidth())
                            DisposableEffect(Unit) { onDispose { disposed[it] = true } }
                        }
                    }
                }
            }
        }
        rule.runOnIdle { repeat(20) { assertEquals(false, disposed[it]) } }
        approachHeight = 400
        rule.waitForIdle()
        lookaheadHeight = 400

        rule.runOnIdle {
            repeat(20) {
                if (it < 8) {
                    assertEquals(false, disposed[it])
                } else {
                    assertEquals(true, disposed[it])
                }
            }
        }
        lookaheadHeight = 300

        rule.runOnIdle { repeat(8) { assertEquals(false, disposed[it]) } }
    }

    @Test
    fun testNoOverScrollWhenSpecified() {
        val state = LazyStaggeredGridState()
        var firstItemOffset: Offset? = null
        var lastItemOffset: Offset? = null
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    CompositionLocalProvider(LocalOverscrollFactory provides null) {
                        Box(Modifier.testTag("grid")) {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                state = state,
                                modifier = Modifier.requiredHeight(500.dp).fillMaxWidth(),
                            ) {
                                items(30) {
                                    BasicText(
                                        "$it",
                                        Modifier.then(
                                                if (it == 0 || it == 29)
                                                    Modifier.onGloballyPositioned { c ->
                                                        // Checking on each placement there's no
                                                        // overscroll
                                                        if (it == 0) {
                                                            firstItemOffset = c.positionInRoot()
                                                            assertTrue(firstItemOffset!!.y <= 0f)
                                                        } else {
                                                            lastItemOffset = c.positionInRoot()
                                                            assertTrue(lastItemOffset!!.y >= 400f)
                                                        }
                                                    }
                                                else Modifier
                                            )
                                            .height(100.dp)
                                            .animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scroll beyond bounds in both directions
        repeat(20) {
            rule.runOnIdle { runBlocking { state.scrollBy(200f) } }
            if (it == 19) {
                assertEquals(20, state.firstVisibleItemIndex)
                rule.runOnIdle { runBlocking { state.scrollToItem(14) } }
                rule.onNodeWithTag("grid").performTouchInput { swipeUp(durationMillis = 50) }
            }
            // Checking on each iteration there is no overscroll
            assertTrue(firstItemOffset == null || firstItemOffset!!.y <= 0)
            assertTrue(lastItemOffset == null || lastItemOffset!!.y >= 400)
        }

        repeat(20) {
            rule.runOnIdle { runBlocking { state.scrollBy(-200f) } }
            if (it == 19) {
                assertEquals(0, state.firstVisibleItemIndex)
                rule.runOnIdle { runBlocking { state.scrollToItem(7) } }
                rule.onNodeWithTag("grid").performTouchInput { swipeDown(durationMillis = 50) }
            }
            // Checking on each iteration there is no overscroll
            assertTrue(firstItemOffset == null || firstItemOffset!!.y <= 0)
            assertTrue(lastItemOffset == null || lastItemOffset!!.y >= 400)
        }
    }

    @Test
    fun testSmallScrollWithLookaheadScope() {
        val itemSize = 10
        val itemSizeDp = with(rule.density) { itemSize.toDp() }
        val containerSizeDp = with(rule.density) { 15.toDp() }
        val scrollDelta = 2f
        val scrollDeltaDp = with(rule.density) { scrollDelta.toDp() }
        val state = LazyStaggeredGridState()
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            LookaheadScope {
                LazyStaggeredGrid(
                    lanes = 1,
                    Modifier.mainAxisSize(containerSizeDp),
                    state = state,
                ) {
                    repeat(20) { item { Box(Modifier.size(itemSizeDp).testTag("$it")) } }
                }
            }
        }

        rule.runOnIdle { runBlocking { scope.launch { state.scrollBy(scrollDelta) } } }

        rule.onNodeWithTag("0").assertMainAxisStartPositionInRootIsEqualTo(-scrollDeltaDp)
        rule
            .onNodeWithTag("1")
            .assertMainAxisStartPositionInRootIsEqualTo(itemSizeDp - scrollDeltaDp)
    }

    private val Offset.mainAxisPosition: Int
        get() = (if (vertical) this.y else this.x).roundToInt()

    private val IntOffset.mainAxisPosition: Int
        get() = if (vertical) this.y else this.x
}

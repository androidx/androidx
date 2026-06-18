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

package androidx.compose.foundation.layout

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFlexBoxApi::class)
@MediumTest
@RunWith(Parameterized::class)
class FlexBoxDirectionTest(private val directionName: String) {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val direction: FlexDirection
        get() =
            when (directionName) {
                "Row" -> FlexDirection.Row
                "Column" -> FlexDirection.Column
                else -> error("Unknown direction: $directionName")
            }

    private val reverseDirection: FlexDirection
        get() =
            when (directionName) {
                "Row" -> FlexDirection.RowReverse
                "Column" -> FlexDirection.ColumnReverse
                else -> error("Unknown direction: $directionName")
            }

    /** Selects the main-axis coordinate based on the parameterized direction. */
    private val mainAxis: (Offset) -> Float
        get() = if (direction == FlexDirection.Row) Offset::x else Offset::y

    /** Selects the cross-axis coordinate based on the parameterized direction. */
    private val crossAxis: (Offset) -> Float
        get() = if (direction == FlexDirection.Row) Offset::y else Offset::x

    /** Selects main-axis size from an IntSize-like pair. */
    private fun mainSize(width: Int, height: Int): Int =
        if (direction == FlexDirection.Row) width else height

    /** Selects cross-axis size from an IntSize-like pair. */
    private fun crossSize(width: Int, height: Int): Int =
        if (direction == FlexDirection.Row) height else width

    /** fillMaxSize on the main axis only. */
    private fun Modifier.fillMaxMainAxis(): Modifier =
        if (direction == FlexDirection.Row) fillMaxWidth() else fillMaxHeight()

    /** Creates a Box with [mainAxisSize] on the main axis and [crossAxisSize] on the cross axis. */
    private fun Modifier.directionSize(mainAxisSize: Dp, crossAxisSize: Dp): Modifier =
        if (direction == FlexDirection.Row) size(mainAxisSize, crossAxisSize)
        else size(crossAxisSize, mainAxisSize)

    /** Sets main-axis dimension only. */
    private fun Modifier.mainAxisSize(size: Dp): Modifier =
        if (direction == FlexDirection.Row) width(size) else height(size)

    /** Sets cross-axis dimension only. */
    private fun Modifier.crossAxisSize(size: Dp): Modifier =
        if (direction == FlexDirection.Row) height(size) else width(size)

    /** Sets minimum main-axis dimension. */
    private fun Modifier.mainAxisSizeMin(size: Dp): Modifier =
        if (direction == FlexDirection.Row) widthIn(min = size) else heightIn(min = size)

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters(): Collection<Array<Any>> = listOf(arrayOf("Row"), arrayOf("Column"))

        private val NoOpDensity =
            object : Density {
                override val density: Float = 1f
                override val fontScale: Float = 1f
            }
    }

    @Test
    fun defaults_noConfig_wrapsContent() {
        var mainSizeResult = 0
        var crossSizeResult = 0
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                mainSizeResult = mainSize(it.width, it.height)
                                crossSizeResult = crossSize(it.width, it.height)
                            },
                        config = { direction(direction) },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.toDp()).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(0f, 20f, 40f).inOrder()
        Truth.assertThat(mainSizeResult).isEqualTo(60)
        Truth.assertThat(crossSizeResult).isEqualTo(20)
    }

    @Test
    fun justifyContent_start() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.Start)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(0f, 20f, 40f).inOrder()
    }

    @Test
    fun justifyContent_end() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.End)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Items packed at end: 200 - 60 = 140 offset
        Truth.assertThat(mainPositions).containsExactly(140f, 160f, 180f).inOrder()
    }

    @Test
    fun justifyContent_center() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.Center)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Centered: (200 - 60) / 2 = 70 offset
        Truth.assertThat(mainPositions).containsExactly(70f, 90f, 110f).inOrder()
    }

    @Test
    fun justifyContent_spaceBetween() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Between: 140 / 2 gaps = 70 each
        Truth.assertThat(mainPositions).containsExactly(0f, 90f, 180f).inOrder()
    }

    @Test
    fun justifyContent_spaceAround() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceAround)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Around: 140 / 3 ≈ 46.67, half ≈ 23
        Truth.assertThat(mainPositions).containsExactly(23.0f, 89.0f, 155.0f).inOrder()
    }

    @Test
    fun justifyContent_spaceEvenly() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceEvenly)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Evenly: 140 / 4 slots = 35 each
        Truth.assertThat(mainPositions).containsExactly(35f, 90f, 145f).inOrder()
    }

    @Test
    fun justifyContent_start_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.Start)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(180f, 160f, 140f).inOrder()
    }

    @Test
    fun justifyContent_end_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.End)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(40f, 20f, 0f).inOrder()
    }

    @Test
    fun justifyContent_center_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.Center)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(110f, 90f, 70f).inOrder()
    }

    @Test
    fun justifyContent_spaceBetween_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(180.0f, 90.0f, 0.0f).inOrder()
    }

    @Test
    fun justifyContent_spaceAround_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceAround)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(155.0f, 89.0f, 23.0f).inOrder()
    }

    @Test
    fun justifyContent_spaceEvenly_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceEvenly)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(145f, 90f, 35f).inOrder()
    }

    @Test
    fun alignItems_start_nonUniformSizes() {
        val crossPositions = mutableListOf<Float>()
        val itemCrossSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            alignItems(FlexAlignItems.Start)
                        }
                    ) {
                        itemCrossSizes.forEachIndexed { index, cs ->
                            Box(
                                Modifier.directionSize(20.dp, cs.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(crossPositions).containsExactly(0f, 0f, 0f)
    }

    @Test
    fun alignItems_end_nonUniformSizes() {
        val crossPositions = mutableListOf<Float>()
        val itemCrossSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            alignItems(FlexAlignItems.End)
                        }
                    ) {
                        itemCrossSizes.forEachIndexed { index, cs ->
                            Box(
                                Modifier.directionSize(20.dp, cs.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Line size = 40. Aligned to end: 40-20=20, 40-40=0, 40-30=10
        Truth.assertThat(crossPositions).containsExactly(20f, 0f, 10f)
    }

    @Test
    fun alignItems_center_nonUniformSizes() {
        val crossPositions = mutableListOf<Float>()
        val itemCrossSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            alignItems(FlexAlignItems.Center)
                        }
                    ) {
                        itemCrossSizes.forEachIndexed { index, cs ->
                            Box(
                                Modifier.directionSize(20.dp, cs.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Line size = 40. Centered: (40-20)/2=10, (40-40)/2=0, (40-30)/2=5
        Truth.assertThat(crossPositions).containsExactly(10f, 0f, 5f)
    }

    @Test
    fun alignItems_stretch() {
        val crossSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            alignItems(FlexAlignItems.Stretch)
                        }
                    ) {
                        // This item decides the line cross-axis size
                        Box(Modifier.directionSize(20.dp, 40.dp))
                        repeat(2) { index ->
                            Box(
                                Modifier.mainAxisSize(20.dp)
                                    // No cross-axis size — should stretch
                                    .onSizeChanged {
                                        crossSizes.add(index, crossSize(it.width, it.height))
                                    }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(crossSizes).containsExactly(40, 40)
    }

    @Test
    fun alignItems_end_fixedCrossAxis() {
        val crossPositions = mutableListOf<Float>()
        val itemCrossSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.size(200.dp),
                    config = {
                        direction(direction)
                        alignItems(FlexAlignItems.End)
                    },
                ) {
                    itemCrossSizes.forEachIndexed { index, cs ->
                        Box(
                            Modifier.directionSize(20.dp, cs.dp).onPlaced {
                                crossPositions.add(index, crossAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Cross-axis is 200. Aligned to end: 200-20=180, 200-40=160, 200-30=170
        Truth.assertThat(crossPositions).containsExactly(180f, 160f, 170f)
    }

    @Test
    fun alignItems_center_fixedCrossAxis() {
        val crossPositions = mutableListOf<Float>()
        val itemCrossSizes = listOf(20, 40, 30)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.size(200.dp),
                    config = {
                        direction(direction)
                        alignItems(FlexAlignItems.Center)
                    },
                ) {
                    itemCrossSizes.forEachIndexed { index, cs ->
                        Box(
                            Modifier.directionSize(20.dp, cs.dp).onPlaced {
                                crossPositions.add(index, crossAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Cross-axis is 200. Centered: (200-20)/2=90, (200-40)/2=80, (200-30)/2=85
        Truth.assertThat(crossPositions).containsExactly(90f, 80f, 85f)
    }

    @Test
    fun alignItems_stretch_fixedCrossAxis() {
        val crossSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.size(200.dp),
                    config = {
                        direction(direction)
                        alignItems(FlexAlignItems.Stretch)
                    },
                ) {
                    repeat(2) { index ->
                        Box(
                            Modifier.mainAxisSize(20.dp).onSizeChanged {
                                crossSizes.add(index, crossSize(it.width, it.height))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(crossSizes).containsExactly(200, 200)
    }

    // AlignItems — reverse direction
    @Test
    fun alignItems_end_reverse() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            alignItems(FlexAlignItems.End)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Cross-axis End on fillMaxSize: 200 - 20 = 180
        Truth.assertThat(crossPositions).containsExactly(180f, 180f, 180f)
    }

    @Test
    fun alignItems_center_reverse() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            alignItems(FlexAlignItems.Center)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(crossPositions).containsExactly(90.0f, 90.0f, 90.0f)
    }

    @Test
    fun gap_addsSpacingBetweenItems() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            gap(10.dp)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(0f, 30f, 60f).inOrder()
    }

    @Test
    fun gap_withWrap_appliesCrossAxisGap() {
        var crossAxisResult = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                crossAxisResult = crossSize(it.width, it.height)
                            },
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                            gap(10.dp)
                        },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 3 items per line (20+10+20+10+20=80 ≤ 100), 2 lines
        // Cross-axis = 20 + 10 + 20 = 50
        Truth.assertThat(crossAxisResult).isEqualTo(50)
    }

    @Test
    fun gap_withReverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            gap(10.dp)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(180f, 150f, 120f).inOrder()
    }

    @Test
    fun gap_withSpaceEvenly_reducesDistributedSpace() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceEvenly)
                            gap(10.dp)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // free = 200 - 60 - 20 = 120, slots = 4, each = 30
        Truth.assertThat(mainPositions).containsExactly(30.0f, 90.0f, 150.0f).inOrder()
    }

    @Test
    fun gap_withSpaceAround_reducesDistributedSpace() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceAround)
                            gap(10.dp)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // free = 200 - 60 - 20 = 120, per-item = 40, half = 20
        Truth.assertThat(mainPositions).containsExactly(20.0f, 90.0f, 160.0f).inOrder()
    }

    @Test
    fun gap_withSpaceBetween_subsumedByLargerSpacing() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                            gap(10.dp)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(0.0f, 90.0f, 180.0f).inOrder()
    }

    @Test
    fun gap_withJustifyEnd_positionsCorrectly() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxMainAxis(),
                        config = {
                            direction(direction)
                            gap(10.dp)
                            justifyContent(FlexJustifyContent.End)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Total = 20+10+20+10+20 = 80, remaining = 120
        Truth.assertThat(mainPositions).containsExactly(120f, 150f, 180f).inOrder()
    }

    @Test
    fun wrap_basic() {
        var crossAxisResult = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                crossAxisResult = crossSize(it.width, it.height)
                            },
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                        },
                    ) {
                        repeat(6) { Box(Modifier.size(20.dp)) }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 5 items per line (100/20), 6 items = 2 lines = 40
        Truth.assertThat(crossAxisResult).isEqualTo(40)
    }

    @Test
    fun wrap_excludesTrailingGap() {
        var crossAxisResult = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                crossAxisResult = crossSize(it.width, it.height)
                            },
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                            gap(10.dp)
                        },
                    ) {
                        // 45 + 10 + 45 = 100 fits exactly. Should NOT wrap.
                        Box(Modifier.directionSize(45.dp, 45.dp))
                        Box(Modifier.directionSize(45.dp, 45.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(crossAxisResult).isEqualTo(45)
    }

    @Test
    fun wrap_gapPreservedAfterLineBreak() {
        var crossAxisResult = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                crossAxisResult = crossSize(it.width, it.height)
                            },
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                            gap(15.dp)
                        },
                    ) {
                        Box(Modifier.directionSize(100.dp, 20.dp))
                        Box(Modifier.directionSize(45.dp, 20.dp))
                        Box(Modifier.directionSize(45.dp, 20.dp))
                    }
                }
            }
        }

        rule.waitForIdle()
        // 3 lines: 20 + 15 + 20 + 15 + 20 = 90
        Truth.assertThat(crossAxisResult).isEqualTo(90)
    }

    @Test
    fun wrapReverse() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            wrap(FlexWrap.WrapReverse)
                        }
                    ) {
                        repeat(6) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 5 items per line, 6 items = 2 lines
        // WrapReverse: first line at cross=20, second line at cross=0
        Truth.assertThat(crossPositions.take(5)).containsExactly(20f, 20f, 20f, 20f, 20f)
        Truth.assertThat(crossPositions[5]).isEqualTo(0f)
    }

    @Test
    fun gap_zeroSizeItem_hasGap() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            gap(10.dp)
                        }
                    ) {
                        Box(
                            Modifier.directionSize(0.dp, 20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                        Box(
                            Modifier.directionSize(20.dp, 20.dp).onPlaced {
                                mainPositions.add(1, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Item 1 at 0, Item 2 at 0 + 10 (gap) = 10
        Truth.assertThat(mainPositions).containsExactly(0f, 10f).inOrder()
    }

    @Test
    fun flexGrow() {
        val mainSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxMainAxis(),
                        config = { direction(direction) },
                    ) {
                        Box(
                            Modifier.size(20.dp).onSizeChanged {
                                mainSizes.add(0, mainSize(it.width, it.height))
                            }
                        )
                        Box(
                            Modifier.size(20.dp)
                                .flex { grow(1f) }
                                .onSizeChanged { mainSizes.add(1, mainSize(it.width, it.height)) }
                        )
                        Box(
                            Modifier.size(20.dp)
                                .flex { grow(2f) }
                                .onSizeChanged { mainSizes.add(2, mainSize(it.width, it.height)) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Available: 200 - 60 = 140. grow=1 gets 140/3, grow=2 gets 280/3
        Truth.assertThat(mainSizes[0]).isEqualTo(20)
        Truth.assertThat(mainSizes[1]).isGreaterThan(20)
        Truth.assertThat(mainSizes[2]).isGreaterThan(mainSizes[1])
    }

    @Test
    fun flexShrink() {
        val mainSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier = Modifier.mainAxisSize(100.dp),
                        config = {
                            direction(direction)
                            wrap(FlexWrap.NoWrap)
                        },
                    ) {
                        Box(
                            Modifier.directionSize(60.dp, 20.dp)
                                .flex { shrink(0f) }
                                .onSizeChanged { mainSizes.add(0, mainSize(it.width, it.height)) }
                        )
                        Box(
                            Modifier.mainAxisSizeMin(20.dp)
                                .crossAxisSize(20.dp)
                                .flex { basis(60.dp) }
                                .onSizeChanged { mainSizes.add(1, mainSize(it.width, it.height)) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Total: 120, available: 100, overflow: 20
        // First (shrink=0): stays 60. Second (shrink=1): shrinks to 40
        Truth.assertThat(mainSizes[0]).isEqualTo(60)
        Truth.assertThat(mainSizes[1]).isEqualTo(40)
    }

    @Test
    fun flexBasisDp() {
        val mainSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(config = { direction(direction) }) {
                        Box(
                            Modifier.flex { basis(50.dp) }
                                .crossAxisSize(20.dp)
                                .onSizeChanged { mainSizes.add(0, mainSize(it.width, it.height)) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainSizes[0]).isEqualTo(50)
    }

    @Test
    fun flexBasisPercent() {
        val mainSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxMainAxis(),
                        config = { direction(direction) },
                    ) {
                        Box(
                            Modifier.flex { basis(0.5f) } // 50%
                                .crossAxisSize(20.dp)
                                .onSizeChanged { mainSizes.add(0, mainSize(it.width, it.height)) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainSizes[0]).isEqualTo(100) // 50% of 200
    }

    @Test
    fun alignSelf() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        config = {
                            direction(direction)
                            alignItems(FlexAlignItems.Start)
                        }
                    ) {
                        // Tallest item decides line cross-axis size = 40
                        Box(
                            Modifier.directionSize(20.dp, 40.dp).onPlaced {
                                crossPositions.add(0, crossAxis(it.positionInParent()))
                            }
                        )
                        // alignSelf = End
                        Box(
                            Modifier.size(20.dp)
                                .flex { alignSelf(FlexAlignSelf.End) }
                                .onPlaced {
                                    crossPositions.add(1, crossAxis(it.positionInParent()))
                                }
                        )
                        // alignSelf = Center
                        Box(
                            Modifier.size(20.dp)
                                .flex { alignSelf(FlexAlignSelf.Center) }
                                .onPlaced {
                                    crossPositions.add(2, crossAxis(it.positionInParent()))
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Line cross size = 40
        // Item 0: Start → 0
        // Item 1: End → 40-20 = 20
        // Item 2: Center → (40-20)/2 = 10
        Truth.assertThat(crossPositions).containsExactly(0f, 20f, 10f).inOrder()
    }

    @Test
    fun order() {
        val mainPositions = mutableMapOf<Int, Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(config = { direction(direction) }) {
                        // Item A with order=2
                        Box(
                            Modifier.size(20.dp)
                                .flex { order(2) }
                                .onPlaced { mainPositions[0] = mainAxis(it.positionInParent()) }
                        )
                        // Item B with order=0 (default)
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions[1] = mainAxis(it.positionInParent())
                            }
                        )
                        // Item C with order=1
                        Box(
                            Modifier.size(20.dp)
                                .flex { order(1) }
                                .onPlaced { mainPositions[2] = mainAxis(it.positionInParent()) }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Visual order: B(0), C(1), A(2) → positions 0, 20, 40
        Truth.assertThat(mainPositions[1]).isEqualTo(0f)
        Truth.assertThat(mainPositions[2]).isEqualTo(20f)
        Truth.assertThat(mainPositions[0]).isEqualTo(40f)
    }

    @Test
    fun empty() {
        var mainSizeResult = 0
        var crossSizeResult = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                mainSizeResult = mainSize(it.width, it.height)
                                crossSizeResult = crossSize(it.width, it.height)
                            },
                        config = { direction(direction) },
                    ) {}
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainSizeResult).isEqualTo(0)
        Truth.assertThat(crossSizeResult).isEqualTo(0)
    }

    @Test
    fun singleItem() {
        var mainSizeResult = 0
        var crossSizeResult = 0
        var mainPos = 0f
        var crossPos = 0f

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(100.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                mainSizeResult = mainSize(it.width, it.height)
                                crossSizeResult = crossSize(it.width, it.height)
                            },
                        config = { direction(direction) },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPos = mainAxis(it.positionInParent())
                                crossPos = crossAxis(it.positionInParent())
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainSizeResult).isEqualTo(20)
        Truth.assertThat(crossSizeResult).isEqualTo(20)
        Truth.assertThat(mainPos).isEqualTo(0f)
        Truth.assertThat(crossPos).isEqualTo(0f)
    }

    @Test
    fun spaceBetween_singleItem() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                        },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(0f)
    }

    @Test
    fun spaceBetween_singleItem_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                        },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // In reverse axes, the single item aligns to the flipped main-axis start edge (200 - 20 =
        // 180)
        Truth.assertThat(mainPositions).containsExactly(180f)
    }

    @Test
    fun spaceEvenly_singleItem() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceEvenly)
                        },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(90f)
    }

    @Test
    fun spaceEvenly_singleItem_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceEvenly)
                        },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Symmetrical distribution centers the item at 90f in both forward and reverse flows
        Truth.assertThat(mainPositions).containsExactly(90f)
    }

    @Test
    fun spaceAround_singleItem() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            justifyContent(FlexJustifyContent.SpaceAround)
                        },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(90f)
    }

    @Test
    fun spaceAround_singleItem_reverse() {
        val mainPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceAround)
                        },
                    ) {
                        Box(
                            Modifier.size(20.dp).onPlaced {
                                mainPositions.add(0, mainAxis(it.positionInParent()))
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        // Symmetrical distribution centers the item at 90f in both forward and reverse flows
        Truth.assertThat(mainPositions).containsExactly(90f)
    }

    @Test
    fun alignContent_start() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                            alignContent(FlexAlignContent.Start)
                        },
                    ) {
                        repeat(6) { index ->
                            Box(
                                Modifier.size(50.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 4 items per line (200/50), 2 lines at top
        val uniqueCross = crossPositions.distinct().sorted()
        Truth.assertThat(uniqueCross).containsExactly(0f, 50f).inOrder()
    }

    @Test
    fun alignContent_center() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                            alignContent(FlexAlignContent.Center)
                        },
                    ) {
                        repeat(6) { index ->
                            Box(
                                Modifier.size(50.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 2 lines × 50 = 100. Centered in 200: offset = 50
        val uniqueCross = crossPositions.distinct().sorted()
        Truth.assertThat(uniqueCross).containsExactly(50f, 100f).inOrder()
    }

    @Test
    fun alignContent_spaceBetween() {
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                            alignContent(FlexAlignContent.SpaceBetween)
                        },
                    ) {
                        repeat(6) { index ->
                            Box(
                                Modifier.size(50.dp).onPlaced {
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 2 lines, space between: first at 0, second at 200-50=150
        val uniqueCross = crossPositions.distinct().sorted()
        Truth.assertThat(uniqueCross).containsExactly(0f, 150f).inOrder()
    }

    @Test
    fun overflow_mainAxis_itemOverflows() {
        val itemSize = 50
        val containerSize = 120
        val mainSizes = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.size(containerSize.dp),
                    config = {
                        direction(direction)
                        wrap(FlexWrap.NoWrap)
                    },
                ) {
                    repeat(3) { index ->
                        Box(
                            Modifier.size(itemSize.dp)
                                .flex { shrink(0f) }
                                .onSizeChanged {
                                    mainSizes.add(index, mainSize(it.width, it.height))
                                }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainSizes).containsExactly(itemSize, itemSize, itemSize).inOrder()
    }

    @Test
    fun overflow_crossAxis_itemsClipped() {
        val itemSize = 50
        val containerSize = 120
        val crossSizes = mutableListOf<Int>()
        val expectedCrossSizes = listOf(50, 50, 50, 50, 20, 20)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.size(containerSize.dp),
                    config = {
                        direction(direction)
                        wrap(FlexWrap.Wrap)
                    },
                ) {
                    repeat(6) { index ->
                        Box(
                            Modifier.size(itemSize.dp)
                                .flex { shrink(0f) }
                                .onSizeChanged {
                                    crossSizes.add(index, crossSize(it.width, it.height))
                                }
                        )
                    }
                }
            }
        }

        Truth.assertThat(crossSizes).containsExactlyElementsIn(expectedCrossSizes).inOrder()
    }

    @Test
    fun zeroSizeChildren() {
        var mainSizeResult = 0
        var crossSizeResult = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier =
                        Modifier.onSizeChanged {
                            mainSizeResult = mainSize(it.width, it.height)
                            crossSizeResult = crossSize(it.width, it.height)
                        },
                    config = { direction(direction) },
                ) {
                    repeat(3) { Box(Modifier.size(0.dp)) }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainSizeResult).isEqualTo(0)
        Truth.assertThat(crossSizeResult).isEqualTo(0)
    }

    @Test
    fun manyChildren() {
        var crossAxisResult = 0
        var itemsPlaced = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(1000.dp)) {
                    FlexBox(
                        modifier =
                            Modifier.mainAxisSize(100.dp).onSizeChanged {
                                crossAxisResult = crossSize(it.width, it.height)
                            },
                        config = {
                            direction(direction)
                            wrap(FlexWrap.Wrap)
                        },
                    ) {
                        repeat(100) { Box(Modifier.size(10.dp).onPlaced { itemsPlaced++ }) }
                    }
                }
            }
        }

        rule.waitForIdle()
        // 10 items per line (100/10), 10 lines
        Truth.assertThat(crossAxisResult).isEqualTo(100)
        Truth.assertThat(itemsPlaced).isEqualTo(100)
    }

    @Test
    fun reusableStyle() {
        val mainPositions1 = mutableListOf<Float>()
        val mainPositions2 = mutableListOf<Float>()

        val centeredStyle = FlexBoxConfig {
            direction(direction)
            justifyContent(FlexJustifyContent.Center)
        }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Column {
                    FlexBox(modifier = Modifier.mainAxisSize(100.dp), config = centeredStyle) {
                        repeat(2) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions1.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                    FlexBox(modifier = Modifier.mainAxisSize(100.dp), config = centeredStyle) {
                        repeat(2) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions2.add(index, mainAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        // Centered: (100 - 40) / 2 = 30
        Truth.assertThat(mainPositions1).containsExactly(30f, 50f).inOrder()
        Truth.assertThat(mainPositions2).containsExactly(30f, 50f).inOrder()
    }

    @Test
    fun nestedFlexBox() {
        var outerMainSize = 0
        var innerMainSize = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier =
                        Modifier.onSizeChanged { outerMainSize = mainSize(it.width, it.height) },
                    // Outer is cross-axis direction so children stack on cross axis
                    config = {
                        direction(
                            if (direction == FlexDirection.Row) FlexDirection.Column
                            else FlexDirection.Row
                        )
                    },
                ) {
                    FlexBox(
                        modifier =
                            Modifier.onSizeChanged {
                                innerMainSize = mainSize(it.width, it.height)
                            },
                        config = { direction(direction) },
                    ) {
                        repeat(3) { Box(Modifier.size(20.dp)) }
                    }
                    Box(Modifier.size(100.dp))
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(innerMainSize).isEqualTo(60) // 3 × 20
        Truth.assertThat(outerMainSize).isEqualTo(100) // max of children
    }

    @Test
    fun complexMultiLine() {
        val positions = mutableListOf<Pair<Float, Float>>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                FlexBox(
                    modifier = Modifier.mainAxisSize(100.dp),
                    config = {
                        direction(direction)
                        wrap(FlexWrap.Wrap)
                        justifyContent(FlexJustifyContent.SpaceBetween)
                        alignItems(FlexAlignItems.Center)
                        gap(10.dp)
                    },
                ) {
                    listOf(30, 40, 20, 50, 25).forEachIndexed { index, mainDim ->
                        Box(
                            Modifier.directionSize(mainDim.dp, 20.dp).onPlaced {
                                positions.add(
                                    index,
                                    mainAxis(it.positionInParent()) to
                                        crossAxis(it.positionInParent()),
                                )
                            }
                        )
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(positions).hasSize(5)
    }

    @Test
    fun combined_reverse_center_alignCenter() {
        val mainPositions = mutableListOf<Float>()
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.Center)
                            alignItems(FlexAlignItems.Center)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(110f, 90f, 70f).inOrder()
        Truth.assertThat(crossPositions).containsExactly(90.0f, 90.0f, 90.0f)
    }

    @Test
    fun combined_reverse_spaceBetween_alignEnd() {
        val mainPositions = mutableListOf<Float>()
        val crossPositions = mutableListOf<Float>()

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides NoOpDensity) {
                Box(Modifier.size(200.dp)) {
                    FlexBox(
                        modifier = Modifier.fillMaxSize(),
                        config = {
                            direction(reverseDirection)
                            justifyContent(FlexJustifyContent.SpaceBetween)
                            alignItems(FlexAlignItems.End)
                        },
                    ) {
                        repeat(3) { index ->
                            Box(
                                Modifier.size(20.dp).onPlaced {
                                    mainPositions.add(index, mainAxis(it.positionInParent()))
                                    crossPositions.add(index, crossAxis(it.positionInParent()))
                                }
                            )
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        Truth.assertThat(mainPositions).containsExactly(180.0f, 90.0f, 0.0f).inOrder()
        Truth.assertThat(crossPositions).containsExactly(180f, 180f, 180f)
    }
}

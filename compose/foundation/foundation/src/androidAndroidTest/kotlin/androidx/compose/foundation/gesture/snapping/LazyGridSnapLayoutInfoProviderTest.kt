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

package androidx.compose.foundation.gesture.snapping

import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.BaseLazyGridTestWithOrientation
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.math.sign
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyGridSnapLayoutInfoProviderTest(orientation: Orientation) :
    BaseLazyGridTestWithOrientation(orientation) {

    private val density: Density get() = rule.density

    @Test
    fun snapStepSize_sameSizeItems_shouldBeAverageItemSize() {
        var expectedItemSize = 0f
        var actualItemSize = 0f

        rule.setContent {
            val density = LocalDensity.current
            val state = rememberLazyGridState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                actualItemSize = with(it) { density.calculateSnapStepSize() }
            }
            expectedItemSize = with(density) { FixedItemSize.toPx() }
            MainLayout(
                state = state,
                layoutInfo = layoutInfoProvider,
                items = 200,
                itemSizeProvider = { FixedItemSize }
            )
        }

        rule.runOnIdle {
            assertEquals(round(expectedItemSize), round(actualItemSize))
        }
    }

    @Test
    fun snapStepSize_differentSizeItems_shouldBeAverageItemSizeOnReferenceIndex() {
        var actualItemSize = 0f
        var expectedItemSize = 0f
        rule.setContent {
            val density = LocalDensity.current
            val state = rememberLazyGridState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                actualItemSize = with(it) { density.calculateSnapStepSize() }
            }
            expectedItemSize = state.layoutInfo.visibleItemsInfo.filter {
                if (vertical) {
                    it.column == 0
                } else {
                    it.row == 0
                }
            }.map {
                if (vertical) it.size.height else it.size.width
            }.average().toFloat()

            MainLayout(state, layoutInfoProvider, DynamicItemSizes.size, { DynamicItemSizes[it] })
        }

        rule.runOnIdle {
            assertEquals(round(expectedItemSize), round(actualItemSize))
        }
    }

    @Test
    fun snapStepSize_withSpacers_shouldBeAverageItemSize() {
        var snapStepSize = 0f
        var actualItemSize = 0f
        rule.setContent {
            val density = LocalDensity.current
            val state = rememberLazyGridState()
            val layoutInfoProvider = remember(state) { createLayoutInfo(state) }.also {
                snapStepSize = with(it) { density.calculateSnapStepSize() }
            }

            actualItemSize = with(density) { (FixedItemSize + FixedItemSize / 2).toPx() }

            MainLayout(
                state = state,
                layoutInfo = layoutInfoProvider,
                items = 200,
                itemSizeProvider = { FixedItemSize }) {
                if (vertical) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(FixedItemSize)
                                .background(Color.Red)
                        )
                        Spacer(
                            modifier = Modifier
                                .size(FixedItemSize / 2)
                                .background(Color.Yellow)
                        )
                    }
                } else {
                    Row {
                        Box(
                            modifier = Modifier
                                .size(FixedItemSize)
                                .background(Color.Red)
                        )
                        Spacer(
                            modifier = Modifier
                                .size(FixedItemSize / 2)
                                .background(Color.Yellow)
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(round(actualItemSize), round(snapStepSize))
        }
    }

    @Test
    fun calculateApproachOffset_highVelocity_approachOffsetIsEqualToDecayMinusItemSize() {
        lateinit var layoutInfoProvider: SnapLayoutInfoProvider
        val decay = splineBasedDecay<Float>(rule.density)
        fun calculateTargetOffset(velocity: Float): Float {
            val offset = decay.calculateTargetValue(0f, velocity).absoluteValue
            return (offset - with(density) { 200.dp.toPx() }).coerceAtLeast(0f) * velocity.sign
        }
        rule.setContent {
            val state = rememberLazyGridState()
            layoutInfoProvider = remember(state) { createLayoutInfo(state) }
            LazyGrid(
                cells = GridCells.Fixed(3),
                state = state,
                flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)
            ) {
                items(200) {
                    Box(modifier = Modifier.size(200.dp))
                }
            }
        }

        rule.runOnIdle {
            assertEquals(
                with(layoutInfoProvider) { density.calculateApproachOffset(10000f) },
                calculateTargetOffset(10000f)
            )
            assertEquals(
                with(layoutInfoProvider) { density.calculateApproachOffset(-10000f) },
                calculateTargetOffset(-10000f)
            )
        }
    }

    @Test
    fun calculateApproachOffset_lowVelocity_approachOffsetIsEqualToZero() {
        lateinit var layoutInfoProvider: SnapLayoutInfoProvider
        rule.setContent {
            val state = rememberLazyGridState()
            layoutInfoProvider = remember(state) { createLayoutInfo(state) }
            LazyGrid(
                cells = GridCells.Fixed(3),
                state = state,
                flingBehavior = rememberSnapFlingBehavior(layoutInfoProvider)
            ) {
                items(200) {
                    Box(modifier = Modifier.size(200.dp))
                }
            }
        }

        rule.runOnIdle {
            assertEquals(
                with(layoutInfoProvider) { density.calculateApproachOffset(1000f) },
                0f
            )
            assertEquals(
                with(layoutInfoProvider) { density.calculateApproachOffset(-1000f) },
                0f
            )
        }
    }

    @Composable
    private fun MainLayout(
        state: LazyGridState,
        layoutInfo: SnapLayoutInfoProvider,
        items: Int,
        itemSizeProvider: (Int) -> Dp,
        gridItem: @Composable (Int) -> Unit = { Box(Modifier.size(itemSizeProvider(it))) }
    ) {
        LazyGrid(
            cells = GridCells.Fixed(3),
            state = state,
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfo)
        ) {
            items(items) { gridItem(it) }
        }
    }

    private fun createLayoutInfo(
        state: LazyGridState,
    ): SnapLayoutInfoProvider {
        return SnapLayoutInfoProvider(state)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = arrayOf(Orientation.Vertical, Orientation.Horizontal)

        val FixedItemSize = 200.dp
        val DynamicItemSizes = (200..500).map { it.dp }
    }
}
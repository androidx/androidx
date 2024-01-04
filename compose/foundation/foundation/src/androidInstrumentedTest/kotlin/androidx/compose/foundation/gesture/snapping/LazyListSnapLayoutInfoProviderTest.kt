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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.list.BaseLazyListTestWithOrientation
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.sign
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyListSnapLayoutInfoProviderTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    private val density: Density
        get() = rule.density

    @Test
    fun calculateApproachOffset_highVelocity_approachOffsetIsEqualToDecayMinusItemSize() {
        lateinit var layoutInfoProvider: SnapLayoutInfoProvider
        val decay = splineBasedDecay<Float>(rule.density)
        fun calculateTargetOffset(velocity: Float): Float {
            val offset = decay.calculateTargetValue(0f, velocity)
            val itemSize = with(density) { 200.dp.toPx() }
            val estimatedNumberOfItemsInDecay = floor(offset.absoluteValue / itemSize)
            val approachOffset = estimatedNumberOfItemsInDecay * itemSize - itemSize
            return approachOffset.coerceAtLeast(0f) * velocity.sign
        }
        rule.setContent {
            val state = rememberLazyListState()
            layoutInfoProvider = remember(state) { createLayoutInfo(state) }
            LazyColumnOrRow(
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
                layoutInfoProvider.calculateApproachOffset(10000f),
                calculateTargetOffset(10000f)
            )
            assertEquals(
                layoutInfoProvider.calculateApproachOffset(-10000f),
                calculateTargetOffset(-10000f)
            )
        }
    }

    @Test
    fun calculateApproachOffset_lowVelocity_approachOffsetIsEqualToZero() {
        lateinit var layoutInfoProvider: SnapLayoutInfoProvider
        rule.setContent {
            val state = rememberLazyListState()
            layoutInfoProvider = remember(state) { createLayoutInfo(state) }
            LazyColumnOrRow(
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
                layoutInfoProvider.calculateApproachOffset(1000f),
                0f
            )
            assertEquals(
                layoutInfoProvider.calculateApproachOffset(-1000f),
                0f
            )
        }
    }

    @Composable
    private fun MainLayout(
        state: LazyListState,
        layoutInfo: SnapLayoutInfoProvider,
        items: Int,
        itemSizeProvider: (Int) -> Dp,
        listItem: @Composable (Int) -> Unit = { Box(Modifier.size(itemSizeProvider(it))) }
    ) {
        LazyColumnOrRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = layoutInfo)
        ) {
            items(items) { listItem(it) }
        }
    }

    private fun createLayoutInfo(state: LazyListState): SnapLayoutInfoProvider {
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

/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.carousel

import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ArrangementTest {

    @Test
    fun test1L1M1S_noAdjustmentsMade() {
        val targetSmallSize = 56f
        val targetLargeSize = 56f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f

        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize + targetMediumSize + targetSmallSize,
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(1),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize).isEqualTo(targetMediumSize)
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize)
    }

    @Test
    fun test1L1M1S_decreasesSmallSize() {
        val targetSmallSize = 56f
        val targetLargeSize = 56f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize + targetMediumSize + targetSmallSize - 10f,
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(1),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt()).isEqualTo(targetMediumSize.roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize - 10f)
    }

    @Test
    fun test1L1M1S_increasesSmallSize() {
        val targetSmallSize = 40f
        val targetLargeSize = 40f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize + targetMediumSize + targetSmallSize + 10f,
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(1),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt()).isEqualTo(targetMediumSize.roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize + 10f)
    }

    @Test
    fun test1L1M1S_decreasesMediumSize() {
        val targetSmallSize = 40f
        val targetLargeSize = 40f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val mediumAdjustment = targetMediumSize * .05f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize +
                targetMediumSize +
                targetSmallSize - mediumAdjustment,
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(1),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt())
            .isEqualTo((targetMediumSize - mediumAdjustment).roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize)
    }

    @Test
    fun test1L1M1S_increasesMediumSize() {
        val targetSmallSize = 56f
        val targetLargeSize = 56f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val mediumAdjustment = targetMediumSize * .05f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize +
                targetMediumSize +
                targetSmallSize + mediumAdjustment,
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(1),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt())
            .isEqualTo((targetMediumSize + mediumAdjustment).roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize)
    }

    @Test
    fun test1L1M2S_increasesSmallSize() {
        val targetSmallSize = 40f
        val targetLargeSize = 40f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val smallAdjustment = 10f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize +
                targetMediumSize +
                (targetSmallSize * 2) +
                (smallAdjustment * 2),
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(2),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt()).isEqualTo(targetMediumSize.roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize + smallAdjustment)
    }

    @Test
    fun test1L1M2S_decreasesSmallSize() {
        val targetSmallSize = 56f
        val targetLargeSize = 56f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val smallAdjustment = 10f

        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = targetLargeSize +
                targetMediumSize +
                (targetSmallSize * 2) - (smallAdjustment * 2),
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(2),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(1),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(1)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt()).isEqualTo(targetMediumSize.roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize - smallAdjustment)
    }

    @Test
    fun test2L2M2S_increasesMediumSize() {
        val targetSmallSize = 56f
        val targetLargeSize = 56f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val mediumAdjustment = targetMediumSize * .05f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = (targetLargeSize * 2) +
                (targetMediumSize * 2) +
                (targetSmallSize * 2) +
                (mediumAdjustment * 2),
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(2),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(2),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(2)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt())
            .isEqualTo((targetMediumSize + mediumAdjustment).roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize)
    }

    @Test
    fun test2L2M2S_decreasesMediumSize() {
        val targetSmallSize = 40f
        val targetLargeSize = 40f * 3f
        val targetMediumSize = (targetLargeSize + targetSmallSize) / 2f
        val mediumAdjustment = targetMediumSize * .05f
        val arrangement = Arrangement.findLowestCostArrangement(
            availableSpace = (targetLargeSize * 2) +
                (targetMediumSize * 2) +
                (targetSmallSize * 2) - (mediumAdjustment * 2),
            targetSmallSize = targetSmallSize,
            minSmallSize = 40f,
            maxSmallSize = 56f,
            smallCounts = intArrayOf(2),
            targetMediumSize = targetMediumSize,
            mediumCounts = intArrayOf(2),
            targetLargeSize = targetLargeSize,
            largeCounts = intArrayOf(2)
        )
        assertThat(arrangement?.largeSize).isEqualTo(targetLargeSize)
        assertThat(arrangement?.mediumSize?.roundToInt())
            .isEqualTo((targetMediumSize - mediumAdjustment).roundToInt())
        assertThat(arrangement?.smallSize).isEqualTo(targetSmallSize)
    }
}

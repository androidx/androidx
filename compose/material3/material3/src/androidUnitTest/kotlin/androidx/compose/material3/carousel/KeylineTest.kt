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

package androidx.compose.material3.carousel

import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeylineTest {

    @Test
    fun testKeylineList_findsFirstAndLastFocalKeylines() {
        val keylineList = createTestKeylineList()
        assertThat(keylineList.firstFocalIndex).isEqualTo(4)
        assertThat(keylineList.lastFocalIndex).isEqualTo(5)
    }

    @Test
    fun testKeylineList_pivotsWithCorrectCutoffsRight() {
        val carouselMainAxisSize = LargeSize + (MediumSize * .75f).roundToInt()
        val keylineList = keylineListOf(carouselMainAxisSize, CarouselAlignment.Start) {
            add(SmallSize, isAnchor = true)
            add(LargeSize)
            add(MediumSize)
            add(SmallSize, isAnchor = true)
        }

        assertThat(keylineList[2].cutoff)
            .isEqualTo(MediumSize - (MediumSize * .75f).roundToInt())
    }

    @Test
    fun testKeylineList_pivotsWithCorrectCutoffsLeft() {
        val carouselMainAxisSize = (MediumSize * .75f).roundToInt() + LargeSize
        val keylineList = keylineListOf(carouselMainAxisSize, CarouselAlignment.End) {
            add(SmallSize, isAnchor = true)
            add(MediumSize)
            add(LargeSize)
            add(SmallSize, isAnchor = true)
        }

        assertThat(keylineList[1].cutoff)
            .isEqualTo(MediumSize * 0.25f)
    }

    @Test
    fun testKeylineList_findsFirstIndexAfterFocalRangeWithSize() {
        val keylineList = createTestKeylineList()
        assertThat(keylineList.firstIndexAfterFocalRangeWithSize(SmallSize))
            .isEqualTo(7)
    }

    @Test
    fun testKeylineList_findsLastIndexBeforeFocalRangeWithSize() {
        val keylineList = createTestKeylineList()
        assertThat(keylineList.lastIndexBeforeFocalRangeWithSize(SmallSize))
            .isEqualTo(2)
    }

    @Test
    fun testKeylineList_getKeylineBefore() {
        val keylineList = createTestKeylineList()

        assertThat(keylineList.getKeylineBefore(unadjustedOffset = 60f)).isEqualTo(keylineList[3])
        assertThat(keylineList.getKeylineBefore(-Float.MAX_VALUE)).isEqualTo(keylineList[0])
        assertThat(keylineList.getKeylineBefore(Float.MAX_VALUE)).isEqualTo(keylineList.last())
    }

    @Test
    fun testKeylineList_getKeylineAfter() {
        val keylineList = createTestKeylineList()

        assertThat(keylineList.getKeylineAfter(60f)).isEqualTo(keylineList[4])
        assertThat(keylineList.getKeylineAfter(-Float.MAX_VALUE)).isEqualTo(keylineList[0])
        assertThat(keylineList.getKeylineAfter(Float.MAX_VALUE)).isEqualTo(keylineList.last())
    }

    @Test
    fun testKeylineListLerp() {
        val carouselMainAxisSize = StrategyTest.large + StrategyTest.medium + StrategyTest.small
        val from = keylineListOf(carouselMainAxisSize, 1, StrategyTest.large / 2) {
            add(StrategyTest.xSmall, isAnchor = true)
            add(StrategyTest.large)
            add(StrategyTest.medium)
            add(StrategyTest.small)
            add(StrategyTest.xSmall, isAnchor = true)
        }
        val to = keylineListOf(
            carouselMainAxisSize,
            2,
            StrategyTest.small + (StrategyTest.large / 2)
        ) {
            add(StrategyTest.xSmall, isAnchor = true)
            add(StrategyTest.small)
            add(StrategyTest.large)
            add(StrategyTest.medium)
            add(StrategyTest.xSmall, isAnchor = true)
        }

        // Create the expected interpolated KeylineList by using the KeylineList class' constructor
        // directly. Otherwise, keylineListOf will set offsets and unadjusted offsets based on the
        // pivot offset and will differ than the directly interpolated output of lerp.
        val half = KeylineList(
            listOf(
                Keyline(StrategyTest.xSmall, -2.5f, -90f, false, true, false, 0f),
                Keyline(60f, 30f, 10f, false, false, false, 0f),
                Keyline(80f, 100f, 110f, true, false, true, 0f),
                Keyline(40f, 160f, 210f, false, false, false, 0f),
                Keyline(StrategyTest.xSmall, 182.5f, 310f, false, true, false, 0f)
            )
        )

        assertThat(lerp(from, to, 0f)).isEqualTo(from)
        assertThat(lerp(from, to, 1f)).isEqualTo(to)
        assertThat(lerp(from, to, .5f)).isEqualTo(half)
    }

    @Test
    fun test_keylineListsShouldBeEqual() {
        val keylineList1 = keylineListOf(120f, CarouselAlignment.Start) {
            add(10f, true)
            add(100f)
            add(20f)
            add(10f, true)
        }
        val keylineList2 = keylineListOf(120f, CarouselAlignment.Start) {
            add(10f, true)
            add(100f)
            add(20f)
            add(10f, true)
        }

        assertThat(keylineList1 == keylineList2).isTrue()
        assertThat(keylineList1.hashCode()).isEqualTo(keylineList2.hashCode())
    }

    @Test
    fun testDifferentSizedItem_keylineListsShouldNotBeEqual() {
        val keylineList1 = keylineListOf(120f, CarouselAlignment.Start) {
            add(11f, true)
            add(100f)
            add(20f)
            add(10f, true)
        }
        val keylineList2 = keylineListOf(120f, CarouselAlignment.Start) {
            add(10f, true)
            add(100f)
            add(20f)
            add(10f, true)
        }

        assertThat(keylineList1 == keylineList2).isFalse()
        assertThat(keylineList1.hashCode()).isNotEqualTo(keylineList2.hashCode())
    }

    companion object {
        private const val LargeSize = 100f
        private const val SmallSize = 20f
        private const val XSmallSize = 5f
        private const val MediumSize = (LargeSize + SmallSize) / 2f

        /**
         * Creates a list of keylines with the following arrangement: [xs-s-s-m-l-l-m-s-s-xs].
         */
        private fun createTestKeylineList(): KeylineList {
            // Arrangement:
            // [xs-s-s-m-l-l-m-s-s-xs]
            val carouselMainAxisSize =
                (XSmallSize * 2) + (SmallSize * 4) + (MediumSize * 2) + (LargeSize * 2)
            return keylineListOf(carouselMainAxisSize, CarouselAlignment.Center) {
                add(XSmallSize, isAnchor = true)
                add(SmallSize)
                add(SmallSize)
                add(MediumSize)
                add(LargeSize)
                add(LargeSize)
                add(MediumSize)
                add(SmallSize)
                add(SmallSize)
                add(XSmallSize, isAnchor = true)
            }
        }
    }
}

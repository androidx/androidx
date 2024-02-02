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

import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StrategyTest {

    val Density = Density(1f, 1f)

    @Test
    fun testStrategy_startAlignedStrategyShiftsEnd() {
        val itemCount = 10
        val carouselMainAxisSize = large + medium + small
        val maxScrollOffset = (itemCount * large) - carouselMainAxisSize
        val defaultKeylineList = createStartAlignedKeylineList()

        val strategy = Strategy { defaultKeylineList }.apply(carouselMainAxisSize)

        assertThat(strategy.getKeylineListForScrollOffset(0f, maxScrollOffset))
            .isEqualTo(defaultKeylineList)

        val lastEndStepOffsets = arrayOf(-2.5f, 10f, 50f, 130f, 182.5f)
        val lastEndStepUnadjustedOffsets = arrayOf(
            130f - 300f,
            130f - 200f,
            130f - 100f,
            130f,
            130f + 100f
        )
        strategy.getKeylineListForScrollOffset(
            maxScrollOffset,
            maxScrollOffset,
        ).forEachIndexed { i, k ->
            assertThat(k.offset).isEqualTo(lastEndStepOffsets[i])
            assertThat(k.unadjustedOffset).isEqualTo(lastEndStepUnadjustedOffsets[i])
        }
    }

    @Test
    fun testStrategy_startAlignedCutoffStrategyShiftsEndWithCutoff() {
        val carouselMainAxisSize = large + medium + medium
        val cutoff = 50f
        val defaultKeylineList = createStartAlignedCutoffKeylineList(cutoff = cutoff)

        val strategy = Strategy { defaultKeylineList }.apply(carouselMainAxisSize)
        val endKeylineList = strategy.getEndKeylines()

        assertThat(defaultKeylineList.lastNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(defaultKeylineList.firstNonAnchor.offset -
            defaultKeylineList.firstNonAnchor.size / 2f).isEqualTo(0f)
        assertThat(endKeylineList.firstNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(endKeylineList.firstNonAnchor.offset -
            endKeylineList.firstNonAnchor.size / 2f).isEqualTo(-cutoff)
        assertThat(endKeylineList.lastNonAnchor.offset +
            endKeylineList.lastNonAnchor.size / 2f).isEqualTo(carouselMainAxisSize)
    }

    @Test
    fun testStrategy_endAlignedCutoffStrategyShiftsStartWithCutoff() {
        val carouselMainAxisSize = large + medium + medium
        val cutoff = 50f
        val defaultKeylineList = createEndAlignedCutoffKeylineList(cutoff = cutoff)

        val strategy = Strategy { defaultKeylineList }.apply(carouselMainAxisSize)
        val startKeylineList = strategy.getStartKeylines()

        assertThat(defaultKeylineList.firstNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(defaultKeylineList.lastNonAnchor.offset +
            defaultKeylineList.lastNonAnchor.size / 2f).isEqualTo(carouselMainAxisSize)
        assertThat(startKeylineList.lastNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(startKeylineList.firstNonAnchor.offset -
            startKeylineList.firstNonAnchor.size / 2f).isEqualTo(0f)
        assertThat(startKeylineList.lastNonAnchor.offset +
            startKeylineList.lastNonAnchor.size / 2f).isEqualTo(carouselMainAxisSize + cutoff)
    }

    @Test
    fun testStrategy_centerAlignedShiftsStart() {
        val itemCount = 12
        val carouselMainAxisSize = (small * 2) + medium + (large * 2) + medium + (small * 2)
        val maxScrollOffset = (itemCount * large) - carouselMainAxisSize
        val defaultKeylines = createCenterAlignedKeylineList()

        val strategy = Strategy { defaultKeylines }.apply(carouselMainAxisSize)

        val startSteps = listOf(
            // default step - [xs | s s m l l m s s | xs]
            createCenterAlignedKeylineList(),
            // step 1 - [xs | s m l l m s s s | xs]
            keylineListOf(carouselMainAxisSize, 3, (small * 1 + medium + (large / 2))) {
                add(xSmall, isAnchor = true)
                add(small)
                add(medium)
                add(large)
                add(large)
                add(medium)
                add(small)
                add(small)
                add(small)
                add(xSmall, isAnchor = true)
            },
            // step 2 - [xs | m l l m s s s s | xs]
            keylineListOf(carouselMainAxisSize, 2, medium + (large / 2)) {
                add(xSmall, isAnchor = true)
                add(medium)
                add(large)
                add(large)
                add(medium)
                add(small)
                add(small)
                add(small)
                add(small)
                add(xSmall, isAnchor = true)
            },
            // step 3 - [xs | l l m m s s s s | xs]
            keylineListOf(carouselMainAxisSize, 1, large / 2) {
                add(xSmall, isAnchor = true)
                add(large)
                add(large)
                add(medium)
                add(medium)
                add(small)
                add(small)
                add(small)
                add(small)
                add(xSmall, isAnchor = true)
            }
        )

        val shiftedStart = strategy.getKeylineListForScrollOffset(
            0f,
            maxScrollOffset,
        )
        assertThat(shiftedStart).isEqualTo(startSteps.last())
        // Make sure the last shift towards start places the first focal item against the start of
        // the carousel container.
        assertThat(shiftedStart.firstFocal.offset - (shiftedStart.firstFocal.size / 2))
            .isEqualTo(0)

        // Test all shift steps start by manually calculating the scroll offset at each step and
        // making sure getKeylineForScrollOffset returns the correct keyline list
        val totalShiftStart =
            startSteps.last().first().unadjustedOffset - startSteps.first().first().unadjustedOffset
        val startStepsScrollOffsets = listOf(
            totalShiftStart, // default step
            totalShiftStart - (startSteps[1].first().unadjustedOffset -
                startSteps[0].first().unadjustedOffset),
            totalShiftStart - (startSteps[2].first().unadjustedOffset -
                startSteps[0].first().unadjustedOffset),
            totalShiftStart - (startSteps[3].first().unadjustedOffset -
                startSteps[0].first().unadjustedOffset), // all the way start
        )

        startStepsScrollOffsets.forEachIndexed { i, scroll ->
            val keylineList =
                strategy.getKeylineListForScrollOffset(
                    scroll,
                    maxScrollOffset,
                )
            keylineList.forEachIndexed { j, keyline ->
                assertEqualWithFloatTolerance(0.01f, keyline, startSteps[i][j])
            }
        }
    }

    @Test
    fun testStrategy_centerAlignedShiftsEnd() {
        val itemCount = 12
        val carouselMainAxisSize = (small * 2) + medium + (large * 2) + medium + (small * 2)
        val maxScrollOffset = (itemCount * large) - carouselMainAxisSize
        val defaultKeylines = createCenterAlignedKeylineList()

        val strategy = Strategy { defaultKeylines }.apply(carouselMainAxisSize)

        val endSteps = listOf(
            // default step
            createCenterAlignedKeylineList(),
            // step 1: Move a small item from after focal to beginning of focal
            keylineListOf(carouselMainAxisSize, 5, (small * 3) + medium + (large / 2)) {
                add(xSmall, isAnchor = true)
                add(small)
                add(small)
                add(small)
                add(medium)
                add(large)
                add(large)
                add(medium)
                add(small)
                add(xSmall, isAnchor = true)
            },
            // step 2: Move another small from after focal to beginning of focal
            keylineListOf(carouselMainAxisSize, 6, (small * 4) + medium + (large / 2)) {
                add(xSmall, isAnchor = true)
                add(small)
                add(small)
                add(small)
                add(small)
                add(medium)
                add(large)
                add(large)
                add(medium)
                add(xSmall, isAnchor = true)
            },

            // step 3: Move medium from after focal to beginning of focal
            keylineListOf(carouselMainAxisSize, 7, (small * 4) + (medium * 2) + (large / 2)) {
                add(xSmall, isAnchor = true)
                add(small)
                add(small)
                add(small)
                add(small)
                add(medium)
                add(medium)
                add(large)
                add(large)
                add(xSmall, isAnchor = true)
            }
        )

        val shiftedEnd = strategy.getKeylineListForScrollOffset(
            maxScrollOffset,
            maxScrollOffset,
        )
        assertThat(shiftedEnd).isEqualTo(endSteps.last())
        // Make sure the last shift towards end places the last focal item against the end of the
        // carousel container.
        assertThat(shiftedEnd.lastFocal.offset + (shiftedEnd.lastFocal.size / 2))
            .isEqualTo(carouselMainAxisSize)

        val totalShiftEnd =
            endSteps.first().last().unadjustedOffset - endSteps.last().last().unadjustedOffset
        val endStepsScrollOffsets = listOf(
            maxScrollOffset - totalShiftEnd, // default step
            maxScrollOffset - totalShiftEnd - (endSteps[1].last().unadjustedOffset -
                endSteps[0].last().unadjustedOffset),
            maxScrollOffset - totalShiftEnd - (endSteps[2].last().unadjustedOffset -
                endSteps[0].last().unadjustedOffset),
            maxScrollOffset - totalShiftEnd - (endSteps[3].last().unadjustedOffset -
                endSteps[0].last().unadjustedOffset), // all the way end
        )

        // Test exact scroll offset returns the correct step
        endStepsScrollOffsets.forEachIndexed { i, scroll ->
            val keylineList = strategy.getKeylineListForScrollOffset(scroll, maxScrollOffset)
            keylineList.forEachIndexed { j, keyline ->
                assertEqualWithFloatTolerance(0.01f, keyline, endSteps[i][j])
            }
        }

        // Test non-exact scroll offset rounds to the correct step
        val almostToStepOneOffset = endStepsScrollOffsets[0] +
            ((endStepsScrollOffsets[1] - endStepsScrollOffsets[0]) * .75f)
        assertThat(
            strategy.getKeylineListForScrollOffset(
                almostToStepOneOffset,
                maxScrollOffset,
                roundToNearestStep = true
            )
        )
            .isEqualTo(endSteps[1])
        val halfWayToStepTwo =
            endStepsScrollOffsets[1] + ((endStepsScrollOffsets[2] - endStepsScrollOffsets[1]) * .5f)
        assertThat(
            strategy.getKeylineListForScrollOffset(
                halfWayToStepTwo,
                maxScrollOffset,
                roundToNearestStep = true
            )
        )
            .isEqualTo(endSteps[2])
        val justPastStepTwo =
            endStepsScrollOffsets[2] + ((endStepsScrollOffsets[3] - endStepsScrollOffsets[2]) * .1f)
        assertThat(strategy.getKeylineListForScrollOffset(
            justPastStepTwo,
            maxScrollOffset,
            roundToNearestStep = true
        ))
            .isEqualTo(endSteps[2])
    }

    @Test
    fun testStrategy_sameAvailableSpaceCreatesEqualObjects() {
        val itemSize = large
        val itemSpacing = 0f
        val strategy1 = Strategy { availableSpace ->
            multiBrowseKeylineList(Density, availableSpace, itemSize, itemSpacing)
        }
        val strategy2 = Strategy { availableSpace ->
            multiBrowseKeylineList(Density, availableSpace, itemSize, itemSpacing)
        }
        strategy1.apply(500f)
        strategy2.apply(500f)

        assertThat(strategy1 == strategy2).isTrue()
        assertThat(strategy1.hashCode()).isEqualTo(strategy2.hashCode())
    }

    @Test
    fun testStrategy_differentAvailableSpaceCreatesUnequalObjects() {
        val itemSize = large
        val itemSpacing = 0f
        val strategy1 = Strategy { availableSpace ->
            multiBrowseKeylineList(Density, availableSpace, itemSize, itemSpacing)
        }
        val strategy2 = Strategy { availableSpace ->
            multiBrowseKeylineList(Density, availableSpace, itemSize, itemSpacing)
        }
        strategy1.apply(500f)
        strategy2.apply(500f + 1f)

        assertThat(strategy1 == strategy2).isFalse()
        assertThat(strategy1.hashCode()).isNotEqualTo(strategy2.hashCode())
    }

    @Test
    fun testStrategy_invalidObjectDoesNotEqualValidObject() {
        val itemSize = large
        val itemSpacing = 0f
        val strategy1 = Strategy { availableSpace ->
            multiBrowseKeylineList(Density, availableSpace, itemSize, itemSpacing)
        }
        val strategy2 = Strategy { availableSpace ->
            multiBrowseKeylineList(Density, availableSpace, itemSize, itemSpacing)
        }
        strategy1.apply(500f)

        assertThat(strategy1 == strategy2).isFalse()
        assertThat(strategy1.hashCode()).isNotEqualTo(strategy2.hashCode())
    }

    private fun assertEqualWithFloatTolerance(
        tolerance: Float,
        actual: Keyline,
        expected: Keyline
    ) {
        assertThat(actual.size)
            .isWithin(tolerance).of(expected.size)
        assertThat(actual.offset)
            .isWithin(tolerance).of(expected.offset)
        assertThat(actual.unadjustedOffset)
            .isWithin(0.01f).of(expected.unadjustedOffset)
        assertThat(actual.isFocal).isEqualTo(expected.isFocal)
        assertThat(actual.isAnchor).isEqualTo(expected.isAnchor)
        assertThat(actual.isPivot).isEqualTo(expected.isPivot)
        assertThat(actual.cutoff)
            .isWithin(tolerance).of(expected.cutoff)
    }

    companion object {
        val large = 100f
        val small = 20f
        val medium = (large + small) / 2
        val xSmall = 5f

        private fun createCenterAlignedKeylineList(): KeylineList {
            // [xs | s s m l l m s s | xs]
            val carouselMainAxisSize = (small * 2) + medium + (large * 2) + medium + (small * 2)
            return keylineListOf(carouselMainAxisSize, CarouselAlignment.Center) {
                add(xSmall, isAnchor = true)
                add(small)
                add(small)
                add(medium)
                add(large)
                add(large)
                add(medium)
                add(small)
                add(small)
                add(xSmall, isAnchor = true)
            }
        }

        private fun createStartAlignedKeylineList(): KeylineList {
            // [xs | l m s | xs]
            return keylineListOf(large + medium + small, CarouselAlignment.Start) {
                add(xSmall, isAnchor = true)
                add(large)
                add(medium)
                add(small)
                add(xSmall, isAnchor = true)
            }
        }

        private fun createStartAlignedCutoffKeylineList(cutoff: Float): KeylineList {
            // [xs | l m m | xs]
            return keylineListOf(large + medium + medium, CarouselAlignment.Start) {
                add(xSmall, isAnchor = true)
                add(large + cutoff)
                add(medium)
                add(medium)
                add(xSmall, isAnchor = true)
            }
        }

        private fun createEndAlignedCutoffKeylineList(cutoff: Float): KeylineList {
            // [xs | m m l | xs]
            return keylineListOf(large + medium + medium, CarouselAlignment.End) {
                add(xSmall, isAnchor = true)
                add(medium)
                add(medium)
                add(large + cutoff)
                add(xSmall, isAnchor = true)
            }
        }
    }
}

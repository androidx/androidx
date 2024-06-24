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

        val strategy =
            Strategy(
                defaultKeylines = defaultKeylineList,
                availableSpace = carouselMainAxisSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        assertThat(strategy.getKeylineListForScrollOffset(0f, maxScrollOffset))
            .isEqualTo(defaultKeylineList)

        val lastEndStepOffsets = arrayOf(-2.5f, 10f, 50f, 130f, 182.5f)
        val lastEndStepUnadjustedOffsets =
            arrayOf(130f - 300f, 130f - 200f, 130f - 100f, 130f, 130f + 100f)
        strategy
            .getKeylineListForScrollOffset(
                maxScrollOffset,
                maxScrollOffset,
            )
            .forEachIndexed { i, k ->
                assertThat(k.offset).isEqualTo(lastEndStepOffsets[i])
                assertThat(k.unadjustedOffset).isEqualTo(lastEndStepUnadjustedOffsets[i])
            }
    }

    @Test
    fun testStrategy_startAlignedCutoffStrategyShiftsEndWithCutoff() {
        val carouselMainAxisSize = large + medium + medium
        val cutoff = 50f
        val defaultKeylineList = createStartAlignedCutoffKeylineList(cutoff = cutoff)

        val strategy =
            Strategy(
                defaultKeylines = defaultKeylineList,
                availableSpace = carouselMainAxisSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val endKeylineList = strategy.endKeylineSteps.last()

        assertThat(defaultKeylineList.lastNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(
                defaultKeylineList.firstNonAnchor.offset -
                    defaultKeylineList.firstNonAnchor.size / 2f
            )
            .isEqualTo(0f)
        assertThat(endKeylineList.firstNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(endKeylineList.firstNonAnchor.offset - endKeylineList.firstNonAnchor.size / 2f)
            .isEqualTo(-cutoff)
        assertThat(endKeylineList.lastNonAnchor.offset + endKeylineList.lastNonAnchor.size / 2f)
            .isEqualTo(carouselMainAxisSize)
    }

    @Test
    fun testStrategy_endAlignedCutoffStrategyShiftsStartWithCutoff() {
        val carouselMainAxisSize = large + medium + medium
        val cutoff = 50f
        val defaultKeylineList = createEndAlignedCutoffKeylineList(cutoff = cutoff)

        val strategy =
            Strategy(
                defaultKeylines = defaultKeylineList,
                availableSpace = carouselMainAxisSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val startKeylineList = strategy.startKeylineSteps.last()

        assertThat(defaultKeylineList.firstNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(
                defaultKeylineList.lastNonAnchor.offset + defaultKeylineList.lastNonAnchor.size / 2f
            )
            .isEqualTo(carouselMainAxisSize)
        assertThat(startKeylineList.lastNonAnchor.cutoff).isEqualTo(cutoff)
        assertThat(
                startKeylineList.firstNonAnchor.offset - startKeylineList.firstNonAnchor.size / 2f
            )
            .isEqualTo(0f)
        assertThat(startKeylineList.lastNonAnchor.offset + startKeylineList.lastNonAnchor.size / 2f)
            .isEqualTo(carouselMainAxisSize + cutoff)
    }

    @Test
    fun testStrategy_centerAlignedShiftsStart() {
        val itemCount = 12
        val carouselMainAxisSize = (small * 2) + medium + (large * 2) + medium + (small * 2)
        val maxScrollOffset = (itemCount * large) - carouselMainAxisSize
        val defaultKeylines = createCenterAlignedKeylineList()

        val strategy =
            Strategy(
                defaultKeylines = defaultKeylines,
                availableSpace = carouselMainAxisSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        val startSteps =
            listOf(
                // default step - [xs | s s m l l m s s | xs]
                createCenterAlignedKeylineList(),
                // step 1 - [xs | s m l l m s s s | xs]
                keylineListOf(
                    carouselMainAxisSize = carouselMainAxisSize,
                    itemSpacing = 0f,
                    pivotIndex = 3,
                    pivotOffset = (small * 1 + medium + (large / 2))
                ) {
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
                keylineListOf(
                    carouselMainAxisSize = carouselMainAxisSize,
                    itemSpacing = 0f,
                    pivotIndex = 2,
                    pivotOffset = medium + (large / 2)
                ) {
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
                keylineListOf(
                    carouselMainAxisSize = carouselMainAxisSize,
                    itemSpacing = 0f,
                    pivotIndex = 1,
                    pivotOffset = large / 2
                ) {
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

        val shiftedStart =
            strategy.getKeylineListForScrollOffset(
                0f,
                maxScrollOffset,
            )
        assertThat(shiftedStart).isEqualTo(startSteps.last())
        // Make sure the last shift towards start places the first focal item against the start of
        // the carousel container.
        assertThat(shiftedStart.firstFocal.offset - (shiftedStart.firstFocal.size / 2)).isEqualTo(0)

        // Test all shift steps start by manually calculating the scroll offset at each step and
        // making sure getKeylineForScrollOffset returns the correct keyline list
        val totalShiftStart =
            startSteps.last().first().unadjustedOffset - startSteps.first().first().unadjustedOffset
        val startStepsScrollOffsets =
            listOf(
                totalShiftStart, // default step
                totalShiftStart -
                    (startSteps[1].first().unadjustedOffset -
                        startSteps[0].first().unadjustedOffset),
                totalShiftStart -
                    (startSteps[2].first().unadjustedOffset -
                        startSteps[0].first().unadjustedOffset),
                totalShiftStart -
                    (startSteps[3].first().unadjustedOffset -
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

        val strategy =
            Strategy(
                defaultKeylines = defaultKeylines,
                availableSpace = carouselMainAxisSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        val endSteps =
            listOf(
                // default step
                createCenterAlignedKeylineList(),
                // step 1: Move a small item from after focal to beginning of focal
                keylineListOf(
                    carouselMainAxisSize = carouselMainAxisSize,
                    itemSpacing = 0f,
                    pivotIndex = 5,
                    pivotOffset = (small * 3) + medium + (large / 2)
                ) {
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
                keylineListOf(
                    carouselMainAxisSize = carouselMainAxisSize,
                    itemSpacing = 0f,
                    pivotIndex = 6,
                    pivotOffset = (small * 4) + medium + (large / 2)
                ) {
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
                keylineListOf(
                    carouselMainAxisSize = carouselMainAxisSize,
                    itemSpacing = 0f,
                    pivotIndex = 7,
                    pivotOffset = (small * 4) + (medium * 2) + (large / 2)
                ) {
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

        val shiftedEnd =
            strategy.getKeylineListForScrollOffset(
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
        val endStepsScrollOffsets =
            listOf(
                maxScrollOffset - totalShiftEnd, // default step
                maxScrollOffset -
                    totalShiftEnd -
                    (endSteps[1].last().unadjustedOffset - endSteps[0].last().unadjustedOffset),
                maxScrollOffset -
                    totalShiftEnd -
                    (endSteps[2].last().unadjustedOffset - endSteps[0].last().unadjustedOffset),
                maxScrollOffset -
                    totalShiftEnd -
                    (endSteps[3].last().unadjustedOffset -
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
        val almostToStepOneOffset =
            endStepsScrollOffsets[0] +
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
        assertThat(
                strategy.getKeylineListForScrollOffset(
                    justPastStepTwo,
                    maxScrollOffset,
                    roundToNearestStep = true
                )
            )
            .isEqualTo(endSteps[2])
    }

    @Test
    fun testStrategy_sameAvailableSpaceCreatesEqualObjects() {
        val itemSize = large
        val itemCount = 10
        val availableSpace = 500f
        val itemSpacing = 0f
        val strategy1 =
            Strategy(
                defaultKeylines =
                    multiBrowseKeylineList(
                        Density,
                        availableSpace,
                        itemSize,
                        itemSpacing,
                        itemCount
                    ),
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val strategy2 =
            Strategy(
                defaultKeylines =
                    multiBrowseKeylineList(
                        Density,
                        availableSpace,
                        itemSize,
                        itemSpacing,
                        itemCount
                    ),
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        assertThat(strategy1 == strategy2).isTrue()
        assertThat(strategy1.hashCode()).isEqualTo(strategy2.hashCode())
    }

    @Test
    fun testStrategy_differentAvailableSpaceCreatesUnequalObjects() {
        val itemSize = large
        val itemSpacing = 0f
        val itemCount = 10
        val strategy1 =
            Strategy(
                defaultKeylines =
                    multiBrowseKeylineList(Density, 500f, itemSize, itemSpacing, itemCount),
                availableSpace = 500f,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val strategy2 =
            Strategy(
                defaultKeylines =
                    multiBrowseKeylineList(Density, 501f, itemSize, itemSpacing, itemCount),
                availableSpace = 501f,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        assertThat(strategy1 == strategy2).isFalse()
        assertThat(strategy1.hashCode()).isNotEqualTo(strategy2.hashCode())
    }

    @Test
    fun testStrategy_invalidObjectDoesNotEqualValidObject() {
        val itemSize = large
        val availableSpace = 500f
        val itemSpacing = 0f
        val itemCount = 10
        val strategy1 =
            Strategy(
                defaultKeylines =
                    multiBrowseKeylineList(
                        Density,
                        availableSpace,
                        itemSize,
                        itemSpacing,
                        itemCount
                    ),
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )
        val strategy2 =
            Strategy(
                defaultKeylines = emptyKeylineList(),
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        assertThat(strategy1 == strategy2).isFalse()
        assertThat(strategy1.hashCode()).isNotEqualTo(strategy2.hashCode())
    }

    @Test
    fun testStrategy_startAlignedStrategyWithNegativeMaxScroll() {
        val itemCount = 1
        val carouselMainAxisSize = large + medium + small
        val maxScrollOffset = (itemCount * large) - carouselMainAxisSize
        val defaultKeylineList = createStartAlignedKeylineList()

        val strategy =
            Strategy(
                defaultKeylines = defaultKeylineList,
                availableSpace = carouselMainAxisSize,
                itemSpacing = 0f,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        assertThat(strategy.getKeylineListForScrollOffset(0f, maxScrollOffset))
            .isEqualTo(defaultKeylineList)
    }

    @Test
    fun testStartKeylineStrategy_endStepsShouldAccountForItemSpacing() {
        val availableSpace = 380f
        val itemSpacing = 8f
        val strategy =
            Strategy(
                defaultKeylines =
                    keylineListOf(availableSpace, itemSpacing, CarouselAlignment.Start) {
                        add(10f, isAnchor = true)
                        add(186f)
                        add(122f)
                        add(56f)
                        add(10f, isAnchor = true)
                    },
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        val middleStep = strategy.endKeylineSteps[1]
        val actualMiddleOffsets = middleStep.map { it.offset }.toFloatArray()
        val expectedMiddleOffsets = floatArrayOf(-13f, 28f, 157f, 319f, 393f)
        assertThat(actualMiddleOffsets).isEqualTo(expectedMiddleOffsets)

        val actualMiddleUnadjustedOffsets = middleStep.map { it.unadjustedOffset }.toFloatArray()
        val expectedMiddleUnadjustedOffsets = floatArrayOf(-231f, -37f, 157f, 351f, 545f)
        assertThat(actualMiddleUnadjustedOffsets).isEqualTo(expectedMiddleUnadjustedOffsets)

        val endStep = strategy.endKeylineSteps[2]
        val actualEndOffsets = endStep.map { it.offset }.toFloatArray()
        val expectedEndOffsets = floatArrayOf(-13f, 28f, 125f, 287f, 393f)
        assertThat(actualEndOffsets).isEqualTo(expectedEndOffsets)

        val actualEndUnadjustedOffsets = endStep.map { it.unadjustedOffset }.toFloatArray()
        val expectedEndUnadjustedOffsets = floatArrayOf(-295f, -101f, 93f, 287f, 481f)
        assertThat(actualEndUnadjustedOffsets).isEqualTo(expectedEndUnadjustedOffsets)
    }

    @Test
    fun testCenterKeylineStrategy_startAndEndStepsShouldAccountForItemSpacing() {
        val availableSpace = 768f
        val itemSpacing = 8f
        val strategy =
            Strategy(
                defaultKeylines =
                    keylineListOf(availableSpace, itemSpacing, CarouselAlignment.Center) {
                        add(10f, isAnchor = true)
                        add(56f)
                        add(122f)
                        add(186f)
                        add(186f)
                        add(122f)
                        add(56f)
                        add(10f, isAnchor = true)
                    },
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 0f,
                afterContentPadding = 0f
            )

        assertThat(strategy.startKeylineSteps).hasSize(3)
        assertThat(strategy.endKeylineSteps).hasSize(3)

        val s1 = strategy.startKeylineSteps[1]
        val s1ActualOffsets = s1.map { it.offset }.toFloatArray()
        // Should move one small from start to end - xs, m, l, l, m, s, s, xs
        val s1ExpectedOffsets = floatArrayOf(-13f, 61f, 223f, 417f, 579f, 676f, 740f, 781f)
        assertThat(s1ActualOffsets).isEqualTo(s1ExpectedOffsets)

        val s1ActualUnadjustedOffsets = s1.map { it.unadjustedOffset }.toFloatArray()
        val s1ExpectedUnadjustedOffsets =
            floatArrayOf(-165f, 29f, 223f, 417f, 611f, 805f, 999f, 1193f)
        assertThat(s1ActualUnadjustedOffsets).isEqualTo(s1ExpectedUnadjustedOffsets)

        val s2 = strategy.startKeylineSteps[2]
        val s2ActualOffsets = s2.map { it.offset }.toFloatArray()
        // Should be - xs, l, l, m, m, s, s, xs
        val s2ExpectedOffsets = floatArrayOf(-13f, 93f, 287f, 449f, 579f, 676f, 740f, 781f)
        assertThat(s2ActualOffsets).isEqualTo(s2ExpectedOffsets)

        val s2ActualUnadjustedOffsets = s2.map { it.unadjustedOffset }.toFloatArray()
        val s2ExpectedUnadjustedOffsets =
            floatArrayOf(-101f, 93f, 287f, 481f, 675f, 869f, 1063f, 1257f)
        assertThat(s2ActualUnadjustedOffsets).isEqualTo(s2ExpectedUnadjustedOffsets)

        val e1 = strategy.endKeylineSteps[1]
        val e1ActualOffsets = e1.map { it.offset }.toFloatArray()
        // Should move one small item to start - xs, s, s, m, l, l, m, xs
        val e1ExpectedOffsets = floatArrayOf(-13f, 28f, 92f, 189f, 351f, 545f, 707f, 781f)
        assertThat(e1ActualOffsets).isEqualTo(e1ExpectedOffsets)

        val e1ActualUnadjustedOffsets = e1.map { it.unadjustedOffset }.toFloatArray()
        val e1ExpectedUnadjustedOffsets =
            floatArrayOf(-425f, -231f, -37f, 157f, 351f, 545f, 739f, 933f)
        assertThat(e1ActualUnadjustedOffsets).isEqualTo(e1ExpectedUnadjustedOffsets)

        val e2 = strategy.endKeylineSteps[2]
        val e2ActualOffsets = e2.map { it.offset }.toFloatArray()
        // Should move one medium item to end
        val e2ExpectedOffsets = floatArrayOf(-13f, 28f, 92f, 189f, 319f, 481f, 675f, 781f)
        assertThat(e2ActualOffsets).isEqualTo(e2ExpectedOffsets)

        val e2ActualUnadjustedOffsets = e2.map { it.unadjustedOffset }.toFloatArray()
        val e2ExpectedUnadjustedOffsets =
            floatArrayOf(-489f, -295f, -101f, 93f, 287f, 481f, 675f, 869f)
        assertThat(e2ActualUnadjustedOffsets).isEqualTo(e2ExpectedUnadjustedOffsets)
    }

    @Test
    fun testCenterStrategy_stepsShouldAccountForContentPadding() {
        val availableSpace = 500f
        val itemSpacing = 0f
        val strategy =
            Strategy(
                defaultKeylines =
                    keylineListOf(availableSpace, itemSpacing, CarouselAlignment.Center) {
                        add(10f, isAnchor = true)
                        add(50f)
                        add(100f)
                        add(200f)
                        add(100f)
                        add(50f)
                        add(10f, isAnchor = true)
                    },
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 16f,
                afterContentPadding = 24f
            )

        val lastStartStep = strategy.startKeylineSteps.last()

        val firstFocalLeft = lastStartStep.firstFocal.offset - (lastStartStep.firstFocal.size / 2f)
        val lastNonAnchorRight =
            lastStartStep.lastNonAnchor.offset + (lastStartStep.lastNonAnchor.size / 2f)
        val lastEndStep = strategy.endKeylineSteps.last()
        val lastFocalRight = lastEndStep.lastFocal.offset + (lastEndStep.lastFocal.size / 2f)
        val firstNonAnchorLeft =
            lastEndStep.firstNonAnchor.offset - (lastEndStep.firstNonAnchor.size / 2f)

        assertThat(firstFocalLeft).isEqualTo(16f)
        assertThat(lastNonAnchorRight).isEqualTo(500f)
        assertThat(lastFocalRight).isEqualTo(500f - 24f)
        assertThat(firstNonAnchorLeft).isWithin(.01f).of(0f)
    }

    @Test
    fun testStartStrategy_twoLargeOneSmall_shouldAccountForPadding() {
        val availableSpace = 444f
        val itemSpacing = 8f
        val strategy =
            Strategy(
                defaultKeylines =
                    keylineListOf(availableSpace, itemSpacing, CarouselAlignment.Start) {
                        add(10f, isAnchor = true)
                        add(186f)
                        add(186f)
                        add(56f)
                        add(10f, isAnchor = true)
                    },
                availableSpace = availableSpace,
                itemSpacing = itemSpacing,
                beforeContentPadding = 16f,
                afterContentPadding = 16f
            )

        assertThat(strategy.itemMainAxisSize).isEqualTo(186f)

        val lastStartStepSmallItem = strategy.startKeylineSteps.last()[3]
        assertThat(lastStartStepSmallItem.offset + (lastStartStepSmallItem.size / 2f))
            .isWithin(.001f)
            .of(444f)

        val lastEndSteps = strategy.endKeylineSteps.last()
        assertThat(lastEndSteps[1].size + 8f + lastEndSteps[2].size + 8f + lastEndSteps[3].size)
            .isEqualTo(444f - 16f)

        val lastEndStepSmallItem = strategy.endKeylineSteps.last()[1]
        assertThat(lastEndStepSmallItem.offset - (lastEndStepSmallItem.size / 2f))
            .isWithin(.001f)
            .of(0f)
    }

    private fun assertEqualWithFloatTolerance(
        tolerance: Float,
        actual: Keyline,
        expected: Keyline
    ) {
        assertThat(actual.size).isWithin(tolerance).of(expected.size)
        assertThat(actual.offset).isWithin(tolerance).of(expected.offset)
        assertThat(actual.unadjustedOffset).isWithin(0.01f).of(expected.unadjustedOffset)
        assertThat(actual.isFocal).isEqualTo(expected.isFocal)
        assertThat(actual.isAnchor).isEqualTo(expected.isAnchor)
        assertThat(actual.isPivot).isEqualTo(expected.isPivot)
        assertThat(actual.cutoff).isWithin(tolerance).of(expected.cutoff)
    }

    companion object {
        val large = 100f
        val small = 20f
        val medium = (large + small) / 2
        val xSmall = 5f

        private fun createCenterAlignedKeylineList(): KeylineList {
            // [xs | s s m l l m s s | xs]
            val carouselMainAxisSize = (small * 2) + medium + (large * 2) + medium + (small * 2)
            return keylineListOf(
                carouselMainAxisSize = carouselMainAxisSize,
                itemSpacing = 0f,
                carouselAlignment = CarouselAlignment.Center
            ) {
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
            return keylineListOf(
                carouselMainAxisSize = large + medium + small,
                itemSpacing = 0f,
                carouselAlignment = CarouselAlignment.Start
            ) {
                add(xSmall, isAnchor = true)
                add(large)
                add(medium)
                add(small)
                add(xSmall, isAnchor = true)
            }
        }

        private fun createStartAlignedCutoffKeylineList(cutoff: Float): KeylineList {
            // [xs | l m m | xs]
            return keylineListOf(
                carouselMainAxisSize = large + medium + medium,
                itemSpacing = 0f,
                carouselAlignment = CarouselAlignment.Start
            ) {
                add(xSmall, isAnchor = true)
                add(large + cutoff)
                add(medium)
                add(medium)
                add(xSmall, isAnchor = true)
            }
        }

        private fun createEndAlignedCutoffKeylineList(cutoff: Float): KeylineList {
            // [xs | m m l | xs]
            return keylineListOf(
                carouselMainAxisSize = large + medium + medium,
                itemSpacing = 0f,
                carouselAlignment = CarouselAlignment.End
            ) {
                add(xSmall, isAnchor = true)
                add(medium)
                add(medium)
                add(large + cutoff)
                add(xSmall, isAnchor = true)
            }
        }
    }
}

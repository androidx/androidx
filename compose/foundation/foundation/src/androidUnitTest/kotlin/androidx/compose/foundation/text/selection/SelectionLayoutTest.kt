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

package androidx.compose.foundation.text.selection

import androidx.collection.LongObjectMap
import androidx.compose.foundation.text.selection.Direction.AFTER
import androidx.compose.foundation.text.selection.Direction.BEFORE
import androidx.compose.foundation.text.selection.Direction.ON
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextRange
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectionLayoutTest {
    @Test
    fun layoutBuilderSizeZero_throws() {
        assertThat(buildSelectionLayoutForTestOrNull {}).isNull()
    }

    @Test
    fun singleLayout_verifySimpleParameters() {
        val selection = getSelection()
        val layout =
            getSingleSelectionLayoutForTest(
                isStartHandle = true,
                previousSelection = selection,
            )
        assertThat(layout.isStartHandle).isTrue()
        assertThat(layout.previousSelection).isEqualTo(selection)
    }

    @Test
    fun layoutBuilder_verifySimpleParameters() {
        val selection = getSelection()
        val layout =
            buildSelectionLayoutForTest(
                isStartHandle = true,
                previousSelection = selection,
            ) {
                appendInfoForTest()
            }
        assertThat(layout.isStartHandle).isTrue()
        assertThat(layout.previousSelection).isEqualTo(selection)
    }

    @Test
    fun singleLayout_sameInfoForAllSelectableInfoFunctions() {
        val layout = getSingleSelectionLayoutForTest()
        // since there is only one info, each info function should return the same
        val info = layout.currentInfo
        assertThat(layout.startInfo).isSameInstanceAs(info)
        assertThat(layout.endInfo).isSameInstanceAs(info)
        assertThat(layout.firstInfo).isSameInstanceAs(info)
        assertThat(layout.lastInfo).isSameInstanceAs(info)
    }

    @Test
    fun size_singleLayout_returnsOne() {
        val selection = getSingleSelectionLayoutForTest()
        assertThat(selection.size).isEqualTo(1)
    }

    @Test
    fun size_layoutBuilderSizeOne_returnsOne() {
        val selection = buildSelectionLayoutForTest { appendInfoForTest() }
        assertThat(selection.size).isEqualTo(1)
    }

    @Test
    fun size_layoutBuilderSizeMoreThanOne_returnsSize() {
        val selection = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 3L,
                startYHandleDirection = BEFORE,
            )
        }
        assertThat(selection.size).isEqualTo(3)
    }

    @Test
    fun startSlot_singleLayout_equalsOnlyInfo() {
        val layout = getSingleSelectionLayoutForTest()
        // when there is only one info, slot doesn't matter
        // so, ensure that the slot is equal to the only info's slot
        assertThat(layout.startSlot).isEqualTo(layout.currentInfo.slot)
    }

    @Test
    fun startSlot_layoutBuilder_onBefore_equalsZero() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startSlot).isEqualTo(0)
    }

    @Test
    fun startSlot_layoutBuilder_onFirst_equalsOne() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startSlot).isEqualTo(1)
    }

    @Test
    fun startSlot_layoutBuilder_onMiddle_equalsTwo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startSlot).isEqualTo(2)
    }

    @Test
    fun startSlot_layoutBuilder_onLast_equalsThree() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = ON,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startSlot).isEqualTo(3)
    }

    @Test
    fun startSlot_layoutBuilder_onAfter_equalsFour() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startSlot).isEqualTo(4)
    }

    @Test
    fun endSlot_singleLayout_equalsOnlyInfo() {
        val layout = getSingleSelectionLayoutForTest()
        // when there is only one info, slot doesn't matter
        // so, ensure that the slot is equal to the only info's slot
        assertThat(layout.endSlot).isEqualTo(layout.currentInfo.slot)
    }

    @Test
    fun endSlot_layoutBuilder_onBefore_equalsZero() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.endSlot).isEqualTo(0)
    }

    @Test
    fun endSlot_layoutBuilder_onFirst_equalsOne() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = ON,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.endSlot).isEqualTo(1)
    }

    @Test
    fun endSlot_layoutBuilder_onMiddle_equalsTwo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.endSlot).isEqualTo(2)
    }

    @Test
    fun endSlot_layoutBuilder_onLast_equalsThree() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.endSlot).isEqualTo(3)
    }

    @Test
    fun endSlot_layoutBuilder_onAfter_equalsFour() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
        }
        assertThat(layout.endSlot).isEqualTo(4)
    }

    @Test
    fun crossStatus_singleLayout_collapsed() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawStartHandleOffset = 0,
                rawEndHandleOffset = 0,
            )
        assertThat(layout.crossStatus).isEqualTo(CrossStatus.COLLAPSED)
    }

    @Test
    fun crossStatus_singleLayout_crossed() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawStartHandleOffset = 1,
                rawEndHandleOffset = 0,
            )
        assertThat(layout.crossStatus).isEqualTo(CrossStatus.CROSSED)
    }

    @Test
    fun crossStatus_singleLayout_notCrossed() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawStartHandleOffset = 0,
                rawEndHandleOffset = 1,
            )
        assertThat(layout.crossStatus).isEqualTo(CrossStatus.NOT_CROSSED)
    }

    @Test
    fun crossStatus_layoutBuilder_collapsed() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = ON,
                rawStartHandleOffset = 0,
                rawEndHandleOffset = 0,
            )
        }
        assertThat(layout.crossStatus).isEqualTo(CrossStatus.COLLAPSED)
    }

    @Test
    fun crossStatus_layoutBuilder_crossed() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.crossStatus).isEqualTo(CrossStatus.CROSSED)
    }

    @Test
    fun crossStatus_layoutBuilder_notCrossed() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.crossStatus).isEqualTo(CrossStatus.NOT_CROSSED)
    }

    // No startInfo test for singleLayout because it is covered in
    // singleLayout_sameInfoForAllSelectableInfoFunctions

    @Test
    fun startInfo_layoutBuilder_onSlotZero_equalsFirstInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun startInfo_layoutBuilder_onSlotOne_equalsFirstInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun startInfo_layoutBuilder_onSlotTwo_equalsSecondInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun startInfo_layoutBuilder_onSlotThree_equalsSecondInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = ON,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun startInfo_layoutBuilder_onSlotFour_equalsSecondInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.startInfo.selectableId).isEqualTo(2L)
    }

    // No endInfo test for singleLayout because it is covered in
    // singleLayout_sameInfoForAllSelectableInfoFunctions

    @Test
    fun endInfo_layoutBuilder_onSlotZero_equalsFirstInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.endInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun endInfo_layoutBuilder_onSlotOne_equalsFirstInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = ON,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.endInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun endInfo_layoutBuilder_onSlotTwo_equalsFirstInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.endInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun endInfo_layoutBuilder_onSlotThree_equalsSecondInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.endInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun endInfo_layoutBuilder_onSlotFour_equalsSecondInfo() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
        }
        assertThat(layout.endInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun currentInfo_layoutBuilder_currentInfo_startHandle_equalsFirst() {
        val layout =
            buildSelectionLayoutForTest(isStartHandle = true) {
                appendInfoForTest(
                    selectableId = 1L,
                    startYHandleDirection = ON,
                    endYHandleDirection = AFTER,
                )
                appendInfoForTest(
                    selectableId = 2L,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = ON,
                )
            }
        assertThat(layout.currentInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun currentInfo_layoutBuilder_endHandle_equalsSecond() {
        val layout =
            buildSelectionLayoutForTest(isStartHandle = false) {
                appendInfoForTest(
                    selectableId = 1L,
                    startYHandleDirection = ON,
                    endYHandleDirection = AFTER,
                )
                appendInfoForTest(
                    selectableId = 2L,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = ON,
                )
            }
        assertThat(layout.currentInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun firstInfo_layoutBuilder_notCrossed_equalsFirst() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.firstInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun firstInfo_layoutBuilder_crossed_equalsFirst() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.firstInfo.selectableId).isEqualTo(1L)
    }

    @Test
    fun lastInfo_layoutBuilder_notCrossed_equalsSecond() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        assertThat(layout.lastInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun lastInfo_layoutBuilder_crossed_equalsSecond() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
            )
        }
        assertThat(layout.lastInfo.selectableId).isEqualTo(2L)
    }

    @Test
    fun middleInfos_singleLayout_isEmpty() {
        val layout = getSingleSelectionLayoutForTest()
        val infoList = mutableListOf<SelectableInfo>()
        layout.forEachMiddleInfo { infoList += it }
        assertThat(infoList).isEmpty()
    }

    @Test
    fun middleInfos_layoutBuilder_twoInfos_isEmpty() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }

        val infoList = mutableListOf<SelectableInfo>()
        layout.forEachMiddleInfo { infoList += it }
        assertThat(infoList).isEmpty()
    }

    @Test
    fun middleInfos_layoutBuilder_threeInfos_containsOneElement() {
        val info: SelectableInfo
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            info =
                appendInfoForTest(
                    selectableId = 2L,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = AFTER,
                )
            appendInfoForTest(
                selectableId = 3L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        val infoList = mutableListOf<SelectableInfo>()
        layout.forEachMiddleInfo { infoList += it }
        assertThat(infoList).containsExactly(info)
    }

    @Test
    fun middleInfos_layoutBuilder_fourInfos_containsTwoElements() {
        val infoOne: SelectableInfo
        val infoTwo: SelectableInfo
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            infoOne =
                appendInfoForTest(
                    selectableId = 2L,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = AFTER,
                )
            infoTwo =
                appendInfoForTest(
                    selectableId = 3L,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = AFTER,
                )
            appendInfoForTest(
                selectableId = 4L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        val infoList = mutableListOf<SelectableInfo>()
        layout.forEachMiddleInfo { infoList += it }
        assertThat(infoList).containsExactly(infoOne, infoTwo).inOrder()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_otherNull_returnsTrue() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        assertThat(layout.shouldRecomputeSelection(null)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_otherMulti_returnsTrue() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        val otherLayout = buildSelectionLayoutForTest {
            appendInfoForTest()
            appendInfoForTest()
        }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_differentHandle_returnsTrue() {
        val layout =
            getSingleSelectionLayoutForTest(
                isStartHandle = true,
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        val otherLayout =
            getSingleSelectionLayoutForTest(
                isStartHandle = false,
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_differentInfo_returnsTrue() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawStartHandleOffset = 0,
                previousSelection = getSelection()
            )
        val otherLayout =
            getSingleSelectionLayoutForTest(
                rawStartHandleOffset = 1,
                previousSelection = getSelection()
            )
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_noPreviousSelection_returnsTrue() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
            )
        val otherLayout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_sameLayout_returnsFalse() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        assertThat(layout.shouldRecomputeSelection(layout)).isFalse()
    }

    @Test
    fun shouldRecomputeSelection_singleLayout_equalLayout_returnsFalse() {
        val layout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        val otherLayout =
            getSingleSelectionLayoutForTest(
                rawPreviousHandleOffset = 5,
                previousSelection = getSelection()
            )
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isFalse()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_otherNull_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        assertThat(layout.shouldRecomputeSelection(null)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_otherSingle_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        val otherLayout =
            getSingleSelectionLayoutForTest(
                previousSelection = getSelection(),
                rawPreviousHandleOffset = 5
            )
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_differentStartSlot_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = AFTER,
                )
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = ON,
                )
            }
        val otherLayout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = ON,
                    endYHandleDirection = AFTER,
                )
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = ON,
                )
            }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_differentEndSlot_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = ON,
                    endYHandleDirection = AFTER,
                )
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = AFTER,
                )
            }
        val otherLayout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = ON,
                    endYHandleDirection = AFTER,
                )
                appendInfoForTest(
                    rawEndHandleOffset = 5,
                    rawPreviousHandleOffset = 5,
                    startYHandleDirection = BEFORE,
                    endYHandleDirection = ON,
                )
            }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_differentHandle_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(isStartHandle = true, previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        val otherLayout =
            buildSelectionLayoutForTest(previousSelection = getSelection(), isStartHandle = false) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_differentSize_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        val otherLayout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_differentInfo_returnsTrue() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        val otherLayout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 4, rawPreviousHandleOffset = 5)
            }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_sameLayout_returnsFalse() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        assertThat(layout.shouldRecomputeSelection(layout)).isFalse()
    }

    @Test
    fun shouldRecomputeSelection_layoutBuilder_equalLayout_returnsFalse() {
        val layout =
            buildSelectionLayoutForTest(previousSelection = getSelection()) {
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
                appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            }
        val otherLayout = buildSelectionLayoutForTest {
            appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
            appendInfoForTest(rawEndHandleOffset = 5, rawPreviousHandleOffset = 5)
        }
        assertThat(layout.shouldRecomputeSelection(otherLayout)).isFalse()
    }

    @Test
    fun createSubSelections_singleLayout_missNonCrossedSelection_returnsCrossedSelection() {
        val layout = getSingleSelectionLayoutForTest()
        val selection = getSelection(startOffset = 1, endOffset = 0, handlesCrossed = false)
        val actual = layout.createSubSelections(selection).toMap()
        assertThat(actual).hasSize(1)
        assertThat(actual.toList().single().second).isEqualTo(selection.copy(handlesCrossed = true))
    }

    @Test
    fun createSubSelections_singleLayout_missCrossedSelection_returnsUncrossedSelection() {
        val layout = getSingleSelectionLayoutForTest()
        val selection = getSelection(startOffset = 0, endOffset = 1, handlesCrossed = true)
        val actual = layout.createSubSelections(selection).toMap()
        assertThat(actual).hasSize(1)
        assertThat(actual.toList().single().second)
            .isEqualTo(selection.copy(handlesCrossed = false))
    }

    @Test
    fun createSubSelections_singleLayout_validSelection_returnsInputSelection() {
        val layout = getSingleSelectionLayoutForTest()
        val selection = getSelection()
        val actual = layout.createSubSelections(selection).toMap()
        assertThat(actual).hasSize(1)
        // We don't care about the selectableId since it isn't used anyways
        assertThat(actual.toList().single().second).isEqualTo(selection)
    }

    @Test
    fun createSubSelections_builtSingleLayout_validSelection_returnsInputSelection() {
        val layout = buildSelectionLayoutForTest { appendInfoForTest(selectableId = 1L) }
        val selection = getSelection()
        assertThat(layout.createSubSelections(selection).toMap()).containsExactly(1L, selection)
    }

    @Test
    fun createSubSelections_layoutBuilder_missNonCrossedSingleSelection_returnsCrossedSelection() {
        val layout = buildSelectionLayoutForTest { appendInfoForTest(selectableId = 1L) }
        val selection = getSelection(startOffset = 1, endOffset = 0, handlesCrossed = false)
        val actual = layout.createSubSelections(selection).toMap()
        assertThat(actual).hasSize(1)
        assertThat(actual.toList().single().second).isEqualTo(selection.copy(handlesCrossed = true))
    }

    @Test
    fun createSubSelections_layoutBuilder_missCrossedSingleSelection_returnsUncrossedSelection() {
        val layout = buildSelectionLayoutForTest { appendInfoForTest(selectableId = 1L) }
        val selection = getSelection(startOffset = 0, endOffset = 1, handlesCrossed = true)
        val actual = layout.createSubSelections(selection).toMap()
        assertThat(actual).hasSize(1)
        assertThat(actual.toList().single().second)
            .isEqualTo(selection.copy(handlesCrossed = false))
    }

    @Test
    fun createSubSelections_layoutBuilder_selectionInOneSelectable_returnsInputSelection() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(selectableId = 1L)
            appendInfoForTest(selectableId = 2L)
        }
        val selection = getSelection(startSelectableId = 2L, endSelectableId = 2L)
        assertThat(layout.createSubSelections(selection).toMap()).containsExactly(2L, selection)
    }

    @Test
    fun createSubSelections_layoutBuilder_selectionInTwoSelectables() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        val selection = getSelection(startSelectableId = 1L, endSelectableId = 2L)
        assertThat(layout.createSubSelections(selection).toMap())
            .containsExactly(
                1L,
                getSelection(startSelectableId = 1L, endSelectableId = 1L),
                2L,
                getSelection(startSelectableId = 2L, endSelectableId = 2L),
            )
    }

    @Test
    fun createSubSelections_layoutBuilder_selectionInThreeSelectables() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 3L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        val selection = getSelection(startSelectableId = 1L, endSelectableId = 3L)
        assertThat(layout.createSubSelections(selection).toMap())
            .containsExactly(
                1L,
                getSelection(startSelectableId = 1L, endSelectableId = 1L),
                2L,
                getSelection(startSelectableId = 2L, endSelectableId = 2L),
                3L,
                getSelection(startSelectableId = 3L, endSelectableId = 3L),
            )
    }

    @Test
    fun createSubSelections_layoutBuilder_selectionInFourSelectables() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = ON,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 3L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = AFTER,
            )
            appendInfoForTest(
                selectableId = 4L,
                startYHandleDirection = BEFORE,
                endYHandleDirection = ON,
            )
        }
        val selection = getSelection(startSelectableId = 1L, endSelectableId = 4L)
        assertThat(layout.createSubSelections(selection).toMap())
            .containsExactly(
                1L,
                getSelection(startSelectableId = 1L, endSelectableId = 1L),
                2L,
                getSelection(startSelectableId = 2L, endSelectableId = 2L),
                3L,
                getSelection(startSelectableId = 3L, endSelectableId = 3L),
                4L,
                getSelection(startSelectableId = 4L, endSelectableId = 4L),
            )
    }

    @Test
    fun createSubSelections_layoutBuilder_crossedSelectionInOneSelectable() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
        }
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 1L,
                endOffset = 0,
                handlesCrossed = true
            )
        assertThat(layout.createSubSelections(selection).toMap()).containsExactly(1L, selection)
    }

    @Test
    fun createSubSelections_layoutBuilder_crossedSelectionInTwoSelectables() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
        }
        val selection =
            getSelection(
                startSelectableId = 2L,
                startOffset = 5,
                endSelectableId = 1L,
                endOffset = 0,
                handlesCrossed = true
            )
        assertThat(layout.createSubSelections(selection).toMap())
            .containsExactly(
                1L,
                getSelection(
                    startSelectableId = 1L,
                    endSelectableId = 1L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
                2L,
                getSelection(
                    startSelectableId = 2L,
                    endSelectableId = 2L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
            )
    }

    @Test
    fun createSubSelections_layoutBuilder_crossedSelectionInThreeSelectables() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = AFTER,
                endYHandleDirection = BEFORE,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
            appendInfoForTest(
                selectableId = 3L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
        }
        val selection =
            getSelection(
                startSelectableId = 3L,
                startOffset = 5,
                endSelectableId = 1L,
                endOffset = 0,
                handlesCrossed = true
            )
        assertThat(layout.createSubSelections(selection).toMap())
            .containsExactly(
                1L,
                getSelection(
                    startSelectableId = 1L,
                    endSelectableId = 1L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
                2L,
                getSelection(
                    startSelectableId = 2L,
                    endSelectableId = 2L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
                3L,
                getSelection(
                    startSelectableId = 3L,
                    endSelectableId = 3L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
            )
    }

    @Test
    fun createSubSelections_layoutBuilder_crossedSelectionInFourSelectables() {
        val layout = buildSelectionLayoutForTest {
            appendInfoForTest(
                selectableId = 1L,
                startYHandleDirection = AFTER,
                endYHandleDirection = ON,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
            appendInfoForTest(
                selectableId = 2L,
                startYHandleDirection = AFTER,
                endYHandleDirection = BEFORE,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
            appendInfoForTest(
                selectableId = 3L,
                startYHandleDirection = AFTER,
                endYHandleDirection = BEFORE,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
            appendInfoForTest(
                selectableId = 4L,
                startYHandleDirection = ON,
                endYHandleDirection = BEFORE,
                rawStartHandleOffset = 5,
                rawEndHandleOffset = 0,
            )
        }
        val selection =
            getSelection(
                startSelectableId = 4L,
                startOffset = 5,
                endSelectableId = 1L,
                endOffset = 0,
                handlesCrossed = true
            )
        assertThat(layout.createSubSelections(selection).toMap())
            .containsExactly(
                1L,
                getSelection(
                    startSelectableId = 1L,
                    endSelectableId = 1L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
                2L,
                getSelection(
                    startSelectableId = 2L,
                    endSelectableId = 2L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
                3L,
                getSelection(
                    startSelectableId = 3L,
                    endSelectableId = 3L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
                4L,
                getSelection(
                    startSelectableId = 4L,
                    endSelectableId = 4L,
                    startOffset = 5,
                    endOffset = 0,
                    handlesCrossed = true
                ),
            )
    }

    @Test
    fun selection_isCollapsed_nullSelection_returnsTrue() {
        assertThat(null.isCollapsed(getSingleSelectionLayoutFake())).isTrue()
    }

    @Test
    fun selection_isCollapsed_nullLayout_returnsTrue() {
        assertThat(getSelection().isCollapsed(null)).isTrue()
    }

    @Test
    fun selection_isCollapsed_singleLayout_empty_returnsTrue() {
        val selection = getSelection(startOffset = 0, endOffset = 0)
        val layout = getSingleSelectionLayoutFake(text = "")
        assertThat(selection.isCollapsed(layout)).isTrue()
    }

    @Test
    fun selection_isCollapsed_singleLayout_collapsed_returnsTrue() {
        val selection = getSelection(startOffset = 0, endOffset = 0)
        val layout = getSingleSelectionLayoutFake(text = "hello")
        assertThat(selection.isCollapsed(layout)).isTrue()
    }

    @Test
    fun selection_isCollapsed_singleLayout_notCollapsed_returnsFalse() {
        val selection = getSelection(startOffset = 0, endOffset = 5)
        val layout = getSingleSelectionLayoutFake(text = "hello")
        assertThat(selection.isCollapsed(layout)).isFalse()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_twoLayouts_empty_returnsTrue() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 0,
                endSelectableId = 2L,
                endOffset = 0
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = ""),
                        getSelectableInfoFake(selectableId = 2L, text = ""),
                    ),
                startSlot = 1,
                endSlot = 3,
            )
        assertThat(selection.isCollapsed(layout)).isTrue()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_twoLayouts_collapsed_returnsTrue() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 2L,
                endOffset = 0
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = "hello"),
                        getSelectableInfoFake(selectableId = 2L, text = "hello"),
                    ),
                startSlot = 1,
                endSlot = 3,
            )
        assertThat(selection.isCollapsed(layout)).isTrue()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_twoLayouts_notCollapsedInFirst_returnsFalse() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 4,
                endSelectableId = 2L,
                endOffset = 0
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = "hello"),
                        getSelectableInfoFake(selectableId = 2L, text = "hello"),
                    ),
                startSlot = 1,
                endSlot = 3,
            )
        assertThat(selection.isCollapsed(layout)).isFalse()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_twoLayouts_notCollapsedInSecond_returnsFalse() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 2L,
                endOffset = 1
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = "hello"),
                        getSelectableInfoFake(selectableId = 2L, text = "hello"),
                    ),
                startSlot = 1,
                endSlot = 3,
            )
        assertThat(selection.isCollapsed(layout)).isFalse()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_threeLayouts_empty_returnsTrue() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 0,
                endSelectableId = 3L,
                endOffset = 0
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = ""),
                        getSelectableInfoFake(selectableId = 2L, text = ""),
                        getSelectableInfoFake(selectableId = 3L, text = ""),
                    ),
                startSlot = 1,
                endSlot = 5,
            )
        assertThat(selection.isCollapsed(layout)).isTrue()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_threeLayouts_collapsed_returnsTrue() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 3L,
                endOffset = 0
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = "hello"),
                        getSelectableInfoFake(selectableId = 2L, text = ""),
                        getSelectableInfoFake(selectableId = 3L, text = "hello"),
                    ),
                startSlot = 1,
                endSlot = 5,
            )
        assertThat(selection.isCollapsed(layout)).isTrue()
    }

    @Test
    fun selection_isCollapsed_layoutBuilder_threeLayouts_notCollapsed_returnsFalse() {
        val selection =
            getSelection(
                startSelectableId = 1L,
                startOffset = 5,
                endSelectableId = 3L,
                endOffset = 0
            )
        val layout =
            getSelectionLayoutFake(
                infos =
                    listOf(
                        getSelectableInfoFake(selectableId = 1L, text = "hello"),
                        getSelectableInfoFake(selectableId = 2L, text = "."),
                        getSelectableInfoFake(selectableId = 3L, text = "hello"),
                    ),
                startSlot = 1,
                endSlot = 5,
            )
        assertThat(selection.isCollapsed(layout)).isFalse()
    }

    /** Calls [getTextFieldSelectionLayout] to get a [SelectionLayout]. */
    @OptIn(ExperimentalContracts::class)
    private fun buildSelectionLayoutForTest(
        currentPosition: Offset = Offset(25f, 5f),
        previousHandlePosition: Offset = Offset.Unspecified,
        containerCoordinates: LayoutCoordinates = MockCoordinates(),
        isStartHandle: Boolean = false,
        previousSelection: Selection? = null,
        selectableIdOrderingComparator: Comparator<Long> = naturalOrder(),
        block: SelectionLayoutBuilder.() -> Unit,
    ): SelectionLayout {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return buildSelectionLayoutForTestOrNull(
                currentPosition = currentPosition,
                previousHandlePosition = previousHandlePosition,
                containerCoordinates = containerCoordinates,
                isStartHandle = isStartHandle,
                previousSelection = previousSelection,
                selectableIdOrderingComparator = selectableIdOrderingComparator,
                block = block
            )
            .let { assertNotNull(it) }
    }

    /** Calls [getTextFieldSelectionLayout] to get a [SelectionLayout]. */
    @OptIn(ExperimentalContracts::class)
    private fun buildSelectionLayoutForTestOrNull(
        currentPosition: Offset = Offset(25f, 5f),
        previousHandlePosition: Offset = Offset.Unspecified,
        containerCoordinates: LayoutCoordinates = MockCoordinates(),
        isStartHandle: Boolean = false,
        previousSelection: Selection? = null,
        selectableIdOrderingComparator: Comparator<Long> = naturalOrder(),
        block: SelectionLayoutBuilder.() -> Unit,
    ): SelectionLayout? {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return SelectionLayoutBuilder(
                currentPosition = currentPosition,
                previousHandlePosition = previousHandlePosition,
                containerCoordinates = containerCoordinates,
                isStartHandle = isStartHandle,
                previousSelection = previousSelection,
                selectableIdOrderingComparator = selectableIdOrderingComparator,
            )
            .run {
                block()
                build()
            }
    }

    private fun SelectionLayoutBuilder.appendInfoForTest(
        selectableId: Long = 1L,
        text: String = "hello",
        rawStartHandleOffset: Int = 0,
        startXHandleDirection: Direction = ON,
        startYHandleDirection: Direction = ON,
        rawEndHandleOffset: Int = 5,
        endXHandleDirection: Direction = ON,
        endYHandleDirection: Direction = ON,
        rawPreviousHandleOffset: Int = -1,
        rtlRanges: List<IntRange> = emptyList(),
        wordBoundaries: List<TextRange> = listOf(),
        lineBreaks: List<Int> = emptyList(),
    ): SelectableInfo {
        val layoutResult =
            getTextLayoutResultMock(
                text = text,
                rtlCharRanges = rtlRanges,
                wordBoundaries = wordBoundaries,
                lineBreaks = lineBreaks,
            )
        return appendInfo(
            selectableId = selectableId,
            rawStartHandleOffset = rawStartHandleOffset,
            startXHandleDirection = startXHandleDirection,
            startYHandleDirection = startYHandleDirection,
            rawEndHandleOffset = rawEndHandleOffset,
            endXHandleDirection = endXHandleDirection,
            endYHandleDirection = endYHandleDirection,
            rawPreviousHandleOffset = rawPreviousHandleOffset,
            textLayoutResult = layoutResult
        )
    }

    /** Calls [getTextFieldSelectionLayout] to get a [SelectionLayout]. */
    private fun getSingleSelectionLayoutForTest(
        text: String = "hello",
        rawStartHandleOffset: Int = 0,
        rawEndHandleOffset: Int = 5,
        rawPreviousHandleOffset: Int = -1,
        rtlRanges: List<IntRange> = emptyList(),
        wordBoundaries: List<TextRange> = listOf(),
        lineBreaks: List<Int> = emptyList(),
        isStartHandle: Boolean = false,
        previousSelection: Selection? = null,
        isStartOfSelection: Boolean = previousSelection == null,
    ): SelectionLayout {
        val layoutResult =
            getTextLayoutResultMock(
                text = text,
                rtlCharRanges = rtlRanges,
                wordBoundaries = wordBoundaries,
                lineBreaks = lineBreaks,
            )
        return getTextFieldSelectionLayout(
            layoutResult = layoutResult,
            rawStartHandleOffset = rawStartHandleOffset,
            rawEndHandleOffset = rawEndHandleOffset,
            rawPreviousHandleOffset = rawPreviousHandleOffset,
            previousSelectionRange = previousSelection?.toTextRange() ?: TextRange.Zero,
            isStartOfSelection = isStartOfSelection,
            isStartHandle = isStartHandle
        )
    }
}

private fun <T> LongObjectMap<T>.toMap(): Map<Long, T> = buildMap {
    this@toMap.forEach { long, obj -> put(long, obj) }
}

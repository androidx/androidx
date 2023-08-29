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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectableInfoTest {
    @Test
    fun verifySimpleParameters() {
        val text = "hi"
        val selectableInfo = getSelectableInfo(
            text = text,
            selectableId = 1L,
            slot = 1,
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 1,
            rawPreviousHandleOffset = -1,
        )

        assertThat(selectableInfo.selectableId).isEqualTo(1L)
        assertThat(selectableInfo.slot).isEqualTo(1)
        assertThat(selectableInfo.rawStartHandleOffset).isEqualTo(0)
        assertThat(selectableInfo.rawEndHandleOffset).isEqualTo(1)
        assertThat(selectableInfo.rawPreviousHandleOffset).isEqualTo(-1)
        assertThat(selectableInfo.inputText).isEqualTo(text)
        assertThat(selectableInfo.textLength).isEqualTo(2)
    }

    @Test
    fun rawCrossedStatus_whenStartGreaterThanEnd_isCrossed() {
        val selectableInfo = getSelectableInfo(
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 0,
        )

        assertThat(selectableInfo.rawCrossStatus).isEqualTo(CrossStatus.CROSSED)
    }

    @Test
    fun rawCrossedStatus_whenStartLessThanEnd_isNotCrossed() {
        val selectableInfo = getSelectableInfo(
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 1,
        )

        assertThat(selectableInfo.rawCrossStatus).isEqualTo(CrossStatus.NOT_CROSSED)
    }

    @Test
    fun rawCrossedStatus_whenStartEqualToEnd_isCollapsed() {
        val selectableInfo = getSelectableInfo(
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 1,
        )

        assertThat(selectableInfo.rawCrossStatus).isEqualTo(CrossStatus.COLLAPSED)
    }

    @Test
    fun shouldRecomputeSelection_whenUnchanged_isFalse() {
        val info = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        val otherInfo = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        assertThat(info.shouldRecomputeSelection(otherInfo)).isFalse()
    }

    @Test
    fun shouldRecomputeSelection_whenSelectableChanged_isTrue() {
        val info = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        val otherInfo = getSelectableInfo(
            selectableId = 2L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        assertThat(info.shouldRecomputeSelection(otherInfo)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_whenStartHandleChanged_isTrue() {
        val info = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        val otherInfo = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 0,
            rawEndHandleOffset = 2,
        )

        assertThat(info.shouldRecomputeSelection(otherInfo)).isTrue()
    }

    @Test
    fun shouldRecomputeSelection_whenEndHandleChanged_isTrue() {
        val info = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 2,
        )

        val otherInfo = getSelectableInfo(
            selectableId = 1L,
            rawStartHandleOffset = 1,
            rawEndHandleOffset = 3,
        )

        assertThat(info.shouldRecomputeSelection(otherInfo)).isTrue()
    }

    @Test
    fun toAnchor_nonEmptyLine_matchesInfo() {
        val offset = 0
        val selectableId = 1L
        val info = getSelectableInfo(
            selectableId = selectableId,
            rtlRanges = emptyList(),
            rtlLines = emptySet(),
        )

        val expected = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = offset,
            selectableId = selectableId
        )

        assertThat(info.anchorForOffset(offset)).isEqualTo(expected)
    }

    @Test
    fun toAnchor_nonEmptyLine_matchesInfo_rtl() {
        val offset = 0
        val selectableId = 1L
        val info = getSelectableInfo(
            selectableId = selectableId,
            rtlRanges = listOf(0..0),
            rtlLines = emptySet(),
        )

        val expected = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Rtl,
            offset = offset,
            selectableId = selectableId
        )

        assertThat(info.anchorForOffset(offset)).isEqualTo(expected)
    }

    @Test
    fun toAnchor_emptyText_usesParagraphDirection() {
        val offset = 0
        val selectableId = 1L
        val info = getSelectableInfo(
            text = "",
            selectableId = selectableId,
            rtlRanges = emptyList(),
            rtlLines = setOf(0),
        )

        val expected = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Rtl,
            offset = offset,
            selectableId = selectableId
        )

        assertThat(info.anchorForOffset(offset)).isEqualTo(expected)
    }

    @Test
    fun toAnchor_emptyText_usesParagraphDirection_rtl() {
        val offset = 0
        val selectableId = 1L
        val info = getSelectableInfo(
            text = "",
            selectableId = selectableId,
            rtlRanges = listOf(0..0),
            rtlLines = emptySet(),
        )

        val expected = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = offset,
            selectableId = selectableId
        )

        assertThat(info.anchorForOffset(offset)).isEqualTo(expected)
    }

    @Test
    fun toAnchor_emptyLine_usesParagraphDirection() {
        val offset = 6
        val selectableId = 1L
        val info = getSelectableInfo(
            text = "hello\n\nhello",
            selectableId = selectableId,
            rtlRanges = emptyList(),
            rtlLines = setOf(1),
            lineBreaks = listOf(6, 7)
        )

        val expected = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Rtl,
            offset = offset,
            selectableId = selectableId
        )

        assertThat(info.anchorForOffset(offset)).isEqualTo(expected)
    }

    @Test
    fun toAnchor_emptyLine_usesParagraphDirection_rtl() {
        val offset = 6
        val selectableId = 1L
        val info = getSelectableInfo(
            text = "hello\n\nhello",
            selectableId = selectableId,
            rtlRanges = listOf(6..6),
            rtlLines = emptySet(),
            lineBreaks = listOf(6, 7)
        )

        val expected = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = offset,
            selectableId = selectableId
        )

        assertThat(info.anchorForOffset(offset)).isEqualTo(expected)
    }

    @Test
    fun makeSingleLayoutSelection_notCrossed() {
        val start = 0
        val end = 5
        val selectableId = 1L
        val info = getSelectableInfo(selectableId = selectableId)

        val expected = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = start,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = end,
                selectableId = selectableId
            ),
            handlesCrossed = false
        )

        assertThat(info.makeSingleLayoutSelection(start, end)).isEqualTo(expected)
    }

    @Test
    fun makeSingleLayoutSelection_crossed() {
        val start = 5
        val end = 0
        val selectableId = 1L
        val info = getSelectableInfo(selectableId = selectableId)

        val expected = Selection(
            start = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = start,
                selectableId = selectableId
            ),
            end = Selection.AnchorInfo(
                direction = ResolvedTextDirection.Ltr,
                offset = end,
                selectableId = selectableId
            ),
            handlesCrossed = true
        )

        assertThat(info.makeSingleLayoutSelection(start, end)).isEqualTo(expected)
    }

    private fun getSelectableInfo(
        text: String = "hello",
        selectableId: Long = 1L,
        slot: Int = 1,
        rawStartHandleOffset: Int = 0,
        rawEndHandleOffset: Int = 5,
        rawPreviousHandleOffset: Int = -1,
        rtlRanges: List<IntRange> = emptyList(),
        rtlLines: Set<Int> = emptySet(),
        wordBoundaries: List<TextRange> = listOf(),
        lineBreaks: List<Int> = emptyList(),
    ): SelectableInfo = SelectableInfo(
        selectableId = selectableId,
        slot = slot,
        rawStartHandleOffset = rawStartHandleOffset,
        rawEndHandleOffset = rawEndHandleOffset,
        rawPreviousHandleOffset = rawPreviousHandleOffset,
        textLayoutResult = getTextLayoutResultMock(
            text = text,
            rtlCharRanges = rtlRanges,
            wordBoundaries = wordBoundaries,
            lineBreaks = lineBreaks,
            rtlLines = rtlLines,
        ),
    )
}

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
import androidx.collection.emptyLongObjectMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.packInts
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

internal fun getSingleSelectionLayoutFake(
    text: String = "hello",
    rawStartHandleOffset: Int = 0,
    rawEndHandleOffset: Int = 5,
    rawPreviousHandleOffset: Int = -1,
    rtlRanges: List<IntRange> = emptyList(),
    wordBoundaries: List<TextRange> = listOf(),
    lineBreaks: List<Int> = emptyList(),
    crossStatus: CrossStatus = when {
        rawStartHandleOffset < rawEndHandleOffset -> CrossStatus.NOT_CROSSED
        rawStartHandleOffset > rawEndHandleOffset -> CrossStatus.CROSSED
        else -> CrossStatus.COLLAPSED
    },
    isStartHandle: Boolean = false,
    previousSelection: Selection? = null,
    shouldRecomputeSelection: Boolean = true,
    subSelections: LongObjectMap<Selection> = emptyLongObjectMap(),
): SelectionLayout {
    return getSelectionLayoutFake(
        infos = listOf(
            getSelectableInfoFake(
                text = text,
                selectableId = 1,
                slot = 1,
                rawStartHandleOffset = rawStartHandleOffset,
                rawEndHandleOffset = rawEndHandleOffset,
                rawPreviousHandleOffset = rawPreviousHandleOffset,
                rtlRanges = rtlRanges,
                wordBoundaries = wordBoundaries,
                lineBreaks = lineBreaks,
            )
        ),
        currentInfoIndex = 0,
        startSlot = 1,
        endSlot = 1,
        crossStatus = crossStatus,
        isStartHandle = isStartHandle,
        previousSelection = previousSelection,
        shouldRecomputeSelection = shouldRecomputeSelection,
        subSelections = subSelections,
    )
}

internal fun getTextLayoutResultMock(
    text: String = "hello",
    rtlCharRanges: List<IntRange> = emptyList(),
    rtlLines: Set<Int> = emptySet(),
    wordBoundaries: List<TextRange> = listOf(),
    lineBreaks: List<Int> = emptyList(),
): TextLayoutResult {
    val annotatedString = AnnotatedString(text)

    val textLayoutInput = TextLayoutInput(
        text = annotatedString,
        style = TextStyle.Default,
        placeholders = emptyList(),
        maxLines = Int.MAX_VALUE,
        softWrap = false,
        overflow = TextOverflow.Visible,
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        fontFamilyResolver = mock(),
        constraints = Constraints(0L)
    )

    fun lineForOffset(offset: Int): Int {
        var line = 0
        lineBreaks.fastForEach {
            if (it > offset) {
                return line
            }
            line++
        }
        return line
    }

    val multiParagraph = mock<MultiParagraph> {
        on { lineCount }.thenAnswer { _ -> lineBreaks.size + 1 }

        on { getBidiRunDirection(any()) }.thenAnswer { invocation ->
            val offset = invocation.arguments[0] as Int
            if (rtlCharRanges.any { offset in it })
                ResolvedTextDirection.Rtl else ResolvedTextDirection.Ltr
        }

        on { getParagraphDirection(any()) }.thenAnswer { invocation ->
            val offset = invocation.arguments[0] as Int
            val line = lineForOffset(offset)
            if (line in rtlLines) ResolvedTextDirection.Rtl else ResolvedTextDirection.Ltr
        }

        on { getWordBoundary(any()) }.thenAnswer { invocation ->
            val offset = invocation.arguments[0] as Int
            val wordBoundary = wordBoundaries.find { offset in it.start..it.end }
            // Workaround: Mockito doesn't work with inline class now. The packed Long is
            // equal to TextRange(start, end).
            packInts(wordBoundary!!.start, wordBoundary.end)
        }

        on { getLineForOffset(any()) }.thenAnswer { invocation ->
            val offset = invocation.arguments[0] as Int
            lineForOffset(offset)
        }

        on { getLineStart(any()) }.thenAnswer { invocation ->
            val lineIndex = invocation.arguments[0] as Int
            if (lineIndex == 0) 0 else lineBreaks[lineIndex - 1]
        }

        on { getLineEnd(any(), any()) }.thenAnswer { invocation ->
            val lineIndex = invocation.arguments[0] as Int
            if (lineIndex == lineBreaks.size) text.length else lineBreaks[lineIndex] - 1
        }
    }

    return TextLayoutResult(textLayoutInput, multiParagraph, IntSize.Zero)
}

internal fun getSelectableInfoFake(
    text: String = "hello",
    selectableId: Long = 1L,
    slot: Int = 1,
    rawStartHandleOffset: Int = 0,
    rawEndHandleOffset: Int = text.length,
    rawPreviousHandleOffset: Int = -1,
    rtlRanges: List<IntRange> = emptyList(),
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
    ),
)

internal fun getSelectionLayoutFake(
    infos: List<SelectableInfo>,
    startSlot: Int,
    endSlot: Int,
    currentInfoIndex: Int = 0,
    crossStatus: CrossStatus = when {
        startSlot < endSlot -> CrossStatus.NOT_CROSSED
        startSlot > endSlot -> CrossStatus.CROSSED
        else -> infos.single().rawCrossStatus
    },
    startInfo: SelectableInfo =
        with(infos) { if (crossStatus == CrossStatus.CROSSED) last() else first() },
    endInfo: SelectableInfo =
        with(infos) { if (crossStatus == CrossStatus.CROSSED) first() else last() },
    firstInfo: SelectableInfo = if (crossStatus == CrossStatus.CROSSED) endInfo else startInfo,
    lastInfo: SelectableInfo = if (crossStatus == CrossStatus.CROSSED) startInfo else endInfo,
    middleInfos: List<SelectableInfo> =
        if (infos.size < 2) emptyList() else infos.subList(1, infos.size - 1),
    isStartHandle: Boolean = false,
    previousSelection: Selection? = null,
    shouldRecomputeSelection: Boolean = true,
    subSelections: LongObjectMap<Selection> = emptyLongObjectMap(),
): SelectionLayout = FakeSelectionLayout(
    size = infos.size,
    crossStatus = crossStatus,
    startSlot = startSlot,
    endSlot = endSlot,
    startInfo = startInfo,
    endInfo = endInfo,
    currentInfo = infos[currentInfoIndex],
    firstInfo = firstInfo,
    lastInfo = lastInfo,
    middleInfos = middleInfos,
    isStartHandle = isStartHandle,
    previousSelection = previousSelection,
    shouldRecomputeSelection = shouldRecomputeSelection,
    subSelections = subSelections,
)

internal class FakeSelectionLayout(
    override val size: Int,
    override val crossStatus: CrossStatus,
    override val startSlot: Int,
    override val endSlot: Int,
    override val startInfo: SelectableInfo,
    override val endInfo: SelectableInfo,
    override val currentInfo: SelectableInfo,
    override val firstInfo: SelectableInfo,
    override val lastInfo: SelectableInfo,
    override val isStartHandle: Boolean,
    override val previousSelection: Selection?,
    private val middleInfos: List<SelectableInfo>,
    private val shouldRecomputeSelection: Boolean,
    private val subSelections: LongObjectMap<Selection>,
) : SelectionLayout {
    override fun createSubSelections(selection: Selection): LongObjectMap<Selection> = subSelections
    override fun forEachMiddleInfo(block: (SelectableInfo) -> Unit) {
        middleInfos.forEach(block)
    }

    override fun shouldRecomputeSelection(other: SelectionLayout?): Boolean =
        shouldRecomputeSelection
}

internal fun getSelection(
    startOffset: Int = 0,
    endOffset: Int = 5,
    startSelectableId: Long = 1L,
    endSelectableId: Long = 1L,
    handlesCrossed: Boolean = startSelectableId == endSelectableId && startOffset > endOffset,
    startLayoutDirection: ResolvedTextDirection = ResolvedTextDirection.Ltr,
    endLayoutDirection: ResolvedTextDirection = ResolvedTextDirection.Ltr,
): Selection = Selection(
    start = Selection.AnchorInfo(
        direction = startLayoutDirection,
        offset = startOffset,
        selectableId = startSelectableId,
    ),
    end = Selection.AnchorInfo(
        direction = endLayoutDirection,
        offset = endOffset,
        selectableId = endSelectableId,
    ),
    handlesCrossed = handlesCrossed,
)

internal class FakeSelectable : Selectable {
    override var selectableId = 0L
    var getTextCalledTimes = 0
    var textToReturn: AnnotatedString? = null

    var rawStartHandleOffset = 0
    var startXHandleDirection = Direction.ON
    var startYHandleDirection = Direction.ON
    var rawEndHandleOffset = 0
    var endXHandleDirection = Direction.ON
    var endYHandleDirection = Direction.ON
    var rawPreviousHandleOffset = -1 // -1 = no previous offset
    var layoutCoordinatesToReturn: LayoutCoordinates? = null
    var textLayoutResultToReturn: TextLayoutResult? = null
    var boundingBoxes: Map<Int, Rect> = emptyMap()

    private val selectableKey = 1L
    var fakeSelectAllSelection: Selection? = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = selectableKey
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 10,
            selectableId = selectableKey
        )
    )

    override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder) {
        builder.appendInfo(
            selectableKey,
            rawStartHandleOffset,
            startXHandleDirection,
            startYHandleDirection,
            rawEndHandleOffset,
            endXHandleDirection,
            endYHandleDirection,
            rawPreviousHandleOffset,
            getTextLayoutResultMock(),
        )
    }

    override fun getSelectAllSelection(): Selection? {
        return fakeSelectAllSelection
    }

    override fun getText(): AnnotatedString {
        getTextCalledTimes++
        return textToReturn!!
    }

    override fun getLayoutCoordinates(): LayoutCoordinates? {
        return layoutCoordinatesToReturn
    }

    override fun textLayoutResult(): TextLayoutResult? {
        return textLayoutResultToReturn
    }

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset {
        return Offset.Zero
    }

    override fun getBoundingBox(offset: Int): Rect {
        return boundingBoxes[offset] ?: Rect.Zero
    }

    override fun getLineLeft(offset: Int): Float {
        return 0f
    }

    override fun getLineRight(offset: Int): Float {
        return 0f
    }

    override fun getCenterYForOffset(offset: Int): Float {
        return 0f
    }

    override fun getRangeOfLineContaining(offset: Int): TextRange {
        return TextRange.Zero
    }

    override fun getLastVisibleOffset(): Int {
        return 0
    }

    fun clear() {
        getTextCalledTimes = 0
        textToReturn = null
    }
}

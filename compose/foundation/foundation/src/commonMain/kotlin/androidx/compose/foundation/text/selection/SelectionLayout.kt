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

import androidx.collection.LongIntMap
import androidx.collection.LongObjectMap
import androidx.collection.MutableLongIntMap
import androidx.collection.MutableLongObjectMap
import androidx.collection.longObjectMapOf
import androidx.collection.mutableLongIntMapOf
import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.text.selection.Direction.AFTER
import androidx.compose.foundation.text.selection.Direction.BEFORE
import androidx.compose.foundation.text.selection.Direction.ON
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.util.fastForEachIndexed

/**
 * Selection data around how the pointer relates to the actual positions of the Text components.
 *
 * # Explanation of Slots
 *
 * The Slot value is meant to sit either *on* an index or *between* indices. The former means the
 * pointer is on a `Text` (like slot value `1` and index `0` below). The latter means the pointer is
 * not on a `Text`, but between `Text`s (like slot value `0` or `2` below). So a slot value of `2`
 * means that the pointer is between the `Text`s at index `0` and `1`, perhaps in padding or a
 * non-selectable `Text`.
 *
 * ```
 * slot value  0  1  2  3  4  5  6  7  8  9  10
 * info index     0     1     2     3     4
 * ```
 *
 * ## Mappings:
 * The `X` represents an impossible slot assignment The `|`, `/`, and `\` represent a slot mapping.
 *
 * ### Mapping minimum slot:
 * ```
 * slot value  0  1  2  3  4  5  6  7  8  9  10
 *              \ |   \ |   \ |   \ |   \ | X
 * info index     0     1     2     3     4
 *
 * min-slot index = slot / 2
 * ```
 *
 * Minimum slot cannot be after the final `Text` (index `4`).
 *
 * ### Mapping maximum slot:
 * ```
 * slot value  0  1  2  3  4  5  6  7  8  9  10
 *              X | /   | /   | /   | /   | /
 * info index     0     1     2     3     4
 * max-slot index = (slot - 1) / 2
 * ```
 *
 * Maximum slot cannot be before the first `Text` (index `0`).
 *
 * ## Assertions
 * * The non-dragging slot should always be directly on a text (odd) because the non-dragging handle
 *   must be anchored somewhere.
 *     * Because of this, we can determine that if `startSlot == endSlot` then it also follows that
 *       `startSlot` and `endSlot` are even.
 */
internal interface SelectionLayout {
    /** The number of [SelectableInfo]s in this [SelectionLayout]. */
    val size: Int

    /** The slot of the start anchor. */
    val startSlot: Int

    /** The slot of the end anchor. */
    val endSlot: Int

    /** The [CrossStatus] of this layout as determined by the slot/offsets. */
    val crossStatus: CrossStatus

    /** The [SelectableInfo] that the start is on. */
    val startInfo: SelectableInfo

    /** The [SelectableInfo] that the end is on. */
    val endInfo: SelectableInfo

    /** The [SelectableInfo] that the start/end is on as determined by [isStartHandle]. */
    val currentInfo: SelectableInfo

    /** The [SelectableInfo] for the first selectable on the screen. */
    val firstInfo: SelectableInfo

    /** The [SelectableInfo] for the last selectable on the screen. */
    val lastInfo: SelectableInfo

    /**
     * Run a function on every [SelectableInfo] between [firstInfo] and [lastInfo] (not including
     * [firstInfo]/[lastInfo]).
     */
    fun forEachMiddleInfo(block: (SelectableInfo) -> Unit)

    /** Whether the start or end anchor is currently being moved. */
    val isStartHandle: Boolean

    /** The previous [Selection] that we are modifying. */
    val previousSelection: Selection?

    /**
     * Whether this layout, compared to another layout, has any relevant changes that would require
     * recomputing selection.
     *
     * @param other the selection layout to check for changes compared to this one
     */
    fun shouldRecomputeSelection(other: SelectionLayout?): Boolean

    /**
     * Splits a selection into a Map of selectable ID to Selections limited to that selectable ID.
     *
     * @param selection The selection to turn into subSelections
     */
    fun createSubSelections(selection: Selection): LongObjectMap<Selection>
}

private class MultiSelectionLayout(
    val selectableIdToInfoListIndex: LongIntMap,
    val infoList: List<SelectableInfo>,
    override val startSlot: Int,
    override val endSlot: Int,
    override val isStartHandle: Boolean,
    override val previousSelection: Selection?,
) : SelectionLayout {
    init {
        check(infoList.size > 1) {
            "MultiSelectionLayout requires an infoList size greater than 1, was ${infoList.size}."
        }
    }

    // Most of these properties are unused unless shouldRecomputeSelection returns true,
    // hence why getters are used everywhere.

    override val size
        get() = infoList.size

    override val crossStatus: CrossStatus
        get() =
            when {
                startSlot < endSlot -> CrossStatus.NOT_CROSSED
                startSlot > endSlot -> CrossStatus.CROSSED
                // because one of the slots is not-dragging, it must be on a text directly
                // because one of the slots is on a text directly and the start/end slots are equal,
                // they both must be odd. Given this, dividing the slot by 2 should give us the
                // correct
                // info index.
                else -> infoList[startSlot / 2].rawCrossStatus
            }

    override val startInfo: SelectableInfo
        get() = infoList[startOrEndSlotToIndex(startSlot, isStartSlot = true)]

    override val endInfo: SelectableInfo
        get() = infoList[startOrEndSlotToIndex(endSlot, isStartSlot = false)]

    override val currentInfo: SelectableInfo
        get() = if (isStartHandle) startInfo else endInfo

    override val firstInfo: SelectableInfo
        get() = if (crossStatus == CrossStatus.CROSSED) endInfo else startInfo

    override val lastInfo: SelectableInfo
        get() = if (crossStatus == CrossStatus.CROSSED) startInfo else endInfo

    override fun forEachMiddleInfo(block: (SelectableInfo) -> Unit) {
        val minIndex = getInfoListIndexBySelectableId(firstInfo.selectableId)
        val maxIndex = getInfoListIndexBySelectableId(lastInfo.selectableId)
        if (minIndex + 1 >= maxIndex) {
            return
        }

        for (i in minIndex + 1 until maxIndex) {
            block(infoList[i])
        }
    }

    override fun shouldRecomputeSelection(other: SelectionLayout?): Boolean =
        previousSelection == null ||
            other == null ||
            other !is MultiSelectionLayout ||
            isStartHandle != other.isStartHandle ||
            startSlot != other.startSlot ||
            endSlot != other.endSlot ||
            shouldAnyInfoRecomputeSelection(other)

    private fun shouldAnyInfoRecomputeSelection(other: MultiSelectionLayout): Boolean {
        if (size != other.size) return true
        for (i in infoList.indices) {
            val thisInfo = infoList[i]
            val otherInfo = other.infoList[i]
            if (thisInfo.shouldRecomputeSelection(otherInfo)) {
                return true
            }
        }
        return false
    }

    override fun createSubSelections(selection: Selection): LongObjectMap<Selection> =
        // Selection is within one selectable, we can return a singleton map of this selection.
        if (selection.start.selectableId == selection.end.selectableId) {
            // this check, if not passed, leads to exceptions when selection
            // highlighting is rendered, so check here instead.
            check(
                (selection.handlesCrossed && selection.start.offset >= selection.end.offset) ||
                    (!selection.handlesCrossed && selection.start.offset <= selection.end.offset)
            ) {
                "unexpectedly miss-crossed selection: $selection"
            }
            longObjectMapOf(selection.start.selectableId, selection)
        } else
            mutableLongObjectMapOf<Selection>().apply {
                val minAnchor = with(selection) { if (handlesCrossed) end else start }
                createAndPutSubSelection(
                    selection,
                    firstInfo,
                    minAnchor.offset,
                    firstInfo.textLength
                )

                forEachMiddleInfo { info ->
                    createAndPutSubSelection(selection, info, minOffset = 0, info.textLength)
                }

                val maxAnchor = with(selection) { if (handlesCrossed) start else end }
                createAndPutSubSelection(selection, lastInfo, minOffset = 0, maxAnchor.offset)
            }

    private fun MutableLongObjectMap<Selection>.createAndPutSubSelection(
        selection: Selection,
        info: SelectableInfo,
        minOffset: Int,
        maxOffset: Int
    ) {
        val subSelection =
            if (selection.handlesCrossed) {
                info.makeSingleLayoutSelection(start = maxOffset, end = minOffset)
            } else {
                info.makeSingleLayoutSelection(start = minOffset, end = maxOffset)
            }

        // this check, if not passed, leads to exceptions when selection
        // highlighting is rendered, so check here instead.
        check(minOffset <= maxOffset) {
            "minOffset should be less than or equal to maxOffset: $subSelection"
        }

        put(info.selectableId, subSelection)
    }

    override fun toString(): String =
        "MultiSelectionLayout(isStartHandle=$isStartHandle, " +
            "startPosition=${(startSlot + 1).toFloat() / 2}, " +
            "endPosition=${(endSlot + 1).toFloat() / 2}, " +
            "crossed=$crossStatus, " +
            "infos=${
            buildString {
                append("[\n\t")
                var first = true
                infoList
                    .fastForEachIndexed { index, info ->
                        if (first) {
                            first = false
                        } else {
                            append(",\n\t")
                        }
                        append("${(index + 1)} -> $info")
                    }
                append("\n]")
            }
        })"

    private fun startOrEndSlotToIndex(slot: Int, isStartSlot: Boolean): Int =
        slotToIndex(
            slot = slot,
            isMinimumSlot =
                when (crossStatus) {
                    // collapsed: doesn't matter whether true or false, it will result in the same
                    // index
                    CrossStatus.COLLAPSED -> true
                    CrossStatus.NOT_CROSSED -> isStartSlot
                    CrossStatus.CROSSED -> !isStartSlot
                }
        )

    private fun slotToIndex(slot: Int, isMinimumSlot: Boolean): Int {
        val slotAdjustment = if (isMinimumSlot) 0 else 1
        return (slot - slotAdjustment) / 2
    }

    private fun getInfoListIndexBySelectableId(id: Long): Int =
        try {
            selectableIdToInfoListIndex[id]
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("Invalid selectableId: $id", e)
        }
}

/**
 * Create a selection layout that has only one slot.
 *
 * @param isStartHandle whether this is the start or end anchor
 * @param previousSelection the previous selection
 * @param info the single [SelectableInfo]
 */
private class SingleSelectionLayout(
    override val isStartHandle: Boolean,
    override val startSlot: Int,
    override val endSlot: Int,
    override val previousSelection: Selection?,
    private val info: SelectableInfo,
) : SelectionLayout {
    companion object {
        const val DEFAULT_SLOT = 1
        const val DEFAULT_SELECTABLE_ID = 1L
    }

    override val size
        get() = 1

    override val crossStatus: CrossStatus
        get() =
            when {
                startSlot < endSlot -> CrossStatus.NOT_CROSSED
                startSlot > endSlot -> CrossStatus.CROSSED
                else -> info.rawCrossStatus
            }

    override val startInfo: SelectableInfo
        get() = info

    override val endInfo: SelectableInfo
        get() = info

    override val currentInfo: SelectableInfo
        get() = info

    override val firstInfo: SelectableInfo
        get() = info

    override val lastInfo: SelectableInfo
        get() = info

    override fun forEachMiddleInfo(block: (SelectableInfo) -> Unit) {
        // there are no middle infos, so do nothing
    }

    override fun shouldRecomputeSelection(other: SelectionLayout?): Boolean =
        previousSelection == null ||
            other == null ||
            other !is SingleSelectionLayout ||
            startSlot != other.startSlot ||
            endSlot != other.endSlot ||
            isStartHandle != other.isStartHandle ||
            info.shouldRecomputeSelection(other.info)

    override fun createSubSelections(selection: Selection): LongObjectMap<Selection> {
        val finalSelection =
            selection.run {
                // uncross handles if necessary
                if (
                    (!handlesCrossed && start.offset > end.offset) ||
                        (handlesCrossed && start.offset <= end.offset)
                ) {
                    copy(handlesCrossed = !handlesCrossed)
                } else {
                    this
                }
            }
        return longObjectMapOf(info.selectableId, finalSelection)
    }

    override fun toString(): String =
        "SingleSelectionLayout(isStartHandle=$isStartHandle, crossed=$crossStatus, info=\n\t$info)"
}

/**
 * Create a selection layout that has only one slot.
 *
 * This is intended for TextField, where multiple selectables is of no concern.
 *
 * @param layoutResult the [TextLayoutResult] for the text field
 * @param rawStartHandleOffset the index of the start handle
 * @param rawEndHandleOffset the index of the end handle
 * @param rawPreviousHandleOffset the previous handle offset based on [isStartHandle], or
 *   [UNASSIGNED_SLOT] if none
 * @param previousSelectionRange the previous selection
 * @param isStartOfSelection whether this is the start of a selection gesture (no previous context)
 * @param isStartHandle whether this is the start or end anchor
 */
internal fun getTextFieldSelectionLayout(
    layoutResult: TextLayoutResult,
    rawStartHandleOffset: Int,
    rawEndHandleOffset: Int,
    rawPreviousHandleOffset: Int,
    previousSelectionRange: TextRange,
    isStartOfSelection: Boolean,
    isStartHandle: Boolean,
): SelectionLayout =
    SingleSelectionLayout(
        isStartHandle = isStartHandle,
        startSlot = SingleSelectionLayout.DEFAULT_SLOT,
        endSlot = SingleSelectionLayout.DEFAULT_SLOT,
        previousSelection =
            if (isStartOfSelection) null
            else
                Selection(
                    start =
                        Selection.AnchorInfo(
                            layoutResult.getTextDirectionForOffset(previousSelectionRange.start),
                            previousSelectionRange.start,
                            SingleSelectionLayout.DEFAULT_SELECTABLE_ID
                        ),
                    end =
                        Selection.AnchorInfo(
                            layoutResult.getTextDirectionForOffset(previousSelectionRange.end),
                            previousSelectionRange.end,
                            SingleSelectionLayout.DEFAULT_SELECTABLE_ID
                        ),
                    handlesCrossed = previousSelectionRange.reversed
                ),
        info =
            SelectableInfo(
                selectableId = SingleSelectionLayout.DEFAULT_SELECTABLE_ID,
                slot = SingleSelectionLayout.DEFAULT_SLOT,
                rawStartHandleOffset = rawStartHandleOffset,
                rawEndHandleOffset = rawEndHandleOffset,
                textLayoutResult = layoutResult,
                rawPreviousHandleOffset = rawPreviousHandleOffset
            ),
    )

/** Whether something is crossed as determined by the position of the start/end. */
internal enum class CrossStatus {
    /** The start comes after the end. */
    CROSSED,

    /** The start comes before the end. */
    NOT_CROSSED,

    /** The start is the same as the end. */
    COLLAPSED
}

/** Slot has not been assigned yet */
internal const val UNASSIGNED_SLOT = -1

/**
 * A builder for [SelectionLayout] that ensures the data structures and slots are properly
 * constructed.
 *
 * @param previousHandlePosition the previous handle position matching the handle directed to by
 *   [isStartHandle]
 * @param containerCoordinates the coordinates of the [SelectionContainer] for converting
 *   [SelectionContainer] coordinates to their respective [Selectable] coordinates
 * @param isStartHandle whether the currently pressed/clicked handle is the start
 * @param selectableIdOrderingComparator determines the ordering of selectables by their IDs
 */
internal class SelectionLayoutBuilder(
    val currentPosition: Offset,
    val previousHandlePosition: Offset,
    val containerCoordinates: LayoutCoordinates,
    val isStartHandle: Boolean,
    val previousSelection: Selection?,
    val selectableIdOrderingComparator: Comparator<Long>
) {
    private val selectableIdToInfoListIndex: MutableLongIntMap = mutableLongIntMapOf()
    private val infoList: MutableList<SelectableInfo> = mutableListOf()
    private var startSlot: Int = UNASSIGNED_SLOT
    private var endSlot: Int = UNASSIGNED_SLOT
    private var currentSlot: Int = UNASSIGNED_SLOT

    /**
     * Finishes building the [SelectionLayout] and returns it.
     *
     * @return the [SelectionLayout] or null if no [SelectableInfo]s were added.
     */
    fun build(): SelectionLayout? {
        val lastSlot = currentSlot + 1
        return when (infoList.size) {
            0 -> {
                return null
            }
            1 -> {
                SingleSelectionLayout(
                    info = infoList.single(),
                    startSlot = if (startSlot == UNASSIGNED_SLOT) lastSlot else startSlot,
                    endSlot = if (endSlot == UNASSIGNED_SLOT) lastSlot else endSlot,
                    previousSelection = previousSelection,
                    isStartHandle = isStartHandle,
                )
            }
            else -> {
                MultiSelectionLayout(
                    selectableIdToInfoListIndex = selectableIdToInfoListIndex,
                    infoList = infoList,
                    startSlot = if (startSlot == UNASSIGNED_SLOT) lastSlot else startSlot,
                    endSlot = if (endSlot == UNASSIGNED_SLOT) lastSlot else endSlot,
                    isStartHandle = isStartHandle,
                    previousSelection = previousSelection,
                )
            }
        }
    }

    /** Appends a selection info to this builder. */
    fun appendInfo(
        selectableId: Long,
        rawStartHandleOffset: Int,
        startXHandleDirection: Direction,
        startYHandleDirection: Direction,
        rawEndHandleOffset: Int,
        endXHandleDirection: Direction,
        endYHandleDirection: Direction,
        rawPreviousHandleOffset: Int,
        textLayoutResult: TextLayoutResult,
    ): SelectableInfo {
        // We need currentSlot to equal the slot of the "last" info when getLayout is called,
        // so increment this before adding the info and leave the correct slot in place at the end.
        currentSlot += 2

        val selectableInfo =
            SelectableInfo(
                selectableId = selectableId,
                slot = currentSlot,
                rawStartHandleOffset = rawStartHandleOffset,
                rawEndHandleOffset = rawEndHandleOffset,
                rawPreviousHandleOffset = rawPreviousHandleOffset,
                textLayoutResult = textLayoutResult,
            )

        startSlot = updateSlot(startSlot, startXHandleDirection, startYHandleDirection)
        endSlot = updateSlot(endSlot, endXHandleDirection, endYHandleDirection)
        selectableIdToInfoListIndex[selectableId] = infoList.size
        infoList += selectableInfo
        return selectableInfo
    }

    /**
     * Find the slot for a selectable given the current position's directions from the selectable.
     *
     * The selectables must be ordered in the order in which they would be selected, and then this
     * function should be called for each of those selectables.
     *
     * It is expected that the input [slot] is also assigned the result of this function.
     *
     * This function is stateful.
     *
     * @param slot the current value of this slot.
     * @param xPositionDirection Where the x-position is relative to the selectable
     * @param yPositionDirection Where the y-position is relative to the selectable
     */
    private fun updateSlot(
        slot: Int,
        xPositionDirection: Direction,
        yPositionDirection: Direction,
    ): Int {
        if (slot != UNASSIGNED_SLOT) {
            // don't overwrite if the slot has already been determined
            return slot
        }

        // slot has not been determined yet,
        // see if we are on or past the selectable we are looking for
        return when (resolve2dDirection(xPositionDirection, yPositionDirection)) {
            // If we get here, that means we never found a selectable that contains our gesture
            // position. This is the first selectable that is after the position,
            // so our slot must be between the previous and current selectables.
            BEFORE -> currentSlot - 1

            // The gesture position is directly on this selectable, so use this one.
            ON -> currentSlot

            // keep looking
            AFTER -> slot
        }
    }
}

/** Where the position of a cursor/press is compared to a selectable. */
internal enum class Direction {
    /** The cursor/press is before the selectable */
    BEFORE,

    /** The cursor/press is on the selectable */
    ON,

    /** The cursor/press is after the selectable */
    AFTER
}

/**
 * Determine direction based on an x/y direction.
 *
 * This will use the [y] direction unless it is [ON], in which case it will use the [x] direction.
 */
internal fun resolve2dDirection(x: Direction, y: Direction): Direction =
    when (y) {
        BEFORE -> BEFORE
        ON ->
            when (x) {
                BEFORE -> BEFORE
                ON -> ON
                AFTER -> AFTER
            }
        AFTER -> AFTER
    }

/** Data about a specific selectable within a [SelectionLayout]. */
internal class SelectableInfo(
    val selectableId: Long,
    val slot: Int,
    val rawStartHandleOffset: Int,
    val rawEndHandleOffset: Int,
    val rawPreviousHandleOffset: Int,
    val textLayoutResult: TextLayoutResult,
) {

    /** The [String] in the selectable. */
    val inputText: String
        get() = textLayoutResult.layoutInput.text.text

    /** The length of the [String] in the selectable. */
    val textLength: Int
        get() = inputText.length

    /** Whether the raw offsets of this info are crossed. */
    val rawCrossStatus: CrossStatus
        get() =
            when {
                rawStartHandleOffset < rawEndHandleOffset -> CrossStatus.NOT_CROSSED
                rawStartHandleOffset > rawEndHandleOffset -> CrossStatus.CROSSED
                else -> CrossStatus.COLLAPSED
            }

    private val startRunDirection
        get() = textLayoutResult.getTextDirectionForOffset(rawStartHandleOffset)

    private val endRunDirection
        get() = textLayoutResult.getTextDirectionForOffset(rawEndHandleOffset)

    /**
     * Whether this info, compared to another info, has any relevant changes that would require
     * recomputing selection.
     *
     * @param other the selectable info to check for changes compared to this one
     */
    fun shouldRecomputeSelection(other: SelectableInfo): Boolean =
        selectableId != other.selectableId ||
            rawStartHandleOffset != other.rawStartHandleOffset ||
            rawEndHandleOffset != other.rawEndHandleOffset

    /** Get a [Selection.AnchorInfo] for this [SelectableInfo] at the given [offset]. */
    fun anchorForOffset(offset: Int): Selection.AnchorInfo =
        Selection.AnchorInfo(
            direction = textLayoutResult.getTextDirectionForOffset(offset),
            offset = offset,
            selectableId = selectableId
        )

    /**
     * Get a [Selection] within the selectable represented by this [SelectableInfo] for the given
     * [start] and [end] offsets.
     */
    fun makeSingleLayoutSelection(start: Int, end: Int): Selection =
        Selection(
            start = anchorForOffset(start),
            end = anchorForOffset(end),
            handlesCrossed = start > end
        )

    override fun toString(): String =
        "SelectionInfo(id=$selectableId, " +
            "range=($rawStartHandleOffset-$startRunDirection,$rawEndHandleOffset-$endRunDirection), " +
            "prevOffset=$rawPreviousHandleOffset)"
}

/**
 * Get the text direction for a given offset.
 *
 * This simply calls [TextLayoutResult.getBidiRunDirection] with one exception, if the offset is an
 * empty line, then we defer to [TextLayoutResult.multiParagraph] and
 * [androidx.compose.ui.text.MultiParagraph.getParagraphDirection]. This is because an empty line
 * always resolves to LTR, even if the paragraph is RTL.
 */
// TODO(b/295197585)
//   Can this logic be moved to a new method in `androidx.compose.ui.text.Paragraph`?
private fun TextLayoutResult.getTextDirectionForOffset(offset: Int): ResolvedTextDirection =
    if (isOffsetAnEmptyLine(offset)) getParagraphDirection(offset) else getBidiRunDirection(offset)

private fun TextLayoutResult.isOffsetAnEmptyLine(offset: Int): Boolean =
    layoutInput.text.isEmpty() ||
        getLineForOffset(offset).let { currentLine ->
            // verify the previous and next offsets either don't exist because they're at a boundary
            // or that they are different lines than the current line.
            (offset == 0 || currentLine != getLineForOffset(offset - 1)) &&
                (offset == layoutInput.text.length || currentLine != getLineForOffset(offset + 1))
        }

/**
 * Verify that the selection is truly collapsed.
 *
 * If the selection is contained within one selectable, this simply checks if the offsets are equal.
 *
 * If the Selection spans multiple selectables, then this will verify that every selected selectable
 * contains a zero-width selection.
 */
internal fun Selection?.isCollapsed(layout: SelectionLayout?): Boolean {
    this ?: return true
    layout ?: return true

    // Selection is within one selectable, simply check if the offsets are the same.
    if (start.selectableId == end.selectableId) {
        return start.offset == end.offset
    }

    // check that maxAnchor offset is 0, else the selection cannot be collapsed.
    val maxAnchor = if (handlesCrossed) start else end
    if (maxAnchor.offset != 0) {
        return false
    }

    // check that the minAnchor offset is equal to the length of the text,
    // else the selection is not collapsed
    val minAnchor = if (handlesCrossed) end else start
    if (layout.firstInfo.textLength != minAnchor.offset) {
        return false
    }

    // Every selectable between the min and max must have empty text,
    // else there is some text selected.
    var allTextsEmpty = true
    layout.forEachMiddleInfo {
        if (it.inputText.isNotEmpty()) {
            allTextsEmpty = false
        }
    }

    return allTextsEmpty
}

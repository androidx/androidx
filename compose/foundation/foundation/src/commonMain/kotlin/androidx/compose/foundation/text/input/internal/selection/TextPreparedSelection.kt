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

package androidx.compose.foundation.text.input.internal.selection

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.findFollowingBreak
import androidx.compose.foundation.text.findParagraphEnd
import androidx.compose.foundation.text.findParagraphStart
import androidx.compose.foundation.text.findPrecedingBreak
import androidx.compose.foundation.text.input.TextFieldCharSequence
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Deletion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Insertion
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Replacement
import androidx.compose.foundation.text.input.internal.IndexTransformationType.Untransformed
import androidx.compose.foundation.text.input.internal.SelectionWedgeAffinity
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.WedgeAffinity
import androidx.compose.foundation.text.input.internal.getIndexTransformationType
import androidx.compose.foundation.text.input.internal.selection.TextFieldPreparedSelection.Companion.NoCharacterFound
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline
import kotlin.math.abs

/**
 * [TextFieldPreparedSelection] provides a scope for many selection-related operations. However,
 * some vertical cursor operations like moving between lines or page up and down require a cache of
 * X position in text to remember where to move the cursor in next line.
 * [TextFieldPreparedSelection] is a disposable scope that cannot hold its own state. This class
 * helps to pass a cached X value between selection operations in different scopes.
 */
internal class TextFieldPreparedSelectionState {
    /**
     * it's set at the start of vertical navigation and used as the preferred value to set a new
     * cursor position.
     */
    var cachedX: Float = Float.NaN

    /** Remove and forget the cached X used for vertical navigation. */
    fun resetCachedX() {
        cachedX = Float.NaN
    }
}

/**
 * This utility class implements many selection-related operations on text (including basic cursor
 * movements and deletions) and combines them, taking into account how the text was rendered. So,
 * for example, [moveCursorToLineEnd] moves it to the visual line end.
 *
 * For many of these operations, it's particularly important to keep the difference between
 * selection start and selection end. In some systems, they are called "anchor" and "caret"
 * respectively. For example, for selection from scratch, after [moveCursorLeftByWord]
 * [moveCursorRightByChar] will move the left side of the selection, but after
 * [moveCursorRightByWord] the right one.
 *
 * @param state Transformed version of TextFieldState that helps to manipulate underlying buffer
 *   through transformed coordinates.
 * @param textLayoutResult Visual representation of text inside [state]. Used to calculate line and
 *   paragraph metrics.
 * @param isFromSoftKeyboard Whether the source event that created this selection context is coming
 *   from the IME.
 * @param visibleTextLayoutHeight Height of the visible area of text inside TextField to decide
 *   where cursor needs to move when page up/down is requested.
 * @param textPreparedSelectionState An object that holds any context that needs to be long lived
 *   between successive [TextFieldPreparedSelection]s, e.g. original X position of the cursor while
 *   moving the cursor up/down.
 */
internal class TextFieldPreparedSelection(
    private val state: TransformedTextFieldState,
    private val textLayoutResult: TextLayoutResult?,
    private val isFromSoftKeyboard: Boolean,
    private val visibleTextLayoutHeight: Float,
    private val textPreparedSelectionState: TextFieldPreparedSelectionState
) {
    /**
     * Read the value from state without read observation to not accidentally cause recompositions.
     * Freezing the initial value is necessary to make atomic operations in the scope of this
     * [TextFieldPreparedSelection]. It is also used to make comparison between the initial state
     * and the modified state of selection and content.
     */
    val initialValue: TextFieldCharSequence

    val initialWedgeAffinity: SelectionWedgeAffinity

    init {
        Snapshot.withoutReadObservation {
            initialValue = state.visualText
            initialWedgeAffinity = state.selectionWedgeAffinity
        }
    }

    /** Current active selection in the context of this [TextFieldPreparedSelection] */
    var selection = initialValue.selection

    var wedgeAffinity: WedgeAffinity? = null

    /** Initial text value. */
    private val text: String = initialValue.text.toString()

    /**
     * Only apply the given [block] if the text is not empty.
     *
     * @param resetCachedX Whether to reset the cachedX parameter in
     *   [TextFieldPreparedSelectionState].
     */
    private inline fun applyIfNotEmpty(
        resetCachedX: Boolean = true,
        block: TextFieldPreparedSelection.() -> Unit
    ): TextFieldPreparedSelection {
        if (resetCachedX) {
            textPreparedSelectionState.resetCachedX()
        }
        if (text.isNotEmpty()) {
            this.block()
        }
        return this
    }

    fun selectAll() = applyIfNotEmpty { selection = TextRange(0, text.length) }

    fun deselect() = applyIfNotEmpty { selection = TextRange(selection.end) }

    fun moveCursorLeftByChar() =
        if (isLtr()) {
            moveCursorPrevByChar()
        } else {
            moveCursorNextByChar()
        }

    fun moveCursorRightByChar() =
        if (isLtr()) {
            moveCursorNextByChar()
        } else {
            moveCursorPrevByChar()
        }

    /** If there is already a selection, collapse it to the left side. Otherwise, execute [or] */
    fun collapseLeftOr(or: TextFieldPreparedSelection.() -> Unit) = applyIfNotEmpty {
        if (selection.collapsed) {
            or(this)
        } else {
            if (isLtr()) {
                selection = TextRange(selection.min)
            } else {
                selection = TextRange(selection.max)
            }
        }
    }

    /** If there is already a selection, collapse it to the right side. Otherwise, execute [or] */
    fun collapseRightOr(or: TextFieldPreparedSelection.() -> Unit) = applyIfNotEmpty {
        if (selection.collapsed) {
            or(this)
        } else {
            if (isLtr()) {
                selection = TextRange(selection.max)
            } else {
                selection = TextRange(selection.min)
            }
        }
    }

    /** Returns the index of the character break preceding the end of [selection]. */
    fun getPrecedingCharacterIndex() = text.findPrecedingBreak(selection.end)

    /**
     * Returns the index of the character break following the end of [selection]. Returns
     * [NoCharacterFound] if there are no more breaks before the end of the string.
     */
    fun getNextCharacterIndex() = text.findFollowingBreak(selection.end)

    /**
     * Moves the current cursor to the index provided by the [proposedCursorMovement] while
     * respecting the existing transformations on the text.
     */
    private inline fun moveCursorTo(
        resetCachedX: Boolean = true,
        proposedCursorMovement: () -> Int
    ) =
        applyIfNotEmpty(resetCachedX) {
            val oldCursor = selection.end
            val (newCursor, newWedgeAffinity) =
                calculateNextCursorPositionAndWedgeAffinity(
                    proposedCursor = proposedCursorMovement(),
                    cursor = oldCursor,
                    transformedTextFieldState = state
                )

            if (newCursor != oldCursor || !selection.collapsed) {
                selection = TextRange(newCursor)
            }
            if (newWedgeAffinity != null) {
                wedgeAffinity = newWedgeAffinity
            }
        }

    fun moveCursorPrevByChar() = moveCursorTo { text.findPrecedingBreak(selection.end) }

    fun moveCursorNextByChar() = moveCursorTo { text.findFollowingBreak(selection.end) }

    fun moveCursorToHome() = moveCursorTo { 0 }

    fun moveCursorToEnd() = moveCursorTo { text.length }

    fun moveCursorLeftByWord() =
        if (isLtr()) {
            moveCursorPrevByWord()
        } else {
            moveCursorNextByWord()
        }

    fun moveCursorRightByWord() =
        if (isLtr()) {
            moveCursorNextByWord()
        } else {
            moveCursorPrevByWord()
        }

    fun moveCursorNextByWord() = moveCursorTo {
        textLayoutResult?.getNextWordOffsetForLayout() ?: text.length
    }

    fun moveCursorPrevByWord() = moveCursorTo {
        textLayoutResult?.getPrevWordOffsetForLayout() ?: 0
    }

    fun moveCursorPrevByParagraph() = moveCursorTo {
        var paragraphStart = text.findParagraphStart(selection.min)
        if (paragraphStart == selection.min && paragraphStart != 0) {
            paragraphStart = text.findParagraphStart(paragraphStart - 1)
        }
        paragraphStart
    }

    fun moveCursorNextByParagraph() = moveCursorTo {
        var paragraphEnd = text.findParagraphEnd(selection.max)
        if (paragraphEnd == selection.max && paragraphEnd != text.length) {
            paragraphEnd = text.findParagraphEnd(paragraphEnd + 1)
        }
        paragraphEnd
    }

    fun moveCursorUpByLine() =
        moveCursorTo(resetCachedX = false) { textLayoutResult?.jumpByLinesOffset(-1) ?: 0 }

    fun moveCursorDownByLine() =
        moveCursorTo(resetCachedX = false) { textLayoutResult?.jumpByLinesOffset(1) ?: text.length }

    fun moveCursorToLineLeftSide() =
        if (isLtr()) {
            moveCursorToLineStart()
        } else {
            moveCursorToLineEnd()
        }

    fun moveCursorToLineRightSide() =
        if (isLtr()) {
            moveCursorToLineEnd()
        } else {
            moveCursorToLineStart()
        }

    fun moveCursorToLineStart() = moveCursorTo {
        textLayoutResult?.getLineStartByOffsetForLayout() ?: 0
    }

    fun moveCursorToLineEnd() = moveCursorTo {
        textLayoutResult?.getLineEndByOffsetForLayout() ?: text.length
    }

    /** Executes PageUp key */
    fun moveCursorUpByPage() = moveCursorTo(false) { jumpByPagesOffset(-1) }

    /** Executes PageDown key */
    fun moveCursorDownByPage() = moveCursorTo(false) { jumpByPagesOffset(1) }

    /** Selects a text from the original selection start to a current selection end. */
    fun selectMovement() =
        applyIfNotEmpty(resetCachedX = false) {
            selection = TextRange(initialValue.selection.start, selection.end)
        }

    fun deleteMovement() =
        applyIfNotEmpty(resetCachedX = false) {
            if (!initialValue.selection.collapsed) {
                state.deleteSelectedText()
            } else {
                state.replaceText(
                    newText = "",
                    range = TextRange(initialValue.selection.start, selection.end),
                    restartImeIfContentChanges = !isFromSoftKeyboard
                )
            }
            // Update the internal selection to where it was moved by the delete operation.
            selection = state.visualText.selection
            // any wedgeAffinity set by the cursor movement is irrelevant after deletion
            wedgeAffinity = WedgeAffinity.Start
        }

    private fun isLtr(): Boolean {
        val direction = textLayoutResult?.getParagraphDirection(selection.end) ?: return true
        return direction == ResolvedTextDirection.Ltr
    }

    private tailrec fun TextLayoutResult.getNextWordOffsetForLayout(
        currentOffset: Int = selection.end
    ): Int {
        if (currentOffset >= initialValue.length) {
            return initialValue.length
        }
        val currentWord = getWordBoundary(charOffset(currentOffset))
        return if (currentWord.end <= currentOffset) {
            getNextWordOffsetForLayout(currentOffset + 1)
        } else {
            currentWord.end
        }
    }

    private tailrec fun TextLayoutResult.getPrevWordOffsetForLayout(
        currentOffset: Int = selection.end
    ): Int {
        if (currentOffset <= 0) {
            return 0
        }
        val currentWord = getWordBoundary(charOffset(currentOffset))
        return if (currentWord.start >= currentOffset) {
            getPrevWordOffsetForLayout(currentOffset - 1)
        } else {
            currentWord.start
        }
    }

    private fun TextLayoutResult.getLineStartByOffsetForLayout(
        currentOffset: Int = selection.min
    ): Int {
        val currentLine = getLineForOffset(currentOffset)
        return getLineStart(currentLine)
    }

    private fun TextLayoutResult.getLineEndByOffsetForLayout(
        currentOffset: Int = selection.max
    ): Int {
        val currentLine = getLineForOffset(currentOffset)
        return getLineEnd(currentLine, true)
    }

    private fun TextLayoutResult.jumpByLinesOffset(linesAmount: Int): Int {
        val currentOffset = selection.end

        if (textPreparedSelectionState.cachedX.isNaN()) {
            textPreparedSelectionState.cachedX = getCursorRect(currentOffset).left
        }

        val targetLine = getLineForOffset(currentOffset) + linesAmount
        when {
            targetLine < 0 -> {
                return 0
            }
            targetLine >= lineCount -> {
                return text.length
            }
        }

        val y = getLineBottom(targetLine) - 1
        val x =
            textPreparedSelectionState.cachedX.also {
                if (
                    (isLtr() && it >= getLineRight(targetLine)) ||
                        (!isLtr() && it <= getLineLeft(targetLine))
                ) {
                    return getLineEnd(targetLine, true)
                }
            }

        return getOffsetForPosition(Offset(x, y))
    }

    /**
     * Returns a cursor position after jumping back or forth by [pagesAmount] number of pages, where
     * `page` is the visible amount of space in the text field. Visible rectangle is calculated by
     * the bounding box of text layout coordinates inside the core coordinates. Please refer to
     * `TextLayoutState` to learn more about these coordinates.
     *
     * If text layout has not been measured yet, this function returns the current offset.
     */
    private fun jumpByPagesOffset(pagesAmount: Int): Int {
        val currentOffset = initialValue.selection.end
        if (textLayoutResult == null || visibleTextLayoutHeight.isNaN()) return currentOffset
        val currentPos = textLayoutResult.getCursorRect(currentOffset)
        val newPos =
            currentPos.translate(
                translateX = 0f,
                translateY = visibleTextLayoutHeight * pagesAmount
            )
        // which line does the new cursor position belong?
        val topLine = textLayoutResult.getLineForVerticalPosition(newPos.top)
        val lineSeparator = textLayoutResult.getLineBottom(topLine)
        return if (abs(newPos.top - lineSeparator) > abs(newPos.bottom - lineSeparator)) {
            // most of new cursor is on top line
            textLayoutResult.getOffsetForPosition(newPos.topLeft)
        } else {
            // most of new cursor is on bottom line
            textLayoutResult.getOffsetForPosition(newPos.bottomLeft)
        }
    }

    private fun charOffset(offset: Int) = offset.coerceAtMost(text.length - 1)

    companion object {
        /**
         * Value returned by [getNextCharacterIndex] and [getPrecedingCharacterIndex] when no valid
         * index could be found, e.g. it would be the end of the string.
         *
         * This is equivalent to `BreakIterator.DONE` on JVM/Android.
         */
        const val NoCharacterFound = -1
    }
}

/**
 * Given the proposed next cursor offset and the current cursor offset in a TextField, calculates
 * the offset of the nearest position of the cursor in the transformed text. Takes into account text
 * transformations ([TransformedTextFieldState]) to avoid putting the cursor in the middle of
 * replacements.
 *
 * @return The next cursor position that respects the existing transformations on the
 *   [transformedTextFieldState], and the new [WedgeAffinity] of the moving cursor.
 */
@VisibleForTesting
internal fun calculateNextCursorPositionAndWedgeAffinity(
    proposedCursor: Int,
    cursor: Int,
    transformedTextFieldState: TransformedTextFieldState
): CursorAndWedgeAffinity {
    if (proposedCursor == NoCharacterFound) {
        // At the start or end of the text, no change.
        return CursorAndWedgeAffinity(cursor)
    }

    val forward = proposedCursor > cursor

    // if a transformation is applied, determine if the proposed cursor position would
    // be in a range where the cursor is not allowed to be. If so, push it to the appropriate edge
    // of that range.
    return transformedTextFieldState.getIndexTransformationType(
        transformedQueryIndex = proposedCursor,
        onResult = { type, _, retransformed ->
            when (type) {
                // Depending on the direction we are moving we might want to adjust the existing
                // wedge affinity so that touching an insertion or a replacement bound doesn't
                // immediately skip that wedge.
                Untransformed ->
                    CursorAndWedgeAffinity(
                        proposedCursor,
                        if (forward) WedgeAffinity.Start else WedgeAffinity.End
                    )

                // It doesn't matter which end of the deleted range we put the cursor, they'll both
                // map to the same transformed offset.
                Deletion -> CursorAndWedgeAffinity(proposedCursor)

                // Moving forward into a replacement means we should jump to the end, moving
                // backwards into it means jump to the start. But also we need to update the wedge
                // affinity so a single jump around the replacement also doesn't force us to jump
                // an insertion at the other end.
                Replacement ->
                    if (forward) {
                        CursorAndWedgeAffinity(retransformed.end, WedgeAffinity.Start)
                    } else {
                        CursorAndWedgeAffinity(retransformed.start, WedgeAffinity.End)
                    }

                // Moving into an insertion is like a replacement in that the cursor may only be
                // placed on either edge of the range. However, since both edges of the range map
                // to the same untransformed index, we need to set the affinity.
                Insertion -> {
                    if (forward) {
                        if (proposedCursor == retransformed.start) {
                            // Moving to start of wedge, update affinity and set cursor.
                            CursorAndWedgeAffinity(proposedCursor, WedgeAffinity.Start)
                        } else {
                            // Moving to middle or end of wedge, update affinity but don't need to
                            // move cursor. No offset change.
                            CursorAndWedgeAffinity(retransformed.end, WedgeAffinity.End)
                        }
                    } else {
                        // We're navigating to or within a wedge. Use affinity (doesn't matter which
                        // one, selection is a cursor).
                        if (proposedCursor == retransformed.end) {
                            // Moving to end of wedge, update affinity and set cursor.
                            CursorAndWedgeAffinity(proposedCursor, WedgeAffinity.End)
                        } else {
                            // Moving to middle or start of wedge, update affinity but don't need to
                            // move cursor. No offset change.
                            CursorAndWedgeAffinity(retransformed.start, WedgeAffinity.Start)
                        }
                    }
                }
            }
        }
    )
}

@JvmInline
internal value class CursorAndWedgeAffinity(private val value: Long) {

    constructor(cursor: Int) : this(packInts(cursor, -1))

    constructor(
        cursor: Int,
        wedgeAffinity: WedgeAffinity?
    ) : this(
        packInts(
            cursor,
            when (wedgeAffinity) {
                WedgeAffinity.Start -> 0
                WedgeAffinity.End -> 1
                null -> -1
            }
        )
    )

    val cursor: Int
        get() = unpackInt1(value)

    val wedgeAffinity: WedgeAffinity?
        get() =
            unpackInt2(value).let {
                when {
                    it < 0 -> null
                    it == 0 -> WedgeAffinity.Start
                    else -> WedgeAffinity.End
                }
            }

    operator fun component1() = cursor

    operator fun component2() = wedgeAffinity
}

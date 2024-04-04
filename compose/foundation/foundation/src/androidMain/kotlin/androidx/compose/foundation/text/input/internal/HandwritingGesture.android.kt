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

package androidx.compose.foundation.text.input.internal

import android.view.inputmethod.DeleteGesture
import android.view.inputmethod.DeleteRangeGesture
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.SelectGesture
import android.view.inputmethod.SelectRangeGesture
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextGranularity
import androidx.compose.ui.text.TextInclusionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditingBuffer
import androidx.compose.ui.text.input.SetSelectionCommand
import kotlin.math.max
import kotlin.math.min

@RequiresApi(34)
internal object HandwritingGestureApi34 {
    @DoNotInline
    internal fun TransformedTextFieldState.performHandwritingGesture(
        handwritingGesture: HandwritingGesture,
        layoutState: TextLayoutState
    ): Int {
        return when (handwritingGesture) {
            is SelectGesture -> performSelectGesture(handwritingGesture, layoutState)
            is DeleteGesture -> performDeleteGesture(handwritingGesture, layoutState)
            is SelectRangeGesture -> performSelectRangeGesture(handwritingGesture, layoutState)
            is DeleteRangeGesture -> performDeleteRangeGesture(handwritingGesture, layoutState)
            else -> InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
        }
    }

    @DoNotInline
    internal fun TransformedTextFieldState.performSelectGesture(
        gesture: SelectGesture,
        layoutState: TextLayoutState
    ): Int {
        val rangeInTransformedText = layoutState.getRangeForScreenRect(
            gesture.selectionArea.toComposeRect(),
            gesture.granularity.toTextGranularity(),
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallback(gesture)

        // TODO(332749926) show toolbar after selection.
        selectCharsIn(rangeInTransformedText)
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    internal fun TransformedTextFieldState.performDeleteGesture(
        gesture: DeleteGesture,
        layoutState: TextLayoutState
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val rangeInTransformedText = layoutState.getRangeForScreenRect(
            gesture.deletionArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallback(gesture)

        performDeletion(
            rangeInTransformedText = rangeInTransformedText,
            adjustRange = (granularity == TextGranularity.Word)
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    internal fun TransformedTextFieldState.performSelectRangeGesture(
        gesture: SelectRangeGesture,
        layoutState: TextLayoutState
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val startRange = layoutState.getRangeForScreenRect(
            gesture.selectionStartArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallback(gesture)
        val endRange = layoutState.getRangeForScreenRect(
            gesture.selectionEndArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallback(gesture)

        selectCharsIn(enclosure(startRange, endRange))
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    internal fun TransformedTextFieldState.performDeleteRangeGesture(
        gesture: DeleteRangeGesture,
        layoutState: TextLayoutState
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val startRange = layoutState.getRangeForScreenRect(
            gesture.deletionStartArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallback(gesture)
        val endRange = layoutState.getRangeForScreenRect(
            gesture.deletionEndArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallback(gesture)

        performDeletion(
            rangeInTransformedText = enclosure(startRange, endRange),
            adjustRange = granularity == TextGranularity.Word
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    private fun TransformedTextFieldState.performDeletion(
        rangeInTransformedText: TextRange,
        adjustRange: Boolean
    ) {
        val rangeInUnTransformedText = if (adjustRange) {
            mapFromTransformed(
                rangeInTransformedText.adjustHandwritingDeleteGestureRange(visualText)
            )
        } else {
            mapFromTransformed(rangeInTransformedText)
        }

        editUntransformedTextAsUser {
            replace(rangeInUnTransformedText.start, rangeInUnTransformedText.end, "")
        }
    }

    @DoNotInline
    private fun TransformedTextFieldState.fallback(gesture: HandwritingGesture): Int {
        val fallbackText = gesture.fallbackText
            ?: return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED

        replaceSelectedText(
            newText = fallbackText,
            clearComposition = true,
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK
    }

    @DoNotInline
    internal fun LegacyTextFieldState.performHandwritingGesture(
        gesture: HandwritingGesture,
        textFieldSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val text = untransformedText ?: return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED
        if (text != layoutResult?.value?.layoutInput?.text) {
            // The text is transformed or layout is null, handwriting gesture failed.
            return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED
        }
        return when (gesture) {
            is SelectGesture ->
                performSelectGesture(gesture, textFieldSelectionManager, editCommandConsumer)
            is DeleteGesture ->
                performDeleteGesture(gesture, text, editCommandConsumer)
            is SelectRangeGesture ->
                performSelectRangeGesture(gesture, textFieldSelectionManager, editCommandConsumer)
            is DeleteRangeGesture ->
                performDeleteRangeGesture(gesture, text, editCommandConsumer)
            else -> InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
        }
    }

    @DoNotInline
    private fun LegacyTextFieldState.performSelectGesture(
        gesture: SelectGesture,
        textSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val range = getRangeForScreenRect(
            gesture.selectionArea.toComposeRect(),
            gesture.granularity.toTextGranularity(),
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallbackOnLegacyTextField(gesture, editCommandConsumer)

        performSelectionOnLegacyTextField(range, textSelectionManager, editCommandConsumer)
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    private fun LegacyTextFieldState.performDeleteGesture(
        gesture: DeleteGesture,
        text: AnnotatedString,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val range = getRangeForScreenRect(
            gesture.deletionArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallbackOnLegacyTextField(gesture, editCommandConsumer)

        performDeletionOnLegacyTextField(
            range = range,
            text = text,
            adjustRange = granularity == TextGranularity.Word,
            editCommandConsumer = editCommandConsumer
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    private fun LegacyTextFieldState.performSelectRangeGesture(
        gesture: SelectRangeGesture,
        textSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val startRange = getRangeForScreenRect(
            gesture.selectionStartArea.toComposeRect(),
            gesture.granularity.toTextGranularity(),
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallbackOnLegacyTextField(gesture, editCommandConsumer)

        val endRange = getRangeForScreenRect(
            gesture.selectionEndArea.toComposeRect(),
            gesture.granularity.toTextGranularity(),
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallbackOnLegacyTextField(gesture, editCommandConsumer)

        performSelectionOnLegacyTextField(
            range = enclosure(startRange, endRange),
            textSelectionManager = textSelectionManager,
            editCommandConsumer = editCommandConsumer
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    private fun LegacyTextFieldState.performDeleteRangeGesture(
        gesture: DeleteRangeGesture,
        text: AnnotatedString,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val startRange = getRangeForScreenRect(
            gesture.deletionStartArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallbackOnLegacyTextField(gesture, editCommandConsumer)

        val endRange = getRangeForScreenRect(
            gesture.deletionEndArea.toComposeRect(),
            granularity,
            TextInclusionStrategy.ContainsCenter
        ) ?: return fallbackOnLegacyTextField(gesture, editCommandConsumer)

        performDeletionOnLegacyTextField(
            range = enclosure(startRange, endRange),
            text = text,
            adjustRange = granularity == TextGranularity.Word,
            editCommandConsumer = editCommandConsumer
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    @DoNotInline
    private fun performSelectionOnLegacyTextField(
        range: TextRange,
        textSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ) {
        editCommandConsumer.invoke(SetSelectionCommand(range.start, range.end))
        textSelectionManager?.enterSelectionMode(showFloatingToolbar = true)
    }

    @DoNotInline
    private fun performDeletionOnLegacyTextField(
        range: TextRange,
        text: AnnotatedString,
        adjustRange: Boolean,
        editCommandConsumer: (EditCommand) -> Unit
    ) {
        val finalRange = if (adjustRange) {
            range.adjustHandwritingDeleteGestureRange(text)
        } else {
            range
        }

        editCommandConsumer.invoke(
            compoundEditCommand(
                SetSelectionCommand(finalRange.end, finalRange.end),
                DeleteSurroundingTextCommand(
                    lengthAfterCursor = 0,
                    lengthBeforeCursor = finalRange.length
                )
            )
        )
    }

    @DoNotInline
    private fun fallbackOnLegacyTextField(
        gesture: HandwritingGesture,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val fallbackText = gesture.fallbackText
            ?: return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED
        editCommandConsumer.invoke(
            CommitTextCommand(fallbackText, newCursorPosition = 1)
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK
    }

    /** Convert the Platform text granularity to Compose [TextGranularity] object. */
    @DoNotInline
    private fun Int.toTextGranularity(): TextGranularity {
        return when (this) {
            HandwritingGesture.GRANULARITY_CHARACTER -> TextGranularity.Character
            HandwritingGesture.GRANULARITY_WORD -> TextGranularity.Word
            else -> TextGranularity.Character
        }
    }
}

private const val LINE_FEED_CODE_POINT = '\n'.code
private const val NBSP_CODE_POINT = '\u00A0'.code

/**
 * For handwriting delete gestures with word granularity, adjust the start and end offsets
 * to remove extra whitespace around the deleted text.
 */
private fun TextRange.adjustHandwritingDeleteGestureRange(
    text: CharSequence
): TextRange {
    var start = this.start
    var end = this.end

    // If the deleted text is at the start of the text, the behavior is the same as the case
    // where the deleted text follows a new line character.
    var codePointBeforeStart: Int = if (start > 0) {
        Character.codePointBefore(text, start)
    } else {
        LINE_FEED_CODE_POINT
    }
    // If the deleted text is at the end of the text, the behavior is the same as the case where
    // the deleted text precedes a new line character.
    var codePointAtEnd: Int = if (end < text.length) {
        Character.codePointAt(text, end)
    } else {
        LINE_FEED_CODE_POINT
    }

    if (codePointBeforeStart.isWhitespaceExceptNewline() &&
        (codePointAtEnd.isWhitespace() || codePointAtEnd.isPunctuation())
    ) {
        // Remove whitespace (except new lines) before the deleted text, in these cases:
        // - There is whitespace following the deleted text
        //     e.g. "one [deleted] three" -> "one | three" -> "one| three"
        // - There is punctuation following the deleted text
        //     e.g. "one [deleted]!" -> "one |!" -> "one|!"
        // - There is a new line following the deleted text
        //     e.g. "one [deleted]\n" -> "one |\n" -> "one|\n"
        // - The deleted text is at the end of the text
        //     e.g. "one [deleted]" -> "one |" -> "one|"
        // (The pipe | indicates the cursor position.)
        do {
            start -= Character.charCount(codePointBeforeStart)
            if (start == 0) break
            codePointBeforeStart = Character.codePointBefore(text, start)
        } while (codePointBeforeStart.isWhitespaceExceptNewline())
        return TextRange(start, end)
    }

    if (codePointAtEnd.isWhitespaceExceptNewline() &&
        (codePointBeforeStart.isWhitespace() ||
            codePointBeforeStart.isPunctuation())
    ) {
        // Remove whitespace (except new lines) after the deleted text, in these cases:
        // - There is punctuation preceding the deleted text
        //     e.g. "([deleted] two)" -> "(| two)" -> "(|two)"
        // - There is a new line preceding the deleted text
        //     e.g. "\n[deleted] two" -> "\n| two" -> "\n|two"
        // - The deleted text is at the start of the text
        //     e.g. "[deleted] two" -> "| two" -> "|two"
        // (The pipe | indicates the cursor position.)
        do {
            end += Character.charCount(codePointAtEnd)
            if (end == text.length) break
            codePointAtEnd = Character.codePointAt(text, end)
        } while (codePointAtEnd.isWhitespaceExceptNewline())
        return TextRange(start, end)
    }

    // Return the original range.
    return this
}

/**
 *  This helper method is copied from [android.text.TextUtils].
 *  It returns true if the code point is a new line.
 */
private fun Int.isNewline(): Boolean {
    val type = Character.getType(this)
    return type == Character.PARAGRAPH_SEPARATOR.toInt() ||
        type == Character.LINE_SEPARATOR.toInt() ||
        this == LINE_FEED_CODE_POINT
}

/**
 *  This helper method is copied from [android.text.TextUtils].
 *  It returns true if the code point is a whitespace.
 */
private fun Int.isWhitespace(): Boolean {
    return Character.isWhitespace(this) ||
        this == NBSP_CODE_POINT
}

/**
 *  This helper method is copied from [android.text.TextUtils].
 *  It returns true if the code point is a whitespace and not a new line.
 */
private fun Int.isWhitespaceExceptNewline(): Boolean {
    return isWhitespace() && !isNewline()
}

/**
 *  This helper method is copied from [android.text.TextUtils].
 *  It returns true if the code point is a punctuation.
 */
private fun Int.isPunctuation(): Boolean {
    val type = Character.getType(this)
    return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
        type == Character.DASH_PUNCTUATION.toInt() ||
        type == Character.END_PUNCTUATION.toInt() ||
        type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
        type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
        type == Character.OTHER_PUNCTUATION.toInt() ||
        type == Character.START_PUNCTUATION.toInt()
}

private fun TextLayoutState.getRangeForScreenRect(
    rectInScreen: Rect,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange? {
    return layoutResult?.multiParagraph?.getRangeForScreenRect(
        rectInScreen,
        textLayoutNodeCoordinates,
        granularity,
        inclusionStrategy
    )
}

private fun LegacyTextFieldState.getRangeForScreenRect(
    rectInScreen: Rect,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange? {
    return layoutResult?.value?.multiParagraph?.getRangeForScreenRect(
        rectInScreen,
        layoutCoordinates,
        granularity,
        inclusionStrategy
    )
}

private fun MultiParagraph.getRangeForScreenRect(
    rectInScreen: Rect,
    layoutCoordinates: LayoutCoordinates?,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange? {
    val screenOriginInLocal = layoutCoordinates?.screenToLocal(Offset.Zero) ?: return null
    val localRect = rectInScreen.translate(screenOriginInLocal)
    return getRangeForRect(localRect, granularity, inclusionStrategy)
}

private fun compoundEditCommand(vararg editCommands: EditCommand): EditCommand {
    return object : EditCommand {
        override fun applyTo(buffer: EditingBuffer) {
            for (editCommand in editCommands) {
                editCommand.applyTo(buffer)
            }
        }
    }
}

/** Return the minimum [TextRange] that contains the both given [TextRange]s. */
private fun enclosure(a: TextRange, b: TextRange): TextRange {
    return TextRange(min(a.start, a.start), max(b.end, b.end))
}

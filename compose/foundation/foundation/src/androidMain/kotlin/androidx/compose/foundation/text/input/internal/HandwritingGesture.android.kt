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

import android.graphics.PointF
import android.os.CancellationSignal
import android.view.inputmethod.DeleteGesture
import android.view.inputmethod.DeleteRangeGesture
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InsertGesture
import android.view.inputmethod.JoinOrSplitGesture
import android.view.inputmethod.PreviewableHandwritingGesture
import android.view.inputmethod.RemoveSpaceGesture
import android.view.inputmethod.SelectGesture
import android.view.inputmethod.SelectRangeGesture
import androidx.annotation.RequiresApi
import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.input.TextHighlightType
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextGranularity
import androidx.compose.ui.text.TextInclusionStrategy
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditingBuffer
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.substring
import kotlin.math.max
import kotlin.math.min

@RequiresApi(34)
internal object HandwritingGestureApi34 {
    internal fun TransformedTextFieldState.performHandwritingGesture(
        handwritingGesture: HandwritingGesture,
        layoutState: TextLayoutState,
        updateSelectionState: (() -> Unit)?,
        viewConfiguration: ViewConfiguration?
    ): Int {
        return when (handwritingGesture) {
            is SelectGesture ->
                performSelectGesture(handwritingGesture, layoutState, updateSelectionState)
            is DeleteGesture -> performDeleteGesture(handwritingGesture, layoutState)
            is SelectRangeGesture ->
                performSelectRangeGesture(handwritingGesture, layoutState, updateSelectionState)
            is DeleteRangeGesture -> performDeleteRangeGesture(handwritingGesture, layoutState)
            is JoinOrSplitGesture ->
                performJoinOrSplitGesture(handwritingGesture, layoutState, viewConfiguration)
            is InsertGesture ->
                performInsertGesture(handwritingGesture, layoutState, viewConfiguration)
            is RemoveSpaceGesture ->
                performRemoveSpaceGesture(handwritingGesture, layoutState, viewConfiguration)
            else -> InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
        }
    }

    internal fun TransformedTextFieldState.previewHandwritingGesture(
        handwritingGesture: PreviewableHandwritingGesture,
        layoutState: TextLayoutState,
        cancellationSignal: CancellationSignal?
    ): Boolean {
        when (handwritingGesture) {
            is SelectGesture -> previewSelectGesture(handwritingGesture, layoutState)
            is DeleteGesture -> previewDeleteGesture(handwritingGesture, layoutState)
            is SelectRangeGesture -> previewSelectRangeGesture(handwritingGesture, layoutState)
            is DeleteRangeGesture -> previewDeleteRangeGesture(handwritingGesture, layoutState)
            else -> return false
        }
        cancellationSignal?.setOnCancelListener { editUntransformedTextAsUser { clearHighlight() } }
        return true
    }

    private fun TransformedTextFieldState.performSelectGesture(
        gesture: SelectGesture,
        layoutState: TextLayoutState,
        updateSelectionState: (() -> Unit)?,
    ): Int {
        val rangeInTransformedText =
            layoutState
                .getRangeForScreenRect(
                    gesture.selectionArea.toComposeRect(),
                    gesture.granularity.toTextGranularity(),
                    TextInclusionStrategy.ContainsCenter
                )
                .apply { if (collapsed) return fallback(gesture) }

        selectCharsIn(rangeInTransformedText)
        updateSelectionState?.invoke()
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.previewSelectGesture(
        gesture: SelectGesture,
        layoutState: TextLayoutState
    ) {
        highlightRange(
            layoutState.getRangeForScreenRect(
                gesture.selectionArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            ),
            TextHighlightType.HandwritingSelectPreview
        )
    }

    private fun TransformedTextFieldState.performDeleteGesture(
        gesture: DeleteGesture,
        layoutState: TextLayoutState
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val rangeInTransformedText =
            layoutState
                .getRangeForScreenRect(
                    gesture.deletionArea.toComposeRect(),
                    granularity,
                    TextInclusionStrategy.ContainsCenter
                )
                .apply { if (collapsed) return fallback(gesture) }

        performDeletion(
            rangeInTransformedText = rangeInTransformedText,
            adjustRange = (granularity == TextGranularity.Word)
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.previewDeleteGesture(
        gesture: DeleteGesture,
        layoutState: TextLayoutState
    ) {
        highlightRange(
            layoutState.getRangeForScreenRect(
                gesture.deletionArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            ),
            TextHighlightType.HandwritingDeletePreview
        )
    }

    private fun TransformedTextFieldState.performSelectRangeGesture(
        gesture: SelectRangeGesture,
        layoutState: TextLayoutState,
        updateSelectionState: (() -> Unit)?,
    ): Int {
        val rangeInTransformedText =
            layoutState
                .getRangeForScreenRects(
                    gesture.selectionStartArea.toComposeRect(),
                    gesture.selectionEndArea.toComposeRect(),
                    gesture.granularity.toTextGranularity(),
                    TextInclusionStrategy.ContainsCenter
                )
                .apply { if (collapsed) return fallback(gesture) }

        selectCharsIn(rangeInTransformedText)
        updateSelectionState?.invoke()
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.previewSelectRangeGesture(
        gesture: SelectRangeGesture,
        layoutState: TextLayoutState
    ) {
        highlightRange(
            layoutState.getRangeForScreenRects(
                gesture.selectionStartArea.toComposeRect(),
                gesture.selectionEndArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            ),
            TextHighlightType.HandwritingSelectPreview
        )
    }

    private fun TransformedTextFieldState.performDeleteRangeGesture(
        gesture: DeleteRangeGesture,
        layoutState: TextLayoutState
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val rangeInTransformedText =
            layoutState
                .getRangeForScreenRects(
                    gesture.deletionStartArea.toComposeRect(),
                    gesture.deletionEndArea.toComposeRect(),
                    granularity,
                    TextInclusionStrategy.ContainsCenter
                )
                .apply { if (collapsed) return fallback(gesture) }

        performDeletion(
            rangeInTransformedText = rangeInTransformedText,
            adjustRange = granularity == TextGranularity.Word
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.previewDeleteRangeGesture(
        gesture: DeleteRangeGesture,
        layoutState: TextLayoutState
    ) {
        highlightRange(
            layoutState.getRangeForScreenRects(
                gesture.deletionStartArea.toComposeRect(),
                gesture.deletionEndArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            ),
            TextHighlightType.HandwritingDeletePreview
        )
    }

    private fun TransformedTextFieldState.performJoinOrSplitGesture(
        gesture: JoinOrSplitGesture,
        layoutState: TextLayoutState,
        viewConfiguration: ViewConfiguration?
    ): Int {
        // Fail when there is an output transformation.
        // If output transformation inserts some spaces to the text, we can't remove them.
        // Do nothing is the best choice in this case.
        // We don't fallback either because the user's intention is unlikely inserting characters.
        if (outputText !== untransformedText) {
            return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED
        }

        val offset =
            layoutState.getOffsetForHandwritingGesture(
                pointInScreen = gesture.joinOrSplitPoint.toOffset(),
                viewConfiguration = viewConfiguration
            )

        // TODO(332963121): support gesture at BiDi boundaries.
        if (offset == -1 || layoutState.layoutResult?.isBiDiBoundary(offset) == true) {
            return fallback(gesture)
        }

        val textRange = visualText.rangeOfWhitespaces(offset)

        if (textRange.collapsed) {
            replaceText(" ", textRange)
        } else {
            performDeletion(rangeInTransformedText = textRange, adjustRange = false)
        }
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.performInsertGesture(
        gesture: InsertGesture,
        layoutState: TextLayoutState,
        viewConfiguration: ViewConfiguration?
    ): Int {
        val offset =
            layoutState.getOffsetForHandwritingGesture(
                pointInScreen = gesture.insertionPoint.toOffset(),
                viewConfiguration = viewConfiguration
            )

        // TODO(332963121): support gesture at BiDi boundaries.
        if (offset == -1) {
            return fallback(gesture)
        }

        replaceText(gesture.textToInsert, TextRange(offset))
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.performRemoveSpaceGesture(
        gesture: RemoveSpaceGesture,
        layoutState: TextLayoutState,
        viewConfiguration: ViewConfiguration?
    ): Int {
        val range =
            layoutState.layoutResult
                .getRangeForRemoveSpaceGesture(
                    startPointInScreen = gesture.startPoint.toOffset(),
                    endPointerInScreen = gesture.endPoint.toOffset(),
                    layoutCoordinates = layoutState.textLayoutNodeCoordinates,
                    viewConfiguration = viewConfiguration
                )
                .apply { if (collapsed) return fallback(gesture) }

        var firstMatchStart = -1
        var lastMatchEnd = -1
        val newText =
            visualText.substring(range).replace(Regex("\\s+")) {
                if (firstMatchStart == -1) {
                    firstMatchStart = it.range.first
                }
                lastMatchEnd = it.range.last + 1
                ""
            }

        // No whitespace is found in the target range, fallback instead
        if (firstMatchStart == -1 || lastMatchEnd == -1) {
            return fallback(gesture)
        }

        // We only replace the part of the text that changes.
        // e.g. The text is "AB CD EF GH IJ" and the original gesture range is "CD EF GH"
        // We'll replace " EF " to "EF" instead of replacing "CD EF GH" to "CDEFGH".
        // By doing so, it'll place the cursor at the index of the last removed space.
        // In the example above, the cursor should be placed after character 'F'.
        val finalRange = TextRange(range.start + firstMatchStart, range.start + lastMatchEnd)
        // Remove the unchanged part from the newText as well. Characters before firstMatchStart
        // and characters after the lastMatchEnd are removed.
        val finalNewText =
            newText.substring(
                startIndex = firstMatchStart,
                endIndex = newText.length - (range.length - lastMatchEnd)
            )

        replaceText(finalNewText, finalRange)
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun TransformedTextFieldState.performDeletion(
        rangeInTransformedText: TextRange,
        adjustRange: Boolean
    ) {
        val finalRange =
            if (adjustRange) {
                rangeInTransformedText.adjustHandwritingDeleteGestureRange(visualText)
            } else {
                rangeInTransformedText
            }
        replaceText("", finalRange)
    }

    private fun TransformedTextFieldState.fallback(gesture: HandwritingGesture): Int {
        editUntransformedTextAsUser { clearHighlight() }

        val fallbackText =
            gesture.fallbackText ?: return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED

        replaceSelectedText(
            newText = fallbackText,
            clearComposition = true,
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK
    }

    private fun TransformedTextFieldState.highlightRange(
        range: TextRange,
        type: TextHighlightType
    ) {
        if (range.collapsed) {
            editUntransformedTextAsUser { clearHighlight() }
        } else {
            highlightCharsIn(type, range)
        }
    }

    internal fun LegacyTextFieldState.performHandwritingGesture(
        gesture: HandwritingGesture,
        textFieldSelectionManager: TextFieldSelectionManager?,
        viewConfiguration: ViewConfiguration?,
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
            is DeleteGesture -> performDeleteGesture(gesture, text, editCommandConsumer)
            is SelectRangeGesture ->
                performSelectRangeGesture(gesture, textFieldSelectionManager, editCommandConsumer)
            is DeleteRangeGesture -> performDeleteRangeGesture(gesture, text, editCommandConsumer)
            is JoinOrSplitGesture ->
                performJoinOrSplitGesture(gesture, text, viewConfiguration, editCommandConsumer)
            is InsertGesture ->
                performInsertGesture(gesture, viewConfiguration, editCommandConsumer)
            is RemoveSpaceGesture ->
                performRemoveSpaceGesture(gesture, text, viewConfiguration, editCommandConsumer)
            else -> InputConnection.HANDWRITING_GESTURE_RESULT_UNSUPPORTED
        }
    }

    internal fun LegacyTextFieldState.previewHandwritingGesture(
        gesture: PreviewableHandwritingGesture,
        textFieldSelectionManager: TextFieldSelectionManager?,
        cancellationSignal: CancellationSignal?
    ): Boolean {
        val text = untransformedText ?: return false
        if (text != layoutResult?.value?.layoutInput?.text) {
            // The text is transformed or layout is null, handwriting gesture failed.
            return false
        }
        when (gesture) {
            is SelectGesture -> previewSelectGesture(gesture, textFieldSelectionManager)
            is DeleteGesture -> previewDeleteGesture(gesture, textFieldSelectionManager)
            is SelectRangeGesture -> previewSelectRangeGesture(gesture, textFieldSelectionManager)
            is DeleteRangeGesture -> previewDeleteRangeGesture(gesture, textFieldSelectionManager)
            else -> return false
        }
        cancellationSignal?.setOnCancelListener {
            textFieldSelectionManager?.clearPreviewHighlight()
        }
        return true
    }

    private fun LegacyTextFieldState.performSelectGesture(
        gesture: SelectGesture,
        textSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val range =
            getRangeForScreenRect(
                    gesture.selectionArea.toComposeRect(),
                    gesture.granularity.toTextGranularity(),
                    TextInclusionStrategy.ContainsCenter
                )
                .apply {
                    if (collapsed) return fallbackOnLegacyTextField(gesture, editCommandConsumer)
                }

        performSelectionOnLegacyTextField(range, textSelectionManager, editCommandConsumer)
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun LegacyTextFieldState.previewSelectGesture(
        gesture: SelectGesture,
        textFieldSelectionManager: TextFieldSelectionManager?
    ) {
        textFieldSelectionManager?.setSelectionPreviewHighlight(
            getRangeForScreenRect(
                gesture.selectionArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            )
        )
    }

    private fun LegacyTextFieldState.performDeleteGesture(
        gesture: DeleteGesture,
        text: AnnotatedString,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val range =
            getRangeForScreenRect(
                    gesture.deletionArea.toComposeRect(),
                    granularity,
                    TextInclusionStrategy.ContainsCenter
                )
                .apply {
                    if (collapsed) return fallbackOnLegacyTextField(gesture, editCommandConsumer)
                }

        performDeletionOnLegacyTextField(
            range = range,
            text = text,
            adjustRange = granularity == TextGranularity.Word,
            editCommandConsumer = editCommandConsumer
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun LegacyTextFieldState.previewDeleteGesture(
        gesture: DeleteGesture,
        textFieldSelectionManager: TextFieldSelectionManager?
    ) {
        textFieldSelectionManager?.setDeletionPreviewHighlight(
            getRangeForScreenRect(
                gesture.deletionArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            )
        )
    }

    private fun LegacyTextFieldState.performSelectRangeGesture(
        gesture: SelectRangeGesture,
        textSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val range =
            getRangeForScreenRects(
                    gesture.selectionStartArea.toComposeRect(),
                    gesture.selectionEndArea.toComposeRect(),
                    gesture.granularity.toTextGranularity(),
                    TextInclusionStrategy.ContainsCenter
                )
                .apply {
                    if (collapsed) return fallbackOnLegacyTextField(gesture, editCommandConsumer)
                }

        performSelectionOnLegacyTextField(
            range = range,
            textSelectionManager = textSelectionManager,
            editCommandConsumer = editCommandConsumer
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun LegacyTextFieldState.previewSelectRangeGesture(
        gesture: SelectRangeGesture,
        textFieldSelectionManager: TextFieldSelectionManager?
    ) {
        textFieldSelectionManager?.setSelectionPreviewHighlight(
            getRangeForScreenRects(
                gesture.selectionStartArea.toComposeRect(),
                gesture.selectionEndArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            )
        )
    }

    private fun LegacyTextFieldState.performDeleteRangeGesture(
        gesture: DeleteRangeGesture,
        text: AnnotatedString,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val granularity = gesture.granularity.toTextGranularity()
        val range =
            getRangeForScreenRects(
                    gesture.deletionStartArea.toComposeRect(),
                    gesture.deletionEndArea.toComposeRect(),
                    granularity,
                    TextInclusionStrategy.ContainsCenter
                )
                .apply {
                    if (collapsed) return fallbackOnLegacyTextField(gesture, editCommandConsumer)
                }
        performDeletionOnLegacyTextField(
            range = range,
            text = text,
            adjustRange = granularity == TextGranularity.Word,
            editCommandConsumer = editCommandConsumer
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun LegacyTextFieldState.previewDeleteRangeGesture(
        gesture: DeleteRangeGesture,
        textFieldSelectionManager: TextFieldSelectionManager?
    ) {
        textFieldSelectionManager?.setDeletionPreviewHighlight(
            getRangeForScreenRects(
                gesture.deletionStartArea.toComposeRect(),
                gesture.deletionEndArea.toComposeRect(),
                gesture.granularity.toTextGranularity(),
                TextInclusionStrategy.ContainsCenter
            )
        )
    }

    private fun LegacyTextFieldState.performJoinOrSplitGesture(
        gesture: JoinOrSplitGesture,
        text: AnnotatedString,
        viewConfiguration: ViewConfiguration?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        if (viewConfiguration == null) {
            return fallbackOnLegacyTextField(gesture, editCommandConsumer)
        }

        val offset =
            getOffsetForHandwritingGesture(
                pointInScreen = gesture.joinOrSplitPoint.toOffset(),
                viewConfiguration = viewConfiguration
            )
        // TODO(332963121): support gesture at BiDi boundaries.
        if (offset == -1 || layoutResult?.value?.isBiDiBoundary(offset) == true) {
            return fallbackOnLegacyTextField(gesture, editCommandConsumer)
        }

        val range = text.rangeOfWhitespaces(offset)
        if (range.collapsed) {
            performInsertionOnLegacyTextField(range.start, " ", editCommandConsumer)
        } else {
            performDeletionOnLegacyTextField(
                range = range,
                text = text,
                adjustRange = false,
                editCommandConsumer = editCommandConsumer
            )
        }

        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun LegacyTextFieldState.performInsertGesture(
        gesture: InsertGesture,
        viewConfiguration: ViewConfiguration?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        if (viewConfiguration == null) {
            return fallbackOnLegacyTextField(gesture, editCommandConsumer)
        }

        val offset =
            getOffsetForHandwritingGesture(
                pointInScreen = gesture.insertionPoint.toOffset(),
                viewConfiguration = viewConfiguration
            )
        // TODO(332963121): support gesture at BiDi boundaries.
        if (offset == -1 || layoutResult?.value?.isBiDiBoundary(offset) == true) {
            return fallbackOnLegacyTextField(gesture, editCommandConsumer)
        }

        performInsertionOnLegacyTextField(offset, gesture.textToInsert, editCommandConsumer)
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun LegacyTextFieldState.performRemoveSpaceGesture(
        gesture: RemoveSpaceGesture,
        text: AnnotatedString,
        viewConfiguration: ViewConfiguration?,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val range =
            layoutResult
                ?.value
                .getRangeForRemoveSpaceGesture(
                    startPointInScreen = gesture.startPoint.toOffset(),
                    endPointerInScreen = gesture.endPoint.toOffset(),
                    layoutCoordinates = layoutCoordinates,
                    viewConfiguration = viewConfiguration
                )
                .apply {
                    if (collapsed) return fallbackOnLegacyTextField(gesture, editCommandConsumer)
                }

        var firstMatchStart = -1
        var lastMatchEnd = -1
        val newText =
            text.substring(range).replace(Regex("\\s+")) {
                if (firstMatchStart == -1) {
                    firstMatchStart = it.range.first
                }
                lastMatchEnd = it.range.last + 1
                ""
            }

        // No whitespace is found in the target range, fallback instead
        if (firstMatchStart == -1 || lastMatchEnd == -1) {
            return fallbackOnLegacyTextField(gesture, editCommandConsumer)
        }

        // We only replace the part of the text that changes.
        // e.g. The text is "AB CD EF GH IJ" and the original gesture range is "CD EF GH"
        // We'll replace " EF " to "EF" instead of replacing "CD EF GH" to "CDEFGH",
        val replacedRangeStart = range.start + firstMatchStart
        val replacedRangeEnd = range.start + lastMatchEnd
        // Remove the unchanged part from the newText as well. Characters before firstMatchStart
        // and characters after the lastMatchEnd are removed.
        val finalNewText =
            newText.substring(
                startIndex = firstMatchStart,
                endIndex = newText.length - (range.length - lastMatchEnd)
            )

        editCommandConsumer.invoke(
            compoundEditCommand(
                SetSelectionCommand(replacedRangeStart, replacedRangeEnd),
                CommitTextCommand(finalNewText, 1)
            )
        )
        return InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS
    }

    private fun performInsertionOnLegacyTextField(
        offset: Int,
        text: String,
        editCommandConsumer: (EditCommand) -> Unit
    ) {
        editCommandConsumer.invoke(
            compoundEditCommand(SetSelectionCommand(offset, offset), CommitTextCommand(text, 1))
        )
    }

    private fun performSelectionOnLegacyTextField(
        range: TextRange,
        textSelectionManager: TextFieldSelectionManager?,
        editCommandConsumer: (EditCommand) -> Unit
    ) {
        editCommandConsumer.invoke(SetSelectionCommand(range.start, range.end))
        textSelectionManager?.enterSelectionMode(showFloatingToolbar = true)
    }

    private fun performDeletionOnLegacyTextField(
        range: TextRange,
        text: AnnotatedString,
        adjustRange: Boolean,
        editCommandConsumer: (EditCommand) -> Unit
    ) {
        val finalRange =
            if (adjustRange) {
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

    private fun fallbackOnLegacyTextField(
        gesture: HandwritingGesture,
        editCommandConsumer: (EditCommand) -> Unit
    ): Int {
        val fallbackText =
            gesture.fallbackText ?: return InputConnection.HANDWRITING_GESTURE_RESULT_FAILED
        editCommandConsumer.invoke(CommitTextCommand(fallbackText, newCursorPosition = 1))
        return InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK
    }

    /** Convert the Platform text granularity to Compose [TextGranularity] object. */
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
 * For handwriting delete gestures with word granularity, adjust the start and end offsets to remove
 * extra whitespace around the deleted text.
 */
private fun TextRange.adjustHandwritingDeleteGestureRange(text: CharSequence): TextRange {
    var start = this.start
    var end = this.end

    // If the deleted text is at the start of the text, the behavior is the same as the case
    // where the deleted text follows a new line character.
    var codePointBeforeStart: Int =
        if (start > 0) {
            Character.codePointBefore(text, start)
        } else {
            LINE_FEED_CODE_POINT
        }
    // If the deleted text is at the end of the text, the behavior is the same as the case where
    // the deleted text precedes a new line character.
    var codePointAtEnd: Int =
        if (end < text.length) {
            Character.codePointAt(text, end)
        } else {
            LINE_FEED_CODE_POINT
        }

    if (
        codePointBeforeStart.isWhitespaceExceptNewline() &&
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

    if (
        codePointAtEnd.isWhitespaceExceptNewline() &&
            (codePointBeforeStart.isWhitespace() || codePointBeforeStart.isPunctuation())
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
 * This helper method is copied from [android.text.TextUtils]. It returns true if the code point is
 * a new line.
 */
private fun Int.isNewline(): Boolean {
    val type = Character.getType(this)
    return type == Character.PARAGRAPH_SEPARATOR.toInt() ||
        type == Character.LINE_SEPARATOR.toInt() ||
        this == LINE_FEED_CODE_POINT
}

/**
 * This helper method is copied from [android.text.TextUtils]. It returns true if the code point is
 * a whitespace.
 */
private fun Int.isWhitespace(): Boolean {
    return Character.isWhitespace(this) || this == NBSP_CODE_POINT
}

/**
 * This helper method is copied from [android.text.TextUtils]. It returns true if the code point is
 * a whitespace and not a new line.
 */
private fun Int.isWhitespaceExceptNewline(): Boolean {
    return isWhitespace() && !isNewline()
}

/**
 * This helper method is copied from [android.text.TextUtils]. It returns true if the code point is
 * a punctuation.
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

private fun PointF.toOffset(): Offset = Offset(x, y)

private fun TextLayoutState.getRangeForScreenRect(
    rectInScreen: Rect,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange {
    return layoutResult
        ?.multiParagraph
        .getRangeForScreenRect(
            rectInScreen,
            textLayoutNodeCoordinates,
            granularity,
            inclusionStrategy
        )
}

private fun TextLayoutState.getRangeForScreenRects(
    startRectInScreen: Rect,
    endRectInScreen: Rect,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange {
    val startRange =
        getRangeForScreenRect(startRectInScreen, granularity, inclusionStrategy).apply {
            if (collapsed) return TextRange.Zero
        }

    val endRange =
        getRangeForScreenRect(endRectInScreen, granularity, inclusionStrategy).apply {
            if (collapsed) return TextRange.Zero
        }

    return enclosure(startRange, endRange)
}

private fun LegacyTextFieldState.getRangeForScreenRect(
    rectInScreen: Rect,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange {
    return layoutResult
        ?.value
        ?.multiParagraph
        .getRangeForScreenRect(rectInScreen, layoutCoordinates, granularity, inclusionStrategy)
}

private fun LegacyTextFieldState.getRangeForScreenRects(
    startRectInScreen: Rect,
    endRectInScreen: Rect,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange {
    val startRange =
        getRangeForScreenRect(startRectInScreen, granularity, inclusionStrategy).apply {
            if (collapsed) return TextRange.Zero
        }

    val endRange =
        getRangeForScreenRect(endRectInScreen, granularity, inclusionStrategy).apply {
            if (collapsed) return TextRange.Zero
        }

    return enclosure(startRange, endRange)
}

private fun CharSequence.rangeOfWhitespaces(offset: Int): TextRange {
    var startOffset = offset
    var endOffset = offset

    while (startOffset > 0) {
        val codePointBeforeStart = codePointBefore(startOffset)
        if (!codePointBeforeStart.isWhitespace()) {
            break
        }

        startOffset -= Character.charCount(codePointBeforeStart)
    }

    while (endOffset < length) {
        val codePointAtEnd = codePointAt(endOffset)
        if (!codePointAtEnd.isWhitespace()) {
            break
        }
        endOffset += charCount(codePointAtEnd)
    }

    return TextRange(startOffset, endOffset)
}

private fun TextLayoutState.getOffsetForHandwritingGesture(
    pointInScreen: Offset,
    viewConfiguration: ViewConfiguration?
): Int {
    return layoutResult
        ?.multiParagraph
        ?.getOffsetForHandwritingGesture(
            pointInScreen,
            textLayoutNodeCoordinates,
            viewConfiguration
        ) ?: -1
}

private fun LegacyTextFieldState.getOffsetForHandwritingGesture(
    pointInScreen: Offset,
    viewConfiguration: ViewConfiguration
): Int {
    return layoutResult
        ?.value
        ?.multiParagraph
        ?.getOffsetForHandwritingGesture(pointInScreen, layoutCoordinates, viewConfiguration) ?: -1
}

private fun TextLayoutResult.isBiDiBoundary(offset: Int): Boolean {
    val line = getLineForOffset(offset)
    if (offset == getLineStart(line) || offset == getLineEnd(line)) {
        return getParagraphDirection(offset) != getBidiRunDirection(offset)
    }

    // Offset can't 0 or text.length at the moment.
    return getBidiRunDirection(offset) != getBidiRunDirection(offset - 1)
}

private fun MultiParagraph?.getRangeForScreenRect(
    rectInScreen: Rect,
    layoutCoordinates: LayoutCoordinates?,
    granularity: TextGranularity,
    inclusionStrategy: TextInclusionStrategy
): TextRange {
    if (this == null || layoutCoordinates == null) {
        return TextRange.Zero
    }

    val screenOriginInLocal = layoutCoordinates.screenToLocal(Offset.Zero)
    val localRect = rectInScreen.translate(screenOriginInLocal)
    return getRangeForRect(localRect, granularity, inclusionStrategy)
}

private fun MultiParagraph.getOffsetForHandwritingGesture(
    pointInScreen: Offset,
    layoutCoordinates: LayoutCoordinates?,
    viewConfiguration: ViewConfiguration?
): Int {
    val localPoint = layoutCoordinates?.screenToLocal(pointInScreen) ?: return -1
    val line = getLineForHandwritingGesture(localPoint, viewConfiguration)
    if (line == -1) return -1

    val adjustedPoint = localPoint.copy(y = (getLineTop(line) + getLineBottom(line)) / 2f)
    return getOffsetForPosition(adjustedPoint)
}

private fun TextLayoutResult?.getRangeForRemoveSpaceGesture(
    startPointInScreen: Offset,
    endPointerInScreen: Offset,
    layoutCoordinates: LayoutCoordinates?,
    viewConfiguration: ViewConfiguration?
): TextRange {
    if (this == null || layoutCoordinates == null) {
        return TextRange.Zero
    }
    val localStartPoint = layoutCoordinates.screenToLocal(startPointInScreen)
    val localEndPoint = layoutCoordinates.screenToLocal(endPointerInScreen)
    val startLine = multiParagraph.getLineForHandwritingGesture(localStartPoint, viewConfiguration)
    val endLine = multiParagraph.getLineForHandwritingGesture(localEndPoint, viewConfiguration)
    val line: Int

    if (startLine == -1) {
        // Both start and end point are out of the line margin. Return null.
        if (endLine == -1) return TextRange.Zero
        line = endLine
    } else {
        line =
            if (endLine == -1) {
                startLine
            } else {
                // RemoveSpaceGesture is a single line gesture, it can't be applied for multiple
                // lines.
                // If start point and end point belongs to different lines, select the top line.
                min(startLine, endLine)
            }
    }

    val lineCenter = (getLineTop(line) + getLineBottom(line)) / 2

    val rect =
        Rect(
            left = min(localStartPoint.x, localEndPoint.x),
            top = lineCenter - 0.1f,
            right = max(localStartPoint.x, localEndPoint.x),
            bottom = lineCenter + 0.1f
        )

    return multiParagraph.getRangeForRect(
        rect,
        TextGranularity.Character,
        TextInclusionStrategy.AnyOverlap
    )
}

private fun MultiParagraph.getLineForHandwritingGesture(
    localPoint: Offset,
    viewConfiguration: ViewConfiguration?
): Int {
    val lineMargin = viewConfiguration?.handwritingGestureLineMargin ?: 0f
    val line = getLineForVerticalPosition(localPoint.y)

    if (
        localPoint.y < getLineTop(line) - lineMargin ||
            localPoint.y > getLineBottom(line) + lineMargin
    ) {
        // The point is not within lineMargin of a line.
        return -1
    }
    if (localPoint.x < -lineMargin || localPoint.x > width + lineMargin) {
        // The point is not within lineMargin of a line.
        return -1
    }
    return line
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

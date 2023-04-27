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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextEditFilter
import androidx.compose.foundation.text2.input.TextFieldBufferWithSelection
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.util.fastForEach

/**
 * Helper class to apply [EditCommand]s on an internal buffer. Used by TextField Composable
 * to combine TextFieldValue lifecycle with the editing operations.
 *
 * When a [TextFieldValue] is suggested by the developer, [reset] should be called.
 * When [TextInputService] provides [EditCommand]s, they should be applied to the internal
 * buffer using [apply].
 */
@OptIn(ExperimentalFoundationApi::class)
internal class EditProcessor(
    initialValue: TextFieldCharSequence = TextFieldCharSequence("", TextRange.Zero),
) {

    private val valueMutableState = mutableStateOf(initialValue)

    /**
     * The current state of the internal editing buffer as a [TextFieldCharSequence] backed by
     * snapshot state, so its readers can get updates in composition context.
     */
    var value: TextFieldCharSequence by valueMutableState
        private set

    val valueState: State<TextFieldCharSequence> get() = valueMutableState

    // The editing buffer used for applying editor commands from IME.
    internal var mBuffer: EditingBuffer = EditingBuffer(
        text = initialValue.toString(),
        selection = initialValue.selectionInChars
    )
        private set

    private val resetListeners = mutableVectorOf<ResetListener>()

    /**
     * Must be called whenever TextFieldValue needs to change directly, not using EditCommands.
     *
     * This method updates the internal editing buffer with the given TextFieldValue.
     * This method may tell the IME about the selection offset changes or extracted text changes.
     *
     * Retro(halilibo); this function seems straightforward but it actually does something very
     * specific for the previous state hoisting design of TextField. In each recomposition, this
     * function was supposed to be called from BasicTextField with the new value. However, this new
     * value wouldn't be new to the internal buffer since the changes coming from IME were already
     * applied in the previous composition and sent through onValueChange to the hoisted state.
     *
     * Therefore, this function has to care for two scenarios. 1) Developer doesn't interfere with
     * the value and the editing buffer doesn't have to change because previous composition value
     * is sent back. 2) Developer interferes and the new value is different than the current buffer
     * state. The difference could be text, selection, or composition.
     *
     * In short, `reset` function used to compare newly arrived value in this composition with the
     * internal buffer for any differences. This won't be necessary anymore since the internal state
     * is going to be the only source of truth for the new BasicTextField. However, `reset` would
     * gain a new responsibility in the cases where developer filters the input or adds a template.
     * This would again introduce a need for sync between internal buffer and the state value.
     */
    fun reset(newValue: TextFieldCharSequence) {
        val bufferState = TextFieldCharSequence(
            mBuffer.toString(),
            mBuffer.selection,
            mBuffer.composition
        )

        var textChanged = false
        var selectionChanged = false
        val compositionChanged = newValue.compositionInChars != mBuffer.composition

        if (!bufferState.contentEquals(newValue)) {
            // reset the buffer in its entirety
            mBuffer = EditingBuffer(
                text = newValue.toString(),
                selection = newValue.selectionInChars
            )
            textChanged = true
        } else if (bufferState.selectionInChars != newValue.selectionInChars) {
            mBuffer.setSelection(newValue.selectionInChars.min, newValue.selectionInChars.max)
            selectionChanged = true
        }

        val composition = newValue.compositionInChars
        if (composition == null || composition.collapsed) {
            mBuffer.commitComposition()
        } else {
            mBuffer.setComposition(composition.min, composition.max)
        }

        // TODO(halilibo): This could be unnecessary when we figure out how to correctly
        //  communicate composing region changes back to IME.
        if (textChanged || (!selectionChanged && compositionChanged)) {
            mBuffer.commitComposition()
        }

        val finalValue = TextFieldCharSequence(
            if (textChanged) newValue else bufferState,
            mBuffer.selection,
            mBuffer.composition
        )

        resetListeners.forEach { it.onReset(bufferState, finalValue) }

        value = finalValue
    }

    /**
     * Applies a set of [editCommands] to the internal text editing buffer.
     *
     * After applying the changes, returns the final state of the editing buffer as a
     * [TextFieldValue]
     *
     * @param editCommands [EditCommand]s to be applied to the editing buffer.
     *
     * @return the [TextFieldValue] representation of the final buffer state.
     */
    fun update(editCommands: List<EditCommand>, filter: TextEditFilter?) {
        var lastCommand: EditCommand? = null
        mBuffer.changeTracker.clearChanges()
        try {
            editCommands.fastForEach {
                lastCommand = it
                mBuffer.update(it)
            }
        } catch (e: Exception) {
            throw RuntimeException(generateBatchErrorMessage(editCommands, lastCommand), e)
        }

        val proposedValue = TextFieldCharSequence(
            text = mBuffer.toString(),
            selection = mBuffer.selection,
            composition = mBuffer.composition
        )

        @Suppress("NAME_SHADOWING")
        val filter = filter
        if (filter == null) {
            value = proposedValue
        } else {
            val oldValue = value
            val mutableValue = TextFieldBufferWithSelection(
                value = proposedValue,
                sourceValue = oldValue,
                initialChanges = mBuffer.changeTracker
            )
            filter.filter(originalValue = oldValue, valueWithChanges = mutableValue)
            // If neither the text nor the selection changed, we want to preserve the composition.
            // Otherwise, the IME will reset it anyway.
            val newValue = mutableValue.toTextFieldCharSequence(proposedValue.compositionInChars)
            if (newValue == proposedValue) {
                value = newValue
            } else {
                reset(newValue)
            }
        }
    }

    private fun generateBatchErrorMessage(
        editCommands: List<EditCommand>,
        failedCommand: EditCommand?,
    ): String = buildString {
        appendLine(
            "Error while applying EditCommand batch to buffer (" +
                "length=${mBuffer.length}, " +
                "composition=${mBuffer.composition}, " +
                "selection=${mBuffer.selection}):"
        )
        @Suppress("ListIterator")
        editCommands.joinTo(this, separator = "\n") {
            val prefix = if (failedCommand === it) " > " else "   "
            prefix + it.toStringForLog()
        }
    }

    internal fun addResetListener(resetListener: ResetListener) {
        resetListeners.add(resetListener)
    }

    internal fun removeResetListener(resetListener: ResetListener) {
        resetListeners.remove(resetListener)
    }

    /**
     * A listener that can be attached to an EditProcessor to listen for reset events. State in
     * EditProcessor can change through filters or direct access. Unlike IME events (EditCommands),
     * these direct changes should be immediately passed onto IME to keep editor state and IME in
     * sync. Moreover, some changes can even require an input session restart to reset the state
     * in IME.
     */
    internal fun interface ResetListener {

        fun onReset(oldValue: TextFieldCharSequence, newValue: TextFieldCharSequence)
    }
}

/**
 * Generate a description of the command that is suitable for logging – this should not include
 * any user-entered text, which may be sensitive.
 */
internal fun EditCommand.toStringForLog(): String = when (this) {
    is CommitTextCommand ->
        "CommitTextCommand(text.length=${text.length}, newCursorPosition=$newCursorPosition)"

    is SetComposingTextCommand ->
        "SetComposingTextCommand(text.length=${text.length}, " +
            "newCursorPosition=$newCursorPosition)"

    is SetComposingRegionCommand -> toString()
    is DeleteSurroundingTextCommand -> toString()
    is DeleteSurroundingTextInCodePointsCommand -> toString()
    is SetSelectionCommand -> toString()
    is FinishComposingTextCommand -> toString()
    is BackspaceCommand -> toString()
    is MoveCursorCommand -> toString()
    is DeleteAllCommand -> toString()
}

private val EmptyAnnotatedString = buildAnnotatedString { }
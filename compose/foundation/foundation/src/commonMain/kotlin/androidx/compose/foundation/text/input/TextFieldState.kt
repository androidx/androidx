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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text.input

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.internal.EditingBuffer
import androidx.compose.foundation.text.input.internal.undo.TextFieldEditUndoBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.Flow

internal fun TextFieldState(initialValue: TextFieldValue): TextFieldState {
    return TextFieldState(
        initialText = initialValue.text,
        initialSelection = initialValue.selection
    )
}

/**
 * The editable text state of a text field, including both the [text] itself and position of the
 * cursor or selection.
 *
 * To change the text field contents programmatically, call [edit], [setTextAndSelectAll],
 * [setTextAndPlaceCursorAtEnd], or [clearText]. Individual parts of the state like [text],
 * [selection], or [composition] can be read from any snapshot restart scope like Composable
 * functions. To observe these members from outside a restart scope, use
 * `snapshotFlow { textFieldState.text }` or `snapshotFlow { textFieldState.selection }`. To
 * observe the entirety of state including text, selection, and composition, call [valueAsFlow].
 *
 * When instantiating this class from a composable, use [rememberTextFieldState] to automatically
 * save and restore the field state. For more advanced use cases, pass [TextFieldState.Saver] to
 * [rememberSaveable].
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldStateCompleteSample
 */
@Stable
class TextFieldState internal constructor(
    initialText: String,
    initialSelection: TextRange,
    initialTextUndoManager: TextUndoManager
) {

    constructor(
        initialText: String = "",
        initialSelection: TextRange = TextRange(initialText.length)
    ) : this(initialText, initialSelection, TextUndoManager())

    /**
     * Manages the history of edit operations that happen in this [TextFieldState].
     */
    internal val textUndoManager: TextUndoManager = initialTextUndoManager

    /**
     * The editing buffer used for applying editor commands from IME. All edits coming from gestures
     * or IME commands, must be reflected on this buffer eventually.
     */
    @VisibleForTesting
    internal var mainBuffer: EditingBuffer = EditingBuffer(
        text = initialText,
        selection = initialSelection.coerceIn(0, initialText.length)
    )

    /**
     * [TextFieldState] does not synchronize calls to [edit] but requires main thread access. It
     * also has no way to disallow reentrant behavior (nested calls to [edit]) through the API.
     * Instead we keep track of whether an edit session is currently running. If [edit] is called
     * concurrently or reentered, it should throw an exception. The only exception is if
     * [TextFieldState] is being modified in two different snapshots. Hence, this value is backed
     * by a snapshot state.
     */
    private var isEditing: Boolean by mutableStateOf(false)

    /**
     * The current text, selection, and composing region. This value will automatically update when
     * the user enters text or otherwise changes the text field contents. To change it
     * programmatically, call [edit].
     *
     * This is backed by snapshot state, so reading this property in a restartable function (e.g.
     * a composable function) will cause the function to restart when the text field's value
     * changes.
     *
     * To observe changes to this property outside a restartable function, see [valueAsFlow].
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldTextDerivedStateSample
     *
     * @see edit
     * @see valueAsFlow
     */
    internal var value: TextFieldCharSequence by mutableStateOf(
        TextFieldCharSequence(initialText, initialSelection)
    )
        private set

    /**
     * The current text content. This value will automatically update when the user enters text or
     * otherwise changes the text field contents. To change it programmatically, call [edit].
     *
     * To observe changes to this property outside a restartable function, use
     * `snapshotFlow { text }`.
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldTextValuesSample
     *
     * @see edit
     * @see snapshotFlow
     */
    val text: CharSequence get() = value.getBackingCharSequence()

    /**
     * The current selection range. If the selection is collapsed, it represents cursor location.
     * This value will automatically update when the user enters text or otherwise changes the text
     * field selection range. To change it programmatically, call [edit].
     *
     * To observe changes to this property outside a restartable function, use
     * `snapshotFlow { selection }`.
     *
     * @see edit
     * @see snapshotFlow
     * @see TextFieldCharSequence.selection
     */
    val selection: TextRange get() = value.selection

    /**
     * The current composing range dictated by the IME. If null, there is no composing region.
     *
     * To observe changes to this property outside a restartable function, use
     * `snapshotFlow { composition }`.
     *
     * @see edit
     * @see snapshotFlow
     * @see TextFieldCharSequence.composition
     */
    val composition: TextRange? get() = value.composition

    /**
     * Runs [block] with a mutable version of the current state. The block can make changes to the
     * text and cursor/selection. See the documentation on [TextFieldBuffer] for a more detailed
     * description of the available operations.
     *
     * Make sure that you do not make concurrent calls to this function or call it again inside
     * [block]'s scope. Doing either of these actions will result in triggering an
     * [IllegalStateException].
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldStateEditSample
     *
     * @see setTextAndPlaceCursorAtEnd
     * @see setTextAndSelectAll
     */
    inline fun edit(block: TextFieldBuffer.() -> Unit) {
        val mutableValue = startEdit()
        try {
            mutableValue.block()
            commitEdit(mutableValue)
        } finally {
            finishEditing()
        }
    }

    override fun toString(): String =
        "TextFieldState(selection=$selection, text=\"$text\")"

    /**
     * Undo history controller for this TextFieldState.
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldUndoSample
     */
    // TextField does not implement UndoState because Undo related APIs should be able to remain
    // separately experimental than TextFieldState
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalFoundationApi
    @get:ExperimentalFoundationApi
    val undoState: UndoState = UndoState(this)

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun startEdit(): TextFieldBuffer {
        check(!isEditing) {
            "TextFieldState does not support concurrent or nested editing."
        }
        isEditing = true
        return TextFieldBuffer(value)
    }

    /**
     * If the text or selection in [newValue] was actually modified, updates this state's internal
     * values. If [newValue] was not modified at all, the state is not updated, and this will not
     * invalidate anyone who is observing this state.
     *
     * @param newValue [TextFieldBuffer] that contains the latest updates
     */
    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun commitEdit(newValue: TextFieldBuffer) {
        val textChanged = newValue.changes.changeCount > 0
        val selectionChanged = newValue.selection != mainBuffer.selection
        if (textChanged || selectionChanged) {
            val finalValue = newValue.toTextFieldCharSequence()
            resetStateAndNotifyIme(finalValue)
        }
        textUndoManager.clearHistory()
    }

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun finishEditing() {
        isEditing = false
    }

    /**
     * An edit block that updates [TextFieldState] on behalf of user actions such as gestures,
     * IME commands, hardware keyboard events, clipboard actions, and more. These modifications
     * must also run through the given [filter] since they are user actions.
     *
     * Be careful that this method is not snapshot aware. It is only safe to call this from main
     * thread, or global snapshot. Also, this function is defined as inline for performance gains,
     * and it's not actually safe to early return from [block].
     *
     * Also all user edits should be recorded by [textUndoManager] since reverting to a previous
     * state requires all edit operations to be executed in reverse. However, some commands like
     * cut, and paste should be atomic operations that do not merge with previous or next operations
     * in the Undo stack. This can be controlled by [undoBehavior].
     *
     * @param inputTransformation [InputTransformation] to run after [block] is applied
     * @param restartImeIfContentChanges Whether IME should be restarted if the proposed changes
     * end up editing the text content. Only pass false to this argument if the source of the
     * changes is IME itself.
     * @param block The function that updates the current editing buffer.
     */
    internal inline fun editAsUser(
        inputTransformation: InputTransformation?,
        restartImeIfContentChanges: Boolean = true,
        undoBehavior: TextFieldEditUndoBehavior = TextFieldEditUndoBehavior.MergeIfPossible,
        block: EditingBuffer.() -> Unit
    ) {
        val previousValue = value

        mainBuffer.changeTracker.clearChanges()
        mainBuffer.block()

        if (mainBuffer.changeTracker.changeCount == 0 &&
            previousValue.selection == mainBuffer.selection &&
            previousValue.composition == mainBuffer.composition) {
            // nothing has changed after applying block.
            return
        }

        commitEditAsUser(
            previousValue = previousValue,
            inputTransformation = inputTransformation,
            restartImeIfContentChanges = restartImeIfContentChanges,
            undoBehavior = undoBehavior
        )
    }

    /**
     * Edits the contents of this [TextFieldState] without going through an [InputTransformation],
     * or recording the changes to the [textUndoManager]. IME would still be notified of any changes
     * committed by [block].
     *
     * This method of editing is not recommended for majority of use cases. It is originally added
     * to support applying of undo/redo actions without clearing the history. Also, it doesn't
     * allocate an additional buffer like [edit] method because changes are ignored and it's not
     * a public API.
     */
    internal inline fun editWithNoSideEffects(block: EditingBuffer.() -> Unit) {
        val previousValue = value

        mainBuffer.changeTracker.clearChanges()
        mainBuffer.block()

        val afterEditValue = TextFieldCharSequence(
            text = mainBuffer.toString(),
            selection = mainBuffer.selection,
            composition = mainBuffer.composition
        )

        value = afterEditValue
        sendChangesToIme(
            oldValue = previousValue,
            newValue = afterEditValue,
            restartImeIfContentChanges = true
        )
    }

    private fun commitEditAsUser(
        previousValue: TextFieldCharSequence,
        inputTransformation: InputTransformation?,
        restartImeIfContentChanges: Boolean,
        undoBehavior: TextFieldEditUndoBehavior
    ) {
        val afterEditValue = TextFieldCharSequence(
            text = mainBuffer.toString(),
            selection = mainBuffer.selection,
            composition = mainBuffer.composition
        )

        if (inputTransformation == null) {
            val oldValue = value
            value = afterEditValue
            sendChangesToIme(
                oldValue = oldValue,
                newValue = afterEditValue,
                restartImeIfContentChanges = restartImeIfContentChanges
            )
            recordEditForUndo(previousValue, value, mainBuffer.changeTracker, undoBehavior)
            return
        }

        val oldValue = value

        // if only difference is composition, don't run filter, don't send it to undo manager
        if (afterEditValue.contentEquals(oldValue) &&
            afterEditValue.selection == oldValue.selection
        ) {
            value = afterEditValue
            sendChangesToIme(
                oldValue = oldValue,
                newValue = afterEditValue,
                restartImeIfContentChanges = restartImeIfContentChanges
            )
            return
        }

        val textFieldBuffer = TextFieldBuffer(
            initialValue = afterEditValue,
            sourceValue = oldValue,
            initialChanges = mainBuffer.changeTracker
        )
        inputTransformation.transformInput(
            originalValue = oldValue,
            valueWithChanges = textFieldBuffer
        )
        // If neither the text nor the selection changed, we want to preserve the composition.
        // Otherwise, the IME will reset it anyway.
        val afterFilterValue = textFieldBuffer.toTextFieldCharSequence(
            composition = afterEditValue.composition
        )
        if (afterFilterValue == afterEditValue) {
            value = afterFilterValue
            sendChangesToIme(
                oldValue = oldValue,
                newValue = afterEditValue,
                restartImeIfContentChanges = restartImeIfContentChanges
            )
        } else {
            resetStateAndNotifyIme(afterFilterValue)
        }
        // mutableValue contains all the changes from user and the filter.
        recordEditForUndo(previousValue, value, textFieldBuffer.changes, undoBehavior)
    }

    /**
     * Records the difference between [previousValue] and [postValue], defined by [changes],
     * into [textUndoManager] according to the strategy defined by [undoBehavior].
     */
    private fun recordEditForUndo(
        previousValue: TextFieldCharSequence,
        postValue: TextFieldCharSequence,
        changes: TextFieldBuffer.ChangeList,
        undoBehavior: TextFieldEditUndoBehavior
    ) {
        when (undoBehavior) {
            TextFieldEditUndoBehavior.ClearHistory -> {
                textUndoManager.clearHistory()
            }
            TextFieldEditUndoBehavior.MergeIfPossible -> {
                textUndoManager.recordChanges(
                    pre = previousValue,
                    post = postValue,
                    changes = changes,
                    allowMerge = true
                )
            }
            TextFieldEditUndoBehavior.NeverMerge -> {
                textUndoManager.recordChanges(
                    pre = previousValue,
                    post = postValue,
                    changes = changes,
                    allowMerge = false
                )
            }
        }
    }

    internal fun addNotifyImeListener(notifyImeListener: NotifyImeListener) {
        notifyImeListeners.add(notifyImeListener)
    }

    internal fun removeNotifyImeListener(notifyImeListener: NotifyImeListener) {
        notifyImeListeners.remove(notifyImeListener)
    }

    /**
     * A listener that can be attached to a [TextFieldState] to listen for change events that may
     * interest IME.
     *
     * State in [TextFieldState] can change through various means but categorically there are two
     * sources; Developer([TextFieldState.edit]) and User([TextFieldState.editAsUser]). Only
     * non-InputTransformed IME sourced changes can skip updating the IME. Otherwise, all changes
     * must be sent to the IME to let it synchronize its state with the [TextFieldState]. Such
     * a communication channel is established by the IME registering a [NotifyImeListener] on a
     * [TextFieldState].
     */
    internal fun interface NotifyImeListener {

        /**
         * Called when the value in [TextFieldState] changes via any source. The
         * [restartImeIfContentChanges] flag determines whether a text change between [oldValue]
         * and [newValue] should restart the ongoing input connection. Selection changes never
         * require a restart.
         */
        fun onChange(
            oldValue: TextFieldCharSequence,
            newValue: TextFieldCharSequence,
            restartImeIfContentChanges: Boolean
        )
    }

    /**
     * Must be called whenever [text] needs to change but the content of the changes are not yet
     * replicated on [mainBuffer].
     *
     * This method updates the internal editing buffer with the given [TextFieldCharSequence], it
     * also notifies the IME about the selection or composition changes.
     */
    @VisibleForTesting
    internal fun resetStateAndNotifyIme(newValue: TextFieldCharSequence) {
        val bufferState = TextFieldCharSequence(
            mainBuffer.toString(),
            mainBuffer.selection,
            mainBuffer.composition
        )

        var textChanged = false
        var selectionChanged = false
        val compositionChanged = newValue.composition != mainBuffer.composition

        if (!bufferState.contentEquals(newValue)) {
            // reset the buffer in its entirety
            mainBuffer = EditingBuffer(
                text = newValue.toString(),
                selection = newValue.selection
            )
            textChanged = true
        } else if (bufferState.selection != newValue.selection) {
            mainBuffer.setSelection(newValue.selection.start, newValue.selection.end)
            selectionChanged = true
        }

        val composition = newValue.composition
        if (composition == null || composition.collapsed) {
            mainBuffer.commitComposition()
        } else {
            mainBuffer.setComposition(composition.min, composition.max)
        }

        if (textChanged || (!selectionChanged && compositionChanged)) {
            mainBuffer.commitComposition()
        }

        val finalValue = TextFieldCharSequence(
            text = if (textChanged) newValue else bufferState,
            selection = mainBuffer.selection,
            composition = mainBuffer.composition
        )

        // value must be set before notifyImeListeners are called. Even though we are sending the
        // previous and current values, a system callback may request the latest state e.g. IME
        // restartInput call is handled before notifyImeListeners return.
        value = finalValue

        sendChangesToIme(
            oldValue = bufferState,
            newValue = finalValue,
            restartImeIfContentChanges = true
        )
    }

    private val notifyImeListeners = mutableVectorOf<NotifyImeListener>()

    /**
     * Sends an update to the IME depending on .
     */
    private fun sendChangesToIme(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldCharSequence,
        restartImeIfContentChanges: Boolean
    ) {
        notifyImeListeners.forEach {
            it.onChange(oldValue, newValue, restartImeIfContentChanges)
        }
    }

    /**
     * Saves and restores a [TextFieldState] for [rememberSaveable].
     *
     * @see rememberTextFieldState
     */
    // Preserve nullability since this is public API.
    @Suppress("RedundantNullableReturnType")
    object Saver : androidx.compose.runtime.saveable.Saver<TextFieldState, Any> {

        override fun SaverScope.save(value: TextFieldState): Any? {
            return listOf(
                value.text.toString(),
                value.selection.start,
                value.selection.end,
                with(TextUndoManager.Companion.Saver) {
                    save(value.textUndoManager)
                }
            )
        }

        override fun restore(value: Any): TextFieldState? {
            val (text, selectionStart, selectionEnd, savedTextUndoManager) = value as List<*>
            return TextFieldState(
                initialText = text as String,
                initialSelection = TextRange(
                    start = selectionStart as Int,
                    end = selectionEnd as Int
                ),
                initialTextUndoManager = with(TextUndoManager.Companion.Saver) {
                    restore(savedTextUndoManager!!)
                }!!
            )
        }
    }
}

/**
 * Returns a [Flow] of the values of [TextFieldState.text], [TextFieldState.selection], and
 * [TextFieldState.composition] as seen from the global snapshot.
 * The initial value is emitted immediately when the flow is collected.
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldTextValuesSample
 */
@ExperimentalFoundationApi
fun TextFieldState.valueAsFlow(): Flow<TextFieldCharSequence> = snapshotFlow { value }

/**
 * Create and remember a [TextFieldState]. The state is remembered using [rememberSaveable] and so
 * will be saved and restored with the composition.
 *
 * If you need to store a [TextFieldState] in another object, use the [TextFieldState.Saver] object
 * to manually save and restore the state.
 */
@Composable
fun rememberTextFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length)
): TextFieldState = rememberSaveable(saver = TextFieldState.Saver) {
    TextFieldState(initialText, initialSelection)
}

/**
 * Sets the text in this [TextFieldState] to [text], replacing any text that was previously there,
 * and places the cursor at the end of the new text.
 *
 * To perform more complicated edits on the text, call [TextFieldState.edit]. This function is
 * equivalent to calling:
 * ```
 * edit {
 *   replace(0, length, text)
 *   placeCursorAtEnd()
 * }
 * ```
 *
 * @see setTextAndSelectAll
 * @see clearText
 * @see TextFieldBuffer.placeCursorAtEnd
 */
@ExperimentalFoundationApi
fun TextFieldState.setTextAndPlaceCursorAtEnd(text: String) {
    edit {
        replace(0, length, text)
        placeCursorAtEnd()
    }
}

/**
 * Sets the text in this [TextFieldState] to [text], replacing any text that was previously there,
 * and selects all the text.
 *
 * To perform more complicated edits on the text, call [TextFieldState.edit]. This function is
 * equivalent to calling:
 * ```
 * edit {
 *   replace(0, length, text)
 *   selectAll()
 * }
 * ```
 *
 * @see setTextAndPlaceCursorAtEnd
 * @see clearText
 * @see TextFieldBuffer.selectAll
 */
@ExperimentalFoundationApi
fun TextFieldState.setTextAndSelectAll(text: String) {
    edit {
        replace(0, length, text)
        selectAll()
    }
}

/**
 * Deletes all the text in the state.
 *
 * To perform more complicated edits on the text, call [TextFieldState.edit]. This function is
 * equivalent to calling:
 * ```
 * edit {
 *   delete(0, length)
 *   placeCursorAtEnd()
 * }
 * ```
 *
 * @see setTextAndPlaceCursorAtEnd
 * @see setTextAndSelectAll
 */
@ExperimentalFoundationApi
fun TextFieldState.clearText() {
    edit {
        delete(0, length)
        placeCursorAtEnd()
    }
}

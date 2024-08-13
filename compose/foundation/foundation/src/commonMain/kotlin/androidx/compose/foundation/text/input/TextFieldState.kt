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
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.text.input.internal.undo.TextFieldEditUndoBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.style.TextDecoration

/**
 * The editable text state of a text field, including both the [text] itself and position of the
 * cursor or selection.
 *
 * To change the text field contents programmatically, call [edit], [setTextAndSelectAll],
 * [setTextAndPlaceCursorAtEnd], or [clearText]. Individual parts of the state like [text],
 * [selection], or [composition] can be read from any snapshot restart scope like Composable
 * functions. To observe these members from outside a restart scope, use `snapshotFlow {
 * textFieldState.text }` or `snapshotFlow { textFieldState.selection }`.
 *
 * When instantiating this class from a composable, use [rememberTextFieldState] to automatically
 * save and restore the field state. For more advanced use cases, pass [TextFieldState.Saver] to
 * [rememberSaveable].
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldStateCompleteSample
 */
@Stable
class TextFieldState
internal constructor(
    initialText: String,
    initialSelection: TextRange,
    initialTextUndoManager: TextUndoManager
) {

    constructor(
        initialText: String = "",
        initialSelection: TextRange = TextRange(initialText.length)
    ) : this(initialText, initialSelection, TextUndoManager())

    /** Manages the history of edit operations that happen in this [TextFieldState]. */
    internal val textUndoManager: TextUndoManager = initialTextUndoManager

    /**
     * The buffer used for applying editor commands from IME. All edits coming from gestures or IME
     * commands must be reflected on this buffer eventually.
     */
    @VisibleForTesting
    internal var mainBuffer: TextFieldBuffer =
        TextFieldBuffer(
            initialValue =
                TextFieldCharSequence(
                    text = initialText,
                    selection = initialSelection.coerceIn(0, initialText.length)
                )
        )

    /**
     * [TextFieldState] does not synchronize calls to [edit] but requires main thread access. It
     * also has no way to disallow reentrant behavior (nested calls to [edit]) through the API.
     * Instead we keep track of whether an edit session is currently running. If [edit] is called
     * concurrently or reentered, it should throw an exception. The only exception is if
     * [TextFieldState] is being modified in two different snapshots. Hence, this value is backed by
     * a snapshot state.
     */
    private var isEditing: Boolean by mutableStateOf(false)

    /**
     * The current text, selection, and composing region. This value will automatically update when
     * the user enters text or otherwise changes the text field contents. To change it
     * programmatically, call [edit].
     *
     * This is backed by snapshot state, so reading this property in a restartable function (e.g. a
     * composable function) will cause the function to restart when the text field's value changes.
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldTextDerivedStateSample
     * @see edit
     */
    internal var value: TextFieldCharSequence by
        mutableStateOf(TextFieldCharSequence(initialText, initialSelection))
        /** Do not set directly. Always go through [updateValueAndNotifyListeners]. */
        private set

    /**
     * The current text content. This value will automatically update when the user enters text or
     * otherwise changes the text field contents. To change it programmatically, call [edit].
     *
     * To observe changes to this property outside a restartable function, use `snapshotFlow { text
     * }`.
     *
     * @sample androidx.compose.foundation.samples.BasicTextFieldTextValuesSample
     * @see edit
     * @see snapshotFlow
     */
    val text: CharSequence
        get() = value.text

    /**
     * The current selection range. If the selection is collapsed, it represents cursor location.
     * This value will automatically update when the user enters text or otherwise changes the text
     * field selection range. To change it programmatically, call [edit].
     *
     * To observe changes to this property outside a restartable function, use `snapshotFlow {
     * selection }`.
     *
     * @see edit
     * @see snapshotFlow
     * @see TextFieldCharSequence.selection
     */
    val selection: TextRange
        get() = value.selection

    /**
     * The current composing range dictated by the IME. If null, there is no composing region.
     *
     * To observe changes to this property outside a restartable function, use `snapshotFlow {
     * composition }`.
     *
     * @see edit
     * @see snapshotFlow
     * @see TextFieldCharSequence.composition
     */
    val composition: TextRange?
        get() = value.composition

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
        Snapshot.withoutReadObservation { "TextFieldState(selection=$selection, text=\"$text\")" }

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
        val isEditingFreeze = Snapshot.withoutReadObservation { isEditing }
        checkPrecondition(!isEditingFreeze) {
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
        if (textChanged) {
            // clear the undo history after a programmatic edit if the text content has changed
            textUndoManager.clearHistory()
        }
        syncMainBufferToTemporaryBuffer(
            temporaryBuffer = newValue,
            textChanged = textChanged,
            selectionChanged = selectionChanged
        )
    }

    @Suppress("ShowingMemberInHiddenClass")
    @PublishedApi
    internal fun finishEditing() {
        isEditing = false
    }

    /**
     * An edit block that updates [TextFieldState] on behalf of user actions such as gestures, IME
     * commands, hardware keyboard events, clipboard actions, and more. These modifications must
     * also run through the given [inputTransformation] since they are user actions.
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
     * @param restartImeIfContentChanges Whether IME should be restarted if the proposed changes end
     *   up editing the text content. Only pass false to this argument if the source of the changes
     *   is IME itself.
     * @param block The function that updates the current buffer.
     */
    internal inline fun editAsUser(
        inputTransformation: InputTransformation?,
        restartImeIfContentChanges: Boolean = true,
        undoBehavior: TextFieldEditUndoBehavior = TextFieldEditUndoBehavior.MergeIfPossible,
        block: TextFieldBuffer.() -> Unit
    ) {
        mainBuffer.changeTracker.clearChanges()
        mainBuffer.block()

        commitEditAsUser(
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
     * allocate an additional buffer like [edit] method because changes are ignored and it's not a
     * public API.
     */
    internal inline fun editWithNoSideEffects(block: TextFieldBuffer.() -> Unit) {
        mainBuffer.changeTracker.clearChanges()
        mainBuffer.block()

        val afterEditValue = mainBuffer.toTextFieldCharSequence()

        updateValueAndNotifyListeners(
            oldValue = value,
            newValue = afterEditValue,
            restartImeIfContentChanges = true
        )
    }

    // Do not inline this function into editAsUser. Inline functions should be kept short.
    private fun commitEditAsUser(
        inputTransformation: InputTransformation?,
        restartImeIfContentChanges: Boolean = true,
        undoBehavior: TextFieldEditUndoBehavior = TextFieldEditUndoBehavior.MergeIfPossible,
    ) {
        val beforeEditValue = value

        // first immediately check whether there's any actual change of content or selection.
        // We only look at the operation count for content change because inputTransformation
        // should still run even if text doesn't change as a result of an input.
        // If there is no change at all, or only composition or highlight has changed, we can end
        // early.
        if (
            mainBuffer.changeTracker.changeCount == 0 &&
                beforeEditValue.selection == mainBuffer.selection
        ) {
            if (
                beforeEditValue.composition != mainBuffer.composition ||
                    beforeEditValue.highlight != mainBuffer.highlight ||
                    beforeEditValue.composingAnnotations != mainBuffer.composingAnnotations
            ) {
                // edit operation caused no change to text content or selection
                // No need to run an existing InputTransformation, or record an undo. Only update
                // the IME that composition has been accepted.
                updateValueAndNotifyListeners(
                    oldValue = value,
                    newValue =
                        TextFieldCharSequence(
                            text = mainBuffer.toString(),
                            selection = mainBuffer.selection,
                            composition = mainBuffer.composition,
                            highlight = mainBuffer.highlight,
                            composingAnnotations =
                                finalizeComposingAnnotations(
                                    composition = mainBuffer.composition,
                                    annotationList = mainBuffer.composingAnnotations
                                )
                        ),
                    restartImeIfContentChanges = restartImeIfContentChanges
                )
            }
            return
        }

        // There's a meaningful change to the buffer, let's run the full logic.
        // first take a _snapshot_ of current state of the mainBuffer after changes are applied.
        val afterEditValue =
            TextFieldCharSequence(
                text = mainBuffer.toString(),
                selection = mainBuffer.selection,
                composition = mainBuffer.composition,
                highlight = mainBuffer.highlight,
                composingAnnotations =
                    finalizeComposingAnnotations(
                        composition = mainBuffer.composition,
                        annotationList = mainBuffer.composingAnnotations
                    )
            )

        // if there's no filter; just record the undo, update the snapshot value, end.
        if (inputTransformation == null) {
            updateValueAndNotifyListeners(
                oldValue = beforeEditValue,
                newValue = afterEditValue,
                restartImeIfContentChanges = restartImeIfContentChanges
            )
            recordEditForUndo(
                previousValue = beforeEditValue,
                postValue = afterEditValue,
                changes = mainBuffer.changeTracker,
                undoBehavior = undoBehavior
            )
            return
        }

        // Prepare a TextFieldBuffer to run InputTransformation. TextFieldBuffer should be
        // initialized with the edits that are already applied on mainBuffer, hence the difference
        // between originalValue and initialValue.
        val textFieldBuffer =
            TextFieldBuffer(
                originalValue = beforeEditValue,
                initialValue = afterEditValue,
                initialChanges = mainBuffer.changeTracker
            )

        // apply the inputTransformation.
        with(inputTransformation) { textFieldBuffer.transformInput() }

        val textChangedByFilter = !textFieldBuffer.asCharSequence().contentEquals(afterEditValue)
        val selectionChangedByFilter = textFieldBuffer.selection != afterEditValue.selection
        if (textChangedByFilter || selectionChangedByFilter) {
            syncMainBufferToTemporaryBuffer(
                temporaryBuffer = textFieldBuffer,
                textChanged = textChangedByFilter,
                selectionChanged = selectionChangedByFilter
            )
        } else {
            updateValueAndNotifyListeners(
                oldValue = beforeEditValue,
                // If neither the text nor the selection changed by the filter, we want to preserve
                // the composition. Otherwise, the IME will reset it anyway.
                newValue =
                    textFieldBuffer.toTextFieldCharSequence(
                        composition = afterEditValue.composition
                    ),
                restartImeIfContentChanges = restartImeIfContentChanges
            )
        }
        // textFieldBuffer contains all the changes from both the user and the filter.
        recordEditForUndo(
            previousValue = beforeEditValue,
            postValue = value,
            changes = textFieldBuffer.changes,
            undoBehavior = undoBehavior
        )
    }

    /**
     * There are 3 types of edits that are defined on [TextFieldState];
     * 1. Programmatic changes that are created by [edit] function,
     * 2. User edits coming from the system that are initiated through IME, semantics, or gestures
     * 3. Applying Undo/Redo actions that should not trigger side effects.
     *
     * Eventually all changes, no matter the source, should be committed to [value]. Also, they have
     * to trigger the content change listeners.
     *
     * Finally notifies the listeners in [notifyImeListeners] that the contents of this
     * [TextFieldState] has changed.
     */
    private fun updateValueAndNotifyListeners(
        oldValue: TextFieldCharSequence,
        newValue: TextFieldCharSequence,
        restartImeIfContentChanges: Boolean
    ) {
        // value must be set before notifyImeListeners are called. Even though we are sending the
        // previous and current values, a system callback may request the latest state e.g. IME
        // restartInput call is handled before notifyImeListeners return.
        value = newValue
        finishEditing()

        notifyImeListeners.forEach { it.onChange(oldValue, newValue, restartImeIfContentChanges) }
    }

    /**
     * Records the difference between [previousValue] and [postValue], defined by [changes], into
     * [textUndoManager] according to the strategy defined by [undoBehavior].
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
     * must be sent to the IME to let it synchronize its state with the [TextFieldState]. Such a
     * communication channel is established by the IME registering a [NotifyImeListener] on a
     * [TextFieldState].
     */
    internal fun interface NotifyImeListener {

        /**
         * Called when the value in [TextFieldState] changes via any source. The
         * [restartImeIfContentChanges] flag determines whether a text change between [oldValue] and
         * [newValue] should restart the ongoing input connection. Selection changes never require a
         * restart.
         */
        fun onChange(
            oldValue: TextFieldCharSequence,
            newValue: TextFieldCharSequence,
            restartImeIfContentChanges: Boolean
        )
    }

    /**
     * Carries changes made to a [temporaryBuffer] into [mainBuffer], then updates the [value]. This
     * usually happens when the edit source is something programmatic like [edit] or
     * [InputTransformation]. Normally IME commands are applied directly on [mainBuffer].
     *
     * @param temporaryBuffer Source buffer that will be used to sync the mainBuffer.
     * @param textChanged Whether the text content inside [temporaryBuffer] is different than
     *   [mainBuffer]'s text content. Although this value can be calculated by this function, some
     *   callers already do the comparison before hand, so there's no need to recalculate it.
     * @param selectionChanged Whether the selection inside [temporaryBuffer] is different than
     *   [mainBuffer]'s selection.
     */
    @VisibleForTesting
    internal fun syncMainBufferToTemporaryBuffer(
        temporaryBuffer: TextFieldBuffer,
        textChanged: Boolean,
        selectionChanged: Boolean
    ) {
        val oldValue = mainBuffer.toTextFieldCharSequence()

        if (textChanged) {
            // reset the buffer in its entirety
            mainBuffer =
                TextFieldBuffer(
                    initialValue =
                        TextFieldCharSequence(
                            text = temporaryBuffer.toString(),
                            selection = temporaryBuffer.selection
                        ),
                )
        } else if (selectionChanged) {
            mainBuffer.selection =
                TextRange(temporaryBuffer.selection.start, temporaryBuffer.selection.end)
        }

        // Composition should be decided by the IME after the content or selection has been
        // changed programmatically, outside the knowledge of the IME.
        mainBuffer.commitComposition()

        val finalValue = mainBuffer.toTextFieldCharSequence()

        // We cannot use `value` as the old value here because intermediate IME changes are only
        // applied on mainBuffer (this only happens if syncMainBufferToTemporaryBuffer is triggered
        // after an InputTransformation). We must pass in the latest state just before finalValue is
        // calculated. This is the state IME knows about and is synced with.
        updateValueAndNotifyListeners(
            oldValue = oldValue,
            newValue = finalValue,
            restartImeIfContentChanges = true
        )
    }

    private val notifyImeListeners = mutableVectorOf<NotifyImeListener>()

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
                with(TextUndoManager.Companion.Saver) { save(value.textUndoManager) }
            )
        }

        override fun restore(value: Any): TextFieldState? {
            val (text, selectionStart, selectionEnd, savedTextUndoManager) = value as List<*>
            return TextFieldState(
                initialText = text as String,
                initialSelection =
                    TextRange(start = selectionStart as Int, end = selectionEnd as Int),
                initialTextUndoManager =
                    with(TextUndoManager.Companion.Saver) { restore(savedTextUndoManager!!) }!!
            )
        }
    }
}

/**
 * Create and remember a [TextFieldState]. The state is remembered using [rememberSaveable] and so
 * will be saved and restored with the composition.
 *
 * If you need to store a [TextFieldState] in another object, use the [TextFieldState.Saver] object
 * to manually save and restore the state.
 *
 * @param initialText The initial text state. If a different value is passed in a subsequent
 *   recomposition, the value of the state will _not_ be updated. To update the state after it's
 *   initialized, call methods on [TextFieldState].
 * @param initialSelection The initial selection state. If a different value is passed in a
 *   subsequent recomposition, the value of the state will _not_ be updated. To update the state
 *   after it's initialized, call methods on [TextFieldState].
 */
@Composable
fun rememberTextFieldState(
    initialText: String = "",
    initialSelection: TextRange = TextRange(initialText.length)
): TextFieldState =
    rememberSaveable(saver = TextFieldState.Saver) { TextFieldState(initialText, initialSelection) }

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
fun TextFieldState.clearText() {
    edit {
        delete(0, length)
        placeCursorAtEnd()
    }
}

/**
 * Final deciding property for which annotations would be rendered for the composing region. If the
 * IME has not set any composing annotations and the composing region is not collapsed, we need to
 * add the specific underline styling.
 */
@Suppress("ListIterator")
private fun finalizeComposingAnnotations(
    composition: TextRange?,
    annotationList: MutableVector<PlacedAnnotation>?
): List<PlacedAnnotation> =
    when {
        annotationList != null && annotationList.isNotEmpty() -> {
            // it is important to freeze the mutable list into an immutable list because
            // mutable list is sustained inside the EditingBuffer and TextFieldCharSequence
            // must be read-only.
            annotationList.asMutableList().toList()
        }
        composition != null && !composition.collapsed -> {
            listOf(
                AnnotatedString.Range(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    start = composition.min,
                    end = composition.max
                )
            )
        }
        else -> emptyList()
    }

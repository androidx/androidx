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
import androidx.compose.foundation.content.internal.ReceiveContentConfiguration
import androidx.compose.foundation.content.internal.getReceiveContentConfiguration
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.InputTransformation
import androidx.compose.foundation.text2.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text2.input.internal.selection.TextToolbarState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.editable
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onImeAction
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private const val MIMETYPE_TEXT = "text/*"

/**
 * Modifier element for most of the functionality of [BasicTextField2] that is attached to the
 * decoration box. This is only half the actual modifiers for the field, the other half are only
 * attached to the internal text field.
 *
 * This modifier handles input events (both key and pointer), semantics, and focus.
 */
@OptIn(ExperimentalFoundationApi::class)
internal data class TextFieldDecoratorModifier(
    private val textFieldState: TransformedTextFieldState,
    private val textLayoutState: TextLayoutState,
    private val textFieldSelectionState: TextFieldSelectionState,
    private val filter: InputTransformation?,
    private val enabled: Boolean,
    private val readOnly: Boolean,
    private val keyboardOptions: KeyboardOptions,
    private val keyboardActions: KeyboardActions,
    private val singleLine: Boolean,
    private val interactionSource: MutableInteractionSource
) : ModifierNodeElement<TextFieldDecoratorModifierNode>() {
    override fun create(): TextFieldDecoratorModifierNode = TextFieldDecoratorModifierNode(
        textFieldState = textFieldState,
        textLayoutState = textLayoutState,
        textFieldSelectionState = textFieldSelectionState,
        filter = filter,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        interactionSource = interactionSource,
    )

    override fun update(node: TextFieldDecoratorModifierNode) {
        node.updateNode(
            textFieldState = textFieldState,
            textLayoutState = textLayoutState,
            textFieldSelectionState = textFieldSelectionState,
            filter = filter,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            interactionSource = interactionSource,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

/** Modifier node for [TextFieldDecoratorModifier]. */
@OptIn(ExperimentalFoundationApi::class)
internal class TextFieldDecoratorModifierNode(
    var textFieldState: TransformedTextFieldState,
    var textLayoutState: TextLayoutState,
    var textFieldSelectionState: TextFieldSelectionState,
    var filter: InputTransformation?,
    var enabled: Boolean,
    var readOnly: Boolean,
    keyboardOptions: KeyboardOptions,
    var keyboardActions: KeyboardActions,
    var singleLine: Boolean,
    var interactionSource: MutableInteractionSource
) : DelegatingNode(),
    PlatformTextInputModifierNode,
    SemanticsModifierNode,
    FocusRequesterModifierNode,
    FocusEventModifierNode,
    GlobalPositionAwareModifierNode,
    PointerInputModifierNode,
    KeyInputModifierNode,
    CompositionLocalConsumerModifierNode,
    ModifierLocalModifierNode,
    ObserverModifierNode,
    LayoutAwareModifierNode {

    private val editable get() = enabled && !readOnly

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        with(textFieldSelectionState) {
            textFieldGestures(
                requestFocus = {
                    if (!isFocused) requestFocus()
                },
                showKeyboard = {
                    if (inputSessionJob != null) {
                        // just reshow the keyboard in existing session
                        requireKeyboardController().show()
                    } else {
                        startInputSession(fromTap = true)
                    }
                }
            )
        }
    })

    /**
     * The last enter event that was submitted to [interactionSource] from [dragAndDropNode]. We
     * need to keep a reference to this event to send a follow-up exit event.
     *
     * We are using interaction source hover state as a hacky capsule to carry dragging events to
     * core modifier node which draws the cursor and shows the magnifier. TextFields are not
     * really focused when a dragging text hovers over them. Focused TextFields should have active
     * input connections that is not required in a drag and drop scenario.
     *
     * When proper hover events are implemented for [interactionSource], the below code in
     * [dragAndDropNode] should be revised.
     */
    private var dragEnterEvent: HoverInteraction.Enter? = null

    private val dragAndDropNode = delegate(
        textFieldDragAndDropNode(
            acceptedMimeTypes = setOf(MIMETYPE_TEXT),
            onEntered = {
                dragEnterEvent = HoverInteraction.Enter().also {
                    interactionSource.tryEmit(it)
                }
            },
            onMoved = { position ->
                val positionOnTextField = textLayoutState.fromWindowToDecoration(position)
                val cursorPosition = textLayoutState.getOffsetForPosition(positionOnTextField)
                textFieldState.selectCharsIn(TextRange(cursorPosition))
                textFieldSelectionState.updateHandleDragging(Handle.Cursor, positionOnTextField)
            },
            onDrop = {
                emitDragExitEvent()
                textFieldSelectionState.clearHandleDragging()
                textFieldState.replaceSelectedText(it.text)
                true
            },
            onExited = {
                emitDragExitEvent()
                textFieldSelectionState.clearHandleDragging()
            },
            onEnded = {
                emitDragExitEvent()
            })
    )

    var keyboardOptions: KeyboardOptions = keyboardOptions.withDefaultsFrom(filter?.keyboardOptions)
        private set

    /**
     * Needs to be kept separate from a window focus so we can restart an input session when the
     * window receives the focus back. Element can stay focused even if the window loses its focus.
     */
    private var isElementFocused: Boolean = false

    /**
     * Keeps focus state of the window
     */
    private var windowInfo: WindowInfo? = null

    private val isFocused: Boolean get() = isElementFocused && windowInfo?.isWindowFocused == true

    /**
     * Manages key events. These events often are sourced by a hardware keyboard but it's also
     * possible that IME or some other platform system simulates a KeyEvent.
     */
    private val textFieldKeyEventHandler = createTextFieldKeyEventHandler()

    private val keyboardActionScope = object : KeyboardActionScope {
        private val focusManager: FocusManager
            get() = currentValueOf(LocalFocusManager)

        override fun defaultKeyboardAction(imeAction: ImeAction) {
            when (imeAction) {
                ImeAction.Next -> {
                    focusManager.moveFocus(FocusDirection.Next)
                }
                ImeAction.Previous -> {
                    focusManager.moveFocus(FocusDirection.Previous)
                }
                ImeAction.Done -> {
                    requireKeyboardController().hide()
                }
                ImeAction.Go, ImeAction.Search, ImeAction.Send,
                ImeAction.Default, ImeAction.None -> Unit
            }
        }
    }

    private val onImeActionPerformed: (ImeAction) -> Unit = { imeAction ->
        val keyboardAction = when (imeAction) {
            ImeAction.Done -> keyboardActions.onDone
            ImeAction.Go -> keyboardActions.onGo
            ImeAction.Next -> keyboardActions.onNext
            ImeAction.Previous -> keyboardActions.onPrevious
            ImeAction.Search -> keyboardActions.onSearch
            ImeAction.Send -> keyboardActions.onSend
            ImeAction.Default, ImeAction.None -> null
            else -> error("invalid ImeAction")
        }
        keyboardAction?.invoke(keyboardActionScope)
            ?: keyboardActionScope.defaultKeyboardAction(imeAction)
    }

    /**
     * A coroutine job that observes text and layout changes in selection state to react to those
     * changes.
     */
    private var inputSessionJob: Job? = null

    private val receiveContentConfigurationProvider: () -> ReceiveContentConfiguration? = {
        getReceiveContentConfiguration()
    }

    /**
     * Updates all the related properties and invalidates internal state based on the changes.
     */
    fun updateNode(
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        filter: InputTransformation?,
        enabled: Boolean,
        readOnly: Boolean,
        keyboardOptions: KeyboardOptions,
        keyboardActions: KeyboardActions,
        singleLine: Boolean,
        interactionSource: MutableInteractionSource
    ) {
        // Find the diff: current previous and new values before updating current.
        val previousWriteable = this.enabled && !this.readOnly
        val writeable = enabled && !readOnly

        val previousEnabled = this.enabled
        val previousTextFieldState = this.textFieldState
        val previousKeyboardOptions = this.keyboardOptions
        val previousTextFieldSelectionState = this.textFieldSelectionState
        val previousFilter = this.filter

        // Apply the diff.
        this.textFieldState = textFieldState
        this.textLayoutState = textLayoutState
        this.textFieldSelectionState = textFieldSelectionState
        this.filter = filter
        this.enabled = enabled
        this.readOnly = readOnly
        this.keyboardOptions = keyboardOptions.withDefaultsFrom(filter?.keyboardOptions)
        this.keyboardActions = keyboardActions
        this.singleLine = singleLine
        this.interactionSource = interactionSource

        // React to diff.
        // Something about the session changed, restart the session.
        if (writeable != previousWriteable ||
            textFieldState != previousTextFieldState ||
            keyboardOptions != previousKeyboardOptions ||
            filter != previousFilter
        ) {
            if (writeable && isFocused) {
                // The old session will be implicitly disposed.
                startInputSession(fromTap = false)
            } else if (!writeable) {
                // We were made read-only or disabled, hide the keyboard.
                disposeInputSession()
            }
        }

        if (previousEnabled != enabled) {
            invalidateSemantics()
        }

        if (textFieldSelectionState != previousTextFieldSelectionState) {
            pointerInputNode.resetPointerInputHandler()
            if (isAttached) {
                textFieldSelectionState.receiveContentConfiguration =
                    receiveContentConfigurationProvider
            }
        }
    }

    override val shouldMergeDescendantSemantics: Boolean
        get() = true

    // This function is called inside a snapshot observer.
    override fun SemanticsPropertyReceiver.applySemantics() {
        val text = textFieldState.outputText
        val selection = text.selectionInChars
        editableText = AnnotatedString(text.toString())
        textSelectionRange = selection

        if (!enabled) disabled()
        if (editable) editable()

        getTextLayoutResult {
            textLayoutState.layoutResult?.let { result -> it.add(result) } ?: false
        }
        setText { newText ->
            if (!editable) return@setText false

            textFieldState.replaceAll(newText)
            true
        }
        @Suppress("NAME_SHADOWING")
        setSelection { start, end, relativeToOriginal ->
            // in traversal mode (relativeToOriginal=true) we get selection from the
            // `textSelectionRange` semantics which is selection in original text. In non-traversal
            // mode selection comes from the Talkback and indices are relative to the transformed
            // text
            val text = if (relativeToOriginal) {
                textFieldState.untransformedText
            } else {
                textFieldState.visualText
            }
            val selection = text.selectionInChars

            if (!enabled ||
                minOf(start, end) < 0 ||
                maxOf(start, end) > text.length
            ) {
                return@setSelection false
            }

            // Selection is already selected, don't need to do any work.
            if (start == selection.start && end == selection.end) {
                return@setSelection true
            }

            val selectionRange = TextRange(start, end)
            // Do not show toolbar if it's a traversal mode (with the volume keys), or if the
            // selection is collapsed.
            if (relativeToOriginal || start == end) {
                textFieldSelectionState.updateTextToolbarState(TextToolbarState.None)
            } else {
                textFieldSelectionState.updateTextToolbarState(TextToolbarState.Selection)
            }
            if (relativeToOriginal) {
                textFieldState.selectUntransformedCharsIn(selectionRange)
            } else {
                textFieldState.selectCharsIn(selectionRange)
            }
            return@setSelection true
        }
        insertTextAtCursor { newText ->
            if (!editable) return@insertTextAtCursor false

            // Finish composing text first because when the field is focused the IME
            // might set composition.
            textFieldState.replaceSelectedText(newText, clearComposition = true)
            true
        }
        onImeAction(keyboardOptions.imeAction) {
            onImeActionPerformed(keyboardOptions.imeAction)
            true
        }
        onClick {
            // according to the documentation, we still need to provide proper semantics actions
            // even if the state is 'disabled'
            if (!isFocused) {
                requestFocus()
            } else if (!readOnly) {
                requireKeyboardController().show()
            }
            true
        }
        onLongClick {
            if (!isFocused) {
                requestFocus()
            }
            textFieldSelectionState.updateTextToolbarState(TextToolbarState.Selection)
            true
        }
        if (!selection.collapsed) {
            copyText {
                textFieldSelectionState.copy()
                true
            }
            if (enabled && !readOnly) {
                cutText {
                    textFieldSelectionState.cut()
                    true
                }
            }
        }
        if (editable) {
            pasteText {
                textFieldSelectionState.paste()
                true
            }
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (isElementFocused == focusState.isFocused) {
            return
        }
        isElementFocused = focusState.isFocused
        textFieldSelectionState.isFocused = this.isFocused

        if (focusState.isFocused) {
            // Deselect when losing focus even if readonly.
            if (editable) {
                startInputSession(fromTap = false)
            }
        } else {
            disposeInputSession()
            textFieldState.collapseSelectionToMax()
        }
    }

    override fun onAttach() {
        onObservedReadsChanged()
        textFieldSelectionState.receiveContentConfiguration = receiveContentConfigurationProvider
    }

    override fun onDetach() {
        disposeInputSession()
        textFieldSelectionState.receiveContentConfiguration = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        textLayoutState.decoratorNodeCoordinates = coordinates
    }

    override fun onRemeasured(size: IntSize) {
        if (!isFocused) return

        // Ensure that the cursor is kept in view if the decoration box is resized while focused.
        // This handles the case where a multi-line text field sitting right above the keyboard
        // grows due to a newline entered while typing, which isn't handled by the cursor moving yet
        // because the resize happens after the text state change, and the resize moves the cursor
        // under the keyboard. This also covers the case where the field shrinks while focused.
        val selection = textFieldState.visualText.selectionInChars
        if (selection.collapsed) {
            coroutineScope.launch {
                textLayoutState.bringCursorIntoView(cursorIndex = selection.start)
            }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        pointerInputNode.onCancelPointerInput()
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean {
        return textFieldKeyEventHandler.onPreKeyEvent(
            event = event,
            textFieldState = textFieldState,
            textFieldSelectionState = textFieldSelectionState,
            focusManager = currentValueOf(LocalFocusManager),
            keyboardController = requireKeyboardController()
        )
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return textFieldKeyEventHandler.onKeyEvent(
            event = event,
            textFieldState = textFieldState,
            textLayoutState = textLayoutState,
            textFieldSelectionState = textFieldSelectionState,
            editable = enabled && !readOnly,
            singleLine = singleLine,
            onSubmit = { onImeActionPerformed(keyboardOptions.imeAction) }
        )
    }

    override fun onObservedReadsChanged() {
        observeReads {
            windowInfo = currentValueOf(LocalWindowInfo)
            startOrDisposeInputSessionOnWindowFocusChange()
        }
    }

    private fun startInputSession(fromTap: Boolean) {
        if (!fromTap && !keyboardOptions.shouldShowKeyboardOnFocus) return

        val receiveContentConfiguration = getReceiveContentConfiguration()

        inputSessionJob = coroutineScope.launch {
            // This will automatically cancel the previous session, if any, so we don't need to
            // cancel the inputSessionJob ourselves.
            establishTextInputSession {
                // Re-start observing changes in case our TextFieldState instance changed.
                launch(start = CoroutineStart.UNDISPATCHED) {
                    textFieldSelectionState.observeChanges()
                }

                launch {
                    keepSelectionInView()
                }

                platformSpecificTextInputSession(
                    state = textFieldState,
                    layoutState = textLayoutState,
                    imeOptions = keyboardOptions.toImeOptions(singleLine),
                    receiveContentConfiguration = receiveContentConfiguration,
                    onImeAction = onImeActionPerformed
                )
            }
        }
    }

    private fun disposeInputSession() {
        inputSessionJob?.cancel()
        inputSessionJob = null
    }

    /**
     * Calls bringCursorIntoView when the cursor position changes. This handles the case where the user
     * types while the cursor is scrolled out of view, as well as any programmatic changes to the
     * cursor while focused.
     *
     * This function suspends indefinitely, should only be called when the field is focused, and
     * cancelled when the field loses focus.
     */
    private suspend fun keepSelectionInView() {
        snapshotFlow { textFieldState.visualText.selectionInChars }
            .filter { it.collapsed }
            .collectLatest {
                textLayoutState.bringCursorIntoView(cursorIndex = it.start)
            }
    }

    private fun startOrDisposeInputSessionOnWindowFocusChange() {
        if (windowInfo == null) return
        if (windowInfo?.isWindowFocused == true && isElementFocused) {
            startInputSession(fromTap = false)
        } else {
            disposeInputSession()
        }
    }

    private fun requireKeyboardController(): SoftwareKeyboardController =
        currentValueOf(LocalSoftwareKeyboardController)
            ?: error("No software keyboard controller")

    private fun emitDragExitEvent() {
        dragEnterEvent?.let {
            interactionSource.tryEmit(HoverInteraction.Exit(it))
            dragEnterEvent = null
        }
    }
}

/**
 * Runs platform-specific text input logic.
 */
internal expect suspend fun PlatformTextInputSession.platformSpecificTextInputSession(
    state: TransformedTextFieldState,
    layoutState: TextLayoutState,
    imeOptions: ImeOptions,
    receiveContentConfiguration: ReceiveContentConfiguration?,
    onImeAction: ((ImeAction) -> Unit)?
): Nothing

/**
 * Returns a [KeyboardOptions] that is merged with [defaults], with this object's values taking
 * precedence.
 */
// TODO(b/295951492) KeyboardOptions can't actually be merged correctly in all cases, because its
//  properties don't all have proper "unspecified" values. I think we can fix that in a
//  backwards-compatible way, but it will require adding new API outside of the text2 package so we
//  should hold off on making them until after the study.
internal fun KeyboardOptions.withDefaultsFrom(defaults: KeyboardOptions?): KeyboardOptions {
    if (defaults == null) return this
    return KeyboardOptions(
        capitalization = if (this.capitalization != KeyboardCapitalization.None) {
            this.capitalization
        } else {
            defaults.capitalization
        },
        autoCorrect = this.autoCorrect && defaults.autoCorrect,
        keyboardType = if (this.keyboardType != KeyboardType.Text) {
            this.keyboardType
        } else {
            defaults.keyboardType
        },
        imeAction = if (this.imeAction != ImeAction.Default) {
            this.imeAction
        } else {
            defaults.imeAction
        }
    )
}

/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.foundation.text.LegacyTextFieldState
import androidx.compose.foundation.text.TextFieldDelegate
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.tapToFocus
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requestAutofill
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDataType
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.inputText
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.onAutofillText
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onImeAction
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteAllCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText

internal data class CoreTextFieldSemanticsModifier(
    val transformedText: TransformedText,
    val value: TextFieldValue,
    val state: LegacyTextFieldState,
    val readOnly: Boolean,
    val enabled: Boolean,
    val isPassword: Boolean,
    val offsetMapping: OffsetMapping,
    val manager: TextFieldSelectionManager,
    val imeOptions: ImeOptions,
    val focusRequester: FocusRequester,
) : ModifierNodeElement<CoreTextFieldSemanticsModifierNode>() {
    override fun create(): CoreTextFieldSemanticsModifierNode =
        CoreTextFieldSemanticsModifierNode(
            transformedText = transformedText,
            value = value,
            state = state,
            readOnly = readOnly,
            enabled = enabled,
            isPassword = isPassword,
            offsetMapping = offsetMapping,
            manager = manager,
            imeOptions = imeOptions,
            focusRequester = focusRequester,
        )

    override fun update(node: CoreTextFieldSemanticsModifierNode) {
        node.updateNodeSemantics(
            transformedText = transformedText,
            value = value,
            state = state,
            readOnly = readOnly,
            enabled = enabled,
            isPassword = isPassword,
            offsetMapping = offsetMapping,
            manager = manager,
            imeOptions = imeOptions,
            focusRequester = focusRequester,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class CoreTextFieldSemanticsModifierNode(
    var transformedText: TransformedText,
    var value: TextFieldValue,
    var state: LegacyTextFieldState,
    var readOnly: Boolean,
    var enabled: Boolean,
    var isPassword: Boolean,
    var offsetMapping: OffsetMapping,
    var manager: TextFieldSelectionManager,
    var imeOptions: ImeOptions,
    var focusRequester: FocusRequester,
) : DelegatingNode(), SemanticsModifierNode {
    init {
        manager.requestAutofillAction = { requestAutofill() }
    }

    override val shouldMergeDescendantSemantics: Boolean
        get() = true

    override fun SemanticsPropertyReceiver.applySemantics() {
        this.inputText = value.annotatedString
        this.editableText = transformedText.text
        this.textSelectionRange = value.selection

        // The developer will set `contentType`. CTF populates the other autofill-related
        // semantics. And since we're in a TextField, set the `contentDataType` to be "Text".
        this.contentDataType = ContentDataType.Text
        onAutofillText { text ->
            state.justAutofilled = true
            state.autofillHighlightOn = true
            handleTextUpdateFromSemantics(state, text.text, readOnly, enabled)
            true
        }

        if (!enabled) this.disabled()
        if (isPassword) this.password()
        val editable = enabled && !readOnly
        isEditable = editable
        getTextLayoutResult {
            if (state.layoutResult != null) {
                it.add(state.layoutResult!!.value)
                true
            } else {
                false
            }
        }

        if (editable) {
            setText { text ->
                handleTextUpdateFromSemantics(state, text.text, readOnly, enabled)
                true
            }

            insertTextAtCursor { text ->
                if (readOnly || !enabled) return@insertTextAtCursor false

                // If the action is performed while in an active text editing session, treat
                // this like an IME command and update the text by going through the buffer.
                // This keeps the buffer state consistent if other IME commands are performed
                // before the next recomposition, and is used for the testing code path.
                state.inputSession?.let { session ->
                    TextFieldDelegate.onEditCommand(
                        // Finish composing text first because when the field is focused the IME
                        // might
                        // set composition.
                        ops = listOf(FinishComposingTextCommand(), CommitTextCommand(text, 1)),
                        editProcessor = state.processor,
                        state.onValueChange,
                        session,
                    )
                }
                    ?: run {
                        val newText =
                            value.text.replaceRange(
                                value.selection.start,
                                value.selection.end,
                                text,
                            )
                        val newCursor = TextRange(value.selection.start + text.length)
                        state.onValueChange(TextFieldValue(newText, newCursor))
                    }
                true
            }
        }

        setSelection { selectionStart, selectionEnd, relativeToOriginalText ->
            // in traversal mode we get selection from the `textSelectionRange` semantics which
            // is selection in original text. In non-traversal mode selection comes from the
            // Talkback and indices are relative to the transformed text
            val start =
                if (relativeToOriginalText) {
                    selectionStart
                } else {
                    offsetMapping.transformedToOriginal(selectionStart)
                }
            val end =
                if (relativeToOriginalText) {
                    selectionEnd
                } else {
                    offsetMapping.transformedToOriginal(selectionEnd)
                }

            if (!enabled) {
                false
            } else if (start == value.selection.start && end == value.selection.end) {
                false
            } else if (
                minOf(start, end) >= 0 && maxOf(start, end) <= value.annotatedString.length
            ) {
                // Do not show toolbar if it's a traversal mode (with the volume keys), or
                // if the cursor just moved to beginning or end.
                if (relativeToOriginalText || start == end) {
                    manager.exitSelectionMode()
                } else {
                    manager.enterSelectionMode()
                }
                state.onValueChange(TextFieldValue(value.annotatedString, TextRange(start, end)))
                true
            } else {
                manager.exitSelectionMode()
                false
            }
        }
        onImeAction(imeOptions.imeAction) {
            // This will perform the appropriate default action if no handler has been
            // specified, so
            // as far as the platform is concerned, we always handle the action and never want
            // to
            // defer to the default _platform_ implementation.
            state.onImeActionPerformed(imeOptions.imeAction)
            true
        }
        onClick {
            // according to the documentation, we still need to provide proper semantics actions
            // even if the state is 'disabled'
            tapToFocus(state, focusRequester, !readOnly)
            true
        }
        onLongClick {
            manager.enterSelectionMode()
            true
        }
        if (!value.selection.collapsed && !isPassword) {
            copyText {
                manager.copy()
                true
            }
            if (enabled && !readOnly) {
                cutText {
                    manager.cut()
                    true
                }
            }
        }
        if (enabled && !readOnly) {
            pasteText {
                manager.paste()
                true
            }
        }
    }

    fun updateNodeSemantics(
        transformedText: TransformedText,
        value: TextFieldValue,
        state: LegacyTextFieldState,
        readOnly: Boolean,
        enabled: Boolean,
        isPassword: Boolean,
        offsetMapping: OffsetMapping,
        manager: TextFieldSelectionManager,
        imeOptions: ImeOptions,
        focusRequester: FocusRequester,
    ) {
        // Find the diff: current previous and new values before updating current.
        val previousEditable = this.enabled && !this.readOnly
        val previousEnabled = this.enabled
        val previousIsPassword = this.isPassword
        val previousImeOptions = this.imeOptions
        val previousManager = this.manager
        val editable = enabled && !readOnly

        // Apply the diff.
        this.transformedText = transformedText
        this.value = value
        this.state = state
        this.readOnly = readOnly
        this.enabled = enabled
        this.offsetMapping = offsetMapping
        this.manager = manager
        this.imeOptions = imeOptions
        this.focusRequester = focusRequester

        if (
            enabled != previousEnabled ||
                editable != previousEditable ||
                imeOptions != previousImeOptions ||
                isPassword != previousIsPassword ||
                !value.selection.collapsed
        ) {
            invalidateSemantics()
        }

        if (manager != previousManager) {
            manager.requestAutofillAction = { requestAutofill() }
        }
    }

    /**
     * In an active input session, semantics updates are handled just as user updates coming from
     * the IME. Otherwise the updates are directly applied on the current state.
     */
    private fun handleTextUpdateFromSemantics(
        state: LegacyTextFieldState,
        text: String,
        readOnly: Boolean,
        enabled: Boolean,
    ) {
        if (readOnly || !enabled) return

        // If the action is performed while in an active text editing session, treat this
        // like an IME command and update the text by going through the buffer.
        state.inputSession?.let { session ->
            TextFieldDelegate.onEditCommand(
                ops = listOf(DeleteAllCommand(), CommitTextCommand(text, 1)),
                editProcessor = state.processor,
                state.onValueChange,
                session,
            )
        } ?: run { state.onValueChange(TextFieldValue(text, TextRange(text.length))) }
    }
}

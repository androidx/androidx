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
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Synchronizes between [TextFieldState], immutable values, and value change callbacks for
 * [BasicTextField2] overloads that take a value+callback for state instead of taking a
 * [TextFieldState] directly. Effectively a fancy `rememberUpdatedState`.
 *
 * Only intended for use from [BasicTextField2].
 *
 * @param writeSelectionFromTextFieldValue If true, [update] will synchronize the selection from the
 * [TextFieldValue] to the [TextFieldState]. The text will be synchronized regardless.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.syncTextFieldState(
    state: TextFieldState,
    value: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    writeSelectionFromTextFieldValue: Boolean,
): Modifier = this.then(
    StateSyncingModifier(
        state = state,
        value = value,
        onValueChanged = onValueChanged,
        writeSelectionFromTextFieldValue = writeSelectionFromTextFieldValue
    )
)

@OptIn(ExperimentalFoundationApi::class)
private class StateSyncingModifier(
    private val state: TextFieldState,
    private val value: TextFieldValue,
    private val onValueChanged: (TextFieldValue) -> Unit,
    private val writeSelectionFromTextFieldValue: Boolean,
) : ModifierNodeElement<StateSyncingModifierNode>() {

    override fun create(): StateSyncingModifierNode =
        StateSyncingModifierNode(state, onValueChanged, writeSelectionFromTextFieldValue)

    override fun update(node: StateSyncingModifierNode) {
        node.update(value, onValueChanged)
    }

    override fun equals(other: Any?): Boolean {
        // Always call update, without comparing the text. Update can compare more efficiently.
        return false
    }

    override fun hashCode(): Int {
        // Avoid calculating hash from values that can change on every recomposition.
        return state.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector properties
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class StateSyncingModifierNode(
    private val state: TextFieldState,
    private var onValueChanged: (TextFieldValue) -> Unit,
    private val writeSelectionFromTextFieldValue: Boolean,
) : Modifier.Node(), ObserverModifierNode, FocusEventModifierNode {

    private var isFocused = false
    private var lastValueWhileFocused: TextFieldValue? = null

    override val shouldAutoInvalidate: Boolean
        get() = false

    /**
     * Synchronizes the latest [value] to the [TextFieldState] and updates our [onValueChanged]
     * callback. Should be called from [ModifierNodeElement.update].
     */
    fun update(value: TextFieldValue, onValueChanged: (TextFieldValue) -> Unit) {
        this.onValueChanged = onValueChanged

        // Don't modify the text programmatically while an edit session is in progress.
        // WARNING: While editing, the code that holds the external state is temporarily not the
        // actual source of truth. This "stealing" of control is generally an anti-pattern. We do it
        // intentionally here because text field state is very sensitive to timing, and if a state
        // update is delivered a frame late, it breaks text input. It is very easy to accidentally
        // introduce small bits of asynchrony in real-world scenarios, e.g. with Flow-based reactive
        // architectures. The benefit of avoiding that easy pitfall outweighs the weirdness in this
        // case.
        if (!isFocused) {
            updateState(value)
        } else {
            this.lastValueWhileFocused = value
        }
    }

    override fun onAttach() {
        // Don't fire the callback on first frame.
        observeTextState(fireOnValueChanged = false)
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (this.isFocused && !focusState.isFocused) {
            // Lost focus, perform deferred synchronization.
            lastValueWhileFocused?.let(::updateState)
            lastValueWhileFocused = null
        }
        this.isFocused = focusState.isFocused
    }

    /** Called by the modifier system when the [TextFieldState] has changed. */
    override fun onObservedReadsChanged() {
        observeTextState()
    }

    private fun updateState(value: TextFieldValue) {
        state.edit {
            // Avoid registering a state change if the text isn't actually different.
            setTextIfChanged(value.text)

            // The BasicTextField2(String) variant can't push a selection value, so ignore it.
            if (writeSelectionFromTextFieldValue) {
                selectCharsIn(value.selection)
            }
        }
    }

    private fun observeTextState(fireOnValueChanged: Boolean = true) {
        lateinit var text: TextFieldCharSequence
        observeReads {
            text = state.text
        }

        // This code is outside of the observeReads lambda so we don't observe any state reads the
        // callback happens to do.
        if (fireOnValueChanged) {
            val newValue = TextFieldValue(
                text = text.toString(),
                selection = text.selectionInChars,
                composition = text.compositionInChars
            )
            onValueChanged(newValue)
        }
    }
}

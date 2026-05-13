/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull

/**
 * Selection state object for a particular [SelectionContainer]. It is invalid for the same
 * SelectionState to be passed to multiple SelectionContainers. Holds the currently selected text
 * and functions to perform Selection actions programmatically.
 *
 * When instantiating this class from a composable, use [rememberSelectionState] to automatically
 * save and restore the selection state. For more advanced use cases, pass [SelectionState.Saver] to
 * [rememberSaveable].
 *
 * @sample androidx.compose.foundation.samples.SelectionStateSample
 */
class SelectionState {
    /** Current [Selection] for this SelectionState. */
    internal var selection: Selection? by mutableStateOf(null)

    /** [SelectionManager] for this SelectionState. */
    internal var manager: SelectionManager? = null
        set(value) {
            if (value != null && field != null && field !== value) {
                error(
                    "A SelectionState can only be bound to one SelectionContainer. " +
                        "Please use rememberSelectionState() to create a unique state for each container."
                )
            }
            field = value
        }

    /**
     * Current selected texts in the [SelectionContainer] corresponding to this SelectionState. Each
     * text composable within the Selection is returned as an AnnotatedString in the list. This
     * field is backed by [androidx.compose.runtime.mutableStateOf]
     */
    private var _selectedTexts by mutableStateOf<List<AnnotatedString>>(emptyList())

    /**
     * The currently selected texts in the [SelectionContainer] corresponding to this
     * SelectionState. Each text composable within the Selection is returned as an AnnotatedString
     * in the list. This field is backed by [androidx.compose.runtime.mutableStateOf] so it can be
     * observed by Composables.
     */
    val selectedTexts: List<AnnotatedString>
        get() = _selectedTexts

    /** Updates selectedTexts to reflect new selected texts. */
    internal fun updateSelectedTexts(newTexts: List<AnnotatedString>) {
        _selectedTexts = newTexts
    }

    /**
     * Sets the current selection to include every selectable Text Composable within the
     * [SelectionContainer].
     *
     * Note: When using [SelectionContainer] with [androidx.compose.foundation.lazy.LazyList],
     * selectAll will only select all selectable Text Composables that are composed.
     *
     * @sample androidx.compose.foundation.samples.SelectAllSample
     */
    fun selectAll() {
        manager?.selectAll()
    }

    /** Clears the current selection for the [SelectionContainer] and removes selection handles. */
    fun clear() {
        manager?.onRelease()
    }

    /**
     * Extends the current selection to the next word boundary, if there is another word boundary
     * available within the [SelectionContainer]. The next word is added at the active edge of the
     * selection. So if the selection is dragged in reverse, this adds a word earlier in the text.
     *
     * If the next word in the selection is in a different Text composable, this extends to the next
     * Text. If there is no selection, this selects the first word in the [SelectionContainer].
     *
     * Word boundaries separate words and non-words such as spaces, symbols, and punctuation. Word
     * boundaries are defined more precisely in Unicode Standard Annex #29
     * <https://www.unicode.org/reports/tr29/#Word_Boundaries>.
     *
     * @sample androidx.compose.foundation.samples.ExtendSelectionSample
     */
    fun extendSelectionByWord() {
        manager?.extendSelectionByWord()
    }

    /**
     * Returns a list of all selectable texts within the [SelectionContainer] in visual/layout
     * order. This returns the entire text content, not the selected or unselected portions. This
     * creates a list of text from all selectable texts in the SelectionContainer, so it is not
     * recommended for containers holding a large volume of text composables.
     *
     * Note: This should not be called during composition as it requires the layout to be ready.
     *
     * @sample androidx.compose.foundation.samples.SelectQuerySample
     * @sample androidx.compose.foundation.samples.SelectThirdTextSample
     */
    fun getSelectableTexts(): List<AnnotatedString> = manager?.getSelectableTexts().orEmpty()

    /**
     * Sets the selection to the specified [TextRange] within the global space of all Texts inside
     * the [SelectionContainer]. This function loops through all selectables in the
     * SelectionContainer, so it is not advised to call within a loop or as an effect that triggers
     * frequently.
     *
     * Note: This should not be called during composition as it requires the layout to be ready.
     *
     * @sample androidx.compose.foundation.samples.SelectQuerySample
     * @sample androidx.compose.foundation.samples.SelectThirdTextSample
     */
    fun select(range: TextRange) {
        manager?.setSelection(range)
    }

    /**
     * Saves and restores a [SelectionState] for [rememberSaveable].
     *
     * @see rememberSelectionState
     */
    companion object {
        @Suppress("UNCHECKED_CAST")
        val Saver: Saver<SelectionState, Any> =
            listSaver(
                save = { state ->
                    val savedTexts =
                        state.selectedTexts.fastMap { text ->
                            with(AnnotatedString.Saver) { save(text) }
                        }

                    listOf(
                        savedTexts,
                        state.selection?.start?.offset,
                        state.selection?.start?.selectableId,
                        state.selection?.start?.direction?.name,
                        state.selection?.end?.offset,
                        state.selection?.end?.selectableId,
                        state.selection?.end?.direction?.name,
                        state.selection?.handlesCrossed,
                    )
                },
                restore = { savedList ->
                    val state = SelectionState()

                    val savedTexts = savedList[0] as List<Any>

                    val annotatedStringSaver = AnnotatedString.Saver as Saver<AnnotatedString, Any>
                    val selectedTexts =
                        savedTexts.fastMapNotNull { savedText ->
                            with(annotatedStringSaver) { restore(savedText) }
                        }
                    state.updateSelectedTexts(selectedTexts)

                    val startOffset = savedList[1] as? Int
                    if (startOffset != null) {
                        val startSelectableId = savedList[2] as Long
                        val startDirectionName = savedList[3] as String
                        val endOffset = savedList[4] as Int
                        val endSelectableId = savedList[5] as Long
                        val endDirectionName = savedList[6] as String
                        val handlesCrossed = savedList[7] as Boolean

                        val startDirection = ResolvedTextDirection.valueOf(startDirectionName)
                        val endDirection = ResolvedTextDirection.valueOf(endDirectionName)

                        state.selection =
                            Selection(
                                start =
                                    Selection.AnchorInfo(
                                        direction = startDirection,
                                        offset = startOffset,
                                        selectableId = startSelectableId,
                                    ),
                                end =
                                    Selection.AnchorInfo(
                                        direction = endDirection,
                                        offset = endOffset,
                                        selectableId = endSelectableId,
                                    ),
                                handlesCrossed = handlesCrossed,
                            )
                    }
                    state
                },
            )
    }
}

/**
 * Create and remember a [SelectionState]. The state is remembered using [rememberSaveable] and will
 * be saved and restored with the composition.
 *
 * If you need to store a [SelectionState] in another object, use the [SelectionState.Saver] object
 * to manually save and restore the state.
 */
@Composable
fun rememberSelectionState(): SelectionState {
    return rememberSaveable(saver = SelectionState.Saver) { SelectionState() }
}

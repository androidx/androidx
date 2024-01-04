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
import androidx.compose.foundation.text2.input.CodepointTransformation
import androidx.compose.foundation.text2.input.InputTransformation
import androidx.compose.foundation.text2.input.TextFieldCharSequence
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.internal.undo.TextFieldEditUndoBehavior
import androidx.compose.foundation.text2.input.toVisualText
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A mutable view of a [TextFieldState] where the text and selection values are transformed by a
 * [CodepointTransformation].
 *
 * [text] returns the transformed text, with selection and composition mapped to the corresponding
 * offsets from the untransformed text. The transformed text is cached in a
 * [derived state][derivedStateOf] and only recalculated when the [TextFieldState] changes or some
 * state read by the [CodepointTransformation] changes.
 *
 * This class defines methods for various operations that can be performed on the underlying
 * [TextFieldState]. When possible, these methods should be used instead of editing the state
 * directly, since this class ensures the correct offset mappings are used. If an operation is too
 * complex to warrant a method here, use [editUntransformedTextAsUser] but be careful to make sure
 * any offsets are mapped correctly.
 *
 * To map offsets from transformed to untransformed text or back, use the [mapFromTransformed] and
 * [mapToTransformed] methods.
 *
 * All operations call [TextFieldState.editAsUser] internally and pass [inputTransformation].
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
internal class TransformedTextFieldState(
    private val textFieldState: TextFieldState,
    private val inputTransformation: InputTransformation?,
    private val codepointTransformation: CodepointTransformation?,
) {
    private val transformedText: State<TransformedText?>? =
        // Don't allocate a derived state object if we don't need it, they're expensive.
        codepointTransformation?.let { transformation ->
            derivedStateOf {
                // text is a state read. transformation may also perform state reads when ran.
                calculateTransformedText(textFieldState.text, transformation)
            }
        }

    /**
     * The text that should be presented to the user in most cases. Ifa  [CodepointTransformation]
     * is specified, this text has the transformation applied. If there's no transformation,
     * this will be the same as [untransformedText].
     */
    val text: TextFieldCharSequence
        get() = transformedText?.value?.text ?: textFieldState.text

    /**
     * The raw text in the underlying [TextFieldState]. This text does not have any
     * [CodepointTransformation] applied.
     */
    val untransformedText: TextFieldCharSequence
        get() = textFieldState.text

    fun placeCursorBeforeCharAt(transformedOffset: Int) {
        selectCharsIn(TextRange(transformedOffset))
    }

    fun selectCharsIn(transformedRange: TextRange) {
        val untransformedRange = mapFromTransformed(transformedRange)
        selectUntransformedCharsIn(untransformedRange)
    }

    fun selectUntransformedCharsIn(untransformedRange: TextRange) {
        textFieldState.editAsUser(inputTransformation) {
            setSelection(untransformedRange.start, untransformedRange.end)
        }
    }

    fun replaceAll(newText: CharSequence) {
        textFieldState.editAsUser(inputTransformation) {
            deleteAll()
            commitText(newText.toString(), 1)
        }
    }

    fun selectAll() {
        textFieldState.editAsUser(inputTransformation) {
            setSelection(0, length)
        }
    }

    fun deleteSelectedText() {
        textFieldState.editAsUser(
            inputTransformation,
            undoBehavior = TextFieldEditUndoBehavior.NeverMerge
        ) {
            // `selection` is read from the buffer, so we don't need to transform it.
            delete(selection.min, selection.max)
            setSelection(selection.min, selection.min)
        }
    }

    fun replaceSelectedText(
        newText: CharSequence,
        clearComposition: Boolean = false,
        undoBehavior: TextFieldEditUndoBehavior = TextFieldEditUndoBehavior.MergeIfPossible
    ) {
        textFieldState.editAsUser(inputTransformation, undoBehavior = undoBehavior) {
            if (clearComposition) {
                commitComposition()
            }

            // `selection` is read from the buffer, so we don't need to transform it.
            val selection = selection
            replace(
                selection.min,
                selection.max,
                newText
            )
            val cursor = selection.min + newText.length
            setSelection(cursor, cursor)
        }
    }

    fun collapseSelectionToMax() {
        textFieldState.editAsUser(inputTransformation) {
            // `selection` is read from the buffer, so we don't need to transform it.
            setSelection(selection.max, selection.max)
        }
    }

    fun collapseSelectionToEnd() {
        textFieldState.editAsUser(inputTransformation) {
            // `selection` is read from the buffer, so we don't need to transform it.
            setSelection(selection.end, selection.end)
        }
    }

    fun undo() {
        textFieldState.undoState.undo()
    }

    fun redo() {
        textFieldState.undoState.redo()
    }

    /**
     * Runs [block] with a buffer that contains the source untransformed text. This is the text that
     * will be fed into the [codepointTransformation]. Any operations performed on this buffer MUST
     * take care to explicitly convert between transformed and untransformed offsets and ranges.
     * When possible, use the other methods on this class to manipulate selection to avoid having
     * to do these conversions manually.
     *
     * @see mapToTransformed
     * @see mapFromTransformed
     */
    inline fun editUntransformedTextAsUser(
        notifyImeOfChanges: Boolean = true,
        block: EditingBuffer.() -> Unit
    ) {
        textFieldState.editAsUser(
            inputTransformation = inputTransformation,
            notifyImeOfChanges = notifyImeOfChanges,
            block = block
        )
    }

    /**
     * Maps an [offset] in the untransformed text to the corresponding offset or range in [text].
     *
     * An untransformed offset will map to non-collapsed range if the offset is in the middle of
     * a surrogate pair in the untransformed text, in which case it will return the range of the
     * codepoint that the surrogate maps to. Offsets on either side of a surrogate pair will return
     * collapsed ranges.
     *
     * If there is no transformation, or the transformation does not change the text, a collapsed
     * range of [offset] will be returned.
     *
     * @see mapFromTransformed
     */
    fun mapToTransformed(offset: Int): TextRange {
        val mapping = transformedText?.value?.offsetMapping ?: return TextRange(offset)
        return mapping.mapFromSource(offset)
    }

    /**
     * Maps a [range] in the untransformed text to the corresponding range in [text].
     *
     * If there is no transformation, or the transformation does not change the text, [range]
     * will be returned.
     *
     * @see mapFromTransformed
     */
    fun mapToTransformed(range: TextRange): TextRange {
        val mapping = transformedText?.value?.offsetMapping ?: return range
        return mapToTransformed(range, mapping)
    }

    /**
     * Maps an [offset] in [text] to the corresponding offset in the untransformed text.
     *
     * Multiple transformed offsets may map to the same untransformed offset. In particular, any
     * offset in the middle of a surrogate pair will map to offset of the corresponding codepoint
     * in the untransformed text.
     *
     * If there is no transformation, or the transformation does not change the text, [offset]
     * will be returned.
     *
     * @see mapToTransformed
     */
    fun mapFromTransformed(offset: Int): Int {
        val mapping = transformedText?.value?.offsetMapping ?: return offset
        return mapping.mapFromDest(offset).min
    }

    /**
     * Maps a [range] in [text] to the corresponding range in the untransformed text.
     *
     * If there is no transformation, or the transformation does not change the text, [range]
     * will be returned.
     *
     * @see mapToTransformed
     */
    fun mapFromTransformed(range: TextRange): TextRange {
        val mapping = transformedText?.value?.offsetMapping ?: return range
        return mapFromTransformed(range, mapping)
    }

    // TODO(b/296583846) Get rid of this.
    /**
     * Adds [notifyImeListener] to the underlying [TextFieldState] and then suspends until
     * cancelled, removing the listener before continuing.
     */
    suspend fun collectImeNotifications(
        notifyImeListener: TextFieldState.NotifyImeListener
    ): Nothing {
        suspendCancellableCoroutine<Nothing> { continuation ->
            textFieldState.addNotifyImeListener(notifyImeListener)
            continuation.invokeOnCancellation {
                textFieldState.removeNotifyImeListener(notifyImeListener)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformedTextFieldState) return false
        if (textFieldState != other.textFieldState) return false
        return codepointTransformation == other.codepointTransformation
    }

    override fun hashCode(): Int {
        var result = textFieldState.hashCode()
        result = 31 * result + (codepointTransformation?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "TransformedTextFieldState(" +
        "textFieldState=$textFieldState, " +
        "codepointTransformation=$codepointTransformation, " +
        "transformedText=$transformedText, " +
        "text=\"$text\"" +
        ")"

    private data class TransformedText(
        val text: TextFieldCharSequence,
        val offsetMapping: OffsetMappingCalculator,
    )

    private companion object {

        /**
         * Applies a [CodepointTransformation] to a [TextFieldCharSequence], returning the
         * transformed text content, the selection/cursor from the [untransformedText] mapped to the
         * offsets in the transformed text, and an [OffsetMappingCalculator] that can be used to map
         * offsets in both directions between the transformed and untransformed text.
         *
         * This function is relatively expensive, since it creates a copy of [untransformedText], so
         * its result should be cached.
         */
        @kotlin.jvm.JvmStatic
        private fun calculateTransformedText(
            untransformedText: TextFieldCharSequence,
            codepointTransformation: CodepointTransformation
        ): TransformedText? {
            val offsetMappingCalculator = OffsetMappingCalculator()

            // This is the call to external code. Returns same instance if no codepoints change.
            val transformedText =
                untransformedText.toVisualText(codepointTransformation, offsetMappingCalculator)

            // Avoid allocations + mapping if there weren't actually any transformations.
            if (transformedText === untransformedText) {
                return null
            }

            val transformedTextWithSelection = TextFieldCharSequence(
                text = transformedText,
                // Pass the calculator explicitly since the one on transformedText won't be updated
                // yet.
                selection = mapToTransformed(
                    untransformedText.selectionInChars,
                    offsetMappingCalculator
                ),
                composition = untransformedText.compositionInChars?.let {
                    mapToTransformed(it, offsetMappingCalculator)
                }
            )
            return TransformedText(transformedTextWithSelection, offsetMappingCalculator)
        }

        @kotlin.jvm.JvmStatic
        private fun mapToTransformed(
            range: TextRange,
            mapping: OffsetMappingCalculator
        ): TextRange {
            val transformedStart = mapping.mapFromSource(range.start)
            // Avoid calculating mapping again if it's going to be the same value.
            val transformedEnd = if (range.collapsed) transformedStart else {
                mapping.mapFromSource(range.end)
            }

            val transformedMin = minOf(transformedStart.min, transformedEnd.min)
            val transformedMax = maxOf(transformedStart.max, transformedEnd.max)
            return if (range.reversed) {
                TextRange(transformedMax, transformedMin)
            } else {
                TextRange(transformedMin, transformedMax)
            }
        }

        @kotlin.jvm.JvmStatic
        private fun mapFromTransformed(
            range: TextRange,
            mapping: OffsetMappingCalculator
        ): TextRange {
            val untransformedStart = mapping.mapFromDest(range.start)
            // Avoid calculating mapping again if it's going to be the same value.
            val untransformedEnd = if (range.collapsed) untransformedStart else {
                mapping.mapFromDest(range.end)
            }

            val untransformedMin = minOf(untransformedStart.min, untransformedEnd.min)
            val untransformedMax = maxOf(untransformedStart.max, untransformedEnd.max)
            return if (range.reversed) {
                TextRange(untransformedMax, untransformedMin)
            } else {
                TextRange(untransformedMin, untransformedMax)
            }
        }
    }
}

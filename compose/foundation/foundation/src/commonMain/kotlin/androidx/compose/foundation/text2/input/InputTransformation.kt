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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.substring
import androidx.compose.ui.text.toUpperCase

/**
 * A function that is ran after every change made to a [TextFieldState] by user input and can change
 * or reject that input.
 *
 * Input transformations are ran after hardware and software keyboard events, when text is pasted or
 * dropped into the field, or when an accessibility service changes the text.
 *
 * To chain filters together, call [then].
 *
 * Prebuilt filters are provided for common filter operations. See:
 *  - [InputTransformation].[maxLengthInChars]`()`
 *  - [InputTransformation].[maxLengthInCodepoints]`()`
 *  - [InputTransformation].[allCaps]`()`
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2CustomInputTransformationSample
 */
@ExperimentalFoundationApi
@Stable
fun interface InputTransformation {

    /**
     * Optional [KeyboardOptions] that will be used as the default keyboard options for configuring
     * the IME. The options passed directly to the text field composable will always override this.
     */
    val keyboardOptions: KeyboardOptions? get() = null

    /**
     * The transform operation. For more information see the documentation on [InputTransformation].
     *
     * To reject all changes in [valueWithChanges], call
     * `valueWithChanges.`[revertAllChanges][TextFieldBuffer.revertAllChanges].
     *
     * @param originalValue The value of the field before the change was performed.
     * @param valueWithChanges The value of the field after the change. This value can be changed
     * in-place to alter or reject the changes or set the selection.
     */
    fun transformInput(originalValue: TextFieldCharSequence, valueWithChanges: TextFieldBuffer)

    companion object : InputTransformation {
        override fun transformInput(
            originalValue: TextFieldCharSequence,
            valueWithChanges: TextFieldBuffer
        ) {
            // Noop.
        }
    }
}

// region Pre-built transformations

/**
 * Creates a filter chain that will run [next] after this. Filters are applied sequentially, so any
 * changes made by this filter will be visible to [next].
 *
 * The returned filter will use the [KeyboardOptions] from [next] if non-null, otherwise it will
 * use the options from this transformation.
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2InputTransformationChainingSample
 *
 * @param next The [InputTransformation] that will be ran after this one.
 */
@ExperimentalFoundationApi
@Stable
@kotlin.jvm.JvmName("thenOrNull")
fun InputTransformation?.then(next: InputTransformation?): InputTransformation? = when {
    this == null -> next
    next == null -> this
    else -> this.then(next)
}

/**
 * Creates a filter chain that will run [next] after this. Filters are applied sequentially, so any
 * changes made by this filter will be visible to [next].
 *
 * The returned filter will use the [KeyboardOptions] from [next] if non-null, otherwise it will
 * use the options from this transformation.
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2InputTransformationChainingSample
 *
 * @param next The [InputTransformation] that will be ran after this one.
 */
@ExperimentalFoundationApi
@Stable
fun InputTransformation.then(next: InputTransformation): InputTransformation =
    FilterChain(this, next)

/**
 * Creates an [InputTransformation] from a function that accepts both the old and proposed
 * [TextFieldCharSequence] and returns the [TextFieldCharSequence] to use for the field.
 *
 * [transformation] can return either `old`, `proposed`, or a completely different value.
 *
 * The selection or cursor will be updated automatically. For more control of selection
 * implement [InputTransformation] directly.
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2InputTransformationByValueChooseSample
 * @sample androidx.compose.foundation.samples.BasicTextField2InputTransformationByValueReplaceSample
 */
@ExperimentalFoundationApi
@Stable
fun InputTransformation.byValue(
    transformation: (
        current: CharSequence,
        proposed: CharSequence
    ) -> CharSequence
): InputTransformation = this.then(InputTransformationByValue(transformation))

/**
 * Returns a [InputTransformation] that forces all text to be uppercase.
 *
 * This transformation automatically configures the keyboard to capitalize all characters.
 *
 * @param locale The [Locale] in which to perform the case conversion.
 */
@ExperimentalFoundationApi
@Stable
fun InputTransformation.allCaps(locale: Locale): InputTransformation =
    this.then(AllCapsTransformation(locale))

/**
 * Returns [InputTransformation] that rejects input which causes the total length of the text field to be
 * more than [maxLength] characters.
 *
 * @see maxLengthInCodepoints
 */
@ExperimentalFoundationApi
@Stable
fun InputTransformation.maxLengthInChars(maxLength: Int): InputTransformation =
    this.then(MaxLengthFilter(maxLength, inCodepoints = false))

/**
 * Returns a [InputTransformation] that rejects input which causes the total length of the text field to
 * be more than [maxLength] codepoints.
 *
 * @see maxLengthInChars
 */
@ExperimentalFoundationApi
@Stable
fun InputTransformation.maxLengthInCodepoints(maxLength: Int): InputTransformation =
    this.then(MaxLengthFilter(maxLength, inCodepoints = true))

// endregion
// region Transformation implementations

@OptIn(ExperimentalFoundationApi::class)
private class FilterChain(
    private val first: InputTransformation,
    private val second: InputTransformation,
) : InputTransformation {

    override val keyboardOptions: KeyboardOptions?
        // TODO(b/295951492) Do proper merging.
        get() = second.keyboardOptions ?: first.keyboardOptions

    override fun transformInput(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBuffer
    ) {
        first.transformInput(originalValue, valueWithChanges)
        second.transformInput(originalValue, valueWithChanges)
    }

    override fun toString(): String = "$first.then($second)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as FilterChain

        if (first != other.first) return false
        if (second != other.second) return false
        if (keyboardOptions != other.keyboardOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = first.hashCode()
        result = 31 * result + second.hashCode()
        result = 32 * result + keyboardOptions.hashCode()
        return result
    }
}

@OptIn(ExperimentalFoundationApi::class)
private data class InputTransformationByValue(
    val transformation: (
        old: CharSequence,
        proposed: CharSequence
    ) -> CharSequence
) : InputTransformation {
    override fun transformInput(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBuffer
    ) {
        val proposed = valueWithChanges.toTextFieldCharSequence()
        val accepted = transformation(originalValue, proposed)
        when {
            // These are reference comparisons – text comparison will be done by setTextIfChanged.
            accepted === proposed -> return
            accepted === originalValue -> valueWithChanges.revertAllChanges()
            else -> {
                valueWithChanges.setTextIfChanged(accepted)
            }
        }
    }

    override fun toString(): String = "InputTransformation.byValue(transformation=$transformation)"
}

// This is a very naive implementation for now, not intended to be production-ready.
@OptIn(ExperimentalFoundationApi::class)
private data class AllCapsTransformation(private val locale: Locale) : InputTransformation {
    override val keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Characters
    )

    override fun transformInput(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBuffer
    ) {
        // only update inserted content
        valueWithChanges.changes.forEachChange { range, _ ->
            if (!range.collapsed) {
                valueWithChanges.replace(
                    range.min,
                    range.max,
                    valueWithChanges.asCharSequence().substring(range).toUpperCase(locale)
                )
            }
        }
    }

    override fun toString(): String = "InputTransformation.allCaps(locale=$locale)"
}

// This is a very naive implementation for now, not intended to be production-ready.
@OptIn(ExperimentalFoundationApi::class)
private data class MaxLengthFilter(
    private val maxLength: Int,
    private val inCodepoints: Boolean
) : InputTransformation {

    init {
        require(maxLength >= 0) { "maxLength must be at least zero, was $maxLength" }
    }

    override fun transformInput(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBuffer
    ) {
        val newLength =
            if (inCodepoints) valueWithChanges.codepointLength else valueWithChanges.length
        if (newLength > maxLength) {
            valueWithChanges.revertAllChanges()
        }
    }

    override fun toString(): String {
        val name = if (inCodepoints) "maxLengthInCodepoints" else "maxLengthInChars"
        return "InputTransformation.$name(maxLength=$maxLength)"
    }
}

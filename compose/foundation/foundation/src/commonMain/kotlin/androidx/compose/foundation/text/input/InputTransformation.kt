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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Stable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.maxTextLength
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
 * - [InputTransformation].[maxLength]`()`
 * - [InputTransformation].[allCaps]`()`
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldCustomInputTransformationSample
 */
@Stable
fun interface InputTransformation {

    /**
     * Optional [KeyboardOptions] that will be used as the default keyboard options for configuring
     * the IME. The options passed directly to the text field composable will always override this.
     */
    val keyboardOptions: KeyboardOptions?
        get() = null

    /**
     * Optional semantics configuration that can update certain characteristics of the applied
     * TextField, e.g. [SemanticsPropertyReceiver.maxTextLength].
     */
    fun SemanticsPropertyReceiver.applySemantics() = Unit

    /**
     * The transform operation. For more information see the documentation on [InputTransformation].
     *
     * This function is scoped to [TextFieldBuffer], a buffer that can be changed in-place to alter
     * or reject the changes or set the selection.
     *
     * To reject all changes in the scoped [TextFieldBuffer], call
     * [revertAllChanges][TextFieldBuffer.revertAllChanges].
     *
     * When multiple [InputTransformation]s are linked together, the [transformInput] function of
     * the first transformation is invoked before the second one. Once the changes are made to
     * [TextFieldBuffer] by the initial [InputTransformation] in the chain, the same instance of
     * [TextFieldBuffer] is forwarded to the subsequent transformation in the chain. Note that
     * [TextFieldBuffer.originalValue] never changes while the buffer is passed along the chain.
     * This sequence persists until the chain reaches its conclusion.
     */
    fun TextFieldBuffer.transformInput()

    companion object : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            // Noop.
        }
    }
}

// region Pre-built transformations

/**
 * Creates a filter chain that will run [next] after this. Filters are applied sequentially, so any
 * changes made by this filter will be visible to [next].
 *
 * The returned filter will use the [KeyboardOptions] from [next] if non-null, otherwise it will use
 * the options from this transformation.
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldInputTransformationChainingSample
 * @param next The [InputTransformation] that will be ran after this one.
 */
@Stable
fun InputTransformation.then(next: InputTransformation): InputTransformation =
    FilterChain(this, next)

/**
 * Creates an [InputTransformation] from a function that accepts both the current and proposed
 * [TextFieldCharSequence] and returns the [TextFieldCharSequence] to use for the field.
 *
 * [transformation] can return either `current`, `proposed`, or a completely different value.
 *
 * The selection or cursor will be updated automatically. For more control of selection implement
 * [InputTransformation] directly.
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldInputTransformationByValueChooseSample
 * @sample androidx.compose.foundation.samples.BasicTextFieldInputTransformationByValueReplaceSample
 */
@Stable
fun InputTransformation.byValue(
    transformation: (current: CharSequence, proposed: CharSequence) -> CharSequence
): InputTransformation = this.then(InputTransformationByValue(transformation))

/**
 * Returns a [InputTransformation] that forces all text to be uppercase.
 *
 * This transformation automatically configures the keyboard to capitalize all characters.
 *
 * @param locale The [Locale] in which to perform the case conversion.
 */
@Stable
fun InputTransformation.allCaps(locale: Locale): InputTransformation =
    this.then(AllCapsTransformation(locale))

/**
 * Returns [InputTransformation] that rejects input which causes the total length of the text field
 * to be more than [maxLength] characters.
 */
@Stable
fun InputTransformation.maxLength(maxLength: Int): InputTransformation =
    this.then(MaxLengthFilter(maxLength))

// endregion
// region Transformation implementations

private class FilterChain(
    private val first: InputTransformation,
    private val second: InputTransformation,
) : InputTransformation {

    override val keyboardOptions: KeyboardOptions?
        get() =
            second.keyboardOptions?.fillUnspecifiedValuesWith(first.keyboardOptions)
                ?: first.keyboardOptions

    override fun SemanticsPropertyReceiver.applySemantics() {
        with(first) { applySemantics() }
        with(second) { applySemantics() }
    }

    override fun TextFieldBuffer.transformInput() {
        with(first) { transformInput() }
        with(second) { transformInput() }
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

private data class InputTransformationByValue(
    val transformation: (current: CharSequence, proposed: CharSequence) -> CharSequence
) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val proposed = toTextFieldCharSequence()
        val accepted = transformation(originalValue, proposed)
        when {
            // These are reference comparisons – text comparison will be done by setTextIfChanged.
            accepted === proposed -> return
            accepted === originalValue -> revertAllChanges()
            else -> {
                setTextIfChanged(accepted)
            }
        }
    }

    override fun toString(): String = "InputTransformation.byValue(transformation=$transformation)"
}

// This is a very naive implementation for now, not intended to be production-ready.
@OptIn(ExperimentalFoundationApi::class)
private data class AllCapsTransformation(private val locale: Locale) : InputTransformation {
    override val keyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.Characters)

    override fun TextFieldBuffer.transformInput() {
        // only update inserted content
        changes.forEachChange { range, _ ->
            if (!range.collapsed) {
                replace(range.min, range.max, asCharSequence().substring(range).toUpperCase(locale))
            }
        }
    }

    override fun toString(): String = "InputTransformation.allCaps(locale=$locale)"
}

// This is a very naive implementation for now, not intended to be production-ready.
private data class MaxLengthFilter(private val maxLength: Int) : InputTransformation {

    init {
        requirePrecondition(maxLength >= 0) { "maxLength must be at least zero" }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        maxTextLength = maxLength
    }

    override fun TextFieldBuffer.transformInput() {
        if (length > maxLength) {
            revertAllChanges()
        }
    }

    override fun toString(): String {
        return "InputTransformation.maxLength($maxLength)"
    }
}

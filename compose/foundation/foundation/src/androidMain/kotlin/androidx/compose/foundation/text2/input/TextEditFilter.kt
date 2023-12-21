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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Stable

/**
 * A function that is ran after every change made to a [TextFieldState] by user input and can change
 * or reject that input.
 *
 * Filters are ran after hardware and software keyboard events, when text is pasted or dropped into
 * the field, or when an accessibility service changes the text.
 *
 * To chain filters together, call [then].
 *
 * Prebuilt filters are provided for common filter operations. See:
 *  - `TextEditFilter`.[maxLengthInChars]`()`
 *  - `TextEditFilter`.[maxLengthInCodepoints]`()`
 *  - `TextEditFilter`.[allCaps]`()`
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2CustomFilterSample
 */
@ExperimentalFoundationApi
@Stable
fun interface TextEditFilter {

    /**
     * Optional [KeyboardOptions] that will be used as the default keyboard options for configuring
     * the IME. The options passed directly to the text field composable will always override this.
     */
    val keyboardOptions: KeyboardOptions? get() = null

    /**
     * The filter operation. For more information see the documentation on [TextEditFilter].
     *
     * To reject all changes in [valueWithChanges], call
     * `valueWithChanges.`[revertAllChanges][TextFieldBufferWithSelection.revertAllChanges].
     *
     * @param originalValue The value of the field before the change was performed.
     * @param valueWithChanges The value of the field after the change. This value can be changed
     * in-place to alter or reject the changes or set the selection.
     */
    fun filter(originalValue: TextFieldCharSequence, valueWithChanges: TextFieldBufferWithSelection)

    companion object
}

/**
 * Creates a filter chain that will run [next] after this. Filters are applied sequentially, so any
 * changes made by this filter will be visible to [next].
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2FilterChainingSample
 *
 * @param next The [TextEditFilter] that will be ran after this one.
 * @param keyboardOptions The [KeyboardOptions] options to use for the chained filter. If not
 * specified, the chained filter will not specify any [KeyboardOptions], even if one or both of
 * this or [next] specified some.
 */
@ExperimentalFoundationApi
@Stable
fun TextEditFilter.then(
    next: TextEditFilter,
    keyboardOptions: KeyboardOptions? = null
): TextEditFilter = FilterChain(this, next, keyboardOptions)

private class FilterChain(
    private val first: TextEditFilter,
    private val second: TextEditFilter,
    override val keyboardOptions: KeyboardOptions?
) : TextEditFilter {

    override fun filter(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBufferWithSelection
    ) {
        first.filter(originalValue, valueWithChanges)
        second.filter(originalValue, valueWithChanges)
    }

    override fun toString(): String = "$first.then($second)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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
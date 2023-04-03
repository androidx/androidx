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
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue

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
 */
@ExperimentalFoundationApi
@Stable
fun interface TextEditFilter {

    /**
     * The filter operation. For more information see the documentation on [TextEditFilter].
     *
     * @sample androidx.compose.foundation.samples.BasicTextField2CustomFilterSample
     *
     * @param oldState The value of the field before the change was performed.
     * @param newState The value of the field after the change. This value can be changed in-place
     * to alter or reject the changes or set the selection.
     */
    fun filter(oldState: TextFieldValue, newState: MutableTextFieldValueWithSelection)

    companion object
}

/**
 * Creates a filter chain that will run [next] after this. Filters are applied in order – [next]
 * will see any the changes made by this filter.
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2FilterChainingSample
 */
@ExperimentalFoundationApi
@Stable
fun TextEditFilter.then(next: TextEditFilter): TextEditFilter = FilterChain(this, next)

private class FilterChain(
    private val first: TextEditFilter,
    private val second: TextEditFilter
) : TextEditFilter {

    override fun filter(oldState: TextFieldValue, newState: MutableTextFieldValueWithSelection) {
        first.filter(oldState, newState)
        second.filter(oldState, newState)
    }

    override fun toString(): String = "$first.then($second)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilterChain

        if (first != other.first) return false
        if (second != other.second) return false

        return true
    }

    override fun hashCode(): Int {
        var result = first.hashCode()
        result = 31 * result + second.hashCode()
        return result
    }
}
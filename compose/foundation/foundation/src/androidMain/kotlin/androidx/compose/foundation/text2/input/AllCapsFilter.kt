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
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase

/**
 * Returns a [TextEditFilter] that forces all text to be uppercase.
 *
 * @param locale The [Locale] in which to perform the case conversion.
 */
@ExperimentalFoundationApi
@Stable
fun TextEditFilter.Companion.allCaps(locale: Locale): TextEditFilter = AllCapsFilter(locale)

// This is a very naive implementation for now, not intended to be production-ready.
@OptIn(ExperimentalFoundationApi::class)
private data class AllCapsFilter(private val locale: Locale) : TextEditFilter {
    override fun filter(oldState: TextFieldValue, newState: MutableTextFieldValueWithSelection) {
        val selection = newState.selectionInCodepoints
        newState.replace(0, newState.length, newState.toString().toUpperCase(locale))
        newState.selectCodepointsIn(selection)
    }

    override fun toString(): String = "TextEditFilter.allCaps(locale=$locale)"
}
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
 * Returns a [InputTransformation] that forces all text to be uppercase.
 *
 * This transformation automatically configures the keyboard to capitalize all characters.
 *
 * @param locale The [Locale] in which to perform the case conversion.
 */
@ExperimentalFoundationApi
@Stable
fun InputTransformation.Companion.allCaps(locale: Locale): InputTransformation =
    AllCapsTransformation(locale)

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

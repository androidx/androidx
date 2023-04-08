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

/**
 * Returns [TextEditFilter] that rejects input which causes the total length of the text field to be
 * more than [maxLength] characters.
 *
 * @see maxLengthInCodepoints
 */
@ExperimentalFoundationApi
@Stable
fun TextEditFilter.Companion.maxLengthInChars(maxLength: Int): TextEditFilter =
    MaxLengthFilter(maxLength, inCodepoints = false)

/**
 * Returns a [TextEditFilter] that rejects input which causes the total length of the text field to
 * be more than [maxLength] codepoints.
 *
 * @see maxLengthInChars
 */
@ExperimentalFoundationApi
@Stable
fun TextEditFilter.Companion.maxLengthInCodepoints(maxLength: Int): TextEditFilter =
    MaxLengthFilter(maxLength, inCodepoints = true)

// This is a very naive implementation for now, not intended to be production-ready.
@OptIn(ExperimentalFoundationApi::class)
private data class MaxLengthFilter(
    private val maxLength: Int,
    private val inCodepoints: Boolean
) : TextEditFilter {

    init {
        require(maxLength >= 0) { "maxLength must be at least zero, was $maxLength" }
    }

    override fun filter(
        originalValue: TextFieldCharSequence,
        valueWithChanges: TextFieldBufferWithSelection
    ) {
        val newLength =
            if (inCodepoints) valueWithChanges.codepointLength else valueWithChanges.length
        if (newLength > maxLength) {
            valueWithChanges.revertAllChanges()
        }
    }

    override fun toString(): String {
        val name = if (inCodepoints) "maxLengthInCodepoints" else "maxLengthInChars"
        return "TextEditFilter.$name(maxLength=$maxLength)"
    }
}
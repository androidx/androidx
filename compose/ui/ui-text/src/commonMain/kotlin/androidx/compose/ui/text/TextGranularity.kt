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

package androidx.compose.ui.text

import kotlin.jvm.JvmInline

/**
 * Used by [Paragraph.getRangeForRect]. It specifies the minimal unit of the text ranges that is
 * considered by the [Paragraph.getRangeForRect].
 */
@JvmInline
value class TextGranularity private constructor(private val value: Int) {
    companion object {
        /**
         * Character level granularity. The text string will be break into ranges each corresponding
         * to a visual character. e.g. "Hi \uD83D\uDE00" will be break into: 'H', 'i', ' ',
         * '\uD83D\uDE00' (grin face emoji).
         */
        val Character = TextGranularity(0)

        /**
         * Word level granularity. The text string will be break into ranges each corresponding to a
         * word. e.g. "Hello world" wil be break into "Hello", "world" the space character is not
         * considered as a word.
         */
        val Word = TextGranularity(1)
    }
}

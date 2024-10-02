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

package androidx.benchmark

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Markdown {
    /** Creates a Markdown link. Escapes relevant Markdown characters (e.g. brackets). */
    private fun createLink(label: String, uri: String) = buildString {
        fun emit(content: String, prefix: Char, suffix: Char) {
            append(prefix)
            var prev: Char? = null
            for (curr in content) {
                val shouldEscape = curr == prefix || curr == suffix
                val isEscaped = prev == '\\'
                if (shouldEscape && !isEscaped) append('\\')
                append(curr)
                prev = curr
            }
            append(suffix)
        }
        emit(label, '[', ']')
        emit(uri, '(', ')')
    }

    fun createFileLink(label: String, path: String) = createLink(label, "file://$path")
}

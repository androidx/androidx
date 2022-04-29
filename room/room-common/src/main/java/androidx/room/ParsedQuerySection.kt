/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room

public sealed class ParsedQuerySection {

    public abstract val text: String

    public data class Text(override val text: String) : ParsedQuerySection()

    public data class BindVar(
        override val text: String,
        val isMultiple: Boolean,
        val value: Any?
    ) : ParsedQuerySection() {
        val varName: String? by lazy {
            if (text.startsWith(":")) {
                text.substring(1)
            } else {
                null
            }
        }

        val parameterCount: Int
            get() {
                if (!isMultiple) {
                    return 1
                }
                return when (value) {
                    is Collection<*> -> value.size
                    is Array<*> -> value.size
                    is ByteArray -> value.size
                    is CharArray -> value.size
                    is ShortArray -> value.size
                    is IntArray -> value.size
                    is LongArray -> value.size
                    is FloatArray -> value.size
                    is DoubleArray -> value.size
                    is BooleanArray -> value.size
                    else -> 1
                }
            }

        val iterator: Iterator<*>?
            get() {
                if (!isMultiple) {
                    return null
                }
                return when (value) {
                    is Collection<*> -> value.iterator()
                    is Array<*> -> value.iterator()
                    is ByteArray -> value.iterator()
                    is CharArray -> value.iterator()
                    is ShortArray -> value.iterator()
                    is IntArray -> value.iterator()
                    is LongArray -> value.iterator()
                    is FloatArray -> value.iterator()
                    is DoubleArray -> value.iterator()
                    is BooleanArray -> value.iterator()
                    else -> null
                }
            }
    }

    public companion object {
        @JvmStatic
        public fun text(text: String): Text = Text(text)

        @JvmStatic
        public fun bindVar(text: String, isMultiple: Boolean, value: Any?): BindVar =
            BindVar(text, isMultiple, value)
    }
}
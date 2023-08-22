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

package androidx.kruth

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.text.padEnd
import kotlin.text.prependIndent

class Fact private constructor(val key: String, val value: String?) {
    override fun toString(): String {
        return if (value == null) key else "$key: $value"
    }

    companion object {
        /**
         * Creates a fact with the given key and value, which will be printed in a format like "key:
         * value." The value is converted to a string by calling [toString] on it.
         */
        @JvmStatic
        @JvmOverloads
        fun fact(key: String, value: Any? = null): Fact {
            return Fact(key, value.toString())
        }

        /**
         * Creates a fact with no value, which will be printed in the format "key" (with no colon or
         * value).
         */
        @JvmStatic
        fun simpleFact(key: String): Fact {
            return Fact(key, null)
        }

        /**
         * Formats the given messages and facts into a string for use as the message of a test failure. In
         * particular, this method horizontally aligns the beginning of fact values.
         */
        @JvmStatic
        fun makeMessage(messages: List<String>, facts: List<Fact>): String {
            val longestKeyLength = facts.filter { it.value != null }
                .maxOfOrNull { it.key.length } ?: 0
            val seenNewlineInValue = facts.filter { it.value != null }
                .any { it.value!!.contains("\n") }
            val messagesToMessage = messages.joinToString("") { it + "\n" }
            val factsToMessage =
                facts.joinToString(
                    separator = "\n",
                    transform = { it.toMessageString(longestKeyLength, seenNewlineInValue) }
                )
            return messagesToMessage + factsToMessage
        }
    }

    /**
     * Helper function to format fact messages with appropriate padding and indentations
     * given the appearance of new line values.
     */
    private fun toMessageString(padKeyToLength: Int, seenNewLineInValue: Boolean) = when {
        value == null -> key
        seenNewLineInValue -> "$key:\n${value.prependIndent("    ")}"
        else -> "${key.padEnd(padKeyToLength)}: $value"
    }
}

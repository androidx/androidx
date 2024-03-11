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

import kotlin.jvm.JvmStatic

// TODO(dustinlam): This needs to implement Serializable on JVM.
class Fact private constructor(internal val key: String, internal val value: String?) {
    override fun toString(): String {
        return if (value == null) key else "$key: $value"
    }

    /**
     * Helper function to format fact messages with appropriate padding and indentations
     * given the appearance of new line values.
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun toMessageString(padKeyToLength: Int, seenNewLineInValue: Boolean) = when {
        value == null -> key
        seenNewLineInValue -> "$key:\n${indent(value)}"
        else -> "${key.padEnd(padKeyToLength)}: $value"
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun indent(value: String): String {
        // We don't want to indent with \t because the text would align exactly with the stack
        // trace. We don't want to indent with \t\t because it would be very far for people with
        // 8-space tabs. Let's compromise and indent by 4 spaces, which is different than both 2-
        // and 8-space tabs.
        return "    ${value.replace("\n", "\n    ")}"
    }

    companion object {
        /**
         * Creates a fact with the given key and value, which will be printed in a format like "key:
         * value." The value is converted to a string by calling [toString] on it.
         */
        @JvmStatic
        fun fact(key: String, value: Any?): Fact {
            return Fact(key, value.toString())
        }

        /**
         * Creates a fact with no value, which will be printed in the format "key" (with no colon or
         * value).
         *
         * In most cases, prefer [fact], which give Truth more flexibility in how to format the fact
         * for display. [simpleFact] is useful primarily for:
         * * messages from no-arg assertions. For example, `isNotEmpty()` would generate the fact
         *   "expected not to be empty"
         * * prose that is part of a larger message. For example, `contains()` sometimes
         *   displays facts like "expected to contain: ..." _"but did not"_ "though it did contain:
         *   ..."
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
        internal fun makeMessage(messages: List<String>, facts: List<Fact>): String {
            val longestKeyLength = facts.filter { it.value != null }
                .maxOfOrNull { it.key.length } ?: 0
            // TODO(cpovirk): Look for other kinds of newlines.
            val seenNewlineInValue = facts.any { it.value?.contains("\n") == true }

            return buildString {
                messages.forEach {
                    append(it)
                    append("\n")
                }

                /*
                 * *Usually* the first fact is printed at the beginning of a new line. However, when
                 * this exception is the cause of another exception, that exception will print it
                 * starting after "Caused by: " on the same line. The other exception sometimes also
                 * reuses this message as its own message. In both of those scenarios, the first
                 * line doesn't start at column 0, so the horizontal alignment is thrown off.
                 *
                 * There's not much we can do about this, short of always starting with a newline
                 * (which would leave a blank line at the beginning of the message in the normal
                 *  case).
                 */
                facts.forEach {
                    append(it.toMessageString(longestKeyLength, seenNewlineInValue))
                    append("\n")
                }

                if (isNotEmpty()) {
                    setLength(length - 1) // remove trailing \n
                }
            }
        }
    }
}

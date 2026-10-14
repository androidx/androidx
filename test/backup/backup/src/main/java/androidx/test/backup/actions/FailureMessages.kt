/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.actions

import androidx.test.backup.surrogateSafeEnd

/** Number of characters of a value that a failure message shows. */
internal const val MAX_SHOWN_CHARS = 64

/** Number of characters that an excerpt shows before the position it points at. */
private const val EXCERPT_LEAD_CHARS = 16

/**
 * Returns [value] as a failure message shows it: quoted, and cut to [MAX_SHOWN_CHARS] characters,
 * followed by its length, when it is longer.
 */
internal fun shown(value: Any?): String {
    val text = "$value"
    return if (text.length <= MAX_SHOWN_CHARS) {
        "'$text'"
    } else {
        "'${text.take(safeEnd(text, MAX_SHOWN_CHARS))}...' (${text.length} chars)"
    }
}

/**
 * Returns the failure message for the text [actual] where [expected] was expected, naming the text
 * [subject].
 *
 * Both texts are shown whole when they are short. Otherwise the message gives their lengths and the
 * first index at which they differ, with an excerpt of each around it.
 */
internal fun textMismatch(subject: String, expected: String, actual: String): String {
    if (expected.length <= MAX_SHOWN_CHARS && actual.length <= MAX_SHOWN_CHARS) {
        return "Expected $subject '$expected' but was '$actual'"
    }
    val index = expected.commonPrefixWith(actual).length
    return "Expected $subject of ${expected.length} chars but was ${actual.length} chars, first " +
        "differing at index $index: expected ${excerpt(expected, index)} but was " +
        excerpt(actual, index)
}

/**
 * Returns the failure message for the bytes [actual] where [expected] was expected, naming the
 * bytes [subject].
 */
internal fun binaryMismatch(subject: String, expected: ByteArray, actual: ByteArray): String {
    val length = minOf(expected.size, actual.size)
    val index = (0 until length).firstOrNull { expected[it] != actual[it] } ?: length
    return "Expected $subject of ${expected.size} bytes but was ${actual.size} bytes, first " +
        "differing at byte $index"
}

/**
 * Returns the part of [text] around [index], quoted, with `...` where it is cut, or `<end>` when
 * [index] is past its end.
 */
private fun excerpt(text: String, index: Int): String {
    if (index >= text.length) return "<end>"
    val start = safeStart(text, maxOf(0, index - EXCERPT_LEAD_CHARS))
    val end = safeEnd(text, minOf(text.length, start + MAX_SHOWN_CHARS))
    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < text.length) "..." else ""
    return "'$prefix${text.substring(start, end)}$suffix'"
}

/** Moves the start [index] of a cut of [text] back, so that it doesn't split a surrogate pair. */
private fun safeStart(text: String, index: Int): Int =
    if (index > 0 && text[index].isLowSurrogate()) index - 1 else index

/** Moves the end [index] of a cut of [text] back, so that it doesn't split a surrogate pair. */
private fun safeEnd(text: String, index: Int): Int = surrogateSafeEnd(text, index)

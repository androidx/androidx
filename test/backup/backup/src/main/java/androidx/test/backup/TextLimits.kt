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

package androidx.test.backup

/**
 * Returns the end index of a cut of [text] at [index], moved back by one if cutting at [index]
 * would split a surrogate pair.
 *
 * A lone surrogate does not survive the trip to the host: `am instrument` prints it as `?`.
 */
internal fun surrogateSafeEnd(text: String, index: Int): Int =
    if (index in 1 until text.length && text[index - 1].isHighSurrogate()) {
        index - 1
    } else {
        index
    }

/**
 * Returns [text], cut to at most [maxChars] characters and followed by [TRUNCATION_MARKER] when it
 * is longer.
 */
internal fun truncated(text: String, maxChars: Int): String =
    if (text.length <= maxChars) {
        text
    } else {
        text.substring(0, surrogateSafeEnd(text, maxChars)) + TRUNCATION_MARKER
    }

/** What [truncated] appends to a text that it cut. */
internal const val TRUNCATION_MARKER = "\n... [truncated]"

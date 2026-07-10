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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.style.LineBreak.Companion.Heading
import androidx.compose.ui.text.style.LineBreak.Companion.Paragraph
import androidx.compose.ui.text.style.LineBreak.Companion.Simple
import kotlin.jvm.JvmInline

/**
 * Configures line breaking behavior when text wraps automatically to fit its container.
 *
 * Offers presets for common use cases:
 *
 * | Preset      | Use Case                    |
 * |-------------|-----------------------------|
 * | [Simple]    | Text fields and inputs      |
 * | [Heading]   | Titles and short text       |
 * | [Paragraph] | Body text and long passages |
 *
 * @sample androidx.compose.ui.text.samples.LineBreakSample
 *
 * Customize Android behavior using:
 * - `Strategy`: Balances layout speed against formatting quality (e.g., greedy vs.
 *   paragraph-optimized breaking).
 * - `Strictness`: Adjusts line-breaking rules for East Asian languages (Chinese, Japanese, and
 *   Korean), determining which characters can start or end a line.
 * - `WordBreak`: Specifies word boundary rules, such as default spacing-based breaking or
 *   phrase-based breaking (ideal for titles).
 *
 * @sample androidx.compose.ui.text.samples.AndroidLineBreakSample
 */
@JvmInline
@Immutable
expect value class LineBreak
@Suppress("KmpVisibilityMismatch")
private constructor(internal val mask: Int) {
    companion object {
        /**
         * Basic, fast line breaking. Ideal for text input fields, as it will cause minimal text
         * reflow when editing.
         */
        @Stable val Simple: LineBreak

        /**
         * Looser breaking rules, suitable for short text such as titles or narrow newspaper
         * columns. For longer lines of text, use [Paragraph] for improved readability.
         */
        @Stable val Heading: LineBreak

        /**
         * Slower, higher quality line breaking for improved readability. Suitable for larger
         * amounts of text.
         */
        @Stable val Paragraph: LineBreak

        /** Represents an unset [LineBreak] value. */
        @Stable val Unspecified: LineBreak
    }
}

/**
 * Returns `true` if it is not [LineBreak.Unspecified].
 *
 * @see LineBreak.Unspecified
 */
@Stable
inline val LineBreak.isSpecified: Boolean
    get() = this != LineBreak.Unspecified

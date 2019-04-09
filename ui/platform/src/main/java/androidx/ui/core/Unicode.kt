/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

// TODO(ryanmentley): This should be in platform, except we need it from Semantics,
// and that puts a loop in the dependency graph.  It should probably not be exposed as public API.
/**
 * Constants for useful Unicode characters.
 *
 * Currently, these characters are all related to bidirectional text.
 *
 * See also:
 *
 *  * <http://unicode.org/reports/tr9/>, which describes the Unicode
 *    bidirectional text algorithm.
 */
object Unicode {
    /**
     * U+202A LEFT-TO-RIGHT EMBEDDING
     *
     * Treat the following text as embedded left-to-right.
     *
     * Use [PDF] to end the embedding.
     */
    const val LRE = "\u202A"

    /**
     * U+202B RIGHT-TO-LEFT EMBEDDING
     *
     * Treat the following text as embedded right-to-left.
     *
     * Use [PDF] to end the embedding.
     */
    const val RLE = "\u202B"

    /**
     * U+202C POP DIRECTIONAL FORMATTING
     *
     * End the scope of the last [LRE], [RLE], [RLO], or [LRO].
     */
    const val PDF = "\u202C"

    /**
     * U+202A LEFT-TO-RIGHT OVERRIDE
     *
     * Force following characters to be treated as strong left-to-right characters.
     *
     * For example, this causes Hebrew text to render backwards.
     *
     * Use [PDF] to end the override.
     */
    const val LRO = "\u202D"

    /**
     * U+202B RIGHT-TO-LEFT OVERRIDE
     *
     * Force following characters to be treated as strong right-to-left characters.
     *
     * For example, this causes English text to render backwards.
     *
     * Use [PDF] to end the override.
     */
    const val RLO = "\u202E"

    /**
     * U+2066 LEFT-TO-RIGHT ISOLATE
     *
     * Treat the following text as isolated and left-to-right.
     *
     * Use [PDI] to end the isolated scope.
     */
    const val LRI = "\u2066"

    /**
     * U+2067 RIGHT-TO-LEFT ISOLATE
     *
     * Treat the following text as isolated and right-to-left.
     *
     * Use [PDI] to end the isolated scope.
     */
    const val RLI = "\u2067"

    /**
     * U+2068 FIRST STRONG ISOLATE
     *
     * Treat the following text as isolated and in the direction of its first
     * strong directional character that is not inside a nested isolate.
     *
     * This essentially "autodetects" the directionality of the text. It is not
     * 100% reliable. For example, Arabic text that starts with an English quote
     * will be detected as LTR, not RTL, which will lead to the text being in a
     * weird order.
     *
     * Use [PDI] to end the isolated scope.
     */
    const val FSI = "\u2068"

    /**
     * U+2069 POP DIRECTIONAL ISOLATE
     *
     * End the scope of the last [LRI], [RLI], or [FSI].
     */
    const val PDI = "\u2069"

    /**
     * U+200E LEFT-TO-RIGHT MARK
     *
     * Left-to-right zero-width character.
     */
    const val LRM = "\u200E"

    /**
     * U+200F RIGHT-TO-LEFT MARK
     *
     * Right-to-left zero-width non-Arabic character.
     */
    const val RLM = "\u200F"

    /**
     * U+061C ARABIC LETTER MARK
     *
     * Right-to-left zero-width Arabic character.
     */
    const val ALM = "\u061C"
}
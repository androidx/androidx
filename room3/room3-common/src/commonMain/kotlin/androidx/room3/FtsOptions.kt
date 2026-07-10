/*
 * Copyright 2018 The Android Open Source Project
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

@file:JvmName("FtsOptions")

package androidx.room3

import kotlin.jvm.JvmName

/** Available option values that can be used with [Fts3], [Fts4] & [Fts5]. */
public object FtsOptions {
    public enum class MatchInfo {
        /**
         * Text matching info as version 3 of the extension module.
         *
         * @see Fts4.matchInfo
         */
        FTS3,

        /**
         * Text matching info as version 4 of the extension module.
         *
         * @see Fts4.matchInfo
         */
        FTS4,
    }

    public enum class Order {
        /**
         * Ascending returning order.
         *
         * @see Fts4.order
         */
        ASC,

        /**
         * Descending returning order.
         *
         * @see Fts4.order
         */
        DESC,
    }

    public enum class Detail {
        /**
         * All information is stored in the FTS index.
         *
         * @see Fts5.detail
         */
        FULL,

        /**
         * Only the row id and the column it appears in is stored.
         *
         * @see Fts5.detail
         */
        COLUMN,

        /**
         * Only the row id is stored.
         *
         * @see Fts5.detail
         */
        NONE,
    }

    /**
     * The name of the default tokenizer used on FTS3 and FTS4 tables.
     *
     * @see Fts4.tokenizer
     * @see Fts4.tokenizerArgs
     */
    public const val TOKENIZER_SIMPLE: String = "simple"

    /**
     * The name of the tokenizer based on the Porter Stemming Algorithm.
     *
     * @see Fts4.tokenizer
     * @see Fts4.tokenizerArgs
     * @see Fts5.tokenizer
     * @see Fts5.tokenizerArgs
     */
    public const val TOKENIZER_PORTER: String = "porter"

    /**
     * The name of a tokenizer implemented by the ICU library.
     *
     * Not available in certain Android builds (e.g. vendor).
     *
     * @see Fts4.tokenizer
     * @see Fts4.tokenizerArgs
     * @see Fts5.tokenizer
     * @see Fts5.tokenizerArgs
     */
    public const val TOKENIZER_ICU: String = "icu"

    /**
     * The name of the tokenizer that classifies all unicode characters as either "separator" or
     * "token" characters according to rules in Unicode Version 6.1.
     *
     * @see Fts4.tokenizer
     * @see Fts4.tokenizerArgs
     * @see Fts5.tokenizer
     * @see Fts5.tokenizerArgs
     */
    public const val TOKENIZER_UNICODE61: String = "unicode61"

    /**
     * The name of the tokenizer which assumes all characters outside the ASCII codepoint range
     * (0-127) are to be treated as token characters.
     *
     * @see Fts5.tokenizer
     * @see Fts5.tokenizerArgs
     */
    public const val TOKENIZER_ASCII: String = "ascii"

    /**
     * The name of the tokenizer that implements a trigram tokenizer that indexes every
     * three-character sequence in the input text.
     *
     * @see Fts5.tokenizer
     * @see Fts5.tokenizerArgs
     */
    public const val TOKENIZER_TRIGRAM: String = "trigram"
}

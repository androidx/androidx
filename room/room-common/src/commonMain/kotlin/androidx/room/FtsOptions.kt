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

package androidx.room

import androidx.annotation.RequiresApi
import kotlin.jvm.JvmName

/** Available option values that can be used with [Fts3] & [Fts4]. */
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

    /**
     * The name of the default tokenizer used on FTS tables.
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
     */
    public const val TOKENIZER_PORTER: String = "porter"

    /**
     * The name of a tokenizer implemented by the ICU library.
     *
     * Not available in certain Android builds (e.g. vendor).
     *
     * @see Fts4.tokenizer
     * @see Fts4.tokenizerArgs
     */
    public const val TOKENIZER_ICU: String = "icu"

    /**
     * The name of the tokenizer that extends the [TOKENIZER_SIMPLE] tokenizer according to rules in
     * Unicode Version 6.1.
     *
     * @see Fts4.tokenizer
     * @see Fts4.tokenizerArgs
     */
    @RequiresApi(21) public const val TOKENIZER_UNICODE61: String = "unicode61"
}

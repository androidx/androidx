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

package androidx.room;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

/**
 * Available option values that can be used with {@link Fts3Entity} & {@link Fts4Entity}.
 */
public class FtsOptions {

    /**
     * Version 3 of the extension module.
     *
     * @see Fts4Entity#matchInfo()
     */
    public static final int FTS3 = 1;

    /**
     * Version 4 of the extension module.
     *
     * @see Fts4Entity#matchInfo()
     */
    public static final int FTS4 = 2;

    @IntDef({FTS3, FTS4})
    @interface FTSVersion {
    }

    /**
     * The name of the default tokenizer used on FTS tables.
     *
     * @see Fts4Entity#tokenizer()
     * @see Fts4Entity#tokenizerArgs()
     */
    public static final int SIMPLE = 0;

    /**
     * The name of the tokenizer based on the Porter Stemming Algorithm.
     *
     * @see Fts4Entity#tokenizer()
     * @see Fts4Entity#tokenizerArgs()
     */
    public static final int PORTER = 1;

    /**
     * The name of a tokenizer implemented by the ICU library.
     * <p>
     * Not available in certain Android builds (e.g. vendor).
     *
     * @see Fts4Entity#tokenizer()
     * @see Fts4Entity#tokenizerArgs()
     */
    public static final int ICU = 2;

    /**
     * The name of the tokenizer that extends the {@link #SIMPLE} tokenizer according to rules in
     * Unicode Version 6.1.
     *
     * @see Fts4Entity#tokenizer()
     * @see Fts4Entity#tokenizerArgs()
     */
    @RequiresApi(21)
    public static final int UNICODE61 = 3;

    @IntDef({SIMPLE, PORTER, UNICODE61})
    public @interface Tokenizer {
    }

    @IntDef({FTS3})
    @interface MatchInfo {
    }

    /**
     * Ascending returning order.
     *
     * @see Fts4Entity#order()
     */
    public static final int ASC = 0;

    /**
     * Descending returning order.
     *
     * @see Fts4Entity#order()
     */
    public static final int DESC = 1;

    @IntDef({ASC, DESC})
    public @interface Order {
    }

    private FtsOptions() {
    }
}

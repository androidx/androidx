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

package androidx.room

import androidx.annotation.RequiresApi
import androidx.room.FtsOptions.TOKENIZER_SIMPLE

/**
 * Marks an [Entity] annotated class as a FTS3 entity. This class will have a mapping SQLite
 * FTS3 table in the database.
 *
 * [FTS3 and FTS4](https://www.sqlite.org/fts3.html) are SQLite virtual table modules
 * that allows full-text searches to be performed on a set of documents.
 *
 * An FTS entity table always has a column named `rowid` that is the equivalent of an
 * `INTEGER PRIMARY KEY` index. Therefore, an FTS entity can only have a single field
 * annotated with [PrimaryKey], it must be named `rowid` and must be of
 * `INTEGER` affinity. The field can be optionally omitted in the class but can still be
 * used in queries.
 *
 * All fields in an FTS entity are of `TEXT` affinity, except the for the 'rowid' field.
 *
 * Example:
 *
 * ```
 * @Entity
 * @Fts3
 * data class Mail (
 *   @PrimaryKey
 *   @ColumnInfo(name = "rowid")
 *   val rowId: Int,
 *   val subject: String,
 *   val body: String
 * )
 * ```
 *
 * @see [Entity]
 * @see [Dao]
 * @see [Database]
 * @see [PrimaryKey]
 * @see [ColumnInfo]
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@RequiresApi(16)
public annotation class Fts3(

    /**
     * The tokenizer to be used in the FTS table.
     *
     * The default value is [FtsOptions.TOKENIZER_SIMPLE]. Tokenizer arguments can be defined
     * with [tokenizerArgs].
     *
     * If a custom tokenizer is used, the tokenizer and its arguments are not verified at compile
     * time.
     *
     * See [SQLite tokenizers documentation](https://www.sqlite.org/fts3.html#tokenizer) for more
     * details.
     *
     * @return The tokenizer to use on the FTS table. Built-in available tokenizers are
     * [FtsOptions.TOKENIZER_SIMPLE], [FtsOptions.TOKENIZER_PORTER] and
     * [FtsOptions.TOKENIZER_UNICODE61].
     * @see [tokenizerArgs]
     */
    val tokenizer: String = TOKENIZER_SIMPLE,

    /**
     * Optional arguments to configure the defined tokenizer.
     * <p>
     * Tokenizer arguments consist of an argument name, followed by an "=" character, followed by
     * the option value. For example, <code>separators=.</code> defines the dot character as an
     * additional separator when using the {@link FtsOptions#TOKENIZER_UNICODE61} tokenizer.
     * <p>
     * The available arguments that can be defined depend on the tokenizer defined, see the
     * [SQLite tokernizers documentation](https://www.sqlite.org/fts3.html#tokenizer) for details.
     *
     * @return A list of tokenizer arguments strings.
     */
    val tokenizerArgs: Array<String> = []
)

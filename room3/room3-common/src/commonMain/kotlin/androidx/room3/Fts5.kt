/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3

import androidx.room3.FtsOptions.TOKENIZER_UNICODE61
import kotlin.reflect.KClass

/**
 * Marks an [Entity] annotated class as a FTS5 entity. This class will have a mapping SQLite FTS5
 * table in the database.
 *
 * [FTS5](https://www.sqlite.org/fts5.html) is a SQLite virtual table module that allows full-text
 * searches to be performed on a set of documents.
 *
 * An FTS entity table always has a column named `rowid` that is the equivalent of an `INTEGER
 * PRIMARY KEY`. Therefore, an FTS entity can only have a single property annotated with
 * [PrimaryKey], it must be named `rowid` and must be of `INTEGER` affinity. The property can be
 * optionally omitted in the class but can still be used in queries.
 *
 * All properties in an FTS entity are of `TEXT` affinity, except the 'rowid' property.
 *
 * Example:
 * ```
 * @Entity
 * @Fts5
 * data class Mail (
 *   @PrimaryKey
 *   @ColumnInfo(name = "rowid")
 *   val rowId: Long,
 *   val subject: String,
 *   val body: String
 * )
 * ```
 *
 * **Warning**: The availability of FTS5 is based on the driver used for the database. For Android
 * specifically the [androidx.sqlite.driver.bundled.BundledSQLiteDriver] supports FTS5.
 *
 * @see [Entity]
 * @see [Dao]
 * @see [Database]
 * @see [PrimaryKey]
 * @see [ColumnInfo]
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Fts5(

    /**
     * The tokenizer to be used in the FTS table.
     *
     * The default value is [FtsOptions.TOKENIZER_UNICODE61]. Tokenizer arguments can be defined
     * with [tokenizerArgs].
     *
     * If a custom tokenizer is used, the tokenizer and its arguments are not verified at compile
     * time.
     *
     * For details, see
     * [SQLite tokenizers documentation](https://www.sqlite.org/fts5.html#tokenizers)
     *
     * @return The tokenizer to use on the FTS table. Built-in available tokenizers are
     *   [FtsOptions.TOKENIZER_UNICODE61], [FtsOptions.TOKENIZER_PORTER],
     *   [FtsOptions.TOKENIZER_ASCII] and [FtsOptions.TOKENIZER_TRIGRAM].
     * @see [tokenizerArgs]
     */
    val tokenizer: String = TOKENIZER_UNICODE61,

    /**
     * Optional arguments to configure the defined tokenizer.
     *
     * Tokenizer arguments are a white-space series of values represented on this annotation value
     * list. For example, `tokenizerArgs = ["separators", "'.'"]` defines the dot character as an
     * additional separator when using the [FtsOptions.TOKENIZER_UNICODE61] tokenizer.
     *
     * The available arguments that can be defined depend on the tokenizer defined, see the
     * [SQLite tokenizers documentation](https://www.sqlite.org/fts5.html#tokenizers) for details.
     *
     * @return A list of tokenizer arguments strings.
     */
    val tokenizerArgs: Array<String> = [],

    /**
     * The external content entity who's mapping table will be used as content for the FTS table.
     *
     * Declaring this value makes the mapping FTS table of this entity operate in "external content"
     * mode. In such mode the FTS table does not store its own content but instead uses the data in
     * the entity mapped table defined in this value. This option allows FTS5 to forego storing the
     * text being indexed which can be used to achieve significant space savings.
     *
     * In "external mode" the content table and the FTS table need to be synced. Room will create
     * the necessary triggers to keep the tables in sync. Therefore, all write operations should be
     * performed against the content entity table and not the FTS table.
     *
     * The content sync triggers created by Room will be removed before migrations are executed and
     * are re-created once migrations are complete. This prevents the triggers from interfering with
     * migrations but means that if data needs to be migrated then write operations might need to be
     * done in both the FTS and content tables.
     *
     * See the
     * [External Content FTS5 Tables](https://www.sqlite.org/fts5.html#external_content_tables)
     * documentation for details.
     *
     * @return The external content entity.
     */
    val contentEntity: KClass<*> = Any::class,

    /**
     * The column name to be used as 'content_rowid'.
     *
     * Declaring this value without setting [contentEntity] is an error.
     *
     * For details, see the
     * [content_rowid= option documentation](https://www.sqlite.org/fts5.html#external_content_tables).
     *
     * @return The column name to be used as 'content_rowid'.
     */
    val contentRowId: String = "",

    /**
     * The list of prefix sizes to index.
     *
     * For details,
     * [the prefix= option documentation](https://www.sqlite.org/fts5.html#prefix_indexes).
     *
     * @return A list of non-zero positive prefix sizes to index.
     */
    val prefix: IntArray = [],

    /**
     * Enable or disable storing column sizes. Defaults to `true`.
     *
     * For details, see the
     * [columnsize= option documentation](https://www.sqlite.org/fts5.html#the_columnsize_option).
     *
     * @return The 'columnsize' option.
     */
    val hasColumnSize: Boolean = true,

    /**
     * The 'detail' option affects what is stored in the virtual table.
     *
     * Using values other than [FtsOptions.Detail.FULL] will reduce the space consumed but will also
     * affect the FTS capabilities and efficiency. The default value is [FtsOptions.Detail.FULL].
     *
     * For details, see the
     * [detail= option documentation](https://www.sqlite.org/fts5.html#the_detail_option).
     *
     * @return The 'detail' option.
     */
    val detail: FtsOptions.Detail = FtsOptions.Detail.FULL,

    /**
     * The list of column names on the FTS table that won't be indexed.
     *
     * For details, see the
     * [UNINDEXED column option documentation](https://www.sqlite.org/fts5.html#the_unindexed_column_option).
     *
     * @return A list of column names that will not be indexed by the FTS extension.
     */
    val notIndexed: Array<String> = [],
)

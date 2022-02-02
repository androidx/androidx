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

import kotlin.reflect.KClass

import androidx.annotation.RequiresApi
import androidx.room.FtsOptions.TOKENIZER_SIMPLE
import androidx.room.FtsOptions.MatchInfo
import androidx.room.FtsOptions.Order

/**
 * Marks an [Entity] annotated class as a FTS4 entity. This class will have a mapping SQLite
 * FTS4 table in the database.
 *
 * [FTS3 and FTS4] (https://www.sqlite.org/fts3.html) are SQLite virtual table modules
 * that allows full-text searches to be performed on a set of documents.
 *
 * An FTS entity table always has a column named `rowid` that is the equivalent of an
 * `INTEGER PRIMARY KEY` index. Therefore, an FTS entity can only have a single field
 * annotated with [PrimaryKey], it must be named `rowid` and must be of
 * `INTEGER` affinity. The field can be optionally omitted in the class but can still be
 * used in queries.
 *
 * All fields in an FTS entity are of `TEXT` affinity, except the for the 'rowid' and
 * 'languageid' fields.
 *
 * Example:
 *
 * ```
 * @Entity
 * @Fts4
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
public annotation class Fts4(

    /**
     * The tokenizer to be used in the FTS table.
     *
     * The default value is [FtsOptions.TOKENIZER_SIMPLE]. Tokenizer arguments can be defined
     * with [tokenizerArgs].
     *
     * If a custom tokenizer is used, the tokenizer and its arguments are not verified at compile
     * time.
     *
     * For details, see [SQLite tokernizers documentation](https://www.sqlite.org/fts3.html#tokenizer)
     *
     * @return The tokenizer to use on the FTS table. Built-in available tokenizers are
     * [FtsOptions.TOKENIZER_SIMPLE], [FtsOptions.TOKENIZER_PORTER] and
     * [FtsOptions.TOKENIZER_UNICODE61].
     * @see [tokenizerArgs]
     */
    val tokenizer: String = TOKENIZER_SIMPLE,

    /**
     * Optional arguments to configure the defined tokenizer.
     *
     * Tokenizer arguments consist of an argument name, followed by an "=" character, followed by
     * the option value. For example, `separators=.` defines the dot character as an
     * additional separator when using the [FtsOptions.TOKENIZER_UNICODE61] tokenizer.
     *
     * The available arguments that can be defined depend on the tokenizer defined, see the
     * [SQLite tokenizers documentation](https://www.sqlite.org/fts3.html#tokenizer) for details.
     *
     * @return A list of tokenizer arguments strings.
     */
    val tokenizerArgs: Array<String> = [],

    /**
     * The external content entity who's mapping table will be used as content for the FTS table.
     *
     * Declaring this value makes the mapping FTS table of this entity operate in "external content"
     * mode. In such mode the FTS table does not store its own content but instead uses the data in
     * the entity mapped table defined in this value. This option allows FTS4 to forego storing the
     * text being indexed which can be used to achieve significant space savings.
     *
     * In "external mode" the content table and the FTS table need to be synced. Room will create
     * the necessary triggers to keep the tables in sync. Therefore, all write operations should
     * be performed against the content entity table and not the FTS table.
     *
     * The content sync triggers created by Room will be removed before migrations are executed and
     * are re-created once migrations are complete. This prevents the triggers from interfering with
     * migrations but means that if data needs to be migrated then write operations might need to be
     * done in both the FTS and content tables.
     *
     * See the [External Content FTS4 Tables](https://www.sqlite.org/fts3.html#_external_content_fts4_tables_)
     * documentation for details.
     *
     * @return The external content entity.
     */
    val contentEntity: KClass<*> = Any::class,

    /**
     * The column name to be used as 'languageid'.
     *
     * Allows the FTS4 extension to use the defined column name to specify the language stored in
     * each row. When this is defined a field of type `INTEGER` with the same name must
     * exist in the class.
     *
     * FTS queries are affected by defining this option, see
     * [the languageid= option documentation](https://www.sqlite.org/fts3.html#the_languageid_option)
     * for details.
     *
     * @return The column name to be used as 'languageid'.
     */
    val languageId: String = "",

    /**
     * The FTS version used to store text matching information.
     *
     * The default value is [MatchInfo.FTS4]. Disk space consumption can be reduced by
     * setting this option to FTS3, see
     * [the matchinfo= option documentation](https://www.sqlite.org/fts3.html#the_matchinfo_option)
     * for details.
     *
     * @return The match info version, either [MatchInfo.FTS4] or [MatchInfo.FTS3].
     */
    val matchInfo: MatchInfo = MatchInfo.FTS4,

    /**
     * The list of column names on the FTS table that won't be indexed.
     *
     * For details, see the
     * [notindexed= option documentation](https://www.sqlite.org/fts3.html#the_notindexed_option).
     *
     * @return A list of column names that will not be indexed by the FTS extension.
     */
    val notIndexed: Array<String> = [],

    /**
     * The list of prefix sizes to index.
     *
     * For details,
     * [the prefix= option documentation](https://www.sqlite.org/fts3.html#the_prefix_option).
     *
     * @return A list of non-zero positive prefix sizes to index.
     */
    val prefix: IntArray = [],

    /**
     * The preferred 'rowid' order of the FTS table.
     *
     * The default value is [Order.ASC]. If many queries are run against the FTS table use
     * `ORDER BY row DESC` then it may improve performance to set this option to
     * [Order.DESC], enabling the FTS module to store its data in a way that optimizes
     * returning results in descending order by `rowid`.
     *
     * @return The preferred order, either [Order.ASC] or [Order.DESC].
     */
    val order: Order = Order.ASC
)

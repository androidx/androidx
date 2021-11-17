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

import static androidx.room.FtsOptions.TOKENIZER_SIMPLE;

import androidx.annotation.RequiresApi;
import androidx.room.FtsOptions.MatchInfo;
import androidx.room.FtsOptions.Order;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link Entity} annotated class as a FTS4 entity. This class will have a mapping SQLite
 * FTS4 table in the database.
 * <p>
 * <a href="https://www.sqlite.org/fts3.html">FTS3 and FTS4</a> are SQLite virtual table modules
 * that allows full-text searches to be performed on a set of documents.
 * <p>
 * An FTS entity table always has a column named <code>rowid</code> that is the equivalent of an
 * <code>INTEGER PRIMARY KEY</code> index. Therefore, an FTS entity can only have a single field
 * annotated with {@link PrimaryKey}, it must be named <code>rowid</code> and must be of
 * <code>INTEGER</code> affinity. The field can be optionally omitted in the class but can still be
 * used in queries.
 * <p>
 * All fields in an FTS entity are of <code>TEXT</code> affinity, except the for the 'rowid' and
 * 'languageid' fields.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Entity
 * {@literal @}Fts4
 * public class Mail {
 *   {@literal @}PrimaryKey
 *   {@literal @}ColumnInfo(name = "rowid")
 *   private final int rowId;
 *   private final String subject;
 *   private final String body;
 *
 *   public Mail(int rowId, String subject, String body) {
 *       this.rowId = rowId;
 *       this.subject = subject;
 *       this.body = body;
 *   }
 *
 *   public String getRowId() {
 *       return rowId;
 *   }
 *   public String getSubject() {
 *       return subject;
 *   }
 *   public void getBody() {
 *       return body;
 *   }
 * }
 * </pre>
 *
 * @see Entity
 * @see Dao
 * @see Database
 * @see PrimaryKey
 * @see ColumnInfo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@RequiresApi(16)
public @interface Fts4 {

    /**
     * The tokenizer to be used in the FTS table.
     * <p>
     * The default value is {@link FtsOptions#TOKENIZER_SIMPLE}. Tokenizer arguments can be defined
     * with {@link #tokenizerArgs()}.
     * <p>
     * If a custom tokenizer is used, the tokenizer and its arguments are not verified at compile
     * time.
     *
     * @return The tokenizer to use on the FTS table. Built-in available tokenizers are
     * {@link FtsOptions#TOKENIZER_SIMPLE}, {@link FtsOptions#TOKENIZER_PORTER} and
     * {@link FtsOptions#TOKENIZER_UNICODE61}.
     * @see #tokenizerArgs()
     * @see <a href="https://www.sqlite.org/fts3.html#tokenizer">SQLite tokernizers
     * documentation</a>
     */
    String tokenizer() default TOKENIZER_SIMPLE;

    /**
     * Optional arguments to configure the defined tokenizer.
     * <p>
     * Tokenizer arguments consist of an argument name, followed by an "=" character, followed by
     * the option value. For example, <code>separators=.</code> defines the dot character as an
     * additional separator when using the {@link FtsOptions#TOKENIZER_UNICODE61} tokenizer.
     * <p>
     * The available arguments that can be defined depend on the tokenizer defined, see the
     * <a href="https://www.sqlite.org/fts3.html#tokenizer">SQLite tokernizers documentation</a> for
     * details.
     *
     * @return A list of tokenizer arguments strings.
     */
    String[] tokenizerArgs() default {};

    /**
     * The external content entity who's mapping table will be used as content for the FTS table.
     * <p>
     * Declaring this value makes the mapping FTS table of this entity operate in "external content"
     * mode. In such mode the FTS table does not store its own content but instead uses the data in
     * the entity mapped table defined in this value. This option allows FTS4 to forego storing the
     * text being indexed which can be used to achieve significant space savings.
     * <p>
     * In "external mode" the content table and the FTS table need to be synced. Room will create
     * the necessary triggers to keep the tables in sync. Therefore, all write operations should
     * be performed against the content entity table and not the FTS table.
     * <p>
     * The content sync triggers created by Room will be removed before migrations are executed and
     * are re-created once migrations are complete. This prevents the triggers from interfering with
     * migrations but means that if data needs to be migrated then write operations might need to be
     * done in both the FTS and content tables.
     *
     * @return The external content entity.
     * @see <a href="https://www.sqlite.org/fts3.html#_external_content_fts4_tables_">External
     * Content FTS4 Tables</a>
     */
    Class<?> contentEntity() default Object.class;

    /**
     * The column name to be used as 'languageid'.
     * <p>
     * Allows the FTS4 extension to use the defined column name to specify the language stored in
     * each row. When this is defined a field of type <code>INTEGER</code> with the same name must
     * exist in the class.
     * <p>
     * FTS queries are affected by defining this option, see
     * <a href=https://www.sqlite.org/fts3.html#the_languageid_option>the languageid= option
     * documentation</a> for details.
     *
     * @return The column name to be used as 'languageid'.
     */
    String languageId() default "";


    /**
     * The FTS version used to store text matching information.
     * <p>
     * The default value is {@link MatchInfo#FTS4}. Disk space consumption can be reduced by
     * setting this option to FTS3, see
     * <a href=https://www.sqlite.org/fts3.html#the_matchinfo_option>the matchinfo= option
     * documentation</a> for details.
     *
     * @return The match info version, either {@link MatchInfo#FTS4} or {@link MatchInfo#FTS3}.
     */
    MatchInfo matchInfo() default MatchInfo.FTS4;

    /**
     * The list of column names on the FTS table that won't be indexed.
     *
     * @return A list of column names that will not be indexed by the FTS extension.
     * @see <a href="https://www.sqlite.org/fts3.html#the_notindexed_option">The notindexed= option
     * documentation</a>
     */
    String[] notIndexed() default {};

    /**
     * The list of prefix sizes to index.
     *
     * @return A list of non-zero positive prefix sizes to index.
     * @see <a href="https://www.sqlite.org/fts3.html#the_prefix_option">The prefix= option
     * documentation</a>
     */
    int[] prefix() default {};

    /**
     * The preferred 'rowid' order of the FTS table.
     * <p>
     * The default value is {@link Order#ASC}. If many queries are run against the FTS table use
     * <code>ORDER BY row DESC</code> then it may improve performance to set this option to
     * {@link Order#DESC}, enabling the FTS module to store its data in a way that optimizes
     * returning results in descending order by rowid.
     *
     * @return The preferred order, either {@link Order#ASC} or {@link Order#DESC}.
     */
    Order order() default Order.ASC;
}

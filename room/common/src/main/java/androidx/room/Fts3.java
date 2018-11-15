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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link Entity} annotated class as a FTS3 entity. This class will have a mapping SQLite
 * FTS3 table in the database.
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
 * All fields in an FTS entity are of <code>TEXT</code> affinity, except the for the 'rowid' field.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Entity
 * {@literal @}Fts3
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
public @interface Fts3 {

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
}

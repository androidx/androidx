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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an entity. This class will have a mapping SQLite FTS3 table in the database.
 * <p>
 * <a href="https://www.sqlite.org/fts3.html">FTS3 and FTS4</a> are SQLite virtual table modules
 * that allows full-text searches to be performed on a set of documents.
 * <p>
 * An FtsEntity table always has a column named <code>rowid</code> that is the equivalent of an
 * <code>INTEGER PRIMARY KEY</code> index. Therefore an FtsEntity can only have a single field
 * annotated with {@link PrimaryKey}, it must be named <code>rowid</code> and must be of
 * <code>INTEGER</code> affinity. The field can be optionally omitted in the class but can still be
 * used in queries.
 * <p>
 * All fields in an FtsEntity are of <code>TEXT</code> affinity, except the for the 'rowid' field.
 * <p>
 * Similar to an {@link Entity}, each FtsEntity must either have a no-arg constructor or a
 * constructor whose parameters match fields (based on type and name).
 * <p>
 * Example:
 * <pre>
 * {@literal @}Fts3Entity
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
public @interface Fts3Entity {

    /**
     * The table name in the SQLite database. If not set, defaults to the class name.
     *
     * @return The SQLite tableName of the FtsEntity.
     */
    String tableName() default "";

    /**
     * The tokenizer to be used in the FTS table.
     * <p>
     * The default value is {@link FtsOptions#SIMPLE}. Tokenizer arguments can be defined with
     * {@link #tokenizerArgs()}.
     *
     * @return The tokenizer to use on the FTS table. This is either {@link FtsOptions#SIMPLE},
     * {@link FtsOptions#PORTER} or {@link FtsOptions#UNICODE61}.
     * @see #tokenizerArgs()
     * @see <a href="https://www.sqlite.org/fts3.html#tokenizer">SQLite tokernizers
     * documentation</a>
     */
    @FtsOptions.Tokenizer int tokenizer() default FtsOptions.SIMPLE;

    /**
     * Optional arguments to configure the defined tokenizer.
     * <p>
     * Tokenizer arguments consist of an argument name, followed by an "=" character, followed by
     * the option value. For example, <code>separators=.</code> defines the dot character as an
     * additional separator when using the {@link FtsOptions#UNICODE61} tokenizer.
     * <p>
     * The available arguments that can be defined depend on the tokenizer defined, see the
     * <a href="https://www.sqlite.org/fts3.html#tokenizer">SQLite tokernizers documentation</a> for
     * details.
     *
     * @return A list of tokenizer arguments strings.
     */
    String[] tokenizerArgs() default {};
}

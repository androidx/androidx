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

package androidx.room.util;

import android.database.Cursor;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A data class that holds the information about an FTS table.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class FtsTableInfo {

    // A set of valid FTS Options
    private static final String[] FTS_OPTIONS = new String[] {
            "tokenize=", "compress=", "content=", "languageid=", "matchinfo=", "notindexed=",
            "order=", "prefix=", "uncompress="};

    /**
     * The table name
     */
    public final String name;

    /**
     * The column names
     */
    public final Set<String> columns;

    /**
     * The set of options. Each value in the set contains the option in the following format:
     * &lt;key&gt;=&lt;value&gt;.
     */
    public final Set<String> options;

    public FtsTableInfo(String name, Set<String> columns, Set<String> options) {
        this.name = name;
        this.columns = columns;
        this.options = options;
    }

    public FtsTableInfo(String name, Set<String> columns, String createSql) {
        this.name = name;
        this.columns = columns;
        this.options = parseOptions(createSql);
    }

    /**
     * Reads the table information from the given database.
     *
     * @param database  The database to read the information from.
     * @param tableName The table name.
     * @return A FtsTableInfo containing the columns and options for the provided table name.
     */
    public static FtsTableInfo read(SupportSQLiteDatabase database, String tableName) {
        Set<String> columns = readColumns(database, tableName);
        Set<String> options = readOptions(database, tableName);

        return new FtsTableInfo(tableName, columns, options);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private static Set<String> readColumns(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = database.query("PRAGMA table_info(`" + tableName + "`)");
        Set<String> columns = new HashSet<>();
        try {
            if (cursor.getColumnCount() > 0) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(nameIndex));
                }
            }
        } finally {
            cursor.close();
        }
        return columns;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private static Set<String> readOptions(SupportSQLiteDatabase database, String tableName) {
        String sql = "";
        Cursor cursor = database.query(
                "SELECT * FROM sqlite_master WHERE `name` = '" + tableName + "'");
        try {
            if (cursor.moveToFirst()) {
                sql = cursor.getString(cursor.getColumnIndexOrThrow("sql"));
            }
        } finally {
            cursor.close();
        }
        return parseOptions(sql);
    }

    /**
     * Parses FTS options from the create statement of an FTS table.
     *
     * This method assumes the given create statement is a valid well-formed SQLite statement as
     * defined in the <a href="https://www.sqlite.org/lang_createvtab.html">CREATE VIRTUAL TABLE
     * syntax diagram</a>.
     *
     * @param createStatement the "CREATE VIRTUAL TABLE" statement.
     * @return the set of FTS option key and values in the create statement.
     */
    @VisibleForTesting
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static Set<String> parseOptions(String createStatement) {
        if (createStatement.isEmpty()) {
            return new HashSet<>();
        }

        // Module arguments are within the parenthesis followed by the module name.
        String argsString = createStatement.substring(
                createStatement.indexOf('(') + 1,
                createStatement.lastIndexOf(')'));

        // Split the module argument string by the comma delimiter, keeping track of quotation so
        // so that if the delimiter is found within a string literal we don't substring at the wrong
        // index. SQLite supports four ways of quoting keywords, see:
        // https://www.sqlite.org/lang_keywords.html
        List<String> args = new ArrayList<>();
        ArrayDeque<Character> quoteStack = new ArrayDeque<>();
        int lastDelimiterIndex = -1;
        for (int i = 0; i < argsString.length(); i++) {
            char c = argsString.charAt(i);
            switch (c) {
                case '\'':
                case '"':
                case '`':
                    if (quoteStack.isEmpty()) {
                        quoteStack.push(c);
                    } else if (quoteStack.peek() == c) {
                        quoteStack.pop();
                    }
                    break;
                case '[':
                    if (quoteStack.isEmpty()) {
                        quoteStack.push(c);
                    }
                    break;
                case ']':
                    if (!quoteStack.isEmpty() && quoteStack.peek() == '[') {
                        quoteStack.pop();
                    }
                    break;
                case ',':
                    if (quoteStack.isEmpty()) {
                        args.add(argsString.substring(lastDelimiterIndex + 1, i).trim());
                        lastDelimiterIndex = i;
                    }
                    break;
            }
        }
        args.add(argsString.substring(lastDelimiterIndex + 1).trim()); // Add final argument.

        // Match args against valid options, otherwise they are column definitions.
        HashSet<String> options = new HashSet<>();
        for (String arg : args) {
            for (String validOption : FTS_OPTIONS) {
                if (arg.startsWith(validOption)) {
                    options.add(arg);
                }
            }
        }

        return options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FtsTableInfo)) return false;

        FtsTableInfo that = (FtsTableInfo) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (columns != null ? !columns.equals(that.columns) : that.columns != null) return false;
        return options != null ? options.equals(that.options) : that.options == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FtsTableInfo{"
                + "name='" + name + '\''
                + ", columns=" + columns
                + ", options=" + options
                + '}';
    }
}

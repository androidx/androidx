/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.support.room.util;

import android.database.Cursor;
import android.support.annotation.RestrictTo;

import com.android.support.db.SupportSQLiteDatabase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A data class that holds the information about a table.
 * <p>
 * It directly maps to the result of {@code PRAGMA table_info(<table_name>)}. Check the
 * <a href="http://www.sqlite.org/pragma.html#pragma_table_info">PRAGMA table_info</a>
 * documentation for more details.
 * <p>
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings({"WeakerAccess", "unused"})
// if you change this class, you must change TableInfoWriter.kt
public class TableInfo {
    /**
     * The table name.
     */
    public final String name;
    /**
     * Unmodifiable map of columns keyed by column name.
     */
    public final Map<String, Column> columns;

    @SuppressWarnings("unused")
    public TableInfo(String name, Map<String, Column> columns) {
        this.name = name;
        this.columns = Collections.unmodifiableMap(columns);
    }

    /**
     * Reads the table information from the given database.
     *
     * @param database  The database to read the information from.
     * @param tableName The table name.
     * @return A TableInfo containing the schema information for the provided table name.
     */
    @SuppressWarnings("SameParameterValue")
    public static TableInfo read(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = database.rawQuery("PRAGMA table_info(`" + tableName + "`)",
                StringUtil.EMPTY_STRING_ARRAY);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            Map<String, Column> columns = extractColumns(cursor);
            return new TableInfo(tableName, columns);
        } finally {
            cursor.close();
        }
    }

    private static Map<String, Column> extractColumns(Cursor cursor) {
        Map<String, Column> columns = new HashMap<>();
        if (cursor.getColumnCount() > 0) {
            int nameIndex = cursor.getColumnIndex("name");
            int typeIndex = cursor.getColumnIndex("type");
            int pkIndex = cursor.getColumnIndex("pk");

            while (cursor.moveToNext()) {
                final String name = cursor.getString(nameIndex);
                final String type = cursor.getString(typeIndex);
                final int primaryKeyPosition = cursor.getInt(pkIndex);
                columns.put(name, new Column(name, type, primaryKeyPosition));
            }
        }
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableInfo tableInfo = (TableInfo) o;

        //noinspection SimplifiableIfStatement
        if (name != null ? !name.equals(tableInfo.name) : tableInfo.name != null) return false;
        return columns != null ? columns.equals(tableInfo.columns) : tableInfo.columns == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TableInfo{name='" + name + '\'' + ", columns=" + columns + '}';
    }

    /**
     * Holds the information about a database column.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Column {
        /**
         * The column name.
         */
        public final String name;
        /**
         * The column type affinity.
         */
        public final String type;
        /**
         * The position of the column in the list of primary keys, 0 if the column is not part
         * of the primary key.
         */
        public final int primaryKeyPosition;

        // if you change this constructor, you must change TableInfoWriter.kt
        public Column(String name, String type, int primaryKeyPosition) {
            this.name = name;
            this.type = type;
            this.primaryKeyPosition = primaryKeyPosition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Column column = (Column) o;

            if (primaryKeyPosition != column.primaryKeyPosition) return false;
            //noinspection SimplifiableIfStatement
            if (!name.equals(column.name)) return false;
            return type != null ? type.equals(column.type) : column.type == null;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + primaryKeyPosition;
            return result;
        }

        @Override
        public String toString() {
            return "Column{"
                    + "name='" + name + '\''
                    + ", type='" + type + '\''
                    + ", primaryKeyPosition=" + primaryKeyPosition
                    + '}';
        }
    }
}

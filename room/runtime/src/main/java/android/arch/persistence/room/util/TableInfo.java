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

package android.arch.persistence.room.util;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A data class that holds the information about a table.
 * <p>
 * It directly maps to the result of {@code PRAGMA table_info(<table_name>)}. Check the
 * <a href="http://www.sqlite.org/pragma.html#pragma_table_info">PRAGMA table_info</a>
 * documentation for more details.
 * <p>
 * Even though SQLite column names are case insensitive, this class uses case sensitive matching.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings({"WeakerAccess", "unused", "TryFinallyCanBeTryWithResources"})
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

    public final Set<ForeignKey> foreignKeys;

    @SuppressWarnings("unused")
    public TableInfo(String name, Map<String, Column> columns, Set<ForeignKey> foreignKeys) {
        this.name = name;
        this.columns = Collections.unmodifiableMap(columns);
        this.foreignKeys = Collections.unmodifiableSet(foreignKeys);
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
        Map<String, Column> columns = readColumns(database, tableName);
        Set<ForeignKey> foreignKeys = readForeignKeys(database, tableName);
        return new TableInfo(tableName, columns, foreignKeys);
    }

    private static Set<ForeignKey> readForeignKeys(SupportSQLiteDatabase database,
            String tableName) {
        Set<ForeignKey> foreignKeys = new HashSet<>();
        // this seems to return everything in order but it is not documented so better be safe
        Cursor cursor = database.query("PRAGMA foreign_key_list(`" + tableName + "`)");
        try {
            final int idColumnIndex = cursor.getColumnIndex("id");
            final int seqColumnIndex = cursor.getColumnIndex("seq");
            final int tableColumnIndex = cursor.getColumnIndex("table");
            final int onDeleteColumnIndex = cursor.getColumnIndex("on_delete");
            final int onUpdateColumnIndex = cursor.getColumnIndex("on_update");

            final List<ForeignKeyWithSequence> ordered = readForeignKeyFieldMappings(cursor);
            final int count = cursor.getCount();
            for (int position = 0; position < count; position++) {
                cursor.moveToPosition(position);
                final int seq = cursor.getInt(seqColumnIndex);
                if (seq != 0) {
                    continue;
                }
                final int id = cursor.getInt(idColumnIndex);
                List<String> myColumns = new ArrayList<>();
                List<String> refColumns = new ArrayList<>();
                for (ForeignKeyWithSequence key : ordered) {
                    if (key.mId == id) {
                        myColumns.add(key.mFrom);
                        refColumns.add(key.mTo);
                    }
                }
                foreignKeys.add(new ForeignKey(
                        cursor.getString(tableColumnIndex),
                        cursor.getString(onDeleteColumnIndex),
                        cursor.getString(onUpdateColumnIndex),
                        myColumns,
                        refColumns
                ));
            }
        } finally {
            cursor.close();
        }
        return foreignKeys;
    }

    private static List<ForeignKeyWithSequence> readForeignKeyFieldMappings(Cursor cursor) {
        final int idColumnIndex = cursor.getColumnIndex("id");
        final int seqColumnIndex = cursor.getColumnIndex("seq");
        final int fromColumnIndex = cursor.getColumnIndex("from");
        final int toColumnIndex = cursor.getColumnIndex("to");
        final int count = cursor.getCount();
        List<ForeignKeyWithSequence> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            result.add(new ForeignKeyWithSequence(
                    cursor.getInt(idColumnIndex),
                    cursor.getInt(seqColumnIndex),
                    cursor.getString(fromColumnIndex),
                    cursor.getString(toColumnIndex)
            ));
        }
        Collections.sort(result);
        return result;
    }

    private static Map<String, Column> readColumns(SupportSQLiteDatabase database,
            String tableName) {
        Cursor cursor = database
                .query("PRAGMA table_info(`" + tableName + "`)");
        //noinspection TryFinallyCanBeTryWithResources
        Map<String, Column> columns = new HashMap<>();
        try {
            if (cursor.getColumnCount() > 0) {
                int nameIndex = cursor.getColumnIndex("name");
                int typeIndex = cursor.getColumnIndex("type");
                int notNullIndex = cursor.getColumnIndex("notnull");
                int pkIndex = cursor.getColumnIndex("pk");

                while (cursor.moveToNext()) {
                    final String name = cursor.getString(nameIndex);
                    final String type = cursor.getString(typeIndex);
                    final boolean notNull = 0 != cursor.getInt(notNullIndex);
                    final int primaryKeyPosition = cursor.getInt(pkIndex);
                    columns.put(name, new Column(name, type, notNull, primaryKeyPosition));
                }
            }
        } finally {
            cursor.close();
        }
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableInfo tableInfo = (TableInfo) o;

        if (!name.equals(tableInfo.name)) return false;
        //noinspection SimplifiableIfStatement
        if (!columns.equals(tableInfo.columns)) return false;
        return foreignKeys.equals(tableInfo.foreignKeys);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + columns.hashCode();
        result = 31 * result + foreignKeys.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TableInfo{"
                + "name='" + name + '\''
                + ", columns=" + columns
                + ", foreignKeys=" + foreignKeys
                + '}';
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
         * Whether or not the column can be NULL.
         */
        public final boolean notNull;
        /**
         * The position of the column in the list of primary keys, 0 if the column is not part
         * of the primary key.
         * <p>
         * This information is only available in API 20+.
         * <a href="https://www.sqlite.org/releaselog/3_7_16_2.html">(SQLite version 3.7.16.2)</a>
         * On older platforms, it will be 1 if the column is part of the primary key and 0
         * otherwise.
         * <p>
         * The {@link #equals(Object)} implementation handles this inconsistency based on
         * API levels os if you are using a custom SQLite deployment, it may return false
         * positives.
         */
        public final int primaryKeyPosition;

        // if you change this constructor, you must change TableInfoWriter.kt
        public Column(String name, String type, boolean notNull, int primaryKeyPosition) {
            this.name = name;
            this.type = type;
            this.notNull = notNull;
            this.primaryKeyPosition = primaryKeyPosition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Column column = (Column) o;
            if (Build.VERSION.SDK_INT >= 20) {
                if (primaryKeyPosition != column.primaryKeyPosition) return false;
            } else {
                if (isPrimaryKey() != column.isPrimaryKey()) return false;
            }

            if (!name.equals(column.name)) return false;
            //noinspection SimplifiableIfStatement
            if (notNull != column.notNull) return false;
            return type != null ? type.equalsIgnoreCase(column.type) : column.type == null;
        }

        /**
         * Returns whether this column is part of the primary key or not.
         *
         * @return True if this column is part of the primary key, false otherwise.
         */
        public boolean isPrimaryKey() {
            return primaryKeyPosition > 0;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (notNull ? 1231 : 1237);
            result = 31 * result + primaryKeyPosition;
            return result;
        }

        @Override
        public String toString() {
            return "Column{"
                    + "name='" + name + '\''
                    + ", type='" + type + '\''
                    + ", notNull=" + notNull
                    + ", primaryKeyPosition=" + primaryKeyPosition
                    + '}';
        }
    }

    /**
     * Holds the information about an SQLite foreign key
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class ForeignKey {
        @NonNull
        public final String referenceTable;
        @NonNull
        public final String onDelete;
        @NonNull
        public final String onUpdate;
        @NonNull
        public final List<String> columnNames;
        @NonNull
        public final List<String> referenceColumnNames;

        public ForeignKey(@NonNull String referenceTable, @NonNull String onDelete,
                @NonNull String onUpdate,
                @NonNull List<String> columnNames, @NonNull List<String> referenceColumnNames) {
            this.referenceTable = referenceTable;
            this.onDelete = onDelete;
            this.onUpdate = onUpdate;
            this.columnNames = Collections.unmodifiableList(columnNames);
            this.referenceColumnNames = Collections.unmodifiableList(referenceColumnNames);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ForeignKey that = (ForeignKey) o;

            if (!referenceTable.equals(that.referenceTable)) return false;
            if (!onDelete.equals(that.onDelete)) return false;
            if (!onUpdate.equals(that.onUpdate)) return false;
            //noinspection SimplifiableIfStatement
            if (!columnNames.equals(that.columnNames)) return false;
            return referenceColumnNames.equals(that.referenceColumnNames);
        }

        @Override
        public int hashCode() {
            int result = referenceTable.hashCode();
            result = 31 * result + onDelete.hashCode();
            result = 31 * result + onUpdate.hashCode();
            result = 31 * result + columnNames.hashCode();
            result = 31 * result + referenceColumnNames.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "ForeignKey{"
                    + "referenceTable='" + referenceTable + '\''
                    + ", onDelete='" + onDelete + '\''
                    + ", onUpdate='" + onUpdate + '\''
                    + ", columnNames=" + columnNames
                    + ", referenceColumnNames=" + referenceColumnNames
                    + '}';
        }
    }

    /**
     * Temporary data holder for a foreign key row in the pragma result. We need this to ensure
     * sorting in the generated foreign key object.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static class ForeignKeyWithSequence implements Comparable<ForeignKeyWithSequence> {
        final int mId;
        final int mSequence;
        final String mFrom;
        final String mTo;

        ForeignKeyWithSequence(int id, int sequence, String from, String to) {
            mId = id;
            mSequence = sequence;
            mFrom = from;
            mTo = to;
        }

        @Override
        public int compareTo(ForeignKeyWithSequence o) {
            final int idCmp = mId - o.mId;
            if (idCmp == 0) {
                return mSequence - o.mSequence;
            } else {
                return idCmp;
            }
        }
    }
}

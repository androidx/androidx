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

package androidx.room.util;

import android.database.Cursor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.room.ColumnInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
@SuppressWarnings({"WeakerAccess", "unused", "TryFinallyCanBeTryWithResources",
        "SimplifiableIfStatement"})
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

    /**
     * Sometimes, Index information is not available (older versions). If so, we skip their
     * verification.
     */
    @Nullable
    public final Set<Index> indices;

    @SuppressWarnings("unused")
    public TableInfo(String name, Map<String, Column> columns, Set<ForeignKey> foreignKeys,
            Set<Index> indices) {
        this.name = name;
        this.columns = Collections.unmodifiableMap(columns);
        this.foreignKeys = Collections.unmodifiableSet(foreignKeys);
        this.indices = indices == null ? null : Collections.unmodifiableSet(indices);
    }

    /**
     * For backward compatibility with dbs created with older versions.
     */
    @SuppressWarnings("unused")
    public TableInfo(String name, Map<String, Column> columns, Set<ForeignKey> foreignKeys) {
        this(name, columns, foreignKeys, Collections.<Index>emptySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableInfo tableInfo = (TableInfo) o;

        if (name != null ? !name.equals(tableInfo.name) : tableInfo.name != null) return false;
        if (columns != null ? !columns.equals(tableInfo.columns) : tableInfo.columns != null) {
            return false;
        }
        if (foreignKeys != null ? !foreignKeys.equals(tableInfo.foreignKeys)
                : tableInfo.foreignKeys != null) {
            return false;
        }
        if (indices == null || tableInfo.indices == null) {
            // if one us is missing index information, seems like we couldn't acquire the
            // information so we better skip.
            return true;
        }
        return indices.equals(tableInfo.indices);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        result = 31 * result + (foreignKeys != null ? foreignKeys.hashCode() : 0);
        // skip index, it is not reliable for comparison.
        return result;
    }

    @Override
    public String toString() {
        return "TableInfo{"
                + "name='" + name + '\''
                + ", columns=" + columns
                + ", foreignKeys=" + foreignKeys
                + ", indices=" + indices
                + '}';
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
        Set<Index> indices = readIndices(database, tableName);
        return new TableInfo(tableName, columns, foreignKeys, indices);
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

    /**
     * @return null if we cannot read the indices due to older sqlite implementations.
     */
    @Nullable
    private static Set<Index> readIndices(SupportSQLiteDatabase database, String tableName) {
        Cursor cursor = database.query("PRAGMA index_list(`" + tableName + "`)");
        try {
            final int nameColumnIndex = cursor.getColumnIndex("name");
            final int originColumnIndex = cursor.getColumnIndex("origin");
            final int uniqueIndex = cursor.getColumnIndex("unique");
            if (nameColumnIndex == -1 || originColumnIndex == -1 || uniqueIndex == -1) {
                // we cannot read them so better not validate any index.
                return null;
            }
            HashSet<Index> indices = new HashSet<>();
            while (cursor.moveToNext()) {
                String origin = cursor.getString(originColumnIndex);
                if (!"c".equals(origin)) {
                    // Ignore auto-created indices
                    continue;
                }
                String name = cursor.getString(nameColumnIndex);
                boolean unique = cursor.getInt(uniqueIndex) == 1;
                Index index = readIndex(database, name, unique);
                if (index == null) {
                    // we cannot read it properly so better not read it
                    return null;
                }
                indices.add(index);
            }
            return indices;
        } finally {
            cursor.close();
        }
    }

    /**
     * @return null if we cannot read the index due to older sqlite implementations.
     */
    @Nullable
    private static Index readIndex(SupportSQLiteDatabase database, String name, boolean unique) {
        Cursor cursor = database.query("PRAGMA index_xinfo(`" + name + "`)");
        try {
            final int seqnoColumnIndex = cursor.getColumnIndex("seqno");
            final int cidColumnIndex = cursor.getColumnIndex("cid");
            final int nameColumnIndex = cursor.getColumnIndex("name");
            if (seqnoColumnIndex == -1 || cidColumnIndex == -1 || nameColumnIndex == -1) {
                // we cannot read them so better not validate any index.
                return null;
            }
            final TreeMap<Integer, String> results = new TreeMap<>();

            while (cursor.moveToNext()) {
                int cid = cursor.getInt(cidColumnIndex);
                if (cid < 0) {
                    // Ignore SQLite row ID
                    continue;
                }
                int seq = cursor.getInt(seqnoColumnIndex);
                String columnName = cursor.getString(nameColumnIndex);
                results.put(seq, columnName);
            }
            final List<String> columns = new ArrayList<>(results.size());
            columns.addAll(results.values());
            return new Index(name, unique, columns);
        } finally {
            cursor.close();
        }
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
         * The column type after it is normalized to one of the basic types according to
         * https://www.sqlite.org/datatype3.html Section 3.1.
         * <p>
         * This is the value Room uses for equality check.
         */
        @ColumnInfo.SQLiteTypeAffinity
        public final int affinity;
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
            this.affinity = findAffinity(type);
        }

        /**
         * Implements https://www.sqlite.org/datatype3.html section 3.1
         *
         * @param type The type that was given to the sqlite
         * @return The normalized type which is one of the 5 known affinities
         */
        @ColumnInfo.SQLiteTypeAffinity
        private static int findAffinity(@Nullable String type) {
            if (type == null) {
                return ColumnInfo.BLOB;
            }
            String uppercaseType = type.toUpperCase(Locale.US);
            if (uppercaseType.contains("INT")) {
                return ColumnInfo.INTEGER;
            }
            if (uppercaseType.contains("CHAR")
                    || uppercaseType.contains("CLOB")
                    || uppercaseType.contains("TEXT")) {
                return ColumnInfo.TEXT;
            }
            if (uppercaseType.contains("BLOB")) {
                return ColumnInfo.BLOB;
            }
            if (uppercaseType.contains("REAL")
                    || uppercaseType.contains("FLOA")
                    || uppercaseType.contains("DOUB")) {
                return ColumnInfo.REAL;
            }
            // sqlite returns NUMERIC here but it is like a catch all. We already
            // have UNDEFINED so it is better to use UNDEFINED for consistency.
            return ColumnInfo.UNDEFINED;
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
            return affinity == column.affinity;
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
            result = 31 * result + affinity;
            result = 31 * result + (notNull ? 1231 : 1237);
            result = 31 * result + primaryKeyPosition;
            return result;
        }

        @Override
        public String toString() {
            return "Column{"
                    + "name='" + name + '\''
                    + ", type='" + type + '\''
                    + ", affinity='" + affinity + '\''
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
        public int compareTo(@NonNull ForeignKeyWithSequence o) {
            final int idCmp = mId - o.mId;
            if (idCmp == 0) {
                return mSequence - o.mSequence;
            } else {
                return idCmp;
            }
        }
    }

    /**
     * Holds the information about an SQLite index
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Index {
        // should match the value in Index.kt
        public static final String DEFAULT_PREFIX = "index_";
        public final String name;
        public final boolean unique;
        public final List<String> columns;

        public Index(String name, boolean unique, List<String> columns) {
            this.name = name;
            this.unique = unique;
            this.columns = columns;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Index index = (Index) o;
            if (unique != index.unique) {
                return false;
            }
            if (!columns.equals(index.columns)) {
                return false;
            }
            if (name.startsWith(Index.DEFAULT_PREFIX)) {
                return index.name.startsWith(Index.DEFAULT_PREFIX);
            } else {
                return name.equals(index.name);
            }
        }

        @Override
        public int hashCode() {
            int result;
            if (name.startsWith(DEFAULT_PREFIX)) {
                result = DEFAULT_PREFIX.hashCode();
            } else {
                result = name.hashCode();
            }
            result = 31 * result + (unique ? 1 : 0);
            result = 31 * result + columns.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Index{"
                    + "name='" + name + '\''
                    + ", unique=" + unique
                    + ", columns=" + columns
                    + '}';
        }
    }
}

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

package android.arch.persistence.room.migration.bundle;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Holds the information about a foreign key reference.
 */
public class ForeignKeyBundle {
    @SerializedName("table")
    private String mTable;
    @SerializedName("onDelete")
    private String mOnDelete;
    @SerializedName("onUpdate")
    private String mOnUpdate;
    @SerializedName("columns")
    private List<String> mColumns;
    @SerializedName("referencedColumns")
    private List<String> mReferencedColumns;

    /**
     * Creates a foreign key bundle with the given parameters.
     *
     * @param table The target table
     * @param onDelete OnDelete action
     * @param onUpdate OnUpdate action
     * @param columns The list of columns in the current table
     * @param referencedColumns The list of columns in the referenced table
     */
    public ForeignKeyBundle(String table, String onDelete, String onUpdate,
            List<String> columns, List<String> referencedColumns) {
        mTable = table;
        mOnDelete = onDelete;
        mOnUpdate = onUpdate;
        mColumns = columns;
        mReferencedColumns = referencedColumns;
    }

    /**
     * Returns the table name
     *
     * @return Returns the table name
     */
    public String getTable() {
        return mTable;
    }

    /**
     * Returns the SQLite foreign key action that will be performed when referenced row is deleted.
     *
     * @return The SQLite on delete action
     */
    public String getOnDelete() {
        return mOnDelete;
    }

    /**
     * Returns the SQLite foreign key action that will be performed when referenced row is updated.
     *
     * @return The SQLite on update action
     */
    public String getOnUpdate() {
        return mOnUpdate;
    }

    /**
     * Returns the ordered list of columns in the current table.
     *
     * @return The list of columns in the current entity.
     */
    public List<String> getColumns() {
        return mColumns;
    }

    /**
     * Returns the ordered list of columns in the referenced table.
     *
     * @return The list of columns in the referenced entity.
     */
    public List<String> getReferencedColumns() {
        return mReferencedColumns;
    }
}

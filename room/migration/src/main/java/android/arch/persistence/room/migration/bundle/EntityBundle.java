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

import android.support.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data class that holds the schema information about an
 * {@link android.arch.persistence.room.Entity Entity}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EntityBundle {

    static final String NEW_TABLE_PREFIX = "_new_";

    @SerializedName("tableName")
    private String mTableName;
    @SerializedName("createSql")
    private String mCreateSql;
    @SerializedName("fields")
    private List<FieldBundle> mFields;
    @SerializedName("primaryKey")
    private PrimaryKeyBundle mPrimaryKey;
    @SerializedName("indices")
    private List<IndexBundle> mIndices;
    @SerializedName("foreignKeys")
    private List<ForeignKeyBundle> mForeignKeys;

    private transient String mNewTableName;
    private transient Map<String, FieldBundle> mFieldsByColumnName;

    /**
     * Creates a new bundle.
     *
     * @param tableName The table name.
     * @param createSql Create query with the table name placeholder.
     * @param fields The list of fields.
     * @param primaryKey The primary key.
     * @param indices The list of indices
     * @param foreignKeys The list of foreign keys
     */
    public EntityBundle(String tableName, String createSql,
            List<FieldBundle> fields,
            PrimaryKeyBundle primaryKey,
            List<IndexBundle> indices,
            List<ForeignKeyBundle> foreignKeys) {
        mTableName = tableName;
        mCreateSql = createSql;
        mFields = fields;
        mPrimaryKey = primaryKey;
        mIndices = indices;
        mForeignKeys = foreignKeys;
    }

    /**
     * @return The table name if it is created during a table schema modification.
     */
    public String getNewTableName() {
        if (mNewTableName == null) {
            mNewTableName = NEW_TABLE_PREFIX + mTableName;
        }
        return mNewTableName;
    }

    /**
     * @return Map of fields keyed by their column names.
     */
    public Map<String, FieldBundle> getFieldsByColumnName() {
        if (mFieldsByColumnName == null) {
            mFieldsByColumnName = new HashMap<>();
            for (FieldBundle bundle : mFields) {
                mFieldsByColumnName.put(bundle.getColumnName(), bundle);
            }
        }
        return mFieldsByColumnName;
    }

    /**
     * @return The table name.
     */
    public String getTableName() {
        return mTableName;
    }

    /**
     * @return The create query with table name placeholder.
     */
    public String getCreateSql() {
        return mCreateSql;
    }

    /**
     * @return List of fields.
     */
    public List<FieldBundle> getFields() {
        return mFields;
    }

    /**
     * @return The primary key description.
     */
    public PrimaryKeyBundle getPrimaryKey() {
        return mPrimaryKey;
    }

    /**
     * @return List of indices.
     */
    public List<IndexBundle> getIndices() {
        return mIndices;
    }

    /**
     * @return List of foreign keys.
     */
    public List<ForeignKeyBundle> getForeignKeys() {
        return mForeignKeys;
    }

    /**
     * @return Create table SQL query that uses the actual table name.
     */
    public String createTable() {
        return BundleUtil.replaceTableName(mCreateSql, getTableName());
    }

    /**
     * @return Create table SQL query that uses the table name with "new" prefix.
     */
    public String createNewTable() {
        return BundleUtil.replaceTableName(mCreateSql, getNewTableName());
    }

    /**
     * @return Renames the table with {@link #getNewTableName()} to {@link #getTableName()}.
     */
    @NotNull
    public String renameToOriginal() {
        return "ALTER TABLE " + getNewTableName() + " RENAME TO " + getTableName();
    }

    /**
     * @return Creates the list of SQL queries that are necessary to create this entity.
     */
    public Collection<String> buildCreateQueries() {
        List<String> result = new ArrayList<>();
        result.add(createTable());
        for (IndexBundle indexBundle : mIndices) {
            result.add(indexBundle.create(getTableName()));
        }
        return result;
    }
}

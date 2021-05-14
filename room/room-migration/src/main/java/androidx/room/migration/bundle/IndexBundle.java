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

package androidx.room.migration.bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.Index;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Data class that holds the schema information about a table Index.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class IndexBundle implements SchemaEquality<IndexBundle> {
    // should match Index.kt
    public static final String DEFAULT_PREFIX = "index_";
    @SerializedName("name")
    private String mName;
    @SerializedName("unique")
    private boolean mUnique;
    @SerializedName("columnNames")
    private List<String> mColumnNames;
    @SerializedName("orders")
    private List<String> mOrders;
    @SerializedName("createSql")
    private String mCreateSql;

    /**
     * @deprecated Use {@link #IndexBundle(String, boolean, List, List, String)}
     */
    @Deprecated
    public IndexBundle(String name, boolean unique, List<String> columnNames, String createSql) {
        this(name, unique, columnNames, null, createSql);
    }

    public IndexBundle(String name, boolean unique, List<String> columnNames, List<String> orders,
            String createSql) {
        mName = name;
        mUnique = unique;
        mColumnNames = columnNames;
        mOrders = orders;
        mCreateSql = createSql;
    }

    public String getName() {
        return mName;
    }

    public boolean isUnique() {
        return mUnique;
    }

    public List<String> getColumnNames() {
        return mColumnNames;
    }

    public List<String> getOrders() {
        return mOrders;
    }

    /**
     * @param tableName The table name.
     * @return Create index SQL query that uses the given table name.
     */
    public String getCreateSql(String tableName) {
        return BundleUtil.replaceTableName(mCreateSql, tableName);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public String create(@NonNull String tableName) {
        return BundleUtil.replaceTableName(mCreateSql, tableName);
    }

    @Override
    public boolean isSchemaEqual(@NonNull IndexBundle other) {
        if (mUnique != other.mUnique) return false;
        if (mName.startsWith(DEFAULT_PREFIX)) {
            if (!other.mName.startsWith(DEFAULT_PREFIX)) {
                return false;
            }
        } else if (other.mName.startsWith(DEFAULT_PREFIX)) {
            return false;
        } else if (!mName.equals(other.mName)) {
            return false;
        }

        // order matters
        if (mColumnNames != null ? !mColumnNames.equals(other.mColumnNames)
                : other.mColumnNames != null) {
            return false;
        }

        int columnsSize = mColumnNames != null ? mColumnNames.size() : 0;
        List<String> orders = mOrders == null || mOrders.isEmpty()
                ? Collections.nCopies(columnsSize, Index.ASC) : mOrders;
        List<String> otherOrders = other.mOrders == null || other.mOrders.isEmpty()
                ? Collections.nCopies(columnsSize, Index.ASC) : other.mOrders;
        if (!orders.equals(otherOrders)) return false;
        return true;
    }
}

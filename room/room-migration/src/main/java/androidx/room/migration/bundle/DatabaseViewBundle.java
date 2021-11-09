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

package androidx.room.migration.bundle;

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

/**
 * Data class that holds the schema information about a
 * {@link androidx.room.DatabaseView DatabaseView}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class DatabaseViewBundle implements SchemaEquality<DatabaseViewBundle> {

    @SerializedName("viewName")
    private String mViewName;
    @SerializedName("createSql")
    private String mCreateSql;

    public DatabaseViewBundle(String viewName, String createSql) {
        mViewName = viewName;
        mCreateSql = createSql;
    }

    /**
     * @return The name of this view.
     */
    public String getViewName() {
        return mViewName;
    }

    /**
     * @return Create view SQL query.
     */
    public String getCreateSql() {
        return mCreateSql;
    }

    /**
     * @return Create view SQL query that uses the actual view name.
     */
    public String createView() {
        return BundleUtil.replaceViewName(mCreateSql, getViewName());
    }

    @Override
    public boolean isSchemaEqual(DatabaseViewBundle other) {
        return mViewName != null && mViewName.equals(other.mViewName)
                && mCreateSql != null && mCreateSql.equals(other.mCreateSql);
    }
}

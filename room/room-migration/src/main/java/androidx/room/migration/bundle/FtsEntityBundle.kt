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

import static androidx.room.migration.bundle.SchemaEqualityUtil.checkSchemaEquality;

import androidx.annotation.RestrictTo;
import androidx.room.Fts3;
import androidx.room.Fts4;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Data class that holds the schema information about an {@link Fts3 FTS3} or {@link Fts4 FTS4}
 * entity.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FtsEntityBundle extends EntityBundle {

    private static final String [] SHADOW_TABLE_NAME_SUFFIXES = {
            "_content", "_segdir", "_segments", "_stat", "_docsize"
    };

    @SerializedName("ftsVersion")
    private final String mFtsVersion;

    @SerializedName("ftsOptions")
    private final FtsOptionsBundle mFtsOptions;

    @SerializedName("contentSyncTriggers")
    private final List<String> mContentSyncSqlTriggers;

    public FtsEntityBundle(
            String tableName,
            String createSql,
            List<FieldBundle> fields,
            PrimaryKeyBundle primaryKey,
            String ftsVersion,
            FtsOptionsBundle ftsOptions,
            List<String> contentSyncSqlTriggers) {
        super(tableName, createSql, fields, primaryKey, Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        mFtsVersion = ftsVersion;
        mFtsOptions = ftsOptions;
        mContentSyncSqlTriggers = contentSyncSqlTriggers;
    }

    /**
     * @return the FTS options.
     */
    public FtsOptionsBundle getFtsOptions() {
        return mFtsOptions;
    }

    /**
     * @return Creates the list of SQL queries that are necessary to create this entity.
     */
    @Override
    public Collection<String> buildCreateQueries() {
        List<String> result = new ArrayList<>();
        result.add(createTable());
        result.addAll(mContentSyncSqlTriggers);
        return result;
    }

    @Override
    public boolean isSchemaEqual(EntityBundle other) {
        boolean isSuperSchemaEqual = super.isSchemaEqual(other);
        if (other instanceof FtsEntityBundle) {
            FtsEntityBundle otherFtsBundle = (FtsEntityBundle) other;
            return isSuperSchemaEqual
                    && mFtsVersion.equals(otherFtsBundle.mFtsVersion)
                    && checkSchemaEquality(mFtsOptions, otherFtsBundle.mFtsOptions);
        } else {
            return isSuperSchemaEqual;
        }
    }

    /**
     * Gets the list of shadow table names corresponding to the FTS virtual table.
     * @return the list of names.
     */
    public List<String> getShadowTableNames() {
        List<String> names = new ArrayList<>(SHADOW_TABLE_NAME_SUFFIXES.length);
        for (String suffix : SHADOW_TABLE_NAME_SUFFIXES) {
            names.add(getTableName() + suffix);
        }
        return names;
    }
}

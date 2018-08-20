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
import androidx.room.Fts3Entity;
import androidx.room.Fts4Entity;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data class that holds the schema information about an
 * {@link Fts3Entity Fts3Entity} or {@link Fts4Entity Fts4Entity}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FtsEntityBundle extends EntityBundle {

    private static final String [] SHADOW_TABLE_NAME_SUFFIXES = {
            "_content", "_segdir", "_segments", "_stat", "_docsize"
    };

    @SerializedName("ftsVersion")
    private final String mFtsVersion;

    @SerializedName("ftsOptions")
    private final FtsOptionsBundle mFtsOptions;

    public FtsEntityBundle(
            String tableName,
            String createSql,
            List<FieldBundle> fields,
            PrimaryKeyBundle primaryKey,
            String ftsVersion,
            FtsOptionsBundle ftsOptions) {
        super(tableName, createSql, fields, primaryKey, Collections.<IndexBundle>emptyList(),
                Collections.<ForeignKeyBundle>emptyList());
        mFtsVersion = ftsVersion;
        mFtsOptions = ftsOptions;
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

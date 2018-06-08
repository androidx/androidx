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

import androidx.annotation.RestrictTo;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data class that holds the schema information for a
 * {@link androidx.room.Database Database}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DatabaseBundle implements SchemaEquality<DatabaseBundle> {
    @SerializedName("version")
    private int mVersion;
    @SerializedName("identityHash")
    private String mIdentityHash;
    @SerializedName("entities")
    private List<EntityBundle> mEntities;
    // then entity where we keep room information
    @SerializedName("setupQueries")
    private List<String> mSetupQueries;
    private transient Map<String, EntityBundle> mEntitiesByTableName;

    /**
     * Creates a new database
     * @param version Version
     * @param identityHash Identity hash
     * @param entities List of entities
     */
    public DatabaseBundle(int version, String identityHash, List<EntityBundle> entities,
            List<String> setupQueries) {
        mVersion = version;
        mIdentityHash = identityHash;
        mEntities = entities;
        mSetupQueries = setupQueries;
    }

    /**
     * @return The identity has of the Database.
     */
    public String getIdentityHash() {
        return mIdentityHash;
    }

    /**
     * @return The database version.
     */
    public int getVersion() {
        return mVersion;
    }

    /**
     * @return List of entities.
     */
    public List<EntityBundle> getEntities() {
        return mEntities;
    }

    /**
     * @return Map of entities, keyed by table name.
     */
    @SuppressWarnings("unused")
    public Map<String, EntityBundle> getEntitiesByTableName() {
        if (mEntitiesByTableName == null) {
            mEntitiesByTableName = new HashMap<>();
            for (EntityBundle bundle : mEntities) {
                mEntitiesByTableName.put(bundle.getTableName(), bundle);
            }
        }
        return mEntitiesByTableName;
    }

    /**
     * @return List of SQL queries to build this database from scratch.
     */
    public List<String> buildCreateQueries() {
        List<String> result = new ArrayList<>();
        for (EntityBundle entityBundle : mEntities) {
            result.addAll(entityBundle.buildCreateQueries());
        }
        result.addAll(mSetupQueries);
        return result;
    }

    @Override
    public boolean isSchemaEqual(DatabaseBundle other) {
        return SchemaEqualityUtil.checkSchemaEquality(getEntitiesByTableName(),
                other.getEntitiesByTableName());
    }
}

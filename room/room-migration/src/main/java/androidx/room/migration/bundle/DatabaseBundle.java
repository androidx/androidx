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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data class that holds the schema information for a
 * {@link androidx.room.Database Database}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class DatabaseBundle implements SchemaEquality<DatabaseBundle> {
    @SerializedName("version")
    private int mVersion;
    @SerializedName("identityHash")
    private String mIdentityHash;
    @SerializedName("entities")
    private List<EntityBundle> mEntities;
    @SerializedName("views")
    private List<DatabaseViewBundle> mViews;
    // then entity where we keep room information
    @SerializedName("setupQueries")
    private List<String> mSetupQueries;
    private transient Map<String, EntityBundle> mEntitiesByTableName;

    /**
     * Creates a new database
     * @param version Version
     * @param identityHash Identity hash
     * @param entities List of entities
     * @param views List of views
     * */
    public DatabaseBundle(int version, String identityHash, List<EntityBundle> entities,
            List<DatabaseViewBundle> views, List<String> setupQueries) {
        mVersion = version;
        mIdentityHash = identityHash;
        mEntities = entities;
        mViews = views;
        mSetupQueries = setupQueries;
    }

    // Used by GSON
    @SuppressWarnings("unused")
    public DatabaseBundle() {
        // Set default values to newly added fields
        mViews = Collections.emptyList();
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
     * @return List of views.
     */
    public List<DatabaseViewBundle> getViews() {
        return mViews;
    }

    /**
     * @return List of SQL queries to build this database from scratch.
     */
    public List<String> buildCreateQueries() {
        List<String> result = new ArrayList<>();
        Collections.sort(mEntities, new FtsEntityCreateComparator());
        for (EntityBundle entityBundle : mEntities) {
            result.addAll(entityBundle.buildCreateQueries());
        }
        for (DatabaseViewBundle viewBundle : mViews) {
            result.add(viewBundle.createView());
        }
        result.addAll(mSetupQueries);
        return result;
    }

    @Override
    public boolean isSchemaEqual(DatabaseBundle other) {
        return SchemaEqualityUtil.checkSchemaEquality(getEntitiesByTableName(),
                other.getEntitiesByTableName());
    }

    // Comparator to sort FTS entities after their declared external content entity so that the
    // content entity table gets created first.
    static final class FtsEntityCreateComparator implements Comparator<EntityBundle> {
        @Override
        public int compare(EntityBundle firstEntity, EntityBundle secondEntity) {
            if (firstEntity instanceof FtsEntityBundle) {
                FtsEntityBundle ftsEntity = (FtsEntityBundle) firstEntity;
                String contentTable = ftsEntity.getFtsOptions().getContentTable();
                if (contentTable.equals(secondEntity.getTableName())) {
                    return 1;
                }
            } else if (secondEntity instanceof FtsEntityBundle) {
                FtsEntityBundle ftsEntity = (FtsEntityBundle) secondEntity;
                String contentTable = ftsEntity.getFtsOptions().getContentTable();
                if (contentTable.equals(firstEntity.getTableName())) {
                    return -1;
                }
            }
            return 0;
        }
    }
}

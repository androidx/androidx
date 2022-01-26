/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.observer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.Set;

/**
 * Contains information about a schema change detected by an {@link AppSearchObserverCallback}.
 *
 * <p>This object will be sent when a schema type having a name matching an observer's schema
 * type filters has been added, updated, or removed.
 *
 * <p>Note that schema changes may cause documents to be migrated or removed. When this happens,
 * individual document updates will NOT be dispatched via {@link DocumentChangeInfo}. The only
 * notification will be of the schema type change via {@link SchemaChangeInfo}. Depending on your
 * use case, you may need to re-query the whole schema type when this happens.
 */
public final class SchemaChangeInfo {
    private final String mPackageName;
    private final String mDatabaseName;
    private final Set<String> mChangedSchemaNames;

    /**
     * Constructs a new {@link SchemaChangeInfo}.
     *
     * @param packageName     The package name of the app which owns the schema that changed.
     * @param databaseName    The database in which the schema that changed resides.
     * @param changedSchemaNames Names of schemas that have changed as part of this notification.
     */
    public SchemaChangeInfo(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull Set<String> changedSchemaNames) {
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        mChangedSchemaNames = Collections.unmodifiableSet(
                Preconditions.checkNotNull(changedSchemaNames));
    }

    /** Returns the package name of the app which owns the schema that changed. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the database in which the schema that was changed resides. */
    @NonNull
    public String getDatabaseName() {
        return mDatabaseName;
    }

    /**
     * Returns the names of schema types affected by this change notification.
     *
     * <p>This will never be empty.
     */
    @NonNull
    public Set<String> getChangedSchemaNames() {
        return mChangedSchemaNames;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaChangeInfo)) return false;
        SchemaChangeInfo that = (SchemaChangeInfo) o;
        return mPackageName.equals(that.mPackageName)
                && mDatabaseName.equals(that.mDatabaseName)
                && mChangedSchemaNames.equals(that.mChangedSchemaNames);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mPackageName, mDatabaseName, mChangedSchemaNames);
    }

    @NonNull
    @Override
    public String toString() {
        return "SchemaChangeInfo{"
                + "packageName='" + mPackageName + '\''
                + ", databaseName='" + mDatabaseName + '\''
                + ", changedSchemaNames='" + mChangedSchemaNames + '\''
                + '}';
    }
}

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
 * Contains information about an individual change detected by an {@link ObserverCallback}.
 *
 * <p>This class reports information about document changes, i.e. when documents were added, updated
 * or removed.
 *
 * <p>Changes are grouped by package, database, schema type and namespace. Each unique
 * combination of these items will generate a unique {@link DocumentChangeInfo}.
 *
 * <p>Note that document changes that happen during schema migration from calling
 * {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync} are not reported via this class.
 * Such changes are reported through {@link SchemaChangeInfo}.
 */
public final class DocumentChangeInfo {
    private final String mPackageName;
    private final String mDatabase;
    private final String mNamespace;
    private final String mSchemaName;
    private final Set<String> mChangedDocumentIds;

    /**
     * Constructs a new {@link DocumentChangeInfo}.
     *
     * @param packageName  The package name of the app which owns the documents that changed.
     * @param database The database in which the documents that changed reside.
     * @param namespace    The namespace in which the documents that changed reside.
     * @param schemaName   The name of the schema type that contains the changed documents.
     * @param changedDocumentIds The set of document IDs that have been changed as part of this
     *                   notification.
     */
    public DocumentChangeInfo(
            @NonNull String packageName,
            @NonNull String database,
            @NonNull String namespace,
            @NonNull String schemaName,
            @NonNull Set<String> changedDocumentIds) {
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabase = Preconditions.checkNotNull(database);
        mNamespace = Preconditions.checkNotNull(namespace);
        mSchemaName = Preconditions.checkNotNull(schemaName);
        mChangedDocumentIds = Collections.unmodifiableSet(
                Preconditions.checkNotNull(changedDocumentIds));
    }

    /** Returns the package name of the app which owns the documents that changed. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the database in which the documents that was changed reside. */
    @NonNull
    public String getDatabaseName() {
        return mDatabase;
    }

    /** Returns the namespace of the documents that changed. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the name of the schema type that contains the changed documents. */
    @NonNull
    public String getSchemaName() {
        return mSchemaName;
    }

    /**
     * Returns the set of document IDs that have been changed as part of this notification.
     *
     * <p>This will never be empty.
     */
    @NonNull
    public Set<String> getChangedDocumentIds() {
        return mChangedDocumentIds;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentChangeInfo)) return false;
        DocumentChangeInfo that = (DocumentChangeInfo) o;
        return mPackageName.equals(that.mPackageName)
                && mDatabase.equals(that.mDatabase)
                && mNamespace.equals(that.mNamespace)
                && mSchemaName.equals(that.mSchemaName)
                && mChangedDocumentIds.equals(that.mChangedDocumentIds);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                mPackageName, mDatabase, mNamespace, mSchemaName, mChangedDocumentIds);
    }

    @NonNull
    @Override
    public String toString() {
        return "DocumentChangeInfo{"
                + "packageName='" + mPackageName + '\''
                + ", database='" + mDatabase + '\''
                + ", namespace='" + mNamespace + '\''
                + ", schemaName='" + mSchemaName + '\''
                + ", changedDocumentIds='" + mChangedDocumentIds + '\''
                + '}';
    }
}

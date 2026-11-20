/*
 * Copyright 2026 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.ChangeTokenCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.DocumentChangeSetCreator;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Describes a set of documents that changed for a given package, database, namespace and schema
 * type, and when each of those documents changed.
 *
 * <p>A {@link DocumentChangeSet} does not convey what kind of change occurred (a document was
 * added, updated or removed), nor the old or new values of the document. It is up to the caller
 * to use the returned document IDs to look up what has changed.
 *
 * @see androidx.appsearch.app.ReadOnlyGlobalSearchSession#getDocumentChangesAsync
 */
// TODO(b/568469783): unhide the API once offline observers are ready.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@FlaggedApi(Flags.FLAG_ENABLE_OFFLINE_OBSERVERS)
@ExperimentalAppSearchApi
@SafeParcelable.Class(creator = "DocumentChangeSetCreator")
@SuppressWarnings("HiddenSuperclass")
public final class DocumentChangeSet implements SafeParcelable {
    /** Creator class for {@link DocumentChangeSet}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Parcelable.@NonNull Creator<DocumentChangeSet> CREATOR =
            new DocumentChangeSetCreator();

    /** Identifies a single changed document and the time at which it changed. */
    @FlaggedApi(Flags.FLAG_ENABLE_OFFLINE_OBSERVERS)
    @ExperimentalAppSearchApi
    @SafeParcelable.Class(creator = "ChangeTokenCreator")
    @SuppressWarnings("HiddenSuperclass")
    public static final class ChangeToken implements SafeParcelable {
        /** Creator class for {@link ChangeToken}. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final Parcelable.@NonNull Creator<ChangeToken> CREATOR =
                new ChangeTokenCreator();

        @AbstractSafeParcelable.Field(id = 1, getter = "getDocumentId")
        private final String mDocumentId;
        @AbstractSafeParcelable.Field(id = 2, getter = "getOperationTimestampMillis")
        private final long mOperationTimestampMillis;

        /**
         * Constructs a new {@link ChangeToken}.
         *
         * @param documentId               The ID of the changed document.
         * @param operationTimestampMillis The time, in milliseconds since epoch, at which the
         *                                 change operation occurred.
         */
        @AbstractSafeParcelable.Constructor
        public ChangeToken(
                @AbstractSafeParcelable.Param(id = 1) @NonNull String documentId,
                @AbstractSafeParcelable.Param(id = 2) long operationTimestampMillis) {
            mDocumentId = Preconditions.checkNotNull(documentId);
            mOperationTimestampMillis = operationTimestampMillis;
        }

        /** Returns the ID of the changed document. */
        public @NonNull String getDocumentId() {
            return mDocumentId;
        }

        /**
         * Returns the time, in milliseconds since epoch, at which the change operation occurred.
         */
        public long getOperationTimestampMillis() {
            return mOperationTimestampMillis;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChangeToken)) {
                return false;
            }
            ChangeToken that = (ChangeToken) o;
            return mDocumentId.equals(that.mDocumentId)
                    && mOperationTimestampMillis == that.mOperationTimestampMillis;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mDocumentId, mOperationTimestampMillis);
        }

        @Override
        public @NonNull String toString() {
            return "ChangeToken{"
                    + "documentId='" + mDocumentId + '\''
                    + ", operationTimestampMillis=" + mOperationTimestampMillis
                    + '}';
        }

        /**
         * Always returns 0 because this class does not contain file descriptors.
         *
         * <p>Defined here instead of inheriting it from {@code AbstractSafeParcelable}, whose final
         * {@code describeContents()} carries an unrelated, already finalized API flag.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Override
        public int describeContents() {
            return 0;
        }

        /** Writes this object to the given {@link Parcel}. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            ChangeTokenCreator.writeToParcel(this, dest, flags);
        }
    }

    @AbstractSafeParcelable.Field(id = 1, getter = "getPackageName")
    private final String mPackageName;
    @AbstractSafeParcelable.Field(id = 2, getter = "getDatabaseName")
    private final String mDatabaseName;
    @AbstractSafeParcelable.Field(id = 3, getter = "getNamespace")
    private final String mNamespace;
    @AbstractSafeParcelable.Field(id = 4, getter = "getSchemaName")
    private final String mSchemaName;
    @AbstractSafeParcelable.Field(id = 5, getter = "getChangeTokens")
    private final List<ChangeToken> mChangeTokens;

    /**
     * Constructs a new {@link DocumentChangeSet}.
     *
     * @param packageName  The package name of the app which owns the documents that changed.
     * @param databaseName The database in which the documents that changed reside.
     * @param namespace    The namespace in which the documents that changed reside.
     * @param schemaName   The name of the schema type of the changed documents.
     * @param changeTokens The list of {@link ChangeToken}s describing which documents changed and
     *                     when, in chronological order of
     *                     {@link ChangeToken#getOperationTimestampMillis}.
     */
    @AbstractSafeParcelable.Constructor
    public DocumentChangeSet(
            @AbstractSafeParcelable.Param(id = 1) @NonNull String packageName,
            @AbstractSafeParcelable.Param(id = 2) @NonNull String databaseName,
            @AbstractSafeParcelable.Param(id = 3) @NonNull String namespace,
            @AbstractSafeParcelable.Param(id = 4) @NonNull String schemaName,
            @AbstractSafeParcelable.Param(id = 5) @NonNull List<ChangeToken> changeTokens) {
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        mNamespace = Preconditions.checkNotNull(namespace);
        mSchemaName = Preconditions.checkNotNull(schemaName);
        mChangeTokens = Collections.unmodifiableList(Preconditions.checkNotNull(changeTokens));
    }

    /** Returns the package name of the app which owns the documents that changed. */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /** Returns the database in which the documents that changed reside. */
    public @NonNull String getDatabaseName() {
        return mDatabaseName;
    }

    /** Returns the namespace of the documents that changed. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the name of the schema type of the documents that changed. */
    public @NonNull String getSchemaName() {
        return mSchemaName;
    }

    /**
     * Returns the list of {@link ChangeToken}s describing which documents changed and when.
     *
     * <p>The tokens are in chronological order of
     * {@link ChangeToken#getOperationTimestampMillis}. Each document appears at most once.
     */
    public @NonNull List<ChangeToken> getChangeTokens() {
        return mChangeTokens;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentChangeSet)) {
            return false;
        }
        DocumentChangeSet that = (DocumentChangeSet) o;
        return mPackageName.equals(that.mPackageName)
                && mDatabaseName.equals(that.mDatabaseName)
                && mNamespace.equals(that.mNamespace)
                && mSchemaName.equals(that.mSchemaName)
                && mChangeTokens.equals(that.mChangeTokens);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                mPackageName, mDatabaseName, mNamespace, mSchemaName, mChangeTokens);
    }

    @Override
    public @NonNull String toString() {
        return "DocumentChangeSet{"
                + "packageName='" + mPackageName + '\''
                + ", databaseName='" + mDatabaseName + '\''
                + ", namespace='" + mNamespace + '\''
                + ", schemaName='" + mSchemaName + '\''
                + ", changeTokens=" + mChangeTokens
                + '}';
    }

    /**
     * Always returns 0 because this class does not contain file descriptors.
     *
     * <p>Defined here instead of inheriting it from {@code AbstractSafeParcelable}, whose final
     * {@code describeContents()} carries an unrelated, already finalized API flag.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public int describeContents() {
        return 0;
    }

    /** Writes this object to the given {@link Parcel}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        DocumentChangeSetCreator.writeToParcel(this, dest, flags);
    }
}

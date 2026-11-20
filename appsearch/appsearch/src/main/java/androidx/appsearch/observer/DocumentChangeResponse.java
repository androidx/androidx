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
import androidx.appsearch.safeparcel.stub.StubCreators.DocumentChangeResponseCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.PaginationTokenCreator;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * The response of
 * {@code androidx.appsearch.app.ReadOnlyGlobalSearchSession#getDocumentChangesAsync}, describing
 * the documents that changed after a given point in time.
 *
 * <p>Results are paginated. If {@link #hasMoreChanges} is {@code true}, call
 * {@code androidx.appsearch.app.ReadOnlyGlobalSearchSession#getDocumentChangesAsync} again with
 * {@link #getNextPaginationToken} to retrieve the next page of changes. The
 * {@link #getNextPaginationToken} is always populated, and can be saved by the caller to later
 * retrieve changes that occur after this call.
 *
 * <p>Retrieving changes is idempotent: retrieving changes again with the same arguments returns
 * the same changes, plus any that have happened since. Callers should therefore be prepared to
 * process the same change more than once.
 */
// TODO(b/568469783): unhide the API once offline observers are ready.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@FlaggedApi(Flags.FLAG_ENABLE_OFFLINE_OBSERVERS)
@ExperimentalAppSearchApi
@SafeParcelable.Class(creator = "DocumentChangeResponseCreator")
@SuppressWarnings("HiddenSuperclass")
public final class DocumentChangeResponse implements SafeParcelable {
    /** Creator class for {@link DocumentChangeResponse}. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Parcelable.@NonNull Creator<DocumentChangeResponse> CREATOR =
            new DocumentChangeResponseCreator();

    /**
     * An opaque token used to retrieve a page of document changes from
     * {@code androidx.appsearch.app.ReadOnlyGlobalSearchSession#getDocumentChangesAsync}.
     *
     * <p>To start retrieving changes, construct a {@link PaginationToken} with the time from which
     * changes are wanted. To continue retrieving changes, pass the
     * {@link DocumentChangeResponse#getNextPaginationToken} of the previous response.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_OFFLINE_OBSERVERS)
    @ExperimentalAppSearchApi
    @SafeParcelable.Class(creator = "PaginationTokenCreator")
    @SuppressWarnings("HiddenSuperclass")
    public static final class PaginationToken implements SafeParcelable {
        /** Creator class for {@link PaginationToken}. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final Parcelable.@NonNull Creator<PaginationToken> CREATOR =
                new PaginationTokenCreator();

        @AbstractSafeParcelable.Field(id = 1, getter = "getLastSyncTimestampMillis")
        private final long mLastSyncTimestampMillis;

        /**
         * Constructs a new {@link PaginationToken}.
         *
         * @param lastSyncTimestampMillis Changes that occurred after this time, in milliseconds
         *                                since epoch, will be returned.
         */
        @AbstractSafeParcelable.Constructor
        public PaginationToken(@AbstractSafeParcelable.Param(id = 1) long lastSyncTimestampMillis) {
            mLastSyncTimestampMillis = lastSyncTimestampMillis;
        }

        /**
         * Returns the time, in milliseconds since epoch, after which changes will be returned.
         */
        public long getLastSyncTimestampMillis() {
            return mLastSyncTimestampMillis;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PaginationToken)) {
                return false;
            }
            PaginationToken that = (PaginationToken) o;
            return mLastSyncTimestampMillis == that.mLastSyncTimestampMillis;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mLastSyncTimestampMillis);
        }

        @Override
        public @NonNull String toString() {
            return "PaginationToken{lastSyncTimestampMillis=" + mLastSyncTimestampMillis + '}';
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
            PaginationTokenCreator.writeToParcel(this, dest, flags);
        }
    }

    @AbstractSafeParcelable.Field(id = 1, getter = "getDocumentChangeSets")
    private final List<DocumentChangeSet> mDocumentChangeSets;
    @AbstractSafeParcelable.Field(id = 2, getter = "hasMoreChanges")
    private final boolean mHasMoreChanges;
    @AbstractSafeParcelable.Field(id = 3, getter = "getNextPaginationToken")
    private final PaginationToken mNextPaginationToken;

    /**
     * Constructs a new {@link DocumentChangeResponse}.
     *
     * @param documentChangeSets  The sets of changed documents, grouped by package, database,
     *                            namespace and schema type.
     * @param hasMoreChanges      Whether there are more changes available to retrieve.
     * @param nextPaginationToken The token to pass to the next call to retrieve the next page of
     *                            changes.
     */
    @AbstractSafeParcelable.Constructor
    public DocumentChangeResponse(
            @AbstractSafeParcelable.Param(id = 1)
                    @NonNull List<DocumentChangeSet> documentChangeSets,
            @AbstractSafeParcelable.Param(id = 2) boolean hasMoreChanges,
            @AbstractSafeParcelable.Param(id = 3) @NonNull PaginationToken nextPaginationToken) {
        mDocumentChangeSets = Collections.unmodifiableList(
                Preconditions.checkNotNull(documentChangeSets));
        mHasMoreChanges = hasMoreChanges;
        mNextPaginationToken = Preconditions.checkNotNull(nextPaginationToken);
    }

    /**
     * Returns the sets of changed documents, grouped by package, database, namespace and schema
     * type.
     *
     * <p>This may be empty if there are no changes.
     */
    public @NonNull List<DocumentChangeSet> getDocumentChangeSets() {
        return mDocumentChangeSets;
    }

    /**
     * Returns whether there are more changes available.
     *
     * <p>If {@code true}, use {@link #getNextPaginationToken} to retrieve the next page of
     * changes.
     */
    public boolean hasMoreChanges() {
        return mHasMoreChanges;
    }

    /**
     * Returns the token to pass to the next call of
     * {@code androidx.appsearch.app.ReadOnlyGlobalSearchSession#getDocumentChangesAsync} to
     * retrieve the next page of changes, or changes that happen after this call.
     */
    public @NonNull PaginationToken getNextPaginationToken() {
        return mNextPaginationToken;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentChangeResponse)) {
            return false;
        }
        DocumentChangeResponse that = (DocumentChangeResponse) o;
        return mHasMoreChanges == that.mHasMoreChanges
                && mDocumentChangeSets.equals(that.mDocumentChangeSets)
                && mNextPaginationToken.equals(that.mNextPaginationToken);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mDocumentChangeSets, mHasMoreChanges, mNextPaginationToken);
    }

    @Override
    public @NonNull String toString() {
        return "DocumentChangeResponse{"
                + "documentChangeSets=" + mDocumentChangeSets
                + ", hasMoreChanges=" + mHasMoreChanges
                + ", nextPaginationToken=" + mNextPaginationToken
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
        DocumentChangeResponseCreator.writeToParcel(this, dest, flags);
    }
}

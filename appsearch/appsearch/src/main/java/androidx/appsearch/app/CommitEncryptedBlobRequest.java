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

package androidx.appsearch.app;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.CommitEncryptedBlobRequestCreator;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates a request to commit a batch of encrypted blobs along with their encryption
 * parameters via {@link AppSearchSession#commitEncryptedBlobAsync}.
 *
 * <p>All blobs in a request are encrypted with the same symmetric Data Encryption Key (DEK). Each
 * blob may have its own content encryption (AEAD) parameters.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@FlaggedApi(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
// NonCanonicalType: Field, Constructor and Param are declared in AbstractSafeParcelable in
// Jetpack and in SafeParcelable in the framework.
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness", "NonCanonicalType"})
@SafeParcelable.Class(creator = "CommitEncryptedBlobRequestCreator")
public final class CommitEncryptedBlobRequest implements SafeParcelable {

    /** Creator class for {@link androidx.appsearch.app.CommitEncryptedBlobRequest}. */
    public static final @NonNull Parcelable.Creator<CommitEncryptedBlobRequest> CREATOR =
            new CommitEncryptedBlobRequestCreator();

    @AbstractSafeParcelable.Field(id = 1)
    final @NonNull byte[] mSymmetricKey;

    @AbstractSafeParcelable.Field(id = 2)
    final @NonNull List<AppSearchBlobHandle> mBlobHandles;

    /**
     * The AEAD parameters of the blob at the same index in {@link #mBlobHandles}. An element is
     * {@code null} if the blob has no AEAD parameters.
     */
    @AbstractSafeParcelable.Field(id = 3)
    final @NonNull byte[][] mAeadParams;

    @AbstractSafeParcelable.Field(id = 4)
    final @NonNull byte[] mOwnerTag;

    @AbstractSafeParcelable.Constructor
    CommitEncryptedBlobRequest(
            @AbstractSafeParcelable.Param(id = 1) @NonNull byte[] symmetricKey,
            @AbstractSafeParcelable.Param(id = 2) @NonNull List<AppSearchBlobHandle> blobHandles,
            @AbstractSafeParcelable.Param(id = 3) @NonNull byte[][] aeadParams,
            @AbstractSafeParcelable.Param(id = 4) @NonNull byte[] ownerTag) {
        Objects.requireNonNull(symmetricKey);
        Objects.requireNonNull(blobHandles);
        Objects.requireNonNull(aeadParams);
        Objects.requireNonNull(ownerTag);
        Preconditions.checkArgument(
                blobHandles.size() == aeadParams.length,
                "blobHandles and aeadParams must have the same size.");
        mSymmetricKey = symmetricKey.clone();
        mBlobHandles = new ArrayList<>(blobHandles);
        mAeadParams = aeadParams.clone();
        mOwnerTag = ownerTag.clone();
    }

    /**
     * Returns the raw symmetric Data Encryption Key (DEK) used to encrypt all blobs in this
     * request.
     */
    public @NonNull byte[] getSymmetricKey() {
        return mSymmetricKey.clone();
    }

    /** Returns the {@link AppSearchBlobHandle}s that identify the blobs to commit. */
    public @NonNull Set<AppSearchBlobHandle> getBlobHandles() {
        return Collections.unmodifiableSet(new ArraySet<>(mBlobHandles));
    }

    /**
     * Returns an unmodifiable map from each {@link AppSearchBlobHandle} to the serialized AEAD
     * parameters used to encrypt the content of that blob.
     *
     * <p>Blobs that were added without AEAD parameters are not in the map.
     */
    public @NonNull Map<AppSearchBlobHandle, byte[]> getAeadParams() {
        Map<AppSearchBlobHandle, byte[]> aeadParams = new ArrayMap<>();
        for (int i = 0; i < mBlobHandles.size(); i++) {
            if (mAeadParams[i] != null) {
                aeadParams.put(mBlobHandles.get(i), mAeadParams[i]);
            }
        }
        return Collections.unmodifiableMap(aeadParams);
    }

    /**
     * Returns the opaque tag identifying the owner of the blobs in this request, such as the
     * account that they belong to.
     */
    public @NonNull byte[] getOwnerTag() {
        return mOwnerTag.clone();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommitEncryptedBlobRequest)) {
            return false;
        }
        CommitEncryptedBlobRequest that = (CommitEncryptedBlobRequest) o;
        if (!Arrays.equals(mSymmetricKey, that.mSymmetricKey)
                || !Arrays.equals(mOwnerTag, that.mOwnerTag)
                || mBlobHandles.size() != that.mBlobHandles.size()) {
            return false;
        }
        // The blobs of two equal requests may be stored in different orders.
        for (int i = 0; i < mBlobHandles.size(); i++) {
            int thatIndex = that.mBlobHandles.indexOf(mBlobHandles.get(i));
            if (thatIndex < 0 || !Arrays.equals(mAeadParams[i], that.mAeadParams[thatIndex])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        // Sum the hash codes of the blobs so that the result does not depend on their order.
        int blobsHashCode = 0;
        for (int i = 0; i < mBlobHandles.size(); i++) {
            blobsHashCode += mBlobHandles.get(i).hashCode() ^ Arrays.hashCode(mAeadParams[i]);
        }
        return Objects.hash(
                Arrays.hashCode(mSymmetricKey), blobsHashCode, Arrays.hashCode(mOwnerTag));
    }

    /**
     * Always returns 0 because this class does not contain file descriptors.
     *
     * <p>Defined here instead of inheriting it from {@code AbstractSafeParcelable}, whose final
     * {@code describeContents()} carries an unrelated, already finalized API flag.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Writes this object to the given {@link Parcel}. */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CommitEncryptedBlobRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link CommitEncryptedBlobRequest} objects. */
    @FlaggedApi(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
    public static final class Builder {
        private final byte[] mSymmetricKey;
        // Maps each blob to its AEAD parameters, or to null if the blob has none.
        private Map<AppSearchBlobHandle, byte[]> mAeadParams = new ArrayMap<>();
        private final byte[] mOwnerTag;
        private boolean mBuilt = false;

        /**
         * Creates a {@link CommitEncryptedBlobRequest.Builder} instance.
         *
         * @param symmetricKey The raw symmetric Data Encryption Key (DEK) used to encrypt all blobs
         *     in this request. It is stored by AppSearch alongside the blob metadata and is never
         *     returned to callers.
         * @param ownerTag An opaque tag identifying the owner of all blobs in this request, such as
         *     the account that they belong to. AppSearch stores the tag with the blob encryption
         *     key. When a wrapped key of a blob is requested, AppSearch only returns it if the
         *     requester is attested to act for the same owner.
         */
        public Builder(@NonNull byte[] symmetricKey, @NonNull byte[] ownerTag) {
            mSymmetricKey = Objects.requireNonNull(symmetricKey).clone();
            mOwnerTag = Objects.requireNonNull(ownerTag).clone();
        }

        /**
         * Adds an encrypted blob without AEAD parameters to be committed.
         *
         * @param handle The {@link AppSearchBlobHandle} that identifies the blob.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addBlobHandle(@NonNull AppSearchBlobHandle handle) {
            Objects.requireNonNull(handle);
            resetIfBuilt();
            mAeadParams.put(handle, null);
            return this;
        }

        /**
         * Adds an encrypted blob along with its AEAD parameters to be committed.
         *
         * @param handle The {@link AppSearchBlobHandle} that identifies the blob.
         * @param aeadParams The serialized AEAD parameters (e.g. algorithm, nonce) used to encrypt
         *     the content of this blob. Stored as is alongside the DEK.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder addBlobHandle(
                @NonNull AppSearchBlobHandle handle, @NonNull byte[] aeadParams) {
            Objects.requireNonNull(handle);
            Objects.requireNonNull(aeadParams);
            resetIfBuilt();
            mAeadParams.put(handle, aeadParams.clone());
            return this;
        }

        /** Builds a new {@link CommitEncryptedBlobRequest}. */
        public @NonNull CommitEncryptedBlobRequest build() {
            mBuilt = true;
            return new CommitEncryptedBlobRequest(
                    mSymmetricKey,
                    new ArrayList<>(mAeadParams.keySet()),
                    mAeadParams.values().toArray(new byte[0][]),
                    mOwnerTag);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                Map<AppSearchBlobHandle, byte[]> aeadParams = new ArrayMap<>(mAeadParams.size());
                aeadParams.putAll(mAeadParams);
                mAeadParams = aeadParams;
                mBuilt = false;
            }
        }
    }
}

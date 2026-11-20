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
import androidx.appsearch.safeparcel.stub.StubCreators.GetWrappedBlobEncryptionKeysRequestCreator;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates a request to get the Data Encryption Keys (DEKs) of a batch of encrypted blobs,
 * wrapped with the public key of a trusted component, via {@link
 * AppSearchSession#getWrappedBlobEncryptionKeysAsync}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@FlaggedApi(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
// NonCanonicalType: Field, Constructor and Param are declared in AbstractSafeParcelable in
// Jetpack and in SafeParcelable in the framework.
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness", "NonCanonicalType"})
@SafeParcelable.Class(creator = "GetWrappedBlobEncryptionKeysRequestCreator")
public final class GetWrappedBlobEncryptionKeysRequest implements SafeParcelable {

    /** Creator class for {@link androidx.appsearch.app.GetWrappedBlobEncryptionKeysRequest}. */
    public static final @NonNull Parcelable.Creator<GetWrappedBlobEncryptionKeysRequest> CREATOR =
            new GetWrappedBlobEncryptionKeysRequestCreator();

    @AbstractSafeParcelable.Field(id = 1)
    final @NonNull List<AppSearchBlobHandle> mBlobHandles;

    @AbstractSafeParcelable.Field(id = 2, getter = "getTrustedComponentPublicKey")
    private final @NonNull byte[] mTrustedComponentPublicKey;

    @AbstractSafeParcelable.Field(id = 3, getter = "getTrustedComponentAttestation")
    private final @NonNull byte[] mTrustedComponentAttestation;

    private @Nullable Set<AppSearchBlobHandle> mBlobHandlesCached;

    @AbstractSafeParcelable.Constructor
    GetWrappedBlobEncryptionKeysRequest(
            @AbstractSafeParcelable.Param(id = 1) @NonNull List<AppSearchBlobHandle> blobHandles,
            @AbstractSafeParcelable.Param(id = 2) @NonNull byte[] trustedComponentPublicKey,
            @AbstractSafeParcelable.Param(id = 3) @NonNull byte[] trustedComponentAttestation) {
        mBlobHandles = new ArrayList<>(Objects.requireNonNull(blobHandles));
        mTrustedComponentPublicKey = Objects.requireNonNull(trustedComponentPublicKey).clone();
        mTrustedComponentAttestation = Objects.requireNonNull(trustedComponentAttestation).clone();
    }

    /** Returns the set of {@link AppSearchBlobHandle}s whose keys should be wrapped. */
    public @NonNull Set<AppSearchBlobHandle> getBlobHandles() {
        if (mBlobHandlesCached == null) {
            mBlobHandlesCached = Collections.unmodifiableSet(new ArraySet<>(mBlobHandles));
        }
        return mBlobHandlesCached;
    }

    /** Returns the public key of the trusted component to wrap the blob DEKs with. */
    public @NonNull byte[] getTrustedComponentPublicKey() {
        return mTrustedComponentPublicKey.clone();
    }

    /**
     * Returns the attestation that binds {@link #getTrustedComponentPublicKey()} to the trusted
     * component.
     */
    public @NonNull byte[] getTrustedComponentAttestation() {
        return mTrustedComponentAttestation.clone();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetWrappedBlobEncryptionKeysRequest)) {
            return false;
        }
        GetWrappedBlobEncryptionKeysRequest that = (GetWrappedBlobEncryptionKeysRequest) o;
        return getBlobHandles().equals(that.getBlobHandles())
                && Arrays.equals(mTrustedComponentPublicKey, that.mTrustedComponentPublicKey)
                && Arrays.equals(mTrustedComponentAttestation, that.mTrustedComponentAttestation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getBlobHandles(),
                Arrays.hashCode(mTrustedComponentPublicKey),
                Arrays.hashCode(mTrustedComponentAttestation));
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
        GetWrappedBlobEncryptionKeysRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link GetWrappedBlobEncryptionKeysRequest} objects. */
    @FlaggedApi(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
    public static final class Builder {
        private ArraySet<AppSearchBlobHandle> mBlobHandles = new ArraySet<>();
        private final byte[] mTrustedComponentPublicKey;
        private final byte[] mTrustedComponentAttestation;
        private boolean mBuilt = false;

        /**
         * Creates a {@link GetWrappedBlobEncryptionKeysRequest.Builder} instance.
         *
         * @param trustedComponentPublicKey The public key of the trusted component to wrap the blob
         *     DEKs with.
         * @param trustedComponentAttestation The attestation that binds {@code
         *     trustedComponentPublicKey} to the trusted component.
         */
        public Builder(
                @NonNull byte[] trustedComponentPublicKey,
                @NonNull byte[] trustedComponentAttestation) {
            mTrustedComponentPublicKey = Objects.requireNonNull(trustedComponentPublicKey).clone();
            mTrustedComponentAttestation =
                    Objects.requireNonNull(trustedComponentAttestation).clone();
        }

        /** Adds one or more {@link AppSearchBlobHandle}s to the request. */
        @CanIgnoreReturnValue
        public @NonNull Builder addBlobHandles(@NonNull AppSearchBlobHandle... handles) {
            Objects.requireNonNull(handles);
            resetIfBuilt();
            return addBlobHandles(Arrays.asList(handles));
        }

        /** Adds a collection of {@link AppSearchBlobHandle}s to the request. */
        @CanIgnoreReturnValue
        public @NonNull Builder addBlobHandles(@NonNull Collection<AppSearchBlobHandle> handles) {
            Objects.requireNonNull(handles);
            resetIfBuilt();
            for (AppSearchBlobHandle handle : handles) {
                Objects.requireNonNull(handle);
                mBlobHandles.add(handle);
            }
            return this;
        }

        /** Builds a new {@link GetWrappedBlobEncryptionKeysRequest}. */
        public @NonNull GetWrappedBlobEncryptionKeysRequest build() {
            mBuilt = true;
            return new GetWrappedBlobEncryptionKeysRequest(
                    new ArrayList<>(mBlobHandles),
                    mTrustedComponentPublicKey,
                    mTrustedComponentAttestation);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mBlobHandles = new ArraySet<>(mBlobHandles);
                mBuilt = false;
            }
        }
    }
}

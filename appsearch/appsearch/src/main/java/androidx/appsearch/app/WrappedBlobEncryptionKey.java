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
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.WrappedBlobEncryptionKeyCreator;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds a Data Encryption Key (DEK) wrapped with the public key of a trusted component, along with
 * its content encryption (AEAD) parameters, returned by {@link
 * AppSearchSession#getWrappedBlobEncryptionKeysAsync}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@FlaggedApi(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
// NonCanonicalType: Field, Constructor and Param are declared in AbstractSafeParcelable in
// Jetpack and in SafeParcelable in the framework.
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness", "NonCanonicalType"})
@SafeParcelable.Class(creator = "WrappedBlobEncryptionKeyCreator")
public final class WrappedBlobEncryptionKey implements SafeParcelable {

    /** Creator class for {@link androidx.appsearch.app.WrappedBlobEncryptionKey}. */
    public static final @NonNull Parcelable.Creator<WrappedBlobEncryptionKey> CREATOR =
            new WrappedBlobEncryptionKeyCreator();

    @AbstractSafeParcelable.Field(id = 1, getter = "getWrappedDek")
    private final @NonNull byte[] mWrappedDek;

    @AbstractSafeParcelable.Field(id = 2, getter = "getAeadParams")
    private final @Nullable byte[] mAeadParams;

    /**
     * Creates a new {@link WrappedBlobEncryptionKey} with the given wrapped DEK and AEAD
     * parameters.
     *
     * @param wrappedDek The DEK wrapped with the public key of the trusted component.
     * @param aeadParams The serialized AEAD parameters (e.g. algorithm, nonce) used to encrypt the
     *     blob content, or {@code null} if the blob was committed without AEAD parameters.
     */
    @AbstractSafeParcelable.Constructor
    public WrappedBlobEncryptionKey(
            @AbstractSafeParcelable.Param(id = 1) @NonNull byte[] wrappedDek,
            @AbstractSafeParcelable.Param(id = 2) @Nullable byte[] aeadParams) {
        mWrappedDek = Objects.requireNonNull(wrappedDek);
        mAeadParams = aeadParams;
    }

    /** Returns the DEK wrapped with the public key of the trusted component. */
    public @NonNull byte[] getWrappedDek() {
        return mWrappedDek;
    }

    /**
     * Returns the serialized AEAD parameters used to encrypt the blob content, or {@code null} if
     * the blob was committed without AEAD parameters.
     *
     * <p>The parameters are returned as they were committed. They are not encrypted.
     */
    public @Nullable byte[] getAeadParams() {
        return mAeadParams;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WrappedBlobEncryptionKey)) {
            return false;
        }
        WrappedBlobEncryptionKey that = (WrappedBlobEncryptionKey) o;
        return Arrays.equals(mWrappedDek, that.mWrappedDek)
                && Arrays.equals(mAeadParams, that.mAeadParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(mWrappedDek), Arrays.hashCode(mAeadParams));
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
        WrappedBlobEncryptionKeyCreator.writeToParcel(this, dest, flags);
    }
}

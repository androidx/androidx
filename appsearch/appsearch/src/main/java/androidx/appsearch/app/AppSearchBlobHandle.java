/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.AppSearchBlobHandleCreator;
import androidx.core.util.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * An identifier to represent a Blob in AppSearch.
 *
 * @exportToFramework:hide
 */
// TODO(b/273591938) improve the java doc when we support set blob property in GenericDocument
// TODO(b/273591938) unhide the API once it read for API review.
@RequiresFeature(
        enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
        name = Features.BLOB_STORAGE)
@FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
@SafeParcelable.Class(creator = "AppSearchBlobHandleCreator")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchBlobHandle extends AbstractSafeParcelable {
    /** The length of the SHA-256 digest in bytes. SHA-256 produces a 256-bit (32-byte) digest. */
    private static final int SHA_256_DIGEST_BYTE_LENGTH = 32;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static final Parcelable.Creator<AppSearchBlobHandle> CREATOR =
            new AppSearchBlobHandleCreator();
    @NonNull
    @Field(id = 1, getter = "getSha256Digest")
    private final byte[] mSha256Digest;

    @NonNull
    @Field(id = 2, getter = "getLabel")
    private final String mLabel;

    @Nullable
    private Integer mHashCode;

    /**
     * Build an {@link AppSearchBlobHandle}.
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Constructor
    AppSearchBlobHandle(
            @Param(id = 1) @NonNull byte[] sha256Digest,
            @Param(id = 2) @NonNull String label) {
        mSha256Digest = Preconditions.checkNotNull(sha256Digest);
        Preconditions.checkState(sha256Digest.length == SHA_256_DIGEST_BYTE_LENGTH,
                "The input digest isn't a sha-256 digest.");
        mLabel = Preconditions.checkNotNull(label);
    }

    /**
     * Returns the SHA-256 hash of the blob that this object is representing.
     *
     * <p> For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code digest}
     * and {@code label} must be equal.
     */
    @NonNull
    public byte[] getSha256Digest() {
        return mSha256Digest;
    }

    /**
     * Returns the label indicating what the blob is with the blob that this object is representing.
     *
     * <p> The label is just a simple string which contains more readable information for the
     * digest. The string is used to indicate and describe the content represented by the digest.
     * The label cannot be used to search {@link AppSearchBlobHandle}.
     *
     * <p> If the label is not set, then this method will return an empty string.
     *
     * <p> For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code digest}
     * and {@code label} must be equal.
     */
    @NonNull
    public String getLabel() {
        return mLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppSearchBlobHandle)) return false;

        AppSearchBlobHandle that = (AppSearchBlobHandle) o;
        if (!Arrays.equals(mSha256Digest, that.mSha256Digest)) return false;
        return mLabel.equals(that.mLabel);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(Arrays.hashCode(mSha256Digest), mLabel);
        }
        return mHashCode;
    }

    /**
     * Create a new AppSearch blob identifier with given digest and empty label.
     *
     * <p> For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code digest}
     * and {@code label} must be equal.
     *
     * @param digest the SHA-256 hash of the blob this is representing.
     *
     * @return a new instance of {@link AppSearchBlobHandle} object.
     */
    @NonNull
    public static AppSearchBlobHandle createWithSha256(@NonNull byte[] digest) {
        return new AppSearchBlobHandle(digest, /*label=*/"");
    }

    /**
     * Create a new AppSearch blob identifier with given digest and label.
     *
     * <p> The label is just a simple string which contains more readable information for the
     * digest. The string is used to indicate and describe the content represented by the digest.
     * The label cannot be used to search {@link AppSearchBlobHandle}.
     *
     * <p> For two objects of {@link AppSearchBlobHandle} to be considered equal, the {@code digest}
     * and {@code label} must be equal.
     *
     * @param digest the SHA-256 hash of the blob this is representing.
     * @param label  a label indicating what the blob is, that can be surfaced to the user. It is
     *               recommended to keep this brief. The label doesn't need to be distinct.
     *
     * @return a new instance of {@link AppSearchBlobHandle} object.
     */
    @NonNull
    public static AppSearchBlobHandle createWithSha256(@NonNull byte[] digest,
            @NonNull String label) {
        Preconditions.checkNotNull(digest);
        Preconditions.checkArgument(digest.length == SHA_256_DIGEST_BYTE_LENGTH,
                "The digest is not a SHA-256 digest");
        Preconditions.checkNotNull(label);
        return new AppSearchBlobHandle(digest, label);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        AppSearchBlobHandleCreator.writeToParcel(this, dest, flags);
    }
}

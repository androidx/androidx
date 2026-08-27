/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.safeparcel;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds data for a {@link PackageIdentifier}.
 *
 * <p>TODO(b/275592563): This class is currently used in GetSchemaResponse as a bundle, and
 * therefore needs to implement Parcelable directly. Reassess if this is still needed once
 * VisibilityConfig becomes available, and if not we should switch to a SafeParcelable
 * implementation instead.
 */
@HideInPlatform
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SafeParcelable.Class(creator = "PackageIdentifierParcelCreator")
@SuppressLint("BanParcelableUsage")
public final class PackageIdentifierParcel extends AbstractSafeParcelable implements Parcelable {
    public static final Parcelable.@NonNull Creator<PackageIdentifierParcel> CREATOR =
            new PackageIdentifierParcelCreator();

    @Field(id = 1, getter = "getPackageName")
    private final String mPackageName;

    @Field(id = 2, getter = "getSha256Certificate")
    private final byte[] mSha256Certificate;

    @Field(id = 3, getter = "getMultiSignerSha256Certificates")
    private final byte[][] mMultiSignerSha256Certificates;

    /**
     * Creates a unique identifier for a package.
     *
     * @see PackageIdentifier
     */
    @Constructor
    public PackageIdentifierParcel(
            @Param(id = 1) @NonNull String packageName,
            @Param(id = 2) byte @Nullable [] sha256Certificate,
            @Param(id = 3) byte @Nullable [][] multiSignerSha256Certificates) {
        mPackageName = Objects.requireNonNull(packageName);
        if (multiSignerSha256Certificates != null && multiSignerSha256Certificates.length > 0) {
            for (int i = 0; i < multiSignerSha256Certificates.length; i++) {
                Objects.requireNonNull(
                        multiSignerSha256Certificates[i],
                        "Certificate at index " + i + " cannot be null");
            }
            mMultiSignerSha256Certificates = multiSignerSha256Certificates;
            mSha256Certificate = multiSignerSha256Certificates[0];
        } else if (sha256Certificate != null) {
            mSha256Certificate = sha256Certificate;
            mMultiSignerSha256Certificates = null;
        } else {
            throw new IllegalArgumentException(
                    "Either sha256Certificate or multiSignerSha256Certificates must be non-null");
        }
    }

    /** Creates a {@link PackageIdentifierParcel} for a single-certificate package. */
    public PackageIdentifierParcel(
            @NonNull String packageName, byte @NonNull [] sha256Certificate) {
        this(packageName, sha256Certificate, /* multiSignerSha256Certificates= */ null);
    }

    /** Creates a {@link PackageIdentifierParcel} for a multi-certificate package. */
    public PackageIdentifierParcel(
            @NonNull String packageName, byte @NonNull [][] multiSignerSha256Certificates) {
        this(packageName, /* sha256Certificate= */ null, multiSignerSha256Certificates);
    }

    public @NonNull String getPackageName() {
        return mPackageName;
    }

    public byte @NonNull [] getSha256Certificate() {
        return mSha256Certificate;
    }

    /** Returns all SHA-256 certificates for the package. */
    public byte @Nullable [][] getMultiSignerSha256Certificates() {
        return mMultiSignerSha256Certificates;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PackageIdentifierParcel)) {
            return false;
        }
        final PackageIdentifierParcel other = (PackageIdentifierParcel) obj;
        return mPackageName.equals(other.mPackageName)
                && Arrays.equals(mSha256Certificate, other.mSha256Certificate)
                && Arrays.deepEquals(
                        mMultiSignerSha256Certificates, other.mMultiSignerSha256Certificates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mPackageName,
                Arrays.hashCode(mSha256Certificate),
                Arrays.deepHashCode(mMultiSignerSha256Certificates));
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PackageIdentifierParcelCreator.writeToParcel(this, dest, flags);
    }
}

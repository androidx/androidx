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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds data for a {@link PackageIdentifier}.
 *
 * TODO(b/275592563): This class is currently used in GetSchemaResponse as a bundle, and
 * therefore needs to implement Parcelable directly. Reassess if this is still needed once
 * VisibilityConfig becomes available, and if not we should switch to a SafeParcelable
 * implementation instead.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SafeParcelable.Class(creator = "PackageIdentifierParcelCreator")
@SuppressLint("BanParcelableUsage")
public final class PackageIdentifierParcel extends AbstractSafeParcelable implements Parcelable {
    @NonNull
    public static final Parcelable.Creator<PackageIdentifierParcel> CREATOR =
            new PackageIdentifierParcelCreator();

    @Field(id = 1, getter = "getPackageName")
    private final String mPackageName;
    @Field(id = 2, getter = "getSha256Certificate")
    private final byte[] mSha256Certificate;

    /**
     * Creates a unique identifier for a package.
     *
     * @see PackageIdentifier
     */
    @Constructor
    public PackageIdentifierParcel(@Param(id = 1) @NonNull String packageName,
            @Param(id = 2) @NonNull byte[] sha256Certificate) {
        mPackageName = Preconditions.checkNotNull(packageName);
        mSha256Certificate = Preconditions.checkNotNull(sha256Certificate);
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public byte[] getSha256Certificate() {
        return mSha256Certificate;
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
                && Arrays.equals(mSha256Certificate, other.mSha256Certificate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackageName, Arrays.hashCode(mSha256Certificate));
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PackageIdentifierParcelCreator.writeToParcel(this, dest, flags);
    }
}

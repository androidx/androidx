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

package androidx.appsearch.safeparcel;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.safeparcel.stub.StubCreators.PackageIdentifierWithTagsParcelCreator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Holds a {@link PackageIdentifier} together with the attribution tags that a caller from that
 * package must present to be granted access.
 */
@HideInPlatform
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SafeParcelable.Class(creator = "PackageIdentifierWithTagsParcelCreator")
public final class PackageIdentifierWithTagsParcel extends AbstractSafeParcelable {
    public static final Parcelable.@NonNull Creator<PackageIdentifierWithTagsParcel> CREATOR =
            new PackageIdentifierWithTagsParcelCreator();

    @Field(id = 1, getter = "getPackageIdentifierParcel")
    private final PackageIdentifierParcel mPackageIdentifierParcel;

    @Field(id = 2, getter = "getAttributionTags")
    private final List<String> mAttributionTags;

    @Constructor
    public PackageIdentifierWithTagsParcel(
            @Param(id = 1) @NonNull PackageIdentifierParcel packageIdentifierParcel,
            @Param(id = 2) @NonNull List<String> attributionTags) {
        mPackageIdentifierParcel = Objects.requireNonNull(packageIdentifierParcel);
        mAttributionTags = Objects.requireNonNull(attributionTags);
    }

    /** Returns the package that is granted access. */
    public @NonNull PackageIdentifierParcel getPackageIdentifierParcel() {
        return mPackageIdentifierParcel;
    }

    /** Returns the attribution tags, one of which a caller from the package must present. */
    public @NonNull List<String> getAttributionTags() {
        return mAttributionTags;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PackageIdentifierWithTagsParcel)) {
            return false;
        }
        PackageIdentifierWithTagsParcel other = (PackageIdentifierWithTagsParcel) obj;
        return mPackageIdentifierParcel.equals(other.mPackageIdentifierParcel)
                && mAttributionTags.equals(other.mAttributionTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackageIdentifierParcel, mAttributionTags);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PackageIdentifierWithTagsParcelCreator.writeToParcel(this, dest, flags);
    }
}

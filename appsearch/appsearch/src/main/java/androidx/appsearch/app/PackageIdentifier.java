/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.safeparcel.PackageIdentifierParcel;
import androidx.core.util.Preconditions;

/**
 * This class represents a uniquely identifiable package.
 */
public class PackageIdentifier {
    @NonNull
    private final PackageIdentifierParcel mPackageIdentifierParcel;

    /**
     * Creates a unique identifier for a package.
     *
     * <p>SHA-256 certificate digests for a signed application can be retrieved with the
     * <a href="{@docRoot}studio/command-line/apksigner/">apksigner tool</a> that is part of the
     * Android SDK build tools. Use {@code apksigner verify --print-certs path/to/apk.apk} to
     * retrieve the SHA-256 certificate digest for the target application. Once retrieved, the
     * SHA-256 certificate digest should be converted to a {@code byte[]} by decoding it in base16:
     * <pre>
     * new android.content.pm.Signature(outputDigest).toByteArray();
     * </pre>
     *
     * @param packageName       Name of the package.
     * @param sha256Certificate SHA-256 certificate digest of the package.
     */
    public PackageIdentifier(@NonNull String packageName, @NonNull byte[] sha256Certificate) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(sha256Certificate);
        mPackageIdentifierParcel = new PackageIdentifierParcel(packageName, sha256Certificate);
    }

    /** @exportToFramework:hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public PackageIdentifier(@NonNull PackageIdentifierParcel packageIdentifierParcel) {
        mPackageIdentifierParcel = Preconditions.checkNotNull(packageIdentifierParcel);
    }

    /**
     * Returns the {@link PackageIdentifierParcel} holding the values for this
     * {@link PackageIdentifier}.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public PackageIdentifierParcel getPackageIdentifierParcel() {
        return mPackageIdentifierParcel;
    }

    @NonNull
    public String getPackageName() {
        return mPackageIdentifierParcel.getPackageName();
    }

    @NonNull
    public byte[] getSha256Certificate() {
        return mPackageIdentifierParcel.getSha256Certificate();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PackageIdentifier)) {
            return false;
        }
        final PackageIdentifier other = (PackageIdentifier) obj;
        return mPackageIdentifierParcel.equals(other.getPackageIdentifierParcel());
    }

    @Override
    public int hashCode() {
        return mPackageIdentifierParcel.hashCode();
    }
}

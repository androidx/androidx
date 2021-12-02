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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.util.BundleUtil;
import androidx.core.util.Preconditions;

/** This class represents a uniquely identifiable package. */
public class PackageIdentifier {
    private static final String PACKAGE_NAME_FIELD = "packageName";
    private static final String SHA256_CERTIFICATE_FIELD = "sha256Certificate";

    private final Bundle mBundle;

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
     * @param packageName Name of the package.
     * @param sha256Certificate SHA-256 certificate digest of the package.
     */
    public PackageIdentifier(@NonNull String packageName, @NonNull byte[] sha256Certificate) {
        mBundle = new Bundle();
        mBundle.putString(PACKAGE_NAME_FIELD, packageName);
        mBundle.putByteArray(SHA256_CERTIFICATE_FIELD, sha256Certificate);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public PackageIdentifier(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    @NonNull
    public String getPackageName() {
        return Preconditions.checkNotNull(mBundle.getString(PACKAGE_NAME_FIELD));
    }

    @NonNull
    public byte[] getSha256Certificate() {
        return Preconditions.checkNotNull(mBundle.getByteArray(SHA256_CERTIFICATE_FIELD));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof PackageIdentifier)) {
            return false;
        }
        final PackageIdentifier other = (PackageIdentifier) obj;
        return BundleUtil.deepEquals(mBundle, other.mBundle);
    }

    @Override
    public int hashCode() {
        return BundleUtil.deepHashCode(mBundle);
    }
}

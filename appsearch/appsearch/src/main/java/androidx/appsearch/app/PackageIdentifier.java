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
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.PackageIdentifierParcel;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a uniquely identifiable package.
 */
// TODO(b/384721898): Switching to JSpecify annotations changes APIs once synced to platform.
//  Do not switch unless you've checked that no APIs are affected.
@SuppressWarnings("JSpecifyNullness")
public class PackageIdentifier {
    private final @NonNull PackageIdentifierParcel mPackageIdentifierParcel;

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
        mPackageIdentifierParcel = new PackageIdentifierParcel(
                Preconditions.checkNotNull(packageName),
                Preconditions.checkNotNull(sha256Certificate));
    }

    /**
     * Creates a unique identifier for a package signed by multiple certificates.
     *
     * <p>This constructor is for multi-signer applications that are simultaneously signed by
     * multiple active certificates under AND-logic (where <b>all</b> certificates are concurrently
     * valid and must be validated together).
     *
     * <p>This list is <b>not</b> for key rotation history (where past keys from a rotation lineage
     * are provided) nor for OR-logic (where matching any single certificate out of several is
     * sufficient). For single-signer applications with or without key rotation, use {@link
     * #PackageIdentifier(String, byte[])}.
     *
     * @param packageName                  Name of the package.
     * @param multiSignerSha256Certificates List of all SHA-256 certificate digests for a
     *                                     multi-signer package. All active co-signing certificates
     *                                     must be provided.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public PackageIdentifier(
            @NonNull String packageName,
            @NonNull List<byte[]> multiSignerSha256Certificates) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(multiSignerSha256Certificates);
        if (multiSignerSha256Certificates.isEmpty()) {
            throw new IllegalArgumentException("multiSignerSha256Certificates cannot be empty");
        }
        for (int i = 0; i < multiSignerSha256Certificates.size(); i++) {
            Preconditions.checkNotNull(
                    multiSignerSha256Certificates.get(i), "cert at index " + i + " cannot be null");
        }
        byte[][] certsArray = multiSignerSha256Certificates.toArray(new byte[0][]);
        mPackageIdentifierParcel = new PackageIdentifierParcel(packageName, certsArray);
    }

    @HideInPlatform
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public PackageIdentifier(@NonNull PackageIdentifierParcel packageIdentifierParcel) {
        mPackageIdentifierParcel = Preconditions.checkNotNull(packageIdentifierParcel);
    }

    /**
     * Returns the {@link PackageIdentifierParcel} holding the values for this
     * {@link PackageIdentifier}.
     */
    @HideInPlatform
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull PackageIdentifierParcel getPackageIdentifierParcel() {
        return mPackageIdentifierParcel;
    }

    /** Returns the name for a package. */
    public @NonNull String getPackageName() {
        return mPackageIdentifierParcel.getPackageName();
    }

    // TODO(b/514617291): Deprecate this method and introduce a getSha256CertificateHistory method.
    /**
     * Returns the SHA-256 certificate for a single-signer package.
     *
     * <p>This method should not be called for multiple-signing certificates. Use
     * {@link #getMultiSignerSha256Certificates} to retrieve all certificates.
     */
    public @NonNull byte[] getSha256Certificate() {
        return mPackageIdentifierParcel.getSha256Certificate().clone();
    }

    /**
     * Returns all SHA-256 certificates for a multi-signer package.
     *
     * <p>Returns an empty list if the package has a single signer.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public @NonNull List<byte[]> getMultiSignerSha256Certificates() {
        byte[][] certs = mPackageIdentifierParcel.getMultiSignerSha256Certificates();
        if (certs == null) {
            return Collections.emptyList();
        }
        // Defensively copy each byte array so callers cannot mutate the internal state of
        // this PackageIdentifier.
        byte[][] copy = new byte[certs.length][];
        for (int i = 0; i < certs.length; i++) {
            copy[i] = certs[i].clone();
        }
        return List.of(copy);
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

/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.core.appdigest;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * A typed checksum of an APK.
 *
 * @see Checksums#getChecksums
 */
public final class Checksum {
    /**
     * Root SHA256 hash of a 4K Merkle tree computed over all file bytes.
     * <a href="https://source.android.com/security/apksigning/v4">See APK Signature Scheme V4</a>.
     * <a href="https://git.kernel.org/pub/scm/fs/fscrypt/fscrypt.git/tree/Documentation/filesystems/fsverity.rst">See fs-verity</a>.
     *
     * Recommended for all new applications.
     * Can be used by kernel to enforce authenticity and integrity of the APK.
     * <a href="https://git.kernel.org/pub/scm/fs/fscrypt/fscrypt.git/tree/Documentation/filesystems/fsverity.rst#">See fs-verity for details</a>
     *
     * @see Checksums#getChecksums
     */
    public static final int TYPE_WHOLE_MERKLE_ROOT_4K_SHA256 = 0x00000001;

    /**
     * MD5 hash computed over all file bytes.
     *
     * @see Checksums#getChecksums
     * @deprecated Not platform enforced. Cryptographically broken and unsuitable for further use.
     *             Use platform enforced digests e.g. {@link #TYPE_WHOLE_MERKLE_ROOT_4K_SHA256}.
     *             Provided for completeness' sake and to support legacy usecases.
     */
    @Deprecated
    public static final int TYPE_WHOLE_MD5 = 0x00000002;

    /**
     * SHA1 hash computed over all file bytes.
     *
     * @see Checksums#getChecksums
     * @deprecated Not platform enforced. Broken and should not be used.
     *             Use platform enforced digests e.g. {@link #TYPE_WHOLE_MERKLE_ROOT_4K_SHA256}.
     *             Provided for completeness' sake and to support legacy usecases.
     */
    @Deprecated
    public static final int TYPE_WHOLE_SHA1 = 0x00000004;

    /**
     * SHA256 hash computed over all file bytes.
     * @deprecated Not platform enforced.
     *             Use platform enforced digests e.g. {@link #TYPE_WHOLE_MERKLE_ROOT_4K_SHA256}.
     *             Provided for completeness' sake and to support legacy usecases.
     *
     * @see Checksums#getChecksums
     */
    @Deprecated
    public static final int TYPE_WHOLE_SHA256 = 0x00000008;

    /**
     * SHA512 hash computed over all file bytes.
     * @deprecated Not platform enforced.
     *             Use platform enforced digests e.g. {@link #TYPE_WHOLE_MERKLE_ROOT_4K_SHA256}.
     *             Provided for completeness' sake and to support legacy usecases.
     *
     * @see Checksums#getChecksums
     */
    @Deprecated
    public static final int TYPE_WHOLE_SHA512 = 0x00000010;

    /**
     * Root SHA256 hash of a 1M Merkle tree computed over protected content.
     * Excludes signing block.
     * <a href="https://source.android.com/security/apksigning/v2">See APK Signature Scheme V2</a>.
     *
     * @see Checksums#getChecksums
     */
    public static final int TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256 = 0x00000020;

    /**
     * Root SHA512 hash of a 1M Merkle tree computed over protected content.
     * Excludes signing block.
     * <a href="https://source.android.com/security/apksigning/v2">See APK Signature Scheme V2</a>.
     *
     * @see Checksums#getChecksums
     */
    public static final int TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512 = 0x00000040;

    @RestrictTo(LIBRARY)
    @IntDef(flag = true, value = {
            TYPE_WHOLE_MERKLE_ROOT_4K_SHA256,
            TYPE_WHOLE_MD5,
            TYPE_WHOLE_SHA1,
            TYPE_WHOLE_SHA256,
            TYPE_WHOLE_SHA512,
            TYPE_PARTIAL_MERKLE_ROOT_1M_SHA256,
            TYPE_PARTIAL_MERKLE_ROOT_1M_SHA512,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {}

    /**
     * Checksum for which split. Null indicates base.apk.
     */
    private final @Nullable String mSplitName;
    /**
     * Checksum type.
     */
    private final @Checksum.Type int mType;
    /**
     * Checksum value.
     */
    private final @NonNull byte[] mValue;
    /**
     * For Installer-provided checksums, package name of the Installer.
     */
    private final @Nullable String mInstallerPackageName;
    /**
     * For Installer-provided checksums, certificate of the Installer.
     */
    private final @Nullable byte[] mInstallerCertificate;

    /**
     * Constructor, internal use only.
     */
    Checksum(@Nullable String splitName, @Checksum.Type int type,
            @NonNull byte[] value) {
        this(splitName, type, value, (String) null, (byte[]) null);
    }

    /**
     * Constructor, internal use only.
     */
    Checksum(@Nullable String splitName, @Checksum.Type int type, @NonNull byte[] value,
            @Nullable String installerPackageName, @Nullable Certificate installerCertificate)
            throws CertificateEncodingException {
        this(splitName, type, value, installerPackageName,
                (installerCertificate != null) ? installerCertificate.getEncoded() : null);
    }

    /**
     * Creates a new Checksum.
     *
     * @param splitName
     *   Checksum for which split. Null indicates base.apk.
     * @param type
     *   Checksum type.
     * @param value
     *   Checksum value.
     * @param installerPackageName
     *   For Installer-provided checksums, package name of the Installer.
     * @param installerCertificate
     *   For Installer-provided checksums, certificate of the Installer.
     */
    Checksum(
            @Nullable String splitName,
            @Type int type,
            @NonNull byte[] value,
            @Nullable String installerPackageName,
            @Nullable byte[] installerCertificate) {
        Preconditions.checkNotNull(value);
        this.mSplitName = splitName;
        this.mType = type;
        this.mValue = value;
        this.mInstallerPackageName = installerPackageName;
        this.mInstallerCertificate = installerCertificate;
    }

    /**
     * Checksum for which split. Null indicates base.apk.
     */
    public @Nullable String getSplitName() {
        return mSplitName;
    }

    /**
     * For Installer-provided checksums, package name of the Installer.
     */
    public @Nullable String getInstallerPackageName() {
        return mInstallerPackageName;
    }

    /**
     * Checksum type.
     */
    public @Checksum.Type int getType() {
        return mType;
    }

    /**
     * Checksum value.
     */
    public @NonNull byte[] getValue() {
        return mValue;
    }

    /**
     * For Installer-provided checksums, certificate of the Installer.
     * @throws CertificateException in case when certificate can't be re-created from serialized
     * data.
     */
    public @Nullable Certificate getInstallerCertificate() throws CertificateException {
        if (mInstallerCertificate == null) {
            return null;
        }
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final InputStream is = new ByteArrayInputStream(mInstallerCertificate);
        final X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
        return cert;
    }
}

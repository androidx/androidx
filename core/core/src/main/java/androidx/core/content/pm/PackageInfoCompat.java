/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.content.pm;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Helper for accessing features in {@link PackageInfo}. */
public final class PackageInfoCompat {
    /**
     * Return {@link android.R.attr#versionCode} and {@link android.R.attr#versionCodeMajor}
     * combined together as a single long value. The {@code versionCodeMajor} is placed in the
     * upper 32 bits on Android P or newer, otherwise these bits are all set to 0.
     *
     * @see PackageInfo#getLongVersionCode()
     */
    @SuppressWarnings("deprecation")
    public static long getLongVersionCode(@NonNull PackageInfo info) {
        if (Build.VERSION.SDK_INT >= 28) {
            return info.getLongVersionCode();
        }
        return info.versionCode;
    }

    /**
     * Retrieve the {@link Signature} array for the given package. This returns some of
     * certificates, depending on whether the package in question is multi-signed or has signing
     * history.
     *
     * <note>
     * <p>
     * Security/identity verification should <b>not</b> be done with this method. This is only
     * intended to return some array of certificates that correspond to a package.
     * </p>
     * <p>
     * If verification if required, either use
     * {@link #hasSignatures(PackageManager, String, Map, boolean)} or manually verify the set of
     * certificates using {@link PackageManager#GET_SIGNING_CERTIFICATES} or
     * {@link PackageManager#GET_SIGNATURES}.
     * </p>
     * </note>
     *
     * @param packageManager The {@link PackageManager} instance to query against.
     * @param packageName    The package to query the {@param packageManager} for. Query by app
     *                       UID is only supported by manually choosing a package name
     *                       returned in {@link PackageManager#getPackagesForUid(int)}.
     * @return an array of certificates the app is signed with
     * @throws PackageManager.NameNotFoundException if the package cannot be found through the
     *                                              provided {@param packageManager}
     */
    @NonNull
    public static List<Signature> getSignatures(@NonNull PackageManager packageManager,
            @NonNull String packageName) throws PackageManager.NameNotFoundException {
        Signature[] array;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageInfo pkgInfo = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES);
            SigningInfo signingInfo = pkgInfo.signingInfo;
            if (Api28Impl.hasMultipleSigners(signingInfo)) {
                array = Api28Impl.getApkContentsSigners(signingInfo);
            } else {
                array = Api28Impl.getSigningCertificateHistory(signingInfo);
            }
        } else {
            // Lint warning's vulnerability is explicitly not handled for this method.
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pkgInfo = packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
            array = pkgInfo.signatures;
        }

        // Framework code implies nullable/empty, although it may be impossible in practice.
        if (array == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(array);
        }
    }

    /**
     * Check if a package on device contains set of a certificates. Supported types are raw X509 or
     * SHA-256 bytes.
     *
     * @param packageManager      The {@link PackageManager} instance to query against.
     * @param packageName         The package to query the {@param packageManager} for. Query by
     *                            app UID is only supported by manually choosing a package name
     *                            returned in {@link PackageManager#getPackagesForUid(int)}.
     * @param certificatesAndType The bytes of the certificate mapped to the type, either
     *                            {@link PackageManager#CERT_INPUT_RAW_X509} or
     *                            {@link PackageManager#CERT_INPUT_SHA256}. A single or multiple
     *                            certificates may be included.
     * @param matchExact          Whether or not to check for presence of all signatures exactly.
     *                            If false, then the check will succeed if the query contains a
     *                            subset of the package certificates. Matching exactly is strongly
     *                            recommended when running on devices below
     *                            {@link Build.VERSION_CODES#LOLLIPOP} due to the fake ID
     *                            vulnerability that allows a package to be modified to include
     *                            an unverified signature.
     * @return true if the package is considered signed by the given certificate set, or false
     * otherwise
     * @throws PackageManager.NameNotFoundException if the package cannot be found through the
     *                                              provided {@param packageManager}
     */
    public static boolean hasSignatures(@NonNull PackageManager packageManager,
            @NonNull String packageName,
            @Size(min = 1) @NonNull Map<byte[], Integer> certificatesAndType, boolean matchExact)
            throws PackageManager.NameNotFoundException {
        // If empty is passed in, return false to prevent accidentally succeeding
        if (certificatesAndType.isEmpty()) {
            return false;
        }

        Set<byte[]> expectedCertBytes = certificatesAndType.keySet();

        // The type has to be checked before any API level branching. If a new type is ever added,
        // this code should fail and will have to be updated manually. To do otherwise would
        // introduce a behavioral difference between the API level that added the new type and
        // devices on prior API levels, which may not be caught by a developer calling this
        // method if they do not test on an old API level.
        for (byte[] bytes : expectedCertBytes) {
            if (bytes == null) {
                throw new IllegalArgumentException("Cert byte array cannot be null when verifying "
                        + packageName);
            }
            Integer type = certificatesAndType.get(bytes);
            if (type == null) {
                throw new IllegalArgumentException("Type must be specified for cert when verifying "
                        + packageName);
            }

            switch (type) {
                case PackageManager.CERT_INPUT_RAW_X509:
                case PackageManager.CERT_INPUT_SHA256:
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported certificate type " + type
                            + " when verifying " + packageName);
            }
        }

        // getSignatures is called first to throw NameNotFoundException if necessary
        final List<Signature> signers = getSignatures(packageManager, packageName);

        // The vulnerability requiring matchExact is not necessary on P, but the signatures
        // must still be checked manually in order to match the behavior described by the
        // method. Otherwise matchExact == true will allow additional certificates if run
        // on a device >= P.
        if (!matchExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // If not matching exact, delegate to the API 28 PackageManager API for checking
            // individual certificates. This is less performant, but goes through a formally
            // supported API.
            for (byte[] bytes : expectedCertBytes) {
                Integer type = certificatesAndType.get(bytes);
                //noinspection ConstantConditions type cannot be null
                if (!Api28Impl.hasSigningCertificate(packageManager, packageName, bytes, type)) {
                    return false;
                }
            }

            return true;
        }

        // Fail if the query is larger than the actual set, or the size doesn't match and it should.
        if (signers.size() == 0
                || certificatesAndType.size() > signers.size()
                || (matchExact && certificatesAndType.size() != signers.size())) {
            return false;
        }

        @SuppressLint("InlinedApi")
        boolean hasSha256 = certificatesAndType.containsValue(PackageManager.CERT_INPUT_SHA256);
        byte[][] sha256Digests = null;
        if (hasSha256) {
            // Since the search does several array contains checks, cache the SHA256 digests here.
            sha256Digests = new byte[signers.size()][];
            for (int index = 0; index < signers.size(); index++) {
                sha256Digests[index] = computeSHA256Digest(signers.get(index).toByteArray());
            }
        }

        for (byte[] bytes : expectedCertBytes) {
            Integer type = certificatesAndType.get(bytes);
            //noinspection ConstantConditions type cannot be null
            switch (type) {
                case PackageManager.CERT_INPUT_RAW_X509:
                    // RAW_X509 is the type that Signatures are and always have been stored as,
                    // so defer to the Signature equals method for the platform.
                    Signature expectedSignature = new Signature(bytes);
                    if (!signers.contains(expectedSignature)) {
                        return false;
                    }
                    break;
                case PackageManager.CERT_INPUT_SHA256:
                    // sha256Digests cannot be null due to pre-checked containsValue for its type
                    //noinspection ConstantConditions
                    if (!byteArrayContains(sha256Digests, bytes)) {
                        return false;
                    }
                    break;
                default:
                    // Impossible to reach this point due to check at beginning of method.
                    throw new IllegalArgumentException("Unsupported certificate type " + type);
            }

            // If this point is reached, all searches have succeeded
            return true;
        }

        return false;
    }

    private static boolean byteArrayContains(@NonNull byte[][] array, @NonNull byte[] expected) {
        for (byte[] item : array) {
            if (Arrays.equals(expected, item)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] computeSHA256Digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            // Can't happen, SHA256 required since API level 1
            throw new RuntimeException("Device doesn't support SHA256 cert checking", e);
        }
    }

    private PackageInfoCompat() {
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static class Api28Impl {
        private Api28Impl() {
        }

        static boolean hasSigningCertificate(@NonNull PackageManager packageManager,
                @NonNull String packageName, @NonNull byte[] bytes, int type) {
            return packageManager.hasSigningCertificate(packageName, bytes, type);
        }

        static boolean hasMultipleSigners(@NonNull SigningInfo signingInfo) {
            return signingInfo.hasMultipleSigners();
        }

        @Nullable
        static Signature[] getApkContentsSigners(@NonNull SigningInfo signingInfo) {
            return signingInfo.getApkContentsSigners();
        }

        @Nullable
        static Signature[] getSigningCertificateHistory(@NonNull SigningInfo signingInfo) {
            return signingInfo.getSigningCertificateHistory();
        }
    }
}

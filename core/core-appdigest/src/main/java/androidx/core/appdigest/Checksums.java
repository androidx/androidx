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

import static androidx.core.appdigest.Checksum.TYPE_WHOLE_MD5;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_MERKLE_ROOT_4K_SHA256;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_SHA1;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_SHA256;
import static androidx.core.appdigest.Checksum.TYPE_WHOLE_SHA512;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Provides checksums for Android applications.
 */
public final class Checksums {
    private static final String TAG = "Checksums";

    /**
     * Trust any Installer to provide checksums for the package.
     * @see #getChecksums
     */
    public static final @NonNull List<Certificate> TRUST_ALL = Collections.singletonList(null);

    /**
     * Don't trust any Installer to provide checksums for the package.
     * This effectively disables optimized Installer-enforced checksums.
     * @see #getChecksums
     */
    public static final @NonNull List<Certificate> TRUST_NONE = Collections.singletonList(null);

    // MessageDigest algorithms.
    private static final String ALGO_MD5 = "MD5";
    private static final String ALGO_SHA1 = "SHA1";
    private static final String ALGO_SHA256 = "SHA256";
    private static final String ALGO_SHA512 = "SHA512";

    private static final int READ_CHUNK_SIZE = 128 * 1024;

    private Checksums() {
    }

    /**
     * Returns the checksums for APKs within a package.
     *
     * By default returns all readily available checksums:
     * - enforced by platform,
     * - enforced by installer.
     * If caller needs a specific checksum type, they can specify it as required.
     *
     * <b>Caution: Android can not verify installer-provided checksums. Make sure you specify
     * trusted installers.</b>
     *
     * @param context The application or activity context.
     * @param includeSplits whether to include checksums for non-base splits (26+).
     * @param packageName whose checksums to return.
     * @param required explicitly request the checksum types. Will incur significant
     *                 CPU/memory/disk usage.
     * @param trustedInstallers for checksums enforced by installer, which installers are to be
     *                          trusted.
     *                          {@link #TRUST_ALL} will return checksums from any installer,
     *                          {@link #TRUST_NONE} disables optimized installer-enforced checksums,
     *                          otherwise the list has to be non-empty list of certificates.
     * @param executor for calculating checksums.
     * @throws IllegalArgumentException if the list of trusted installer certificates is empty.
     * @throws PackageManager.NameNotFoundException if a package with the given name cannot be
     *                                              found on the system.
     */
    public static @NonNull ListenableFuture<Checksum[]> getChecksums(@NonNull Context context,
            @NonNull String packageName, boolean includeSplits, final @Checksum.Type int required,
            @NonNull List<Certificate> trustedInstallers, @NonNull Executor executor)
            throws CertificateEncodingException, PackageManager.NameNotFoundException {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(trustedInstallers);
        Preconditions.checkNotNull(executor);

        if (Build.VERSION.SDK_INT >= 31) {
            return ChecksumsApiSImpl.getChecksums(context, packageName, includeSplits, required,
                    trustedInstallers, executor);
        }

        final ApplicationInfo applicationInfo =
                context.getPackageManager().getApplicationInfo(packageName, 0);
        if (applicationInfo == null) {
            throw new PackageManager.NameNotFoundException(packageName);
        }

        final ResolvableFuture<Checksum[]> result = ResolvableFuture.create();

        if (required == 0) {
            result.set(new Checksum[0]);
            return result;
        }

        final List<Pair<String, File>> filesToChecksum = new ArrayList<>();

        // Adding base split.
        final String baseSplitName = null;
        filesToChecksum.add(Pair.create(baseSplitName, new File(applicationInfo.sourceDir)));

        // Adding other splits.
        if (Build.VERSION.SDK_INT >= 26 && includeSplits && applicationInfo.splitNames != null) {
            for (int i = 0, size = applicationInfo.splitNames.length; i < size; ++i) {
                filesToChecksum.add(Pair.create(applicationInfo.splitNames[i],
                        new File(applicationInfo.splitSourceDirs[i])));
            }
        }

        for (int i = 0, size = filesToChecksum.size(); i < size; ++i) {
            final File file = filesToChecksum.get(i).second;
            if (!file.exists()) {
                throw new IllegalStateException("File not found: " + file.getPath());
            }
        }

        final String installerPackageName;
        if (Build.VERSION.SDK_INT >= 31) {
            installerPackageName = ChecksumsApiSImpl.getInstallerPackageName(context, packageName);
        } else {
            installerPackageName = null;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                getChecksumsSync(context, filesToChecksum, required, installerPackageName,
                        trustedInstallers, result);
            }
        });
        return result;
    }

    /**
     * Returns the checksums for an APK. Use {@link #getChecksums} unless the package has
     * not yet been installed on the device.
     * A possible use case is replying to {@link Intent#ACTION_PACKAGE_NEEDS_VERIFICATION}
     * broadcast.
     *
     * By default returns all readily available checksums:
     * - enforced by platform,
     * - enforced by installer.
     * If caller needs a specific checksum type, they can specify it as required.
     *
     * <b>Caution: Android can not verify installer-provided checksums. Make sure you specify
     * trusted installers.</b>
     *
     * @param context The application or activity context.
     * @param filePath whose checksums to return.
     * @param required explicitly request the checksum types. Will incur significant
     *                 CPU/memory/disk usage.
     * @param installerPackageName package name of the installer of the file
     * @param trustedInstallers for checksums enforced by installer, which installers are to be
     *                          trusted.
     *                          {@link #TRUST_ALL} will return checksums from any installer,
     *                          {@link #TRUST_NONE} disables optimized installer-enforced checksums,
     *                          otherwise the list has to be non-empty list of certificates.
     * @param executor for calculating checksums.
     * @throws IllegalArgumentException if the list of trusted installer certificates is empty.
     */
    public static @NonNull ListenableFuture<Checksum[]> getFileChecksums(@NonNull Context context,
            @NonNull String filePath, final @Checksum.Type int required,
            @Nullable String installerPackageName, @NonNull List<Certificate> trustedInstallers,
            @NonNull Executor executor) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(filePath);
        Preconditions.checkNotNull(trustedInstallers);
        Preconditions.checkNotNull(executor);

        final ResolvableFuture<Checksum[]> result = ResolvableFuture.create();

        final List<Pair<String, File>> filesToChecksum = new ArrayList<>();

        final String splitName = null;
        filesToChecksum.add(Pair.create(splitName, new File(filePath)));

        executor.execute(new Runnable() {
            @Override
            public void run() {
                getChecksumsSync(context, filesToChecksum, required, installerPackageName,
                        trustedInstallers, result);
            }
        });
        return result;
    }

    static void getChecksumsSync(
            @NonNull Context context,
            @NonNull List<Pair<String, File>> filesToChecksum,
            @Checksum.Type int required,
            @Nullable String installerPackageName,
            @Nullable List<Certificate> trustedInstallers,
            ResolvableFuture<Checksum[]> result) {
        List<Checksum> allChecksums = new ArrayList<>();
        for (int i = 0, isize = filesToChecksum.size(); i < isize; ++i) {
            final String split = filesToChecksum.get(i).first;
            final File file = filesToChecksum.get(i).second;
            try {
                final SparseArray<Checksum> checksums = new SparseArray<>();

                if (Build.VERSION.SDK_INT >= 31) {
                    try {
                        ChecksumsApiSImpl.getInstallerChecksums(context, split, file, required,
                                installerPackageName, trustedInstallers, checksums);
                    } catch (Throwable e) {
                        Log.e(TAG, "Installer checksum calculation error", e);
                    }
                }

                getRequiredApkChecksums(split, file, required, checksums);

                for (int j = 0, jsize = checksums.size(); j < jsize; ++j) {
                    allChecksums.add(checksums.valueAt(j));
                }
            } catch (Throwable e) {
                Log.e(TAG, "Required checksum calculation error", e);
            }
        }
        result.set(allChecksums.toArray(new Checksum[allChecksums.size()]));
    }

    /**
     * Fetch or calculate checksums for the specific file.
     */
    @SuppressWarnings("deprecation") /* WHOLE_MD5, WHOLE_SHA1 */
    private static void getRequiredApkChecksums(String split, File file,
            @Checksum.Type int required, SparseArray<Checksum> checksums) {
        // Manually calculating required checksums if not readily available.
        if (isRequired(TYPE_WHOLE_MERKLE_ROOT_4K_SHA256, required, checksums)) {
            try {
                byte[] generatedRootHash =
                        VerityTreeBuilder.computeChunkVerityTreeAndDigest(file.getAbsolutePath());
                checksums.put(TYPE_WHOLE_MERKLE_ROOT_4K_SHA256,
                        new Checksum(split, TYPE_WHOLE_MERKLE_ROOT_4K_SHA256, generatedRootHash));
            } catch (IOException | NoSuchAlgorithmException e) {
                Log.e(TAG, "Error calculating TYPE_WHOLE_MERKLE_ROOT_4K_SHA256", e);
            }
        }

        calculateChecksumIfRequired(checksums, split, file, required, TYPE_WHOLE_MD5);
        calculateChecksumIfRequired(checksums, split, file, required, TYPE_WHOLE_SHA1);
        calculateChecksumIfRequired(checksums, split, file, required, TYPE_WHOLE_SHA256);
        calculateChecksumIfRequired(checksums, split, file, required, TYPE_WHOLE_SHA512);
    }

    @SuppressWarnings("deprecation") /* WHOLE_MD5, WHOLE_SHA1 */
    private static String getMessageDigestAlgoForChecksumType(int type)
            throws NoSuchAlgorithmException {
        switch (type) {
            case TYPE_WHOLE_MD5:
                return ALGO_MD5;
            case TYPE_WHOLE_SHA1:
                return ALGO_SHA1;
            case TYPE_WHOLE_SHA256:
                return ALGO_SHA256;
            case TYPE_WHOLE_SHA512:
                return ALGO_SHA512;
            default:
                throw new NoSuchAlgorithmException("Invalid checksum type: " + type);
        }
    }

    static boolean isRequired(@Checksum.Type int type,
            @Checksum.Type int required, SparseArray<Checksum> checksums) {
        if ((required & type) == 0) {
            return false;
        }
        if (checksums.indexOfKey(type) >= 0) {
            return false;
        }
        return true;
    }

    private static void calculateChecksumIfRequired(SparseArray<Checksum> checksums,
            String split, File file, int required, int type) {
        if (isRequired(type, required, checksums)) {
            final byte[] checksum = getApkChecksum(file, type);
            if (checksum != null) {
                checksums.put(type, new Checksum(split, type, checksum));
            }
        }
    }

    private static byte[] getApkChecksum(File file, int type) {
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                byte[] dataBytes = new byte[READ_CHUNK_SIZE];
                int nread = 0;

                final String algo = getMessageDigestAlgoForChecksumType(type);
                MessageDigest md = MessageDigest.getInstance(algo);
                while ((nread = fis.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                }

                return md.digest();
            } finally {
                fis.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading " + file.getAbsolutePath() + " to compute hash.", e);
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Device does not support MessageDigest algorithm", e);
            return null;
        }
    }
}

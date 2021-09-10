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

package androidx.security.app.authenticator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.collection.LruCache;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods to verify the signatures of apps based on the expected signing identities
 * provided to the {@link Builder#setPermissionAllowMap(Map)} and {@link
 * Builder#setExpectedIdentities(Map)} builder methods.
 */
class AppSignatureVerifier {
    private static final String TAG = AppSignatureVerifier.class.getSimpleName();
    private static final String EXPECTED_IDENTITY_QUERY = "expected-identity";
    private static final int DEFAULT_CACHE_SIZE = 16;

    private final PackageManager mPackageManager;
    private final String mDigestAlgorithm;
    private final Cache mCache;
    /**
     * A mapping from permission to allowed packages / signing identities.
     */
    private final Map<String, Map<String, Set<String>>> mPermissionAllowMap;
    /**
     * A mapping from package name to expected signing identities.
     */
    private final Map<String, Set<String>> mExpectedIdentities;

    /**
     * Private constructor; instances should be instantiated through a {@link Builder} obtained
     * with {@link #builder}.
     */
    AppSignatureVerifier(Context context,
            Map<String, Map<String, Set<String>>> permissionAllowMap,
            Map<String, Set<String>> expectedIdentities,
            String digestAlgorithm,
            Cache cache) {
        mPackageManager = context.getPackageManager();
        mPermissionAllowMap = permissionAllowMap;
        mExpectedIdentities = expectedIdentities;
        mDigestAlgorithm = digestAlgorithm;
        mCache = cache;
    }

    /**
     * Returns a new {@link Builder} that can be used to instantiate a new {@code
     * AppSignatureVerifier}.
     */
    static Builder builder(Context context) {
        return new Builder(context);
    }

    /**
     * Provides methods to configure a new {@code AppSignatureVerifier} instance.
     */
    static class Builder {
        private final Context mContext;
        private String mDigestAlgorithm;
        private Cache mCache;
        private Map<String, Map<String, Set<String>>> mPermissionAllowMap;
        private Map<String, Set<String>> mExpectedIdentities;

        /**
         * Constructor accepting the {@code context} used to instantiate a new {@code
         * AppSignatureVerifier}.
         */
        Builder(Context context) {
            mContext = context;
        }

        /**
         * Sets the {@code digestAlgorithm} to be used by the {@code AppSignatureVerifier}; all
         * signing identities provided to {@link #setPermissionAllowMap} and
         * {@link #setExpectedIdentities} must be computed using this same {@code digestAlgorithm}.
         */
        Builder setDigestAlgorithm(String digestAlgorithm) {
            mDigestAlgorithm = digestAlgorithm;
            return this;
        }

        /**
         * Sets the {@code cache} to be used by the {@code AppSignatureVerifier}.
         */
        Builder setCache(Cache cache) {
            mCache = cache;
            return this;
        }

        /**
         * Sets the {@code permissionAllowMap} to be used by the {@code AppSignatureVerifier}.
         *
         * This {@code Map} should contain a mapping from permission names to a mapping of package
         * names to expected signing identities; each permission can also contain a mapping to
         * the {@link AppAuthenticator#ALL_PACKAGES_TAG} which allow signing identities to be
         * specified without knowing the exact packages that will be signed by them.
         */
        Builder setPermissionAllowMap(Map<String, Map<String, Set<String>>> permissionAllowMap) {
            mPermissionAllowMap = permissionAllowMap;
            return this;
        }

        /**
         * Sets the {@code expectedIdentities} to be used by the {@code AppSignatureVerifier}.
         *
         * This {@code Map} should contain a mapping from package name to the expected signing
         * certificate digest(s).
         */
        Builder setExpectedIdentities(Map<String, Set<String>> expectedIdentities) {
            mExpectedIdentities = expectedIdentities;
            return this;
        }

        /**
         * Builds a new {@code AppSignatureVerifier} instance using the provided configuration.
         */
        AppSignatureVerifier build() {
            if (mPermissionAllowMap == null) {
                mPermissionAllowMap = new ArrayMap<>();
            }
            if (mExpectedIdentities == null) {
                mExpectedIdentities = new ArrayMap<>();
            }
            if (mDigestAlgorithm == null) {
                mDigestAlgorithm = AppAuthenticator.DEFAULT_DIGEST_ALGORITHM;
            }
            if (mCache == null) {
                mCache = new Cache(DEFAULT_CACHE_SIZE);
            }
            return new AppSignatureVerifier(mContext, mPermissionAllowMap, mExpectedIdentities,
                    mDigestAlgorithm, mCache);
        }
    }

    /**
     * Verifies the signing identity of the provided {@code packageName} for the specified {@code
     * permission}, returning {@code true} if the signing identity matches that declared under
     * the {@code permission} for the {@code package} as specified to {@link
     * Builder#setPermissionAllowMap}.
     */
    boolean verifySigningIdentity(String packageName, String permission) {
        // If there are no declared expected certificate digests for the specified package or
        // all-packages under the permission then return immediately.
        Map<String, Set<String>> allowedCertDigests = mPermissionAllowMap.get(permission);
        if (allowedCertDigests == null) {
            Log.d(TAG, "No expected signing identities declared for permission " + permission);
            return false;
        }
        Set<String> packageCertDigests = allowedCertDigests.get(packageName);
        Set<String> allPackagesCertDigests =
                allowedCertDigests.get(AppAuthenticator.ALL_PACKAGES_TAG);
        if (packageCertDigests == null && allPackagesCertDigests == null) {
            return false;
        }
        return verifySigningIdentityForQuery(packageName, permission, packageCertDigests,
                allPackagesCertDigests);
    }

    /**
     * Verifies the signing identity of the provided {@code packageName} against the expected
     * signing identity set through {@link Builder#setExpectedIdentities(Map)}.
     */
    boolean verifyExpectedIdentity(String packageName) {
        Set<String> packageCertDigests = mExpectedIdentities.get(packageName);
        if (packageCertDigests == null) {
            return false;
        }
        return verifySigningIdentityForQuery(packageName, EXPECTED_IDENTITY_QUERY,
                packageCertDigests, null);
    }

    /**
     * Verifies the signing identity of the provided {@code packageName} based on the provided
     * {@code query} against the expected {@code packageCertDigests}, and where applicable the
     * {@code allPackageCertDigests}.
     *
     * The {@code query} can either be a permission or {@code EXPECTED_IDENTITY_QUERY} when
     * verifying the identity of another app before establishing communication.
     */
    boolean verifySigningIdentityForQuery(String packageName, String query,
            Set<String> packageCertDigests, Set<String> allPackagesCertDigests) {
        AppSigningInfo appSigningInfo;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                appSigningInfo = Api28Impl.getAppSigningInfo(mPackageManager, packageName);
            } else {
                appSigningInfo = DefaultImpl.getAppSigningInfo(mPackageManager, packageName);
            }
        } catch (AppSignatureVerifierException e) {
            Log.e(TAG, "Caught an exception obtaining signing info for package " + packageName, e);
            return false;
        }
        // If a previous verification result exists for this package and query, and the package
        // has not yet been updated, then use the result from the previous verification. An app's
        // signing identity can only be changed on an update which should result in an update of
        // the last update time.
        CacheEntry cacheEntry = mCache.get(packageName, query);
        if (cacheEntry != null
                && cacheEntry.getLastUpdateTime() == appSigningInfo.getLastUpdateTime()) {
            return cacheEntry.getVerificationResult();
        }
        boolean verificationResult;
        // API levels >= 28 support obtaining the signing lineage of a package after a key
        // rotation; if the signing lineage is available then verify each entry in the lineage
        // against the expected signing identities.
        if (appSigningInfo.getSigningLineage() != null) {
            verificationResult = verifySigningLineage(appSigningInfo.getSigningLineage(),
                    packageCertDigests, allPackagesCertDigests);
        } else {
            verificationResult = verifyCurrentSigners(appSigningInfo.getCurrentSignatures(),
                    packageCertDigests, allPackagesCertDigests);
        }
        mCache.put(packageName, query, CacheEntry.create(verificationResult,
                appSigningInfo.getLastUpdateTime()));
        return verificationResult;
    }

    /**
     * Verifies the provided {@code signatures} signing lineage against the expected signing
     * identities in the {@code packageCertDigests} and {@code allPackagesCertDigests}.
     *
     * <p>A signing identity is successfully verified if any of the signatures in the lineage
     * matches any of the expected signing certificate digest declarations in the provided {@code
     * Map}s.
     */
    private boolean verifySigningLineage(List<Signature> signatures, Set<String> packageCertDigests,
            Set<String> allPackagesCertDigests) {
        for (Signature signature : signatures) {
            String signatureDigest = AppAuthenticatorUtils.computeDigest(mDigestAlgorithm,
                    signature.toByteArray());
            if (packageCertDigests != null && packageCertDigests.contains(signatureDigest)) {
                return true;
            }
            if (allPackagesCertDigests != null
                    && allPackagesCertDigests.contains(signatureDigest)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies the provided current {@code signatures} against the expected signing identities
     * in the {@code packageCertDigests} and {@code allPackagesCertDigests}.
     *
     * <p>A signing identity is successfully verified if all of the current signers are
     * declared in either of the expected signing certificate digest {@code Map}s.
     */
    boolean verifyCurrentSigners(List<Signature> signatures, Set<String> packageCertDigests,
            Set<String> allPackagesCertDigests) {
        List<String> signatureDigests = new ArrayList<>(signatures.size());
        for (Signature signature : signatures) {
            signatureDigests.add(AppAuthenticatorUtils.computeDigest(mDigestAlgorithm,
                    signature.toByteArray()));
        }
        if (packageCertDigests != null && packageCertDigests.containsAll(signatureDigests)) {
            return true;
        }
        return allPackagesCertDigests != null
                && allPackagesCertDigests.containsAll(signatureDigests);
    }

    /**
     * Provides a method to support package signature queries for API levels >= 28. Starting at
     * API level 28 the platform added support for app signing key rotation, so apps signed by a
     * single signer can include the entire signing lineage from
     * {@link AppSigningInfo#getSigningLineage()}.
     */
    @RequiresApi(28)
    private static class Api28Impl {
        private Api28Impl() {
        }

        /**
         * Returns the {@link AppSigningInfo} for the specified {@code packageName} using the
         * provided {@code packageManager}, including full signing lineage for apps signed by a
         * single signer.
         *
         * @throws AppSignatureVerifierException if the specified package is not found, or if the
         * {@code SigningInfo} is not returned for the package.
         */
        static AppSigningInfo getAppSigningInfo(PackageManager packageManager,
                String packageName) throws AppSignatureVerifierException {
            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES);
            } catch (PackageManager.NameNotFoundException e) {
                throw new AppSignatureVerifierException("Package " + packageName + " not found", e);
            }
            if (packageInfo.signingInfo == null) {
                throw new AppSignatureVerifierException(
                        "No SigningInfo returned for package " + packageName);
            }
            return AppSigningInfo.create(packageName,
                    packageInfo.signingInfo.getApkContentsSigners(),
                    packageInfo.signingInfo.getSigningCertificateHistory(),
                    packageInfo.lastUpdateTime);
        }
    }

    /**
     * Provides a method to support package signature queries for API levels < 28. Prior to API
     * level 28 the platform only supported returning an app's original signature(s), and an app
     * signed with a rotated signing key will still return the original signing key when queried
     * with the {@link PackageManager#GET_SIGNATURES} flag.
     */
    private static class DefaultImpl {
        private DefaultImpl() {
        }

        /**
         * Returns the {@link AppSigningInfo} for the specified {@code packageName} using the
         * provided {@code packageManager}, containing only the original / current signer for the
         * package.
         *
         * @throws AppSignatureVerifierException if the specified package is not found, or if the
         * {@code signatures} are not returned for the package.
         */
        // Suppress the deprecation and GetSignatures warnings for the GET_SIGNATURES flag and the
        // use of PackageInfo.Signatures since this method is intended for API levels < 28 which
        // only support these.
        @SuppressWarnings("deprecation")
        @SuppressLint("PackageManagerGetSignatures")
        static AppSigningInfo getAppSigningInfo(PackageManager packageManager,
                String packageName) throws AppSignatureVerifierException {
            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);
            } catch (PackageManager.NameNotFoundException e) {
                throw new AppSignatureVerifierException("Package " + packageName + " not found", e);
            }
            if (packageInfo.signatures == null) {
                throw new AppSignatureVerifierException(
                        "No signatures returned for package " + packageName);
            }
            // When using the GET_SIGNATURES flag to obtain the app's signing info only the
            // current signers are returned, so set the lineage to null in the AppSigningInfo.
            return AppSigningInfo.create(packageName, packageInfo.signatures, null,
                    packageInfo.lastUpdateTime);
        }
    }

    /**
     * Cache containing previous signing identity version results stored by package name and
     * query where the query is either the permission name or the {@code EXPECTED_IDENTITY_QUERY}.
     */
    static class Cache extends LruCache<String, CacheEntry> {
        /**
         * Constructs a new {@code Cache} with the provided {@code maxSize}.
         */
        Cache(int maxSize) {
            super(maxSize);
        }

        /**
         * Returns the {@link CacheEntry} in the cache for the specified {@code packageName} and
         * {@code query}.
         */
        CacheEntry get(String packageName, String query) {
            return get(packageName + query);
        }

        /**
         * Puts the provided {@code cacheEntry} in the cache for the specified {@code packageName}
         * and {@code query}.
         */
        void put(String packageName, String query, CacheEntry cacheEntry) {
            put(packageName + query, cacheEntry);
        }
    }

    /**
     * Value class containing the verification result and the last update time for an entry in
     * the {@link Cache}.
     */
    @AutoValue
    abstract static class CacheEntry {
        abstract boolean getVerificationResult();
        abstract long getLastUpdateTime();

        /**
         * Creates a new instance with the provided {@code verificationResult} and {@code
         * lastUpdateTime}.
         */
        static CacheEntry create(boolean verificationResult, long lastUpdateTime) {
            return new AutoValue_AppSignatureVerifier_CacheEntry(verificationResult,
                    lastUpdateTime);
        }
    }

    /**
     * Value class containing generic signing info for a package.
     */
    // Suppressing the AutoValue immutable field warning as this class is only used internally
    // and is not worth bringing in the dependency for an ImmutableList.
    @SuppressWarnings("AutoValueImmutableFields")
    @AutoValue
    abstract static class AppSigningInfo {
        abstract String getPackageName();
        abstract List<Signature> getCurrentSignatures();
        @Nullable
        abstract List<Signature> getSigningLineage();
        abstract long getLastUpdateTime();

        /**
         * Creates a new instance with the provided {@code packageName}, {@code currentSignatures},
         * {@code signingLineage}, and {@code lastUpdateTime}.
         *
         * <p>Note, the {@code signingLineage} can be null as this was not available prior to API
         * level 28, but a non-null value must be specified for the {@code currentSignatures}.
         */
        static AppSigningInfo create(@NonNull String packageName,
                @NonNull Signature[] currentSignatures, Signature[] signingLineage,
                long lastUpdateTime) {
            return new AutoValue_AppSignatureVerifier_AppSigningInfo(packageName,
                    Arrays.asList(currentSignatures),
                    signingLineage != null ? Arrays.asList(signingLineage) : null,
                    lastUpdateTime);
        }
    }

    /**
     * This {@code Exception} is thrown when an unexpected error is encountered when querying for
     * or verifying package signing identities.
     */
    private static class AppSignatureVerifierException extends Exception {
        AppSignatureVerifierException(@NonNull String message) {
            super(message);
        }

        AppSignatureVerifierException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }
    }
}

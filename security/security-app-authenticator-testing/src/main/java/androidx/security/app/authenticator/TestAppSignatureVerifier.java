/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context;

import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.util.Map;
import java.util.Set;

/**
 * An extension of the {@link AppSignatureVerifier} used by the {@link AppAuthenticator} that can
 * be injected into the {@code AppAuthenticator} to configure it to behave as required by the test.
 *
 * <p>This test class supports setting a {@link TestAppAuthenticatorBuilder.TestPolicy},
 * configuring generic acceptance per package, specifying the signing identity per package, and
 * treating packages as not installed.
 */
class TestAppSignatureVerifier extends AppSignatureVerifier {
    /**
     * A Set of classes to be treated as always accepted as long as they are in the XML config file.
     */
    private final Set<String> mSignatureAcceptedPackages;
    /**
     * A Set of classes to be treated as not installed.
     */
    private final Set<String> mNotInstalledPackages;
    /**
     * A mapping from the package name to the digest to be used as the signing identity for the
     * package during the test.
     */
    private final Map<String, String> mSigningIdentities;
    /**
     * The test policy to be used.
     */
    private final @TestAppAuthenticatorBuilder.TestPolicy int mTestPolicy;

    /**
     * Constructor that should only be invoked by the {@link Builder}.
     */
    TestAppSignatureVerifier(Context context,
            Map<String, Map<String, Set<String>>> permissionAllowMap,
            Map<String, Set<String>> expectedIdentities,
            Set<String> signatureAcceptedPackages,
            Set<String> notInstalledPackages,
            Map<String, String> signingIdentities,
            @TestAppAuthenticatorBuilder.TestPolicy int testPolicy) {
        super(context, permissionAllowMap, expectedIdentities,
                AppAuthenticator.DEFAULT_DIGEST_ALGORITHM, new NullCache());
        mSignatureAcceptedPackages = signatureAcceptedPackages;
        mNotInstalledPackages = notInstalledPackages;
        mSigningIdentities = signingIdentities;
        mTestPolicy = testPolicy;
    }

    /*
     * Builder for a new {@link TestAppSignatureVerifier} that allows this test class to be
     * configured as required for the test.
     */
    static class Builder {
        private final Context mContext;
        private Map<String, Map<String, Set<String>>> mPermissionAllowMap;
        private Map<String, Set<String>> mExpectedIdentities;
        private Set<String> mSignatureAcceptedPackages;
        private Set<String> mNotInstalledPackages;
        private String mDigestAlgorithm;
        private Map<String, String> mSigningIdentities;
        private @ TestAppAuthenticatorBuilder.TestPolicy int mTestPolicy;

        /**
         * Constructor accepting the {@code context} used to instantiate a new {@code
         * TestAppSignatureVerifier}.
         */
        Builder(Context context) {
            mContext = context;
            mSignatureAcceptedPackages = new ArraySet<>();
            mNotInstalledPackages = new ArraySet<>();
            mSigningIdentities = new ArrayMap<>();
        }

        /**
         * Configures the resulting {@link TestAppSignatureVerifier} to always return that the
         * signing identity matches the expected value when the specified {@code packageName} is
         * queried.
         *
         * @param packageName the name of the package for which the signing identity should be
         *                    treated as matching the expected value
         * @return this instance of the {@code Builder}
         */
        Builder setSignatureAcceptedForPackage(String packageName) {
            mSignatureAcceptedPackages.add(packageName);
            return this;
        }

        /**
         * Sets the provided {@code certDigest} as the signing identity for the specified {@code
         * packageName}.
         *
         * @param packageName the name of the package that will use the provided signing identity
         * @param certDigest the digest to be treated as the signing identity of the specified
         *                  package
         * @return this instance of the {@code Builder}
         */
        Builder setSigningIdentityForPackage(String packageName, String certDigest) {
            mSigningIdentities.put(packageName, certDigest);
            return this;
        }

        /**
         * Sets the {@code permissionAllowMap} to be used by the {@code TestAppSignatureVerifier}.
         *
         * This {@code Map} should contain a mapping from permission names to a mapping of package
         * names to expected signing identities; each permission can also contain a mapping to
         * the {@link AppAuthenticator#ALL_PACKAGES_TAG} which allow signing identities to be
         * specified without knowing the exact packages that will be signed by them.
         *
         * @return this instance of the {@code Builder}
         */
        Builder setPermissionAllowMap(Map<String, Map<String, Set<String>>> permissionAllowMap) {
            mPermissionAllowMap = permissionAllowMap;
            return this;
        }

        /**
         * Sets the {@code expectedIdentities} to be used by the {@code TestAppSignatureVerifier}.
         *
         * This {@code Map} should contain a mapping from package name to the expected signing
         * certificate digest(s).
         *
         * @return this instance of the {@code Builder}
         */
        Builder setExpectedIdentities(Map<String, Set<String>> expectedIdentities) {
            mExpectedIdentities = expectedIdentities;
            return this;
        }

        /**
         * Sets the test policy to be used by the {@code TestAppSignatureVerifier}.
         *
         * @return this instance of the {@code Builder}
         */
        Builder setTestPolicy(@ TestAppAuthenticatorBuilder.TestPolicy int testPolicy) {
            mTestPolicy = testPolicy;
            return this;
        }

        /**
         * Treats the provided {@code packageName} as not being installed by the resulting {@link
         * TestAppSignatureVerifier}.
         *
         * @param packageName the name of the package to be treated as not installed
         * @return this instance of the {@code Builder}
         */
        Builder setPackageNotInstalled(String packageName) {
            mNotInstalledPackages.add(packageName);
            return this;
        }

        /**
         * Builds a new {@code TestAppSignatureVerifier} instance using the provided configuration.
         */
        TestAppSignatureVerifier build() {
            if (mPermissionAllowMap == null) {
                mPermissionAllowMap = new ArrayMap<>();
            }
            if (mExpectedIdentities == null) {
                mExpectedIdentities = new ArrayMap<>();
            }
            if (mDigestAlgorithm == null) {
                mDigestAlgorithm = AppAuthenticator.DEFAULT_DIGEST_ALGORITHM;
            }
            return new TestAppSignatureVerifier(mContext, mPermissionAllowMap, mExpectedIdentities,
                    mSignatureAcceptedPackages, mNotInstalledPackages, mSigningIdentities,
                    mTestPolicy);
        }
    }

    /**
     * Responds to a signing identity query using the specified config for the provided {@code
     * packageName} where the package is expected to have the signing identity in the {@code
     * packageCertDigests} and, where applicable, {@code all-packages} are supported with the
     * {@code allPackagesCertDigests}.
     *
     * <p>Package queries are performed in the following order:
     * <ul>
     *     <li>If the test policy is {@code POLICY_DENY_ALL} then {@code false} is returned</li>
     *     {li>If the test policy is {@code POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES} then
     *     {@code true} is returned as long as the specified package is explicitly declared with one
     *     or more signing identities for this query</li>
     *     <li>If the package is configured to be treated as not installed {@code false} is
     *     returned</li>
     *     <li>If the package is configured to have its signing identity accepted then {@code
     *     true} is returned</li>
     *     <li>If a signing identity is configured for the package then it is compared against
     *     the expected signing identity declared in the XML config; if there is a match then
     *     {@code true} is returned</li>
     * </ul>
     * @param packageName the name of the package being queried
     * @param query the type of query being performed
     * @param packageCertDigests a {@code Set} of certificate digests that are expected for the
     *                           package
     * @param allPackagesCertDigests a {@code Set} of certificate digests that are expected for
     *                               any package for this query
     * @return {@code true} if the package can be treated as successfully verified based on the
     * test configuration
     */
    @Override
    boolean verifySigningIdentityForQuery(String packageName, String query,
            Set<String> packageCertDigests, Set<String> allPackagesCertDigests) {
        if (mTestPolicy ==  TestAppAuthenticatorBuilder.POLICY_DENY_ALL) {
            return false;
        }
        if (mTestPolicy
                == TestAppAuthenticatorBuilder.POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES) {
            // packageCertDigests will only be set if the package is explicitly declared for the
            // query
            return packageCertDigests != null;
        }
        if (mNotInstalledPackages.contains(packageName)) {
            return false;
        }
        if (mSignatureAcceptedPackages.contains(packageName)) {
            return true;
        }
        String certDigest = mSigningIdentities.get(packageName);
        if (certDigest != null) {
            if (packageCertDigests != null && packageCertDigests.contains(certDigest)) {
                return true;
            }
            if (allPackagesCertDigests != null && allPackagesCertDigests.contains(certDigest)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A test version of the {@code Cache} that always returns {@code null} for a cache query;
     * this is intended to always force the test to go through the configured verification
     * process as opposed to returning a previous query result.
     */
    static class NullCache extends Cache {
        /**
         * Instantiates a new NullCache; since it is not intended to return cached values a max
         * size is not accepted, but a value of 1 is used since a value <= 0 is treated as an
         * error by the {@link androidx.collection.LruCache}.
         */
        NullCache() {
            super(1);
        }

        /**
         * Overrides the {@link Cache#get} method to return a null value for all cache queries.
         */
        @Override
        CacheEntry get(String packageName, String query) {
            return null;
        }
    }
}

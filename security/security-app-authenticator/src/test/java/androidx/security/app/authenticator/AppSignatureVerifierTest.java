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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import androidx.collection.ArrayMap;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
// API Level 28 introduced signing key rotation, so run the tests with and without rotation support.
@Config(minSdk = 27, maxSdk = 28)
// This test class supports setting the results for the GET_SIGNATURES flag which is deprecated in
// API levels >= 28 but is the only option available to obtain a package's signing info in
// API levels < 28.
@SuppressWarnings("deprecation")
public class AppSignatureVerifierTest {
    private static final String TEST_PACKAGE_NAME = "com.android.testapp";
    private static final String TEST_PERMISSION_NAME = "com.android.testapp.TEST_PERMISSION";
    private static final long LAST_UPDATE_TIME = 1234;

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String SIGNATURE1 = "01234567890a";
    private static final String SIGNATURE2 = "abcdef012345";
    private static final String SIGNATURE3 = "543201fedcba";
    private static final String SIGNATURE1_DIGEST =
            "1bb82badeb591f3a7ba3f82e938d9364e35b2fc649da7d4aea29313cb5214e0c";
    private static final String SIGNATURE2_DIGEST =
            "91a5dc2d6f379fcabb87d7d131a1ebccf789cfc3a4716f622adc0086b8d9742b";
    private static final String SIGNATURE3_DIGEST =
            "1b39648cad5202eeec496cc224d138c7744bb8675a734e29cae723de3fccad3d";

    @Mock
    private Context mMockContext;
    @Mock
    private PackageManager mMockPackageManager;

    private AppSignatureVerifierTestBuilder mBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mMockContext.getPackageManager()).thenReturn(mMockPackageManager);
        mBuilder = new AppSignatureVerifierTestBuilder(mMockContext);
    }

    @Test
    public void verifySigningIdentity_oneSignerDigestInPackageCertSet() throws Exception {
        // When a package only has a single signer and that signer's digest is in the package
        // cert Set then the verifier should return true.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_oneSignerDigestInAllPackagesCertSet() throws Exception {
        // When a package only has a single signer and that signer's digest is in the all-packages
        // cert Set then the verifier should return true.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setAllPackagesCertDigestsForPermission(Set.of(SIGNATURE1_DIGEST),
                        TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_oneSignerDigestNotInPackageCertSet() throws Exception {
        // When a package only has a single signer and that signer's digest is not in the package
        // cert Set then the verifier should return false.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_oneSignerDigestNotInAllPackagesCertSet() throws Exception {
        // When a package only has a single signer and that signer's digest is not in the
        // all-packages cert Set then the verifier should return false.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setAllPackagesCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST),
                        TEST_PERMISSION_NAME)
                .build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_multipleSignersDigestsInPackageCertSet() throws Exception {
        // When a package is signed with multiple signers all of the digests of the signers must
        // be in one of the Sets; this test verifies when the package cert Set contains all of the
        // signing digests then the verifier returns true.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST, SIGNATURE1_DIGEST),
                        TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_multipleSignersDigestsInAllPackagesCertSet()
            throws Exception {
        // When a package is signed with multiple signers all of the digests of the signers must
        // be in one of the Sets; this test verifies when the all-packages cert Set contains all of
        // the signing digests then the verifier returns true.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setAllPackagesCertDigestsForPermission(
                        Set.of(SIGNATURE2_DIGEST, SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_multipleSignersNotInEitherSet() throws Exception {
        // When a package is signed with multiple signers and neither of the sets contains all of
        // the signers the Verifier should return false.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .setAllPackagesCertDigestsForPermission(
                        Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_unknownPackageName() throws Exception {
        // When a package name is specified that is not on the device the #getPackageInfo call
        // should result in a NameNotFoundException; when this is caught the verifier should
        // return false.
        final String unknownPackageName = "com.android.unknown";
        when(mMockPackageManager.getPackageInfo(unknownPackageName,
                PackageManager.GET_SIGNATURES)).thenThrow(
                PackageManager.NameNotFoundException.class);
        // API Level 28 introduced the GET_SIGNING_CERTIFICATES flag to obtain the full signing
        // lineage of a package.
        if (Build.VERSION.SDK_INT >= 28) {
            when(mMockPackageManager.getPackageInfo(unknownPackageName,
                    PackageManager.GET_SIGNING_CERTIFICATES)).thenThrow(
                    PackageManager.NameNotFoundException.class);
        }
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertFalse(verifier.verifySigningIdentity(unknownPackageName, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_emptyDigestSets() throws Exception {
        // When both the package and all-packages cert digest Sets are empty then
        // verifySigningIdentity should return false immediately without querying the
        // PackageManager for the signatures.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1).build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_rotatedKeyOriginalDigestInPackageCertSet() throws Exception {
        // When a package is signed with a rotated signing key the signing lineage should be
        // included in the SigningInfo result (for API >= 28), or the original signer in the
        // signatures array. When the first signer is in the package cert digest Set then the
        // verifier should return true for all API levels.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_rotatedKeyOriginalDigestInAllPackagesCertSet()
            throws Exception {
        // When a package is signed with a rotated signing key the signing lineage should be
        // included in the SigningInfo result (for API >= 28), or the original signer in the
        // signatures array. When the first signer is in the all-packages cert digest Set then the
        // verifier should return true for all API levels.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setAllPackagesCertDigestsForPermission(
                        Set.of(SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    @Config(maxSdk = 27)
    public void verifySigningIdentity_rotatedKeyDigestInPackageCertSetApi27() throws Exception {
        // When a package is signed with a rotated signing key and only this newly rotated
        // signing key is in the digest Sets then API levels < 28 will return false because only
        // the original signer of the package will be returned from the #getPackageInfo API. This
        // test is intended to stress if an app is targeting API levels < 28 and interacting with
        // packages with rotated keys the original signing key must also be included in the
        // allow-list.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    @Config(maxSdk = 27)
    public void verifySigningIdentity_rotatedKeyDigestInAllPackagesCertSetApi27() throws Exception {
        // Similar to above if only the rotated key is in the all-packages cert set then for API
        // levels < 28 the verifier will return false since it will only have the original
        // signing certificate to compare against.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setAllPackagesCertDigestsForPermission(
                        Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    @Config(minSdk = 28)
    public void verifySigningIdentity_rotatedKeyDigestInPackageCertSetApi28() throws Exception {
        // When a package is signed with a rotated signing key and only this newly rotated
        // signing key is in the package cert Set then API Level 28+ devices should return true
        // for the rotated key, but since earlier API Levels only see the original signing key
        // they would return false.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    @Config(minSdk = 28)
    public void verifySigningIdentity_rotatedKeyDigestInAllPackagesCertSetApi28() throws Exception {
        // When a package is signed with a rotated signing key and only this newly rotated
        // signing key is in the all-packages cert Set then API Level 28+ devices should return
        // true for the rotated key, but since earlier API Levels only see the original signing key
        // they would return false.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setAllPackagesCertDigestsForPermission(
                        Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifyExpectedIdentity_singleSignerInSet() throws Exception {
        // Since an app typically verifies the signing identity of an app before establishing
        // communication with that app they are not tied to a permission and all-package
        // declarations are not allowed. This test verifies if an app is signed with a single
        // signing key and that signing certificate's digest is in the Set then the verifier
        // returns true.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setExpectedIdentities(Set.of(SIGNATURE2_DIGEST, SIGNATURE1_DIGEST))
                .build();

        assertTrue(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    public void verifyExpectedIdentity_singleSignerNotInSet() throws Exception {
        // When a package is signed by a single signer and that signer is not in the Set then the
        // verifier should return false.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setExpectedIdentities(Set.of(SIGNATURE2_DIGEST))
                .build();

        assertFalse(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    public void verifyExpectedIdentity_multipleSignersInSet() throws Exception {
        // When a package is signed by multiple signers and all of the signers are in the Set
        // then the verifier should return true.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setExpectedIdentities(
                        Set.of(SIGNATURE1_DIGEST, SIGNATURE2_DIGEST, SIGNATURE3_DIGEST))
                .build();

        assertTrue(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    public void verifyExpectedIdentity_multipleSignersNotInSet() throws Exception {
        // When a package is signed by multiple signers all of the signers must be in the Set;
        // this test verifies is only one of the signers is in the Set then the verifier returns
        // false.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setExpectedIdentities(Set.of(SIGNATURE1_DIGEST, SIGNATURE3_DIGEST))
                .build();

        assertFalse(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    public void verifyExpectedIdentity_rotatedKeyOriginalInSet() throws Exception {
        // When a package is signed with a rotated key and the original signing key is in the Set
        // then the verifier should return true.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setExpectedIdentities(Set.of(SIGNATURE1_DIGEST))
                .build();

        assertTrue(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    @Config(maxSdk = 27)
    public void verifyExpectedIdentity_rotatedKeyOnlyNewInSetApi27() throws Exception {
        // When a package is signed with a rotated key API levels < 28 will only return the
        // original signing key. If an app is targeting API levels < 28 and only the new signing
        // certificate digest is in the Set then the verifier will return false on these API levels.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setExpectedIdentities(Set.of(SIGNATURE2_DIGEST))
                .build();

        assertFalse(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    @Config(minSdk = 28)
    public void verifyExpectedIdentity_rotatedKeyOnlyNewInSetApi28() throws Exception {
        // When a package is signed with a rotated key API levels >= 28 will return the current
        // signing certificate as well as the previous signers in the lineage. This test verifies
        // if only the new signing certificate digest is in the Set then the verifier will return
        // true for API levels >= 28.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addCurrentSigner(SIGNATURE2)
                .setExpectedIdentities(Set.of(SIGNATURE2_DIGEST))
                .build();

        assertTrue(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    @Config(minSdk = 28)
    public void verifyExpectedIdentity_multipleKeyRotationsMiddleInSetApi28() throws Exception {
        // This test verifies if a package's signing key has been rotated multiple times and only
        // one of the intermediate signing certificate digests is in the Set the verifier will
        // match this certificate from the lineage and return true.
        AppSignatureVerifier verifier = mBuilder.addSignerInLineage(SIGNATURE1)
                .addSignerInLineage(SIGNATURE2)
                .addCurrentSigner(SIGNATURE3)
                .setExpectedIdentities(Set.of(SIGNATURE2_DIGEST))
                .build();

        assertTrue(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    public void verifyExpectedIdentity_singleSignerNoDigestsInSet() throws Exception {
        // If the caller has not specified any expected signing certificate digests for a package
        // then the verifier should return false without querying the PackageManager.
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1).build();

        assertFalse(verifier.verifyExpectedIdentity(TEST_PACKAGE_NAME));
    }

    @Test
    public void verifySigningIdentity_valueWrittenToCache() throws Exception {
        // When a package is first verified its signing certificate digest(s) are computed and
        // compared against the expected certificates. Since a package's signing identity cannot
        // change without an update the verifier uses a cache to keep track of the results of
        // previous queries; this test verifies when a package is successfully verified that
        // package and its verification results are written to the cache.
        AppSignatureVerifier.Cache cache = new AppSignatureVerifier.Cache(1);
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE1_DIGEST), TEST_PERMISSION_NAME)
                .setCache(cache)
                .build();
        verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME);
        AppSignatureVerifier.CacheEntry cacheEntry =
                cache.get(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME);

        assertTrue(cacheEntry.getVerificationResult());
    }

    @Test
    public void verifySigningIdentity_requestInCache() throws Exception {
        // After a package has been verified if it has not been updated before a subsequent
        // request is made the verification result from the cache can be used. This test
        // intentionally sets a different signing identity from that of the package being
        // verified to ensure the value is taken from the cache; note that on a real device a
        // signature change like this would not be possible without an app update.
        AppSignatureVerifier.Cache cache = new AppSignatureVerifier.Cache(1);
        cache.put(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME,
                AppSignatureVerifier.CacheEntry.create(true, LAST_UPDATE_TIME));
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .setCache(cache)
                .build();

        assertTrue(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    @Test
    public void verifySigningIdentity_packageUpdatedAfterCacheEntryCreated() throws Exception {
        // When an app is updated or removed / reinstalled the signing identity of the app can
        // change; the verifier uses the lastUpdateTime to determine if an app has been updated
        // and if its signatures need to be verified again. This test verifies if a package
        // previously passed the verification but has since been updated with a new signing
        // identity the cache entry is invalidated and the verifier returns false.
        AppSignatureVerifier.Cache cache = new AppSignatureVerifier.Cache(1);
        cache.put(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME,
                AppSignatureVerifier.CacheEntry.create(true, LAST_UPDATE_TIME));
        AppSignatureVerifier verifier = mBuilder.addCurrentSigner(SIGNATURE1)
                .setPackageCertDigestsForPermission(Set.of(SIGNATURE2_DIGEST), TEST_PERMISSION_NAME)
                .setCache(cache)
                .setLastUpdateTime(LAST_UPDATE_TIME + 1)
                .build();

        assertFalse(verifier.verifySigningIdentity(TEST_PACKAGE_NAME, TEST_PERMISSION_NAME));
    }

    /**
     * Utility test builder that can be used to configure the AppSignatureVerifier under test as
     * well as the signing identity to be returned by the {@link PackageManager#getPackageInfo}
     * query for the package with name {@code TEST_PACKAGE_NAME}.
     */
    private static class AppSignatureVerifierTestBuilder {
        private final List<Signature> mCurrentSigners;
        private final List<Signature> mSigningLineage;
        private final PackageManager mMockPackageManager;
        private long mLastUpdateTime;
        private final AppSignatureVerifier.Builder mBuilder;
        private Map<String, Map<String, Set<String>>> mPermissionAllowMap;
        private Map<String, Set<String>> mExpectedIdentities;

        private AppSignatureVerifierTestBuilder(Context mockContext) {
            mCurrentSigners = new ArrayList<>();
            mSigningLineage = new ArrayList<>();
            mMockPackageManager = mockContext.getPackageManager();
            mLastUpdateTime = LAST_UPDATE_TIME;
            mBuilder = AppSignatureVerifier.builder(mockContext);
        }

        private AppSignatureVerifierTestBuilder addCurrentSigner(String signer) {
            mCurrentSigners.add(new Signature(signer));
            return this;
        }

        private AppSignatureVerifierTestBuilder addSignerInLineage(String signer) {
            mSigningLineage.add(new Signature(signer));
            return this;
        }

        private AppSignatureVerifierTestBuilder setLastUpdateTime(long lastUpdateTime) {
            mLastUpdateTime = lastUpdateTime;
            return this;
        }

        private AppSignatureVerifierTestBuilder setCache(AppSignatureVerifier.Cache cache) {
            mBuilder.setCache(cache);
            return this;
        }

        private AppSignatureVerifierTestBuilder setPackageCertDigestsForPermission(
                Set<String> packageCertDigests, String permission) {
            Map<String, Set<String>> allowedPackageCerts = null;
            if (mPermissionAllowMap == null) {
                mPermissionAllowMap = new ArrayMap<>();
            } else {
                allowedPackageCerts = mPermissionAllowMap.get(permission);
            }
            if (allowedPackageCerts == null) {
                allowedPackageCerts = new ArrayMap<>();
            }
            allowedPackageCerts.put(TEST_PACKAGE_NAME, packageCertDigests);
            mPermissionAllowMap.put(permission, allowedPackageCerts);
            return this;
        }

        private AppSignatureVerifierTestBuilder setAllPackagesCertDigestsForPermission(
                Set<String> allPackagesCertDigests, String permission) {
            Map<String, Set<String>> allowedPackageCerts = null;
            if (mPermissionAllowMap == null) {
                mPermissionAllowMap = new ArrayMap<>();
            } else {
                allowedPackageCerts = mPermissionAllowMap.get(permission);
            }
            if (allowedPackageCerts == null) {
                allowedPackageCerts = new ArrayMap<>();
            }
            allowedPackageCerts.put(AppAuthenticator.ALL_PACKAGES_TAG, allPackagesCertDigests);
            mPermissionAllowMap.put(permission, allowedPackageCerts);
            return this;
        }

        private AppSignatureVerifierTestBuilder setExpectedIdentities(
                Set<String> expectedIdentities) {
            if (mExpectedIdentities == null) {
                mExpectedIdentities = new ArrayMap<>();
            }
            mExpectedIdentities.put(TEST_PACKAGE_NAME, expectedIdentities);
            return this;
        }

        private AppSignatureVerifier build() throws Exception {
            if (mCurrentSigners.isEmpty()) {
                throw new IllegalArgumentException("At least one current signer must be specified");
            }
            Signature[] signatures = new Signature[mCurrentSigners.size()];
            mCurrentSigners.toArray(signatures);
            // If there is more than one current signer then the SigningInfo should return null
            // for the lineage.
            Signature[] signingLineage = null;
            if (mCurrentSigners.size() == 1) {
                // When there is only a single signer the current signer should be the last element
                // in the signing lineage.
                mSigningLineage.add(mCurrentSigners.get(0));
                signingLineage = new Signature[mSigningLineage.size()];
                mSigningLineage.toArray(signingLineage);
            }

            PackageInfo packageInfo = new PackageInfo();
            // In the case of a rotated signing key the GET_SIGNATURES result will return the
            // original signing key in the signatures array, so if the signing lineage is not
            // empty then use the first key from the lineage for the signatures.
            packageInfo.signatures = signingLineage != null
                    ? new Signature[]{signingLineage[0]} : signatures;
            packageInfo.packageName = TEST_PACKAGE_NAME;
            packageInfo.lastUpdateTime = mLastUpdateTime;
            when(mMockPackageManager.getPackageInfo(TEST_PACKAGE_NAME,
                    PackageManager.GET_SIGNATURES)).thenReturn(packageInfo);
            // API Level 28 introduced the SigningInfo to the PackageInfo object which is populated
            // when the PackageManager.GET_SIGNING_CERTIFICATES flag is specified.
            if (Build.VERSION.SDK_INT >= 28) {
                packageInfo = new PackageInfo();
                SigningInfo signingInfo = Shadow.newInstanceOf(SigningInfo.class);
                shadowOf(signingInfo).setSignatures(signatures);
                shadowOf(signingInfo).setPastSigningCertificates(signingLineage);
                packageInfo.signingInfo = signingInfo;
                packageInfo.packageName = TEST_PACKAGE_NAME;
                packageInfo.lastUpdateTime = mLastUpdateTime;
                when(mMockPackageManager.getPackageInfo(TEST_PACKAGE_NAME,
                        PackageManager.GET_SIGNING_CERTIFICATES)).thenReturn(packageInfo);
            }
            // Build the AppSignatureVerifier with the provided parameters to be used for the test.
            return mBuilder.setPermissionAllowMap(mPermissionAllowMap)
                    .setExpectedIdentities(mExpectedIdentities)
                    .setDigestAlgorithm(DIGEST_ALGORITHM)
                    .build();
        }
    }
}

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.security.app.authenticator.testing.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;

import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class TestAppSignatureVerifierTest {
    private static final String DECLARED_PACKAGE = "com.android.app1";
    private static final String UNDECLARED_PACKAGE = "com.android.undeclared.app";
    private static final String QUERY_TYPE = "test";
    private static final String TEST_PERMISSION =
            "androidx.security.app.authenticator.TEST_PERMISSION";

    private TestAppSignatureVerifier.Builder mBuilder;
    private Set<String> mPackageCertDigests;
    private Set<String> mAllPackagesCertDigests;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mBuilder = new TestAppSignatureVerifier.Builder(context);

        XmlPullParser parser = context.getResources().getXml(R.xml.test_config);
        AppAuthenticator.AppAuthenticatorConfig config =
                AppAuthenticator.createConfigFromParser(parser);
        Map<String, Map<String, Set<String>>> permissionAllowMap = config.getPermissionAllowMap();
        mBuilder.setPermissionAllowMap(permissionAllowMap);
        mBuilder.setExpectedIdentities(config.getExpectedIdentities());
        // Set the test policy to custom since a majority of these test will use a custom config,
        // and those that test a policy will set it explicitly.
        mBuilder.setTestPolicy(TestAppAuthenticatorBuilder.POLICY_CUSTOM);
        mPackageCertDigests = permissionAllowMap.get(TEST_PERMISSION).get(DECLARED_PACKAGE);
        mAllPackagesCertDigests =
                permissionAllowMap.get(TEST_PERMISSION).get(AppAuthenticator.ALL_PACKAGES_TAG);
    }

    @Test
    public void verifySigningIdentityForQuery_policyAcceptDeclared_returnsExpectedResult()
            throws Exception {
        // The TestAppSignatureVerifier supports specifying a test policy; this test verifies the
        // policy that accepts all declared packages does properly accept a package declared in
        // the XML config and does not accept one that could potentially pass through an
        // all-packages declaration.
        TestAppSignatureVerifier verifier =
                mBuilder.setTestPolicy(
                        TestAppAuthenticatorBuilder.POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES)
                        .build();

        assertTrue(verifier.verifySigningIdentityForQuery(DECLARED_PACKAGE, QUERY_TYPE,
                mPackageCertDigests, mAllPackagesCertDigests));
        assertFalse(verifier.verifySigningIdentityForQuery(UNDECLARED_PACKAGE, QUERY_TYPE, null,
                mAllPackagesCertDigests));
    }

    @Test
    public void verifySigningIdentityForQuery_policyDenyAll_returnsNoMatch() throws Exception {
        // The POLICY_DENY_ALL should cause all queries to be denied, even for packages
        // explicitly declared in the app-authenticator's XML configuration.
        TestAppSignatureVerifier verifier =
                mBuilder.setTestPolicy(TestAppAuthenticatorBuilder.POLICY_DENY_ALL).build();

        assertFalse(verifier.verifySigningIdentityForQuery(DECLARED_PACKAGE, QUERY_TYPE,
                mPackageCertDigests, mAllPackagesCertDigests));
        assertFalse(verifier.verifySigningIdentityForQuery(UNDECLARED_PACKAGE, QUERY_TYPE, null,
                mAllPackagesCertDigests));
    }

    @Test
    public void verifySigningIdentityForQuery_packageNotInstalled_returnsNoMatch()
            throws Exception {
        // The TestAppSignatureVerifier can be configured to treat apps as not installed; since
        // they do not have a signing identity on the device this should cause the verifier to
        // return no match for the exp signing identity.
        TestAppSignatureVerifier verifier =
                mBuilder.setPackageNotInstalled(DECLARED_PACKAGE).build();

        assertFalse(verifier.verifySigningIdentityForQuery(DECLARED_PACKAGE, QUERY_TYPE,
                mPackageCertDigests, mAllPackagesCertDigests));
    }

    @Test
    public void verifySigningIdentityForQuery_packageSignatureAccepted_returnsMatch()
            throws Exception {
        // The TestAppSignatureVerifier can be configured to treat an app's signing identity as
        // accepted.
        TestAppSignatureVerifier verifier =
                mBuilder.setSignatureAcceptedForPackage(DECLARED_PACKAGE).build();

        assertTrue(verifier.verifySigningIdentityForQuery(DECLARED_PACKAGE, QUERY_TYPE,
                mPackageCertDigests, mAllPackagesCertDigests));
    }

    @Test
    public void verifySigningIdentityForQuery_packageSignatureSet_returnsExpectedValue()
            throws Exception {
        // The TestAppSignatureVerifier supports setting an explicit signing identity for a package;
        // this can be used to test both that the configured identity matches the expected
        // identity from the config file, but can also be used as a test case to verify a signing
        // identity that is no longer trusted is not added back to the config file.
        TestAppSignatureVerifier verifier =
                mBuilder.setSigningIdentityForPackage(DECLARED_PACKAGE,
                        "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8")
                        .build();
        // Use new Sets of package certs that do not match the configured digest above; note that
        // the all-packages set is required as well since the config does have the same
        // certificate under that element too.
        Set<String> newPackageCertDigests = Set.of(
                "f2ca1bb6c7e907d06dafe4687e579fce76b37e4e93b7605022da52e6ccc26fd2");
        Set<String> newAllPackagesCertDigests = Set.of(
                "7d6fd7774f0d87624da6dcf16d0d3d104c3191e771fbe2f39c86aed4b2bf1a0f");

        assertTrue(verifier.verifySigningIdentityForQuery(DECLARED_PACKAGE, QUERY_TYPE,
                mPackageCertDigests, mAllPackagesCertDigests));
        assertFalse(verifier.verifySigningIdentityForQuery(DECLARED_PACKAGE, QUERY_TYPE,
                newPackageCertDigests, newAllPackagesCertDigests));
    }
}

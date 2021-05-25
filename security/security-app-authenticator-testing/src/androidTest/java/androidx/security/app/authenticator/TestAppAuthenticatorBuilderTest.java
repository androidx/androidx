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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.content.res.Resources;

import androidx.security.app.authenticator.testing.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class TestAppAuthenticatorBuilderTest {
    private static final String DECLARED_PACKAGE1 = "com.android.app1";
    private static final String DECLARED_PACKAGE2 = "com.android.app2";
    private static final String UNDECLARED_PACKAGE = "com.android.undeclared.app";
    private static final String EXPECTED_IDENTITY_PACKAGE = "com.social.app";
    private static final String TEST_PERMISSION =
            "androidx.security.app.authenticator.TEST_PERMISSION";

    private Context mContext;
    private Resources mResources;
    private TestAppAuthenticatorBuilder mBuilderFromResource;
    private TestAppAuthenticatorBuilder mBuilderFromInputStream;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mResources = mContext.getResources();
        mBuilderFromResource = TestAppAuthenticatorBuilder.createFromResource(mContext,
                R.xml.test_config);
        mBuilderFromInputStream = TestAppAuthenticatorBuilder.createFromInputStream(mContext,
                mResources.openRawResource(R.raw.test_config));
    }

    @Test
    public void verifyAppIdentity_defaultPolicyDeclaredPackage_returnsMatch() throws Exception {
        // By default the TestAppAuthenticator returns a test instance that will report all declared
        // packages have the expected signing identity. This test verifies this default behavior
        // using the declared test packages from the config.
        AppAuthenticator appAuthenticatorFromResource = mBuilderFromResource.build();
        AppAuthenticator appAuthenticatorFromInputStream = mBuilderFromInputStream.build();

        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.SIGNATURE_MATCH,
                appAuthenticatorFromResource.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertEquals(AppAuthenticator.SIGNATURE_MATCH,
                appAuthenticatorFromInputStream.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        // Since the AppAuthenticator with this policy should return permission granted the
        // enforce version of this method should not throw any exceptions.
        appAuthenticatorFromResource.enforceCallingAppIdentity(
                DECLARED_PACKAGE1, TEST_PERMISSION);
        appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                DECLARED_PACKAGE1, TEST_PERMISSION);
        appAuthenticatorFromResource.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE);
        appAuthenticatorFromInputStream.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE);
    }

    @Test
    public void verifyAppIdentity_defaultPolicyUndeclaredPackage_returnsNoMatch() throws Exception {
        // This test verifies the default policy used by the instance returned from the
        // TestAppAuthenticator will report a package as not having an expected signing identity
        // if it is not declared in the XML config.
        AppAuthenticator appAuthenticatorFromResource = mBuilderFromResource.build();
        AppAuthenticator appAuthenticatorFromInputStream = mBuilderFromInputStream.build();

        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        UNDECLARED_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        UNDECLARED_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                appAuthenticatorFromResource.checkAppIdentity(UNDECLARED_PACKAGE));
        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                appAuthenticatorFromInputStream.checkAppIdentity(UNDECLARED_PACKAGE));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceCallingAppIdentity(
                        UNDECLARED_PACKAGE, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                        UNDECLARED_PACKAGE, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceAppIdentity(UNDECLARED_PACKAGE));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceAppIdentity(UNDECLARED_PACKAGE));


    }

    @Test
    public void verifyAppIdentity_denyAllPolicyDeclaredPackage_returnsNoMatch() throws Exception {
        // The TestAppAuthenticator also provides an option to specify a deny all policy that will
        // report any package does not have the expected signing identity even if it is declared
        // in the XML config.
        AppAuthenticator appAuthenticatorFromResource =
                mBuilderFromResource.setTestPolicy(
                        TestAppAuthenticatorBuilder.POLICY_DENY_ALL).build();
        AppAuthenticator appAuthenticatorFromInputStream =
                mBuilderFromInputStream.setTestPolicy(
                        TestAppAuthenticatorBuilder.POLICY_DENY_ALL).build();

        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                appAuthenticatorFromResource.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                appAuthenticatorFromInputStream.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE));
    }

    @Test
    public void verifyAppIdentity_declaredPackageWithExpectedSigningIdentity_returnsMatch()
            throws Exception {
        // The TestAppAuthenticator provides an option to specify the signing identity of a package;
        // this test verifies when the specified signing identity matches that in the provided XML
        // config the instance returned from TestAppAuthenticator reports the match.
        AppAuthenticator appAuthenticatorFromResource =
                mBuilderFromResource.setSigningIdentityForPackage(DECLARED_PACKAGE1,
                        "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8")
                        .setSigningIdentityForPackage(EXPECTED_IDENTITY_PACKAGE,
                        "d78405f761ff6236cc9b570347a570aba0c62a129a3ac30c831c64d09ad95469")
                        .build();
        AppAuthenticator appAuthenticatorFromInputStream =
                mBuilderFromInputStream.setSigningIdentityForPackage(DECLARED_PACKAGE1,
                        "fb5dbd3c669af9fc236c6991e6387b7f11ff0590997f22d0f5c74ff40e04fca8")
                        .setSigningIdentityForPackage(EXPECTED_IDENTITY_PACKAGE,
                        "d78405f761ff6236cc9b570347a570aba0c62a129a3ac30c831c64d09ad95469")
                        .build();

        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.SIGNATURE_MATCH,
                appAuthenticatorFromResource.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertEquals(AppAuthenticator.SIGNATURE_MATCH,
                appAuthenticatorFromInputStream.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        appAuthenticatorFromResource.enforceCallingAppIdentity(
                DECLARED_PACKAGE1, TEST_PERMISSION);
        appAuthenticatorFromResource.enforceCallingAppIdentity(
                DECLARED_PACKAGE1, TEST_PERMISSION);
        appAuthenticatorFromResource.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE);
        appAuthenticatorFromInputStream.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE);
    }

    @Test
    public void callingAppIdentity_undeclaredPackageWithExpectedSigningIdentity_returnsMatch()
            throws Exception {
        // By using the setSigningIdentityForPackage method a test can set the signing identity
        // for a package that is not explicitly declared in the XML config; this can be useful
        // for configs that make use of the all-packages tag and thus cannot use the default
        // "accept all declared packages" policy.
        // Note, the expected-identity tag does not support an all-packages declaration, so only
        // the calling identity is verified here.
        AppAuthenticator appAuthenticatorFromResource =
                mBuilderFromResource.setSigningIdentityForPackage(UNDECLARED_PACKAGE,
                        "681b0e56a796350c08647352a4db800cc44b2adc8f4c72fa350bd05d4d50264d")
                        .build();
        AppAuthenticator appAuthenticatorFromInputStream =
                mBuilderFromInputStream.setSigningIdentityForPackage(UNDECLARED_PACKAGE,
                        "681b0e56a796350c08647352a4db800cc44b2adc8f4c72fa350bd05d4d50264d")
                        .build();

        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        UNDECLARED_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        UNDECLARED_PACKAGE, TEST_PERMISSION));
        appAuthenticatorFromResource.enforceCallingAppIdentity(UNDECLARED_PACKAGE, TEST_PERMISSION);
        appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                UNDECLARED_PACKAGE, TEST_PERMISSION);
    }

    @Test
    public void callingAppIdentity_packageUidMismatch_returnsUidMismatch() throws Exception {
        // The uid of the calling app can be set through the TestAppAuthenticator to test scenarios
        // when the ID of the calling app does not match that of the specified package. This test
        // verifies the AppAuthenticator instance returned from the TestAppAuthenticator returns
        // the proper result for this mismatch.
        final int packageUid = 10001;
        final int callingUid = 10123;
        final int callingPid = 1234;
        AppAuthenticator appAuthenticatorFromResource =
                mBuilderFromResource.setUidForPackage(DECLARED_PACKAGE1, packageUid).build();
        AppAuthenticator appAuthenticatorFromInputStream =
                mBuilderFromInputStream.setUidForPackage(DECLARED_PACKAGE1, packageUid).build();

        assertEquals(AppAuthenticator.PERMISSION_DENIED_PACKAGE_UID_MISMATCH,
                appAuthenticatorFromResource.checkCallingAppIdentity(DECLARED_PACKAGE1,
                        TEST_PERMISSION, callingPid, callingUid));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_PACKAGE_UID_MISMATCH,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(DECLARED_PACKAGE1,
                        TEST_PERMISSION, callingPid, callingUid));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceCallingAppIdentity(DECLARED_PACKAGE1,
                        TEST_PERMISSION, callingPid, callingUid));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceCallingAppIdentity(DECLARED_PACKAGE1,
                        TEST_PERMISSION, callingPid, callingUid));
    }

    @Test
    public void verifyAppIdentity_signatureAcceptedForPackage_returnsExpectedResult()
            throws Exception {
        // The TestAppAuthenticator allows packages to be individually set to accept the signing
        // identity. This test verifies the signature is accepted for specified packages, but is
        // rejected for all other packages.
        AppAuthenticator appAuthenticatorFromResource =
                mBuilderFromResource.setSignatureAcceptedForPackage(
                        DECLARED_PACKAGE1).setSignatureAcceptedForPackage(
                        EXPECTED_IDENTITY_PACKAGE).build();
        AppAuthenticator appAuthenticatorFromInputStream =
                mBuilderFromInputStream.setSignatureAcceptedForPackage(
                        DECLARED_PACKAGE1).setSignatureAcceptedForPackage(
                        EXPECTED_IDENTITY_PACKAGE).build();

        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromResource.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                appAuthenticatorFromInputStream.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        // A package declared in the XML config but not set explicitly to be accepted should be
        // rejected.
        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        DECLARED_PACKAGE2, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        DECLARED_PACKAGE2, TEST_PERMISSION));
        appAuthenticatorFromResource.enforceCallingAppIdentity(DECLARED_PACKAGE1, TEST_PERMISSION);
        appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                DECLARED_PACKAGE1, TEST_PERMISSION);
        appAuthenticatorFromResource.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE);
        appAuthenticatorFromInputStream.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE);
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceCallingAppIdentity(
                        DECLARED_PACKAGE2, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                        DECLARED_PACKAGE2, TEST_PERMISSION));
    }

    @Test
    public void callingAppIdentity_packageNotInstalled_returnsUnknownPackage() throws Exception {
        // The TestAppAuthenticator can be configured to treat a package as uninstalled to verify
        // scenarios where the package being queried is not available on the device.
        AppAuthenticator appAuthenticatorFromResource =
                mBuilderFromResource.setPackageNotInstalled(
                        DECLARED_PACKAGE1).setPackageNotInstalled(
                        EXPECTED_IDENTITY_PACKAGE).build();
        AppAuthenticator appAuthenticatorFromInputStream =
                mBuilderFromInputStream.setPackageNotInstalled(
                        DECLARED_PACKAGE1).setPackageNotInstalled(
                        EXPECTED_IDENTITY_PACKAGE).build();

        assertEquals(AppAuthenticator.PERMISSION_DENIED_UNKNOWN_PACKAGE,
                appAuthenticatorFromResource.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_UNKNOWN_PACKAGE,
                appAuthenticatorFromInputStream.checkCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                appAuthenticatorFromResource.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                appAuthenticatorFromInputStream.checkAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceCallingAppIdentity(
                        DECLARED_PACKAGE1, TEST_PERMISSION));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromResource.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE));
        assertThrows(SecurityException.class, () ->
                appAuthenticatorFromInputStream.enforceAppIdentity(EXPECTED_IDENTITY_PACKAGE));
    }
}

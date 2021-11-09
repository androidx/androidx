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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.security.app.authenticator.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(AndroidJUnit4.class)
@DoNotInstrument
// API Level 28 introduced signing key rotation, so run the tests with and without rotation support.
@Config(minSdk = 27, maxSdk = 28)
public class AppAuthenticatorTest {
    private static final String TEST_PACKAGE = "com.android.app1";
    private static final String TEST_PERMISSION = "androidx.security.app.authenticator"
            + ".TEST_PERMISSION";
    private static final int TEST_PID = 54321;
    private static final int TEST_UID = 12345;

    private AppAuthenticator mAppAuthenticator;

    @Mock
    private AppAuthenticatorUtils mMockAppAuthenticatorUtils;
    @Mock
    private AppSignatureVerifier mMockAppSignatureVerifier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();
        mAppAuthenticator = AppAuthenticator.createFromResource(context,
                R.xml.all_supported_elements_and_attributes);
        mAppAuthenticator.setAppSignatureVerifier(mMockAppSignatureVerifier);
        mAppAuthenticator.setAppAuthenticatorUtils(mMockAppAuthenticatorUtils);

        when(mMockAppAuthenticatorUtils.getCallingUid()).thenReturn(TEST_UID);
        when(mMockAppAuthenticatorUtils.getUidForPackage(TEST_PACKAGE)).thenReturn(TEST_UID);
    }

    @Test
    public void enforceCallingAppIdentity_signatureMatch_returns() throws Exception {
        // When enforceCallingAppIdentity is invoked against a package / permission for which the
        // verifier should return success the method should complete without throwing a
        // SecurityException.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(true);

        // The enforce methods throw a SecurityException if the signing identity verification
        // fails; since the verification should be successful for this, and some subsequent
        // tests, no assertion is required as making it past this statement without a
        // SecurityException indicates the test was successful.
        mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION);
        mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION, TEST_PID,
                TEST_UID);
    }

    @Test
    public void enforceCallingAppIdentity_noSignatureMatch_throwsException() throws Exception {
        // When enforceCallingAppIdentity is invoked against a package / permission for which the
        // verifier should return a failure verifying the signing identity the method should
        // throw a SecurityException.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION,
                        TEST_PID, TEST_UID));
    }

    @Test
    public void enforceCallingAppIdentity_uidMismatch_throwsException() throws Exception {
        // enforceCallingAppIdentity optionally accepts the pid / uid of the calling app; if the
        // provided uid does not match that of the package on the device a SecurityException
        // should be thrown.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(true);
        when(mMockAppAuthenticatorUtils.getUidForPackage(TEST_PACKAGE)).thenReturn(23456);

        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION,
                        TEST_PID, TEST_UID));
    }

    @Test
    public void enforceCallingAppIdentity_noPackage_throwsException() throws Exception {
        // If the specified package does not exist on the device then enforceCallingAppIdentity
        // should receive a NameNotFoundException when checking for the UID; this should result
        // in a SecurityException.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(true);
        when(mMockAppAuthenticatorUtils.getUidForPackage(TEST_PACKAGE)).thenThrow(
                PackageManager.NameNotFoundException.class);

        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION,
                        TEST_PID, TEST_UID));
    }

    @Test
    public void checkCallingAppIdentity_signatureMatch_returnsMatch() throws Exception {
        // When checkCallingAppIdentity is invoked against a package / permission for which the
        // verifier should return success the method should return PERMISSION_GRANTED.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(true);

        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                mAppAuthenticator.checkCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_GRANTED,
                mAppAuthenticator.checkCallingAppIdentity(
                        TEST_PACKAGE, TEST_PERMISSION, TEST_PID, TEST_UID));
    }

    @Test
    public void checkCallingAppIdentity_noSignatureMatch_returnsNoMatch() throws Exception {
        // When checkCallingAppIdentity is invoked against a package / permission for which the
        // verifier should return a failure verifying the signing identity the method should
        // return PERMISSION_DENIED_NO_MATCH.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(false);

        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                mAppAuthenticator.checkCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_NO_MATCH,
                mAppAuthenticator.checkCallingAppIdentity(
                        TEST_PACKAGE, TEST_PERMISSION, TEST_PID, TEST_UID));
    }

    @Test
    public void checkCallingAppIdentity_uidMismatch_returnsUidMismatch() throws Exception {
        // checkCallingAppIdentity optionally accepts the pid / uid of the calling app; if the
        // provided uid does not match that of the package on the device
        // PERMISSION_DENIED_PACKAGE_UID_MISMATCH should be returned.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(true);
        when(mMockAppAuthenticatorUtils.getUidForPackage(TEST_PACKAGE)).thenReturn(23456);

        assertEquals(AppAuthenticator.PERMISSION_DENIED_PACKAGE_UID_MISMATCH,
                mAppAuthenticator.checkCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_PACKAGE_UID_MISMATCH,
                mAppAuthenticator.checkCallingAppIdentity(
                        TEST_PACKAGE, TEST_PERMISSION, TEST_PID, TEST_UID));
    }

    @Test
    public void checkCallingAppIdentity_noPackage_returnsUnknownPackage() throws Exception {
        // If the specified package does not exist on the device then checkCallingAppIdentity
        // should receive a NameNotFoundException when checking for the UID; this should result
        // in PERMISSION_DENIED_UNKNOWN_PACKAGE.
        when(mMockAppSignatureVerifier.verifySigningIdentity(TEST_PACKAGE,
                TEST_PERMISSION)).thenReturn(true);
        when(mMockAppAuthenticatorUtils.getUidForPackage(TEST_PACKAGE)).thenThrow(
                PackageManager.NameNotFoundException.class);

        assertEquals(AppAuthenticator.PERMISSION_DENIED_UNKNOWN_PACKAGE,
                mAppAuthenticator.checkCallingAppIdentity(TEST_PACKAGE, TEST_PERMISSION));
        assertEquals(AppAuthenticator.PERMISSION_DENIED_UNKNOWN_PACKAGE,
                mAppAuthenticator.checkCallingAppIdentity(
                        TEST_PACKAGE, TEST_PERMISSION, TEST_PID, TEST_UID));
    }

    @Test
    public void enforceAppIdentity_signatureMatch_returns() throws Exception {
        // enforceAppIdentity is intended to be used to verify the signing identity of an app
        // before interacting with it; if the specified package has the expected signing identity
        // enforceAppIdentity should return without throwing an exception.
        when(mMockAppSignatureVerifier.verifyExpectedIdentity(TEST_PACKAGE)).thenReturn(true);

        mAppAuthenticator.enforceAppIdentity(TEST_PACKAGE);
    }

    @Test
    public void enforceAppIdentity_noSignatureMatch_throwsException() throws Exception {
        // When enforceAppIdentity is invoked with a package for which the signing identity on
        // the device does not match the expected identity the method should throw a
        // SecurityException.
        when(mMockAppSignatureVerifier.verifyExpectedIdentity(TEST_PACKAGE)).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> mAppAuthenticator.enforceAppIdentity(TEST_PACKAGE));
    }

    @Test
    public void checkAppIdentity_signatureMatch_returnsMatch() throws Exception {
        // checkAppIdentity is intended to be used to verify the signing identity of an app
        // before interacting with it; if the specified package has the expected signing identity
        // checkAppIdentity should return SIGNATURE_MATCH.
        when(mMockAppSignatureVerifier.verifyExpectedIdentity(TEST_PACKAGE)).thenReturn(true);

        assertEquals(AppAuthenticator.SIGNATURE_MATCH,
                mAppAuthenticator.checkAppIdentity(TEST_PACKAGE));
    }

    @Test
    public void checkAppIdentity_noSignatureMatch_returnsNoMatch() throws Exception {
        // When checkAppIdentity is invoked with a package for which the signing identity on
        // the device does not match the expected identity the method should return
        // SIGNATURE_NO_MATCH.
        when(mMockAppSignatureVerifier.verifyExpectedIdentity(TEST_PACKAGE)).thenReturn(false);

        assertEquals(AppAuthenticator.SIGNATURE_NO_MATCH,
                mAppAuthenticator.checkAppIdentity(TEST_PACKAGE));
    }
}

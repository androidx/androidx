/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.biometric;

import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED;
import static androidx.biometric.BiometricManager.BIOMETRIC_STATUS_UNKNOWN;
import static androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager.Authenticators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class BiometricManagerTest {
    @Mock private androidx.core.hardware.fingerprint.FingerprintManagerCompat mFingerprintManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsSuccess_WhenManagerReturnsSuccess_OnApi29AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = 29)
    public void testCanAuthenticate_ReturnsError_WhenManagerReturnsNoneEnrolled_OnApi29AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = 29)
    public void testCanAuthenticate_ReturnsError_WhenManagerReturnsNoHardware_OnApi29AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenCombinationNotSupported_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_UNSUPPORTED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenCombinationNotSupported_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_UNSUPPORTED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenNoAuthenticatorsAllowed_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        assertThat(biometricManager.canAuthenticate(0)).isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenNoAuthenticatorsAllowed_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        assertThat(biometricManager.canAuthenticate(0)).isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecurable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecurable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_BiometricOnly_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setDeviceSecurable(true)
                .setFingerprintHardwarePresent(false)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_BiometricOnly_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setFingerprintHardwarePresent(false)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_CredentialAllowed_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setDeviceSecurable(true)
                .setFingerprintHardwarePresent(false)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_CredentialAllowed_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setFingerprintHardwarePresent(false)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsSuccess_WhenDeviceCredentialAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsSuccess_WhenDeviceCredentialAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticateStrong_ReturnsSuccess_WhenStrongBiometricGuaranteed_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .setStrongBiometricGuaranteed(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticateStrong_ReturnsError_WhenWeakUnavailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticateStrong_ReturnsSuccess_WhenFingerprintAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsSuccess_WhenFingerprintAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsUnknown_WhenFingerprintUnavailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setBiometricManager(frameworkBiometricManager)
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_STATUS_UNKNOWN);
    }


    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsUnknown_WhenFingerprintUnavailable_OnApi28() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_STATUS_UNKNOWN);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenNoFingerprintHardware_OnApi28() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    public void testCanAuthenticate_ReturnsError_WhenFingerprintUnavailable_OnApi27AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = new TestInjector.Builder()
                .setFingerprintManager(mFingerprintManager)
                .setDeviceSecurable(true)
                .setDeviceSecuredWithCredential(true)
                .setFingerprintHardwarePresent(true)
                .build();

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    /**
     * A configurable injector to be used for testing.
     */
    private static class TestInjector implements BiometricManager.Injector {
        @Nullable private final android.hardware.biometrics.BiometricManager mBiometricManager;
        @Nullable private final androidx.core.hardware.fingerprint.FingerprintManagerCompat
                mFingerprintManager;
        private final boolean mIsDeviceSecurable;
        private final boolean mIsDeviceSecuredWithCredential;
        private final boolean mIsFingerprintHardwarePresent;
        private final boolean mIsStrongBiometricGuaranteed;

        private TestInjector(
                @Nullable android.hardware.biometrics.BiometricManager biometricManager,
                @Nullable androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        fingerprintManager,
                boolean isDeviceSecurable,
                boolean isDeviceSecuredWithCredential,
                boolean isFingerprintHardwarePresent,
                boolean isStrongBiometricGuaranteed) {
            mBiometricManager = biometricManager;
            mFingerprintManager = fingerprintManager;
            mIsDeviceSecurable = isDeviceSecurable;
            mIsDeviceSecuredWithCredential = isDeviceSecuredWithCredential;
            mIsFingerprintHardwarePresent = isFingerprintHardwarePresent;
            mIsStrongBiometricGuaranteed = isStrongBiometricGuaranteed;
        }

        @Nullable
        @Override
        public android.hardware.biometrics.BiometricManager getBiometricManager() {
            return mBiometricManager;
        }

        @Nullable
        @Override
        public androidx.core.hardware.fingerprint.FingerprintManagerCompat getFingerprintManager() {
            return mFingerprintManager;
        }

        @Override
        public boolean isDeviceSecurable() {
            return mIsDeviceSecurable;
        }

        @Override
        public boolean isDeviceSecuredWithCredential() {
            return mIsDeviceSecuredWithCredential;
        }

        @Override
        public boolean isFingerprintHardwarePresent() {
            return mIsFingerprintHardwarePresent;
        }

        @Override
        public boolean isStrongBiometricGuaranteed() {
            return mIsStrongBiometricGuaranteed;
        }

        static final class Builder {
            @Nullable private android.hardware.biometrics.BiometricManager mBiometricManager = null;
            @Nullable private androidx.core.hardware.fingerprint.FingerprintManagerCompat
                    mFingerprintManager = null;
            private boolean mIsDeviceSecurable = false;
            private boolean mIsDeviceSecuredWithCredential = false;
            private boolean mIsFingerprintHardwarePresent = false;
            private boolean mIsStrongBiometricGuaranteed = false;

            Builder setBiometricManager(
                    @Nullable android.hardware.biometrics.BiometricManager biometricManager) {
                mBiometricManager = biometricManager;
                return this;
            }

            Builder setFingerprintManager(
                    @Nullable androidx.core.hardware.fingerprint.FingerprintManagerCompat
                            fingerprintManager) {
                mFingerprintManager = fingerprintManager;
                return this;
            }

            Builder setDeviceSecurable(boolean deviceSecurable) {
                mIsDeviceSecurable = deviceSecurable;
                return this;
            }

            Builder setDeviceSecuredWithCredential(boolean deviceSecuredWithCredential) {
                mIsDeviceSecuredWithCredential = deviceSecuredWithCredential;
                return this;
            }

            Builder setFingerprintHardwarePresent(boolean fingerprintHardwarePresent) {
                mIsFingerprintHardwarePresent = fingerprintHardwarePresent;
                return this;
            }

            Builder setStrongBiometricGuaranteed(boolean strongBiometricGuaranteed) {
                mIsStrongBiometricGuaranteed = strongBiometricGuaranteed;
                return this;
            }

            TestInjector build() {
                return new TestInjector(
                        mBiometricManager,
                        mFingerprintManager,
                        mIsDeviceSecurable,
                        mIsDeviceSecuredWithCredential,
                        mIsFingerprintHardwarePresent,
                        mIsStrongBiometricGuaranteed);
            }
        }
    }
}

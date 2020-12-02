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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class BiometricManagerTest {
    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsSuccess_WhenManagerReturnsSuccess_OnApi29AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenManagerReturnsNoneEnrolled_OnApi29AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenManagerReturnsNoHardware_OnApi29AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

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

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_UNSUPPORTED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenCombinationNotSupported_OnApi28AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(true);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

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

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        assertThat(biometricManager.canAuthenticate(0)).isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenNoAuthenticatorsAllowed_OnApi28AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(true);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        assertThat(biometricManager.canAuthenticate(0)).isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecurable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                false /* isDeviceSecurable */,
                false /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecurable_OnApi28AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(true);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                false /* isDeviceSecurable */,
                false /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecured_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                false /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecured_OnApi28AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(true);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                false /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q, maxSdk = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsSuccess_WhenDeviceCredentialAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                false /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsSuccess_WhenDeviceCredentialAvailable_OnApi28AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(false);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

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

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                true /* isStrongBiometricGuaranteed */);

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

        final BiometricManager.Injector injector = createInjector(
                frameworkBiometricManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */,
                false /* isStrongBiometricGuaranteed */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsSuccess_WhenFingerprintAvailable_OnApi28AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(true);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsUnknown_WhenFingerprintUnavailable_OnApi28() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(false);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_STATUS_UNKNOWN);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenNoFingerprintHardware_OnApi28() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(false);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                false /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    public void testCanAuthenticate_ReturnsError_WhenFingerprintUnavailable_OnApi27AndBelow() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager =
                mock(androidx.core.hardware.fingerprint.FingerprintManagerCompat.class);
        when(fingerprintManager.isHardwareDetected()).thenReturn(true);
        when(fingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager.Injector injector = createInjector(
                fingerprintManager,
                true /* isDeviceSecurable */,
                true /* isDeviceSecuredWithCredential */,
                true /* isFingerprintHardwarePresent */);

        final BiometricManager biometricManager = new BiometricManager(injector);
        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    private static BiometricManager.Injector createInjector(
            @Nullable final android.hardware.biometrics.BiometricManager biometricManager,
            final boolean isDeviceSecurable,
            final boolean isDeviceSecuredWithCredential,
            final boolean isFingerprintHardwarePresent,
            final boolean isStrongBiometricGuaranteed) {

        return new BiometricManager.Injector() {
            @Override
            @Nullable
            public android.hardware.biometrics.BiometricManager getBiometricManager() {
                return biometricManager;
            }

            @Override
            @Nullable
            public androidx.core.hardware.fingerprint.FingerprintManagerCompat
                    getFingerprintManager() {
                return null;
            }

            @Override
            public boolean isDeviceSecurable() {
                return isDeviceSecurable;
            }

            @Override
            public boolean isDeviceSecuredWithCredential() {
                return isDeviceSecuredWithCredential;
            }

            @Override
            public boolean isFingerprintHardwarePresent() {
                return isFingerprintHardwarePresent;
            }

            @Override
            public boolean isStrongBiometricGuaranteed() {
                return isStrongBiometricGuaranteed;
            }
        };
    }

    private static BiometricManager.Injector createInjector(
            @Nullable final androidx.core.hardware.fingerprint.FingerprintManagerCompat
                    fingerprintManager,
            final boolean isDeviceSecurable,
            final boolean isDeviceSecuredWithCredential,
            final boolean isFingerprintHardwarePresent) {

        return new BiometricManager.Injector() {
            @Override
            @Nullable
            public android.hardware.biometrics.BiometricManager getBiometricManager() {
                return null;
            }

            @Override
            @Nullable
            public androidx.core.hardware.fingerprint.FingerprintManagerCompat
                    getFingerprintManager() {
                return fingerprintManager;
            }

            @Override
            public boolean isDeviceSecurable() {
                return isDeviceSecurable;
            }

            @Override
            public boolean isDeviceSecuredWithCredential() {
                return isDeviceSecuredWithCredential;
            }

            @Override
            public boolean isFingerprintHardwarePresent() {
                return isFingerprintHardwarePresent;
            }

            @Override
            public boolean isStrongBiometricGuaranteed() {
                return false;
            }
        };
    }
}

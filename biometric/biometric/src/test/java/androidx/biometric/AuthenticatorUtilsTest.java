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

package androidx.biometric;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.biometric.BiometricManager.Authenticators;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@LargeTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AuthenticatorUtilsTest {
    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.Q)
    public void testIsSupportedCombination_OnApi28To29() {
        assertThat(AuthenticatorUtils.isSupportedCombination(0)).isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(Authenticators.BIOMETRIC_STRONG))
                .isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(Authenticators.BIOMETRIC_WEAK))
                .isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(Authenticators.DEVICE_CREDENTIAL))
                .isFalse();
        assertThat(AuthenticatorUtils.isSupportedCombination(
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL)).isFalse();
        assertThat(AuthenticatorUtils.isSupportedCombination(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)).isTrue();
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    public void testIsSupportedCombination_OnApi27AndBelow() {
        assertThat(AuthenticatorUtils.isSupportedCombination(0)).isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(Authenticators.BIOMETRIC_STRONG))
                .isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(Authenticators.BIOMETRIC_WEAK))
                .isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(Authenticators.DEVICE_CREDENTIAL))
                .isFalse();
        assertThat(AuthenticatorUtils.isSupportedCombination(
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL)).isTrue();
        assertThat(AuthenticatorUtils.isSupportedCombination(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)).isTrue();
    }

    @Test
    public void testIsDeviceCredentialAllowed() {
        assertThat(AuthenticatorUtils.isDeviceCredentialAllowed(0)).isFalse();
        assertThat(AuthenticatorUtils.isDeviceCredentialAllowed(Authenticators.BIOMETRIC_STRONG))
                .isFalse();
        assertThat(AuthenticatorUtils.isDeviceCredentialAllowed(Authenticators.BIOMETRIC_WEAK))
                .isFalse();
        assertThat(AuthenticatorUtils.isDeviceCredentialAllowed(Authenticators.DEVICE_CREDENTIAL))
                .isTrue();
        assertThat(AuthenticatorUtils.isDeviceCredentialAllowed(
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL)).isTrue();
        assertThat(AuthenticatorUtils.isDeviceCredentialAllowed(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)).isTrue();
    }

    @Test
    public void testIsWeakBiometricAllowed() {
        assertThat(AuthenticatorUtils.isWeakBiometricAllowed(0)).isFalse();
        assertThat(AuthenticatorUtils.isWeakBiometricAllowed(Authenticators.BIOMETRIC_STRONG))
                .isFalse();
        assertThat(AuthenticatorUtils.isWeakBiometricAllowed(Authenticators.BIOMETRIC_WEAK))
                .isTrue();
        assertThat(AuthenticatorUtils.isWeakBiometricAllowed(Authenticators.DEVICE_CREDENTIAL))
                .isFalse();
        assertThat(AuthenticatorUtils.isWeakBiometricAllowed(
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL)).isFalse();
        assertThat(AuthenticatorUtils.isWeakBiometricAllowed(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)).isTrue();
    }
}

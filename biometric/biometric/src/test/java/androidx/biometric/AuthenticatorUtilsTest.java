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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import javax.crypto.Cipher;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AuthenticatorUtilsTest {
    private int[] mAuthenticatorCombinations;

    @Before
    public void setUp() {
        mAuthenticatorCombinations = new int[]{
                Authenticators.BIOMETRIC_STRONG,
                Authenticators.BIOMETRIC_WEAK,
                Authenticators.DEVICE_CREDENTIAL,
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL,
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL};
    }

    @Test
    public void testConvertToString() {
        assertThat(AuthenticatorUtils.convertToString(0)).isEqualTo("0");
        for (int authenticators : mAuthenticatorCombinations) {
            final String converted = AuthenticatorUtils.convertToString(authenticators);
            assertThat(converted).isNotEmpty();
            assertThat(converted).isNotEqualTo(String.valueOf(authenticators));
        }
    }

    @Test
    public void testGetConsolidatedAuthenticators_UsesAllowedAuthenticators() throws Exception {
        for (int authenticators : mAuthenticatorCombinations) {
            // Can't build prompt info with unsupported authenticator combinations.
            if (!AuthenticatorUtils.isSupportedCombination(authenticators)) {
                continue;
            }

            final BiometricPrompt.PromptInfo info =
                    createPromptInfo(authenticators, true /* deviceCredentialAllowed */);
            assertThat(AuthenticatorUtils.getConsolidatedAuthenticators(info, null /* crypto */))
                    .isEqualTo(authenticators);
            assertThat(AuthenticatorUtils.getConsolidatedAuthenticators(info, createCryptoObject()))
                    .isEqualTo(authenticators);
        }
    }

    @Test
    public void testGetConsolidatedAuthenticators_WithDeviceCredentialAllowed() throws Exception {
        final BiometricPrompt.PromptInfo info =
                createPromptInfo(0 /* authenticators */, true /* deviceCredentialAllowed */);
        assertThat(AuthenticatorUtils.getConsolidatedAuthenticators(info, null /* crypto */))
                .isEqualTo(Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(AuthenticatorUtils.getConsolidatedAuthenticators(info, createCryptoObject()))
                .isEqualTo(Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL);
    }

    @Test
    public void testGetConsolidatedAuthenticators_WithDeviceCredentialDisallowed()
            throws Exception {
        final BiometricPrompt.PromptInfo info =
                createPromptInfo(0 /* authenticators */, false /* deviceCredentialAllowed */);
        assertThat(AuthenticatorUtils.getConsolidatedAuthenticators(info, null /* crypto */))
                .isEqualTo(Authenticators.BIOMETRIC_WEAK);
        assertThat(AuthenticatorUtils.getConsolidatedAuthenticators(info, createCryptoObject()))
                .isEqualTo(Authenticators.BIOMETRIC_STRONG);
    }

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
    public void testIsSomeBiometricAllowed() {
        assertThat(AuthenticatorUtils.isSomeBiometricAllowed(0)).isFalse();
        assertThat(AuthenticatorUtils.isSomeBiometricAllowed(Authenticators.BIOMETRIC_STRONG))
                .isTrue();
        assertThat(AuthenticatorUtils.isSomeBiometricAllowed(Authenticators.BIOMETRIC_WEAK))
                .isTrue();
        assertThat(AuthenticatorUtils.isSomeBiometricAllowed(Authenticators.DEVICE_CREDENTIAL))
                .isFalse();
        assertThat(AuthenticatorUtils.isSomeBiometricAllowed(
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL)).isTrue();
        assertThat(AuthenticatorUtils.isSomeBiometricAllowed(
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

    @SuppressWarnings("deprecation")
    private static BiometricPrompt.PromptInfo createPromptInfo(
            @BiometricManager.AuthenticatorTypes int authenticators,
            boolean deviceCredentialAllowed) {

        final BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title")
                .setAllowedAuthenticators(authenticators)
                .setDeviceCredentialAllowed(deviceCredentialAllowed);

        // Negative button text is required iff device credential is not allowed.
        final boolean isNegativeButtonTextRequired = authenticators != 0
                ? (authenticators & Authenticators.DEVICE_CREDENTIAL) == 0
                : !deviceCredentialAllowed;
        if (isNegativeButtonTextRequired) {
            builder.setNegativeButtonText("Cancel");
        }

        return builder.build();
    }

    private static BiometricPrompt.CryptoObject createCryptoObject() throws Exception {
        return new BiometricPrompt.CryptoObject(Cipher.getInstance("AES/CBC/PKCS7Padding"));
    }
}

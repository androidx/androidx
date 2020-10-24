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

import static org.mockito.Mockito.mock;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CryptoObjectUtilsTest {
    @Mock private Cipher mCipher;
    @Mock private Mac mMac;
    @Mock private Signature mSignature;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testUnwrapFromBiometricPrompt_WithNullCryptoObject() {
        assertThat(CryptoObjectUtils.unwrapFromBiometricPrompt(null)).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testUnwrapFromBiometricPrompt_WithCipherCryptoObject() {
        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                new android.hardware.biometrics.BiometricPrompt.CryptoObject(mCipher);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromBiometricPrompt(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isEqualTo(mCipher);
        assertThat(unwrappedCrypto.getSignature()).isNull();
        assertThat(unwrappedCrypto.getMac()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testUnwrapFromBiometricPrompt_WithSignatureCryptoObject() {
        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                new android.hardware.biometrics.BiometricPrompt.CryptoObject(mSignature);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromBiometricPrompt(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isNull();
        assertThat(unwrappedCrypto.getSignature()).isEqualTo(mSignature);
        assertThat(unwrappedCrypto.getMac()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testUnwrapFromBiometricPrompt_WithMacCryptoObject() {
        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                new android.hardware.biometrics.BiometricPrompt.CryptoObject(mMac);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromBiometricPrompt(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isNull();
        assertThat(unwrappedCrypto.getSignature()).isNull();
        assertThat(unwrappedCrypto.getMac()).isEqualTo(mMac);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void testUnwrapFromBiometricPrompt_WithIdentityCredentialCryptoObject() {
        final android.security.identity.IdentityCredential identityCredential =
                mock(android.security.identity.IdentityCredential.class);
        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                new android.hardware.biometrics.BiometricPrompt.CryptoObject(identityCredential);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromBiometricPrompt(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isNull();
        assertThat(unwrappedCrypto.getSignature()).isNull();
        assertThat(unwrappedCrypto.getMac()).isNull();
        assertThat(unwrappedCrypto.getIdentityCredential()).isEqualTo(identityCredential);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testWrapForBiometricPrompt_WithNullCryptoObject() {
        assertThat(CryptoObjectUtils.wrapForBiometricPrompt(null)).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testWrapForBiometricPrompt_WithCipherCryptoObject() {
        final BiometricPrompt.CryptoObject unwrappedCrypto =
                new BiometricPrompt.CryptoObject(mCipher);

        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                CryptoObjectUtils.wrapForBiometricPrompt(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isEqualTo(mCipher);
        assertThat(wrappedCrypto.getSignature()).isNull();
        assertThat(wrappedCrypto.getMac()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testWrapForBiometricPrompt_WithSignatureCryptoObject() {
        final BiometricPrompt.CryptoObject unwrappedCrypto =
                new BiometricPrompt.CryptoObject(mSignature);

        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                CryptoObjectUtils.wrapForBiometricPrompt(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isNull();
        assertThat(wrappedCrypto.getSignature()).isEqualTo(mSignature);
        assertThat(wrappedCrypto.getMac()).isNull();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.P)
    public void testWrapForBiometricPrompt_WithMacCryptoObject() {
        final BiometricPrompt.CryptoObject unwrappedCrypto = new BiometricPrompt.CryptoObject(mMac);

        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                CryptoObjectUtils.wrapForBiometricPrompt(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isNull();
        assertThat(wrappedCrypto.getSignature()).isNull();
        assertThat(wrappedCrypto.getMac()).isEqualTo(mMac);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void testWrapForBiometricPrompt_WithIdentityCredentialCryptoObject() {
        final android.security.identity.IdentityCredential identityCredential =
                mock(android.security.identity.IdentityCredential.class);
        final BiometricPrompt.CryptoObject unwrappedCrypto =
                new BiometricPrompt.CryptoObject(identityCredential);

        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCrypto =
                CryptoObjectUtils.wrapForBiometricPrompt(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isNull();
        assertThat(wrappedCrypto.getSignature()).isNull();
        assertThat(wrappedCrypto.getMac()).isNull();
        assertThat(wrappedCrypto.getIdentityCredential()).isEqualTo(identityCredential);
    }

    @Test
    public void testUnwrapFromFingerprintManager_WithNullCryptoObject() {
        assertThat(CryptoObjectUtils.unwrapFromFingerprintManager(null)).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testUnwrapFromFingerprintManager_WithCipherCryptoObject() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                wrappedCrypto = new androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        .CryptoObject(mCipher);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromFingerprintManager(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isEqualTo(mCipher);
        assertThat(unwrappedCrypto.getSignature()).isNull();
        assertThat(unwrappedCrypto.getMac()).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testUnwrapFromFingerprintManager_WithSignatureCryptoObject() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                wrappedCrypto = new androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        .CryptoObject(mSignature);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromFingerprintManager(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isNull();
        assertThat(unwrappedCrypto.getSignature()).isEqualTo(mSignature);
        assertThat(unwrappedCrypto.getMac()).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testUnwrapFromFingerprintManager_WithMacCryptoObject() {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                wrappedCrypto = new androidx.core.hardware.fingerprint.FingerprintManagerCompat
                        .CryptoObject(mMac);

        final BiometricPrompt.CryptoObject unwrappedCrypto =
                CryptoObjectUtils.unwrapFromFingerprintManager(wrappedCrypto);

        assertThat(unwrappedCrypto).isNotNull();
        assertThat(unwrappedCrypto.getCipher()).isNull();
        assertThat(unwrappedCrypto.getSignature()).isNull();
        assertThat(unwrappedCrypto.getMac()).isEqualTo(mMac);
    }

    @Test
    public void testWrapForFingerprintManager_WithNullCryptoObject() {
        assertThat(CryptoObjectUtils.wrapForFingerprintManager(null)).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWrapForFingerprintManager_WithCipherCryptoObject() {
        final BiometricPrompt.CryptoObject unwrappedCrypto =
                new BiometricPrompt.CryptoObject(mCipher);

        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                wrappedCrypto = CryptoObjectUtils.wrapForFingerprintManager(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isEqualTo(mCipher);
        assertThat(wrappedCrypto.getSignature()).isNull();
        assertThat(wrappedCrypto.getMac()).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWrapForFingerprintManager_WithSignatureCryptoObject() {
        final BiometricPrompt.CryptoObject unwrappedCrypto =
                new BiometricPrompt.CryptoObject(mSignature);

        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                wrappedCrypto = CryptoObjectUtils.wrapForFingerprintManager(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isNull();
        assertThat(wrappedCrypto.getSignature()).isEqualTo(mSignature);
        assertThat(wrappedCrypto.getMac()).isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testWrapForFingerprintManager_WithMacCryptoObject() {
        final BiometricPrompt.CryptoObject unwrappedCrypto = new BiometricPrompt.CryptoObject(mMac);

        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                wrappedCrypto = CryptoObjectUtils.wrapForFingerprintManager(unwrappedCrypto);

        assertThat(wrappedCrypto).isNotNull();
        assertThat(wrappedCrypto.getCipher()).isNull();
        assertThat(wrappedCrypto.getSignature()).isNull();
        assertThat(wrappedCrypto.getMac()).isEqualTo(mMac);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.R)
    public void testWrapForFingerprintManager_WithIdentityCredentialCryptoObject() {
        final android.security.identity.IdentityCredential identityCredential =
                mock(android.security.identity.IdentityCredential.class);
        final BiometricPrompt.CryptoObject unwrappedCrypto =
                new BiometricPrompt.CryptoObject(identityCredential);

        assertThat(CryptoObjectUtils.wrapForFingerprintManager(unwrappedCrypto)).isNull();
    }
}

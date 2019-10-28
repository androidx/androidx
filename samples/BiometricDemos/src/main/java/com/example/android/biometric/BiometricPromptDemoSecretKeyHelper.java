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

package com.example.android.biometric;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Helper class that provides methods for generating and accessing secret keys, and showcases
 * the differences between the keys that require biometrics and the keys bound to user credentials.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
final class BiometricPromptDemoSecretKeyHelper {
    private BiometricPromptDemoSecretKeyHelper() { }

    /**
     * Generates a key that requires the user to authenticate with a biometric before each use.
     */
    static void generateBiometricBoundKey(String keyName,
            boolean invalidatedByBiometricEnrollment)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException {
        generateKey(keyName, true, invalidatedByBiometricEnrollment, -1);
    }

    /**
     * Generates a key that can only be used if the user is authenticated via secure lock screen or
     * {@link androidx.biometric.BiometricPrompt.PromptInfo.Builder#setDeviceCredentialAllowed(
     *boolean)}
     */
    static void generateCredentialBoundKey(String keyName, int validityDuration)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException {
        generateKey(keyName, false, false, validityDuration);
    }

    private static void generateKey(String keyName, boolean biometricBound,
            boolean invalidatedByBiometricEnrollment, int validityDuration)
            throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                keyName,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true);

        if (biometricBound) {
            // Require the user to authenticate for every use of the key. This is the default, i.e.
            // userAuthenticationValidityDurationSeconds is -1 unless specified otherwise.
            // Explicitly setting it to -1 here for the sake of example.
            // For this to work, at least one biometric must be enrolled.
            builder.setUserAuthenticationValidityDurationSeconds(-1);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level 24+.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // it isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Invalidate the keys if a new biometric has been enrolled.
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
        } else {
            // Sets the duration for which the key can be used after the last user authentication.
            // For this to work, authentication must happen either via secure lock screen or the
            // ConfirmDeviceCredential flow, which can be done by creating BiometricPrompt with
            // BiometricPrompt.PromptInfo.Builder#setDeviceCredentialAllowed(true)
            builder.setUserAuthenticationValidityDurationSeconds(validityDuration);
        }

        KeyGenerator keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        keyGenerator.init(builder.build());

        // Generates and stores the key in Android KeyStore under the keystoreAlias (keyName)
        // specified in the builder.
        keyGenerator.generateKey();
    }

    static SecretKey getSecretKey(String keyName)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(keyName, null);
    }

    static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7);
    }
}

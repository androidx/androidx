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

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utility class for converting between different types of crypto objects that may be used
 * internally by {@link BiometricPrompt} and {@link BiometricManager}.
 */
class CryptoObjectUtils {
    // Prevent instantiation.
    private CryptoObjectUtils() {}

    /**
     * Unwraps a crypto object returned by {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * @param cryptoObject A crypto object from {@link android.hardware.biometrics.BiometricPrompt}.
     * @return An equivalent {@link androidx.biometric.BiometricPrompt.CryptoObject} instance.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    static @Nullable BiometricPrompt.CryptoObject unwrapFromBiometricPrompt(
            @Nullable android.hardware.biometrics.BiometricPrompt.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getMac());
        } else {
            return null;
        }
    }

    /**
     * Wraps a crypto object to be passed to {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * @param cryptoObject An instance of {@link androidx.biometric.BiometricPrompt.CryptoObject}.
     * @return An equivalent crypto object that is compatible with
     *  {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    static @Nullable android.hardware.biometrics.BiometricPrompt.CryptoObject
            wrapForBiometricPrompt(@Nullable BiometricPrompt.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    cryptoObject.getMac());
        } else {
            return null;
        }
    }

    /**
     * Unwraps a crypto object returned by
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     *
     * @param cryptoObject A crypto object from
     *                     {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     * @return An equivalent {@link androidx.biometric.BiometricPrompt.CryptoObject} instance.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    static BiometricPrompt.CryptoObject unwrapFromFingerprintManager(
            @Nullable androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
                    cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getMac());
        } else {
            return null;
        }
    }

    /**
     * Wraps a crypto object to be passed to
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     *
     * @param cryptoObject An instance of {@link androidx.biometric.BiometricPrompt.CryptoObject}.
     * @return An equivalent crypto object that is compatible with
     *  {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    static androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
            wrapForFingerprintManager(@Nullable BiometricPrompt.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cryptoObject.getMac());
        } else {
            return null;
        }
    }
}

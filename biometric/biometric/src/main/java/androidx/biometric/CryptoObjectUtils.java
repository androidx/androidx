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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

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
    @RequiresApi(Build.VERSION_CODES.P)
    @Nullable
    static BiometricPrompt.CryptoObject unwrapFromBiometricPrompt(
            @Nullable android.hardware.biometrics.BiometricPrompt.CryptoObject cryptoObject) {

        if (cryptoObject == null) {
            return null;
        }

        final Cipher cipher = Api28Impl.getCipher(cryptoObject);
        if (cipher != null) {
            return new BiometricPrompt.CryptoObject(cipher);
        }

        final Signature signature = Api28Impl.getSignature(cryptoObject);
        if (signature != null) {
            return new BiometricPrompt.CryptoObject(signature);
        }

        final Mac mac = Api28Impl.getMac(cryptoObject);
        if (mac != null) {
            return new BiometricPrompt.CryptoObject(mac);
        }

        return null;
    }

    /**
     * Wraps a crypto object to be passed to {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * @param cryptoObject An instance of {@link androidx.biometric.BiometricPrompt.CryptoObject}.
     * @return An equivalent crypto object that is compatible with
     *  {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @Nullable
    static android.hardware.biometrics.BiometricPrompt.CryptoObject
            wrapForBiometricPrompt(@Nullable BiometricPrompt.CryptoObject cryptoObject) {

        if (cryptoObject == null) {
            return null;
        }

        final Cipher cipher = cryptoObject.getCipher();
        if (cipher != null) {
            return Api28Impl.create(cipher);
        }

        final Signature signature = cryptoObject.getSignature();
        if (signature != null) {
            return Api28Impl.create(signature);
        }

        final Mac mac = cryptoObject.getMac();
        if (mac != null) {
            return Api28Impl.create(mac);
        }

        return null;
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
        }

        final Cipher cipher = cryptoObject.getCipher();
        if (cipher != null) {
            return new BiometricPrompt.CryptoObject(cipher);
        }

        final Signature signature = cryptoObject.getSignature();
        if (signature != null) {
            return new BiometricPrompt.CryptoObject(signature);
        }

        final Mac mac = cryptoObject.getMac();
        if (mac != null) {
            return new BiometricPrompt.CryptoObject(mac);
        }

        return null;
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
        }

        final Cipher cipher = cryptoObject.getCipher();
        if (cipher != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cipher);
        }

        final Signature signature = cryptoObject.getSignature();
        if (signature != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    signature);
        }

        final Mac mac = cryptoObject.getMac();
        if (mac != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    mac);
        }

        return null;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private static class Api28Impl {
        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given cipher.
         *
         * @param cipher The cipher object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        static android.hardware.biometrics.BiometricPrompt.CryptoObject create(
                @NonNull Cipher cipher) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(cipher);
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given
         * signature.
         *
         * @param signature The signature object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        static android.hardware.biometrics.BiometricPrompt.CryptoObject create(
                @NonNull Signature signature) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(signature);
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given MAC.
         *
         * @param mac The MAC object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        static android.hardware.biometrics.BiometricPrompt.CryptoObject create(@NonNull Mac mac) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(mac);
        }

        /**
         * Gets the cipher associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped cipher object, or {@code null}.
         */
        @Nullable
        static Cipher getCipher(
                @NonNull android.hardware.biometrics.BiometricPrompt.CryptoObject crypto) {
            return crypto.getCipher();
        }

        /**
         * Gets the signature associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped signature object, or {@code null}.
         */
        @Nullable
        static Signature getSignature(
                @NonNull android.hardware.biometrics.BiometricPrompt.CryptoObject crypto) {
            return crypto.getSignature();
        }

        /**
         * Gets the MAC associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped MAC object, or {@code null}.
         */
        @Nullable
        static Mac getMac(
                @NonNull android.hardware.biometrics.BiometricPrompt.CryptoObject crypto) {
            return crypto.getMac();
        }
    }
}

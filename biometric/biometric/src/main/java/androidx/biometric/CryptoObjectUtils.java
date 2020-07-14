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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Utility class for creating and converting between different types of crypto objects that may be
 * used internally by {@link BiometricPrompt} and {@link BiometricManager}.
 */
class CryptoObjectUtils {
    private static final String TAG = "CryptoObjectUtils";

    /**
     * The key name used when creating a fake crypto object.
     */
    private static final String FAKE_KEY_NAME = "androidxBiometric";

    /**
     * The name of the Android keystore instance.
     */
    private static final String KEYSTORE_INSTANCE = "AndroidKeyStore";

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

        // Identity credential is only supported on API 30 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final android.security.identity.IdentityCredential identityCredential =
                    Api30Impl.getIdentityCredential(cryptoObject);
            if (identityCredential != null) {
                return new BiometricPrompt.CryptoObject(identityCredential);
            }
        }

        return null;
    }

    /**
     * Wraps a crypto object to be passed to {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * @param cryptoObject An instance of {@link androidx.biometric.BiometricPrompt.CryptoObject}.
     * @return An equivalent crypto object that is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
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

        // Identity credential is only supported on API 30 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final android.security.identity.IdentityCredential identityCredential =
                    cryptoObject.getIdentityCredential();
            if (identityCredential != null) {
                return Api30Impl.create(identityCredential);
            }
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
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && cryptoObject.getIdentityCredential() != null) {
            Log.e(TAG, "Identity credential is not supported by FingerprintManager.");
            return null;
        }

        return null;
    }

    /**
     * Creates a {@link androidx.biometric.BiometricPrompt.CryptoObject} instance that can be passed
     * to {@link BiometricManager} and {@link BiometricPrompt} in order to force crypto-based
     * authentication behavior.
     *
     * @return An internal-only instance of {@link androidx.biometric.BiometricPrompt.CryptoObject}.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Nullable
    static BiometricPrompt.CryptoObject createFakeCryptoObject() {
        try {
            final KeyStore keystore = KeyStore.getInstance(KEYSTORE_INSTANCE);
            keystore.load(null);

            final KeyGenParameterSpec.Builder keySpecBuilder =
                    Api23Impl.createKeyGenParameterSpecBuilder(
                            FAKE_KEY_NAME,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
            Api23Impl.setBlockModeCBC(keySpecBuilder);
            Api23Impl.setEncryptionPaddingPKCS7(keySpecBuilder);

            final KeyGenerator keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE);
            final KeyGenParameterSpec keySpec = Api23Impl.buildKeyGenParameterSpec(keySpecBuilder);
            Api23Impl.initKeyGenerator(keyGenerator, keySpec);
            keyGenerator.generateKey();

            final SecretKey secretKey =
                    (SecretKey) keystore.getKey(FAKE_KEY_NAME, null /* password */);
            final Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            return new BiometricPrompt.CryptoObject(cipher);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | CertificateException
                | KeyStoreException | InvalidKeyException | InvalidAlgorithmParameterException
                | UnrecoverableKeyException | IOException | NoSuchProviderException e) {
            Log.w(TAG, "Failed to create fake crypto object.", e);
            return null;
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        // Prevent instantiation.
        private Api30Impl() {}

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given identity
         * credential.
         *
         * @param identityCredential The identity credential object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        @NonNull
        static android.hardware.biometrics.BiometricPrompt.CryptoObject create(
                @NonNull android.security.identity.IdentityCredential identityCredential) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(identityCredential);
        }

        /**
         * Gets the identity credential associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped identity credential object, or {@code null}.
         */
        @Nullable
        static android.security.identity.IdentityCredential getIdentityCredential(
                @NonNull android.hardware.biometrics.BiometricPrompt.CryptoObject crypto) {
            return crypto.getIdentityCredential();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private static class Api28Impl {
        // Prevent instantiation.
        private Api28Impl() {}

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given cipher.
         *
         * @param cipher The cipher object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        @NonNull
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
        @NonNull
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
        @NonNull
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

    /**
     * Nested class to avoid verification errors for methods introduced in Android 6.0 (API 23).
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private static class Api23Impl {
        // Prevent instantiation.
        private Api23Impl() {}

        /**
         * Creates a new instance of {@link KeyGenParameterSpec.Builder}.
         *
         * @param keystoreAlias The keystore alias for the resulting key.
         * @param purposes      The purposes for which the resulting key will be used.
         * @return An instance of {@link KeyGenParameterSpec.Builder}.
         */
        @SuppressWarnings("SameParameterValue")
        @NonNull
        static KeyGenParameterSpec.Builder createKeyGenParameterSpecBuilder(
                @NonNull String keystoreAlias, int purposes) {
            return new KeyGenParameterSpec.Builder(keystoreAlias, purposes);
        }

        /**
         * Sets CBC block mode for the given key spec builder.
         *
         * @param keySpecBuilder An instance of {@link KeyGenParameterSpec.Builder}.
         */
        static void setBlockModeCBC(@NonNull KeyGenParameterSpec.Builder keySpecBuilder) {
            keySpecBuilder.setBlockModes(KeyProperties.BLOCK_MODE_CBC);
        }

        /**
         * Sets PKCS7 encryption padding for the given key spec builder.
         *
         * @param keySpecBuilder An instance of {@link KeyGenParameterSpec.Builder}.
         */
        static void setEncryptionPaddingPKCS7(@NonNull KeyGenParameterSpec.Builder keySpecBuilder) {
            keySpecBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
        }

        /**
         * Builds a key spec from the given builder.
         *
         * @param keySpecBuilder An instance of {@link KeyGenParameterSpec.Builder}.
         * @return A {@link KeyGenParameterSpec} created from the given builder.
         */
        @NonNull
        static KeyGenParameterSpec buildKeyGenParameterSpec(
                @NonNull KeyGenParameterSpec.Builder keySpecBuilder) {
            return keySpecBuilder.build();
        }

        /**
         * Calls {@link KeyGenerator#init(AlgorithmParameterSpec)} for the given key generator and
         * spec.
         *
         * @param keyGenerator An instance of {@link KeyGenerator}.
         * @param keySpec      The key spec with which to initialize the generator.
         *
         * @throws InvalidAlgorithmParameterException If the key spec is invalid.
         */
        static void initKeyGenerator(
                @NonNull KeyGenerator keyGenerator, @NonNull KeyGenParameterSpec keySpec)
                throws InvalidAlgorithmParameterException {
            keyGenerator.init(keySpec);
        }
    }
}

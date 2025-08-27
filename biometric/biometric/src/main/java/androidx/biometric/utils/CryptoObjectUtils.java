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

package androidx.biometric.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Utility class for creating and converting between different types of crypto objects that may be
 * used internally by {@link BiometricPrompt} and {@link BiometricManager}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CryptoObjectUtils {
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
    private CryptoObjectUtils() {
    }

    /**
     * Unwraps a crypto object returned by {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * @param cryptoObject A crypto object from {@link android.hardware.biometrics.BiometricPrompt}.
     * @return An equivalent {@link androidx.biometric.BiometricPrompt.CryptoObject} instance.
     */
    @SuppressWarnings("deprecation")
    @RequiresApi(Build.VERSION_CODES.P)
    static BiometricPrompt.@Nullable CryptoObject unwrapFromBiometricPrompt(
            android.hardware.biometrics.BiometricPrompt.@Nullable CryptoObject cryptoObject) {

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

        // Presentation session is only supported on API 33 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            final android.security.identity.PresentationSession presentationSession =
                    Api33Impl.getPresentationSession(cryptoObject);
            if (presentationSession != null) {
                return new BiometricPrompt.CryptoObject(presentationSession);
            }
        }

        // Operation handle is only supported on API 35 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // This should be the bottom one and only be reachable when cryptoObject was
            // constructed with operation handle. cryptoObject from other constructors should
            // already be unwrapped and returned above.
            final long operationHandle = Api35Impl.getOperationHandle(cryptoObject);
            if (operationHandle != 0) {
                return new BiometricPrompt.CryptoObject(operationHandle);
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
    @SuppressWarnings("deprecation")
    public static android.hardware.biometrics.BiometricPrompt.@Nullable CryptoObject
            wrapForBiometricPrompt(BiometricPrompt.@Nullable CryptoObject cryptoObject) {

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

        // Presentation session is only supported on API 33 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            final android.security.identity.PresentationSession presentationSession =
                    cryptoObject.getPresentationSession();
            if (presentationSession != null) {
                return Api33Impl.create(presentationSession);
            }
        }

        // Operation handle is only supported on API 35 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            final long operationHandle = cryptoObject.getOperationHandleCryptoObject();
            if (operationHandle != 0) {
                return Api35Impl.create(operationHandle);
            }
        }

        return null;
    }

    /**
     * Get the {@code operationHandle} associated with this object or 0 if none. This needs to be
     * achieved by getting the corresponding
     * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} and then get its
     * operation handle.
     *
     * @param cryptoObject An instance of {@link androidx.biometric.BiometricPrompt.CryptoObject}.
     * @return The {@code operationHandle} associated with this object or 0 if none.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public static long getOperationHandle(BiometricPrompt.@Nullable CryptoObject cryptoObject) {
        final android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCryptoObject =
                CryptoObjectUtils.wrapForBiometricPrompt(cryptoObject);
        if (wrappedCryptoObject != null) {
            return Api35Impl.getOperationHandle(wrappedCryptoObject);
        }
        return 0;
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
    static BiometricPrompt.@Nullable CryptoObject unwrapFromFingerprintManager(
            androidx.core.hardware.fingerprint.FingerprintManagerCompat.@Nullable CryptoObject
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
    public static androidx.core.hardware.fingerprint.FingerprintManagerCompat.@Nullable CryptoObject
            wrapForFingerprintManager(BiometricPrompt.@Nullable CryptoObject cryptoObject) {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && cryptoObject.getPresentationSession() != null) {
            Log.e(TAG, "Presentation session is not supported by FingerprintManager.");
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Log.e(TAG, "Operation handle is not supported by FingerprintManager.");
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
    @SuppressLint("TrulyRandom")
    public static BiometricPrompt.@Nullable CryptoObject createFakeCryptoObject() {
        try {
            final KeyStore keystore = KeyStore.getInstance(KEYSTORE_INSTANCE);
            keystore.load(null);

            final KeyGenParameterSpec.Builder keySpecBuilder =
                    new KeyGenParameterSpec.Builder(
                            FAKE_KEY_NAME,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
            keySpecBuilder.setBlockModes(KeyProperties.BLOCK_MODE_CBC);
            keySpecBuilder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            final KeyGenerator keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE);
            final KeyGenParameterSpec keySpec = keySpecBuilder.build();
            keyGenerator.init(keySpec);
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
     * Nested class to avoid verification errors for methods introduced in Android 15.0 (API 35).
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private static class Api35Impl {
        // Prevent instantiation.
        private Api35Impl() {
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given
         * operation handle.
         *
         * @param operationHandle The operation handle to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        static android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject create(
                long operationHandle) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(operationHandle);
        }

        /**
         * Gets the operation handle associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped operation handle object, or {@code null}.
         */
        static long getOperationHandle(
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto) {
            return crypto.getOperationHandle();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 13.0 (API 33).
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static class Api33Impl {
        // Prevent instantiation.
        private Api33Impl() {
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given
         * presentation session.
         *
         * @param presentationSession The presentation session object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        @SuppressWarnings("deprecation")
        static android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject create(
                android.security.identity.@NonNull PresentationSession presentationSession) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(
                    presentationSession);
        }

        /**
         * Gets the presentation session associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped presentation session object, or {@code null}.
         */
        @SuppressWarnings("deprecation")
        static android.security.identity.@Nullable PresentationSession getPresentationSession(
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto) {
            return crypto.getPresentationSession();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11.0 (API 30).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        // Prevent instantiation.
        private Api30Impl() {
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given identity
         * credential.
         *
         * @param identityCredential The identity credential object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        @SuppressWarnings("deprecation")
        static android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject create(
                android.security.identity.@NonNull IdentityCredential identityCredential) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(identityCredential);
        }

        /**
         * Gets the identity credential associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped identity credential object, or {@code null}.
         */
        @SuppressWarnings("deprecation")
        static android.security.identity.@Nullable IdentityCredential getIdentityCredential(
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto) {
            return crypto.getIdentityCredential();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private static class Api28Impl {
        // Prevent instantiation.
        private Api28Impl() {
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.CryptoObject} from the given cipher.
         *
         * @param cipher The cipher object to be wrapped.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         */
        static android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject create(
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
        static android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject create(
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
        static android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject create(
                @NonNull Mac mac) {
            return new android.hardware.biometrics.BiometricPrompt.CryptoObject(mac);
        }

        /**
         * Gets the cipher associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped cipher object, or {@code null}.
         */
        static @Nullable Cipher getCipher(
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto) {
            return crypto.getCipher();
        }

        /**
         * Gets the signature associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped signature object, or {@code null}.
         */
        static @Nullable Signature getSignature(
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto) {
            return crypto.getSignature();
        }

        /**
         * Gets the MAC associated with the given crypto object, if any.
         *
         * @param crypto An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.CryptoObject}.
         * @return The wrapped MAC object, or {@code null}.
         */
        static @Nullable Mac getMac(
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto) {
            return crypto.getMac();
        }
    }
}

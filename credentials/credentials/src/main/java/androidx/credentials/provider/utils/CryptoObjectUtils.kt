/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.provider.utils

import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.identity.IdentityCredential
import androidx.annotation.RequiresApi
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

/**
 * Utility class for creating and converting between different types of crypto objects that may be
 * used internally by [androidx.biometric.BiometricPrompt] or the
 * [androidx.biometric.BiometricManager].
 *
 * Borrowed from [package androidx.biometric] to support credential manager operations where both
 * framework and jetpack data types are relevant.
 *
 * TODO(b/369394452) : Remove once biometrics new stable library reached.
 */
internal object CryptoObjectUtils {

    /**
     * Wraps a crypto object to be passed to [android.hardware.biometrics.BiometricPrompt].
     *
     * @param cryptoObject An instance of [androidx.biometric.BiometricPrompt.CryptoObject].
     * @return An equivalent crypto object that is compatible with
     *   [android.hardware.biometrics.BiometricPrompt].
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun wrapForBiometricPrompt(
        cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject?
    ): BiometricPrompt.CryptoObject? {
        if (cryptoObject == null) {
            return null
        }

        val cipher = cryptoObject.cipher
        if (cipher != null) {
            return Api28Impl.create(cipher)
        }

        val signature = cryptoObject.signature
        if (signature != null) {
            return Api28Impl.create(signature)
        }

        val mac = cryptoObject.mac
        if (mac != null) {
            return Api28Impl.create(mac)
        }

        // Identity credential is only supported on API 30 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val identityCredential = cryptoObject.identityCredential
            if (identityCredential != null) {
                return Api30Impl.create(identityCredential)
            }
        }

        // Presentation session is only supported on API 33 and above, and thus not available in
        // the supported stable library (biometrics 1.1.0).

        // Operation handle is only supported on API 35 and above, and thus not available in
        // the supported stable library (biometrics 1.1.0).
        return null
    }

    /**
     * Get the `operationHandle` associated with this object or 0 if none. This needs to be achieved
     * by getting the corresponding [android.hardware.biometrics.BiometricPrompt.CryptoObject] and
     * then get its operation handle.
     *
     * @param cryptoObject An instance of [androidx.biometric.BiometricPrompt.CryptoObject].
     * @return The `operationHandle` associated with this object or 0 if none.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getOperationHandle(cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject?): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val wrappedCryptoObject = wrapForBiometricPrompt(cryptoObject)
            if (wrappedCryptoObject != null) {
                return Api35Impl.getOperationHandle(wrappedCryptoObject)
            }
        }
        return 0
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 15.0 (API 35).
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private object Api35Impl {
        /**
         * Gets the operation handle associated with the given crypto object, if any.
         *
         * @param crypto An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
         * @return The wrapped operation handle object, or `null`.
         */
        fun getOperationHandle(crypto: BiometricPrompt.CryptoObject): Long {
            return crypto.operationHandle
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11.0 (API 30).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private object Api30Impl {
        /**
         * Creates an instance of the framework class
         * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given identity
         * credential.
         *
         * @param identityCredential The identity credential object to be wrapped.
         * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
         */
        @Suppress("deprecation")
        fun create(identityCredential: IdentityCredential): BiometricPrompt.CryptoObject {
            return BiometricPrompt.CryptoObject(identityCredential)
        }
    }

    /** Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28). */
    @RequiresApi(Build.VERSION_CODES.P)
    private object Api28Impl {
        /**
         * Creates an instance of the framework class
         * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given cipher.
         *
         * @param cipher The cipher object to be wrapped.
         * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
         */
        fun create(cipher: Cipher): BiometricPrompt.CryptoObject {
            return BiometricPrompt.CryptoObject(cipher)
        }

        /**
         * Creates an instance of the framework class
         * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given signature.
         *
         * @param signature The signature object to be wrapped.
         * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
         */
        fun create(signature: Signature): BiometricPrompt.CryptoObject {
            return BiometricPrompt.CryptoObject(signature)
        }

        /**
         * Creates an instance of the framework class
         * [android.hardware.biometrics.BiometricPrompt.CryptoObject] from the given MAC.
         *
         * @param mac The MAC object to be wrapped.
         * @return An instance of [android.hardware.biometrics.BiometricPrompt.CryptoObject].
         */
        fun create(mac: Mac): BiometricPrompt.CryptoObject {
            return BiometricPrompt.CryptoObject(mac)
        }
    }
}

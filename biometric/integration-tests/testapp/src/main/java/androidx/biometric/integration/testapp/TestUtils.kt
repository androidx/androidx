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

package androidx.biometric.integration.testapp

import android.os.Build
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey

object TestUtils {
    internal const val KEY_LOG_TEXT = "key_log_text"
    internal const val KEY_NAME = "mySecretKey"
    internal const val KEYSTORE_INSTANCE = "AndroidKeyStore"
    internal const val PAYLOAD = "hello"

    /**
     * Converts an authentication result object to a string that represents its contents.
     */
    internal fun BiometricPrompt.AuthenticationResult.toDataString(): String {
        val typeString = authenticationType.toAuthenticationTypeString()
        return "crypto = $cryptoObject, type = $typeString"
    }

    /**
     * Converts an authentication status code to a string that represents the status.
     */
    internal fun Int.toAuthenticationStatusString(): String = when (this) {
        BiometricManager.BIOMETRIC_SUCCESS -> "SUCCESS"
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "STATUS_UNKNOWN"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "ERROR_UNSUPPORTED"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "ERROR_HW_UNAVAILABLE"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "ERROR_NONE_ENROLLED"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "ERROR_NO_HARDWARE"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
            "ERROR_SECURITY_UPDATE_REQUIRED"
        else -> "Unrecognized error: $this"
    }

    /**
     * Converts an authentication result type to a string that represents the method of
     * authentication.
     */
    internal fun Int.toAuthenticationTypeString(): String = when (this) {
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN -> "UNKNOWN"
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC -> "BIOMETRIC"
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL -> "DEVICE_CREDENTIAL"
        else -> "Unrecognized type: $this"
    }

    /**
     * Returns the cipher that will be used for encryption.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/" +
                KeyProperties.BLOCK_MODE_CBC + "/" +
                KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

    /**
     * Returns the previously generated secret key from keystore.
     */
    internal fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_INSTANCE)
        keyStore.load(null)
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }
}
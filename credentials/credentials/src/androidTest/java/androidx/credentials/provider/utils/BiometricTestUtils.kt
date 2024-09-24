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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.CryptoObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@RequiresApi(35)
object BiometricTestUtils {

    /** A name used to refer to an app-specified secret key. */
    private const val KEY_NAME = "mySecretKey"

    /** The name of the Android keystore provider instance. */
    private const val KEYSTORE_INSTANCE = "AndroidKeyStore"

    /**
     * Retrieve the operationHandle for a CryptoObject by converting it to the framework equivalent
     * type.
     */
    internal fun getTestCryptoObjectOpId(cryptoObject: CryptoObject) =
        CryptoObjectUtils.getOperationHandle(cryptoObject = cryptoObject)

    /**
     * Returns a [BiometricPrompt.CryptoObject] for crypto-based authentication. Adapted from:
     * [package androidx.biometric.samples.auth].
     */
    @RequiresApi(35)
    internal fun createCryptoObject(): BiometricPrompt.CryptoObject {
        val keyPurpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val keySpec =
            KeyGenParameterSpec.Builder(KEY_NAME, keyPurpose)
                .apply {
                    setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    // Disable user authentication requirement for test purposes.
                    // In reality, given this is primarily for biometric flows, Authenticator
                    // Types are expected, but emulators used in testing lack a
                    // lockscreen. This allows us to generate a more official
                    // CryptoObject instead of relying on mocks with this compromise.
                    setUserAuthenticationRequired(false)
                }
                .build()

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE).run {
            init(keySpec)
            generateKey()
        }

        val cipher =
            Cipher.getInstance(
                    "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/" +
                        KeyProperties.ENCRYPTION_PADDING_PKCS7
                )
                .apply {
                    val keyStore = KeyStore.getInstance(KEYSTORE_INSTANCE).apply { load(null) }
                    init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_NAME, null) as SecretKey)
                }

        return BiometricPrompt.CryptoObject(cipher)
    }
}

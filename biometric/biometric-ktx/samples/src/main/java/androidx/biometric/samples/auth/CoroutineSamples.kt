/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.biometric.samples.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.Sampled
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptErrorException
import androidx.biometric.auth.AuthPromptFailureException
import androidx.biometric.auth.AuthPromptHost
import androidx.biometric.auth.Class2BiometricAuthPrompt
import androidx.biometric.auth.Class2BiometricOrCredentialAuthPrompt
import androidx.biometric.auth.Class3BiometricAuthPrompt
import androidx.biometric.auth.Class3BiometricOrCredentialAuthPrompt
import androidx.biometric.auth.CredentialAuthPrompt
import androidx.biometric.auth.authenticate
import androidx.fragment.app.Fragment
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

// Stubbed definitions for samples
private const val KEYSTORE_INSTANCE = "AndroidKeyStore"
private const val KEY_NAME = "mySecretKey"
private const val title = ""
private const val subtitle = ""
private const val description = ""
private const val negativeButtonText = ""
private fun sendEncryptedPayload(payload: ByteArray?): ByteArray? = payload

@Sampled
suspend fun Fragment.class2BiometricAuth() {
    val payload = "A message to encrypt".toByteArray(Charset.defaultCharset())

    // Construct AuthPrompt with localized Strings to be displayed to UI.
    val authPrompt = Class2BiometricAuthPrompt.Builder(title, negativeButtonText).apply {
        setSubtitle(subtitle)
        setDescription(description)
        setConfirmationRequired(true)
    }.build()

    try {
        val authResult = authPrompt.authenticate(AuthPromptHost(this))

        // Encrypt a payload using the result of crypto-based auth.
        val encryptedPayload = authResult.cryptoObject?.cipher?.doFinal(payload)

        // Use the encrypted payload somewhere interesting.
        sendEncryptedPayload(encryptedPayload)
    } catch (e: AuthPromptErrorException) {
        // Handle irrecoverable error during authentication.
        // Possible values for AuthPromptErrorException.errorCode are listed in the @IntDef,
        // androidx.biometric.BiometricPrompt.AuthenticationError.
    } catch (e: AuthPromptFailureException) {
        // Handle auth failure due biometric credentials being rejected.
    }
}

@Sampled
suspend fun Fragment.class2BiometricOrCredentialAuth() {
    val payload = "A message to encrypt".toByteArray(Charset.defaultCharset())

    // Construct AuthPrompt with localized Strings to be displayed to UI.
    val authPrompt = Class2BiometricOrCredentialAuthPrompt.Builder(title).apply {
        setSubtitle(subtitle)
        setDescription(description)
        setConfirmationRequired(true)
    }.build()

    try {
        val authResult = authPrompt.authenticate(AuthPromptHost(this))

        // Encrypt a payload using the result of crypto-based auth.
        val encryptedPayload = authResult.cryptoObject?.cipher?.doFinal(payload)

        // Use the encrypted payload somewhere interesting.
        sendEncryptedPayload(encryptedPayload)
    } catch (e: AuthPromptErrorException) {
        // Handle irrecoverable error during authentication.
        // Possible values for AuthPromptErrorException.errorCode are listed in the @IntDef,
        // androidx.biometric.BiometricPrompt.AuthenticationError.
    } catch (e: AuthPromptFailureException) {
        // Handle auth failure due biometric credentials being rejected.
    }
}

@Sampled
@Suppress("NewApi", "ClassVerificationFailure")
suspend fun Fragment.class3BiometricAuth() {
    // To use Class3 authentication, we need to create a CryptoObject.
    // First create a spec for the key to be generated.
    val keyPurpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    val keySpec = KeyGenParameterSpec.Builder(KEY_NAME, keyPurpose).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        setUserAuthenticationRequired(true)

        // Require authentication for each use of the key.
        val timeout = 0
        // Set the key type according to the allowed auth types.
        val keyType =
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
        setUserAuthenticationParameters(timeout, keyType)
    }.build()

    // Generate and store the key in the Android keystore.
    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE).run {
        init(keySpec)
        generateKey()
    }

    // Prepare the crypto object to use for authentication.
    val cipher = Cipher.getInstance(
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
    ).apply {
        val keyStore = KeyStore.getInstance(KEYSTORE_INSTANCE).apply { load(null) }
        init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_NAME, null) as SecretKey)
    }

    val cryptoObject = BiometricPrompt.CryptoObject(cipher)
    val payload = "A message to encrypt".toByteArray(Charset.defaultCharset())

    // Construct AuthPrompt with localized Strings to be displayed to UI.
    val authPrompt = Class3BiometricAuthPrompt.Builder(title, negativeButtonText).apply {
        setSubtitle(subtitle)
        setDescription(description)
        setConfirmationRequired(true)
    }.build()

    try {
        val authResult = authPrompt.authenticate(AuthPromptHost(this), cryptoObject)

        // Encrypt a payload using the result of crypto-based auth.
        val encryptedPayload = authResult.cryptoObject?.cipher?.doFinal(payload)

        // Use the encrypted payload somewhere interesting.
        sendEncryptedPayload(encryptedPayload)
    } catch (e: AuthPromptErrorException) {
        // Handle irrecoverable error during authentication.
        // Possible values for AuthPromptErrorException.errorCode are listed in the @IntDef,
        // androidx.biometric.BiometricPrompt.AuthenticationError.
    } catch (e: AuthPromptFailureException) {
        // Handle auth failure due biometric credentials being rejected.
    }
}

@Sampled
@Suppress("NewApi", "ClassVerificationFailure")
suspend fun Fragment.class3BiometricOrCredentialAuth() {
    // To use Class3 authentication, we need to create a CryptoObject.
    // First create a spec for the key to be generated.
    val keyPurpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    val keySpec = KeyGenParameterSpec.Builder(KEY_NAME, keyPurpose).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        setUserAuthenticationRequired(true)

        // Require authentication for each use of the key.
        val timeout = 0
        // Set the key type according to the allowed auth types.
        val keyType =
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
        setUserAuthenticationParameters(timeout, keyType)
    }.build()

    // Generate and store the key in the Android keystore.
    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE).run {
        init(keySpec)
        generateKey()
    }

    // Prepare the crypto object to use for authentication.
    val cipher = Cipher.getInstance(
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
    ).apply {
        val keyStore = KeyStore.getInstance(KEYSTORE_INSTANCE).apply { load(null) }
        init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_NAME, null) as SecretKey)
    }

    val cryptoObject = BiometricPrompt.CryptoObject(cipher)
    val payload = "A message to encrypt".toByteArray(Charset.defaultCharset())

    // Construct AuthPrompt with localized Strings to be displayed to UI.
    val authPrompt = Class3BiometricOrCredentialAuthPrompt.Builder(title).apply {
        setSubtitle(subtitle)
        setDescription(description)
        setConfirmationRequired(true)
    }.build()

    try {
        val authResult = authPrompt.authenticate(AuthPromptHost(this), cryptoObject)

        // Encrypt a payload using the result of crypto-based auth.
        val encryptedPayload = authResult.cryptoObject?.cipher?.doFinal(payload)

        // Use the encrypted payload somewhere interesting.
        sendEncryptedPayload(encryptedPayload)
    } catch (e: AuthPromptErrorException) {
        // Handle irrecoverable error during authentication.
        // Possible values for AuthPromptErrorException.errorCode are listed in the @IntDef,
        // androidx.biometric.BiometricPrompt.AuthenticationError.
    } catch (e: AuthPromptFailureException) {
        // Handle auth failure due biometric credentials being rejected.
    }
}

@Sampled
@Suppress("NewApi", "ClassVerificationFailure")
suspend fun Fragment.credentialAuth() {
    // To use credential authentication, we need to create a CryptoObject.
    // First create a spec for the key to be generated.
    val keyPurpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    val keySpec = KeyGenParameterSpec.Builder(KEY_NAME, keyPurpose).apply {
        setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        setUserAuthenticationRequired(true)

        // Require authentication for each use of the key.
        val timeout = 0
        // Set the key type according to the allowed auth type.
        val keyType = KeyProperties.AUTH_DEVICE_CREDENTIAL
        setUserAuthenticationParameters(timeout, keyType)
    }.build()

    // Generate and store the key in the Android keystore.
    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE).run {
        init(keySpec)
        generateKey()
    }

    // Prepare the crypto object to use for authentication.
    val cipher = Cipher.getInstance(
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
    ).apply {
        val keyStore = KeyStore.getInstance(KEYSTORE_INSTANCE).apply { load(null) }
        init(Cipher.ENCRYPT_MODE, keyStore.getKey(KEY_NAME, null) as SecretKey)
    }

    val cryptoObject = BiometricPrompt.CryptoObject(cipher)
    val payload = "A message to encrypt".toByteArray(Charset.defaultCharset())

    // Construct AuthPrompt with localized Strings to be displayed to UI.
    val authPrompt = CredentialAuthPrompt.Builder(title).apply {
        setDescription(description)
    }.build()

    try {
        val authResult = authPrompt.authenticate(AuthPromptHost(this), cryptoObject)

        // Encrypt a payload using the result of crypto-based auth.
        val encryptedPayload = authResult.cryptoObject?.cipher?.doFinal(payload)

        // Use the encrypted payload somewhere interesting.
        sendEncryptedPayload(encryptedPayload)
    } catch (e: AuthPromptErrorException) {
        // Handle irrecoverable error during authentication.
        // Possible values for AuthPromptErrorException.errorCode are listed in the @IntDef,
        // androidx.biometric.BiometricPrompt.AuthenticationError.
    } catch (e: AuthPromptFailureException) {
        // Handle auth failure due biometric credentials being rejected.
    }
}

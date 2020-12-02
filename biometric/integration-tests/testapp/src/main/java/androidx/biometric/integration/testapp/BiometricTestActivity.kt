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

package androidx.biometric.integration.testapp

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.integration.testapp.TestUtils.KEYSTORE_INSTANCE
import androidx.biometric.integration.testapp.TestUtils.KEY_LOG_TEXT
import androidx.biometric.integration.testapp.TestUtils.KEY_NAME
import androidx.biometric.integration.testapp.TestUtils.PAYLOAD
import androidx.biometric.integration.testapp.TestUtils.getCipher
import androidx.biometric.integration.testapp.TestUtils.getSecretKey
import androidx.biometric.integration.testapp.TestUtils.toAuthenticationStatusString
import androidx.biometric.integration.testapp.TestUtils.toDataString
import androidx.fragment.app.FragmentActivity
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.KeyGenerator.getInstance

/**
 * Main activity for the AndroidX Biometric test app.
 */
@SuppressLint("SyntheticAccessor")
class BiometricTestActivity : FragmentActivity() {
    // The prompt used for authentication.
    private lateinit var biometricPrompt: BiometricPrompt

    // Individual UI elements.
    private lateinit var allowBiometricStrongCheckbox: CheckBox
    private lateinit var allowBiometricWeakCheckbox: CheckBox
    private lateinit var allowDeviceCredentialCheckbox: CheckBox
    private lateinit var cancelOnConfigChangeCheckbox: CheckBox
    private lateinit var requireConfirmationCheckbox: CheckBox
    private lateinit var useCryptoAuthCheckbox: CheckBox
    private lateinit var logView: TextView

    /**
     * A bit field representing the currently allowed authenticator type(s).
     */
    private val allowedAuthenticators: Int
        get() {
            var authenticators = 0
            if (allowBiometricStrongCheckbox.isChecked) {
                authenticators = authenticators or Authenticators.BIOMETRIC_STRONG
            }
            if (allowBiometricWeakCheckbox.isChecked) {
                authenticators = authenticators or Authenticators.BIOMETRIC_WEAK
            }
            if (allowDeviceCredentialCheckbox.isChecked) {
                authenticators = authenticators or Authenticators.DEVICE_CREDENTIAL
            }
            return authenticators
        }

    /**
     * A bit field representing the authentication type(s) that can authorize use of the secret key.
     */
    private val keyType: Int
        get() {
            var type = 0
            if (allowBiometricStrongCheckbox.isChecked) {
                type = type or KeyProperties.AUTH_BIOMETRIC_STRONG
            }
            if (allowDeviceCredentialCheckbox.isChecked) {
                type = type or KeyProperties.AUTH_DEVICE_CREDENTIAL
            }
            return type
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_test)

        // Get checkboxes from the UI so we can access their checked state later.
        allowBiometricStrongCheckbox = findViewById(R.id.checkbox_allow_biometric_strong)
        allowBiometricWeakCheckbox = findViewById(R.id.checkbox_allow_biometric_weak)
        allowDeviceCredentialCheckbox = findViewById(R.id.checkbox_allow_device_credential)
        cancelOnConfigChangeCheckbox = findViewById(R.id.checkbox_cancel_config_change)
        requireConfirmationCheckbox = findViewById(R.id.checkbox_require_confirmation)
        useCryptoAuthCheckbox = findViewById(R.id.checkbox_use_crypto_auth)

        // Set the button callbacks.
        findViewById<Button>(R.id.button_can_authenticate).setOnClickListener { canAuthenticate() }
        findViewById<Button>(R.id.button_authenticate).setOnClickListener { authenticate() }
        findViewById<Button>(R.id.button_clear_log).setOnClickListener { clearLog() }

        // Restore logged messages on activity recreation (e.g. due to device rotation).
        logView = findViewById(R.id.text_view_log)
        if (savedInstanceState != null) {
            logView.text = savedInstanceState.getCharSequence(KEY_LOG_TEXT, "")
        }

        // Reconnect the prompt by reinitializing with the new callback.
        biometricPrompt = BiometricPrompt(
            this,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    log("onAuthenticationError $errorCode: $errString")
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    log("onAuthenticationSucceeded: ${result.toDataString()}")

                    // Encrypt a test payload using the result of crypto-based auth.
                    if (useCryptoAuthCheckbox.isChecked) {
                        val encryptedPayload = result.cryptoObject?.cipher?.doFinal(
                            PAYLOAD.toByteArray(Charset.defaultCharset())
                        )
                        log("Encrypted payload: ${encryptedPayload?.contentToString()}")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    log("onAuthenticationFailed")
                }
            }
        )
    }

    override fun onStop() {
        super.onStop()

        // If option is selected, dismiss the prompt on rotation.
        if (cancelOnConfigChangeCheckbox.isChecked && isChangingConfigurations) {
            biometricPrompt.cancelAuthentication()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the current log messages to be restored on activity recreation.
        outState.putCharSequence(KEY_LOG_TEXT, logView.text)
    }

    /**
     * Logs the authentication status given by [BiometricManager.canAuthenticate].
     */
    private fun canAuthenticate() {
        val result = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        log("canAuthenticate: ${result.toAuthenticationStatusString()}")
    }

    /**
     * Launches the [BiometricPrompt] to begin authentication.
     */
    private fun authenticate() {
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setDescription(getString(R.string.biometric_prompt_description))
            .setConfirmationRequired(requireConfirmationCheckbox.isChecked)
            .setAllowedAuthenticators(allowedAuthenticators)
            .apply {
                // Set the negative button text ONLY if device credential auth is not allowed.
                if (allowedAuthenticators and Authenticators.DEVICE_CREDENTIAL == 0) {
                    setNegativeButtonText(getString(R.string.biometric_prompt_negative_label))
                }
            }

        val info: BiometricPrompt.PromptInfo?
        try {
            info = infoBuilder.build()
        } catch (e: IllegalArgumentException) {
            log("IllegalArgumentException: ${e.message}")
            return
        }

        if (useCryptoAuthCheckbox.isChecked) {
            authenticateWithCrypto(info)
        } else {
            biometricPrompt.authenticate(info)
        }
    }

    /**
     * Launches the [BiometricPrompt] to begin crypto-based authentication.
     */
    @Suppress("DEPRECATION")
    private fun authenticateWithCrypto(info: BiometricPrompt.PromptInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            log("Error: Key-gen not supported prior to API 23. Falling back to non-crypto auth.")
            biometricPrompt.authenticate(info)
            return
        }

        // Create a spec for the key to be generated.
        val keyPurpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val keySpec = KeyGenParameterSpec.Builder(KEY_NAME, keyPurpose)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .apply {
                // Require authentication for each use of the key.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0 /* timeout */, keyType)
                } else {
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
            }
            .build()

        // Generate and store the key in the Android keystore.
        val keyGenerator = getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_INSTANCE)
        keyGenerator.init(keySpec)
        keyGenerator.generateKey()

        // Prepare the crypto object to use for authentication.
        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val crypto = BiometricPrompt.CryptoObject(cipher)

        try {
            biometricPrompt.authenticate(info, crypto)
        } catch (e: IllegalArgumentException) {
            log("IllegalArgumentException: ${e.message}")
        }
    }

    /**
     * Clears all logged messages from the in-app [TextView].
     */
    private fun clearLog() {
        logView.text = ""
    }

    /**
     * Logs a new [message] to the in-app [TextView].
     */
    @SuppressLint("SetTextI18n")
    private fun log(message: CharSequence) {
        logView.text = "${message}\n${logView.text}"
    }
}

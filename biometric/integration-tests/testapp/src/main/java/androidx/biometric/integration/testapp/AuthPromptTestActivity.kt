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
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricAuthentication
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.biometric.auth.startClass3BiometricAuthentication
import androidx.biometric.auth.startClass3BiometricOrCredentialAuthentication
import androidx.biometric.auth.startCredentialAuthentication
import androidx.biometric.integration.testapp.R.layout.activity_authprompt_test
import androidx.biometric.integration.testapp.R.string.biometric_prompt_description
import androidx.biometric.integration.testapp.R.string.biometric_prompt_negative_label
import androidx.biometric.integration.testapp.R.string.biometric_prompt_subtitle
import androidx.biometric.integration.testapp.R.string.biometric_prompt_title
import androidx.biometric.integration.testapp.TestUtils.KEYSTORE_INSTANCE
import androidx.biometric.integration.testapp.TestUtils.KEY_LOG_TEXT
import androidx.biometric.integration.testapp.TestUtils.KEY_NAME
import androidx.biometric.integration.testapp.TestUtils.PAYLOAD
import androidx.biometric.integration.testapp.TestUtils.getCipher
import androidx.biometric.integration.testapp.TestUtils.getSecretKey
import androidx.biometric.integration.testapp.TestUtils.toAuthenticationStatusString
import androidx.biometric.integration.testapp.TestUtils.toDataString
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_authprompt_test.authType
import kotlinx.android.synthetic.main.activity_authprompt_test.button_authenticate
import kotlinx.android.synthetic.main.activity_authprompt_test.button_can_authenticate
import kotlinx.android.synthetic.main.activity_authprompt_test.button_clear_log
import kotlinx.android.synthetic.main.activity_authprompt_test.checkbox_cancel_config_change
import kotlinx.android.synthetic.main.activity_authprompt_test.checkbox_require_confirmation
import kotlinx.android.synthetic.main.activity_authprompt_test.checkbox_use_crypto_auth
import kotlinx.android.synthetic.main.activity_authprompt_test.class2_biometric_button
import kotlinx.android.synthetic.main.activity_authprompt_test.class2_biometric_or_credential_button
import kotlinx.android.synthetic.main.activity_authprompt_test.class3_biometric_button
import kotlinx.android.synthetic.main.activity_authprompt_test.class3_biometric_or_credential_button
import kotlinx.android.synthetic.main.activity_authprompt_test.credential_button
import kotlinx.android.synthetic.main.activity_authprompt_test.text_view_log
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.KeyGenerator.getInstance

/**
 * Main activity for the AndroidX Biometric test app.
 */
class AuthPromptTestActivity : FragmentActivity() {

    /**
     * Sample custom callback that logs all authentication events
     */
    private class MyCallback(val useCryptoAuth: Boolean) : AuthPromptCallback() {

        @SuppressLint("SyntheticAccessor")
        override fun onAuthenticationError(
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence
        ) {
            super.onAuthenticationError(activity, errorCode, errString)
            activity as AuthPromptTestActivity
            activity.log("onAuthenticationError $errorCode: $errString")
        }

        @SuppressLint("SyntheticAccessor")
        override fun onAuthenticationSucceeded(
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult
        ) {
            super.onAuthenticationSucceeded(activity, result)
            activity as AuthPromptTestActivity
            activity.log("onAuthenticationSucceeded: ${result.toDataString()}")

            // Encrypt a test payload using the result of crypto-based auth.
            if (useCryptoAuth) {
                val encryptedPayload = result.cryptoObject?.cipher?.doFinal(
                    PAYLOAD.toByteArray(Charset.defaultCharset())
                )
                activity.log("Encrypted payload: ${encryptedPayload?.contentToString()}")
            }
        }

        @SuppressLint("SyntheticAccessor")
        override fun onAuthenticationFailed(activity: FragmentActivity?) {
            super.onAuthenticationFailed(activity)
            activity as AuthPromptTestActivity
            activity.log("onAuthenticationFailed")
        }
    }

    // Individual UI elements.
    private lateinit var authPrompt: AuthPrompt // The new API prompt used for authentication.

    /**
     * A bit field representing the currently allowed authenticator type(s).
     */
    private val allowedAuthenticators: Int
        get() {
            var authenticators = 0
            if (class3_biometric_button.isChecked ||
                class3_biometric_or_credential_button.isChecked
            ) {
                authenticators = authenticators or Authenticators.BIOMETRIC_STRONG
            }
            if (class2_biometric_button.isChecked ||
                class2_biometric_or_credential_button.isChecked
            ) {
                authenticators = authenticators or Authenticators.BIOMETRIC_WEAK
            }
            if (class2_biometric_or_credential_button.isChecked ||
                class3_biometric_or_credential_button.isChecked ||
                credential_button.isChecked
            ) {
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
            if (class3_biometric_button.isChecked ||
                class3_biometric_or_credential_button.isChecked
            ) {
                type = type or KeyProperties.AUTH_BIOMETRIC_STRONG
            }
            if (class2_biometric_or_credential_button.isChecked ||
                class3_biometric_or_credential_button.isChecked ||
                credential_button.isChecked
            ) {
                type = type or KeyProperties.AUTH_DEVICE_CREDENTIAL
            }
            return type
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_authprompt_test)
        checkbox_use_crypto_auth.isEnabled = false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            checkbox_use_crypto_auth.isEnabled = false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            credential_button.isEnabled = false
            class3_biometric_or_credential_button.isEnabled = false
        }

        authType.setOnCheckedChangeListener { _, checkedAuthClassId ->
            var enableCrypto = (
                (checkedAuthClassId != R.id.class2_biometric_button) &&
                    (checkedAuthClassId != R.id.class2_biometric_or_credential_button)
                )
            checkbox_use_crypto_auth.isEnabled = enableCrypto
            if (!enableCrypto) checkbox_use_crypto_auth.isChecked = enableCrypto
        }

        // Set the button callbacks.
        button_can_authenticate.setOnClickListener { canAuthenticate() }
        button_authenticate.setOnClickListener { authenticate() }
        button_clear_log.setOnClickListener { clearLog() }

        // Restore logged messages on activity recreation (e.g. due to device rotation).
        if (savedInstanceState != null) {
            text_view_log.text = savedInstanceState.getCharSequence(KEY_LOG_TEXT, "")
        }
    }

    override fun onStop() {
        super.onStop()

        // If option is selected, dismiss the prompt on rotation.
        if (checkbox_cancel_config_change.isChecked && isChangingConfigurations) {
            authPrompt.cancelAuthentication()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the current log messages to be restored on activity recreation.
        outState.putCharSequence(KEY_LOG_TEXT, text_view_log.text)
    }

    /**
     * Logs the authentication status given by [BiometricManager.canAuthenticate].
     */
    @SuppressLint("SyntheticAccessor")
    private fun canAuthenticate() {
        val result = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        log("canAuthenticate: ${result.toAuthenticationStatusString()}")
    }

    /**
     * Launches the [AuthPrompt] to begin authentication.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun authenticate() {
        val title = getString(biometric_prompt_title)
        val subtitle = getString(biometric_prompt_subtitle)
        val description = getString(biometric_prompt_description)
        val confirmationRequired = checkbox_require_confirmation.isChecked
        val negativeButtonText = getString(biometric_prompt_negative_label)

        @SuppressLint("SyntheticAccessor")
        val callback = MyCallback(checkbox_use_crypto_auth.isChecked)

        when (authType.checkedRadioButtonId) {
            R.id.class2_biometric_button ->
                authPrompt = startClass2BiometricAuthentication(
                    title,
                    negativeButtonText,
                    callback = callback
                )
            R.id.class3_biometric_button ->
                authPrompt = if (checkbox_use_crypto_auth.isChecked) {
                    startClass3BiometricAuthentication(
                        crypto = getCryptoObject(),
                        title,
                        negativeButtonText,
                        subtitle,
                        description,
                        callback = callback
                    )
                } else {
                    startClass3BiometricAuthentication(
                        crypto = null,
                        title,
                        negativeButtonText,
                        subtitle,
                        description,
                        callback = callback
                    )
                }
            R.id.class2_biometric_or_credential_button ->
                authPrompt = startClass2BiometricOrCredentialAuthentication(
                    title,
                    subtitle,
                    description,
                    confirmationRequired,
                    callback = callback
                )
            R.id.class3_biometric_or_credential_button ->
                authPrompt = if (checkbox_use_crypto_auth.isChecked) {
                    startClass3BiometricOrCredentialAuthentication(
                        crypto = getCryptoObject(),
                        title,
                        subtitle,
                        description,
                        confirmationRequired,
                        callback = callback
                    )
                } else {
                    startClass3BiometricOrCredentialAuthentication(
                        crypto = null,
                        title,
                        subtitle,
                        description,
                        confirmationRequired,
                        callback = callback
                    )
                }
            R.id.credential_button ->
                authPrompt = if (checkbox_use_crypto_auth.isChecked) {
                    startCredentialAuthentication(
                        crypto = getCryptoObject(),
                        title,
                        description,
                        callback = callback
                    )
                } else {
                    startCredentialAuthentication(
                        crypto = null,
                        title,
                        description,
                        callback = callback
                    )
                }
        }
    }

    /**
     * Returns [BiometricPrompt.CryptoObject] for crypto-based authentication.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("UnsafeNewApiCall", "SyntheticAccessor", "TrulyRandom")
    @Suppress("DEPRECATION")
    private fun getCryptoObject(): BiometricPrompt.CryptoObject {
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

        return BiometricPrompt.CryptoObject(cipher)
    }

    /**
     * Clears all logged messages from the in-app [TextView].
     */
    private fun clearLog() {
        text_view_log.text = ""
    }

    /**
     * Logs a new [message] to the in-app [TextView].
     */
    @SuppressLint("SetTextI18n")
    private fun log(message: CharSequence) {
        text_view_log.text = "${message}\n${text_view_log.text}"
    }
}
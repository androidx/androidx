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

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricAuthentication
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.biometric.auth.authenticateWithClass3Biometrics
import androidx.biometric.auth.startClass3BiometricOrCredentialAuthentication
import androidx.biometric.auth.startCredentialAuthentication
import androidx.biometric.integration.testapp.R.string.biometric_prompt_description
import androidx.biometric.integration.testapp.R.string.biometric_prompt_negative_text
import androidx.biometric.integration.testapp.R.string.biometric_prompt_subtitle
import androidx.biometric.integration.testapp.R.string.biometric_prompt_title
import androidx.biometric.integration.testapp.databinding.AuthPromptTestActivityBinding
import androidx.fragment.app.FragmentActivity
import java.nio.charset.Charset

/**
 * Interactive test activity for the [androidx.biometric.auth] APIs.
 */
class AuthPromptTestActivity : FragmentActivity() {
    private lateinit var binding: AuthPromptTestActivityBinding

    /**
     * A handle to the prompt for an ongoing authentication session.
     */
    private var authPrompt: AuthPrompt? = null

    /**
     * A bit field representing the currently allowed authenticator type(s).
     */
    private val allowedAuthenticators: Int
        get() {
            var authenticators = 0

            if (binding.class3BiometricButton.isChecked ||
                binding.class3BiometricOrCredentialButton.isChecked
            ) {
                authenticators = authenticators or Authenticators.BIOMETRIC_STRONG
            }

            if (binding.class2BiometricButton.isChecked ||
                binding.class2BiometricOrCredentialButton.isChecked
            ) {
                authenticators = authenticators or Authenticators.BIOMETRIC_WEAK
            }

            if (binding.class2BiometricOrCredentialButton.isChecked ||
                binding.class3BiometricOrCredentialButton.isChecked ||
                binding.credentialButton.isChecked
            ) {
                authenticators = authenticators or Authenticators.DEVICE_CREDENTIAL
            }

            return authenticators
        }

    /**
     * Whether the selected options allow for biometric authentication.
     */
    private val isBiometricAllowed: Boolean
        get() {
            return binding.class2BiometricButton.isChecked ||
                binding.class2BiometricOrCredentialButton.isChecked ||
                binding.class3BiometricButton.isChecked ||
                binding.class3BiometricOrCredentialButton.isChecked
        }

    /**
     * Whether the selected options allow for device credential authentication.
     */
    private val isCredentialAllowed: Boolean
        get() {
            return binding.class2BiometricOrCredentialButton.isChecked ||
                binding.class3BiometricOrCredentialButton.isChecked ||
                binding.credentialButton.isChecked
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthPromptTestActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disallow unsupported authentication type combinations.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            binding.class3BiometricOrCredentialButton.isEnabled = false
            binding.credentialButton.isEnabled = false
        }

        // Crypto-based authentication is not supported prior to Android 6.0 (API 23).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            binding.common.useCryptoAuthCheckbox.isEnabled = false
        }

        // Set button callbacks.
        binding.authTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            updateCryptoCheckboxState(checkedId)
        }
        binding.common.canAuthenticateButton.setOnClickListener { canAuthenticate() }
        binding.common.authenticateButton.setOnClickListener { authenticate() }
        binding.common.clearLogButton.setOnClickListener { clearLog() }

        // Restore logged messages on activity recreation (e.g. due to device rotation).
        if (savedInstanceState != null) {
            binding.common.logTextView.text = savedInstanceState.getCharSequence(KEY_LOG_TEXT, "")
        }
    }

    override fun onStop() {
        super.onStop()

        // If option is selected, dismiss the prompt on rotation.
        if (binding.common.cancelConfigChangeCheckbox.isChecked && isChangingConfigurations) {
            authPrompt?.cancelAuthentication()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the current log messages to be restored on activity recreation.
        outState.putCharSequence(KEY_LOG_TEXT, binding.common.logTextView.text)
    }

    /**
     * Updates the state of the crypto-based auth checkbox when a given [checkedId] is selected
     * from the authentication types radio group.
     */
    private fun updateCryptoCheckboxState(checkedId: Int) {
        // Crypto-based authentication is not supported prior to Android 6.0 (API 23).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        val isCheckboxEnabled = checkedId != R.id.class2_biometric_button &&
            checkedId != R.id.class2_biometric_or_credential_button

        binding.common.useCryptoAuthCheckbox.isEnabled = isCheckboxEnabled
        if (!isCheckboxEnabled) {
            binding.common.useCryptoAuthCheckbox.isChecked = false
        }
    }

    /**
     * Logs the authentication status given by [BiometricManager.canAuthenticate].
     */
    private fun canAuthenticate() {
        val result = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        log("canAuthenticate: ${result.toAuthenticationStatusString()}")
    }

    /**
     * Launches the appropriate [AuthPrompt] to begin authentication.
     */
    private fun authenticate() {
        val title = getString(biometric_prompt_title)
        val subtitle = getString(biometric_prompt_subtitle)
        val description = getString(biometric_prompt_description)
        val negativeButtonText = getString(biometric_prompt_negative_text)
        val confirmationRequired = binding.common.requireConfirmationCheckbox.isChecked
        val callback = AuthCallback()

        authPrompt = when (val buttonId = binding.authTypeGroup.checkedRadioButtonId) {
            R.id.class2_biometric_button ->
                startClass2BiometricAuthentication(
                    title = title,
                    negativeButtonText = negativeButtonText,
                    callback = callback
                )

            R.id.class3_biometric_button ->
                authenticateWithClass3Biometrics(
                    crypto = createCryptoOrNull(),
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    negativeButtonText = negativeButtonText,
                    callback = callback
                )

            R.id.class2_biometric_or_credential_button ->
                startClass2BiometricOrCredentialAuthentication(
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    confirmationRequired = confirmationRequired,
                    callback = callback
                )

            R.id.class3_biometric_or_credential_button ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startClass3BiometricOrCredentialAuthentication(
                        crypto = createCryptoOrNull(),
                        title = title,
                        subtitle = subtitle,
                        description = description,
                        confirmationRequired = confirmationRequired,
                        callback = callback
                    )
                } else {
                    val sdkInt = Build.VERSION.SDK_INT
                    log("Error: Class 3 biometric or credential auth not supported on API $sdkInt.")
                    null
                }

            R.id.credential_button ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startCredentialAuthentication(
                        crypto = createCryptoOrNull(),
                        title = title,
                        description = description,
                        callback = callback
                    )
                } else {
                    val sdkInt = Build.VERSION.SDK_INT
                    log("Error: Credential-only auth not supported on API $sdkInt.")
                    null
                }

            else -> throw IllegalStateException("Invalid checked button ID: $buttonId")
        }
    }

    /**
     * Returns a new crypto object for authentication or `null`, based on the selected options.
     */
    private fun createCryptoOrNull(): BiometricPrompt.CryptoObject? {
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            binding.common.useCryptoAuthCheckbox.isChecked
        ) {
            createCryptoObject(isBiometricAllowed, isCredentialAllowed)
        } else {
            null
        }
    }

    /**
     * Logs a new [message] to the in-app [TextView].
     */
    internal fun log(message: CharSequence) {
        binding.common.logTextView.prependLogMessage(message)
    }

    /**
     * Clears all logged messages from the in-app [TextView].
     */
    private fun clearLog() {
        binding.common.logTextView.text = ""
    }

    /**
     * Sample callback that logs all authentication events.
     */
    private class AuthCallback : AuthPromptCallback() {
        override fun onAuthenticationError(
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence
        ) {
            super.onAuthenticationError(activity, errorCode, errString)
            if (activity is AuthPromptTestActivity) {
                activity.log("onAuthenticationError $errorCode: $errString")
            }
        }

        override fun onAuthenticationSucceeded(
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult
        ) {
            super.onAuthenticationSucceeded(activity, result)
            if (activity is AuthPromptTestActivity) {
                activity.log("onAuthenticationSucceeded: ${result.toDataString()}")

                // Encrypt a test payload using the result of crypto-based auth.
                if (activity.binding.common.useCryptoAuthCheckbox.isChecked) {
                    val encryptedPayload = result.cryptoObject?.cipher?.doFinal(
                        PAYLOAD.toByteArray(Charset.defaultCharset())
                    )
                    activity.log("Encrypted payload: ${encryptedPayload?.contentToString()}")
                }
            }
        }

        override fun onAuthenticationFailed(activity: FragmentActivity?) {
            super.onAuthenticationFailed(activity)
            if (activity is AuthPromptTestActivity) {
                activity.log("onAuthenticationFailed")
            }
        }
    }
}
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
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_test)

        // Get checkboxes from the UI so we can access their checked state later.
        allowBiometricStrongCheckbox = findViewById(R.id.checkbox_allow_biometric_strong)
        allowBiometricWeakCheckbox = findViewById(R.id.checkbox_allow_biometric_weak)
        allowDeviceCredentialCheckbox = findViewById(R.id.checkbox_allow_device_credential)
        cancelOnConfigChangeCheckbox = findViewById(R.id.checkbox_cancel_config_change)
        requireConfirmationCheckbox = findViewById(R.id.checkbox_require_confirmation)

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
                    log("onAuthenticationSucceeded: crypto = ${result.cryptoObject}")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    log("onAuthenticationFailed")
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()

        // If option is selected, dismiss the prompt on rotation.
        if (cancelOnConfigChangeCheckbox.isChecked) {
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

        // Set the negative button text ONLY if device credential authentication is not enabled.
        if (allowedAuthenticators and Authenticators.DEVICE_CREDENTIAL == 0) {
            infoBuilder.setNegativeButtonText(getString(R.string.biometric_prompt_negative_label))
        }

        biometricPrompt.authenticate(infoBuilder.build())
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
    private fun log(message: CharSequence) {
        logView.append("${message}\n")
    }

    companion object {
        private const val KEY_LOG_TEXT = "key_log_text"

        /**
         * Converts an authentication status code to a string that represents the status.
         */
        private fun Int.toAuthenticationStatusString(): String = when (this) {
            BiometricManager.BIOMETRIC_SUCCESS -> "SUCCESS"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "STATUS_UNKNOWN"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "ERROR_UNSUPPORTED"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "ERROR_HW_UNAVAILABLE"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "ERROR_NONE_ENROLLED"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "ERROR_NO_HARDWARE"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "ERROR_SECURITY_UPDATE_REQUIRED"
            else -> "Unknown error: $this"
        }
    }
}

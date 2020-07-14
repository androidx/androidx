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
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Main activity for the AndroidX Biometric test app.
 */
@SuppressLint("SyntheticAccessor")
class BiometricTestActivity : FragmentActivity() {
    // Prompt-related fields.
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt

    // Individual UI elements.
    private lateinit var deviceCredentialCheckbox: CheckBox
    private lateinit var cancelOnConfigChangeCheckbox: CheckBox
    private lateinit var requireConfirmationCheckbox: CheckBox
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_test)
        executor = ContextCompat.getMainExecutor(this)

        val showPromptButton = findViewById<Button>(R.id.button_show_prompt)
        showPromptButton.setOnClickListener { showPrompt() }

        // Get checkboxes from the UI so we can access their checked state later.
        deviceCredentialCheckbox = findViewById(R.id.checkbox_device_credential)
        cancelOnConfigChangeCheckbox = findViewById(R.id.checkbox_cancel_config_change)
        requireConfirmationCheckbox = findViewById(R.id.checkbox_require_confirmation)

        val clearLogButton = findViewById<Button>(R.id.button_clear_log)
        clearLogButton.setOnClickListener { clearLogs() }

        // Restore logged messages on activity recreation (e.g. due to device rotation).
        logView = findViewById(R.id.text_view_log)
        if (savedInstanceState != null) {
            logView.text = savedInstanceState.getCharSequence(KEY_LOG_TEXT, "")
        }

        // Reconnect the prompt by reinitializing with the new executor and callback.
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    log("onAuthenticationError $errorCode: $errString")
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    log("onAuthenticationSuccess: crypto = ${result.cryptoObject}")
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
     * Launches the [BiometricPrompt] to begin authentication.
     */
    private fun showPrompt() {
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setDescription(getString(R.string.biometric_prompt_description))
            .setConfirmationRequired(requireConfirmationCheckbox.isChecked)

        if (deviceCredentialCheckbox.isChecked) {
            infoBuilder.setDeviceCredentialAllowed(true)
        } else {
            infoBuilder.setNegativeButtonText(getString(R.string.biometric_prompt_negative_label))
        }

        biometricPrompt.authenticate(infoBuilder.build())
    }

    /**
     * Clears all logged messages from the in-app [TextView].
     */
    private fun clearLogs() {
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
    }
}

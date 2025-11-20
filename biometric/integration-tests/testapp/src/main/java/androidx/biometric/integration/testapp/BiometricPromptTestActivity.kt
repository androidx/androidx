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
import androidx.biometric.PromptContentItemBulletedText
import androidx.biometric.PromptVerticalListContentView
import androidx.biometric.integration.testapp.databinding.BiometricPromptTestActivityBinding
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.security.InvalidAlgorithmParameterException

private const val IDENTITY_CHECK = 1 shl 16

/** Interactive test activity for the [BiometricPrompt] and [BiometricManager] APIs. */
class BiometricPromptTestActivity : FragmentActivity() {
    private lateinit var binding: BiometricPromptTestActivityBinding

    /** The prompt used for authentication. */
    private lateinit var biometricPrompt: BiometricPrompt

    /** A bit field representing the currently allowed authenticator type(s). */
    private val allowedAuthenticators: Int
        get() {
            var authenticators = 0
            if (binding.allowBiometricStrongCheckbox.isChecked) {
                authenticators = authenticators or Authenticators.BIOMETRIC_STRONG
            }
            if (binding.allowBiometricWeakCheckbox.isChecked) {
                authenticators = authenticators or Authenticators.BIOMETRIC_WEAK
            }
            if (binding.allowDeviceCredentialCheckbox.isChecked) {
                authenticators = authenticators or Authenticators.DEVICE_CREDENTIAL
            }
            if (binding.allowIdentityCheckbox.isChecked) {
                authenticators = authenticators or IDENTITY_CHECK
            }
            return authenticators
        }

    /** Whether the selected options allow for biometric authentication. */
    private val isBiometricAllowed: Boolean
        get() {
            return binding.allowBiometricStrongCheckbox.isChecked ||
                binding.allowBiometricWeakCheckbox.isChecked
        }

    /** Whether the selected options allow for device credential authentication. */
    private val isCredentialAllowed: Boolean
        get() = binding.allowDeviceCredentialCheckbox.isChecked

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BiometricPromptTestActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set button callbacks.
        binding.common.canAuthenticateButton.setOnClickListener { canAuthenticate() }
        binding.common.authenticateButton.setOnClickListener { authenticate() }
        binding.common.clearLogButton.setOnClickListener { clearLog() }

        // Restore logged messages on activity recreation (e.g. due to device rotation).
        if (savedInstanceState != null) {
            binding.common.logTextView.text = savedInstanceState.getCharSequence(KEY_LOG_TEXT, "")
        }

        // Reconnect the prompt by reinitializing with the new callback.
        biometricPrompt = BiometricPrompt(this, AuthCallback(this))
    }

    override fun onStop() {
        super.onStop()

        // If option is selected, dismiss the prompt on rotation.
        if (binding.common.cancelConfigChangeCheckbox.isChecked && isChangingConfigurations) {
            biometricPrompt.cancelAuthentication()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the current log messages to be restored on activity recreation.
        outState.putCharSequence(KEY_LOG_TEXT, binding.common.logTextView.text)
    }

    /** Logs the authentication status given by [BiometricManager.canAuthenticate]. */
    private fun canAuthenticate() {
        val result = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        log("canAuthenticate: ${result.toAuthenticationStatusString()}")
    }

    /** Launches the [BiometricPrompt] to begin authentication. */
    private fun authenticate() {
        val infoBuilder =
            BiometricPrompt.PromptInfo.Builder().apply {
                setTitle(getString(R.string.biometric_prompt_title))
                setSubtitle(getString(R.string.biometric_prompt_subtitle))
                setDescription(getString(R.string.biometric_prompt_description))
                setConfirmationRequired(binding.common.requireConfirmationCheckbox.isChecked)
                setAllowedAuthenticators(allowedAuthenticators)

                // Set the negative button text ONLY if device credential auth is not allowed.
                if (allowedAuthenticators and Authenticators.DEVICE_CREDENTIAL == 0) {
                    setNegativeButtonText(getString(R.string.biometric_prompt_negative_text))
                }
            }

        if (binding.common.plainTextContent.isChecked) {
            infoBuilder.setDescription("Description")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            infoBuilder.setContentView(
                PromptVerticalListContentView.Builder()
                    .apply {
                        setDescription("Vertical list description")
                        addListItem(PromptContentItemBulletedText("test item1"))
                        addListItem(PromptContentItemBulletedText("test item2"))
                    }
                    .build()
            )
        }

        val info: BiometricPrompt.PromptInfo?
        try {
            info = infoBuilder.build()
        } catch (e: IllegalArgumentException) {
            log("$e")
            return
        }

        if (binding.common.useCryptoAuthCheckbox.isChecked) {
            authenticateWithCrypto(info)
        } else {
            biometricPrompt.authenticate(info)
        }
    }

    /** Launches the [BiometricPrompt] to begin crypto-based authentication. */
    @Suppress("DEPRECATION")
    private fun authenticateWithCrypto(info: BiometricPrompt.PromptInfo) {
        try {
            val cryptoObject = createCryptoObject(isBiometricAllowed, isCredentialAllowed)
            biometricPrompt.authenticate(info, cryptoObject)
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is InvalidAlgorithmParameterException -> log("$e")
                else -> throw e
            }
        }
    }

    /** Logs a new [message] to the in-app [TextView]. */
    internal fun log(message: CharSequence) {
        binding.common.logTextView.prependLogMessage(message)
    }

    /** Clears all logged messages from the in-app [TextView]. */
    private fun clearLog() {
        binding.common.logTextView.text = ""
    }

    /** Sample callback that logs all authentication events. */
    private class AuthCallback(activity: BiometricPromptTestActivity) :
        BiometricPrompt.AuthenticationCallback() {
        private val activityRef = WeakReference(activity)

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            activityRef.get()?.log("onAuthenticationError $errorCode: $errString")
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            activityRef.get()?.run {
                log("onAuthenticationSucceeded: ${result.toDataString()}")

                // Encrypt a test payload using the result of crypto-based auth.
                if (binding.common.useCryptoAuthCheckbox.isChecked) {
                    val encryptedPayload =
                        result.cryptoObject
                            ?.cipher
                            ?.doFinal(PAYLOAD.toByteArray(Charset.defaultCharset()))
                    log("Encrypted payload: ${encryptedPayload?.contentToString()}")
                }
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            activityRef.get()?.log("onAuthenticationFailed")
        }
    }
}

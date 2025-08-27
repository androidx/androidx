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
package androidx.biometric.integration.testapp

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationRequest.Companion.credentialRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.PromptContentItemBulletedText
import androidx.biometric.integration.testapp.databinding.AuthenticationResultTestActivityBinding
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.FragmentActivity
import java.nio.charset.Charset
import java.security.InvalidAlgorithmParameterException

/** Interactive test activity for the [androidx.biometric:biometric-ktx] APIs. */
class AuthenticationResultTestActivity : FragmentActivity() {
    private lateinit var binding: AuthenticationResultTestActivityBinding

    /** Whether the selected options allow for biometric authentication. */
    private val isBiometricAllowed: Boolean
        get() {
            return binding.class2BiometricButton.isChecked ||
                binding.class3BiometricButton.isChecked
        }

    /** Whether the selected options allow for device credential authentication. */
    private val isCredentialAllowed: Boolean
        get() {
            return binding.credentialFallback.isChecked || binding.credentialButton.isChecked
        }

    private val authResultLauncher =
        registerForAuthenticationResult(
            object : AuthenticationResultCallback {
                override fun onAuthResult(result: AuthenticationResult) {
                    when (result) {
                        is AuthenticationResult.Success -> onAuthenticationSucceeded(result)
                        is AuthenticationResult.Error -> onAuthenticationError(result)
                    }
                }

                override fun onAuthFailure() {
                    onAuthenticationFailed()
                }
            }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthenticationResultTestActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateCredentialOnlyAndBodyContent()
        updateFallbackAndOtherOptions()
        binding.authTypeGroup.setOnCheckedChangeListener { _, _ -> updateFallbackAndOtherOptions() }
        binding.credentialFallback.setOnCheckedChangeListener { _, _ -> updateOtherOptions() }

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
            authResultLauncher.cancel()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current log messages to be restored on activity recreation.
        outState.putCharSequence(KEY_LOG_TEXT, binding.common.logTextView.text)
    }

    private fun authenticate() {
        val title = "Title"
        val subtitle = "Subtitle"
        val bodyContent =
            if (binding.common.plainTextContent.isChecked) {
                AuthenticationRequest.BodyContent.PlainText("Description")
            } else if (binding.common.verticalListContent.isChecked) {
                AuthenticationRequest.BodyContent.VerticalList(
                    "Vertical list description",
                    listOf(
                        PromptContentItemBulletedText("test item1"),
                        PromptContentItemBulletedText("test item2"),
                    ),
                )
            } else {
                null
            }

        val authRequest =
            if (binding.credentialButton.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    credentialRequest(title = title) {
                        setSubtitle(subtitle)
                        setContent(bodyContent)
                        setCryptoObject(createCryptoOrNull())
                    }
                } else {
                    log("Credential-only authentication is not supported prior to API 30.")
                    null
                }
            } else {
                biometricRequest(
                    title = title,
                    authFallback =
                        if (binding.credentialFallback.isChecked) {
                            Biometric.Fallback.DeviceCredential
                        } else {
                            Biometric.Fallback.NegativeButton("Cancel button")
                        },
                ) {
                    setSubtitle(subtitle)
                    setContent(bodyContent)
                    setMinStrength(
                        if (binding.class2BiometricButton.isChecked) {
                            Biometric.Strength.Class2
                        } else {
                            Biometric.Strength.Class3(cryptoObject = createCryptoOrNull())
                        }
                    )
                    setIsConfirmationRequired(binding.common.requireConfirmationCheckbox.isChecked)
                }
            }

        try {
            authRequest?.let { authResultLauncher.launch(it) }
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException,
                is InvalidAlgorithmParameterException -> log("$e")
                else -> throw e
            }
        }
    }

    /** Logs the authentication status given by [BiometricManager.canAuthenticate]. */
    private fun canAuthenticate() {
        /** A bit field representing the currently allowed authenticator type(s). */
        var allowedAuthenticators: Int =
            if (binding.class3BiometricButton.isChecked) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            } else if (binding.class2BiometricButton.isChecked) {
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            } else if (binding.credentialButton.isChecked) {
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                0
            }
        if (binding.credentialFallback.isChecked) {
            allowedAuthenticators =
                allowedAuthenticators or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

        val result = BiometricManager.from(this).canAuthenticate(allowedAuthenticators)
        log("canAuthenticate: ${result.toAuthenticationStatusString()}")
    }

    private fun updateCredentialOnlyAndBodyContent() {
        // < API 30: [Credential](device credential only) is not supported.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            binding.credentialButton.isEnabled = false
        }

        // < API 35, non-plain text body content is not supported
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            binding.common.verticalListContent.isEnabled = false
        }
        if (!binding.common.verticalListContent.isEnabled) {
            binding.common.plainTextContent.isChecked = true
        }
    }

    private fun updateFallbackAndOtherOptions() {
        val authType = binding.authTypeGroup.checkedRadioButtonId
        val isClass3 = authType == R.id.class3_biometric_button
        val isCredential = authType == R.id.credential_button
        // Fallback is for biometric authentication only
        // API 28/29: Class3 + DeviceCredential is not supported.
        binding.credentialFallback.isEnabled =
            !isCredential &&
                !((Build.VERSION.SDK_INT == Build.VERSION_CODES.P ||
                    Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) && isClass3)
        if (!binding.credentialFallback.isEnabled) {
            binding.credentialFallback.isChecked = false
        }
        binding.negativeButtonFallback.isEnabled = !isCredential
        if (!binding.negativeButtonFallback.isEnabled) {
            binding.negativeButtonFallback.isChecked = false
        }
        updateOtherOptions()
    }

    /**
     * Updates the states of crypto-based auth checkbox and require confirmation checkbox with
     * authentication type and api level.
     */
    private fun updateOtherOptions() {
        val authType = binding.authTypeGroup.checkedRadioButtonId
        val isClass3 = authType == R.id.class3_biometric_button
        val isClass2 = authType == R.id.class2_biometric_button
        val isCredential = authType == R.id.credential_button
        val isFallbackDeviceCredential = binding.credentialFallback.isChecked

        // CryptoObject cannot be used if it's class2
        //  < API 28: For Class3 + DeviceCredential, cryptoObject cannot be used
        binding.common.useCryptoAuthCheckbox.isEnabled =
            !isClass2 &&
                !(Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
                    isClass3 &&
                    isFallbackDeviceCredential)
        if (!binding.common.useCryptoAuthCheckbox.isEnabled) {
            binding.common.useCryptoAuthCheckbox.isChecked = false
        }

        // Require confirmation is for biometric authentication only
        binding.common.requireConfirmationCheckbox.isEnabled = !isCredential
        if (!binding.common.requireConfirmationCheckbox.isEnabled) {
            binding.common.requireConfirmationCheckbox.isChecked = true
        }
    }

    private fun onAuthenticationError(result: AuthenticationResult.Error) {
        log("onAuthenticationError " + result.errorCode + " " + result.errString)
    }

    private fun onAuthenticationSucceeded(result: AuthenticationResult.Success) {
        log("onAuthenticationSucceeded with type " + result.authType)
        // Encrypt a test payload using the result of crypto-based auth.
        if (binding.common.useCryptoAuthCheckbox.isChecked) {
            val encryptedPayload =
                result.crypto?.cipher?.doFinal(PAYLOAD.toByteArray(Charset.defaultCharset()))
            log("Encrypted payload: ${encryptedPayload?.contentToString()}")
        }
    }

    private fun onAuthenticationFailed() {
        log("onAuthenticationFailed, try again")
    }

    /** Returns a new crypto object for authentication or `null`, based on the selected options. */
    private fun createCryptoOrNull(): BiometricPrompt.CryptoObject? {
        return if (binding.common.useCryptoAuthCheckbox.isChecked) {
            createCryptoObject(isBiometricAllowed, isCredentialAllowed)
        } else {
            null
        }
    }

    /** Logs a new [message] to the in-app [TextView]. */
    private fun log(message: CharSequence) {
        binding.common.logTextView.prependLogMessage(message)
    }

    /** Clears all logged messages from the in-app [TextView]. */
    private fun clearLog() {
        binding.common.logTextView.text = ""
    }
}

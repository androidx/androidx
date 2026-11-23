/*
 * Copyright 2026 The Android Open Source Project
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

import android.os.Bundle
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.BiometricManager
import androidx.biometric.integration.testapp.databinding.SimpleBiometricAuthActivityBinding
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.FragmentActivity

/** Minimal standard biometric authentication sample activity. */
class SimpleBiometricAuthActivity : FragmentActivity() {
    private lateinit var binding: SimpleBiometricAuthActivityBinding

    private val authLauncher =
        registerForAuthenticationResult(
            object : AuthenticationResultCallback {
                override fun onAuthResult(result: AuthenticationResult) {
                    when (result) {
                        is AuthenticationResult.Success ->
                            log("onAuthResult: Success, auth type: ${result.authType}")
                        is AuthenticationResult.Error ->
                            log(
                                "onAuthResult: Error, code: ${result.errorCode}, msg: ${result.errString}"
                            )
                        is AuthenticationResult.CustomFallbackSelected ->
                            log("onAuthResult: CustomFallbackSelected: ${result.fallback.text}")
                    }
                }

                override fun onAuthAttemptFailed() {
                    log("onAuthAttemptFailed: Try again")
                }
            }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SimpleBiometricAuthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.canAuthenticateButton.setOnClickListener {
            val status =
                BiometricManager.from(this)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            log("canAuthenticate (Class 2/Weak): ${status.toAuthenticationStatusString()}")
        }

        binding.authenticateButton.setOnClickListener {
            try {
                val title =
                    binding.titleEditText.text.toString().ifEmpty { "Simple Biometric Auth" }
                val subtitle = binding.subtitleEditText.text.toString().ifEmpty { null }
                val requireConfirmation = binding.requireConfirmationCheckbox.isChecked

                val request =
                    biometricRequest(title) {
                        setSubtitle(subtitle)
                        setIsConfirmationRequired(requireConfirmation)
                    }
                authLauncher.launch(request)
            } catch (e: Exception) {
                log("Launch error: $e")
            }
        }

        binding.clearLogButton.setOnClickListener { binding.logTextView.text = "" }
    }

    private fun log(message: CharSequence) {
        binding.logTextView.prependLogMessage(message)
    }
}

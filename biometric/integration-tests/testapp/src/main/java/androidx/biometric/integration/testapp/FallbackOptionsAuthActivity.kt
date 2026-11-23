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
import android.widget.ArrayAdapter
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.integration.testapp.databinding.FallbackOptionsAuthActivityBinding
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.FragmentActivity

/** Fallback options sample activity demonstrating single vs multiple fallback options. */
class FallbackOptionsAuthActivity : FragmentActivity() {
    private lateinit var binding: FallbackOptionsAuthActivityBinding

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
                            log(
                                "onAuthResult: CustomFallbackSelected: ${result.fallback.text} (iconType: ${result.fallback.iconType})"
                            )
                    }
                }

                override fun onAuthAttemptFailed() {
                    log("onAuthAttemptFailed: Try again")
                }
            }
        )

    private val fallbackOptions =
        listOf(
            "None (Default Cancel)",
            "Negative Button",
            "Device Credential",
            "Multiple Fallback Options",
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FallbackOptionsAuthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fallbackOptions)
        binding.fallbackSpinner.adapter = adapter
        binding.fallbackSpinner.setSelection(3) // Default to Multiple Fallback Options

        binding.authenticateButton.setOnClickListener {
            try {
                val selectedOption = binding.fallbackSpinner.selectedItemPosition
                val fallbacks: Array<Biometric.Fallback> =
                    when (selectedOption) {
                        0 -> emptyArray()
                        1 -> arrayOf(Biometric.Fallback.CustomOption("Cancel"))
                        2 -> arrayOf(Biometric.Fallback.DeviceCredential)
                        3 ->
                            arrayOf(
                                Biometric.Fallback.DeviceCredential,
                                Biometric.Fallback.CustomOption(
                                    "Use account password",
                                    Biometric.Fallback.ICON_TYPE_PASSWORD,
                                ),
                                Biometric.Fallback.CustomOption(
                                    "Sign in with a QR code",
                                    Biometric.Fallback.ICON_TYPE_QR_CODE,
                                ),
                            )
                        else -> emptyArray()
                    }

                val request =
                    biometricRequest("Biometric Fallbacks", *fallbacks) {
                        setSubtitle("Testing fallback options")
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

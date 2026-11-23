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

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.integration.testapp.databinding.AdvancedAuthActivityBinding
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors

/** Advanced biometric authentication sample activity (logos and background executors). */
class AdvancedAuthActivity : FragmentActivity() {
    private lateinit var binding: AdvancedAuthActivityBinding
    private val customExecutor = Executors.newSingleThreadExecutor()

    private val callback =
        object : AuthenticationResultCallback {
            override fun onAuthResult(result: AuthenticationResult) {
                runOnUiThread {
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
            }

            override fun onAuthAttemptFailed() {
                runOnUiThread { log("onAuthAttemptFailed: Try again") }
            }
        }

    private val defaultLauncher = registerForAuthenticationResult(callback)
    private val executorLauncher = registerForAuthenticationResult(customExecutor, callback)

    private val strengthOptions = listOf("Class 2 Biometric", "Class 3 Biometric")
    private val logoOptions = listOf("None", "Resource", "Bitmap")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdvancedAuthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.strengthSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, strengthOptions)
        binding.logoSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, logoOptions)
        binding.strengthSpinner.setSelection(1) // Default to Class 3
        binding.logoSpinner.setSelection(1) // Default to Resource

        binding.authenticateButton.setOnClickListener {
            try {
                val isClass2 = binding.strengthSpinner.selectedItemPosition == 0
                val logoPosition = binding.logoSpinner.selectedItemPosition
                val useCustomExecutor = binding.useCustomExecutorCheckbox.isChecked

                val request =
                    biometricRequest("Advanced Auth") {
                        setSubtitle("Testing logo and executor")
                        setMinStrength(
                            if (isClass2) {
                                Biometric.Strength.Class2
                            } else {
                                Biometric.Strength.Class3()
                            }
                        )
                        when (logoPosition) {
                            0 -> {}
                            1 -> {
                                setLogoRes(android.R.drawable.ic_dialog_info)
                                setLogoDescription("Test resource logo")
                            }
                            2 -> {
                                val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.MAGENTA)
                                setLogoBitmap(bmp)
                                setLogoDescription("Test bitmap logo")
                            }
                        }
                    }

                val launcher = if (useCustomExecutor) executorLauncher else defaultLauncher
                launcher.launch(request)
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

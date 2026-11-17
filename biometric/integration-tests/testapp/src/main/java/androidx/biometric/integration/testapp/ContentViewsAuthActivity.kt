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

import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.PromptContentItemBulletedText
import androidx.biometric.integration.testapp.databinding.ContentViewsAuthActivityBinding
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.FragmentActivity

/** Content views sample activity demonstrating plain text, vertical list, and more options. */
class ContentViewsAuthActivity : FragmentActivity() {
    private lateinit var binding: ContentViewsAuthActivityBinding

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

    private val contentOptions =
        listOf(
            "Plain Text",
            "Vertical List (API 35+)",
            "More Options Button (API 35+)",
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ContentViewsAuthActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, contentOptions)
        binding.contentTypeSpinner.adapter = adapter

        binding.authenticateButton.setOnClickListener {
            try {
                val selectedOption = binding.contentTypeSpinner.selectedItemPosition
                val bodyContent: AuthenticationRequest.BodyContent? =
                    when (selectedOption) {
                        0 ->
                            AuthenticationRequest.BodyContent.PlainText(
                                "This is a sample plain text description to display as " +
                                    "body content on the prompt."
                            )
                        1 -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                AuthenticationRequest.BodyContent.VerticalList(
                                    "This is a sample vertical list description to " +
                                        "display as body content on the prompt.",
                                    listOf(
                                        PromptContentItemBulletedText("Item 1"),
                                        PromptContentItemBulletedText("Item 2"),
                                        PromptContentItemBulletedText("Item 3"),
                                        PromptContentItemBulletedText("Item 4"),
                                    ),
                                )
                            } else {
                                log("VerticalList requires API 35+.")
                                null
                            }
                        }
                        2 -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                AuthenticationRequest.BodyContent.ContentViewWithMoreOptionsButton(
                                    "More options button description"
                                )
                            } else {
                                log("ContentViewWithMoreOptionsButton requires API 35+.")
                                null
                            }
                        }
                        else -> null
                    }

                val request =
                    biometricRequest("Content View Auth") {
                        setSubtitle("Testing prompt content views")
                        setContent(bodyContent)
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

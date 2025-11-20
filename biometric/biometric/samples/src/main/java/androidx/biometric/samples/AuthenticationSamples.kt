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
package androidx.biometric.samples

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.Sampled
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationRequest.Biometric
import androidx.biometric.AuthenticationRequest.Companion.biometricRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.PromptContentItemBulletedText
import androidx.biometric.registerForAuthenticationResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

private const val TAG = "AuthenticationSamples"

@Sampled
fun activitySample() {
    class MyActivityForBiometricAuth : FragmentActivity() {
        val requestAuthentication =
            registerForAuthenticationResult(
                object : AuthenticationResultCallback {
                    override fun onAuthResult(result: AuthenticationResult) {
                        when (result) {
                            // Handle successful authentication
                            is AuthenticationResult.Success -> {
                                Log.i(TAG, "onAuthenticationSucceeded with type ${result.authType}")
                            }
                            // Handle authentication error, e.g. negative button click, user
                            // cancellation, etc
                            is AuthenticationResult.Error -> {
                                Log.i(
                                    TAG,
                                    "onAuthenticationError " +
                                        "with error code: ${result.errorCode} " +
                                        "and error string: ${result.errString}",
                                )
                            }
                        }
                    }

                    // Handle intermediate authentication failure, this is optional and
                    // not needed in most cases
                    override fun onAuthFailure() {
                        Log.i(TAG, "onAuthenticationFailed, try again")
                    }
                }
            )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val authRequest =
                biometricRequest(
                    title = "Title",
                    authFallback = Biometric.Fallback.DeviceCredential,
                ) {
                    setSubtitle("Subtitle")
                    setContent(
                        AuthenticationRequest.BodyContent.VerticalList(
                            "Vertical list description",
                            listOf(
                                PromptContentItemBulletedText("test item1"),
                                PromptContentItemBulletedText("test item2"),
                            ),
                        )
                    )
                    setMinStrength(Biometric.Strength.Class3(/*optional: cryptoObject*/ ))
                    setIsConfirmationRequired(true)
                }

            Button(this).setOnClickListener { requestAuthentication.launch(authRequest) }
        }
    }
}

@Sampled
fun fragmentSample() {
    class MyFragmentForCredentialOnlyAuth : Fragment() {
        val requestAuthentication =
            registerForAuthenticationResult { result: AuthenticationResult ->
                when (result) {
                    // Handle successful authentication
                    is AuthenticationResult.Success -> {
                        Log.i(TAG, "onAuthenticationSucceeded with type ${result.authType}")
                    }
                    // Handle authentication error, e.g. negative button click, user
                    // cancellation, etc
                    is AuthenticationResult.Error -> {
                        Log.i(
                            TAG,
                            "onAuthenticationError " +
                                "with error code: ${result.errorCode} " +
                                "and error string: ${result.errString}",
                        )
                    }
                }
            }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val authRequest =
                biometricRequest(
                    title = "Title",
                    authFallback = Biometric.Fallback.DeviceCredential,
                ) {
                    setSubtitle("Subtitle")
                    setContent(
                        AuthenticationRequest.BodyContent.VerticalList(
                            "Vertical list description",
                            listOf(
                                PromptContentItemBulletedText("test item1"),
                                PromptContentItemBulletedText("test item2"),
                            ),
                        )
                    )
                    setMinStrength(Biometric.Strength.Class3(/*optional: cryptoObject*/ ))
                    setIsConfirmationRequired(true)
                }

            Button(context).setOnClickListener { requestAuthentication.launch(authRequest) }
        }
    }
}

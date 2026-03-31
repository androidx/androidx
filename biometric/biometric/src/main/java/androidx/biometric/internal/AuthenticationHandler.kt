/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.biometric.internal

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

private const val TAG = "AuthenticationHandler"

/**
 * This interface abstracts the underlying authentication mechanisms, allowing different
 * implementations (e.g., BiometricPrompt, FingerprintManager, KeyguardManager) to be used
 * interchangeably.
 */
internal interface AuthenticationHandler {

    /**
     * Initiates an authentication flow using the provided prompt [info] and an optional
     * [BiometricPrompt.CryptoObject].
     *
     * This is the primary method for starting a biometric or device credential authentication
     * attempt. The specific UI and underlying mechanism will depend on the implementation of this
     * handle.
     */
    fun authenticate(info: BiometricPrompt.PromptInfo, crypto: BiometricPrompt.CryptoObject?)

    /** Explicitly cancels any ongoing authentication process. */
    fun cancelAuthentication(canceledFrom: CanceledFrom)

    companion object {
        /**
         * Creates and returns a new instance of an [AuthenticationHandler].
         *
         * This factory method returns a delegating authentication handler that determines the
         * appropriate underlying implementation (e.g., using BiometricPrompt, FingerprintManager,
         * or KeyguardManager) based on the device's capabilities and the provided authentication
         * configuration.
         */
        @JvmStatic
        @JvmName("create")
        fun create(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            viewModel: AuthenticationViewModel,
            confirmCredentialActivityLauncher: Runnable,
            clientExecutor: Executor?,
            authenticationCallback: AuthenticationCallback?,
        ): AuthenticationHandler {
            return DefaultAuthenticationHandler(
                context,
                lifecycleOwner,
                viewModel,
                confirmCredentialActivityLauncher,
                clientExecutor ?: PromptExecutor(),
                authenticationCallback ?: DefaultClientAuthenticationCallback(),
            )
        }
    }
}

private class DefaultClientAuthenticationCallback : AuthenticationCallback() {
    override fun onAuthenticationFailed() {
        logClientCallbackNullError()
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        logClientCallbackNullError()
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        logClientCallbackNullError()
    }

    private fun logClientCallbackNullError() {
        Log.e(
            TAG,
            "Callbacks are not re-registered when the caller's activity/fragment is " + "recreated!",
        )
    }
}

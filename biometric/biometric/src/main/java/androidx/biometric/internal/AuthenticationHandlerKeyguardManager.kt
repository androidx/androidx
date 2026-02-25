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
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import kotlinx.coroutines.Runnable

/**
 * An [AuthenticationHandler] implementation that manages authentication flows primarily delegating
 * to an internal [AuthenticationManager] instance.
 *
 * This handler is tailored for scenarios involving device credentials (like PIN/Pattern/Password),
 * using the provided [confirmCredentialActivityLauncher] to initiate the system's confirm device
 * credential screen
 */
internal class AuthenticationHandlerKeyguardManager(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    viewModel: AuthenticationViewModel,
    val confirmCredentialActivityLauncher: Runnable,
    clientExecutor: Executor,
    clientAuthenticationCallback: AuthenticationCallback,
) : AuthenticationHandler {
    private val authenticationManager =
        AuthenticationManager(
            context,
            lifecycleOwner,
            viewModel,
            confirmCredentialActivityLauncher,
            clientExecutor,
            clientAuthenticationCallback,
        )

    init {
        authenticationManager.initialize()
    }

    override fun authenticate(
        info: BiometricPrompt.PromptInfo,
        crypto: BiometricPrompt.CryptoObject?,
    ) {
        authenticationManager.authenticate(info, crypto) { confirmCredentialActivityLauncher.run() }
    }

    override fun cancelAuthentication(canceledFrom: CanceledFrom) {
        authenticationManager.cancelAuthentication(canceledFrom)
    }
}

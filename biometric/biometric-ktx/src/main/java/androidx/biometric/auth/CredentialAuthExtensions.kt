/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.biometric.auth

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Builds a [CredentialAuthPrompt] hosted on the current [FragmentActivity], which configures a
 * [BiometricPrompt] for authentication with device credential modalities (device PIN, pattern, or
 * password) and begins authentication.
 *
 * @param crypto A crypto object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param executor The executor that will run authentication callback methods. If null, callback
 * methods will be executed on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @param description The description to be displayed on the prompt, null by default.
 * @return [AuthPrompt] wrapper that can be used for cancellation and dismissal of the
 * biometric prompt using [AuthPrompt]#cancelAuthentication()
 */
public fun FragmentActivity.startCredentialAuthentication(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence? = null,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startCredentialAuthenticationInternal(
        AuthPromptHost(this),
        crypto,
        title,
        description,
        executor,
        callback
    )
}

/**
 * Builds a [CredentialAuthPrompt] hosted on the current [Fragment], which configures a
 * [BiometricPrompt] for authentication with device credential modalities (device PIN, pattern, or
 * password) and begins authentication.
 *
 * @param crypto A crypto object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param executor The executor that will run authentication callback methods. If null, callback
 * methods will be executed on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @param description The description to be displayed on the prompt, null by default.
 * @return [AuthPrompt] wrapper that can be used for cancellation and dismissal of the
 * biometric prompt using [AuthPrompt]#cancelAuthentication()
 */
public fun Fragment.startCredentialAuthentication(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence? = null,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startCredentialAuthenticationInternal(
        AuthPromptHost(this),
        crypto,
        title,
        description,
        executor,
        callback
    )
}

/**
 * Helper function for shared logic in [Fragment.startCredentialAuthentication] and
 * [FragmentActivity.startCredentialAuthentication] for building the [CredentialAuthPrompt],
 * starting authentication, and returning the AuthPrompt wrapper for cancellation and dismissal
 * of the biometric prompt using [AuthPrompt]#cancelAuthentication()
 */
private fun startCredentialAuthenticationInternal(
    authPromptHost: AuthPromptHost,
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence? = null,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    val credentialAuthBuilder =
        if (executor != null) {
            CredentialAuthPrompt.Builder(
                authPromptHost, title, executor, callback
            )
        } else {
            CredentialAuthPrompt.Builder(
                authPromptHost, title, callback
            )
        }

    return credentialAuthBuilder.apply {
        description?.let { setDescription(it) }
        crypto?.let { setCrypto(it) }
    }.build().startAuthentication()
}
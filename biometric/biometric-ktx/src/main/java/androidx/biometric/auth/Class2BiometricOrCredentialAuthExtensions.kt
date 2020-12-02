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
 * Builds a [Class2BiometricOrCredentialAuthPrompt] hosted on the current [FragmentActivity], which
 * configures a [BiometricPrompt] for authentication with Class 2 biometric modalities (fingerprint,
 * iris, face, etc) or device credential modalities (device PIN, pattern, or password) and begins
 * authentication.
 *
 * Class 2 (formerly known as Weak) refers to the strength of the biometric sensor, as specified
 * in the Android 11 CDD. Class 2 authentication can be used for applications that don't require
 * cryptographic operations.
 *
 * @param title The title to be displayed on the prompt.
 * @param executor The executor that will run authentication callback methods. If null, callback
 * methods will be executed on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @param subtitle The subtitle to be displayed on the prompt, null by default.
 * @param description The description to be displayed on the prompt, null by default.
 * @param confirmationRequired Whether explicit user confirmation is required after a passive
 *                             biometric, true by default.
 * @return [AuthPrompt] wrapper that can be used for cancellation and dismissal of the
 * biometric prompt using [AuthPrompt]#cancelAuthentication()
 */
public fun FragmentActivity.startClass2BiometricOrCredentialAuthentication(
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startClass2BiometricOrCredentialAuthenticationInternal(
        AuthPromptHost(this),
        title,
        subtitle,
        description,
        confirmationRequired,
        executor,
        callback
    )
}

/**
 * Builds a [Class2BiometricOrCredentialAuthPrompt] hosted on the current [Fragment], which
 * configures a [BiometricPrompt] for authentication with Class 2 biometric modalities (fingerprint,
 * iris, face, etc) or device credential modalities (device PIN, pattern, or password) and begins
 * authentication.
 *
 * Class 2 (formerly known as Weak) refers to the strength of the biometric sensor, as specified
 * in the Android 11 CDD. Class 2 authentication can be used for applications that don't require
 * cryptographic operations.
 *
 * @param title The title to be displayed on the prompt.
 * @param executor The executor that will run authentication callback methods. If null, callback
 * methods will be executed on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @param subtitle The subtitle to be displayed on the prompt, null by default.
 * @param description The description to be displayed on the prompt, null by default.
 * @param confirmationRequired Whether explicit user confirmation is required after a passive
 *                             biometric, true by default.
 * @return [AuthPrompt] wrapper that can be used for cancellation and dismissal of the
 * biometric prompt using [AuthPrompt]#cancelAuthentication()
 */
public fun Fragment.startClass2BiometricOrCredentialAuthentication(
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startClass2BiometricOrCredentialAuthenticationInternal(
        AuthPromptHost(this),
        title,
        subtitle,
        description,
        confirmationRequired,
        executor,
        callback
    )
}

/**
 * Helper function for shared logic in [Fragment.startClass2BiometricOrCredentialAuthentication] and
 * [FragmentActivity.startClass2BiometricOrCredentialAuthentication] for building the
 * [Class2BiometricOrCredentialAuthPrompt], starting authentication, and returning the AuthPrompt
 * wrapper for cancellation and dismissal of the biometric prompt using
 * [AuthPrompt]#cancelAuthentication()
 */
private fun startClass2BiometricOrCredentialAuthenticationInternal(
    authPromptHost: AuthPromptHost,
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    val class2BiometricOrCredentialAuthBuilder =
        if (executor != null) {
            Class2BiometricOrCredentialAuthPrompt.Builder(
                authPromptHost, title, executor, callback
            )
        } else {
            Class2BiometricOrCredentialAuthPrompt.Builder(
                authPromptHost, title, callback
            )
        }

    return class2BiometricOrCredentialAuthBuilder.apply {
        subtitle?.let { setSubtitle(it) }
        description?.let { setDescription(it) }
        setConfirmationRequired(confirmationRequired)
    }.build().startAuthentication()
}
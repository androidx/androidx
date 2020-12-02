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
 * Builds a [Class3BiometricAuthPrompt] hosted on the current [FragmentActivity], which configures a
 * [BiometricPrompt] for authentication with Class 3 biometric modalities
 * (fingerprint, iris, face, etc) and begins authentication.
 *
 * Class 3 (formerly known as Strong) refers to the strength of the biometric sensor, as specified
 * in the Android 11 CDD. Class 3 authentication can be used for applications that use
 * cryptographic operations.
 *
 * @param crypto A crypto object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param negativeButtonText The label to be used for the negative button on the prompt.
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
public fun FragmentActivity.startClass3BiometricAuthentication(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    negativeButtonText: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startClass3BiometricAuthenticationInternal(
        AuthPromptHost(this),
        crypto,
        title,
        negativeButtonText,
        subtitle,
        description,
        confirmationRequired,
        executor,
        callback
    )
}

/**
 * Builds a [Class3BiometricAuthPrompt] hosted on the current [Fragment], which configures a
 * [BiometricPrompt] for authentication with Class 3 biometric modalities (fingerprint, iris, face,
 * etc) and begins authentication.
 *
 * Class 3 (formerly known as Strong) refers to the strength of the biometric sensor, as specified
 * in the Android 11 CDD. Class 3 authentication can be used for applications that use cryptographic
 * operations.
 *
 * @param crypto A crypto object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param negativeButtonText The label to be used for the negative button on the prompt.
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
public fun Fragment.startClass3BiometricAuthentication(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    negativeButtonText: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startClass3BiometricAuthenticationInternal(
        AuthPromptHost(this),
        crypto,
        title,
        negativeButtonText,
        subtitle,
        description,
        confirmationRequired,
        executor,
        callback
    )
}

/**
 * Helper function for shared logic in [Fragment.startClass3BiometricAuthentication] and
 * [FragmentActivity.startClass3BiometricAuthentication] for building the
 * [Class3BiometricAuthPrompt], starting authentication, and returning the AuthPrompt wrapper for
 * cancellation and dismissal of the biometric prompt using [AuthPrompt]#cancelAuthentication()
 */
private fun startClass3BiometricAuthenticationInternal(
    authPromptHost: AuthPromptHost,
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    negativeButtonText: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    val class3BiometricAuthBuilder =
        if (executor != null) {
            Class3BiometricAuthPrompt.Builder(
                authPromptHost, title, negativeButtonText, executor, callback
            )
        } else {
            Class3BiometricAuthPrompt.Builder(
                authPromptHost, title, negativeButtonText, callback
            )
        }

    return class3BiometricAuthBuilder.apply {
        subtitle?.let { setSubtitle(it) }
        description?.let { setDescription(it) }
        setConfirmationRequired(confirmationRequired)
        crypto?.let { setCrypto(it) }
    }.build().startAuthentication()
}
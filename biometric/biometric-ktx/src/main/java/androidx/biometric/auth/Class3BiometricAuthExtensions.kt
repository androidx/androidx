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
 * Prompts the user to authenticate with a **Class 3** biometric (e.g. fingerprint, face, or iris).
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param negativeButtonText The label for the negative button on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see Class3BiometricAuthPrompt
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
 * Prompts the user to authenticate with a **Class 3** biometric (e.g. fingerprint, face, or iris).
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param negativeButtonText The label for the negative button on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see Class3BiometricAuthPrompt
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
 * Creates a [Class3BiometricAuthPrompt] with the given parameters and starts authentication.
 */
private fun startClass3BiometricAuthenticationInternal(
    host: AuthPromptHost,
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    negativeButtonText: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    val prompt = Class3BiometricAuthPrompt.Builder(title, negativeButtonText).apply {
        subtitle?.let { setSubtitle(it) }
        description?.let { setDescription(it) }
        setConfirmationRequired(confirmationRequired)
    }.build()

    return if (executor == null) {
        prompt.startAuthentication(host, crypto, callback)
    } else {
        prompt.startAuthentication(host, crypto, executor, callback)
    }
}
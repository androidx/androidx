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

import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor

/**
 * Shows an authentication prompt to the user.
 *
 * @param host A wrapper for the component that will host the prompt.
 *
 * @return [AuthenticationResult] for a successful authentication.
 *
 * @throws AuthPromptErrorException  when an unrecoverable error has been encountered and
 * authentication has stopped.
 * @throws AuthPromptFailureException when an authentication attempt by the user has been rejected.
 *
 * @see Class2BiometricOrCredentialAuthPrompt.authenticate(
 *     AuthPromptHost,
 *     AuthPromptCallback
 * )
 *
 * @sample androidx.biometric.samples.auth.class2BiometricOrCredentialAuth
 */
public suspend fun Class2BiometricOrCredentialAuthPrompt.authenticate(
    host: AuthPromptHost,
): AuthenticationResult {
    return suspendCancellableCoroutine { continuation ->
        val authPrompt = startAuthentication(
            host,
            Runnable::run,
            CoroutineAuthPromptCallback(continuation)
        )

        continuation.invokeOnCancellation {
            authPrompt.cancelAuthentication()
        }
    }
}

/**
 * Prompts the user to authenticate with a **Class 2** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * Note that **Class 3** biometrics are guaranteed to meet the requirements for **Class 2** and thus
 * will also be accepted.
 *
 * @param title The title to be displayed on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see Class2BiometricOrCredentialAuthPrompt
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
 * Prompts the user to authenticate with a **Class 2** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * Note that **Class 3** biometrics are guaranteed to meet the requirements for **Class 2** and thus
 * will also be accepted.
 *
 * @param title The title to be displayed on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 *
 * @return [AuthenticationResult] for a successful authentication.
 *
 * @throws AuthPromptErrorException  when an unrecoverable error has been encountered and
 * authentication has stopped.
 * @throws AuthPromptFailureException when an authentication attempt by the user has been rejected.
 *
 * @see Class2BiometricOrCredentialAuthPrompt
 */
public suspend fun FragmentActivity.authenticateWithClass2BiometricsOrCredentials(
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
): AuthenticationResult {
    val authPrompt = buildClass2BiometricOrCredentialAuthPrompt(
        title,
        subtitle,
        description,
        confirmationRequired,
    )

    return authPrompt.authenticate(AuthPromptHost(this))
}

/**
 * Prompts the user to authenticate with a **Class 2** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * Note that **Class 3** biometrics are guaranteed to meet the requirements for **Class 2** and thus
 * will also be accepted.
 *
 * @param title The title to be displayed on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see Class2BiometricOrCredentialAuthPrompt
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
 * Prompts the user to authenticate with a **Class 2** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * Note that **Class 3** biometrics are guaranteed to meet the requirements for **Class 2** and thus
 * will also be accepted.
 *
 * @param title The title to be displayed on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @throws AuthPromptErrorException  when an unrecoverable error has been encountered and
 * authentication has stopped.
 * @throws AuthPromptFailureException when an authentication attempt by the user has been rejected.
 *
 * @see Class2BiometricOrCredentialAuthPrompt
 */
public suspend fun Fragment.authenticateWithClass2BiometricsOrCredentials(
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
): AuthenticationResult {
    val authPrompt = buildClass2BiometricOrCredentialAuthPrompt(
        title,
        subtitle,
        description,
        confirmationRequired,
    )

    return authPrompt.authenticate(AuthPromptHost(this))
}

/**
 * Creates a [Class2BiometricOrCredentialAuthPrompt] with the given parameters and starts
 * authentication.
 */
private fun startClass2BiometricOrCredentialAuthenticationInternal(
    host: AuthPromptHost,
    title: CharSequence,
    subtitle: CharSequence?,
    description: CharSequence?,
    confirmationRequired: Boolean,
    executor: Executor?,
    callback: AuthPromptCallback
): AuthPrompt {
    val prompt = buildClass2BiometricOrCredentialAuthPrompt(
        title,
        subtitle,
        description,
        confirmationRequired,
    )

    return if (executor == null) {
        prompt.startAuthentication(host, callback)
    } else {
        prompt.startAuthentication(host, executor, callback)
    }
}

/**
 * Creates a [Class2BiometricOrCredentialAuthPrompt] with the given parameters.
 */
private fun buildClass2BiometricOrCredentialAuthPrompt(
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
): Class2BiometricOrCredentialAuthPrompt = Class2BiometricOrCredentialAuthPrompt.Builder(title)
    .apply {
        subtitle?.let { setSubtitle(it) }
        description?.let { setDescription(it) }
        setConfirmationRequired(confirmationRequired)
    }
    .build()

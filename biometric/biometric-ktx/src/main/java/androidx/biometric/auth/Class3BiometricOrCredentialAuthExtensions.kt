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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor

/**
 * Shows an authentication prompt to the user.
 *
 * @param host A wrapper for the component that will host the prompt.
 * @param crypto A cryptographic object to be associated with this authentication.
 *
 * @return [AuthenticationResult] for a successful authentication.
 *
 * @throws AuthPromptErrorException  when an unrecoverable error has been encountered and
 * authentication has stopped.
 * @throws AuthPromptFailureException when an authentication attempt by the user has been rejected.
 *
 * @see Class3BiometricOrCredentialAuthPrompt.authenticate(
 *     AuthPromptHost,
 *     AuthPromptCallback
 * )
 *
 * @sample androidx.biometric.samples.auth.class3BiometricOrCredentialAuth
 */
@RequiresApi(Build.VERSION_CODES.R)
public suspend fun Class3BiometricOrCredentialAuthPrompt.authenticate(
    host: AuthPromptHost,
    crypto: CryptoObject?,
): AuthenticationResult {
    return suspendCancellableCoroutine { continuation ->
        val authPrompt = startAuthentication(
            host,
            crypto,
            Runnable::run,
            CoroutineAuthPromptCallback(continuation)
        )

        continuation.invokeOnCancellation {
            authPrompt.cancelAuthentication()
        }
    }
}

/**
 * Prompts the user to authenticate with a **Class 3** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see Class3BiometricOrCredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public fun FragmentActivity.startClass3BiometricOrCredentialAuthentication(
    crypto: CryptoObject?,
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startClass3BiometricOrCredentialAuthenticationInternal(
        AuthPromptHost(this),
        crypto,
        title,
        subtitle,
        description,
        confirmationRequired,
        executor,
        callback
    )
}

/**
 * Prompts the user to authenticate with a **Class 3** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
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
 * @see Class3BiometricOrCredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public suspend fun FragmentActivity.authenticateWithClass3BiometricsOrCredentials(
    crypto: CryptoObject?,
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
): AuthenticationResult {
    val authPrompt = buildClass3BiometricOrCredentialAuthPrompt(
        title,
        subtitle,
        description,
        confirmationRequired
    )

    return authPrompt.authenticate(AuthPromptHost(this), crypto)
}

/**
 * Prompts the user to authenticate with a **Class 3** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param subtitle An optional subtitle to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param confirmationRequired Whether user confirmation should be required for passive biometrics.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see Class3BiometricOrCredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public fun Fragment.startClass3BiometricOrCredentialAuthentication(
    crypto: CryptoObject?,
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    return startClass3BiometricOrCredentialAuthenticationInternal(
        AuthPromptHost(this),
        crypto,
        title,
        subtitle,
        description,
        confirmationRequired,
        executor,
        callback
    )
}

/**
 * Prompts the user to authenticate with a **Class 3** biometric (e.g. fingerprint, face, or iris)
 * or the screen lock credential (i.e. PIN, pattern, or password) for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
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
 * @see Class3BiometricOrCredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public suspend fun Fragment.authenticateWithClass3BiometricsOrCredentials(
    crypto: CryptoObject?,
    title: CharSequence,
    subtitle: CharSequence? = null,
    description: CharSequence? = null,
    confirmationRequired: Boolean = true,
): AuthenticationResult {
    val authPrompt = buildClass3BiometricOrCredentialAuthPrompt(
        title,
        subtitle,
        description,
        confirmationRequired,
    )

    return authPrompt.authenticate(AuthPromptHost(this), crypto)
}

/**
 * Creates a [Class3BiometricOrCredentialAuthPrompt] with the given parameters and starts
 * authentication.
 */
@RequiresApi(Build.VERSION_CODES.R)
private fun startClass3BiometricOrCredentialAuthenticationInternal(
    host: AuthPromptHost,
    crypto: CryptoObject?,
    title: CharSequence,
    subtitle: CharSequence?,
    description: CharSequence?,
    confirmationRequired: Boolean,
    executor: Executor?,
    callback: AuthPromptCallback
): AuthPrompt {
    val prompt = buildClass3BiometricOrCredentialAuthPrompt(
        title,
        subtitle,
        description,
        confirmationRequired
    )

    return if (executor == null) {
        prompt.startAuthentication(host, crypto, callback)
    } else {
        prompt.startAuthentication(host, crypto, executor, callback)
    }
}

/**
 * Creates a [Class3BiometricOrCredentialAuthPrompt] with the given parameters.
 */
@RequiresApi(Build.VERSION_CODES.R)
private fun buildClass3BiometricOrCredentialAuthPrompt(
    title: CharSequence,
    subtitle: CharSequence?,
    description: CharSequence?,
    confirmationRequired: Boolean,
): Class3BiometricOrCredentialAuthPrompt = Class3BiometricOrCredentialAuthPrompt.Builder(title)
    .apply {
        subtitle?.let { setSubtitle(it) }
        description?.let { setDescription(it) }
        setConfirmationRequired(confirmationRequired)
    }
    .build()

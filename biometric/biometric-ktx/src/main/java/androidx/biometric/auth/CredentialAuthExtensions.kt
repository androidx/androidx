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
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine

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
 * @see CredentialAuthPrompt.authenticate(
 *     AuthPromptHost host,
 *     BiometricPrompt.CryptoObject,
 *     AuthPromptCallback
 * )
 *
 * @sample androidx.biometric.samples.auth.credentialAuth
 */
@RequiresApi(Build.VERSION_CODES.R)
public suspend fun CredentialAuthPrompt.authenticate(
    host: AuthPromptHost,
    crypto: BiometricPrompt.CryptoObject?,
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
 * Prompts the user to authenticate with the screen lock credential (i.e. PIN, pattern, or password)
 * for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see CredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
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
 * Prompts the user to authenticate with the screen lock credential (i.e. PIN, pattern, or password)
 * for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @return [AuthenticationResult] for a successful authentication.
 *
 * @throws AuthPromptErrorException  when an unrecoverable error has been encountered and
 * authentication has stopped.
 * @throws AuthPromptFailureException when an authentication attempt by the user has been rejected.
 *
 * @see CredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public suspend fun FragmentActivity.authenticateWithCredentials(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence? = null
): AuthenticationResult {
    val authPrompt = buildCredentialAuthPrompt(title, description)
    return authPrompt.authenticate(AuthPromptHost(this), crypto)
}

/**
 * Prompts the user to authenticate with the screen lock credential (i.e. PIN, pattern, or password)
 * for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @param executor An executor for [callback] methods. If `null`, these will run on the main thread.
 * @param callback The object that will receive and process authentication events.
 * @return An [AuthPrompt] handle to the shown prompt.
 *
 * @see CredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
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
 * Prompts the user to authenticate with the screen lock credential (i.e. PIN, pattern, or password)
 * for the device.
 *
 * @param crypto A cryptographic object to be associated with this authentication.
 * @param title The title to be displayed on the prompt.
 * @param description An optional description to be displayed on the prompt.
 * @return [AuthenticationResult] for a successful authentication.
 *
 * @throws AuthPromptErrorException  when an unrecoverable error has been encountered and
 * authentication has stopped.
 * @throws AuthPromptFailureException when an authentication attempt by the user has been rejected.
 *
 * @see CredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public suspend fun Fragment.authenticateWithCredentials(
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence? = null
): AuthenticationResult {
    val authPrompt = buildCredentialAuthPrompt(title, description)
    return authPrompt.authenticate(AuthPromptHost(this), crypto)
}

/**
 * Creates a [CredentialAuthPrompt] with the given parameters and starts authentication.
 */
@RequiresApi(Build.VERSION_CODES.R)
private fun startCredentialAuthenticationInternal(
    host: AuthPromptHost,
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence?,
    executor: Executor?,
    callback: AuthPromptCallback
): AuthPrompt {
    val prompt = buildCredentialAuthPrompt(title, description)
    return if (executor == null) {
        prompt.startAuthentication(host, crypto, callback)
    } else {
        prompt.startAuthentication(host, crypto, executor, callback)
    }
}

/**
 * Creates a [CredentialAuthPrompt] with the given parameters.
 */
@RequiresApi(Build.VERSION_CODES.R)
private fun buildCredentialAuthPrompt(
    title: CharSequence,
    description: CharSequence?
): CredentialAuthPrompt = CredentialAuthPrompt.Builder(title)
    .apply { description?.let { setDescription(it) } }
    .build()

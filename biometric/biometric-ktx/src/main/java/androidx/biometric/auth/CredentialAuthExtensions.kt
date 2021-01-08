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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

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
 * Creates a [CredentialAuthPrompt] with the given parameters and starts authentication.
 */
@RequiresApi(Build.VERSION_CODES.R)
private fun startCredentialAuthenticationInternal(
    host: AuthPromptHost,
    crypto: BiometricPrompt.CryptoObject?,
    title: CharSequence,
    description: CharSequence? = null,
    executor: Executor? = null,
    callback: AuthPromptCallback
): AuthPrompt {
    val prompt = CredentialAuthPrompt.Builder(title).apply {
        description?.let { setDescription(it) }
    }.build()

    return if (executor == null) {
        prompt.startAuthentication(host, crypto, callback)
    } else {
        prompt.startAuthentication(host, crypto, executor, callback)
    }
}
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
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resumeWithException

/**
 * Implementation of [AuthPromptCallback] used to transform callback results for coroutine APIs.
 */
internal class CoroutineAuthPromptCallback(
    private val continuation: CancellableContinuation<AuthenticationResult>
) : AuthPromptCallback() {
    override fun onAuthenticationError(
        activity: FragmentActivity?,
        errorCode: Int,
        errString: CharSequence
    ) {
        continuation.resumeWithException(AuthPromptErrorException(errorCode, errString))
    }

    override fun onAuthenticationSucceeded(
        activity: FragmentActivity?,
        result: AuthenticationResult
    ) {
        continuation.resumeWith(Result.success(result))
    }

    override fun onAuthenticationFailed(activity: FragmentActivity?) {
        continuation.resumeWithException(AuthPromptFailureException())
    }
}

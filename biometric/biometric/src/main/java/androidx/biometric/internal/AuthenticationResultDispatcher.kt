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

import android.app.KeyguardManager
import android.content.Context
import android.util.Log
import androidx.biometric.AuthenticationRequest
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.R
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.ErrorUtils
import java.util.concurrent.Executor

private const val TAG = "AuthResultDispatcher"

/**
 * An abstract class for dispatching biometric authentication results to a client.
 *
 * This class serves as a central coordinator for the authentication flow, translating the raw
 * results from the underlying authentication system (e.g., success, failure, error) and dispatching
 * them to the client's provided callback. It also manages the state of the authentication prompt
 * and handles various edge cases.
 *
 * Subclasses can provide specific behavior for different authentication prompts by overriding
 * methods.
 *
 * @param context The application context.
 * @param viewModel The [AuthenticationViewModel] to manage the state of the authentication prompt.
 * @param clientExecutor The [Executor] on which to run the client's callback methods.
 * @param clientAuthenticationCallback The client's original [AuthenticationCallback] to be invoked.
 * @param dismiss A lambda function to dismiss the authentication prompt UI.
 */
internal abstract class AuthenticationResultDispatcher(
    val context: Context,
    val viewModel: AuthenticationViewModel,
    val clientExecutor: Executor,
    val clientAuthenticationCallback: AuthenticationCallback,
    val confirmCredentialActivityLauncher: Runnable,
    val dismiss: () -> Unit,
) {
    /**
     * Handles an authentication error by converting it to a known error code and dispatching it to
     * the client.
     *
     * Subclasses can override this to provide custom error handling logic.
     *
     * @param errorCode The raw error code from the authentication system.
     * @param errorMessage An optional raw error message from the authentication system.
     */
    open fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        // Ensure we're only sending publicly defined errors.
        val knownErrorCode = ErrorUtils.toKnownErrorCodeForAuthenticate(errorCode)
        val errorString = errorMessage ?: context.getString(R.string.default_error_msg)

        sendErrorAndDismiss(knownErrorCode, errorString)
    }

    /**
     * Calls [KeyguardManager.createConfirmDeviceCredentialIntent] to show the credential view
     * fallback.
     *
     * Subclasses should override this method to launch the appropriate confirm credential activity.
     */
    open fun showKMAsFallback() {
        confirmCredentialActivityLauncher.run()
    }

    /**
     * Handles a successful authentication result and dispatches it to the client.
     *
     * @param result The successful [BiometricPrompt.AuthenticationResult].
     */
    fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        sendSuccessAndDismiss(result)
    }

    /** Handles an intermediate authentication failure event and dispatches it to the client. */
    open fun onAuthenticationFailed() {
        sendFailureToClient()
    }

    /**
     * Sends an unrecoverable error result with [errorCode] and [errorString] to the client and
     * dismisses the prompt.
     *
     * @param errorCode The error code to send.
     * @param errorString The error message to send.
     */
    fun sendErrorAndDismiss(errorCode: Int, errorString: CharSequence) {
        sendErrorToClient(errorCode, errorString)
        dismiss()
    }

    /** Sends an unrecoverable error result to the client callback. */
    fun sendErrorToClient(errorCode: Int, errorString: CharSequence) {
        if (!viewModel.isAwaitingResult) {
            Log.w(TAG, "Error not sent to client. Client is not awaiting a result.")
            return
        }

        viewModel.isAwaitingResult = false
        clientExecutor.execute {
            clientAuthenticationCallback.onAuthenticationError(errorCode, errorString)
        }
    }

    /** Sends a successful authentication result to the client and dismisses the prompt. */
    private fun sendSuccessAndDismiss(result: BiometricPrompt.AuthenticationResult) {
        sendSuccessToClient(result)
        dismiss()
    }

    /** Sends a successful authentication result to the client callback. */
    private fun sendSuccessToClient(result: BiometricPrompt.AuthenticationResult) {
        if (!viewModel.isAwaitingResult) {
            Log.w(TAG, "Success not sent to client. Client is not awaiting a result.")
            return
        }

        viewModel.isAwaitingResult = false
        clientExecutor.execute { clientAuthenticationCallback.onAuthenticationSucceeded(result) }
    }

    /** Sends an authentication failure event to the client callback. */
    fun sendFailureToClient() {
        if (!viewModel.isAwaitingResult) {
            Log.w(TAG, "Failure not sent to client. Client is not awaiting a result.")
            return
        }

        clientExecutor.execute { clientAuthenticationCallback.onAuthenticationFailed() }
    }

    /**
     * Sends an unrecoverable fallback option result with [fallback] to the client and dismisses the
     * prompt.
     *
     * @param fallback The selected fallback option
     */
    fun sendFallbackOptionAndDismiss(
        fallback: AuthenticationRequest.Biometric.Fallback.CustomOption
    ) {
        sendFallbackOption(fallback)
        dismiss()
    }

    /** Sends an unrecoverable fallback option result to the client. */
    fun sendFallbackOption(fallback: AuthenticationRequest.Biometric.Fallback.CustomOption) {
        if (!viewModel.isAwaitingResult) {
            Log.w(TAG, "Error not sent to client. Client is not awaiting a result.")
            return
        }

        viewModel.isAwaitingResult = false
        clientExecutor.execute { clientAuthenticationCallback.onFallbackSelected(fallback) }
    }
}

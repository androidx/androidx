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

import android.content.Context
import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.ui.FingerprintDialogActivity
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.ErrorUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.Executor
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * An authentication handler that uses the legacy `FingerprintManager` API to display a fingerprint
 * prompt.
 *
 * This handler is responsible for launching [FingerprintDialogActivity] to show the UI, and for
 * handling the results of the authentication.
 *
 * @param context The application context.
 * @param lifecycleOwner The lifecycle owner for observing lifecycle events.
 * @param viewModel The [AuthenticationViewModel] that holds the state for the ongoing
 *   authentication.
 * @param confirmCredentialActivityLauncher A [Runnable] to launch the confirm credential activity
 *   as a fallback.
 * @param clientExecutor The executor for posting results to the client callback.
 * @param clientAuthenticationCallback The client-provided callback for receiving authentication
 *   events.
 */
internal class AuthenticationHandlerFingerprintManager(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val viewModel: AuthenticationViewModel,
    val confirmCredentialActivityLauncher: Runnable,
    val clientExecutor: Executor,
    clientAuthenticationCallback: AuthenticationCallback,
) : AuthenticationHandler {
    private val authenticationManager =
        AuthenticationManager(
            context,
            lifecycleOwner,
            viewModel,
            confirmCredentialActivityLauncher,
            clientExecutor,
            clientAuthenticationCallback,
        )

    private val resultDispatcher =
        object :
            AuthenticationResultDispatcher(
                context,
                viewModel,
                clientExecutor,
                clientAuthenticationCallback,
                confirmCredentialActivityLauncher,
                { dismiss() },
            ) {
            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                this@AuthenticationHandlerFingerprintManager.onAuthenticationError(
                    errorCode,
                    errorMessage,
                )
            }

            override fun showKMAsFallback() {
                this@AuthenticationHandlerFingerprintManager.showKMAsFallback()
            }
        }

    private val uiStateObserver =
        object : AuthenticationUiStateObserver() {
            override fun createObserverJob(): Job =
                lifecycleOwner.lifecycleScope.launch {
                    viewModel.isNegativeButtonPressPending.collect {
                        authenticationManager.isNegativeButtonPressPendingObserver()
                    }
                }
        }

    init {
        authenticationManager.initialize(resultDispatcher, uiStateObserver)
    }

    override fun authenticate(
        info: BiometricPrompt.PromptInfo,
        crypto: BiometricPrompt.CryptoObject?,
    ) {
        authenticationManager.authenticate(info, crypto) { showAuthentication() }
    }

    override fun cancelAuthentication(canceledFrom: CanceledFrom) {
        authenticationManager.cancelAuthentication(canceledFrom)
    }

    /** Shows the fingerprint dialog UI to the user and begins authentication. */
    private fun showAuthentication() {
        val intent = Intent(context, FingerprintDialogActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Dismisses any visible authentication UI. */
    private fun dismiss() {
        authenticationManager.dismiss()
    }

    /** Shows the keyguard manager as a fallback for authentication. */
    private fun showKMAsFallback() {
        confirmCredentialActivityLauncher.run()
    }

    private fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        // Ensure we're only sending publicly defined errors.
        val knownErrorCode = ErrorUtils.toKnownErrorCodeForAuthenticate(errorCode)
        if (
            ErrorUtils.isLockoutError(knownErrorCode) &&
                context.isManagingDeviceCredentialButton(viewModel.allowedAuthenticators)
        ) {
            showKMAsFallback()
            return
        }

        // Avoid passing a null error string to the client callback.
        val errorString =
            errorMessage ?: ErrorUtils.getFingerprintErrorString(context, knownErrorCode)

        if (knownErrorCode == BiometricPrompt.ERROR_CANCELED) {
            // User-initiated cancellation errors should already be handled.
            if (viewModel.canceledFrom.isNotUserInitiated()) {
                resultDispatcher.sendErrorToClient(knownErrorCode, errorString)
            }

            dismiss()
        } else {
            resultDispatcher.sendErrorAndDismiss(knownErrorCode, errorString)
        }
    }
}

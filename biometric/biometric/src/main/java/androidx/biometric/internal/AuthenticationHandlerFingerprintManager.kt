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

import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.ui.FingerprintDialogController
import androidx.biometric.utils.ErrorUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * An authentication handler that uses the legacy `FingerprintManager` API to display a fingerprint
 * prompt.
 *
 * This handler is responsible for using [FingerprintDialogController] to show the UI in the host
 * window, and for handling the results of the authentication.
 */
internal class AuthenticationHandlerFingerprintManager(
    private val authenticationManager: AuthenticationManager
) : AuthenticationHandler {
    val context
        get() = authenticationManager.context

    val lifecycleOwner
        get() = authenticationManager.lifecycleOwner

    val viewModel
        get() = authenticationManager.viewModel

    val confirmCredentialActivityLauncher
        get() = authenticationManager.confirmCredentialActivityLauncher

    private val fingerprintDialogController =
        FingerprintDialogController(context, lifecycleOwner, viewModel)

    private val resultDispatcher =
        object :
            AuthenticationResultDispatcher(
                context,
                viewModel,
                authenticationManager.clientExecutor,
                authenticationManager.clientAuthenticationCallback,
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
                    if (viewModel.isPromptShowing && !viewModel.isConfirmingDeviceCredential) {
                        fingerprintDialogController.showDialogForReconnect()
                    }
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
        fingerprintDialogController.dismiss()
    }

    /** Shows the fingerprint dialog UI to the user and begins authentication. */
    private fun showAuthentication() {
        fingerprintDialogController.showAuthentication()
    }

    /** Dismisses any visible authentication UI. */
    private fun dismiss() {
        authenticationManager.dismiss()
    }

    /** Shows the keyguard manager as a fallback for authentication. */
    private fun showKMAsFallback() {
        fingerprintDialogController.dismiss()
        // Launch the confirm credential screen before canceling the fingerprint sensor. Launching
        // sets viewModel.isConfirmingDeviceCredential, which is what suppresses the ERROR_CANCELED
        // that the framework delivers in response to the cancellation below. Doing it in this
        // order keeps that suppression independent of callback timing.
        confirmCredentialActivityLauncher.run()
        viewModel.cancellationSignalProvider.cancel()
    }

    private fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        // Ensure we're only sending publicly defined errors.
        val knownErrorCode = ErrorUtils.toKnownErrorCodeForAuthenticate(errorCode)
        if (ErrorUtils.isLockoutError(knownErrorCode) && viewModel.isOverriddenDeviceCredential) {
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

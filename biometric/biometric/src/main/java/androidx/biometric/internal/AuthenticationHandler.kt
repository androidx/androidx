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
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.DeviceUtils
import androidx.biometric.utils.KeyguardUtils
import androidx.biometric.utils.PackageUtils
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

private const val TAG = "AuthenticationHandler"

/**
 * This interface abstracts the underlying authentication mechanisms, allowing different
 * implementations (e.g., BiometricPrompt, FingerprintManager, KeyguardManager) to be used
 * interchangeably.
 */
internal interface AuthenticationHandler {

    /**
     * Initiates an authentication flow using the provided prompt [info] and an optional
     * [BiometricPrompt.CryptoObject].
     *
     * This is the primary method for starting a biometric or device credential authentication
     * attempt. The specific UI and underlying mechanism will depend on the implementation of this
     * handle.
     */
    fun authenticate(info: BiometricPrompt.PromptInfo, crypto: BiometricPrompt.CryptoObject?)

    /** Explicitly cancels any ongoing authentication process. */
    fun cancelAuthentication(canceledFrom: CanceledFrom)

    companion object {
        /**
         * Creates and returns a new instance of an [AuthenticationHandler].
         *
         * This factory method determines the appropriate authentication handler implementation
         * (e.g., using BiometricPrompt, FingerprintManager, or KeyguardManager) based on the
         * current device's capabilities and Android version.
         */
        @JvmStatic
        @JvmName("create")
        fun create(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            viewModel: AuthenticationViewModel,
            confirmCredentialActivityLauncher: Runnable,
            clientExecutor: Executor?,
            authenticationCallback: AuthenticationCallback?,
        ): AuthenticationHandler {
            val allowedAuthenticators = viewModel.allowedAuthenticators
            val isKeyguardManagerNeededForBiometricAndCredential =
                isKeyguardManagerNeededForBiometricAndCredential(allowedAuthenticators)
            viewModel.isUsingKeyguardManagerForBiometricAndCredential =
                isKeyguardManagerNeededForBiometricAndCredential

            /**
             * The [Executor] on which authentication callback methods will be invoked. Defaults to
             * a [PromptExecutor] if not provided.
             */
            val clientExecutor = clientExecutor ?: PromptExecutor()

            /** The [AuthenticationCallback] for the ongoing authentication. */
            val clientAuthenticationCallback: AuthenticationCallback =
                authenticationCallback ?: DefaultClientAuthenticationCallback()

            return when {
                DeviceUtils.isWearOS(context) ||
                    context.isKeyguardManagerNeededForNoBiometric(allowedAuthenticators) ||
                    isKeyguardManagerNeededForBiometricAndCredential -> {
                    AuthenticationHandlerKeyguardManager(
                        context,
                        lifecycleOwner,
                        viewModel,
                        confirmCredentialActivityLauncher,
                        clientExecutor,
                        clientAuthenticationCallback,
                    )
                }
                context.isUsingFingerprintDialog(viewModel.cryptoObject) ->
                    AuthenticationHandlerFingerprintManager(
                        context,
                        lifecycleOwner,
                        viewModel,
                        confirmCredentialActivityLauncher,
                        clientExecutor,
                        clientAuthenticationCallback,
                    )
                else ->
                    AuthenticationHandlerBiometricPrompt(
                        context,
                        lifecycleOwner,
                        viewModel,
                        confirmCredentialActivityLauncher,
                        clientExecutor,
                        clientAuthenticationCallback,
                    )
            }
        }
    }
}

private class DefaultClientAuthenticationCallback : AuthenticationCallback() {
    override fun onAuthenticationFailed() {
        logClientCallbackNullError()
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        logClientCallbackNullError()
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        logClientCallbackNullError()
    }

    private fun logClientCallbackNullError() {
        Log.e(
            TAG,
            "Callbacks are not re-registered when the caller's activity/fragment is " + "recreated!",
        )
    }
}

/**
 * Checks if this fragment is responsible for drawing and handling the result of a device credential
 * fallback button on the prompt.
 */
internal fun Context.isManagingDeviceCredentialButton(allowedAuthenticators: Int) =
    Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
        KeyguardUtils.isDeviceSecuredWithCredential(this) &&
        AuthenticatorUtils.isDeviceCredentialAllowed(allowedAuthenticators)

/**
 * Checks if this fragment should invoke [KeyguardManager.createConfirmDeviceCredentialIntent]
 * directly to start authentication when both biometric and credential are allowed, rather than
 * explicitly showing a dialog.
 */
private fun isKeyguardManagerNeededForBiometricAndCredential(allowedAuthenticators: Int): Boolean {
    // Devices from some vendors should use KeyguardManager for authentication if both
    // biometric and credential authenticator types are allowed (on API 29).
    return Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
        AuthenticatorUtils.isWeakBiometricAllowed(allowedAuthenticators) &&
        AuthenticatorUtils.isDeviceCredentialAllowed(allowedAuthenticators)
}

/**
 * Checks if this fragment should invoke [KeyguardManager.createConfirmDeviceCredentialIntent]
 * directly to start authentication when no biometric, rather than explicitly showing a dialog.
 */
internal fun Context.isKeyguardManagerNeededForNoBiometric(allowedAuthenticators: Int): Boolean {
    // On API 29, BiometricPrompt fails to launch the confirm device credential Settings
    // activity if no biometric hardware is present.
    if (
        Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
            !hasFingerprint() &&
            !hasFace() &&
            !hasIris()
    ) {
        return true
    }

    return isManagingDeviceCredentialButton(allowedAuthenticators) &&
        (BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) !=
            BiometricManager.BIOMETRIC_SUCCESS)
}

/**
 * Checks if this fragment should display the fingerprint dialog authentication UI to the user,
 * rather than delegate to the framework [android.hardware.biometrics.BiometricPrompt].
 */
@VisibleForTesting
internal fun Context.isUsingFingerprintDialog(crypto: BiometricPrompt.CryptoObject?) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
        isFingerprintDialogNeededForCrypto(crypto) ||
        isFingerprintDialogNeededForErrorHandling()

/**
 * Checks if this fragment should display the fingerprint dialog authentication UI for an ongoing
 * crypto-based authentication attempt.
 *
 * @see DeviceUtils.shouldUseFingerprintForCrypto
 */
private fun Context.isFingerprintDialogNeededForCrypto(crypto: BiometricPrompt.CryptoObject?) =
    crypto != null &&
        DeviceUtils.shouldUseFingerprintForCrypto(this, Build.MANUFACTURER, Build.MODEL)

/**
 * Checks if this fragment should invoke the fingerprint dialog, rather than the framework biometric
 * prompt, to handle an authentication error.
 *
 * @return Whether this fragment should invoke the fingerprint dialog.
 */
private fun Context.isFingerprintDialogNeededForErrorHandling(): Boolean {
    // On API 28, BiometricPrompt internally calls FingerprintManager#getErrorString(),
    // which
    // requires fingerprint hardware to be present (b/151443237).
    return Build.VERSION.SDK_INT == Build.VERSION_CODES.P && !hasFingerprint()
}

internal fun Context?.hasFingerprint() = PackageUtils.hasSystemFeatureFingerprint(this)

private fun Context?.hasFace() = PackageUtils.hasSystemFeatureFace(this)

private fun Context?.hasIris() = PackageUtils.hasSystemFeatureIris(this)

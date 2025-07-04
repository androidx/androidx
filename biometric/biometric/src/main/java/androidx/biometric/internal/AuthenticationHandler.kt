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
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.DeviceUtils
import androidx.biometric.utils.KeyguardUtils
import androidx.biometric.utils.PackageUtils
import java.util.concurrent.Executor

/**
 * This interface abstracts the underlying authentication mechanisms, allowing different
 * implementations (e.g., BiometricPrompt, FingerprintManager, KeyguardManager) to be used
 * interchangeably.
 */
internal interface AuthenticationHandler {

    /**
     * Prepares the authentication, setting up view model observers. This should be called **only**
     * when authentication is actually running.
     */
    fun prepareAuth()

    /**
     * Cleans up resources and unregisters any observers or callbacks associated with the
     * authentication handler.
     */
    fun destroy()

    /**
     * Initiates an authentication flow using the provided prompt [info] and an optional
     * [BiometricPrompt.CryptoObject].
     *
     * This is the primary method for starting a biometric or device credential authentication
     * attempt. The specific UI and underlying mechanism will depend on the implementation of this
     * handle.
     *
     * Note that [prepareAuth] needs to be called in this function because we're using lazy binding
     * for the view model. This means the view model is only bound when authentication is actually
     * happening. We also expose [prepareAuth] separately because we need to rebind the view model's
     * observers if the activity gets recreated (which might have already triggered a [destroy]
     * call) while an authentication is still running.
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
            viewModel: BiometricViewModel,
            confirmCredentialActivityLauncher: Runnable,
            clientExecutor: Executor?,
            authenticationCallback: AuthenticationCallback?,
        ): AuthenticationHandler {
            val allowedAuthenticators = viewModel.allowedAuthenticators
            val isKeyguardManagerNeededForBiometricAndCredential =
                isKeyguardManagerNeededForBiometricAndCredential(allowedAuthenticators)
            viewModel.setUsingKeyguardManagerForBiometricAndCredential(
                isKeyguardManagerNeededForBiometricAndCredential
            )

            val authenticationManager =
                AuthenticationManager(
                    context,
                    viewModel,
                    confirmCredentialActivityLauncher,
                    clientExecutor,
                    authenticationCallback,
                )

            return when {
                DeviceUtils.isWearOS(context) ||
                    context.isKeyguardManagerNeededForNoBiometric(allowedAuthenticators) ||
                    isKeyguardManagerNeededForBiometricAndCredential -> {
                    // TODO: add AuthenticationHandlerKeyguardManager
                    AuthenticationHandlerBiometricPrompt(authenticationManager)
                }
                context.isUsingFingerprintDialog(viewModel.cryptoObject) ->
                    // TODO: add AuthenticationHandlerFingerprintManager
                    AuthenticationHandlerBiometricPrompt(authenticationManager)
                else -> AuthenticationHandlerBiometricPrompt(authenticationManager)
            }
        }
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
private fun Context.isUsingFingerprintDialog(crypto: BiometricPrompt.CryptoObject?) =
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

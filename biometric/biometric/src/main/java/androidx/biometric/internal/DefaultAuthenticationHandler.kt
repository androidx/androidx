/*
 * Copyright 2026 The Android Open Source Project
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

/**
 * A delegating [AuthenticationHandler] that lazily creates and manages the appropriate underlying
 * authentication mechanism based on the provided [BiometricPrompt.PromptInfo] and
 * [BiometricPrompt.CryptoObject].
 */
internal class DefaultAuthenticationHandler(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AuthenticationViewModel,
    private val confirmCredentialActivityLauncher: Runnable,
    private val clientExecutor: Executor,
    private val clientAuthenticationCallback: AuthenticationCallback,
) : AuthenticationHandler {
    val key: Int = viewModel.generateNextHandlerKey()
    @VisibleForTesting var internalHandler: AuthenticationHandler? = null

    init {
        // If the prompt is already showing (e.g., after a configuration change),
        // reconnect to the existing authentication session.
        if (viewModel.isPromptShowing && key == viewModel.currentAuthenticationKey) {
            internalHandler = createHandler()
        }
    }

    override fun authenticate(
        info: BiometricPrompt.PromptInfo,
        crypto: BiometricPrompt.CryptoObject?,
    ) {
        // currentAuthenticationKey must be set prior to observing for correct validation.
        viewModel.currentAuthenticationKey = key
        viewModel.isPromptShowing = true
        viewModel.isAwaitingResult = true

        // PromptInfo has to be set prior to others.
        // TODO(b/493161907): Remove the same settings in AuthenticationManager.
        viewModel.setPromptInfo(info)
        viewModel.isIdentityCheckAvailable =
            BiometricManager.from(context).isIdentityCheckAvailable()
        viewModel.cryptoObject = crypto

        internalHandler = createHandler()
        internalHandler?.authenticate(info, crypto)
    }

    override fun cancelAuthentication(canceledFrom: CanceledFrom) {
        internalHandler?.cancelAuthentication(canceledFrom)
        internalHandler = null
    }

    private fun createHandler(): AuthenticationHandler {
        val allowedAuthenticators = viewModel.allowedAuthenticators
        viewModel.isUsingKeyguardManagerForBiometricAndCredential =
            isKeyguardManagerNeededForBiometricAndCredential(allowedAuthenticators)

        val isKeyguardManagerNeeded =
            DeviceUtils.isWearOS(context) ||
                viewModel.isUsingKeyguardManagerForBiometricAndCredential ||
                context.isKeyguardManagerNeededForNoBiometric(allowedAuthenticators)

        val isFingerprintNeeded =
            !isKeyguardManagerNeeded && context.isUsingFingerprintDialog(viewModel.cryptoObject)

        val onDismissed = {
            internalHandler = null
            viewModel.currentAuthenticationKey = 0
            viewModel.isPromptShowing = false
            viewModel.isConfirmingDeviceCredential = false
        }

        val manager =
            AuthenticationManager(
                context,
                lifecycleOwner,
                viewModel,
                confirmCredentialActivityLauncher,
                clientExecutor,
                clientAuthenticationCallback,
                onDismissed,
            )

        return when {
            isKeyguardManagerNeeded -> AuthenticationHandlerKeyguardManager(manager)
            isFingerprintNeeded -> AuthenticationHandlerFingerprintManager(manager)
            else -> AuthenticationHandlerBiometricPrompt(manager)
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
    // which requires fingerprint hardware to be present (b/151443237).
    return Build.VERSION.SDK_INT == Build.VERSION_CODES.P && !hasFingerprint()
}

internal fun Context?.hasFingerprint() = PackageUtils.hasSystemFeatureFingerprint(this)

private fun Context?.hasFace() = PackageUtils.hasSystemFeatureFace(this)

private fun Context?.hasIris() = PackageUtils.hasSystemFeatureIris(this)

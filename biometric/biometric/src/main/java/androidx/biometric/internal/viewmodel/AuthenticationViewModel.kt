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

package androidx.biometric.internal.viewmodel

import android.content.DialogInterface
import android.graphics.Bitmap
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.PromptContentView
import androidx.biometric.internal.CanceledFrom
import androidx.biometric.internal.data.AuthenticationStateRepository
import androidx.biometric.internal.data.PromptConfigRepository
import androidx.biometric.utils.AuthenticationCallbackProvider
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.CancellationSignalProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * A container for data associated with an ongoing authentication session, including intermediate
 * values needed to display the prompt UI.
 *
 * This model and all of its data is persisted over the lifetime of the client activity that hosts
 * the [androidx.biometric.BiometricPrompt].
 */
internal class AuthenticationViewModel(
    private val promptConfigRepository: PromptConfigRepository,
    private val authenticationStateRepository: AuthenticationStateRepository,
) : ViewModel() {
    /** The crypto object associated with the current authentication session. */
    var cryptoObject: BiometricPrompt.CryptoObject?
        get() = promptConfigRepository.cryptoObject
        set(value) {
            promptConfigRepository.cryptoObject = value
        }

    /** Indicating where the dialog was last canceled from. */
    var canceledFrom: CanceledFrom
        get() = authenticationStateRepository.canceledFrom
        set(value) {
            authenticationStateRepository.canceledFrom = value
        }

    /** Whether the prompt is currently showing. */
    var isPromptShowing: Boolean
        get() = promptConfigRepository.isPromptShowing
        set(value) {
            promptConfigRepository.isPromptShowing = value
        }

    /** Whether the client callback is awaiting an authentication result. */
    var isAwaitingResult: Boolean
        get() = authenticationStateRepository.isAwaitingResult
        set(value) {
            authenticationStateRepository.isAwaitingResult = value
        }

    /** Whether the user is currently authenticating with their PIN, pattern, or password. */
    var isConfirmingDeviceCredential: Boolean
        get() = promptConfigRepository.isConfirmingDeviceCredential
        set(value) {
            promptConfigRepository.isConfirmingDeviceCredential = value
        }

    /** Whether the prompt should delay showing the authentication UI. */
    var isDelayingPrompt: Boolean
        get() = promptConfigRepository.isDelayingPrompt
        set(value) {
            promptConfigRepository.isDelayingPrompt = value
        }

    /** Whether the prompt should ignore cancel requests not initiated by the client. */
    var isIgnoringCancel: Boolean
        get() = authenticationStateRepository.isIgnoringCancel
        set(value) {
            authenticationStateRepository.isIgnoringCancel = value
        }

    /**
     * Whether [android.app.KeyguardManager] is being used directly for authentication with both
     * biometric and credential authenticator types allowed.
     */
    var isUsingKeyguardManagerForBiometricAndCredential: Boolean
        get() = promptConfigRepository.isUsingKeyguardManagerForBiometricAndCredential
        set(value) {
            promptConfigRepository.isUsingKeyguardManagerForBiometricAndCredential = value
        }

    /** Whether the identity check is available on the current API level. */
    var isIdentityCheckAvailable: Boolean
        get() = promptConfigRepository.isIdentityCheckAvailable
        set(value) {
            promptConfigRepository.isIdentityCheckAvailable = value
        }

    /** Info about the appearance and behavior of the prompt provided by the client application. */
    private val promptInfo: BiometricPrompt.PromptInfo?
        get() = promptConfigRepository.promptInfo

    /** The type(s) of authenticators that may be invoked by the biometric prompt. */
    val allowedAuthenticators: Int
        @BiometricManager.AuthenticatorTypes get() = promptConfigRepository.allowedAuthenticators

    /** The logo res to be shown on the biometric prompt. */
    @Suppress("MissingPermission")
    val logoRes: Int
        get() = promptInfo?.logoRes ?: -1

    /** The logo bitmap to be shown on the biometric prompt. */
    @Suppress("MissingPermission")
    val logoBitmap: Bitmap?
        get() = promptInfo?.logoBitmap

    /** The logo description to be shown on the biometric prompt. */
    @Suppress("MissingPermission")
    val logoDescription: String?
        get() = promptInfo?.logoDescription

    /** The title to be shown on the biometric prompt. */
    val title: CharSequence?
        get() = promptInfo?.title

    /** The subtitle to be shown on the biometric prompt. */
    val subtitle: CharSequence?
        get() = promptInfo?.subtitle

    /** The description to be shown on the biometric prompt. */
    val description: CharSequence?
        get() = promptInfo?.description

    /** The prompt content view to be shown on the biometric prompt. */
    val contentView: PromptContentView?
        get() = promptInfo?.contentView

    /** If the confirmation required option is enabled for the biometric prompt. */
    val isConfirmationRequired: Boolean
        get() = promptInfo?.isConfirmationRequired ?: true

    /** The text that should be shown for the negative button on the biometric prompt. */
    val negativeButtonText: CharSequence?
        get() = promptConfigRepository.negativeButtonText

    /** A provider for cross-platform compatible cancellation signal objects. */
    val cancellationSignalProvider: CancellationSignalProvider
        get() = authenticationStateRepository.cancellationSignalProvider

    /** A provider for cross-platform compatible authentication callbacks. */
    val authenticationCallbackProvider: AuthenticationCallbackProvider by lazy {
        AuthenticationCallbackProvider(CallbackListener(this))
    }

    /** A dialog listener for the negative button shown on the prompt. */
    val negativeButtonListener: DialogInterface.OnClickListener by lazy {
        NegativeButtonListener(this)
    }

    /** A dialog listener for the more options button shown on the prompt content. */
    val moreOptionsButtonListener: DialogInterface.OnClickListener by lazy {
        MoreOptionsButtonListener(this)
    }

    /** A flow that emits the successful authentication result. */
    val authenticationResult: Flow<BiometricPrompt.AuthenticationResult>
        get() = authenticationStateRepository.authenticationResult

    /** A flow that emits the authentication error data. */
    val authenticationError: Flow<BiometricErrorData>
        get() = authenticationStateRepository.authenticationError

    /** A flow that holds the current authentication help message. */
    val authenticationHelpMessage: Flow<CharSequence?>
        get() = authenticationStateRepository.authenticationHelpMessage

    /** A flow that emits when an authentication failure occurs. */
    val isAuthenticationFailurePending: Flow<Unit>
        get() = authenticationStateRepository.isAuthenticationFailurePending

    /** A flow that emits when the negative button is pressed. */
    val isNegativeButtonPressPending: Flow<Unit>
        get() = authenticationStateRepository.isNegativeButtonPressPending

    /** A flow that emits when the more options button is pressed. */
    val isMoreOptionsButtonPressPending: Flow<Unit>
        get() = authenticationStateRepository.isMoreOptionsButtonPressPending

    /**
     * Attempts to infer the type of authenticator that was used to authenticate the user.
     *
     * @return The inferred authentication type, or
     *   [BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN] if unknown.
     */
    private val inferredAuthenticationResultType: Int
        @BiometricPrompt.AuthenticationResultType
        get() {
            return if (
                AuthenticatorUtils.isSomeBiometricAllowed(allowedAuthenticators) &&
                    !AuthenticatorUtils.isDeviceCredentialAllowed(allowedAuthenticators)
            ) {
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
            } else {
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
            }
        }

    /** Sets the [BiometricPrompt.PromptInfo] for the current authentication session. */
    fun setPromptInfo(promptInfo: BiometricPrompt.PromptInfo?) {
        promptConfigRepository.promptInfo = promptInfo
    }

    /** Sets a text override for the negative button. */
    fun setNegativeButtonTextOverride(negativeButtonTextOverride: CharSequence?) {
        promptConfigRepository.setNegativeButtonTextOverride(negativeButtonTextOverride)
    }

    /**
     * Sets whether the prompt should delay showing its UI after a given amount of time.
     *
     * @param delayingPrompt Whether the prompt UI should be delayed.
     * @param delayedTime The amount of time to wait, in milliseconds.
     */
    fun setDelayedDelayingPrompt(delayingPrompt: Boolean, delayedTime: Long) {
        viewModelScope.launch {
            promptConfigRepository.setDelayedDelayingPrompt(delayingPrompt, delayedTime)
        }
    }

    /**
     * Sets whether the prompt should ignore cancel requests from the framework after a given amount
     * of time.
     *
     * @param ignoringCancel Whether framework-initiated cancellations should be ignored.
     * @param delayedTime The amount of time to wait, in milliseconds.
     */
    fun setDelayedIgnoringCancel(ignoringCancel: Boolean, delayedTime: Long) {
        viewModelScope.launch {
            authenticationStateRepository.setDelayedIgnoringCancel(ignoringCancel, delayedTime)
        }
    }

    /** Emits a successful authentication result to be sent to the client. */
    fun setAuthenticationResult(authenticationResult: BiometricPrompt.AuthenticationResult?) {
        viewModelScope.launch {
            authenticationStateRepository.setAuthenticationResult(authenticationResult)
        }
    }

    /** Emits an authentication error to be sent to the client. */
    fun setAuthenticationError(authenticationError: BiometricErrorData?) {
        viewModelScope.launch {
            authenticationStateRepository.setAuthenticationError(authenticationError)
        }
    }

    /** Sets the current authentication help message. */
    fun setAuthenticationHelpMessage(authenticationHelpMessage: CharSequence?) {
        viewModelScope.launch {
            authenticationStateRepository.setAuthenticationHelpMessage(authenticationHelpMessage)
        }
    }

    /** Emits an event for a recoverable authentication failure. */
    fun setAuthenticationFailurePending() {
        viewModelScope.launch { authenticationStateRepository.setAuthenticationFailurePending() }
    }

    /** Emits an event for a negative button press. */
    fun setNegativeButtonPressPending() {
        viewModelScope.launch { authenticationStateRepository.setNegativeButtonPressPending() }
    }

    /** Emits an event for a more options button press. */
    fun setMoreOptionsButtonPressPending() {
        viewModelScope.launch { authenticationStateRepository.setMoreOptionsButtonPressPending() }
    }

    /**
     * The authentication callback listener passed to
     * [androidx.biometric.utils.AuthenticationCallbackProvider] when
     * [authenticationCallbackProvider] is called.
     */
    private class CallbackListener(viewModel: AuthenticationViewModel?) :
        AuthenticationCallbackProvider.Listener() {
        private val viewModelRef: WeakReference<AuthenticationViewModel> = WeakReference(viewModel)

        override fun onSuccess(result: BiometricPrompt.AuthenticationResult) {
            val viewModel = viewModelRef.get()
            if (viewModel != null && viewModel.isAwaitingResult) {
                // Try to infer the authentication type if unknown.
                var finalResult = result
                if (
                    result.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
                ) {
                    finalResult =
                        BiometricPrompt.AuthenticationResult(
                            result.cryptoObject,
                            viewModel.inferredAuthenticationResultType,
                        )
                }
                viewModel.setAuthenticationResult(finalResult)
            }
        }

        override fun onError(errorCode: Int, errorMessage: CharSequence?) {
            val viewModel = viewModelRef.get()
            if (
                viewModel != null &&
                    !viewModel.isConfirmingDeviceCredential &&
                    viewModel.isAwaitingResult
            ) {
                viewModel.setAuthenticationError(BiometricErrorData(errorCode, errorMessage))
            }
        }

        override fun onHelp(helpMessage: CharSequence?) {
            viewModelRef.get()?.setAuthenticationHelpMessage(helpMessage)
        }

        override fun onFailure() {
            val viewModel = viewModelRef.get()
            if (viewModel != null && viewModel.isAwaitingResult) {
                viewModel.setAuthenticationFailurePending()
            }
        }
    }

    /** The dialog listener that is returned by [negativeButtonListener]. */
    private class NegativeButtonListener(viewModel: AuthenticationViewModel?) :
        DialogInterface.OnClickListener {
        private val viewModelRef: WeakReference<AuthenticationViewModel> = WeakReference(viewModel)

        override fun onClick(dialogInterface: DialogInterface, which: Int) {
            viewModelRef.get()?.setNegativeButtonPressPending()
        }
    }

    /** The dialog listener that is returned by [moreOptionsButtonListener]. */
    private class MoreOptionsButtonListener(viewModel: AuthenticationViewModel?) :
        DialogInterface.OnClickListener {
        private val viewModelRef: WeakReference<AuthenticationViewModel> = WeakReference(viewModel)

        override fun onClick(dialogInterface: DialogInterface, which: Int) {
            viewModelRef.get()?.setMoreOptionsButtonPressPending()
        }
    }
}

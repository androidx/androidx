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
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.R
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.DeviceUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

/**
 * The amount of time (in milliseconds) to wait before showing the authentication UI if
 * [BiometricViewModel.isDelayingPrompt] is `true`.
 */
private const val SHOW_PROMPT_DELAY_MS = 600
private const val TAG = "AuthHandler"

/**
 * A class providing common methods and shared logic for concrete implementations of the
 * [AuthenticationHandler] interface. It allows [AuthenticationHandler] implementations to compose
 * common logic, for better separation of concerns and reusability.
 */
internal class AuthenticationManager(
    val context: Context,
    val viewModel: BiometricViewModel,
    val confirmCredentialActivityLauncher: Runnable,
    clientExecutor: Executor?,
    authenticationCallback: AuthenticationCallback?,
) {

    /** The [AuthenticationCallback] for the ongoing authentication. */
    val authenticationCallback: AuthenticationCallback =
        authenticationCallback ?: DefaultAuthenticationCallback()

    /**
     * The [Executor] on which authentication callback methods will be invoked. Defaults to a
     * [PromptExecutor] if not provided.
     */
    val clientExecutor: Executor = clientExecutor ?: PromptExecutor()

    /**
     * Handler associated with the main application looper, used for UI-related tasks if necessary
     * for callbacks or other operations.
     */
    val mainHandler = Handler(Looper.getMainLooper())

    private val authenticationResultObserver =
        Observer { authenticationResult: BiometricPrompt.AuthenticationResult? ->
            if (authenticationResult != null) {
                if (viewModel.isPromptShowing) {
                    onAuthenticationSucceeded(authenticationResult)
                }
                viewModel.setAuthenticationResult(null)
            }
        }

    private lateinit var onAuthenticationError:
        (errorCode: Int, errorMessage: CharSequence?) -> Unit
    private val authenticationErrorObserver = Observer { authenticationError: BiometricErrorData? ->
        if (authenticationError != null) {
            if (viewModel.isPromptShowing) {
                onAuthenticationError(
                    authenticationError.errorCode,
                    authenticationError.errorMessage,
                )
            }
            viewModel.setAuthenticationError(null)
        }
    }

    private val isAuthenticationFailurePendingObserver =
        Observer { authenticationFailurePending: Boolean? ->
            if (authenticationFailurePending != null && authenticationFailurePending) {
                if (viewModel.isPromptShowing) {
                    onAuthenticationFailed()
                }
                viewModel.setAuthenticationFailurePending(false)
            }
        }

    /**
     * Negative button press observer, this should only be used for biometric prompt and fingerprint
     * manager dialog
     */
    val isNegativeButtonPressPendingObserver = Observer { negativeButtonPressPending: Boolean? ->
        if (negativeButtonPressPending != null && negativeButtonPressPending) {
            if (viewModel.isPromptShowing) {
                if (context.isManagingDeviceCredentialButton(viewModel.allowedAuthenticators)) {
                    showKMAsFallback()
                } else {
                    onCancelButtonPressed()
                }
            }
            viewModel.setNegativeButtonPressPending(false)
        }
    }

    /** Prepares the authentication, setting up view model observers. */
    fun prepareAuth(onAuthenticationError: (errorCode: Int, errorMessage: CharSequence?) -> Unit) {
        this.onAuthenticationError = onAuthenticationError
        connectViewModelForAuthCallback()

        // Some device credential implementations in API 29 cause the prompt to receive a cancel
        // signal immediately after it's shown (b/162022588).
        // TODO(b/162022588): mViewModel.info hasn't been set. So isDeviceCredentialAllowed()
        //  check will always be false. Reproduce the bug and fix it.
        if (
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                AuthenticatorUtils.isDeviceCredentialAllowed(viewModel.allowedAuthenticators)
        ) {
            viewModel.setIgnoringCancel(true)
            // TODO: add a setDelayedIgnoringCancel in viewmodel and replace this class with
            // viewModel.setDelayedIgnoringCancel(false, 250L)
            mainHandler.postDelayed(StopIgnoringCancelRunnable(viewModel), 250L)
        }

        // TODO(b/263800618): Add lifecycle observer to cancel authentication when the app enters
        // the background. ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver), or
        // find a better alternative to handle backgrounded cancelling authentication, e.g. combine
        // LifecycleEventObserver and fragment.isRemoving()/!activity.isChangingConfigurations().
    }

    /**
     * Cleans up resources and unregisters any view model observers associated with the
     * authentication handler.
     */
    fun destroy() {
        disconnectViewModelForAuthCallback()
        // TODO(b/263800618): Remove lifecycle observer
        // ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
    }

    /**
     * Connects the [BiometricViewModel] for the ongoing authentication session, if this is called,
     * [disconnectViewModelForAuthCallback] must be called to remove observers.
     */
    private fun connectViewModelForAuthCallback() {
        viewModel.authenticationResult.observeForever(authenticationResultObserver)
        viewModel.authenticationError.observeForever(authenticationErrorObserver)
        viewModel
            .isAuthenticationFailurePending()
            .observeForever(isAuthenticationFailurePendingObserver)
    }

    /** Disconnects the [BiometricViewModel] for the ongoing authentication session. */
    private fun disconnectViewModelForAuthCallback() {
        viewModel.authenticationResult.removeObserver(authenticationResultObserver)
        viewModel.authenticationError.removeObserver(authenticationErrorObserver)
        viewModel
            .isAuthenticationFailurePending()
            .removeObserver(isAuthenticationFailurePendingObserver)
    }

    /**
     * Shows the prompt UI to the user and begins an authentication session.
     *
     * @param showAuthentication A lambda function that encapsulates the action of displaying the
     *   authentication prompt. This is where the actual framework call to show the authentication
     *   UI should be called.
     */
    fun authenticate(
        info: BiometricPrompt.PromptInfo,
        crypto: BiometricPrompt.CryptoObject?,
        showAuthentication: () -> Unit,
    ) {
        // PromptInfo has to be set prior to others.
        viewModel.setPromptInfo(info)

        viewModel.setIsIdentityCheckAvailable(
            BiometricManager.from(context).isIdentityCheckAvailable()
        )

        viewModel.setCryptoObject(crypto)

        viewModel.setNegativeButtonTextOverride(
            if (context.isManagingDeviceCredentialButton(viewModel.allowedAuthenticators)) {
                context.getString(R.string.confirm_device_credential_password)
            } else {
                // Don't override the negative button text from the client.
                null
            }
        )

        // Fall back to device credential immediately if no known biometrics are available.
        if (context.isKeyguardManagerNeededForNoBiometric(viewModel.allowedAuthenticators)) {
            viewModel.setDelayingPrompt(false)
        }

        // Check if we should delay showing the authentication prompt.
        if (viewModel.isDelayingPrompt) {
            mainHandler.postDelayed(
                { showPromptForAuthentication(showAuthentication) },
                SHOW_PROMPT_DELAY_MS.toLong(),
            )
        } else {
            showPromptForAuthentication(showAuthentication)
        }
    }

    /**
     * Cancels the ongoing authentication session with the [canceledFrom] info and sends an error to
     * the client callback.
     */
    fun cancelAuthentication(canceledFrom: CanceledFrom) {
        if (canceledFrom != CanceledFrom.CLIENT && viewModel.isIgnoringCancel) {
            return
        }
        viewModel.cancellationSignalProvider.cancel()
    }

    /**
     * Calls [KeyguardManager.createConfirmDeviceCredentialIntent] to show the credential view
     * fallback
     */
    fun showKMAsFallback() {
        // TODO: launch confirm credential activity
    }

    /**
     * Callback that is run when the view model reports that the cancel button has been pressed on
     * the prompt.
     */
    private fun onCancelButtonPressed() {
        val negativeButtonText: CharSequence? = viewModel.negativeButtonText

        sendErrorAndDismiss(
            androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            negativeButtonText ?: context.getString(R.string.default_error_msg),
        )

        cancelAuthentication(CanceledFrom.NEGATIVE_BUTTON)
    }

    /**
     * Shows any of the framework biometric prompt, or framework credential view, or AndroidX
     * fingerprint UI dialog to the user and begins authentication.
     */
    @Suppress("WeakerAccess")
    private fun showPromptForAuthentication(showAuthentication: () -> Unit) {
        if (!viewModel.isPromptShowing) {
            viewModel.setPromptShowing(true)
            viewModel.setAwaitingResult(true)

            showAuthentication()
        }
    }

    /** Removes any associated UI from the client activity/fragment. */
    private fun dismiss() {
        viewModel.setPromptShowing(false)

        // Wait before showing again to work around a dismissal logic issue on API 29 (b/157783075).
        if (DeviceUtils.shouldDelayShowingPrompt(context, Build.MODEL)) {
            viewModel.setDelayingPrompt(true)
            // TODO: add a setDelayedDelayingPrompt in viewmodel and replace this class with
            // viewModel.setDelayedDelayingPrompt(false, SHOW_PROMPT_DELAY_MS.toLong())
            mainHandler.postDelayed(
                StopDelayingPromptRunnable(viewModel),
                SHOW_PROMPT_DELAY_MS.toLong(),
            )
        }
        destroy()
    }

    /** Callback that is run when the view model receives a successful authentication [result]. */
    /* synthetic access */ @VisibleForTesting
    private fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        sendSuccessAndDismiss(result)
    }

    /** Callback that is run when the view model reports a failed authentication attempt. */
    private fun onAuthenticationFailed() {
        sendFailureToClient()
    }

    /**
     * Sends an unrecoverable error result with [errorCode] and [errorString] to the client and
     * dismisses the prompt.
     */
    fun sendErrorAndDismiss(errorCode: Int, errorString: CharSequence) {
        sendErrorToClient(errorCode, errorString)
        dismiss()
    }

    /** Sends an unrecoverable error result to the client callback. */
    private fun sendErrorToClient(errorCode: Int, errorString: CharSequence) {
        if (viewModel.isConfirmingDeviceCredential) {
            Log.v(TAG, "Error not sent to client. User is confirming their device credential.")
            return
        }

        if (!viewModel.isAwaitingResult) {
            Log.w(TAG, "Error not sent to client. Client is not awaiting a result.")
            return
        }

        viewModel.setAwaitingResult(false)
        clientExecutor.execute {
            authenticationCallback.onAuthenticationError(errorCode, errorString)
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

        viewModel.setAwaitingResult(false)
        clientExecutor.execute { authenticationCallback.onAuthenticationSucceeded(result) }
    }

    /** Sends an authentication failure event to the client callback. */
    private fun sendFailureToClient() {
        if (!viewModel.isAwaitingResult) {
            Log.w(TAG, "Failure not sent to client. Client is not awaiting a result.")
            return
        }

        clientExecutor.execute { authenticationCallback.onAuthenticationFailed() }
    }
}

/** Represents the source or reason why an authentication operation was canceled. */
internal enum class CanceledFrom {
    INTERNAL,
    USER,
    NEGATIVE_BUTTON,
    CLIENT,
    MORE_OPTIONS_BUTTON,
}

/** An executor used by [android.hardware.biometrics.BiometricPrompt] to run framework code. */
internal class PromptExecutor : Executor {
    private val promptHandler = Handler(Looper.getMainLooper())

    override fun execute(runnable: Runnable) {
        promptHandler.post(runnable)
    }
}

private class DefaultAuthenticationCallback : AuthenticationCallback() {
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

private class AppLifecycleListener(val onBackgrounded: () -> Unit = {}) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        // app moved to background
        onBackgrounded()
    }
}

// TODO(b/178855209): Remove this after converting viewmodel to kotlin
/**
 * A runnable with a weak reference to a [BiometricViewModel] that can be used to invoke
 * [BiometricViewModel.setDelayingPrompt] with a value of `false`.
 */
private class StopDelayingPromptRunnable(viewModel: BiometricViewModel?) : Runnable {
    private val mViewModelRef: WeakReference<BiometricViewModel?> =
        WeakReference<BiometricViewModel?>(viewModel)

    override fun run() {
        if (mViewModelRef.get() != null) {
            mViewModelRef.get()!!.isDelayingPrompt = false
        }
    }
}

/**
 * A runnable with a weak reference to a {@link BiometricViewModel} that can be used to invoke
 * {@link BiometricViewModel#setIgnoringCancel(boolean)} with a value of {@code false}.
 */
private class StopIgnoringCancelRunnable(viewModel: BiometricViewModel?) : Runnable {
    private val mViewModelRef: WeakReference<BiometricViewModel?>? =
        WeakReference<BiometricViewModel?>(viewModel)

    override fun run() {
        if (mViewModelRef!!.get() != null) {
            mViewModelRef.get()!!.isIgnoringCancel = false
        }
    }
}

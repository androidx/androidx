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

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.PromptContentView
import android.os.Build
import android.os.CancellationSignal
import android.text.TextUtils
import android.util.Log
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.biometric.R
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.CryptoObjectUtils
import androidx.biometric.utils.ErrorUtils
import androidx.biometric.utils.PromptContentViewUtils
import androidx.lifecycle.Observer
import java.util.concurrent.Executor

private const val TAG = "AuthHandlerBP"

internal class AuthenticationHandlerBiometricPrompt(
    private val authenticationManager: AuthenticationManager
) : AuthenticationHandler {
    private val context = authenticationManager.context
    private val viewModel = authenticationManager.viewModel
    private val clientExecutor = authenticationManager.clientExecutor
    private val isNegativeButtonPressPendingObserver =
        authenticationManager.isNegativeButtonPressPendingObserver
    private var isPrepared: Boolean = false

    private val isMoreOptionsButtonPressPendingObserver =
        Observer { moreOptionsButtonPressPending: Boolean? ->
            if (moreOptionsButtonPressPending != null && moreOptionsButtonPressPending) {
                if (viewModel.isPromptShowing) {
                    onMoreOptionsButtonPressed()
                }
                viewModel.setMoreOptionsButtonPressPending(false)
            }
        }

    override fun prepareAuth() {
        if (isPrepared) {
            return
        }
        authenticationManager.prepareAuth { errorCode, errorMessage ->
            onAuthenticationError(errorCode, errorMessage)
        }
        connectViewModelForButtons()

        isPrepared = true
    }

    override fun destroy() {
        authenticationManager.destroy()
        disconnectViewModelForButtons()
        isPrepared = false
    }

    override fun authenticate(info: PromptInfo, crypto: CryptoObject?) {
        prepareAuth()
        authenticationManager.authenticate(info, crypto) { showAuthentication() }
    }

    override fun cancelAuthentication(canceledFrom: CanceledFrom) {
        authenticationManager.cancelAuthentication(canceledFrom)
        destroy()
    }

    private fun connectViewModelForButtons() {
        viewModel.isNegativeButtonPressPending.observeForever(isNegativeButtonPressPendingObserver)
        viewModel.isMoreOptionsButtonPressPending.observeForever(
            isMoreOptionsButtonPressPendingObserver
        )
    }

    private fun disconnectViewModelForButtons() {
        viewModel.isNegativeButtonPressPending.removeObserver(isNegativeButtonPressPendingObserver)
        viewModel.isMoreOptionsButtonPressPending.removeObserver(
            isMoreOptionsButtonPressPendingObserver
        )
    }

    /**
     * Shows the framework [android.hardware.biometrics.BiometricPrompt] UI to the user and begins
     * authentication.
     */
    @SuppressLint("NewApi")
    private fun showAuthentication() {
        val builder = Api28Impl.createPromptBuilder(context.applicationContext)

        val title: CharSequence? = viewModel.title
        val subtitle: CharSequence? = viewModel.subtitle
        val description: CharSequence? = viewModel.description
        if (title != null) {
            Api28Impl.setTitle(builder, title)
        }
        if (subtitle != null) {
            Api28Impl.setSubtitle(builder, subtitle)
        }
        if (description != null) {
            Api28Impl.setDescription(builder, description)
        }

        val negativeButtonText: CharSequence? = viewModel.negativeButtonText
        if (negativeButtonText != null && !TextUtils.isEmpty(negativeButtonText)) {
            Api28Impl.setNegativeButton(
                builder,
                negativeButtonText,
                clientExecutor,
                viewModel.negativeButtonListener,
            )
        }

        // Set the confirmation required option introduced in Android 10 (API 29).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.setConfirmationRequired(builder, viewModel.isConfirmationRequired)
        }

        // Set or emulate the allowed authenticators option introduced in Android 11 (API 30).
        @BiometricManager.AuthenticatorTypes
        val authenticators: Int = viewModel.allowedAuthenticators
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.setAllowedAuthenticators(builder, authenticators)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.setDeviceCredentialAllowed(
                builder,
                AuthenticatorUtils.isDeviceCredentialAllowed(authenticators),
            )
        }

        // Set the custom biometric prompt features introduced in Android 15 (API 35).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val logoRes: Int = viewModel.logoRes
            val logoBitmap: Bitmap? = viewModel.logoBitmap
            val logoDescription: String? = viewModel.logoDescription
            val contentView =
                PromptContentViewUtils.wrapForBiometricPrompt(
                    viewModel.contentView,
                    clientExecutor,
                    viewModel.moreOptionsButtonListener,
                )
            if (logoRes != -1) {
                Api35Impl.setLogoRes(builder, logoRes)
            }
            if (logoBitmap != null) {
                Api35Impl.setLogoBitmap(builder, logoBitmap)
            }
            if (logoDescription != null && !logoDescription.isEmpty()) {
                Api35Impl.setLogoDescription(builder, logoDescription)
            }
            if (contentView != null) {
                Api35Impl.setContentView(builder, contentView)
            }
        }

        authenticateWithBiometricPrompt(Api28Impl.buildPrompt(builder), context)
    }

    /** Requests user authentication with the given framework [biometricPrompt] and [context]. */
    @RequiresApi(Build.VERSION_CODES.P)
    @VisibleForTesting
    private fun authenticateWithBiometricPrompt(
        biometricPrompt: BiometricPrompt,
        context: Context,
    ) {
        val cryptoObject = CryptoObjectUtils.wrapForBiometricPrompt(viewModel.cryptoObject)
        val cancellationSignal: CancellationSignal =
            viewModel.cancellationSignalProvider.getBiometricCancellationSignal()
        val executor: Executor = PromptExecutor()
        val callback: BiometricPrompt.AuthenticationCallback =
            viewModel.authenticationCallbackProvider.getBiometricCallback()

        try {
            if (cryptoObject == null) {
                Api28Impl.authenticate(biometricPrompt, cancellationSignal, executor, callback)
            } else {
                Api28Impl.authenticate(
                    biometricPrompt,
                    cryptoObject,
                    cancellationSignal,
                    executor,
                    callback,
                )
            }
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with biometric prompt.", e)
            val errorCode = androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE
            val errorString = context.getString(R.string.default_error_msg)
            authenticationManager.sendErrorAndDismiss(errorCode, errorString)
        }
    }

    /** Callback that is run when the view model receives an unrecoverable error result. */
    private fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
        // Ensure we're only sending publicly defined errors.
        val knownErrorCode = ErrorUtils.toKnownErrorCode(errorCode)
        if (
            ErrorUtils.isLockoutError(knownErrorCode) &&
                context.isManagingDeviceCredentialButton(viewModel.allowedAuthenticators)
        ) {
            authenticationManager.showKMAsFallback()
            return
        }

        var errorString = errorMessage
        if (errorString == null) {
            errorString = context.getString(R.string.default_error_msg)
        }
        authenticationManager.sendErrorAndDismiss(knownErrorCode, errorString)
    }

    /**
     * Callback that is run when the view model reports that the more options button has been
     * pressed on the prompt content.
     */
    private fun onMoreOptionsButtonPressed() {
        authenticationManager.sendErrorAndDismiss(
            androidx.biometric.BiometricPrompt.ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON,
            context.getString(R.string.content_view_more_options_button_clicked),
        )
        cancelAuthentication(CanceledFrom.MORE_OPTIONS_BUTTON)
    }
}

/** Nested class to avoid verification errors for methods introduced in Android 15.0 (API 35). */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SuppressLint("MissingPermission")
private object Api35Impl {
    /**
     * Sets the prompt content view for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param logoRes A drawable resource of the logo that will be shown on the prompt.
     */
    @DoNotInline
    fun setLogoRes(builder: BiometricPrompt.Builder, logoRes: Int) {
        builder.setLogoRes(logoRes)
    }

    /**
     * Sets the prompt content view for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param logoBitmap A bitmap drawable of the logo that will be shown on the prompt.
     */
    @DoNotInline
    fun setLogoBitmap(builder: BiometricPrompt.Builder, logoBitmap: Bitmap) {
        builder.setLogoBitmap(logoBitmap)
    }

    /**
     * Sets the prompt content view for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param logoDescription The content view for the prompt.
     */
    @DoNotInline
    fun setLogoDescription(builder: BiometricPrompt.Builder, logoDescription: String?) {
        builder.setLogoDescription(logoDescription!!)
    }

    /**
     * Sets the prompt content view for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param contentView The content view for the prompt.
     */
    @DoNotInline
    fun setContentView(builder: BiometricPrompt.Builder, contentView: PromptContentView) {
        builder.setContentView(contentView)
    }
}

/** Nested class to avoid verification errors for methods introduced in Android 11 (API 30). */
@RequiresApi(Build.VERSION_CODES.R)
private object Api30Impl {
    /**
     * Sets the allowed authenticator type(s) for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param allowedAuthenticators A bit field representing allowed authenticator types.
     */
    @DoNotInline // This is expected because AndroidX and framework annotation are not identical
    @Suppress("WrongConstant")
    fun setAllowedAuthenticators(
        builder: BiometricPrompt.Builder,
        @BiometricManager.AuthenticatorTypes allowedAuthenticators: Int,
    ) {
        builder.setAllowedAuthenticators(allowedAuthenticators)
    }
}

/** Nested class to avoid verification errors for methods introduced in Android 10 (API 29). */
@RequiresApi(Build.VERSION_CODES.Q)
private object Api29Impl {
    /**
     * Sets the "confirmation required" option for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param confirmationRequired The value for the "confirmation required" option.
     */
    @DoNotInline
    fun setConfirmationRequired(builder: BiometricPrompt.Builder, confirmationRequired: Boolean) {
        builder.setConfirmationRequired(confirmationRequired)
    }

    /**
     * Sets the "device credential allowed" option for the given framework prompt builder.
     *
     * @param builder An instance of [ ].
     * @param deviceCredentialAllowed The value for the "device credential allowed" option.
     */
    @Suppress("deprecation")
    @DoNotInline
    fun setDeviceCredentialAllowed(
        builder: BiometricPrompt.Builder,
        deviceCredentialAllowed: Boolean,
    ) {
        builder.setDeviceCredentialAllowed(deviceCredentialAllowed)
    }
}

/** Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28). */
@RequiresApi(Build.VERSION_CODES.P)
private object Api28Impl {
    /**
     * Creates an instance of the framework class [BiometricPrompt.Builder].
     *
     * @param context The application or activity context.
     * @return An instance of [BiometricPrompt.Builder].
     */
    @DoNotInline
    fun createPromptBuilder(context: Context): BiometricPrompt.Builder {
        return BiometricPrompt.Builder(context)
    }

    /**
     * Sets the title for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param title The title for the prompt.
     */
    @DoNotInline
    fun setTitle(builder: BiometricPrompt.Builder, title: CharSequence) {
        builder.setTitle(title)
    }

    /**
     * Sets the subtitle for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param subtitle The subtitle for the prompt.
     */
    @DoNotInline
    fun setSubtitle(builder: BiometricPrompt.Builder, subtitle: CharSequence) {
        builder.setSubtitle(subtitle)
    }

    /**
     * Sets the description for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param description The description for the prompt.
     */
    @DoNotInline
    fun setDescription(builder: BiometricPrompt.Builder, description: CharSequence) {
        builder.setDescription(description)
    }

    /**
     * Sets the negative button text and behavior for the given framework prompt builder.
     *
     * @param builder An instance of [BiometricPrompt.Builder].
     * @param text The text for the negative button.
     * @param executor An executor for the negative button callback.
     * @param listener A listener for the negative button press event.
     */
    @DoNotInline
    fun setNegativeButton(
        builder: BiometricPrompt.Builder,
        text: CharSequence,
        executor: Executor,
        listener: DialogInterface.OnClickListener,
    ) {
        builder.setNegativeButton(text, executor, listener)
    }

    /**
     * Creates an instance of the framework class [BiometricPrompt] from the given builder.
     *
     * @param builder The builder for the prompt.
     * @return An instance of [BiometricPrompt].
     */
    @DoNotInline
    fun buildPrompt(builder: BiometricPrompt.Builder): BiometricPrompt {
        return builder.build()
    }

    /**
     * Starts (non-crypto) authentication for the given framework biometric prompt.
     *
     * @param biometricPrompt An instance of [BiometricPrompt].
     * @param cancellationSignal A cancellation signal object for the prompt.
     * @param executor An executor for authentication callbacks.
     * @param callback An object that will receive authentication events.
     */
    @DoNotInline
    fun authenticate(
        biometricPrompt: BiometricPrompt,
        cancellationSignal: CancellationSignal,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback,
    ) {
        biometricPrompt.authenticate(cancellationSignal, executor, callback)
    }

    /**
     * Starts (crypto-based) authentication for the given framework biometric prompt.
     *
     * @param biometricPrompt An instance of [BiometricPrompt].
     * @param crypto A crypto object associated with the given authentication.
     * @param cancellationSignal A cancellation signal object for the prompt.
     * @param executor An executor for authentication callbacks.
     * @param callback An object that will receive authentication events.
     */
    @DoNotInline
    fun authenticate(
        biometricPrompt: BiometricPrompt,
        crypto: BiometricPrompt.CryptoObject,
        cancellationSignal: CancellationSignal,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback,
    ) {
        biometricPrompt.authenticate(crypto, cancellationSignal, executor, callback)
    }
}

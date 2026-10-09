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

package androidx.biometric.internal.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.FingerprintDialogModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.CryptoObjectUtils
import androidx.biometric.utils.DeviceUtils
import androidx.biometric.utils.ErrorUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A controller that hosts the fingerprint dialog UI in the caller's window for the legacy
 * `FingerprintManager` API.
 *
 * This controller is invoked by
 * [androidx.biometric.internal.AuthenticationHandlerFingerprintManager] to display the fingerprint
 * prompt to the user. It is responsible for managing the lifecycle of the dialog and communicating
 * authentication events back to the handler via the scoped [AuthenticationViewModel].
 */
internal class FingerprintDialogController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val authenticationViewModel: AuthenticationViewModel,
) {
    internal companion object {
        /** The delay in milliseconds to hide the dialog after an error. */
        const val HIDE_DIALOG_DELAY_MS = 2000
        const val DISMISS_INSTANTLY_DELAY_MS = 500L
        private const val MESSAGE_DISPLAY_TIME_MS = 2000L
        private const val TAG = "FingerprintDialogController"
    }

    /** The currently displayed fingerprint dialog. */
    private var fingerprintDialog: AlertDialog? = null
    private var resetDialogJob: Job? = null
    private var dismissInstantlyJob: Job? = null
    private var observersJob: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var isObserving = false

    private var fingerprintIcon: ImageView? = null
    private var helpMessageView: TextView? = null
    /**
     * The color used for error and failure messages. Resolved against the dialog's themed context
     * in [showAlertDialog]: prefer the AppCompat `colorError` attribute (defined by AppCompat and
     * Material themes), fall back to the platform `android:colorError` attribute for hosts on
     * platform themes, and finally to the library's bundled color so the text is never transparent.
     */
    private var errorTextColor: Int = 0

    /** The color used for help and status messages. Resolved alongside [errorTextColor]. */
    private var normalTextColor: Int = 0

    /** Resolves [errorTextColor] and [normalTextColor] from the given dialog-themed [context]. */
    private fun resolveTextColors(context: Context) {
        val themedErrorColor =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getThemedColorOrNull(Api26Impl.colorErrorAttr)
                    ?: context.getThemedColorOrNull(Api26Impl.platformColorErrorAttr)
            } else {
                null
            }
        errorTextColor =
            themedErrorColor ?: ContextCompat.getColor(context, R.color.biometric_error_color)
        normalTextColor =
            context.getThemedColorOrNull(android.R.attr.textColorSecondary)
                ?: helpMessageView?.currentTextColor
                ?: errorTextColor
    }

    /** The state holder for managing the UI state of the fingerprint dialog. */
    private val fingerprintDialogModel: FingerprintDialogModel
        get() = authenticationViewModel.fingerprintDialogModel

    /** Whether the fingerprint alert dialog is currently showing. */
    val isDialogShowing: Boolean
        get() = fingerprintDialog?.isShowing == true

    /**
     * Shows the fingerprint dialog UI, connects state observers, and starts fingerprint
     * authentication.
     *
     * If the host activity is finishing or destroyed, authentication is not started and
     * [BiometricPrompt.ERROR_CANCELED] is reported instead so the client always receives a result.
     * If the dialog cannot be shown for any other reason (e.g. a non-activity context),
     * authentication still proceeds without a dialog.
     */
    fun showAuthentication() {
        if (
            context.isHostActivityFinishing() ||
                lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED
        ) {
            Log.w(TAG, "Host is finishing or destroyed. Canceling authentication.")
            val errorCode = BiometricPrompt.ERROR_CANCELED
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(
                    errorCode,
                    ErrorUtils.getFingerprintErrorString(context, errorCode),
                )
            )
            return
        }

        fingerprintDialogModel.setCancelPending(false)
        resetDialog()
        showDialogForReconnect()
        startFingerprintAuthentication()
    }

    /**
     * Shows the fingerprint dialog UI and connects state observers (for example, when reconnecting
     * after a configuration change).
     */
    fun showDialogForReconnect() {
        if (isObserving) {
            return
        }
        isObserving = true

        val observer = LifecycleEventObserver { owner, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                val isChangingConfigurations = owner.isChangingConfigurations(context)
                dismiss()
                if (!isChangingConfigurations) {
                    cancelAuthentication(CanceledFrom.INTERNAL)
                }
            }
        }
        lifecycleObserver = observer
        lifecycleOwner.lifecycle.addObserver(observer)

        if (context.canShowDialog()) {
            showAlertDialog()
        } else {
            Log.w(TAG, "Unable to show fingerprint dialog. Authenticating without a dialog.")
        }
        connectObservers()
    }

    /** Dismisses the currently shown fingerprint dialog and disconnects UI observers. */
    fun dismiss() {
        isObserving = false
        lifecycleObserver?.let {
            lifecycleOwner.lifecycle.removeObserver(it)
            lifecycleObserver = null
        }
        resetDialogJob?.cancel()
        resetDialogJob = null
        dismissInstantlyJob?.cancel()
        dismissInstantlyJob = null
        observersJob?.cancel()
        observersJob = null
        destroyAlertDialog()
    }

    /**
     * Creates and shows the fingerprint [AlertDialog].
     *
     * @return The created dialog, or `null` if the dialog should be hidden on the current device.
     */
    private fun showAlertDialog(): AlertDialog? {
        if (DeviceUtils.shouldHideFingerprintDialog(context, Build.MODEL)) {
            return null
        }

        fingerprintDialogModel.isDismissedInstantly = true
        // For showing error message
        dismissInstantlyJob?.cancel()
        dismissInstantlyJob =
            lifecycleOwner.lifecycleScope.launch {
                delay(DISMISS_INSTANTLY_DELAY_MS)
                fingerprintDialogModel.isDismissedInstantly = false
            }

        val builder = AlertDialog.Builder(context)
        builder.setTitle(authenticationViewModel.title)

        // We have to use builder.context instead of the usual context in order to get
        // the appropriately themed context for this dialog.
        val layout =
            LayoutInflater.from(builder.context).inflate(R.layout.fingerprint_dialog_layout, null)

        val subtitleView = layout.findViewById<TextView?>(R.id.fingerprint_subtitle)
        if (subtitleView != null) {
            val subtitle = authenticationViewModel.subtitle
            if (TextUtils.isEmpty(subtitle)) {
                subtitleView.visibility = View.GONE
            } else {
                subtitleView.visibility = View.VISIBLE
                subtitleView.text = subtitle
            }
        }

        val descriptionView = layout.findViewById<TextView?>(R.id.fingerprint_description)
        if (descriptionView != null) {
            val description = authenticationViewModel.description
            if (TextUtils.isEmpty(description)) {
                descriptionView.visibility = View.GONE
            } else {
                descriptionView.visibility = View.VISIBLE
                descriptionView.text = description
            }
        }

        fingerprintIcon = layout.findViewById(R.id.fingerprint_icon)
        helpMessageView = layout.findViewById(R.id.fingerprint_error)
        resolveTextColors(builder.context)

        val negativeButtonText =
            if (
                AuthenticatorUtils.isDeviceCredentialAllowed(
                    authenticationViewModel.allowedAuthenticators
                )
            ) {
                context.getString(R.string.confirm_device_credential_password)
            } else {
                authenticationViewModel.singleFallbackOptionText
            }
        builder.setNegativeButton(negativeButtonText) { _, _ ->
            authenticationViewModel.setNegativeButtonPressPending()
        }

        builder.setView(layout)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnCancelListener { _ -> fingerprintDialogModel.setCancelPending(true) }
        try {
            dialog.show()
            fingerprintDialog = dialog
        } catch (e: Exception) {
            Log.e(TAG, "Unable to show fingerprint dialog", e)
        }

        return fingerprintDialog
    }

    /** Destroys the currently shown fingerprint dialog. */
    private fun destroyAlertDialog() {
        fingerprintDialog?.let {
            try {
                if (it.isShowing) {
                    it.dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing fingerprint dialog", e)
            } finally {
                fingerprintDialog = null
            }
        }
        fingerprintIcon = null
        helpMessageView = null

        // Always set this to true. In case the user tries to authenticate again
        // the UI will not be shown.
        fingerprintDialogModel.isDismissedInstantly = true
    }

    /**
     * Cancels the ongoing authentication.
     *
     * @param canceledFrom The source of the cancellation.
     */
    private fun cancelAuthentication(canceledFrom: CanceledFrom) {
        if (canceledFrom == CanceledFrom.USER) {
            val errorCode = BiometricPrompt.ERROR_USER_CANCELED
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(
                    errorCode,
                    ErrorUtils.getFingerprintErrorString(context, errorCode),
                )
            )
        }
        authenticationViewModel.cancellationSignalProvider.cancel()
    }

    /** Connects observers to the ViewModels to react to state changes. */
    private fun connectObservers() {
        observersJob?.cancel()
        observersJob =
            lifecycleOwner.lifecycleScope.launch {
                launch {
                    fingerprintDialogModel.isCancelPending.collect { isCancelPending ->
                        if (isCancelPending) {
                            fingerprintDialogModel.setCancelPending(false)
                            cancelAuthentication(CanceledFrom.USER)
                            dismiss()
                        }
                    }
                }

                launch {
                    fingerprintDialogModel.state.collect { currentState ->
                        if (
                            fingerprintDialogModel.previousState != FingerprintDialogState.NONE ||
                                currentState != FingerprintDialogState.FINGERPRINT
                        ) {
                            resetDialogJob?.cancel()
                            resetDialogJob = launch {
                                delay(MESSAGE_DISPLAY_TIME_MS)
                                resetDialog()
                                resetDialogJob = null
                            }
                        }
                    }
                }

                launch {
                    fingerprintDialogModel.drawableResId.collect { resId ->
                        if (resId != 0) {
                            val icon = ContextCompat.getDrawable(context, resId)
                            fingerprintIcon?.setImageDrawable(icon)
                            if (fingerprintDialogModel.shouldAnimateIcon.value) {
                                icon?.let { Api21ImplForFm.startAnimation(it) }
                            }
                        }
                    }
                }

                launch {
                    fingerprintDialogModel.helpMessageInfo.collect {
                        (helpMessage, shouldUseErrorColor) ->
                        if (helpMessageView?.text != helpMessage) {
                            helpMessageView?.text = helpMessage
                            helpMessageView?.setTextColor(
                                if (shouldUseErrorColor) errorTextColor else normalTextColor
                            )
                        }
                    }
                }

                launch {
                    authenticationViewModel.authenticationHelpMessage.collect { help ->
                        help?.let { showFingerprintErrorMessage(help) }
                    }
                }

                launch {
                    authenticationViewModel.isNegativeButtonPressPending.collect { dismiss() }
                }

                launch {
                    authenticationViewModel.authenticationResult.collect { dismiss() }
                }

                launch {
                    authenticationViewModel.authenticationError.collect { error ->
                        val knownErrorCode =
                            ErrorUtils.toKnownErrorCodeForAuthenticate(error.errorCode)
                        // Define the special cases where we should NOT show an error message.
                        val isLockoutHandledByButton =
                            ErrorUtils.isLockoutError(knownErrorCode) &&
                                authenticationViewModel.isOverriddenDeviceCredential

                        val isCanceled = knownErrorCode == BiometricPrompt.ERROR_CANCELED

                        // Only show the error message if it's not one of the special cases
                        // and the dialog is not already being dismissed instantly.
                        if (
                            !isLockoutHandledByButton &&
                                !isCanceled &&
                                !fingerprintDialogModel.isDismissedInstantly
                        ) {
                            showFingerprintErrorMessage(error.errorMessage)
                            launch {
                                delay(context.getDismissDialogDelay().toLong())
                                dismiss()
                            }
                        } else {
                            dismiss()
                        }
                    }
                }

                launch {
                    authenticationViewModel.isAuthenticationFailurePending.collect { _ ->
                        showFingerprintErrorMessage(
                            context.getString(R.string.fingerprint_not_recognized)
                        )
                    }
                }
            }
    }

    /**
     * Checks for pre-authentication errors and starts the authentication process if none are found.
     */
    private fun startFingerprintAuthentication() {
        val appContext = context.applicationContext ?: context
        @Suppress("deprecation")
        val fingerprintManagerCompat =
            androidx.biometric.internal.FingerprintManagerCompat.from(appContext)
        val errorCode = fingerprintDialogModel.fingerprintPreAuthChecker(fingerprintManagerCompat)
        if (errorCode != BiometricPrompt.BIOMETRIC_SUCCESS) {
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(
                    errorCode,
                    ErrorUtils.getFingerprintErrorString(appContext, errorCode),
                )
            )
            return
        }

        fingerprintDialogModel.isDismissedInstantly = true
        authenticateWithFingerprint(fingerprintManagerCompat)
    }

    /**
     * Requests user authentication with the given fingerprint manager.
     *
     * @param fingerprintManager The fingerprint manager that will be used for authentication.
     */
    @Suppress("deprecation")
    private fun authenticateWithFingerprint(
        fingerprintManager: androidx.biometric.internal.FingerprintManagerCompat
    ) {
        val crypto =
            CryptoObjectUtils.wrapForFingerprintManager(authenticationViewModel.cryptoObject)
        val cancellationSignal =
            authenticationViewModel.cancellationSignalProvider.fingerprintCancellationSignal

        try {
            fingerprintManager.authenticate(
                crypto,
                0, /* flags */
                cancellationSignal,
                authenticationViewModel.authenticationCallbackProvider.fingerprintCallback,
                null, /* handler */
            )
        } catch (e: NullPointerException) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with fingerprint.", e)
            val errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(
                    errorCode,
                    ErrorUtils.getFingerprintErrorString(context, errorCode),
                )
            )
        }
    }

    /**
     * Shows an error message on the fingerprint dialog.
     *
     * @param errorMessage The error message to display.
     */
    private fun showFingerprintErrorMessage(errorMessage: CharSequence?) {
        val helpMessage = errorMessage ?: context.getString(R.string.default_error_msg)
        fingerprintDialogModel.setState(FingerprintDialogState.FINGERPRINT_ERROR, helpMessage)
    }

    /**
     * Resets the appearance of the dialog to its initial state (i.e. waiting for authentication).
     */
    @Suppress("WeakerAccess") /* synthetic access */
    private fun resetDialog() {
        fingerprintDialogModel.resetState(
            context.getString(R.string.fingerprint_dialog_touch_sensor)
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findActivity()
        else -> null
    }

/** Whether this context resolves to an [Activity] that is finishing or already destroyed. */
private fun Context.isHostActivityFinishing(): Boolean {
    val activity = findActivity() ?: return false
    return activity.isFinishing || activity.isDestroyed
}

/**
 * Whether a dialog can be attached to this context's window. An [Application] context has no
 * window; a finishing or destroyed [Activity] must not have dialogs attached to it.
 */
private fun Context.canShowDialog(): Boolean {
    if (isHostActivityFinishing()) {
        return false
    }
    return this !is Application
}

private fun LifecycleOwner.isChangingConfigurations(context: Context): Boolean {
    val hostActivity = context.findActivity()
    return when (this) {
        is Activity -> isChangingConfigurations
        is Fragment -> activity?.isChangingConfigurations ?: !isRemoving
        else -> hostActivity?.isChangingConfigurations == true
    }
}

/**
 * Gets the theme color corresponding to a given style attribute.
 *
 * @param attr The desired attribute.
 * @return The theme color for that attribute, or `null` if the attribute is not defined in this
 *   context's theme.
 */
private fun Context.getThemedColorOrNull(attr: Int): Int? {
    val arr = obtainStyledAttributes(intArrayOf(attr))
    try {
        return if (arr.hasValue(0)) arr.getColor(0, /* index */ 0 /* defValue */) else null
    } finally {
        arr.recycle()
    }
}

/**
 * Gets the delay in milliseconds to dismiss the dialog after an error.
 *
 * @return The delay in milliseconds.
 */
private fun Context?.getDismissDialogDelay(): Int {
    return if (this != null && DeviceUtils.shouldHideFingerprintDialog(this, Build.MODEL)) 0
    else FingerprintDialogController.HIDE_DIALOG_DELAY_MS
}

/** Represents the various UI states of the fingerprint dialog. */
internal enum class FingerprintDialogState {
    /** The dialog is not showing or is in a default state. */
    NONE,

    /** The dialog is actively listening for a fingerprint. */
    FINGERPRINT,

    /** The dialog is showing an error message. */
    FINGERPRINT_ERROR,

    /** The dialog is showing a success state after authentication. */
    FINGERPRINT_AUTHENTICATED,
}

/** Nested class to avoid verification errors for methods introduced in Android 8.0 (API 26). */
@RequiresApi(Build.VERSION_CODES.O)
internal object Api26Impl {
    val colorErrorAttr: Int
        /** Gets the resource ID of the AppCompat `colorError` style attribute. */
        get() {
            return androidx.appcompat.R.attr.colorError
        }

    val platformColorErrorAttr: Int
        /** Gets the resource ID of the platform `android:colorError` style attribute. */
        get() {
            return android.R.attr.colorError
        }
}

/** Nested class to avoid verification errors for methods introduced in Android 5.0 (API 21). */
internal object Api21ImplForFm {
    /**
     * Starts animating the given icon if it is an [AnimatedVectorDrawable].
     *
     * @param icon A [Drawable] icon asset.
     */
    fun startAnimation(icon: Drawable) {
        if (icon is AnimatedVectorDrawable) {
            icon.start()
        }
    }
}

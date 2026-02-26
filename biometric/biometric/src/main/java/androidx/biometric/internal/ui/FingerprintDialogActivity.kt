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

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.internal.data.CanceledFrom
import androidx.biometric.internal.isManagingDeviceCredentialButton
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.AuthenticationViewModelFactory
import androidx.biometric.internal.viewmodel.FingerprintDialogViewModel
import androidx.biometric.utils.AuthenticatorUtils
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.CryptoObjectUtils
import androidx.biometric.utils.DeviceUtils
import androidx.biometric.utils.ErrorUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A transparent [ComponentActivity] that hosts the fingerprint dialog UI for the legacy
 * `FingerprintManager` API.
 *
 * This activity is launched by
 * [androidx.biometric.internal.AuthenticationHandlerFingerprintManager] to display the fingerprint
 * prompt to the user. It is responsible for managing the lifecycle of the dialog and communicating
 * authentication events back to the handler via the shared [AuthenticationViewModel].
 *
 * This activity is responsible for initiating the authentication process by calling
 * `FingerprintManagerCompat.authenticate()` to prevent race conditions. By initiating the call,
 * this activity ensures it is the direct recipient of all framework callbacks from
 * `FingerprintManager`. This allows the UI to react immediately to events like errors or failures
 * without the delay of waiting for the handler to process the event and update the ViewModel, which
 * could otherwise lead to inconsistent UI states.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FingerprintDialogActivity : ComponentActivity() {
    internal companion object {
        @VisibleForTesting var fingerprintDialogViewModelFactory: ViewModelProvider.Factory? = null

        /** The delay in milliseconds to hide the dialog after an error. */
        const val HIDE_DIALOG_DELAY_MS = 2000
        const val DISMISS_INSTANTLY_DELAY_MS = 500L
        private const val MESSAGE_DISPLAY_TIME_MS = 2000L
        private const val TAG = "FingerprintDialogController"
    }

    /** The currently displayed fingerprint dialog. */
    private var fingerprintDialog: AlertDialog? = null
    private var resetDialogJob: Job? = null

    private var fingerprintIcon: ImageView? = null
    private var helpMessageView: TextView? = null
    private var errorTextColor = 0
    private var normalTextColor = 0

    /** The ViewModel for managing the state of the fingerprint dialog UI. */
    private val fingerprintDialogViewModel: FingerprintDialogViewModel by viewModels {
        fingerprintDialogViewModelFactory ?: defaultViewModelProviderFactory
    }

    /** The ViewModel for the authentication. */
    private val authenticationViewModel: AuthenticationViewModel by viewModels {
        AuthenticationViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        errorTextColor =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getThemedColorFor(Api26Impl.colorErrorAttr)
            } else {
                ContextCompat.getColor(this, R.color.biometric_error_color)
            }
        normalTextColor = getThemedColorFor(android.R.attr.textColorSecondary)

        showAlertDialog()
        connectObservers()

        if (savedInstanceState == null) {
            showAuthentication()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        destroyAlertDialog()

        if (!isChangingConfigurations) {
            cancelAuthentication(CanceledFrom.INTERNAL)
        }
    }

    /**
     * Creates and shows the fingerprint [AlertDialog].
     *
     * @return The created dialog, or `null` if the dialog should be hidden on the current device.
     */
    private fun showAlertDialog(): AlertDialog? {
        if (DeviceUtils.shouldHideFingerprintDialog(this, Build.MODEL)) {
            return null
        }

        // For showing error message
        lifecycleScope.launch {
            delay(DISMISS_INSTANTLY_DELAY_MS)
            fingerprintDialogViewModel.isDismissedInstantly = false
        }

        val builder = AlertDialog.Builder(this)
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

        val negativeButtonText =
            if (
                AuthenticatorUtils.isDeviceCredentialAllowed(
                    authenticationViewModel.allowedAuthenticators
                )
            ) {
                getString(R.string.confirm_device_credential_password)
            } else {
                authenticationViewModel.negativeButtonText
            }
        builder.setNegativeButton(negativeButtonText) { _, _ ->
            authenticationViewModel.setNegativeButtonPressPending()
        }

        builder.setView(layout)
        fingerprintDialog = builder.create()
        fingerprintDialog?.apply {
            setCanceledOnTouchOutside(false)
            setOnCancelListener { _ -> fingerprintDialogViewModel.setCancelPending(true) }
            show()
        }
        resetDialog()

        return fingerprintDialog
    }

    /** Destroys the currently shown fingerprint dialog. */
    private fun destroyAlertDialog() {
        fingerprintDialog?.let {
            try {
                it.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing fingerprint dialog", e)
            } finally {
                fingerprintDialog = null
            }
        }

        // Always set this to true. In case the user tries to authenticate again
        // the UI will not be shown.
        fingerprintDialogViewModel.isDismissedInstantly = true
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
                BiometricErrorData(errorCode, ErrorUtils.getFingerprintErrorString(this, errorCode))
            )
        }
        authenticationViewModel.cancellationSignalProvider.cancel()
    }

    /** Connects observers to the ViewModels to react to state changes. */
    private fun connectObservers() {
        lifecycleScope.launch {
            fingerprintDialogViewModel.isCancelPending.collect { isCancelPending ->
                if (isCancelPending) {
                    cancelAuthentication(CanceledFrom.USER)
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            fingerprintDialogViewModel.state.collect { currentState ->
                if (
                    fingerprintDialogViewModel.previousState != FingerprintDialogState.NONE ||
                        currentState != FingerprintDialogState.FINGERPRINT
                ) {
                    resetDialogJob?.cancel()
                    resetDialogJob =
                        lifecycleScope.launch {
                            delay(MESSAGE_DISPLAY_TIME_MS)
                            resetDialog()
                            resetDialogJob = null
                        }
                }
            }
        }

        lifecycleScope.launch {
            fingerprintDialogViewModel.drawableResId.collect { resId ->
                if (resId != 0) {
                    val icon = ContextCompat.getDrawable(this@FingerprintDialogActivity, resId)
                    fingerprintIcon?.setImageDrawable(icon)
                    if (fingerprintDialogViewModel.shouldAnimateIcon.value) {
                        icon?.let { Api21ImplForFm.startAnimation(it) }
                    }
                }
            }
        }

        lifecycleScope.launch {
            fingerprintDialogViewModel.helpMessageInfo.collect { (helpMessage, shouldUseErrorColor)
                ->
                if (helpMessageView?.text != helpMessage) {
                    helpMessageView?.text = helpMessage
                    helpMessageView?.setTextColor(
                        if (shouldUseErrorColor) errorTextColor else normalTextColor
                    )
                }
            }
        }

        lifecycleScope.launch {
            authenticationViewModel.authenticationHelpMessage.collect { help ->
                help?.let { showFingerprintErrorMessage(help) }
            }
        }

        lifecycleScope.launch {
            authenticationViewModel.isNegativeButtonPressPending.collect { finish() }
        }

        lifecycleScope.launch { authenticationViewModel.authenticationResult.collect { finish() } }

        lifecycleScope.launch {
            authenticationViewModel.authenticationError.collect { error ->
                val knownErrorCode = ErrorUtils.toKnownErrorCodeForAuthenticate(error.errorCode)
                // Define the special cases where we should NOT show an error message.
                val isLockoutHandledByButton =
                    ErrorUtils.isLockoutError(knownErrorCode) &&
                        isManagingDeviceCredentialButton(
                            authenticationViewModel.allowedAuthenticators
                        )

                val isCanceled = knownErrorCode == BiometricPrompt.ERROR_CANCELED

                // Only show the error message if it's not one of the special cases
                // and the dialog is not already being dismissed instantly.
                if (
                    !isLockoutHandledByButton &&
                        !isCanceled &&
                        !fingerprintDialogViewModel.isDismissedInstantly
                ) {
                    showFingerprintErrorMessage(error.errorMessage)
                    lifecycleScope.launch {
                        delay(getDismissDialogDelay().toLong())
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            authenticationViewModel.isAuthenticationFailurePending.collect { _ ->
                showFingerprintErrorMessage(getString(R.string.fingerprint_not_recognized))
            }
        }
    }

    /**
     * Checks for pre-authentication errors and starts the authentication process if none are found.
     */
    private fun showAuthentication() {
        @Suppress("deprecation")
        val fingerprintManagerCompat =
            androidx.biometric.internal.FingerprintManagerCompat.from(applicationContext)
        val errorCode =
            fingerprintDialogViewModel.fingerprintPreAuthChecker(fingerprintManagerCompat)
        if (errorCode != BiometricPrompt.BIOMETRIC_SUCCESS) {
            authenticationViewModel.setAuthenticationError(
                BiometricErrorData(
                    errorCode,
                    ErrorUtils.getFingerprintErrorString(applicationContext, errorCode),
                )
            )
            return
        }

        fingerprintDialogViewModel.isDismissedInstantly = true
        authenticateWithFingerprint(fingerprintManagerCompat, applicationContext)
    }

    /**
     * Requests user authentication with the given fingerprint manager.
     *
     * @param fingerprintManager The fingerprint manager that will be used for authentication.
     * @param context The application context.
     */
    @Suppress("deprecation")
    private fun authenticateWithFingerprint(
        fingerprintManager: androidx.biometric.internal.FingerprintManagerCompat,
        context: Context,
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
                BiometricErrorData(errorCode, ErrorUtils.getFingerprintErrorString(this, errorCode))
            )
        }
    }

    /**
     * Shows an error message on the fingerprint dialog.
     *
     * @param errorMessage The error message to display.
     */
    private fun showFingerprintErrorMessage(errorMessage: CharSequence?) {
        val helpMessage = errorMessage ?: getString(R.string.default_error_msg)
        fingerprintDialogViewModel.setState(FingerprintDialogState.FINGERPRINT_ERROR, helpMessage)
    }

    /**
     * Resets the appearance of the dialog to its initial state (i.e. waiting for authentication).
     */
    @Suppress("WeakerAccess") /* synthetic access */
    private fun resetDialog() {
        fingerprintDialogViewModel.resetState(getString(R.string.fingerprint_dialog_touch_sensor))
    }
}

/**
 * Gets the theme color corresponding to a given style attribute.
 *
 * @param attr The desired attribute.
 * @return The theme color for that attribute.
 */
private fun Context.getThemedColorFor(attr: Int): Int {
    val tv = TypedValue()
    val theme = getTheme()
    theme.resolveAttribute(attr, tv, true /* resolveRefs */)
    val arr = obtainStyledAttributes(tv.data, intArrayOf(attr))

    val color = arr.getColor(0, /* index */ 0 /* defValue */)
    arr.recycle()
    return color
}

/**
 * Gets the delay in milliseconds to dismiss the dialog after an error.
 *
 * @return The delay in milliseconds.
 */
private fun Context?.getDismissDialogDelay(): Int {
    return if (this != null && DeviceUtils.shouldHideFingerprintDialog(this, Build.MODEL)) 0
    else FingerprintDialogActivity.HIDE_DIALOG_DELAY_MS
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
        /** Gets the resource ID of the `colorError` style attribute. */
        get() {
            return androidx.appcompat.R.attr.colorError
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

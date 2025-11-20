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

import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.internal.ui.FingerprintDialogState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter

/** A [ViewModel] that manages the UI state of the fingerprint dialog. */
internal class FingerprintDialogViewModel(
    @Suppress("deprecation")
    val fingerprintPreAuthChecker: (androidx.biometric.internal.FingerprintManagerCompat) -> Int =
        ::checkForFingerprintPreAuthenticationErrors
) : ViewModel() {

    /** Whether the fingerprint dialog should always be dismissed instantly. */
    var isDismissedInstantly: Boolean = true

    private val _isCancelPending = MutableStateFlow(false)
    /**
     * A flow that emits `true` when the user has manually canceled out of the fingerprint dialog.
     */
    val isCancelPending: Flow<Boolean> = _isCancelPending.filter { it }

    /** The previous state of the fingerprint dialog UI. */
    private var _previousState: FingerprintDialogState = FingerprintDialogState.NONE
    val previousState: FingerprintDialogState
        get() = _previousState

    private val _state = MutableStateFlow(FingerprintDialogState.NONE)
    /** The current state of the fingerprint dialog UI. */
    val state: StateFlow<FingerprintDialogState> = _state

    private val _helpMessage = MutableStateFlow<Pair<CharSequence, Boolean>>(Pair("", false))
    /**
     * Pair( CharSequence: A human-readable message to be displayed below the icon on the
     * fingerprint dialog, Boolean : Whether the error text color should be used for the help
     * message)
     */
    val helpMessageInfo: StateFlow<Pair<CharSequence, Boolean>> = _helpMessage

    private val _drawableResId = MutableStateFlow(0)
    /** The resource ID of the drawable to be shown. */
    val drawableResId: StateFlow<Int> = _drawableResId

    private val _shouldAnimateIcon = MutableStateFlow(false)
    /** Whether the icon should be animated. */
    val shouldAnimateIcon: StateFlow<Boolean> = _shouldAnimateIcon

    /**
     * Sets the value for [isCancelPending].
     *
     * @param cancelPending `true` if a user-initiated cancellation is pending.
     */
    fun setCancelPending(cancelPending: Boolean) {
        _isCancelPending.value = cancelPending
    }

    /**
     * Sets the value for [state].
     *
     * @param newState The new UI state for the dialog.
     * @param newHelpMessage The new help message to be displayed.
     */
    fun setState(newState: FingerprintDialogState, newHelpMessage: CharSequence) {
        _state.value = newState
        _helpMessage.value =
            Pair(newHelpMessage, newState == FingerprintDialogState.FINGERPRINT_ERROR)
        _drawableResId.value = getAssetForTransition(previousState, newState)
        _shouldAnimateIcon.value = shouldAnimateForTransition(previousState, newState)

        // Set previous state after updating UI values above.
        _previousState = newState
    }

    /**
     * Resets the value for [state].
     *
     * @param defaultHelpMessage The default help message to be displayed.
     */
    fun resetState(defaultHelpMessage: CharSequence) {
        _previousState = FingerprintDialogState.NONE
        setState(FingerprintDialogState.FINGERPRINT, defaultHelpMessage)
    }

    /**
     * Checks if the fingerprint icon should animate when transitioning between dialog states.
     *
     * @param previousState The previous state for the fingerprint dialog.
     * @param state The new state for the fingerprint dialog.
     * @return Whether the fingerprint icon should animate.
     */
    private fun shouldAnimateForTransition(
        previousState: FingerprintDialogState,
        state: FingerprintDialogState,
    ): Boolean {
        if (
            previousState == FingerprintDialogState.NONE &&
                state == FingerprintDialogState.FINGERPRINT
        ) {
            return false
        } else if (
            previousState == FingerprintDialogState.FINGERPRINT &&
                state == FingerprintDialogState.FINGERPRINT_ERROR
        ) {
            return true
        } else if (
            previousState == FingerprintDialogState.FINGERPRINT_ERROR &&
                state == FingerprintDialogState.FINGERPRINT
        ) {
            return true
        } else if (
            previousState == FingerprintDialogState.FINGERPRINT &&
                state == FingerprintDialogState.FINGERPRINT_AUTHENTICATED
        ) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            return false
        }
        return false
    }

    /**
     * Gets the icon or animation asset that should appear when transitioning between dialog states.
     *
     * @param previousState The previous state for the fingerprint dialog.
     * @param state The new state for the fingerprint dialog.
     * @return A drawable resource ID to be used for the fingerprint icon.
     */
    private fun getAssetForTransition(
        previousState: FingerprintDialogState,
        state: FingerprintDialogState,
    ): Int {
        return if (
            previousState == FingerprintDialogState.NONE &&
                state == FingerprintDialogState.FINGERPRINT
        ) {
            R.drawable.fingerprint_dialog_fp_icon
        } else if (
            previousState == FingerprintDialogState.FINGERPRINT &&
                state == FingerprintDialogState.FINGERPRINT_ERROR
        ) {
            R.drawable.fingerprint_dialog_error
        } else if (
            previousState == FingerprintDialogState.FINGERPRINT_ERROR &&
                state == FingerprintDialogState.FINGERPRINT
        ) {
            R.drawable.fingerprint_dialog_fp_icon
        } else if (
            previousState == FingerprintDialogState.FINGERPRINT &&
                state == FingerprintDialogState.FINGERPRINT_AUTHENTICATED
        ) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            R.drawable.fingerprint_dialog_fp_icon
        } else {
            0
        }
    }
}

/**
 * Checks for possible error conditions prior to starting fingerprint authentication.
 *
 * @return 0 if there is no error, or a nonzero integer identifying the specific error.
 */
@Suppress("deprecation")
private fun checkForFingerprintPreAuthenticationErrors(
    fingerprintManager: androidx.biometric.internal.FingerprintManagerCompat
): Int {
    if (!fingerprintManager.isHardwareDetected) {
        return BiometricPrompt.ERROR_HW_NOT_PRESENT
    } else if (!fingerprintManager.hasEnrolledFingerprints()) {
        return BiometricPrompt.ERROR_NO_BIOMETRICS
    }
    return BiometricPrompt.BIOMETRIC_SUCCESS
}

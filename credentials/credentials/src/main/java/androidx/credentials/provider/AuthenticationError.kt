/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.credentials.provider

import android.hardware.biometrics.BiometricPrompt
import android.util.Log
import androidx.annotation.RestrictTo
import java.util.Objects
import org.jetbrains.annotations.VisibleForTesting

/**
 * Error returned from the Biometric Prompt flow that is executed by
 * [androidx.credentials.CredentialManager] after the user makes a selection on the Credential
 * Manager account selector.
 *
 * @property errorCode the error code denoting what kind of error was encountered while the
 *   biometric prompt flow failed, must be one of the error codes defined in
 *   [androidx.biometric.BiometricPrompt] such as
 *   [androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE] or
 *   [androidx.biometric.BiometricPrompt.ERROR_TIMEOUT]
 * @property errorMsg the message associated with the [errorCode] in the form that can be displayed
 *   on a UI.
 * @see AuthenticationErrorTypes
 */
class AuthenticationError
@JvmOverloads
constructor(
    val errorCode: @AuthenticationErrorTypes Int,
    val errorMsg: CharSequence? = null,
) {
    internal companion object {
        internal val TAG = "AuthenticationError"
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_ERROR =
            "androidx.credentials.provider.BIOMETRIC_AUTH_ERROR_CODE"
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_ERROR_FALLBACK = "BIOMETRIC_AUTH_ERROR_CODE"
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE =
            "androidx.credentials.provider.BIOMETRIC_AUTH_ERROR_MESSAGE"
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE_FALLBACK = "BIOMETRIC_AUTH_ERROR_MESSAGE"
        // The majority of this is unexpected to be sent, or the values are equal,
        // but should it arrive for any reason, is handled properly. This way
        // providers can be confident the Jetpack codes alone are enough.
        @VisibleForTesting
        internal val biometricFrameworkToJetpackErrorMap =
            linkedMapOf(
                BiometricPrompt.BIOMETRIC_ERROR_CANCELED to
                    androidx.biometric.BiometricPrompt.ERROR_CANCELED,
                BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT to
                    androidx.biometric.BiometricPrompt.ERROR_HW_NOT_PRESENT,
                BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE to
                    androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE,
                BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT to
                    androidx.biometric.BiometricPrompt.ERROR_LOCKOUT,
                BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT to
                    androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS to
                    androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS,
                BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL to
                    androidx.biometric.BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                BiometricPrompt.BIOMETRIC_ERROR_NO_SPACE to
                    androidx.biometric.BiometricPrompt.ERROR_NO_SPACE,
                BiometricPrompt.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED to
                    androidx.biometric.BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
                BiometricPrompt.BIOMETRIC_ERROR_TIMEOUT to
                    androidx.biometric.BiometricPrompt.ERROR_TIMEOUT,
                BiometricPrompt.BIOMETRIC_ERROR_UNABLE_TO_PROCESS to
                    androidx.biometric.BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED to
                    androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.BIOMETRIC_ERROR_VENDOR to
                    androidx.biometric.BiometricPrompt.ERROR_VENDOR
                // TODO(b/340334264) : Add NEGATIVE_BUTTON from FW once avail, or wrap this in
                // a credential manager specific error.
            )

        internal fun convertFrameworkBiometricErrorToJetpack(frameworkCode: Int): Int {
            // Ignoring getOrDefault to allow this object down to API 21
            return if (biometricFrameworkToJetpackErrorMap.containsKey(frameworkCode)) {
                biometricFrameworkToJetpackErrorMap[frameworkCode]!!
            } else {
                Log.i(TAG, "Unexpected error code, $frameworkCode, ")
                frameworkCode
            }
        }

        /**
         * Generates an instance of this class, to be called by an UI consumer that calls
         * [BiometricPrompt] API and needs the result to be wrapped by this class. The caller of
         * this API must specify whether the framework [android.hardware.biometrics.BiometricPrompt]
         * API or the jetpack [androidx.biometric.BiometricPrompt] API is used through
         * [isFrameworkBiometricPrompt].
         *
         * @param uiErrorCode the error code used to create this error instance, typically using the
         *   [androidx.biometric.BiometricPrompt]'s constants if conversion isn't desired, or
         *   [android.hardware.biometrics.BiometricPrompt]'s constants if conversion *is* desired.
         * @param uiErrorMessage the message associated with the [uiErrorCode] in the form that can
         *   be displayed on a UI.
         * @param isFrameworkBiometricPrompt the bit indicating whether or not this error code
         *   requires conversion or not, set to true by default
         * @return an authentication error that has properly handled conversion of the err code
         */
        @JvmOverloads
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        internal fun createFrom(
            uiErrorCode: Int,
            uiErrorMessage: CharSequence,
            isFrameworkBiometricPrompt: Boolean = true,
        ): AuthenticationError =
            AuthenticationError(
                errorCode =
                    if (isFrameworkBiometricPrompt)
                        convertFrameworkBiometricErrorToJetpack(uiErrorCode)
                    else uiErrorCode,
                errorMsg = uiErrorMessage,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is AuthenticationError) {
            return this.errorCode == other.errorCode && this.errorMsg == other.errorMsg
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(this.errorCode, this.errorMsg)
    }
}

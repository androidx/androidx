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
import androidx.credentials.provider.AuthenticationError.Companion.TAG
import java.util.Objects
import org.jetbrains.annotations.VisibleForTesting

/**
 * Successful result returned from the Biometric Prompt authentication flow handled by
 * [androidx.credentials.CredentialManager].
 *
 * @property authenticationType the type of authentication (e.g. device credential or biometric)
 *   that was requested from and successfully provided by the user, corresponds to constants defined
 *   in [androidx.biometric.BiometricPrompt] such as
 *   [androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC] or
 *   [androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL]
 */
class AuthenticationResult(
    val authenticationType: @AuthenticationResultTypes Int,
) {
    internal companion object {
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_RESULT_TYPE =
            "androidx.credentials.provider.BIOMETRIC_AUTH_RESULT"
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_RESULT_TYPE_FALLBACK = "BIOMETRIC_AUTH_RESULT"
        @VisibleForTesting
        internal val biometricFrameworkToJetpackResultMap =
            linkedMapOf(
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC to
                    androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
                BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL to
                    androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL,
                // TODO(b/340334264) : Add TYPE_UNKNOWN once avail from fw, though unexpected unless
                // very low API level, and may be ignored until jp only impl added in QPR, or other
                // ctr can be used directly once avail/ready
            )

        internal fun convertFrameworkBiometricResultToJetpack(frameworkCode: Int): Int {
            // Ignoring getOrDefault to allow this object down to API 21
            return if (biometricFrameworkToJetpackResultMap.containsKey(frameworkCode)) {
                biometricFrameworkToJetpackResultMap[frameworkCode]!!
            } else {
                Log.i(TAG, "Non framework result code, $frameworkCode, ")
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
         * @param uiAuthenticationType the type of authentication (e.g. device credential or
         *   biometric) that was requested from and successfully provided by the user, corresponds
         *   to constants defined in [androidx.biometric.BiometricPrompt] if conversion is not
         *   desired, or in [android.hardware.biometrics.BiometricPrompt] if conversion is desired
         * @param isFrameworkBiometricPrompt the bit indicating whether or not this error code
         *   requires conversion or not, set to true by default
         * @return an authentication result that has properly handled conversion of the result types
         */
        @JvmOverloads
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        internal fun createFrom(
            uiAuthenticationType: Int,
            isFrameworkBiometricPrompt: Boolean = true,
        ): AuthenticationResult =
            AuthenticationResult(
                authenticationType =
                    if (isFrameworkBiometricPrompt)
                        convertFrameworkBiometricResultToJetpack(uiAuthenticationType)
                    else uiAuthenticationType
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is AuthenticationResult) {
            return this.authenticationType == other.authenticationType
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(this.authenticationType)
    }
}

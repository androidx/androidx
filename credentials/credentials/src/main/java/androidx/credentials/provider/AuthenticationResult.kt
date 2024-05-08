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

import androidx.annotation.RestrictTo
import java.util.Objects
import org.jetbrains.annotations.VisibleForTesting

/**
 * Successful result returned from the Biometric Prompt authentication
 * flow handled by [androidx.credentials.CredentialManager].
 *
 * @property authenticationType the type of authentication (e.g. device credential or biometric)
 * that was requested from and successfully provided by the user, corresponds to
 * constants defined in [android.hardware.biometrics.BiometricManager] such as
 * [android.hardware.biometrics.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC]
 * or
 * [android.hardware.biometrics.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthenticationResult(val authenticationType: Int) {

    companion object {
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_RESULT_TYPE =
            "BIOMETRIC_AUTH_RESULT"
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

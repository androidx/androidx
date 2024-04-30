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
 * Error returned from the Biometric Prompt flow that is executed
 * by [androidx.credentials.CredentialManager] after the user
 * makes a selection on the Credential Manager account selector.
 *
 * @property errorCode the error code denoting what kind of error
 * was encountered while the biometric prompt flow failed, must
 * be one of the error codes defined in
 * [android.hardware.biometrics.BiometricManager] such as
 * [android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED]
 * or
 * [android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE]
 * @property errorMsg the message associated with the [errorCode] in the
 * form that can be displayed on a UI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthenticationError @JvmOverloads constructor(
    val errorCode: Int,
    val errorMsg: CharSequence? = null
) {
    companion object {
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_ERROR = "BIOMETRIC_AUTH_ERROR"
        @VisibleForTesting
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        const val EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE = "EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other is AuthenticationError) {
            return this.errorCode == other.errorCode &&
                this.errorMsg == other.errorMsg
        }
        return false
    }

    override fun hashCode(): Int {
        return Objects.hash(this.errorCode, this.errorMsg)
    }
}

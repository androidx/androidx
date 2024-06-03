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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_HW_NOT_PRESENT
import androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS
import androidx.biometric.BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.ERROR_NO_SPACE
import androidx.biometric.BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED
import androidx.biometric.BiometricPrompt.ERROR_TIMEOUT
import androidx.biometric.BiometricPrompt.ERROR_UNABLE_TO_PROCESS
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_VENDOR

/**
 * This acts as a parameter hint for what [BiometricPrompt]'s error constants should be. You can
 * learn more about the constants from [BiometricPrompt] to utilize best practices.
 *
 * @see BiometricPrompt
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            ERROR_CANCELED,
            ERROR_HW_NOT_PRESENT,
            ERROR_HW_UNAVAILABLE,
            ERROR_LOCKOUT,
            ERROR_LOCKOUT_PERMANENT,
            ERROR_NO_BIOMETRICS,
            ERROR_NO_DEVICE_CREDENTIAL,
            ERROR_NO_SPACE,
            ERROR_SECURITY_UPDATE_REQUIRED,
            ERROR_TIMEOUT,
            ERROR_UNABLE_TO_PROCESS,
            ERROR_USER_CANCELED,
            ERROR_VENDOR
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class AuthenticationErrorTypes

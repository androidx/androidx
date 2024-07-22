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
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN

/**
 * This acts as a parameter hint for what [BiometricPrompt]'s result constants should be. You can
 * learn more about the constants from [BiometricPrompt] to utilize best practices.
 *
 * @see BiometricPrompt
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    value =
        [
            AUTHENTICATION_RESULT_TYPE_BIOMETRIC,
            AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL,
            AUTHENTICATION_RESULT_TYPE_UNKNOWN
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class AuthenticationResultTypes

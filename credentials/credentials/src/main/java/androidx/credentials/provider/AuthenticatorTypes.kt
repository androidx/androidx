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

import android.hardware.biometrics.BiometricManager
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * This allows verification when users pass in [BiometricManager.Authenticators] constants; namely
 * we can have a parameter hint that indicates what they should be. You can learn more about the
 * constants from [BiometricManager.Authenticators] to utilize best practices.
 *
 * @see BiometricManager.Authenticators
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(value = [BIOMETRIC_STRONG, BIOMETRIC_WEAK, DEVICE_CREDENTIAL])
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class AuthenticatorTypes

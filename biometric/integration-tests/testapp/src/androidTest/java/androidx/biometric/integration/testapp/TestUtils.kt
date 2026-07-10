/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric.integration.testapp

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS

/** Checks [context] to determine if the device has an enrolled biometric authentication method. */
internal fun hasEnrolledBiometric(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS
}

/** Checks [context] to determine if the device is currently locked. */
internal fun isDeviceLocked(context: Context): Boolean {
    val keyguard = context.getSystemService(KeyguardManager::class.java)

    return keyguard?.isDeviceLocked ?: false
}

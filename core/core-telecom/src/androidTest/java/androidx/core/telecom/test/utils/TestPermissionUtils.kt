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

package androidx.core.telecom.test.utils

import android.Manifest
import android.os.Build
import androidx.test.rule.GrantPermissionRule

/** A utility object for creating permission rules for Android tests. */
object TestPermissionUtils {

    /**
     * Creates a GrantPermissionRule that requests the appropriate Bluetooth permissions based on
     * the current Android SDK version.
     *
     * @return A GrantPermissionRule configured with the necessary Bluetooth permissions.
     */
    fun createBluetoothPermissionRule(): GrantPermissionRule {
        // Define permissions for Android 12 (API 31) and above
        val modernPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                // For older SDKs, the array is empty as specific permissions are not needed
                // beyond the legacy ones, which are often handled differently.
                emptyArray()
            }

        // Define legacy permissions for older versions
        val legacyPermissions =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH)
            } else {
                emptyArray()
            }

        // Combine and grant all necessary permissions
        val allPermissions = modernPermissions + legacyPermissions
        return GrantPermissionRule.grant(*allPermissions)
    }
}

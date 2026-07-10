/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.testutils

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Filters the given permissions list to only those declared/registered on the host system's
 * PackageManager. This allows XR-specific signature permissions to be gracefully skipped on
 * standard phone and tablet devices where they do not exist.
 */
public fun filterSupportedPermissions(vararg permissions: String): Array<String> {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val packageManager = context.packageManager
    return permissions
        .filter { permission ->
            try {
                packageManager.getPermissionInfo(permission, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
        .toTypedArray()
}

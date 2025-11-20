/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.camera.camera2.pipe.config.CameraPipeContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This tracks internal permission requests to avoid querying multiple times.
 *
 * This class assumes that permissions are one way - They can be granted, but not un-granted without
 * restarting the application process.
 */
@Singleton
internal class Permissions
@Inject
constructor(@CameraPipeContext private val cameraPipeContext: Context) {
    @Volatile private var _hasCameraPermission = false
    val hasCameraPermission: Boolean
        get() = checkCameraPermission()

    private fun checkCameraPermission(): Boolean {
        if (Build.FINGERPRINT == "robolectric") {
            // If we're running under Robolectric, assume we have camera permission since
            // Robolectric doesn't seem to stub out the self permission calls properly.
            // See b/422237649 for details.
            return true
        }

        // Granted camera permission is cached here to reduce the number of binder transactions
        // executed.  This is considered okay because when a user revokes a permission at runtime,
        // Android's PermissionManagerService kills the app via the onPermissionRevoked callback,
        // allowing the code to avoid re-querying after checkSelfPermission returns true.
        if (!_hasCameraPermission) {
            Debug.traceStart { "CXCP#checkCameraPermission" }
            if (
                cameraPipeContext.checkSelfPermission(Manifest.permission.CAMERA) ==
                    PERMISSION_GRANTED
            ) {
                _hasCameraPermission = true
            }
            Debug.traceStop()
        }
        return _hasCameraPermission
    }
}

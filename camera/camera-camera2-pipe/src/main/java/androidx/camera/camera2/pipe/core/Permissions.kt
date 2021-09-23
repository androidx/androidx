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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.compat.Api23Compat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This tracks internal permission requests to avoid querying multiple times.
 *
 * This class assumes that permissions are one way - They can be granted, but not un-granted
 * without restarting the application process.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
internal class Permissions @Inject constructor(private val context: Context) {
    @Volatile
    private var _hasCameraPermission = false
    val hasCameraPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkCameraPermission()
        } else {
            // On older versions of Android, permissions are required in order to install a package
            // and so the permission check is redundant.
            true
        }

    @RequiresApi(23)
    private fun checkCameraPermission(): Boolean {
        // Granted camera permission is cached here to reduce the number of binder transactions
        // executed.  This is considered okay because when a user revokes a permission at runtime,
        // Android's PermissionManagerService kills the app via the onPermissionRevoked callback,
        // allowing the code to avoid re-querying after checkSelfPermission returns true.
        if (!_hasCameraPermission) {
            Debug.traceStart { "CXCP#checkCameraPermission" }
            if (Api23Compat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PERMISSION_GRANTED
            ) {
                _hasCameraPermission = true
            }
            Debug.traceStop()
        }
        return _hasCameraPermission
    }
}
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

package androidx.camera.camera2.pipe.impl

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides caching and querying of CameraMetadata.
 *
 * This class is designed to be thread safe and provides suspend functions for querying and
 * accessing CameraMetadata.
 */
@Singleton
class CameraMetadataCache @Inject constructor(
    private val context: Context
) {
    @GuardedBy("cache")
    private val cache = ArrayMap<String, CameraMetadata>()

    @Volatile
    private var hasCameraPermission = false

    suspend fun get(cameraId: CameraId): CameraMetadata {
        synchronized(cache) {
            val existing = cache[cameraId.value]
            if (existing != null) {
                return existing
            }
        }

        // Suspend and query CameraMetadata on a background thread.
        return withContext(Dispatchers.IO) {
            awaitMetadata(cameraId)
        }
    }

    fun awaitMetadata(cameraId: CameraId): CameraMetadata {
        return Debug.trace("awaitMetadata") {
            synchronized(cache) {
                val existing = cache[cameraId.value]
                if (existing != null) {
                    return@trace existing
                } else if (!isMetadataRedacted()) {
                    val result = createCameraMetadata(cameraId, false)
                    cache[cameraId.value] = result
                    return@trace result
                }
            }
            return@trace createCameraMetadata(cameraId, true)
        }
    }

    private fun createCameraMetadata(cameraId: CameraId, redacted: Boolean): CameraMetadataImpl {
        val cameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics =
            cameraManager.getCameraCharacteristics(cameraId.value)
        return CameraMetadataImpl(cameraId, redacted, characteristics, emptyMap())
    }

    private fun isMetadataRedacted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Some CameraCharacteristic properties are redacted on Q or higher if the application
            // does not currently hold the CAMERA permission.
            return !checkCameraPermission()
        }
        return false
    }

    @RequiresApi(23)
    private fun checkCameraPermission(): Boolean {
        // Granted camera permission is cached here to reduce the number of binder transactions
        // executed.  This is considered okay because when a user revokes a permission at runtime,
        // Android's PermissionManagerService kills the app via the onPermissionRevoked callback,
        // allowing the code to avoid re-querying after checkSelfPermission returns true.
        if (!hasCameraPermission &&
            context.checkSelfPermission(permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) {
            hasCameraPermission = true
        }
        return hasCameraPermission
    }
}

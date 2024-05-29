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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraAccessException
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.internal.CameraErrorListener

/**
 * Thrown when an operation cannot be executed because underlying object is closed or in an unusable
 * state.
 */
internal class ObjectUnavailableException(e: Throwable) : Exception(e)

/** Catch specific exceptions that are not normally thrown, log them, then rethrow. */
@Throws(ObjectUnavailableException::class)
internal inline fun <T> catchAndReportCameraExceptions(
    cameraId: CameraId,
    cameraErrorListener: CameraErrorListener,
    crossinline block: () -> T
): T? {
    // Camera2 has, at different points in time, thrown a large number of checked and/or
    // unchecked exceptions under different circumstances that are not listed in the
    // documentation. This method catches and recasts these exceptions into a common exception
    // type.
    //
    // Specific examples:
    // * Some exceptions (such as IllegalArgumentException) can happen if a surface is destroyed
    //   out of band during configuration.
    // * Some exceptions (such as IllegalStateException) can be thrown but are not reported as
    //   being thrown by various methods on some versions of the OS.
    // * Some exceptions (such as SecurityException) can happen even when the application has
    //   permission to access the camera but a higher priority or security sensitive service is
    //   currently using the camera.
    // * Some exceptions (such as UnsupportedOperationException) can be thrown on some versions
    //   of the OS (b/28617016)
    try {
        return block()
    } catch (e: Exception) {
        Log.warn { "Unexpected error: " + e.message }
        when (e) {
            is CameraAccessException -> {
                cameraErrorListener.onCameraError(
                    cameraId,
                    CameraError.from(e),
                    // CameraAccessException indicates the task failed because the camera is
                    // unavailable, such as when the camera is in use or disconnected. Such errors
                    // can be recovered when the camera becomes available.
                    willAttemptRetry = true,
                )
                return null
            }
            is IllegalArgumentException,
            is IllegalStateException,
            is SecurityException,
            is UnsupportedOperationException,
            is NullPointerException -> {
                cameraErrorListener.onCameraError(
                    cameraId,
                    CameraError.ERROR_GRAPH_CONFIG,
                    willAttemptRetry = false
                )
                return null
            }
            else -> throw e
        }
    }
}

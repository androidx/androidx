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

package androidx.camera.integration.extensions.utils

import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector

object CameraSelectorUtil {

    @JvmStatic
    @OptIn(ExperimentalCamera2Interop::class)
    fun createCameraSelectorById(cameraId: String) =
        CameraSelector.Builder().addCameraFilter(CameraFilter { cameraInfos ->
            cameraInfos.forEach {
                if (Camera2CameraInfo.from(it).cameraId.equals(cameraId)) {
                    return@CameraFilter listOf<CameraInfo>(it)
                }
            }

            throw IllegalArgumentException("No camera can be find for id: $cameraId")
        }).build()
}
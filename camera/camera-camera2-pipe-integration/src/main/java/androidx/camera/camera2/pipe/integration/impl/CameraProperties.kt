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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.impl

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import javax.inject.Inject

/** Pre-computed camera properties */
interface CameraProperties {
    val cameraId: CameraId
    val metadata: CameraMetadata

    // TODO: Consider exposing additional properties, such as quirks.
}

@CameraScope
class CameraPipeCameraProperties @Inject constructor(
    private val cameraPipe: CameraPipe,
    private val cameraConfig: CameraConfig
) : CameraProperties {
    override val cameraId: CameraId
        get() = cameraConfig.cameraId
    override val metadata: CameraMetadata by lazy { cameraPipe.cameras().awaitMetadata(cameraId) }
}
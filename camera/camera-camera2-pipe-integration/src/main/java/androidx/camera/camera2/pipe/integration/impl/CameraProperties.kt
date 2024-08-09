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

package androidx.camera.camera2.pipe.integration.impl

import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import javax.inject.Inject

/** Pre-computed camera properties */
public interface CameraProperties {
    public val cameraId: CameraId
    public val metadata: CameraMetadata

    // TODO: Consider exposing additional properties, such as quirks.
}

@CameraScope
public class CameraPipeCameraProperties
@Inject
constructor(
    private val cameraConfig: CameraConfig,
    private val cameraMetadata: CameraMetadata?,
) : CameraProperties {
    override val cameraId: CameraId
        get() = cameraConfig.cameraId

    // TODO(b/270615090): Here it's safe to use the !! operator because all consumers of the
    //  metadata don't read it during CameraX initialization. Long term however, CameraProperties
    //  can probably be removed entirely.
    override val metadata: CameraMetadata = cameraMetadata!!
}

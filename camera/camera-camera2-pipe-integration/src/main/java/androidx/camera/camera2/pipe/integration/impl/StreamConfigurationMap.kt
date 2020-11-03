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

import android.content.Context
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.impl.Log.debug
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig

/**
 * Provide utilities for interacting with the set of guaranteed stream combinations.
 */
class StreamConfigurationMap(context: Context, cameraManager: Any?) : CameraDeviceSurfaceManager {
    private val cameraPipe: CameraPipe = cameraManager as CameraPipe

    init {
        debug { "Created StreamConfigurationMap from $context" }
    }

    override fun checkSupported(cameraId: String, surfaceConfigList: List<SurfaceConfig>): Boolean {
        // TODO: This method needs to check to see if the list of SurfaceConfig's is in the map of
        //   guaranteed stream configurations for this camera's support level.
        return cameraPipe.cameras().findAll().contains(CameraId(cameraId))
    }

    override fun transformSurfaceConfig(
        cameraId: String,
        imageFormat: Int,
        size: Size
    ): SurfaceConfig? {
        // TODO: Many of the "find a stream combination that will work" is already provided by the
        //   existing camera2 implementation, and this implementation should leverage that work.

        TODO("Not Implemented")
    }

    override fun getSuggestedResolutions(
        cameraId: String,
        existingSurfaces: List<SurfaceConfig>,
        newUseCaseConfigs: List<UseCaseConfig<*>?>
    ): Map<UseCaseConfig<*>, Size> {
        // TODO: Many of the "find a stream combination that will work" is already provided by the
        //   existing camera2 implementation, and this implementation should leverage that work.

        TODO("Not Implemented")
    }
}
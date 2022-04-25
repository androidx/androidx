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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.graphics.ImageFormat
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.UseCaseConfig

internal val MAXIMUM_PREVIEW_SIZE = Size(1920, 1080)

/**
 * Adapt the [CameraDeviceSurfaceManager] interface to [CameraPipe].
 *
 * This class provides Context-specific utility methods for querying and computing supported
 * outputs.
 */
class CameraSurfaceAdapter(
    context: Context,
    cameraComponent: Any?,
    availableCameraIds: Set<String>
) : CameraDeviceSurfaceManager {
    private val component = cameraComponent as CameraAppComponent

    init {
        debug { "AvailableCameraIds = $availableCameraIds" }
        debug { "Created StreamConfigurationMap from $context" }
    }

    override fun checkSupported(cameraId: String, surfaceConfigList: List<SurfaceConfig>): Boolean {
        // TODO: This method needs to check to see if the list of SurfaceConfig's is in the map of
        //   guaranteed stream configurations for this camera's support level.
        return component.getAvailableCameraIds().contains(cameraId)
    }

    override fun transformSurfaceConfig(
        cameraId: String,
        imageFormat: Int,
        size: Size
    ): SurfaceConfig? {
        // TODO: Many of the "find a stream combination that will work" is already provided by the
        //   existing camera2 implementation, and this implementation should leverage that work.

        val configType = when (imageFormat) {
            ImageFormat.YUV_420_888 -> SurfaceConfig.ConfigType.YUV
            ImageFormat.JPEG -> SurfaceConfig.ConfigType.JPEG
            ImageFormat.RAW_SENSOR -> SurfaceConfig.ConfigType.RAW
            else -> SurfaceConfig.ConfigType.PRIV
        }

        val configSize = ConfigSize.PREVIEW
        return SurfaceConfig.create(configType, configSize)
    }

    override fun getSuggestedResolutions(
        cameraId: String,
        existingSurfaces: List<SurfaceConfig>,
        newUseCaseConfigs: List<UseCaseConfig<*>?>
    ): Map<UseCaseConfig<*>, Size> {
        // TODO: Many of the "find a stream combination that will work" is already provided by the
        //   existing camera2 implementation, and this implementation should leverage that work.

        val sizes: MutableMap<UseCaseConfig<*>, Size> = mutableMapOf()
        for (config in newUseCaseConfigs) {
            sizes[config as UseCaseConfig<*>] = MAXIMUM_PREVIEW_SIZE
        }
        return sizes
    }
}
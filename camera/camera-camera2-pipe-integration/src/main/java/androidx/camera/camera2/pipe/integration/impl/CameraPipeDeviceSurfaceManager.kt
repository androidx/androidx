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
import android.util.Log
import android.util.Rational
import android.util.Size
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig

/**
 * Provide the guaranteed supported stream capabilities provided by CameraPipe.
 * @constructor Creates a CameraPipeDeviceSurfaceManager from the provided [Context].
 */
class CameraPipeDeviceSurfaceManager(context: Context) : CameraDeviceSurfaceManager {
    companion object {
        private const val TAG = "CameraPipeSurfaceMgr"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }

    private val cameraPipe: CameraPipe = CameraPipe(CameraPipe.Config(context))

    init {
        if (DEBUG) {
            Log.d(TAG, "Initialized CameraFactory [Context: $context, CameraPipe: $cameraPipe]")
        }
    }

    override fun checkSupported(cameraId: String, surfaceConfigList: List<SurfaceConfig>): Boolean {
        return false
    }

    override fun transformSurfaceConfig(
        cameraId: String,
        imageFormat: Int,
        size: Size
    ): SurfaceConfig? {
        TODO("Not implemented.")
    }

    override fun getMaxOutputSize(cameraId: String, imageFormat: Int): Size? {
        TODO("Not implemented.")
    }

    override fun getSuggestedResolutions(
        cameraId: String,
        existingSurfaces: List<SurfaceConfig>,
        newUseCaseConfigs: List<UseCaseConfig<*>?>
    ): Map<UseCaseConfig<*>, Size> {
        TODO("Not implemented.")
    }

    override fun getPreviewSize(): Size {
        TODO("Not implemented.")
    }

    override fun requiresCorrectedAspectRatio(cameraId: String): Boolean {
        return false
    }

    override fun getCorrectedAspectRatio(cameraId: String, rotation: Int): Rational? {
        TODO("Not implemented.")
    }
}
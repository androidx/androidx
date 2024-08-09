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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Pair
import android.util.Size
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig

/**
 * Adapt the [CameraDeviceSurfaceManager] interface to [CameraPipe].
 *
 * This class provides Context-specific utility methods for querying and computing supported
 * outputs.
 */
public class CameraSurfaceAdapter(
    context: Context,
    cameraComponent: Any?,
    availableCameraIds: Set<String>
) : CameraDeviceSurfaceManager {
    private val component = cameraComponent as CameraAppComponent
    private val supportedSurfaceCombinationMap =
        mutableMapOf<String, SupportedSurfaceCombination?>()

    init {
        debug { "AvailableCameraIds = $availableCameraIds" }
        debug { "Created StreamConfigurationMap from $context" }
        initSupportedSurfaceCombinationMap(context, availableCameraIds)
    }

    /** Prepare supportedSurfaceCombinationMap for surface adapter. */
    private fun initSupportedSurfaceCombinationMap(
        context: Context,
        availableCameraIds: Set<String>
    ) {
        for (cameraId in availableCameraIds) {
            try {
                val cameraMetadata =
                    component.getCameraDevices().awaitCameraMetadata(CameraId(cameraId))!!
                val streamConfigurationMap =
                    cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                val cameraQuirks =
                    CameraQuirks(
                        cameraMetadata,
                        StreamConfigurationMapCompat(
                            streamConfigurationMap,
                            OutputSizesCorrector(cameraMetadata, streamConfigurationMap)
                        )
                    )
                supportedSurfaceCombinationMap[cameraId] =
                    SupportedSurfaceCombination(
                        context,
                        cameraMetadata,
                        EncoderProfilesProviderAdapter(cameraId, cameraQuirks.quirks)
                    )
            } catch (exception: DoNotDisturbException) {
                Log.error {
                    "Failed to create supported surface combinations: " +
                        "Do Not Disturb mode is on"
                }
            }
        }
    }

    /**
     * Transform to a SurfaceConfig object with cameraId, image format and size info
     *
     * @param cameraMode the working camera mode.
     * @param cameraId the camera id of the camera device to transform the object
     * @param imageFormat the image format info for the surface configuration object
     * @param size the size info for the surface configuration object
     * @return new {@link SurfaceConfig} object
     */
    override fun transformSurfaceConfig(
        cameraMode: Int,
        cameraId: String,
        imageFormat: Int,
        size: Size
    ): SurfaceConfig {
        checkIfSupportedCombinationExist(cameraId)

        return supportedSurfaceCombinationMap[cameraId]!!.transformSurfaceConfig(
            cameraMode,
            imageFormat,
            size
        )
    }

    /**
     * Check whether the supportedSurfaceCombination for the camera id exists
     *
     * @param cameraId the camera id of the camera device used by the use case.
     */
    private fun checkIfSupportedCombinationExist(cameraId: String): Boolean {
        return supportedSurfaceCombinationMap.containsKey(cameraId)
    }

    /**
     * Retrieves a map of suggested stream specifications for the given list of use cases.
     *
     * @param cameraMode the working camera mode.
     * @param cameraId the camera id of the camera device used by the use cases
     * @param existingSurfaces list of surfaces already configured and used by the camera. The
     *   resolutions for these surface can not change.
     * @param newUseCaseConfigsSupportedSizeMap map of configurations of the use cases to the
     *   supported sizes list that will be given a suggested stream specification
     * @param isPreviewStabilizationOn whether the preview stabilization is enabled.
     * @param hasVideoCapture whether the use cases has video capture.
     * @return map of suggested stream specifications for given use cases
     * @throws IllegalArgumentException if {@code newUseCaseConfigs} is an empty list, if there
     *   isn't a supported combination of surfaces available, or if the {@code cameraId} is not a
     *   valid id.
     */
    override fun getSuggestedStreamSpecs(
        cameraMode: Int,
        cameraId: String,
        existingSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigsSupportedSizeMap: Map<UseCaseConfig<*>, List<Size>>,
        isPreviewStabilizationOn: Boolean,
        hasVideoCapture: Boolean
    ): Pair<Map<UseCaseConfig<*>, StreamSpec>, Map<AttachedSurfaceInfo, StreamSpec>> {

        if (!checkIfSupportedCombinationExist(cameraId)) {
            throw IllegalArgumentException(
                "No such camera id in supported combination list: $cameraId"
            )
        }

        return supportedSurfaceCombinationMap[cameraId]!!.getSuggestedStreamSpecifications(
            cameraMode,
            existingSurfaces,
            newUseCaseConfigsSupportedSizeMap,
            isPreviewStabilizationOn,
            hasVideoCapture
        )
    }
}

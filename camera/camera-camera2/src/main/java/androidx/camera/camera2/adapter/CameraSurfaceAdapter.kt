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

package androidx.camera.camera2.adapter

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.config.CameraAppComponent
import androidx.camera.camera2.config.CameraModule
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.impl.FeatureCombinationQueryImpl
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.core.InitializationException
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraUpdateException
import androidx.camera.core.impl.StreamUseCase
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceStreamSpecQueryResult
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.stabilization.VideoStabilization
import androidx.core.util.Preconditions

/**
 * Adapt the [CameraDeviceSurfaceManager] interface to [CameraPipe].
 *
 * This class provides Context-specific utility methods for querying and computing supported
 * outputs, and its internal state is updated transactionally.
 */
public class CameraSurfaceAdapter(
    private val context: Context,
    cameraComponent: Any?,
    availableCameraIds: Set<String>,
) : CameraDeviceSurfaceManager {
    private val component = cameraComponent as CameraAppComponent
    private val lock = Any()

    @GuardedBy("lock")
    private var supportedSurfaceCombinationMap = mapOf<String, SupportedSurfaceCombination>()

    init {
        try {
            // Initial population must also be robust.
            onCamerasUpdated(availableCameraIds.toList())
        } catch (e: CameraUpdateException) {
            // If initial creation fails, the camera is in a bad state.
            throw InitializationException(e)
        }
    }

    /**
     * Transactionally updates the supported surface combinations from the full list of available
     * camera IDs. This method is optimized to only build combinations for newly added cameras.
     *
     * @throws CameraUpdateException if the update fails, signaling a rollback is required.
     */
    override fun onCamerasUpdated(cameraIds: List<String>) {
        // === Stage 1: Prepare (Pre-computation, outside lock) ===
        val combinationsToCreate: List<String>
        synchronized(lock) {
            combinationsToCreate = cameraIds - supportedSurfaceCombinationMap.keys
        }

        if (combinationsToCreate.isNotEmpty()) {
            Camera2Logger.debug { "Creating new surface combinations for: $combinationsToCreate" }
        }

        // This heavy work can throw CameraUpdateException, which is the signal to the coordinator.
        val newCombinations = buildSurfaceCombinations(combinationsToCreate)

        // === Stage 2: Commit (Atomic Swap, inside lock) ===
        synchronized(lock) {
            val finalCombinations = mutableMapOf<String, SupportedSurfaceCombination>()

            // 1. Keep existing, still-valid combinations.
            for (cameraId in cameraIds) {
                if (supportedSurfaceCombinationMap.containsKey(cameraId)) {
                    finalCombinations[cameraId] = supportedSurfaceCombinationMap[cameraId]!!
                }
            }

            // 2. Add the newly created combinations.
            finalCombinations.putAll(newCombinations)

            // 3. Atomically replace the map.
            supportedSurfaceCombinationMap = finalCombinations
            Camera2Logger.debug {
                "Committed new surface combination map. Total cameras: ${finalCombinations.size}"
            }
        }
    }

    /**
     * Builds a map of [SupportedSurfaceCombination] for the given camera IDs. This is the "prepare"
     * stage of the transaction and contains all failable work.
     */
    private fun buildSurfaceCombinations(
        cameraIdsToBuild: List<String>
    ): Map<String, SupportedSurfaceCombination> {
        val newMap = mutableMapOf<String, SupportedSurfaceCombination>()
        if (cameraIdsToBuild.isEmpty()) {
            return newMap
        }

        try {
            for (cameraId in cameraIdsToBuild) {
                val cameraMetadata =
                    component.getCameraDevices().awaitCameraMetadata(CameraId(cameraId))
                        ?: continue // Skip if metadata is not available

                val streamConfigurationMap =
                    cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                val cameraQuirks =
                    CameraQuirks(
                        cameraMetadata,
                        StreamConfigurationMapCompat(
                            streamConfigurationMap,
                            OutputSizesCorrector(cameraMetadata, streamConfigurationMap),
                        ),
                    )
                newMap[cameraId] =
                    SupportedSurfaceCombination(
                        context,
                        cameraMetadata,
                        CameraModule.provideEncoderProfilesProvider(cameraId, cameraQuirks),
                        // TODO: b/417839748 - Decide on the appropriate API level for CameraX
                        //  feature combo API
                        if (Build.VERSION.SDK_INT >= 35) {
                            FeatureCombinationQueryImpl(
                                cameraMetadata,
                                component.getCameraPipe(),
                                cameraQuirks,
                            )
                        } else {
                            FeatureCombinationQuery.NO_OP_FEATURE_COMBINATION_QUERY
                        },
                    )
            }
        } catch (e: DoNotDisturbException) {
            throw CameraUpdateException("Failed to query camera metadata", e)
        } catch (e: Exception) {
            throw CameraUpdateException("Failed to build surface combinations", e)
        }
        return newMap
    }

    /**
     * Transform to a SurfaceConfig object with cameraId, image format and size info
     *
     * @param cameraMode the working camera mode.
     * @param cameraId the camera id of the camera device to transform the object
     * @param imageFormat the image format info for the surface configuration object
     * @param size the size info for the surface configuration object
     * @param streamUseCase the stream use case for the surface configuration object
     * @return new {@link SurfaceConfig} object
     * @throws IllegalArgumentException if the {@code cameraId} is not found in the supported
     *   combinations, or if there isn't a supported combination of surfaces available for the given
     *   parameters.
     */
    override fun transformSurfaceConfig(
        cameraMode: Int,
        cameraId: String,
        imageFormat: Int,
        size: Size,
        streamUseCase: StreamUseCase,
    ): SurfaceConfig {
        Preconditions.checkArgument(
            checkIfSupportedCombinationExist(cameraId),
            "No such camera id in supported combination list: $cameraId",
        )

        val combination =
            synchronized(lock) { supportedSurfaceCombinationMap[cameraId] }
                ?: throw IllegalArgumentException(
                    "No such camera id in supported combination list: $cameraId"
                )

        return combination.transformSurfaceConfig(cameraMode, imageFormat, size, streamUseCase)
    }

    /**
     * Check whether the supportedSurfaceCombination for the camera id exists
     *
     * @param cameraId the camera id of the camera device used by the use case.
     */
    @VisibleForTesting
    internal fun checkIfSupportedCombinationExist(cameraId: String): Boolean {
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
     * @param videoStabilization the video stabilization mode.
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
        videoStabilization: VideoStabilization,
        hasVideoCapture: Boolean,
        isFeatureComboInvocation: Boolean,
        findMaxSupportedFrameRate: Boolean,
    ): SurfaceStreamSpecQueryResult {

        Preconditions.checkArgument(
            checkIfSupportedCombinationExist(cameraId),
            "No such camera id in supported combination list: $cameraId",
        )

        val combination =
            synchronized(lock) { supportedSurfaceCombinationMap[cameraId] }
                ?: throw IllegalArgumentException(
                    "No such camera id in supported combination list: $cameraId"
                )

        return combination.getSuggestedStreamSpecifications(
            cameraMode,
            existingSurfaces,
            newUseCaseConfigsSupportedSizeMap,
            videoStabilization,
            hasVideoCapture,
            isFeatureComboInvocation,
            findMaxSupportedFrameRate,
        )
    }
}

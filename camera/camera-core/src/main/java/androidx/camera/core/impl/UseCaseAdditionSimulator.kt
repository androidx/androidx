/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.impl

import androidx.camera.core.CameraUseCaseAdapterProvider
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.impl.ResolvedFeatureGroup
import androidx.camera.core.featuregroup.impl.ResolvedFeatureGroup.Companion.resolveFeatureGroup
import androidx.camera.core.impl.UseCaseAdditionSimulator.cameraUseCaseAdapterProvider
import androidx.camera.core.impl.UseCaseAdditionSimulator.simulateAddUseCases
import androidx.camera.core.internal.CalculatedUseCaseInfo
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.internal.StreamSpecQueryResult

/**
 * A simulator for the addition of use cases to a [CameraUseCaseAdapter].
 *
 * This allows for a "dry run" of adding use cases to determine how the camera would configure
 * itself without actually binding the use cases. This is useful for predicting the outcome of
 * adding use cases and validating configurations before committing to them.
 */
public object UseCaseAdditionSimulator {
    /**
     * The [CameraUseCaseAdapterProvider] used to obtain a [CameraUseCaseAdapter] instance for the
     * simulation.
     *
     * This provider allows the simulator to access the necessary camera functionalities to perform
     * the "dry run" of adding use cases and must be set before calling [simulateAddUseCases] API.
     */
    @JvmStatic public lateinit var cameraUseCaseAdapterProvider: CameraUseCaseAdapterProvider

    /**
     * Simulates adding use cases to a [CameraUseCaseAdapter] and calculates the resulting use case
     * info.
     *
     * This method allows for a "dry run" of adding use cases without actually binding them to the
     * camera. It determines how the camera would configure itself given the provided
     * [cameraInfoInternal] and [sessionConfig].
     *
     * The [CameraUseCaseAdapter] instance is obtained through the [cameraUseCaseAdapterProvider]
     * property which must be set before calling this API.
     *
     * @param cameraInfoInternal The internal information of the underlying camera to simulate on.
     * @param sessionConfig The session configuration containing the use cases and other settings.
     *   If there are any unresolved configurations in the session config (e.g.
     *   [SessionConfig.preferredFeatureGroup]]), they will be resolved in the same manner as actual
     *   binding flow.
     * @param findMaxSupportedFrameRate Whether to find the maximum supported frame rate during the
     *   simulation, false by default to improve latency in cases where this info is not required.
     *   This info will be able available through the [StreamSpecQueryResult]s of the returned
     *   [CalculatedUseCaseInfo].
     * @return A [CalculatedUseCaseInfo] object containing the simulated configuration result.
     * @throws IllegalStateException If a [CameraUseCaseAdapterProvider] has not been set yet.
     * @throws IllegalArgumentException If the underlying camera capabilities don't support the
     *   combination of features defined by [sessionConfig].
     * @throws CameraUseCaseAdapter.CameraException If the underlying camera capabilities don't
     *   support adding use cases with the provided [sessionConfig].
     */
    @OptIn(ExperimentalSessionConfig::class)
    @Throws(IllegalStateException::class, CameraUseCaseAdapter.CameraException::class)
    @JvmOverloads
    @JvmStatic
    public fun simulateAddUseCases(
        cameraInfoInternal: CameraInfoInternal,
        sessionConfig: SessionConfig,
        findMaxSupportedFrameRate: Boolean = false,
        resolvedFeatureGroup: ResolvedFeatureGroup? = null,
    ): CalculatedUseCaseInfo {
        check(::cameraUseCaseAdapterProvider.isInitialized) {
            "mCameraUseCaseAdapterProvider must be initialized first!"
        }

        val cameraUseCaseAdapter = cameraUseCaseAdapterProvider.provide(cameraInfoInternal.cameraId)
        cameraUseCaseAdapter.viewPort = sessionConfig.viewPort
        cameraUseCaseAdapter.effects = sessionConfig.effects
        cameraUseCaseAdapter.sessionType = sessionConfig.sessionType
        cameraUseCaseAdapter.frameRate = sessionConfig.frameRateRange

        return cameraUseCaseAdapter.simulateAddUseCases(
            sessionConfig.useCases,
            resolvedFeatureGroup ?: sessionConfig.resolveFeatureGroup(cameraInfoInternal),
            /*findMaxSupportedFrameRate=*/ findMaxSupportedFrameRate,
        )
    }
}

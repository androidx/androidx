/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.CameraFacingDirection
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.ConfigMode
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.PlaneTrackingMode
import com.google.ar.core.Config as ArCoreConfig
import kotlin.time.ComparableTimeMark

/**
 * Implementation of the [androidx.xr.arcore.runtime.PerceptionRuntime] interface using ARCore.
 *
 * @property lifecycleManager that manages the lifecycle of the ARCore session
 * @property perceptionManager that manages the perception capabilities of a runtime using ARCore
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreRuntime
internal constructor(
    override val lifecycleManager: ArCoreManager,
    override val perceptionManager: ArCorePerceptionManager,
) : PerceptionRuntime {

    override fun initialize() {
        lifecycleManager.create()
    }

    override fun resume() {
        lifecycleManager.resume()
    }

    override fun pause() {
        lifecycleManager.pause()
    }

    override suspend fun update(): ComparableTimeMark? {
        return lifecycleManager.update()
    }

    override fun configure(config: Config) {
        lifecycleManager.configure(config)
    }

    override fun isSupported(configMode: ConfigMode): Boolean {
        if (configMode is DepthEstimationMode) {
            return isDepthModeSupportedInArCore1x(configMode)
        } else if (configMode is GeospatialMode) {
            return isGeoSpatialModeSupportedInArCore1x(configMode)
        }
        return SUPPORTED_CONFIG_MODES.contains(configMode)
    }

    override fun destroy() {
        lifecycleManager.stop()
    }

    private fun isDepthModeSupportedInArCore1x(depthEstimationMode: DepthEstimationMode): Boolean {
        val arCoreDepthMode =
            when (depthEstimationMode) {
                DepthEstimationMode.SMOOTH_ONLY,
                DepthEstimationMode.SMOOTH_AND_RAW -> ArCoreConfig.DepthMode.AUTOMATIC
                DepthEstimationMode.RAW_ONLY -> ArCoreConfig.DepthMode.RAW_DEPTH_ONLY
                else -> ArCoreConfig.DepthMode.DISABLED
            }
        return lifecycleManager._session.isDepthModeSupported(arCoreDepthMode)
    }

    private fun isGeoSpatialModeSupportedInArCore1x(geospatialMode: GeospatialMode): Boolean {
        val arCoreGeospatialMode =
            when (geospatialMode) {
                GeospatialMode.VPS_AND_GPS -> ArCoreConfig.GeospatialMode.ENABLED
                else -> ArCoreConfig.GeospatialMode.DISABLED
            }
        return lifecycleManager._session.isGeospatialModeSupported(arCoreGeospatialMode)
    }

    internal companion object {
        internal val SUPPORTED_CONFIG_MODES: Set<ConfigMode> =
            setOf(
                CameraFacingDirection.WORLD,
                CameraFacingDirection.USER,
                DeviceTrackingMode.DISABLED,
                DeviceTrackingMode.LAST_KNOWN,
                FaceTrackingMode.DISABLED,
                FaceTrackingMode.MESHES,
                PlaneTrackingMode.DISABLED,
                PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            )
    }
}

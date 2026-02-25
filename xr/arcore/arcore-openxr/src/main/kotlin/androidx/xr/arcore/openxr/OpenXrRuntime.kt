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

package androidx.xr.arcore.openxr

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.ConfigMode
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import kotlin.time.ComparableTimeMark

/**
 * Implementation of the [PerceptionRuntime] interface using OpenXR.
 *
 * @property lifecycleManager that manages the lifecycle of the OpenXR session
 * @property perceptionManager that manages the perception capabilities of a runtime using OpenXR
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrRuntime
internal constructor(
    override val lifecycleManager: OpenXrManager,
    override val perceptionManager: OpenXrPerceptionManager,
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
        if (configMode == GeospatialMode.VPS_AND_GPS) {
            return nativeIsGeospatialSupported()
        }
        return SUPPORTED_CONFIG_MODES.contains(configMode)
    }

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        val blendMode = nativeGetPreferredBlendMode()
        return blendMode ?: DisplayBlendMode.NO_DISPLAY
    }

    override fun destroy() {
        lifecycleManager.stop()
    }

    internal companion object {
        @VisibleForTesting
        internal val SUPPORTED_CONFIG_MODES: Set<ConfigMode> =
            setOf(
                PlaneTrackingMode.DISABLED,
                PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                HandTrackingMode.DISABLED,
                HandTrackingMode.BOTH,
                DeviceTrackingMode.DISABLED,
                DeviceTrackingMode.LAST_KNOWN,
                DepthEstimationMode.DISABLED,
                DepthEstimationMode.RAW_ONLY,
                DepthEstimationMode.SMOOTH_ONLY,
                AnchorPersistenceMode.DISABLED,
                AnchorPersistenceMode.LOCAL,
                FaceTrackingMode.DISABLED,
                FaceTrackingMode.BLEND_SHAPES,
                GeospatialMode.DISABLED,
                EyeTrackingMode.DISABLED,
                EyeTrackingMode.COARSE_TRACKING,
                EyeTrackingMode.FINE_TRACKING,
            )
    }

    private external fun nativeGetPreferredBlendMode(): DisplayBlendMode?

    private external fun nativeIsGeospatialSupported(): Boolean
}

internal fun DisplayBlendMode.Companion.fromOpenXrEnvironmentBlendMode(
    type: Int
): DisplayBlendMode =
    when (type) {
        1 -> NO_DISPLAY // XR_ENVIRONMENT_BLEND_MODE_OPAQUE
        2 -> ADDITIVE // XR_ENVIRONMENT_BLEND_MODE_ADDITIVE
        3 -> ALPHA_BLEND // XR_ENVIRONMENT_BLEND_MODE_ALPHA_BLEND
        else -> {
            throw IllegalStateException("Invalid environment blend mode.")
        }
    }

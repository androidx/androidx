/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.compat.workaround

import android.util.Size
import androidx.camera.camera2.compat.quirk.CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.stabilization.VideoStabilization

/**
 * Validates whether a stream combination crashes the camera HAL due to an HLG10 stream of UHD or
 * larger with video stabilization enabled.
 *
 * @see CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk
 */
public class Hlg10UhdVideoStabilizationValidator(quirks: Quirks) {

    private val isQuirkEnabled =
        quirks.contains(CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk::class.java)

    /**
     * Returns `true` if the combination should be treated as unsupported.
     *
     * @param videoStabilization the video stabilization setting of the session.
     * @param surfaceConfigs the surface configurations of the session.
     * @param dynamicRangesBySurfaceConfig the resolved dynamic range for each [SurfaceConfig].
     * @param resolutionProvider resolves the output [Size] for a [SurfaceConfig]; invoked only for
     *   [DynamicRange.HLG_10_BIT] streams when validation is active.
     */
    public fun isUnsupported(
        videoStabilization: VideoStabilization,
        surfaceConfigs: List<SurfaceConfig>,
        dynamicRangesBySurfaceConfig: Map<SurfaceConfig, DynamicRange>,
        resolutionProvider: (SurfaceConfig) -> Size,
    ): Boolean =
        isQuirkEnabled &&
            videoStabilization == VideoStabilization.ON &&
            surfaceConfigs.any { surfaceConfig ->
                dynamicRangesBySurfaceConfig[surfaceConfig] == DynamicRange.HLG_10_BIT &&
                    resolutionProvider(surfaceConfig).area() >= UHD_AREA
            }

    private fun Size.area(): Long = width.toLong() * height

    private companion object {
        private const val UHD_AREA = 3840L * 2160L
    }
}

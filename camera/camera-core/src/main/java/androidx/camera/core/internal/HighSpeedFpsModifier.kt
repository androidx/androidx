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

package androidx.camera.core.internal

import android.media.MediaCodec
import android.util.Range
import androidx.camera.core.Logger
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig.OutputConfig

/**
 * Modifier for the FPS range for preview-only scenarios in high-speed camera sessions.
 *
 * This class adjusts the FPS range of a repeating capture request when only a preview surface is
 * active in a high-speed camera session. This is done to improve device compatibility by ensuring a
 * lower bound of 30 FPS before recording starts.
 *
 * High-speed sessions typically allow only one preview and one video surface. This modifier ensures
 * that when only the preview is active, the FPS range is adjusted to [30, high-speed fps], which is
 * guaranteed to be supported.
 *
 * See b/405045641 for more detail.
 */
public class HighSpeedFpsModifier {

    private companion object {
        private const val TAG = "HighSpeedFpsModifier"
        private const val PREVIEW_ONLY_FPS_LOWER = 30
    }

    /**
     * Modifies the FPS range for a preview-only repeating capture request in a high-speed camera
     * session.
     *
     * This method checks if the current output configuration includes a video surface and if the
     * repeating capture request does not. If these conditions are met, and the FPS range is a fixed
     * high-speed range (e.g., [120, 120]), it adjusts the range to [30, high-speed fps].
     *
     * @param outputConfigs The collection of output configurations.
     * @param repeatingConfigBuilder The builder for the repeating capture configuration.
     */
    public fun modifyFpsForPreviewOnlyRepeating(
        outputConfigs: Collection<OutputConfig>,
        repeatingConfigBuilder: CaptureConfig.Builder,
    ) {
        if (
            outputConfigs.size == 2 &&
                outputConfigs.hasVideoSurface() &&
                !repeatingConfigBuilder.hasVideoSurface()
        ) {
            repeatingConfigBuilder.expectedFrameRateRange
                ?.takeIf { it.isHighSpeedFixedFps() }
                ?.let { repeatingConfigBuilder.setExpectedFrameRateRange(it.toPreviewOnlyRange()) }
        }
    }

    private fun Range<Int>.isHighSpeedFixedFps(): Boolean = upper >= 120 && lower == upper

    private fun Range<Int>.toPreviewOnlyRange(): Range<Int> {
        return Range(PREVIEW_ONLY_FPS_LOWER, upper).also {
            Logger.d(TAG, "Modified high-speed FPS range from $this to $it")
        }
    }

    private fun Collection<OutputConfig>.hasVideoSurface(): Boolean = any {
        it.surface.isVideoSurface()
    }

    private fun CaptureConfig.Builder.hasVideoSurface(): Boolean =
        surfaces.any { it.isVideoSurface() }

    private fun DeferrableSurface.isVideoSurface(): Boolean =
        containerClass == MediaCodec::class.java
}

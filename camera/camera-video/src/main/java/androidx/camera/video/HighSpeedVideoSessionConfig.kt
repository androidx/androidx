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
package androidx.camera.video

import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession
import android.util.Range
import androidx.annotation.RestrictTo
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.MirrorMode.MIRROR_MODE_OFF
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED

/**
 * A [SessionConfig] for high-speed video recording sessions.
 *
 * This class encapsulates the necessary configurations for a high-speed video recording session,
 * including video capture, optional preview and frame rate. Once configured, this config can be
 * bound to a camera and lifecycle using
 * `androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle` or
 * `androidx.camera.lifecycle.LifecycleCameraProvider.bindToLifecycle`.
 *
 * The supported frame rate range can be queried using [CameraInfo.getSupportedFrameRateRanges] with
 * a specific [HighSpeedVideoSessionConfig]. Supported frame rate is at least 120 FPS and is in
 * multiples of 30. Common high-speed frame rates include 120 FPS, 240 FPS, and sometimes higher
 * (like 480 FPS or even 960 FPS on some devices). If the given frame rate is not one of the
 * supported frame rate ranges, an `IllegalArgumentException` will be thrown when binding to
 * lifecycle.
 *
 * High-speed recording is subject to specific constraints, aligning with those imposed by Android's
 * camera2 [CameraConstrainedHighSpeedCaptureSession] API.
 *
 * **Constraints:**
 * - [videoCapture] and [preview] must not have their target frame rates set explicitly.
 * - [videoCapture] must not have its mirror mode set.
 * - If [preview] is present, its resolution selector, target resolution and target aspect ratio
 *   must not be set. [preview] will get the same resolution as [videoCapture].
 * - [ViewPort] and [CameraEffect] are not supported.
 *
 * Recording a high-speed video follows the same process as recording a regular video. This involves
 * creating a [Recorder] with the desired [QualitySelector] and other settings. The supported
 * qualities and related settings for high-speed sessions can be queried using
 * [Recorder.getHighSpeedVideoCapabilities].
 *
 * **Slow-Motion Video Recording:** When the [isSlowMotionEnabled] flag is set to `true`, the
 * recorded high-speed video will be processed and saved at a standard frame rate of 30 FPS. This
 * creates the slow-motion effect upon playback. The resulting playback speed will be inversely
 * proportional to the ratio of the high recording frame rate to the standard playback frame rate
 * (30 FPS).
 *
 * For example:
 * - If you record at 120 FPS with `isSlowMotionEnabled` set to `true`, the video will be saved at
 *   30 FPS, resulting in a **1/4x speed** during playback (120 / 30 = 4).
 * - If you record at 240 FPS with `isSlowMotionEnabled` set to `true`, the video will be saved at
 *   30 FPS, resulting in a **1/8x speed** during playback (240 / 30 = 8).
 *
 * If [isSlowMotionEnabled] is `false`, the video will be saved at the actual recording frame rate
 * specified by the [frameRateRange] parameter, e.g. 120 FPS, without slow-motion effect.
 *
 * See the sample code below for recording a slow-motion video:
 *
 * @sample androidx.camera.video.samples.slowMotionVideoSample
 * @property videoCapture The [VideoCapture] use case for video recording.
 * @property preview Optional [Preview] use case for displaying a preview during recording.
 * @property frameRateRange The desired frame rate range for high-speed video recording. The value
 *   must be one of the supported frame rates queried by [CameraInfo.getSupportedFrameRateRanges]
 *   with a specific [HighSpeedVideoSessionConfig], or an [IllegalArgumentException] will be thrown
 *   when binding to lifecycle.
 * @property isSlowMotionEnabled Whether to apply slow-motion effects to the recorded video.
 * @throws IllegalArgumentException if any of the constraints are violated.
 * @See androidx.camera.lifecycle.ProcessCameraProvider.bindToLifecycle
 * @See Recorder.getHighSpeedVideoCapabilities
 */
@ExperimentalHighSpeedVideo
@OptIn(ExperimentalSessionConfig::class)
public class HighSpeedVideoSessionConfig
@JvmOverloads
constructor(
    public val videoCapture: VideoCapture<*>,
    public val preview: Preview? = null,
    frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
    public val isSlowMotionEnabled: Boolean = false,
) : SessionConfig(listOfNotNull(videoCapture, preview), frameRateRange = frameRateRange) {

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public override val sessionType: Int = SESSION_TYPE_HIGH_SPEED

    /**
     * Builder for [HighSpeedVideoSessionConfig]
     *
     * @param videoCapture The [VideoCapture] use case for video recording.
     */
    public class Builder(private val videoCapture: VideoCapture<*>) {
        private var preview: Preview? = null
        private var frameRateRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED
        private var isSlowMotionEnabled: Boolean = false

        /** Sets the [Preview] use case for displaying a preview during recording. */
        public fun setPreview(preview: Preview?): Builder {
            this.preview = preview
            return this
        }

        /** Sets whether to apply slow-motion effects to the recorded video. */
        public fun setSlowMotionEnabled(enabled: Boolean): Builder {
            isSlowMotionEnabled = enabled
            return this
        }

        /**
         * Set the frame rate range for the camera session.
         *
         * See [HighSpeedVideoSessionConfig.frameRateRange] for more details.
         */
        public fun setFrameRateRange(frameRateRange: Range<Int>): Builder {
            this.frameRateRange = frameRateRange
            return this
        }

        /** Builds a [HighSpeedVideoSessionConfig] from the current configuration. */
        public fun build(): HighSpeedVideoSessionConfig {
            return HighSpeedVideoSessionConfig(
                videoCapture = videoCapture,
                preview = preview,
                frameRateRange = frameRateRange,
                isSlowMotionEnabled = isSlowMotionEnabled,
            )
        }
    }

    init {
        validateSettingsOrThrow(videoCapture, preview)

        if (isSlowMotionEnabled) {
            (videoCapture.output as Recorder).videoEncodingFrameRate = SLOW_MOTION_ENCODE_FRAME_RATE
        }
    }

    private fun validateSettingsOrThrow(videoCapture: VideoCapture<*>, preview: Preview?) {
        require(videoCapture.mirrorMode == MIRROR_MODE_OFF) {
            "VideoCapture.Builder.setMirrorMode() is not allowed for high-speed video."
        }

        require(videoCapture.targetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED) {
            "VideoCapture.Builder.setTargetFrameRate() is not allowed for high-speed video."
        }

        if (preview != null) {
            require(preview.targetFrameRate == FRAME_RATE_RANGE_UNSPECIFIED) {
                "Preview.Builder.setTargetFrameRate() is not allowed for high-speed video."
            }

            (preview.currentConfig as ImageOutputConfig).let { previewOutputConfig ->
                require(previewOutputConfig.getResolutionSelector(null) == null) {
                    "Preview.Builder.setResolutionSelector() is not allowed for high-speed video."
                }

                require(previewOutputConfig.getTargetResolution(null) == null) {
                    "Preview.Builder.setTargetResolution() is not allowed for high-speed video."
                }

                require(!previewOutputConfig.hasTargetAspectRatio()) {
                    "Preview.Builder.setTargetAspectRatio() is not allowed for high-speed video."
                }
            }
        }
    }

    private companion object {
        private const val SLOW_MOTION_ENCODE_FRAME_RATE = 30
    }
}

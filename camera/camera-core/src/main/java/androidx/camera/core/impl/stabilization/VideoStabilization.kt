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

package androidx.camera.core.impl.stabilization

/**
 * The video stabilization modes that can be applied to the camera, corresponding to
 * [android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE].
 */
public enum class VideoStabilization {
    /** No specific stabilization is specified. */
    UNSPECIFIED,

    /** Stabilization is disabled. */
    OFF,

    /** Stabilization is applied to the video stream only. */
    ON,

    /** Stabilization is applied to both the preview and video streams. */
    PREVIEW;

    internal companion object {
        /**
         * Creates a [VideoStabilization] based on the logic to combine [StabilizationMode] values
         * from CameraX [androidx.camera.core.Preview] and `androidx.camera.video.VideoCapture` use
         * cases.
         */
        @JvmStatic
        internal fun from(
            @StabilizationMode.Mode previewStabilizationMode: Int = StabilizationMode.UNSPECIFIED,
            @StabilizationMode.Mode videoStabilizationMode: Int = StabilizationMode.UNSPECIFIED,
        ): VideoStabilization {
            return if (
                previewStabilizationMode == StabilizationMode.OFF ||
                    videoStabilizationMode == StabilizationMode.OFF
            ) {
                OFF
            } else if (previewStabilizationMode == StabilizationMode.ON) {
                PREVIEW
            } else if (videoStabilizationMode == StabilizationMode.ON) {
                ON
            } else {
                UNSPECIFIED
            }
        }
    }
}

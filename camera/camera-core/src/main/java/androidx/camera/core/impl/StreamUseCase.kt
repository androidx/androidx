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

import android.hardware.camera2.CameraMetadata

/**
 * Represents the intended use case for a camera stream.
 *
 * This enum maps to the various `SCALER_AVAILABLE_STREAM_USE_CASES` constants from
 * `android.hardware.camera2.CameraMetadata`, providing a more structured and type-safe way to
 * specify how a camera stream will be consumed. Using appropriate stream use cases can allow the
 * camera HAL to optimize camera pipelines for different scenarios, potentially improving
 * performance and quality.
 */
public enum class StreamUseCase(intValue: Int) {
    /**
     * Default use case.
     *
     * This corresponds to [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT].
     */
    DEFAULT(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT),

    /**
     * Preview use case.
     *
     * Optimized for displaying a real-time preview on the screen. This corresponds to
     * [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW].
     */
    PREVIEW(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW),

    /**
     * Video record use case.
     *
     * Optimized for high-quality video recording. This corresponds to
     * [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD].
     */
    VIDEO_RECORD(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD),

    /**
     * Still capture use case.
     *
     * Optimized for taking high-resolution still photos. This corresponds to
     * [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE].
     */
    STILL_CAPTURE(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE),

    /**
     * Video call use case.
     *
     * Optimized for video conferencing or video calls. This corresponds to
     * [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL].
     */
    VIDEO_CALL(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL),

    /**
     * Preview, video, and still capture use case.
     *
     * A general-purpose use case that balances performance for preview, video recording, and still
     * image capture simultaneously. This corresponds to
     * [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL].
     */
    PREVIEW_VIDEO_STILL(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL),

    /**
     * Cropped RAW use case.
     *
     * For capturing cropped RAW sensor data. This corresponds to
     * [CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW].
     */
    CROPPED_RAW(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_CROPPED_RAW);

    /** The corresponding `long` value from `android.hardware.camera2.CameraMetadata`. */
    public val value: Long = intValue.toLong()
}

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

import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FEATURE_TYPE_RECORDING_QUALITY
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FEATURE_TYPE_VIDEO_STABILIZATION
import androidx.camera.core.featuregroup.impl.feature.VideoStabilizationFeature
import androidx.camera.core.impl.stabilization.VideoStabilization
import androidx.camera.video.featuregroup.RecordingQualityFeature

/** A collection of [GroupableFeature] instances that are specific to video recording. */
public object GroupableFeatures {
    /**
     * A feature object of type [FEATURE_TYPE_RECORDING_QUALITY] representing [Quality.SD] video
     * recording.
     */
    @JvmField public val SD_RECORDING: GroupableFeature = RecordingQualityFeature(Quality.SD)

    /**
     * A feature object of type [FEATURE_TYPE_RECORDING_QUALITY] representing [Quality.HD] video
     * recording.
     */
    @JvmField public val HD_RECORDING: GroupableFeature = RecordingQualityFeature(Quality.HD)

    /**
     * A feature object of type [FEATURE_TYPE_RECORDING_QUALITY] representing [Quality.FHD] video
     * recording.
     */
    @JvmField public val FHD_RECORDING: GroupableFeature = RecordingQualityFeature(Quality.FHD)

    /**
     * A feature object of type [FEATURE_TYPE_RECORDING_QUALITY] representing [Quality.UHD] video
     * recording.
     */
    @JvmField public val UHD_RECORDING: GroupableFeature = RecordingQualityFeature(Quality.UHD)

    /**
     * A feature object of type [FEATURE_TYPE_VIDEO_STABILIZATION] representing a video
     * stabilization mode that applies to the video recording only, not the preview.
     *
     * [VideoCapture.Builder.setVideoStabilizationEnabled], the non-groupable API corresponding to
     * this feature, must not be used when using this feature as that may lead to conflicting
     * [androidx.camera.core.SessionConfig] values.
     *
     * @see androidx.camera.core.featuregroup.GroupableFeature.PREVIEW_STABILIZATION
     */
    @JvmField
    public val VIDEO_STABILIZATION: GroupableFeature =
        VideoStabilizationFeature(VideoStabilization.ON)
}

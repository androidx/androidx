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

package androidx.camera.core.featuregroup.impl.feature

import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.stabilization.VideoStabilization
import androidx.camera.core.impl.stabilization.VideoStabilization.OFF
import androidx.camera.core.impl.stabilization.VideoStabilization.ON
import androidx.camera.core.impl.stabilization.VideoStabilization.PREVIEW
import androidx.camera.core.impl.stabilization.VideoStabilization.UNSPECIFIED

/**
 * Denotes the video stabilization mode that is applied to the camera.
 *
 * This feature should not be instantiated directly for usage, instead use the
 * [GroupableFeature.PREVIEW_STABILIZATION] object.
 */
public class VideoStabilizationFeature(public val videoStabilization: VideoStabilization) :
    GroupableFeature() {
    override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.VIDEO_STABILIZATION

    override fun isSupportedIndividually(
        cameraInfoInternal: CameraInfoInternal,
        sessionConfig: SessionConfig,
    ): Boolean =
        when (videoStabilization) {
            ON -> cameraInfoInternal.isVideoStabilizationSupported
            PREVIEW -> cameraInfoInternal.isPreviewStabilizationSupported
            OFF,
            UNSPECIFIED -> true
        }

    override fun toString(): String {
        return "VideoStabilizationFeature(mode=${videoStabilization.name})"
    }

    internal companion object {
        @JvmField val DEFAULT_STABILIZATION = OFF
    }
}

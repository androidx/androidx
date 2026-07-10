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

package androidx.camera.core.featuregroup.impl

import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal.DYNAMIC_RANGE
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal.FPS_RANGE
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal.IMAGE_FORMAT
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal.RECORDING_QUALITY
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal.VIDEO_STABILIZATION
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.utils.UseCaseUtil.isVideoCapture
import androidx.camera.core.streamsharing.StreamSharing

/**
 * Enum class representing the different types of use cases supported by CameraX.
 *
 * Each use case type is associated with optional `surfaceClass` information, which represents the
 * type of surface it can output to, when applicable.
 *
 * @property surfaceClass The class of the surface that this use case type can output to, or null if
 *   it isn't applicable.
 */
public enum class UseCaseType(public val surfaceClass: Class<*>?) {
    // TODO: b/400852239 - Check if the surface class types are appropriate

    /** Represents [Preview] use case. */
    PREVIEW(SurfaceHolder::class.java),

    /** Represents [ImageCapture] use case. */
    IMAGE_CAPTURE(null),

    /** Represents [ImageAnalysis] use case. */
    IMAGE_ANALYSIS(null),

    /**
     * Represents `VideoCapture` use case.
     *
     * Note that camera-core module does not depend on camera-video and thus can't refer to the
     * VideoCapture use case directly.
     */
    VIDEO_CAPTURE(android.media.MediaCodec::class.java),

    /** Represents [StreamSharing] use case. */
    STREAM_SHARING(SurfaceTexture::class.java),

    /** Represents an undefined/unknown use case. */
    UNDEFINED(null);

    override fun toString(): String {
        return when (this) {
            PREVIEW -> "Preview"
            IMAGE_CAPTURE -> "ImageCapture"
            IMAGE_ANALYSIS -> "ImageAnalysis"
            VIDEO_CAPTURE -> "VideoCapture"
            STREAM_SHARING -> "StreamSharing"
            UNDEFINED -> "Undefined"
        }
    }

    public companion object {
        /**
         * Returns the [UseCaseType] corresponding to the given [UseCase] for feature combination.
         *
         * If the given use case is not supported for feature combination, [UNDEFINED] is returned.
         *
         * @return The corresponding [UseCaseType]. If the use case is not supported with feature
         *   combination, [UseCaseType.UNDEFINED] is returned.
         */
        @JvmStatic
        public fun UseCase.getFeatureGroupUseCaseType(): UseCaseType {
            return if (this is Preview) {
                PREVIEW
            } else if (this is ImageCapture) {
                IMAGE_CAPTURE
            } else if (this is ImageAnalysis) {
                IMAGE_ANALYSIS
            } else if (this.isVideoCapture()) {
                VIDEO_CAPTURE
            } else if (this is StreamSharing) {
                STREAM_SHARING
            } else {
                UNDEFINED
            }
        }

        /**
         * Returns the [UseCaseType] corresponding to the given [UseCaseConfig] for feature
         * combination.
         *
         * If the given use case config is not supported for feature combination, [UNDEFINED] is
         * returned.
         *
         * @return The corresponding [UseCaseType]. If the use case config is not supported with
         *   feature combination, [UseCaseType.UNDEFINED] is returned.
         */
        @JvmStatic
        public fun UseCaseConfig<*>.getFeatureGroupUseCaseType(): UseCaseType {
            return when (captureType) {
                CaptureType.IMAGE_ANALYSIS -> IMAGE_ANALYSIS
                CaptureType.IMAGE_CAPTURE -> IMAGE_CAPTURE
                CaptureType.PREVIEW -> PREVIEW
                CaptureType.VIDEO_CAPTURE -> VIDEO_CAPTURE
                CaptureType.STREAM_SHARING -> STREAM_SHARING
                else -> UNDEFINED
            }
        }

        /**
         * Gets the [FeatureTypeInternal] that app has configured to a [UseCase] directly, null if
         * none found.
         */
        internal fun UseCase.getAppConfiguredGroupableFeatureType(): FeatureTypeInternal? =
            FeatureTypeInternal.entries.find { it.isConfiguredToUseCaseByApp(this) }

        private fun FeatureTypeInternal.isConfiguredToUseCaseByApp(useCase: UseCase): Boolean =
            when (this) {
                DYNAMIC_RANGE -> useCase.isDynamicRangeConfiguredByApp()
                FPS_RANGE -> useCase.isFpsRangeConfiguredByApp()
                VIDEO_STABILIZATION -> useCase.isStabilizationModeConfiguredByApp()
                IMAGE_FORMAT -> useCase.isImageFormatConfiguredByApp()
                RECORDING_QUALITY -> useCase.isRecordingQualityConfiguredByApp()
            }

        private fun UseCase.isDynamicRangeConfiguredByApp() = appConfig.hasDynamicRange()

        private fun UseCase.isFpsRangeConfiguredByApp() = appConfig.hasTargetFrameRate()

        private fun UseCase.isStabilizationModeConfiguredByApp() =
            appConfig.containsOption(UseCaseConfig.OPTION_PREVIEW_STABILIZATION_MODE) ||
                appConfig.containsOption(UseCaseConfig.OPTION_VIDEO_STABILIZATION_MODE)

        private fun UseCase.isImageFormatConfiguredByApp() =
            appConfig.containsOption(ImageCaptureConfig.OPTION_OUTPUT_FORMAT)

        private fun UseCase.isRecordingQualityConfiguredByApp(): Boolean =
            appConfig.retrieveOption(
                UseCaseConfig.OPTION_IS_VIDEO_QUALITY_SELECTOR_DEFAULT,
                true,
            ) == false
    }
}

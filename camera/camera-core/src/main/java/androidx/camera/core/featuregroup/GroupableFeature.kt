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

package androidx.camera.core.featuregroup

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FEATURE_TYPE_DYNAMIC_RANGE
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FEATURE_TYPE_FPS_RANGE
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FEATURE_TYPE_IMAGE_FORMAT
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FEATURE_TYPE_VIDEO_STABILIZATION
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featuregroup.impl.feature.DynamicRangeFeature
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal
import androidx.camera.core.featuregroup.impl.feature.FpsRangeFeature
import androidx.camera.core.featuregroup.impl.feature.ImageFormatFeature
import androidx.camera.core.featuregroup.impl.feature.VideoStabilizationFeature
import androidx.camera.core.featuregroup.impl.feature.VideoStabilizationFeature.StabilizationMode
import androidx.camera.core.impl.CameraInfoInternal

/**
 * Represents distinct, groupable camera functionalities that can be requested for a camera session.
 *
 * CameraX provides various implementations of this class as objects to denote various groupable
 * features, i.e. [HDR_HLG10], [FPS_60] [PREVIEW_STABILIZATION], [IMAGE_ULTRA_HDR]. Since features
 * may not be supported together as a combination even if each of them are supported individually,
 * these groupable features can be configured as a group in a [androidx.camera.core.SessionConfig]
 * to ensure compatibility when they are used together. When configuring the camera with a session
 * config containing groups of features, their impact on the camera session as a combination will
 * also be considered and compatibility issue will be reported or handled properly based on the
 * exact API used (i.e. exception will be thrown if [SessionConfig.requiredFeatureGroup] is not
 * supported, or some/all features from [SessionConfig.preferredFeatureGroup] will be dropped based
 * on priority).
 *
 * Additionally, the [androidx.camera.core.CameraInfo.isFeatureGroupSupported] API can be used to
 * check if a group of features is supported together on a device.
 *
 * @sample androidx.camera.core.samples.startCameraWithSomeHighQualityFeatures
 * @see androidx.camera.core.SessionConfig.Builder.setRequiredFeatureGroup
 * @see androidx.camera.core.SessionConfig.Builder.setPreferredFeatureGroup
 */
@ExperimentalSessionConfig
public abstract class GroupableFeature
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal constructor() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    internal abstract val featureTypeInternal: FeatureTypeInternal

    /**
     * The type of this feature, which will be one of the following values.
     * - [FEATURE_TYPE_DYNAMIC_RANGE]
     * - [FEATURE_TYPE_FPS_RANGE]
     * - [FEATURE_TYPE_VIDEO_STABILIZATION]
     * - [FEATURE_TYPE_IMAGE_FORMAT]
     */
    public val featureType: @FeatureType Int by lazy {
        // lazy is required here to workaround an issue of java.lang.ExceptionInInitializerError for
        // trying to use an enum when all objects may not be initialized yet.
        featureTypeInternal.toFeatureType()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun isSupportedIndividually(
        cameraInfoInternal: CameraInfoInternal,
        sessionConfig: SessionConfig,
    ): Boolean = true

    private fun FeatureTypeInternal.toFeatureType(): @FeatureType Int {
        return when (this) {
            FeatureTypeInternal.DYNAMIC_RANGE -> FEATURE_TYPE_DYNAMIC_RANGE
            FeatureTypeInternal.FPS_RANGE -> FEATURE_TYPE_FPS_RANGE
            FeatureTypeInternal.VIDEO_STABILIZATION -> FEATURE_TYPE_VIDEO_STABILIZATION
            FeatureTypeInternal.IMAGE_FORMAT -> FEATURE_TYPE_IMAGE_FORMAT
        }
    }

    @IntDef(
        FEATURE_TYPE_DYNAMIC_RANGE,
        FEATURE_TYPE_FPS_RANGE,
        FEATURE_TYPE_VIDEO_STABILIZATION,
        FEATURE_TYPE_IMAGE_FORMAT,
    )
    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class FeatureType

    @ExperimentalSessionConfig
    public companion object {
        /**
         * A feature object of type [FEATURE_TYPE_DYNAMIC_RANGE] representing the 10-bit High
         * Log-Gamma (HLG) HDR.
         *
         * The dynamic range is applied to both the [Preview] and `VideoCapture` use cases when they
         * are added together with this feature.
         *
         * @see DynamicRange.HLG_10_BIT
         */
        @JvmField
        public val HDR_HLG10: GroupableFeature = DynamicRangeFeature(DynamicRange.HLG_10_BIT)

        /**
         * A feature object of type [FEATURE_TYPE_FPS_RANGE] representing 60 FPS (i.e. both the
         * lower and upper bound of the FPS range is 60).
         *
         * When used, this feature ensures the camera always operates at a constant 60 FPS.
         */
        @JvmField public val FPS_60: GroupableFeature = FpsRangeFeature(60, 60)

        /**
         * A feature object of type [FEATURE_TYPE_VIDEO_STABILIZATION] representing a video
         * stabilization mode that applies to the preview as well.
         *
         * @see Preview.Builder.setPreviewStabilizationEnabled
         */
        @JvmField
        public val PREVIEW_STABILIZATION: GroupableFeature =
            VideoStabilizationFeature(StabilizationMode.PREVIEW)

        /**
         * A feature object of type [FEATURE_TYPE_IMAGE_FORMAT] that makes the [ImageCapture] use
         * case capture Ultra HDR JPEG images.
         *
         * @see ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
         */
        @JvmField
        public val IMAGE_ULTRA_HDR: GroupableFeature =
            ImageFormatFeature(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)

        /**
         * Represents the dynamic range feature that can be used for functionalities like HDR video
         * recording.
         *
         * @see HDR_HLG10
         */
        public const val FEATURE_TYPE_DYNAMIC_RANGE: Int = 0

        /**
         * Represents the FPS range feature that can be used for functionalities like 60 FPS video
         * recording.
         *
         * @see FPS_60
         */
        public const val FEATURE_TYPE_FPS_RANGE: Int = 1

        /**
         * Represents the video stabilization feature that can be used to apply stabilization to the
         * preview and recorded video.
         *
         * @see PREVIEW_STABILIZATION
         */
        public const val FEATURE_TYPE_VIDEO_STABILIZATION: Int = 2

        /**
         * Represents the image format feature that can be used to capture JPEG Ultra HDR images.
         *
         * @see IMAGE_ULTRA_HDR
         */
        public const val FEATURE_TYPE_IMAGE_FORMAT: Int = 3
    }
}

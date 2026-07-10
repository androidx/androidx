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

import android.graphics.ImageFormat
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.DynamicRanges.canResolve
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.video.internal.config.VideoConfigUtil
import androidx.camera.video.internal.encoder.VideoEncoderInfo

/**
 * An implementation of [VideoCapabilities] that filters results based on a specific MIME type.
 *
 * This class validates support by intersecting camera capabilities, encoder capabilities for the
 * given MIME type, and available [EncoderProfilesProvider].
 */
internal class MimeMatchedVideoCapabilities(
    private val mime: String,
    private val cameraInfo: CameraInfoInternal,
    videoEncoderInfoFinder: VideoEncoderInfo.Finder,
) : VideoCapabilities {

    /** Data class to hold pre-calculated validated capability information. */
    private data class ValidatedData(
        val dynamicRanges: Set<DynamicRange> = emptySet(),
        val qualityToSizeMap: Map<Quality, Size> = emptyMap(),
    )

    private val validatedData: ValidatedData by lazy {
        val encoderInfo = videoEncoderInfoFinder.find(mime) ?: return@lazy ValidatedData()

        // 1. Resolve DynamicRanges: Intersection of Camera and MIME support
        val cameraDynamicRanges = cameraInfo.supportedDynamicRanges
        if (cameraDynamicRanges.isEmpty()) return@lazy ValidatedData()

        val mimeDynamicRanges = VideoConfigUtil.getDynamicRangesForMime(mime)
        val finalDynamicRanges = cameraDynamicRanges.intersect(mimeDynamicRanges)
        if (finalDynamicRanges.isEmpty()) return@lazy ValidatedData()

        // 2. Resolve Qualities: Filter by Camera resolutions and Encoder size constraints
        val supportedResolutions =
            cameraInfo.getSupportedResolutions(ImageFormat.PRIVATE).toHashSet()
        val finalQualityToSizeMap: Map<Quality, Size> =
            Quality.getSortedQualities()
                .filterIsInstance<Quality.ConstantQuality>()
                .mapNotNull { quality ->
                    val matchingSize =
                        quality.typicalSizes.firstOrNull { size ->
                            size in supportedResolutions &&
                                encoderInfo.isSizeSupported(size.width, size.height)
                        }
                    matchingSize?.let { quality to it }
                }
                .toMap()

        if (finalQualityToSizeMap.isEmpty()) return@lazy ValidatedData()

        ValidatedData(dynamicRanges = finalDynamicRanges, qualityToSizeMap = finalQualityToSizeMap)
    }

    override fun getSupportedDynamicRanges(): Set<DynamicRange> = validatedData.dynamicRanges

    override fun getSupportedQualities(dynamicRange: DynamicRange): List<Quality> {
        return if (canResolve(dynamicRange, validatedData.dynamicRanges)) {
            validatedData.qualityToSizeMap.keys.toList()
        } else {
            emptyList()
        }
    }

    override fun isQualitySupported(quality: Quality, dynamicRange: DynamicRange): Boolean {
        return canResolve(dynamicRange, validatedData.dynamicRanges) &&
            validatedData.qualityToSizeMap.containsKey(quality)
    }

    override fun getResolution(quality: Quality, dynamicRange: DynamicRange): Size? {
        return if (canResolve(dynamicRange, validatedData.dynamicRanges)) {
            validatedData.qualityToSizeMap[quality]
        } else {
            null
        }
    }

    override fun isStabilizationSupported(): Boolean = cameraInfo.isVideoStabilizationSupported

    override fun toString(): String {
        return "MimeMatchedVideoCapabilities(mime=$mime, cameraInfo=$cameraInfo)"
    }
}

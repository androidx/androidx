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

import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.video.internal.QualityExploredEncoderProfilesProvider
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.VideoQualityQuirk
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.camera.video.internal.workaround.QualityAddedEncoderProfilesProvider
import androidx.camera.video.internal.workaround.QualityResolutionModifiedEncoderProfilesProvider
import androidx.camera.video.internal.workaround.QualityValidatedEncoderProfilesProvider

/** Resolver for providing [EncoderProfilesProvider] used by [Recorder]. */
internal object EncoderProfilesProviderResolver {

    private const val TAG = "EncoderProfilesResolver"

    /** Resolves the [EncoderProfilesProvider] based on the camera info and source. */
    fun resolve(
        cameraInfo: CameraInfoInternal,
        @Recorder.VideoCapabilitiesSource videoCapabilitiesSource: Int,
        @Quality.QualitySource qualitySource: Int,
        videoEncoderInfoFinder: VideoEncoderInfo.Finder,
    ): EncoderProfilesProvider {
        require(
            videoCapabilitiesSource == Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE ||
                videoCapabilitiesSource == Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
        ) {
            "Not a supported video capabilities source: $videoCapabilitiesSource"
        }

        var provider = cameraInfo.encoderProfilesProvider

        val deviceQuirks = DeviceQuirks.getAll()
        if (qualitySource == Quality.QUALITY_SOURCE_HIGH_SPEED) {
            if (!cameraInfo.isHighSpeedSupported) {
                return EncoderProfilesProvider.EMPTY
            }

            // Filter for validated qualities (e.g. excluding broken high-speed profiles)
            if (deviceQuirks.contains(VideoQualityQuirk::class.java)) {
                provider =
                    QualityValidatedEncoderProfilesProvider(provider, cameraInfo, deviceQuirks)
            }

            // TODO(b/399585664): explore high speed quality when video source is
            //  VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
            return provider
        }

        val cameraSupportedResolutions =
            cameraInfo.getSupportedResolutions(
                ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            )
        fun explore(
            baseProvider: EncoderProfilesProvider,
            targetQualities: List<Quality>,
            targetDynamicRanges: Set<DynamicRange>,
        ) =
            QualityExploredEncoderProfilesProvider(
                baseProvider,
                targetQualities,
                targetDynamicRanges,
                cameraSupportedResolutions,
                videoEncoderInfoFinder,
            )

        if (!CapabilitiesByQuality.containsSupportedQuality(provider, qualitySource)) {
            Logger.w(TAG, "Camera EncoderProfilesProvider doesn't contain any supported Quality.")
            // No supported quality, e.g. an external camera without CamcorderProfile: create FHD,
            // HD and SD profiles from default values.
            // Limit maximum supported video resolution to 1080p(FHD).
            // While 2160p(UHD) may be reported as supported by the Camera and MediaCodec APIs,
            // testing on lab devices has shown that recording at this resolution is not always
            // reliable. This aligns with the Android 5.1 CDD, which recommends 1080p as the
            // supported resolution.
            // See: https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf,
            // 5.2. Video Encoding.
            provider =
                explore(
                    EncoderProfilesProvider.EMPTY,
                    listOf(Quality.FHD, Quality.HD, Quality.SD),
                    setOf(DynamicRange.SDR),
                )
        }

        // Decorate with extra supported qualities
        provider =
            QualityAddedEncoderProfilesProvider(
                provider,
                deviceQuirks,
                cameraInfo,
                videoEncoderInfoFinder,
            )

        // Explore all qualities and HDR dynamic ranges supported by the camera and a video encoder
        if (videoCapabilitiesSource == Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES) {
            provider =
                explore(
                    provider,
                    Quality.getSortedQualities(),
                    getCodecExploredDynamicRanges(cameraInfo),
                )
        }

        // Modify matching resolutions based on camera support
        provider = QualityResolutionModifiedEncoderProfilesProvider(provider, deviceQuirks)

        // Camcorder profile source: add an HLG10 profile to every quality that has a video profile.
        // Qualities without a video profile are not targeted, so no quality is added. The codec
        // capabilities source explores HLG10 above together with the other HDR formats.
        if (
            videoCapabilitiesSource == Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE &&
                cameraInfo.isHlg10Supported
        ) {
            provider =
                explore(
                    provider,
                    provider.getQualitiesWithProfile(),
                    setOf(DynamicRange.HLG_10_BIT),
                )
        }

        // Filter for validated qualities
        provider = QualityValidatedEncoderProfilesProvider(provider, cameraInfo, deviceQuirks)

        return provider
    }

    /**
     * Returns the dynamic ranges to explore for
     * [Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES]: SDR, and HLG10, HDR10, HDR10+ and
     * Dolby Vision 10-bit if supported by the camera.
     *
     * Only the dynamic ranges that have an [android.media.EncoderProfiles.VideoProfile] HDR format
     * are explored. The SMPTE ST 2094-50 variants of HLG10 and HDR10 use the same HDR format, so a
     * camera supporting such a variant explores HLG10 or HDR10.
     */
    private fun getCodecExploredDynamicRanges(cameraInfo: CameraInfoInternal): Set<DynamicRange> =
        buildSet {
            add(DynamicRange.SDR)
            if (cameraInfo.isHlg10Supported) {
                add(DynamicRange.HLG_10_BIT)
            }
            if (cameraInfo.isHdr10Supported) {
                add(DynamicRange.HDR10_10_BIT)
            }
            val cameraSupportedDynamicRanges = cameraInfo.supportedDynamicRanges
            if (DynamicRange.HDR10_PLUS_10_BIT in cameraSupportedDynamicRanges) {
                add(DynamicRange.HDR10_PLUS_10_BIT)
            }
            if (DynamicRange.DOLBY_VISION_10_BIT in cameraSupportedDynamicRanges) {
                add(DynamicRange.DOLBY_VISION_10_BIT)
            }
        }

    /** Returns the qualities in [Quality.getSortedQualities] that have a video profile. */
    private fun EncoderProfilesProvider.getQualitiesWithProfile(): List<Quality> =
        Quality.getSortedQualities().filter {
            val qualityValue =
                (it as Quality.ConstantQuality).getQualityValue(Quality.QUALITY_SOURCE_REGULAR)
            !getAll(qualityValue)?.videoProfiles.isNullOrEmpty()
        }

    /** Extension property to check HDR10 support from supported dynamic ranges. */
    private val CameraInfoInternal.isHdr10Supported: Boolean
        get() = supportedDynamicRanges.any {
            (it.encoding == DynamicRange.ENCODING_HDR10 ||
                it.encoding == DynamicRange.ENCODING_HDR10_SMPTE_2094_50) &&
                it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT
        }

    /** Extension property to check HLG10 support from supported dynamic ranges. */
    private val CameraInfoInternal.isHlg10Supported: Boolean
        get() = supportedDynamicRanges.any {
            (it.encoding == DynamicRange.ENCODING_HLG ||
                it.encoding == DynamicRange.ENCODING_HLG_SMPTE_2094_50) &&
                it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT
        }
}

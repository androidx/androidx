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
import androidx.camera.video.internal.BackupHdrProfileEncoderProfilesProvider
import androidx.camera.video.internal.QualityExploredEncoderProfilesProvider
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.camera.video.internal.workaround.DefaultEncoderProfilesProvider
import androidx.camera.video.internal.workaround.QualityAddedEncoderProfilesProvider
import androidx.camera.video.internal.workaround.QualityResolutionModifiedEncoderProfilesProvider
import androidx.camera.video.internal.workaround.QualityValidatedEncoderProfilesProvider
import java.util.Collections

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

        if (qualitySource == Quality.QUALITY_SOURCE_HIGH_SPEED) {
            if (!cameraInfo.isHighSpeedSupported) {
                return EncoderProfilesProvider.EMPTY
            }

            // TODO(b/399585664): explore high speed quality when video source is
            //  VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
            return provider
        }

        if (!CapabilitiesByQuality.containsSupportedQuality(provider, qualitySource)) {
            Logger.w(TAG, "Camera EncoderProfilesProvider doesn't contain any supported Quality.")
            // Limit maximum supported video resolution to 1080p(FHD).
            // While 2160p(UHD) may be reported as supported by the Camera and MediaCodec APIs,
            // testing on lab devices has shown that recording at this resolution is not always
            // reliable. This aligns with the Android 5.1 CDD, which recommends 1080p as the
            // supported resolution.
            // See: https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf,
            // 5.2. Video Encoding.
            val targetQualities = listOf(Quality.FHD, Quality.HD, Quality.SD)
            provider =
                DefaultEncoderProfilesProvider(cameraInfo, targetQualities, videoEncoderInfoFinder)
        }

        val deviceQuirks = DeviceQuirks.getAll()

        // Decorate with extra supported qualities
        provider =
            QualityAddedEncoderProfilesProvider(
                provider,
                deviceQuirks,
                cameraInfo,
                videoEncoderInfoFinder,
            )

        if (videoCapabilitiesSource == Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES) {
            provider =
                QualityExploredEncoderProfilesProvider(
                    provider,
                    Quality.getSortedQualities(),
                    Collections.singleton(DynamicRange.SDR),
                    cameraInfo.getSupportedResolutions(
                        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    ),
                    videoEncoderInfoFinder,
                )
        }

        // Modify matching resolutions based on camera support
        provider = QualityResolutionModifiedEncoderProfilesProvider(provider, deviceQuirks)

        // Add backup HDR profiles (HLG10)
        if (cameraInfo.isHlg10Supported) {
            provider = BackupHdrProfileEncoderProfilesProvider(provider, videoEncoderInfoFinder)
        }

        // Filter for validated qualities
        provider = QualityValidatedEncoderProfilesProvider(provider, cameraInfo, deviceQuirks)

        return provider
    }

    /** Extension property to check HLG10 support from supported dynamic ranges. */
    private val CameraInfoInternal.isHlg10Supported: Boolean
        get() =
            supportedDynamicRanges.any {
                it.encoding == DynamicRange.ENCODING_HLG &&
                    it.bitDepth == DynamicRange.BIT_DEPTH_10_BIT
            }
}

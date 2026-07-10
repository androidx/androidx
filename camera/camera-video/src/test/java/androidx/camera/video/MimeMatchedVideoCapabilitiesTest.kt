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
import android.media.MediaFormat
import android.util.Range
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_VGA
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class MimeMatchedVideoCapabilitiesTest {

    companion object {
        private const val MIME_AVC = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private val encoderInfoFinder = VideoEncoderInfo.Finder { _ -> FakeVideoEncoderInfo() }

    private lateinit var cameraInfo: FakeCameraInfoInternal
    private lateinit var videoCapabilities: MimeMatchedVideoCapabilities

    @Before
    fun setUp() {
        // Setup Camera Info with default behaviors
        cameraInfo =
            FakeCameraInfoInternal().apply {
                supportedDynamicRanges = setOf(SDR, HLG_10_BIT)
                setSupportedResolutions(
                    ImageFormat.PRIVATE,
                    listOf(
                        RESOLUTION_2160P,
                        RESOLUTION_1080P,
                        RESOLUTION_720P,
                        RESOLUTION_480P,
                        RESOLUTION_VGA,
                    ),
                )
            }

        videoCapabilities = MimeMatchedVideoCapabilities(MIME_AVC, cameraInfo, encoderInfoFinder)
    }

    @Test
    fun getSupportedDynamicRanges_intersectsCameraAndMime() {
        val caps = MimeMatchedVideoCapabilities(MIME_AVC, cameraInfo, encoderInfoFinder)

        val ranges = caps.getSupportedDynamicRanges()
        assertThat(ranges).containsExactly(SDR)
    }

    @Test
    fun getSupportedQualities_returnsQualitiesWithinEncoderRange() {
        // Arrange: Restrict encoder to 1080p maximum
        val constrainedEncoderInfo =
            FakeVideoEncoderInfo(
                supportedWidths = Range(0, 1920),
                supportedHeights = Range(0, 1080),
            )
        val caps = MimeMatchedVideoCapabilities(MIME_AVC, cameraInfo) { constrainedEncoderInfo }

        // Act & Assert: UHD (2160p) should be filtered out because it exceeds encoder limits
        val supportedQualities = caps.getSupportedQualities(SDR)
        assertThat(supportedQualities).contains(HD)
        assertThat(supportedQualities).doesNotContain(UHD)
    }

    @Test
    fun getSupportedQualities_filtersByAlignment() {
        // Arrange: Use a strange alignment that standard resolutions won't meet
        val constrainedEncoderInfo = FakeVideoEncoderInfo(widthAlignment = 1000)
        val caps = MimeMatchedVideoCapabilities(MIME_AVC, cameraInfo) { constrainedEncoderInfo }

        // Act & Assert
        assertThat(caps.getSupportedQualities(SDR)).isEmpty()
    }

    @Test
    fun getResolution_returnsCorrectSizeForQuality() {
        // SD typical sizes include 720x480.
        // If supported by camera and encoder, it should be returned.
        val size = videoCapabilities.getResolution(SD, SDR)
        assertThat(size).isEqualTo(RESOLUTION_480P) // 720x480
    }

    @Test
    fun isQualitySupported_returnsFalseForUnsupportedEncoderSize() {
        // Arrange: Encoder doesn't support 4K
        val constrainedEncoderInfo =
            FakeVideoEncoderInfo(
                supportedWidths = Range(0, 1920),
                supportedHeights = Range(0, 1080),
            )
        val caps = MimeMatchedVideoCapabilities(MIME_AVC, cameraInfo) { constrainedEncoderInfo }

        assertThat(caps.isQualitySupported(UHD, SDR)).isFalse()
        assertThat(caps.isQualitySupported(HD, SDR)).isTrue()
    }

    @Test
    fun isStabilizationSupported_delegatesToCamera() {
        cameraInfo.isVideoStabilizationSupported = true
        assertThat(videoCapabilities.isStabilizationSupported()).isTrue()

        cameraInfo.isVideoStabilizationSupported = false
        assertThat(videoCapabilities.isStabilizationSupported()).isFalse()
    }

    @Test
    fun unresolvableDynamicRange_returnsEmptyOrNull() {
        // HDR10 is not in cameraInfo.supportedDynamicRanges
        val unsupportedRange = HDR10_10_BIT

        assertThat(videoCapabilities.getSupportedQualities(unsupportedRange)).isEmpty()
        assertThat(videoCapabilities.getResolution(HD, unsupportedRange)).isNull()
    }
}

/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.config

import android.content.Context
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class VideoEncoderConfigVideoProfileResolverTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )

        private const val FRAME_RATE_30 = 30
        private const val FRAME_RATE_45 = 45
    }

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultVideoSpec = VideoSpec.builder().build()
    private val timebase = Timebase.UPTIME

    private lateinit var dynamicRanges: Set<DynamicRange>
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var videoCapabilities: VideoCapabilities

    @Before
    fun setUp() {
        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        CameraXUtil.initialize(context, cameraConfig).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        dynamicRanges = videoCapabilities.supportedDynamicRanges
        dynamicRanges.forEach {
            Assume.assumeTrue(videoCapabilities.getSupportedQualities(it).isNotEmpty())
        }
    }

    @After
    fun tearDown() {
        if (this::cameraUseCaseAdapter.isInitialized) {
            runBlocking(Dispatchers.Main) {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }

        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun defaultVideoSpecProducesValidSettings_forSurfaceSizeEquivalentToQuality() {
        dynamicRanges.forEach { dynamicRange ->
            val supportedProfiles =
                videoCapabilities.getSupportedQualities(dynamicRange).map {
                    videoCapabilities.getProfiles(it, dynamicRange)!!
                }

            supportedProfiles.forEach {
                val videoProfile = it.defaultVideoProfile
                val config =
                    VideoEncoderConfigVideoProfileResolver(
                            videoProfile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            videoProfile.resolution,
                            videoProfile,
                            dynamicRange,
                            Range(videoProfile.frameRate, videoProfile.frameRate),
                        )
                        .get()

                assertThat(config.mimeType).isEqualTo(videoProfile.mediaType)
                assertThat(config.bitrate).isEqualTo(videoProfile.bitrate)
                assertThat(config.resolution).isEqualTo(videoProfile.resolution)
                assertThat(config.captureFrameRate).isEqualTo(videoProfile.frameRate)
                assertThat(config.encodeFrameRate).isEqualTo(videoProfile.frameRate)
            }
        }
    }

    @Test
    fun bitrateIncreasesOrDecreasesWithIncreaseOrDecreaseInSurfaceSize() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = profile.resolution
            val profileFrameRate = Range(profile.frameRate, profile.frameRate)

            val defaultBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        profileFrameRate,
                    )
                    .get()
                    .bitrate

            val increasedSurfaceSize = Size(surfaceSize.width + 100, surfaceSize.height + 100)
            val decreasedSurfaceSize = Size(surfaceSize.width - 100, surfaceSize.height - 100)

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            increasedSurfaceSize,
                            profile,
                            dynamicRange,
                            profileFrameRate,
                        )
                        .get()
                        .bitrate
                )
                .isGreaterThan(defaultBitrate)

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            decreasedSurfaceSize,
                            profile,
                            dynamicRange,
                            profileFrameRate,
                        )
                        .get()
                        .bitrate
                )
                .isLessThan(defaultBitrate)
        }
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = profile.resolution

            val defaultBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                    )
                    .get()
                    .bitrate

            // Create video spec with limit 20% higher than default.
            val higherBitrate = (defaultBitrate * 1.2).toInt()
            val higherVideoSpec =
                VideoSpec.builder().setBitrate(Range(higherBitrate, Int.MAX_VALUE)).build()

            // Create video spec with limit 20% lower than default.
            val lowerBitrate = (defaultBitrate * 0.8).toInt()
            val lowerVideoSpec = VideoSpec.builder().setBitrate(Range(0, lowerBitrate)).build()

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            timebase,
                            higherVideoSpec,
                            surfaceSize,
                            profile,
                            dynamicRange,
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                        )
                        .get()
                        .bitrate
                )
                .isEqualTo(higherBitrate)

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            timebase,
                            lowerVideoSpec,
                            surfaceSize,
                            profile,
                            dynamicRange,
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                        )
                        .get()
                        .bitrate
                )
                .isEqualTo(lowerBitrate)
        }
    }

    @Test
    fun frameRateIsDefault_whenNoExpectedRangeProvided() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = profile.resolution

            assertThat(
                    VideoEncoderConfigVideoProfileResolver(
                            profile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            surfaceSize,
                            profile,
                            dynamicRange,
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
                        )
                        .get()
                        .encodeFrameRate
                )
                .isEqualTo(VideoConfigUtil.VIDEO_FRAME_RATE_FIXED_DEFAULT)
        }
    }

    @Test
    fun frameRateIsChosenFromUpperOfExpectedRange_whenProvided() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = profile.resolution

            val expectedCaptureFrameRateRange = Range(FRAME_RATE_30, FRAME_RATE_45)

            val resolvedFrameRate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        expectedCaptureFrameRateRange,
                    )
                    .get()
                    .encodeFrameRate

            assertThat(resolvedFrameRate).isEqualTo(expectedCaptureFrameRateRange.upper)
        }
    }

    @Test
    fun bitrateScalesWithFrameRateOperatingRange() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = profile.resolution

            // Construct a range which is constant and half the profile FPS
            val operatingFrameRate = profile.frameRate / 2
            val operatingRange = Range(operatingFrameRate, operatingFrameRate)

            val resolvedBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        operatingRange,
                    )
                    .get()
                    .bitrate

            assertThat(resolvedBitrate)
                .isEqualTo(
                    (profile.bitrate * (operatingFrameRate.toDouble() / profile.frameRate)).toInt()
                )
        }
    }

    @Test
    fun codecProfileLevel_isResolvedFromVideoProfile() {
        dynamicRanges.forEach { dynamicRange ->
            val supportedProfiles =
                videoCapabilities.getSupportedQualities(dynamicRange).flatMap {
                    videoCapabilities.getProfiles(it, dynamicRange)!!.videoProfiles
                }

            supportedProfiles.forEach { videoProfile ->
                val surfaceSize = videoProfile.resolution

                val resolvedProfile =
                    VideoEncoderConfigVideoProfileResolver(
                            videoProfile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            surfaceSize,
                            videoProfile,
                            dynamicRange,
                            Range(videoProfile.frameRate, videoProfile.frameRate),
                        )
                        .get()
                        .profile

                assertThat(resolvedProfile).isEqualTo(videoProfile.profile)
            }
        }
    }

    @Test
    fun supportedHdrDynamicRanges_mapToSpecifiedVideoEncoderDataSpace() {
        dynamicRanges.forEach { dynamicRange ->
            val supportedProfiles =
                videoCapabilities
                    .getSupportedQualities(dynamicRange)
                    .flatMap { videoCapabilities.getProfiles(it, dynamicRange)!!.videoProfiles }
                    .toSet()

            supportedProfiles.forEach { videoProfile ->
                val surfaceSize = videoProfile.resolution

                val resolvedDataSpace =
                    VideoEncoderConfigVideoProfileResolver(
                            videoProfile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            surfaceSize,
                            videoProfile,
                            dynamicRange,
                            Range(videoProfile.frameRate, videoProfile.frameRate),
                        )
                        .get()
                        .dataSpace

                // SDR should always map to UNSPECIFIED, while others should not
                if (dynamicRange == DynamicRange.SDR) {
                    assertThat(resolvedDataSpace)
                        .isEqualTo(VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED)
                } else {
                    assertThat(resolvedDataSpace)
                        .isNotEqualTo(VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED)
                }
            }
        }
    }
}

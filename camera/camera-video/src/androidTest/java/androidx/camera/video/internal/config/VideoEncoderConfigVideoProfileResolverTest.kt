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
import androidx.camera.core.CameraSelector
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
    private val cameraConfig: CameraXConfig
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val defaultVideoSpec = VideoSpec.builder().build()
    private val timebase = Timebase.UPTIME

    private lateinit var dynamicRanges: Set<DynamicRange>
    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var videoCapabilities: VideoCapabilities

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
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
                            Size(videoProfile.width, videoProfile.height),
                            videoProfile,
                            dynamicRange,
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                        )
                        .get()

                assertThat(config.mimeType).isEqualTo(videoProfile.mediaType)
                assertThat(config.bitrate).isEqualTo(videoProfile.bitrate)
                assertThat(config.resolution)
                    .isEqualTo(Size(videoProfile.width, videoProfile.height))
                assertThat(config.frameRate).isEqualTo(videoProfile.frameRate)
            }
        }
    }

    @Test
    fun bitrateIncreasesOrDecreasesWithIncreaseOrDecreaseInSurfaceSize() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = Size(profile.width, profile.height)

            val defaultBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
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
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
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
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
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
            val surfaceSize = Size(profile.width, profile.height)

            val defaultBitrate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
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
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
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
                            SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
                        )
                        .get()
                        .bitrate
                )
                .isEqualTo(lowerBitrate)
        }
    }

    @Test
    fun resolvedFrameRateIsClampedToOperatingRate() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = Size(profile.width, profile.height)

            // Construct operating ranges that are both lower and higher than the profile FPS
            val lowerOperatingRange = Range(profile.frameRate / 4, profile.frameRate / 2)
            val higherOperatingRange = Range(profile.frameRate * 2, profile.frameRate * 4)

            val clampedDownFrameRate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        lowerOperatingRange
                    )
                    .get()
                    .frameRate

            val clampedUpFrameRate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        higherOperatingRange
                    )
                    .get()
                    .frameRate

            assertThat(clampedDownFrameRate).isEqualTo(lowerOperatingRange.upper)
            assertThat(clampedUpFrameRate).isEqualTo(higherOperatingRange.lower)
        }
    }

    @Test
    fun resolvedFrameRateInsideOperatingRangeIsUnchanged() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = Size(profile.width, profile.height)

            // Construct a range that includes the profile FPS
            val operatingRange = Range(profile.frameRate / 2, profile.frameRate * 2)

            val resolvedFrameRate =
                VideoEncoderConfigVideoProfileResolver(
                        profile.mediaType,
                        timebase,
                        defaultVideoSpec,
                        surfaceSize,
                        profile,
                        dynamicRange,
                        operatingRange
                    )
                    .get()
                    .frameRate

            assertThat(resolvedFrameRate).isEqualTo(profile.frameRate)
        }
    }

    @Test
    fun bitrateScalesWithFrameRateOperatingRange() {
        dynamicRanges.forEach { dynamicRange ->
            val profile =
                videoCapabilities.getProfiles(Quality.HIGHEST, dynamicRange)!!.defaultVideoProfile
            val surfaceSize = Size(profile.width, profile.height)

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
                        operatingRange
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
                val surfaceSize = Size(videoProfile.width, videoProfile.height)

                val resolvedProfile =
                    VideoEncoderConfigVideoProfileResolver(
                            videoProfile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            surfaceSize,
                            videoProfile,
                            dynamicRange,
                            Range(videoProfile.frameRate, videoProfile.frameRate)
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
                val surfaceSize = Size(videoProfile.width, videoProfile.height)

                val resolvedDataSpace =
                    VideoEncoderConfigVideoProfileResolver(
                            videoProfile.mediaType,
                            timebase,
                            defaultVideoSpec,
                            surfaceSize,
                            videoProfile,
                            dynamicRange,
                            Range(videoProfile.frameRate, videoProfile.frameRate)
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

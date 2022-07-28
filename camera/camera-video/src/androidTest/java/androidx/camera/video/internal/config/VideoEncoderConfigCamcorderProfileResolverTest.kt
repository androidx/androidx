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
import android.util.Range
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.video.Quality
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.VideoSpec
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class VideoEncoderConfigCamcorderProfileResolverTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {

    companion object {
        // TODO(b/177918193): We currently cannot communicate the frame rate to the camera,
        //  so we only support 30fps. For now we want to ensure we only ever get an FPS of 30,
        //  but this check can be removed once we can change frame rate.
        const val FIXED_FRAME_RATE = 30

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val defaultVideoSpec = VideoSpec.builder().build()

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var videoCapabilities: VideoCapabilities

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        CameraXUtil.initialize(
            context,
            cameraConfig
        ).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        videoCapabilities = VideoCapabilities.from(cameraInfo)
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
        val supportedProfiles = videoCapabilities.supportedQualities.map {
            videoCapabilities.getProfile(it)!!
        }

        supportedProfiles.forEach {
            val config = VideoEncoderConfigCamcorderProfileResolver(
                it.videoCodecMimeType!!,
                defaultVideoSpec,
                Size(it.videoFrameWidth, it.videoFrameHeight),
                it
            ).get()

            assertThat(config.mimeType).isEqualTo(it.videoCodecMimeType)
            // TODO(b/177918193): Frame rate ratio needs to scale bitrate since we use a fixed
            //  frame rate. The output config frame rate may not match the profile frame rate.
            val scaledBitrate =
                (it.videoBitRate * config.frameRate.toDouble() / it.videoFrameRate).toInt()
            assertThat(config.bitrate).isEqualTo(scaledBitrate)
            assertThat(config.resolution).isEqualTo(Size(it.videoFrameWidth, it.videoFrameHeight))
            assertThat(config.frameRate).isEqualTo(FIXED_FRAME_RATE)
        }
    }

    @Test
    fun bitrateIncreasesOrDecreasesWithIncreaseOrDecreaseInSurfaceSize() {
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)!!
        val surfaceSize = Size(profile.videoFrameWidth, profile.videoFrameHeight)

        val defaultBitrate = VideoEncoderConfigCamcorderProfileResolver(
            profile.videoCodecMimeType!!,
            defaultVideoSpec,
            surfaceSize,
            profile
        ).get().bitrate

        val increasedSurfaceSize = Size(surfaceSize.width + 100, surfaceSize.height + 100)
        val decreasedSurfaceSize = Size(surfaceSize.width - 100, surfaceSize.height - 100)

        assertThat(
            VideoEncoderConfigCamcorderProfileResolver(
                profile.videoCodecMimeType!!,
                defaultVideoSpec,
                increasedSurfaceSize,
                profile
            ).get().bitrate
        ).isGreaterThan(defaultBitrate)

        assertThat(
            VideoEncoderConfigCamcorderProfileResolver(
                profile.videoCodecMimeType!!,
                defaultVideoSpec,
                decreasedSurfaceSize,
                profile
            ).get().bitrate
        ).isLessThan(defaultBitrate)
    }

    @Test
    fun bitrateRangeInVideoSpecClampsBitrate() {
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)!!
        val surfaceSize = Size(profile.videoFrameWidth, profile.videoFrameHeight)

        val defaultBitrate = VideoEncoderConfigCamcorderProfileResolver(
            profile.videoCodecMimeType!!,
            defaultVideoSpec,
            surfaceSize,
            profile
        ).get().bitrate

        // Create video spec with limit 20% higher than default.
        val higherBitrate = (defaultBitrate * 1.2).toInt()
        val higherVideoSpec =
            VideoSpec.builder().setBitrate(Range(higherBitrate, Int.MAX_VALUE)).build()

        // Create video spec with limit 20% lower than default.
        val lowerBitrate = (defaultBitrate * 0.8).toInt()
        val lowerVideoSpec = VideoSpec.builder().setBitrate(Range(0, lowerBitrate)).build()

        assertThat(
            VideoEncoderConfigCamcorderProfileResolver(
                profile.videoCodecMimeType!!,
                higherVideoSpec,
                surfaceSize,
                profile
            ).get().bitrate
        ).isEqualTo(higherBitrate)

        assertThat(
            VideoEncoderConfigCamcorderProfileResolver(
                profile.videoCodecMimeType!!,
                lowerVideoSpec,
                surfaceSize,
                profile
            ).get().bitrate
        ).isEqualTo(lowerBitrate)
    }

    // TODO(b/177918193): We currently cannot communicate the frame rate to the camera,
    //  so we only support 30fps. Ensure the encoder config always is 30 so the encoder
    //  gets the correct frame rate.
    //  This test can be removed once setting the frame rate is supported.
    @Test
    fun frameRateIsAlways30() {
        // Give a VideoSpec with a frame rate higher than 30
        val videoSpec = VideoSpec.builder().setFrameRate(Range(60, 60)).build()
        val profile = videoCapabilities.getProfile(Quality.HIGHEST)!!
        val surfaceSize = Size(profile.videoFrameWidth, profile.videoFrameHeight)

        assertThat(
            VideoEncoderConfigCamcorderProfileResolver(
                profile.videoCodecMimeType!!,
                videoSpec,
                surfaceSize,
                profile
            ).get().frameRate
        ).isEqualTo(
            FIXED_FRAME_RATE
        )
    }
}
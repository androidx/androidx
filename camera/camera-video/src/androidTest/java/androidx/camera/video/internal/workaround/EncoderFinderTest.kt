/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.workaround

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.text.TextUtils
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.Timebase
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaCodecInfoReportIncorrectInfoQuirk
import androidx.camera.video.internal.config.VideoEncoderConfigVideoProfileResolver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class EncoderFinderTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
    private val lensFacing: Int,
    private var cameraSelector: CameraSelector,
    private var quality: Quality,
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    companion object {

        @JvmStatic
        private val cameraSelectors =
            arrayOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)

        @JvmStatic
        private val timebase = Timebase.UPTIME

        @JvmStatic
        private val availableQualities = arrayOf(
            Quality.SD,
            Quality.HD,
            Quality.FHD,
            Quality.UHD,
            Quality.LOWEST,
            Quality.HIGHEST,
        )

        @JvmStatic
        private val cameraxConfigs =
            listOf(Camera2Config::class.simpleName, CameraPipeConfig::class.simpleName)

        @JvmStatic
        @Parameterized.Parameters(name = "config={0}, lensFacing={2}, quality={4}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            cameraxConfigs.forEach { configImplName ->
                cameraSelectors.forEach { cameraSelector ->
                    availableQualities.forEach { quality ->
                        add(
                            arrayOf(
                                configImplName,
                                when (configImplName) {
                                    CameraPipeConfig::class.simpleName ->
                                        CameraPipeConfig.defaultConfig()
                                    Camera2Config::class.simpleName ->
                                        Camera2Config.defaultConfig()
                                    else -> Camera2Config.defaultConfig()
                                },
                                cameraSelector.lensFacing,
                                cameraSelector,
                                quality
                            )
                        )
                    }
                }
            }
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var camera: Camera

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        CameraXUtil.initialize(context, cameraConfig).get()
        camera = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
    }

    @After
    fun tearDown() {
        // Ensure all cameras are released for the next test
        CameraXUtil.shutdown()[10, TimeUnit.SECONDS]
    }

    @LabTestRule.LabTestOnly
    @Test
    fun findEncoderForFormat_EncoderProfiles() {
        // Arrange.
        val cameraInfo = camera.cameraInfo as CameraInfoInternal
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        for (dynamicRange in videoCapabilities.supportedDynamicRanges) {
            Assume.assumeTrue(
                "Quality $quality is not supported on the device.",
                videoCapabilities.isQualitySupported(quality, dynamicRange)
            )

            val encoderProfiles = videoCapabilities.getProfiles(quality, dynamicRange)
            val videoProfile = encoderProfiles!!.defaultVideoProfile
            val resolution = Size(videoProfile.width, videoProfile.height)
            val videoSpec = VideoSpec.builder()
                .setQualitySelector(QualitySelector.from(quality))
                .build()

            val mediaFormat = VideoEncoderConfigVideoProfileResolver(
                videoProfile.mediaType,
                timebase,
                videoSpec,
                resolution,
                videoProfile,
                SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
            ).get().toMediaFormat()

            // Act.
            val encoderName = EncoderFinder().findEncoderForFormat(
                mediaFormat,
                MediaCodecList(MediaCodecList.ALL_CODECS)
            )

            // Assert.
            assertTrue(
                "Cannot find video encoder & the device config is not listed in Quirk.",
                !TextUtils.isEmpty(encoderName) || isInQuirk(mediaFormat)
            )
        }
    }

    private fun isInQuirk(mediaFormat: MediaFormat): Boolean {
        val quirk = DeviceQuirks.get(
            MediaCodecInfoReportIncorrectInfoQuirk::class.java
        ) ?: return false
        return quirk.isUnSupportMediaCodecInfo(mediaFormat)
    }
}
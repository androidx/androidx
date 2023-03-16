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

package androidx.camera.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaCodecInfoReportIncorrectInfoQuirk
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test used to find out compatibility issue.
 *
 * All tests in this file should always pass. Every time we want to add a new test, it means there
 * is also a corresponding quirk in the codebase and we want to find more devices with the same
 * root cause. Tests should use [assumeTrue] to skip related quirks so that the problematic device
 * will pass the test. Once a new failure is found in the mobile harness test results, we should
 * add the device to the relevant quirk to pass the test.
 */
@SmallTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class DeviceCompatibilityTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    private val context: Context = ApplicationProvider.getApplicationContext()
    // TODO(b/278168212): Only SDR is checked by now. Need to extend to HDR dynamic ranges.
    private val dynamicRange = DynamicRange.SDR
    private val zeroRange by lazy { android.util.Range.create(0, 0) }

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @Before
    fun setup() {
        CameraXUtil.initialize(context, cameraConfig).get()
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun mediaCodecInfoShouldSupportEncoderProfilesSizes() {
        assumeTrue(DeviceQuirks.get(MediaCodecInfoReportIncorrectInfoQuirk::class.java) == null)

        // Arrange: Collect all supported profiles from default back/front camera.
        val supportedProfiles = mutableListOf<VideoValidatedEncoderProfilesProxy>()
        supportedProfiles.addAll(getSupportedProfiles(DEFAULT_BACK_CAMERA))
        supportedProfiles.addAll(getSupportedProfiles(DEFAULT_FRONT_CAMERA))
        assumeTrue(supportedProfiles.isNotEmpty())

        supportedProfiles.forEach { profile ->
            // Arrange: Find the codec and its video capabilities.
            // If mime is null, skip the test instead of failing it since this isn't the purpose
            // of the test.
            val videoProfile = profile.defaultVideoProfile
            val mime = videoProfile.mediaType
            if (mime == VideoProfileProxy.MEDIA_TYPE_NONE) {
                return@forEach
            }
            val capabilities = MediaCodec.createEncoderByType(mime).let { codec ->
                try {
                    codec.codecInfo.getCapabilitiesForType(mime).videoCapabilities
                } finally {
                    codec.release()
                }
            }

            // Act.
            val (width, height) = videoProfile.width to videoProfile.height
            val supportedWidths = capabilities.supportedWidths
            val supportedHeights = capabilities.supportedHeights
            val supportedWidthsForHeight = capabilities.getWidthsForHeightQuietly(height)
            val supportedHeightForWidth = capabilities.getHeightsForWidthQuietly(width)

            // Assert.
            val msg = "mime: $mime, size: ${width}x$height is not in " +
                "supported widths $supportedWidths/$supportedWidthsForHeight " +
                "or heights $supportedHeights/$supportedHeightForWidth, " +
                "the width/height alignment is " +
                "${capabilities.widthAlignment}/${capabilities.heightAlignment}."
            assertWithMessage(msg).that(width).isIn(supportedWidths.toClosed())
            assertWithMessage(msg).that(height).isIn(supportedHeights.toClosed())
            assertWithMessage(msg).that(width).isIn(supportedWidthsForHeight.toClosed())
            assertWithMessage(msg).that(height).isIn(supportedHeightForWidth.toClosed())
        }
    }

    private fun getSupportedProfiles(
        cameraSelector: CameraSelector
    ): List<VideoValidatedEncoderProfilesProxy> {
        if (!CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!)) {
            return emptyList()
        }
        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        return videoCapabilities.getSupportedQualities(dynamicRange).mapNotNull { quality ->
            videoCapabilities.getProfiles(quality, dynamicRange)
        }
    }

    private fun android.util.Range<Int>.toClosed() = Range.closed(lower, upper)

    private fun MediaCodecInfo.VideoCapabilities.getWidthsForHeightQuietly(height: Int):
        android.util.Range<Int> {
        return try {
            getSupportedWidthsFor(height)
        } catch (e: IllegalArgumentException) {
            zeroRange
        }
    }

    private fun MediaCodecInfo.VideoCapabilities.getHeightsForWidthQuietly(width: Int):
        android.util.Range<Int> {
        return try {
            getSupportedHeightsFor(width)
        } catch (e: IllegalArgumentException) {
            zeroRange
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }
}

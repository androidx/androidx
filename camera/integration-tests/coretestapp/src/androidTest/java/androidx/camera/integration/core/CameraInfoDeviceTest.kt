/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.os.Build
import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.ExperimentalHighSpeedVideo
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class CameraInfoDeviceTest(private val implName: String, private val cameraXConfig: CameraXConfig) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(
                if (implName == Camera2Config::class.simpleName) {
                    Camera2Config.defaultConfig()
                } else {
                    CameraPipeConfig.defaultConfig()
                }
            )
        )

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var camera: Camera
    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    @Before
    fun setUp() = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        withTimeout(10000) {
            ProcessCameraProvider.configureInstance(cameraXConfig)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        }
        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, null)
            cameraInfo = camera.cameraInfo as CameraInfoInternal
        }
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun allCamerasAdvertiseSdr() {
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            assertThat(cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED)))
                .contains(DynamicRange.SDR)
        }
    }

    @Test
    fun underSpecifiedDynamicRange_neverReturnedFromQuery() {
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            cameraInfo.querySupportedDynamicRanges(setOf(DynamicRange.UNSPECIFIED)).forEach {
                assertWithMessage("$cameraInfo advertises under-specified dynamic range: $it")
                    .that(it.isFullySpecified)
                    .isTrue()
            }
        }
    }

    @Test
    fun emptyCandidateDynamicRangeSet_throwsIllegalArgumentException() {
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            assertThrows(IllegalArgumentException::class.java) {
                cameraInfo.querySupportedDynamicRanges(emptySet())
            }
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM - 1)
    fun isTorchStrengthLevelSupported_returnFalseWhenApiNotMet() {
        assertThat(cameraProvider.getCameraInfo(cameraSelector).isTorchStrengthSupported).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getMaxTorchStrengthLevel_greaterThanOneWhenSupported() {
        assumeTrue(cameraInfo.isTorchStrengthSupported)

        assertThat(cameraInfo.maxTorchStrengthLevel).isGreaterThan(1)
    }

    @Test
    fun getMaxTorchStrengthLevel_returnUnsupported() {
        assumeTrue(!cameraInfo.isTorchStrengthSupported)

        assertThat(cameraInfo.maxTorchStrengthLevel)
            .isEqualTo(CameraInfo.TORCH_STRENGTH_LEVEL_UNSUPPORTED)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun getTorchStrengthLevel_returnValidValueWhenSupported() {
        assumeTrue(cameraInfo.isTorchStrengthSupported)

        val torchStrengthLevel = cameraInfo.torchStrengthLevel.value
        assertThat(torchStrengthLevel).isAtMost(cameraInfo.maxTorchStrengthLevel)
        assertThat(torchStrengthLevel).isAtLeast(1)
    }

    @Test
    fun getTorchStrengthLevel_returnUnsupported() {
        assumeTrue(!cameraInfo.isTorchStrengthSupported)

        assertThat(cameraInfo.torchStrengthLevel.value)
            .isEqualTo(CameraInfo.TORCH_STRENGTH_LEVEL_UNSUPPORTED)
    }

    @OptIn(ExperimentalSessionConfig::class)
    @Test
    fun getSupportedFrameRateRanges_withPreviewAndImageCapture_returnsValidSubset() {
        // Arrange.
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val useCases = listOf(preview, imageCapture)
        assumeTrue(camera.isUseCasesCombinationSupported(*useCases.toTypedArray()))
        val sessionConfig = SessionConfig(useCases)

        // Act.
        val allSupportedFps = cameraInfo.supportedFrameRateRanges
        val supportedFpsForSessionConfig = cameraInfo.getSupportedFrameRateRanges(sessionConfig)

        // Assert.
        assertThat(supportedFpsForSessionConfig).isNotEmpty()
        assertThat(allSupportedFps).containsAtLeastElementsIn(supportedFpsForSessionConfig)
    }

    @OptIn(ExperimentalSessionConfig::class)
    @Test
    fun getSupportedFrameRateRanges_withTargetFrameRateSet_returnsValidSubset() {
        // Arrange.
        val preview = Preview.Builder().setTargetFrameRate(Range.create(60, 60)).build()
        val imageCapture = ImageCapture.Builder().build()
        val useCases = listOf(preview, imageCapture)
        assumeTrue(camera.isUseCasesCombinationSupported(*useCases.toTypedArray()))
        val sessionConfig = SessionConfig(useCases)

        // Act.
        val allSupportedFps = cameraInfo.supportedFrameRateRanges
        val supportedFpsForSessionConfig = cameraInfo.getSupportedFrameRateRanges(sessionConfig)

        // Assert.
        assertThat(supportedFpsForSessionConfig).isNotEmpty()
        assertThat(allSupportedFps).containsAtLeastElementsIn(supportedFpsForSessionConfig)
    }

    @OptIn(ExperimentalSessionConfig::class)
    @Test
    fun getSupportedFrameRateRanges_withPreviewAndVideoCaptureUhd_returnsValidSubset() {
        // Arrange.
        val preview = Preview.Builder().build()
        val videoCapture =
            VideoCapture.withOutput(
                Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.UHD)).build()
            )
        val useCases = listOf(preview, videoCapture)
        assumeTrue(camera.isUseCasesCombinationSupported(*useCases.toTypedArray()))
        val sessionConfig = SessionConfig(useCases)

        // Act.
        val allSupportedFps = cameraInfo.supportedFrameRateRanges
        val supportedFpsForSessionConfig = cameraInfo.getSupportedFrameRateRanges(sessionConfig)

        // Assert.
        assertThat(supportedFpsForSessionConfig).isNotEmpty()
        assertThat(allSupportedFps).containsAtLeastElementsIn(supportedFpsForSessionConfig)
    }

    @OptIn(ExperimentalSessionConfig::class)
    @Test
    fun getSupportedFrameRateRanges_withStreamSharing_returnsValidSubset() {
        // Arrange.
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val useCases = listOf(preview, imageCapture, imageAnalysis, videoCapture)
        assumeTrue(camera.isUseCasesCombinationSupported(*useCases.toTypedArray()))
        val sessionConfig = SessionConfig(useCases)

        // Act.
        val allSupportedFps = cameraInfo.supportedFrameRateRanges
        val supportedFpsForSessionConfig = cameraInfo.getSupportedFrameRateRanges(sessionConfig)

        // Assert.
        assertThat(supportedFpsForSessionConfig).isNotEmpty()
        assertThat(allSupportedFps).containsAtLeastElementsIn(supportedFpsForSessionConfig)
    }

    @OptIn(ExperimentalSessionConfig::class, ExperimentalHighSpeedVideo::class)
    @Test
    fun getSupportedFrameRateRanges_withHighSpeedVideoSessionConfig() {
        // Arrange.
        val videoCapabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo)
        assumeTrue(videoCapabilities != null)

        val preview = Preview.Builder().build()
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val sessionConfig =
            HighSpeedVideoSessionConfig(videoCapture = videoCapture, preview = preview)

        // Act.
        val allSupportedFps = cameraInfo.supportedHighSpeedFrameRateRanges
        val supportedFpsForSessionConfig = cameraInfo.getSupportedFrameRateRanges(sessionConfig)

        // Assert.
        assertThat(supportedFpsForSessionConfig).isNotEmpty()
        assertThat(allSupportedFps).containsAtLeastElementsIn(supportedFpsForSessionConfig)
    }
}

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

package androidx.camera.video

import android.content.Context
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
import androidx.camera.video.internal.encoder.VideoEncoderInfoImpl
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
class RecorderVideoCapabilitiesTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var videoCapabilities: RecorderVideoCapabilities

    @Before
    fun setUp() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        val cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        CameraXUtil.initialize(context, cameraConfig).get()

        cameraInfo =
            CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
                as CameraInfoInternal
        videoCapabilities =
            RecorderVideoCapabilities(
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                cameraInfo,
                QUALITY_SOURCE_REGULAR,
                VideoEncoderInfoImpl.FINDER,
            )
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun supportStandardDynamicRange() {
        assumeFalse(isSpecificSkippedDevice())
        assertThat(videoCapabilities.supportedDynamicRanges).contains(SDR)
    }

    @Test
    fun supportedQualitiesOfSdrIsNotEmpty() {
        assumeFalse(isSpecificSkippedDevice())
        assertThat(videoCapabilities.getSupportedQualities(SDR)).isNotEmpty()
    }

    @Test
    fun whenCameraDoesNotSupportHighSpeed_highSpeedVideoCapabilitiesIsEmpty() {
        assumeFalse(cameraInfo.isHighSpeedSupported)

        for (sourceType in
            listOf(
                VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE,
                VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES,
            )) {
            val capabilities =
                RecorderVideoCapabilities(
                    sourceType,
                    cameraInfo,
                    Recorder.VIDEO_RECORDING_TYPE_HIGH_SPEED,
                    VideoEncoderInfoImpl.FINDER,
                )
            assertThat(capabilities.supportedDynamicRanges).isEmpty()
        }
    }

    private fun isSpecificSkippedDevice(): Boolean {
        // skip for b/231903433
        val isNokia2Point1 =
            "nokia".equals(Build.BRAND, true) && "nokia 2.1".equals(Build.MODEL, true)
        val isMotoE5Play =
            "motorola".equals(Build.BRAND, true) && "moto e5 play".equals(Build.MODEL, true)

        return isNokia2Point1 || isMotoE5Play
    }
}

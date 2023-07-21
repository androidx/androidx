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
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.testing.AndroidUtil
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
@SdkSuppress(minSdkVersion = 21)
class RecorderVideoCapabilitiesTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var videoCapabilities: RecorderVideoCapabilities

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        CameraXUtil.initialize(context, cameraConfig).get()

        val cameraInfo = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
        videoCapabilities = RecorderVideoCapabilities.from(cameraInfo)
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun supportStandardDynamicRange() {
        assumeFalse(isSpecificSkippedDevice())
        assumeFalse(AndroidUtil.isEmulatorAndAPI21())
        assertThat(videoCapabilities.supportedDynamicRanges).contains(SDR)
    }

    @Test
    fun supportedQualitiesOfSdrIsNotEmpty() {
        assumeFalse(isSpecificSkippedDevice())
        assumeFalse(AndroidUtil.isEmulatorAndAPI21())
        assertThat(videoCapabilities.getSupportedQualities(SDR)).isNotEmpty()
    }

    private fun isSpecificSkippedDevice(): Boolean {
        // skip for b/231903433
        val isNokia2Point1 = "nokia".equals(Build.BRAND, true) &&
            "nokia 2.1".equals(Build.MODEL, true)
        val isMotoE5Play = "motorola".equals(Build.BRAND, true) &&
            "moto e5 play".equals(Build.MODEL, true)

        return isNokia2Point1 || isMotoE5Play
    }
}
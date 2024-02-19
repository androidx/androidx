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

package androidx.camera.integration.extensions.camera2extensions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.assumeCameraExtensionSupported
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.testing.impl.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Tests for checking mandatory support of certain extensions capabilities. */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 33)
class Camera2ExtensionsCapabilitiesTest(private val config: CameraIdExtensionModePair) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    companion object {
        val context = ApplicationProvider.getApplicationContext<Context>()

        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() = Camera2ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var extensionsCharacteristics: CameraExtensionCharacteristics
    private lateinit var characteristics: CameraCharacteristics

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(Camera2ExtensionsTestUtil.isTargetDeviceExcludedForExtensionsTest())
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)

        val (cameraId, extensionMode) = config

        extensionsCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        characteristics = cameraManager.getCameraCharacteristics(cameraId)
        assumeCameraExtensionSupported(extensionMode, extensionsCharacteristics)
    }

    @Test
    fun getAvailableCaptureRequestKeys_supportsZoom_after1_3() {
        val keys = extensionsCharacteristics.getAvailableCaptureRequestKeys(config.extensionMode)
        Truth.assertThat(keys)
            .containsAnyOf(CaptureRequest.CONTROL_ZOOM_RATIO, CaptureRequest.SCALER_CROP_REGION)
    }

    @Test
    fun getAvailableCaptureRequestKeys_supportsAutoFocus_after1_3() {
        // Assume this is not a fixed lens
        val minFocusDistance =
            characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        assumeTrue(minFocusDistance != null && minFocusDistance > 0f)

        val keys = extensionsCharacteristics.getAvailableCaptureRequestKeys(config.extensionMode)
        val requiredAutoFocusKeys =
            listOf(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_REGIONS,
            )
        Truth.assertThat(keys).containsAtLeastElementsIn(requiredAutoFocusKeys)
    }

    @Test
    fun getAvailableCaptureResultKeys_supportsAutoFocus_after1_3() {
        // Assume this is not a fixed lens
        val minFocusDistance =
            characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        assumeTrue(minFocusDistance != null && minFocusDistance > 0f)

        val keys = extensionsCharacteristics.getAvailableCaptureResultKeys(config.extensionMode)
        Truth.assertThat(keys).contains(CaptureResult.CONTROL_AF_STATE)
    }
}

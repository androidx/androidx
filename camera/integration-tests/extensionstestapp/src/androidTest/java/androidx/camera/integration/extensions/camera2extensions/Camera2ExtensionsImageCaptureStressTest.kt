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

package androidx.camera.integration.extensions.camera2extensions

import android.content.Context
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.media.ImageReader
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.assumeCameraExtensionSupported
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.createCaptureImageReader
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.openCameraDevice
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.openExtensionSession
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil.takePicture
import androidx.camera.integration.extensions.util.assertImageIsValid
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.StressTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Stress test to verify that the camera can successfully capture images for all supported
 * extension modes for each cameras ID.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 31)
class Camera2ExtensionsImageCaptureStressTest(private val config: CameraIdExtensionModePair) {
    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() = Camera2ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageReader: ImageReader
    private lateinit var captureSurface: Surface
    private lateinit var extensionSession: CameraExtensionSession

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(Camera2ExtensionsTestUtil.isTargetDeviceExcludedForExtensionsTest())

        val (cameraId, extensionMode) = config

        val extensionsCharacteristics = cameraManager.getCameraExtensionCharacteristics(cameraId)
        assumeCameraExtensionSupported(extensionMode, extensionsCharacteristics)

        cameraDevice = openCameraDevice(cameraManager, cameraId)
        imageReader = createCaptureImageReader(extensionsCharacteristics, extensionMode)
        captureSurface = imageReader.surface
        val outputConfigurationCapture = OutputConfiguration(captureSurface)
        extensionSession = openExtensionSession(
            cameraDevice,
            extensionMode,
            listOf(outputConfigurationCapture)
        )
        assertThat(extensionSession).isNotNull()
    }

    @After
    fun tearDown() {
        if (::extensionSession.isInitialized) {
            extensionSession.close()
        }

        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }

        if (::imageReader.isInitialized) {
            imageReader.close()
        }

        if (::captureSurface.isInitialized) {
            captureSurface.release()
        }
    }

    @Test
    fun captureImage(): Unit = runBlocking {
        repeat(Camera2ExtensionsTestUtil.getStressTestRepeatingCount()) {
            val image = takePicture(cameraDevice, extensionSession, imageReader)
            assertThat(image).isNotNull()

            image?.let {
                assertThat(it.timestamp).isGreaterThan(0)
                assertImageIsValid(it, imageReader.width, imageReader.height)
            }

            image!!.close()
        }
    }
}

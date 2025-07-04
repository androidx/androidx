/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.extensions.CameraExtensionsInfo
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.internal.Camera2ExtensionsUtil
import androidx.camera.extensions.internal.ExtensionVersion
import androidx.camera.extensions.internal.ExtensionsUtils
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.CameraXExtensionTestParams
import androidx.camera.integration.extensions.utils.CameraSelectorUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CameraExtensionsInfoTest(private val config: CameraXExtensionTestParams) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = config.implName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            PreTestCameraIdList(config.cameraXConfig)
        )

    @get:Rule
    val permissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    companion object {
        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraExtensionsInfo: CameraExtensionsInfo
    private lateinit var baseCameraSelector: CameraSelector
    private lateinit var extensionCameraSelector: CameraSelector
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var camera: Camera
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture

    @Before
    fun setup() {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())

        ProcessCameraProvider.configureInstance(config.cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        baseCameraSelector = CameraSelectorUtil.createCameraSelectorById(config.cameraId)
        assumeTrue(extensionsManager.isExtensionAvailable(baseCameraSelector, config.extensionMode))

        extensionCameraSelector =
            extensionsManager.getExtensionEnabledCameraSelector(
                baseCameraSelector,
                config.extensionMode,
            )

        instrumentation.runOnMainSync {
            fakeLifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
            preview = Preview.Builder().build()
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
            imageCapture = ImageCapture.Builder().build()
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    extensionCameraSelector,
                    preview,
                    imageCapture,
                )
        }

        cameraExtensionsInfo = extensionsManager.getCameraExtensionsInfo(camera.cameraInfo)
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()
        }

        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()
        }
    }

    @Test
    fun isExtensionStrengthAvailable_returnCorrectValue() {
        val available =
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    config.implName == CAMERA_PIPE_IMPLEMENTATION_OPTION
            ) {
                isCamera2ExtensionStrengthSupported()
            } else if (ExtensionVersion.isAdvancedExtenderSupported()) {
                isAdvancedExtenderExtensionStrengthSupported()
            } else {
                false
            }
        assertThat(cameraExtensionsInfo.isExtensionStrengthAvailable).isEqualTo(available)

        if (available) {
            // Getting the extension strength value should not cause exception
            cameraExtensionsInfo.extensionStrength!!.value
        } else {
            assertThat(cameraExtensionsInfo.extensionStrength).isNull()
        }
    }

    private fun isCamera2ExtensionStrengthSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).let {
                val characteristics = it.getCameraExtensionCharacteristics(config.cameraId)
                val camera2ExtensionMode =
                    Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode(config.extensionMode)

                return characteristics
                    .getAvailableCaptureRequestKeys(camera2ExtensionMode)
                    .contains(CaptureRequest.EXTENSION_STRENGTH)
            }
        }
        return false
    }

    private fun isAdvancedExtenderExtensionStrengthSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return CameraXExtensionsTestUtil.createAdvancedExtenderImpl(
                    config.extensionMode,
                    config.cameraId,
                    camera.cameraInfo,
                )
                .apply {
                    init(
                        config.cameraId,
                        ExtensionsUtils.getCameraCharacteristicsMap(
                            camera.cameraInfo as CameraInfoInternal
                        ),
                    )
                }
                .availableCaptureRequestKeys
                .contains(CaptureRequest.EXTENSION_STRENGTH)
        }
        return false
    }

    @Test
    fun isCurrentExtensionModeAvailable_returnCorrectValue() {
        val available =
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    config.implName == CAMERA_PIPE_IMPLEMENTATION_OPTION
            ) {
                isCamera2CurrentExtensionModeSupported()
            } else if (ExtensionVersion.isAdvancedExtenderSupported()) {
                isAdvancedExtenderCurrentExtensionModeSupported()
            } else {
                false
            }
        assertThat(cameraExtensionsInfo.isCurrentExtensionModeAvailable).isEqualTo(available)

        if (available) {
            // Getting the current extension type value should not cause exception
            cameraExtensionsInfo.currentExtensionMode!!.value
        } else {
            assertThat(cameraExtensionsInfo.currentExtensionMode).isNull()
        }
    }

    private fun isCamera2CurrentExtensionModeSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).let {
                val characteristics = it.getCameraExtensionCharacteristics(config.cameraId)
                val camera2ExtensionMode =
                    Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode(config.extensionMode)

                return characteristics
                    .getAvailableCaptureResultKeys(camera2ExtensionMode)
                    .contains(CaptureResult.EXTENSION_CURRENT_TYPE)
            }
        }
        return false
    }

    private fun isAdvancedExtenderCurrentExtensionModeSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return CameraXExtensionsTestUtil.createAdvancedExtenderImpl(
                    config.extensionMode,
                    config.cameraId,
                    camera.cameraInfo,
                )
                .apply {
                    init(
                        config.cameraId,
                        ExtensionsUtils.getCameraCharacteristicsMap(
                            camera.cameraInfo as CameraInfoInternal
                        ),
                    )
                }
                .availableCaptureResultKeys
                .contains(CaptureResult.EXTENSION_CURRENT_TYPE)
        }
        return false
    }
}

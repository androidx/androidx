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

package androidx.camera.integration.extensions

import android.Manifest
import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA2_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.CameraExtensionsActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.HOME_TIMEOUT_MS
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.integration.extensions.utils.ExtensionModeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.StressTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val DEFAULT_BACK_CAMERA_ID = "0"

/** Stress tests to verify that Preview and ImageCapture can work well when switching cameras. */
@LargeTest
@RunWith(Parameterized::class)
class SwitchCameraStressTest(
    private val configName: String,
    private val cameraXConfig: CameraXConfig,
    private val extensionMode: Int
) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = configName == CAMERA_PIPE_IMPLEMENTATION_OPTION)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraXConfig))

    @get:Rule
    val permissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private var startingExtensionMode: Int = extensionMode

    companion object {
        @ClassRule @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "cameraXConfig = {0}, extensionMode = {2}")
        @JvmStatic
        fun parameters(): List<Array<Any>> {
            return ExtensionModeUtil.AVAILABLE_EXTENSION_MODES.flatMap { extensionMode ->
                listOf(
                    arrayOf(
                        CAMERA2_IMPLEMENTATION_OPTION,
                        Camera2Config.defaultConfig(),
                        extensionMode
                    ),
                    arrayOf(
                        CAMERA_PIPE_IMPLEMENTATION_OPTION,
                        CameraPipeConfig.defaultConfig(),
                        extensionMode
                    ),
                )
            }
        }
    }

    private var isTestStarted = false

    @Before
    fun setup() {
        assumeTrue(CameraUtil.deviceHasCamera())
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        ProcessCameraProvider.configureInstance(cameraXConfig)
        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]

        val isBackCameraSupported =
            extensionsManager.isExtensionAvailable(
                CameraSelector.DEFAULT_BACK_CAMERA,
                extensionMode
            )
        val isFrontCameraSupported =
            extensionsManager.isExtensionAvailable(
                CameraSelector.DEFAULT_FRONT_CAMERA,
                extensionMode
            )

        // Checks whether the extension mode can be supported first before launching the activity.
        // Only runs the test when at least one of the back or front cameras support the target
        // testing extension mode
        assumeTrue(isBackCameraSupported || isFrontCameraSupported)

        if (!isBackCameraSupported) {
            startingExtensionMode =
                CameraXExtensionsTestUtil.getFirstSupportedExtensionMode(
                    extensionsManager,
                    DEFAULT_BACK_CAMERA_ID
                )
        }

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()
        isTestStarted = true
    }

    @After
    fun tearDown() {
        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        cameraProvider.shutdownAsync()

        val extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider)[
                    10000, TimeUnit.MILLISECONDS]
        extensionsManager.shutdown()

        if (isTestStarted) {
            // Unfreeze rotation so the device can choose the orientation via its own policy. Be
            // nice
            // to other tests :)
            device.unfreezeRotation()
            device.pressHome()
            device.waitForIdle(HOME_TIMEOUT_MS)
        }
    }

    @Test
    fun switchCameraTenTimes_canCaptureImageInEachTime() {
        val activityScenario =
            launchCameraExtensionsActivity(DEFAULT_BACK_CAMERA_ID, startingExtensionMode)

        with(activityScenario) {
            use {
                repeat(CameraXExtensionsTestUtil.getStressTestRepeatingCount()) {
                    // Waits for the take picture success callback.
                    takePictureAndWaitForImageSavedIdle()

                    withActivity {
                        // Switches camera
                        switchCameras()
                        // Switches to the target testing extension mode as possible because some
                        // extension modes may not be supported in some lens facing of cameras.
                        switchToExtensionMode(extensionMode)
                    }

                    // Waits for preview view turned to STREAMING state after switching camera
                    waitForPreviewViewStreaming()
                }
            }
        }
    }

    @Test
    fun canCaptureImage_afterSwitchCameraTenTimes() {
        val activityScenario =
            launchCameraExtensionsActivity(DEFAULT_BACK_CAMERA_ID, startingExtensionMode)

        with(activityScenario) {
            use {
                repeat(CameraXExtensionsTestUtil.getStressTestRepeatingCount()) {
                    withActivity {
                        // Switches camera
                        switchCameras()
                        // Switches to the target testing extension mode as possible because some
                        // extension modes may not be supported in some lens facing of cameras.
                        switchToExtensionMode(extensionMode)
                    }
                }

                // Waits for the take picture success callback.
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }
}
